package com.prio.app.feature.tasks.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.data.local.entity.TaskEntity
import com.prio.core.data.repository.GoalRepository
import com.prio.core.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for TaskDetailSheet.
 * 
 * Implements 3.1.4 from ACTION_PLAN.md:
 * - Task detail viewing and editing
 * - AI explanation display
 * - Quadrant override
 * - Goal linking
 * - Delete with undo support
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Task ID passed via navigation
    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: 0L
    
    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()
    
    private val _effect = Channel<TaskDetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    
    // Original task for undo
    private var originalTask: TaskEntity? = null

    /**
     * Active goals for the goal-picker dialog.
     * Maps GoalEntity → LinkedGoalInfo so the UI doesn't import data-layer types.
     */
    val availableGoals: StateFlow<List<LinkedGoalInfo>> =
        goalRepository.getAllActiveGoals()
            .map { goals ->
                goals.map { goal ->
                    LinkedGoalInfo(
                        id = goal.id,
                        title = goal.title,
                        progress = goal.progress.toInt(),
                        category = goal.category.name
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    init {
        if (taskId > 0) {
            loadTask(taskId)
        }
    }
    
    /**
     * Load task for new task ID (can be called from outside).
     */
    fun loadTask(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val task = taskRepository.getTaskById(id)
                if (task != null) {
                    originalTask = task
                    
                    // Load linked goal if exists
                    val linkedGoal = task.goalId?.let { goalId ->
                        goalRepository.getGoalById(goalId)?.let { goal ->
                            LinkedGoalInfo(
                                id = goal.id,
                                title = goal.title,
                                progress = goal.progress.toInt(),
                                category = goal.category.name
                            )
                        }
                    }
                    
                    // Load subtasks
                    val subtasks = taskRepository.getSubtasks(id).firstOrNull()?.map { subtask ->
                        SubtaskUiModel(
                            id = subtask.id,
                            title = subtask.title,
                            isCompleted = subtask.isCompleted
                        )
                    } ?: emptyList()
                    
                    _uiState.update { current ->
                        current.copy(
                            id = task.id,
                            title = task.title,
                            notes = task.notes,
                            quadrant = task.quadrant,
                            aiExplanation = task.aiExplanation,
                            isCompleted = task.isCompleted,
                            completedAt = task.completedAt?.let { formatCompletedDate(it) },
                            dueDateFormatted = task.dueDate?.let { formatDueDate(it) } ?: "No due date",
                            linkedGoal = linkedGoal,
                            subtasks = subtasks,
                            isLoading = false
                        )
                    }
                } else {
                    _effect.send(TaskDetailEffect.ShowError("Task not found"))
                    _effect.send(TaskDetailEffect.Dismiss)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(TaskDetailEffect.ShowError("Failed to load task: ${e.message}"))
            }
        }
    }
    
    /**
     * Handle UI events.
     */
    fun onEvent(event: TaskDetailEvent) {
        when (event) {
            is TaskDetailEvent.UpdateTitle -> updateTitle(event.title)
            is TaskDetailEvent.UpdateNotes -> updateNotes(event.notes)
            is TaskDetailEvent.UpdateQuadrant -> updateQuadrant(event.quadrant)
            is TaskDetailEvent.UpdateDueDate -> updateDueDate(event.dateMillis)
            is TaskDetailEvent.UpdateGoalLink -> updateGoalLink(event.goalId)
            is TaskDetailEvent.ToggleComplete -> toggleComplete()
            is TaskDetailEvent.Delete -> { /* Show confirmation handled in UI */ }
            is TaskDetailEvent.ConfirmDelete -> deleteTask()
            is TaskDetailEvent.DismissDelete -> { /* Handled in UI */ }
            is TaskDetailEvent.OpenDueDatePicker -> openDueDatePicker()
            is TaskDetailEvent.OpenGoalPicker -> openGoalPicker()
            is TaskDetailEvent.OpenRecurrencePicker -> openRecurrencePicker()
            is TaskDetailEvent.OpenReminderPicker -> openReminderPicker()
            is TaskDetailEvent.ToggleSubtask -> toggleSubtask(event.subtaskId)
            is TaskDetailEvent.DeleteSubtask -> deleteSubtask(event.subtaskId)
            is TaskDetailEvent.AddSubtask -> addSubtask(event.title)
            is TaskDetailEvent.Dismiss -> dismiss()
            is TaskDetailEvent.Save -> saveChanges()
            is TaskDetailEvent.ToggleEditing -> toggleEditing()
            is TaskDetailEvent.DuplicateTask -> duplicateTask()
            is TaskDetailEvent.CopyToClipboard -> copyToClipboard()
        }
    }
    
    private fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, isEditing = true) }
    }
    
    private fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes.ifBlank { null }, isEditing = true) }
    }
    
    private fun updateQuadrant(quadrant: EisenhowerQuadrant) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val originalQuadrant = originalTask?.quadrant
            
            // Track override for learning
            if (originalQuadrant != null && originalQuadrant != quadrant) {
                // TODO: Store override for ML improvement
                // trackQuadrantOverride(originalQuadrant, quadrant, currentState.title)
            }
            
            _uiState.update { it.copy(quadrant = quadrant, isEditing = true) }
            
            // Auto-save quadrant change
            try {
                taskRepository.updateQuadrant(currentState.id, quadrant)
                _effect.send(TaskDetailEffect.ShowSnackbar(
                    "Priority changed to ${quadrant.name.replace("_", " ")}"
                ))
            } catch (e: Exception) {
                _effect.send(TaskDetailEffect.ShowError("Failed to update priority"))
            }
        }
    }

    /**
     * Update due date from date picker.
     * Persists immediately and refreshes the formatted label.
     */
    private fun updateDueDate(dateMillis: Long?) {
        viewModelScope.launch {
            try {
                val currentId = _uiState.value.id
                val newDueDate = dateMillis?.let { Instant.fromEpochMilliseconds(it) }
                taskRepository.updateTaskDueDate(currentId, newDueDate)
                _uiState.update {
                    it.copy(
                        dueDateFormatted = newDueDate?.let { d -> formatDueDate(d) } ?: "No due date"
                    )
                }
                originalTask = originalTask?.copy(dueDate = newDueDate)
                _effect.send(TaskDetailEffect.ShowSnackbar(
                    newDueDate?.let { "Due date updated" } ?: "Due date cleared"
                ))
            } catch (e: Exception) {
                _effect.send(TaskDetailEffect.ShowError("Failed to update due date"))
            }
        }
    }

    /**
     * Link or unlink a goal.
     * Persists immediately and refreshes the linked goal info.
     */
    private fun updateGoalLink(goalId: Long?) {
        viewModelScope.launch {
            try {
                val currentId = _uiState.value.id
                val task = originalTask?.copy(goalId = goalId) ?: return@launch
                taskRepository.updateTask(task)
                originalTask = task

                val linkedGoal = goalId?.let { id ->
                    goalRepository.getGoalById(id)?.let { goal ->
                        LinkedGoalInfo(
                            id = goal.id,
                            title = goal.title,
                            progress = goal.progress.toInt(),
                            category = goal.category.name
                        )
                    }
                }
                _uiState.update { it.copy(linkedGoal = linkedGoal) }
                _effect.send(TaskDetailEffect.ShowSnackbar(
                    if (goalId != null) "Goal linked" else "Goal unlinked"
                ))
            } catch (e: Exception) {
                _effect.send(TaskDetailEffect.ShowError("Failed to update goal"))
            }
        }
    }
    
    private fun toggleComplete() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val newCompletedState = !currentState.isCompleted
            
            _uiState.update { 
                it.copy(
                    isCompleted = newCompletedState,
                    completedAt = if (newCompletedState) {
                        formatCompletedDate(clock.now())
                    } else null
                )
            }
            
            try {
                if (newCompletedState) {
                    taskRepository.completeTask(currentState.id)
                    _effect.send(TaskDetailEffect.ShowSnackbar(
                        message = "Task completed",
                        actionLabel = "Undo"
                    ))
                } else {
                    taskRepository.uncompleteTask(currentState.id)
                    _effect.send(TaskDetailEffect.ShowSnackbar("Task marked incomplete"))
                }
            } catch (e: Exception) {
                // Revert UI state
                _uiState.update { 
                    it.copy(
                        isCompleted = !newCompletedState,
                        completedAt = originalTask?.completedAt?.let { formatCompletedDate(it) }
                    )
                }
                _effect.send(TaskDetailEffect.ShowError("Failed to update task"))
            }
        }
    }
    
    private fun deleteTask() {
        viewModelScope.launch {
            try {
                val taskToDelete = originalTask ?: return@launch
                
                // Delete task (undo handled at UI layer with 5s snackbar)
                taskRepository.deleteTaskById(_uiState.value.id)
                
                _effect.send(TaskDetailEffect.ShowSnackbar(
                    message = "Task deleted",
                    actionLabel = "Undo"
                ))
                _effect.send(TaskDetailEffect.Dismiss)
                _effect.send(TaskDetailEffect.TaskDeleted(taskToDelete.id))
            } catch (e: Exception) {
                _effect.send(TaskDetailEffect.ShowError("Failed to delete task"))
            }
        }
    }
    
    private fun openDueDatePicker() {
        viewModelScope.launch {
            _effect.send(TaskDetailEffect.OpenDatePicker(_uiState.value.id))
        }
    }
    
    private fun openGoalPicker() {
        viewModelScope.launch {
            _effect.send(TaskDetailEffect.OpenGoalPicker(_uiState.value.id))
        }
    }
    
    private fun openRecurrencePicker() {
        viewModelScope.launch {
            _effect.send(TaskDetailEffect.OpenRecurrencePicker(_uiState.value.id))
        }
    }
    
    private fun openReminderPicker() {
        viewModelScope.launch {
            _effect.send(TaskDetailEffect.OpenReminderPicker(_uiState.value.id))
        }
    }
    
    private fun toggleSubtask(subtaskId: Long) {
        viewModelScope.launch {
            val subtasks = _uiState.value.subtasks.toMutableList()
            val index = subtasks.indexOfFirst { it.id == subtaskId }
            if (index != -1) {
                val subtask = subtasks[index]
                subtasks[index] = subtask.copy(isCompleted = !subtask.isCompleted)
                _uiState.update { it.copy(subtasks = subtasks) }
                
                // Save to repository
                try {
                    if (subtasks[index].isCompleted) {
                        taskRepository.completeTask(subtaskId)
                    } else {
                        taskRepository.uncompleteTask(subtaskId)
                    }
                } catch (e: Exception) {
                    // Revert on error
                    subtasks[index] = subtask
                    _uiState.update { it.copy(subtasks = subtasks) }
                }
            }
        }
    }
    
    private fun deleteSubtask(subtaskId: Long) {
        viewModelScope.launch {
            val subtasks = _uiState.value.subtasks.toMutableList()
            val removed = subtasks.removeAll { it.id == subtaskId }
            if (removed) {
                _uiState.update { it.copy(subtasks = subtasks) }
                
                try {
                    taskRepository.deleteTaskById(subtaskId)
                } catch (e: Exception) {
                    // Reload subtasks on error
                    loadTask(_uiState.value.id)
                }
            }
        }
    }
    
    private fun addSubtask(title: String) {
        viewModelScope.launch {
            try {
                val parentId = _uiState.value.id

                // Preserve in-flight edits (notes, title, quadrant) before reload
                val pendingTitle = _uiState.value.title
                val pendingNotes = _uiState.value.notes
                val pendingQuadrant = _uiState.value.quadrant
                val wasEditing = _uiState.value.isEditing

                // Auto-save current edits so they aren't lost
                if (wasEditing) {
                    val task = originalTask?.copy(
                        title = pendingTitle,
                        notes = pendingNotes,
                        quadrant = pendingQuadrant
                    )
                    if (task != null) {
                        taskRepository.updateTask(task)
                        originalTask = task
                    }
                }

                taskRepository.createTask(
                    title = title,
                    parentTaskId = parentId
                )
                // Reload to get updated subtasks
                loadTask(parentId)

                // Restore editing state after reload
                if (wasEditing) {
                    _uiState.update {
                        it.copy(
                            title = pendingTitle,
                            notes = pendingNotes,
                            quadrant = pendingQuadrant,
                            isEditing = true
                        )
                    }
                }
            } catch (e: Exception) {
                _effect.send(TaskDetailEffect.ShowError("Failed to add subtask"))
            }
        }
    }
    
    private fun dismiss() {
        viewModelScope.launch {
            // Auto-save if there are changes
            if (_uiState.value.isEditing) {
                saveChanges()
            }
            _effect.send(TaskDetailEffect.Dismiss)
        }
    }
    
    private fun saveChanges() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.isEditing) return@launch
            
            try {
                val task = originalTask?.copy(
                    title = state.title,
                    notes = state.notes,
                    quadrant = state.quadrant
                ) ?: return@launch
                
                taskRepository.updateTask(task)
                _uiState.update { it.copy(isEditing = false) }
            } catch (e: Exception) {
                _effect.send(TaskDetailEffect.ShowError("Failed to save changes"))
            }
        }
    }
    
    /**
     * Toggle editing mode.
     * Per 1.1.2: overflow "Edit" toggles inline editing.
     */
    private fun toggleEditing() {
        val currentlyEditing = _uiState.value.isEditing
        if (currentlyEditing) {
            // Exiting edit mode — auto-save
            saveChanges()
        }
        _uiState.update { it.copy(isEditing = !currentlyEditing) }
    }
    
    /**
     * Duplicate the current task.
     * Per 1.1.2: overflow "Duplicate" creates a copy of this task.
     */
    private fun duplicateTask() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val newTaskId = taskRepository.createTask(
                    title = "${state.title} (copy)",
                    notes = state.notes,
                    dueDate = originalTask?.dueDate,
                    quadrant = state.quadrant,
                    aiExplanation = state.aiExplanation,
                    goalId = state.linkedGoal?.id
                )
                _effect.send(TaskDetailEffect.ShowSnackbar(
                    message = "Task duplicated",
                    actionLabel = "View"
                ))
            } catch (e: Exception) {
                _effect.send(TaskDetailEffect.ShowError("Failed to duplicate task"))
            }
        }
    }
    
    /**
     * Copy task title + notes to clipboard.
     * Per 1.1.2: overflow "Copy to Clipboard".
     */
    private fun copyToClipboard() {
        viewModelScope.launch {
            val state = _uiState.value
            val clipText = buildString {
                append(state.title)
                state.notes?.let { notes ->
                    append("\n\n")
                    append(notes)
                }
            }
            _effect.send(TaskDetailEffect.CopyToClipboard(clipText))
            _effect.send(TaskDetailEffect.ShowSnackbar(message = "Copied to clipboard"))
        }
    }
    
    private fun formatDueDate(instant: Instant): String {
        val now = clock.now()
        val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        
        return when {
            localDate == today -> "Today"
            localDate == tomorrow -> "Tomorrow"
            instant < now -> "Overdue"
            else -> {
                // Format as "Feb 4, 2026"
                "${localDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${localDate.dayOfMonth}, ${localDate.year}"
            }
        }
    }
    
    private fun formatCompletedDate(instant: Instant): String {
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = local.hour
        val minute = local.minute.toString().padStart(2, '0')
        val amPm = if (hour < 12) "AM" else "PM"
        val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        
        return "${local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${local.dayOfMonth}, ${local.year} at $hour12:$minute $amPm"
    }
}

/**
 * Side effects from TaskDetailViewModel.
 */
sealed interface TaskDetailEffect {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : TaskDetailEffect
    data class ShowError(val message: String) : TaskDetailEffect
    object Dismiss : TaskDetailEffect
    data class TaskDeleted(val taskId: Long) : TaskDetailEffect
    data class OpenDatePicker(val taskId: Long) : TaskDetailEffect
    data class OpenGoalPicker(val taskId: Long) : TaskDetailEffect
    data class OpenRecurrencePicker(val taskId: Long) : TaskDetailEffect
    data class OpenReminderPicker(val taskId: Long) : TaskDetailEffect
    data class CopyToClipboard(val text: String) : TaskDetailEffect
}

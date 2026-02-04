package com.prio.app.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.data.local.entity.TaskEntity
import com.prio.core.data.repository.GoalRepository
import com.prio.core.data.repository.TaskRepository
import com.prio.core.domain.eisenhower.EisenhowerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * ViewModel for TaskListScreen.
 * 
 * Implements task 3.1.2: Task List screen with filters
 * Per 1.1.1 Task List Screen Specification.
 * 
 * Features:
 * - LazyColumn with quadrant sections (DO FIRST, SCHEDULE, DELEGATE, MAYBE LATER)
 * - Quadrant badges with correct colors
 * - Overdue indicator (red left border)
 * - Sort by Q1→Q4
 * - 60fps scroll performance
 * - Empty states per 1.1.10
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository,
    private val eisenhowerEngine: EisenhowerEngine,
    private val clock: Clock
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()
    
    private val _effect = Channel<TaskListEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    
    // For undo functionality
    private var lastCompletedTask: TaskEntity? = null
    private var lastDeletedTask: TaskEntity? = null
    
    // Section collapse state
    private val sectionCollapseState = MutableStateFlow(
        mapOf(
            EisenhowerQuadrant.DO_FIRST to true,     // Always expanded
            EisenhowerQuadrant.SCHEDULE to true,
            EisenhowerQuadrant.DELEGATE to true,
            EisenhowerQuadrant.ELIMINATE to true
        )
    )
    
    init {
        observeTasks()
    }
    
    /**
     * Observe tasks from repository and transform to UI state.
     */
    private fun observeTasks() {
        combine(
            taskRepository.getAllTasks(),
            sectionCollapseState
        ) { tasks, collapseState ->
            transformToUiState(tasks, collapseState)
        }.launchIn(viewModelScope)
    }
    
    /**
     * Transform task entities to UI state with sections.
     */
    private suspend fun transformToUiState(
        tasks: List<TaskEntity>,
        collapseState: Map<EisenhowerQuadrant, Boolean>
    ) {
        val now = clock.now()
        val showCompleted = _uiState.value.showCompletedTasks
        val searchQuery = _uiState.value.searchQuery
        val filter = _uiState.value.selectedFilter
        
        // Filter tasks
        var filteredTasks = tasks
        
        // Hide completed unless toggled
        if (!showCompleted) {
            filteredTasks = filteredTasks.filter { !it.isCompleted }
        }
        
        // Apply search
        if (searchQuery.isNotBlank()) {
            filteredTasks = filteredTasks.filter { task ->
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.notes?.contains(searchQuery, ignoreCase = true) == true
            }
        }
        
        // Apply filter
        filteredTasks = when (filter) {
            TaskFilter.All -> filteredTasks
            TaskFilter.Today -> filteredTasks.filter { task ->
                task.dueDate?.let { dueDate ->
                    val todayStart = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    val taskDate = dueDate.toLocalDateTime(TimeZone.currentSystemDefault()).date
                    todayStart == taskDate
                } ?: false
            }
            TaskFilter.Upcoming -> filteredTasks.filter { task ->
                task.dueDate?.let { dueDate ->
                    val todayStart = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    val taskDate = dueDate.toLocalDateTime(TimeZone.currentSystemDefault()).date
                    taskDate > todayStart
                } ?: false
            }
            TaskFilter.HasGoal -> filteredTasks.filter { it.goalId != null }
            TaskFilter.Recurring -> filteredTasks.filter { it.isRecurring }
        }
        
        // Group by quadrant with sort order: DO_FIRST → SCHEDULE → DELEGATE → ELIMINATE
        val sections = EisenhowerQuadrant.entries.mapNotNull { quadrant ->
            val quadrantTasks = filteredTasks
                .filter { it.quadrant == quadrant }
                .sortedWith(
                    compareByDescending<TaskEntity> { it.urgencyScore }
                        .thenBy { it.dueDate }
                        .thenBy { it.position }
                        .thenBy { it.createdAt }
                )
                .map { entity -> mapToUiModel(entity, now) }
            
            if (quadrantTasks.isEmpty() && !showCompleted) {
                null // Hide empty sections
            } else {
                TaskSection(
                    quadrant = quadrant,
                    tasks = quadrantTasks,
                    isExpanded = collapseState[quadrant] ?: true
                )
            }
        }
        
        val doFirstCount = sections
            .find { it.quadrant == EisenhowerQuadrant.DO_FIRST }
            ?.count ?: 0
        
        val totalActive = tasks.count { !it.isCompleted }
        
        _uiState.update { current ->
            current.copy(
                sections = sections,
                totalActiveCount = totalActive,
                doFirstCount = doFirstCount,
                isLoading = false,
                error = null
            )
        }
    }
    
    /**
     * Map TaskEntity to TaskUiModel.
     */
    private suspend fun mapToUiModel(
        entity: TaskEntity,
        now: kotlinx.datetime.Instant
    ): TaskUiModel {
        val isOverdue = entity.dueDate?.let { dueDate ->
            dueDate < now && !entity.isCompleted
        } ?: false
        
        val dueText = entity.dueDate?.let { formatDueDate(it, now) }
        
        // Get goal name if linked
        val goalName = entity.goalId?.let { goalId ->
            try {
                goalRepository.getGoalById(goalId)?.title
            } catch (e: Exception) {
                null
            }
        }
        
        return TaskUiModel(
            id = entity.id,
            title = entity.title,
            quadrant = entity.quadrant,
            isCompleted = entity.isCompleted,
            isOverdue = isOverdue,
            dueText = dueText,
            goalName = goalName,
            aiExplanation = entity.aiExplanation,
            hasNotes = !entity.notes.isNullOrBlank(),
            hasReminder = false, // TODO: Implement reminders
            isRecurring = entity.isRecurring
        )
    }
    
    /**
     * Format due date for display.
     * Per 1.1.1 due date formatting spec.
     */
    private fun formatDueDate(dueDate: kotlinx.datetime.Instant, now: kotlinx.datetime.Instant): String {
        val timeZone = TimeZone.currentSystemDefault()
        val dueDateLocal = dueDate.toLocalDateTime(timeZone).date
        val nowLocal = now.toLocalDateTime(timeZone).date
        
        val daysDiff = dueDateLocal.toEpochDays() - nowLocal.toEpochDays()
        
        return when {
            daysDiff < -1 -> "Overdue (${-daysDiff} days)"
            daysDiff == -1 -> "Overdue (yesterday)"
            daysDiff == 0 -> {
                // Show time if due today
                val time = dueDate.toLocalDateTime(timeZone).time
                if (time.hour > 0 || time.minute > 0) {
                    "Today ${formatTime(time.hour, time.minute)}"
                } else {
                    "Today"
                }
            }
            daysDiff == 1 -> "Tomorrow"
            daysDiff <= 7 -> {
                // Show day of week
                val dayOfWeek = java.time.DayOfWeek.values()[dueDateLocal.dayOfWeek.ordinal]
                dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            }
            else -> {
                // Show date
                val formatter = DateTimeFormatter.ofPattern("MMM d")
                val localDate = java.time.LocalDate.of(dueDateLocal.year, dueDateLocal.monthNumber, dueDateLocal.dayOfMonth)
                localDate.format(formatter)
            }
        }
    }
    
    private fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour < 12) "am" else "pm"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return if (minute > 0) {
            "$displayHour:${minute.toString().padStart(2, '0')}$period"
        } else {
            "$displayHour$period"
        }
    }
    
    // ========================================================================
    // Event Handling
    // ========================================================================
    
    fun onEvent(event: TaskListEvent) {
        when (event) {
            is TaskListEvent.OnTaskClick -> handleTaskClick(event.taskId)
            is TaskListEvent.OnTaskCheckboxClick -> handleTaskComplete(event.taskId)
            is TaskListEvent.OnTaskQuadrantChange -> handleQuadrantChange(event.taskId, event.newQuadrant)
            is TaskListEvent.OnTaskSwipeComplete -> handleTaskComplete(event.taskId)
            is TaskListEvent.OnTaskSwipeDelete -> handleTaskDelete(event.taskId)
            is TaskListEvent.OnSectionToggle -> handleSectionToggle(event.quadrant)
            is TaskListEvent.OnFilterSelect -> handleFilterSelect(event.filter)
            is TaskListEvent.OnSearchQueryChange -> handleSearchQuery(event.query)
            is TaskListEvent.OnViewModeChange -> handleViewModeChange(event.mode)
            TaskListEvent.OnToggleShowCompleted -> handleToggleShowCompleted()
            TaskListEvent.OnSearchToggle -> handleSearchToggle()
            TaskListEvent.OnFabClick -> handleFabClick()
            TaskListEvent.OnRefresh -> handleRefresh()
            TaskListEvent.OnUndoComplete -> handleUndoComplete()
            TaskListEvent.OnUndoDelete -> handleUndoDelete()
            TaskListEvent.OnClearFilters -> handleClearFilters()
        }
    }
    
    private fun handleTaskClick(taskId: Long) {
        viewModelScope.launch {
            _effect.send(TaskListEffect.NavigateToTaskDetail(taskId))
        }
    }
    
    private fun handleTaskComplete(taskId: Long) {
        viewModelScope.launch {
            try {
                // Store for undo
                lastCompletedTask = taskRepository.getTaskById(taskId)
                
                taskRepository.completeTask(taskId)
                
                _effect.send(
                    TaskListEffect.ShowSnackbar(
                        message = "Task completed",
                        actionLabel = "Undo"
                    )
                )
                
                // Check if all tasks are now complete
                if (_uiState.value.isAllCompleted) {
                    _effect.send(TaskListEffect.ShowCompleteConfetti)
                }
            } catch (e: Exception) {
                _effect.send(TaskListEffect.ShowSnackbar("Failed to complete task"))
            }
        }
    }
    
    private fun handleQuadrantChange(taskId: Long, newQuadrant: EisenhowerQuadrant) {
        viewModelScope.launch {
            try {
                taskRepository.updateQuadrant(taskId, newQuadrant)
                _effect.send(
                    TaskListEffect.ShowSnackbar("Moved to ${newQuadrant.displayName}")
                )
            } catch (e: Exception) {
                _effect.send(TaskListEffect.ShowSnackbar("Failed to update priority"))
            }
        }
    }
    
    private fun handleTaskDelete(taskId: Long) {
        viewModelScope.launch {
            try {
                // Store for undo
                lastDeletedTask = taskRepository.getTaskById(taskId)
                
                taskRepository.deleteTaskById(taskId)
                
                _effect.send(
                    TaskListEffect.ShowSnackbar(
                        message = "Task deleted",
                        actionLabel = "Undo"
                    )
                )
            } catch (e: Exception) {
                _effect.send(TaskListEffect.ShowSnackbar("Failed to delete task"))
            }
        }
    }
    
    private fun handleSectionToggle(quadrant: EisenhowerQuadrant) {
        // DO_FIRST section cannot be collapsed
        if (quadrant == EisenhowerQuadrant.DO_FIRST) return
        
        sectionCollapseState.update { current ->
            current.toMutableMap().apply {
                this[quadrant] = !(this[quadrant] ?: true)
            }
        }
    }
    
    private fun handleFilterSelect(filter: TaskFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }
    
    private fun handleSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    private fun handleViewModeChange(mode: TaskViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }
    
    private fun handleToggleShowCompleted() {
        _uiState.update { it.copy(showCompletedTasks = !it.showCompletedTasks) }
    }
    
    private fun handleSearchToggle() {
        _uiState.update { it.copy(isSearchActive = !it.isSearchActive, searchQuery = "") }
    }
    
    private fun handleFabClick() {
        viewModelScope.launch {
            _effect.send(TaskListEffect.NavigateToQuickCapture)
        }
    }
    
    private fun handleRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Recalculate urgency scores
                taskRepository.recalculateAllUrgencyScores()
            } catch (e: Exception) {
                _effect.send(TaskListEffect.ShowSnackbar("Failed to refresh"))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    private fun handleUndoComplete() {
        viewModelScope.launch {
            lastCompletedTask?.let { task ->
                taskRepository.uncompleteTask(task.id)
                lastCompletedTask = null
            }
        }
    }
    
    private fun handleUndoDelete() {
        viewModelScope.launch {
            lastDeletedTask?.let { task ->
                taskRepository.insertTask(task)
                lastDeletedTask = null
            }
        }
    }
    
    private fun handleClearFilters() {
        _uiState.update { 
            it.copy(
                selectedFilter = TaskFilter.All,
                searchQuery = "",
                isSearchActive = false
            )
        }
    }
}

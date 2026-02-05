package com.prio.app.feature.goals.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prio.core.common.model.GoalStatus
import com.prio.core.data.local.entity.GoalEntity
import com.prio.core.data.local.entity.MilestoneEntity
import com.prio.core.data.local.entity.TaskEntity
import com.prio.core.data.repository.GoalRepository
import com.prio.core.data.repository.TaskRepository
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for GoalDetailScreen.
 *
 * Implements task 3.2.2: Goal Detail screen per 1.1.4 Goals Screens Spec.
 * Per GL-002 (Progress Visualization) and GL-006 (Goal Analytics).
 *
 * Features:
 * - Progress hero (circular, animated 800ms)
 * - 3-tab layout: Tasks / Milestones / Analytics
 * - Linked tasks grouped by quadrant, "+ Add" button
 * - Milestone timeline with completion checkoff
 * - AI insight card (context-aware: encouragement / recovery / nudge)
 * - Confetti on 100% completion
 */
@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val goalId: Long = savedStateHandle.get<Long>("goalId") ?: 0L

    private val _uiState = MutableStateFlow(GoalDetailUiState())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()

    private val _effect = Channel<GoalDetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        if (goalId > 0) {
            observeGoalData()
        }
    }

    /**
     * Observe goal, milestones, and linked tasks reactively.
     */
    private fun observeGoalData() {
        combine(
            goalRepository.getGoalByIdFlow(goalId),
            goalRepository.getMilestonesByGoalId(goalId),
            taskRepository.getTasksByGoalId(goalId)
        ) { goal, milestones, tasks ->
            if (goal != null) {
                updateUiState(goal, milestones, tasks)
            }
        }.launchIn(viewModelScope)
    }

    /**
     * Transform data to UI state.
     */
    private fun updateUiState(
        goal: GoalEntity,
        milestones: List<MilestoneEntity>,
        tasks: List<TaskEntity>
    ) {
        val status = goalRepository.calculateGoalStatus(goal)
        val now = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Format target date
        val targetDateText = goal.targetDate?.let { date ->
            try {
                val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
                javaDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            } catch (e: Exception) {
                date.toString()
            }
        }

        // Calculate time remaining
        val timeRemaining = goal.targetDate?.let { targetDate ->
            val daysLeft = now.daysUntil(targetDate)
            when {
                daysLeft < 0 -> "${-daysLeft} days overdue"
                daysLeft == 0 -> "Due today"
                daysLeft == 1 -> "1 day left"
                daysLeft < 7 -> "$daysLeft days left"
                daysLeft < 30 -> "${daysLeft / 7} weeks left"
                else -> "${daysLeft / 30} months left"
            }
        }

        // Map tasks
        val activeTasks = tasks.filter { !it.isCompleted }
        val completedTasks = tasks.filter { it.isCompleted }

        val linkedTaskModels = activeTasks.map { task ->
            val taskDueDate = task.dueDate?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
            LinkedTaskUiModel(
                id = task.id,
                title = task.title,
                quadrant = task.quadrant.displayName,
                quadrantEmoji = task.quadrant.emoji,
                isCompleted = false,
                dueText = taskDueDate?.let { formatDueDate(it) },
                isOverdue = taskDueDate?.let { it < now } ?: false
            )
        }

        val completedTaskModels = completedTasks.map { task ->
            LinkedTaskUiModel(
                id = task.id,
                title = task.title,
                quadrant = task.quadrant.displayName,
                quadrantEmoji = task.quadrant.emoji,
                isCompleted = true
            )
        }

        // Map milestones with state
        val milestoneModels = milestones.map { milestone ->
            val milestoneTargetDate = milestone.targetDate
            val state = when {
                milestone.isCompleted -> MilestoneState.COMPLETED
                milestoneTargetDate != null && milestoneTargetDate < now -> MilestoneState.OVERDUE
                milestone.position == milestones.indexOfFirst { !it.isCompleted } -> MilestoneState.IN_PROGRESS
                else -> MilestoneState.UPCOMING
            }

            MilestoneUiModel(
                id = milestone.id,
                title = milestone.title,
                targetDate = milestone.targetDate?.let { formatTargetDate(it) },
                isCompleted = milestone.isCompleted,
                completedAt = milestone.completedAt?.let { formatInstant(it) },
                state = state,
                position = milestone.position
            )
        }

        // Generate AI insight based on status
        val aiInsight = generateAiInsight(status, goal.progress, milestones)

        // Check for 100% completion (confetti trigger)
        val justCompleted = goal.progress >= 100 && !_uiState.value.isCompleted

        _uiState.update { current ->
            current.copy(
                isLoading = false,
                goalId = goal.id,
                title = goal.title,
                description = goal.description,
                category = goal.category,
                progress = goal.progress / 100f,
                status = status,
                targetDate = targetDateText,
                timeRemaining = timeRemaining,
                linkedTasks = linkedTaskModels,
                completedTasks = completedTaskModels,
                milestones = milestoneModels,
                milestonesCompleted = milestones.count { it.isCompleted },
                milestonesTotal = milestones.size,
                aiInsight = aiInsight,
                isCompleted = goal.isCompleted,
                showConfetti = justCompleted
            )
        }
    }

    /**
     * Generate context-aware AI insight.
     * Per 1.1.4: on track → encouragement, behind → recovery, near milestone → nudge.
     */
    private fun generateAiInsight(
        status: GoalStatus,
        progress: Int,
        milestones: List<MilestoneEntity>
    ): String {
        // Find next incomplete milestone
        val nextMilestone = milestones.firstOrNull { !it.isCompleted }

        return when {
            progress >= 90 -> "🎯 Almost there! You're at ${progress}% — just a few more tasks to go!"
            nextMilestone != null && status == GoalStatus.ON_TRACK -> {
                "💪 Great pace! Focus on \"${nextMilestone.title}\" to keep momentum."
            }
            status == GoalStatus.AT_RISK -> {
                "⚡ Time to refocus. Try completing 2-3 linked tasks this week to get back on track."
            }
            status == GoalStatus.BEHIND -> {
                "📋 Slightly behind schedule. Consider breaking remaining tasks into smaller steps."
            }
            status == GoalStatus.ON_TRACK -> {
                "✨ You're on track! Keep up the consistent progress."
            }
            else -> "📊 Track your progress by completing linked tasks."
        }
    }

    /**
     * Handle UI events.
     */
    fun onEvent(event: GoalDetailEvent) {
        when (event) {
            is GoalDetailEvent.OnTabSelect -> {
                _uiState.update { it.copy(selectedTab = event.tab) }
            }
            is GoalDetailEvent.OnMilestoneToggle -> {
                toggleMilestone(event.milestoneId)
            }
            is GoalDetailEvent.OnTaskClick -> {
                viewModelScope.launch {
                    _effect.send(GoalDetailEffect.NavigateToTask(event.taskId))
                }
            }
            GoalDetailEvent.OnToggleCompletedTasks -> {
                _uiState.update { it.copy(showCompletedTasks = !it.showCompletedTasks) }
            }
            GoalDetailEvent.OnAddTask -> {
                viewModelScope.launch {
                    _effect.send(GoalDetailEffect.OpenQuickCapture)
                }
            }
            GoalDetailEvent.OnAddMilestone -> {
                val canAdd = _uiState.value.milestonesTotal < GoalRepository.MAX_MILESTONES_PER_GOAL
                if (canAdd) {
                    _uiState.update { it.copy(showAddMilestoneDialog = true) }
                } else {
                    viewModelScope.launch {
                        _effect.send(GoalDetailEffect.ShowSnackbar(
                            "Maximum ${GoalRepository.MAX_MILESTONES_PER_GOAL} milestones per goal"
                        ))
                    }
                }
            }
            is GoalDetailEvent.OnConfirmAddMilestone -> {
                addMilestone(event.title)
            }
            GoalDetailEvent.OnDismissAddMilestoneDialog -> {
                _uiState.update { it.copy(showAddMilestoneDialog = false) }
            }
            is GoalDetailEvent.OnDeleteMilestone -> {
                deleteMilestone(event.milestoneId)
            }
            GoalDetailEvent.OnEditGoal -> {
                // Will navigate to edit screen
            }
            GoalDetailEvent.OnDeleteGoal -> {
                deleteGoal()
            }
            GoalDetailEvent.OnCompleteGoal -> {
                completeGoal()
            }
            GoalDetailEvent.OnNavigateBack -> {
                viewModelScope.launch {
                    _effect.send(GoalDetailEffect.NavigateBack)
                }
            }
            GoalDetailEvent.OnDismissConfetti -> {
                _uiState.update { it.copy(showConfetti = false) }
            }
        }
    }

    private fun toggleMilestone(milestoneId: Long) {
        viewModelScope.launch {
            val milestone = _uiState.value.milestones.find { it.id == milestoneId }
            if (milestone != null) {
                if (milestone.isCompleted) {
                    goalRepository.uncompleteMilestone(milestoneId)
                } else {
                    goalRepository.completeMilestone(milestoneId)
                }
                // Recalculate progress after milestone change
                goalRepository.recalculateProgress(goalId)
            }
        }
    }

    private fun deleteGoal() {
        viewModelScope.launch {
            goalRepository.deleteGoalById(goalId)
            _effect.send(GoalDetailEffect.NavigateBack)
        }
    }

    private fun completeGoal() {
        viewModelScope.launch {
            goalRepository.completeGoal(goalId)
            _effect.send(GoalDetailEffect.ShowConfetti)
        }
    }

    /**
     * Add a new milestone to this goal.
     * Per GL-004: 0-5 milestones, respects max limit.
     */
    private fun addMilestone(title: String) {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val result = goalRepository.addMilestone(
                goalId = goalId,
                title = trimmed,
                targetDate = null
            )
            if (result != null) {
                _uiState.update { it.copy(showAddMilestoneDialog = false) }
                _effect.send(GoalDetailEffect.ShowSnackbar("Milestone added"))
            } else {
                _effect.send(GoalDetailEffect.ShowSnackbar(
                    "Maximum ${GoalRepository.MAX_MILESTONES_PER_GOAL} milestones reached"
                ))
            }
        }
    }

    /**
     * Delete a milestone.
     */
    private fun deleteMilestone(milestoneId: Long) {
        viewModelScope.launch {
            goalRepository.deleteMilestoneById(milestoneId)
            _effect.send(GoalDetailEffect.ShowSnackbar("Milestone removed"))
        }
    }

    // Helper formatters
    private fun formatDueDate(date: LocalDate): String {
        return try {
            val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
            javaDate.format(DateTimeFormatter.ofPattern("MMM d"))
        } catch (e: Exception) {
            date.toString()
        }
    }

    private fun formatTargetDate(date: LocalDate): String {
        return try {
            val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
            javaDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        } catch (e: Exception) {
            date.toString()
        }
    }

    private fun formatInstant(instant: kotlinx.datetime.Instant): String {
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return try {
            val javaDate = java.time.LocalDate.of(
                dateTime.date.year,
                dateTime.date.monthNumber,
                dateTime.date.dayOfMonth
            )
            javaDate.format(DateTimeFormatter.ofPattern("MMM d"))
        } catch (e: Exception) {
            dateTime.date.toString()
        }
    }
}

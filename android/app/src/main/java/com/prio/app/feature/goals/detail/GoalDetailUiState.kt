package com.prio.app.feature.goals.detail

import com.prio.core.common.model.GoalCategory
import com.prio.core.common.model.GoalStatus

/**
 * UI state for the Goal Detail screen.
 *
 * Implements task 3.2.2: Goal Detail screen per 1.1.4 Goals Screens Spec.
 * Per GL-002 (Progress Visualization) and GL-006 (Goal Analytics).
 *
 * Features:
 * - Progress hero (120dp circular progress ring, animated)
 * - 3-tab layout: Tasks / Milestones / Analytics
 * - Linked tasks grouped by quadrant
 * - Milestone timeline
 * - AI insight card
 */
data class GoalDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val goalId: Long = 0,
    val title: String = "",
    val description: String? = null,
    val category: GoalCategory = GoalCategory.PERSONAL,
    val progress: Float = 0f,    // 0.0 to 1.0
    val status: GoalStatus = GoalStatus.ON_TRACK,
    val targetDate: String? = null,
    val timeRemaining: String? = null,
    val selectedTab: GoalDetailTab = GoalDetailTab.TASKS,
    val linkedTasks: List<LinkedTaskUiModel> = emptyList(),
    val completedTasks: List<LinkedTaskUiModel> = emptyList(),
    val milestones: List<MilestoneUiModel> = emptyList(),
    val milestonesCompleted: Int = 0,
    val milestonesTotal: Int = 0,
    val milestoneContribution: Float = 0f,  // 0.0-1.0 portion of progress from milestones
    val taskContribution: Float = 0f,       // 0.0-1.0 portion of progress from tasks
    val showCompletedTasks: Boolean = false,
    val aiInsight: String? = null,
    val isCompleted: Boolean = false,
    val showConfetti: Boolean = false,
    // Milestone creation dialog state (3.2.4)
    val showAddMilestoneDialog: Boolean = false,
    val canAddMilestone: Boolean = true, // false when max 5 reached
    // Analytics data
    val weeklyActivity: List<Int> = emptyList(), // tasks completed per day over 7 days
    val progressHistory: List<Float> = emptyList() // weekly progress snapshots
)

/**
 * Tab options for Goal Detail screen.
 * Per 1.1.4: 📋 Tasks | 🏁 Milestones | 📊 Analytics
 */
enum class GoalDetailTab(val label: String, val emoji: String) {
    TASKS("Tasks", "📋"),
    MILESTONES("Milestones", "🏁"),
    ANALYTICS("Analytics", "📊")
}

/**
 * Linked task model for display in goal detail.
 */
data class LinkedTaskUiModel(
    val id: Long,
    val title: String,
    val quadrant: String,
    val quadrantEmoji: String,
    val isCompleted: Boolean = false,
    val dueText: String? = null,
    val isOverdue: Boolean = false
)

/**
 * Milestone model for display in goal detail.
 * Per 1.1.4: Timeline with ✅ completed / ⏳ in-progress / ○ upcoming / ⚠️ overdue states.
 */
data class MilestoneUiModel(
    val id: Long,
    val title: String,
    val targetDate: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: String? = null,
    val state: MilestoneState = MilestoneState.UPCOMING,
    val position: Int = 0
)

/**
 * Milestone state for timeline visualization.
 */
enum class MilestoneState(val emoji: String, val label: String) {
    COMPLETED("✅", "Completed"),
    IN_PROGRESS("⏳", "In Progress"),
    UPCOMING("○", "Upcoming"),
    OVERDUE("⚠️", "Overdue")
}

/**
 * Events from GoalDetailScreen to ViewModel.
 */
sealed interface GoalDetailEvent {
    data class OnTabSelect(val tab: GoalDetailTab) : GoalDetailEvent
    data class OnMilestoneToggle(val milestoneId: Long) : GoalDetailEvent
    data class OnDeleteMilestone(val milestoneId: Long) : GoalDetailEvent
    data class OnTaskClick(val taskId: Long) : GoalDetailEvent
    data object OnToggleCompletedTasks : GoalDetailEvent
    data object OnAddTask : GoalDetailEvent
    data object OnAddMilestone : GoalDetailEvent
    data class OnConfirmAddMilestone(val title: String) : GoalDetailEvent
    data object OnDismissAddMilestoneDialog : GoalDetailEvent
    data object OnEditGoal : GoalDetailEvent
    data object OnDeleteGoal : GoalDetailEvent
    data object OnCompleteGoal : GoalDetailEvent
    data object OnNavigateBack : GoalDetailEvent
    data object OnDismissConfetti : GoalDetailEvent
}

/**
 * One-time effects from ViewModel to UI.
 */
sealed interface GoalDetailEffect {
    data object NavigateBack : GoalDetailEffect
    data class NavigateToTask(val taskId: Long) : GoalDetailEffect
    data object OpenQuickCapture : GoalDetailEffect
    data class ShowSnackbar(val message: String) : GoalDetailEffect
    data object ShowConfetti : GoalDetailEffect
}

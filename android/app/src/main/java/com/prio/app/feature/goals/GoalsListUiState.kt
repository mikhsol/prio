package com.prio.app.feature.goals

import com.prio.core.common.model.GoalCategory
import com.prio.core.common.model.GoalStatus

/**
 * UI state for the Goals List screen.
 *
 * Implements task 3.2.1: Goals List screen per 1.1.4 Goals Screens Spec.
 * Per GL-005: "Goals list accessible from main navigation"
 *
 * Features:
 * - Overview card with summary stats
 * - Category filter chips
 * - Goals grouped by status (at-risk first)
 * - Empty state with CTA
 */
data class GoalsListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val sections: List<GoalSection> = emptyList(),
    val overviewStats: GoalOverviewStats = GoalOverviewStats(),
    val selectedCategoryFilter: GoalCategory? = null,
    val activeGoalCount: Int = 0,
    val canCreateNewGoal: Boolean = true,
    val showArchivedGoals: Boolean = false,
    val archivedGoals: List<GoalUiModel> = emptyList(),
    val archivedGoalCount: Int = 0
) {
    val isEmpty: Boolean
        get() = sections.all { it.goals.isEmpty() }

    val isMaxGoalsReached: Boolean
        get() = !canCreateNewGoal
}

/**
 * A section in the goals list, grouped by status.
 * Per 1.1.4: ⚠️ At Risk → ⏳ Slightly Behind → ✅ On Track
 */
data class GoalSection(
    val status: GoalStatus,
    val goals: List<GoalUiModel>,
    val isExpanded: Boolean = true
) {
    val title: String
        get() = when (status) {
            GoalStatus.AT_RISK -> "⚠️ At Risk"
            GoalStatus.BEHIND -> "⏳ Slightly Behind"
            GoalStatus.ON_TRACK -> "✅ On Track"
            GoalStatus.COMPLETED -> "🎉 Completed"
        }

    val count: Int
        get() = goals.size
}

/**
 * Goal UI model for display in the list.
 * Mapped from domain GoalEntity with calculated fields.
 */
data class GoalUiModel(
    val id: Long,
    val title: String,
    val description: String?,
    val category: GoalCategory,
    val progress: Float,  // 0.0 to 1.0
    val status: GoalStatus,
    val targetDate: String?,
    val milestonesCompleted: Int = 0,
    val milestonesTotal: Int = 0,
    val linkedTasksCount: Int = 0,
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false
)

/**
 * Overview statistics displayed in the summary card.
 * Per 1.1.4: Active, On Track, At Risk counts + average progress ring.
 */
data class GoalOverviewStats(
    val activeCount: Int = 0,
    val onTrackCount: Int = 0,
    val atRiskCount: Int = 0,
    val behindCount: Int = 0,
    val averageProgress: Float = 0f,
    val completedThisMonth: Int = 0
)

/**
 * Events from GoalsListScreen to ViewModel.
 */
sealed interface GoalsListEvent {
    data class OnGoalClick(val goalId: Long) : GoalsListEvent
    data class OnCategoryFilterSelect(val category: GoalCategory?) : GoalsListEvent
    data class OnSectionToggle(val status: GoalStatus) : GoalsListEvent
    data class OnGoalArchive(val goalId: Long) : GoalsListEvent
    data class OnGoalUnarchive(val goalId: Long) : GoalsListEvent
    data class OnGoalComplete(val goalId: Long) : GoalsListEvent
    data object OnCreateGoalClick : GoalsListEvent
    data object OnRefresh : GoalsListEvent
    data object OnUndoArchive : GoalsListEvent
    data object OnToggleArchivedGoals : GoalsListEvent
}

/**
 * One-time effects from ViewModel to UI.
 */
sealed interface GoalsListEffect {
    data class NavigateToGoalDetail(val goalId: Long) : GoalsListEffect
    data object NavigateToCreateGoal : GoalsListEffect
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : GoalsListEffect
    data object ShowMaxGoalsWarning : GoalsListEffect
    data object ShowCompletionConfetti : GoalsListEffect
}

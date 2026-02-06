package com.prio.app.feature.insights

import com.prio.core.common.model.GoalStatus
import kotlinx.datetime.LocalDate

/**
 * UI State for the Insights/Analytics screen.
 *
 * Milestone 3.5: Basic Analytics (Simplified).
 * Combines stats (3.5.2), completion chart (3.5.3), and goal progress trend (3.5.4).
 */
data class InsightsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,

    // 3.5.2 — Simple stats
    val weeklyTasksCompleted: Int = 0,
    val weeklyTasksCreated: Int = 0,
    val completionRate: Float = 0f,
    val isCompletionOnTarget: Boolean = false,
    val todayTasksCompleted: Int = 0,
    val todayTasksCreated: Int = 0,
    val aiAccuracy: Float = 0f,
    val isAiAccuracyOnTarget: Boolean = false,

    // 3.5.3 — Task completion chart (7-day bar chart)
    val chartData: List<DayChartPoint> = emptyList(),

    // 3.5.4 — Goal progress trend (Jordan persona)
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val activeGoals: Int = 0,
    val onTrackGoals: Int = 0,
    val atRiskGoals: Int = 0,
    val completedGoalsThisMonth: Int = 0,
    val goalProgressItems: List<GoalProgressUiModel> = emptyList(),

    // Quadrant breakdown
    val quadrantBreakdown: QuadrantBreakdownUi = QuadrantBreakdownUi()
)

/**
 * Single day bar chart data point for the 7-day completion chart.
 */
data class DayChartPoint(
    val date: LocalDate,
    val dayLabel: String,        // "Mon", "Tue", ...
    val tasksCompleted: Int,
    val q1: Int = 0,
    val q2: Int = 0,
    val q3: Int = 0,
    val q4: Int = 0,
    val isToday: Boolean = false
)

/**
 * Goal progress item for the goal trend section.
 */
data class GoalProgressUiModel(
    val id: Long,
    val title: String,
    val progress: Float,          // 0f..1f
    val status: GoalStatus,
    val targetDateText: String?,
    val weeklyDelta: Int          // Change in progress from last week (percentage points)
)

/**
 * Quadrant breakdown display model.
 */
data class QuadrantBreakdownUi(
    val q1: Int = 0,
    val q2: Int = 0,
    val q3: Int = 0,
    val q4: Int = 0
) {
    val total: Int get() = q1 + q2 + q3 + q4
}

/**
 * Events from UI → ViewModel.
 */
sealed interface InsightsEvent {
    data object OnRefresh : InsightsEvent
    data class OnGoalClick(val goalId: Long) : InsightsEvent
}

/**
 * One-time effects from ViewModel → UI.
 */
sealed interface InsightsEffect {
    data class NavigateToGoal(val goalId: Long) : InsightsEffect
    data class ShowSnackbar(val message: String) : InsightsEffect
}

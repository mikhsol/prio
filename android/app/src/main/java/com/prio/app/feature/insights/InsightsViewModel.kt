package com.prio.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prio.core.common.model.GoalStatus
import com.prio.core.data.repository.AnalyticsRepository
import com.prio.core.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for the Insights (Analytics) screen.
 *
 * Milestone 3.5: Basic Analytics (Simplified).
 * - 3.5.1: Analytics data collection (wired in repositories)
 * - 3.5.2: Simple stats (weekly tasks completed, goals progress, streaks)
 * - 3.5.3: Task completion chart (7-day bar chart)
 * - 3.5.4: Goal progress trend (Jordan persona: weekly delta + streak counter)
 */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val goalRepository: GoalRepository,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    private val _effect = Channel<InsightsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadInsights()
    }

    fun onEvent(event: InsightsEvent) {
        when (event) {
            InsightsEvent.OnRefresh -> loadInsights()
            is InsightsEvent.OnGoalClick -> {
                viewModelScope.launch {
                    _effect.send(InsightsEffect.NavigateToGoal(event.goalId))
                }
            }
        }
    }

    private fun loadInsights() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // 3.5.2 — Simple stats
                val summary = analyticsRepository.getProductivitySummary()
                val todayAnalytics = analyticsRepository.getAnalyticsForDate(
                    clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                )

                // 3.5.3 — Chart data
                val weeklyData = analyticsRepository.getWeeklyCompletionData()
                val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val chartData = weeklyData.map { point ->
                    DayChartPoint(
                        date = point.date,
                        dayLabel = formatDayLabel(point.date.dayOfWeek),
                        tasksCompleted = point.tasksCompleted,
                        q1 = point.q1Completed,
                        q2 = point.q2Completed,
                        q3 = point.q3Completed,
                        q4 = point.q4Completed,
                        isToday = point.date == today
                    )
                }

                // 3.5.4 — Goal progress trend
                val currentStreak = analyticsRepository.getCurrentStreak()
                val longestStreak = analyticsRepository.getLongestStreak()
                val goalStats = goalRepository.getDashboardStats()
                val activeGoals = goalRepository.getAllActiveGoalsSync()
                val goalProgressItems = activeGoals.map { goal ->
                    val status = goalRepository.calculateGoalStatus(goal)
                    val targetDateText = goal.targetDate?.let { date ->
                        try {
                            val javaDate = java.time.LocalDate.of(
                                date.year, date.monthNumber, date.dayOfMonth
                            )
                            javaDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))
                        } catch (e: Exception) {
                            date.toString()
                        }
                    }
                    GoalProgressUiModel(
                        id = goal.id,
                        title = goal.title,
                        progress = goal.progress / 100f,
                        status = status,
                        targetDateText = targetDateText,
                        weeklyDelta = 0 // Requires historical tracking; set to 0 for MVP
                    )
                }.sortedWith(
                    compareBy<GoalProgressUiModel> {
                        when (it.status) {
                            GoalStatus.AT_RISK -> 0
                            GoalStatus.BEHIND -> 1
                            GoalStatus.ON_TRACK -> 2
                            GoalStatus.COMPLETED -> 3
                        }
                    }.thenByDescending { it.progress }
                )

                // Quadrant breakdown (weekly)
                val quadrantBreakdown = QuadrantBreakdownUi(
                    q1 = weeklyData.sumOf { it.q1Completed },
                    q2 = weeklyData.sumOf { it.q2Completed },
                    q3 = weeklyData.sumOf { it.q3Completed },
                    q4 = weeklyData.sumOf { it.q4Completed }
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        weeklyTasksCompleted = summary.totalTasksCompleted,
                        weeklyTasksCreated = summary.totalTasksCreated,
                        completionRate = summary.completionRate,
                        isCompletionOnTarget = summary.isCompletionRateOnTarget,
                        todayTasksCompleted = todayAnalytics?.tasksCompleted ?: 0,
                        todayTasksCreated = todayAnalytics?.tasksCreated ?: 0,
                        aiAccuracy = summary.aiAccuracy,
                        isAiAccuracyOnTarget = summary.isAiAccuracyOnTarget,
                        chartData = chartData,
                        currentStreak = currentStreak,
                        longestStreak = longestStreak,
                        activeGoals = goalStats.activeGoals,
                        onTrackGoals = goalStats.onTrackCount,
                        atRiskGoals = goalStats.atRiskCount,
                        completedGoalsThisMonth = goalStats.completedThisMonth,
                        goalProgressItems = goalProgressItems,
                        quadrantBreakdown = quadrantBreakdown
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to load insights: ${e.message}")
                }
            }
        }
    }

    private fun formatDayLabel(dayOfWeek: kotlinx.datetime.DayOfWeek): String {
        return when (dayOfWeek) {
            kotlinx.datetime.DayOfWeek.MONDAY -> "Mon"
            kotlinx.datetime.DayOfWeek.TUESDAY -> "Tue"
            kotlinx.datetime.DayOfWeek.WEDNESDAY -> "Wed"
            kotlinx.datetime.DayOfWeek.THURSDAY -> "Thu"
            kotlinx.datetime.DayOfWeek.FRIDAY -> "Fri"
            kotlinx.datetime.DayOfWeek.SATURDAY -> "Sat"
            kotlinx.datetime.DayOfWeek.SUNDAY -> "Sun"
            else -> ""
        }
    }
}

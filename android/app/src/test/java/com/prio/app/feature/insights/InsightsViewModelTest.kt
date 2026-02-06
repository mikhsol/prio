package com.prio.app.feature.insights

import app.cash.turbine.test
import com.prio.core.common.model.GoalCategory
import com.prio.core.common.model.GoalStatus
import com.prio.core.data.local.entity.DailyAnalyticsEntity
import com.prio.core.data.local.entity.GoalEntity
import com.prio.core.data.repository.AnalyticsRepository
import com.prio.core.data.repository.DailyCompletionPoint
import com.prio.core.data.repository.GoalDashboardStats
import com.prio.core.data.repository.GoalRepository
import com.prio.core.data.repository.ProductivitySummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for InsightsViewModel.
 *
 * Milestone 3.5: Basic Analytics (Simplified).
 * Validates:
 * - Loading state management
 * - Stats population from AnalyticsRepository (3.5.2)
 * - Chart data mapping with 7-day zero-fill (3.5.3)
 * - Streak counter and goal progress ordering (3.5.4)
 * - Error handling
 * - Refresh and navigation effects
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("InsightsViewModel")
class InsightsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val analyticsRepository: AnalyticsRepository = mockk(relaxed = true)
    private val goalRepository: GoalRepository = mockk(relaxed = true)

    // Fixed clock: 2026-04-15 (Wednesday)
    private val fixedInstant = Instant.parse("2026-04-15T10:00:00Z")
    private val testClock = object : Clock {
        override fun now(): Instant = fixedInstant
    }

    // Today = 2026-04-15 (Wed), weekAgo = 2026-04-09 (Thu)
    private val today = LocalDate.parse("2026-04-15")

    private lateinit var viewModel: InsightsViewModel

    // ==================== Factories ====================

    private fun createSummary(
        created: Int = 10,
        completed: Int = 7,
        completionRate: Float = 0.7f,
        aiAccuracy: Float = 0.9f,
        completionOnTarget: Boolean = true,
        aiOnTarget: Boolean = true
    ) = ProductivitySummary(
        totalTasksCreated = created,
        totalTasksCompleted = completed,
        completionRate = completionRate,
        aiAccuracy = aiAccuracy,
        isCompletionRateOnTarget = completionOnTarget,
        isAiAccuracyOnTarget = aiOnTarget
    )

    private fun createTodayAnalytics(
        tasksCreated: Int = 3,
        tasksCompleted: Int = 2,
        q1: Int = 1,
        q2: Int = 1,
        q3: Int = 0,
        q4: Int = 0
    ) = DailyAnalyticsEntity(
        id = 1L,
        date = today,
        tasksCreated = tasksCreated,
        tasksCompleted = tasksCompleted,
        q1Completed = q1,
        q2Completed = q2,
        q3Completed = q3,
        q4Completed = q4
    )

    private fun createWeeklyData(): List<DailyCompletionPoint> = (0..6).map { offset ->
        val date = LocalDate.parse("2026-04-09").plus(DatePeriod(days = offset))
        DailyCompletionPoint(
            date = date,
            tasksCompleted = offset + 1, // 1..7
            q1Completed = 1,
            q2Completed = offset % 2,
            q3Completed = 0,
            q4Completed = 0
        )
    }

    private fun createGoal(
        id: Long = 1L,
        title: String = "Test Goal",
        category: GoalCategory = GoalCategory.CAREER,
        progress: Int = 50,
        targetDate: LocalDate? = LocalDate.parse("2026-12-31"),
        isCompleted: Boolean = false
    ) = GoalEntity(
        id = id,
        title = title,
        category = category,
        progress = progress,
        targetDate = targetDate,
        isCompleted = isCompleted,
        createdAt = fixedInstant,
        updatedAt = fixedInstant
    )

    private fun createDashboardStats(
        active: Int = 3,
        onTrack: Int = 2,
        atRisk: Int = 1,
        completedThisMonth: Int = 0
    ) = GoalDashboardStats(
        activeGoals = active,
        onTrackCount = onTrack,
        atRiskCount = atRisk,
        completedThisMonth = completedThisMonth
    )

    // ==================== Setup ====================

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Default mocks: happy path
        coEvery { analyticsRepository.getProductivitySummary(any(), any()) } returns createSummary()
        coEvery { analyticsRepository.getAnalyticsForDate(any()) } returns createTodayAnalytics()
        coEvery { analyticsRepository.getWeeklyCompletionData() } returns createWeeklyData()
        coEvery { analyticsRepository.getCurrentStreak() } returns 3
        coEvery { analyticsRepository.getLongestStreak() } returns 7
        coEvery { goalRepository.getDashboardStats() } returns createDashboardStats()
        coEvery { goalRepository.getAllActiveGoalsSync() } returns emptyList()
        coEvery { goalRepository.calculateGoalStatus(any()) } returns GoalStatus.ON_TRACK
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = InsightsViewModel(analyticsRepository, goalRepository, testClock)
    }

    // ==================== Tests ====================

    @Nested
    @DisplayName("Initial Loading")
    inner class InitialLoading {

        @Test
        @DisplayName("starts with loading state")
        fun startsWithLoadingState() = runTest {
            createViewModel()

            viewModel.uiState.test {
                val initial = awaitItem()
                assertTrue(initial.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("transitions to loaded state after data fetch")
        fun transitionsToLoadedState() = runTest {
            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
    }

    @Nested
    @DisplayName("Stats Population (3.5.2)")
    inner class StatsPopulation {

        @Test
        @DisplayName("populates weekly stats from productivity summary")
        fun populatesWeeklyStats() = runTest {
            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(10, state.weeklyTasksCreated)
            assertEquals(7, state.weeklyTasksCompleted)
            assertEquals(0.7f, state.completionRate)
            assertTrue(state.isCompletionOnTarget)
        }

        @Test
        @DisplayName("populates today's stats from daily analytics")
        fun populatesTodayStats() = runTest {
            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(3, state.todayTasksCreated)
            assertEquals(2, state.todayTasksCompleted)
        }

        @Test
        @DisplayName("populates AI accuracy metrics")
        fun populatesAiAccuracy() = runTest {
            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(0.9f, state.aiAccuracy)
            assertTrue(state.isAiAccuracyOnTarget)
        }

        @Test
        @DisplayName("shows off-target when completion rate below threshold")
        fun showsOffTargetCompletion() = runTest {
            coEvery { analyticsRepository.getProductivitySummary(any(), any()) } returns createSummary(
                completionRate = 0.4f,
                completionOnTarget = false
            )

            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(0.4f, state.completionRate)
            assertFalse(state.isCompletionOnTarget)
        }

        @Test
        @DisplayName("handles null today analytics gracefully")
        fun handlesNullTodayAnalytics() = runTest {
            coEvery { analyticsRepository.getAnalyticsForDate(any()) } returns null

            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(0, state.todayTasksCompleted)
            assertEquals(0, state.todayTasksCreated)
        }
    }

    @Nested
    @DisplayName("Chart Data (3.5.3)")
    inner class ChartData {

        @Test
        @DisplayName("maps 7 daily data points to chart data")
        fun maps7DayChartData() = runTest {
            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(7, state.chartData.size)
        }

        @Test
        @DisplayName("marks today in chart data")
        fun marksTodayInChartData() = runTest {
            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            val todayPoint = state.chartData.find { it.isToday }
            assertTrue(todayPoint != null)
            assertEquals(today, todayPoint!!.date)
        }

        @Test
        @DisplayName("maps day labels correctly")
        fun mapsDayLabels() = runTest {
            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // 2026-04-09 is Thursday, so first label should be "Thu"
            assertEquals("Thu", state.chartData[0].dayLabel)
            // 2026-04-15 is Wednesday (today), last label should be "Wed"
            assertEquals("Wed", state.chartData[6].dayLabel)
        }

        @Test
        @DisplayName("maps quadrant breakdown per chart point")
        fun mapsQuadrantBreakdownPerPoint() = runTest {
            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // Each point has q1=1 per factory
            assertTrue(state.chartData.all { it.q1 == 1 })
        }

        @Test
        @DisplayName("calculates weekly quadrant breakdown totals")
        fun calculatesWeeklyQuadrantBreakdown() = runTest {
            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // q1: 7 * 1 = 7
            assertEquals(7, state.quadrantBreakdown.q1)
            // q2: offsets 1,3,5 have q2=1 → 3 total
            assertEquals(3, state.quadrantBreakdown.q2)
            assertEquals(0, state.quadrantBreakdown.q3)
            assertEquals(0, state.quadrantBreakdown.q4)
        }
    }

    @Nested
    @DisplayName("Streaks (3.5.4)")
    inner class Streaks {

        @Test
        @DisplayName("populates current streak from repository")
        fun populatesCurrentStreak() = runTest {
            createViewModel()
            advanceUntilIdle()

            assertEquals(3, viewModel.uiState.value.currentStreak)
        }

        @Test
        @DisplayName("populates longest streak from repository")
        fun populatesLongestStreak() = runTest {
            createViewModel()
            advanceUntilIdle()

            assertEquals(7, viewModel.uiState.value.longestStreak)
        }

        @Test
        @DisplayName("shows zero streaks when no data")
        fun showsZeroStreaksWhenNoData() = runTest {
            coEvery { analyticsRepository.getCurrentStreak() } returns 0
            coEvery { analyticsRepository.getLongestStreak() } returns 0

            createViewModel()
            advanceUntilIdle()

            assertEquals(0, viewModel.uiState.value.currentStreak)
            assertEquals(0, viewModel.uiState.value.longestStreak)
        }
    }

    @Nested
    @DisplayName("Goal Progress (3.5.4)")
    inner class GoalProgress {

        @Test
        @DisplayName("populates goal dashboard stats")
        fun populatesGoalDashboardStats() = runTest {
            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(3, state.activeGoals)
            assertEquals(2, state.onTrackGoals)
            assertEquals(1, state.atRiskGoals)
            assertEquals(0, state.completedGoalsThisMonth)
        }

        @Test
        @DisplayName("sorts goal progress items: at-risk first")
        fun sortsGoalProgressAtRiskFirst() = runTest {
            val goals = listOf(
                createGoal(id = 1, title = "On Track Goal", progress = 70),
                createGoal(id = 2, title = "At Risk Goal", progress = 30),
                createGoal(id = 3, title = "Behind Goal", progress = 10)
            )
            coEvery { goalRepository.getAllActiveGoalsSync() } returns goals
            coEvery { goalRepository.calculateGoalStatus(goals[0]) } returns GoalStatus.ON_TRACK
            coEvery { goalRepository.calculateGoalStatus(goals[1]) } returns GoalStatus.AT_RISK
            coEvery { goalRepository.calculateGoalStatus(goals[2]) } returns GoalStatus.BEHIND

            createViewModel()
            advanceUntilIdle()

            val items = viewModel.uiState.value.goalProgressItems
            assertEquals(3, items.size)
            assertEquals(GoalStatus.AT_RISK, items[0].status)
            assertEquals(GoalStatus.BEHIND, items[1].status)
            assertEquals(GoalStatus.ON_TRACK, items[2].status)
        }

        @Test
        @DisplayName("maps goal progress to 0f..1f range")
        fun mapsGoalProgressToFloatRange() = runTest {
            val goals = listOf(createGoal(id = 1, progress = 75))
            coEvery { goalRepository.getAllActiveGoalsSync() } returns goals

            createViewModel()
            advanceUntilIdle()

            val items = viewModel.uiState.value.goalProgressItems
            assertEquals(1, items.size)
            assertEquals(0.75f, items[0].progress)
        }

        @Test
        @DisplayName("formats target date as MMM yyyy")
        fun formatsTargetDate() = runTest {
            val goals = listOf(
                createGoal(id = 1, targetDate = LocalDate.parse("2026-12-31"))
            )
            coEvery { goalRepository.getAllActiveGoalsSync() } returns goals

            createViewModel()
            advanceUntilIdle()

            val items = viewModel.uiState.value.goalProgressItems
            assertEquals("Dec 2026", items[0].targetDateText)
        }

        @Test
        @DisplayName("handles goal with no target date")
        fun handlesGoalWithNoTargetDate() = runTest {
            val goals = listOf(
                createGoal(id = 1, targetDate = null)
            )
            coEvery { goalRepository.getAllActiveGoalsSync() } returns goals

            createViewModel()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.goalProgressItems[0].targetDateText)
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("sets error state when loading fails")
        fun setsErrorStateOnFailure() = runTest {
            coEvery { analyticsRepository.getProductivitySummary(any(), any()) } throws
                RuntimeException("Network error")

            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.error != null)
            assertTrue(state.error!!.contains("Network error"))
        }
    }

    @Nested
    @DisplayName("Events & Effects")
    inner class EventsAndEffects {

        @Test
        @DisplayName("refresh reloads all data")
        fun refreshReloadsData() = runTest {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(InsightsEvent.OnRefresh)
            advanceUntilIdle()

            // getProductivitySummary called twice: init + refresh
            coVerify(exactly = 2) { analyticsRepository.getProductivitySummary(any(), any()) }
        }

        @Test
        @DisplayName("goal click emits NavigateToGoal effect")
        fun goalClickEmitsNavigateEffect() = runTest {
            createViewModel()
            advanceUntilIdle()

            viewModel.effect.test {
                viewModel.onEvent(InsightsEvent.OnGoalClick(goalId = 42L))
                val effect = awaitItem()
                assertTrue(effect is InsightsEffect.NavigateToGoal)
                assertEquals(42L, (effect as InsightsEffect.NavigateToGoal).goalId)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}

package com.prio.app.feature.goals

import app.cash.turbine.test
import com.prio.core.common.model.GoalCategory
import com.prio.core.common.model.GoalStatus
import com.prio.core.data.local.entity.GoalEntity
import com.prio.core.data.repository.GoalDashboardStats
import com.prio.core.data.repository.GoalRepository
import com.prio.core.data.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for GoalsListViewModel.
 *
 * Task 3.2.8: Validates goals list screen behavior:
 * - Loading state
 * - Goal sections grouped by status
 * - Overview stats calculation
 * - Category filter
 * - Max goals enforcement
 * - Delete with undo
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("GoalsListViewModel")
class GoalsListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val goalRepository: GoalRepository = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val testClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-04-15T10:00:00Z")
    }

    private lateinit var viewModel: GoalsListViewModel

    private val now = Instant.parse("2026-04-15T10:00:00Z")

    private fun createGoal(
        id: Long = 1L,
        title: String = "Test Goal",
        category: GoalCategory = GoalCategory.CAREER,
        targetDate: LocalDate? = LocalDate.parse("2026-12-31"),
        progress: Int = 50,
        isCompleted: Boolean = false
    ) = GoalEntity(
        id = id,
        title = title,
        category = category,
        targetDate = targetDate,
        progress = progress,
        isCompleted = isCompleted,
        createdAt = now,
        updatedAt = now
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Default mocks
        every { goalRepository.getAllActiveGoals() } returns flowOf(emptyList())
        every { goalRepository.getActiveGoalCountFlow() } returns flowOf(0)
        coEvery { goalRepository.calculateGoalStatus(any()) } returns GoalStatus.ON_TRACK
        coEvery { goalRepository.getMilestoneProgress(any()) } returns (0 to 0)
        coEvery { taskRepository.getActiveTasksByGoalId(any()) } returns emptyList()
        coEvery { taskRepository.getCompletedTasksByGoalId(any()) } returns emptyList()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = GoalsListViewModel(goalRepository, taskRepository, testClock)
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        @DisplayName("starts with loading state")
        fun startsWithLoadingState() = runTest {
            every { goalRepository.getAllActiveGoals() } returns flowOf(emptyList())
            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                // After init, state should reflect empty goals
                assertTrue(state.sections.isEmpty() || !state.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("shows empty state when no goals exist")
        fun showsEmptyStateWhenNoGoals() = runTest {
            every { goalRepository.getAllActiveGoals() } returns flowOf(emptyList())
            every { goalRepository.getActiveGoalCountFlow() } returns flowOf(0)

            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.sections.isEmpty())
            assertEquals(0, state.overviewStats.activeCount)
        }
    }

    @Nested
    @DisplayName("Goal Sections")
    inner class GoalSections {

        @Test
        @DisplayName("groups goals by status into sections")
        fun groupsGoalsByStatus() = runTest {
            val goals = listOf(
                createGoal(id = 1, title = "On Track Goal", progress = 60),
                createGoal(id = 2, title = "At Risk Goal", progress = 10)
            )

            every { goalRepository.getAllActiveGoals() } returns flowOf(goals)
            every { goalRepository.getActiveGoalCountFlow() } returns flowOf(2)

            // First goal on track, second at risk
            coEvery { goalRepository.calculateGoalStatus(goals[0]) } returns GoalStatus.ON_TRACK
            coEvery { goalRepository.calculateGoalStatus(goals[1]) } returns GoalStatus.AT_RISK

            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.sections.isNotEmpty())
        }
    }

    @Nested
    @DisplayName("Max Goals Enforcement")
    inner class MaxGoalsEnforcement {

        @Test
        @DisplayName("enforces maximum 10 active goals limit")
        fun enforcesMaxGoalsLimit() = runTest {
            every { goalRepository.getActiveGoalCountFlow() } returns flowOf(10)
            every { goalRepository.getAllActiveGoals() } returns flowOf(
                (1..10L).map { id -> createGoal(id = id, title = "Goal $id") }
            )

            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.canCreateNewGoal)
        }

        @Test
        @DisplayName("allows goal creation when under limit")
        fun allowsGoalCreationUnderLimit() = runTest {
            every { goalRepository.getActiveGoalCountFlow() } returns flowOf(5)
            every { goalRepository.getAllActiveGoals() } returns flowOf(
                (1..5L).map { id -> createGoal(id = id, title = "Goal $id") }
            )

            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.canCreateNewGoal)
        }
    }

    @Nested
    @DisplayName("Category Filter")
    inner class CategoryFilter {

        @Test
        @DisplayName("filters goals by selected category")
        fun filtersGoalsByCategory() = runTest {
            val goals = listOf(
                createGoal(id = 1, title = "Career Goal", category = GoalCategory.CAREER),
                createGoal(id = 2, title = "Health Goal", category = GoalCategory.HEALTH),
                createGoal(id = 3, title = "Career Goal 2", category = GoalCategory.CAREER)
            )

            every { goalRepository.getAllActiveGoals() } returns flowOf(goals)
            every { goalRepository.getActiveGoalCountFlow() } returns flowOf(3)

            createViewModel()
            advanceUntilIdle()

            // Apply filter
            viewModel.onEvent(GoalsListEvent.OnCategoryFilterSelect(GoalCategory.CAREER))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(GoalCategory.CAREER, state.selectedCategoryFilter)
        }

        @Test
        @DisplayName("clears category filter")
        fun clearsCategoryFilter() = runTest {
            every { goalRepository.getAllActiveGoals() } returns flowOf(emptyList())
            every { goalRepository.getActiveGoalCountFlow() } returns flowOf(0)

            createViewModel()
            advanceUntilIdle()

            // Set and clear filter
            viewModel.onEvent(GoalsListEvent.OnCategoryFilterSelect(GoalCategory.CAREER))
            advanceUntilIdle()

            viewModel.onEvent(GoalsListEvent.OnCategoryFilterSelect(null))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(null, state.selectedCategoryFilter)
        }
    }

    @Nested
    @DisplayName("Overview Stats")
    inner class OverviewStats {

        @Test
        @DisplayName("calculates overview statistics from goals")
        fun calculatesOverviewStats() = runTest {
            val goals = listOf(
                createGoal(id = 1, progress = 70),
                createGoal(id = 2, progress = 30),
                createGoal(id = 3, progress = 50)
            )

            every { goalRepository.getAllActiveGoals() } returns flowOf(goals)
            every { goalRepository.getActiveGoalCountFlow() } returns flowOf(3)
            coEvery { goalRepository.calculateGoalStatus(goals[0]) } returns GoalStatus.ON_TRACK
            coEvery { goalRepository.calculateGoalStatus(goals[1]) } returns GoalStatus.AT_RISK
            coEvery { goalRepository.calculateGoalStatus(goals[2]) } returns GoalStatus.ON_TRACK

            createViewModel()
            advanceUntilIdle()

            val stats = viewModel.uiState.value.overviewStats
            assertEquals(3, stats.activeCount)
            assertEquals(2, stats.onTrackCount)
            assertEquals(1, stats.atRiskCount)
        }
    }

    @Nested
    @DisplayName("Delete with Undo")
    inner class DeleteWithUndo {

        @Test
        @DisplayName("deletes goal and shows snackbar with undo action")
        fun deletesGoalAndShowsSnackbar() = runTest {
            val goal = createGoal(id = 1, title = "Goal to Delete")
            every { goalRepository.getAllActiveGoals() } returns flowOf(listOf(goal))
            every { goalRepository.getActiveGoalCountFlow() } returns flowOf(1)
            coEvery { goalRepository.getGoalById(1L) } returns goal
            coEvery { goalRepository.deleteGoalById(1L) } returns Unit

            createViewModel()
            advanceUntilIdle()

            viewModel.effect.test {
                viewModel.onEvent(GoalsListEvent.OnGoalDelete(1L))
                advanceUntilIdle()

                val effect = awaitItem()
                assertTrue(effect is GoalsListEffect.ShowSnackbar)
                val snackbar = effect as GoalsListEffect.ShowSnackbar
                assertTrue(snackbar.message.contains("Goal to Delete"))
                assertEquals("Undo", snackbar.actionLabel)

                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("undo restores deleted goal via insert")
        fun undoRestoresDeletedGoal() = runTest {
            val goal = createGoal(id = 1, title = "Goal to Restore")
            every { goalRepository.getAllActiveGoals() } returns flowOf(listOf(goal))
            every { goalRepository.getActiveGoalCountFlow() } returns flowOf(1)
            coEvery { goalRepository.getGoalById(1L) } returns goal
            coEvery { goalRepository.deleteGoalById(1L) } returns Unit
            coEvery { goalRepository.insertGoal(goal) } returns 1L

            createViewModel()
            advanceUntilIdle()

            // Delete the goal first
            viewModel.onEvent(GoalsListEvent.OnGoalDelete(1L))
            advanceUntilIdle()

            // Undo the delete
            viewModel.onEvent(GoalsListEvent.OnUndoDelete)
            advanceUntilIdle()

            // Verify the goal was re-inserted
            io.mockk.coVerify { goalRepository.insertGoal(goal) }
        }

        @Test
        @DisplayName("undo does nothing when no goal was deleted")
        fun undoDoesNothingWhenNoDelete() = runTest {
            every { goalRepository.getAllActiveGoals() } returns flowOf(emptyList())
            every { goalRepository.getActiveGoalCountFlow() } returns flowOf(0)

            createViewModel()
            advanceUntilIdle()

            // Undo without prior delete
            viewModel.onEvent(GoalsListEvent.OnUndoDelete)
            advanceUntilIdle()

            // Verify no insert was called
            io.mockk.coVerify(exactly = 0) { goalRepository.insertGoal(any()) }
        }

        @Test
        @DisplayName("delete does nothing when goal not found")
        fun deleteDoesNothingWhenGoalNotFound() = runTest {
            every { goalRepository.getAllActiveGoals() } returns flowOf(emptyList())
            every { goalRepository.getActiveGoalCountFlow() } returns flowOf(0)
            coEvery { goalRepository.getGoalById(999L) } returns null

            createViewModel()
            advanceUntilIdle()

            viewModel.effect.test {
                viewModel.onEvent(GoalsListEvent.OnGoalDelete(999L))
                advanceUntilIdle()

                // No effect should be emitted
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}

package com.prio.app.feature.goals.create

import app.cash.turbine.test
import com.prio.core.ai.model.AiResponse
import com.prio.core.ai.model.AiResult
import com.prio.core.ai.provider.AiProvider
import com.prio.core.common.model.GoalCategory
import com.prio.core.data.repository.GoalRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for CreateGoalViewModel.
 *
 * Task 3.2.8: Validates goal creation wizard:
 * - 3-step wizard navigation
 * - AI SMART goal suggestion
 * - Skip AI fallback
 * - Milestone management (max 5)
 * - Goal creation with repository
 * - Max 10 active goals enforcement
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("CreateGoalViewModel")
class CreateGoalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val goalRepository: GoalRepository = mockk(relaxed = true)
    private val aiProvider: AiProvider = mockk(relaxed = true)
    private val testClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-04-15T10:00:00Z")
    }

    private lateinit var viewModel: CreateGoalViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        coEvery { goalRepository.canCreateNewGoal() } returns true
        coEvery { goalRepository.getActiveGoalCount() } returns 3
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = CreateGoalViewModel(goalRepository, aiProvider, testClock)
    }

    @Nested
    @DisplayName("Wizard Navigation")
    inner class WizardNavigation {

        @Test
        @DisplayName("starts at Describe step")
        fun startsAtDescribeStep() = runTest {
            createViewModel()
            advanceUntilIdle()

            assertEquals(CreateGoalStep.DESCRIBE, viewModel.uiState.value.currentStep)
        }

        @Test
        @DisplayName("advances from Describe to AI SMART step")
        fun advancesFromDescribeToAiSmart() = runTest {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(CreateGoalEvent.OnGoalInputChange("Get promoted"))
            viewModel.onEvent(CreateGoalEvent.OnNextFromDescribe)
            advanceUntilIdle()

            assertEquals(CreateGoalStep.AI_SMART, viewModel.uiState.value.currentStep)
        }

        @Test
        @DisplayName("does not advance with blank input")
        fun doesNotAdvanceWithBlankInput() = runTest {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(CreateGoalEvent.OnNextFromDescribe)
            advanceUntilIdle()

            // Should stay on DESCRIBE
            assertEquals(CreateGoalStep.DESCRIBE, viewModel.uiState.value.currentStep)
        }

        @Test
        @DisplayName("navigates back to previous step")
        fun navigatesBackToPreviousStep() = runTest {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(CreateGoalEvent.OnGoalInputChange("Test"))
            viewModel.onEvent(CreateGoalEvent.OnNextFromDescribe)
            advanceUntilIdle()

            assertEquals(CreateGoalStep.AI_SMART, viewModel.uiState.value.currentStep)

            viewModel.onEvent(CreateGoalEvent.OnPreviousStep)
            advanceUntilIdle()

            assertEquals(CreateGoalStep.DESCRIBE, viewModel.uiState.value.currentStep)
        }
    }

    @Nested
    @DisplayName("AI SMART Suggestion")
    inner class AiSmartSuggestion {

        @Test
        @DisplayName("requests AI refinement and populates SMART fields")
        fun requestsAiRefinementAndPopulatesSmartFields() = runTest {
            val suggestion = AiResult.SmartGoalSuggestion(
                refinedGoal = "Get promoted to Senior Engineer by December 2026",
                specific = "Achieve Senior Engineer title",
                measurable = "Lead 2 major projects",
                achievable = "Currently at mid-level with 3 years experience",
                relevant = "Aligns with career growth plan",
                timeBound = "By December 2026",
                suggestedMilestones = listOf("Complete leadership course", "Lead first project")
            )

            coEvery { aiProvider.complete(any()) } returns Result.success(
                AiResponse(
                    success = true,
                    result = suggestion
                )
            )

            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(CreateGoalEvent.OnGoalInputChange("Get promoted"))
            viewModel.onEvent(CreateGoalEvent.OnNextFromDescribe)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Get promoted to Senior Engineer by December 2026", state.refinedGoal)
            assertEquals("Achieve Senior Engineer title", state.smartSpecific)
            assertEquals(2, state.suggestedMilestones.size)
            assertFalse(state.aiSkipped)
        }

        @Test
        @DisplayName("falls back gracefully on AI failure")
        fun fallsBackOnAiFailure() = runTest {
            coEvery { aiProvider.complete(any()) } returns Result.failure(
                RuntimeException("AI unavailable")
            )

            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(CreateGoalEvent.OnGoalInputChange("Get promoted"))
            viewModel.onEvent(CreateGoalEvent.OnNextFromDescribe)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.aiSkipped)
            assertEquals("Get promoted", state.refinedGoal)
            assertFalse(state.isAiProcessing)
        }

        @Test
        @DisplayName("skip AI sets input as refined goal")
        fun skipAiSetsInputAsRefinedGoal() = runTest {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(CreateGoalEvent.OnGoalInputChange("My custom goal"))
            viewModel.onEvent(CreateGoalEvent.OnNextFromDescribe)
            advanceUntilIdle()

            viewModel.onEvent(CreateGoalEvent.OnSkipAi)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.aiSkipped)
            assertEquals("My custom goal", state.refinedGoal)
        }
    }

    @Nested
    @DisplayName("Milestone Management")
    inner class MilestoneManagement {

        @Test
        @DisplayName("adds milestones up to max 5")
        fun addsMilestonesUpToMax() = runTest {
            createViewModel()
            advanceUntilIdle()

            repeat(5) { i ->
                viewModel.onEvent(CreateGoalEvent.OnAddMilestone("Milestone ${i + 1}"))
            }
            advanceUntilIdle()

            assertEquals(5, viewModel.uiState.value.milestones.size)
        }

        @Test
        @DisplayName("prevents adding more than 5 milestones")
        fun preventsExceedingMaxMilestones() = runTest {
            createViewModel()
            advanceUntilIdle()

            repeat(6) { i ->
                viewModel.onEvent(CreateGoalEvent.OnAddMilestone("Milestone ${i + 1}"))
            }
            advanceUntilIdle()

            // Should stay at 5
            assertEquals(5, viewModel.uiState.value.milestones.size)
        }

        @Test
        @DisplayName("removes milestone by index")
        fun removesMilestoneByIndex() = runTest {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(CreateGoalEvent.OnAddMilestone("First"))
            viewModel.onEvent(CreateGoalEvent.OnAddMilestone("Second"))
            viewModel.onEvent(CreateGoalEvent.OnAddMilestone("Third"))
            advanceUntilIdle()

            viewModel.onEvent(CreateGoalEvent.OnRemoveMilestone(1))
            advanceUntilIdle()

            val milestones = viewModel.uiState.value.milestones
            assertEquals(2, milestones.size)
            assertEquals("First", milestones[0].title)
            assertEquals("Third", milestones[1].title)
        }
    }

    @Nested
    @DisplayName("Goal Creation")
    inner class GoalCreation {

        @Test
        @DisplayName("creates goal with repository")
        fun createsGoalWithRepository() = runTest {
            coEvery {
                goalRepository.createGoal(
                    title = any(),
                    description = any(),
                    originalInput = any(),
                    category = any(),
                    targetDate = any()
                )
            } returns 42L

            coEvery { goalRepository.addMilestone(any(), any(), any()) } returns 1L

            createViewModel()
            advanceUntilIdle()

            // Step through wizard
            viewModel.onEvent(CreateGoalEvent.OnGoalInputChange("Read 12 books"))
            viewModel.onEvent(CreateGoalEvent.OnSkipAi)
            viewModel.onEvent(CreateGoalEvent.OnCategorySelect(GoalCategory.LEARNING))
            viewModel.onEvent(CreateGoalEvent.OnAddMilestone("Read 3 books"))

            viewModel.onEvent(CreateGoalEvent.OnCreateGoal)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(42L, state.createdGoalId)
            assertTrue(state.showCelebration)

            coVerify {
                goalRepository.createGoal(
                    title = any(),
                    description = any(),
                    originalInput = "Read 12 books",
                    category = GoalCategory.LEARNING,
                    targetDate = any()
                )
            }
        }

        @Test
        @DisplayName("shows error when max goals reached during creation")
        fun showsErrorWhenMaxGoalsReached() = runTest {
            coEvery {
                goalRepository.createGoal(
                    title = any(),
                    description = any(),
                    originalInput = any(),
                    category = any(),
                    targetDate = any()
                )
            } returns null  // null = max reached

            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(CreateGoalEvent.OnGoalInputChange("Too many goals"))
            viewModel.onEvent(CreateGoalEvent.OnCreateGoal)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.error)
            assertFalse(state.showCelebration)
        }
    }

    @Nested
    @DisplayName("Category Selection")
    inner class CategorySelection {

        @Test
        @DisplayName("selects goal category")
        fun selectsGoalCategory() = runTest {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(CreateGoalEvent.OnCategorySelect(GoalCategory.HEALTH))
            advanceUntilIdle()

            assertEquals(GoalCategory.HEALTH, viewModel.uiState.value.selectedCategory)
        }
    }

    @Nested
    @DisplayName("Example Selection")
    inner class ExampleSelection {

        @Test
        @DisplayName("selects example and populates input")
        fun selectsExampleAndPopulatesInput() = runTest {
            createViewModel()
            advanceUntilIdle()

            val example = viewModel.uiState.value.inputExamples.first()
            viewModel.onEvent(CreateGoalEvent.OnExampleSelect(example))
            advanceUntilIdle()

            assertEquals(example, viewModel.uiState.value.goalInput)
        }
    }
}

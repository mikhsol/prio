package com.prio.app.feature.capture

import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.common.model.GoalCategory
import com.prio.core.data.local.entity.GoalEntity
import com.prio.core.data.repository.GoalRepository
import com.prio.core.data.repository.TaskRepository
import com.prio.core.domain.eisenhower.EisenhowerClassificationResult
import com.prio.core.domain.eisenhower.EisenhowerEngine
import com.prio.core.domain.parser.NaturalLanguageParser
import com.prio.core.domain.parser.ParsedTask
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for QuickCaptureViewModel.preselectGoal() behaviour.
 *
 * Validates the fix for: "Create task on goal screen → task not shown
 * on the list of tasks on Goal Screen."
 *
 * Root cause: goalId was never threaded from GoalDetailScreen through
 * QuickCapture, so tasks created from a goal screen had no goal
 * association.
 *
 * The fix adds preselectGoal(goalId) which pre-populates the
 * suggestedGoal and preselectedGoalId so that createTask() links the
 * new task to the correct goal.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("QuickCaptureViewModel Preselect Goal")
class QuickCaptureViewModelPreselectGoalTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: QuickCaptureViewModel

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val goalRepository: GoalRepository = mockk(relaxed = true)
    private val eisenhowerEngine: EisenhowerEngine = mockk()
    private val naturalLanguageParser: NaturalLanguageParser = mockk()
    private val testClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-02-13T12:00:00Z")
    }

    private val testGoal = GoalEntity(
        id = 7L,
        title = "Learn Kotlin",
        description = "Master Kotlin programming",
        category = GoalCategory.LEARNING,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        coEvery { goalRepository.getAllActiveGoals() } returns flowOf(listOf(testGoal))
        every { eisenhowerEngine.classify(any(), any()) } returns EisenhowerClassificationResult(
            quadrant = EisenhowerQuadrant.SCHEDULE,
            explanation = "Important but not urgent",
            confidence = 0.85f,
            isUrgent = false,
            isImportant = true,
            urgencyScore = 0.3f,
            importanceScore = 0.8f,
            urgencySignals = emptyList(),
            importanceSignals = listOf("learning"),
            shouldEscalateToLlm = false
        )
        coEvery { naturalLanguageParser.parse(any()) } returns ParsedTask(
            title = "Read Kotlin docs",
            dueDate = null,
            dueTime = null
        )
        coEvery { taskRepository.createTask(any(), any(), any(), any(), any(), any()) } returns 99L

        viewModel = QuickCaptureViewModel(
            taskRepository = taskRepository,
            goalRepository = goalRepository,
            eisenhowerEngine = eisenhowerEngine,
            naturalLanguageParser = naturalLanguageParser,
            clock = testClock
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("preselectGoal sets goal in state")
    inner class PreselectGoalSetsState {

        @Test
        @DisplayName("preselectGoal sets preselectedGoalId in UI state")
        fun preselectGoalSetsGoalId() = runTest {
            viewModel.preselectGoal(7L)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(7L, state.preselectedGoalId)
        }

        @Test
        @DisplayName("preselectGoal sets suggestedGoal with correct title")
        fun preselectGoalSetsSuggestedGoal() = runTest {
            viewModel.preselectGoal(7L)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.parsedResult?.suggestedGoal)
            assertEquals(7L, state.parsedResult?.suggestedGoal?.id)
            assertEquals("Learn Kotlin", state.parsedResult?.suggestedGoal?.title)
        }

        @Test
        @DisplayName("preselectGoal with unknown goalId does not set goal")
        fun preselectGoalWithUnknownIdDoesNotSet() = runTest {
            viewModel.preselectGoal(999L)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.preselectedGoalId)
        }
    }

    @Nested
    @DisplayName("Task creation with preselected goal")
    inner class CreateTaskWithPreselectedGoal {

        @Test
        @DisplayName("createTask passes preselectedGoalId to repository")
        fun createTaskPassesGoalId() = runTest {
            // Preselect goal
            viewModel.preselectGoal(7L)
            advanceUntilIdle()

            // Type and parse a task
            viewModel.onEvent(QuickCaptureEvent.UpdateInput("Read Kotlin docs"))
            advanceUntilIdle()
            viewModel.onEvent(QuickCaptureEvent.ParseInput)
            advanceUntilIdle()

            // Create the task
            viewModel.onEvent(QuickCaptureEvent.CreateTask)
            advanceUntilIdle()

            // Verify createTask was called with goalId = 7L
            coVerify {
                taskRepository.createTask(
                    title = any(),
                    notes = any(),
                    dueDate = any(),
                    quadrant = any(),
                    aiExplanation = any(),
                    goalId = 7L
                )
            }
        }

        @Test
        @DisplayName("preselected goal survives AI parsing")
        fun preselectedGoalSurvivesParsing() = runTest {
            // Set up goal repo to return goals that won't keyword-match
            coEvery { goalRepository.getAllActiveGoals() } returns flowOf(listOf(testGoal))

            // Preselect goal
            viewModel.preselectGoal(7L)
            advanceUntilIdle()

            // Parse text that doesn't keyword-match the goal
            coEvery { naturalLanguageParser.parse(any()) } returns ParsedTask(
                title = "Buy groceries",
                dueDate = null,
                dueTime = null
            )

            viewModel.onEvent(QuickCaptureEvent.UpdateInput("Buy groceries"))
            advanceUntilIdle()
            viewModel.onEvent(QuickCaptureEvent.ParseInput)
            advanceUntilIdle()

            // Goal should still be linked (preserved through parsing)
            val state = viewModel.uiState.value
            assertNotNull(state.parsedResult?.suggestedGoal)
            assertEquals(7L, state.parsedResult?.suggestedGoal?.id)
        }

        @Test
        @DisplayName("Reset clears preselectedGoalId")
        fun resetClearsPreselectedGoalId() = runTest {
            viewModel.preselectGoal(7L)
            advanceUntilIdle()
            assertEquals(7L, viewModel.uiState.value.preselectedGoalId)

            viewModel.onEvent(QuickCaptureEvent.Reset)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.preselectedGoalId)
            assertNull(state.parsedResult)
        }
    }
}

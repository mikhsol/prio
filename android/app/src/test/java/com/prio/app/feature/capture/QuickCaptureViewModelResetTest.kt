package com.prio.app.feature.capture

import app.cash.turbine.test
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.data.repository.GoalRepository
import com.prio.core.data.repository.TaskRepository
import com.prio.core.domain.eisenhower.EisenhowerClassificationResult
import com.prio.core.domain.eisenhower.EisenhowerEngine
import com.prio.core.domain.parser.NaturalLanguageParser
import com.prio.core.domain.parser.ParsedTask
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for QuickCaptureViewModel state reset behaviour.
 *
 * Validates that:
 * - After task creation, form fields (inputText, parsedResult, showPreview) are cleared
 * - Reset event returns the state to its default values
 * - Re-opening QuickCapture after task creation shows a clean form
 *
 * Regression coverage for bug: "Create goal → Add first task → sees stale
 * data from last created task instead of clean placeholder."
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("QuickCaptureViewModel State Reset")
class QuickCaptureViewModelResetTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: QuickCaptureViewModel

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val goalRepository: GoalRepository = mockk(relaxed = true)
    private val eisenhowerEngine: EisenhowerEngine = mockk()
    private val naturalLanguageParser: NaturalLanguageParser = mockk()
    private val testClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-02-05T12:00:00Z")
    }

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        coEvery { goalRepository.getAllActiveGoals() } returns flowOf(emptyList())
        every { eisenhowerEngine.classify(any(), any()) } returns EisenhowerClassificationResult(
            quadrant = EisenhowerQuadrant.DO_FIRST,
            explanation = "Urgent and important",
            confidence = 0.9f,
            isUrgent = true,
            isImportant = true,
            urgencyScore = 0.9f,
            importanceScore = 0.9f,
            urgencySignals = listOf("deadline"),
            importanceSignals = listOf("critical"),
            shouldEscalateToLlm = false
        )
        coEvery { naturalLanguageParser.parse(any()) } returns ParsedTask(
            title = "Buy groceries",
            dueDate = null,
            dueTime = null
        )
        coEvery { taskRepository.createTask(any(), any(), any(), any(), any(), any()) } returns 42L

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
    @DisplayName("Task creation clears form fields")
    inner class CreateTaskClearsForm {

        @Test
        @DisplayName("After createTask, inputText is empty")
        fun inputTextClearedAfterCreate() = runTest {
            // Type and parse
            viewModel.onEvent(QuickCaptureEvent.UpdateInput("Buy groceries"))
            advanceUntilIdle()
            viewModel.onEvent(QuickCaptureEvent.ParseInput)
            advanceUntilIdle()

            // Verify parsedResult is set
            assertNotNull(viewModel.uiState.value.parsedResult)

            // Create the task
            viewModel.onEvent(QuickCaptureEvent.CreateTask)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isCreated, "isCreated should be true")
            assertEquals("", state.inputText, "inputText should be empty after task creation")
        }

        @Test
        @DisplayName("After createTask, parsedResult is null")
        fun parsedResultClearedAfterCreate() = runTest {
            viewModel.onEvent(QuickCaptureEvent.UpdateInput("Buy groceries"))
            advanceUntilIdle()
            viewModel.onEvent(QuickCaptureEvent.ParseInput)
            advanceUntilIdle()

            viewModel.onEvent(QuickCaptureEvent.CreateTask)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.parsedResult, "parsedResult should be null after task creation")
        }

        @Test
        @DisplayName("After createTask, showPreview is false")
        fun showPreviewClearedAfterCreate() = runTest {
            viewModel.onEvent(QuickCaptureEvent.UpdateInput("Buy groceries"))
            advanceUntilIdle()
            viewModel.onEvent(QuickCaptureEvent.ParseInput)
            advanceUntilIdle()

            viewModel.onEvent(QuickCaptureEvent.CreateTask)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.showPreview, "showPreview should be false after task creation")
        }
    }

    @Nested
    @DisplayName("Reset event restores default state")
    inner class ResetRestoresDefaults {

        @Test
        @DisplayName("Reset after task creation restores clean state")
        fun resetAfterCreateRestoresCleanState() = runTest {
            // Create a task first
            viewModel.onEvent(QuickCaptureEvent.UpdateInput("Buy groceries"))
            advanceUntilIdle()
            viewModel.onEvent(QuickCaptureEvent.ParseInput)
            advanceUntilIdle()
            viewModel.onEvent(QuickCaptureEvent.CreateTask)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isCreated)

            // Reset (simulates re-opening QuickCapture)
            viewModel.onEvent(QuickCaptureEvent.Reset)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("", state.inputText, "inputText should be empty after reset")
            assertNull(state.parsedResult, "parsedResult should be null after reset")
            assertFalse(state.showPreview, "showPreview should be false after reset")
            assertFalse(state.isCreating, "isCreating should be false after reset")
            assertFalse(state.isCreated, "isCreated should be false after reset")
            assertNull(state.error, "error should be null after reset")
            assertFalse(state.isVoiceInputActive, "isVoiceInputActive should be false after reset")
        }

        @Test
        @DisplayName("Consecutive create-reset cycles produce clean state each time")
        fun consecutiveCreateResetCyclesProduceCleanState() = runTest {
            // First cycle: create a task
            viewModel.onEvent(QuickCaptureEvent.UpdateInput("Task one"))
            advanceUntilIdle()
            viewModel.onEvent(QuickCaptureEvent.ParseInput)
            advanceUntilIdle()
            viewModel.onEvent(QuickCaptureEvent.CreateTask)
            advanceUntilIdle()
            viewModel.onEvent(QuickCaptureEvent.Reset)
            advanceUntilIdle()

            // State should be clean
            var state = viewModel.uiState.value
            assertEquals("", state.inputText)
            assertNull(state.parsedResult)
            assertFalse(state.isCreated)

            // Second cycle: create another task
            viewModel.onEvent(QuickCaptureEvent.UpdateInput("Task two"))
            advanceUntilIdle()
            viewModel.onEvent(QuickCaptureEvent.ParseInput)
            advanceUntilIdle()
            viewModel.onEvent(QuickCaptureEvent.CreateTask)
            advanceUntilIdle()

            // After second create, form fields should still be cleared
            state = viewModel.uiState.value
            assertTrue(state.isCreated)
            assertEquals("", state.inputText, "inputText should be empty after second create")
            assertNull(state.parsedResult, "parsedResult should be null after second create")

            // Reset again
            viewModel.onEvent(QuickCaptureEvent.Reset)
            advanceUntilIdle()

            state = viewModel.uiState.value
            assertEquals("", state.inputText)
            assertNull(state.parsedResult)
            assertFalse(state.isCreated)
        }
    }

    @Nested
    @DisplayName("Dismiss resets state")
    inner class DismissResetsState {

        @Test
        @DisplayName("Dismiss event resets all form fields")
        fun dismissResetsAllFields() = runTest {
            viewModel.onEvent(QuickCaptureEvent.UpdateInput("Some task"))
            advanceUntilIdle()
            viewModel.onEvent(QuickCaptureEvent.ParseInput)
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.parsedResult)

            viewModel.onEvent(QuickCaptureEvent.Dismiss)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("", state.inputText)
            assertNull(state.parsedResult)
            assertFalse(state.showPreview)
            assertFalse(state.isCreated)
        }
    }
}

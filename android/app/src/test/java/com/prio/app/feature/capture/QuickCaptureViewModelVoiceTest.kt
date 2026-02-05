package com.prio.app.feature.capture

import app.cash.turbine.test
import com.prio.app.feature.capture.voice.VoiceErrorType
import com.prio.app.feature.capture.voice.VoiceInputState
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
import io.mockk.verify
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
 * Unit tests for QuickCaptureViewModel voice input integration.
 * 
 * Validates 3.1.5.B.3 requirements:
 * - Android SpeechRecognizer integration via effects
 * - On-device processing preference
 * - Permission handling flow
 * - Voice state propagation to UI
 * - Error recovery (retry / type instead)
 * - Voice result → text input → AI parse pipeline
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("QuickCaptureViewModel Voice Input")
class QuickCaptureViewModelVoiceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: QuickCaptureViewModel

    // Mocks
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

        // Default mock behaviors
        coEvery { goalRepository.getAllActiveGoals() } returns flowOf(emptyList())
        every { eisenhowerEngine.classify(any(), any()) } returns EisenhowerClassificationResult(
            quadrant = EisenhowerQuadrant.SCHEDULE,
            explanation = "Classified as schedule",
            confidence = 0.85f,
            isUrgent = false,
            isImportant = true,
            urgencyScore = 0.2f,
            importanceScore = 0.6f,
            urgencySignals = emptyList(),
            importanceSignals = listOf("general task"),
            shouldEscalateToLlm = false
        )
        coEvery { naturalLanguageParser.parse(any()) } returns ParsedTask(
            title = "test task",
            dueDate = null,
            dueTime = null
        )

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
    @DisplayName("Starting voice input")
    inner class StartVoiceInput {

        @Test
        @DisplayName("StartVoiceInput emits StartVoiceRecognition effect")
        fun emitsStartVoiceRecognitionEffect() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(QuickCaptureEvent.StartVoiceInput)
                advanceUntilIdle()

                val effect = awaitItem()
                assertTrue(effect is QuickCaptureEffect.StartVoiceRecognition)
            }
        }

        @Test
        @DisplayName("StartVoiceInput sets isVoiceInputActive and voiceState to Initializing")
        fun setsVoiceActiveAndInitializing() = runTest {
            viewModel.onEvent(QuickCaptureEvent.StartVoiceInput)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isVoiceInputActive)
            assertTrue(state.voiceState is VoiceInputState.Initializing)
        }
    }

    @Nested
    @DisplayName("Voice state updates")
    inner class VoiceStateUpdates {

        @Test
        @DisplayName("updateVoiceState propagates Listening state to UI")
        fun propagatesListeningState() = runTest {
            viewModel.onEvent(QuickCaptureEvent.StartVoiceInput)
            advanceUntilIdle()

            val listeningState = VoiceInputState.Listening(
                partialText = "call mom",
                audioLevel = 0.6f
            )
            viewModel.updateVoiceState(listeningState)

            val state = viewModel.uiState.value
            assertEquals(listeningState, state.voiceState)
        }

        @Test
        @DisplayName("updateVoiceState with Result triggers onVoiceResult pipeline")
        fun resultTriggersParsingPipeline() = runTest {
            coEvery { naturalLanguageParser.parse("buy groceries tomorrow") } returns ParsedTask(
                title = "Buy groceries",
                dueDate = Instant.parse("2026-02-06T09:00:00Z"),
                dueTime = null
            )

            viewModel.onEvent(QuickCaptureEvent.StartVoiceInput)
            advanceUntilIdle()

            viewModel.updateVoiceState(
                VoiceInputState.Result(text = "buy groceries tomorrow", confidence = 0.9f)
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // Voice should be deactivated
            assertFalse(state.isVoiceInputActive)
            // Input should be populated with voice text
            assertEquals("buy groceries tomorrow", state.inputText)
            // Parsed result should be present from AI pipeline
            assertNotNull(state.parsedResult)
            assertEquals("Buy groceries", state.parsedResult?.title)
        }

        @Test
        @DisplayName("updateVoiceState with Error keeps voice active for retry UI")
        fun errorKeepsVoiceActive() = runTest {
            viewModel.onEvent(QuickCaptureEvent.StartVoiceInput)
            advanceUntilIdle()

            val error = VoiceInputState.Error(
                errorType = VoiceErrorType.NO_MATCH,
                message = "Couldn't understand that"
            )
            viewModel.updateVoiceState(error)

            val state = viewModel.uiState.value
            assertTrue(state.isVoiceInputActive, "Voice should remain active to show error overlay")
            assertTrue(state.voiceState is VoiceInputState.Error)
        }
    }

    @Nested
    @DisplayName("Stopping and canceling voice")
    inner class StopAndCancel {

        @Test
        @DisplayName("StopVoiceInput deactivates voice and resets to Idle")
        fun stopDeactivates() = runTest {
            viewModel.onEvent(QuickCaptureEvent.StartVoiceInput)
            advanceUntilIdle()

            viewModel.onEvent(QuickCaptureEvent.StopVoiceInput)

            val state = viewModel.uiState.value
            assertFalse(state.isVoiceInputActive)
            assertTrue(state.voiceState is VoiceInputState.Idle)
        }

        @Test
        @DisplayName("CancelVoiceInput returns to typing mode")
        fun cancelReturnsToTyping() = runTest {
            viewModel.onEvent(QuickCaptureEvent.StartVoiceInput)
            advanceUntilIdle()

            viewModel.onEvent(QuickCaptureEvent.CancelVoiceInput)

            val state = viewModel.uiState.value
            assertFalse(state.isVoiceInputActive)
            assertTrue(state.voiceState is VoiceInputState.Idle)
        }
    }

    @Nested
    @DisplayName("Retrying voice input")
    inner class RetryVoiceInput {

        @Test
        @DisplayName("RetryVoiceInput re-emits StartVoiceRecognition effect")
        fun retryEmitsEffect() = runTest {
            viewModel.effect.test {
                // First start
                viewModel.onEvent(QuickCaptureEvent.StartVoiceInput)
                advanceUntilIdle()
                val first = awaitItem()
                assertTrue(first is QuickCaptureEffect.StartVoiceRecognition)

                // Simulate error
                viewModel.updateVoiceState(
                    VoiceInputState.Error(VoiceErrorType.NO_MATCH, "Couldn't understand")
                )

                // Retry
                viewModel.onEvent(QuickCaptureEvent.RetryVoiceInput)
                advanceUntilIdle()
                val retry = awaitItem()
                assertTrue(retry is QuickCaptureEffect.StartVoiceRecognition)
            }
        }

        @Test
        @DisplayName("RetryVoiceInput resets voice state to Initializing")
        fun retryResetsToInitializing() = runTest {
            viewModel.onEvent(QuickCaptureEvent.StartVoiceInput)
            advanceUntilIdle()

            viewModel.updateVoiceState(
                VoiceInputState.Error(VoiceErrorType.NO_MATCH, "Failed")
            )

            viewModel.onEvent(QuickCaptureEvent.RetryVoiceInput)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isVoiceInputActive)
            assertTrue(state.voiceState is VoiceInputState.Initializing)
        }
    }

    @Nested
    @DisplayName("Reset clears voice state")
    inner class ResetClearsVoice {

        @Test
        @DisplayName("Dismiss resets all state including voice")
        fun dismissClearsVoice() = runTest {
            viewModel.onEvent(QuickCaptureEvent.StartVoiceInput)
            advanceUntilIdle()

            viewModel.updateVoiceState(
                VoiceInputState.Listening(partialText = "test", audioLevel = 0.5f)
            )

            viewModel.onEvent(QuickCaptureEvent.Dismiss)

            val state = viewModel.uiState.value
            assertFalse(state.isVoiceInputActive)
            assertTrue(state.voiceState is VoiceInputState.Idle)
            assertEquals("", state.inputText)
        }
    }
}

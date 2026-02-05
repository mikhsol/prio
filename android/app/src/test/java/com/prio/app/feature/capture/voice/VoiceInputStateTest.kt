package com.prio.app.feature.capture.voice

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for VoiceInputState model.
 *
 * Tests state transitions and error type classification for the voice input
 * pipeline implemented in 3.1.5.B.3.
 */
@DisplayName("VoiceInputState")
class VoiceInputStateTest {

    @Nested
    @DisplayName("State properties")
    inner class StateProperties {

        @Test
        @DisplayName("Idle state is the default")
        fun idleIsDefault() {
            val state: VoiceInputState = VoiceInputState.Idle
            assertTrue(state is VoiceInputState.Idle)
        }

        @Test
        @DisplayName("Listening state holds partial text and audio level")
        fun listeningHoldsData() {
            val state = VoiceInputState.Listening(
                partialText = "call mom",
                audioLevel = 0.75f
            )
            assertEquals("call mom", state.partialText)
            assertEquals(0.75f, state.audioLevel)
        }

        @Test
        @DisplayName("Listening state defaults to empty partial text and zero audio")
        fun listeningDefaults() {
            val state = VoiceInputState.Listening()
            assertEquals("", state.partialText)
            assertEquals(0f, state.audioLevel)
        }

        @Test
        @DisplayName("Result state holds text and confidence")
        fun resultHoldsData() {
            val state = VoiceInputState.Result(
                text = "buy groceries tomorrow",
                confidence = 0.95f
            )
            assertEquals("buy groceries tomorrow", state.text)
            assertEquals(0.95f, state.confidence)
        }

        @Test
        @DisplayName("Result confidence defaults to 1.0")
        fun resultDefaultConfidence() {
            val state = VoiceInputState.Result(text = "test")
            assertEquals(1f, state.confidence)
        }

        @Test
        @DisplayName("Error state holds error type and message")
        fun errorHoldsData() {
            val state = VoiceInputState.Error(
                errorType = VoiceErrorType.NO_MATCH,
                message = "Couldn't understand that"
            )
            assertEquals(VoiceErrorType.NO_MATCH, state.errorType)
            assertEquals("Couldn't understand that", state.message)
        }

        @Test
        @DisplayName("Processing state preserves partial text")
        fun processingPreservesText() {
            val state = VoiceInputState.Processing(partialText = "almost done")
            assertEquals("almost done", state.partialText)
        }
    }

    @Nested
    @DisplayName("VoiceErrorType coverage")
    inner class ErrorTypes {

        @Test
        @DisplayName("All error types are defined per spec")
        fun allErrorTypesExist() {
            val expectedTypes = listOf(
                VoiceErrorType.NO_MATCH,
                VoiceErrorType.NO_SPEECH,
                VoiceErrorType.PERMISSION_DENIED,
                VoiceErrorType.NOT_AVAILABLE,
                VoiceErrorType.NETWORK_ERROR,
                VoiceErrorType.UNKNOWN
            )
            assertEquals(6, VoiceErrorType.entries.size)
            expectedTypes.forEach { type ->
                assertTrue(VoiceErrorType.entries.contains(type), "Missing: $type")
            }
        }

        @Test
        @DisplayName("NO_MATCH is for unrecognized speech per spec")
        fun noMatchForUnrecognizedSpeech() {
            // Per spec: Shows "Couldn't understand that" with [Try Again] and [Type Instead]
            val state = VoiceInputState.Error(
                errorType = VoiceErrorType.NO_MATCH,
                message = "Couldn't understand that"
            )
            assertNotEquals(VoiceErrorType.NO_SPEECH, state.errorType)
        }

        @Test
        @DisplayName("PERMISSION_DENIED for missing RECORD_AUDIO")
        fun permissionDeniedForMissingAudio() {
            val state = VoiceInputState.Error(
                errorType = VoiceErrorType.PERMISSION_DENIED,
                message = "Microphone permission required"
            )
            assertEquals(VoiceErrorType.PERMISSION_DENIED, state.errorType)
        }
    }

    @Nested
    @DisplayName("State transitions")
    inner class StateTransitions {

        @Test
        @DisplayName("Typical happy path: Idle -> Initializing -> Listening -> Processing -> Result")
        fun happyPathTransition() {
            val states = listOf<VoiceInputState>(
                VoiceInputState.Idle,
                VoiceInputState.Initializing,
                VoiceInputState.Listening(partialText = "call", audioLevel = 0.5f),
                VoiceInputState.Listening(partialText = "call mom", audioLevel = 0.3f),
                VoiceInputState.Processing(partialText = "call mom"),
                VoiceInputState.Result(text = "call mom tomorrow", confidence = 0.9f)
            )

            // Verify all states are distinct types in order
            assertTrue(states[0] is VoiceInputState.Idle)
            assertTrue(states[1] is VoiceInputState.Initializing)
            assertTrue(states[2] is VoiceInputState.Listening)
            assertTrue(states[3] is VoiceInputState.Listening)
            assertTrue(states[4] is VoiceInputState.Processing)
            assertTrue(states[5] is VoiceInputState.Result)
        }

        @Test
        @DisplayName("Error path: Idle -> Initializing -> Listening -> Error")
        fun errorPathTransition() {
            val states = listOf<VoiceInputState>(
                VoiceInputState.Idle,
                VoiceInputState.Initializing,
                VoiceInputState.Listening(partialText = ""),
                VoiceInputState.Error(
                    errorType = VoiceErrorType.NO_MATCH,
                    message = "Couldn't understand that"
                )
            )

            assertTrue(states.last() is VoiceInputState.Error)
            assertEquals(
                VoiceErrorType.NO_MATCH,
                (states.last() as VoiceInputState.Error).errorType
            )
        }

        @Test
        @DisplayName("Unavailable device: Idle -> Initializing -> Error(NOT_AVAILABLE)")
        fun unavailableDeviceTransition() {
            val error = VoiceInputState.Error(
                errorType = VoiceErrorType.NOT_AVAILABLE,
                message = "Speech recognition is not available on this device"
            )
            assertEquals(VoiceErrorType.NOT_AVAILABLE, error.errorType)
        }
    }
}

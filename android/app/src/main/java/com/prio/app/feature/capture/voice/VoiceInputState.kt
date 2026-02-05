package com.prio.app.feature.capture.voice

/**
 * States for the voice input pipeline.
 *
 * Per 1.1.3 Quick Capture Flow Specification:
 * - Large 🎤 icon displayed when listening
 * - Audio visualization waveform
 * - "Listening..." label
 * - Live transcription area shows text in real-time
 * - Auto-stops after 2 seconds of silence
 *
 * Implements 3.1.5.B.3 from ACTION_PLAN.md.
 */
sealed interface VoiceInputState {
    /** Voice input is idle / not active. */
    data object Idle : VoiceInputState

    /** Waiting for SpeechRecognizer to become ready. */
    data object Initializing : VoiceInputState

    /** Actively listening to user speech. */
    data class Listening(
        /** Partial transcription text updated in real-time. */
        val partialText: String = "",
        /** Current RMS dB level for waveform visualization (0.0–1.0 normalized). */
        val audioLevel: Float = 0f
    ) : VoiceInputState

    /** Processing final result after user stopped speaking. */
    data class Processing(
        val partialText: String = ""
    ) : VoiceInputState

    /** Voice recognition produced a final result. */
    data class Result(
        val text: String,
        val confidence: Float = 1f
    ) : VoiceInputState

    /** Voice recognition encountered an error. */
    data class Error(
        val errorType: VoiceErrorType,
        val message: String
    ) : VoiceInputState
}

/**
 * Categorized voice recognition errors for appropriate UI handling.
 */
enum class VoiceErrorType {
    /** User's speech could not be understood. */
    NO_MATCH,
    /** No speech detected within timeout. */
    NO_SPEECH,
    /** Microphone permission denied. */
    PERMISSION_DENIED,
    /** SpeechRecognizer not available on device. */
    NOT_AVAILABLE,
    /** Network error (shouldn't happen with on-device, but fallback). */
    NETWORK_ERROR,
    /** Generic / unknown error. */
    UNKNOWN
}

package com.prio.app.feature.capture.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

/**
 * Wraps Android's [SpeechRecognizer] API for on-device speech-to-text.
 *
 * Per 1.1.3 Quick Capture Flow Specification:
 * - Uses Android Speech-to-Text API with streaming transcription
 * - Auto-stops after 2 seconds of silence
 * - Visual audio waveform animation (via RMS dB callback)
 * - Haptic pulse on voice detection
 * - Privacy: on-device processing preferred
 *
 * Implements task 3.1.5.B.3 from ACTION_PLAN.md.
 *
 * Usage:
 * ```
 * val manager = VoiceInputManager(context)
 * if (manager.isAvailable()) {
 *     manager.startListening().collect { state ->
 *         when (state) {
 *             is VoiceInputState.Listening -> updateUI(state.partialText, state.audioLevel)
 *             is VoiceInputState.Result -> onFinalText(state.text)
 *             is VoiceInputState.Error -> showError(state.message)
 *         }
 *     }
 * }
 * ```
 *
 * @param context Application or Activity context for SpeechRecognizer creation
 */
class VoiceInputManager(private val context: Context) {

    companion object {
        private const val TAG = "VoiceInputManager"

        /**
         * Maximum RMS dB value observed from SpeechRecognizer.
         * Used to normalize audio levels to 0.0–1.0 range.
         */
        private const val MAX_RMS_DB = 12f

        /**
         * Silence timeout in milliseconds.
         * Per spec: auto-stop after 2 seconds of silence.
         */
        private const val SILENCE_TIMEOUT_MS = 2000L

        /**
         * Maximum listening duration in milliseconds (safety limit).
         * Prevents indefinite listening if silence detection fails.
         */
        private const val MAX_LISTEN_DURATION_MS = 30_000L
    }

    private val _state = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    val state: StateFlow<VoiceInputState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    /**
     * Check if speech recognition is available on this device.
     *
     * @return true if SpeechRecognizer is supported
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Check if on-device speech recognition is available (API 31+).
     * Falls back to cloud-based if not available.
     *
     * @return true if on-device recognition is supported
     */
    fun isOnDeviceAvailable(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } else {
            false
        }
    }

    /**
     * Start listening for voice input and emit states as a Flow.
     *
     * The flow emits [VoiceInputState] values:
     * 1. [VoiceInputState.Initializing] — SpeechRecognizer is being set up
     * 2. [VoiceInputState.Listening] — Partial results and audio levels
     * 3. [VoiceInputState.Result] — Final transcription
     * 4. [VoiceInputState.Error] — If recognition fails
     *
     * The flow completes after a [Result] or [Error] state.
     *
     * @return Flow of voice input states
     */
    fun startListening(): Flow<VoiceInputState> = callbackFlow {
        _state.value = VoiceInputState.Initializing
        trySend(VoiceInputState.Initializing)

        if (!isAvailable()) {
            val error = VoiceInputState.Error(
                errorType = VoiceErrorType.NOT_AVAILABLE,
                message = "Speech recognition is not available on this device"
            )
            _state.value = error
            trySend(error)
            close()
            return@callbackFlow
        }

        // Prefer on-device recognizer for privacy (API 31+)
        val recognizer = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
            && isOnDeviceAvailable()
        ) {
            Timber.d("Using on-device SpeechRecognizer")
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            Timber.d("Using default SpeechRecognizer (may use cloud)")
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        speechRecognizer = recognizer

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Timber.d("$TAG: Ready for speech")
                val listening = VoiceInputState.Listening()
                _state.value = listening
                trySend(listening)
            }

            override fun onBeginningOfSpeech() {
                Timber.d("$TAG: Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Normalize RMS dB to 0.0–1.0 range for waveform visualization
                val normalized = (rmsdB / MAX_RMS_DB).coerceIn(0f, 1f)
                val current = _state.value
                if (current is VoiceInputState.Listening) {
                    val updated = current.copy(audioLevel = normalized)
                    _state.value = updated
                    trySend(updated)
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Not used for our purposes
            }

            override fun onEndOfSpeech() {
                Timber.d("$TAG: End of speech detected")
                val current = _state.value
                val partialText = when (current) {
                    is VoiceInputState.Listening -> current.partialText
                    else -> ""
                }
                val processing = VoiceInputState.Processing(partialText)
                _state.value = processing
                trySend(processing)
            }

            override fun onError(error: Int) {
                Timber.e("$TAG: Recognition error: $error")
                val voiceError = mapSpeechError(error)
                _state.value = voiceError
                trySend(voiceError)
                close()
            }

            override fun onResults(results: Bundle?) {
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                val confidenceScores = results
                    ?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                if (!matches.isNullOrEmpty()) {
                    val bestMatch = matches[0]
                    val confidence = confidenceScores?.firstOrNull() ?: 1f

                    Timber.d("$TAG: Final result: '$bestMatch' (confidence: $confidence)")

                    val result = VoiceInputState.Result(
                        text = bestMatch,
                        confidence = confidence
                    )
                    _state.value = result
                    trySend(result)
                } else {
                    val error = VoiceInputState.Error(
                        errorType = VoiceErrorType.NO_MATCH,
                        message = "Couldn't understand that"
                    )
                    _state.value = error
                    trySend(error)
                }
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partialMatches = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if (!partialMatches.isNullOrEmpty()) {
                    val partialText = partialMatches[0]
                    Timber.d("$TAG: Partial result: '$partialText'")

                    val listening = VoiceInputState.Listening(
                        partialText = partialText,
                        audioLevel = (_state.value as? VoiceInputState.Listening)?.audioLevel ?: 0f
                    )
                    _state.value = listening
                    trySend(listening)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Timber.d("$TAG: Event: $eventType")
            }
        }

        recognizer.setRecognitionListener(listener)

        // Build recognition intent
        val intent = createRecognizerIntent()
        recognizer.startListening(intent)

        Timber.d("$TAG: Started listening")

        awaitClose {
            Timber.d("$TAG: Flow closing, stopping recognizer")
            stopInternal()
        }
    }

    /**
     * Stop listening and release resources.
     */
    fun stopListening() {
        stopInternal()
        _state.value = VoiceInputState.Idle
    }

    /**
     * Cancel current recognition without waiting for results.
     */
    fun cancel() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error cancelling recognizer")
        }
        _state.value = VoiceInputState.Idle
    }

    /**
     * Release all resources. Call when the voice input is no longer needed.
     */
    fun destroy() {
        stopInternal()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _state.value = VoiceInputState.Idle
    }

    // ========================================================================
    // Private helpers
    // ========================================================================

    private fun stopInternal() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error stopping recognizer")
        }
    }

    /**
     * Create the recognition intent with on-device processing preferences.
     *
     * Key settings per spec:
     * - Partial results enabled for real-time transcription
     * - Language model: free-form (natural language)
     * - Prefer on-device processing for privacy
     */
    private fun createRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            // Enable partial results for streaming transcription
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            // Set max results
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            // Prefer offline/on-device recognition for privacy (API 23+)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)

            // Silence detection: complete after 2s silence per spec
            // EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS controls
            // how long to wait after speech ends before finalizing
            putExtra(
                "android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS",
                SILENCE_TIMEOUT_MS
            )
            // Minimum silence before treating as end of speech
            putExtra(
                "android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS",
                SILENCE_TIMEOUT_MS
            )

            // Set a maximum duration for safety
            putExtra(
                "android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS",
                500L
            )
        }
    }

    /**
     * Map SpeechRecognizer error codes to our typed error model.
     */
    private fun mapSpeechError(errorCode: Int): VoiceInputState.Error {
        return when (errorCode) {
            SpeechRecognizer.ERROR_NO_MATCH -> VoiceInputState.Error(
                errorType = VoiceErrorType.NO_MATCH,
                message = "Couldn't understand that"
            )
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceInputState.Error(
                errorType = VoiceErrorType.NO_SPEECH,
                message = "No speech detected"
            )
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceInputState.Error(
                errorType = VoiceErrorType.PERMISSION_DENIED,
                message = "Microphone permission required"
            )
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceInputState.Error(
                errorType = VoiceErrorType.UNKNOWN,
                message = "Speech recognizer is busy, try again"
            )
            SpeechRecognizer.ERROR_CLIENT -> VoiceInputState.Error(
                errorType = VoiceErrorType.UNKNOWN,
                message = "Voice input error, please try again"
            )
            SpeechRecognizer.ERROR_AUDIO -> VoiceInputState.Error(
                errorType = VoiceErrorType.UNKNOWN,
                message = "Audio recording error"
            )
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceInputState.Error(
                errorType = VoiceErrorType.NETWORK_ERROR,
                message = "Network error. On-device recognition may not be available."
            )
            SpeechRecognizer.ERROR_SERVER -> VoiceInputState.Error(
                errorType = VoiceErrorType.NETWORK_ERROR,
                message = "Server error. Try again."
            )
            else -> VoiceInputState.Error(
                errorType = VoiceErrorType.UNKNOWN,
                message = "Voice input failed (error $errorCode)"
            )
        }
    }
}

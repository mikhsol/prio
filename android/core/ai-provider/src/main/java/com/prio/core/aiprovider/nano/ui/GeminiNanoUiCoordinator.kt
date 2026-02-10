package com.prio.core.aiprovider.nano.ui

import com.prio.core.aiprovider.nano.GeminiNanoAvailability
import com.prio.core.aiprovider.nano.GeminiNanoAvailability.FeatureStatus
import com.prio.core.aiprovider.nano.GeminiNanoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ViewModel-like coordinator for Gemini Nano UI actions.
 *
 * Task 3.6.7: Manages download triggers, progress observation, and state
 * for the Settings > AI Model section and Onboarding model setup screen.
 *
 * This class is **not** a Jetpack ViewModel — it lives in the :core:ai-provider
 * module so it can be shared across app-layer ViewModels without circular
 * dependency.
 */
@Singleton
class GeminiNanoUiCoordinator @Inject constructor(
    private val availability: GeminiNanoAvailability,
    private val provider: GeminiNanoProvider
) {
    companion object {
        private const val TAG = "GeminiNanoUiCoord"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(GeminiNanoUiState())
    val uiState: StateFlow<GeminiNanoUiState> = _uiState.asStateFlow()

    init {
        // Observe availability status changes → UI state
        scope.launch {
            combine(
                availability.promptStatus,
                availability.summarizationStatus,
                provider.isAvailable
            ) { prompt, summary, ready ->
                GeminiNanoUiState(
                    promptStatus = prompt,
                    summarizationStatus = summary,
                    isReady = ready
                )
            }.collect { newState ->
                _uiState.update { current ->
                    newState.copy(
                        isLoading = current.isLoading,
                        message = current.message
                    )
                }
            }
        }
    }

    /**
     * Check whether Gemini Nano is available on this device.
     * Call on Settings screen open or Onboarding screen 4 entry.
     */
    fun checkAvailability() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                availability.checkAll()
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Availability check failed")
                _uiState.update { it.copy(message = "Check failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Trigger Gemini Nano model download (if status is DOWNLOADABLE).
     */
    fun downloadAndActivate() {
        scope.launch {
            val status = availability.promptStatus.value
            if (status !is FeatureStatus.Downloadable && status !is FeatureStatus.Available) {
                _uiState.update { it.copy(message = "Gemini Nano not available for download") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val success = provider.initialize()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = if (success) "Gemini Nano activated!" else "Activation failed"
                    )
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Download/activation failed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Clear the transient user message.
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

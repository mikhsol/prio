package com.prio.core.aiprovider.nano.ui

import com.prio.core.aiprovider.nano.GeminiNanoAvailability.FeatureStatus

/**
 * UI state for the Gemini Nano model management screen.
 *
 * Task 3.6.7: Download management and progress UI.
 * Task 3.6.8: Onboarding model setup integration.
 */
data class GeminiNanoUiState(
    /** Current Prompt API status */
    val promptStatus: FeatureStatus = FeatureStatus.Unavailable("Not checked"),
    /** Current Summarization API status */
    val summarizationStatus: FeatureStatus = FeatureStatus.Unavailable("Not checked"),
    /** Whether the provider is initialized and ready */
    val isReady: Boolean = false,
    /** Whether a download/initialization operation is in progress */
    val isLoading: Boolean = false,
    /** User-facing message (e.g., error, success) */
    val message: String? = null
) {
    /** Whether Gemini Nano is available on this device (available or downloadable) */
    val isSupported: Boolean
        get() = promptStatus is FeatureStatus.Available ||
                promptStatus is FeatureStatus.Downloadable ||
                summarizationStatus is FeatureStatus.Available ||
                summarizationStatus is FeatureStatus.Downloadable

    /** Download progress percentage (0-100) or null if not downloading */
    val downloadProgress: Int?
        get() = when (promptStatus) {
            is FeatureStatus.Downloading -> promptStatus.progressPercent
            else -> null
        }

    /** Short summary for settings / onboarding display */
    val statusSummary: String
        get() = when {
            isReady -> "Ready — Gemini Nano active"
            promptStatus is FeatureStatus.Available -> "Available — tap to activate"
            promptStatus is FeatureStatus.Downloading -> "Downloading… ${(promptStatus as FeatureStatus.Downloading).progressPercent}%"
            promptStatus is FeatureStatus.Downloadable -> "Available for download"
            else -> "Not available on this device"
        }
}

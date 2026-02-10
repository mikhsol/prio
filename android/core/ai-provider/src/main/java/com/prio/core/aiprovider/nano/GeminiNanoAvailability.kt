package com.prio.core.aiprovider.nano

import android.content.Context
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus as MlKitFeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for detecting Gemini Nano (Android AI Core) availability.
 *
 * Task 3.6.1: Feature detection wrapping ML Kit GenAI `checkFeatureStatus()` /
 * `checkStatus()` for both Prompt and Summarization APIs.
 *
 * Devices without AI Core (mid-range, unlocked bootloaders) gracefully
 * receive [FeatureStatus.Unavailable] and fall back to llama.cpp or rule-based.
 */
@Singleton
class GeminiNanoAvailability @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "GeminiNanoAvailability"
    }

    /**
     * Sealed class representing the status of a Gemini Nano feature.
     */
    sealed class FeatureStatus {
        /** Feature is available and ready for inference. */
        data object Available : FeatureStatus()

        /** Feature model can be downloaded (AI Core present, model not yet fetched). */
        data object Downloadable : FeatureStatus()

        /** Feature is currently downloading. */
        data class Downloading(val progressPercent: Int) : FeatureStatus()

        /** Feature is not available on this device. */
        data class Unavailable(val reason: String) : FeatureStatus()
    }

    private val _promptStatus = MutableStateFlow<FeatureStatus>(FeatureStatus.Unavailable("Not checked"))
    val promptStatus: StateFlow<FeatureStatus> = _promptStatus.asStateFlow()

    private val _summarizationStatus = MutableStateFlow<FeatureStatus>(FeatureStatus.Unavailable("Not checked"))
    val summarizationStatus: StateFlow<FeatureStatus> = _summarizationStatus.asStateFlow()

    /**
     * Whether any Gemini Nano feature is available or downloadable.
     */
    val isSupported: Boolean
        get() = _promptStatus.value is FeatureStatus.Available ||
                _promptStatus.value is FeatureStatus.Downloadable ||
                _summarizationStatus.value is FeatureStatus.Available ||
                _summarizationStatus.value is FeatureStatus.Downloadable

    /**
     * Whether any Gemini Nano feature is ready for inference right now.
     */
    val isReady: Boolean
        get() = _promptStatus.value is FeatureStatus.Available ||
                _summarizationStatus.value is FeatureStatus.Available

    /**
     * Check availability of both Prompt and Summarization APIs.
     * Safe to call on any device — returns [FeatureStatus.Unavailable] if
     * AI Core or Google Play Services are absent.
     */
    suspend fun checkAll() {
        checkPromptApi()
        checkSummarizationApi()
    }

    /**
     * Check Prompt API availability using [GenerativeModel.checkStatus].
     */
    suspend fun checkPromptApi() {
        try {
            val model = Generation.getClient()
            val statusCode = model.checkStatus()
            _promptStatus.value = mapFeatureStatus(statusCode, "Prompt")
            model.close()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Prompt API check failed")
            _promptStatus.value = FeatureStatus.Unavailable(
                "Check failed: ${e.message ?: "unknown error"}"
            )
        }
    }

    /**
     * Check Summarization API availability using [Summarizer.checkFeatureStatus].
     */
    suspend fun checkSummarizationApi() {
        try {
            val options = SummarizerOptions.builder(context)
                .setInputType(SummarizerOptions.InputType.ARTICLE)
                .build()
            val summarizer = Summarization.getClient(options)
            val statusCode = withContext(Dispatchers.IO) {
                summarizer.checkFeatureStatus().get()
            }
            _summarizationStatus.value = mapFeatureStatus(statusCode, "Summarization")
            summarizer.close()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Summarization API check failed")
            _summarizationStatus.value = FeatureStatus.Unavailable(
                "Check failed: ${e.message ?: "unknown error"}"
            )
        }
    }

    /**
     * Trigger model download for Prompt API and observe progress.
     * Returns `true` when download completes successfully.
     */
    suspend fun downloadPromptModel(): Boolean {
        return try {
            _promptStatus.value = FeatureStatus.Downloading(0)
            val model = Generation.getClient()
            var success = false
            model.download().collect { status ->
                when (status) {
                    is DownloadStatus.DownloadStarted -> {
                        Timber.tag(TAG).d("Prompt model download started: ${status.bytesToDownload} bytes")
                        _promptStatus.value = FeatureStatus.Downloading(1)
                    }
                    is DownloadStatus.DownloadProgress -> {
                        Timber.tag(TAG).d("Prompt model download progress: ${status.totalBytesDownloaded} bytes")
                        _promptStatus.value = FeatureStatus.Downloading(50)
                    }
                    is DownloadStatus.DownloadCompleted -> {
                        _promptStatus.value = FeatureStatus.Available
                        Timber.tag(TAG).i("Prompt model download completed")
                        success = true
                    }
                    is DownloadStatus.DownloadFailed -> {
                        val errorMsg = status.e.message ?: "unknown"
                        _promptStatus.value = FeatureStatus.Unavailable("Download failed: $errorMsg")
                        Timber.tag(TAG).e("Prompt model download failed: $errorMsg")
                    }
                }
            }
            model.close()
            success
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Prompt model download exception")
            _promptStatus.value = FeatureStatus.Unavailable("Download error: ${e.message}")
            false
        }
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /**
     * Map the ML Kit [DownloadStatus] integer constants to our sealed [FeatureStatus].
     */
    private fun mapFeatureStatus(statusCode: Int, apiName: String): FeatureStatus {
        return when (statusCode) {
            MlKitFeatureStatus.AVAILABLE -> {
                Timber.tag(TAG).i("$apiName API: AVAILABLE")
                FeatureStatus.Available
            }
            MlKitFeatureStatus.DOWNLOADABLE -> {
                Timber.tag(TAG).i("$apiName API: DOWNLOADABLE")
                FeatureStatus.Downloadable
            }
            else -> {
                Timber.tag(TAG).w("$apiName API: UNAVAILABLE (status=$statusCode)")
                FeatureStatus.Unavailable("AI Core feature not available (code=$statusCode)")
            }
        }
    }
}

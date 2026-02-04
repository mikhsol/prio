package com.prio.core.ai.registry

import android.content.Context
import com.prio.core.ai.provider.ModelDefinition
import com.prio.core.ai.provider.PredefinedModels
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages model downloads with progress tracking and resume support.
 * 
 * Based on ACTION_PLAN.md Milestone 2.2.4:
 * - Downloads model with progress tracking
 * - SHA-256 verification after download
 * - Resume on failure (range requests)
 * - Atomic file operations (temp file → final file)
 * 
 * Performance considerations:
 * - Model sizes: Phi-3 (2.3GB), Mistral 7B (4.1GB), Gemma 2B (1.7GB)
 * - Uses chunked downloads with 8KB buffer
 * - Supports HTTP Range headers for resume
 */
@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelRegistry: ModelRegistry
) {
    companion object {
        private const val BUFFER_SIZE = 8192
        private const val TEMP_SUFFIX = ".downloading"
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 60_000
    }
    
    /** Current download state */
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(
            val modelId: String,
            val progress: Float,
            val downloadedBytes: Long,
            val totalBytes: Long
        ) : DownloadState()
        data class Verifying(val modelId: String) : DownloadState()
        data class Completed(val modelId: String) : DownloadState()
        data class Failed(val modelId: String, val error: String) : DownloadState()
        data class Cancelled(val modelId: String) : DownloadState()
    }
    
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()
    
    /** Current download progress (0.0 to 1.0) */
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    /** Flag to cancel current download */
    @Volatile
    private var isCancelled = false
    
    /** Currently downloading model ID */
    @Volatile
    private var currentDownloadId: String? = null
    
    /**
     * Download a model with progress tracking and resume support.
     * 
     * @param modelId The model ID to download
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return Result indicating success or failure
     */
    suspend fun downloadModel(
        modelId: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        Timber.d("Starting download for model: $modelId")
        
        val definition = modelRegistry.getModelDefinition(modelId)
            ?: return@withContext Result.failure(IllegalArgumentException("Unknown model: $modelId"))
        
        // Can't download rule-based
        if (definition.id == PredefinedModels.RULE_BASED.id) {
            return@withContext Result.failure(IllegalArgumentException("Rule-based model doesn't require download"))
        }
        
        // Already downloaded?
        if (modelRegistry.isModelDownloaded(modelId)) {
            Timber.d("Model already downloaded: $modelId")
            return@withContext Result.success(Unit)
        }
        
        // Check if another download is in progress
        if (currentDownloadId != null && currentDownloadId != modelId) {
            return@withContext Result.failure(
                IllegalStateException("Another download in progress: $currentDownloadId")
            )
        }
        
        // Reset state
        isCancelled = false
        currentDownloadId = modelId
        _progress.value = 0f
        
        val modelsDir = modelRegistry.getModelsDirectory()
        val targetFile = File(modelsDir, definition.fileName)
        val tempFile = File(modelsDir, definition.fileName + TEMP_SUFFIX)
        
        try {
            // Check available storage
            val requiredSpace = definition.sizeBytes
            val availableSpace = modelsDir.freeSpace
            if (availableSpace < requiredSpace * 1.1) { // 10% buffer
                throw IOException("Insufficient storage. Need ${formatBytes(requiredSpace)}, have ${formatBytes(availableSpace)}")
            }
            
            // Start download
            _downloadState.value = DownloadState.Downloading(
                modelId = modelId,
                progress = 0f,
                downloadedBytes = 0,
                totalBytes = definition.sizeBytes
            )
            
            // Get existing download progress for resume
            val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
            
            downloadWithResume(
                url = definition.downloadUrl,
                tempFile = tempFile,
                expectedSize = definition.sizeBytes,
                startByte = existingBytes,
                onProgress = { downloadedBytes, totalBytes ->
                    val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                    _progress.value = progress
                    _downloadState.value = DownloadState.Downloading(
                        modelId = modelId,
                        progress = progress,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes
                    )
                    onProgress?.invoke(progress)
                }
            )
            
            if (isCancelled) {
                _downloadState.value = DownloadState.Cancelled(modelId)
                Timber.d("Download cancelled: $modelId")
                return@withContext Result.failure(CancellationException("Download cancelled"))
            }
            
            // Verify hash
            _downloadState.value = DownloadState.Verifying(modelId)
            Timber.d("Verifying SHA-256 for: $modelId")
            
            if (definition.sha256.isNotEmpty()) {
                val actualHash = calculateSha256(tempFile)
                if (!actualHash.equals(definition.sha256, ignoreCase = true)) {
                    tempFile.delete()
                    throw IOException("SHA-256 verification failed. Expected: ${definition.sha256}, Got: $actualHash")
                }
                Timber.d("SHA-256 verified for: $modelId")
            } else {
                Timber.w("No SHA-256 hash configured for model: $modelId, skipping verification")
            }
            
            // Atomic move: temp → final
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                // Fallback: copy and delete
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            
            // Register with registry
            modelRegistry.registerDownloadedModel(modelId)
            
            _downloadState.value = DownloadState.Completed(modelId)
            _progress.value = 1f
            Timber.i("Model download completed: $modelId")
            
            Result.success(Unit)
            
        } catch (e: CancellationException) {
            _downloadState.value = DownloadState.Cancelled(modelId)
            Timber.d("Download cancelled: $modelId")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Download failed for model: $modelId")
            _downloadState.value = DownloadState.Failed(modelId, e.message ?: "Unknown error")
            Result.failure(e)
        } finally {
            currentDownloadId = null
        }
    }
    
    /**
     * Cancel the current download.
     */
    fun cancelDownload() {
        Timber.d("Cancelling download for: $currentDownloadId")
        isCancelled = true
    }
    
    /**
     * Check if a download can be resumed.
     */
    fun canResumeDownload(modelId: String): Boolean {
        val definition = modelRegistry.getModelDefinition(modelId) ?: return false
        val tempFile = File(modelRegistry.getModelsDirectory(), definition.fileName + TEMP_SUFFIX)
        return tempFile.exists() && tempFile.length() > 0
    }
    
    /**
     * Get the progress of a partially downloaded model.
     */
    fun getPartialDownloadProgress(modelId: String): Float {
        val definition = modelRegistry.getModelDefinition(modelId) ?: return 0f
        val tempFile = File(modelRegistry.getModelsDirectory(), definition.fileName + TEMP_SUFFIX)
        if (!tempFile.exists()) return 0f
        return tempFile.length().toFloat() / definition.sizeBytes
    }
    
    /**
     * Delete partial download file.
     */
    fun deletePartialDownload(modelId: String): Boolean {
        val definition = modelRegistry.getModelDefinition(modelId) ?: return false
        val tempFile = File(modelRegistry.getModelsDirectory(), definition.fileName + TEMP_SUFFIX)
        return if (tempFile.exists()) tempFile.delete() else true
    }
    
    /**
     * Download with HTTP Range support for resume.
     */
    private fun downloadWithResume(
        url: String,
        tempFile: File,
        expectedSize: Long,
        startByte: Long,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ) {
        Timber.d("Downloading from $url, starting at byte $startByte")
        
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Prio/1.0")
            
            // Request range for resume
            if (startByte > 0) {
                connection.setRequestProperty("Range", "bytes=$startByte-")
                Timber.d("Requesting range: bytes=$startByte-")
            }
            
            connection.connect()
            
            val responseCode = connection.responseCode
            Timber.d("HTTP response: $responseCode")
            
            // Check response
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    // Server doesn't support range, start over
                    if (startByte > 0) {
                        Timber.w("Server doesn't support range requests, starting from beginning")
                        tempFile.delete()
                    }
                }
                HttpURLConnection.HTTP_PARTIAL -> {
                    // Range request successful
                    Timber.d("Resuming from byte $startByte")
                }
                else -> {
                    throw IOException("HTTP error: $responseCode ${connection.responseMessage}")
                }
            }
            
            val contentLength = connection.contentLengthLong
            val totalBytes = if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                startByte + contentLength
            } else {
                if (contentLength > 0) contentLength else expectedSize
            }
            
            // Open file for append or overwrite
            val append = responseCode == HttpURLConnection.HTTP_PARTIAL && startByte > 0
            RandomAccessFile(tempFile, "rw").use { raf ->
                if (append) {
                    raf.seek(startByte)
                } else {
                    raf.setLength(0)
                }
                
                connection.inputStream.use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var downloadedBytes = if (append) startByte else 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled) {
                            Timber.d("Download cancelled during transfer")
                            return
                        }
                        
                        raf.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        onProgress(downloadedBytes, totalBytes)
                    }
                    
                    Timber.d("Download transfer complete: $downloadedBytes bytes")
                }
            }
            
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Calculate SHA-256 hash of a file.
     */
    private fun calculateSha256(file: File): String {
        Timber.d("Calculating SHA-256 for: ${file.name}")
        
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Format bytes to human-readable string.
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> String.format("%.2f GB", bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> String.format("%.2f MB", bytes / 1_000_000.0)
            bytes >= 1_000 -> String.format("%.2f KB", bytes / 1_000.0)
            else -> "$bytes B"
        }
    }
    
    /**
     * Get estimated download time based on typical connection speeds.
     * 
     * @param modelId The model to estimate for
     * @param mbps Estimated connection speed in Mbps
     * @return Estimated time in seconds
     */
    fun estimateDownloadTime(modelId: String, mbps: Float = 10f): Long {
        val definition = modelRegistry.getModelDefinition(modelId) ?: return 0
        val bytesPerSecond = (mbps * 1_000_000 / 8).toLong()
        return definition.sizeBytes / bytesPerSecond
    }
    
    /**
     * Check if there's enough storage for a model.
     */
    fun hasEnoughStorage(modelId: String): Boolean {
        val definition = modelRegistry.getModelDefinition(modelId) ?: return false
        val requiredSpace = definition.sizeBytes
        val availableSpace = modelRegistry.getModelsDirectory().freeSpace
        return availableSpace >= requiredSpace * 1.1 // 10% buffer
    }
    
    /**
     * Get the storage space that would be freed by deleting a model.
     */
    fun getModelStorageSize(modelId: String): Long {
        val definition = modelRegistry.getModelDefinition(modelId) ?: return 0
        val file = File(modelRegistry.getModelsDirectory(), definition.fileName)
        return if (file.exists()) file.length() else 0
    }
}

/**
 * Download progress information.
 */
data class DownloadProgress(
    val modelId: String,
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val bytesPerSecond: Long = 0,
    val estimatedSecondsRemaining: Long = 0
)

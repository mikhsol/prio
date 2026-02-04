package com.prio.core.ai.registry

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.prio.core.ai.provider.AiCapability
import com.prio.core.ai.provider.ModelDefinition
import com.prio.core.ai.provider.ModelInfo
import com.prio.core.ai.provider.PredefinedModels
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages available models and supports runtime switching.
 * Key for easy model replacement without code changes.
 * 
 * Based on ARCHITECTURE.md Model Registry design and ACTION_PLAN.md Milestone 2.2.3.
 * 
 * Responsibilities:
 * - Lists available models (downloaded + available for download)
 * - Tracks model versions and checksums
 * - Handles model lifecycle (download, verify, delete)
 * - Supports runtime model switching
 * 
 * Device Tier Support (from 0.2.4):
 * - Tier 1-2 (65% market, 6GB+): Full LLM support
 * - Tier 3-4: Rule-based only
 */
@Singleton
class ModelRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: DataStore<Preferences>
) {
    
    companion object {
        private val KEY_ACTIVE_MODEL = stringPreferencesKey("active_model_id")
        private val KEY_DOWNLOADED_MODELS = stringSetPreferencesKey("downloaded_model_ids")
        
        /** Default models directory within app files */
        private const val MODELS_DIR = "models"
    }
    
    /** All known models in the catalog */
    private val modelCatalog: List<ModelDefinition> = PredefinedModels.ALL
    
    /** Currently active model ID */
    private val _activeModelId = MutableStateFlow<String?>(null)
    val activeModelId: StateFlow<String?> = _activeModelId.asStateFlow()
    
    /** Set of downloaded model IDs */
    private val _downloadedModelIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedModelIds: StateFlow<Set<String>> = _downloadedModelIds.asStateFlow()
    
    /** Available models with download status */
    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels.asStateFlow()
    
    /** Model download/operation in progress */
    private val _isOperationInProgress = MutableStateFlow(false)
    val isOperationInProgress: StateFlow<Boolean> = _isOperationInProgress.asStateFlow()
    
    /** Last error message */
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()
    
    /**
     * Initialize the registry by loading saved preferences.
     */
    suspend fun initialize() {
        Timber.d("Initializing ModelRegistry")
        
        // Load saved preferences
        preferences.data.first().let { prefs ->
            _activeModelId.value = prefs[KEY_ACTIVE_MODEL] ?: PredefinedModels.DEFAULT.id
            _downloadedModelIds.value = prefs[KEY_DOWNLOADED_MODELS] ?: emptySet()
        }
        
        // Scan models directory to verify downloaded models
        scanDownloadedModels()
        
        // Update available models list
        refreshAvailableModels()
        
        Timber.d("ModelRegistry initialized. Active: ${_activeModelId.value}, Downloaded: ${_downloadedModelIds.value}")
    }
    
    /**
     * Get all models in the catalog with their current status.
     */
    fun getModelCatalog(): List<ModelDefinition> = modelCatalog
    
    /**
     * Get a specific model definition by ID.
     */
    fun getModelDefinition(modelId: String): ModelDefinition? {
        return modelCatalog.find { it.id == modelId }
    }
    
    /**
     * Check if a model is downloaded and available.
     */
    fun isModelDownloaded(modelId: String): Boolean {
        // Rule-based is always available
        if (modelId == PredefinedModels.RULE_BASED.id) return true
        
        return _downloadedModelIds.value.contains(modelId) && 
               getModelFile(modelId).exists()
    }
    
    /**
     * Get the file path for a downloaded model.
     * Returns null if model is not downloaded.
     */
    fun getModelPath(modelId: String): String? {
        val definition = getModelDefinition(modelId) ?: return null
        
        // Rule-based has no file
        if (definition.fileName.isEmpty()) return null
        
        val file = getModelFile(modelId)
        return if (file.exists()) file.absolutePath else null
    }
    
    /**
     * Get the path for the currently active model.
     */
    suspend fun getActiveModelPath(): String? {
        val activeId = _activeModelId.value ?: return null
        return getModelPath(activeId)
    }
    
    /**
     * Get the currently active model definition.
     */
    fun getActiveModel(): ModelDefinition? {
        return _activeModelId.value?.let { getModelDefinition(it) }
    }
    
    /**
     * Set the active model for inference.
     * Model must be downloaded or be the rule-based model.
     */
    suspend fun setActiveModel(modelId: String): Result<Unit> {
        Timber.d("Setting active model to: $modelId")
        
        val definition = getModelDefinition(modelId)
            ?: return Result.failure(IllegalArgumentException("Unknown model: $modelId"))
        
        // Verify model is available
        if (!isModelDownloaded(modelId)) {
            return Result.failure(IllegalStateException("Model not downloaded: $modelId"))
        }
        
        // Update preference and state
        preferences.edit { prefs ->
            prefs[KEY_ACTIVE_MODEL] = modelId
        }
        _activeModelId.value = modelId
        
        Timber.i("Active model set to: ${definition.displayName}")
        return Result.success(Unit)
    }
    
    /**
     * Delete a downloaded model.
     * Cannot delete the currently active model.
     */
    suspend fun deleteModel(modelId: String): Result<Unit> {
        Timber.d("Deleting model: $modelId")
        
        // Cannot delete rule-based
        if (modelId == PredefinedModels.RULE_BASED.id) {
            return Result.failure(IllegalArgumentException("Cannot delete rule-based model"))
        }
        
        // Cannot delete active model
        if (modelId == _activeModelId.value) {
            return Result.failure(IllegalStateException("Cannot delete active model. Switch to another model first."))
        }
        
        return try {
            _isOperationInProgress.value = true
            
            val file = getModelFile(modelId)
            if (file.exists()) {
                file.delete()
                Timber.d("Deleted model file: ${file.absolutePath}")
            }
            
            // Update preferences
            val newDownloaded = _downloadedModelIds.value - modelId
            preferences.edit { prefs ->
                prefs[KEY_DOWNLOADED_MODELS] = newDownloaded
            }
            _downloadedModelIds.value = newDownloaded
            
            refreshAvailableModels()
            
            Timber.i("Model deleted: $modelId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete model: $modelId")
            _lastError.value = "Failed to delete model: ${e.message}"
            Result.failure(e)
        } finally {
            _isOperationInProgress.value = false
        }
    }
    
    /**
     * Get models suitable for a specific device tier.
     * 
     * @param ramGb Available RAM in GB
     * @return List of models that can run on this device
     */
    fun getModelsForDeviceTier(ramGb: Int): List<ModelDefinition> {
        return modelCatalog.filter { it.minRamGb <= ramGb }
    }
    
    /**
     * Get the recommended model for a device.
     * 
     * @param ramGb Available RAM in GB
     * @return Best model for this device's capabilities
     */
    fun getRecommendedModel(ramGb: Int): ModelDefinition {
        // Filter to models that fit in RAM
        val suitable = getModelsForDeviceTier(ramGb)
            .filter { isModelDownloaded(it.id) }
        
        // Prefer Phi-3 if available and fits
        suitable.find { it.id == PredefinedModels.PHI3_MINI_4K.id }?.let { return it }
        
        // Otherwise return any LLM model
        suitable.find { it.id != PredefinedModels.RULE_BASED.id }?.let { return it }
        
        // Fallback to rule-based
        return PredefinedModels.RULE_BASED
    }
    
    /**
     * Get storage space used by downloaded models.
     */
    fun getStorageUsed(): Long {
        return getModelsDirectory().listFiles()?.sumOf { it.length() } ?: 0L
    }
    
    /**
     * Get storage space required for a model.
     */
    fun getStorageRequired(modelId: String): Long {
        return getModelDefinition(modelId)?.sizeBytes ?: 0L
    }
    
    /**
     * Register a model as downloaded (called by ModelDownloadManager).
     */
    suspend fun registerDownloadedModel(modelId: String) {
        val newDownloaded = _downloadedModelIds.value + modelId
        preferences.edit { prefs ->
            prefs[KEY_DOWNLOADED_MODELS] = newDownloaded
        }
        _downloadedModelIds.value = newDownloaded
        refreshAvailableModels()
        
        Timber.i("Registered downloaded model: $modelId")
    }
    
    /**
     * Get the models directory.
     */
    fun getModelsDirectory(): File {
        return File(context.filesDir, MODELS_DIR).also {
            if (!it.exists()) it.mkdirs()
        }
    }
    
    /**
     * Get the file for a specific model.
     */
    private fun getModelFile(modelId: String): File {
        val definition = getModelDefinition(modelId)
            ?: throw IllegalArgumentException("Unknown model: $modelId")
        return File(getModelsDirectory(), definition.fileName)
    }
    
    /**
     * Scan the models directory to verify downloaded models.
     */
    private suspend fun scanDownloadedModels() {
        Timber.d("Scanning models directory")
        
        val modelsDir = getModelsDirectory()
        val existingFiles = modelsDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
        
        // Find models that exist on disk
        val actuallyDownloaded = modelCatalog
            .filter { it.fileName.isNotEmpty() && existingFiles.contains(it.fileName) }
            .map { it.id }
            .toSet()
        
        // Add rule-based which is always "downloaded"
        val allDownloaded = actuallyDownloaded + PredefinedModels.RULE_BASED.id
        
        // Update if different from saved preferences
        if (allDownloaded != _downloadedModelIds.value) {
            Timber.d("Updating downloaded models: $allDownloaded")
            preferences.edit { prefs ->
                prefs[KEY_DOWNLOADED_MODELS] = allDownloaded
            }
            _downloadedModelIds.value = allDownloaded
        }
    }
    
    /**
     * Refresh the available models list with current download status.
     */
    private fun refreshAvailableModels() {
        _availableModels.value = modelCatalog.map { definition ->
            ModelInfo(
                modelId = definition.id,
                displayName = definition.displayName,
                provider = "local",
                contextLength = definition.contextLength,
                capabilities = definition.capabilities,
                sizeBytes = definition.sizeBytes,
                version = null,
                description = definition.description,
                isDownloaded = isModelDownloaded(definition.id)
            )
        }
    }
    
    /**
     * Observe the active model changes.
     */
    fun observeActiveModel(): Flow<ModelDefinition?> {
        return _activeModelId.map { id ->
            id?.let { getModelDefinition(it) }
        }
    }
    
    /**
     * Clear all errors.
     */
    fun clearError() {
        _lastError.value = null
    }
}

package com.prio.core.aiprovider.provider

import com.prio.core.ai.model.AiContext
import com.prio.core.ai.model.AiRequest
import com.prio.core.ai.model.AiRequestType
import com.prio.core.ai.model.AiResponse
import com.prio.core.ai.model.AiResponseMetadata
import com.prio.core.ai.model.AiResult
import com.prio.core.ai.model.AiStreamChunk
import com.prio.core.ai.provider.AiCapabilities
import com.prio.core.ai.provider.AiCapability
import com.prio.core.ai.provider.AiProvider
import com.prio.core.ai.provider.ModelDefinition
import com.prio.core.ai.provider.ModelInfo
import com.prio.core.ai.provider.PredefinedModels
import com.prio.core.ai.provider.PromptTemplate
import com.prio.core.ai.registry.ModelRegistry
import com.prio.core.aiprovider.llm.EisenhowerPromptBuilder
import com.prio.core.aiprovider.llm.LlamaEngine
import com.prio.core.aiprovider.llm.PromptFormatter
import com.prio.core.common.model.EisenhowerQuadrant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device AI provider using llama.cpp for local LLM inference.
 * 
 * Task 2.2.6: Implement OnDeviceAiProvider
 * 
 * This provider:
 * - Uses llama.cpp via JNI for on-device inference
 * - Supports Phi-3-mini, Mistral 7B, Gemma 2B models
 * - Provides Eisenhower classification with 2-3s latency on Tier 1 devices
 * - Implements the AiProvider interface for pluggable architecture
 * 
 * Performance targets (from 0.2.2 benchmarks):
 * - Phi-3-mini: 2-3 seconds, ~40% accuracy (needs improvement)
 * - Mistral 7B: 45-60 seconds, 80% accuracy
 * - RAM usage: 3.5-5GB depending on model
 */
@Singleton
class OnDeviceAiProvider @Inject constructor(
    private val llamaEngine: LlamaEngine,
    private val modelRegistry: ModelRegistry
) : AiProvider {
    
    companion object {
        private const val TAG = "OnDeviceAiProvider"
        const val PROVIDER_ID = "on-device"
    }
    
    private val _isAvailable = MutableStateFlow(false)
    override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()
    
    override val providerId: String = PROVIDER_ID
    
    override val displayName: String = "On-Device AI"
    
    override val capabilities: Set<AiCapability> = setOf(
        AiCapability.CLASSIFICATION,
        AiCapability.EXTRACTION,
        AiCapability.GENERATION
    )
    
    private var currentModelDefinition: ModelDefinition? = null
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Initialize the provider.
     * Loads the active model from ModelRegistry if available.
     */
    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i("Initializing OnDeviceAiProvider")
        
        // Initialize llama engine
        llamaEngine.initialize().onFailure { error ->
            Timber.tag(TAG).e(error, "Failed to initialize LlamaEngine")
            return@withContext false
        }
        
        // Check if we have an active model
        val activeModelId = modelRegistry.activeModelId.value
        if (activeModelId != null) {
            val modelFile = modelRegistry.getModelFile(activeModelId)
            if (modelFile?.exists() == true) {
                currentModelDefinition = PredefinedModels.ALL.find { it.id == activeModelId }
                val result = llamaEngine.loadModel(modelFile.absolutePath)
                _isAvailable.value = result.success
                Timber.tag(TAG).i("Model loaded: ${result.success}")
                return@withContext result.success
            }
        }
        
        // No model loaded yet, but provider is ready for model loading
        _isAvailable.value = LlamaEngine.isNativeLibraryAvailable()
        Timber.tag(TAG).i("Provider initialized, awaiting model. Native library available: ${_isAvailable.value}")
        true
    }
    
    /**
     * Load a specific model.
     * 
     * @param modelId The ID of the model to load
     * @return true if model loaded successfully
     */
    suspend fun loadModel(modelId: String): Boolean = withContext(Dispatchers.IO) {
        val modelFile = modelRegistry.getModelFile(modelId)
        if (modelFile?.exists() != true) {
            Timber.tag(TAG).e("Model file not found for: $modelId")
            return@withContext false
        }
        
        currentModelDefinition = PredefinedModels.ALL.find { it.id == modelId }
        val result = llamaEngine.loadModel(modelFile.absolutePath)
        _isAvailable.value = result.success && !result.isStub
        
        if (result.success) {
            modelRegistry.setActiveModel(modelId)
        }
        
        result.success
    }
    
    /**
     * Execute an AI request.
     */
    override suspend fun complete(request: AiRequest): Result<AiResponse> = withContext(Dispatchers.IO) {
        if (!_isAvailable.value || !llamaEngine.isLoaded) {
            return@withContext Result.failure(
                IllegalStateException("OnDeviceAiProvider not available - model not loaded")
            )
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            val response = when (request.type) {
                AiRequestType.CLASSIFY_EISENHOWER -> classifyEisenhower(request)
                AiRequestType.PARSE_TASK -> parseTask(request)
                AiRequestType.GENERATE_BRIEFING -> generateBriefing(request)
                else -> {
                    // General generation for unsupported types
                    generalGenerate(request)
                }
            }
            
            val latency = System.currentTimeMillis() - startTime
            Timber.tag(TAG).d("Request ${request.type} completed in ${latency}ms")
            
            Result.success(
                response.copy(
                    metadata = response.metadata.copy(
                        provider = PROVIDER_ID,
                        model = currentModelDefinition?.id ?: "unknown",
                        latencyMs = latency
                    )
                )
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error processing request: ${request.type}")
            Result.failure(e)
        }
    }
    
    /**
     * Classify a task into an Eisenhower quadrant.
     */
    private suspend fun classifyEisenhower(request: AiRequest): AiResponse {
        val template = currentModelDefinition?.promptTemplate ?: PromptTemplate.PHI3
        val prompt = EisenhowerPromptBuilder.buildClassificationPrompt(request.input, template)
        
        val result = llamaEngine.generate(
            prompt = prompt,
            maxTokens = request.options.maxTokens,
            temperature = request.options.temperature,
            topP = request.options.topP
        )
        
        if (result.error != null) {
            return AiResponse(
                success = false,
                requestId = request.id,
                result = null,
                error = result.error,
                errorCode = "GENERATION_FAILED"
            )
        }
        
        return parseEisenhowerResponse(request.id, result.text, result.tokensGenerated)
    }
    
    /**
     * Parse the LLM response for Eisenhower classification.
     */
    private fun parseEisenhowerResponse(
        requestId: String, 
        text: String,
        tokensUsed: Int
    ): AiResponse {
        try {
            // Try to extract JSON from the response
            val jsonMatch = Regex("""\{[^}]+\}""").find(text)
            if (jsonMatch != null) {
                val jsonObj = json.decodeFromString<JsonObject>(jsonMatch.value)
                
                val quadrantStr = jsonObj["quadrant"]?.jsonPrimitive?.content?.uppercase() ?: "SCHEDULE"
                val confidence = jsonObj["confidence"]?.jsonPrimitive?.float ?: 0.6f
                val reasoning = jsonObj["reasoning"]?.jsonPrimitive?.content ?: "AI classification"
                
                val quadrant = when (quadrantStr) {
                    "DO" -> EisenhowerQuadrant.DO
                    "SCHEDULE" -> EisenhowerQuadrant.SCHEDULE
                    "DELEGATE" -> EisenhowerQuadrant.DELEGATE
                    "ELIMINATE" -> EisenhowerQuadrant.ELIMINATE
                    else -> EisenhowerQuadrant.SCHEDULE
                }
                
                val isUrgent = quadrant == EisenhowerQuadrant.DO || quadrant == EisenhowerQuadrant.DELEGATE
                val isImportant = quadrant == EisenhowerQuadrant.DO || quadrant == EisenhowerQuadrant.SCHEDULE
                
                return AiResponse(
                    success = true,
                    requestId = requestId,
                    result = AiResult.EisenhowerClassification(
                        quadrant = quadrant,
                        confidence = confidence,
                        explanation = reasoning,
                        isUrgent = isUrgent,
                        isImportant = isImportant
                    ),
                    rawText = text,
                    metadata = AiResponseMetadata(
                        tokensUsed = tokensUsed,
                        wasRuleBased = false,
                        confidenceScore = confidence
                    )
                )
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to parse Eisenhower response: $text")
        }
        
        // Fallback: try to detect quadrant from text
        val quadrant = detectQuadrantFromText(text)
        return AiResponse(
            success = true,
            requestId = requestId,
            result = AiResult.EisenhowerClassification(
                quadrant = quadrant,
                confidence = 0.5f,
                explanation = "Parsed from raw output",
                isUrgent = quadrant == EisenhowerQuadrant.DO || quadrant == EisenhowerQuadrant.DELEGATE,
                isImportant = quadrant == EisenhowerQuadrant.DO || quadrant == EisenhowerQuadrant.SCHEDULE
            ),
            rawText = text,
            metadata = AiResponseMetadata(
                tokensUsed = tokensUsed,
                wasRuleBased = false,
                confidenceScore = 0.5f
            )
        )
    }
    
    private fun detectQuadrantFromText(text: String): EisenhowerQuadrant {
        val upperText = text.uppercase()
        return when {
            upperText.contains("\"DO\"") || upperText.contains("QUADRANT: DO") -> EisenhowerQuadrant.DO
            upperText.contains("\"SCHEDULE\"") || upperText.contains("QUADRANT: SCHEDULE") -> EisenhowerQuadrant.SCHEDULE
            upperText.contains("\"DELEGATE\"") || upperText.contains("QUADRANT: DELEGATE") -> EisenhowerQuadrant.DELEGATE
            upperText.contains("\"ELIMINATE\"") || upperText.contains("QUADRANT: ELIMINATE") -> EisenhowerQuadrant.ELIMINATE
            else -> EisenhowerQuadrant.SCHEDULE // Safe default
        }
    }
    
    /**
     * Parse a natural language task input.
     */
    private suspend fun parseTask(request: AiRequest): AiResponse {
        // Task parsing prompt
        val template = currentModelDefinition?.promptTemplate ?: PromptTemplate.PHI3
        val systemPrompt = """Extract task details from natural language input.
Output JSON: {"title": "...", "due_date": "YYYY-MM-DD or null", "due_time": "HH:MM or null", "priority": "high/medium/low or null", "tags": []}"""
        
        val prompt = PromptFormatter.format(template, systemPrompt, "Parse: \"${request.input}\"")
        
        val result = llamaEngine.generate(
            prompt = prompt,
            maxTokens = request.options.maxTokens,
            temperature = request.options.temperature
        )
        
        if (result.error != null) {
            return AiResponse(
                success = false,
                requestId = request.id,
                result = null,
                error = result.error,
                errorCode = "GENERATION_FAILED"
            )
        }
        
        // Parse the result - simplified for now
        return AiResponse(
            success = true,
            requestId = request.id,
            result = AiResult.ParsedTask(
                title = request.input,
                confidence = 0.7f
            ),
            rawText = result.text,
            metadata = AiResponseMetadata(
                tokensUsed = result.tokensGenerated,
                wasRuleBased = false
            )
        )
    }
    
    /**
     * Generate a daily briefing.
     */
    private suspend fun generateBriefing(request: AiRequest): AiResponse {
        val template = currentModelDefinition?.promptTemplate ?: PromptTemplate.PHI3
        val systemPrompt = """Generate a brief, motivational daily briefing summary.
Be concise and actionable. Focus on priorities."""
        
        val prompt = PromptFormatter.format(template, systemPrompt, request.input)
        
        val result = llamaEngine.generate(
            prompt = prompt,
            maxTokens = request.options.maxTokens,
            temperature = 0.7f // Slightly higher for creative briefings
        )
        
        if (result.error != null) {
            return AiResponse(
                success = false,
                requestId = request.id,
                result = null,
                error = result.error
            )
        }
        
        return AiResponse(
            success = true,
            requestId = request.id,
            result = AiResult.BriefingContent(
                greeting = "Good morning!",
                summary = result.text,
                topPriorities = emptyList(),
                insights = emptyList()
            ),
            rawText = result.text,
            metadata = AiResponseMetadata(
                tokensUsed = result.tokensGenerated
            )
        )
    }
    
    /**
     * General text generation for other request types.
     */
    private suspend fun generalGenerate(request: AiRequest): AiResponse {
        val template = currentModelDefinition?.promptTemplate ?: PromptTemplate.PHI3
        val prompt = PromptFormatter.format(template, request.systemPrompt, request.input)
        
        val result = llamaEngine.generate(
            prompt = prompt,
            maxTokens = request.options.maxTokens,
            temperature = request.options.temperature
        )
        
        return AiResponse(
            success = result.error == null,
            requestId = request.id,
            result = if (result.error == null) {
                AiResult.ChatResponse(message = result.text)
            } else null,
            rawText = result.text,
            error = result.error,
            metadata = AiResponseMetadata(
                tokensUsed = result.tokensGenerated
            )
        )
    }
    
    /**
     * Stream response (not fully implemented for on-device).
     * For now, returns single chunk with full response.
     */
    override suspend fun stream(request: AiRequest): Flow<AiStreamChunk> = flow {
        val result = complete(request)
        result.onSuccess { response ->
            emit(AiStreamChunk(
                text = response.rawText ?: "",
                isComplete = true,
                tokenIndex = response.metadata.tokensUsed
            ))
        }.onFailure { error ->
            emit(AiStreamChunk(
                text = "",
                isComplete = true
            ))
        }
    }
    
    override suspend fun getModelInfo(): ModelInfo {
        val modelDef = currentModelDefinition ?: return ModelInfo(
            modelId = "none",
            displayName = "No model loaded",
            provider = PROVIDER_ID,
            contextLength = 0,
            capabilities = emptySet()
        )
        
        return ModelInfo(
            modelId = modelDef.id,
            displayName = modelDef.displayName,
            provider = PROVIDER_ID,
            contextLength = modelDef.contextLength,
            capabilities = modelDef.capabilities,
            sizeBytes = modelDef.sizeBytes,
            description = modelDef.description,
            isDownloaded = true
        )
    }
    
    override suspend fun estimateCost(request: AiRequest): Float? = null // On-device is free
    
    override suspend fun release() {
        llamaEngine.unload()
        _isAvailable.value = false
        currentModelDefinition = null
        Timber.tag(TAG).i("OnDeviceAiProvider released")
    }
}

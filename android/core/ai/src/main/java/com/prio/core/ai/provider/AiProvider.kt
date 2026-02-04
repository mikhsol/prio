package com.prio.core.ai.provider

import com.prio.core.ai.model.AiRequest
import com.prio.core.ai.model.AiResponse
import com.prio.core.ai.model.AiStreamChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * AI Provider interface for pluggable AI architecture.
 * 
 * Based on ARCHITECTURE.md AI Provider Architecture and ACTION_PLAN.md Milestone 2.2.
 * Enables easy model replacement and cloud fallback without code changes.
 * 
 * Design Goals (from ARCHITECTURE.md):
 * 1. Easy Model Replacement - Swap LLM models without code changes
 * 2. Hybrid AI Routing - Seamlessly switch between on-device and cloud providers
 * 3. Backend Proxy Ready - Connect to cloud LLMs via backend gateway when needed
 * 4. Graceful Fallback - Rule-based fallback when AI unavailable
 * 5. Provider Agnostic - Same interface works for all AI backends
 * 
 * Performance Budgets (from 0.2.2 benchmarks):
 * - Rule-based classification: <50ms (verified)
 * - LLM classification (Phi-3): 2-3 seconds (verified on Pixel 9a)
 * - Model loading: 1.5s Phi-3, 33-45s Mistral
 * - RAM budget: <4GB (Phi-3: 3.5GB)
 */
interface AiProvider {
    
    /**
     * Unique identifier for this provider.
     * Examples: "on-device", "cloud-gateway", "rule-based"
     */
    val providerId: String
    
    /**
     * Display name for user-facing settings.
     * Examples: "On-Device AI", "Cloud AI (via Prio)", "Basic (Offline)"
     */
    val displayName: String
    
    /**
     * Set of capabilities this provider supports.
     * Used for routing decisions and feature availability.
     */
    val capabilities: Set<AiCapability>
    
    /**
     * Observable availability state.
     * Emits true when provider is ready to accept requests (model loaded, API reachable, etc.).
     */
    val isAvailable: StateFlow<Boolean>
    
    /**
     * Execute an AI request synchronously (blocks until complete).
     * 
     * @param request The AI request containing prompt, parameters, and metadata
     * @return Result wrapping AiResponse on success or error details on failure
     */
    suspend fun complete(request: AiRequest): Result<AiResponse>
    
    /**
     * Execute an AI request with streaming response.
     * Yields chunks as they are generated - useful for real-time UI updates.
     * 
     * @param request The AI request
     * @return Flow of stream chunks, each containing partial text
     */
    suspend fun stream(request: AiRequest): Flow<AiStreamChunk>
    
    /**
     * Get current model information.
     * 
     * @return ModelInfo describing the active model
     */
    suspend fun getModelInfo(): ModelInfo
    
    /**
     * Estimate cost before making request (cloud providers only).
     * 
     * @param request The request to estimate cost for
     * @return Estimated cost in USD, null for on-device providers
     */
    suspend fun estimateCost(request: AiRequest): Float? = null
    
    /**
     * Initialize the provider (load model, connect to API, etc.).
     * Should be called before first use.
     * 
     * @return true if initialization succeeded
     */
    suspend fun initialize(): Boolean
    
    /**
     * Release resources when no longer needed.
     * Should be called when provider is being destroyed.
     */
    suspend fun release()
}

/**
 * AI capabilities that providers can support.
 * Used for routing and feature gating.
 */
enum class AiCapability {
    /** Can classify text into categories (Eisenhower, priority, etc.) */
    CLASSIFICATION,
    
    /** Can extract structured data from natural language */
    EXTRACTION,
    
    /** Can generate text (briefings, suggestions, etc.) */
    GENERATION,
    
    /** Supports streaming responses for real-time UI */
    STREAMING,
    
    /** Supports context length >4K tokens */
    LONG_CONTEXT,
    
    /** Supports tool/function calling */
    FUNCTION_CALLING
}

/**
 * Provider-specific capabilities configuration.
 * More detailed than AiCapability enum for feature decisions.
 */
data class AiCapabilities(
    val supportsEisenhowerClassification: Boolean = true,
    val supportsTaskParsing: Boolean = true,
    val supportsGoalSuggestion: Boolean = true,
    val supportsBriefingGeneration: Boolean = true,
    val supportsActionItemExtraction: Boolean = true,
    val supportsGeneralChat: Boolean = false,
    val supportsStreaming: Boolean = false,
    val isOnDevice: Boolean = true,
    val requiresNetwork: Boolean = false,
    val estimatedLatencyMs: Long = 50,
    val maxInputTokens: Int = 4096,
    val contextLength: Int = 4096
)

/**
 * Information about a specific AI model.
 */
data class ModelInfo(
    val modelId: String,
    val displayName: String,
    val provider: String,
    val contextLength: Int,
    val capabilities: Set<AiCapability>,
    val sizeBytes: Long? = null,
    val version: String? = null,
    val description: String = "",
    val isDownloaded: Boolean = false
)

/**
 * Model definition for the model catalog.
 * Contains all metadata needed to download, verify, and use a model.
 */
data class ModelDefinition(
    val id: String,
    val displayName: String,
    val fileName: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val sha256: String,
    val capabilities: Set<AiCapability>,
    val contextLength: Int,
    val recommendedForTasks: Set<AiTaskType> = emptySet(),
    val description: String = "",
    val minRamGb: Int = 4,
    val promptTemplate: PromptTemplate = PromptTemplate.CHATML
)

/**
 * Prompt template formats for different model families.
 * Each model family has its own chat template format.
 */
enum class PromptTemplate {
    /** Phi-3 format: <|user|>...<|end|><|assistant|> */
    PHI3,
    
    /** ChatML format: <|im_start|>user\n...<|im_end|> */
    CHATML,
    
    /** Mistral format: [INST]...[/INST] */
    MISTRAL,
    
    /** Llama 2 format: [INST]<<SYS>>...<</SYS>>...[/INST] */
    LLAMA2,
    
    /** Llama 3 format: <|begin_of_text|><|start_header_id|>... */
    LLAMA3,
    
    /** Gemma format: <start_of_turn>user\n... */
    GEMMA,
    
    /** Raw format - no special tokens */
    RAW
}

/**
 * AI task types for routing and prompt selection.
 */
enum class AiTaskType {
    CLASSIFY_EISENHOWER,
    PARSE_TASK,
    SUGGEST_SMART_GOAL,
    GENERATE_BRIEFING,
    EXTRACT_ACTION_ITEMS,
    SUMMARIZE,
    GENERAL_CHAT
}

/**
 * Predefined models available in the app.
 * Based on benchmarks from Milestone 0.2.
 */
object PredefinedModels {
    
    val PHI3_MINI_4K = ModelDefinition(
        id = "phi-3-mini-4k-instruct-q4",
        displayName = "Phi-3 Mini (Recommended)",
        fileName = "Phi-3-mini-4k-instruct-q4.gguf",
        sizeBytes = 2_300_000_000L,
        downloadUrl = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf",
        sha256 = "e7c76c1dc7e1c3c3e3a3e3b3f3d3c3a3e3b3f3d3", // Placeholder - to be verified
        capabilities = setOf(AiCapability.CLASSIFICATION, AiCapability.EXTRACTION, AiCapability.GENERATION),
        contextLength = 4096,
        recommendedForTasks = setOf(AiTaskType.CLASSIFY_EISENHOWER, AiTaskType.PARSE_TASK),
        description = "Fast on-device AI (2.3 GB). 2-3s inference on modern devices.",
        minRamGb = 4,
        promptTemplate = PromptTemplate.PHI3
    )
    
    val MISTRAL_7B = ModelDefinition(
        id = "mistral-7b-instruct-v0.2-q4",
        displayName = "Mistral 7B",
        fileName = "mistral-7b-instruct-v0.2.Q4_K_M.gguf",
        sizeBytes = 4_100_000_000L,
        downloadUrl = "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf",
        sha256 = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4", // Placeholder - to be verified
        capabilities = setOf(AiCapability.CLASSIFICATION, AiCapability.EXTRACTION, AiCapability.GENERATION, AiCapability.LONG_CONTEXT),
        contextLength = 8192,
        recommendedForTasks = setOf(AiTaskType.CLASSIFY_EISENHOWER, AiTaskType.GENERATE_BRIEFING),
        description = "Higher accuracy (4.1 GB). 80% Eisenhower accuracy but 45-60s inference.",
        minRamGb = 6,
        promptTemplate = PromptTemplate.MISTRAL
    )
    
    val GEMMA_2B = ModelDefinition(
        id = "gemma-2-2b-it-q4",
        displayName = "Gemma 2 2B (Smaller)",
        fileName = "gemma-2-2b-it-q4_k_m.gguf",
        sizeBytes = 1_700_000_000L,
        downloadUrl = "https://huggingface.co/google/gemma-2-2b-it-gguf/resolve/main/gemma-2-2b-it-q4_k_m.gguf",
        sha256 = "b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5", // Placeholder - to be verified
        capabilities = setOf(AiCapability.CLASSIFICATION, AiCapability.EXTRACTION),
        contextLength = 8192,
        recommendedForTasks = setOf(AiTaskType.CLASSIFY_EISENHOWER),
        description = "Smaller model (1.7 GB). Good for devices with less RAM.",
        minRamGb = 3,
        promptTemplate = PromptTemplate.GEMMA
    )
    
    val RULE_BASED = ModelDefinition(
        id = "rule-based",
        displayName = "Fast Mode (Offline)",
        fileName = "",
        sizeBytes = 0,
        downloadUrl = "",
        sha256 = "",
        capabilities = setOf(AiCapability.CLASSIFICATION, AiCapability.EXTRACTION),
        contextLength = Int.MAX_VALUE,
        recommendedForTasks = setOf(AiTaskType.CLASSIFY_EISENHOWER, AiTaskType.PARSE_TASK),
        description = "Instant response, no download required. 75% accuracy.",
        minRamGb = 0,
        promptTemplate = PromptTemplate.RAW
    )
    
    /** All available models in the catalog */
    val ALL = listOf(PHI3_MINI_4K, MISTRAL_7B, GEMMA_2B, RULE_BASED)
    
    /** Default model for new users */
    val DEFAULT = PHI3_MINI_4K
}

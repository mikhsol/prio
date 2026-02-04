package com.prio.core.ai.provider

import com.prio.core.ai.model.AiRequest
import com.prio.core.ai.model.AiResponse

/**
 * AI Provider interface for pluggable AI architecture.
 * 
 * Based on ARCHITECTURE.md AI Provider Architecture and ACTION_PLAN.md Milestone 2.2.
 * Enables easy model replacement and cloud fallback without code changes.
 */
interface AiProvider {
    
    /**
     * Unique identifier for this provider.
     */
    val providerId: String
    
    /**
     * Display name for user-facing settings.
     */
    val displayName: String
    
    /**
     * Whether this provider is currently available (model loaded, API reachable, etc.).
     */
    suspend fun isAvailable(): Boolean
    
    /**
     * Execute an AI request.
     */
    suspend fun execute(request: AiRequest): AiResponse
    
    /**
     * Get the capabilities of this provider.
     */
    fun getCapabilities(): AiCapabilities
    
    /**
     * Initialize the provider (load model, connect to API, etc.).
     */
    suspend fun initialize(): Boolean
    
    /**
     * Release resources when no longer needed.
     */
    suspend fun release()
}

/**
 * Capabilities of an AI provider.
 */
data class AiCapabilities(
    val supportsEisenhowerClassification: Boolean = true,
    val supportsTaskParsing: Boolean = true,
    val supportsGoalSuggestion: Boolean = true,
    val supportsBriefingGeneration: Boolean = true,
    val supportsActionItemExtraction: Boolean = true,
    val supportsGeneralChat: Boolean = false,
    val isOnDevice: Boolean = true,
    val requiresNetwork: Boolean = false,
    val estimatedLatencyMs: Long = 50,
    val maxInputTokens: Int = 4096
)

/**
 * Available AI models for the app.
 */
enum class AiModelInfo(
    val modelId: String,
    val displayName: String,
    val description: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val sha256: String
) {
    PHI3_MINI_4K(
        modelId = "phi-3-mini-4k-instruct-q4",
        displayName = "Phi-3 Mini",
        description = "Fast on-device AI (2.3 GB)",
        sizeBytes = 2_300_000_000L,
        downloadUrl = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf",
        sha256 = "" // To be filled with actual hash
    ),
    MISTRAL_7B(
        modelId = "mistral-7b-instruct-v02-q4",
        displayName = "Mistral 7B",
        description = "Higher accuracy (4.1 GB)",
        sizeBytes = 4_100_000_000L,
        downloadUrl = "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf",
        sha256 = "" // To be filled with actual hash
    ),
    RULE_BASED(
        modelId = "rule-based",
        displayName = "Fast Mode",
        description = "Instant, no download required",
        sizeBytes = 0,
        downloadUrl = "",
        sha256 = ""
    )
}

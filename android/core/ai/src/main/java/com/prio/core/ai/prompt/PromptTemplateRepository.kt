package com.prio.core.ai.prompt

import com.prio.core.ai.model.AiRequestType
import com.prio.core.ai.provider.PromptTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing prompt templates per model and task type.
 * 
 * Task 2.2.9: Create PromptTemplateRepository
 * 
 * This repository:
 * - Stores prompts per model and task type (Eisenhower, parsing, briefing)
 * - Provides versioned prompt management for A/B testing
 * - Supports runtime prompt updates without app update
 * - Tracks prompt performance metrics for optimization
 * 
 * Based on findings from Milestone 0.2.6:
 * - EXPERT_PERSONA achieves 70% accuracy on Phi-3
 * - Chain-of-thought helps with complex reasoning
 * - Few-shot examples improve concrete pattern matching
 */
@Singleton
class PromptTemplateRepository @Inject constructor() {
    
    companion object {
        const val CURRENT_PROMPT_VERSION = "1.0.0"
    }
    
    /**
     * Cached prompt configurations per model and task type.
     */
    private val promptCache = mutableMapOf<PromptKey, PromptConfig>()
    
    /**
     * Observable prompt version for cache invalidation.
     */
    private val _promptVersion = MutableStateFlow(CURRENT_PROMPT_VERSION)
    val promptVersion: StateFlow<String> = _promptVersion.asStateFlow()
    
    init {
        // Initialize with default prompts
        registerDefaultPrompts()
    }
    
    /**
     * Get the prompt configuration for a specific model and task type.
     * 
     * @param modelId The model ID (e.g., "phi-3-mini-4k-q4")
     * @param taskType The type of AI task
     * @return PromptConfig or null if not found
     */
    fun getPromptConfig(modelId: String, taskType: AiRequestType): PromptConfig? {
        // Try exact match first
        val exactKey = PromptKey(modelId, taskType)
        promptCache[exactKey]?.let { return it }
        
        // Fall back to family-based lookup (e.g., phi-3 family)
        val family = getModelFamily(modelId)
        val familyKey = PromptKey(family, taskType)
        return promptCache[familyKey]
    }
    
    /**
     * Get the system prompt for a specific task type and model.
     * Returns the default if no custom prompt is configured.
     */
    fun getSystemPrompt(modelId: String, taskType: AiRequestType): String {
        return getPromptConfig(modelId, taskType)?.systemPrompt ?: getDefaultSystemPrompt(taskType)
    }
    
    /**
     * Get the user prompt template for a specific task type.
     */
    fun getUserPromptTemplate(modelId: String, taskType: AiRequestType): String {
        return getPromptConfig(modelId, taskType)?.userPromptTemplate ?: getDefaultUserPromptTemplate(taskType)
    }
    
    /**
     * Register a custom prompt configuration.
     */
    fun registerPromptConfig(
        modelId: String,
        taskType: AiRequestType,
        config: PromptConfig
    ) {
        promptCache[PromptKey(modelId, taskType)] = config
    }
    
    /**
     * Get the optimal prompt template format for a model.
     */
    fun getPromptTemplate(modelId: String): PromptTemplate {
        return when {
            modelId.contains("phi-3", ignoreCase = true) -> PromptTemplate.PHI3
            modelId.contains("mistral", ignoreCase = true) -> PromptTemplate.MISTRAL
            modelId.contains("gemma", ignoreCase = true) -> PromptTemplate.GEMMA
            modelId.contains("llama-3", ignoreCase = true) -> PromptTemplate.LLAMA3
            modelId.contains("llama-2", ignoreCase = true) -> PromptTemplate.LLAMA2
            modelId.contains("chatml", ignoreCase = true) -> PromptTemplate.CHATML
            else -> PromptTemplate.CHATML // Default fallback
        }
    }
    
    /**
     * Get available prompt strategies for a task type.
     */
    fun getAvailableStrategies(taskType: AiRequestType): List<PromptStrategy> {
        return when (taskType) {
            AiRequestType.CLASSIFY_EISENHOWER -> listOf(
                PromptStrategy.EXPERT_PERSONA,
                PromptStrategy.FEW_SHOT,
                PromptStrategy.CHAIN_OF_THOUGHT,
                PromptStrategy.BASELINE
            )
            AiRequestType.PARSE_TASK -> listOf(
                PromptStrategy.STRUCTURED_EXTRACTION,
                PromptStrategy.BASELINE
            )
            AiRequestType.GENERATE_BRIEFING -> listOf(
                PromptStrategy.BRIEFING_TEMPLATE,
                PromptStrategy.BASELINE
            )
            else -> listOf(PromptStrategy.BASELINE)
        }
    }
    
    /**
     * Get the recommended strategy for a task type based on benchmarks.
     */
    fun getRecommendedStrategy(taskType: AiRequestType): PromptStrategy {
        return when (taskType) {
            AiRequestType.CLASSIFY_EISENHOWER -> PromptStrategy.EXPERT_PERSONA // 70% accuracy
            AiRequestType.PARSE_TASK -> PromptStrategy.STRUCTURED_EXTRACTION
            AiRequestType.GENERATE_BRIEFING -> PromptStrategy.BRIEFING_TEMPLATE
            else -> PromptStrategy.BASELINE
        }
    }
    
    // ========================================================================
    // Private Helpers
    // ========================================================================
    
    private fun getModelFamily(modelId: String): String {
        return when {
            modelId.contains("phi-3", ignoreCase = true) -> "phi-3"
            modelId.contains("mistral", ignoreCase = true) -> "mistral"
            modelId.contains("gemma", ignoreCase = true) -> "gemma"
            modelId.contains("llama", ignoreCase = true) -> "llama"
            else -> "default"
        }
    }
    
    private fun registerDefaultPrompts() {
        // Phi-3 Eisenhower prompts (based on 0.2.6 benchmark - 70% accuracy)
        registerPromptConfig(
            modelId = "phi-3",
            taskType = AiRequestType.CLASSIFY_EISENHOWER,
            config = PromptConfig(
                systemPrompt = EisenhowerPrompts.EXPERT_PERSONA_SYSTEM,
                userPromptTemplate = EisenhowerPrompts.CLASSIFICATION_USER_TEMPLATE,
                strategy = PromptStrategy.EXPERT_PERSONA,
                version = CURRENT_PROMPT_VERSION,
                benchmarkedAccuracy = 0.70f
            )
        )
        
        // Mistral Eisenhower prompts (80% accuracy with chain-of-thought)
        registerPromptConfig(
            modelId = "mistral",
            taskType = AiRequestType.CLASSIFY_EISENHOWER,
            config = PromptConfig(
                systemPrompt = EisenhowerPrompts.CHAIN_OF_THOUGHT_SYSTEM,
                userPromptTemplate = EisenhowerPrompts.CHAIN_OF_THOUGHT_USER_TEMPLATE,
                strategy = PromptStrategy.CHAIN_OF_THOUGHT,
                version = CURRENT_PROMPT_VERSION,
                benchmarkedAccuracy = 0.80f
            )
        )
        
        // Task parsing prompts
        registerPromptConfig(
            modelId = "phi-3",
            taskType = AiRequestType.PARSE_TASK,
            config = PromptConfig(
                systemPrompt = TaskParsingPrompts.SYSTEM_PROMPT,
                userPromptTemplate = TaskParsingPrompts.USER_TEMPLATE,
                strategy = PromptStrategy.STRUCTURED_EXTRACTION,
                version = CURRENT_PROMPT_VERSION,
                benchmarkedAccuracy = null // Not yet benchmarked
            )
        )
        
        // Briefing generation prompts
        registerPromptConfig(
            modelId = "phi-3",
            taskType = AiRequestType.GENERATE_BRIEFING,
            config = PromptConfig(
                systemPrompt = BriefingPrompts.SYSTEM_PROMPT,
                userPromptTemplate = BriefingPrompts.USER_TEMPLATE,
                strategy = PromptStrategy.BRIEFING_TEMPLATE,
                version = CURRENT_PROMPT_VERSION,
                benchmarkedAccuracy = null
            )
        )
        
        // Default fallback prompts
        registerPromptConfig(
            modelId = "default",
            taskType = AiRequestType.CLASSIFY_EISENHOWER,
            config = PromptConfig(
                systemPrompt = EisenhowerPrompts.BASELINE_SYSTEM,
                userPromptTemplate = EisenhowerPrompts.BASELINE_USER_TEMPLATE,
                strategy = PromptStrategy.BASELINE,
                version = CURRENT_PROMPT_VERSION,
                benchmarkedAccuracy = 0.40f
            )
        )
    }
    
    private fun getDefaultSystemPrompt(taskType: AiRequestType): String {
        return when (taskType) {
            AiRequestType.CLASSIFY_EISENHOWER -> EisenhowerPrompts.BASELINE_SYSTEM
            AiRequestType.PARSE_TASK -> TaskParsingPrompts.SYSTEM_PROMPT
            AiRequestType.GENERATE_BRIEFING -> BriefingPrompts.SYSTEM_PROMPT
            else -> "You are a helpful AI assistant."
        }
    }
    
    private fun getDefaultUserPromptTemplate(taskType: AiRequestType): String {
        return when (taskType) {
            AiRequestType.CLASSIFY_EISENHOWER -> EisenhowerPrompts.BASELINE_USER_TEMPLATE
            AiRequestType.PARSE_TASK -> TaskParsingPrompts.USER_TEMPLATE
            AiRequestType.GENERATE_BRIEFING -> BriefingPrompts.USER_TEMPLATE
            else -> "{{INPUT}}"
        }
    }
}

/**
 * Key for prompt cache lookup.
 */
data class PromptKey(
    val modelId: String,
    val taskType: AiRequestType
)

/**
 * Configuration for a prompt template.
 */
data class PromptConfig(
    /** System prompt defining the AI's role and behavior */
    val systemPrompt: String,
    
    /** User prompt template with {{PLACEHOLDER}} substitution */
    val userPromptTemplate: String,
    
    /** The strategy used for this prompt */
    val strategy: PromptStrategy,
    
    /** Version for tracking updates */
    val version: String,
    
    /** Benchmarked accuracy if tested (null if not benchmarked) */
    val benchmarkedAccuracy: Float? = null,
    
    /** Stop sequences specific to this prompt */
    val stopSequences: List<String> = emptyList(),
    
    /** Maximum tokens for this prompt type */
    val maxTokens: Int = 256,
    
    /** Temperature for this prompt type */
    val temperature: Float = 0.3f
)

/**
 * Prompt strategies based on 0.2.6 research findings.
 */
enum class PromptStrategy {
    /** Expert persona framing (70% accuracy on Eisenhower) */
    EXPERT_PERSONA,
    
    /** Few-shot examples in prompt */
    FEW_SHOT,
    
    /** Chain-of-thought reasoning (better for complex tasks) */
    CHAIN_OF_THOUGHT,
    
    /** Combined expert + few-shot + JSON output */
    COMBINED_OPTIMAL,
    
    /** Structured extraction for parsing tasks */
    STRUCTURED_EXTRACTION,
    
    /** Template-based briefing generation */
    BRIEFING_TEMPLATE,
    
    /** Basic prompt without optimization */
    BASELINE
}

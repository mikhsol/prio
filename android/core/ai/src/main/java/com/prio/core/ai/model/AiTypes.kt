package com.prio.core.ai.model

import com.prio.core.common.model.EisenhowerQuadrant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * AI request and response types for classification and parsing.
 * 
 * Based on ACTION_PLAN.md Milestone 2.2 and 0.2.5 LLM Recommendation.
 * These types are designed to be serializable for both on-device LLM
 * and future cloud API integration (matching backend API contract).
 * 
 * Performance Budgets (from 0.2.2 benchmarks):
 * - Rule-based classification: <50ms
 * - LLM classification (Phi-3): 2-3 seconds
 * - RAM budget: <4GB
 */

/**
 * Request type for AI operations.
 * Maps to backend /api/v1/ai/ endpoints.
 */
@Serializable
enum class AiRequestType {
    @SerialName("classify_eisenhower")
    CLASSIFY_EISENHOWER,
    
    @SerialName("parse_task")
    PARSE_TASK,
    
    @SerialName("suggest_smart_goal")
    SUGGEST_SMART_GOAL,
    
    @SerialName("generate_briefing")
    GENERATE_BRIEFING,
    
    @SerialName("extract_action_items")
    EXTRACT_ACTION_ITEMS,
    
    @SerialName("summarize")
    SUMMARIZE,
    
    @SerialName("general_chat")
    GENERAL_CHAT
}

/**
 * Unified AI request format used across all providers.
 * Designed to be serializable for both local and network use.
 * Matches backend API contract at /api/v1/ai/complete
 */
@Serializable
data class AiRequest(
    /** Unique request ID for tracking and correlation */
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),
    
    /** The type of AI operation to perform */
    @SerialName("type")
    val type: AiRequestType,
    
    /** The input text/prompt for the AI */
    @SerialName("input")
    val input: String,
    
    /** Optional system prompt for custom behavior */
    @SerialName("system_prompt")
    val systemPrompt: String? = null,
    
    /** Additional context to improve accuracy */
    @SerialName("context")
    val context: AiContext? = null,
    
    /** Generation parameters */
    @SerialName("options")
    val options: AiRequestOptions = AiRequestOptions(),
    
    /** Request metadata for routing and tracking */
    @SerialName("metadata")
    val metadata: RequestMetadata = RequestMetadata()
)

/**
 * Context for AI request (helps improve classification accuracy).
 * Provides additional signals for Eisenhower classification.
 */
@Serializable
data class AiContext(
    /** User's existing goals for relevance detection */
    @SerialName("existing_goals")
    val existingGoals: List<String> = emptyList(),
    
    /** Recent tasks for pattern matching */
    @SerialName("recent_tasks")
    val recentTasks: List<String> = emptyList(),
    
    /** Current time in ISO-8601 format */
    @SerialName("current_time")
    val currentTime: String? = null,
    
    /** User's timezone (IANA format) */
    @SerialName("user_timezone")
    val userTimezone: String? = null,
    
    /** Previous quadrant if reclassifying */
    @SerialName("previous_quadrant")
    val previousQuadrant: EisenhowerQuadrant? = null,
    
    /** Custom key-value context for future extensions */
    @SerialName("custom")
    val custom: Map<String, String> = emptyMap()
)

/**
 * Options for AI request generation.
 */
@Serializable
data class AiRequestOptions(
    /** Maximum tokens to generate */
    @SerialName("max_tokens")
    val maxTokens: Int = 256,
    
    /** Temperature for randomness (0.0-2.0) */
    @SerialName("temperature")
    val temperature: Float = 0.3f,
    
    /** Top-p nucleus sampling */
    @SerialName("top_p")
    val topP: Float = 0.9f,
    
    /** Whether to use LLM (false = rule-based only) */
    @SerialName("use_llm")
    val useLlm: Boolean = true,
    
    /** Whether to fallback to rule-based on LLM failure */
    @SerialName("fallback_to_rule_based")
    val fallbackToRuleBased: Boolean = true,
    
    /** Stop sequences for generation */
    @SerialName("stop_sequences")
    val stopSequences: List<String> = emptyList(),
    
    /** Minimum confidence threshold for classification */
    @SerialName("min_confidence")
    val minConfidence: Float = 0.7f
)

/**
 * AI response from classification or parsing.
 * Matches backend API response at /api/v1/ai/complete
 */
@Serializable
data class AiResponse(
    /** Whether the request succeeded */
    @SerialName("success")
    val success: Boolean,
    
    /** The request ID for correlation */
    @SerialName("request_id")
    val requestId: String = "",
    
    /** The result of the AI operation */
    @SerialName("result")
    val result: AiResult?,
    
    /** Raw text response (for debugging) */
    @SerialName("raw_text")
    val rawText: String? = null,
    
    /** Error message if success is false */
    @SerialName("error")
    val error: String? = null,
    
    /** Error code for programmatic handling */
    @SerialName("error_code")
    val errorCode: String? = null,
    
    /** Response metadata for analytics */
    @SerialName("metadata")
    val metadata: AiResponseMetadata = AiResponseMetadata()
)

/**
 * Result type union for different AI operations.
 * Uses sealed interface for type-safe polymorphism with JSON serialization.
 */
@Serializable
sealed interface AiResult {
    
    /**
     * Result of Eisenhower Matrix classification.
     * Primary AI feature - classifies tasks into DO/SCHEDULE/DELEGATE/ELIMINATE.
     * 
     * Accuracy targets (from 0.2.3):
     * - Rule-based: 75% overall
     * - LLM (Phi-3): 40-50% (needs improvement)
     * - LLM (Mistral 7B): 80% (but too slow)
     */
    @Serializable
    @SerialName("eisenhower_classification")
    data class EisenhowerClassification(
        @SerialName("quadrant")
        val quadrant: EisenhowerQuadrant,
        
        @SerialName("confidence")
        val confidence: Float,
        
        @SerialName("explanation")
        val explanation: String,
        
        @SerialName("is_urgent")
        val isUrgent: Boolean,
        
        @SerialName("is_important")
        val isImportant: Boolean,
        
        /** Urgency signals detected in input */
        @SerialName("urgency_signals")
        val urgencySignals: List<String> = emptyList(),
        
        /** Importance signals detected in input */
        @SerialName("importance_signals")
        val importanceSignals: List<String> = emptyList()
    ) : AiResult
    
    /**
     * Result of natural language task parsing.
     * Extracts structured data from free-form input.
     */
    @Serializable
    @SerialName("parsed_task")
    data class ParsedTask(
        @SerialName("title")
        val title: String,
        
        @SerialName("due_date")
        val dueDate: String? = null,
        
        @SerialName("due_time")
        val dueTime: String? = null,
        
        @SerialName("priority")
        val priority: String? = null,
        
        @SerialName("suggested_quadrant")
        val suggestedQuadrant: EisenhowerQuadrant? = null,
        
        @SerialName("recurrence")
        val recurrence: String? = null,
        
        @SerialName("tags")
        val tags: List<String> = emptyList(),
        
        @SerialName("confidence")
        val confidence: Float = 0f
    ) : AiResult
    
    /**
     * Result of SMART goal suggestion.
     * Helps refine vague goals into specific, measurable objectives.
     */
    @Serializable
    @SerialName("smart_goal_suggestion")
    data class SmartGoalSuggestion(
        @SerialName("refined_goal")
        val refinedGoal: String,
        
        @SerialName("specific")
        val specific: String,
        
        @SerialName("measurable")
        val measurable: String,
        
        @SerialName("achievable")
        val achievable: String,
        
        @SerialName("relevant")
        val relevant: String,
        
        @SerialName("time_bound")
        val timeBound: String,
        
        @SerialName("suggested_milestones")
        val suggestedMilestones: List<String> = emptyList()
    ) : AiResult
    
    /**
     * Result of daily briefing generation.
     * Creates personalized morning/evening summaries.
     */
    @Serializable
    @SerialName("briefing_content")
    data class BriefingContent(
        @SerialName("greeting")
        val greeting: String,
        
        @SerialName("summary")
        val summary: String,
        
        @SerialName("top_priorities")
        val topPriorities: List<String>,
        
        @SerialName("insights")
        val insights: List<String>,
        
        @SerialName("motivational_quote")
        val motivationalQuote: String? = null,
        
        @SerialName("briefing_type")
        val briefingType: String = "morning"
    ) : AiResult
    
    /**
     * Result of action item extraction from meeting notes.
     */
    @Serializable
    @SerialName("extracted_action_items")
    data class ExtractedActionItems(
        @SerialName("items")
        val items: List<ActionItemResult>
    ) : AiResult
    
    /**
     * A single extracted action item.
     */
    @Serializable
    data class ActionItemResult(
        @SerialName("description")
        val description: String,
        
        @SerialName("assignee")
        val assignee: String? = null,
        
        @SerialName("due_date")
        val dueDate: String? = null,
        
        @SerialName("priority")
        val priority: String? = null
    )
    
    /**
     * Result of text summarization.
     */
    @Serializable
    @SerialName("summary")
    data class SummaryResult(
        @SerialName("summary")
        val summary: String,
        
        @SerialName("key_points")
        val keyPoints: List<String> = emptyList(),
        
        @SerialName("word_count")
        val wordCount: Int = 0
    ) : AiResult
    
    /**
     * Result of general chat/conversation.
     */
    @Serializable
    @SerialName("chat_response")
    data class ChatResponse(
        @SerialName("message")
        val message: String
    ) : AiResult
    
    /**
     * Result of general text generation.
     * Used for briefings, summaries, and other generated content.
     */
    @Serializable
    @SerialName("generated_text")
    data class GeneratedText(
        @SerialName("text")
        val text: String,
        
        @SerialName("sections")
        val sections: List<String> = emptyList()
    ) : AiResult
}

/**
 * Metadata about AI response for analytics and debugging.
 */
@Serializable
data class AiResponseMetadata(
    @SerialName("provider")
    val provider: String = "unknown",
    
    @SerialName("model")
    val model: String = "unknown",
    
    @SerialName("latency_ms")
    val latencyMs: Long = 0,
    
    @SerialName("tokens_used")
    val tokensUsed: Int = 0,
    
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    
    @SerialName("was_rule_based")
    val wasRuleBased: Boolean = false,
    
    @SerialName("was_llm_fallback")
    val wasLlmFallback: Boolean = false,
    
    @SerialName("estimated_cost_usd")
    val estimatedCostUsd: Float? = null,
    
    @SerialName("confidence_score")
    val confidenceScore: Float = 0f
)

/**
 * Token usage information for cost tracking and analytics.
 */
@Serializable
data class TokenUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    
    @SerialName("completion_tokens")
    val completionTokens: Int,
    
    @SerialName("total_tokens")
    val totalTokens: Int,
    
    @SerialName("estimated_cost_usd")
    val estimatedCostUsd: Float? = null
)

/**
 * A chunk of streamed AI response.
 * Used for real-time UI updates during generation.
 */
@Serializable
data class AiStreamChunk(
    @SerialName("request_id")
    val requestId: String = "",
    
    @SerialName("text")
    val text: String,
    
    @SerialName("is_complete")
    val isComplete: Boolean = false,
    
    @SerialName("tokens_generated")
    val tokensGenerated: Int = 0,
    
    @SerialName("token_index")
    val tokenIndex: Int = 0,
    
    @SerialName("metadata")
    val metadata: AiStreamMetadata? = null
)

/**
 * Metadata for stream chunks.
 */
@Serializable
data class AiStreamMetadata(
    @SerialName("tokens_generated")
    val tokensGenerated: Int = 0,
    
    @SerialName("generation_time_ms")
    val generationTimeMs: Long = 0,
    
    @SerialName("tokens_per_second")
    val tokensPerSecond: Float = 0f
)

/**
 * Request metadata for tracking and routing.
 */
@Serializable
data class RequestMetadata(
    @SerialName("client_version")
    val clientVersion: String = "1.0.0",
    
    @SerialName("requested_at")
    val requestedAt: Long = System.currentTimeMillis(),
    
    @SerialName("preferred_provider")
    val preferredProvider: String? = null,
    
    @SerialName("allow_cloud_fallback")
    val allowCloudFallback: Boolean = true,
    
    @SerialName("device_tier")
    val deviceTier: Int = 1,
    
    @SerialName("request_id")
    val requestId: String = java.util.UUID.randomUUID().toString()
)

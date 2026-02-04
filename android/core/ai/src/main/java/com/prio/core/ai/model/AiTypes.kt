package com.prio.core.ai.model

import com.prio.core.common.model.EisenhowerQuadrant
import kotlinx.serialization.Serializable

/**
 * AI request types for classification and parsing.
 * 
 * Based on ACTION_PLAN.md Milestone 2.2 and 0.2.5 LLM Recommendation.
 * These types are designed to be serializable for both on-device LLM
 * and future cloud API integration.
 */

/**
 * Request type for AI operations.
 */
@Serializable
enum class AiRequestType {
    CLASSIFY_EISENHOWER,
    PARSE_TASK,
    SUGGEST_SMART_GOAL,
    GENERATE_BRIEFING,
    EXTRACT_ACTION_ITEMS,
    GENERAL_CHAT
}

/**
 * AI request for task classification or parsing.
 */
@Serializable
data class AiRequest(
    val type: AiRequestType,
    val input: String,
    val context: AiContext? = null,
    val options: AiRequestOptions = AiRequestOptions()
)

/**
 * Context for AI request (helps improve accuracy).
 */
@Serializable
data class AiContext(
    val existingGoals: List<String> = emptyList(),
    val recentTasks: List<String> = emptyList(),
    val currentTime: String? = null,
    val userTimezone: String? = null,
    val previousQuadrant: EisenhowerQuadrant? = null
)

/**
 * Options for AI request.
 */
@Serializable
data class AiRequestOptions(
    val maxTokens: Int = 256,
    val temperature: Float = 0.3f,
    val useLlm: Boolean = true,
    val fallbackToRuleBased: Boolean = true
)

/**
 * AI response from classification or parsing.
 */
@Serializable
data class AiResponse(
    val success: Boolean,
    val result: AiResult?,
    val error: String? = null,
    val metadata: AiResponseMetadata = AiResponseMetadata()
)

/**
 * Result type union for different AI operations.
 */
@Serializable
sealed interface AiResult {
    
    @Serializable
    data class EisenhowerClassification(
        val quadrant: EisenhowerQuadrant,
        val confidence: Float,
        val explanation: String,
        val isUrgent: Boolean,
        val isImportant: Boolean
    ) : AiResult
    
    @Serializable
    data class ParsedTask(
        val title: String,
        val dueDate: String? = null,
        val dueTime: String? = null,
        val priority: String? = null,
        val suggestedQuadrant: EisenhowerQuadrant? = null,
        val confidence: Float = 0f
    ) : AiResult
    
    @Serializable
    data class SmartGoalSuggestion(
        val refinedGoal: String,
        val specific: String,
        val measurable: String,
        val achievable: String,
        val relevant: String,
        val timeBound: String,
        val suggestedMilestones: List<String> = emptyList()
    ) : AiResult
    
    @Serializable
    data class BriefingContent(
        val greeting: String,
        val summary: String,
        val topPriorities: List<String>,
        val insights: List<String>,
        val motivationalQuote: String? = null
    ) : AiResult
    
    @Serializable
    data class ExtractedActionItems(
        val items: List<ActionItemResult>
    ) : AiResult
    
    @Serializable
    data class ActionItemResult(
        val description: String,
        val assignee: String? = null,
        val dueDate: String? = null
    )
    
    @Serializable
    data class ChatResponse(
        val message: String
    ) : AiResult
}

/**
 * Metadata about AI response for analytics.
 */
@Serializable
data class AiResponseMetadata(
    val provider: String = "unknown",
    val model: String = "unknown",
    val latencyMs: Long = 0,
    val tokensUsed: Int = 0,
    val wasRuleBased: Boolean = false,
    val wasLlmFallback: Boolean = false
)

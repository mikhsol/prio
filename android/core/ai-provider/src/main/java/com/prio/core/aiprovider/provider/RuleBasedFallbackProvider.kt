package com.prio.core.aiprovider.provider

import com.prio.core.ai.model.AiRequest
import com.prio.core.ai.model.AiRequestType
import com.prio.core.ai.model.AiResponse
import com.prio.core.ai.model.AiResponseMetadata
import com.prio.core.ai.model.AiResult
import com.prio.core.ai.model.AiStreamChunk
import com.prio.core.ai.provider.AiCapability
import com.prio.core.ai.provider.AiProvider
import com.prio.core.ai.provider.ModelInfo
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.domain.eisenhower.EisenhowerEngine
import com.prio.core.domain.eisenhower.EisenhowerClassificationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rule-based Eisenhower classifier as primary/fallback provider.
 * 
 * Task 2.2.7: Implement RuleBasedFallbackProvider
 * 
 * Based on 0.2.5 LLM Selection Recommendation:
 * - Rule-based is the PRIMARY classifier for MVP
 * - Achieves 75% accuracy (verified in 0.2.3)
 * - Latency: <50ms (verified)
 * - Always available, no model loading required
 * - Includes confidence scoring for LLM escalation
 * 
 * This provider **delegates** to [EisenhowerEngine] for the core classification
 * logic, wrapping it in the [AiProvider] interface for the pluggable AI architecture.
 * 
 * @see EisenhowerEngine for the classification implementation
 */
@Singleton
class RuleBasedFallbackProvider @Inject constructor(
    private val eisenhowerEngine: EisenhowerEngine
) : AiProvider {
    
    companion object {
        private const val TAG = "RuleBasedProvider"
        const val PROVIDER_ID = "rule-based"
        
        /**
         * Confidence threshold below which LLM escalation is recommended.
         * Based on 0.2.5: Rule-based handles clear cases, LLM for ambiguous ones.
         */
        const val LLM_ESCALATION_THRESHOLD = EisenhowerEngine.LLM_ESCALATION_THRESHOLD
    }
    
    private val _isAvailable = MutableStateFlow(true) // Always available
    override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()
    
    override val providerId: String = PROVIDER_ID
    override val displayName: String = "Fast Mode (Offline)"
    
    override val capabilities: Set<AiCapability> = setOf(
        AiCapability.CLASSIFICATION,
        AiCapability.EXTRACTION,
        AiCapability.GENERATION
    )
    
    // ========================================================================
    // AiProvider Implementation
    // ========================================================================
    
    override suspend fun initialize(): Boolean {
        Timber.tag(TAG).i("RuleBasedFallbackProvider initialized (always available)")
        return true
    }
    
    override suspend fun complete(request: AiRequest): Result<AiResponse> {
        val startTime = System.currentTimeMillis()
        
        return try {
            val response = when (request.type) {
                AiRequestType.CLASSIFY_EISENHOWER -> classifyEisenhower(request)
                AiRequestType.PARSE_TASK -> parseTask(request)
                AiRequestType.SUGGEST_SMART_GOAL -> suggestSmartGoal(request)
                else -> {
                    Result.failure(UnsupportedOperationException(
                        "Request type ${request.type} not supported by rule-based provider"
                    ))
                }
            }
            
            val latency = System.currentTimeMillis() - startTime
            Timber.tag(TAG).d("Request ${request.type} completed in ${latency}ms")
            
            response.map { resp ->
                resp.copy(
                    metadata = resp.metadata.copy(
                        provider = PROVIDER_ID,
                        model = "rule-based-v1",
                        latencyMs = latency,
                        wasRuleBased = true
                    )
                )
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error processing request")
            Result.failure(e)
        }
    }
    
    /**
     * Classify a task into Eisenhower quadrant using EisenhowerEngine.
     */
    private fun classifyEisenhower(request: AiRequest): Result<AiResponse> {
        // Delegate to EisenhowerEngine for actual classification
        val classification = eisenhowerEngine.classify(request.input)
        
        val result = AiResult.EisenhowerClassification(
            quadrant = classification.quadrant,
            confidence = classification.confidence,
            explanation = classification.explanation,
            isUrgent = classification.isUrgent,
            isImportant = classification.isImportant,
            urgencySignals = classification.urgencySignals,
            importanceSignals = classification.importanceSignals
        )
        
        return Result.success(AiResponse(
            success = true,
            requestId = request.id,
            result = result,
            metadata = AiResponseMetadata(
                wasRuleBased = true,
                confidenceScore = classification.confidence
            )
        ))
    }
    
    /**
     * Parse a natural language task input using patterns.
     */
    private fun parseTask(request: AiRequest): Result<AiResponse> {
        val taskText = request.input
        
        // Extract due date patterns
        val dueDateMatch = extractDueDate(taskText)
        val dueTimeMatch = extractDueTime(taskText)
        
        // Clean title by removing date/time expressions
        var title = taskText
        dueDateMatch?.let { title = title.replace(it.raw, "").trim() }
        dueTimeMatch?.let { title = title.replace(it.raw, "").trim() }
        
        // Remove common prefixes
        title = title
            .replace(Regex("(?i)^(remind me to|remind me|add task|create task|todo:?)\\s+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .replaceFirstChar { it.uppercase() }
        
        // Get classification for suggested quadrant using EisenhowerEngine
        val classification = eisenhowerEngine.classify(taskText)
        
        val result = AiResult.ParsedTask(
            title = title,
            dueDate = dueDateMatch?.date,
            dueTime = dueTimeMatch?.time,
            suggestedQuadrant = classification.quadrant,
            confidence = classification.confidence
        )
        
        return Result.success(AiResponse(
            success = true,
            requestId = request.id,
            result = result,
            metadata = AiResponseMetadata(wasRuleBased = true)
        ))
    }
    
    // ========================================================================
    // SMART Goal Suggestion (rule-based template)
    // ========================================================================

    /**
     * Generate a basic SMART goal template from user input using heuristics.
     *
     * This is a lightweight fallback for when no LLM provider is available.
     * It structures the user's raw goal text into SMART components using
     * simple string analysis — no AI model required.
     */
    private fun suggestSmartGoal(request: AiRequest): Result<AiResponse> {
        val input = request.input.trim()

        // Clean & capitalize the goal text
        val refinedGoal = input
            .replaceFirstChar { it.uppercase() }
            .let { if (it.endsWith('.')) it else it }

        // Build simple SMART scaffolding from the input
        val result = AiResult.SmartGoalSuggestion(
            refinedGoal = refinedGoal,
            specific = "Define exactly what \"$refinedGoal\" means to you and the key actions involved.",
            measurable = "Decide how you will track progress (e.g., frequency, quantity, milestones).",
            achievable = "Break this into smaller steps you can start this week.",
            relevant = "Consider why this goal matters to you right now.",
            timeBound = "Set a target date or timeframe to complete this goal.",
            suggestedMilestones = listOf(
                "Define success criteria",
                "Complete first step",
                "Reach halfway point",
                "Final review & completion"
            )
        )

        return Result.success(
            AiResponse(
                success = true,
                requestId = request.id,
                result = result,
                metadata = AiResponseMetadata(
                    wasRuleBased = true,
                    confidenceScore = 0.5f // Lower confidence — template, not AI-generated
                )
            )
        )
    }

    // ========================================================================
    // Date/Time Extraction Helpers
    // ========================================================================
    
    private data class DateMatch(val date: String, val raw: String)
    private data class TimeMatch(val time: String, val raw: String)
    
    private fun extractDueDate(text: String): DateMatch? {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        
        // Today
        if (Regex("(?i)\\btoday\\b").containsMatchIn(text)) {
            return DateMatch(today.format(formatter), "today")
        }
        
        // Tomorrow
        Regex("(?i)\\btomorrow\\b").find(text)?.let {
            return DateMatch(today.plusDays(1).format(formatter), it.value)
        }
        
        // Day of week
        val dayOfWeekPattern = Regex("(?i)\\b(on\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b")
        dayOfWeekPattern.find(text)?.let { match ->
            val dayName = match.groups[2]?.value?.lowercase() ?: return@let
            val targetDayOfWeek = java.time.DayOfWeek.values().find { 
                it.name.lowercase() == dayName 
            } ?: return@let
            var targetDate = today
            while (targetDate.dayOfWeek != targetDayOfWeek) {
                targetDate = targetDate.plusDays(1)
            }
            return DateMatch(targetDate.format(formatter), match.value)
        }
        
        // In X days
        Regex("(?i)\\bin\\s+(\\d+|one|two|three|four|five)\\s+days?\\b").find(text)?.let { match ->
            val daysStr = match.groups[1]?.value ?: return@let
            val days = when (daysStr.lowercase()) {
                "one" -> 1
                "two" -> 2
                "three" -> 3
                "four" -> 4
                "five" -> 5
                else -> daysStr.toIntOrNull() ?: return@let
            }
            return DateMatch(today.plusDays(days.toLong()).format(formatter), match.value)
        }
        
        // Next week
        if (Regex("(?i)\\bnext week\\b").containsMatchIn(text)) {
            return DateMatch(today.plusWeeks(1).format(formatter), "next week")
        }
        
        return null
    }
    
    private fun extractDueTime(text: String): TimeMatch? {
        // HH:MM or H:MM format
        Regex("\\b(\\d{1,2}):(\\d{2})\\s*(am|pm)?\\b", RegexOption.IGNORE_CASE).find(text)?.let { match ->
            var hour = match.groups[1]?.value?.toInt() ?: return@let
            val minute = match.groups[2]?.value ?: "00"
            val ampm = match.groups[3]?.value?.lowercase()
            
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            
            return TimeMatch(String.format("%02d:%s", hour, minute), match.value)
        }
        
        // "at X am/pm"
        Regex("\\bat\\s+(\\d{1,2})\\s*(am|pm)\\b", RegexOption.IGNORE_CASE).find(text)?.let { match ->
            var hour = match.groups[1]?.value?.toInt() ?: return@let
            val ampm = match.groups[2]?.value?.lowercase()
            
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            
            return TimeMatch(String.format("%02d:00", hour), match.value)
        }
        
        return null
    }
    
    // ========================================================================
    // Streaming & Model Info (minimal for rule-based)
    // ========================================================================
    
    override suspend fun stream(request: AiRequest): Flow<AiStreamChunk> = flow {
        val result = complete(request)
        result.onSuccess { response ->
            emit(AiStreamChunk(
                text = response.result.toString(),
                isComplete = true
            ))
        }
    }
    
    override suspend fun getModelInfo(): ModelInfo = ModelInfo(
        modelId = "rule-based-v1",
        displayName = "Fast Mode (Offline)",
        provider = PROVIDER_ID,
        contextLength = Int.MAX_VALUE,
        capabilities = capabilities,
        sizeBytes = 0,
        description = "Instant response, no download required. 75% accuracy.",
        isDownloaded = true
    )
    
    override suspend fun estimateCost(request: AiRequest): Float? = null
    
    override suspend fun release() {
        // Nothing to release - rule-based has no state
    }
}

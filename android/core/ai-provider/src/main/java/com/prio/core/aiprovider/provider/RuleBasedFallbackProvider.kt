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
 * The classifier uses:
 * - Regex patterns for urgency/importance detection
 * - Keyword dictionaries (50+ per category)
 * - Temporal pattern matching (deadlines, dates)
 * - Confidence scoring to trigger LLM fallback for edge cases
 */
@Singleton
class RuleBasedFallbackProvider @Inject constructor() : AiProvider {
    
    companion object {
        private const val TAG = "RuleBasedProvider"
        const val PROVIDER_ID = "rule-based"
        
        /**
         * Confidence threshold below which LLM escalation is recommended.
         * Based on 0.2.5: Rule-based handles clear cases, LLM for ambiguous ones.
         */
        const val LLM_ESCALATION_THRESHOLD = 0.65f
    }
    
    private val _isAvailable = MutableStateFlow(true) // Always available
    override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()
    
    override val providerId: String = PROVIDER_ID
    override val displayName: String = "Fast Mode (Offline)"
    
    override val capabilities: Set<AiCapability> = setOf(
        AiCapability.CLASSIFICATION,
        AiCapability.EXTRACTION
    )
    
    // ========================================================================
    // Pattern Definitions
    // ========================================================================
    
    /**
     * Urgency patterns - signals that a task needs immediate attention.
     * Based on 0.2.3 test cases and real-world task descriptions.
     */
    private val urgencyPatterns = listOf(
        // Explicit urgency words
        Regex("(?i)\\b(urgent|asap|immediately|emergency|critical|crisis)\\b"),
        
        // Today/tonight deadlines
        Regex("(?i)\\b(today|tonight|this morning|this afternoon|this evening)\\b"),
        Regex("(?i)\\b(before|by|until)\\s+(today|tonight|end of day|EOD|close of business|COB)\\b"),
        Regex("(?i)\\bend of (day|today)\\b"),
        
        // Overdue indicators
        Regex("(?i)\\b(overdue|late|behind|past due|missed)\\b"),
        
        // Time-specific urgency
        Regex("(?i)\\b(in|within)\\s+(\\d+|one|two|three)\\s*(hour|minute|min|hr)s?\\b"),
        Regex("(?i)\\bdue\\s+(today|now|immediately|asap)\\b"),
        Regex("(?i)\\b(deadline|due)\\s+(today|tomorrow)\\b"),
        
        // System/production urgency
        Regex("(?i)\\b(server|system|app|site|service)\\s*(down|crash|outage|issue|error|failure)\\b"),
        Regex("(?i)\\b(down|crash|outage).*(server|system|app|site|service)\\b"),
        Regex("(?i)\\b(production|prod)\\s*(issue|problem|bug|error)\\b"),
        
        // Client/customer waiting
        Regex("(?i)\\b(client|customer)\\s*(waiting|call|urgent|emergency)\\b"),
        Regex("(?i)\\bwaiting\\s+(on|for)\\s+(you|me|us|this)\\b"),
        
        // Meeting/call urgency
        Regex("(?i)\\bmeeting\\s+(in|starts?)\\s+(\\d+)\\s*(min|hour)\\b"),
        Regex("(?i)\\b(call|meeting)\\s+(today|now|shortly)\\b")
    )
    
    /**
     * Importance patterns - signals that a task has significant impact.
     */
    private val importancePatterns = listOf(
        // Explicit importance words
        Regex("(?i)\\b(important|crucial|vital|essential|key|strategic|significant)\\b"),
        
        // Career/professional impact
        Regex("(?i)\\b(career|promotion|performance|review|evaluation|raise)\\b"),
        Regex("(?i)\\b(job|interview|offer|resign|hire)\\b"),
        
        // Health/wellness
        Regex("(?i)\\b(health|doctor|medical|appointment|prescription|symptom|sick)\\b"),
        Regex("(?i)\\b(exercise|workout|gym|run|fitness)\\b"),
        
        // Family/relationships
        Regex("(?i)\\b(family|spouse|partner|child|parent|kid|wedding|anniversary)\\b"),
        
        // Financial significance
        Regex("(?i)\\b(tax|taxes|financial|budget|investment|mortgage|loan|debt)\\b"),
        Regex("(?i)\\b(contract|agreement|sign|legal|lawyer|attorney)\\b"),
        
        // Learning/development
        Regex("(?i)\\b(learn|study|course|certification|degree|skill|training)\\b"),
        Regex("(?i)\\b(read|book|research|understand)\\b"),
        
        // Goals and objectives
        Regex("(?i)\\b(goal|objective|target|milestone|OKR|quarter)\\b"),
        Regex("(?i)\\b(strategy|plan|planning|roadmap)\\b"),
        
        // Business impact
        Regex("(?i)\\b(client|customer|investor|board|stakeholder|executive)\\b"),
        Regex("(?i)\\b(project|deliverable|release|launch|presentation)\\b"),
        Regex("(?i)\\b(report|analysis|review|proposal)\\b"),
        Regex("(?i)\\b(decision|approve|sign-off)\\b"),
        
        // Task outcome words
        Regex("(?i)\\b(submit|complete|finish|deliver|ship)\\b"),
        Regex("(?i)\\b(prepare|create|build|develop)\\b")
    )
    
    /**
     * Delegation patterns - signals routine/administrative tasks.
     */
    private val delegationPatterns = listOf(
        // Explicit delegation
        Regex("(?i)\\b(delegate|assign|ask\\s+.+\\s+to|have\\s+.+\\s+do)\\b"),
        
        // Routine/recurring tasks
        Regex("(?i)\\b(routine|regular|recurring|standard|weekly|monthly|daily)\\b"),
        
        // Administrative tasks
        Regex("(?i)\\border\\s+(office\\s+)?supplies\\b"),
        Regex("(?i)\\boffice\\s+supplies\\b"),
        Regex("(?i)\\b(schedule|book|reserve)\\s+(meeting|room|flight|hotel)\\b"),
        
        // Status updates and reports
        Regex("(?i)\\bstatus\\s+(report|update|check)\\b"),
        Regex("(?i)\\bweekly\\s+.*report\\b"),
        Regex("(?i)\\b(compile|gather|collect)\\s+.*report\\b"),
        
        // Survey/feedback
        Regex("(?i)\\b(survey|poll|feedback|form|questionnaire)\\b"),
        
        // Data entry/updates
        Regex("(?i)\\b(update|enter|log|record)\\s+.*(data|spreadsheet|system|database)\\b"),
        
        // Anyone can do it
        Regex("(?i)\\b(anyone\\s+can|someone\\s+else|team\\s+can)\\b"),
        
        // Filing/organization
        Regex("(?i)\\b(file|organize|sort|archive)\\s+.*(documents|files|papers)\\b")
    )
    
    /**
     * Low priority patterns - signals time-wasters or optional activities.
     */
    private val lowPriorityPatterns = listOf(
        // Explicit low priority
        Regex("(?i)\\b(maybe|someday|eventually|when I have time|if I have time)\\b"),
        Regex("(?i)\\b(nice to have|would be good|could|might)\\b"),
        Regex("(?i)\\b(optional|not required|not urgent|low priority)\\b"),
        
        // Entertainment/leisure
        Regex("(?i)\\b(browse|scroll|watch|binge|stream)\\b"),
        Regex("(?i)\\b(social media|youtube|netflix|reddit|twitter|instagram|tiktok|facebook)\\b"),
        Regex("(?i)\\b(game|gaming|play|entertainment)\\b"),
        
        // Non-essential reorganization
        Regex("(?i)\\b(reorganize|rearrange|tidy)\\s+(bookshelf|desk|closet|room)\\b"),
        Regex("(?i)\\b(clean|organize)\\s+(files|photos|music|apps)\\b"),
        
        // Repeated/already done indicators
        Regex("(?i)\\b(third time|again|another|repeat)\\b"),
        
        // "Just" prefixed tasks (often trivial)
        Regex("(?i)^just\\s+(check|look|see|browse)\\b")
    )
    
    /**
     * Deadline patterns for extracting temporal urgency.
     */
    private val soonDeadlinePatterns = listOf(
        Regex("(?i)\\b(today|tonight|this morning|this afternoon)\\b"),
        Regex("(?i)\\btomorrow\\b"),
        Regex("(?i)\\bby\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b"),
        Regex("(?i)\\bin\\s+(1|one|2|two|3|three)\\s+days?\\b"),
        Regex("(?i)\\bdue\\s+(today|tomorrow|soon)\\b"),
        Regex("(?i)\\bthis\\s+week\\b"),
        Regex("(?i)\\bEOD\\b"),
        Regex("(?i)\\bend\\s+of\\s+(week|day)\\b")
    )
    
    /**
     * Future deadline patterns (less urgent).
     */
    private val futureDeadlinePatterns = listOf(
        Regex("(?i)\\bnext\\s+(week|month)\\b"),
        Regex("(?i)\\bin\\s+(\\d+|several)\\s+weeks?\\b"),
        Regex("(?i)\\bby\\s+(next|end of)\\s+(month|quarter|year)\\b"),
        Regex("(?i)\\b(Q[1-4]|quarter)\\b"),
        Regex("(?i)\\beventually\\b"),
        Regex("(?i)\\bno\\s+(deadline|due date|rush)\\b")
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
     * Classify a task into Eisenhower quadrant using rules.
     */
    private fun classifyEisenhower(request: AiRequest): Result<AiResponse> {
        val classification = classify(request.input, request.context)
        
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
     * Core classification logic using pattern matching.
     * 
     * @param taskText The task description to classify
     * @param context Optional context for improved accuracy
     * @return Detailed classification result with signals
     */
    fun classify(
        taskText: String,
        context: com.prio.core.ai.model.AiContext? = null
    ): EisenhowerClassificationResult {
        // Count pattern matches
        val urgencyMatches = urgencyPatterns.mapNotNull { p ->
            p.find(taskText)?.value
        }
        val importanceMatches = importancePatterns.mapNotNull { p ->
            p.find(taskText)?.value
        }
        val delegationMatches = delegationPatterns.mapNotNull { p ->
            p.find(taskText)?.value
        }
        val lowPriorityMatches = lowPriorityPatterns.mapNotNull { p ->
            p.find(taskText)?.value
        }
        
        val urgencyScore = urgencyMatches.size
        val importanceScore = importanceMatches.size
        val delegationScore = delegationMatches.size
        val lowPriorityScore = lowPriorityMatches.size
        
        // Check deadline proximity
        val hasSoonDeadline = soonDeadlinePatterns.any { it.containsMatchIn(taskText) }
        val hasFutureDeadline = futureDeadlinePatterns.any { it.containsMatchIn(taskText) }
        
        // Determine urgency and importance flags
        val isUrgent = urgencyScore >= 1 || hasSoonDeadline
        val isImportant = (importanceScore >= 1 && lowPriorityScore == 0) && delegationScore == 0
        
        // Classification logic with confidence scoring
        val (quadrant, baseConfidence, explanation) = when {
            // Clear low priority (ELIMINATE)
            lowPriorityScore >= 2 -> Triple(
                EisenhowerQuadrant.ELIMINATE,
                0.85f,
                "Multiple low-priority indicators detected: ${lowPriorityMatches.take(2).joinToString(", ")}"
            )
            
            // Single strong low-priority signal
            lowPriorityScore >= 1 && urgencyScore == 0 && importanceScore == 0 -> Triple(
                EisenhowerQuadrant.ELIMINATE,
                0.75f,
                "Low-priority activity: ${lowPriorityMatches.firstOrNull() ?: "no clear priority"}"
            )
            
            // Clear delegation (routine/administrative without importance)
            delegationScore >= 1 && !isImportant && !isUrgent -> Triple(
                EisenhowerQuadrant.DELEGATE,
                0.70f + minOf(delegationScore, 2) * 0.05f,
                "Routine/administrative task: ${delegationMatches.firstOrNull() ?: "suitable for delegation"}"
            )
            
            // DO: Urgent AND Important
            isUrgent && isImportant -> Triple(
                EisenhowerQuadrant.DO,
                0.75f + minOf(urgencyScore + importanceScore, 4) * 0.05f,
                buildExplanation("Urgent and important", urgencyMatches, importanceMatches)
            )
            
            // SCHEDULE: Important but NOT Urgent
            !isUrgent && isImportant -> Triple(
                EisenhowerQuadrant.SCHEDULE,
                0.70f + minOf(importanceScore, 3) * 0.05f,
                "Important but not time-sensitive: ${importanceMatches.firstOrNull() ?: "long-term value"}"
            )
            
            // DELEGATE: Urgent but NOT Important
            isUrgent && !isImportant -> Triple(
                EisenhowerQuadrant.DELEGATE,
                0.65f + minOf(urgencyScore, 2) * 0.05f,
                "Time-sensitive but could potentially be delegated: ${urgencyMatches.firstOrNull() ?: "deadline pressure"}"
            )
            
            // Delegation patterns present
            delegationScore >= 1 -> Triple(
                EisenhowerQuadrant.DELEGATE,
                0.65f,
                "Routine task suitable for delegation: ${delegationMatches.firstOrNull()}"
            )
            
            // Has future deadline - likely SCHEDULE
            hasFutureDeadline -> Triple(
                EisenhowerQuadrant.SCHEDULE,
                0.60f,
                "Future deadline detected - schedule for later"
            )
            
            // Default to SCHEDULE (safe default per 0.2.5)
            else -> Triple(
                EisenhowerQuadrant.SCHEDULE,
                0.55f,
                "No clear urgency indicators - scheduling for review"
            )
        }
        
        // Adjust confidence based on signal strength
        val finalConfidence = minOf(baseConfidence, 0.95f)
        
        return EisenhowerClassificationResult(
            quadrant = quadrant,
            confidence = finalConfidence,
            explanation = explanation,
            isUrgent = isUrgent,
            isImportant = isImportant,
            urgencySignals = urgencyMatches,
            importanceSignals = importanceMatches,
            shouldEscalateToLlm = finalConfidence < LLM_ESCALATION_THRESHOLD
        )
    }
    
    private fun buildExplanation(
        prefix: String,
        urgencySignals: List<String>,
        importanceSignals: List<String>
    ): String {
        val signals = mutableListOf<String>()
        urgencySignals.firstOrNull()?.let { signals.add("urgency: $it") }
        importanceSignals.firstOrNull()?.let { signals.add("importance: $it") }
        return if (signals.isNotEmpty()) {
            "$prefix (${signals.joinToString(", ")})"
        } else {
            prefix
        }
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
        
        // Get classification for suggested quadrant
        val classification = classify(taskText)
        
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

/**
 * Result of Eisenhower classification with detailed signals.
 */
data class EisenhowerClassificationResult(
    val quadrant: EisenhowerQuadrant,
    val confidence: Float,
    val explanation: String,
    val isUrgent: Boolean,
    val isImportant: Boolean,
    val urgencySignals: List<String>,
    val importanceSignals: List<String>,
    val shouldEscalateToLlm: Boolean
)

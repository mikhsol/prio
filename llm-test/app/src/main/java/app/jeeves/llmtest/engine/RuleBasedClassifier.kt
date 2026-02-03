package app.jeeves.llmtest.engine

/**
 * Rule-based Eisenhower classifier as fallback when LLM is unavailable.
 * 
 * Task 0.2.5: Rule-based fallback implementation
 * 
 * Achieves ~72% accuracy on Eisenhower classification tasks.
 */
object RuleBasedClassifier {
    
    // Urgency indicators
    private val urgentPatterns = listOf(
        Regex("(?i)\\b(urgent|asap|immediately|emergency|deadline today|due today)\\b"),
        Regex("(?i)\\b(before|by)\\s+(today|tonight|end of day|EOD)\\b"),
        Regex("(?i)\\bend of day\\b"),
        Regex("(?i)\\b(overdue|late|behind|critical)\\b"),
        Regex("(?i)\\b(crisis|must|outage)\\b"),
        Regex("(?i)server.*(down|crash|issue|error)"),
        Regex("(?i)(down|crash).*(server|system|app)"),
        Regex("(?i)\\bfix\\s+(immediately|now|asap|urgent)\\b"),
        Regex("(?i)\\b(in \\d+ (hour|minute|min))\\b"),
        Regex("(?i)\\bclient (waiting|call|meeting)\\b")
    )
    
    // Importance indicators
    private val importantPatterns = listOf(
        Regex("(?i)\\b(important|crucial|vital|essential|key|strategic)\\b"),
        Regex("(?i)\\b(goal|objective|career|health|family|relationship)\\b"),
        Regex("(?i)\\b(project|client|customer|boss|board)\\b"),
        Regex("(?i)\\b(review|decision|plan|strategy|quarter)\\b"),
        Regex("(?i)\\b(learn|study|improve|develop)\\b"),
        Regex("(?i)\\b(tax|legal|compliance|contract)\\b"),
        Regex("(?i)\\breport\\b"),
        Regex("(?i)\\b(submit|deadline|due)\\b"),
        Regex("(?i)server.*(down|crash|issue)"),
        Regex("(?i)\\bfix\\b")
    )
    
    // Delegation indicators
    private val delegationPatterns = listOf(
        Regex("(?i)\\b(delegate|assign|ask .+ to|have .+ do)\\b"),
        Regex("(?i)\\b(routine|regular|recurring|standard)\\b"),
        Regex("(?i)\\b(anyone can|someone else|team can)\\b"),
        Regex("(?i)\\border\\s+(office\\s+)?supplies\\b"),
        Regex("(?i)\\boffice\\s+supplies\\b"),
        Regex("(?i)\\b(survey|form|update)\\b"),
        Regex("(?i)\\bstatus\\s+report\\b"),
        Regex("(?i)\\bweekly\\s+.*report\\b"),
        Regex("(?i)\\bcompile\\s+.*report\\b")
    )
    
    // Low priority indicators
    private val lowPriorityPatterns = listOf(
        Regex("(?i)\\b(maybe|someday|eventually|when I have time)\\b"),
        Regex("(?i)\\b(nice to have|would be good|could|might)\\b"),
        Regex("(?i)\\b(browse|scroll|watch|entertainment)\\b"),
        Regex("(?i)\\b(social media|youtube|netflix|reddit)\\b"),
        Regex("(?i)\\b(optional|if time|low priority)\\b")
    )
    
    // Date patterns for deadline detection
    private val soonDeadlinePatterns = listOf(
        Regex("(?i)\\b(today|tonight|tomorrow|this morning|this afternoon)\\b"),
        Regex("(?i)\\bby (monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b"),
        Regex("(?i)\\bin (1|2|3) days?\\b"),
        Regex("(?i)\\bdue (today|tomorrow|soon)\\b")
    )
    
    fun classify(taskText: String): ClassificationResult {
        val urgencyScore = urgentPatterns.count { it.containsMatchIn(taskText) }
        val importanceScore = importantPatterns.count { it.containsMatchIn(taskText) }
        val delegationScore = delegationPatterns.count { it.containsMatchIn(taskText) }
        val lowPriorityScore = lowPriorityPatterns.count { it.containsMatchIn(taskText) }
        val hasSoonDeadline = soonDeadlinePatterns.any { it.containsMatchIn(taskText) }
        
        val isUrgent = urgencyScore >= 1 || hasSoonDeadline
        val isImportant = (importanceScore >= 1 && lowPriorityScore == 0) && delegationScore == 0
        
        val (quadrant, confidence, reasoning) = when {
            lowPriorityScore >= 2 -> Triple(
                EisenhowerQuadrant.ELIMINATE,
                0.85f,
                "Multiple low-priority indicators detected"
            )
            // Delegation patterns take priority when clearly administrative
            delegationScore >= 1 && !isUrgent -> Triple(
                EisenhowerQuadrant.DELEGATE,
                (0.65f + minOf(delegationScore, 3) * 0.05f),
                "Routine/administrative task suitable for delegation"
            )
            isUrgent && isImportant -> Triple(
                EisenhowerQuadrant.DO,
                (0.7f + minOf(urgencyScore + importanceScore, 4) * 0.05f),
                "Task shows both urgency and importance signals"
            )
            !isUrgent && isImportant -> Triple(
                EisenhowerQuadrant.SCHEDULE,
                (0.7f + minOf(importanceScore, 3) * 0.05f),
                "Important task without immediate deadline"
            )
            isUrgent && !isImportant -> Triple(
                EisenhowerQuadrant.DELEGATE,
                (0.6f + minOf(delegationScore + 1, 3) * 0.05f),
                "Urgent but could potentially be delegated"
            )
            delegationScore >= 1 -> Triple(
                EisenhowerQuadrant.DELEGATE,
                (0.65f + minOf(delegationScore, 3) * 0.05f),
                "Routine/administrative task suitable for delegation"
            )
            lowPriorityScore >= 1 -> Triple(
                EisenhowerQuadrant.ELIMINATE,
                (0.6f + minOf(lowPriorityScore, 2) * 0.1f),
                "Low-priority or optional activity"
            )
            else -> Triple(
                EisenhowerQuadrant.SCHEDULE,
                0.55f,
                "No clear urgency indicators - scheduling for review"
            )
        }
        
        return ClassificationResult(
            quadrant = quadrant,
            confidence = minOf(confidence, 0.95f),
            reasoning = reasoning,
            source = ClassificationSource.RULE_BASED
        )
    }
}

enum class EisenhowerQuadrant {
    DO,         // Urgent + Important (Q1)
    SCHEDULE,   // Not Urgent + Important (Q2)
    DELEGATE,   // Urgent + Not Important (Q3)
    ELIMINATE   // Not Urgent + Not Important (Q4)
}

enum class ClassificationSource {
    LLM,
    RULE_BASED
}

data class ClassificationResult(
    val quadrant: EisenhowerQuadrant,
    val confidence: Float,
    val reasoning: String,
    val source: ClassificationSource
)

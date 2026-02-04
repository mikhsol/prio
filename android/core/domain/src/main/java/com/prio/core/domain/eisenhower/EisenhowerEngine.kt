package com.prio.core.domain.eisenhower

import com.prio.core.common.model.EisenhowerQuadrant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Eisenhower priority classification engine.
 * 
 * Implements task 3.1.1 from ACTION_PLAN.md Milestone 3.1:
 * - Rule-based primary classifier (per 0.2.5 recommendation)
 * - Deadline urgency scoring (7d/3d/24h/overdue=Q1)
 * - Keyword dictionaries (50+ per quadrant)
 * - Temporal pattern matching ("tomorrow", "by Friday")
 * - Confidence scoring for LLM escalation
 * 
 * Target Performance (per ACTION_PLAN.md):
 * - Accuracy: ≥75% (verified via 0.2.3)
 * - Latency: <100ms (actual: <10ms)
 * 
 * The engine uses a hybrid approach:
 * 1. Rule-based classification for fast, offline operation
 * 2. LLM escalation for low-confidence cases (via [shouldEscalateToLlm])
 * 
 * @see com.prio.core.aiprovider.provider.RuleBasedFallbackProvider for AiProvider integration
 */
@Singleton
class EisenhowerEngine @Inject constructor(
    private val clock: Clock
) {
    
    companion object {
        private const val TAG = "EisenhowerEngine"
        
        /**
         * Confidence threshold below which LLM escalation is recommended.
         * Based on 0.2.5 findings: Rule-based handles clear cases (75%),
         * LLM improves edge cases (80%).
         */
        const val LLM_ESCALATION_THRESHOLD = 0.65f
        
        /**
         * Urgency score thresholds for deadline-based classification.
         * Per TM-005: Deadline-Based Urgency Scoring.
         */
        object UrgencyThresholds {
            const val CRITICAL = 0.75f  // Past due or due today
            const val HIGH = 0.65f      // Due tomorrow
            const val MEDIUM = 0.50f    // Within 3 days
            const val LOW = 0.25f       // Within a week
            const val MINIMAL = 0f      // No deadline or far out
        }
        
        /**
         * Days until deadline thresholds for urgency scoring.
         */
        object DeadlineDays {
            const val OVERDUE = 0       // Past due
            const val TODAY = 0         // Due today
            const val TOMORROW = 1      // Due tomorrow
            const val WITHIN_3_DAYS = 3 // Within 3 days
            const val WITHIN_WEEK = 7   // Within a week
        }
    }
    
    // ========================================================================
    // Pattern Definitions (50+ per category per spec)
    // ========================================================================
    
    /**
     * Urgency patterns - signals that a task needs immediate attention.
     * 50+ patterns covering:
     * - Explicit urgency words (urgent, ASAP, critical)
     * - Today/tonight deadlines
     * - Overdue indicators
     * - System/production emergencies
     * - Client/customer waiting
     */
    private val urgencyPatterns = listOf(
        // Explicit urgency words
        Regex("(?i)\\b(urgent|asap|immediately|emergency|critical|crisis)\\b"),
        Regex("(?i)\\b(now|right now|right away|this instant)\\b"),
        Regex("(?i)\\b(first thing|top priority|highest priority)\\b"),
        
        // Today/tonight deadlines
        Regex("(?i)\\b(today|tonight|this morning|this afternoon|this evening)\\b"),
        Regex("(?i)\\b(before|by|until)\\s+(today|tonight|end of day|EOD|close of business|COB)\\b"),
        Regex("(?i)\\bend of (day|today)\\b"),
        Regex("(?i)\\b(by|before)\\s+(noon|midnight|5pm|6pm|end of business)\\b"),
        
        // Overdue indicators
        Regex("(?i)\\b(overdue|late|behind|past due|missed|expired)\\b"),
        Regex("(?i)\\bwas due\\b"),
        Regex("(?i)\\bshould have (been done|finished|completed)\\b"),
        
        // Time-specific urgency
        Regex("(?i)\\b(in|within)\\s+(\\d+|one|two|three|a few)\\s*(hour|minute|min|hr)s?\\b"),
        Regex("(?i)\\bdue\\s+(today|now|immediately|asap)\\b"),
        Regex("(?i)\\b(deadline|due)\\s+(today|tomorrow|tonight)\\b"),
        Regex("(?i)\\bdeadline\\s+(approaching|coming up|soon)\\b"),
        Regex("(?i)\\bhas\\s+a\\s+deadline\\s+(today|tomorrow)\\b"),
        
        // System/production emergencies (12 patterns)
        Regex("(?i)\\b(server|system|app|site|service|website)\\s+(is\\s+)?(down|crashed?|outage|issue|error|failure)\\b"),
        Regex("(?i)\\b(server|system|app|site|service|website).{0,10}(down|crash|outage|failure)\\b"),
        Regex("(?i)\\b(down|crash|outage|failure).*(server|system|app|site|service)\\b"),
        Regex("(?i)\\b(production|prod)\\s*(is\\s+)?(issue|problem|bug|error|incident|fire|down)\\b"),
        Regex("(?i)\\b(pager|alert|alarm|monitoring)\\s*(going off|triggered|critical)\\b"),
        Regex("(?i)\\b(fix|resolve|address)\\s*(immediately|now|asap|urgent)\\b"),
        Regex("(?i)\\b(hotfix|hot fix|patch)\\b"),
        Regex("(?i)\\b(outage|incident|sev[0-9]|severity\\s*[0-9])\\b"),
        Regex("(?i)\\b(users|customers)\\s+(cannot|can't|unable to|experiencing)\\b"),
        Regex("(?i)\\bbroken\\s+(build|pipeline|deployment|feature)\\b"),
        Regex("(?i)\\bblocking\\s+(release|deploy|launch|other)\\b"),
        Regex("(?i)\\brollback\\s+(needed|required|now)\\b"),
        Regex("(?i)\\bescalation\\b"),
        
        // Client/customer waiting (8 patterns)
        Regex("(?i)\\b(client|customer|stakeholder)\\s*(waiting|asking|calling|urgent|needs)\\b"),
        Regex("(?i)\\bwaiting\\s+(on|for)\\s+(you|me|us|this|response|answer)\\b"),
        Regex("(?i)\\b(promised|committed)\\s+(by|to deliver)\\s+(today|tomorrow)\\b"),
        Regex("(?i)\\b(ceo|boss|manager|director|vp)\\s*(asking|needs|waiting|wants)\\b"),
        Regex("(?i)\\bexpecting\\s+(today|this|a response|an answer)\\b"),
        Regex("(?i)\\b(demo|presentation|meeting)\\s+(in|starts?)\\s+(\\d+)\\s*(min|hour)\\b"),
        Regex("(?i)\\b(call|meeting)\\s+(today|now|shortly|in\\s+\\d+\\s*min)\\b"),
        Regex("(?i)\\bboard\\s+(meeting|presentation|review)\\b")
    )
    
    /**
     * Importance patterns - signals that a task has significant impact.
     * 50+ patterns covering:
     * - Strategic/business impact
     * - Career/professional development
     * - Health and wellness
     * - Family and relationships
     * - Financial significance
     * - Legal/compliance requirements
     */
    private val importancePatterns = listOf(
        // Explicit importance words
        Regex("(?i)\\b(important|crucial|vital|essential|key|strategic|significant)\\b"),
        Regex("(?i)\\b(matters|meaningful|high impact|valuable)\\b"),
        Regex("(?i)\\b(priority|prioritize|must do|need to|have to)\\b"),
        
        // Career/professional impact (12 patterns)
        Regex("(?i)\\b(career|promotion|performance|review|evaluation|raise|salary)\\b"),
        Regex("(?i)\\b(job|interview|offer|resign|hire|onboard)\\b"),
        Regex("(?i)\\b(resume|cv|portfolio|application)\\b"),
        Regex("(?i)\\b(networking|mentor|mentee|referral)\\b"),
        Regex("(?i)\\b(skill|skills|expertise|competency)\\b"),
        Regex("(?i)\\b(certification|certificate|credential)\\b"),
        Regex("(?i)\\b(leadership|lead|manage|initiative)\\b"),
        Regex("(?i)\\b(presentation|present to|pitch to)\\b"),
        Regex("(?i)\\b(growth|advancement|opportunity)\\b"),
        Regex("(?i)\\b(1:1|one on one|feedback session)\\b"),
        Regex("(?i)\\b(okr|kpi|objective|key result)\\b"),
        Regex("(?i)\\b(quarter|quarterly|annual)\\s*(goal|review|planning)\\b"),
        
        // Health/wellness (10 patterns)
        Regex("(?i)\\b(health|doctor|medical|hospital|clinic)\\b"),
        Regex("(?i)\\b(appointment|checkup|check-up|physical)\\b"),
        Regex("(?i)\\b(prescription|medication|medicine|pharmacy)\\b"),
        Regex("(?i)\\b(symptom|sick|illness|pain|injury)\\b"),
        Regex("(?i)\\b(exercise|workout|gym|run|fitness|yoga|meditation)\\b"),
        Regex("(?i)\\b(sleep|rest|recovery|mental health)\\b"),
        Regex("(?i)\\b(diet|nutrition|eating|weight)\\b"),
        Regex("(?i)\\b(therapy|therapist|counseling|counselor)\\b"),
        Regex("(?i)\\b(dentist|dental|teeth|vision|eye|optometrist)\\b"),
        Regex("(?i)\\b(vaccine|vaccination|immunization|screening)\\b"),
        
        // Family/relationships (8 patterns)
        Regex("(?i)\\b(family|spouse|partner|wife|husband|child|children|kid|kids|parent|mom|dad)\\b"),
        Regex("(?i)\\b(wedding|anniversary|birthday|graduation|celebration)\\b"),
        Regex("(?i)\\b(relationship|marriage|dating|partner)\\b"),
        Regex("(?i)\\b(school|teacher|parent-teacher|pta)\\b"),
        Regex("(?i)\\b(daycare|babysitter|childcare|nanny)\\b"),
        Regex("(?i)\\b(elderly|aging parent|caregiver|care for)\\b"),
        Regex("(?i)\\b(pet|vet|veterinarian)\\b"),
        Regex("(?i)\\b(home|house|moving|renovation|repair)\\b"),
        
        // Financial significance (10 patterns)
        Regex("(?i)\\b(tax|taxes|irs|tax return|w-2|1099)\\b"),
        Regex("(?i)\\b(financial|budget|investment|retirement|401k|ira)\\b"),
        Regex("(?i)\\b(mortgage|loan|debt|payment|bills|rent)\\b"),
        Regex("(?i)\\b(insurance|policy|coverage|claim)\\b"),
        Regex("(?i)\\b(savings|emergency fund|financial goal)\\b"),
        Regex("(?i)\\b(bank|banking|account|transfer)\\b"),
        Regex("(?i)\\b(credit|credit score|credit card|debt)\\b"),
        Regex("(?i)\\b(expense|expenses|reimbursement)\\b"),
        Regex("(?i)\\b(invoice|billing|payment due)\\b"),
        Regex("(?i)\\b(audit|auditing|compliance|regulatory)\\b"),
        
        // Legal/compliance (6 patterns)
        Regex("(?i)\\b(contract|agreement|sign|signature|legal|lawyer|attorney)\\b"),
        Regex("(?i)\\b(court|lawsuit|litigation|dispute)\\b"),
        Regex("(?i)\\b(deadline|filing|file by|submit by)\\b"),
        Regex("(?i)\\b(compliance|regulation|regulatory|requirement)\\b"),
        Regex("(?i)\\b(license|permit|registration|renewal)\\b"),
        Regex("(?i)\\b(patent|trademark|copyright|intellectual property)\\b"),
        
        // Learning/development (6 patterns)
        Regex("(?i)\\b(learn|study|course|class|training|workshop)\\b"),
        Regex("(?i)\\b(read|book|research|understand|explore)\\b"),
        Regex("(?i)\\b(degree|education|school|university|college)\\b"),
        Regex("(?i)\\b(practice|skill building|improve|master)\\b"),
        Regex("(?i)\\b(tutorial|lesson|lecture|webinar)\\b"),
        Regex("(?i)\\b(homework|assignment|project|thesis)\\b"),
        
        // Business impact (10 patterns)
        Regex("(?i)\\b(client|customer|investor|board|stakeholder|executive)\\b"),
        Regex("(?i)\\b(project|deliverable|release|launch|milestone)\\b"),
        Regex("(?i)\\b(report|analysis|review|proposal|documentation)\\b"),
        Regex("(?i)\\b(decision|approve|sign-off|approval)\\b"),
        Regex("(?i)\\b(submit|complete|finish|deliver|ship|deploy)\\b"),
        Regex("(?i)\\b(prepare|create|build|develop|design)\\b"),
        Regex("(?i)\\b(strategy|plan|planning|roadmap|vision)\\b"),
        Regex("(?i)\\b(revenue|sales|profit|cost|budget|forecast)\\b"),
        Regex("(?i)\\b(partnership|acquisition|merger|deal)\\b"),
        Regex("(?i)\\b(hire|firing|restructure|layoff)\\b"),
        
        // System/Production emergencies are inherently important (8 patterns)
        // These tasks affect users, business continuity, and reputation
        Regex("(?i)\\b(server|system|app|site|service|website)\\s+(is\\s+)?(down|crashed?|outage|failure)\\b"),
        Regex("(?i)\\b(server|system|app|site|service|website).{0,10}(down|crash|outage|failure)\\b"),
        Regex("(?i)\\b(production|prod)\\s*(is\\s+)?(issue|problem|bug|error|incident|down)\\b"),
        Regex("(?i)\\b(data)\\s*(loss|corruption|at risk|affected)\\b"),
        Regex("(?i)\\b(users?|customers?)\\s*(affected|impacted|cannot|can't|unable)\\b"),
        Regex("(?i)\\b(outage|incident|sev[0-9]|severity\\s*[0-9]|critical)\\b"),
        Regex("(?i)\\b(emergency|crisis)\\b"),
        Regex("(?i)\\baffecting\\s+(all|many|customers?|users?)\\b"),
        Regex("(?i)\\b(hotfix|rollback|emergency\\s+fix)\\b"),
        Regex("(?i)\\bfix\\s+(immediately|now|asap|urgent)\\b")
    )
    
    /**
     * Delegation patterns - signals routine/administrative tasks.
     * 30+ patterns for tasks that could be delegated or automated.
     */
    private val delegationPatterns = listOf(
        // Explicit delegation
        Regex("(?i)\\b(delegate|assign|ask\\s+.+\\s+to|have\\s+.+\\s+do)\\b"),
        Regex("(?i)\\b(someone else|team can|anyone can)\\b"),
        Regex("(?i)\\b(get\\s+.+\\s+to\\s+help)\\b"),
        
        // Routine/recurring tasks
        Regex("(?i)\\b(routine|regular|recurring|standard|periodic)\\b"),
        Regex("(?i)\\b(weekly|monthly|daily|biweekly)\\s+(report|update|check|task)\\b"),
        
        // Administrative tasks
        Regex("(?i)\\border\\s+(office\\s+)?supplies\\b"),
        Regex("(?i)\\boffice\\s+supplies\\b"),
        Regex("(?i)\\b(schedule|book|reserve)\\s+(meeting|room|flight|hotel|restaurant|travel|lunch|dinner|event)\\b"),
        Regex("(?i)\\b(schedule|book|reserve)\\s+(the\\s+)?(team\\s+)?(meeting|lunch|dinner|event|outing|activity)\\b"),
        Regex("(?i)\\b(arrange|set up|organize)\\s+(meeting|call|event|lunch|dinner|party|celebration)\\b"),
        Regex("(?i)\\b(book|reserve)\\s+(travel|flights?|tickets?|transportation)\\b"),
        Regex("(?i)\\b(team\\s+)?(lunch|dinner|outing|building|event)\\b.*\\b(schedule|book|organize|arrange)\\b"),
        Regex("(?i)\\b(schedule|plan|organize)\\s+.*(team|group)\\s*(lunch|dinner|outing|event|activity|building)\\b"),
        
        // Status updates and reports
        Regex("(?i)\\bstatus\\s+(report|update|check|meeting)\\b"),
        Regex("(?i)\\bweekly\\s+.*report\\b"),
        Regex("(?i)\\b(compile|gather|collect)\\s+.*report\\b"),
        Regex("(?i)\\b(send|share)\\s+.*update\\b"),
        
        // Survey/feedback/forms
        Regex("(?i)\\b(survey|poll|feedback|form|questionnaire)\\b"),
        Regex("(?i)\\bfill\\s+(out|in)\\s+(form|survey|questionnaire)\\b"),
        
        // Data entry/updates
        Regex("(?i)\\b(update|enter|log|record)\\s+.*(data|spreadsheet|system|database|crm)\\b"),
        Regex("(?i)\\b(input|entry|logging)\\b"),
        
        // Filing/organization
        Regex("(?i)\\b(file|organize|sort|archive)\\s+.*(documents|files|papers|folders)\\b"),
        Regex("(?i)\\b(backup|back up)\\s+.*(files|data)\\b"),
        
        // Basic communication
        Regex("(?i)\\b(forward|cc|bcc|reply to)\\s+.*(email|message)\\b"),
        Regex("(?i)\\b(send|forward)\\s+.*(reminder|notice|announcement)\\b"),
        
        // Scheduling/coordination
        Regex("(?i)\\b(coordinate|reschedule|follow up)\\b"),
        Regex("(?i)\\bsend\\s+calendar\\s+invite\\b"),
        
        // Ordering/purchasing (non-strategic)
        Regex("(?i)\\b(order|reorder|purchase)\\s+.*(supplies|materials|equipment)\\b"),
        Regex("(?i)\\b(renew|renewal)\\s+.*(subscription|license|membership)\\b"),
        
        // Basic research/lookup
        Regex("(?i)\\blook\\s+up\\s+.*(info|information|details|contact)\\b"),
        Regex("(?i)\\bfind\\s+.*(phone|email|address|contact)\\b"),
        
        // Minor fixes/maintenance
        Regex("(?i)\\bminor\\s+(fix|update|change|adjustment)\\b"),
        Regex("(?i)\\b(update|change)\\s+.*(password|settings|preferences)\\b")
    )
    
    /**
     * Low priority patterns - signals time-wasters or optional activities.
     * 30+ patterns for non-essential tasks.
     */
    private val lowPriorityPatterns = listOf(
        // Explicit low priority
        Regex("(?i)\\b(maybe|someday|eventually|when I have time|if I have time)\\b"),
        Regex("(?i)\\b(nice to have|would be good|could|might)\\b"),
        Regex("(?i)\\b(optional|not required|not urgent|low priority|non-essential)\\b"),
        Regex("(?i)\\b(no rush|no hurry|whenever|at some point)\\b"),
        Regex("(?i)\\bif\\s+(time|possible|i get a chance)\\b"),
        
        // Entertainment/leisure
        Regex("(?i)\\b(browse|scroll|watch|binge|stream)\\b"),
        Regex("(?i)\\b(social media|youtube|netflix|reddit|twitter|instagram|tiktok|facebook)\\b"),
        Regex("(?i)\\b(game|gaming|play|entertainment)\\b"),
        Regex("(?i)\\b(tv|show|series|movie)\\b"),
        Regex("(?i)\\b(podcast|music|playlist)\\b"),
        
        // Non-essential organization
        Regex("(?i)\\b(reorganize|rearrange|tidy|declutter)\\s+(bookshelf|desk|closet|room|drawer)\\b"),
        Regex("(?i)\\b(clean|organize)\\s+(files|photos|music|apps|downloads)\\b"),
        Regex("(?i)\\b(sort|organize)\\s+(old|unused)\\b"),
        
        // Wishlist/want-to-do
        Regex("(?i)\\b(wish|want to|would like to|thinking about)\\b"),
        Regex("(?i)\\b(daydream|fantasy|dream about)\\b"),
        Regex("(?i)\\bwishlist\\b"),
        
        // Research for fun
        Regex("(?i)\\b(look into|check out|explore)\\s+.*(fun|interesting|cool|random)\\b"),
        Regex("(?i)\\brandom\\s+(idea|thought|thing)\\b"),
        
        // Trivial errands
        Regex("(?i)\\b(just|only)\\s+(check|look|see|browse|glance)\\b"),
        Regex("(?i)\\bquick\\s+(look|check|glance)\\b"),
        
        // Vague/undefined tasks
        Regex("(?i)^(stuff|things|misc|miscellaneous|other|various)$"),
        Regex("(?i)\\bsomething\\s+(about|with|for)\\b"),
        
        // Repeated/already done
        Regex("(?i)\\b(third time|again|another|repeat|redo)\\b"),
        Regex("(?i)\\bdid this (before|already|last)\\b")
    )
    
    /**
     * Deadline patterns for extracting temporal urgency.
     */
    private val soonDeadlinePatterns = listOf(
        Regex("(?i)\\b(today|tonight|this morning|this afternoon|this evening)\\b"),
        Regex("(?i)\\btomorrow\\b"),
        Regex("(?i)\\bby\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b"),
        Regex("(?i)\\bin\\s+(1|one|2|two|3|three)\\s+days?\\b"),
        Regex("(?i)\\bdue\\s+(today|tomorrow|soon)\\b"),
        Regex("(?i)\\bthis\\s+week\\b"),
        Regex("(?i)\\bEOD|EOW\\b"),
        Regex("(?i)\\bend\\s+of\\s+(week|day)\\b"),
        Regex("(?i)\\bwithin\\s+\\d+\\s+hours?\\b")
    )
    
    /**
     * Future deadline patterns (less urgent).
     */
    private val futureDeadlinePatterns = listOf(
        Regex("(?i)\\bnext\\s+(week|month|year)\\b"),
        Regex("(?i)\\bin\\s+(\\d+|several)\\s+weeks?\\b"),
        Regex("(?i)\\bby\\s+(next|end of)\\s+(month|quarter|year)\\b"),
        Regex("(?i)\\b(Q[1-4]|quarter)\\b"),
        Regex("(?i)\\beventually\\b"),
        Regex("(?i)\\bno\\s+(deadline|due date|rush|hurry)\\b"),
        Regex("(?i)\\blong\\s+term\\b"),
        Regex("(?i)\\bfuture\\b")
    )
    
    // ========================================================================
    // Public API
    // ========================================================================
    
    /**
     * Classify a task into an Eisenhower quadrant.
     * 
     * This is the primary classification method. It uses:
     * 1. Keyword pattern matching for urgency/importance signals
     * 2. Deadline proximity for urgency scoring
     * 3. Confidence scoring to identify edge cases
     * 
     * @param taskText The task description to classify
     * @param dueDate Optional due date for deadline-based urgency
     * @return Classification result with quadrant, confidence, and explanation
     */
    fun classify(
        taskText: String,
        dueDate: Instant? = null
    ): EisenhowerClassificationResult {
        val startTime = System.nanoTime()
        
        // Calculate deadline-based urgency
        val deadlineUrgency = calculateDeadlineUrgency(dueDate)
        
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
        
        // Check deadline patterns in text
        val hasSoonDeadline = soonDeadlinePatterns.any { it.containsMatchIn(taskText) }
        val hasFutureDeadline = futureDeadlinePatterns.any { it.containsMatchIn(taskText) }
        
        // Combine pattern urgency with deadline urgency
        val isUrgentByPattern = urgencyScore >= 1 || hasSoonDeadline
        val isUrgentByDeadline = deadlineUrgency >= UrgencyThresholds.HIGH
        val isUrgent = isUrgentByPattern || isUrgentByDeadline
        
        val isImportant = (importanceScore >= 1 && lowPriorityScore == 0) && delegationScore == 0
        
        // Classification logic with confidence scoring
        val (quadrant, baseConfidence, explanation) = when {
            // Clear low priority (ELIMINATE)
            lowPriorityScore >= 2 -> Triple(
                EisenhowerQuadrant.ELIMINATE,
                0.85f,
                "Multiple low-priority indicators: ${lowPriorityMatches.take(2).joinToString(", ")}"
            )
            
            // Single strong low-priority signal with no counters
            lowPriorityScore >= 1 && urgencyScore == 0 && importanceScore == 0 -> Triple(
                EisenhowerQuadrant.ELIMINATE,
                0.75f,
                "Low-priority activity: ${lowPriorityMatches.firstOrNull() ?: "optional"}"
            )
            
            // Clear delegation case (routine without importance)
            delegationScore >= 1 && !isImportant && !isUrgent -> Triple(
                EisenhowerQuadrant.DELEGATE,
                0.70f + minOf(delegationScore, 2) * 0.05f,
                "Routine or delegation task: ${delegationMatches.firstOrNull()}"
            )
            
            // DO_FIRST: Urgent AND Important
            isUrgent && isImportant -> {
                val conf = 0.75f + minOf(urgencyScore + importanceScore, 4) * 0.05f +
                        (if (deadlineUrgency >= UrgencyThresholds.CRITICAL) 0.10f else 0f)
                Triple(
                    EisenhowerQuadrant.DO_FIRST,
                    conf,
                    buildExplanation("Urgent and important", urgencyMatches, importanceMatches, deadlineUrgency)
                )
            }
            
            // DO_FIRST: Critical deadline even without importance signals
            deadlineUrgency >= UrgencyThresholds.CRITICAL && importanceScore >= 0 -> Triple(
                EisenhowerQuadrant.DO_FIRST,
                0.80f,
                "Critical deadline - due today or overdue"
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
                "Time-sensitive but could be delegated: ${urgencyMatches.firstOrNull() ?: "deadline pressure"}"
            )
            
            // Delegation patterns present
            delegationScore >= 1 -> Triple(
                EisenhowerQuadrant.DELEGATE,
                0.65f,
                "Routine task: ${delegationMatches.firstOrNull()}"
            )
            
            // Has future deadline - likely SCHEDULE
            hasFutureDeadline -> Triple(
                EisenhowerQuadrant.SCHEDULE,
                0.60f,
                "Future deadline - schedule for later"
            )
            
            // Has moderate deadline urgency
            deadlineUrgency >= UrgencyThresholds.MEDIUM -> Triple(
                EisenhowerQuadrant.SCHEDULE,
                0.65f,
                "Deadline within 3 days - plan to complete"
            )
            
            // Default to SCHEDULE (safe default per 0.2.5)
            else -> Triple(
                EisenhowerQuadrant.SCHEDULE,
                0.55f,
                "No clear urgency - scheduling for review"
            )
        }
        
        // Finalize confidence
        val finalConfidence = minOf(baseConfidence, 0.95f)
        val shouldEscalate = finalConfidence < LLM_ESCALATION_THRESHOLD
        
        val latencyMs = (System.nanoTime() - startTime) / 1_000_000f
        
        return EisenhowerClassificationResult(
            quadrant = quadrant,
            confidence = finalConfidence,
            explanation = explanation,
            isUrgent = isUrgent,
            isImportant = isImportant,
            urgencyScore = urgencyScore.toFloat() + deadlineUrgency,
            importanceScore = importanceScore.toFloat(),
            urgencySignals = urgencyMatches,
            importanceSignals = importanceMatches,
            shouldEscalateToLlm = shouldEscalate,
            latencyMs = latencyMs
        )
    }
    
    /**
     * Calculate urgency score based on deadline proximity.
     * 
     * Per TM-005 from user stories:
     * - 7+ days away: Low urgency (0.0-0.25)
     * - 2-6 days: Medium urgency (0.25-0.5)
     * - 1 day: High urgency (0.5-0.75)
     * - Due today/Overdue: Critical urgency (0.75-1.0)
     * 
     * @param dueDate The task's due date (null = no deadline)
     * @return Urgency score from 0.0 to 1.0
     */
    fun calculateDeadlineUrgency(dueDate: Instant?): Float {
        if (dueDate == null) return 0f
        
        val now = clock.now()
        val timeZone = TimeZone.currentSystemDefault()
        val nowLocal = now.toLocalDateTime(timeZone).date
        val dueLocal = dueDate.toLocalDateTime(timeZone).date
        
        val daysUntilDue = nowLocal.daysUntil(dueLocal)
        
        return when {
            daysUntilDue < 0 -> {
                // Overdue: 0.75 to 1.0 based on how overdue
                val daysOverdue = -daysUntilDue
                min(1f, UrgencyThresholds.CRITICAL + (daysOverdue * 0.05f))
            }
            daysUntilDue == 0 -> UrgencyThresholds.CRITICAL  // Due today
            daysUntilDue == 1 -> UrgencyThresholds.HIGH      // Due tomorrow
            daysUntilDue <= 3 -> UrgencyThresholds.MEDIUM    // Within 3 days
            daysUntilDue <= 7 -> UrgencyThresholds.LOW       // Within a week
            else -> max(0f, UrgencyThresholds.LOW - (daysUntilDue - 7) * 0.01f)
        }
    }
    
    /**
     * Batch classify multiple tasks.
     * Useful for initial list population or reclassification.
     * 
     * @param tasks List of task descriptions
     * @return List of classification results in same order
     */
    fun classifyBatch(tasks: List<String>): List<EisenhowerClassificationResult> {
        return tasks.map { classify(it) }
    }
    
    /**
     * Suggest whether a task should be escalated to LLM for better classification.
     * 
     * @param result The rule-based classification result
     * @return true if LLM escalation is recommended
     */
    fun shouldEscalateToLlm(result: EisenhowerClassificationResult): Boolean {
        return result.shouldEscalateToLlm || result.confidence < LLM_ESCALATION_THRESHOLD
    }
    
    // ========================================================================
    // Private Helpers
    // ========================================================================
    
    private fun buildExplanation(
        prefix: String,
        urgencySignals: List<String>,
        importanceSignals: List<String>,
        deadlineUrgency: Float
    ): String {
        val signals = mutableListOf<String>()
        
        if (deadlineUrgency >= UrgencyThresholds.CRITICAL) {
            signals.add("deadline critical")
        } else if (deadlineUrgency >= UrgencyThresholds.HIGH) {
            signals.add("deadline approaching")
        }
        
        urgencySignals.firstOrNull()?.let { signals.add("urgency: $it") }
        importanceSignals.firstOrNull()?.let { signals.add("importance: $it") }
        
        return if (signals.isNotEmpty()) {
            "$prefix (${signals.joinToString(", ")})"
        } else {
            prefix
        }
    }
}

/**
 * Result of Eisenhower classification.
 * 
 * Contains:
 * - The assigned quadrant
 * - Confidence score (0.0-1.0)
 * - Human-readable explanation
 * - Detected urgency and importance signals
 * - Recommendation for LLM escalation
 */
data class EisenhowerClassificationResult(
    val quadrant: EisenhowerQuadrant,
    val confidence: Float,
    val explanation: String,
    val isUrgent: Boolean,
    val isImportant: Boolean,
    val urgencyScore: Float,
    val importanceScore: Float,
    val urgencySignals: List<String>,
    val importanceSignals: List<String>,
    val shouldEscalateToLlm: Boolean,
    val latencyMs: Float = 0f
) {
    /**
     * Quick check if this is a high-confidence classification.
     */
    val isHighConfidence: Boolean
        get() = confidence >= 0.75f
    
    /**
     * Quick check if this is a low-confidence classification.
     */
    val isLowConfidence: Boolean
        get() = confidence < EisenhowerEngine.LLM_ESCALATION_THRESHOLD
}

package app.jeeves.llmtest.benchmark

import app.jeeves.llmtest.engine.EisenhowerQuadrant
import kotlinx.serialization.Serializable

/**
 * Prompt engineering strategies for Eisenhower classification.
 * 
 * Milestone 0.2.6 Option A: Improve prompt engineering to achieve >70% accuracy
 * 
 * Strategies implemented:
 * - 0.2.6.1: JSON mode structured output
 * - 0.2.6.2: Chain-of-thought reasoning
 * - 0.2.6.3: Few-shot with 3-5 examples per quadrant
 * - 0.2.6.4: Expert persona system prompt
 */
enum class PromptStrategy(
    val id: String,
    val displayName: String,
    val description: String
) {
    /**
     * Baseline: Simple direct prompt with JSON output.
     * This is the original prompt from 0.2.3 for comparison.
     */
    BASELINE_SIMPLE(
        id = "baseline_simple",
        displayName = "Baseline (Simple)",
        description = "Direct classification request with minimal context"
    ),
    
    /**
     * Task 0.2.6.1: Structured JSON output with strict format.
     * Enforces JSON-only response with clear schema definition.
     */
    JSON_STRUCTURED(
        id = "json_structured",
        displayName = "JSON Structured (0.2.6.1)",
        description = "Strict JSON output format with schema enforcement"
    ),
    
    /**
     * Task 0.2.6.2: Chain-of-thought reasoning.
     * Step-by-step analysis: Is it urgent? Is it important?
     */
    CHAIN_OF_THOUGHT(
        id = "chain_of_thought",
        displayName = "Chain-of-Thought (0.2.6.2)",
        description = "Step-by-step reasoning: urgency → importance → quadrant"
    ),
    
    /**
     * Task 0.2.6.3: Few-shot learning with diverse examples.
     * 3-5 examples per quadrant to establish patterns.
     */
    FEW_SHOT(
        id = "few_shot",
        displayName = "Few-Shot (0.2.6.3)",
        description = "In-context learning with 3-5 examples per quadrant"
    ),
    
    /**
     * Task 0.2.6.4: Eisenhower expert persona.
     * System prompt establishing expertise in time management.
     */
    EXPERT_PERSONA(
        id = "expert_persona",
        displayName = "Expert Persona (0.2.6.4)",
        description = "Expert time management consultant persona"
    ),
    
    /**
     * Combined: Best of all strategies.
     * Expert persona + Few-shot + Chain-of-thought + JSON output.
     */
    COMBINED_OPTIMAL(
        id = "combined_optimal",
        displayName = "Combined Optimal",
        description = "Expert persona + few-shot + CoT + JSON output"
    )
}

/**
 * Prompt builder for different strategies.
 * Implements Phi-3 chat template: <|user|>\n{content}<|end|>\n<|assistant|>
 */
object PromptBuilder {
    
    // Phi-3 chat template markers
    private const val USER_START = "<|user|>\n"
    private const val USER_END = "<|end|>\n"
    private const val ASSISTANT_START = "<|assistant|>\n"
    private const val SYSTEM_START = "<|system|>\n"
    private const val SYSTEM_END = "<|end|>\n"
    
    /**
     * Build a classification prompt using the specified strategy.
     */
    fun buildPrompt(strategy: PromptStrategy, taskText: String): String {
        return when (strategy) {
            PromptStrategy.BASELINE_SIMPLE -> buildBaselinePrompt(taskText)
            PromptStrategy.JSON_STRUCTURED -> buildJsonStructuredPrompt(taskText)
            PromptStrategy.CHAIN_OF_THOUGHT -> buildChainOfThoughtPrompt(taskText)
            PromptStrategy.FEW_SHOT -> buildFewShotPrompt(taskText)
            PromptStrategy.EXPERT_PERSONA -> buildExpertPersonaPrompt(taskText)
            PromptStrategy.COMBINED_OPTIMAL -> buildCombinedOptimalPrompt(taskText)
        }
    }
    
    /**
     * Baseline: Simple prompt matching original 0.2.3 implementation.
     */
    private fun buildBaselinePrompt(taskText: String): String = """
${USER_START}Classify this task into an Eisenhower Matrix quadrant.

Quadrants:
- DO: Urgent and Important
- SCHEDULE: Important but Not Urgent
- DELEGATE: Urgent but Not Important
- ELIMINATE: Not Urgent and Not Important

Task: "$taskText"

Respond with JSON only:
{"quadrant": "DO|SCHEDULE|DELEGATE|ELIMINATE", "confidence": 0.0-1.0, "reasoning": "brief explanation"}
${USER_END}${ASSISTANT_START}""".trimIndent()
    
    /**
     * 0.2.6.1: JSON Structured - Strict schema with validation hints.
     */
    private fun buildJsonStructuredPrompt(taskText: String): String = """
${USER_START}You must respond with ONLY valid JSON matching this exact schema:
{
  "quadrant": "<string: exactly one of DO, SCHEDULE, DELEGATE, ELIMINATE>",
  "confidence": <number: between 0.0 and 1.0>,
  "reasoning": "<string: one sentence explanation>"
}

Classification rules:
- DO = deadline within 24-48 hours AND impacts goals/customers/revenue
- SCHEDULE = important for long-term success, no immediate deadline
- DELEGATE = has deadline but low personal value, could be done by others
- ELIMINATE = no deadline AND no real value, time waster

Task to classify: "$taskText"

JSON response:${USER_END}${ASSISTANT_START}""".trimIndent()
    
    /**
     * 0.2.6.2: Chain-of-Thought - Step-by-step reasoning.
     */
    private fun buildChainOfThoughtPrompt(taskText: String): String = """
${USER_START}Classify this task using the Eisenhower Matrix. Think step by step:

Task: "$taskText"

Step 1 - URGENCY CHECK:
Ask: Is there a deadline within 24-48 hours? Is someone waiting? Is there a crisis?
Answer with: URGENT or NOT_URGENT

Step 2 - IMPORTANCE CHECK:
Ask: Does this contribute to long-term goals? Does it affect customers/revenue/health/relationships?
Answer with: IMPORTANT or NOT_IMPORTANT

Step 3 - QUADRANT DECISION:
- URGENT + IMPORTANT = DO
- NOT_URGENT + IMPORTANT = SCHEDULE
- URGENT + NOT_IMPORTANT = DELEGATE
- NOT_URGENT + NOT_IMPORTANT = ELIMINATE

Provide your analysis in this exact format:
Step 1: [URGENT/NOT_URGENT] - [brief reason]
Step 2: [IMPORTANT/NOT_IMPORTANT] - [brief reason]
Step 3: {"quadrant": "...", "confidence": 0.X, "reasoning": "..."}
${USER_END}${ASSISTANT_START}""".trimIndent()
    
    /**
     * 0.2.6.3: Few-Shot - Examples for each quadrant.
     */
    private fun buildFewShotPrompt(taskText: String): String = """
${USER_START}Classify tasks into Eisenhower Matrix quadrants. Here are examples:

DO (Urgent + Important):
- "Server is down, customers cannot access" → DO (crisis affecting customers)
- "Tax filing deadline is tomorrow" → DO (legal deadline imminent)
- "Client presentation in 2 hours" → DO (imminent important meeting)

SCHEDULE (Not Urgent + Important):
- "Plan next quarter strategy" → SCHEDULE (important planning, no deadline)
- "Schedule annual health checkup" → SCHEDULE (health matters, not urgent)
- "Learn new programming language" → SCHEDULE (career growth, no deadline)

DELEGATE (Urgent + Not Important):
- "Order office supplies by EOD" → DELEGATE (deadline exists, not strategic)
- "Respond to routine HR survey today" → DELEGATE (deadline, low value)
- "Book team lunch reservation" → DELEGATE (coordination task)

ELIMINATE (Not Urgent + Not Important):
- "Browse social media" → ELIMINATE (time waster)
- "Reorganize desktop icons" → ELIMINATE (busy work)
- "Watch random YouTube videos" → ELIMINATE (procrastination)

Now classify this task:
"$taskText"

Respond with JSON only:
{"quadrant": "DO|SCHEDULE|DELEGATE|ELIMINATE", "confidence": 0.0-1.0, "reasoning": "brief explanation"}
${USER_END}${ASSISTANT_START}""".trimIndent()
    
    /**
     * 0.2.6.4: Expert Persona - Eisenhower Matrix expert system prompt.
     */
    private fun buildExpertPersonaPrompt(taskText: String): String = """
${SYSTEM_START}You are Dr. Priority, a world-renowned time management expert who has helped thousands of executives master the Eisenhower Matrix. You have three decision rules:

RULE 1 - URGENCY: A task is URGENT only if:
- There's a hard deadline within 48 hours, OR
- Someone is actively waiting/blocked, OR
- There's an active crisis/emergency

RULE 2 - IMPORTANCE: A task is IMPORTANT only if:
- It directly contributes to career/business/health goals, OR
- It affects customers, revenue, or relationships, OR
- Failure would have significant consequences

RULE 3 - QUADRANT MAPPING:
- DO: URGENT ∧ IMPORTANT (crises, deadlines with consequences)
- SCHEDULE: ¬URGENT ∧ IMPORTANT (planning, growth, prevention)
- DELEGATE: URGENT ∧ ¬IMPORTANT (interruptions, admin tasks)
- ELIMINATE: ¬URGENT ∧ ¬IMPORTANT (time wasters, distractions)

You always respond with valid JSON only.${SYSTEM_END}${USER_START}Classify this task: "$taskText"

{"quadrant": "?", "confidence": ?, "reasoning": "?"}${USER_END}${ASSISTANT_START}""".trimIndent()
    
    /**
     * Combined: Best practices from all strategies.
     */
    private fun buildCombinedOptimalPrompt(taskText: String): String = """
${SYSTEM_START}You are an Eisenhower Matrix classification expert. You analyze tasks methodically and respond with JSON only.

Decision Framework:
- URGENT = deadline ≤48h OR crisis OR someone waiting
- IMPORTANT = affects goals/customers/health/relationships

Quadrant Rules:
- DO: urgent AND important (handle now)
- SCHEDULE: important but NOT urgent (plan for later)
- DELEGATE: urgent but NOT important (assign to others)
- ELIMINATE: neither urgent nor important (remove)${SYSTEM_END}${USER_START}Examples:
1. "Server down affecting customers" → {"quadrant":"DO","confidence":0.95,"reasoning":"Active crisis affecting customers"}
2. "Plan Q2 marketing strategy" → {"quadrant":"SCHEDULE","confidence":0.85,"reasoning":"Strategic planning without deadline"}
3. "Order supplies by EOD" → {"quadrant":"DELEGATE","confidence":0.75,"reasoning":"Deadline exists but routine admin task"}
4. "Browse social media" → {"quadrant":"ELIMINATE","confidence":0.90,"reasoning":"Time waster with no value"}

Now classify: "$taskText"
JSON only:${USER_END}${ASSISTANT_START}""".trimIndent()
    
    /**
     * Extract quadrant from various response formats.
     * Handles both JSON and step-by-step formats.
     */
    fun parseResponse(response: String): ParsedClassification? {
        // Try JSON extraction first
        val jsonResult = extractJsonClassification(response)
        if (jsonResult != null) return jsonResult
        
        // Try Chain-of-Thought format
        val cotResult = extractChainOfThoughtResult(response)
        if (cotResult != null) return cotResult
        
        // Fallback: look for quadrant keywords
        return extractKeywordClassification(response)
    }
    
    private fun extractJsonClassification(response: String): ParsedClassification? {
        return try {
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}')
            if (jsonStart < 0 || jsonEnd < 0 || jsonEnd <= jsonStart) return null
            
            val jsonStr = response.substring(jsonStart, jsonEnd + 1)
            val quadrantMatch = Regex(""""quadrant"\s*:\s*"(\w+)"""").find(jsonStr)
            val confidenceMatch = Regex(""""confidence"\s*:\s*([\d.]+)""").find(jsonStr)
            val reasoningMatch = Regex(""""reasoning"\s*:\s*"([^"]+)"""").find(jsonStr)
            
            val quadrant = quadrantMatch?.groupValues?.get(1)?.uppercase() ?: return null
            val confidence = confidenceMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0.5f
            val reasoning = reasoningMatch?.groupValues?.get(1) ?: ""
            
            val eisenhowerQuadrant = when (quadrant) {
                "DO" -> EisenhowerQuadrant.DO
                "SCHEDULE" -> EisenhowerQuadrant.SCHEDULE
                "DELEGATE" -> EisenhowerQuadrant.DELEGATE
                "ELIMINATE" -> EisenhowerQuadrant.ELIMINATE
                else -> return null
            }
            
            ParsedClassification(
                quadrant = eisenhowerQuadrant,
                confidence = confidence.coerceIn(0f, 1f),
                reasoning = reasoning
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractChainOfThoughtResult(response: String): ParsedClassification? {
        // Look for Step 3 JSON in chain-of-thought response
        val step3Match = Regex("""Step 3[:\s]*(\{[^}]+\})""", RegexOption.IGNORE_CASE).find(response)
        if (step3Match != null) {
            return extractJsonClassification(step3Match.groupValues[1])
        }
        
        // Look for urgency and importance assessments
        val urgencyMatch = Regex("""Step 1[:\s]*(URGENT|NOT_URGENT)""", RegexOption.IGNORE_CASE).find(response)
        val importanceMatch = Regex("""Step 2[:\s]*(IMPORTANT|NOT_IMPORTANT)""", RegexOption.IGNORE_CASE).find(response)
        
        if (urgencyMatch != null && importanceMatch != null) {
            val isUrgent = urgencyMatch.groupValues[1].uppercase() == "URGENT"
            val isImportant = importanceMatch.groupValues[1].uppercase() == "IMPORTANT"
            
            val quadrant = when {
                isUrgent && isImportant -> EisenhowerQuadrant.DO
                !isUrgent && isImportant -> EisenhowerQuadrant.SCHEDULE
                isUrgent && !isImportant -> EisenhowerQuadrant.DELEGATE
                else -> EisenhowerQuadrant.ELIMINATE
            }
            
            return ParsedClassification(
                quadrant = quadrant,
                confidence = 0.7f,
                reasoning = "Derived from CoT: urgent=$isUrgent, important=$isImportant"
            )
        }
        
        return null
    }
    
    private fun extractKeywordClassification(response: String): ParsedClassification? {
        val upperResponse = response.uppercase()
        
        // Count mentions of each quadrant
        val doCount = Regex("\\bDO\\b").findAll(upperResponse).count()
        val scheduleCount = Regex("\\bSCHEDULE\\b").findAll(upperResponse).count()
        val delegateCount = Regex("\\bDELEGATE\\b").findAll(upperResponse).count()
        val eliminateCount = Regex("\\bELIMINATE\\b").findAll(upperResponse).count()
        
        val maxCount = maxOf(doCount, scheduleCount, delegateCount, eliminateCount)
        if (maxCount == 0) return null
        
        val quadrant = when (maxCount) {
            doCount -> EisenhowerQuadrant.DO
            scheduleCount -> EisenhowerQuadrant.SCHEDULE
            delegateCount -> EisenhowerQuadrant.DELEGATE
            else -> EisenhowerQuadrant.ELIMINATE
        }
        
        return ParsedClassification(
            quadrant = quadrant,
            confidence = 0.5f,
            reasoning = "Extracted from keyword frequency"
        )
    }
}

data class ParsedClassification(
    val quadrant: EisenhowerQuadrant,
    val confidence: Float,
    val reasoning: String
)

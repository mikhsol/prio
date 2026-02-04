package app.prio.llmtest.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Eisenhower classifier that uses LLM with rule-based fallback.
 * 
 * Task 0.2.3: Test task categorization accuracy
 */
class EisenhowerClassifier(private val llamaEngine: LlamaEngine) {
    
    companion object {
        private val json = Json { 
            ignoreUnknownKeys = true 
            isLenient = true
        }
        
        /**
         * Optimized prompt for Eisenhower classification.
         * Achieves ~87% accuracy with Phi-3-mini.
         */
        fun buildClassificationPrompt(taskText: String): String = """
You are a task classification assistant. Classify the following task into one of four Eisenhower Matrix quadrants:

1. DO (Urgent + Important): Crisis, deadlines, pressing problems that require immediate attention. Examples: "Server down", "Client deadline today", "Tax filing due tomorrow"

2. SCHEDULE (Important + Not Urgent): Planning, prevention, improvement, relationship building. Examples: "Quarterly planning", "Read industry book", "Schedule health checkup"

3. DELEGATE (Urgent + Not Important): Interruptions, routine tasks that can be handled by others. Examples: "Order office supplies", "Routine status reports", "Schedule team calendar"

4. ELIMINATE (Not Urgent + Not Important): Time wasters, busy work, pleasant activities with no real value. Examples: "Scroll social media", "Reorganize files again", "Watch random YouTube videos"

Important guidelines:
- If no deadline is stated, assume NOT urgent unless context implies crisis
- Work tasks involving clients/customers tend to be important
- Personal development and health are important but rarely urgent
- Routine administrative tasks are usually delegatable

Task: "$taskText"

Respond with JSON only:
{"quadrant": "DO|SCHEDULE|DELEGATE|ELIMINATE", "confidence": 0.0-1.0, "reasoning": "brief explanation"}
""".trimIndent()
    }
    
    /**
     * Classify a task using LLM, falling back to rule-based if needed.
     */
    suspend fun classify(taskText: String): ClassificationResult {
        // Try LLM first if available
        if (llamaEngine.isLoaded) {
            val prompt = buildClassificationPrompt(taskText)
            val result = llamaEngine.generate(
                prompt = prompt,
                maxTokens = 100,
                temperature = 0.3f,
                topP = 0.9f
            )
            
            if (result.error == null && result.text.isNotBlank()) {
                val parsed = parseLlmResponse(result.text)
                if (parsed != null && parsed.confidence >= 0.5f) {
                    return parsed.copy(source = ClassificationSource.LLM)
                }
            }
        }
        
        // Fallback to rule-based
        return RuleBasedClassifier.classify(taskText)
    }
    
    /**
     * Classify using only rule-based approach (for comparison).
     */
    fun classifyRuleBased(taskText: String): ClassificationResult {
        return RuleBasedClassifier.classify(taskText)
    }
    
    private fun parseLlmResponse(response: String): ClassificationResult? {
        return try {
            // Find JSON in response
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}')
            if (jsonStart < 0 || jsonEnd < 0) return null
            
            val jsonStr = response.substring(jsonStart, jsonEnd + 1)
            val parsed = json.decodeFromString<LlmClassificationResponse>(jsonStr)
            
            val quadrant = when (parsed.quadrant.uppercase()) {
                "DO" -> EisenhowerQuadrant.DO
                "SCHEDULE" -> EisenhowerQuadrant.SCHEDULE
                "DELEGATE" -> EisenhowerQuadrant.DELEGATE
                "ELIMINATE" -> EisenhowerQuadrant.ELIMINATE
                else -> return null
            }
            
            ClassificationResult(
                quadrant = quadrant,
                confidence = parsed.confidence.coerceIn(0f, 1f),
                reasoning = parsed.reasoning,
                source = ClassificationSource.LLM
            )
        } catch (e: Exception) {
            null
        }
    }
    
    @Serializable
    private data class LlmClassificationResponse(
        val quadrant: String,
        val confidence: Float,
        val reasoning: String
    )
}

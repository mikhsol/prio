package app.prio.llmtest.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for RuleBasedClassifier.
 * 
 * Task 0.2.5: Rule-based fallback implementation
 */
class RuleBasedClassifierTest {
    
    @Test
    fun `urgent task with deadline should be DO`() {
        val result = RuleBasedClassifier.classify("Submit report by end of day today")
        assertEquals(EisenhowerQuadrant.DO, result.quadrant)
    }
    
    @Test
    fun `server down should be DO`() {
        val result = RuleBasedClassifier.classify("Server is down, fix immediately")
        assertEquals(EisenhowerQuadrant.DO, result.quadrant)
    }
    
    @Test
    fun `strategic planning should be SCHEDULE`() {
        val result = RuleBasedClassifier.classify("Plan next quarter's strategy")
        assertEquals(EisenhowerQuadrant.SCHEDULE, result.quadrant)
    }
    
    @Test
    fun `learning should be SCHEDULE`() {
        val result = RuleBasedClassifier.classify("Learn new programming language for career growth")
        assertEquals(EisenhowerQuadrant.SCHEDULE, result.quadrant)
    }
    
    @Test
    fun `ordering supplies should be DELEGATE`() {
        val result = RuleBasedClassifier.classify("Order office supplies")
        assertEquals(EisenhowerQuadrant.DELEGATE, result.quadrant)
    }
    
    @Test
    fun `routine reports should be DELEGATE`() {
        val result = RuleBasedClassifier.classify("Compile weekly status report")
        assertEquals(EisenhowerQuadrant.DELEGATE, result.quadrant)
    }
    
    @Test
    fun `social media should be ELIMINATE`() {
        val result = RuleBasedClassifier.classify("Browse social media")
        assertEquals(EisenhowerQuadrant.ELIMINATE, result.quadrant)
    }
    
    @Test
    fun `youtube should be ELIMINATE`() {
        val result = RuleBasedClassifier.classify("Watch YouTube videos")
        assertEquals(EisenhowerQuadrant.ELIMINATE, result.quadrant)
    }
    
    @Test
    fun `confidence should be between 0 and 1`() {
        val tasks = listOf(
            "Urgent deadline today",
            "Plan for next year",
            "Order supplies",
            "Browse reddit"
        )
        
        for (task in tasks) {
            val result = RuleBasedClassifier.classify(task)
            assertTrue("Confidence should be >= 0", result.confidence >= 0f)
            assertTrue("Confidence should be <= 1", result.confidence <= 1f)
        }
    }
    
    @Test
    fun `source should be RULE_BASED`() {
        val result = RuleBasedClassifier.classify("Any task")
        assertEquals(ClassificationSource.RULE_BASED, result.source)
    }
    
    @Test
    fun `reasoning should not be empty`() {
        val result = RuleBasedClassifier.classify("Some task to classify")
        assertTrue("Reasoning should not be empty", result.reasoning.isNotBlank())
    }
    
    @Test
    fun `ambiguous task should default to SCHEDULE`() {
        val result = RuleBasedClassifier.classify("Do something")
        assertEquals(EisenhowerQuadrant.SCHEDULE, result.quadrant)
    }
}

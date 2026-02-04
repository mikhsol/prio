package app.jeeves.llmtest.benchmark

import app.jeeves.llmtest.engine.EisenhowerQuadrant
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for prompt engineering strategies.
 * 
 * Milestone 0.2.6 Option A: Tests prompt building and response parsing
 * for different strategies.
 */
class PromptStrategyTest {
    
    @Test
    fun `baseline prompt contains quadrant definitions`() {
        val prompt = PromptBuilder.buildPrompt(PromptStrategy.BASELINE_SIMPLE, "Test task")
        
        assertTrue("Prompt should contain DO quadrant", prompt.contains("DO"))
        assertTrue("Prompt should contain SCHEDULE quadrant", prompt.contains("SCHEDULE"))
        assertTrue("Prompt should contain DELEGATE quadrant", prompt.contains("DELEGATE"))
        assertTrue("Prompt should contain ELIMINATE quadrant", prompt.contains("ELIMINATE"))
        assertTrue("Prompt should contain task text", prompt.contains("Test task"))
    }
    
    @Test
    fun `JSON structured prompt enforces schema`() {
        val prompt = PromptBuilder.buildPrompt(PromptStrategy.JSON_STRUCTURED, "Test task")
        
        assertTrue("Prompt should mention JSON", prompt.contains("JSON"))
        assertTrue("Prompt should define schema", prompt.contains("quadrant"))
        assertTrue("Prompt should define confidence", prompt.contains("confidence"))
        assertTrue("Prompt should define reasoning", prompt.contains("reasoning"))
    }
    
    @Test
    fun `chain-of-thought prompt has step-by-step structure`() {
        val prompt = PromptBuilder.buildPrompt(PromptStrategy.CHAIN_OF_THOUGHT, "Test task")
        
        assertTrue("Prompt should have Step 1", prompt.contains("Step 1"))
        assertTrue("Prompt should have Step 2", prompt.contains("Step 2"))
        assertTrue("Prompt should have Step 3", prompt.contains("Step 3"))
        assertTrue("Prompt should mention URGENT", prompt.contains("URGENT"))
        assertTrue("Prompt should mention IMPORTANT", prompt.contains("IMPORTANT"))
    }
    
    @Test
    fun `few-shot prompt contains examples for all quadrants`() {
        val prompt = PromptBuilder.buildPrompt(PromptStrategy.FEW_SHOT, "Test task")
        
        // Should have example sections for each quadrant
        assertTrue("Prompt should have DO examples", prompt.contains("DO (Urgent"))
        assertTrue("Prompt should have SCHEDULE examples", prompt.contains("SCHEDULE (Not Urgent"))
        assertTrue("Prompt should have DELEGATE examples", prompt.contains("DELEGATE (Urgent"))
        assertTrue("Prompt should have ELIMINATE examples", prompt.contains("ELIMINATE (Not Urgent"))
        
        // Should have concrete examples
        assertTrue("Prompt should have server down example", prompt.contains("Server"))
    }
    
    @Test
    fun `expert persona prompt establishes expertise`() {
        val prompt = PromptBuilder.buildPrompt(PromptStrategy.EXPERT_PERSONA, "Test task")
        
        assertTrue("Prompt should have system tag", prompt.contains("<|system|>"))
        assertTrue("Prompt should establish expertise", 
            prompt.contains("expert") || prompt.contains("Dr.") || prompt.contains("specialist"))
        assertTrue("Prompt should define rules", prompt.contains("RULE"))
    }
    
    @Test
    fun `combined prompt uses all techniques`() {
        val prompt = PromptBuilder.buildPrompt(PromptStrategy.COMBINED_OPTIMAL, "Test task")
        
        // Should have system prompt (expert persona)
        assertTrue("Prompt should have system tag", prompt.contains("<|system|>"))
        
        // Should have examples (few-shot)
        assertTrue("Prompt should have examples", prompt.contains("→"))
        
        // Should request JSON output
        assertTrue("Prompt should request JSON", prompt.contains("JSON"))
    }
    
    // =========================================================================
    // Response Parsing Tests
    // =========================================================================
    
    @Test
    fun `parse valid JSON response`() {
        val response = """{"quadrant": "DO", "confidence": 0.85, "reasoning": "Test reason"}"""
        val parsed = PromptBuilder.parseResponse(response)
        
        assertNotNull("Should parse valid JSON", parsed)
        assertEquals("Should parse DO quadrant", EisenhowerQuadrant.DO, parsed?.quadrant)
        assertEquals("Should parse confidence", 0.85f, parsed?.confidence ?: 0f, 0.01f)
        assertEquals("Should parse reasoning", "Test reason", parsed?.reasoning)
    }
    
    @Test
    fun `parse JSON with surrounding text`() {
        val response = """Here is my analysis:
            {"quadrant": "SCHEDULE", "confidence": 0.75, "reasoning": "Important but not urgent"}
            I hope this helps."""
        val parsed = PromptBuilder.parseResponse(response)
        
        assertNotNull("Should parse JSON from mixed content", parsed)
        assertEquals("Should parse SCHEDULE", EisenhowerQuadrant.SCHEDULE, parsed?.quadrant)
    }
    
    @Test
    fun `parse chain-of-thought response`() {
        val response = """Step 1: NOT_URGENT - No deadline mentioned
Step 2: IMPORTANT - Affects career growth
Step 3: {"quadrant": "SCHEDULE", "confidence": 0.80, "reasoning": "Important career task without deadline"}"""
        val parsed = PromptBuilder.parseResponse(response)
        
        assertNotNull("Should parse CoT response", parsed)
        assertEquals("Should extract SCHEDULE", EisenhowerQuadrant.SCHEDULE, parsed?.quadrant)
    }
    
    @Test
    fun `derive quadrant from CoT steps when JSON missing`() {
        val response = """Step 1: URGENT - deadline tomorrow
Step 2: IMPORTANT - client deliverable
Final: This should be done now."""
        val parsed = PromptBuilder.parseResponse(response)
        
        assertNotNull("Should derive from steps", parsed)
        assertEquals("URGENT + IMPORTANT = DO", EisenhowerQuadrant.DO, parsed?.quadrant)
    }
    
    @Test
    fun `parse lowercase quadrant names`() {
        val response = """{"quadrant": "delegate", "confidence": 0.7, "reasoning": "routine task"}"""
        val parsed = PromptBuilder.parseResponse(response)
        
        assertNotNull("Should handle lowercase", parsed)
        assertEquals("Should parse delegate", EisenhowerQuadrant.DELEGATE, parsed?.quadrant)
    }
    
    @Test
    fun `fallback to keyword extraction`() {
        val response = """This task is definitely something to ELIMINATE. It's a time waster."""
        val parsed = PromptBuilder.parseResponse(response)
        
        assertNotNull("Should extract from keywords", parsed)
        assertEquals("Should find ELIMINATE keyword", EisenhowerQuadrant.ELIMINATE, parsed?.quadrant)
    }
    
    @Test
    fun `return null for unparseable response`() {
        val response = """I don't understand the question."""
        val parsed = PromptBuilder.parseResponse(response)
        
        assertNull("Should return null for garbage", parsed)
    }
    
    @Test
    fun `clamp confidence to valid range`() {
        val response = """{"quadrant": "DO", "confidence": 1.5, "reasoning": "test"}"""
        val parsed = PromptBuilder.parseResponse(response)
        
        assertNotNull(parsed)
        assertTrue("Confidence should be ≤ 1.0", (parsed?.confidence ?: 0f) <= 1.0f)
    }
    
    // =========================================================================
    // Phi-3 Template Tests
    // =========================================================================
    
    @Test
    fun `prompts use Phi-3 chat template`() {
        for (strategy in PromptStrategy.entries) {
            val prompt = PromptBuilder.buildPrompt(strategy, "Test")
            assertTrue(
                "Strategy $strategy should have user tag",
                prompt.contains("<|user|>")
            )
            assertTrue(
                "Strategy $strategy should have assistant tag",
                prompt.contains("<|assistant|>")
            )
        }
    }
    
    // =========================================================================
    // Extended Dataset Tests
    // =========================================================================
    
    @Test
    fun `extended dataset has 50 test cases`() {
        assertEquals("Should have 50 test cases", 50, ExtendedTestDataset.TEST_CASES_50.size)
    }
    
    @Test
    fun `extended dataset has balanced quadrants`() {
        val byQuadrant = ExtendedTestDataset.getByQuadrant()
        
        for (quadrant in EisenhowerQuadrant.entries) {
            val count = byQuadrant[quadrant]?.size ?: 0
            assertTrue(
                "Quadrant $quadrant should have at least 10 cases, got $count",
                count >= 10
            )
        }
    }
    
    @Test
    fun `all test cases have unique IDs`() {
        val ids = ExtendedTestDataset.TEST_CASES_50.map { it.id }
        assertEquals(
            "All IDs should be unique",
            ids.size,
            ids.toSet().size
        )
    }
    
    @Test
    fun `20-case subset matches first 20 of 50-case set`() {
        val first20 = ExtendedTestDataset.TEST_CASES_50.take(20)
        val test20 = ExtendedTestDataset.TEST_CASES_20
        
        assertEquals("20-case set should be first 20", first20, test20)
    }
}

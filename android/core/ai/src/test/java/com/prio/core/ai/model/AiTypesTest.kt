package com.prio.core.ai.model

import com.prio.core.common.model.EisenhowerQuadrant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for AI types serialization.
 */
class AiTypesTest {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    @Test
    fun `AiRequest serializes correctly`() {
        val request = AiRequest(
            type = AiRequestType.CLASSIFY_EISENHOWER,
            input = "Call dentist tomorrow at 2pm",
            context = AiContext(
                existingGoals = listOf("Improve health"),
                currentTime = "2026-02-04T09:00:00"
            )
        )
        
        val jsonString = json.encodeToString(request)
        assertTrue(jsonString.contains("CLASSIFY_EISENHOWER"))
        assertTrue(jsonString.contains("Call dentist"))
        
        val decoded = json.decodeFromString<AiRequest>(jsonString)
        assertEquals(request.type, decoded.type)
        assertEquals(request.input, decoded.input)
    }
    
    @Test
    fun `AiResponse with EisenhowerClassification serializes correctly`() {
        val result = AiResult.EisenhowerClassification(
            quadrant = EisenhowerQuadrant.SCHEDULE,
            confidence = 0.85f,
            explanation = "Health appointment with a specific time",
            isUrgent = false,
            isImportant = true
        )
        
        val response = AiResponse(
            success = true,
            result = result,
            metadata = AiResponseMetadata(
                provider = "rule-based",
                latencyMs = 45
            )
        )
        
        val jsonString = json.encodeToString(response)
        assertTrue(jsonString.contains("SCHEDULE"))
        assertTrue(jsonString.contains("Health appointment"))
        
        val decoded = json.decodeFromString<AiResponse>(jsonString)
        assertTrue(decoded.success)
        assertTrue(decoded.result is AiResult.EisenhowerClassification)
    }
    
    @Test
    fun `AiResult ParsedTask serializes correctly`() {
        val result = AiResult.ParsedTask(
            title = "Call dentist",
            dueDate = "2026-02-05",
            dueTime = "14:00",
            priority = "normal",
            suggestedQuadrant = EisenhowerQuadrant.SCHEDULE,
            confidence = 0.9f
        )
        
        val jsonString = json.encodeToString<AiResult>(result)
        assertTrue(jsonString.contains("Call dentist"))
        assertTrue(jsonString.contains("2026-02-05"))
    }
    
    @Test
    fun `AiRequestOptions has sensible defaults`() {
        val options = AiRequestOptions()
        
        assertEquals(256, options.maxTokens)
        assertEquals(0.3f, options.temperature)
        assertTrue(options.useLlm)
        assertTrue(options.fallbackToRuleBased)
    }
}

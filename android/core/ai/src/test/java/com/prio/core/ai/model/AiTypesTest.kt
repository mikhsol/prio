package com.prio.core.ai.model

import com.prio.core.common.model.EisenhowerQuadrant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested

/**
 * Unit tests for AI types serialization.
 * 
 * These tests verify that all AI types serialize/deserialize correctly
 * and match the backend API contract (snake_case JSON keys).
 */
class AiTypesTest {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    
    @Nested
    inner class AiRequestTests {
        
        @Test
        fun `AiRequest serializes with snake_case keys`() {
            val request = AiRequest(
                id = "test-123",
                type = AiRequestType.CLASSIFY_EISENHOWER,
                input = "Call dentist tomorrow at 2pm",
                context = AiContext(
                    existingGoals = listOf("Improve health"),
                    currentTime = "2026-02-04T09:00:00"
                )
            )
            
            val jsonString = json.encodeToString(request)
            
            // Verify snake_case keys
            assertTrue(jsonString.contains("\"type\""))
            assertTrue(jsonString.contains("\"classify_eisenhower\""))
            assertTrue(jsonString.contains("\"existing_goals\""))
            assertTrue(jsonString.contains("\"current_time\""))
            assertTrue(jsonString.contains("\"system_prompt\""))
            
            val decoded = json.decodeFromString<AiRequest>(jsonString)
            assertEquals(request.type, decoded.type)
            assertEquals(request.input, decoded.input)
            assertEquals(request.id, decoded.id)
        }
        
        @Test
        fun `AiRequest generates unique ID by default`() {
            val request1 = AiRequest(type = AiRequestType.PARSE_TASK, input = "test")
            val request2 = AiRequest(type = AiRequestType.PARSE_TASK, input = "test")
            
            assertNotEquals(request1.id, request2.id)
        }
        
        @Test
        fun `AiRequestType enum serializes with snake_case`() {
            val types = listOf(
                AiRequestType.CLASSIFY_EISENHOWER to "classify_eisenhower",
                AiRequestType.PARSE_TASK to "parse_task",
                AiRequestType.SUGGEST_SMART_GOAL to "suggest_smart_goal",
                AiRequestType.GENERATE_BRIEFING to "generate_briefing",
                AiRequestType.EXTRACT_ACTION_ITEMS to "extract_action_items",
                AiRequestType.SUMMARIZE to "summarize",
                AiRequestType.GENERAL_CHAT to "general_chat"
            )
            
            for ((enumValue, expectedJson) in types) {
                val jsonString = json.encodeToString(enumValue)
                assertTrue(jsonString.contains(expectedJson), 
                    "Expected $expectedJson in $jsonString")
            }
        }
    }
    
    @Nested
    inner class AiResponseTests {
        
        @Test
        fun `AiResponse with EisenhowerClassification serializes correctly`() {
            val result = AiResult.EisenhowerClassification(
                quadrant = EisenhowerQuadrant.SCHEDULE,
                confidence = 0.85f,
                explanation = "Health appointment with a specific time",
                isUrgent = false,
                isImportant = true,
                urgencySignals = listOf("tomorrow"),
                importanceSignals = listOf("health", "dentist")
            )
            
            val response = AiResponse(
                success = true,
                requestId = "req-123",
                result = result,
                metadata = AiResponseMetadata(
                    provider = "rule-based",
                    latencyMs = 45,
                    wasRuleBased = true
                )
            )
            
            val jsonString = json.encodeToString(response)
            
            // Verify snake_case keys
            assertTrue(jsonString.contains("\"request_id\""))
            assertTrue(jsonString.contains("\"is_urgent\""))
            assertTrue(jsonString.contains("\"is_important\""))
            assertTrue(jsonString.contains("\"urgency_signals\""))
            assertTrue(jsonString.contains("\"importance_signals\""))
            assertTrue(jsonString.contains("\"latency_ms\""))
            assertTrue(jsonString.contains("\"was_rule_based\""))
            
            val decoded = json.decodeFromString<AiResponse>(jsonString)
            assertTrue(decoded.success)
            assertTrue(decoded.result is AiResult.EisenhowerClassification)
            
            val decodedResult = decoded.result as AiResult.EisenhowerClassification
            assertEquals(EisenhowerQuadrant.SCHEDULE, decodedResult.quadrant)
            assertEquals(0.85f, decodedResult.confidence)
        }
        
        @Test
        fun `AiResponse with error serializes correctly`() {
            val response = AiResponse(
                success = false,
                requestId = "req-456",
                result = null,
                error = "Model not loaded",
                errorCode = "MODEL_NOT_AVAILABLE"
            )
            
            val jsonString = json.encodeToString(response)
            assertTrue(jsonString.contains("\"error_code\""))
            assertTrue(jsonString.contains("MODEL_NOT_AVAILABLE"))
            
            val decoded = json.decodeFromString<AiResponse>(jsonString)
            assertFalse(decoded.success)
            assertNull(decoded.result)
            assertEquals("Model not loaded", decoded.error)
        }
    }
    
    @Nested
    inner class AiResultTests {
        
        @Test
        fun `ParsedTask serializes correctly`() {
            val result = AiResult.ParsedTask(
                title = "Call dentist",
                dueDate = "2026-02-05",
                dueTime = "14:00",
                priority = "normal",
                suggestedQuadrant = EisenhowerQuadrant.SCHEDULE,
                recurrence = "weekly",
                tags = listOf("health", "personal"),
                confidence = 0.9f
            )
            
            val jsonString = json.encodeToString<AiResult>(result)
            assertTrue(jsonString.contains("\"due_date\""))
            assertTrue(jsonString.contains("\"due_time\""))
            assertTrue(jsonString.contains("\"suggested_quadrant\""))
            assertTrue(jsonString.contains("\"parsed_task\""))
        }
        
        @Test
        fun `SmartGoalSuggestion serializes correctly`() {
            val result = AiResult.SmartGoalSuggestion(
                refinedGoal = "Complete certification by Q2",
                specific = "Pass AWS Solutions Architect exam",
                measurable = "Score 80% or higher",
                achievable = "Study 2 hours daily for 3 months",
                relevant = "Supports career growth goal",
                timeBound = "Before June 30, 2026",
                suggestedMilestones = listOf("Complete course", "Take practice tests", "Schedule exam")
            )
            
            val jsonString = json.encodeToString<AiResult>(result)
            assertTrue(jsonString.contains("\"refined_goal\""))
            assertTrue(jsonString.contains("\"time_bound\""))
            assertTrue(jsonString.contains("\"suggested_milestones\""))
            assertTrue(jsonString.contains("\"smart_goal_suggestion\""))
        }
        
        @Test
        fun `BriefingContent serializes correctly`() {
            val result = AiResult.BriefingContent(
                greeting = "Good morning!",
                summary = "You have 5 tasks today",
                topPriorities = listOf("Submit report", "Team standup", "Review PR"),
                insights = listOf("You completed 80% of yesterday's tasks"),
                motivationalQuote = "The secret of getting ahead is getting started.",
                briefingType = "morning"
            )
            
            val jsonString = json.encodeToString<AiResult>(result)
            assertTrue(jsonString.contains("\"top_priorities\""))
            assertTrue(jsonString.contains("\"motivational_quote\""))
            assertTrue(jsonString.contains("\"briefing_type\""))
            assertTrue(jsonString.contains("\"briefing_content\""))
        }
        
        @Test
        fun `ExtractedActionItems serializes correctly`() {
            val result = AiResult.ExtractedActionItems(
                items = listOf(
                    AiResult.ActionItemResult(
                        description = "Send follow-up email",
                        assignee = "John",
                        dueDate = "2026-02-05",
                        priority = "high"
                    ),
                    AiResult.ActionItemResult(
                        description = "Update documentation",
                        assignee = null,
                        dueDate = null,
                        priority = "medium"
                    )
                )
            )
            
            val jsonString = json.encodeToString<AiResult>(result)
            assertTrue(jsonString.contains("\"extracted_action_items\""))
            assertTrue(jsonString.contains("\"due_date\""))
        }
        
        @Test
        fun `SummaryResult serializes correctly`() {
            val result = AiResult.SummaryResult(
                summary = "Meeting discussed Q1 goals and budget allocation.",
                keyPoints = listOf("Approved Q1 budget", "New hire starting March"),
                wordCount = 150
            )
            
            val jsonString = json.encodeToString<AiResult>(result)
            assertTrue(jsonString.contains("\"key_points\""))
            assertTrue(jsonString.contains("\"word_count\""))
            assertTrue(jsonString.contains("\"summary\""))
        }
    }
    
    @Nested
    inner class OptionsAndMetadataTests {
        
        @Test
        fun `AiRequestOptions has sensible defaults`() {
            val options = AiRequestOptions()
            
            assertEquals(256, options.maxTokens)
            assertEquals(0.3f, options.temperature)
            assertEquals(0.9f, options.topP)
            assertTrue(options.useLlm)
            assertTrue(options.fallbackToRuleBased)
            assertEquals(0.7f, options.minConfidence)
            assertTrue(options.stopSequences.isEmpty())
        }
        
        @Test
        fun `AiRequestOptions serializes with snake_case`() {
            val options = AiRequestOptions(
                maxTokens = 512,
                temperature = 0.5f,
                useLlm = false,
                minConfidence = 0.8f
            )
            
            val jsonString = json.encodeToString(options)
            assertTrue(jsonString.contains("\"max_tokens\""))
            assertTrue(jsonString.contains("\"use_llm\""))
            assertTrue(jsonString.contains("\"fallback_to_rule_based\""))
            assertTrue(jsonString.contains("\"min_confidence\""))
            assertTrue(jsonString.contains("\"stop_sequences\""))
        }
        
        @Test
        fun `AiResponseMetadata serializes correctly`() {
            val metadata = AiResponseMetadata(
                provider = "on-device",
                model = "phi-3-mini",
                latencyMs = 2500,
                tokensUsed = 150,
                promptTokens = 100,
                completionTokens = 50,
                wasRuleBased = false,
                wasLlmFallback = false,
                confidenceScore = 0.85f
            )
            
            val jsonString = json.encodeToString(metadata)
            assertTrue(jsonString.contains("\"latency_ms\""))
            assertTrue(jsonString.contains("\"tokens_used\""))
            assertTrue(jsonString.contains("\"prompt_tokens\""))
            assertTrue(jsonString.contains("\"completion_tokens\""))
            assertTrue(jsonString.contains("\"was_rule_based\""))
            assertTrue(jsonString.contains("\"confidence_score\""))
        }
        
        @Test
        fun `TokenUsage serializes correctly`() {
            val usage = TokenUsage(
                promptTokens = 100,
                completionTokens = 50,
                totalTokens = 150,
                estimatedCostUsd = 0.0015f
            )
            
            val jsonString = json.encodeToString(usage)
            assertTrue(jsonString.contains("\"prompt_tokens\""))
            assertTrue(jsonString.contains("\"completion_tokens\""))
            assertTrue(jsonString.contains("\"total_tokens\""))
            assertTrue(jsonString.contains("\"estimated_cost_usd\""))
        }
        
        @Test
        fun `RequestMetadata serializes correctly`() {
            val metadata = RequestMetadata(
                clientVersion = "1.0.0",
                preferredProvider = "on-device",
                allowCloudFallback = false,
                deviceTier = 2
            )
            
            val jsonString = json.encodeToString(metadata)
            assertTrue(jsonString.contains("\"client_version\""))
            assertTrue(jsonString.contains("\"requested_at\""))
            assertTrue(jsonString.contains("\"preferred_provider\""))
            assertTrue(jsonString.contains("\"allow_cloud_fallback\""))
            assertTrue(jsonString.contains("\"device_tier\""))
            assertTrue(jsonString.contains("\"request_id\""))
        }
    }
    
    @Nested
    inner class StreamingTests {
        
        @Test
        fun `AiStreamChunk serializes correctly`() {
            val chunk = AiStreamChunk(
                text = "The task",
                isComplete = false,
                tokenIndex = 5,
                metadata = AiStreamMetadata(
                    tokensGenerated = 5,
                    generationTimeMs = 100,
                    tokensPerSecond = 50f
                )
            )
            
            val jsonString = json.encodeToString(chunk)
            assertTrue(jsonString.contains("\"is_complete\""))
            assertTrue(jsonString.contains("\"token_index\""))
            assertTrue(jsonString.contains("\"tokens_generated\""))
            assertTrue(jsonString.contains("\"generation_time_ms\""))
            assertTrue(jsonString.contains("\"tokens_per_second\""))
        }
        
        @Test
        fun `AiStreamChunk final chunk`() {
            val finalChunk = AiStreamChunk(
                text = "classified as DO.",
                isComplete = true,
                tokenIndex = 10
            )
            
            assertTrue(finalChunk.isComplete)
            assertNull(finalChunk.metadata)
        }
    }
}

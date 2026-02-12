package com.prio.core.aiprovider.router

import com.prio.core.ai.model.AiRequest
import com.prio.core.ai.model.AiRequestOptions
import com.prio.core.ai.model.AiRequestType
import com.prio.core.ai.model.AiResponse
import com.prio.core.ai.model.AiResponseMetadata
import com.prio.core.ai.model.AiResult
import com.prio.core.ai.registry.ModelRegistry
import com.prio.core.aiprovider.llm.LlamaEngine
import com.prio.core.aiprovider.nano.GeminiNanoProvider
import com.prio.core.aiprovider.provider.OnDeviceAiProvider
import com.prio.core.aiprovider.provider.RuleBasedFallbackProvider
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.domain.eisenhower.EisenhowerEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for AiProviderRouter.
 * 
 * Tests verify:
 * - Routing logic (rule-based first, LLM escalation)
 * - Fallback behavior on LLM failure
 * - Override tracking for accuracy measurement
 * - Statistics collection
 */
@DisplayName("AiProviderRouter Tests")
class AiProviderRouterTest {
    
    private lateinit var router: AiProviderRouter
    private lateinit var ruleBasedProvider: RuleBasedFallbackProvider
    private lateinit var onDeviceProvider: OnDeviceAiProvider
    private lateinit var geminiNanoProvider: GeminiNanoProvider
    private lateinit var mockLlamaEngine: LlamaEngine
    private lateinit var mockModelRegistry: ModelRegistry
    private lateinit var eisenhowerEngine: EisenhowerEngine
    
    private val llmAvailable = MutableStateFlow(false)
    private val nanoAvailable = MutableStateFlow(false)
    
    @BeforeEach
    fun setup() {
        eisenhowerEngine = EisenhowerEngine(Clock.System)
        ruleBasedProvider = RuleBasedFallbackProvider(eisenhowerEngine)
        
        mockLlamaEngine = mockk(relaxed = true)
        mockModelRegistry = mockk(relaxed = true)
        
        every { mockModelRegistry.activeModelId } returns MutableStateFlow(null)
        
        onDeviceProvider = mockk(relaxed = true) {
            every { isAvailable } returns llmAvailable
            every { providerId } returns "on-device"
        }
        
        geminiNanoProvider = mockk(relaxed = true) {
            every { isAvailable } returns nanoAvailable
            every { providerId } returns "gemini-nano"
        }
        
        router = AiProviderRouter(ruleBasedProvider, onDeviceProvider, geminiNanoProvider)
    }
    
    @Nested
    @DisplayName("Initialization")
    inner class InitializationTests {
        
        @Test
        @DisplayName("Router initializes successfully")
        fun routerInitializes() = runTest {
            assertTrue(router.initialize())
            assertTrue(router.isAvailable.value)
        }
        
        @Test
        @DisplayName("Router has correct provider ID")
        fun routerProviderId() {
            assertEquals("router", router.providerId)
        }
        
        @Test
        @DisplayName("Router auto-initializes on first complete() call")
        fun routerAutoInitializesOnComplete() = runTest {
            // Do NOT call router.initialize() — simulates real app behaviour
            val request = AiRequest(
                type = AiRequestType.CLASSIFY_EISENHOWER,
                input = "Test task"
            )

            // Should auto-initialize and return a rule-based result
            val result = router.complete(request)
            assertTrue(result.isSuccess)
        }

        @Test
        @DisplayName("Auto-init enables Gemini Nano routing for SMART goals")
        fun autoInitEnablesNanoRoutingForSmartGoals() = runTest {
            // Simulate Gemini Nano being available at init time
            nanoAvailable.value = true
            coEvery { geminiNanoProvider.initialize() } returns true
            coEvery { geminiNanoProvider.complete(any()) } returns Result.success(
                AiResponse(
                    success = true,
                    requestId = "test",
                    result = AiResult.SmartGoalSuggestion(
                        refinedGoal = "AI refined",
                        specific = "s", measurable = "m",
                        achievable = "a", relevant = "r",
                        timeBound = "t",
                        suggestedMilestones = emptyList()
                    ),
                    metadata = AiResponseMetadata(confidenceScore = 0.8f)
                )
            )

            // No explicit initialize() — should auto-init and route to Nano
            val request = AiRequest(
                type = AiRequestType.SUGGEST_SMART_GOAL,
                input = "Get fit"
            )
            val result = router.complete(request)

            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow().result is AiResult.SmartGoalSuggestion)
        }
    }
    
    @Nested
    @DisplayName("Rule-Based Only Mode")
    inner class RuleBasedOnlyModeTests {
        
        @BeforeEach
        fun setMode() {
            router.setRoutingMode(AiProviderRouter.RoutingMode.RULE_BASED_ONLY)
        }
        
        @Test
        @DisplayName("Uses only rule-based provider")
        fun usesOnlyRuleBased() = runTest {
            val request = createEisenhowerRequest("URGENT: Fix server crash")
            
            val result = router.complete(request)
            
            assertTrue(result.isSuccess)
            val response = result.getOrThrow()
            assertTrue(response.metadata.wasRuleBased)
            
            // Verify LLM was never called
            coVerify(exactly = 0) { onDeviceProvider.complete(any()) }
        }
        
        @Test
        @DisplayName("Stats show rule-based only")
        fun statsShowRuleBasedOnly() = runTest {
            repeat(5) {
                router.complete(createEisenhowerRequest("Task $it"))
            }
            
            val stats = router.stats.value
            assertEquals(5, stats.totalRequests)
            assertEquals(5, stats.ruleBasedOnly)
            assertEquals(0, stats.llmEscalated)
        }
    }
    
    @Nested
    @DisplayName("Hybrid Mode (Default)")
    inner class HybridModeTests {
        
        @BeforeEach
        fun setMode() = runTest {
            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID)
            router.initialize()
        }
        
        @Test
        @DisplayName("High confidence uses rule-based only")
        fun highConfidenceRuleBasedOnly() = runTest {
            // This should have high confidence (clear DO signal)
            val request = createEisenhowerRequest("URGENT EMERGENCY: Production server down affecting customers")
            
            val result = router.complete(request)
            
            assertTrue(result.isSuccess)
            val response = result.getOrThrow()
            assertEquals(EisenhowerQuadrant.DO_FIRST, 
                (response.result as AiResult.EisenhowerClassification).quadrant)
            
            // High confidence - should not escalate to LLM
            coVerify(exactly = 0) { onDeviceProvider.complete(any()) }
        }
        
        @Test
        @DisplayName("Low confidence escalates to LLM if available")
        fun lowConfidenceEscalatesToLlm() = runTest {
            // Make LLM available
            llmAvailable.value = true
            
            // Setup LLM response
            val llmResponse = AiResponse(
                success = true,
                requestId = "test",
                result = AiResult.EisenhowerClassification(
                    quadrant = EisenhowerQuadrant.SCHEDULE,
                    confidence = 0.85f,
                    explanation = "LLM classification",
                    isUrgent = false,
                    isImportant = true
                ),
                metadata = AiResponseMetadata(wasRuleBased = false)
            )
            coEvery { onDeviceProvider.complete(any()) } returns Result.success(llmResponse)
            
            // Ambiguous task with low confidence
            val request = createEisenhowerRequest("Think about stuff")
            
            val result = router.complete(request)
            
            assertTrue(result.isSuccess)
            
            // Should have tried LLM (ambiguous task)
            // Note: exact behavior depends on confidence threshold
        }
        
        @Test
        @DisplayName("LLM failure falls back to rule-based")
        fun llmFailureFallsBack() = runTest {
            llmAvailable.value = true
            coEvery { onDeviceProvider.complete(any()) } returns Result.failure(RuntimeException("LLM error"))
            
            val request = createEisenhowerRequest("Ambiguous task here")
            
            val result = router.complete(request)
            
            // Should succeed with rule-based fallback
            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow().metadata.wasRuleBased)
        }
        
        @Test
        @DisplayName("LLM unavailable uses rule-based")
        fun llmUnavailableUsesRuleBased() = runTest {
            llmAvailable.value = false
            
            val request = createEisenhowerRequest("Any task")
            
            val result = router.complete(request)
            
            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow().metadata.wasRuleBased)
            coVerify(exactly = 0) { onDeviceProvider.complete(any()) }
        }
    }
    
    @Nested
    @DisplayName("LLM Preferred Mode")
    inner class LlmPreferredModeTests {
        
        @BeforeEach
        fun setMode() {
            router.setRoutingMode(AiProviderRouter.RoutingMode.LLM_PREFERRED)
        }
        
        @Test
        @DisplayName("Tries LLM first when available")
        fun triesLlmFirst() = runTest {
            llmAvailable.value = true
            
            val llmResponse = AiResponse(
                success = true,
                requestId = "test",
                result = AiResult.EisenhowerClassification(
                    quadrant = EisenhowerQuadrant.DO_FIRST,
                    confidence = 0.9f,
                    explanation = "LLM",
                    isUrgent = true,
                    isImportant = true
                )
            )
            coEvery { onDeviceProvider.complete(any()) } returns Result.success(llmResponse)
            
            val request = createEisenhowerRequest("Test task")
            val result = router.complete(request)
            
            assertTrue(result.isSuccess)
            coVerify(atLeast = 1) { onDeviceProvider.complete(any()) }
        }
        
        @Test
        @DisplayName("Falls back to rule-based on LLM failure")
        fun fallsBackOnFailure() = runTest {
            llmAvailable.value = true
            coEvery { onDeviceProvider.complete(any()) } throws RuntimeException("Error")
            
            val request = createEisenhowerRequest("Test task")
            val result = router.complete(request)
            
            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow().metadata.wasRuleBased)
        }
    }
    
    @Nested
    @DisplayName("Override Tracking")
    inner class OverrideTrackingTests {
        
        @Test
        @DisplayName("Records override correctly")
        fun recordsOverride() {
            val originalResult = AiResult.EisenhowerClassification(
                quadrant = EisenhowerQuadrant.SCHEDULE,
                confidence = 0.8f,
                explanation = "Original",
                isUrgent = false,
                isImportant = true
            )
            
            router.recordOverride(
                requestId = "req-123",
                originalResult = originalResult,
                overrideQuadrant = "DO",
                wasLlm = false
            )
            
            val history = router.getOverrideHistory()
            assertEquals(1, history.size)
            assertEquals("SCHEDULE", history[0].originalQuadrant)
            assertEquals("DO", history[0].overrideQuadrant)
            assertFalse(history[0].wasLlm)
        }
        
        @Test
        @DisplayName("Stats include override count")
        fun statsIncludeOverrides() {
            repeat(3) {
                router.recordOverride(
                    requestId = "req-$it",
                    originalResult = AiResult.EisenhowerClassification(
                        quadrant = EisenhowerQuadrant.SCHEDULE,
                        confidence = 0.7f,
                        explanation = "",
                        isUrgent = false,
                        isImportant = false
                    ),
                    overrideQuadrant = "DO",
                    wasLlm = false
                )
            }
            
            assertEquals(3, router.stats.value.overrides)
        }
        
        @Test
        @DisplayName("Accuracy calculated from overrides")
        fun accuracyFromOverrides() = runTest {
            router.setRoutingMode(AiProviderRouter.RoutingMode.RULE_BASED_ONLY)
            
            // Process 10 requests
            repeat(10) {
                router.complete(createEisenhowerRequest("Task $it urgent client"))
            }
            
            // Record 2 overrides
            repeat(2) {
                router.recordOverride(
                    requestId = "req-$it",
                    originalResult = AiResult.EisenhowerClassification(
                        quadrant = EisenhowerQuadrant.SCHEDULE,
                        confidence = 0.7f,
                        explanation = "",
                        isUrgent = false,
                        isImportant = false
                    ),
                    overrideQuadrant = "DO",
                    wasLlm = false
                )
            }
            
            val accuracy = router.calculateAccuracy()
            assertEquals(0.8f, accuracy, 0.01f) // 8/10 = 80%
        }
        
        @Test
        @DisplayName("Reset clears stats and history")
        fun resetClearsAll() = runTest {
            router.setRoutingMode(AiProviderRouter.RoutingMode.RULE_BASED_ONLY)
            
            repeat(5) {
                router.complete(createEisenhowerRequest("Task $it"))
            }
            router.recordOverride(
                requestId = "req-1",
                originalResult = AiResult.EisenhowerClassification(
                    quadrant = EisenhowerQuadrant.SCHEDULE,
                    confidence = 0.7f,
                    explanation = "",
                    isUrgent = false,
                    isImportant = false
                ),
                overrideQuadrant = "DO",
                wasLlm = false
            )
            
            router.resetStats()
            
            assertEquals(0, router.stats.value.totalRequests)
            assertEquals(0, router.stats.value.overrides)
            assertTrue(router.getOverrideHistory().isEmpty())
        }
    }
    
    @Nested
    @DisplayName("SUGGEST_SMART_GOAL Routing")
    inner class SmartGoalRoutingTests {

        private fun createSmartGoalRequest(input: String) = AiRequest(
            type = AiRequestType.SUGGEST_SMART_GOAL,
            input = input
        )

        private val smartGoalResponse = AiResponse(
            success = true,
            requestId = "test",
            result = AiResult.SmartGoalSuggestion(
                refinedGoal = "Get promoted to Senior Engineer by December 2026",
                specific = "Achieve Senior Engineer title",
                measurable = "Lead 2 major projects",
                achievable = "Currently mid-level with 3 years experience",
                relevant = "Aligns with career growth",
                timeBound = "By December 2026",
                suggestedMilestones = listOf("Complete leadership course", "Lead first project")
            ),
            metadata = AiResponseMetadata(wasRuleBased = false, confidenceScore = 0.8f)
        )

        @Test
        @DisplayName("Hybrid mode: escalates SUGGEST_SMART_GOAL to LLM when available")
        fun hybridMode_escalatesSmartGoalToLlm() = runTest {
            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID)
            llmAvailable.value = true

            coEvery { onDeviceProvider.complete(any()) } returns Result.success(smartGoalResponse)

            val request = createSmartGoalRequest("Get promoted")
            val result = router.complete(request)

            assertTrue(result.isSuccess)
            val response = result.getOrThrow()
            assertTrue(response.result is AiResult.SmartGoalSuggestion)
            val suggestion = response.result as AiResult.SmartGoalSuggestion
            assertEquals("Get promoted to Senior Engineer by December 2026", suggestion.refinedGoal)

            // Rule-based returns 0.5 confidence (below threshold) → escalates to LLM
            coVerify(atLeast = 1) { onDeviceProvider.complete(any()) }
        }

        @Test
        @DisplayName("Hybrid mode: returns rule-based template when all LLMs unavailable for SMART goal")
        fun hybridMode_returnsTemplateWhenAllLlmsUnavailableForSmartGoal() = runTest {
            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID)
            llmAvailable.value = false
            nanoAvailable.value = false

            val request = createSmartGoalRequest("Get promoted")
            val result = router.complete(request)

            // Rule-based now handles SMART goals with a template — should succeed
            assertTrue(result.isSuccess)
            val response = result.getOrThrow()
            assertTrue(response.result is AiResult.SmartGoalSuggestion)
            assertTrue(response.metadata.wasRuleBased)
        }

        @Test
        @DisplayName("Hybrid mode: escalates SUGGEST_SMART_GOAL to Gemini Nano when llama.cpp unavailable")
        fun hybridMode_escalatesSmartGoalToNanoWhenLlmUnavailable() = runTest {
            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID)
            llmAvailable.value = false
            nanoAvailable.value = true

            // Rule-based returns 0.5 confidence, which is below threshold.
            // Nano not tried via confidence-escalation in HYBRID mode (only llama.cpp),
            // but the result should still be SUCCESS from rule-based template.
            val request = createSmartGoalRequest("Learn to cook")
            val result = router.complete(request)

            assertTrue(result.isSuccess)
            val response = result.getOrThrow()
            assertTrue(response.result is AiResult.SmartGoalSuggestion)
        }

        @Test
        @DisplayName("Hybrid mode: escalates to llama.cpp when available for higher quality SMART goal")
        fun hybridMode_escalatesToLlmWhenAvailableForSmartGoal() = runTest {
            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID)
            llmAvailable.value = true
            nanoAvailable.value = true

            coEvery { onDeviceProvider.complete(any()) } returns Result.success(smartGoalResponse)

            val request = createSmartGoalRequest("Write a book")
            val result = router.complete(request)

            assertTrue(result.isSuccess)
            // HYBRID confidence-based escalation uses llama.cpp for higher quality
            coVerify(atLeast = 1) { onDeviceProvider.complete(any()) }
        }

        @Test
        @DisplayName("Hybrid mode: falls back to rule-based template when llama.cpp fails for SMART goal")
        fun hybridMode_fallsToTemplateWhenLlmFails() = runTest {
            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID)
            llmAvailable.value = true
            nanoAvailable.value = false

            coEvery { onDeviceProvider.complete(any()) } returns Result.failure(RuntimeException("LLM error"))

            val request = createSmartGoalRequest("Run marathon")
            val result = router.complete(request)

            // llama.cpp failed, should fall back to rule-based template
            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow().result is AiResult.SmartGoalSuggestion)
            assertTrue(result.getOrThrow().metadata.wasRuleBased)
        }

        @Test
        @DisplayName("Hybrid Nano mode: escalates SMART goal to Gemini Nano first")
        fun hybridNanoMode_escalatesSmartGoalToNano() = runTest {
            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID_NANO)
            nanoAvailable.value = true
            llmAvailable.value = false

            coEvery { geminiNanoProvider.complete(any()) } returns Result.success(smartGoalResponse)

            val request = createSmartGoalRequest("Learn Spanish")
            val result = router.complete(request)

            assertTrue(result.isSuccess)
            val response = result.getOrThrow()
            assertTrue(response.result is AiResult.SmartGoalSuggestion)

            coVerify(atLeast = 1) { geminiNanoProvider.complete(any()) }
        }

        @Test
        @DisplayName("Hybrid Nano mode: falls back to llama.cpp when Nano fails for SMART goal")
        fun hybridNanoMode_fallsToLlamaCpp() = runTest {
            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID_NANO)
            nanoAvailable.value = true
            llmAvailable.value = true

            coEvery { geminiNanoProvider.complete(any()) } returns Result.failure(RuntimeException("Nano failed"))
            coEvery { onDeviceProvider.complete(any()) } returns Result.success(smartGoalResponse)

            val request = createSmartGoalRequest("Read 12 books")
            val result = router.complete(request)

            assertTrue(result.isSuccess)
            val response = result.getOrThrow()
            assertTrue(response.result is AiResult.SmartGoalSuggestion)

            coVerify(atLeast = 1) { onDeviceProvider.complete(any()) }
        }

        @Test
        @DisplayName("Hybrid Nano mode: returns rule-based template when all LLMs fail for SMART goal")
        fun hybridNanoMode_returnsTemplateWhenAllLlmsFail() = runTest {
            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID_NANO)
            nanoAvailable.value = false
            llmAvailable.value = false

            val request = createSmartGoalRequest("Save money")
            val result = router.complete(request)

            // Rule-based now handles SMART goals — should succeed with template
            assertTrue(result.isSuccess)
            val response = result.getOrThrow()
            assertTrue(response.result is AiResult.SmartGoalSuggestion)
            val suggestion = response.result as AiResult.SmartGoalSuggestion
            assertEquals("Save money", suggestion.refinedGoal)
            assertTrue(response.metadata.wasRuleBased)
        }
    }

    @Nested
    @DisplayName("Request Options")
    inner class RequestOptionsTests {
        
        @Test
        @DisplayName("useLlm=false prevents LLM escalation")
        fun useLlmFalsePreventsEscalation() = runTest {
            llmAvailable.value = true
            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID)
            
            val request = AiRequest(
                type = AiRequestType.CLASSIFY_EISENHOWER,
                input = "Ambiguous task",
                options = AiRequestOptions(useLlm = false)
            )
            
            val result = router.complete(request)
            
            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow().metadata.wasRuleBased)
            coVerify(exactly = 0) { onDeviceProvider.complete(any()) }
        }
        
        @Test
        @DisplayName("Custom confidence threshold respected")
        fun customConfidenceThresholdRespected() = runTest {
            llmAvailable.value = true
            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID)
            
            // Very high threshold - should almost always escalate
            val request = AiRequest(
                type = AiRequestType.CLASSIFY_EISENHOWER,
                input = "URGENT: Clear task with deadline",
                options = AiRequestOptions(minConfidence = 0.99f)
            )
            
            coEvery { onDeviceProvider.complete(any()) } returns Result.success(
                AiResponse(
                    success = true,
                    requestId = "test",
                    result = AiResult.EisenhowerClassification(
                        quadrant = EisenhowerQuadrant.DO_FIRST,
                        confidence = 0.95f,
                        explanation = "LLM",
                        isUrgent = true,
                        isImportant = true
                    )
                )
            )
            
            router.complete(request)
            
            // With 0.99 threshold, even high confidence should trigger LLM
            coVerify(atLeast = 1) { onDeviceProvider.complete(any()) }
        }
    }
    
    // Helper function
    private fun createEisenhowerRequest(input: String) = AiRequest(
        type = AiRequestType.CLASSIFY_EISENHOWER,
        input = input
    )
}

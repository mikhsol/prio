package com.prio.core.aiprovider.router

import com.prio.core.ai.model.AiRequest
import com.prio.core.ai.model.AiRequestOptions
import com.prio.core.ai.model.AiRequestType
import com.prio.core.ai.model.AiResponse
import com.prio.core.ai.model.AiResponseMetadata
import com.prio.core.ai.model.AiResult
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Task 3.6.10: Integration tests for HYBRID_NANO routing mode.
 *
 * Verifies the 3-tier fallback chain introduced in Milestone 3.6:
 *   1. Rule-based (<50ms) — if confidence ≥ threshold, done
 *   2. Gemini Nano (~1-2s) — if AI Core available
 *   3. llama.cpp (~2-3s)   — fallback LLM
 *
 * Also tests auto-selection of HYBRID_NANO mode during initialization
 * and the registration of GeminiNanoProvider in the router.
 */
@DisplayName("HYBRID_NANO Routing Integration")
class HybridNanoRoutingTest {

    private lateinit var router: AiProviderRouter
    private lateinit var ruleBasedProvider: RuleBasedFallbackProvider
    private lateinit var onDeviceProvider: OnDeviceAiProvider
    private lateinit var geminiNanoProvider: GeminiNanoProvider
    private lateinit var eisenhowerEngine: EisenhowerEngine

    private val llmAvailable = MutableStateFlow(false)
    private val nanoAvailable = MutableStateFlow(false)

    private val nanoResponse = AiResponse(
        success = true,
        requestId = "nano-test",
        result = AiResult.EisenhowerClassification(
            quadrant = EisenhowerQuadrant.DO_FIRST,
            confidence = 0.88f,
            explanation = "Gemini Nano classification",
            isUrgent = true,
            isImportant = true
        ),
        metadata = AiResponseMetadata(
            provider = "gemini-nano",
            model = "gemini-nano",
            wasRuleBased = false
        )
    )

    private val llmResponse = AiResponse(
        success = true,
        requestId = "llm-test",
        result = AiResult.EisenhowerClassification(
            quadrant = EisenhowerQuadrant.SCHEDULE,
            confidence = 0.85f,
            explanation = "llama.cpp classification",
            isUrgent = false,
            isImportant = true
        ),
        metadata = AiResponseMetadata(
            provider = "on-device",
            model = "phi-3",
            wasRuleBased = false
        )
    )

    @BeforeEach
    fun setup() {
        eisenhowerEngine = EisenhowerEngine(Clock.System)
        ruleBasedProvider = RuleBasedFallbackProvider(eisenhowerEngine)

        onDeviceProvider = mockk(relaxed = true) {
            every { isAvailable } returns llmAvailable
            every { providerId } returns "on-device"
        }

        geminiNanoProvider = mockk(relaxed = true) {
            every { isAvailable } returns nanoAvailable
            every { providerId } returns "gemini-nano"
        }

        router = AiProviderRouter(ruleBasedProvider, onDeviceProvider, geminiNanoProvider)
        router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID_NANO)
    }

    private fun createRequest(input: String) = AiRequest(
        type = AiRequestType.CLASSIFY_EISENHOWER,
        input = input
    )

    private fun createAmbiguousRequest(input: String = "Think about some stuff") = AiRequest(
        type = AiRequestType.CLASSIFY_EISENHOWER,
        input = input,
        options = AiRequestOptions(minConfidence = 0.99f) // Force escalation
    )

    // =========================================================================
    // Auto-selection during initialization
    // =========================================================================

    @Nested
    @DisplayName("Auto-selection on init")
    inner class AutoSelectionTests {

        @Test
        fun `auto-selects HYBRID_NANO when Gemini Nano initializes successfully`() = runTest {
            coEvery { geminiNanoProvider.initialize() } returns true

            // Start with default HYBRID
            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID)

            router.initialize()

            assertEquals(AiProviderRouter.RoutingMode.HYBRID_NANO, router.routingMode.value)
        }

        @Test
        fun `stays on HYBRID when Gemini Nano initialization fails`() = runTest {
            coEvery { geminiNanoProvider.initialize() } returns false

            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID)
            router.initialize()

            assertEquals(AiProviderRouter.RoutingMode.HYBRID, router.routingMode.value)
        }

        @Test
        fun `stays on HYBRID when Gemini Nano throws exception`() = runTest {
            coEvery { geminiNanoProvider.initialize() } throws RuntimeException("AI Core missing")

            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID)
            router.initialize()

            assertEquals(AiProviderRouter.RoutingMode.HYBRID, router.routingMode.value)
        }
    }

    // =========================================================================
    // 3-tier fallback chain
    // =========================================================================

    @Nested
    @DisplayName("Fallback chain")
    inner class FallbackChainTests {

        @Test
        fun `high-confidence rule-based returns immediately without LLM`() = runTest {
            nanoAvailable.value = true
            llmAvailable.value = true

            // Clear urgent signal → high confidence rule-based
            val request = createRequest("URGENT EMERGENCY: Production server down NOW!")
            val result = router.complete(request)

            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow().metadata.wasRuleBased)
            coVerify(exactly = 0) { geminiNanoProvider.complete(any()) }
            coVerify(exactly = 0) { onDeviceProvider.complete(any()) }
        }

        @Test
        fun `low-confidence escalates to Gemini Nano when available`() = runTest {
            nanoAvailable.value = true
            coEvery { geminiNanoProvider.complete(any()) } returns Result.success(nanoResponse)

            val request = createAmbiguousRequest()
            val result = router.complete(request)

            assertTrue(result.isSuccess)
            coVerify(atLeast = 1) { geminiNanoProvider.complete(any()) }
        }

        @Test
        fun `falls to llama_cpp when Gemini Nano fails`() = runTest {
            nanoAvailable.value = true
            llmAvailable.value = true
            coEvery { geminiNanoProvider.complete(any()) } returns Result.failure(RuntimeException("Nano error"))
            coEvery { onDeviceProvider.complete(any()) } returns Result.success(llmResponse)

            val request = createAmbiguousRequest()
            val result = router.complete(request)

            assertTrue(result.isSuccess)
            coVerify(atLeast = 1) { onDeviceProvider.complete(any()) }
        }

        @Test
        fun `falls to llama_cpp when Gemini Nano unavailable`() = runTest {
            nanoAvailable.value = false
            llmAvailable.value = true
            coEvery { onDeviceProvider.complete(any()) } returns Result.success(llmResponse)

            val request = createAmbiguousRequest()
            val result = router.complete(request)

            assertTrue(result.isSuccess)
            coVerify(exactly = 0) { geminiNanoProvider.complete(any()) }
            coVerify(atLeast = 1) { onDeviceProvider.complete(any()) }
        }

        @Test
        fun `falls to rule-based when ALL LLMs fail`() = runTest {
            nanoAvailable.value = true
            llmAvailable.value = true
            coEvery { geminiNanoProvider.complete(any()) } returns Result.failure(RuntimeException("Nano error"))
            coEvery { onDeviceProvider.complete(any()) } returns Result.failure(RuntimeException("LLM error"))

            val request = createAmbiguousRequest()
            val result = router.complete(request)

            // Should still succeed with rule-based fallback
            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow().metadata.wasRuleBased)
        }

        @Test
        fun `falls to rule-based when ALL LLMs unavailable`() = runTest {
            nanoAvailable.value = false
            llmAvailable.value = false

            val request = createAmbiguousRequest()
            val result = router.complete(request)

            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow().metadata.wasRuleBased)
        }
    }

    // =========================================================================
    // Provider registration & routing mode
    // =========================================================================

    @Nested
    @DisplayName("Provider registration")
    inner class ProviderRegistration {

        @Test
        fun `router accepts GeminiNanoProvider in constructor`() {
            // This test simply verifies the 3-param constructor works
            val r = AiProviderRouter(ruleBasedProvider, onDeviceProvider, geminiNanoProvider)
            assertEquals("router", r.providerId)
        }

        @Test
        fun `HYBRID_NANO mode is selectable`() {
            router.setRoutingMode(AiProviderRouter.RoutingMode.HYBRID_NANO)
            assertEquals(AiProviderRouter.RoutingMode.HYBRID_NANO, router.routingMode.value)
        }

        @Test
        fun `switching away from HYBRID_NANO skips Gemini Nano`() = runTest {
            nanoAvailable.value = true
            coEvery { geminiNanoProvider.complete(any()) } returns Result.success(nanoResponse)

            router.setRoutingMode(AiProviderRouter.RoutingMode.RULE_BASED_ONLY)
            val request = createRequest("Some task")
            router.complete(request)

            coVerify(exactly = 0) { geminiNanoProvider.complete(any()) }
        }
    }

    // =========================================================================
    // Statistics tracking
    // =========================================================================

    @Nested
    @DisplayName("Stats tracking in HYBRID_NANO")
    inner class StatsTracking {

        @Test
        fun `successful Nano escalation counted as llmEscalated`() = runTest {
            nanoAvailable.value = true
            coEvery { geminiNanoProvider.complete(any()) } returns Result.success(nanoResponse)

            router.complete(createAmbiguousRequest())

            val stats = router.stats.value
            assertEquals(1, stats.totalRequests)
            assertTrue(stats.llmEscalated >= 1)
        }

        @Test
        fun `all LLMs failing counted as llmFailed`() = runTest {
            nanoAvailable.value = true
            llmAvailable.value = true
            coEvery { geminiNanoProvider.complete(any()) } throws RuntimeException()
            coEvery { onDeviceProvider.complete(any()) } throws RuntimeException()

            router.complete(createAmbiguousRequest())

            val stats = router.stats.value
            assertTrue(stats.llmFailed >= 1)
        }
    }

    // =========================================================================
    // Release / cleanup
    // =========================================================================

    @Nested
    @DisplayName("Lifecycle")
    inner class LifecycleTests {

        @Test
        fun `release delegates to GeminiNanoProvider`() = runTest {
            router.release()
            coVerify { geminiNanoProvider.release() }
        }

        @Test
        fun `release also delegates to OnDeviceProvider`() = runTest {
            router.release()
            coVerify { onDeviceProvider.release() }
        }
    }
}

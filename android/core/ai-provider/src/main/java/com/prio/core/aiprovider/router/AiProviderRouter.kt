package com.prio.core.aiprovider.router

import com.prio.core.ai.model.AiRequest
import com.prio.core.ai.model.AiRequestType
import com.prio.core.ai.model.AiResponse
import com.prio.core.ai.model.AiResponseMetadata
import com.prio.core.ai.model.AiResult
import com.prio.core.ai.model.AiStreamChunk
import com.prio.core.ai.provider.AiCapability
import com.prio.core.ai.provider.AiProvider
import com.prio.core.ai.provider.ModelInfo
import com.prio.core.aiprovider.nano.GeminiNanoProvider
import com.prio.core.aiprovider.provider.OnDeviceAiProvider
import com.prio.core.aiprovider.provider.RuleBasedFallbackProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Provider Router that intelligently routes requests between providers.
 * 
 * Task 2.2.8: Implement AiProviderRouter with fallback chain
 * 
 * Routing Strategy (per 0.2.5 recommendation):
 * 1. **Rule-based FIRST** (primary) - Fast, 75% accuracy, always available
 * 2. **LLM for edge cases** - When rule-based confidence is low (<65%)
 * 3. **Fallback on failure** - If LLM fails, return rule-based result
 * 
 * This hybrid approach achieves:
 * - <50ms for 80%+ of requests (high-confidence rule-based)
 * - 2-3s for ambiguous cases (LLM refinement)
 * - 100% availability (rule-based always works)
 * - Target 75-80% overall accuracy
 * 
 * Override Tracking:
 * - Records when user overrides AI classification
 * - Used for accuracy measurement per 0.3.8 metrics
 */
@Singleton
class AiProviderRouter @Inject constructor(
    private val ruleBasedProvider: RuleBasedFallbackProvider,
    private val onDeviceProvider: OnDeviceAiProvider,
    private val geminiNanoProvider: GeminiNanoProvider
) : AiProvider {
    
    companion object {
        private const val TAG = "AiProviderRouter"
        const val PROVIDER_ID = "router"
        
        /**
         * Confidence threshold for LLM escalation.
         * Below this, we try LLM for better accuracy.
         */
        const val LLM_ESCALATION_THRESHOLD = 0.65f
        
        /**
         * Request types that can be escalated to LLM.
         */
        private val ESCALATION_ELIGIBLE_TYPES = setOf(
            AiRequestType.CLASSIFY_EISENHOWER,
            AiRequestType.PARSE_TASK,
            AiRequestType.SUGGEST_SMART_GOAL
        )
        
        /**
         * Request types that always use rule-based (too fast to need LLM).
         */
        private val RULE_BASED_ONLY_TYPES = setOf<AiRequestType>()
    }
    
    private val _isAvailable = MutableStateFlow(true) // Router is always available
    override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()
    
    override val providerId: String = PROVIDER_ID
    override val displayName: String = "Smart Router"
    
    override val capabilities: Set<AiCapability> = setOf(
        AiCapability.CLASSIFICATION,
        AiCapability.EXTRACTION,
        AiCapability.GENERATION
    )
    
    /**
     * Routing mode configuration.
     */
    enum class RoutingMode {
        /** Always use rule-based (fastest, 75% accuracy) */
        RULE_BASED_ONLY,
        
        /** Rule-based first, LLM for low-confidence cases (default) */
        HYBRID,
        
        /**
         * Rule-based first, prefer Gemini Nano over llama.cpp for LLM escalation.
         * Milestone 3.6: AI Core devices get zero-download AI; others fall back to llama.cpp.
         */
        HYBRID_NANO,
        
        /** Always try LLM first, rule-based fallback (slower, potentially higher accuracy) */
        LLM_PREFERRED,
        
        /** Use LLM only, no fallback (experimental) */
        LLM_ONLY
    }
    
    private var _routingMode = MutableStateFlow(RoutingMode.HYBRID)
    val routingMode: StateFlow<RoutingMode> = _routingMode.asStateFlow()
    
    /**
     * Statistics for monitoring routing behavior.
     */
    data class RoutingStats(
        val totalRequests: Long = 0,
        val ruleBasedOnly: Long = 0,
        val llmEscalated: Long = 0,
        val llmFailed: Long = 0,
        val overrides: Long = 0,
        val averageRuleBasedLatencyMs: Long = 0,
        val averageLlmLatencyMs: Long = 0
    )
    
    private val _stats = MutableStateFlow(RoutingStats())
    val stats: StateFlow<RoutingStats> = _stats.asStateFlow()
    
    /**
     * Override tracking for accuracy measurement.
     */
    data class OverrideRecord(
        val requestId: String,
        val originalQuadrant: String,
        val overrideQuadrant: String,
        val wasLlm: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    private val overrideHistory = mutableListOf<OverrideRecord>()
    
    override suspend fun initialize(): Boolean {
        Timber.tag(TAG).i("Initializing AiProviderRouter")
        
        // Initialize all providers
        ruleBasedProvider.initialize()
        
        // Try Gemini Nano first (zero cost, zero download on supported devices)
        val nanoReady = try {
            geminiNanoProvider.initialize()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Gemini Nano initialization failed — will try llama.cpp")
            false
        }
        
        // Try to initialize LLM provider (may not have model yet)
        val llmReady = try {
            onDeviceProvider.initialize()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "LLM provider initialization failed - will use rule-based only")
            false
        }
        
        // Auto-select best routing mode based on available providers
        if (nanoReady) {
            _routingMode.value = RoutingMode.HYBRID_NANO
            Timber.tag(TAG).i("Gemini Nano available — auto-selecting HYBRID_NANO mode")
        }
        
        Timber.tag(TAG).i("Router initialized. Nano=$nanoReady, LLM=$llmReady")
        return true // Router is always ready (rule-based always works)
    }
    
    /**
     * Set routing mode.
     */
    fun setRoutingMode(mode: RoutingMode) {
        _routingMode.value = mode
        Timber.tag(TAG).i("Routing mode set to: $mode")
    }
    
    /**
     * Process an AI request with smart routing.
     */
    override suspend fun complete(request: AiRequest): Result<AiResponse> {
        val startTime = System.currentTimeMillis()
        
        return when (_routingMode.value) {
            RoutingMode.RULE_BASED_ONLY -> {
                processWithRuleBasedOnly(request, startTime)
            }
            RoutingMode.HYBRID -> {
                processWithHybrid(request, startTime)
            }
            RoutingMode.HYBRID_NANO -> {
                processWithHybridNano(request, startTime)
            }
            RoutingMode.LLM_PREFERRED -> {
                processWithLlmPreferred(request, startTime)
            }
            RoutingMode.LLM_ONLY -> {
                processWithLlmOnly(request, startTime)
            }
        }
    }
    
    /**
     * Rule-based only mode - fastest, 75% accuracy.
     */
    private suspend fun processWithRuleBasedOnly(
        request: AiRequest,
        startTime: Long
    ): Result<AiResponse> {
        val result = ruleBasedProvider.complete(request)
        updateStats(ruleBasedOnly = true, latencyMs = System.currentTimeMillis() - startTime)
        return result.map { response ->
            response.copy(
                metadata = response.metadata.copy(
                    provider = PROVIDER_ID,
                    wasRuleBased = true
                )
            )
        }
    }
    
    /**
     * Hybrid mode - rule-based first, LLM for edge cases (default).
     * 
     * Strategy:
     * 1. Run rule-based classification
     * 2. If confidence >= threshold, return immediately
     * 3. If confidence < threshold AND LLM available, escalate to LLM
     * 4. If LLM fails, return rule-based result
     */
    private suspend fun processWithHybrid(
        request: AiRequest,
        startTime: Long
    ): Result<AiResponse> {
        // Step 1: Get rule-based result
        val ruleBasedResult = ruleBasedProvider.complete(request)
        val ruleBasedLatency = System.currentTimeMillis() - startTime
        
        if (ruleBasedResult.isFailure) {
            Timber.tag(TAG).w("Rule-based failed unexpectedly")
            updateStats(ruleBasedOnly = true, latencyMs = ruleBasedLatency)
            return ruleBasedResult
        }
        
        val ruleBasedResponse = ruleBasedResult.getOrThrow()
        val confidence = ruleBasedResponse.metadata.confidenceScore
        
        // Step 2: Check if we should escalate to LLM
        val shouldEscalate = shouldEscalateToLlm(request, confidence)
        
        if (!shouldEscalate) {
            // High confidence - return rule-based result
            Timber.tag(TAG).d("Rule-based confidence ($confidence) >= threshold, using rule-based result")
            updateStats(ruleBasedOnly = true, latencyMs = ruleBasedLatency)
            return Result.success(ruleBasedResponse.copy(
                metadata = ruleBasedResponse.metadata.copy(provider = PROVIDER_ID)
            ))
        }
        
        // Step 3: Try LLM escalation
        if (!onDeviceProvider.isAvailable.value) {
            Timber.tag(TAG).d("LLM not available, using rule-based result")
            updateStats(ruleBasedOnly = true, latencyMs = ruleBasedLatency)
            return Result.success(ruleBasedResponse.copy(
                metadata = ruleBasedResponse.metadata.copy(provider = PROVIDER_ID)
            ))
        }
        
        Timber.tag(TAG).d("Escalating to LLM (confidence=$confidence < $LLM_ESCALATION_THRESHOLD)")
        
        val llmResult = try {
            onDeviceProvider.complete(request)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "LLM escalation failed")
            null
        }
        
        val totalLatency = System.currentTimeMillis() - startTime
        
        // Step 4: Return LLM result if successful, otherwise rule-based
        return if (llmResult?.isSuccess == true) {
            val llmResponse = llmResult.getOrThrow()
            updateStats(
                ruleBasedOnly = false,
                llmEscalated = true,
                latencyMs = totalLatency,
                llmLatencyMs = totalLatency - ruleBasedLatency
            )
            Result.success(llmResponse.copy(
                metadata = llmResponse.metadata.copy(
                    provider = PROVIDER_ID,
                    wasLlmFallback = true
                )
            ))
        } else {
            Timber.tag(TAG).d("LLM failed, falling back to rule-based result")
            updateStats(
                ruleBasedOnly = true,
                llmFailed = true,
                latencyMs = totalLatency
            )
            Result.success(ruleBasedResponse.copy(
                metadata = ruleBasedResponse.metadata.copy(provider = PROVIDER_ID)
            ))
        }
    }
    
    /**
     * Hybrid Nano mode — rule-based first, then Gemini Nano, then llama.cpp fallback.
     *
     * Milestone 3.6 routing priority:
     * 1. Rule-based (<50ms, 75% accuracy) — if confidence ≥ threshold, done
     * 2. Gemini Nano (~1-2s, 0 MB APK) — if AI Core available
     * 3. llama.cpp (~2-3s, 2.3GB) — fallback LLM
     */
    private suspend fun processWithHybridNano(
        request: AiRequest,
        startTime: Long
    ): Result<AiResponse> {
        // Step 1: Rule-based
        val ruleBasedResult = ruleBasedProvider.complete(request)
        val ruleBasedLatency = System.currentTimeMillis() - startTime

        if (ruleBasedResult.isFailure) {
            updateStats(ruleBasedOnly = true, latencyMs = ruleBasedLatency)
            return ruleBasedResult
        }

        val ruleBasedResponse = ruleBasedResult.getOrThrow()
        val confidence = ruleBasedResponse.metadata.confidenceScore

        if (!shouldEscalateToLlm(request, confidence)) {
            Timber.tag(TAG).d("HYBRID_NANO: Rule-based confidence ($confidence) sufficient")
            updateStats(ruleBasedOnly = true, latencyMs = ruleBasedLatency)
            return Result.success(ruleBasedResponse.copy(
                metadata = ruleBasedResponse.metadata.copy(provider = PROVIDER_ID)
            ))
        }

        // Step 2: Try Gemini Nano
        if (geminiNanoProvider.isAvailable.value) {
            Timber.tag(TAG).d("HYBRID_NANO: Escalating to Gemini Nano (confidence=$confidence)")
            val nanoResult = try {
                geminiNanoProvider.complete(request)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Gemini Nano escalation failed")
                null
            }

            if (nanoResult?.isSuccess == true) {
                val totalLatency = System.currentTimeMillis() - startTime
                updateStats(
                    ruleBasedOnly = false,
                    llmEscalated = true,
                    latencyMs = totalLatency,
                    llmLatencyMs = totalLatency - ruleBasedLatency
                )
                return Result.success(nanoResult.getOrThrow().copy(
                    metadata = nanoResult.getOrThrow().metadata.copy(
                        provider = PROVIDER_ID,
                        wasLlmFallback = true
                    )
                ))
            }
        }

        // Step 3: Try llama.cpp fallback
        if (onDeviceProvider.isAvailable.value) {
            Timber.tag(TAG).d("HYBRID_NANO: Gemini Nano unavailable, trying llama.cpp")
            val llmResult = try {
                onDeviceProvider.complete(request)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "llama.cpp fallback failed")
                null
            }

            if (llmResult?.isSuccess == true) {
                val totalLatency = System.currentTimeMillis() - startTime
                updateStats(
                    ruleBasedOnly = false,
                    llmEscalated = true,
                    latencyMs = totalLatency,
                    llmLatencyMs = totalLatency - ruleBasedLatency
                )
                return Result.success(llmResult.getOrThrow().copy(
                    metadata = llmResult.getOrThrow().metadata.copy(
                        provider = PROVIDER_ID,
                        wasLlmFallback = true
                    )
                ))
            }
        }

        // Step 4: All LLMs failed — return rule-based result
        Timber.tag(TAG).d("HYBRID_NANO: All LLMs failed, using rule-based")
        val totalLatency = System.currentTimeMillis() - startTime
        updateStats(ruleBasedOnly = true, llmFailed = true, latencyMs = totalLatency)
        return Result.success(ruleBasedResponse.copy(
            metadata = ruleBasedResponse.metadata.copy(provider = PROVIDER_ID)
        ))
    }

    /**
     * LLM preferred mode - try LLM first, rule-based fallback.
     */
    private suspend fun processWithLlmPreferred(
        request: AiRequest,
        startTime: Long
    ): Result<AiResponse> {
        if (!onDeviceProvider.isAvailable.value) {
            return processWithRuleBasedOnly(request, startTime)
        }
        
        val llmResult = try {
            onDeviceProvider.complete(request)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "LLM failed, falling back to rule-based")
            null
        }
        
        val latency = System.currentTimeMillis() - startTime
        
        return if (llmResult?.isSuccess == true) {
            updateStats(llmEscalated = true, latencyMs = latency, llmLatencyMs = latency)
            llmResult.map { response ->
                response.copy(
                    metadata = response.metadata.copy(provider = PROVIDER_ID)
                )
            }
        } else {
            updateStats(llmFailed = true, ruleBasedOnly = true, latencyMs = latency)
            ruleBasedProvider.complete(request).map { response ->
                response.copy(
                    metadata = response.metadata.copy(
                        provider = PROVIDER_ID,
                        wasRuleBased = true
                    )
                )
            }
        }
    }
    
    /**
     * LLM only mode - no fallback (experimental).
     */
    private suspend fun processWithLlmOnly(
        request: AiRequest,
        startTime: Long
    ): Result<AiResponse> {
        if (!onDeviceProvider.isAvailable.value) {
            return Result.failure(IllegalStateException("LLM not available and no fallback allowed"))
        }
        
        val result = onDeviceProvider.complete(request)
        val latency = System.currentTimeMillis() - startTime
        updateStats(llmEscalated = true, latencyMs = latency, llmLatencyMs = latency)
        return result.map { response ->
            response.copy(
                metadata = response.metadata.copy(provider = PROVIDER_ID)
            )
        }
    }
    
    /**
     * Determine if a request should be escalated to LLM.
     */
    private fun shouldEscalateToLlm(request: AiRequest, confidence: Float): Boolean {
        // Check if request type supports escalation
        if (request.type !in ESCALATION_ELIGIBLE_TYPES) {
            return false
        }
        
        // Check user preference
        if (!request.options.useLlm) {
            return false
        }
        
        // Check confidence threshold
        val threshold = request.options.minConfidence.takeIf { it > 0 } ?: LLM_ESCALATION_THRESHOLD
        return confidence < threshold
    }
    
    /**
     * Update routing statistics.
     */
    private fun updateStats(
        ruleBasedOnly: Boolean = false,
        llmEscalated: Boolean = false,
        llmFailed: Boolean = false,
        latencyMs: Long = 0,
        llmLatencyMs: Long = 0
    ) {
        _stats.value = _stats.value.copy(
            totalRequests = _stats.value.totalRequests + 1,
            ruleBasedOnly = _stats.value.ruleBasedOnly + if (ruleBasedOnly) 1 else 0,
            llmEscalated = _stats.value.llmEscalated + if (llmEscalated) 1 else 0,
            llmFailed = _stats.value.llmFailed + if (llmFailed) 1 else 0
        )
    }
    
    /**
     * Record a user override for accuracy tracking.
     * 
     * @param requestId The original request ID
     * @param originalResult The AI-suggested result
     * @param overrideResult The user's correction
     * @param wasLlm Whether the original was from LLM
     */
    fun recordOverride(
        requestId: String,
        originalResult: AiResult.EisenhowerClassification,
        overrideQuadrant: String,
        wasLlm: Boolean
    ) {
        val record = OverrideRecord(
            requestId = requestId,
            originalQuadrant = originalResult.quadrant.name,
            overrideQuadrant = overrideQuadrant,
            wasLlm = wasLlm
        )
        overrideHistory.add(record)
        _stats.value = _stats.value.copy(overrides = _stats.value.overrides + 1)
        
        Timber.tag(TAG).i("Override recorded: ${record.originalQuadrant} -> ${record.overrideQuadrant}")
    }
    
    /**
     * Get override history for accuracy analysis.
     */
    fun getOverrideHistory(): List<OverrideRecord> = overrideHistory.toList()
    
    /**
     * Calculate accuracy based on override rate.
     * Lower override rate = higher accuracy.
     */
    fun calculateAccuracy(): Float {
        val total = _stats.value.totalRequests
        val overrides = _stats.value.overrides
        return if (total > 0) {
            1f - (overrides.toFloat() / total.toFloat())
        } else {
            0f
        }
    }
    
    /**
     * Reset statistics.
     */
    fun resetStats() {
        _stats.value = RoutingStats()
        overrideHistory.clear()
    }
    
    // ========================================================================
    // AiProvider interface methods
    // ========================================================================
    
    override suspend fun stream(request: AiRequest): Flow<AiStreamChunk> = flow {
        val result = complete(request)
        result.onSuccess { response ->
            emit(AiStreamChunk(
                text = response.rawText ?: "",
                isComplete = true,
                tokenIndex = response.metadata.tokensUsed
            ))
        }.onFailure {
            emit(AiStreamChunk(text = "", isComplete = true))
        }
    }
    
    override suspend fun getModelInfo(): ModelInfo {
        // Return info about currently active provider
        return if (onDeviceProvider.isAvailable.value) {
            onDeviceProvider.getModelInfo()
        } else {
            ruleBasedProvider.getModelInfo()
        }
    }
    
    override suspend fun estimateCost(request: AiRequest): Float? = null // All on-device
    
    override suspend fun release() {
        geminiNanoProvider.release()
        onDeviceProvider.release()
        // Rule-based has nothing to release
        Timber.tag(TAG).i("AiProviderRouter released")
    }
}

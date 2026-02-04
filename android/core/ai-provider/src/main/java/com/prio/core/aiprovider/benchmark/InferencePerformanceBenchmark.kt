package com.prio.core.aiprovider.benchmark

import com.prio.core.ai.model.AiRequest
import com.prio.core.ai.model.AiRequestType
import com.prio.core.ai.model.AiResponse
import com.prio.core.ai.prompt.EisenhowerPrompts
import com.prio.core.ai.prompt.PromptStrategy
import com.prio.core.ai.prompt.PromptTemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.roundToLong

/**
 * Performance benchmark for AI inference latency verification.
 * 
 * Task 2.2.12: Performance test - inference under 3 seconds
 * 
 * Verified baselines from 0.2.2 benchmark:
 * - Phi-3: 2-3s on Pixel 9a (Tier 1)
 * - Target: 90%+ queries <5s on Tier 2 (6GB RAM) devices
 * - Build flags: -march=armv8.2-a+dotprod+fp16 for 2.5x speedup
 * 
 * Performance Budgets:
 * - Model loading: <2s (Phi-3), <45s (Mistral 7B)
 * - Classification: <3s (Tier 1), <5s (Tier 2)
 * - Rule-based fallback: <50ms (always)
 */
object InferencePerformanceBenchmark {
    
    private const val TAG = "InferencePerfBench"
    
    // Target latencies in milliseconds
    const val TARGET_CLASSIFICATION_TIER1_MS = 3000L // 3 seconds for Pixel 9a class
    const val TARGET_CLASSIFICATION_TIER2_MS = 5000L // 5 seconds for 6GB RAM devices
    const val TARGET_CLASSIFICATION_TIER3_MS = 8000L // 8 seconds for 4GB RAM devices
    const val TARGET_RULE_BASED_MS = 50L // 50ms for rule-based
    const val TARGET_MODEL_LOAD_PHI3_MS = 2000L // 2 seconds
    const val TARGET_MODEL_LOAD_MISTRAL_MS = 45000L // 45 seconds
    
    // Success threshold
    const val TARGET_SUCCESS_RATE = 0.90f // 90% of queries meet target
    
    /**
     * Run comprehensive latency benchmark.
     * 
     * @param classifier Function that performs classification (provider.complete)
     * @param testCases List of test tasks
     * @param warmupRuns Number of warmup runs before measurement
     * @param onProgress Progress callback
     * @return Benchmark results
     */
    suspend fun runLatencyBenchmark(
        classifier: suspend (String) -> Result<AiResponse>,
        testCases: List<String>,
        warmupRuns: Int = 2,
        deviceTier: DeviceTier = DeviceTier.TIER_1,
        onProgress: ((String) -> Unit)? = null
    ): LatencyBenchmarkResult = withContext(Dispatchers.Default) {
        val targetLatency = when (deviceTier) {
            DeviceTier.TIER_1 -> TARGET_CLASSIFICATION_TIER1_MS
            DeviceTier.TIER_2 -> TARGET_CLASSIFICATION_TIER2_MS
            DeviceTier.TIER_3 -> TARGET_CLASSIFICATION_TIER3_MS
        }
        
        onProgress?.invoke("Starting latency benchmark on ${deviceTier.displayName}")
        onProgress?.invoke("Target: ${targetLatency}ms for 90%+ of queries")
        onProgress?.invoke("Test cases: ${testCases.size}, Warmup: $warmupRuns")
        
        // Warmup runs
        onProgress?.invoke("\n--- Warmup Phase ---")
        repeat(warmupRuns) { i ->
            val task = testCases.getOrElse(i) { testCases.first() }
            val start = System.currentTimeMillis()
            classifier(task)
            val elapsed = System.currentTimeMillis() - start
            onProgress?.invoke("Warmup ${i + 1}: ${elapsed}ms")
        }
        
        // Measurement runs
        onProgress?.invoke("\n--- Measurement Phase ---")
        val measurements = mutableListOf<LatencyMeasurement>()
        
        for ((index, task) in testCases.withIndex()) {
            val startTime = System.currentTimeMillis()
            val result = classifier(task)
            val elapsedMs = System.currentTimeMillis() - startTime
            
            val meetsTarget = elapsedMs <= targetLatency
            val status = if (meetsTarget) "✓" else "✗"
            
            onProgress?.invoke("[$status] ${index + 1}/${testCases.size}: ${elapsedMs}ms - ${task.take(40)}...")
            
            measurements.add(
                LatencyMeasurement(
                    taskDescription = task,
                    latencyMs = elapsedMs,
                    success = result.isSuccess,
                    meetsTarget = meetsTarget,
                    errorMessage = result.exceptionOrNull()?.message
                )
            )
        }
        
        // Calculate statistics
        val latencies = measurements.map { it.latencyMs }
        val successfulMeasurements = measurements.filter { it.success }
        val meetingTarget = measurements.count { it.meetsTarget }
        val successRate = meetingTarget.toFloat() / measurements.size
        
        val result = LatencyBenchmarkResult(
            deviceTier = deviceTier,
            targetLatencyMs = targetLatency,
            totalTests = measurements.size,
            successfulTests = successfulMeasurements.size,
            meetingTarget = meetingTarget,
            successRate = successRate,
            passedBenchmark = successRate >= TARGET_SUCCESS_RATE,
            minLatencyMs = latencies.minOrNull() ?: 0L,
            maxLatencyMs = latencies.maxOrNull() ?: 0L,
            avgLatencyMs = latencies.average().roundToLong(),
            medianLatencyMs = latencies.sorted().let { it[it.size / 2] },
            p90LatencyMs = latencies.sorted().let { it[(it.size * 0.9).toInt().coerceAtMost(it.size - 1)] },
            p99LatencyMs = latencies.sorted().let { it[(it.size * 0.99).toInt().coerceAtMost(it.size - 1)] },
            measurements = measurements
        )
        
        // Log summary
        onProgress?.invoke("\n--- Benchmark Summary ---")
        onProgress?.invoke("Device Tier: ${deviceTier.displayName}")
        onProgress?.invoke("Target: ${targetLatency}ms")
        onProgress?.invoke("Success Rate: ${String.format("%.1f", successRate * 100)}% (target: ${TARGET_SUCCESS_RATE * 100}%)")
        onProgress?.invoke("Passed: ${if (result.passedBenchmark) "✅ YES" else "❌ NO"}")
        onProgress?.invoke("")
        onProgress?.invoke("Latency Stats:")
        onProgress?.invoke("  Min: ${result.minLatencyMs}ms")
        onProgress?.invoke("  Max: ${result.maxLatencyMs}ms")
        onProgress?.invoke("  Avg: ${result.avgLatencyMs}ms")
        onProgress?.invoke("  Median: ${result.medianLatencyMs}ms")
        onProgress?.invoke("  P90: ${result.p90LatencyMs}ms")
        onProgress?.invoke("  P99: ${result.p99LatencyMs}ms")
        
        result
    }
    
    /**
     * Standard test cases for latency benchmarking.
     * Covers various task complexities.
     */
    val STANDARD_TEST_CASES = listOf(
        // Short tasks (quick classification)
        "Call mom",
        "Buy groceries",
        "Review PR",
        "Submit report",
        "Pay bills",
        
        // Medium tasks (normal complexity)
        "Urgent: Fix production server issue",
        "Schedule meeting with team for next week",
        "Read that book I've been putting off",
        "Research vacation destinations for summer",
        "Complete quarterly tax filing by Friday",
        
        // Long/complex tasks (stress test)
        "Prepare comprehensive presentation for the board meeting next Tuesday covering Q4 results",
        "Review and approve the new marketing strategy document with budget projections",
        "Organize team building event for 20 people including catering and venue booking"
    )
    
    /**
     * Extended test dataset (20 cases for thorough benchmarking).
     */
    val EXTENDED_TEST_CASES = listOf(
        // DO quadrant (urgent + important)
        "Server is down and customers are affected",
        "Client deadline in 2 hours",
        "Urgent bug fix needed for production",
        "Medical emergency - call doctor now",
        "Submit tax return today or face penalties",
        
        // SCHEDULE quadrant (important, not urgent)
        "Plan next quarter's OKRs",
        "Take online course on machine learning",
        "Write long-term career development plan",
        "Schedule regular exercise routine",
        "Research investment options for retirement",
        
        // DELEGATE quadrant (urgent, not important)
        "Order office supplies for the team",
        "Book travel for conference next month",
        "Schedule recurring team status meetings",
        "Update meeting notes in shared doc",
        "Respond to routine vendor inquiry",
        
        // ELIMINATE quadrant (neither)
        "Browse social media during lunch",
        "Watch random YouTube videos",
        "Reorganize desk for the third time",
        "Check news sites repeatedly",
        "Reply to spam emails"
    )
}

/**
 * Device tier classification based on RAM and CPU.
 */
enum class DeviceTier(val displayName: String, val ramGb: Int) {
    TIER_1("High-end (8GB+ RAM)", 8),   // Pixel 9a, Samsung S24+
    TIER_2("Mid-range (6GB RAM)", 6),   // Pixel 7a, Samsung A54
    TIER_3("Entry-level (4GB RAM)", 4)  // Budget devices
}

/**
 * Individual latency measurement.
 */
data class LatencyMeasurement(
    val taskDescription: String,
    val latencyMs: Long,
    val success: Boolean,
    val meetsTarget: Boolean,
    val errorMessage: String? = null
)

/**
 * Complete benchmark result.
 */
data class LatencyBenchmarkResult(
    val deviceTier: DeviceTier,
    val targetLatencyMs: Long,
    val totalTests: Int,
    val successfulTests: Int,
    val meetingTarget: Int,
    val successRate: Float,
    val passedBenchmark: Boolean,
    val minLatencyMs: Long,
    val maxLatencyMs: Long,
    val avgLatencyMs: Long,
    val medianLatencyMs: Long,
    val p90LatencyMs: Long,
    val p99LatencyMs: Long,
    val measurements: List<LatencyMeasurement>
) {
    /**
     * Generate markdown report.
     */
    fun toMarkdownReport(): String = buildString {
        appendLine("# Inference Latency Benchmark Report")
        appendLine()
        appendLine("## Configuration")
        appendLine("- Device Tier: ${deviceTier.displayName}")
        appendLine("- Target Latency: ${targetLatencyMs}ms")
        appendLine("- Success Threshold: 90%")
        appendLine()
        appendLine("## Results")
        appendLine("| Metric | Value |")
        appendLine("|--------|-------|")
        appendLine("| Total Tests | $totalTests |")
        appendLine("| Successful | $successfulTests |")
        appendLine("| Meeting Target | $meetingTarget |")
        appendLine("| **Success Rate** | **${String.format("%.1f", successRate * 100)}%** |")
        appendLine("| **Passed** | **${if (passedBenchmark) "✅ YES" else "❌ NO"}** |")
        appendLine()
        appendLine("## Latency Statistics")
        appendLine("| Stat | Value |")
        appendLine("|------|-------|")
        appendLine("| Min | ${minLatencyMs}ms |")
        appendLine("| Max | ${maxLatencyMs}ms |")
        appendLine("| Average | ${avgLatencyMs}ms |")
        appendLine("| Median | ${medianLatencyMs}ms |")
        appendLine("| P90 | ${p90LatencyMs}ms |")
        appendLine("| P99 | ${p99LatencyMs}ms |")
        appendLine()
        appendLine("## Individual Results")
        appendLine("| # | Task | Latency | Target Met |")
        appendLine("|---|------|---------|------------|")
        measurements.forEachIndexed { index, m ->
            val status = if (m.meetsTarget) "✓" else "✗"
            appendLine("| ${index + 1} | ${m.taskDescription.take(50)} | ${m.latencyMs}ms | $status |")
        }
    }
}

/**
 * Model loading performance metrics.
 */
data class ModelLoadBenchmark(
    val modelId: String,
    val modelSizeBytes: Long,
    val loadTimeMs: Long,
    val ramUsageBytes: Long,
    val meetsTarget: Boolean,
    val targetLoadTimeMs: Long
) {
    fun toMarkdownRow(): String {
        val sizeGb = modelSizeBytes / (1024.0 * 1024.0 * 1024.0)
        val ramMb = ramUsageBytes / (1024.0 * 1024.0)
        val status = if (meetsTarget) "✓" else "✗"
        return "| $modelId | ${String.format("%.2f", sizeGb)} GB | ${loadTimeMs}ms | ${String.format("%.0f", ramMb)} MB | $status |"
    }
}

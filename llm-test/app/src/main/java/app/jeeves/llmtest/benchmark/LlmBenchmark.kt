package app.jeeves.llmtest.benchmark

import android.content.Context
import android.os.Build
import app.jeeves.llmtest.engine.LlamaEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Benchmark runner for Phi-3-mini performance testing.
 * 
 * Task 0.2.2: Benchmark Phi-3-mini-4k-instruct (Q4_K_M) on 2 reference devices
 * 
 * Measures:
 * - Model load time
 * - First token latency (TTFT)
 * - Tokens per second (throughput)
 * - Memory usage
 * - Classification task timing
 */
class LlmBenchmark(
    private val context: Context,
    private val llamaEngine: LlamaEngine
) {
    companion object {
        private const val TAG = "LlmBenchmark"
        
        // Standard benchmark prompts
        val BENCHMARK_PROMPTS = listOf(
            // Short prompt (~50 tokens)
            "What is 2 + 2? Answer with just the number.",
            
            // Medium prompt (~100 tokens)
            """Classify this task into Eisenhower quadrant: "Submit quarterly report by Friday"
               Answer: DO, SCHEDULE, DELEGATE, or ELIMINATE.""",
            
            // Classification prompt (~150 tokens)
            """You are a task classification assistant. Classify this task:
               Task: "Prepare presentation for board meeting tomorrow"
               Respond with JSON: {"quadrant": "DO", "confidence": 0.9}""",
            
            // Longer generation prompt
            """Generate a brief 3-item todo list for someone planning a product launch.
               Keep each item under 10 words."""
        )
        
        const val WARMUP_RUNS = 2
        const val BENCHMARK_RUNS = 5
    }
    
    /**
     * Run complete benchmark suite.
     */
    suspend fun runFullBenchmark(modelPath: String? = null): BenchmarkReport = withContext(Dispatchers.IO) {
        val deviceInfo = collectDeviceInfo()
        val benchmarkResults = mutableListOf<BenchmarkResult>()
        
        // Load model
        val loadResult = if (modelPath != null && File(modelPath).exists()) {
            llamaEngine.loadModel(modelPath)
        } else {
            // Use stub for testing
            llamaEngine.loadStubModel()
        }
        
        if (!loadResult.success) {
            return@withContext BenchmarkReport(
                deviceInfo = deviceInfo,
                modelInfo = ModelInfo(
                    name = "Phi-3-mini-4k-instruct",
                    quantization = "Q4_K_M",
                    sizeBytes = 0,
                    isStub = true
                ),
                loadTimeMs = 0,
                memoryUsageBytes = 0,
                results = emptyList(),
                summary = BenchmarkSummary(0.0, 0, 0.0, 0.0),
                error = loadResult.error
            )
        }
        
        val modelInfo = ModelInfo(
            name = "Phi-3-mini-4k-instruct",
            quantization = "Q4_K_M",
            sizeBytes = loadResult.memoryBytes,
            isStub = loadResult.isStub
        )
        
        // Warmup runs
        android.util.Log.i(TAG, "Running warmup...")
        repeat(WARMUP_RUNS) {
            llamaEngine.generate(BENCHMARK_PROMPTS[0], maxTokens = 10)
        }
        
        // Benchmark each prompt
        for ((index, prompt) in BENCHMARK_PROMPTS.withIndex()) {
            android.util.Log.i(TAG, "Benchmarking prompt ${index + 1}/${BENCHMARK_PROMPTS.size}")
            
            val times = mutableListOf<Long>()
            val tokenCounts = mutableListOf<Int>()
            
            repeat(BENCHMARK_RUNS) { run ->
                val result = llamaEngine.generate(
                    prompt = prompt,
                    maxTokens = 100,
                    temperature = 0.3f
                )
                times.add(result.inferenceTimeMs)
                tokenCounts.add(result.tokensGenerated)
                
                android.util.Log.d(TAG, "Run ${run + 1}: ${result.inferenceTimeMs}ms, ${result.tokensGenerated} tokens")
            }
            
            val avgTime = times.average()
            val avgTokens = tokenCounts.average()
            val tokensPerSec = if (avgTime > 0) avgTokens / (avgTime / 1000.0) else 0.0
            
            benchmarkResults.add(
                BenchmarkResult(
                    promptName = "Prompt ${index + 1}",
                    promptLength = prompt.length,
                    avgInferenceTimeMs = avgTime,
                    avgTokensGenerated = avgTokens,
                    tokensPerSecond = tokensPerSec,
                    minTimeMs = times.minOrNull() ?: 0,
                    maxTimeMs = times.maxOrNull() ?: 0,
                    stdDevMs = calculateStdDev(times)
                )
            )
        }
        
        // Calculate summary
        val summary = BenchmarkSummary(
            avgTokensPerSecond = benchmarkResults.map { it.tokensPerSecond }.average(),
            avgInferenceTimeMs = benchmarkResults.map { it.avgInferenceTimeMs }.average().toLong(),
            firstTokenLatencyMs = benchmarkResults.firstOrNull()?.minTimeMs?.toDouble() ?: 0.0,
            p95InferenceTimeMs = calculateP95(benchmarkResults.map { it.maxTimeMs })
        )
        
        BenchmarkReport(
            deviceInfo = deviceInfo,
            modelInfo = modelInfo,
            loadTimeMs = loadResult.loadTimeMs,
            memoryUsageBytes = loadResult.memoryBytes,
            results = benchmarkResults,
            summary = summary,
            error = null
        )
    }
    
    /**
     * Run quick benchmark for single prompt.
     */
    suspend fun runQuickBenchmark(prompt: String, runs: Int = 3): BenchmarkResult = withContext(Dispatchers.IO) {
        val times = mutableListOf<Long>()
        val tokenCounts = mutableListOf<Int>()
        
        repeat(runs) {
            val result = llamaEngine.generate(prompt, maxTokens = 100)
            times.add(result.inferenceTimeMs)
            tokenCounts.add(result.tokensGenerated)
        }
        
        val avgTime = times.average()
        val avgTokens = tokenCounts.average()
        
        BenchmarkResult(
            promptName = "Quick Benchmark",
            promptLength = prompt.length,
            avgInferenceTimeMs = avgTime,
            avgTokensGenerated = avgTokens,
            tokensPerSecond = if (avgTime > 0) avgTokens / (avgTime / 1000.0) else 0.0,
            minTimeMs = times.minOrNull() ?: 0,
            maxTimeMs = times.maxOrNull() ?: 0,
            stdDevMs = calculateStdDev(times)
        )
    }
    
    private fun collectDeviceInfo(): DeviceInfo {
        val runtime = Runtime.getRuntime()
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            device = Build.DEVICE,
            sdkVersion = Build.VERSION.SDK_INT,
            cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            totalMemoryMb = runtime.maxMemory() / (1024 * 1024),
            availableMemoryMb = runtime.freeMemory() / (1024 * 1024),
            cpuCores = runtime.availableProcessors()
        )
    }
    
    private fun calculateStdDev(values: List<Long>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
    
    private fun calculateP95(values: List<Long>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val index = (sorted.size * 0.95).toInt().coerceIn(0, sorted.size - 1)
        return sorted[index].toDouble()
    }
}

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val device: String,
    val sdkVersion: Int,
    val cpuAbi: String,
    val totalMemoryMb: Long,
    val availableMemoryMb: Long,
    val cpuCores: Int
)

data class ModelInfo(
    val name: String,
    val quantization: String,
    val sizeBytes: Long,
    val isStub: Boolean
)

data class BenchmarkResult(
    val promptName: String,
    val promptLength: Int,
    val avgInferenceTimeMs: Double,
    val avgTokensGenerated: Double,
    val tokensPerSecond: Double,
    val minTimeMs: Long,
    val maxTimeMs: Long,
    val stdDevMs: Double
)

data class BenchmarkSummary(
    val avgTokensPerSecond: Double,
    val avgInferenceTimeMs: Long,
    val firstTokenLatencyMs: Double,
    val p95InferenceTimeMs: Double
)

data class BenchmarkReport(
    val deviceInfo: DeviceInfo,
    val modelInfo: ModelInfo,
    val loadTimeMs: Long,
    val memoryUsageBytes: Long,
    val results: List<BenchmarkResult>,
    val summary: BenchmarkSummary,
    val error: String?
) {
    fun toMarkdown(): String = buildString {
        appendLine("# LLM Benchmark Report")
        appendLine()
        appendLine("## Device Information")
        appendLine("| Property | Value |")
        appendLine("|----------|-------|")
        appendLine("| Device | ${deviceInfo.manufacturer} ${deviceInfo.model} |")
        appendLine("| CPU | ${deviceInfo.cpuCores} cores (${deviceInfo.cpuAbi}) |")
        appendLine("| RAM | ${deviceInfo.totalMemoryMb} MB total |")
        appendLine("| Android | API ${deviceInfo.sdkVersion} |")
        appendLine()
        appendLine("## Model Information")
        appendLine("| Property | Value |")
        appendLine("|----------|-------|")
        appendLine("| Model | ${modelInfo.name} |")
        appendLine("| Quantization | ${modelInfo.quantization} |")
        appendLine("| Size | ${modelInfo.sizeBytes / 1_000_000} MB |")
        appendLine("| Mode | ${if (modelInfo.isStub) "Stub (simulated)" else "Real"} |")
        appendLine("| Load Time | ${loadTimeMs} ms |")
        appendLine("| Memory Usage | ${memoryUsageBytes / 1_000_000} MB |")
        appendLine()
        appendLine("## Benchmark Results")
        appendLine("| Prompt | Avg Time (ms) | Tokens | Tokens/sec |")
        appendLine("|--------|---------------|--------|------------|")
        for (result in results) {
            appendLine("| ${result.promptName} | ${result.avgInferenceTimeMs.toLong()} | ${result.avgTokensGenerated.toInt()} | ${String.format("%.1f", result.tokensPerSecond)} |")
        }
        appendLine()
        appendLine("## Summary")
        appendLine("- **Average Tokens/sec**: ${String.format("%.1f", summary.avgTokensPerSecond)}")
        appendLine("- **Average Inference Time**: ${summary.avgInferenceTimeMs} ms")
        appendLine("- **First Token Latency**: ${String.format("%.0f", summary.firstTokenLatencyMs)} ms")
        appendLine("- **P95 Inference Time**: ${String.format("%.0f", summary.p95InferenceTimeMs)} ms")
        
        if (error != null) {
            appendLine()
            appendLine("## Errors")
            appendLine("- $error")
        }
    }
}

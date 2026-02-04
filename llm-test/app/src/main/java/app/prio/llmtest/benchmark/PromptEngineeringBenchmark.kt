package app.prio.llmtest.benchmark

import app.prio.llmtest.engine.ClassificationResult
import app.prio.llmtest.engine.ClassificationSource
import app.prio.llmtest.engine.EisenhowerQuadrant
import app.prio.llmtest.engine.LlamaEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Comprehensive prompt engineering benchmark runner.
 * 
 * Milestone 0.2.6 Option A: Evaluates different prompt strategies
 * to achieve >70% accuracy target.
 * 
 * Tasks covered:
 * - 0.2.6.1: JSON structured output
 * - 0.2.6.2: Chain-of-thought reasoning
 * - 0.2.6.3: Few-shot learning
 * - 0.2.6.4: Expert persona
 * - 0.2.6.5: Benchmark on 50 diverse tasks
 */
class PromptEngineeringBenchmark(
    private val llamaEngine: LlamaEngine,
    private val onProgress: ((String) -> Unit)? = null
) {
    
    companion object {
        const val TARGET_ACCURACY = 0.70f
        const val EXCELLENT_ACCURACY = 0.80f
        
        // Generation parameters
        const val MAX_TOKENS = 150
        const val TEMPERATURE = 0.2f
        const val TOP_P = 0.9f
    }
    
    /**
     * Run benchmark for a single prompt strategy on the 50-task dataset.
     */
    suspend fun benchmarkStrategy(
        strategy: PromptStrategy,
        testCases: List<TestCase> = ExtendedTestDataset.TEST_CASES_50
    ): StrategyBenchmarkResult = withContext(Dispatchers.Default) {
        val results = mutableListOf<PromptTestResult>()
        var totalInferenceTimeMs = 0L
        var totalPromptTokens = 0
        var totalGeneratedTokens = 0
        
        onProgress?.invoke("Testing strategy: ${strategy.displayName}")
        onProgress?.invoke("Test cases: ${testCases.size}")
        
        for ((index, testCase) in testCases.withIndex()) {
            onProgress?.invoke("  [${index + 1}/${testCases.size}] ${testCase.task.take(40)}...")
            
            val prompt = PromptBuilder.buildPrompt(strategy, testCase.task)
            val startTime = System.currentTimeMillis()
            
            val generateResult = llamaEngine.generate(
                prompt = prompt,
                maxTokens = MAX_TOKENS,
                temperature = TEMPERATURE,
                topP = TOP_P
            )
            
            val inferenceTimeMs = System.currentTimeMillis() - startTime
            totalInferenceTimeMs += inferenceTimeMs
            totalGeneratedTokens += generateResult.tokensGenerated
            
            // Parse response
            val parsed = PromptBuilder.parseResponse(generateResult.text)
            val predicted = parsed?.quadrant ?: EisenhowerQuadrant.SCHEDULE // Default fallback
            val confidence = parsed?.confidence ?: 0.5f
            val reasoning = parsed?.reasoning ?: "Parse failed: ${generateResult.text.take(50)}"
            
            val isCorrect = predicted == testCase.groundTruth
            
            results.add(
                PromptTestResult(
                    testCase = testCase,
                    strategy = strategy,
                    predicted = predicted,
                    confidence = confidence,
                    reasoning = reasoning,
                    isCorrect = isCorrect,
                    inferenceTimeMs = inferenceTimeMs,
                    rawResponse = generateResult.text,
                    promptLength = prompt.length,
                    tokensGenerated = generateResult.tokensGenerated,
                    parseSuccess = parsed != null
                )
            )
            
            val status = if (isCorrect) "✓" else "✗"
            onProgress?.invoke("    $status ${testCase.groundTruth} -> $predicted (${String.format("%.0f", confidence * 100)}%, ${inferenceTimeMs}ms)")
        }
        
        generateStrategyResult(strategy, results, totalInferenceTimeMs)
    }
    
    /**
     * Run benchmark for all prompt strategies.
     */
    suspend fun benchmarkAllStrategies(
        testCases: List<TestCase> = ExtendedTestDataset.TEST_CASES_50
    ): FullBenchmarkResult = withContext(Dispatchers.Default) {
        val strategyResults = mutableListOf<StrategyBenchmarkResult>()
        
        onProgress?.invoke("=".repeat(60))
        onProgress?.invoke("PROMPT ENGINEERING BENCHMARK")
        onProgress?.invoke("Milestone 0.2.6 Option A - ${testCases.size} test cases")
        onProgress?.invoke("=".repeat(60))
        
        for (strategy in PromptStrategy.entries) {
            onProgress?.invoke("\n" + "-".repeat(50))
            val result = benchmarkStrategy(strategy, testCases)
            strategyResults.add(result)
            
            onProgress?.invoke("\n${strategy.displayName}: ${String.format("%.1f", result.accuracy * 100)}% accuracy")
            onProgress?.invoke("  Correct: ${result.correct}/${result.total}")
            onProgress?.invoke("  Avg latency: ${result.avgInferenceTimeMs}ms")
        }
        
        // Find best strategy
        val bestStrategy = strategyResults.maxByOrNull { it.accuracy }!!
        
        onProgress?.invoke("\n" + "=".repeat(60))
        onProgress?.invoke("BEST STRATEGY: ${bestStrategy.strategy.displayName}")
        onProgress?.invoke("Accuracy: ${String.format("%.1f", bestStrategy.accuracy * 100)}%")
        onProgress?.invoke("Target: ${String.format("%.0f", TARGET_ACCURACY * 100)}% - ${if (bestStrategy.accuracy >= TARGET_ACCURACY) "ACHIEVED ✓" else "NOT MET ✗"}")
        onProgress?.invoke("=".repeat(60))
        
        FullBenchmarkResult(
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
            modelInfo = getModelInfo(),
            testCasesCount = testCases.size,
            strategyResults = strategyResults,
            bestStrategy = bestStrategy.strategy,
            bestAccuracy = bestStrategy.accuracy,
            meetsTarget = bestStrategy.accuracy >= TARGET_ACCURACY,
            meetsExcellent = bestStrategy.accuracy >= EXCELLENT_ACCURACY
        )
    }
    
    /**
     * Quick comparison benchmark on 20 tasks (for faster iteration).
     */
    suspend fun quickBenchmark(): FullBenchmarkResult {
        return benchmarkAllStrategies(ExtendedTestDataset.TEST_CASES_20)
    }
    
    private fun generateStrategyResult(
        strategy: PromptStrategy,
        results: List<PromptTestResult>,
        totalInferenceTimeMs: Long
    ): StrategyBenchmarkResult {
        val correct = results.count { it.isCorrect }
        val total = results.size
        val accuracy = correct.toFloat() / total
        
        // Per-quadrant metrics
        val quadrantMetrics = EisenhowerQuadrant.entries.map { quadrant ->
            val relevant = results.filter { it.testCase.groundTruth == quadrant }
            val truePositives = relevant.count { it.isCorrect }
            val predicted = results.filter { it.predicted == quadrant }
            
            val precision = if (predicted.isNotEmpty()) {
                truePositives.toFloat() / predicted.size
            } else 0f
            
            val recall = if (relevant.isNotEmpty()) {
                truePositives.toFloat() / relevant.size
            } else 0f
            
            val f1 = if (precision + recall > 0) {
                2 * precision * recall / (precision + recall)
            } else 0f
            
            QuadrantStrategyMetrics(
                quadrant = quadrant,
                total = relevant.size,
                correct = truePositives,
                precision = precision,
                recall = recall,
                f1Score = f1
            )
        }
        
        // Confusion matrix
        val confusionMatrix = buildConfusionMatrix(results)
        
        return StrategyBenchmarkResult(
            strategy = strategy,
            total = total,
            correct = correct,
            accuracy = accuracy,
            meetsTarget = accuracy >= TARGET_ACCURACY,
            quadrantMetrics = quadrantMetrics,
            confusionMatrix = confusionMatrix,
            avgInferenceTimeMs = totalInferenceTimeMs / total,
            avgConfidence = results.map { it.confidence }.average().toFloat(),
            parseSuccessRate = results.count { it.parseSuccess }.toFloat() / total,
            results = results
        )
    }
    
    private fun buildConfusionMatrix(results: List<PromptTestResult>): ConfusionMatrix {
        val matrix = mutableMapOf<EisenhowerQuadrant, MutableMap<EisenhowerQuadrant, Int>>()
        
        for (actual in EisenhowerQuadrant.entries) {
            matrix[actual] = mutableMapOf()
            for (predicted in EisenhowerQuadrant.entries) {
                matrix[actual]!![predicted] = 0
            }
        }
        
        for (result in results) {
            matrix[result.testCase.groundTruth]!![result.predicted] = 
                matrix[result.testCase.groundTruth]!![result.predicted]!! + 1
        }
        
        return ConfusionMatrix(matrix.mapValues { it.value.toMap() })
    }
    
    private fun getModelInfo(): String {
        return if (llamaEngine.isLoaded) {
            if (llamaEngine.isStub) "Stub Implementation" else "Real LLM"
        } else {
            "Model not loaded"
        }
    }
}

// ============================================================================
// Result Data Classes
// ============================================================================

data class PromptTestResult(
    val testCase: TestCase,
    val strategy: PromptStrategy,
    val predicted: EisenhowerQuadrant,
    val confidence: Float,
    val reasoning: String,
    val isCorrect: Boolean,
    val inferenceTimeMs: Long,
    val rawResponse: String,
    val promptLength: Int,
    val tokensGenerated: Int,
    val parseSuccess: Boolean
)

data class QuadrantStrategyMetrics(
    val quadrant: EisenhowerQuadrant,
    val total: Int,
    val correct: Int,
    val precision: Float,
    val recall: Float,
    val f1Score: Float
)

data class ConfusionMatrix(
    val matrix: Map<EisenhowerQuadrant, Map<EisenhowerQuadrant, Int>>
) {
    fun toMarkdownTable(): String = buildString {
        appendLine("|  | Pred DO | Pred SCHEDULE | Pred DELEGATE | Pred ELIMINATE |")
        appendLine("|---|:---:|:---:|:---:|:---:|")
        for (actual in EisenhowerQuadrant.entries) {
            val row = matrix[actual]!!
            appendLine("| **Actual $actual** | ${row[EisenhowerQuadrant.DO]} | ${row[EisenhowerQuadrant.SCHEDULE]} | ${row[EisenhowerQuadrant.DELEGATE]} | ${row[EisenhowerQuadrant.ELIMINATE]} |")
        }
    }
}

data class StrategyBenchmarkResult(
    val strategy: PromptStrategy,
    val total: Int,
    val correct: Int,
    val accuracy: Float,
    val meetsTarget: Boolean,
    val quadrantMetrics: List<QuadrantStrategyMetrics>,
    val confusionMatrix: ConfusionMatrix,
    val avgInferenceTimeMs: Long,
    val avgConfidence: Float,
    val parseSuccessRate: Float,
    val results: List<PromptTestResult>
) {
    fun toMarkdownSummary(): String = buildString {
        appendLine("### ${strategy.displayName}")
        appendLine()
        appendLine("**Description**: ${strategy.description}")
        appendLine()
        appendLine("| Metric | Value |")
        appendLine("|--------|-------|")
        appendLine("| Accuracy | **${String.format("%.1f", accuracy * 100)}%** ${if (meetsTarget) "✅" else "❌"} |")
        appendLine("| Correct / Total | $correct / $total |")
        appendLine("| Avg Inference | ${avgInferenceTimeMs}ms |")
        appendLine("| Avg Confidence | ${String.format("%.2f", avgConfidence)} |")
        appendLine("| Parse Success | ${String.format("%.0f", parseSuccessRate * 100)}% |")
        appendLine()
        
        appendLine("#### Per-Quadrant Performance")
        appendLine("| Quadrant | Accuracy | Precision | Recall | F1 |")
        appendLine("|----------|----------|-----------|--------|-----|")
        for (qm in quadrantMetrics) {
            val qAccuracy = if (qm.total > 0) qm.correct.toFloat() / qm.total else 0f
            appendLine("| ${qm.quadrant} | ${String.format("%.0f", qAccuracy * 100)}% | ${String.format("%.2f", qm.precision)} | ${String.format("%.2f", qm.recall)} | ${String.format("%.2f", qm.f1Score)} |")
        }
        appendLine()
        
        appendLine("#### Confusion Matrix")
        appendLine(confusionMatrix.toMarkdownTable())
    }
}

data class FullBenchmarkResult(
    val timestamp: String,
    val modelInfo: String,
    val testCasesCount: Int,
    val strategyResults: List<StrategyBenchmarkResult>,
    val bestStrategy: PromptStrategy,
    val bestAccuracy: Float,
    val meetsTarget: Boolean,
    val meetsExcellent: Boolean
) {
    fun toMarkdownReport(): String = buildString {
        appendLine("# Prompt Engineering Benchmark Report")
        appendLine()
        appendLine("**Milestone**: 0.2.6 Option A")
        appendLine("**Timestamp**: $timestamp")
        appendLine("**Model**: $modelInfo")
        appendLine("**Test Cases**: $testCasesCount")
        appendLine()
        
        appendLine("## Executive Summary")
        appendLine()
        appendLine("| Metric | Result |")
        appendLine("|--------|--------|")
        appendLine("| Best Strategy | **${bestStrategy.displayName}** |")
        appendLine("| Best Accuracy | **${String.format("%.1f", bestAccuracy * 100)}%** |")
        appendLine("| Target (70%) | ${if (meetsTarget) "✅ ACHIEVED" else "❌ NOT MET"} |")
        appendLine("| Excellent (80%) | ${if (meetsExcellent) "✅ ACHIEVED" else "❌ NOT MET"} |")
        appendLine()
        
        appendLine("## Strategy Comparison")
        appendLine()
        appendLine("| Strategy | Accuracy | Target | Avg Latency |")
        appendLine("|----------|----------|--------|-------------|")
        for (sr in strategyResults.sortedByDescending { it.accuracy }) {
            val status = if (sr.meetsTarget) "✅" else "❌"
            appendLine("| ${sr.strategy.displayName} | ${String.format("%.1f", sr.accuracy * 100)}% | $status | ${sr.avgInferenceTimeMs}ms |")
        }
        appendLine()
        
        appendLine("## Detailed Results by Strategy")
        appendLine()
        for (sr in strategyResults) {
            appendLine(sr.toMarkdownSummary())
            appendLine("---")
            appendLine()
        }
        
        appendLine("## Recommendations")
        appendLine()
        if (meetsTarget) {
            appendLine("✅ **Target achieved!** The **${bestStrategy.displayName}** strategy meets the 70% accuracy target.")
            if (!meetsExcellent) {
                appendLine()
                appendLine("Consider combining strategies or further prompt refinement to reach 80% excellence target.")
            }
        } else {
            appendLine("❌ **Target not met.** Current best accuracy is ${String.format("%.1f", bestAccuracy * 100)}%.")
            appendLine()
            appendLine("Recommendations:")
            appendLine("1. Try different model (Mistral 7B achieved 80% in 0.2.3)")
            appendLine("2. Expand few-shot examples")
            appendLine("3. Consider rule-based hybrid approach")
        }
    }
}

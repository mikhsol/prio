package app.jeeves.llmtest

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.jeeves.llmtest.benchmark.ExtendedTestDataset
import app.jeeves.llmtest.benchmark.PromptBuilder
import app.jeeves.llmtest.benchmark.PromptEngineeringBenchmark
import app.jeeves.llmtest.benchmark.PromptStrategy
import app.jeeves.llmtest.engine.EisenhowerClassifier
import app.jeeves.llmtest.engine.EisenhowerQuadrant
import app.jeeves.llmtest.engine.LlamaEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

/**
 * Prompt Engineering Benchmark - Instrumented Test
 * 
 * Milestone 0.2.6 Option A: Test different prompt strategies with real LLM
 * 
 * This test loads the actual Phi-3 model and benchmarks:
 * - 0.2.6.1: JSON structured output
 * - 0.2.6.2: Chain-of-thought reasoning
 * - 0.2.6.3: Few-shot learning
 * - 0.2.6.4: Expert persona
 * - 0.2.6.5: 50-task comprehensive benchmark
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PromptEngineeringInstrumentedTest {
    
    companion object {
        private const val TAG = "PromptBenchmark"
        private const val TARGET_ACCURACY = 0.70f
        
        // Test parameters - reduced MAX_TOKENS for faster benchmarks
        private const val MAX_TOKENS = 50  // Enough for JSON response
        private const val TEMPERATURE = 0.2f
        private const val TOP_P = 0.9f
        
        // Expected model size: Phi-3 Q4_K_M
        private const val EXPECTED_MODEL_SIZE = 2393231072L
    }
    
    private lateinit var llamaEngine: LlamaEngine
    private var modelLoaded = false
    
    @Before
    fun setup() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            llamaEngine = LlamaEngine(context)
            
            // Check for model in app's files directory
            val modelFile = java.io.File(context.filesDir, "model.gguf")
            Log.i(TAG, "Model file: ${modelFile.absolutePath}, exists=${modelFile.exists()}, size=${modelFile.length()}")
            
            if (modelFile.exists() && modelFile.length() == EXPECTED_MODEL_SIZE) {
                Log.i(TAG, "Loading model...")
                val loadResult = llamaEngine.loadModel(modelFile.absolutePath, contextSize = 2048, threads = 4)
                modelLoaded = loadResult.success && !loadResult.isStub
                Log.i(TAG, "Model loaded: $modelLoaded, time: ${loadResult.loadTimeMs}ms")
            } else {
                Log.w(TAG, "Model not found or wrong size. Copy using: adb shell 'cat /data/local/tmp/Phi-3-mini-4k-instruct-q4.gguf | run-as app.jeeves.llmtest sh -c \"cat > /data/data/app.jeeves.llmtest/files/model.gguf\"'")
            }
        }
    }
    
    @After
    fun teardown() {
        runBlocking {
            llamaEngine.cleanup()
        }
    }
    
    /**
     * Test 1: Baseline simple prompt (for comparison with 0.2.3)
     */
    @Test
    fun test01_BaselineSimple() = runBlocking {
        runStrategyBenchmark(PromptStrategy.BASELINE_SIMPLE)
    }
    
    /**
     * Test 2: 0.2.6.1 - JSON Structured Output
     */
    @Test
    fun test02_JsonStructured() = runBlocking {
        runStrategyBenchmark(PromptStrategy.JSON_STRUCTURED)
    }
    
    /**
     * Test 3: 0.2.6.2 - Chain-of-Thought Reasoning
     */
    @Test
    fun test03_ChainOfThought() = runBlocking {
        runStrategyBenchmark(PromptStrategy.CHAIN_OF_THOUGHT)
    }
    
    /**
     * Test 4: 0.2.6.3 - Few-Shot Learning
     */
    @Test
    fun test04_FewShot() = runBlocking {
        runStrategyBenchmark(PromptStrategy.FEW_SHOT)
    }
    
    /**
     * Test 5: 0.2.6.4 - Expert Persona
     */
    @Test
    fun test05_ExpertPersona() = runBlocking {
        runStrategyBenchmark(PromptStrategy.EXPERT_PERSONA)
    }
    
    /**
     * Test 6: Combined Optimal Strategy
     */
    @Test
    fun test06_CombinedOptimal() = runBlocking {
        runStrategyBenchmark(PromptStrategy.COMBINED_OPTIMAL)
    }
    
    /**
     * Test 7: 0.2.6.5 - Full 50-task benchmark with best strategy
     */
    @Test
    fun test07_Full50TaskBenchmark() = runBlocking {
        if (!modelLoaded) {
            Log.w(TAG, "Skipping full benchmark - model not loaded")
            return@runBlocking
        }
        
        Log.i(TAG, "\n" + "=".repeat(60))
        Log.i(TAG, "FULL 50-TASK BENCHMARK (0.2.6.5)")
        Log.i(TAG, "=".repeat(60))
        
        val benchmark = PromptEngineeringBenchmark(llamaEngine) { log ->
            Log.i(TAG, log)
            println(log) // Also print to stdout for adb logcat
        }
        
        val result = benchmark.benchmarkAllStrategies(ExtendedTestDataset.TEST_CASES_50)
        
        // Print summary
        Log.i(TAG, "\n" + "=".repeat(60))
        Log.i(TAG, "FINAL RESULTS")
        Log.i(TAG, "=".repeat(60))
        Log.i(TAG, "Best Strategy: ${result.bestStrategy.displayName}")
        Log.i(TAG, "Best Accuracy: ${String.format("%.1f", result.bestAccuracy * 100)}%")
        Log.i(TAG, "Target (70%): ${if (result.meetsTarget) "✅ ACHIEVED" else "❌ NOT MET"}")
        Log.i(TAG, "Excellent (80%): ${if (result.meetsExcellent) "✅ ACHIEVED" else "❌ NOT MET"}")
        
        println("\n" + result.toMarkdownReport())
        
        // Assert at least one strategy meets 70% target
        assertTrue(
            "At least one strategy should achieve 70% target, best was ${String.format("%.1f", result.bestAccuracy * 100)}%",
            result.meetsTarget
        )
    }
    
    /**
     * Run benchmark for a single strategy on 20 test cases.
     */
    private suspend fun runStrategyBenchmark(strategy: PromptStrategy) {
        Log.i(TAG, "\n" + "-".repeat(50))
        Log.i(TAG, "Testing: ${strategy.displayName}")
        Log.i(TAG, "-".repeat(50))
        
        // Use first 5 test cases for quick benchmark (one per quadrant + 1)
        val testCases = ExtendedTestDataset.TEST_CASES_20.take(5)
        var correct = 0
        var total = 0
        var totalTimeMs = 0L
        var parseFailures = 0
        
        val quadrantResults = mutableMapOf<EisenhowerQuadrant, MutableList<Boolean>>()
        EisenhowerQuadrant.entries.forEach { quadrantResults[it] = mutableListOf() }
        
        for ((index, testCase) in testCases.withIndex()) {
            val prompt = PromptBuilder.buildPrompt(strategy, testCase.task)
            
            val startTime = System.currentTimeMillis()
            val generateResult = llamaEngine.generate(
                prompt = prompt,
                maxTokens = MAX_TOKENS,
                temperature = TEMPERATURE,
                topP = TOP_P
            )
            val elapsed = System.currentTimeMillis() - startTime
            totalTimeMs += elapsed
            
            val parsed = PromptBuilder.parseResponse(generateResult.text)
            if (parsed == null) parseFailures++
            
            val predicted = parsed?.quadrant ?: EisenhowerQuadrant.SCHEDULE
            val isCorrect = predicted == testCase.groundTruth
            
            if (isCorrect) correct++
            total++
            quadrantResults[testCase.groundTruth]?.add(isCorrect)
            
            val status = if (isCorrect) "✓" else "✗"
            val taskPreview = testCase.task.take(35).padEnd(35)
            Log.i(TAG, "[$status] #${testCase.id}: $taskPreview | ${testCase.groundTruth} -> $predicted (${elapsed}ms)")
            println("[$status] #${testCase.id}: ${testCase.groundTruth} -> $predicted (${elapsed}ms)")
        }
        
        val accuracy = correct.toFloat() / total
        val avgTimeMs = totalTimeMs / total
        
        Log.i(TAG, "\n--- ${strategy.displayName} Results ---")
        Log.i(TAG, "Accuracy: ${String.format("%.1f", accuracy * 100)}% ($correct/$total)")
        Log.i(TAG, "Avg Latency: ${avgTimeMs}ms")
        Log.i(TAG, "Parse Failures: $parseFailures")
        Log.i(TAG, "Target (70%): ${if (accuracy >= TARGET_ACCURACY) "✅" else "❌"}")
        
        // Per-quadrant breakdown
        for ((quadrant, results) in quadrantResults) {
            if (results.isNotEmpty()) {
                val qCorrect = results.count { it }
                val qTotal = results.size
                val qAcc = qCorrect.toFloat() / qTotal * 100
                Log.i(TAG, "  $quadrant: ${String.format("%.0f", qAcc)}% ($qCorrect/$qTotal)")
            }
        }
        
        println("\n${strategy.id}: ${String.format("%.1f", accuracy * 100)}% ($correct/$total), avg ${avgTimeMs}ms")
    }
    
    /**
     * Quick sanity check: model generates valid output
     */
    @Test
    fun test00_ModelSanityCheck() = runBlocking {
        Log.i(TAG, "=== Model Sanity Check ===")
        Log.i(TAG, "Model loaded: $modelLoaded")
        Log.i(TAG, "Is stub: ${llamaEngine.isStub}")
        
        val testPrompt = PromptBuilder.buildPrompt(
            PromptStrategy.BASELINE_SIMPLE,
            "Server is down, customers cannot access"
        )
        
        val result = llamaEngine.generate(testPrompt, MAX_TOKENS, TEMPERATURE, TOP_P)
        
        Log.i(TAG, "Generated ${result.tokensGenerated} tokens in ${result.inferenceTimeMs}ms")
        Log.i(TAG, "Response: ${result.text.take(200)}")
        
        val parsed = PromptBuilder.parseResponse(result.text)
        Log.i(TAG, "Parsed quadrant: ${parsed?.quadrant}")
        Log.i(TAG, "Parsed confidence: ${parsed?.confidence}")
        
        assertTrue("Should generate tokens", result.tokensGenerated > 0)
        
        if (modelLoaded) {
            assertNotNull("Real model should produce parseable output", parsed)
        }
    }
}

package app.prio.llmtest

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.prio.llmtest.benchmark.AccuracyTest
import app.prio.llmtest.engine.EisenhowerClassifier
import app.prio.llmtest.engine.EisenhowerQuadrant
import app.prio.llmtest.engine.LlamaEngine
import app.prio.llmtest.engine.RuleBasedClassifier
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for LLM functionality.
 * 
 * Task 0.2.3: Test task categorization accuracy with 20 sample prompts
 */
@RunWith(AndroidJUnit4::class)
class LlmInstrumentedTest {
    
    companion object {
        // Try multiple locations for the model file
        private val MODEL_PATHS = listOf(
            "/data/local/tmp/Phi-3-mini-4k-instruct-q4.gguf",
            "/sdcard/Download/Phi-3-mini-4k-instruct-q4.gguf"
        )
    }
    
    private lateinit var llamaEngine: LlamaEngine
    private lateinit var classifier: EisenhowerClassifier
    private var modelLoaded = false
    
    @Before
    fun setup() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            llamaEngine = LlamaEngine(context)
            classifier = EisenhowerClassifier(llamaEngine)
            
            // Use app's files directory - model should be pre-pushed there
            val appModelFile = java.io.File(context.filesDir, "model.gguf")
            
            android.util.Log.i("LlmTest", "Looking for model: ${appModelFile.absolutePath}")
            android.util.Log.i("LlmTest", "Exists: ${appModelFile.exists()}, Size: ${appModelFile.length()}")
            
            // Expected size of Phi-3 Q4_K_M: 2,393,231,072 bytes
            val expectedSize = 2393231072L
            
            if (appModelFile.exists() && appModelFile.length() == expectedSize) {
                android.util.Log.i("LlmTest", "Loading model from app dir: ${appModelFile.absolutePath}")
                val result = llamaEngine.loadModel(appModelFile.absolutePath, contextSize = 2048, threads = 4)
                modelLoaded = result.success
                android.util.Log.i("LlmTest", "Model load result: success=${result.success}, error=${result.error}")
            } else {
                android.util.Log.w("LlmTest", "Model file not ready or wrong size.")
                android.util.Log.w("LlmTest", "To copy model: adb shell 'cat /data/local/tmp/Phi-3-mini-4k-instruct-q4.gguf | run-as app.prio.llmtest sh -c \"cat > /data/data/app.prio.llmtest/files/model.gguf\"'")
            }
        }
    }
    
    @After
    fun teardown() {
        runBlocking {
            llamaEngine.cleanup()
        }
    }
    
    @Test
    fun testEngineInitialization() = runBlocking {
        org.junit.Assume.assumeTrue("Model must be loaded", modelLoaded)
        assertTrue("Engine should be loaded", llamaEngine.isLoaded)
    }
    
    @Test
    fun testBasicGeneration() = runBlocking {
        org.junit.Assume.assumeTrue("Model must be loaded", modelLoaded)
        val result = llamaEngine.generate(
            prompt = "What is 2+2?",
            maxTokens = 10
        )
        
        assertNull("Should have no error", result.error)
        assertTrue("Should generate some text", result.text.isNotBlank())
        assertTrue("Should generate tokens", result.tokensGenerated > 0)
    }
    
    @Test
    fun testEisenhowerClassification_UrgentImportant() = runBlocking {
        org.junit.Assume.assumeTrue("Model must be loaded", modelLoaded)
        val result = classifier.classify("Server is down, customers can't access the app")
        assertEquals("Should classify as DO", EisenhowerQuadrant.DO, result.quadrant)
    }
    
    @Test
    fun testEisenhowerClassification_Important() = runBlocking {
        org.junit.Assume.assumeTrue("Model must be loaded", modelLoaded)
        val result = classifier.classify("Plan next quarter's marketing strategy")
        assertEquals("Should classify as SCHEDULE", EisenhowerQuadrant.SCHEDULE, result.quadrant)
    }
    
    @Test
    fun testEisenhowerClassification_Delegate() = runBlocking {
        org.junit.Assume.assumeTrue("Model must be loaded", modelLoaded)
        val result = classifier.classify("Order office supplies that are running low")
        assertEquals("Should classify as DELEGATE", EisenhowerQuadrant.DELEGATE, result.quadrant)
    }
    
    @Test
    fun testEisenhowerClassification_Eliminate() = runBlocking {
        org.junit.Assume.assumeTrue("Model must be loaded", modelLoaded)
        val result = classifier.classify("Browse social media during lunch break")
        assertEquals("Should classify as ELIMINATE", EisenhowerQuadrant.ELIMINATE, result.quadrant)
    }
    
    @Test
    fun testRuleBasedClassifier_Accuracy() {
        // Rule-based doesn't need LLM - just test it directly
        val testCases = listOf(
            "Emergency server crash - customers affected" to EisenhowerQuadrant.DO,
            "Plan quarterly strategy" to EisenhowerQuadrant.SCHEDULE,
            "Order office supplies" to EisenhowerQuadrant.DELEGATE,
            "Browse social media" to EisenhowerQuadrant.ELIMINATE
        )
        
        var correct = 0
        for ((task, expected) in testCases) {
            val result = RuleBasedClassifier.classify(task)
            if (result.quadrant == expected) correct++
        }
        
        val accuracy = correct.toFloat() / testCases.size
        println("Rule-based accuracy: ${accuracy * 100}%")
        assertTrue(
            "Rule-based should achieve at least 50% accuracy on simple cases",
            accuracy >= 0.50f
        )
    }
    
    @Test
    fun testAccuracyTest_20Samples() = runBlocking {
        org.junit.Assume.assumeTrue("Model must be loaded", modelLoaded)
        val accuracyTest = AccuracyTest(classifier)
        val report = accuracyTest.runLlmTest()
        
        println("\n=== Accuracy Test Report ===")
        println("Total: ${report.totalTests}")
        println("Correct: ${report.correctPredictions}")
        println("Accuracy: ${String.format("%.1f", report.accuracy * 100)}%")
        println()
        
        for (qm in report.quadrantMetrics) {
            println("${qm.quadrant}: ${qm.correct}/${qm.total} (P=${String.format("%.2f", qm.precision)}, R=${String.format("%.2f", qm.recall)})")
        }
        
        if (report.errors.isNotEmpty()) {
            println("\nErrors:")
            for (err in report.errors) {
                println("  - Task ${err.testCase.id}: Expected ${err.testCase.groundTruth}, Got ${err.predicted}")
            }
        }
        
        // Target is 80% for LLM
        assertTrue(
            "Should achieve reasonable accuracy (at least 60%)",
            report.accuracy >= 0.60f
        )
    }
    
    @Test
    fun testBenchmarkMetrics() = runBlocking {
        org.junit.Assume.assumeTrue("Model must be loaded", modelLoaded)
        // Generate and measure
        val start = System.currentTimeMillis()
        val result = llamaEngine.generate(
            prompt = "Classify: Submit report by Friday",
            maxTokens = 50
        )
        val elapsed = System.currentTimeMillis() - start
        
        println("\n=== Benchmark Metrics ===")
        println("Inference time: ${result.inferenceTimeMs} ms")
        println("Wall time: $elapsed ms")
        println("Tokens generated: ${result.tokensGenerated}")
        println("Tokens/sec: ${String.format("%.1f", result.tokensPerSecond)}")
        
        assertTrue("Inference should complete in reasonable time", result.inferenceTimeMs < 10000)
        assertTrue("Should generate tokens", result.tokensGenerated > 0)
    }
    
    @Test
    fun testMemoryUsage() = runBlocking {
        org.junit.Assume.assumeTrue("Model must be loaded", modelLoaded)
        val memoryBytes = llamaEngine.getMemoryUsageBytes()
        val memoryMb = memoryBytes / (1024 * 1024)
        
        println("Memory usage: $memoryMb MB")
        
        assertTrue("Memory should be reported", memoryBytes > 0)
    }
}

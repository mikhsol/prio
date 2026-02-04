package app.jeeves.llmtest

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.jeeves.llmtest.benchmark.AccuracyTest
import app.jeeves.llmtest.benchmark.ExtendedTestDataset
import app.jeeves.llmtest.benchmark.FullBenchmarkResult
import app.jeeves.llmtest.benchmark.LlmBenchmark
import app.jeeves.llmtest.benchmark.PromptEngineeringBenchmark
import app.jeeves.llmtest.benchmark.PromptStrategy
import app.jeeves.llmtest.engine.EisenhowerClassifier
import app.jeeves.llmtest.engine.LlamaEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for LLM Test main screen.
 * 
 * Milestone 0.2.6: Added prompt engineering benchmark support.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val llamaEngine = LlamaEngine(application)
    private val classifier = EisenhowerClassifier(llamaEngine)
    private val benchmark = LlmBenchmark(application, llamaEngine)
    private val accuracyTest = AccuracyTest(classifier)
    private val promptBenchmark = PromptEngineeringBenchmark(llamaEngine) { log ->
        addLog(log)
    }
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        collectDeviceInfo()
    }
    
    private fun collectDeviceInfo() {
        val runtime = Runtime.getRuntime()
        _uiState.update {
            it.copy(
                deviceInfo = DeviceInfoUi(
                    manufacturer = Build.MANUFACTURER,
                    model = Build.MODEL,
                    cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
                    cpuCores = runtime.availableProcessors(),
                    totalRamMb = runtime.maxMemory() / (1024 * 1024),
                    sdkVersion = Build.VERSION.SDK_INT
                )
            )
        }
    }
    
    fun loadModel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            addLog("Loading model (stub mode)...")
            
            try {
                val result = llamaEngine.loadStubModel()
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isModelLoaded = result.success,
                        isStub = result.isStub,
                        loadTimeMs = result.loadTimeMs,
                        memoryUsageMb = result.memoryBytes / (1024 * 1024)
                    )
                }
                
                if (result.success) {
                    addLog("✅ Model loaded in ${result.loadTimeMs}ms")
                } else {
                    addLog("❌ Failed to load: ${result.error}")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                addLog("❌ Error: ${e.message}")
            }
        }
    }
    
    fun unloadModel() {
        viewModelScope.launch {
            llamaEngine.unload()
            _uiState.update {
                it.copy(
                    isModelLoaded = false,
                    isStub = true,
                    loadTimeMs = 0,
                    memoryUsageMb = 0
                )
            }
            addLog("Model unloaded")
        }
    }
    
    fun runBenchmark() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBenchmarking = true, benchmarkResult = null) }
            addLog("Starting benchmark...")
            
            try {
                val report = benchmark.runFullBenchmark()
                
                val summary = buildString {
                    appendLine("Tokens/sec: ${String.format("%.1f", report.summary.avgTokensPerSecond)}")
                    appendLine("Avg Time: ${report.summary.avgInferenceTimeMs}ms")
                    appendLine("TTFT: ${String.format("%.0f", report.summary.firstTokenLatencyMs)}ms")
                    appendLine("Mode: ${if (report.modelInfo.isStub) "Stub" else "Real"}")
                }
                
                _uiState.update {
                    it.copy(
                        isBenchmarking = false,
                        benchmarkResult = summary
                    )
                }
                addLog("✅ Benchmark complete: ${String.format("%.1f", report.summary.avgTokensPerSecond)} t/s")
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isBenchmarking = false) }
                addLog("❌ Benchmark error: ${e.message}")
            }
        }
    }
    
    fun runAccuracyTest(useLlm: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingAccuracy = true) }
            addLog("Running ${if (useLlm) "LLM" else "Rule-Based"} accuracy test...")
            
            try {
                val report = if (useLlm) {
                    accuracyTest.runLlmTest()
                } else {
                    accuracyTest.runRuleBasedTest()
                }
                
                _uiState.update {
                    if (useLlm) {
                        it.copy(
                            isTestingAccuracy = false,
                            llmAccuracy = report.accuracy
                        )
                    } else {
                        it.copy(
                            isTestingAccuracy = false,
                            ruleBasedAccuracy = report.accuracy
                        )
                    }
                }
                
                val status = if (report.passesTarget) "✅ PASS" else "❌ FAIL"
                addLog("${report.classifierName}: ${String.format("%.1f", report.accuracy * 100)}% $status (target: 80%)")
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isTestingAccuracy = false) }
                addLog("❌ Test error: ${e.message}")
            }
        }
    }
    
    /**
     * Milestone 0.2.6: Run prompt engineering benchmark.
     * Tests all prompt strategies on 50 diverse tasks.
     */
    fun runPromptEngineeringBenchmark(quick: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingPrompts = true, promptBenchmarkResult = null) }
            addLog("Starting Prompt Engineering Benchmark (0.2.6)...")
            addLog("Test cases: ${if (quick) 20 else 50}")
            
            try {
                val result = if (quick) {
                    promptBenchmark.quickBenchmark()
                } else {
                    promptBenchmark.benchmarkAllStrategies()
                }
                
                _uiState.update {
                    it.copy(
                        isTestingPrompts = false,
                        promptBenchmarkResult = result
                    )
                }
                
                val status = if (result.meetsTarget) "✅ ACHIEVED" else "❌ NOT MET"
                addLog("Best: ${result.bestStrategy.displayName} at ${String.format("%.1f", result.bestAccuracy * 100)}%")
                addLog("70% target: $status")
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isTestingPrompts = false) }
                addLog("❌ Prompt benchmark error: ${e.message}")
            }
        }
    }
    
    /**
     * Test a single prompt strategy.
     */
    fun testSingleStrategy(strategy: PromptStrategy, quick: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingPrompts = true) }
            addLog("Testing strategy: ${strategy.displayName}...")
            
            try {
                val testCases = if (quick) ExtendedTestDataset.TEST_CASES_20 else ExtendedTestDataset.TEST_CASES_50
                val result = promptBenchmark.benchmarkStrategy(strategy, testCases)
                
                addLog("${strategy.displayName}: ${String.format("%.1f", result.accuracy * 100)}% (${result.correct}/${result.total})")
                addLog("Avg latency: ${result.avgInferenceTimeMs}ms")
                
                _uiState.update { it.copy(isTestingPrompts = false) }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isTestingPrompts = false) }
                addLog("❌ Strategy test error: ${e.message}")
            }
        }
    }
    
    fun classifyTask(taskText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isClassifying = true) }
            
            try {
                val result = classifier.classify(taskText)
                
                _uiState.update {
                    it.copy(
                        isClassifying = false,
                        lastClassification = ClassificationResultUi(
                            quadrant = result.quadrant.name,
                            confidence = result.confidence,
                            reasoning = result.reasoning,
                            source = result.source.name
                        )
                    )
                }
                
                addLog("Classified as ${result.quadrant} (${String.format("%.0f", result.confidence * 100)}%)")
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isClassifying = false) }
                addLog("❌ Classification error: ${e.message}")
            }
        }
    }
    
    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
        _uiState.update {
            it.copy(logs = it.logs + "[$timestamp] $message")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            llamaEngine.cleanup()
        }
    }
}

data class MainUiState(
    val deviceInfo: DeviceInfoUi = DeviceInfoUi(),
    val isLoading: Boolean = false,
    val isModelLoaded: Boolean = false,
    val isStub: Boolean = true,
    val loadTimeMs: Long = 0,
    val memoryUsageMb: Long = 0,
    val isBenchmarking: Boolean = false,
    val benchmarkResult: String? = null,
    val isTestingAccuracy: Boolean = false,
    val llmAccuracy: Float? = null,
    val ruleBasedAccuracy: Float? = null,
    val isClassifying: Boolean = false,
    val lastClassification: ClassificationResultUi? = null,
    val isTestingPrompts: Boolean = false,
    val promptBenchmarkResult: FullBenchmarkResult? = null,
    val logs: List<String> = emptyList()
)

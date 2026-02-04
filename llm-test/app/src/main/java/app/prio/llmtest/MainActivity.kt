package app.prio.llmtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.prio.llmtest.ui.theme.LlmTestTheme

/**
 * Main Activity for LLM Test Application.
 * 
 * Milestone 0.2: On-Device LLM Technical Research
 * 
 * This app provides:
 * - Task 0.2.1: llama.cpp Android test project with JNI
 * - Task 0.2.2: Phi-3-mini benchmarking
 * - Task 0.2.3: Eisenhower classification accuracy testing
 * - Task 0.2.4: Device compatibility information
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LlmTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prio LLM Test") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device Info Card
            item {
                DeviceInfoCard(uiState.deviceInfo)
            }
            
            // Model Status Card
            item {
                ModelStatusCard(
                    isLoaded = uiState.isModelLoaded,
                    isStub = uiState.isStub,
                    loadTimeMs = uiState.loadTimeMs,
                    memoryMb = uiState.memoryUsageMb,
                    onLoadModel = { viewModel.loadModel() },
                    onUnloadModel = { viewModel.unloadModel() },
                    isLoading = uiState.isLoading
                )
            }
            
            // Benchmark Card
            item {
                BenchmarkCard(
                    isEnabled = uiState.isModelLoaded && !uiState.isLoading,
                    onRunBenchmark = { viewModel.runBenchmark() },
                    benchmarkResult = uiState.benchmarkResult,
                    isRunning = uiState.isBenchmarking
                )
            }
            
            // Accuracy Test Card
            item {
                AccuracyTestCard(
                    isEnabled = uiState.isModelLoaded && !uiState.isLoading,
                    onRunLlmTest = { viewModel.runAccuracyTest(useLlm = true) },
                    onRunRuleBasedTest = { viewModel.runAccuracyTest(useLlm = false) },
                    llmAccuracy = uiState.llmAccuracy,
                    ruleBasedAccuracy = uiState.ruleBasedAccuracy,
                    isRunning = uiState.isTestingAccuracy
                )
            }
            
            // Classification Demo Card
            item {
                ClassificationDemoCard(
                    isEnabled = uiState.isModelLoaded && !uiState.isLoading,
                    onClassify = { viewModel.classifyTask(it) },
                    lastResult = uiState.lastClassification,
                    isClassifying = uiState.isClassifying
                )
            }
            
            // Results Log
            if (uiState.logs.isNotEmpty()) {
                item {
                    LogCard(logs = uiState.logs)
                }
            }
        }
    }
}

@Composable
fun DeviceInfoCard(info: DeviceInfoUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Device Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Device: ${info.manufacturer} ${info.model}")
            Text("CPU: ${info.cpuCores} cores (${info.cpuAbi})")
            Text("RAM: ${info.totalRamMb} MB available")
            Text("Android: API ${info.sdkVersion}")
        }
    }
}

@Composable
fun ModelStatusCard(
    isLoaded: Boolean,
    isStub: Boolean,
    loadTimeMs: Long,
    memoryMb: Long,
    onLoadModel: () -> Unit,
    onUnloadModel: () -> Unit,
    isLoading: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Model Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoaded) {
                Text("✅ Model Loaded")
                Text("Mode: ${if (isStub) "Stub (simulated)" else "Real llama.cpp"}")
                Text("Load Time: ${loadTimeMs} ms")
                Text("Memory: ${memoryMb} MB")
            } else {
                Text("❌ Model Not Loaded")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onLoadModel,
                    enabled = !isLoaded && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Load Model")
                    }
                }
                
                OutlinedButton(
                    onClick = onUnloadModel,
                    enabled = isLoaded && !isLoading
                ) {
                    Text("Unload")
                }
            }
        }
    }
}

@Composable
fun BenchmarkCard(
    isEnabled: Boolean,
    onRunBenchmark: () -> Unit,
    benchmarkResult: String?,
    isRunning: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Performance Benchmark",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Task 0.2.2: Benchmark Phi-3-mini performance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onRunBenchmark,
                enabled = isEnabled && !isRunning
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Running...")
                } else {
                    Text("Run Benchmark")
                }
            }
            
            if (benchmarkResult != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = benchmarkResult,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun AccuracyTestCard(
    isEnabled: Boolean,
    onRunLlmTest: () -> Unit,
    onRunRuleBasedTest: () -> Unit,
    llmAccuracy: Float?,
    ruleBasedAccuracy: Float?,
    isRunning: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Accuracy Test (20 samples)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Task 0.2.3: Test Eisenhower classification accuracy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRunLlmTest,
                    enabled = isEnabled && !isRunning
                ) {
                    Text("Test LLM")
                }
                
                OutlinedButton(
                    onClick = onRunRuleBasedTest,
                    enabled = !isRunning
                ) {
                    Text("Test Rule-Based")
                }
            }
            
            if (isRunning) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            if (llmAccuracy != null || ruleBasedAccuracy != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                llmAccuracy?.let {
                    val passColor = if (it >= 0.8f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    Text(
                        text = "LLM Accuracy: ${String.format("%.1f", it * 100)}% ${if (it >= 0.8f) "✅" else "❌"}",
                        color = passColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                ruleBasedAccuracy?.let {
                    Text(
                        text = "Rule-Based Accuracy: ${String.format("%.1f", it * 100)}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassificationDemoCard(
    isEnabled: Boolean,
    onClassify: (String) -> Unit,
    lastResult: ClassificationResultUi?,
    isClassifying: Boolean
) {
    var taskText by remember { mutableStateOf("") }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Try Classification",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = taskText,
                onValueChange = { taskText = it },
                label = { Text("Enter a task...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = isEnabled && !isClassifying
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { onClassify(taskText) },
                enabled = isEnabled && taskText.isNotBlank() && !isClassifying
            ) {
                if (isClassifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Classify")
            }
            
            lastResult?.let { result ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = result.quadrant,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Confidence: ${String.format("%.0f", result.confidence * 100)}%")
                        Text("Source: ${result.source}")
                        Text(
                            text = result.reasoning,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogCard(logs: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Logs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            logs.takeLast(10).forEach { log ->
                Text(
                    text = log,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// UI State classes
data class DeviceInfoUi(
    val manufacturer: String = "",
    val model: String = "",
    val cpuAbi: String = "",
    val cpuCores: Int = 0,
    val totalRamMb: Long = 0,
    val sdkVersion: Int = 0
)

data class ClassificationResultUi(
    val quadrant: String,
    val confidence: Float,
    val reasoning: String,
    val source: String
)

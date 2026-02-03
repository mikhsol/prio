# Jeeves - Technical Architecture

## Overview

Jeeves is designed as an **offline-first, privacy-first** personal assistant with an Android-first architecture. The system prioritizes on-device processing, minimal backend dependency, and a pluggable module system. Core AI features run locally using on-device LLMs, enabling full functionality without internet connectivity.

---

## Architecture Principles

### Core Design Philosophy

1. **Offline-First**: All MVP features work without internet connectivity
2. **On-Device AI**: LLM inference runs locally on the smartphone
3. **Privacy-First**: Personal data never leaves the device unless user opts in
4. **Pluggable Architecture**: Features are modular plugins that can be added/removed
5. **Cost-Efficient**: Minimal backend infrastructure = minimal operational costs
6. **Cross-Platform Ready**: Kotlin Multiplatform for future iOS support

### 12-Factor App Compliance (Adapted for Mobile)
1. **Codebase**: Single repo with modular structure
2. **Dependencies**: Explicitly declared via Gradle catalogs
3. **Config**: BuildConfig and local properties, never hardcoded
4. **Backing Services**: Local Room DB as primary, cloud as optional
5. **Build, Release, Run**: CI/CD with signed releases
6. **Processes**: Single-process Android app with background workers
7. **Port Binding**: N/A for mobile (API server optional)
8. **Concurrency**: Kotlin Coroutines for async operations
9. **Disposability**: Fast app startup, graceful state restoration
10. **Dev/Prod Parity**: Debug/Release builds mirror production behavior
11. **Logs**: Structured logging with Timber
12. **Admin Processes**: Developer tools in debug builds only

---

## System Architecture

### High-Level Architecture (Offline-First)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ANDROID APPLICATION                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         PRESENTATION LAYER                              │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │ │
│  │  │   Jetpack    │  │   Widgets    │  │ Notifications │  │  Quick     │  │ │
│  │  │   Compose    │  │   (Glance)   │  │    Service    │  │  Settings  │  │ │
│  │  │   Screens    │  │              │  │               │  │   Tile     │  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                         │
│  ┌────────────────────────────────┴───────────────────────────────────────┐ │
│  │                         DOMAIN LAYER                                    │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │ │
│  │  │  ViewModels  │  │   Use Cases  │  │   Domain     │  │  Plugin    │  │ │
│  │  │   (MVVM)     │  │              │  │   Models     │  │  Manager   │  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                         │
│  ┌────────────────────────────────┴───────────────────────────────────────┐ │
│  │                          AI LAYER (ON-DEVICE)                           │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │ │
│  │  │   LLM        │  │   NLP        │  │   Task       │  │  Intent    │  │ │
│  │  │   Engine     │  │   Processor  │  │   Classifier │  │  Parser    │  │ │
│  │  │ (llama.cpp)  │  │              │  │              │  │            │  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                         │
│  ┌────────────────────────────────┴───────────────────────────────────────┐ │
│  │                          DATA LAYER                                     │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │ │
│  │  │ Repositories │  │   Room DB    │  │  DataStore   │  │  Calendar  │  │ │
│  │  │              │  │  (SQLite)    │  │ (Preferences)│  │  Provider  │  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                         │
│  ┌────────────────────────────────┴───────────────────────────────────────┐ │
│  │                          PLUGIN SYSTEM                                  │ │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐          │ │
│  │  │   Tasks    │ │   Goals    │ │  Calendar  │ │  [Future]  │          │ │
│  │  │   Plugin   │ │   Plugin   │ │   Plugin   │ │  Plugins   │          │ │
│  │  └────────────┘ └────────────┘ └────────────┘ └────────────┘          │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                          ┌──────────┴──────────┐
                          │   OPTIONAL CLOUD    │
                          │   (Sync & Backup)   │
                          └─────────────────────┘
```

### Optional Backend Architecture (Minimal)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          OPTIONAL BACKEND SERVICES                           │
│                        (Only for Premium Features)                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   Auth Service   │  │   Sync Service   │  │   Backup Service  │          │
│  │      (Go)        │  │     (Rust)       │  │       (Go)        │          │
│  │                  │  │                  │  │                   │          │
│  │  • OAuth 2.0     │  │  • E2E Encrypted │  │  • Encrypted      │          │
│  │  • JWT tokens    │  │  • CRDT sync     │  │    blob storage   │          │
│  │  • Device mgmt   │  │  • Conflict res  │  │  • Point-in-time  │          │
│  └────────┬─────────┘  └────────┬─────────┘  └─────────┬─────────┘          │
│           │                     │                       │                    │
│  ┌────────┴─────────┐          │                       │                    │
│  │   AI Gateway     │          │                       │                    │
│  │     (Rust)       │──────────┴───────────────────────┘                    │
│  │                  │                                                        │
│  │  • Model routing │                                                        │
│  │  • Usage tracking│                                                        │
│  │  • Rate limiting │                                                        │
│  │  • Cost control  │                                                        │
│  └────────┬─────────┘                                                        │
│           │                                                                  │
│           ├─────────────────────┬───────────────────────┐                    │
│           ▼                     ▼                       ▼                    │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   OpenAI API     │  │  Anthropic API   │  │   Google API     │          │
│  │   (GPT-4o)       │  │  (Claude family) │  │   (Gemini)       │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│           │                                                                  │
│           ├───────────────────────────────────────────────┐                  │
│           ▼                                               ▼                  │
│  ┌──────────────────┐                         ┌──────────────────┐          │
│  │    xAI API       │                         │     Redis        │          │
│  │    (Grok)        │                         │  (Usage cache,   │          │
│  └──────────────────┘                         │   rate limits)   │          │
│                                               └──────────────────┘          │
│                                 │                                            │
│                     ┌───────────┴───────────┐                                │
│                     │     PostgreSQL        │                                │
│                     │   (User accounts,     │                                │
│                     │    sync metadata,     │                                │
│                     │    AI usage, agents)  │                                │
│                     └───────────────────────┘                                │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## AI Provider Architecture

### Design Goals

The AI layer is designed with **provider abstraction** as a core principle, enabling:
1. **Easy Model Replacement**: Swap LLM models without code changes
2. **Hybrid AI Routing**: Seamlessly switch between on-device and cloud providers
3. **Backend Proxy Ready**: Connect to cloud LLMs via backend gateway when needed
4. **Graceful Fallback**: Rule-based fallback when AI unavailable
5. **Provider Agnostic**: Same interface works for all AI backends

### AI Provider Abstraction Layer

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AI PROVIDER ABSTRACTION LAYER                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         AiProvider INTERFACE                            │ │
│  │                                                                          │ │
│  │  interface AiProvider {                                                  │ │
│  │    val providerId: String                                               │ │
│  │    val capabilities: Set<AiCapability>                                  │ │
│  │    val isAvailable: Flow<Boolean>                                       │ │
│  │                                                                          │ │
│  │    suspend fun complete(request: AiRequest): AiResponse                 │ │
│  │    suspend fun stream(request: AiRequest): Flow<AiStreamChunk>          │ │
│  │    suspend fun getModelInfo(): ModelInfo                                │ │
│  │  }                                                                       │ │
│  │                                                                          │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                         │
│  ┌─────────────┬──────────────────┼───────────────────┬──────────────────┐  │
│  │             │                  │                   │                  │  │
│  ▼             ▼                  ▼                   ▼                  ▼  │
│  ┌───────────┐ ┌────────────────┐ ┌─────────────────┐ ┌────────────────┐   │
│  │  OnDevice │ │ CloudGateway   │ │   DirectCloud   │ │  RuleBased     │   │
│  │  Provider │ │ Provider       │ │   Provider      │ │  Fallback      │   │
│  │           │ │ (via Backend)  │ │ (direct API)    │ │  Provider      │   │
│  │ • llama.cpp│ │               │ │                 │ │                │   │
│  │ • GGUF    │ │ • Auth proxy   │ │ • OpenAI        │ │ • Regex        │   │
│  │ • Local   │ │ • Cost control │ │ • Anthropic     │ │ • Keywords     │   │
│  │           │ │ • Rate limit   │ │ • Google        │ │ • No network   │   │
│  └───────────┘ └────────────────┘ └─────────────────┘ └────────────────┘   │
│       │               │                   │                  │              │
│       └───────────────┴───────────────────┴──────────────────┘              │
│                                    │                                         │
│  ┌────────────────────────────────┴───────────────────────────────────────┐ │
│  │                         AiProviderRouter                                │ │
│  │                                                                          │ │
│  │  • Selects optimal provider based on context                            │ │
│  │  • Implements fallback chain: OnDevice → Cloud → RuleBased              │ │
│  │  • Respects user preferences (privacy mode, cost limits)                │ │
│  │  • Monitors provider availability                                       │ │
│  │                                                                          │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Core Interfaces (Kotlin)

```kotlin
/**
 * Unified AI request format used across all providers.
 * Designed to be serializable for both local and network use.
 */
@Serializable
data class AiRequest(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val systemPrompt: String? = null,
    val taskType: AiTaskType,
    val parameters: AiParameters = AiParameters(),
    val context: Map<String, String> = emptyMap(),
    val metadata: RequestMetadata = RequestMetadata()
)

@Serializable
data class AiParameters(
    val maxTokens: Int = 256,
    val temperature: Float = 0.3f,
    val topP: Float = 0.9f,
    val stopSequences: List<String> = emptyList()
)

@Serializable
data class RequestMetadata(
    val clientVersion: String = BuildConfig.VERSION_NAME,
    val requestedAt: Long = System.currentTimeMillis(),
    val preferredProvider: String? = null,  // User preference
    val allowCloudFallback: Boolean = true
)

@Serializable
data class AiResponse(
    val requestId: String,
    val text: String,
    val providerId: String,           // Which provider handled this
    val modelId: String,              // Which specific model
    val tokenUsage: TokenUsage? = null,
    val latencyMs: Long,
    val metadata: ResponseMetadata = ResponseMetadata()
)

@Serializable
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val estimatedCostUsd: Float? = null  // For cloud providers
)

enum class AiTaskType {
    CLASSIFY_EISENHOWER,    // Classify task into quadrant
    PARSE_TASK,             // Extract task details from natural language
    GENERATE_BRIEFING,      // Generate daily briefing
    EXTRACT_ACTIONS,        // Extract action items from text
    SUGGEST_TASKS,          // Suggest tasks for goals
    SUMMARIZE,              // Summarize meeting notes
    CHAT                    // Free-form agent chat
}

enum class AiCapability {
    CLASSIFICATION,         // Can classify text
    EXTRACTION,             // Can extract structured data
    GENERATION,             // Can generate text
    STREAMING,              // Supports streaming responses
    LONG_CONTEXT,           // Supports >4K tokens
    FUNCTION_CALLING        // Supports tool/function calls
}

/**
 * Core AI Provider interface - all providers implement this.
 * Designed for easy swapping and testing.
 */
interface AiProvider {
    val providerId: String
    val displayName: String
    val capabilities: Set<AiCapability>
    val isAvailable: StateFlow<Boolean>
    
    /** Synchronous completion (blocks until done) */
    suspend fun complete(request: AiRequest): Result<AiResponse>
    
    /** Streaming completion (yields chunks) */
    suspend fun stream(request: AiRequest): Flow<AiStreamChunk>
    
    /** Get current model info */
    suspend fun getModelInfo(): ModelInfo
    
    /** Estimate cost before making request (cloud only) */
    suspend fun estimateCost(request: AiRequest): Float? = null
}

data class ModelInfo(
    val modelId: String,
    val displayName: String,
    val provider: String,
    val contextLength: Int,
    val capabilities: Set<AiCapability>,
    val sizeBytes: Long? = null,  // For downloaded models
    val version: String? = null
)
```

### On-Device LLM Provider

```
┌─────────────────────────────────────────────────────────────────┐
│                      ON-DEVICE AI ENGINE                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    LLM INFERENCE ENGINE                    │   │
│  │                                                            │   │
│  │  ┌────────────────┐  ┌────────────────┐  ┌─────────────┐  │   │
│  │  │  Model Loader  │  │   Tokenizer    │  │  Inference  │  │   │
│  │  │  (GGUF format) │  │                │  │   Runtime   │  │   │
│  │  └────────────────┘  └────────────────┘  └─────────────┘  │   │
│  │                                                            │   │
│  │  Runtime Options:                                          │   │
│  │  • llama.cpp (via JNI) - Primary                          │   │
│  │  • MediaPipe LLM Inference - Alternative                  │   │
│  │  • ONNX Runtime - Fallback                                │   │
│  │                                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│  ┌──────────────────────────┴───────────────────────────────┐   │
│  │                    MODEL REGISTRY                          │   │
│  │                                                            │   │
│  │  • Lists available models (downloaded + downloadable)     │   │
│  │  • Tracks model versions and checksums                    │   │
│  │  • Handles model lifecycle (download, verify, delete)     │   │
│  │  • Supports runtime model switching                       │   │
│  │                                                            │   │
│  │  Supported Models:                                         │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │   │
│  │  │   Phi-3-mini │  │   Gemma-2B   │  │    Qwen2.5-3B    │ │   │
│  │  │   Q4_K_M     │  │   Q4_K_M     │  │    Q4_K_M        │ │   │
│  │  │   2.4 GB     │  │   1.7 GB     │  │    2.0 GB        │ │   │
│  │  │   87% acc    │  │   82% acc    │  │    85% acc       │ │   │
│  │  └──────────────┘  └──────────────┘  └──────────────────┘ │   │
│  │                                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│  ┌──────────────────────────┴───────────────────────────────┐   │
│  │                    PROMPT TEMPLATES                        │   │
│  │                                                            │   │
│  │  • Eisenhower classification prompts (model-optimized)    │   │
│  │  • Task parsing prompts with JSON output                  │   │
│  │  • Briefing generation templates                          │   │
│  │  • Per-model prompt variants for best results             │   │
│  │                                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### On-Device Provider Implementation

```kotlin
class OnDeviceAiProvider(
    private val context: Context,
    private val modelRegistry: ModelRegistry,
    private val promptTemplates: PromptTemplateRepository
) : AiProvider {
    
    override val providerId = "on-device"
    override val displayName = "On-Device AI"
    override val capabilities = setOf(
        AiCapability.CLASSIFICATION,
        AiCapability.EXTRACTION,
        AiCapability.GENERATION
    )
    
    private val _isAvailable = MutableStateFlow(false)
    override val isAvailable: StateFlow<Boolean> = _isAvailable
    
    private var currentEngine: LlamaCppEngine? = null
    private val engineLock = Mutex()
    
    override suspend fun complete(request: AiRequest): Result<AiResponse> = 
        withContext(Dispatchers.Default) {
            runCatching {
                val startTime = System.currentTimeMillis()
                
                val engine = ensureEngineLoaded()
                val prompt = promptTemplates.buildPrompt(request, getCurrentModelId())
                
                val result = engine.generate(
                    prompt = prompt,
                    maxTokens = request.parameters.maxTokens,
                    temperature = request.parameters.temperature,
                    topP = request.parameters.topP
                )
                
                AiResponse(
                    requestId = request.id,
                    text = result.text,
                    providerId = providerId,
                    modelId = getCurrentModelId(),
                    tokenUsage = TokenUsage(
                        promptTokens = result.promptTokens,
                        completionTokens = result.completionTokens,
                        totalTokens = result.totalTokens
                    ),
                    latencyMs = System.currentTimeMillis() - startTime
                )
            }
        }
    
    override suspend fun stream(request: AiRequest): Flow<AiStreamChunk> = flow {
        // Streaming implementation for real-time UI updates
        val engine = ensureEngineLoaded()
        val prompt = promptTemplates.buildPrompt(request, getCurrentModelId())
        
        engine.generateStreaming(prompt, request.parameters).collect { chunk ->
            emit(AiStreamChunk(text = chunk.text, isComplete = chunk.isComplete))
        }
    }
    
    /** Switch to a different model at runtime */
    suspend fun switchModel(modelId: String) {
        engineLock.withLock {
            currentEngine?.unload()
            currentEngine = null
            modelRegistry.setActiveModel(modelId)
            ensureEngineLoaded()
        }
    }
    
    private suspend fun ensureEngineLoaded(): LlamaCppEngine {
        return engineLock.withLock {
            currentEngine ?: run {
                val modelPath = modelRegistry.getActiveModelPath()
                LlamaCppEngine(context, modelPath).also {
                    it.load()
                    currentEngine = it
                    _isAvailable.value = true
                }
            }
        }
    }
}
```

### Model Registry (Runtime Model Switching)

```kotlin
/**
 * Manages available models and supports runtime switching.
 * Key for easy model replacement without code changes.
 */
class ModelRegistry(
    private val context: Context,
    private val preferences: DataStore<Preferences>,
    private val downloadManager: ModelDownloadManager
) {
    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels
    
    private val _activeModelId = MutableStateFlow<String?>(null)
    val activeModelId: StateFlow<String?> = _activeModelId
    
    /** All known models (downloaded + available for download) */
    private val modelCatalog = listOf(
        ModelDefinition(
            id = "phi-3-mini-4k-q4km",
            displayName = "Phi-3 Mini (Recommended)",
            fileName = "phi-3-mini-4k-instruct-q4_k_m.gguf",
            sizeBytes = 2_400_000_000L,
            downloadUrl = "https://cdn.jeeves.app/models/phi-3-mini-4k-q4km.gguf",
            sha256 = "abc123...",
            capabilities = setOf(AiCapability.CLASSIFICATION, AiCapability.EXTRACTION),
            contextLength = 4096,
            recommendedForTasks = setOf(AiTaskType.CLASSIFY_EISENHOWER, AiTaskType.PARSE_TASK)
        ),
        ModelDefinition(
            id = "gemma-2-2b-q4km",
            displayName = "Gemma 2 2B (Smaller)",
            fileName = "gemma-2-2b-it-q4_k_m.gguf",
            sizeBytes = 1_700_000_000L,
            downloadUrl = "https://cdn.jeeves.app/models/gemma-2-2b-q4km.gguf",
            sha256 = "def456...",
            capabilities = setOf(AiCapability.CLASSIFICATION, AiCapability.EXTRACTION),
            contextLength = 8192,
            recommendedForTasks = setOf(AiTaskType.CLASSIFY_EISENHOWER)
        ),
        ModelDefinition(
            id = "qwen2.5-3b-q4km",
            displayName = "Qwen 2.5 3B (Multilingual)",
            fileName = "qwen2.5-3b-instruct-q4_k_m.gguf",
            sizeBytes = 2_000_000_000L,
            downloadUrl = "https://cdn.jeeves.app/models/qwen2.5-3b-q4km.gguf",
            sha256 = "ghi789...",
            capabilities = setOf(AiCapability.CLASSIFICATION, AiCapability.EXTRACTION, AiCapability.GENERATION),
            contextLength = 32768,
            recommendedForTasks = setOf(AiTaskType.GENERATE_BRIEFING, AiTaskType.CHAT)
        )
    )
    
    suspend fun downloadModel(modelId: String, onProgress: (Float) -> Unit): Result<Unit>
    suspend fun deleteModel(modelId: String): Result<Unit>
    suspend fun setActiveModel(modelId: String)
    suspend fun getActiveModelPath(): String
    fun isModelDownloaded(modelId: String): Boolean
}
```

### Cloud Gateway Provider (Backend Proxy)

For users who want cloud AI while maintaining privacy, requests go through our backend proxy:

```kotlin
/**
 * Cloud AI access via Jeeves backend.
 * Benefits:
 * - API keys stay on server (user never sees them)
 * - Usage tracking and cost control
 * - Rate limiting per user
 * - Unified billing
 */
class CloudGatewayProvider(
    private val apiClient: JeevesApiClient,
    private val authManager: AuthManager,
    private val preferences: UserPreferences
) : AiProvider {
    
    override val providerId = "cloud-gateway"
    override val displayName = "Cloud AI (via Jeeves)"
    override val capabilities = setOf(
        AiCapability.CLASSIFICATION,
        AiCapability.EXTRACTION,
        AiCapability.GENERATION,
        AiCapability.STREAMING,
        AiCapability.LONG_CONTEXT,
        AiCapability.FUNCTION_CALLING
    )
    
    private val _isAvailable = MutableStateFlow(false)
    override val isAvailable: StateFlow<Boolean> = _isAvailable
    
    init {
        // Check availability based on auth + network + quota
        viewModelScope.launch {
            combine(
                authManager.isAuthenticated,
                networkMonitor.isConnected,
                quotaManager.hasQuotaRemaining
            ) { auth, network, quota ->
                auth && network && quota
            }.collect { available ->
                _isAvailable.value = available
            }
        }
    }
    
    override suspend fun complete(request: AiRequest): Result<AiResponse> = 
        runCatching {
            val cloudRequest = CloudAiRequest(
                request = request,
                preferredModel = preferences.cloudModel,
                userId = authManager.currentUserId
            )
            
            apiClient.post("/api/v1/ai/complete", cloudRequest)
        }
    
    override suspend fun stream(request: AiRequest): Flow<AiStreamChunk> = flow {
        val cloudRequest = CloudAiRequest(
            request = request,
            preferredModel = preferences.cloudModel,
            userId = authManager.currentUserId
        )
        
        apiClient.streamPost("/api/v1/ai/stream", cloudRequest).collect { chunk ->
            emit(chunk)
        }
    }
    
    override suspend fun estimateCost(request: AiRequest): Float {
        // Estimate tokens and calculate cost
        val estimatedTokens = request.prompt.length / 4 + request.parameters.maxTokens
        return pricingTable.getPrice(preferences.cloudModel, estimatedTokens)
    }
}
```

### Rule-Based Fallback Provider

```kotlin
/**
 * Zero-dependency fallback when AI is unavailable.
 * Uses regex patterns and keyword matching.
 */
class RuleBasedFallbackProvider : AiProvider {
    
    override val providerId = "rule-based"
    override val displayName = "Basic (Offline)"
    override val capabilities = setOf(AiCapability.CLASSIFICATION, AiCapability.EXTRACTION)
    override val isAvailable = MutableStateFlow(true)  // Always available
    
    override suspend fun complete(request: AiRequest): Result<AiResponse> = 
        runCatching {
            val startTime = System.currentTimeMillis()
            
            val result = when (request.taskType) {
                AiTaskType.CLASSIFY_EISENHOWER -> classifyEisenhower(request.prompt)
                AiTaskType.PARSE_TASK -> parseTask(request.prompt)
                else -> throw UnsupportedOperationException("Task type not supported in fallback mode")
            }
            
            AiResponse(
                requestId = request.id,
                text = result,
                providerId = providerId,
                modelId = "rule-based-v1",
                latencyMs = System.currentTimeMillis() - startTime
            )
        }
    
    private fun classifyEisenhower(text: String): String {
        // Urgency detection patterns
        val urgentPatterns = listOf(
            Regex("(?i)\\b(urgent|asap|immediately|deadline today|due today)\\b"),
            Regex("(?i)\\b(overdue|critical|crisis|emergency)\\b")
        )
        
        // Importance detection patterns
        val importantPatterns = listOf(
            Regex("(?i)\\b(important|crucial|vital|essential|strategic)\\b"),
            Regex("(?i)\\b(goal|career|health|project|client|boss)\\b")
        )
        
        val isUrgent = urgentPatterns.any { it.containsMatchIn(text) }
        val isImportant = importantPatterns.any { it.containsMatchIn(text) }
        
        val quadrant = when {
            isUrgent && isImportant -> "DO"
            !isUrgent && isImportant -> "SCHEDULE"
            isUrgent && !isImportant -> "DELEGATE"
            else -> "ELIMINATE"
        }
        
        return """{"quadrant": "$quadrant", "confidence": 0.72}"""
    }
}
```

### AI Provider Router (Smart Routing)

```kotlin
/**
 * Routes AI requests to the optimal provider based on:
 * - User preferences (privacy mode, cost limits)
 * - Provider availability
 * - Task complexity
 * - Network status
 */
class AiProviderRouter(
    private val onDeviceProvider: OnDeviceAiProvider,
    private val cloudGatewayProvider: CloudGatewayProvider,
    private val ruleBasedProvider: RuleBasedFallbackProvider,
    private val preferences: DataStore<UserPreferences>,
    private val networkMonitor: NetworkMonitor
) {
    /**
     * Routing priority (configurable):
     * 1. On-Device (if available and preferred)
     * 2. Cloud Gateway (if allowed and available)
     * 3. Rule-Based Fallback (always available)
     */
    suspend fun route(request: AiRequest): Result<AiResponse> {
        val prefs = preferences.data.first()
        val providers = getProviderChain(request, prefs)
        
        for (provider in providers) {
            if (provider.isAvailable.value) {
                val result = provider.complete(request)
                if (result.isSuccess) return result
                
                // Log failure and try next provider
                Timber.w("Provider ${provider.providerId} failed, trying next")
            }
        }
        
        return Result.failure(AiUnavailableException("All AI providers unavailable"))
    }
    
    private fun getProviderChain(
        request: AiRequest,
        prefs: UserPreferences
    ): List<AiProvider> {
        return buildList {
            // Privacy mode: never use cloud
            if (prefs.privacyMode) {
                add(onDeviceProvider)
                add(ruleBasedProvider)
                return@buildList
            }
            
            // User preference for provider order
            when (prefs.preferredAiSource) {
                AiSource.ON_DEVICE_FIRST -> {
                    add(onDeviceProvider)
                    if (request.metadata.allowCloudFallback) add(cloudGatewayProvider)
                    add(ruleBasedProvider)
                }
                AiSource.CLOUD_FIRST -> {
                    add(cloudGatewayProvider)
                    add(onDeviceProvider)
                    add(ruleBasedProvider)
                }
                AiSource.AUTO -> {
                    // Smart routing based on task type
                    if (request.taskType.isComplexTask()) {
                        add(cloudGatewayProvider)
                        add(onDeviceProvider)
                    } else {
                        add(onDeviceProvider)
                        add(cloudGatewayProvider)
                    }
                    add(ruleBasedProvider)
                }
            }
        }
    }
}

enum class AiSource { ON_DEVICE_FIRST, CLOUD_FIRST, AUTO }
```

---

## Model Replacement Guide

### Adding a New On-Device Model

The architecture supports adding new LLM models without code changes:

1. **Add Model to Registry Catalog**:
```kotlin
// In ModelRegistry, add to modelCatalog list:
ModelDefinition(
    id = "new-model-q4km",
    displayName = "New Model Name",
    fileName = "new-model-q4_k_m.gguf",
    sizeBytes = 2_000_000_000L,
    downloadUrl = "https://cdn.jeeves.app/models/new-model.gguf",
    sha256 = "...",
    capabilities = setOf(AiCapability.CLASSIFICATION, AiCapability.EXTRACTION),
    contextLength = 4096,
    recommendedForTasks = setOf(AiTaskType.CLASSIFY_EISENHOWER)
)
```

2. **Add Model-Specific Prompts** (if needed):
```kotlin
// In PromptTemplateRepository, add prompt variant:
"new-model-q4km" to EisenhowerPrompt(
    systemPrompt = "You are a task classifier...",
    outputFormat = OutputFormat.JSON
)
```

3. **Test and Validate**:
   - Run benchmark suite with new model
   - Verify Eisenhower accuracy >80%
   - Check inference time <3s on mid-range devices

4. **Deploy**:
   - Upload model to CDN
   - Update app config (no code deployment needed for model catalog)

### Switching to Cloud Backend

To enable cloud AI (post-MVP), the architecture is designed for minimal changes:

1. **Backend Implements API Contract**:
   - `POST /api/v1/ai/complete` - matches `AiRequest`/`AiResponse`
   - `POST /api/v1/ai/stream` - SSE streaming
   - See [Phase 7 in POST_MVP_ROADMAP.md](POST_MVP_ROADMAP.md)

2. **Enable CloudGatewayProvider**:
```kotlin
// CloudGatewayProvider is already stubbed, implement HTTP calls:
class CloudGatewayProvider(...) : AiProvider {
    override suspend fun complete(request: AiRequest): Result<AiResponse> {
        return apiClient.post("/api/v1/ai/complete", request)
    }
}
```

3. **Configure User Preferences**:
```kotlin
// User can choose in Settings:
data class UserPreferences(
    val preferredAiSource: AiSource = AiSource.ON_DEVICE_FIRST,
    val cloudModel: String = "gpt-4o-mini",
    val allowCloudFallback: Boolean = true,
    val monthlyCloudBudgetUsd: Float = 5.00f
)
```

4. **AiProviderRouter Handles Automatically**:
   - No changes needed to feature code (Tasks, Goals, Briefings)
   - Router selects provider based on preferences and availability

### Hybrid Mode Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           HYBRID AI FLOW                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   User creates task: "Call mom about birthday next Sunday"                   │
│                              │                                               │
│                              ▼                                               │
│   ┌────────────────────────────────────────────────────────────────────┐    │
│   │                     AiProviderRouter                                │    │
│   │                                                                      │    │
│   │  1. Check user preference (AiSource.AUTO)                           │    │
│   │  2. Task type = PARSE_TASK (simple, on-device preferred)            │    │
│   │  3. Check OnDeviceProvider.isAvailable → true                       │    │
│   │                                                                      │    │
│   └────────────────────────────────────────────────────────────────────┘    │
│                              │                                               │
│                              ▼                                               │
│   ┌────────────────────────────────────────────────────────────────────┐    │
│   │                    OnDeviceAiProvider                               │    │
│   │                                                                      │    │
│   │  • Load Phi-3-mini Q4_K_M (if not loaded)                           │    │
│   │  • Build prompt from PromptTemplateRepository                       │    │
│   │  • Run inference via llama.cpp                                      │    │
│   │  • Parse JSON response                                               │    │
│   │                                                                      │    │
│   └────────────────────────────────────────────────────────────────────┘    │
│                              │                                               │
│                              ▼                                               │
│   AiResponse {                                                               │
│       text: '{"task": "Call mom", "date": "2026-02-08", ...}',              │
│       providerId: "on-device",                                               │
│       modelId: "phi-3-mini-4k-q4km",                                        │
│       latencyMs: 1200                                                        │
│   }                                                                          │
│                                                                              │
│   ════════════════════════════════════════════════════════════════════════  │
│                                                                              │
│   User requests briefing: "Generate my morning briefing"                     │
│                              │                                               │
│                              ▼                                               │
│   ┌────────────────────────────────────────────────────────────────────┐    │
│   │                     AiProviderRouter                                │    │
│   │                                                                      │    │
│   │  1. Check user preference (AiSource.AUTO)                           │    │
│   │  2. Task type = GENERATE_BRIEFING (complex, cloud preferred)        │    │
│   │  3. User has cloudAiEnabled + quota remaining                       │    │
│   │  4. Network available                                                │    │
│   │  → Route to CloudGatewayProvider                                    │    │
│   │                                                                      │    │
│   └────────────────────────────────────────────────────────────────────┘    │
│                              │                                               │
│                              ▼                                               │
│   ┌────────────────────────────────────────────────────────────────────┐    │
│   │                   CloudGatewayProvider                              │    │
│   │                                                                      │    │
│   │  • POST /api/v1/ai/complete                                         │    │
│   │  • Backend routes to GPT-4o-mini                                    │    │
│   │  • Stream response via SSE                                          │    │
│   │                                                                      │    │
│   └────────────────────────────────────────────────────────────────────┘    │
│                              │                                               │
│                              ▼                                               │
│   AiResponse {                                                               │
│       text: "Good morning! You have 3 meetings today...",                   │
│       providerId: "cloud-gateway",                                           │
│       modelId: "gpt-4o-mini",                                               │
│       tokenUsage: { prompt: 1200, completion: 350, cost: $0.002 }           │
│   }                                                                          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### Model Selection Criteria

| Model | Size | RAM Required | Speed (token/s) | Quality | Recommendation |
|-------|------|--------------|-----------------|---------|----------------|
| Phi-3-mini (Q4_K_M) | 2.4GB | 3.5GB | 12-28 | 87% acc | ✅ Primary choice |
| Gemma 2 2B (Q4_K_M) | 1.7GB | 2.5GB | 16-35 | 82% acc | ✅ Low-memory alternative |
| Qwen2.5-3B (Q4_K_M) | 2.0GB | 3.0GB | 10-25 | 85% acc | ✅ Multilingual/long context |
| TinyLlama (Q4_K_M) | 0.7GB | 1.2GB | 32-65 | 68% acc | Fallback for ultra-low-end |

### Model Download Strategy

```kotlin
sealed class ModelState {
    object NotDownloaded : ModelState()
    data class Downloading(val progress: Float, val bytesDownloaded: Long) : ModelState()
    data class Verifying(val progress: Float) : ModelState()
    object Ready : ModelState()
    data class Error(val message: String, val retryable: Boolean) : ModelState()
}

class ModelDownloadManager(
    private val context: Context,
    private val httpClient: OkHttpClient
) {
    /**
     * Download with:
     * - Resume support for interrupted downloads
     * - SHA-256 verification
     * - Progress reporting
     * - Background-safe (WorkManager integration)
     */
    suspend fun downloadModel(
        modelDef: ModelDefinition,
        onProgress: (ModelState) -> Unit
    ): Result<File>
    
    suspend fun verifyModel(modelFile: File, expectedSha256: String): Boolean
    suspend fun deleteModel(modelId: String): Result<Unit>
}
```

### Fallback Strategy

For devices that cannot run LLM inference:

1. **Rule-based NLP**: Regex + keyword matching for task parsing (72% accuracy)
2. **Cloud Fallback**: Optional cloud AI via backend proxy
3. **Simplified UI**: Direct input fields with dropdown selections

---

## Cloud AI Gateway Architecture (Backend Proxy)

For premium users or when on-device AI is insufficient, the backend serves as an AI proxy:

### Backend AI Gateway Design (Rust)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      JEEVES AI GATEWAY (Rust/Axum)                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        REQUEST HANDLER                                 │   │
│  │                                                                        │   │
│  │  POST /api/v1/ai/complete     - Synchronous completion                │   │
│  │  POST /api/v1/ai/stream       - Streaming completion (SSE)            │   │
│  │  GET  /api/v1/ai/models       - List available models                 │   │
│  │  GET  /api/v1/ai/usage        - Get usage stats                       │   │
│  │  POST /api/v1/ai/estimate     - Estimate request cost                 │   │
│  │                                                                        │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                    MIDDLEWARE CHAIN                                    │   │
│  │                                                                        │   │
│  │  1. Authentication (JWT validation)                                   │   │
│  │  2. Rate Limiting (per user: 60 req/min free, 300 req/min pro)       │   │
│  │  3. Quota Check (monthly token/cost limits)                          │   │
│  │  4. Request Validation & Sanitization                                │   │
│  │  5. Logging & Metrics                                                 │   │
│  │                                                                        │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                    PROVIDER ADAPTER LAYER                              │   │
│  │                                                                        │   │
│  │  trait LlmProvider {                                                  │   │
│  │      async fn complete(&self, req: AiRequest) -> Result<AiResponse>   │   │
│  │      async fn stream(&self, req: AiRequest) -> impl Stream<Item=...>  │   │
│  │      fn supports(&self, capability: Capability) -> bool               │   │
│  │  }                                                                     │   │
│  │                                                                        │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌───────────┬───────────┬────────┴───┬─────────────┬──────────────────┐   │
│  │  OpenAI   │ Anthropic │   Google   │    xAI      │   Local LLM      │   │
│  │  Adapter  │  Adapter  │   Adapter  │   Adapter   │   (vLLM/Ollama)  │   │
│  │           │           │            │             │                   │   │
│  │ • GPT-4o  │ • Claude  │ • Gemini   │ • Grok-2    │ • Self-hosted    │   │
│  │ • GPT-4o  │   3.5     │   1.5 Pro  │ • Grok-2    │   Phi-3/Gemma    │   │
│  │   mini    │   Sonnet  │ • Gemini   │   mini      │ • Cost-free      │   │
│  │           │ • Claude  │   1.5 Flash│             │   after infra    │   │
│  │           │   3 Haiku │            │             │                   │   │
│  └───────────┴───────────┴────────────┴─────────────┴──────────────────┘   │
│                                    │                                         │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                    MODEL ROUTER                                        │   │
│  │                                                                        │   │
│  │  Routing Logic:                                                        │   │
│  │  • User preference → Use specified model                              │   │
│  │  • Task-based routing → Simple tasks to cheaper models                │   │
│  │  • Failover → Auto-switch on provider errors                          │   │
│  │  • Cost optimization → Route to self-hosted when possible             │   │
│  │                                                                        │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                    USAGE & BILLING                                     │   │
│  │                                                                        │   │
│  │  • Track tokens per request (input + output)                          │   │
│  │  • Calculate cost per provider pricing                                │   │
│  │  • Enforce monthly limits (soft + hard)                               │   │
│  │  • Generate usage reports                                             │   │
│  │                                                                        │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Backend Gateway Implementation (Rust)

```rust
// AI Gateway service structure
pub struct AiGateway {
    providers: HashMap<String, Box<dyn LlmProvider>>,
    router: ModelRouter,
    usage_tracker: UsageTracker,
    rate_limiter: RateLimiter,
}

#[async_trait]
pub trait LlmProvider: Send + Sync {
    fn provider_id(&self) -> &str;
    fn supported_models(&self) -> Vec<ModelInfo>;
    
    async fn complete(&self, request: &AiRequest) -> Result<AiResponse, ProviderError>;
    
    async fn stream(
        &self, 
        request: &AiRequest
    ) -> Result<impl Stream<Item = Result<StreamChunk, ProviderError>>, ProviderError>;
}

// Example: OpenAI adapter
pub struct OpenAiAdapter {
    client: reqwest::Client,
    api_key: SecretString,
    base_url: Url,
}

#[async_trait]
impl LlmProvider for OpenAiAdapter {
    fn provider_id(&self) -> &str { "openai" }
    
    async fn complete(&self, request: &AiRequest) -> Result<AiResponse, ProviderError> {
        let openai_req = OpenAiChatRequest {
            model: request.model_id.clone(),
            messages: vec![
                ChatMessage { role: "system", content: &request.system_prompt },
                ChatMessage { role: "user", content: &request.prompt },
            ],
            max_tokens: request.parameters.max_tokens,
            temperature: request.parameters.temperature,
        };
        
        let response = self.client
            .post(self.base_url.join("/v1/chat/completions")?)
            .bearer_auth(self.api_key.expose_secret())
            .json(&openai_req)
            .send()
            .await?;
        
        // Parse and return
        let openai_resp: OpenAiChatResponse = response.json().await?;
        Ok(AiResponse::from_openai(openai_resp, request.id.clone()))
    }
}

// Request/Response types matching mobile client
#[derive(Debug, Serialize, Deserialize)]
pub struct AiRequest {
    pub id: String,
    pub prompt: String,
    #[serde(default)]
    pub system_prompt: Option<String>,
    pub task_type: AiTaskType,
    pub parameters: AiParameters,
    #[serde(default)]
    pub preferred_model: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct AiResponse {
    pub request_id: String,
    pub text: String,
    pub provider_id: String,
    pub model_id: String,
    pub token_usage: TokenUsage,
    pub latency_ms: u64,
}
```

### Supported Cloud Models

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AI GATEWAY (Rust)                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        REQUEST HANDLER                                 │   │
│  │  • JWT authentication                                                 │   │
│  │  • Rate limiting (per user, per minute)                              │   │
│  │  • Usage quota enforcement                                           │   │
│  │  • Request validation & sanitization                                 │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        MODEL ROUTER                                    │   │
│  │                                                                        │   │
│  │  • Route to user-selected model provider                              │   │
│  │  • Automatic failover if provider unavailable                         │   │
│  │  • Cost optimization (prefer cheaper models when appropriate)         │   │
│  │  • Response streaming support                                         │   │
│  │                                                                        │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌───────────┬───────────┬────────┴───┬─────────────┬──────────────────┐   │
│  │  OpenAI   │ Anthropic │   Google   │    xAI      │   Fallback       │   │
│  │  Adapter  │  Adapter  │   Adapter  │   Adapter   │   (On-device)    │   │
│  └───────────┴───────────┴────────────┴─────────────┴──────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Supported Cloud Models

| Provider | Models | Best For | Pricing Tier |
|----------|--------|----------|--------------|
| **OpenAI** | GPT-4o, GPT-4o-mini | General purpose, fast | $$ |
| **Anthropic** | Claude 3.5 Sonnet, Claude 3 Haiku | Analysis, safety | $$-$$$ |
| **Google** | Gemini 1.5 Pro, Gemini 1.5 Flash | Multimodal, long context | $$ |
| **xAI** | Grok-2, Grok-2 mini | Creative, real-time info | $$ |

### Hybrid AI Routing

```kotlin
interface AiRouter {
    suspend fun route(request: AiRequest): AiResponse
}

class HybridAiRouter(
    private val onDeviceEngine: LlmEngine,
    private val cloudGateway: CloudAiGateway,
    private val preferences: UserPreferences
) : AiRouter {
    
    override suspend fun route(request: AiRequest): AiResponse {
        // Decision logic for on-device vs cloud
        val useCloud = when {
            // Always use on-device if offline
            !networkAvailable -> false
            
            // User explicitly disabled cloud
            !preferences.cloudAiEnabled -> false
            
            // User has no quota remaining
            preferences.cloudQuotaRemaining <= 0 -> false
            
            // Complex requests -> cloud (if user allows)
            request.complexity == Complexity.HIGH -> true
            
            // Custom agent requests -> cloud
            request.agentId != null -> true
            
            // Simple requests -> on-device (faster, free)
            else -> false
        }
        
        return if (useCloud) {
            cloudGateway.invoke(request, preferences.preferredModel)
        } else {
            onDeviceEngine.invoke(request)
        }
    }
}

data class AiRequest(
    val prompt: String,
    val taskType: TaskType,
    val complexity: Complexity,
    val agentId: String? = null,  // For custom agents
    val context: Map<String, Any> = emptyMap()
)

enum class Complexity { LOW, MEDIUM, HIGH }
enum class TaskType { CATEGORIZE, SUMMARIZE, GENERATE, ANALYZE, CHAT }
```

### Cost Control

```kotlin
data class UsageQuota(
    val monthlyLimitUsd: Float = 5.00f,  // Default $5/month
    val currentUsageUsd: Float = 0f,
    val requestsThisMonth: Int = 0,
    val resetDate: LocalDate
)

class CostController(
    private val usageRepository: UsageRepository
) {
    suspend fun canMakeRequest(estimatedCostUsd: Float): Boolean {
        val quota = usageRepository.getCurrentQuota()
        return quota.currentUsageUsd + estimatedCostUsd <= quota.monthlyLimitUsd
    }
    
    suspend fun recordUsage(request: AiRequest, response: AiResponse) {
        val cost = calculateCost(request, response)
        usageRepository.addUsage(cost)
    }
    
    private fun calculateCost(request: AiRequest, response: AiResponse): Float {
        val inputTokens = request.prompt.tokenCount()
        val outputTokens = response.text.tokenCount()
        return getPricing(request.model).calculate(inputTokens, outputTokens)
    }
}
```

---

## Custom AI Agents Architecture (Post-MVP - Premium)

### Agent System Design

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CUSTOM AI AGENTS SYSTEM                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        AGENT REGISTRY                                  │   │
│  │                                                                        │   │
│  │  • Store agent configurations (local + sync)                          │   │
│  │  • Manage agent lifecycle (create, update, delete)                    │   │
│  │  • Link agents to goals                                               │   │
│  │  • Track agent usage and effectiveness                                │   │
│  │                                                                        │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        AGENT EXECUTOR                                  │   │
│  │                                                                        │   │
│  │  • Build prompts from agent configuration                             │   │
│  │  • Inject user context (goals, tasks, history)                        │   │
│  │  • Execute via AI Router (cloud or on-device)                         │   │
│  │  • Parse and validate agent responses                                 │   │
│  │                                                                        │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌──────────────┬──────────────┬──────────────┬────────────────────────┐   │
│  │ Built-in     │ User-Created │ Marketplace  │ Goal-Linked            │   │
│  │ Agents       │ Agents       │ Agents       │ Agents                 │   │
│  └──────────────┴──────────────┴──────────────┴────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Agent Data Model

```kotlin
@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val iconEmoji: String,  // e.g., "🏃", "💼", "📚"
    
    // Agent Configuration
    val systemPrompt: String,  // Core personality and instructions
    val expertise: List<String>,  // Domain tags
    val personality: AgentPersonality,
    
    // Behavior Settings
    val proactivityLevel: ProactivityLevel,  // How often agent reaches out
    val communicationStyle: CommunicationStyle,
    
    // Data Access Permissions
    val canReadTasks: Boolean = true,
    val canReadGoals: Boolean = true,
    val canReadCalendar: Boolean = true,
    val canCreateTasks: Boolean = false,
    val canCreateReminders: Boolean = false,
    
    // Linked Goal (optional)
    val linkedGoalId: String? = null,
    
    // AI Model Preference
    val preferredModel: String = "claude-3-sonnet",
    
    // Metadata
    val isBuiltIn: Boolean = false,
    val templateId: String? = null,  // If created from template
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 0
)

data class AgentPersonality(
    val formalityLevel: Float,  // 0.0 (casual) to 1.0 (formal)
    val verbosityLevel: Float,  // 0.0 (concise) to 1.0 (detailed)
    val encouragementLevel: Float,  // 0.0 (direct) to 1.0 (encouraging)
    val useEmojis: Boolean = true
)

enum class ProactivityLevel {
    PASSIVE,      // Only responds when asked
    BALANCED,     // Occasional suggestions
    PROACTIVE     // Regular check-ins and suggestions
}

enum class CommunicationStyle {
    COACH,        // Encouraging, supportive
    MENTOR,       // Educational, guiding
    ASSISTANT,    // Helpful, efficient
    ACCOUNTABILITY  // Direct, challenging
}
```

### Agent Builder Flow

```kotlin
class AgentBuilder {
    
    // Step 1: Purpose
    fun setPurpose(purpose: AgentPurpose): AgentBuilder
    
    // Step 2: Personality
    fun setPersonality(personality: AgentPersonality): AgentBuilder
    
    // Step 3: Expertise
    fun setExpertise(domains: List<String>): AgentBuilder
    fun setCustomInstructions(instructions: String): AgentBuilder
    
    // Step 4: Permissions
    fun setPermissions(permissions: AgentPermissions): AgentBuilder
    
    // Step 5: Link to Goal (optional)
    fun linkToGoal(goalId: String): AgentBuilder
    
    // Step 6: Choose AI Model
    fun setPreferredModel(modelId: String): AgentBuilder
    
    // Build
    fun build(): AgentEntity
    
    // Generate system prompt from configuration
    private fun generateSystemPrompt(): String {
        return """
            You are ${config.name}, a personal AI assistant specialized in ${config.expertise.joinToString()}.
            
            Your communication style:
            - Formality: ${describeFormality(config.personality.formalityLevel)}
            - Detail level: ${describeVerbosity(config.personality.verbosityLevel)}
            - Tone: ${describeTone(config.personality.encouragementLevel)}
            ${if (config.personality.useEmojis) "- Use emojis appropriately" else "- Do not use emojis"}
            
            Your role: ${config.communicationStyle.description}
            
            ${config.customInstructions ?: ""}
            
            Always be helpful, respectful, and focused on the user's goals.
        """.trimIndent()
    }
}
```

### Built-in Agent Templates

```kotlin
object AgentTemplates {
    
    val PROJECT_MANAGER = AgentTemplate(
        id = "project-manager",
        name = "Project Manager",
        iconEmoji = "📊",
        description = "Helps with sprint planning, deadline tracking, and blocker identification",
        expertise = listOf("project-management", "deadlines", "planning"),
        defaultPersonality = AgentPersonality(
            formalityLevel = 0.7f,
            verbosityLevel = 0.5f,
            encouragementLevel = 0.5f
        ),
        communicationStyle = CommunicationStyle.ASSISTANT,
        systemPromptTemplate = """
            You are a project management assistant. Help users:
            - Break down large projects into manageable tasks
            - Identify dependencies and blockers
            - Track progress and deadlines
            - Suggest priorities using Eisenhower matrix principles
        """
    )
    
    val CAREER_COACH = AgentTemplate(
        id = "career-coach",
        name = "Career Coach",
        iconEmoji = "💼",
        description = "Guides career development, goal setting, and skill building",
        expertise = listOf("career", "networking", "skills", "goals"),
        defaultPersonality = AgentPersonality(
            formalityLevel = 0.5f,
            verbosityLevel = 0.6f,
            encouragementLevel = 0.8f
        ),
        communicationStyle = CommunicationStyle.MENTOR,
        systemPromptTemplate = """
            You are a career development coach. Help users:
            - Set and achieve career goals
            - Identify skill gaps and learning opportunities
            - Plan networking activities
            - Prepare for interviews and reviews
        """
    )
    
    val FITNESS_COACH = AgentTemplate(
        id = "fitness-coach",
        name = "Fitness Coach",
        iconEmoji = "🏃",
        description = "Motivates workouts, tracks habits, and provides accountability",
        expertise = listOf("fitness", "health", "habits", "motivation"),
        defaultPersonality = AgentPersonality(
            formalityLevel = 0.3f,
            verbosityLevel = 0.4f,
            encouragementLevel = 0.9f
        ),
        communicationStyle = CommunicationStyle.COACH,
        systemPromptTemplate = """
            You are an enthusiastic fitness coach. Help users:
            - Stay motivated with workouts
            - Build consistent exercise habits
            - Track progress and celebrate wins
            - Overcome obstacles and excuses
        """
    )
    
    // Additional templates: LEARNING_COACH, FINANCE_ADVISOR, WRITING_COACH, HOME_MANAGER
}
```

### Agent Execution

```kotlin
class AgentExecutor(
    private val agentRepository: AgentRepository,
    private val aiRouter: AiRouter,
    private val contextBuilder: ContextBuilder
) {
    suspend fun chat(agentId: String, userMessage: String): AgentResponse {
        val agent = agentRepository.getAgent(agentId)
        
        // Build context from user data (respecting permissions)
        val context = contextBuilder.build(
            includeTasks = agent.canReadTasks,
            includeGoals = agent.canReadGoals,
            includeCalendar = agent.canReadCalendar,
            linkedGoalId = agent.linkedGoalId
        )
        
        // Build the full prompt
        val prompt = buildPrompt(agent, userMessage, context)
        
        // Route to appropriate AI (cloud for agents)
        val response = aiRouter.route(
            AiRequest(
                prompt = prompt,
                taskType = TaskType.CHAT,
                complexity = Complexity.MEDIUM,
                agentId = agentId
            )
        )
        
        // Parse response for any actions
        val actions = parseActions(response.text)
        
        return AgentResponse(
            text = response.text,
            suggestedActions = actions,
            model = response.model
        )
    }
    
    private suspend fun buildPrompt(
        agent: AgentEntity,
        userMessage: String,
        context: UserContext
    ): String {
        return """
            ${agent.systemPrompt}
            
            Current context:
            ${context.format()}
            
            User: $userMessage
        """.trimIndent()
    }
}

data class AgentResponse(
    val text: String,
    val suggestedActions: List<SuggestedAction> = emptyList(),
    val model: String
)

sealed class SuggestedAction {
    data class CreateTask(val title: String, val dueDate: LocalDate?) : SuggestedAction()
    data class CreateReminder(val text: String, val time: LocalDateTime) : SuggestedAction()
    data class UpdateGoal(val goalId: String, val progress: Float) : SuggestedAction()
}
```

---

## Plugin Architecture

### Plugin System Design

```
┌─────────────────────────────────────────────────────────────────┐
│                        PLUGIN SYSTEM                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                     PLUGIN MANAGER                          │ │
│  │                                                              │ │
│  │  • Plugin discovery and registration                        │ │
│  │  • Lifecycle management (init, start, stop, destroy)        │ │
│  │  • Dependency injection for plugins                         │ │
│  │  • Event bus for inter-plugin communication                 │ │
│  │  • Permission management                                    │ │
│  │                                                              │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                     PLUGIN INTERFACE                        │ │
│  │                                                              │ │
│  │  interface JeevesPlugin {                                   │ │
│  │    val id: String                                           │ │
│  │    val name: String                                         │ │
│  │    val version: String                                      │ │
│  │    val dependencies: List<String>                           │ │
│  │                                                              │ │
│  │    fun initialize(context: PluginContext)                   │ │
│  │    fun onStart()                                            │ │
│  │    fun onStop()                                             │ │
│  │    fun getNavigationItems(): List<NavigationItem>           │ │
│  │    fun getWidgets(): List<Widget>                           │ │
│  │  }                                                          │ │
│  │                                                              │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────────┐  │
│  │   Tasks  │  Goals   │ Calendar │  Health  │   Email      │  │
│  │  Plugin  │  Plugin  │  Plugin  │  Plugin  │   Plugin     │  │
│  │  (MVP)   │  (MVP)   │  (MVP)   │ (Future) │  (Future)    │  │
│  └──────────┴──────────┴──────────┴──────────┴──────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### MVP Plugins

#### 1. Tasks Plugin (Core)
- Eisenhower matrix view and logic
- Task CRUD operations
- Priority calculation engine
- Smart reminders and nudges

#### 2. Goals Plugin (Core)  
- Goal CRUD with milestones
- Progress tracking
- Task-to-goal linking
- Analytics and insights

#### 3. Calendar Plugin (Core)
- Device calendar integration
- Daily briefings
- Meeting notes
- Time-blocking

---

## Data Architecture

### Local Database (Room)

```kotlin
// Core entities

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String?,
    val dueDate: Long?,
    val dueTime: Long?,
    
    // Eisenhower Matrix
    val isUrgent: Boolean = false,
    val isImportant: Boolean = false,
    val quadrant: Int = 4, // 1=Do, 2=Schedule, 3=Delegate, 4=Eliminate
    
    // Priority and ordering
    val priority: Int = 0, // Calculated score
    val manualOrder: Int = 0,
    
    // Status
    val status: TaskStatus = TaskStatus.PENDING,
    val completedAt: Long? = null,
    
    // Organization
    val projectId: String? = null,
    val parentTaskId: String? = null,
    val tags: List<String> = emptyList(),
    
    // Goals linkage
    val goalId: String? = null,
    
    // Recurrence
    val recurrenceRule: String? = null, // RRULE format
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 0,
    val isDeleted: Boolean = false
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String?,
    val category: GoalCategory,
    
    // Timeline
    val startDate: Long,
    val targetDate: Long,
    
    // Progress
    val progressType: ProgressType, // PERCENTAGE, COUNT, BOOLEAN
    val currentValue: Float = 0f,
    val targetValue: Float = 100f,
    
    // Status
    val status: GoalStatus = GoalStatus.ACTIVE,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 0
)

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val calendarEventId: String, // Link to device calendar
    val title: String,
    val startTime: Long,
    val endTime: Long,
    
    // Meeting content
    val agenda: String?,
    val notes: String?,
    val actionItems: List<String> = emptyList(), // Extracted action items
    val summary: String?, // AI-generated summary
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_analytics")
data class DailyAnalyticsEntity(
    @PrimaryKey val date: Long, // Date only, no time
    val tasksCreated: Int = 0,
    val tasksCompleted: Int = 0,
    val tasksMissedDeadline: Int = 0,
    val goalProgress: Map<String, Float> = emptyMap(), // goalId -> progress
    val focusTimeMinutes: Int = 0,
    val meetingsCount: Int = 0
)

enum class TaskStatus { PENDING, IN_PROGRESS, COMPLETED, CANCELLED }
enum class GoalStatus { ACTIVE, COMPLETED, ABANDONED, PAUSED }
enum class GoalCategory { CAREER, HEALTH, PERSONAL, FINANCIAL, LEARNING, RELATIONSHIP }
enum class ProgressType { PERCENTAGE, COUNT, BOOLEAN }
```

### DataStore (Preferences)

```kotlin
data class UserPreferences(
    // Eisenhower defaults
    val defaultUrgentThresholdDays: Int = 3,
    val autoCategorizeTasks: Boolean = true,
    
    // Notifications
    val morningBriefingTime: LocalTime = LocalTime.of(7, 0),
    val eveningSummaryTime: LocalTime = LocalTime.of(21, 0),
    val nudgeFrequency: NudgeFrequency = NudgeFrequency.BALANCED,
    
    // AI settings
    val llmModelId: String = "phi-3-mini",
    val enableVoiceInput: Boolean = true,
    
    // Privacy
    val analyticsEnabled: Boolean = false,
    val syncEnabled: Boolean = false,
    
    // UI
    val theme: Theme = Theme.SYSTEM,
    val startScreen: StartScreen = StartScreen.TODAY
)
```

---

## Android App Architecture

### Module Structure

```
apps/android/
├── app/                          # Main application module
│   ├── src/main/
│   │   ├── kotlin/
│   │   │   └── com/jeeves/app/
│   │   │       ├── JeevesApplication.kt
│   │   │       ├── MainActivity.kt
│   │   │       └── navigation/
│   │   └── res/
│   └── build.gradle.kts
│
├── core/                         # Core shared modules
│   ├── common/                   # Common utilities
│   ├── ui/                       # Design system, components
│   ├── data/                     # Room DB, DataStore
│   ├── domain/                   # Domain models, use cases
│   ├── ai/                       # LLM inference (llama.cpp JNI)
│   │   ├── src/main/
│   │   │   ├── cpp/              # NDK/JNI code for llama.cpp
│   │   │   └── kotlin/
│   │   │       └── com/jeeves/ai/
│   │   │           ├── LlamaCppEngine.kt
│   │   │           └── jni/
│   │   └── build.gradle.kts      # NDK configuration
│   │
│   ├── ai-provider/              # AI Provider Abstraction Layer
│   │   └── src/main/kotlin/
│   │       └── com/jeeves/ai/provider/
│   │           ├── AiProvider.kt           # Core interface
│   │           ├── AiRequest.kt            # Request types
│   │           ├── AiResponse.kt           # Response types
│   │           ├── AiProviderRouter.kt     # Provider routing
│   │           ├── ModelRegistry.kt        # Model management
│   │           ├── PromptTemplates.kt      # Prompt repository
│   │           ├── ondevice/
│   │           │   └── OnDeviceAiProvider.kt
│   │           ├── cloud/
│   │           │   └── CloudGatewayProvider.kt
│   │           └── fallback/
│   │               └── RuleBasedFallbackProvider.kt
│   │
│   └── analytics/                # Local analytics
│
├── plugins/                      # Feature plugins
│   ├── tasks/                    # Eisenhower tasks
│   ├── goals/                    # Goals & progress
│   ├── calendar/                 # Smart calendar
│   └── [future]/                 # Post-MVP plugins
│
└── sync/                         # Optional cloud sync
    └── src/main/kotlin/
```

### AI Provider Module Dependencies

```
:app
  └── :plugins:tasks
        └── :core:ai-provider     # Uses AiProviderRouter
              ├── :core:ai        # On-device LLM (llama.cpp)
              └── :core:data      # For ModelRegistry persistence
```

### Dependency Injection (Hilt)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideLlmEngine(
        @ApplicationContext context: Context,
        modelManager: ModelManager
    ): LlmEngine = LlamaCppEngine(context, modelManager)
    
    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        llmEngine: LlmEngine
    ): TaskRepository = TaskRepositoryImpl(taskDao, llmEngine)
}
```

---

## Smart Features Implementation

### Eisenhower Priority Engine

```kotlin
class EisenhowerEngine(
    private val llmEngine: LlmEngine,
    private val preferences: UserPreferences
) {
    suspend fun classifyTask(task: TaskInput): EisenhowerClassification {
        // Step 1: Extract deadline if present
        val deadline = llmEngine.extractDeadline(task.text)
        
        // Step 2: Calculate urgency based on deadline
        val urgency = calculateUrgency(deadline, preferences.defaultUrgentThresholdDays)
        
        // Step 3: Assess importance using LLM
        val importance = llmEngine.assessImportance(
            taskText = task.text,
            userGoals = getActiveGoals(),
            context = getCurrentContext()
        )
        
        // Step 4: Determine quadrant
        val quadrant = when {
            urgency && importance -> Quadrant.DO          // Q1: Do First
            !urgency && importance -> Quadrant.SCHEDULE    // Q2: Schedule
            urgency && !importance -> Quadrant.DELEGATE    // Q3: Delegate
            else -> Quadrant.ELIMINATE                      // Q4: Don't Do
        }
        
        return EisenhowerClassification(
            quadrant = quadrant,
            urgency = urgency,
            importance = importance,
            suggestedAction = getSuggestedAction(quadrant),
            confidence = calculateConfidence()
        )
    }
    
    private fun getSuggestedAction(quadrant: Quadrant): String = when (quadrant) {
        Quadrant.DO -> "Do this immediately"
        Quadrant.SCHEDULE -> "Block time on your calendar for this"
        Quadrant.DELEGATE -> "Consider delegating or simplifying"
        Quadrant.ELIMINATE -> "Is this really necessary?"
    }
}
```

### Daily Briefing Generator

```kotlin
class BriefingGenerator(
    private val taskRepository: TaskRepository,
    private val calendarRepository: CalendarRepository,
    private val llmEngine: LlmEngine
) {
    suspend fun generateMorningBriefing(): DailyBriefing {
        val today = LocalDate.now()
        
        val tasks = taskRepository.getTasksDueOn(today)
        val overdue = taskRepository.getOverdueTasks()
        val events = calendarRepository.getEventsForDay(today)
        val goals = goalRepository.getActiveGoals()
        
        val summary = llmEngine.generateBriefing(
            prompt = """
                Generate a concise, encouraging morning briefing:
                
                Today's Schedule:
                ${events.format()}
                
                Tasks Due Today (${tasks.size}):
                ${tasks.format()}
                
                Overdue Tasks (${overdue.size}):
                ${overdue.format()}
                
                Active Goals:
                ${goals.format()}
                
                Keep it under 150 words. Be encouraging but realistic.
            """.trimIndent()
        )
        
        return DailyBriefing(
            date = today,
            summary = summary,
            topPriorityTasks = tasks.sortedByDescending { it.priority }.take(3),
            upcomingEvents = events,
            goalReminders = getGoalReminders(goals)
        )
    }
}
```

---

## Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| App cold start | < 3s | Firebase Performance |
| LLM first inference | < 5s | Local timing |
| LLM subsequent inference | < 2s | Local timing |
| Task creation (NLP) | < 3s | Local timing |
| UI frame rate | 60fps | GPU profiler |
| Memory usage (idle) | < 200MB | Android Profiler |
| Memory usage (LLM active) | < 800MB | Android Profiler |
| Battery (1hr active use) | < 5% | Battery historian |
| APK size (without model) | < 30MB | Build output |

---

## Cost Analysis

### Operational Costs (Per Month)

| Component | Cost | Notes |
|-----------|------|-------|
| Backend (Auth + Sync) | $50-200 | Minimal usage, scales with premium users |
| Model CDN | $20-50 | Model download bandwidth |
| Database | $50-100 | PostgreSQL for user accounts only |
| Monitoring | $0-50 | Free tier sufficient initially |
| **Total** | **$120-400/mo** | For first 10,000 users |

### Cost Per User

| User Type | Backend Cost/Month | Notes |
|-----------|-------------------|-------|
| Free (offline only) | $0 | No backend usage |
| Free (with sync) | ~$0.02 | Minimal sync data |
| Premium | ~$0.05 | Full sync + backup |

### Comparison with Cloud-First Approach

| Approach | Cost/1000 MAU | Notes |
|----------|---------------|-------|
| **Jeeves (on-device)** | $5-10 | Minimal backend |
| Cloud LLM approach | $500-2000 | API costs dominate |
| Hybrid approach | $100-300 | Selective cloud usage |

---

## Security Architecture

### On-Device Security

```
┌─────────────────────────────────────────────────────────────────┐
│                    MOBILE SECURITY LAYERS                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                 APPLICATION LAYER                          │   │
│  │  • ProGuard/R8 obfuscation                                │   │
│  │  • Root detection (optional warning)                       │   │
│  │  • Debugger detection (release builds)                    │   │
│  │  • SSL certificate pinning (for sync)                     │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                 DATA LAYER                                  │   │
│  │  • SQLCipher for database encryption (AES-256)            │   │
│  │  • EncryptedSharedPreferences for settings                │   │
│  │  • Android Keystore for encryption keys                   │   │
│  │  • Biometric unlock option                                │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                 NETWORK LAYER                               │   │
│  │  • TLS 1.3 only                                           │   │
│  │  • Certificate pinning                                     │   │
│  │  • E2E encryption for sync (user-controlled keys)         │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Development Roadmap

### Phase 1: MVP (Weeks 1-8)
- Core architecture setup
- Room database with all entities
- Basic UI with Jetpack Compose
- LLM integration (llama.cpp)
- Tasks plugin with Eisenhower matrix
- Goals plugin with progress tracking
- Calendar plugin (read-only integration)
- Daily briefings

### Phase 2: Polish (Weeks 9-12)  
- AI tuning and prompt optimization
- Performance optimization
- Analytics and insights views
- Notifications and widgets
- Extensive testing

### Phase 3: Launch (Weeks 13-14)
- Play Store submission
- Beta testing
- Documentation
- Marketing launch

### Phase 4: Post-Launch (Months 4+)
- iOS development
- Email plugin
- Health plugin
- Optional cloud sync
- Community plugins

---

*Document Owner: Principal Backend/Infrastructure Engineer*
*Last Updated: February 2026*
*Status: Approved for MVP Development*

---

## Data Architecture

### Primary Database (PostgreSQL)

```sql
-- Core user data
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    preferences JSONB DEFAULT '{}'
);

-- Tasks
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    title TEXT NOT NULL,
    description TEXT,
    due_date TIMESTAMPTZ,
    priority INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'pending',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    sync_version BIGINT DEFAULT 0
);

-- Conversations
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    started_at TIMESTAMPTZ DEFAULT NOW(),
    ended_at TIMESTAMPTZ,
    context JSONB DEFAULT '{}'
);

-- Messages
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES conversations(id),
    role VARCHAR(50) NOT NULL, -- user, assistant, system
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'
);
```

### Caching Strategy (Redis)

```
# Session cache
session:{user_id} -> JWT claims (TTL: 1h)

# Rate limiting
ratelimit:{user_id}:{endpoint} -> count (TTL: 1m)

# User preferences cache
preferences:{user_id} -> JSONB (TTL: 15m)

# Active conversation context
context:{user_id} -> conversation state (TTL: 30m)

# Feature flags
flags:{user_id} -> evaluated flags (TTL: 5m)
```

### Vector Database (Pinecone/pgvector)

Used for:
- Semantic search over user's notes and tasks
- RAG for personalized responses
- Similar task/event suggestions
- Knowledge base retrieval

---

## Security Architecture

### Defense in Depth

```
┌─────────────────────────────────────────────────────────────────┐
│                          WAF / DDoS Protection                   │
├─────────────────────────────────────────────────────────────────┤
│                          API Gateway                             │
│                    (Rate Limiting, Auth)                         │
├─────────────────────────────────────────────────────────────────┤
│                          Service Mesh                            │
│                    (mTLS, Traffic Control)                       │
├─────────────────────────────────────────────────────────────────┤
│                    Application Security                          │
│              (Input Validation, OWASP Controls)                  │
├─────────────────────────────────────────────────────────────────┤
│                       Data Security                              │
│              (Encryption at Rest, Field-level)                   │
└─────────────────────────────────────────────────────────────────┘
```

### Encryption

| Data Type | At Rest | In Transit | Method |
|-----------|---------|------------|--------|
| User credentials | AES-256-GCM | TLS 1.3 | Argon2id hashing |
| Personal data | AES-256-GCM | TLS 1.3 | Field-level encryption |
| Conversation history | AES-256-GCM | TLS 1.3 | User-key encryption |
| API keys | AES-256-GCM | TLS 1.3 | HSM-backed |

---

## Infrastructure

### Kubernetes Deployment

```yaml
# Example service deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-engine
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      containers:
      - name: ai-engine
        image: jeeves/ai-engine:latest
        resources:
          requests:
            cpu: "500m"
            memory: "512Mi"
          limits:
            cpu: "2000m"
            memory: "2Gi"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
```

### Observability Stack

- **Metrics**: Prometheus + Grafana
- **Logging**: Loki + Grafana
- **Tracing**: Jaeger / OpenTelemetry
- **Alerting**: Alertmanager + PagerDuty

---

## API Design

### RESTful Endpoints

```
# Authentication
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout

# User
GET    /api/v1/user/profile
PATCH  /api/v1/user/profile
GET    /api/v1/user/preferences
PUT    /api/v1/user/preferences

# Tasks
GET    /api/v1/tasks
POST   /api/v1/tasks
GET    /api/v1/tasks/{id}
PATCH  /api/v1/tasks/{id}
DELETE /api/v1/tasks/{id}

# Conversations
POST   /api/v1/conversations
GET    /api/v1/conversations/{id}
POST   /api/v1/conversations/{id}/messages
GET    /api/v1/conversations/{id}/messages

# Assistant
POST   /api/v1/assistant/query
POST   /api/v1/assistant/voice
GET    /api/v1/assistant/suggestions

# Sync
POST   /api/v1/sync/push
POST   /api/v1/sync/pull
```

### WebSocket Protocol

```json
// Client -> Server
{
  "type": "query",
  "id": "uuid",
  "payload": {
    "text": "What's on my schedule today?"
  }
}

// Server -> Client (streaming response)
{
  "type": "response",
  "id": "uuid",
  "payload": {
    "text": "You have 3 meetings...",
    "complete": false
  }
}
```

---

## Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| API p50 latency | < 100ms | Prometheus |
| API p99 latency | < 500ms | Prometheus |
| AI query latency | < 2s | Prometheus |
| App cold start | < 2s | Firebase/Crashlytics |
| Database query | < 50ms | pg_stat_statements |
| Availability | 99.9% | Uptime monitoring |

---

*Document Owner: Principal Backend/Infrastructure Engineer*
*Last Updated: August 2025*
*Status: Living Document*

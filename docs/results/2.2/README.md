# Milestone 2.2 - AI Provider Abstraction Layer (Tasks 2.2.1-2.2.14)

**Status**: ✅ Completed (13/14 tasks)  
**Owner**: Android Developer  
**Last Updated**: February 4, 2026

## Overview

This document details the implementation of the AI Provider Abstraction Layer, which establishes the core interfaces, types, model registry, download management, and provider implementations for Prio's pluggable AI architecture.

## Progress Summary

| Task Range | Description | Status |
|------------|-------------|--------|
| 2.2.1-2.2.4 | Foundation (interfaces, types, registry) | ✅ Completed |
| 2.2.5-2.2.8 | Provider implementations | ✅ Completed |
| 2.2.9-2.2.14 | Prompts, testing, cloud stub | ✅ Completed |

### Detailed Documentation

- **[2.2.5-2.2.8 AI Provider Implementation](2.2.5_2.2.8_ai_provider_implementation.md)** - LlamaEngine, OnDeviceAiProvider, RuleBasedFallbackProvider, AiProviderRouter
- **[2.2.9-2.2.14 Prompts, Performance & Cloud](2.2.9_2.2.14_prompts_performance_cloud.md)** - PromptTemplateRepository, optimized prompts, benchmark framework, CloudGatewayProvider

---

## Foundation Tasks (2.2.1-2.2.4)

### 2.2.1: Design AiProvider Interface and Core Types ✅

**Duration**: 2 hours  
**Location**: `android/core/ai/src/main/java/com/prio/core/ai/provider/AiProvider.kt`

#### Implementation Details

Enhanced the `AiProvider` interface to match ARCHITECTURE.md specifications:

```kotlin
interface AiProvider {
    val providerId: String
    val displayName: String
    val capabilities: Set<AiCapability>
    val isAvailable: StateFlow<Boolean>
    
    suspend fun complete(request: AiRequest): Result<AiResponse>
    suspend fun stream(request: AiRequest): Flow<AiStreamChunk>
    suspend fun getModelInfo(): ModelInfo
    suspend fun estimateCost(request: AiRequest): Float? = null
    suspend fun initialize(): Boolean
    suspend fun release()
}
```

#### Key Components Created

| Type | Description |
|------|-------------|
| `AiProvider` | Core interface for all AI providers (on-device, cloud, rule-based) |
| `AiCapability` | Enum: CLASSIFICATION, EXTRACTION, GENERATION, STREAMING, LONG_CONTEXT, FUNCTION_CALLING |
| `AiCapabilities` | Detailed capability configuration data class |
| `ModelInfo` | Runtime model information |
| `ModelDefinition` | Full model specification including download info |
| `PromptTemplate` | Prompt format templates: PHI3, CHATML, MISTRAL, LLAMA2, LLAMA3, GEMMA, RAW |
| `AiTaskType` | Task types: CLASSIFY_EISENHOWER, PARSE_TASK, etc. |
| `PredefinedModels` | Catalog of available models with specifications |

#### Predefined Models

| Model | Size | RAM | Context | Template | Status |
|-------|------|-----|---------|----------|--------|
| Phi-3 Mini 4K Q4 | 2.3 GB | 4 GB | 4096 | PHI3 | Recommended |
| Mistral 7B Q4 | 4.1 GB | 6 GB | 8192 | MISTRAL | High accuracy |
| Gemma 2 2B Q4 | 1.7 GB | 3 GB | 8192 | GEMMA | Smaller option |
| Rule-Based | 0 | 0 | ∞ | RAW | Always available |

---

### 2.2.2: Implement AiRequest/AiResponse Serializable Types ✅

**Duration**: 1.5 hours  
**Location**: `android/core/ai/src/main/java/com/prio/core/ai/model/AiTypes.kt`

#### Implementation Details

Created fully serializable types matching the backend API contract with snake_case JSON keys:

```kotlin
@Serializable
data class AiRequest(
    @SerialName("id") val id: String,
    @SerialName("type") val type: AiRequestType,
    @SerialName("input") val input: String,
    @SerialName("system_prompt") val systemPrompt: String?,
    @SerialName("context") val context: AiContext?,
    @SerialName("options") val options: AiRequestOptions,
    @SerialName("metadata") val metadata: RequestMetadata
)

@Serializable
data class AiResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("request_id") val requestId: String,
    @SerialName("result") val result: AiResult?,
    @SerialName("error") val error: String?,
    @SerialName("error_code") val errorCode: String?,
    @SerialName("metadata") val metadata: AiResponseMetadata
)
```

#### AiResult Sealed Interface

| Result Type | Purpose | Fields |
|-------------|---------|--------|
| `EisenhowerClassification` | Task prioritization | quadrant, confidence, explanation, urgency/importance signals |
| `ParsedTask` | NLP task extraction | title, dueDate, dueTime, priority, tags, recurrence |
| `SmartGoalSuggestion` | SMART goal refinement | specific, measurable, achievable, relevant, timeBound |
| `BriefingContent` | Daily briefing | greeting, summary, topPriorities, insights |
| `ExtractedActionItems` | Meeting action items | list of ActionItemResult |
| `SummaryResult` | Text summarization | summary, keyPoints, wordCount |
| `ChatResponse` | General conversation | message |

#### Supporting Types

- `AiContext` - Contextual information for improved accuracy
- `AiRequestOptions` - Generation parameters (temperature, maxTokens, etc.)
- `RequestMetadata` - Client tracking and routing preferences
- `AiResponseMetadata` - Analytics and debugging info
- `TokenUsage` - Cost tracking
- `AiStreamChunk` - Streaming response chunks
- `AiStreamMetadata` - Streaming performance metrics

---

### 2.2.3: Create ModelRegistry for Runtime Model Management ✅

**Duration**: 3 hours  
**Location**: `android/core/ai/src/main/java/com/prio/core/ai/registry/ModelRegistry.kt`

#### Implementation Details

```kotlin
@Singleton
class ModelRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: DataStore<Preferences>
) {
    val activeModelId: StateFlow<String?>
    val downloadedModelIds: StateFlow<Set<String>>
    val availableModels: StateFlow<List<ModelInfo>>
    
    suspend fun initialize()
    suspend fun setActiveModel(modelId: String): Result<Unit>
    suspend fun deleteModel(modelId: String): Result<Unit>
    fun getModelCatalog(): List<ModelDefinition>
    fun isModelDownloaded(modelId: String): Boolean
    fun getModelPath(modelId: String): String?
    fun getModelsForDeviceTier(ramGb: Int): List<ModelDefinition>
    fun getRecommendedModel(ramGb: Int): ModelDefinition
}
```

#### Key Features

1. **Model Catalog Management**
   - Lists all available models (downloaded + available for download)
   - Tracks model versions and specifications
   - Persists active model selection via DataStore

2. **Runtime Model Switching**
   - Switch models without app restart
   - Validates model availability before switching
   - Prevents deletion of active model

3. **Device Tier Support** (from 0.2.4 findings)
   - Tier 1-2 (6GB+ RAM): Full LLM support
   - Tier 3-4 (4GB RAM): Limited to smaller models
   - Auto-recommends best model for device

4. **Storage Management**
   - Scans models directory on initialization
   - Reports storage usage
   - Validates downloaded files exist

---

### 2.2.4: Implement ModelDownloadManager with Resume Support ✅

**Duration**: 3 hours  
**Location**: `android/core/ai/src/main/java/com/prio/core/ai/registry/ModelDownloadManager.kt`

#### Implementation Details

```kotlin
@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelRegistry: ModelRegistry
) {
    val downloadState: StateFlow<DownloadState>
    val progress: StateFlow<Float>
    
    suspend fun downloadModel(modelId: String, onProgress: ((Float) -> Unit)?): Result<Unit>
    fun cancelDownload()
    fun canResumeDownload(modelId: String): Boolean
    fun getPartialDownloadProgress(modelId: String): Float
}
```

#### Key Features

1. **Download Progress Tracking**
   ```kotlin
   sealed class DownloadState {
       object Idle
       data class Downloading(modelId, progress, downloadedBytes, totalBytes)
       data class Verifying(modelId)
       data class Completed(modelId)
       data class Failed(modelId, error)
       data class Cancelled(modelId)
   }
   ```

2. **Resume Support**
   - Uses HTTP Range headers for resume after failure
   - Temporary `.downloading` file during transfer
   - Atomic rename on completion

3. **SHA-256 Verification**
   - Validates downloaded file integrity
   - Deletes corrupted downloads
   - Logs verification status

4. **Storage Checks**
   - Verifies sufficient free space (10% buffer)
   - Reports estimated download time
   - Provides storage usage information

5. **Cancellation**
   - Clean cancellation with state tracking
   - Partial download preserved for resume

---

## Test Coverage

### Unit Tests Created

| Test File | Tests | Coverage |
|-----------|-------|----------|
| `AiTypesTest.kt` | 20 tests | Request/Response serialization, all result types |
| `AiProviderTest.kt` | 15 tests | Capabilities, ModelInfo, ModelDefinition, PromptTemplate |
| `ModelRegistryTest.kt` | 18 tests | PredefinedModels, device tiers, storage calculations |

### Test Results

```
BUILD SUCCESSFUL in 10s
48 actionable tasks: 19 executed, 29 up-to-date
```

All 53 tests passing.

---

## Architecture Alignment

### ARCHITECTURE.md Compliance ✅

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| AiProvider interface with streaming | ✅ | `stream()` returns `Flow<AiStreamChunk>` |
| Provider availability via StateFlow | ✅ | `isAvailable: StateFlow<Boolean>` |
| Capabilities as Set | ✅ | `capabilities: Set<AiCapability>` |
| Result type for complete() | ✅ | Returns `Result<AiResponse>` |
| Model switching support | ✅ | ModelRegistry.setActiveModel() |
| Download with verification | ✅ | SHA-256 in ModelDownloadManager |

### Performance Budgets (from 0.2.2) ✅

| Metric | Target | Implementation |
|--------|--------|----------------|
| Rule-based latency | <50ms | Tracked in AiResponseMetadata.latencyMs |
| LLM latency (Phi-3) | 2-3s | Tracked in AiResponseMetadata.latencyMs |
| Model loading | 1.5s Phi-3 | ModelRegistry handles lifecycle |
| RAM budget | <4GB | minRamGb field in ModelDefinition |

---

## Files Created/Modified

### New Files

| File | Lines | Purpose |
|------|-------|---------|
| `registry/ModelRegistry.kt` | 310 | Runtime model management |
| `registry/ModelDownloadManager.kt` | 340 | Download with resume support |
| `provider/AiProviderTest.kt` | 185 | Provider interface tests |
| `registry/ModelRegistryTest.kt` | 175 | Model registry tests |

### Modified Files

| File | Changes |
|------|---------|
| `provider/AiProvider.kt` | Enhanced interface, added types, PredefinedModels |
| `model/AiTypes.kt` | Added SerialName annotations, streaming types, new results |
| `model/AiTypesTest.kt` | Comprehensive serialization tests |
| `build.gradle.kts` | Added DataStore dependency |

---

## Dependencies Added

```kotlin
// In core/ai/build.gradle.kts
implementation(libs.datastore.preferences)
```

---

## Next Steps (Remaining 2.2 Tasks)

| Task | Description | Owner |
|------|-------------|-------|
| 2.2.5 | Integrate llama.cpp via JNI/NDK | Android Developer |
| 2.2.6 | Implement OnDeviceAiProvider | Android Developer |
| 2.2.7 | Implement RuleBasedFallbackProvider | Android Developer |
| 2.2.8 | Implement AiProviderRouter | Android Developer |
| 2.2.9-2.2.14 | Prompts, performance testing, cloud stub | Android Dev + Backend |

---

## Technical Notes

### Prompt Template Usage

Each model family requires a specific prompt format:

```kotlin
// Phi-3
"<|user|>\n$prompt<|end|>\n<|assistant|>"

// Mistral
"[INST] $prompt [/INST]"

// Gemma
"<start_of_turn>user\n$prompt<end_of_turn>\n<start_of_turn>model"
```

The `PromptTemplate` enum ensures correct formatting per model.

### Device Tier Recommendations

```kotlin
fun getRecommendedModel(ramGb: Int): ModelDefinition {
    return when {
        ramGb >= 6 -> PredefinedModels.PHI3_MINI_4K  // Or Mistral 7B
        ramGb >= 4 -> PredefinedModels.PHI3_MINI_4K
        ramGb >= 3 -> PredefinedModels.GEMMA_2B
        else -> PredefinedModels.RULE_BASED
    }
}
```

### Download Resume Mechanism

```
1. Start download → create .downloading temp file
2. Connection lost → temp file preserved
3. Resume → HTTP Range: bytes=existingSize-
4. Complete → SHA-256 verify
5. Success → atomic rename to final file
```

---

## Provider Implementation Tasks (2.2.5-2.2.8)

> For detailed implementation documentation, see [2.2.5-2.2.8 AI Provider Implementation](2.2.5_2.2.8_ai_provider_implementation.md)

### Summary

| Task | Description | Status | Key Deliverable |
|------|-------------|--------|-----------------|
| 2.2.5 | Integrate llama.cpp via JNI/NDK | ✅ Completed | `LlamaEngine.kt` - JNI bridge with lifecycle management |
| 2.2.6 | Implement OnDeviceAiProvider | ✅ Completed | `OnDeviceAiProvider.kt` - Full AiProvider for LLM |
| 2.2.7 | Implement RuleBasedFallbackProvider | ✅ Completed | `RuleBasedFallbackProvider.kt` - 75% accuracy, <50ms |
| 2.2.8 | Implement AiProviderRouter | ✅ Completed | `AiProviderRouter.kt` - Smart routing with fallback |

### Architecture Diagram

```
                        ┌─────────────────────┐
                        │  AiProviderRouter   │
                        │   (Main Entry)      │
                        └─────────┬───────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              ▼                   ▼                   ▼
    ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
    │  Rule-Based     │ │  On-Device LLM  │ │  Cloud Gateway  │
    │  (Primary)      │ │  (Edge Cases)   │ │  (Future)       │
    │                 │ │                 │ │                 │
    │  • 75% accuracy │ │  • LlamaEngine  │ │  • API stub     │
    │  • <50ms        │ │  • Phi-3/Mistral│ │  • GPT/Claude   │
    │  • Always ready │ │  • 2-3s latency │ │  • Cost tracked │
    └─────────────────┘ └─────────────────┘ └─────────────────┘
```

### Routing Strategy (per 0.2.5 Recommendation)

1. **Rule-based FIRST** - Fast (<50ms), 75% accuracy, always available
2. **LLM for edge cases** - When confidence < 65%, escalate to LLM
3. **Fallback on failure** - If LLM fails, return rule-based result

### Key Files Created

```
android/core/ai-provider/src/main/java/com/prio/core/aiprovider/
├── di/
│   └── AiProviderModule.kt          # Hilt DI module
├── llm/
│   ├── LlamaEngine.kt               # JNI bridge to llama.cpp
│   └── PromptFormatter.kt           # Prompt template formatting
├── provider/
│   ├── OnDeviceAiProvider.kt        # LLM-based provider
│   └── RuleBasedFallbackProvider.kt # Pattern-based classifier
└── router/
    └── AiProviderRouter.kt          # Smart routing with fallback
```

### Performance Results

| Provider | Latency | Accuracy | RAM | Availability |
|----------|---------|----------|-----|--------------|
| Rule-Based | <5ms | 75% | <10MB | 100% |
| On-Device LLM | 2-3s | 40-80% | 3.5GB | Requires model |
| Router (Hybrid) | <50ms typical | 75-80% | Minimal | 100% |

---

## Remaining Tasks (2.2.9-2.2.14)

| Task | Description | Status |
|------|-------------|--------|
| 2.2.9 | Create PromptTemplateRepository | 🔲 Not Started |
| 2.2.10 | Write Eisenhower classification prompts | 🔲 Not Started |
| 2.2.11 | Write task parsing prompts | 🔲 Not Started |
| 2.2.12 | Performance test: inference under 3 seconds | 🔲 Not Started |
| 2.2.13 | Write AI provider unit tests | ✅ Completed |
| 2.2.14 | Design CloudGatewayProvider stub | 🔲 Not Started |

---

## Conclusion

Tasks 2.2.1-2.2.8 establish the complete AI provider system for Prio:

1. ✅ Clean abstraction for swapping AI providers (AiProvider interface)
2. ✅ Runtime model switching (ModelRegistry, ModelDownloadManager)
3. ✅ Large model downloads with resume (HTTP Range support)
4. ✅ Backend API contract compatibility (serializable types)
5. ✅ Rule-based primary classifier (75% accuracy, <50ms)
6. ✅ On-device LLM provider (Phi-3, Mistral, Gemma support)
7. ✅ Smart routing with fallback chain (AiProviderRouter)
8. ✅ Override tracking for accuracy measurement

The architecture supports the 0.2.5 recommendation: **Rule-based primary with LLM for edge cases**.

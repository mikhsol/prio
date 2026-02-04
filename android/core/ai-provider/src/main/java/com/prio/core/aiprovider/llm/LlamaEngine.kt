package com.prio.core.aiprovider.llm

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JNI bridge to llama.cpp for on-device LLM inference.
 * 
 * Task 2.2.5: Integrate llama.cpp via JNI/NDK
 * 
 * This class provides:
 * - Model loading/unloading with lifecycle management
 * - Text generation with configurable parameters
 * - Benchmark metrics (load time, inference time, tokens/sec)
 * - State management with Kotlin Flow
 * 
 * Performance Budgets (from 0.2.2 benchmarks):
 * - Model loading: 1.5s for Phi-3-mini on Pixel 9a
 * - Inference: 2-3 seconds for classification
 * - RAM: 3.5GB for Phi-3-mini Q4
 */
@Singleton
class LlamaEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var modelHandle: Long = 0
    private val mutex = Mutex()
    private var isInitialized = false
    
    private val _state = MutableStateFlow(LlamaEngineState())
    val state: StateFlow<LlamaEngineState> = _state.asStateFlow()
    
    companion object {
        private const val TAG = "LlamaEngine"
        
        const val DEFAULT_CONTEXT_SIZE = 2048
        const val DEFAULT_THREADS = 4
        const val DEFAULT_TEMPERATURE = 0.3f
        const val DEFAULT_TOP_P = 0.9f
        const val DEFAULT_MAX_TOKENS = 256
        
        private var libraryLoaded = false
        private var libraryError: String? = null
        
        init {
            try {
                System.loadLibrary("llama_jni")
                libraryLoaded = true
                Timber.tag(TAG).i("llama_jni native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                libraryError = e.message
                Timber.tag(TAG).w(e, "Failed to load llama_jni native library - stub mode will be used")
            }
        }
        
        /**
         * Check if the native library is available.
         */
        fun isNativeLibraryAvailable(): Boolean = libraryLoaded
    }
    
    // Native method declarations - will use stub if library not loaded
    private external fun initBackend()
    private external fun nativeLoadModel(modelPath: String, contextSize: Int, nThreads: Int): Long
    private external fun nativeGenerate(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float
    ): String
    private external fun nativeUnloadModel(handle: Long)
    private external fun getMemoryUsage(handle: Long): Long
    private external fun getLoadTimeMs(handle: Long): Long
    private external fun getLastInferenceTimeMs(handle: Long): Long
    private external fun getLastTokenCount(handle: Long): Int
    private external fun isStubImplementation(handle: Long): Boolean
    private external fun cleanupBackend()
    
    /**
     * Initialize the llama.cpp backend. Call once on app startup.
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!isInitialized) {
                if (!libraryLoaded) {
                    val error = "Native library not available: ${libraryError ?: "unknown error"}"
                    Timber.tag(TAG).w(error)
                    _state.value = _state.value.copy(
                        isInitialized = false,
                        error = error,
                        isStubMode = true
                    )
                    return@withContext Result.failure(IllegalStateException(error))
                }
                
                try {
                    initBackend()
                    isInitialized = true
                    _state.value = _state.value.copy(isInitialized = true)
                    Timber.tag(TAG).i("LlamaEngine initialized")
                    Result.success(Unit)
                } catch (e: Exception) {
                    val error = "Failed to initialize llama.cpp backend: ${e.message}"
                    Timber.tag(TAG).e(e, error)
                    _state.value = _state.value.copy(error = error)
                    Result.failure(e)
                }
            } else {
                Result.success(Unit)
            }
        }
    }
    
    /**
     * Load a GGUF model from the given path.
     * 
     * @param modelPath Absolute path to the .gguf model file
     * @param contextSize Context window size (default 2048)
     * @param threads Number of CPU threads to use (default 4)
     * @return LoadResult with success status and timing info
     */
    suspend fun loadModel(
        modelPath: String,
        contextSize: Int = DEFAULT_CONTEXT_SIZE,
        threads: Int = DEFAULT_THREADS
    ): LoadResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            // Initialize if needed
            if (!isInitialized) {
                if (!libraryLoaded) {
                    val result = LoadResult(
                        success = false,
                        loadTimeMs = 0,
                        memoryBytes = 0,
                        isStub = true,
                        error = "Native library not available"
                    )
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isStubMode = true,
                        error = result.error
                    )
                    return@withContext result
                }
                initBackend()
                isInitialized = true
            }
            
            // Unload existing model
            if (modelHandle != 0L) {
                nativeUnloadModel(modelHandle)
                modelHandle = 0
            }
            
            val file = File(modelPath)
            if (!file.exists()) {
                val error = "Model file not found: $modelPath"
                Timber.tag(TAG).e(error)
                val result = LoadResult(
                    success = false,
                    loadTimeMs = 0,
                    memoryBytes = 0,
                    isStub = true,
                    error = error
                )
                _state.value = _state.value.copy(isLoading = false, error = error)
                return@withContext result
            }
            
            Timber.tag(TAG).i("Loading model: $modelPath (context=$contextSize, threads=$threads)")
            
            try {
                modelHandle = nativeLoadModel(modelPath, contextSize, threads)
                
                if (modelHandle == 0L) {
                    val error = "Failed to load model - native call returned null handle"
                    _state.value = _state.value.copy(isLoading = false, error = error)
                    return@withContext LoadResult(
                        success = false,
                        loadTimeMs = 0,
                        memoryBytes = 0,
                        isStub = true,
                        error = error
                    )
                }
                
                val loadTime = getLoadTimeMs(modelHandle)
                val memoryUsage = getMemoryUsage(modelHandle)
                val isStub = isStubImplementation(modelHandle)
                
                Timber.tag(TAG).i(
                    "Model loaded in ${loadTime}ms, memory: ${memoryUsage / 1_000_000}MB, stub: $isStub"
                )
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    isModelLoaded = true,
                    isStubMode = isStub,
                    loadedModelPath = modelPath,
                    loadTimeMs = loadTime,
                    memoryUsageBytes = memoryUsage
                )
                
                LoadResult(
                    success = true,
                    loadTimeMs = loadTime,
                    memoryBytes = memoryUsage,
                    isStub = isStub,
                    error = null
                )
            } catch (e: Exception) {
                val error = "Exception loading model: ${e.message}"
                Timber.tag(TAG).e(e, error)
                _state.value = _state.value.copy(isLoading = false, error = error)
                LoadResult(
                    success = false,
                    loadTimeMs = 0,
                    memoryBytes = 0,
                    isStub = true,
                    error = error
                )
            }
        }
    }
    
    /**
     * Generate text completion for the given prompt.
     * 
     * @param prompt The input prompt for generation
     * @param maxTokens Maximum number of tokens to generate
     * @param temperature Temperature for sampling (0.0-2.0)
     * @param topP Top-p nucleus sampling parameter
     * @return GenerateResult with generated text and timing info
     */
    suspend fun generate(
        prompt: String,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
        temperature: Float = DEFAULT_TEMPERATURE,
        topP: Float = DEFAULT_TOP_P
    ): GenerateResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (modelHandle == 0L) {
                return@withContext GenerateResult(
                    text = "",
                    inferenceTimeMs = 0,
                    tokensGenerated = 0,
                    tokensPerSecond = 0.0,
                    error = "Model not loaded"
                )
            }
            
            Timber.tag(TAG).d("Generating (maxTokens=$maxTokens, temp=$temperature, topP=$topP)")
            
            try {
                val result = nativeGenerate(modelHandle, prompt, maxTokens, temperature, topP)
                val inferenceTime = getLastInferenceTimeMs(modelHandle)
                val tokenCount = getLastTokenCount(modelHandle)
                
                val tokensPerSec = if (inferenceTime > 0) {
                    tokenCount.toDouble() / (inferenceTime.toDouble() / 1000.0)
                } else {
                    0.0
                }
                
                Timber.tag(TAG).d("Generated $tokenCount tokens in ${inferenceTime}ms (${String.format("%.1f", tokensPerSec)} t/s)")
                
                _state.value = _state.value.copy(
                    lastInferenceTimeMs = inferenceTime,
                    lastTokensGenerated = tokenCount
                )
                
                GenerateResult(
                    text = result,
                    inferenceTimeMs = inferenceTime,
                    tokensGenerated = tokenCount,
                    tokensPerSecond = tokensPerSec,
                    error = null
                )
            } catch (e: Exception) {
                val error = "Generation failed: ${e.message}"
                Timber.tag(TAG).e(e, error)
                GenerateResult(
                    text = "",
                    inferenceTimeMs = 0,
                    tokensGenerated = 0,
                    tokensPerSecond = 0.0,
                    error = error
                )
            }
        }
    }
    
    /**
     * Check if a model is currently loaded.
     */
    val isLoaded: Boolean
        get() = modelHandle != 0L
    
    /**
     * Check if using stub implementation.
     */
    val isStub: Boolean
        get() = if (modelHandle != 0L && libraryLoaded) {
            try {
                isStubImplementation(modelHandle)
            } catch (e: Exception) {
                true
            }
        } else true
    
    /**
     * Get current memory usage in bytes.
     */
    suspend fun getMemoryUsageBytes(): Long = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (modelHandle == 0L || !libraryLoaded) 0 else {
                try {
                    getMemoryUsage(modelHandle)
                } catch (e: Exception) {
                    0
                }
            }
        }
    }
    
    /**
     * Unload the current model.
     */
    suspend fun unload() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (modelHandle != 0L && libraryLoaded) {
                try {
                    nativeUnloadModel(modelHandle)
                    Timber.tag(TAG).i("Model unloaded")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error unloading model")
                }
                modelHandle = 0
            }
            _state.value = _state.value.copy(
                isModelLoaded = false,
                loadedModelPath = null,
                memoryUsageBytes = 0
            )
        }
    }
    
    /**
     * Cleanup backend resources.
     */
    suspend fun cleanup() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (modelHandle != 0L && libraryLoaded) {
                try {
                    nativeUnloadModel(modelHandle)
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Error unloading model during cleanup")
                }
                modelHandle = 0
            }
            if (isInitialized && libraryLoaded) {
                try {
                    cleanupBackend()
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Error cleaning up backend")
                }
                isInitialized = false
            }
            _state.value = LlamaEngineState()
            Timber.tag(TAG).i("LlamaEngine cleaned up")
        }
    }
    
    /**
     * Result of model loading operation.
     */
    data class LoadResult(
        val success: Boolean,
        val loadTimeMs: Long,
        val memoryBytes: Long,
        val isStub: Boolean,
        val error: String?
    )
    
    /**
     * Result of text generation operation.
     */
    data class GenerateResult(
        val text: String,
        val inferenceTimeMs: Long,
        val tokensGenerated: Int,
        val tokensPerSecond: Double,
        val error: String?
    )
}

/**
 * State of the LlamaEngine for UI observation.
 */
data class LlamaEngineState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val isModelLoaded: Boolean = false,
    val isStubMode: Boolean = false,
    val loadedModelPath: String? = null,
    val loadTimeMs: Long = 0,
    val memoryUsageBytes: Long = 0,
    val lastInferenceTimeMs: Long = 0,
    val lastTokensGenerated: Int = 0,
    val error: String? = null
)

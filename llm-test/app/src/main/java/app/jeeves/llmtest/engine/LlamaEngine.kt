package app.jeeves.llmtest.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * JNI bridge to llama.cpp for on-device LLM inference.
 * 
 * Task 0.2.1: Set up llama.cpp Android test project with JNI
 * 
 * This class provides:
 * - Model loading/unloading with lifecycle management
 * - Text generation with configurable parameters
 * - Benchmark metrics (load time, inference time, tokens/sec)
 * - Stub implementation fallback when llama.cpp is unavailable
 */
class LlamaEngine(private val context: Context) {
    
    private var modelHandle: Long = 0
    private val mutex = Mutex()
    private var isInitialized = false
    
    companion object {
        private const val TAG = "LlamaEngine"
        
        const val DEFAULT_CONTEXT_SIZE = 2048
        const val DEFAULT_THREADS = 4
        const val DEFAULT_TEMPERATURE = 0.3f
        const val DEFAULT_TOP_P = 0.9f
        const val DEFAULT_MAX_TOKENS = 256
        
        init {
            try {
                System.loadLibrary("llama_jni")
            } catch (e: UnsatisfiedLinkError) {
                android.util.Log.e(TAG, "Failed to load native library", e)
            }
        }
    }
    
    // Native method declarations
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
    suspend fun initialize() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!isInitialized) {
                initBackend()
                isInitialized = true
                android.util.Log.i(TAG, "LlamaEngine initialized")
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
            // Initialize if needed
            if (!isInitialized) {
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
                android.util.Log.e(TAG, "Model file not found: $modelPath")
                return@withContext LoadResult(
                    success = false,
                    loadTimeMs = 0,
                    memoryBytes = 0,
                    isStub = true,
                    error = "Model file not found: $modelPath"
                )
            }
            
            android.util.Log.i(TAG, "Loading model: $modelPath")
            modelHandle = nativeLoadModel(modelPath, contextSize, threads)
            
            if (modelHandle == 0L) {
                return@withContext LoadResult(
                    success = false,
                    loadTimeMs = 0,
                    memoryBytes = 0,
                    isStub = true,
                    error = "Failed to load model"
                )
            }
            
            val loadTime = getLoadTimeMs(modelHandle)
            val memoryUsage = getMemoryUsage(modelHandle)
            val isStub = isStubImplementation(modelHandle)
            
            android.util.Log.i(TAG, "Model loaded in ${loadTime}ms, memory: ${memoryUsage / 1_000_000}MB, stub: $isStub")
            
            LoadResult(
                success = true,
                loadTimeMs = loadTime,
                memoryBytes = memoryUsage,
                isStub = isStub,
                error = null
            )
        }
    }
    
    /**
     * Load model using stub implementation (for testing without actual model file).
     */
    suspend fun loadStubModel(): LoadResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!isInitialized) {
                initBackend()
                isInitialized = true
            }
            
            if (modelHandle != 0L) {
                nativeUnloadModel(modelHandle)
                modelHandle = 0
            }
            
            // Load with a dummy path - stub will handle it
            modelHandle = nativeLoadModel("/stub/model.gguf", DEFAULT_CONTEXT_SIZE, DEFAULT_THREADS)
            
            val loadTime = getLoadTimeMs(modelHandle)
            val memoryUsage = getMemoryUsage(modelHandle)
            
            LoadResult(
                success = true,
                loadTimeMs = loadTime,
                memoryBytes = memoryUsage,
                isStub = true,
                error = null
            )
        }
    }
    
    /**
     * Generate text completion for the given prompt.
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
            
            val startTime = System.currentTimeMillis()
            val result = nativeGenerate(modelHandle, prompt, maxTokens, temperature, topP)
            val inferenceTime = getLastInferenceTimeMs(modelHandle)
            val tokenCount = getLastTokenCount(modelHandle)
            
            val tokensPerSec = if (inferenceTime > 0) {
                tokenCount.toDouble() / (inferenceTime.toDouble() / 1000.0)
            } else {
                0.0
            }
            
            GenerateResult(
                text = result,
                inferenceTimeMs = inferenceTime,
                tokensGenerated = tokenCount,
                tokensPerSecond = tokensPerSec,
                error = null
            )
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
        get() = if (modelHandle != 0L) isStubImplementation(modelHandle) else true
    
    /**
     * Get current memory usage in bytes.
     */
    suspend fun getMemoryUsageBytes(): Long = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (modelHandle == 0L) 0 else getMemoryUsage(modelHandle)
        }
    }
    
    /**
     * Unload the current model.
     */
    suspend fun unload() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (modelHandle != 0L) {
                nativeUnloadModel(modelHandle)
                modelHandle = 0
                android.util.Log.i(TAG, "Model unloaded")
            }
        }
    }
    
    /**
     * Cleanup backend resources.
     */
    suspend fun cleanup() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (modelHandle != 0L) {
                nativeUnloadModel(modelHandle)
                modelHandle = 0
            }
            if (isInitialized) {
                cleanupBackend()
                isInitialized = false
            }
            android.util.Log.i(TAG, "LlamaEngine cleaned up")
        }
    }
    
    data class LoadResult(
        val success: Boolean,
        val loadTimeMs: Long,
        val memoryBytes: Long,
        val isStub: Boolean,
        val error: String?
    )
    
    data class GenerateResult(
        val text: String,
        val inferenceTimeMs: Long,
        val tokensGenerated: Int,
        val tokensPerSecond: Double,
        val error: String?
    )
}

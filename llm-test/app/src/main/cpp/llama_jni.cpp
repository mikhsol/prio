/**
 * Jeeves LLM Test Project - JNI Bridge to llama.cpp
 * 
 * This file provides the JNI interface for on-device LLM inference.
 * For the test project, it includes a stub implementation that simulates
 * llama.cpp behavior when the actual library is not available.
 * 
 * Task 0.2.1: Set up llama.cpp Android test project with JNI
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <memory>
#include <mutex>
#include <chrono>
#include <random>
#include <sstream>
#include <cmath>
#include <iomanip>
#include <thread>

#define LOG_TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#if LLAMA_AVAILABLE
#include "llama.h"
#endif

// ============================================================================
// Context wrapper for thread-safe model access
// ============================================================================

struct LlamaContext {
#if LLAMA_AVAILABLE
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
#endif
    std::mutex mutex;
    bool is_stub = false;
    
    // Benchmark metrics
    long long load_time_ms = 0;
    long long last_inference_time_ms = 0;
    int last_tokens_generated = 0;
    size_t memory_usage_bytes = 0;
    
    LlamaContext() {
#if !LLAMA_AVAILABLE
        is_stub = true;
#endif
    }
    
    ~LlamaContext() {
#if LLAMA_AVAILABLE
        if (ctx) llama_free(ctx);
        if (model) llama_free_model(model);
#endif
    }
};

// ============================================================================
// Stub implementation for testing without llama.cpp
// Simulates realistic timing and provides rule-based classification
// ============================================================================

namespace stub {

// Simulated model parameters
const size_t SIMULATED_MODEL_SIZE = 2400000000; // 2.4 GB
const int SIMULATED_TOKENS_PER_SEC = 18;
const int SIMULATED_LOAD_TIME_MS = 3500;

// Eisenhower classification patterns
struct Pattern {
    const char* regex_like;
    const char* quadrant;
    float confidence;
};

const Pattern URGENT_PATTERNS[] = {
    {"urgent", "Q1", 0.85f},
    {"asap", "Q1", 0.80f},
    {"immediately", "Q1", 0.90f},
    {"deadline today", "Q1", 0.95f},
    {"due today", "Q1", 0.90f},
    {"emergency", "Q1", 0.95f},
    {"critical", "Q1", 0.85f},
    {"server down", "Q1", 0.95f},
    {"client waiting", "Q1", 0.85f},
};

const Pattern IMPORTANT_PATTERNS[] = {
    {"plan", "Q2", 0.80f},
    {"strategy", "Q2", 0.85f},
    {"goal", "Q2", 0.80f},
    {"learn", "Q2", 0.75f},
    {"health", "Q2", 0.80f},
    {"exercise", "Q2", 0.75f},
    {"relationship", "Q2", 0.80f},
    {"career", "Q2", 0.85f},
    {"quarterly", "Q2", 0.80f},
};

const Pattern DELEGATE_PATTERNS[] = {
    {"routine", "Q3", 0.75f},
    {"order supplies", "Q3", 0.80f},
    {"schedule meeting", "Q3", 0.70f},
    {"file", "Q3", 0.65f},
    {"organize", "Q3", 0.65f},
    {"survey", "Q3", 0.70f},
    {"report", "Q3", 0.65f},
};

const Pattern ELIMINATE_PATTERNS[] = {
    {"social media", "Q4", 0.90f},
    {"youtube", "Q4", 0.80f},
    {"browse", "Q4", 0.75f},
    {"scroll", "Q4", 0.85f},
    {"optional", "Q4", 0.70f},
    {"someday", "Q4", 0.75f},
    {"maybe", "Q4", 0.65f},
};

std::string to_lower(const std::string& s) {
    std::string result = s;
    for (char& c : result) {
        c = std::tolower(c);
    }
    return result;
}

bool contains(const std::string& text, const char* pattern) {
    return to_lower(text).find(pattern) != std::string::npos;
}

std::string classify_eisenhower(const std::string& task_text) {
    std::string lower_text = to_lower(task_text);
    
    float urgent_score = 0.0f;
    float important_score = 0.0f;
    float delegate_score = 0.0f;
    float eliminate_score = 0.0f;
    
    // Check patterns
    for (const auto& p : URGENT_PATTERNS) {
        if (contains(lower_text, p.regex_like)) {
            urgent_score = std::max(urgent_score, p.confidence);
        }
    }
    
    for (const auto& p : IMPORTANT_PATTERNS) {
        if (contains(lower_text, p.regex_like)) {
            important_score = std::max(important_score, p.confidence);
        }
    }
    
    for (const auto& p : DELEGATE_PATTERNS) {
        if (contains(lower_text, p.regex_like)) {
            delegate_score = std::max(delegate_score, p.confidence);
        }
    }
    
    for (const auto& p : ELIMINATE_PATTERNS) {
        if (contains(lower_text, p.regex_like)) {
            eliminate_score = std::max(eliminate_score, p.confidence);
        }
    }
    
    // Determine quadrant
    std::string quadrant;
    float confidence;
    std::string reasoning;
    
    bool is_urgent = urgent_score > 0.5f;
    bool is_important = important_score > 0.5f || urgent_score > 0.8f;
    
    if (eliminate_score > 0.7f) {
        quadrant = "ELIMINATE";
        confidence = eliminate_score;
        reasoning = "Task appears to be low-value or time-wasting activity";
    } else if (is_urgent && is_important) {
        quadrant = "DO";
        confidence = std::max(urgent_score, important_score);
        reasoning = "Task is both urgent and important - requires immediate attention";
    } else if (!is_urgent && is_important) {
        quadrant = "SCHEDULE";
        confidence = important_score;
        reasoning = "Task is important but not time-sensitive - schedule for focused work";
    } else if (is_urgent && !is_important) {
        quadrant = "DELEGATE";
        confidence = std::max(urgent_score, delegate_score);
        reasoning = "Task is urgent but not personally important - consider delegation";
    } else if (delegate_score > 0.5f) {
        quadrant = "DELEGATE";
        confidence = delegate_score;
        reasoning = "Routine task that can be handled by others";
    } else {
        // Default to SCHEDULE for ambiguous tasks
        quadrant = "SCHEDULE";
        confidence = 0.6f;
        reasoning = "Task importance unclear - scheduling for review";
    }
    
    std::ostringstream json;
    json << "{\"quadrant\": \"" << quadrant << "\", "
         << "\"confidence\": " << std::fixed << std::setprecision(2) << confidence << ", "
         << "\"reasoning\": \"" << reasoning << "\"}";
    
    return json.str();
}

void simulate_delay(int tokens) {
    // Simulate realistic inference time
    int delay_ms = (tokens * 1000) / SIMULATED_TOKENS_PER_SEC;
    std::this_thread::sleep_for(std::chrono::milliseconds(delay_ms));
}

} // namespace stub

// ============================================================================
// JNI Functions
// ============================================================================

extern "C" {

JNIEXPORT void JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_initBackend(JNIEnv* env, jobject thiz) {
#if LLAMA_AVAILABLE
    llama_backend_init();
    LOGI("llama.cpp backend initialized (real implementation)");
#else
    LOGI("llama.cpp backend initialized (stub implementation)");
#endif
}

JNIEXPORT jlong JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_nativeLoadModel(
    JNIEnv* env,
    jobject thiz,
    jstring modelPath,
    jint contextSize,
    jint nThreads
) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model from: %s (context=%d, threads=%d)", path, contextSize, nThreads);
    
    auto start = std::chrono::steady_clock::now();
    auto* wrapper = new LlamaContext();
    
#if LLAMA_AVAILABLE
    // Real llama.cpp implementation
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    
    wrapper->model = llama_load_model_from_file(path, model_params);
    if (!wrapper->model) {
        LOGE("Failed to load model");
        env->ReleaseStringUTFChars(modelPath, path);
        delete wrapper;
        return 0;
    }
    
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = contextSize;
    ctx_params.n_threads = nThreads;
    ctx_params.n_threads_batch = nThreads;
    
    wrapper->ctx = llama_new_context_with_model(wrapper->model, ctx_params);
    if (!wrapper->ctx) {
        LOGE("Failed to create context");
        llama_free_model(wrapper->model);
        env->ReleaseStringUTFChars(modelPath, path);
        delete wrapper;
        return 0;
    }
    
    wrapper->memory_usage_bytes = llama_get_state_size(wrapper->ctx);
#else
    // Stub implementation - simulate loading
    std::this_thread::sleep_for(std::chrono::milliseconds(stub::SIMULATED_LOAD_TIME_MS));
    wrapper->is_stub = true;
    wrapper->memory_usage_bytes = stub::SIMULATED_MODEL_SIZE;
#endif
    
    auto end = std::chrono::steady_clock::now();
    wrapper->load_time_ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    
    env->ReleaseStringUTFChars(modelPath, path);
    LOGI("Model loaded in %lld ms. Memory: %zu bytes", wrapper->load_time_ms, wrapper->memory_usage_bytes);
    
    return reinterpret_cast<jlong>(wrapper);
}

JNIEXPORT jstring JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_nativeGenerate(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jstring prompt,
    jint maxTokens,
    jfloat temperature,
    jfloat topP
) {
    if (handle == 0) {
        return env->NewStringUTF("");
    }
    
    auto* wrapper = reinterpret_cast<LlamaContext*>(handle);
    std::lock_guard<std::mutex> lock(wrapper->mutex);
    
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    std::string promptCpp(promptStr);
    env->ReleaseStringUTFChars(prompt, promptStr);
    
    auto start = std::chrono::steady_clock::now();
    std::string result;
    int tokens_generated = 0;
    
#if LLAMA_AVAILABLE
    // Real llama.cpp implementation
    std::vector<llama_token> tokens(llama_n_ctx(wrapper->ctx));
    int n_tokens = llama_tokenize(
        wrapper->model, promptCpp.c_str(), promptCpp.length(),
        tokens.data(), tokens.size(), true, false
    );
    tokens.resize(n_tokens);
    
    llama_kv_cache_clear(wrapper->ctx);
    
    llama_batch batch = llama_batch_init(tokens.size(), 0, 1);
    for (size_t i = 0; i < tokens.size(); i++) {
        llama_batch_add(batch, tokens[i], i, {0}, false);
    }
    batch.logits[batch.n_tokens - 1] = true;
    
    if (llama_decode(wrapper->ctx, batch) != 0) {
        LOGE("Prompt decode failed");
        llama_batch_free(batch);
        return env->NewStringUTF("");
    }
    llama_batch_free(batch);
    
    llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(topP, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(42));
    
    int n_cur = tokens.size();
    for (int i = 0; i < maxTokens; i++) {
        llama_token new_token = llama_sampler_sample(sampler, wrapper->ctx, -1);
        
        if (llama_token_is_eog(wrapper->model, new_token)) break;
        
        char buf[256];
        int n = llama_token_to_piece(wrapper->model, new_token, buf, sizeof(buf), 0, true);
        if (n > 0) result.append(buf, n);
        tokens_generated++;
        
        llama_batch next_batch = llama_batch_init(1, 0, 1);
        llama_batch_add(next_batch, new_token, n_cur, {0}, true);
        if (llama_decode(wrapper->ctx, next_batch) != 0) {
            llama_batch_free(next_batch);
            break;
        }
        llama_batch_free(next_batch);
        n_cur++;
    }
    llama_sampler_free(sampler);
#else
    // Stub implementation - use rule-based classification
    LOGD("Using stub implementation for generation");
    
    // Check if this is an Eisenhower classification request
    if (promptCpp.find("Eisenhower") != std::string::npos || 
        promptCpp.find("quadrant") != std::string::npos) {
        
        // Extract task text from prompt
        size_t task_start = promptCpp.find("Task:");
        if (task_start != std::string::npos) {
            std::string task_text = promptCpp.substr(task_start + 5);
            result = stub::classify_eisenhower(task_text);
            tokens_generated = 50; // Approximate
        } else {
            result = "{\"quadrant\": \"SCHEDULE\", \"confidence\": 0.5, \"reasoning\": \"Unable to parse task\"}";
            tokens_generated = 30;
        }
    } else {
        // Generic response
        result = "This is a stub response. In production, this would be generated by Phi-3-mini.";
        tokens_generated = 20;
    }
    
    // Simulate realistic timing
    stub::simulate_delay(tokens_generated);
#endif
    
    auto end = std::chrono::steady_clock::now();
    wrapper->last_inference_time_ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    wrapper->last_tokens_generated = tokens_generated;
    
    LOGD("Generated %d tokens in %lld ms", tokens_generated, wrapper->last_inference_time_ms);
    
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_nativeUnloadModel(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle != 0) {
        auto* wrapper = reinterpret_cast<LlamaContext*>(handle);
        delete wrapper;
        LOGI("Model unloaded");
    }
}

JNIEXPORT jlong JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_getMemoryUsage(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0) return 0;
    auto* wrapper = reinterpret_cast<LlamaContext*>(handle);
    return static_cast<jlong>(wrapper->memory_usage_bytes);
}

JNIEXPORT jlong JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_getLoadTimeMs(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0) return 0;
    auto* wrapper = reinterpret_cast<LlamaContext*>(handle);
    return static_cast<jlong>(wrapper->load_time_ms);
}

JNIEXPORT jlong JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_getLastInferenceTimeMs(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0) return 0;
    auto* wrapper = reinterpret_cast<LlamaContext*>(handle);
    return static_cast<jlong>(wrapper->last_inference_time_ms);
}

JNIEXPORT jint JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_getLastTokenCount(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0) return 0;
    auto* wrapper = reinterpret_cast<LlamaContext*>(handle);
    return static_cast<jint>(wrapper->last_tokens_generated);
}

JNIEXPORT jboolean JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_isStubImplementation(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0) return JNI_TRUE;
    auto* wrapper = reinterpret_cast<LlamaContext*>(handle);
    return wrapper->is_stub ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_cleanupBackend(JNIEnv* env, jobject thiz) {
#if LLAMA_AVAILABLE
    llama_backend_free();
#endif
    LOGI("llama.cpp backend cleaned up");
}

} // extern "C"

/**
 * Jeeves LLM Test Project - JNI Bridge to llama.cpp
 * 
 * Updated for llama.cpp 2024+ API
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
        if (model) llama_model_free(model);
#endif
    }
};

// ============================================================================
// Stub implementation for testing without llama.cpp
// ============================================================================

namespace stub {

const size_t SIMULATED_MODEL_SIZE = 2400000000;
const int SIMULATED_TOKENS_PER_SEC = 18;
const int SIMULATED_LOAD_TIME_MS = 3500;

std::string to_lower(const std::string& s) {
    std::string result = s;
    for (char& c : result) c = std::tolower(c);
    return result;
}

bool contains(const std::string& text, const char* pattern) {
    return to_lower(text).find(pattern) != std::string::npos;
}

std::string classify_eisenhower(const std::string& task_text) {
    std::string lower_text = to_lower(task_text);
    
    std::string quadrant = "SCHEDULE";
    float confidence = 0.6f;
    std::string reasoning = "Default classification";
    
    // Urgency patterns
    bool is_urgent = contains(lower_text, "urgent") || contains(lower_text, "asap") ||
        contains(lower_text, "immediately") || contains(lower_text, "deadline today") ||
        contains(lower_text, "due today") || contains(lower_text, "emergency") ||
        contains(lower_text, "server down") || contains(lower_text, "crisis") ||
        contains(lower_text, "in 2 hours") || contains(lower_text, "in 30 minute");
    
    // Importance patterns
    bool is_important = contains(lower_text, "client") || contains(lower_text, "customer") ||
        contains(lower_text, "board") || contains(lower_text, "investor") ||
        contains(lower_text, "strategy") || contains(lower_text, "goal") ||
        contains(lower_text, "health") || contains(lower_text, "career") ||
        contains(lower_text, "tax") || contains(lower_text, "contract");
    
    // Low priority patterns
    bool is_low = contains(lower_text, "social media") || contains(lower_text, "youtube") ||
        contains(lower_text, "browse") || contains(lower_text, "optional") ||
        contains(lower_text, "reorganize") || contains(lower_text, "third time");
    
    // Delegation patterns
    bool is_delegatable = contains(lower_text, "order supplies") || contains(lower_text, "survey") ||
        contains(lower_text, "status report") || contains(lower_text, "schedule team");
    
    if (is_low) {
        quadrant = "ELIMINATE";
        confidence = 0.85f;
        reasoning = "Low priority activity detected";
    } else if (is_urgent && is_important) {
        quadrant = "DO";
        confidence = 0.9f;
        reasoning = "Both urgent and important";
    } else if (!is_urgent && is_important) {
        quadrant = "SCHEDULE";
        confidence = 0.8f;
        reasoning = "Important but not time-sensitive";
    } else if (is_delegatable || (is_urgent && !is_important)) {
        quadrant = "DELEGATE";
        confidence = 0.75f;
        reasoning = "Routine task suitable for delegation";
    }
    
    std::ostringstream json;
    json << "{\"quadrant\": \"" << quadrant << "\", "
         << "\"confidence\": " << std::fixed << std::setprecision(2) << confidence << ", "
         << "\"reasoning\": \"" << reasoning << "\"}";
    
    return json.str();
}

void simulate_delay(int tokens) {
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
    JNIEnv* env, jobject thiz, jstring modelPath, jint contextSize, jint nThreads
) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model from: %s (context=%d, threads=%d)", path, contextSize, nThreads);
    
    // Check if file is readable
    FILE* f = fopen(path, "rb");
    if (!f) {
        LOGE("Cannot open file: %s (errno=%d)", path, errno);
        env->ReleaseStringUTFChars(modelPath, path);
        return 0;
    }
    fseek(f, 0, SEEK_END);
    long size = ftell(f);
    fclose(f);
    LOGI("File size: %ld bytes", size);
    
    auto start = std::chrono::steady_clock::now();
    auto* wrapper = new LlamaContext();
    
#if LLAMA_AVAILABLE
    LOGI("Creating model params...");
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    
    LOGI("Calling llama_model_load_from_file...");
    wrapper->model = llama_model_load_from_file(path, model_params);
    if (!wrapper->model) {
        LOGE("Failed to load model - llama_model_load_from_file returned null");
        env->ReleaseStringUTFChars(modelPath, path);
        delete wrapper;
        return 0;
    }
    LOGI("Model loaded successfully");
    
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = contextSize;
    ctx_params.n_threads = nThreads;
    ctx_params.n_threads_batch = nThreads;
    
    LOGI("Creating context...");
    wrapper->ctx = llama_init_from_model(wrapper->model, ctx_params);
    if (!wrapper->ctx) {
        LOGE("Failed to create context");
        llama_model_free(wrapper->model);
        env->ReleaseStringUTFChars(modelPath, path);
        delete wrapper;
        return 0;
    }
    LOGI("Context created successfully");
    
    wrapper->memory_usage_bytes = llama_state_get_size(wrapper->ctx);
#else
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
    JNIEnv* env, jobject thiz, jlong handle, jstring prompt,
    jint maxTokens, jfloat temperature, jfloat topP
) {
    if (handle == 0) return env->NewStringUTF("");
    
    auto* wrapper = reinterpret_cast<LlamaContext*>(handle);
    std::lock_guard<std::mutex> lock(wrapper->mutex);
    
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    std::string promptCpp(promptStr);
    env->ReleaseStringUTFChars(prompt, promptStr);
    
    auto start = std::chrono::steady_clock::now();
    std::string result;
    int tokens_generated = 0;
    
#if LLAMA_AVAILABLE
    // Get vocabulary
    const llama_vocab* vocab = llama_model_get_vocab(wrapper->model);
    
    // Tokenize input
    std::vector<llama_token> tokens(llama_n_ctx(wrapper->ctx));
    int n_tokens = llama_tokenize(vocab, promptCpp.c_str(), promptCpp.length(),
                                   tokens.data(), tokens.size(), true, false);
    if (n_tokens < 0) {
        LOGE("Tokenization failed");
        return env->NewStringUTF("");
    }
    tokens.resize(n_tokens);
    LOGD("Tokenized %d tokens", n_tokens);
    
    // Clear KV cache
    llama_memory_t mem = llama_get_memory(wrapper->ctx);
    llama_memory_clear(mem, true);
    
    // Create batch for prompt
    llama_batch batch = llama_batch_init(tokens.size(), 0, 1);
    for (size_t i = 0; i < tokens.size(); i++) {
        batch.token[i] = tokens[i];
        batch.pos[i] = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = (i == tokens.size() - 1);
    }
    batch.n_tokens = tokens.size();
    
    if (llama_decode(wrapper->ctx, batch) != 0) {
        LOGE("Prompt decode failed");
        llama_batch_free(batch);
        return env->NewStringUTF("");
    }
    llama_batch_free(batch);
    
    // Setup sampler
    llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(topP, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(42));
    
    // Generate tokens
    int n_cur = tokens.size();
    for (int i = 0; i < maxTokens; i++) {
        llama_token new_token = llama_sampler_sample(sampler, wrapper->ctx, -1);
        
        if (llama_vocab_is_eog(vocab, new_token)) break;
        
        char buf[256];
        int n = llama_token_to_piece(vocab, new_token, buf, sizeof(buf), 0, true);
        if (n > 0) result.append(buf, n);
        tokens_generated++;
        
        // Decode next token
        llama_batch next_batch = llama_batch_init(1, 0, 1);
        next_batch.token[0] = new_token;
        next_batch.pos[0] = n_cur;
        next_batch.n_seq_id[0] = 1;
        next_batch.seq_id[0][0] = 0;
        next_batch.logits[0] = true;
        next_batch.n_tokens = 1;
        
        if (llama_decode(wrapper->ctx, next_batch) != 0) {
            llama_batch_free(next_batch);
            break;
        }
        llama_batch_free(next_batch);
        n_cur++;
    }
    llama_sampler_free(sampler);
#else
    LOGD("Using stub implementation for generation");
    if (promptCpp.find("Eisenhower") != std::string::npos || 
        promptCpp.find("quadrant") != std::string::npos ||
        promptCpp.find("classify") != std::string::npos) {
        size_t task_start = promptCpp.rfind("\"");
        if (task_start != std::string::npos) {
            size_t task_end = promptCpp.rfind("\"", task_start - 1);
            if (task_end != std::string::npos) {
                std::string task_text = promptCpp.substr(task_end + 1, task_start - task_end - 1);
                result = stub::classify_eisenhower(task_text);
            }
        }
        if (result.empty()) {
            result = stub::classify_eisenhower(promptCpp);
        }
        tokens_generated = 50;
    } else {
        result = "This is a stub response.";
        tokens_generated = 20;
    }
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
    return static_cast<jlong>(reinterpret_cast<LlamaContext*>(handle)->memory_usage_bytes);
}

JNIEXPORT jlong JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_getLoadTimeMs(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0) return 0;
    return static_cast<jlong>(reinterpret_cast<LlamaContext*>(handle)->load_time_ms);
}

JNIEXPORT jlong JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_getLastInferenceTimeMs(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0) return 0;
    return static_cast<jlong>(reinterpret_cast<LlamaContext*>(handle)->last_inference_time_ms);
}

JNIEXPORT jint JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_getLastTokenCount(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0) return 0;
    return static_cast<jint>(reinterpret_cast<LlamaContext*>(handle)->last_tokens_generated);
}

JNIEXPORT jboolean JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_isStubImplementation(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0) return JNI_TRUE;
    return reinterpret_cast<LlamaContext*>(handle)->is_stub ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_app_jeeves_llmtest_engine_LlamaEngine_cleanupBackend(JNIEnv* env, jobject thiz) {
#if LLAMA_AVAILABLE
    llama_backend_free();
#endif
    LOGI("llama.cpp backend cleaned up");
}

} // extern "C"

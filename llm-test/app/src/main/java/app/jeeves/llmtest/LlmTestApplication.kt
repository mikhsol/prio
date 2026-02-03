package app.jeeves.llmtest

import android.app.Application

/**
 * Application class for LLM Test project.
 * Initializes the llama.cpp backend on app start.
 */
class LlmTestApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Backend initialization will happen when LlamaEngine is first used
    }
}

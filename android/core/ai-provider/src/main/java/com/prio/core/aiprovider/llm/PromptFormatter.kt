package com.prio.core.aiprovider.llm

import com.prio.core.ai.provider.PromptTemplate

/**
 * Prompt formatting utilities for different LLM models.
 * 
 * Task 2.2.5: Integrate llama.cpp via JNI/NDK
 * 
 * Each model family has its own chat template format that must be
 * followed for optimal results. This class provides formatting
 * utilities to wrap prompts in the correct template.
 * 
 * Templates verified from:
 * - Phi-3: Microsoft documentation
 * - Mistral: Mistral AI documentation
 * - ChatML: OpenAI convention
 * - Gemma: Google documentation
 */
object PromptFormatter {
    
    /**
     * Format a prompt using the specified template.
     * 
     * @param template The template format to use
     * @param systemPrompt Optional system prompt
     * @param userPrompt The user's input
     * @return Formatted prompt string ready for LLM inference
     */
    fun format(
        template: PromptTemplate,
        systemPrompt: String?,
        userPrompt: String
    ): String {
        return when (template) {
            PromptTemplate.PHI3 -> formatPhi3(systemPrompt, userPrompt)
            PromptTemplate.MISTRAL -> formatMistral(systemPrompt, userPrompt)
            PromptTemplate.CHATML -> formatChatMl(systemPrompt, userPrompt)
            PromptTemplate.LLAMA2 -> formatLlama2(systemPrompt, userPrompt)
            PromptTemplate.LLAMA3 -> formatLlama3(systemPrompt, userPrompt)
            PromptTemplate.GEMMA -> formatGemma(systemPrompt, userPrompt)
            PromptTemplate.RAW -> userPrompt
        }
    }
    
    /**
     * Phi-3 format: <|user|>...<|end|><|assistant|>
     * 
     * Used by Microsoft Phi-3 family models.
     * The system prompt is embedded in the first user message.
     */
    private fun formatPhi3(systemPrompt: String?, userPrompt: String): String {
        val fullPrompt = if (systemPrompt != null) {
            "$systemPrompt\n\n$userPrompt"
        } else {
            userPrompt
        }
        return "<|user|>\n$fullPrompt<|end|>\n<|assistant|>\n"
    }
    
    /**
     * Mistral format: [INST]...[/INST]
     * 
     * Used by Mistral AI models (Mistral 7B, etc.).
     */
    private fun formatMistral(systemPrompt: String?, userPrompt: String): String {
        val fullPrompt = if (systemPrompt != null) {
            "$systemPrompt\n\n$userPrompt"
        } else {
            userPrompt
        }
        return "<s>[INST] $fullPrompt [/INST]"
    }
    
    /**
     * ChatML format: <|im_start|>role\ncontent<|im_end|>
     * 
     * Common format used by many models including some fine-tuned variants.
     */
    private fun formatChatMl(systemPrompt: String?, userPrompt: String): String {
        val sb = StringBuilder()
        if (systemPrompt != null) {
            sb.append("<|im_start|>system\n$systemPrompt<|im_end|>\n")
        }
        sb.append("<|im_start|>user\n$userPrompt<|im_end|>\n")
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }
    
    /**
     * Llama 2 format: [INST]<<SYS>>system<</SYS>>user[/INST]
     * 
     * Used by Meta Llama 2 models.
     */
    private fun formatLlama2(systemPrompt: String?, userPrompt: String): String {
        return if (systemPrompt != null) {
            "<s>[INST] <<SYS>>\n$systemPrompt\n<</SYS>>\n\n$userPrompt [/INST]"
        } else {
            "<s>[INST] $userPrompt [/INST]"
        }
    }
    
    /**
     * Llama 3 format: <|begin_of_text|><|start_header_id|>role<|end_header_id|>
     * 
     * Used by Meta Llama 3 and 3.1 models.
     */
    private fun formatLlama3(systemPrompt: String?, userPrompt: String): String {
        val sb = StringBuilder()
        sb.append("<|begin_of_text|>")
        if (systemPrompt != null) {
            sb.append("<|start_header_id|>system<|end_header_id|>\n\n$systemPrompt<|eot_id|>")
        }
        sb.append("<|start_header_id|>user<|end_header_id|>\n\n$userPrompt<|eot_id|>")
        sb.append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        return sb.toString()
    }
    
    /**
     * Gemma format: <start_of_turn>role\ncontent<end_of_turn>
     * 
     * Used by Google Gemma models.
     */
    private fun formatGemma(systemPrompt: String?, userPrompt: String): String {
        val sb = StringBuilder()
        if (systemPrompt != null) {
            // Gemma doesn't have official system prompt support, embed in user message
            sb.append("<start_of_turn>user\n$systemPrompt\n\n$userPrompt<end_of_turn>\n")
        } else {
            sb.append("<start_of_turn>user\n$userPrompt<end_of_turn>\n")
        }
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }
    
    /**
     * Get stop sequences for a given template.
     * These sequences signal the end of generation.
     */
    fun getStopSequences(template: PromptTemplate): List<String> {
        return when (template) {
            PromptTemplate.PHI3 -> listOf("<|end|>", "<|user|>")
            PromptTemplate.MISTRAL -> listOf("</s>", "[INST]")
            PromptTemplate.CHATML -> listOf("<|im_end|>", "<|im_start|>")
            PromptTemplate.LLAMA2 -> listOf("</s>", "[INST]")
            PromptTemplate.LLAMA3 -> listOf("<|eot_id|>", "<|start_header_id|>")
            PromptTemplate.GEMMA -> listOf("<end_of_turn>", "<start_of_turn>")
            PromptTemplate.RAW -> emptyList()
        }
    }
}

/**
 * Eisenhower classification prompt builder.
 * 
 * Builds optimized prompts for Eisenhower Matrix classification
 * based on benchmark findings from Milestone 0.2.
 */
object EisenhowerPromptBuilder {
    
    /**
     * System prompt for Eisenhower classification.
     * Optimized based on 0.2.3 accuracy testing.
     */
    private const val EISENHOWER_SYSTEM_PROMPT = """You are a productivity assistant that classifies tasks using the Eisenhower Matrix.

The Eisenhower Matrix has 4 quadrants:
- DO (Urgent + Important): Tasks with imminent deadlines that directly impact key goals. Do these first.
- SCHEDULE (Important, Not Urgent): Tasks that matter for long-term goals but have no immediate deadline. Schedule time for these.
- DELEGATE (Urgent, Not Important): Time-sensitive but routine tasks that don't require your expertise. Consider delegating.
- ELIMINATE (Not Urgent, Not Important): Low-value activities that waste time. Minimize or eliminate.

URGENCY signals: deadlines, "today", "ASAP", "urgent", time pressure, "by [date]", "due", "deadline"
IMPORTANCE signals: impacts goals, career, health, key relationships, client/customer, strategic value, learning

Respond with ONLY a JSON object in this exact format:
{"quadrant": "DO|SCHEDULE|DELEGATE|ELIMINATE", "confidence": 0.0-1.0, "reasoning": "brief explanation"}"""

    /**
     * Build a prompt for classifying a task into an Eisenhower quadrant.
     * 
     * @param taskText The task description to classify
     * @param template The prompt template for the target model
     * @return Formatted prompt ready for LLM inference
     */
    fun buildClassificationPrompt(
        taskText: String,
        template: PromptTemplate
    ): String {
        val userPrompt = "Classify this task into the Eisenhower Matrix:\n\"$taskText\""
        return PromptFormatter.format(template, EISENHOWER_SYSTEM_PROMPT, userPrompt)
    }
    
    /**
     * Build a simpler prompt for models that struggle with complex instructions.
     * Uses chain-of-thought reasoning.
     */
    fun buildSimpleClassificationPrompt(
        taskText: String,
        template: PromptTemplate
    ): String {
        val simpleSystemPrompt = """Classify tasks using Eisenhower Matrix. Think step by step:
1. Is it URGENT? (deadline soon, time-sensitive)
2. Is it IMPORTANT? (impacts goals, career, health, relationships)
Then classify: DO (both), SCHEDULE (important only), DELEGATE (urgent only), ELIMINATE (neither).
Output JSON only: {"quadrant": "X", "confidence": 0.X, "reasoning": "..."}"""
        
        val userPrompt = "Task: \"$taskText\"\nClassify:"
        return PromptFormatter.format(template, simpleSystemPrompt, userPrompt)
    }
}

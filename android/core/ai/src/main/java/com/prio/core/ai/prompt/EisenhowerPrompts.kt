package com.prio.core.ai.prompt

/**
 * Eisenhower Matrix classification prompts optimized through benchmark testing.
 * 
 * Task 2.2.10: Write Eisenhower classification prompts
 * 
 * Based on Milestone 0.2.6 research findings:
 * - EXPERT_PERSONA achieves 70% accuracy on Phi-3 (target met)
 * - Chain-of-thought works best with Mistral 7B (80% accuracy)
 * - Few-shot helps with DELEGATE quadrant detection
 * - Baseline achieves only 20-40% accuracy
 * 
 * Performance verified on:
 * - Device: Pixel 9a (Tensor G4, 8GB RAM)
 * - Model: Phi-3-mini-4k-instruct Q4_K_M
 * - Latency: 3-8 seconds per classification
 */
object EisenhowerPrompts {
    
    // ========================================================================
    // EXPERT_PERSONA Strategy (70% accuracy on Phi-3)
    // Recommended for production use
    // ========================================================================
    
    /**
     * Expert persona system prompt.
     * 
     * Key design decisions:
     * - Establishes authoritative productivity consultant persona
     * - Provides clear, concise quadrant definitions
     * - Requests single-word response to reduce parsing complexity
     * - Tested with Phi-3 template format
     * 
     * Benchmark Results (from 0.2.6):
     * - Overall: 70% accuracy
     * - DO: 100% (5/5)
     * - SCHEDULE: 80% (4/5)
     * - DELEGATE: 40% (2/5)
     * - ELIMINATE: 60% (3/5)
     */
    const val EXPERT_PERSONA_SYSTEM = """You are an Eisenhower Matrix expert. Classify tasks as:
- DO: urgent AND important (crisis, deadline today, critical issues)
- SCHEDULE: important but NOT urgent (planning, learning, relationships)
- DELEGATE: urgent but NOT important (routine requests, minor issues)
- ELIMINATE: NOT urgent and NOT important (time wasters, distractions)

Answer with one word only: DO, SCHEDULE, DELEGATE, or ELIMINATE."""

    /**
     * User prompt template for expert persona strategy.
     */
    const val CLASSIFICATION_USER_TEMPLATE = """Task: "{{TASK}}"

Classification:"""

    // ========================================================================
    // CHAIN_OF_THOUGHT Strategy (80% accuracy on Mistral 7B)
    // Slower but more accurate for complex tasks
    // ========================================================================
    
    /**
     * Chain-of-thought system prompt.
     * 
     * Asks the model to reason step-by-step before classification.
     * Works best with larger models (Mistral 7B, Gemma 7B).
     * 
     * Note: Too slow for real-time use (45-60s on Mistral)
     * Use for batch processing or edge case refinement.
     */
    const val CHAIN_OF_THOUGHT_SYSTEM = """You are a productivity expert. Classify tasks using the Eisenhower Matrix.

Think step by step:
1. Is there time pressure (deadline, waiting, urgent words)? → URGENT
2. Does it impact goals, career, health, or key relationships? → IMPORTANT
3. Apply the matrix:
   - Urgent + Important = DO
   - Important only = SCHEDULE  
   - Urgent only = DELEGATE
   - Neither = ELIMINATE

Provide brief reasoning then final classification."""

    const val CHAIN_OF_THOUGHT_USER_TEMPLATE = """Classify this task: "{{TASK}}"

Step 1 (Urgency):"""

    // ========================================================================
    // FEW_SHOT Strategy (45% accuracy on Phi-3)
    // Helps with pattern matching but prone to overfitting
    // ========================================================================
    
    /**
     * Few-shot prompt with example classifications.
     * 
     * Benchmark findings:
     * - Strong DELEGATE detection (80%)
     * - Prone to parsing issues with longer outputs
     * - Best for concrete, similar patterns
     */
    const val FEW_SHOT_SYSTEM = """Classify tasks into the Eisenhower Matrix."""
    
    const val FEW_SHOT_USER_TEMPLATE = """Examples:
"Server is down, customers affected" → DO
"Plan quarterly marketing strategy" → SCHEDULE
"Order office supplies for the team" → DELEGATE
"Browse social media during lunch" → ELIMINATE

Now classify: "{{TASK}}" →"""

    // ========================================================================
    // STRUCTURED_JSON Strategy
    // For applications requiring structured output
    // ========================================================================
    
    /**
     * JSON-structured output prompt.
     * 
     * Use when you need:
     * - Confidence scores
     * - Reasoning explanations
     * - Urgency/importance flags
     * 
     * Note: May have parsing issues with Phi-3 (20% success rate)
     * Works better with larger models.
     */
    const val STRUCTURED_JSON_SYSTEM = """You are a productivity assistant that classifies tasks using the Eisenhower Matrix.

The Eisenhower Matrix has 4 quadrants:
- DO (Urgent + Important): Tasks with imminent deadlines that directly impact key goals
- SCHEDULE (Important, Not Urgent): Tasks that matter for long-term goals but have no immediate deadline
- DELEGATE (Urgent, Not Important): Time-sensitive but routine tasks that don't require your expertise
- ELIMINATE (Not Urgent, Not Important): Low-value activities that waste time

URGENCY signals: deadlines, "today", "ASAP", "urgent", time pressure, "by [date]", "due", "deadline"
IMPORTANCE signals: impacts goals, career, health, key relationships, client/customer, strategic value

Respond with ONLY a JSON object in this exact format:
{"quadrant": "DO|SCHEDULE|DELEGATE|ELIMINATE", "confidence": 0.0-1.0, "reasoning": "brief explanation"}"""

    const val STRUCTURED_JSON_USER_TEMPLATE = """Classify this task:
"{{TASK}}"

JSON:"""

    // ========================================================================
    // BASELINE Strategy (20-40% accuracy)
    // Minimal prompt for comparison/fallback
    // ========================================================================
    
    const val BASELINE_SYSTEM = """Classify tasks as DO, SCHEDULE, DELEGATE, or ELIMINATE."""
    
    const val BASELINE_USER_TEMPLATE = """Task: {{TASK}}
Classification:"""

    // ========================================================================
    // OPTIMIZED Strategy for Phi-3 (combining best practices)
    // ========================================================================
    
    /**
     * Optimized prompt combining expert persona with structured hints.
     * 
     * Design decisions:
     * - Short, focused system prompt (Phi-3 works better with concise prompts)
     * - Clear urgency/importance keywords
     * - Single-word response requirement
     * - Phi-3 template format: <|user|>...<|end|><|assistant|>
     */
    const val OPTIMIZED_PHI3_SYSTEM = """Eisenhower Matrix classifier.

DO = urgent crisis/deadline + important goal impact
SCHEDULE = important for goals but no deadline
DELEGATE = urgent routine task, not strategic
ELIMINATE = not urgent, not important, time waster

Keywords for urgency: deadline, today, ASAP, urgent, waiting, overdue
Keywords for importance: goal, career, health, client, strategic, learn

One word answer only."""

    const val OPTIMIZED_PHI3_USER_TEMPLATE = """{{TASK}}

Answer:"""

    // ========================================================================
    // Model-Specific Full Prompts (formatted for each template)
    // ========================================================================
    
    /**
     * Build complete prompt for Phi-3 with correct template.
     */
    fun buildPhi3Prompt(task: String, useExpertPersona: Boolean = true): String {
        val systemPrompt = if (useExpertPersona) EXPERT_PERSONA_SYSTEM else OPTIMIZED_PHI3_SYSTEM
        val userPrompt = if (useExpertPersona) {
            CLASSIFICATION_USER_TEMPLATE.replace("{{TASK}}", task)
        } else {
            OPTIMIZED_PHI3_USER_TEMPLATE.replace("{{TASK}}", task)
        }
        return "<|user|>\n$systemPrompt\n\n$userPrompt<|end|>\n<|assistant|>\n"
    }
    
    /**
     * Build complete prompt for Mistral with correct template.
     */
    fun buildMistralPrompt(task: String, useChainOfThought: Boolean = true): String {
        val systemPrompt = if (useChainOfThought) CHAIN_OF_THOUGHT_SYSTEM else EXPERT_PERSONA_SYSTEM
        val userPrompt = if (useChainOfThought) {
            CHAIN_OF_THOUGHT_USER_TEMPLATE.replace("{{TASK}}", task)
        } else {
            CLASSIFICATION_USER_TEMPLATE.replace("{{TASK}}", task)
        }
        return "<s>[INST] $systemPrompt\n\n$userPrompt [/INST]"
    }
    
    /**
     * Build complete prompt for Gemma with correct template.
     */
    fun buildGemmaPrompt(task: String): String {
        val content = "$EXPERT_PERSONA_SYSTEM\n\n${CLASSIFICATION_USER_TEMPLATE.replace("{{TASK}}", task)}"
        return "<start_of_turn>user\n$content<end_of_turn>\n<start_of_turn>model\n"
    }
    
    // ========================================================================
    // Quadrant Parsing Utilities
    // ========================================================================
    
    /**
     * Parse quadrant from model output.
     * Handles various output formats and noise.
     */
    fun parseQuadrant(output: String): String? {
        val normalized = output.uppercase().trim()
        
        // Direct match
        if (normalized.startsWith("DO") || normalized == "DO") return "DO"
        if (normalized.startsWith("SCHEDULE")) return "SCHEDULE"
        if (normalized.startsWith("DELEGATE")) return "DELEGATE"
        if (normalized.startsWith("ELIMINATE")) return "ELIMINATE"
        
        // Search for quadrant name in longer output
        return when {
            normalized.contains("DO NOW") || 
            normalized.contains("QUADRANT 1") ||
            normalized.contains("Q1") ||
            (normalized.contains("DO") && !normalized.contains("SCHEDULE")) -> "DO"
            
            normalized.contains("SCHEDULE") || 
            normalized.contains("QUADRANT 2") ||
            normalized.contains("Q2") -> "SCHEDULE"
            
            normalized.contains("DELEGATE") || 
            normalized.contains("QUADRANT 3") ||
            normalized.contains("Q3") -> "DELEGATE"
            
            normalized.contains("ELIMINATE") || 
            normalized.contains("QUADRANT 4") ||
            normalized.contains("Q4") ||
            normalized.contains("DROP") ||
            normalized.contains("DELETE") -> "ELIMINATE"
            
            else -> null
        }
    }
    
    /**
     * Get stop sequences for Eisenhower classification.
     * Helps the model stop after providing the classification.
     */
    fun getStopSequences(): List<String> = listOf(
        "\n",
        ".",
        "<|end|>",
        "<|user|>",
        "</s>",
        "[INST]",
        "<end_of_turn>",
        "Task:",
        "Classify"
    )
}

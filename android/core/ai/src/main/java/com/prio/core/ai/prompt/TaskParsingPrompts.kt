package com.prio.core.ai.prompt

/**
 * Task parsing prompts for natural language input.
 * 
 * Task 2.2.11: Write task parsing prompts
 * 
 * Based on TM-002 User Story requirements:
 * - Parse task title from natural language input
 * - Extract due date/time (e.g., "tomorrow", "next Friday", "3pm")
 * - Detect priority signals (e.g., "urgent", "important", "ASAP")
 * - Identify project/area context if mentioned
 * 
 * Examples from spec:
 * - "Call mom tomorrow at 5pm" → Title: "Call mom", Due: Tomorrow 5:00 PM
 * - "urgent: finish report by Friday" → Title: "Finish report", Due: Friday, Priority: High
 * - "dentist appointment next Tuesday morning" → Title: "Dentist appointment", Due: Next Tuesday 9:00 AM
 */
object TaskParsingPrompts {
    
    // ========================================================================
    // Main Parsing Prompt (Structured JSON Output)
    // ========================================================================
    
    /**
     * System prompt for task parsing.
     * 
     * Extracts:
     * - title: The main task description (cleaned up)
     * - due_date: Date in YYYY-MM-DD format or relative ("tomorrow", "next_friday")
     * - due_time: Time in HH:mm format (24-hour)
     * - priority: high, medium, low, or null
     * - project: Project/area context if mentioned
     * - recurrence: Repeat pattern if mentioned
     */
    const val SYSTEM_PROMPT = """You are a task parser. Extract structured information from natural language task descriptions.

Output JSON with these fields:
- title: Main task description (required, cleaned up)
- due_date: Date string or null (formats: YYYY-MM-DD, today, tomorrow, next_monday, etc.)
- due_time: Time in HH:mm format or null (use 24-hour format)
- priority: "high", "medium", "low", or null
- project: Project/area context or null (e.g., "work", "home", "health")
- recurrence: Repeat pattern or null (e.g., "daily", "weekly", "monthly")

Priority signals:
- high: "urgent", "ASAP", "important", "critical", "!!", "priority"
- medium: "soon", "this week", implicit deadlines
- low: "someday", "maybe", "when I have time"

Relative dates (based on current date):
- today, tonight → today's date
- tomorrow → next day
- this/next monday-sunday → calculate from today
- next week → 7 days from today
- end of month → last day of current month

Time keywords:
- morning → 09:00
- noon/lunch → 12:00
- afternoon → 14:00
- evening → 18:00
- night → 20:00
- EOD/end of day → 17:00

Respond with ONLY valid JSON, no explanation."""

    /**
     * User prompt template for task parsing.
     */
    const val USER_TEMPLATE = """Parse this task: "{{TASK}}"

Current date: {{CURRENT_DATE}}
Current day: {{CURRENT_DAY}}

JSON:"""

    // ========================================================================
    // Simplified Parsing Prompt (for faster inference)
    // ========================================================================
    
    /**
     * Simplified system prompt for quick parsing.
     * Focuses on essential fields only.
     */
    const val SIMPLE_SYSTEM_PROMPT = """Parse task to JSON: {"title": "...", "due": "...", "priority": "high|medium|low|null"}

Rules:
- title: Clean task name without dates/priority words
- due: ISO date/time or relative (today, tomorrow, next_friday, etc.)
- priority: high if urgent/ASAP/important, else null"""

    const val SIMPLE_USER_TEMPLATE = """{{TASK}}
Today: {{CURRENT_DATE}}

JSON:"""

    // ========================================================================
    // Few-Shot Parsing Prompt (with examples)
    // ========================================================================
    
    /**
     * Few-shot prompt with example parsings.
     * Helps model understand expected format through examples.
     */
    const val FEW_SHOT_SYSTEM_PROMPT = """Parse task descriptions into JSON format."""
    
    const val FEW_SHOT_USER_TEMPLATE = """Examples:
"Call mom tomorrow at 5pm" → {"title": "Call mom", "due_date": "tomorrow", "due_time": "17:00", "priority": null}
"urgent: finish report by Friday" → {"title": "Finish report", "due_date": "next_friday", "due_time": null, "priority": "high"}
"buy groceries" → {"title": "Buy groceries", "due_date": null, "due_time": null, "priority": null}
"dentist next Tuesday morning" → {"title": "Dentist", "due_date": "next_tuesday", "due_time": "09:00", "priority": null}
"!! submit taxes by April 15" → {"title": "Submit taxes", "due_date": "2026-04-15", "due_time": null, "priority": "high"}

Now parse: "{{TASK}}" →"""

    // ========================================================================
    // Model-Specific Formatted Prompts
    // ========================================================================
    
    /**
     * Build complete prompt for Phi-3.
     */
    fun buildPhi3Prompt(task: String, currentDate: String, currentDay: String): String {
        val userPrompt = USER_TEMPLATE
            .replace("{{TASK}}", task)
            .replace("{{CURRENT_DATE}}", currentDate)
            .replace("{{CURRENT_DAY}}", currentDay)
        return "<|user|>\n$SYSTEM_PROMPT\n\n$userPrompt<|end|>\n<|assistant|>\n"
    }
    
    /**
     * Build simplified prompt for Phi-3 (faster inference).
     */
    fun buildPhi3SimplePrompt(task: String, currentDate: String): String {
        val userPrompt = SIMPLE_USER_TEMPLATE
            .replace("{{TASK}}", task)
            .replace("{{CURRENT_DATE}}", currentDate)
        return "<|user|>\n$SIMPLE_SYSTEM_PROMPT\n\n$userPrompt<|end|>\n<|assistant|>\n"
    }
    
    /**
     * Build few-shot prompt for Phi-3.
     */
    fun buildPhi3FewShotPrompt(task: String): String {
        val userPrompt = FEW_SHOT_USER_TEMPLATE.replace("{{TASK}}", task)
        return "<|user|>\n$FEW_SHOT_SYSTEM_PROMPT\n\n$userPrompt<|end|>\n<|assistant|>\n"
    }
    
    /**
     * Build complete prompt for Mistral.
     */
    fun buildMistralPrompt(task: String, currentDate: String, currentDay: String): String {
        val userPrompt = USER_TEMPLATE
            .replace("{{TASK}}", task)
            .replace("{{CURRENT_DATE}}", currentDate)
            .replace("{{CURRENT_DAY}}", currentDay)
        return "<s>[INST] $SYSTEM_PROMPT\n\n$userPrompt [/INST]"
    }
    
    // ========================================================================
    // Response Parsing Utilities
    // ========================================================================
    
    /**
     * Regex patterns for fallback parsing when LLM fails.
     * Used as rule-based backup per TM-002 acceptance criteria.
     */
    object FallbackPatterns {
        // Date patterns
        val TODAY = Regex("(?i)\\b(today|tonight)\\b")
        val TOMORROW = Regex("(?i)\\btomorrow\\b")
        val NEXT_WEEK = Regex("(?i)\\bnext\\s+week\\b")
        val DAY_OF_WEEK = Regex("(?i)\\b(this|next)?\\s*(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b")
        val SPECIFIC_DATE = Regex("\\b(\\d{1,2})[/\\-](\\d{1,2})(?:[/\\-](\\d{2,4}))?\\b")
        val MONTH_DAY = Regex("(?i)\\b(january|february|march|april|may|june|july|august|september|october|november|december)\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b")
        
        // Time patterns
        val TIME_12H = Regex("(?i)\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b")
        val TIME_24H = Regex("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b")
        val TIME_KEYWORD = Regex("(?i)\\b(morning|noon|afternoon|evening|night|EOD|end of day)\\b")
        
        // Priority patterns
        val HIGH_PRIORITY = Regex("(?i)\\b(urgent|ASAP|important|critical|!!|priority|high priority)\\b")
        val LOW_PRIORITY = Regex("(?i)\\b(someday|maybe|when I have time|low priority)\\b")
        
        // Project/context patterns
        val PROJECT = Regex("(?i)\\b(for|at|@)\\s+(work|home|office|school|gym|health|family|personal)\\b")
        
        /**
         * Extract time from keyword.
         */
        fun timeFromKeyword(keyword: String): String? {
            return when (keyword.lowercase()) {
                "morning" -> "09:00"
                "noon", "lunch" -> "12:00"
                "afternoon" -> "14:00"
                "evening" -> "18:00"
                "night" -> "20:00"
                "eod", "end of day" -> "17:00"
                else -> null
            }
        }
        
        /**
         * Parse 12-hour time to 24-hour format.
         */
        fun parse12HourTime(hour: String, minute: String?, ampm: String): String {
            var h = hour.toIntOrNull() ?: return "12:00"
            val m = minute?.toIntOrNull() ?: 0
            
            if (ampm.lowercase() == "pm" && h != 12) h += 12
            if (ampm.lowercase() == "am" && h == 12) h = 0
            
            return "%02d:%02d".format(h, m)
        }
    }
    
    /**
     * Get stop sequences for task parsing.
     */
    fun getStopSequences(): List<String> = listOf(
        "\n\n",
        "Parse",
        "Example",
        "<|end|>",
        "</s>",
        "[INST]"
    )
}

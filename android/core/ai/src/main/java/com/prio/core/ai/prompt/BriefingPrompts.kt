package com.prio.core.ai.prompt

/**
 * Briefing generation prompts for daily/weekly summaries.
 * 
 * Task 2.2.10/2.2.11: Complete prompt templates
 * 
 * Based on PRODUCT_BRIEF.md Daily Summaries requirements:
 * - Morning briefing: today's schedule, tasks, priorities
 * - Evening recap: accomplishments, carry-overs
 * - AI-generated insights and suggestions
 * 
 * From ACTION_PLAN.md Strategic Context:
 * - Daily AI briefings have highest retention driver score (2.18)
 * - Key habit formation engine for user engagement
 */
object BriefingPrompts {
    
    // ========================================================================
    // Morning Briefing Prompt
    // ========================================================================
    
    /**
     * System prompt for morning briefing generation.
     * 
     * Generates a concise, actionable morning summary including:
     * - Today's focus (top 3 priorities)
     * - Schedule highlights
     * - Goal progress check-ins
     * - One motivational insight
     */
    const val SYSTEM_PROMPT = """You are a supportive productivity assistant. Generate a concise morning briefing.

Format your response as:
1. TOP FOCUS (3 items max)
   - Most important/urgent task
   - Second priority
   - Third priority

2. TODAY'S SCHEDULE (if any meetings)
   - Key meetings with times

3. GOAL CHECK-IN (1 goal in progress)
   - Quick progress note

4. INSIGHT (1 sentence)
   - Motivational or productivity tip

Keep it brief (under 150 words). Be encouraging but not cheesy.
Use bullet points and clear structure."""

    /**
     * User prompt template for morning briefing.
     * Placeholders filled with user's actual data.
     */
    const val USER_TEMPLATE = """Generate my morning briefing for {{CURRENT_DATE}} ({{CURRENT_DAY}}).

My tasks for today:
{{TASKS_LIST}}

My meetings today:
{{MEETINGS_LIST}}

My active goals:
{{GOALS_LIST}}

Tasks completed yesterday:
{{YESTERDAY_COMPLETED}}

Briefing:"""

    // ========================================================================
    // Evening Recap Prompt
    // ========================================================================
    
    /**
     * System prompt for evening recap.
     */
    const val EVENING_SYSTEM_PROMPT = """You are a supportive productivity assistant. Generate a concise evening recap.

Format your response as:
1. ACCOMPLISHED TODAY
   - Key wins (2-3 items)

2. CARRY FORWARD
   - Important incomplete tasks

3. TOMORROW'S FOCUS
   - Top priority for tomorrow

4. REFLECTION (1 sentence)
   - Brief encouraging note on progress

Keep it brief (under 100 words). Be positive about progress made."""

    const val EVENING_USER_TEMPLATE = """Generate my evening recap for {{CURRENT_DATE}}.

Tasks completed today:
{{COMPLETED_LIST}}

Tasks not completed:
{{INCOMPLETE_LIST}}

Tomorrow's calendar:
{{TOMORROW_MEETINGS}}

Recap:"""

    // ========================================================================
    // Weekly Summary Prompt
    // ========================================================================
    
    /**
     * System prompt for weekly summary.
     */
    const val WEEKLY_SYSTEM_PROMPT = """You are a productivity analyst. Generate a weekly productivity summary.

Format your response as:
1. WEEK HIGHLIGHTS
   - Key accomplishments (3-5 items)

2. BY THE NUMBERS
   - Tasks completed: X
   - Goals progressed: Y
   - Time blocked: Z hours

3. PATTERNS NOTICED
   - Most productive day
   - Most completed quadrant
   - Any concerning trends

4. NEXT WEEK FOCUS
   - Top 2-3 priorities

Keep analytical but encouraging. Under 200 words."""

    const val WEEKLY_USER_TEMPLATE = """Generate my weekly summary for {{WEEK_START}} to {{WEEK_END}}.

Tasks completed this week:
{{COMPLETED_TASKS}}

Goal progress:
{{GOAL_PROGRESS}}

Tasks by quadrant:
- DO: {{DO_COUNT}} completed
- SCHEDULE: {{SCHEDULE_COUNT}} completed
- DELEGATE: {{DELEGATE_COUNT}} completed
- ELIMINATE: {{ELIMINATE_COUNT}} dropped

Daily breakdown:
{{DAILY_STATS}}

Summary:"""

    // ========================================================================
    // Quick Insight Prompt (for widget/notification)
    // ========================================================================
    
    /**
     * Short prompt for quick productivity insight.
     * Used for widgets, notifications, quick glances.
     */
    const val QUICK_INSIGHT_SYSTEM = """Generate a single-sentence productivity insight based on the user's data. Be specific and actionable."""
    
    const val QUICK_INSIGHT_USER = """Tasks pending: {{PENDING_COUNT}}
Overdue: {{OVERDUE_COUNT}}
Completed today: {{COMPLETED_TODAY}}
Top priority: {{TOP_TASK}}

One-sentence insight:"""

    // ========================================================================
    // Model-Specific Formatted Prompts
    // ========================================================================
    
    /**
     * Build morning briefing prompt for Phi-3.
     */
    fun buildPhi3MorningBriefing(
        currentDate: String,
        currentDay: String,
        tasksList: String,
        meetingsList: String,
        goalsList: String,
        yesterdayCompleted: String
    ): String {
        val userPrompt = USER_TEMPLATE
            .replace("{{CURRENT_DATE}}", currentDate)
            .replace("{{CURRENT_DAY}}", currentDay)
            .replace("{{TASKS_LIST}}", tasksList.ifEmpty { "No tasks scheduled" })
            .replace("{{MEETINGS_LIST}}", meetingsList.ifEmpty { "No meetings" })
            .replace("{{GOALS_LIST}}", goalsList.ifEmpty { "No active goals" })
            .replace("{{YESTERDAY_COMPLETED}}", yesterdayCompleted.ifEmpty { "None tracked" })
        
        return "<|user|>\n$SYSTEM_PROMPT\n\n$userPrompt<|end|>\n<|assistant|>\n"
    }
    
    /**
     * Build evening recap prompt for Phi-3.
     */
    fun buildPhi3EveningRecap(
        currentDate: String,
        completedList: String,
        incompleteList: String,
        tomorrowMeetings: String
    ): String {
        val userPrompt = EVENING_USER_TEMPLATE
            .replace("{{CURRENT_DATE}}", currentDate)
            .replace("{{COMPLETED_LIST}}", completedList.ifEmpty { "No tasks completed" })
            .replace("{{INCOMPLETE_LIST}}", incompleteList.ifEmpty { "All tasks completed!" })
            .replace("{{TOMORROW_MEETINGS}}", tomorrowMeetings.ifEmpty { "No meetings scheduled" })
        
        return "<|user|>\n$EVENING_SYSTEM_PROMPT\n\n$userPrompt<|end|>\n<|assistant|>\n"
    }
    
    /**
     * Build quick insight prompt for Phi-3.
     */
    fun buildPhi3QuickInsight(
        pendingCount: Int,
        overdueCount: Int,
        completedToday: Int,
        topTask: String?
    ): String {
        val userPrompt = QUICK_INSIGHT_USER
            .replace("{{PENDING_COUNT}}", pendingCount.toString())
            .replace("{{OVERDUE_COUNT}}", overdueCount.toString())
            .replace("{{COMPLETED_TODAY}}", completedToday.toString())
            .replace("{{TOP_TASK}}", topTask ?: "None")
        
        return "<|user|>\n$QUICK_INSIGHT_SYSTEM\n\n$userPrompt<|end|>\n<|assistant|>\n"
    }
    
    /**
     * Build morning briefing prompt for Mistral.
     */
    fun buildMistralMorningBriefing(
        currentDate: String,
        currentDay: String,
        tasksList: String,
        meetingsList: String,
        goalsList: String,
        yesterdayCompleted: String
    ): String {
        val userPrompt = USER_TEMPLATE
            .replace("{{CURRENT_DATE}}", currentDate)
            .replace("{{CURRENT_DAY}}", currentDay)
            .replace("{{TASKS_LIST}}", tasksList.ifEmpty { "No tasks scheduled" })
            .replace("{{MEETINGS_LIST}}", meetingsList.ifEmpty { "No meetings" })
            .replace("{{GOALS_LIST}}", goalsList.ifEmpty { "No active goals" })
            .replace("{{YESTERDAY_COMPLETED}}", yesterdayCompleted.ifEmpty { "None tracked" })
        
        return "<s>[INST] $SYSTEM_PROMPT\n\n$userPrompt [/INST]"
    }
    
    // ========================================================================
    // Helper Functions
    // ========================================================================
    
    /**
     * Format task list for briefing prompt.
     */
    fun formatTaskList(tasks: List<TaskSummary>): String {
        if (tasks.isEmpty()) return "No tasks"
        return tasks.take(10).joinToString("\n") { task ->
            val quadrantEmoji = when (task.quadrant) {
                "DO" -> "🔴"
                "SCHEDULE" -> "🟡"
                "DELEGATE" -> "🟠"
                else -> "⚪"
            }
            val dueStr = task.dueTime?.let { " (due $it)" } ?: ""
            "$quadrantEmoji ${task.title}$dueStr"
        }
    }
    
    /**
     * Format meeting list for briefing prompt.
     */
    fun formatMeetingList(meetings: List<MeetingSummary>): String {
        if (meetings.isEmpty()) return "No meetings"
        return meetings.take(5).joinToString("\n") { meeting ->
            "📅 ${meeting.startTime}: ${meeting.title}"
        }
    }
    
    /**
     * Format goal list for briefing prompt.
     */
    fun formatGoalList(goals: List<GoalSummary>): String {
        if (goals.isEmpty()) return "No active goals"
        return goals.take(3).joinToString("\n") { goal ->
            "🎯 ${goal.title} (${goal.progressPercent}% complete)"
        }
    }
    
    /**
     * Get stop sequences for briefing generation.
     */
    fun getStopSequences(): List<String> = listOf(
        "\n\n\n",
        "<|end|>",
        "</s>",
        "[INST]",
        "Generate",
        "User:"
    )
}

/**
 * Summary data classes for briefing generation.
 */
data class TaskSummary(
    val title: String,
    val quadrant: String,
    val dueTime: String? = null
)

data class MeetingSummary(
    val title: String,
    val startTime: String
)

data class GoalSummary(
    val title: String,
    val progressPercent: Int
)

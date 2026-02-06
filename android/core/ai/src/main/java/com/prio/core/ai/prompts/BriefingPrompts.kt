package com.prio.core.ai.prompts

/**
 * Prompt templates for daily briefing AI insight generation.
 *
 * Used by BriefingGenerator (task 3.4.1) to produce personalized
 * morning and evening insights via on-device LLM or rule-based fallback.
 *
 * Per CB-001/CB-003 user stories and 1.1.5/1.1.7 UX specs.
 *
 * Prompt design principles:
 * - Concise: Single insight per briefing, ≤2 sentences
 * - Actionable: Suggest specific next step
 * - Encouraging: Positive tone even on low-completion days
 * - Context-aware: Use task, calendar, goal data
 */
object BriefingPrompts {

    // ==================== Morning Briefing ====================

    /**
     * System prompt for morning insight generation.
     */
    const val MORNING_SYSTEM_PROMPT = """You are Prio, a private productivity assistant that runs entirely on the user's device. Generate a single, concise morning insight (1-2 sentences) based on today's context. Be helpful, specific, and encouraging. Never be judgmental. Do not use markdown. Do not include greetings."""

    /**
     * Build the morning insight user prompt with context placeholders filled.
     *
     * @param urgentTaskCount Number of Q1 (Do First) tasks today
     * @param importantTaskCount Number of Q2 (Schedule) tasks today
     * @param meetingCount Number of calendar events today
     * @param meetingHours Total hours of meetings today
     * @param focusHoursAvailable Estimated free hours for deep work
     * @param overdueCount Number of overdue tasks
     * @param topGoalTitle Title of the spotlight goal (null if none)
     * @param topGoalProgress Progress percentage of spotlight goal (0-100)
     * @param dayOfWeek Day of week name (e.g., "Monday")
     * @param yesterdayCompleted Tasks completed yesterday
     */
    fun buildMorningInsightPrompt(
        urgentTaskCount: Int,
        importantTaskCount: Int,
        meetingCount: Int,
        meetingHours: Float,
        focusHoursAvailable: Float,
        overdueCount: Int,
        topGoalTitle: String?,
        topGoalProgress: Int?,
        dayOfWeek: String,
        yesterdayCompleted: Int
    ): String = buildString {
        appendLine("Today is $dayOfWeek.")
        appendLine("Tasks: $urgentTaskCount urgent (Do First), $importantTaskCount important (Schedule).")
        if (overdueCount > 0) appendLine("Overdue: $overdueCount tasks.")
        appendLine("Calendar: $meetingCount meetings (${"%.1f".format(meetingHours)} hours).")
        appendLine("Available focus time: ${"%.1f".format(focusHoursAvailable)} hours.")
        if (topGoalTitle != null && topGoalProgress != null) {
            appendLine("Spotlight goal: \"$topGoalTitle\" at ${topGoalProgress}% progress.")
        }
        if (yesterdayCompleted > 0) {
            appendLine("Yesterday you completed $yesterdayCompleted tasks.")
        }
        appendLine()
        append("Generate one actionable insight for today.")
    }

    // ==================== Evening Summary ====================

    /**
     * System prompt for evening reflection generation.
     */
    const val EVENING_SYSTEM_PROMPT = """You are Prio, a private productivity assistant. Generate a single, concise evening reflection (2-3 sentences) based on today's results. Be encouraging and positive. Acknowledge specific accomplishments. Never shame the user for incomplete tasks. Do not use markdown. Do not include greetings."""

    /**
     * Build the evening reflection prompt.
     *
     * @param tasksCompleted Number of tasks completed today
     * @param totalTasks Total tasks that were due/active today
     * @param completedTitles Top completed task titles (max 3)
     * @param notDoneCount Tasks not completed
     * @param goalProgressDelta Goal progress change today (e.g., "+2%")
     * @param spotlightGoalTitle Goal that made progress (null if none)
     * @param meetingCount Meetings attended today
     * @param dayOfWeek Day of week name
     */
    fun buildEveningReflectionPrompt(
        tasksCompleted: Int,
        totalTasks: Int,
        completedTitles: List<String>,
        notDoneCount: Int,
        goalProgressDelta: String?,
        spotlightGoalTitle: String?,
        meetingCount: Int,
        dayOfWeek: String
    ): String = buildString {
        appendLine("Today is $dayOfWeek.")
        appendLine("Completed: $tasksCompleted of $totalTasks tasks.")
        if (completedTitles.isNotEmpty()) {
            appendLine("Key completions: ${completedTitles.joinToString(", ")}.")
        }
        if (notDoneCount > 0) {
            appendLine("$notDoneCount tasks not done (will carry over).")
        }
        if (spotlightGoalTitle != null && goalProgressDelta != null) {
            appendLine("Goal \"$spotlightGoalTitle\" progress: $goalProgressDelta today.")
        }
        if (meetingCount > 0) {
            appendLine("Attended $meetingCount meetings.")
        }
        if (dayOfWeek == "Friday") {
            appendLine("It's Friday—weekend ahead.")
        }
        appendLine()
        append("Generate a brief encouraging reflection.")
    }

    // ==================== Rule-Based Fallback Insights ====================

    /**
     * Rule-based morning insights when LLM is unavailable.
     * Returns the best-matching insight from a curated list.
     */
    fun getRuleBasedMorningInsight(
        urgentTaskCount: Int,
        meetingCount: Int,
        meetingHours: Float,
        focusHoursAvailable: Float,
        overdueCount: Int,
        topGoalTitle: String?,
        topGoalProgress: Int?,
        dayOfWeek: String,
        yesterdayCompleted: Int
    ): String = when {
        overdueCount >= 5 ->
            "You have $overdueCount overdue tasks. Consider rescheduling or breaking them into smaller steps."
        overdueCount >= 3 ->
            "You have $overdueCount overdue tasks—review and reschedule what you can."
        meetingHours >= 6 ->
            "Meeting-heavy day (${"%.0f".format(meetingHours)}h). Protect any gaps for your top priority."
        meetingHours >= 4 ->
            "Busy calendar today. Block 30 minutes between meetings for notes and next actions."
        urgentTaskCount == 0 && meetingCount == 0 ->
            "Clear schedule today! Great time to work on your goals and important tasks."
        urgentTaskCount == 0 ->
            "No urgent tasks today! Focus on important work that moves your goals forward."
        focusHoursAvailable >= 4 ->
            "You have ${"%.0f".format(focusHoursAvailable)} hours of focus time. Tackle your biggest task first."
        focusHoursAvailable < 1 ->
            "Very limited focus time today. Prioritize only your #1 task between meetings."
        topGoalTitle != null && topGoalProgress != null && topGoalProgress < 30 ->
            "Your \"$topGoalTitle\" goal is at ${topGoalProgress}%. Even one small action helps."
        yesterdayCompleted >= 8 ->
            "You completed $yesterdayCompleted tasks yesterday! Keep the momentum going."
        dayOfWeek == "Monday" ->
            "Fresh week! Start strong by focusing on your top 3 priorities."
        dayOfWeek == "Friday" ->
            "Wrap up open items before the weekend. Review your week's progress."
        else ->
            "You have $urgentTaskCount urgent tasks. Start with the most important one."
    }

    /**
     * Rule-based evening reflections when LLM is unavailable.
     */
    fun getRuleBasedEveningReflection(
        tasksCompleted: Int,
        totalTasks: Int,
        notDoneCount: Int,
        goalProgressDelta: String?,
        spotlightGoalTitle: String?,
        dayOfWeek: String
    ): String {
        val completionPct = if (totalTasks > 0) (tasksCompleted * 100 / totalTasks) else 0
        return when {
            completionPct >= 90 ->
                "Amazing day! You completed $tasksCompleted tasks. Well earned rest tonight."
            completionPct >= 70 -> {
                val goalPart = if (spotlightGoalTitle != null && goalProgressDelta != null) {
                    " Your \"$spotlightGoalTitle\" goal moved $goalProgressDelta."
                } else ""
                "Great progress! $tasksCompleted of $totalTasks tasks done.$goalPart"
            }
            completionPct >= 50 ->
                "Good work today! You completed $tasksCompleted tasks. The rest will be there tomorrow."
            completionPct >= 30 ->
                "Busy day? You still made progress on $tasksCompleted tasks. Tomorrow is a fresh start!"
            totalTasks == 0 ->
                "Quiet day today. Sometimes the best productivity is rest and reflection."
            tasksCompleted == 0 ->
                "Every day is different. Tomorrow is a fresh chance to tackle your priorities!"
            else ->
                "You completed $tasksCompleted tasks today. Every task counts toward your goals!"
        }
    }
}

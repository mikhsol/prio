package com.prio.app.feature.briefing

import com.prio.core.ai.model.AiContext
import com.prio.core.ai.model.AiRequest
import com.prio.core.ai.model.AiRequestOptions
import com.prio.core.ai.model.AiRequestType
import com.prio.core.ai.model.AiResult
import com.prio.core.ai.prompts.BriefingPrompts
import com.prio.core.ai.provider.AiProvider
import com.prio.core.aiprovider.di.MainAiProvider
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.data.local.entity.GoalEntity
import com.prio.core.data.local.entity.MeetingEntity
import com.prio.core.data.local.entity.TaskEntity
import com.prio.core.data.repository.AnalyticsRepository
import com.prio.core.data.repository.GoalRepository
import com.prio.core.data.repository.MeetingRepository
import com.prio.core.data.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core briefing generation engine for task 3.4.1.
 *
 * Generates morning and evening briefings by aggregating data from:
 * - TaskRepository (Q1/Q2 tasks, overdue, completion)
 * - GoalRepository (progress, at-risk detection)
 * - MeetingRepository (today's schedule)
 * - AnalyticsRepository (yesterday's stats)
 *
 * Uses hybrid approach per ACTION_PLAN.md:
 * - Rule-based primary for insight text (fast, <50ms)
 * - LLM enhancement for insight text when available (<3s)
 *
 * Performance target: Generate briefing in <3 seconds total.
 *
 * @see BriefingPrompts for AI prompt templates
 * @see MorningBriefingData for morning briefing output
 * @see EveningSummaryData for evening summary output
 */
@Singleton
class BriefingGenerator @Inject constructor(
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository,
    private val meetingRepository: MeetingRepository,
    private val analyticsRepository: AnalyticsRepository,
    @MainAiProvider private val aiProvider: AiProvider,
    private val clock: Clock
) {

    companion object {
        private const val TAG = "BriefingGenerator"
        private const val MAX_TOP_PRIORITIES = 3
        private const val MAX_COMPLETED_DISPLAY = 7
        private const val WORK_HOURS_START = 8
        private const val WORK_HOURS_END = 18
        private const val MINUTES_PER_HOUR = 60f
    }

    /**
     * Generate morning briefing data.
     *
     * Per 1.1.5 spec:
     * - Greeting, Top Priorities, Schedule Preview, Goal Spotlight, AI Insight
     * - Generates in <3 seconds (spec timing target)
     *
     * @param userName User's display name for greeting
     * @return Complete morning briefing data
     */
    suspend fun generateMorningBriefing(userName: String? = null): MorningBriefingData =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            val timeZone = TimeZone.currentSystemDefault()
            val now = clock.now()
            val today = now.toLocalDateTime(timeZone).date
            val todayStart = today.atStartOfDayIn(timeZone)
            val tomorrowStart = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
            val yesterday = today.plus(-1, DateTimeUnit.DAY)
            val yesterdayStart = yesterday.atStartOfDayIn(timeZone)

            // Parallel data fetching for speed (using sync/suspend methods)
            val tasksDeferred = async { taskRepository.getAllActiveTasksSync() }
            val meetingsDeferred = async { meetingRepository.getMeetingsForDateSync(todayStart, tomorrowStart) }
            val goalsDeferred = async { goalRepository.getAllActiveGoalsSync() }
            val yesterdayCompletedDeferred = async {
                taskRepository.getTasksCompletedOnDate(yesterdayStart.toEpochMilliseconds())
            }

            val allTasks = tasksDeferred.await()
            val meetings = meetingsDeferred.await()
            val activeGoals = goalsDeferred.await()
            val yesterdayCompleted = yesterdayCompletedDeferred.await()

            // Categorize tasks
            val overdueTasks = allTasks.filter { task ->
                !task.isCompleted && task.dueDate != null && task.dueDate!! < todayStart
            }
            val todayTasks = allTasks.filter { task ->
                !task.isCompleted && task.dueDate != null &&
                    task.dueDate!! >= todayStart && task.dueDate!! < tomorrowStart
            }
            val urgentTasks = allTasks.filter { task ->
                !task.isCompleted && task.quadrant == EisenhowerQuadrant.DO_FIRST
            }
            val importantTasks = allTasks.filter { task ->
                !task.isCompleted && task.quadrant == EisenhowerQuadrant.SCHEDULE
            }

            // Top priorities: Q1 first, then Q2 with today's due date
            val topPriorities = buildTopPriorities(urgentTasks, importantTasks, todayTasks)

            // Calculate meeting hours and focus time
            val meetingMinutes = meetings.sumOf { meeting ->
                val duration = meeting.endTime.epochSeconds - meeting.startTime.epochSeconds
                (duration / 60).toInt()
            }
            val meetingHours = meetingMinutes / MINUTES_PER_HOUR
            val workHours = (WORK_HOURS_END - WORK_HOURS_START).toFloat()
            val focusHoursAvailable = (workHours - meetingHours).coerceAtLeast(0f)

            // Goal spotlight: at-risk first, then most recent progress
            val goalSpotlight = selectGoalSpotlight(activeGoals)

            // Schedule preview (next 3 events)
            val schedulePreview = meetings
                .filter { it.startTime >= now }
                .sortedBy { it.startTime }
                .take(3)
                .map { meeting ->
                    val localTime = meeting.startTime.toLocalDateTime(timeZone)
                    SchedulePreviewItem(
                        title = meeting.title,
                        time = "%02d:%02d".format(localTime.hour, localTime.minute),
                        durationMinutes = ((meeting.endTime.epochSeconds - meeting.startTime.epochSeconds) / 60).toInt()
                    )
                }

            // Eisenhower quadrant counts
            val quadrantCounts = QuadrantCounts(
                doFirst = urgentTasks.size,
                schedule = importantTasks.size,
                delegate = allTasks.count {
                    !it.isCompleted && it.quadrant == EisenhowerQuadrant.DELEGATE
                },
                eliminate = allTasks.count {
                    !it.isCompleted && it.quadrant == EisenhowerQuadrant.ELIMINATE
                }
            )

            // Generate AI insight
            val dayOfWeek = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val insight = generateMorningInsight(
                urgentTaskCount = urgentTasks.size,
                importantTaskCount = importantTasks.size,
                meetingCount = meetings.size,
                meetingHours = meetingHours,
                focusHoursAvailable = focusHoursAvailable,
                overdueCount = overdueTasks.size,
                topGoalTitle = goalSpotlight?.title,
                topGoalProgress = goalSpotlight?.progress,
                dayOfWeek = dayOfWeek,
                yesterdayCompleted = yesterdayCompleted
            )

            // Greeting based on time of day per 1.1.5 spec
            val greeting = buildGreeting(userName, now, timeZone)
            val formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))

            val elapsed = System.currentTimeMillis() - startTime
            Timber.i("$TAG: Morning briefing generated in ${elapsed}ms")

            MorningBriefingData(
                greeting = greeting,
                date = formattedDate,
                topPriorities = topPriorities,
                schedulePreview = schedulePreview,
                goalSpotlight = goalSpotlight,
                insight = insight,
                quadrantCounts = quadrantCounts,
                overdueCount = overdueTasks.size,
                totalTodayTasks = todayTasks.size + urgentTasks.filter { it.dueDate == null || it.dueDate!! >= todayStart }.size,
                focusHoursAvailable = focusHoursAvailable,
                meetingCount = meetings.size,
                generatedAt = now,
                generationTimeMs = elapsed,
                quickStats = QuickStats(
                    tasksCompletedThisWeek = 0, // Will be filled by ViewModel
                    activeGoals = activeGoals.size,
                    weekProgressDelta = 0
                )
            )
        }

    /**
     * Generate evening summary data.
     *
     * Per 1.1.7 spec:
     * - Accomplishments, Not Done (with move-to-tomorrow), Goal Progress, Tomorrow Preview, AI Reflection
     *
     * @return Complete evening summary data
     */
    suspend fun generateEveningSummary(): EveningSummaryData =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            val timeZone = TimeZone.currentSystemDefault()
            val now = clock.now()
            val today = now.toLocalDateTime(timeZone).date
            val todayStart = today.atStartOfDayIn(timeZone)
            val tomorrowStart = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
            val tomorrowEnd = today.plus(2, DateTimeUnit.DAY).atStartOfDayIn(timeZone)

            // Parallel data fetching (using sync/suspend methods)
            val completedDeferred = async {
                taskRepository.getTasksCompletedInRange(
                    todayStart.toEpochMilliseconds(),
                    tomorrowStart.toEpochMilliseconds()
                )
            }
            val allActiveDeferred = async { taskRepository.getAllActiveTasksSync() }
            val meetingsDeferred = async { meetingRepository.getMeetingsForDateSync(todayStart, tomorrowStart) }
            val goalsDeferred = async { goalRepository.getAllActiveGoalsSync() }
            val tomorrowMeetingsDeferred = async {
                meetingRepository.getMeetingsForDateSync(tomorrowStart, tomorrowEnd)
            }

            val completedTasks = completedDeferred.await()
            val allActive = allActiveDeferred.await()
            val todayMeetings = meetingsDeferred.await()
            val activeGoals = goalsDeferred.await()
            val tomorrowMeetings = tomorrowMeetingsDeferred.await()

            // Tasks not completed that were due today
            val notDoneTasks = allActive.filter { task ->
                !task.isCompleted && task.dueDate != null &&
                    task.dueDate!! >= todayStart && task.dueDate!! < tomorrowStart
            }

            // Overdue tasks (from before today)
            val overdueTasks = allActive.filter { task ->
                !task.isCompleted && task.dueDate != null && task.dueDate!! < todayStart
            }

            // Combine not-done for display
            val allNotDone = (notDoneTasks + overdueTasks).distinctBy { it.id }

            // Goal progress — find goal with most progress today
            val goalSpotlight = selectGoalSpotlight(activeGoals)

            // Completion percentage per 1.1.7 messaging tiers
            val totalTodayTasks = completedTasks.size + notDoneTasks.size
            val completionPercentage = if (totalTodayTasks > 0) {
                (completedTasks.size * 100) / totalTodayTasks
            } else 0

            // Completion message per spec tiers
            val completionMessage = when {
                completionPercentage >= 90 -> "🎉 Amazing day! You crushed it!"
                completionPercentage in 70..89 -> "🎉 Great progress! Well done."
                completionPercentage in 50..69 -> "Good work today! Some tasks for tomorrow."
                completionPercentage in 30..49 -> "Busy day? You still made progress!"
                totalTodayTasks == 0 -> "Quiet day today. Sometimes rest is productive too."
                else -> "Every task counts. Tomorrow is a fresh start!"
            }

            // Tomorrow preview
            val tomorrowTasks = allActive.filter { task ->
                !task.isCompleted && task.dueDate != null &&
                    task.dueDate!! >= tomorrowStart && task.dueDate!! < tomorrowEnd
            }
            val tomorrowTopPriority = tomorrowTasks
                .filter { it.quadrant == EisenhowerQuadrant.DO_FIRST }
                .minByOrNull { it.dueDate ?: Instant.DISTANT_FUTURE }
                ?: tomorrowTasks.minByOrNull { it.dueDate ?: Instant.DISTANT_FUTURE }

            val tomorrowPreview = TomorrowPreview(
                topPriorityTitle = tomorrowTopPriority?.title,
                meetingCount = tomorrowMeetings.size,
                taskCount = tomorrowTasks.size + allNotDone.size, // includes carryover
                carryOverCount = allNotDone.size
            )

            // AI reflection
            val dayOfWeek = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val reflection = generateEveningReflection(
                tasksCompleted = completedTasks.size,
                totalTasks = totalTodayTasks,
                completedTitles = completedTasks.take(3).map { it.title },
                notDoneCount = allNotDone.size,
                goalProgressDelta = null, // TODO: Track intra-day delta
                spotlightGoalTitle = goalSpotlight?.title,
                meetingCount = todayMeetings.size,
                dayOfWeek = dayOfWeek
            )

            val elapsed = System.currentTimeMillis() - startTime
            Timber.i("$TAG: Evening summary generated in ${elapsed}ms")

            EveningSummaryData(
                date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                completedTasks = completedTasks.take(MAX_COMPLETED_DISPLAY).map { task ->
                    CompletedTaskItem(id = task.id, title = task.title)
                },
                tasksCompletedCount = completedTasks.size,
                totalTodayTasks = totalTodayTasks,
                completionPercentage = completionPercentage,
                completionMessage = completionMessage,
                notDoneTasks = allNotDone.map { task ->
                    NotDoneTaskItem(
                        id = task.id,
                        title = task.title,
                        quadrant = task.quadrant,
                        selectedAction = NotDoneAction.MOVE_TO_TOMORROW // default per spec
                    )
                },
                goalSpotlight = goalSpotlight,
                tomorrowPreview = tomorrowPreview,
                reflection = reflection,
                generatedAt = now,
                generationTimeMs = elapsed,
                isDayClosed = false
            )
        }

    // ==================== Private Helpers ====================

    /**
     * Build top priorities list per 1.1.5 spec task selection logic:
     * 1. All Q1 (Do First) tasks
     * 2. If <3, add Q2 due today
     * 3. Max 3-5 shown, sorted by due time then priority
     */
    private fun buildTopPriorities(
        urgentTasks: List<TaskEntity>,
        importantTasks: List<TaskEntity>,
        todayTasks: List<TaskEntity>
    ): List<TopPriorityItem> {
        val priorities = mutableListOf<TaskEntity>()

        // Add Q1 tasks first
        priorities.addAll(urgentTasks.sortedBy { it.dueDate ?: Instant.DISTANT_FUTURE })

        // Fill remaining slots with Q2 due today
        if (priorities.size < MAX_TOP_PRIORITIES) {
            val q2Today = importantTasks
                .filter { it in todayTasks }
                .sortedBy { it.dueDate ?: Instant.DISTANT_FUTURE }
            priorities.addAll(q2Today.take(MAX_TOP_PRIORITIES - priorities.size))
        }

        return priorities.take(MAX_TOP_PRIORITIES).map { task ->
            val timeZone = TimeZone.currentSystemDefault()
            val dueText = task.dueDate?.let {
                val localDt = it.toLocalDateTime(timeZone)
                val today = clock.now().toLocalDateTime(timeZone).date
                when {
                    localDt.date == today -> {
                        if (localDt.hour > 0 || localDt.minute > 0) {
                            "Due ${"%02d:%02d".format(localDt.hour, localDt.minute)}"
                        } else "Due today"
                    }
                    else -> "Due ${localDt.date}"
                }
            } ?: "No due date"

            TopPriorityItem(
                id = task.id,
                title = task.title,
                dueText = dueText,
                quadrant = task.quadrant
            )
        }
    }

    /**
     * Select goal spotlight per 1.1.5 spec logic:
     * 1. At-risk goal (needs attention)
     * 2. Goal with linked task due today
     * 3. Goal with most recent progress
     * 4. Random rotation
     */
    private fun selectGoalSpotlight(goals: List<GoalEntity>): GoalSpotlightData? {
        if (goals.isEmpty()) return null

        // Try at-risk goals first
        val atRisk = goals.filter { it.progress < 30 && !it.isCompleted }
        if (atRisk.isNotEmpty()) {
            val goal = atRisk.first()
            return GoalSpotlightData(
                id = goal.id,
                title = goal.title,
                progress = goal.progress,
                isAtRisk = true,
                nextAction = "Focus on making progress to get back on track."
            )
        }

        // Otherwise pick the one with most progress (active engagement)
        val active = goals.filter { !it.isCompleted }.sortedByDescending { it.progress }
        val goal = active.firstOrNull() ?: return null
        return GoalSpotlightData(
            id = goal.id,
            title = goal.title,
            progress = goal.progress,
            isAtRisk = false,
            nextAction = if (goal.progress >= 80) "Almost there! Push to finish." else null
        )
    }

    /**
     * Generate morning insight using hybrid approach.
     * Tries LLM first, falls back to rule-based.
     */
    private suspend fun generateMorningInsight(
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
    ): String {
        // Rule-based is always the fallback (and fast <50ms)
        val ruleBasedInsight = BriefingPrompts.getRuleBasedMorningInsight(
            urgentTaskCount = urgentTaskCount,
            meetingCount = meetingCount,
            meetingHours = meetingHours,
            focusHoursAvailable = focusHoursAvailable,
            overdueCount = overdueCount,
            topGoalTitle = topGoalTitle,
            topGoalProgress = topGoalProgress,
            dayOfWeek = dayOfWeek,
            yesterdayCompleted = yesterdayCompleted
        )

        // Try LLM enhancement if available
        return try {
            if (!aiProvider.isAvailable.value) {
                Timber.d("$TAG: AI provider not available, using rule-based insight")
                return ruleBasedInsight
            }

            val prompt = BriefingPrompts.buildMorningInsightPrompt(
                urgentTaskCount = urgentTaskCount,
                importantTaskCount = importantTaskCount,
                meetingCount = meetingCount,
                meetingHours = meetingHours,
                focusHoursAvailable = focusHoursAvailable,
                overdueCount = overdueCount,
                topGoalTitle = topGoalTitle,
                topGoalProgress = topGoalProgress,
                dayOfWeek = dayOfWeek,
                yesterdayCompleted = yesterdayCompleted
            )

            val request = AiRequest(
                type = AiRequestType.GENERATE_BRIEFING,
                input = prompt,
                systemPrompt = BriefingPrompts.MORNING_SYSTEM_PROMPT,
                options = AiRequestOptions(
                    maxTokens = 100,
                    temperature = 0.7f,
                    useLlm = true,
                    fallbackToRuleBased = true
                )
            )

            val response = aiProvider.complete(request)
            response.getOrNull()?.let { aiResponse ->
                when (val result = aiResponse.result) {
                    is AiResult.BriefingContent -> result.insights.firstOrNull() ?: ruleBasedInsight
                    is AiResult.GeneratedText -> result.text.takeIf { it.isNotBlank() } ?: ruleBasedInsight
                    else -> aiResponse.rawText?.takeIf { it.isNotBlank() } ?: ruleBasedInsight
                }
            } ?: ruleBasedInsight
        } catch (e: Exception) {
            Timber.w(e, "$TAG: LLM insight generation failed, using rule-based")
            ruleBasedInsight
        }
    }

    /**
     * Generate evening reflection using hybrid approach.
     */
    private suspend fun generateEveningReflection(
        tasksCompleted: Int,
        totalTasks: Int,
        completedTitles: List<String>,
        notDoneCount: Int,
        goalProgressDelta: String?,
        spotlightGoalTitle: String?,
        meetingCount: Int,
        dayOfWeek: String
    ): String {
        val ruleBasedReflection = BriefingPrompts.getRuleBasedEveningReflection(
            tasksCompleted = tasksCompleted,
            totalTasks = totalTasks,
            notDoneCount = notDoneCount,
            goalProgressDelta = goalProgressDelta,
            spotlightGoalTitle = spotlightGoalTitle,
            dayOfWeek = dayOfWeek
        )

        return try {
            if (!aiProvider.isAvailable.value) {
                return ruleBasedReflection
            }

            val prompt = BriefingPrompts.buildEveningReflectionPrompt(
                tasksCompleted = tasksCompleted,
                totalTasks = totalTasks,
                completedTitles = completedTitles,
                notDoneCount = notDoneCount,
                goalProgressDelta = goalProgressDelta,
                spotlightGoalTitle = spotlightGoalTitle,
                meetingCount = meetingCount,
                dayOfWeek = dayOfWeek
            )

            val request = AiRequest(
                type = AiRequestType.GENERATE_BRIEFING,
                input = prompt,
                systemPrompt = BriefingPrompts.EVENING_SYSTEM_PROMPT,
                options = AiRequestOptions(
                    maxTokens = 150,
                    temperature = 0.7f,
                    useLlm = true,
                    fallbackToRuleBased = true
                )
            )

            val response = aiProvider.complete(request)
            response.getOrNull()?.let { aiResponse ->
                when (val result = aiResponse.result) {
                    is AiResult.BriefingContent -> result.summary.takeIf { it.isNotBlank() } ?: ruleBasedReflection
                    is AiResult.GeneratedText -> result.text.takeIf { it.isNotBlank() } ?: ruleBasedReflection
                    else -> aiResponse.rawText?.takeIf { it.isNotBlank() } ?: ruleBasedReflection
                }
            } ?: ruleBasedReflection
        } catch (e: Exception) {
            Timber.w(e, "$TAG: LLM reflection generation failed, using rule-based")
            ruleBasedReflection
        }
    }

    /**
     * Build time-based greeting per 1.1.5 spec.
     */
    private fun buildGreeting(userName: String?, now: Instant, timeZone: TimeZone): String {
        val hour = now.toLocalDateTime(timeZone).hour
        val nameStr = userName?.let { ", $it" } ?: ""
        return when (hour) {
            in 5..11 -> "Good morning$nameStr"
            in 12..16 -> "Good afternoon$nameStr"
            in 17..20 -> "Good evening$nameStr"
            else -> "Hello$nameStr"
        }
    }
}

// ==================== Data Models ====================

/**
 * Complete morning briefing data per 1.1.5 Today Dashboard spec.
 */
data class MorningBriefingData(
    val greeting: String,
    val date: String,
    val topPriorities: List<TopPriorityItem>,
    val schedulePreview: List<SchedulePreviewItem>,
    val goalSpotlight: GoalSpotlightData?,
    val insight: String,
    val quadrantCounts: QuadrantCounts,
    val overdueCount: Int,
    val totalTodayTasks: Int,
    val focusHoursAvailable: Float,
    val meetingCount: Int,
    val generatedAt: Instant,
    val generationTimeMs: Long,
    val quickStats: QuickStats
)

data class TopPriorityItem(
    val id: Long,
    val title: String,
    val dueText: String,
    val quadrant: EisenhowerQuadrant
)

data class SchedulePreviewItem(
    val title: String,
    val time: String,
    val durationMinutes: Int
)

data class GoalSpotlightData(
    val id: Long,
    val title: String,
    val progress: Int,
    val isAtRisk: Boolean,
    val nextAction: String? = null
)

data class QuadrantCounts(
    val doFirst: Int,
    val schedule: Int,
    val delegate: Int,
    val eliminate: Int
)

data class QuickStats(
    val tasksCompletedThisWeek: Int,
    val activeGoals: Int,
    val weekProgressDelta: Int
)

/**
 * Complete evening summary data per 1.1.7 Evening Summary spec.
 */
data class EveningSummaryData(
    val date: String,
    val completedTasks: List<CompletedTaskItem>,
    val tasksCompletedCount: Int,
    val totalTodayTasks: Int,
    val completionPercentage: Int,
    val completionMessage: String,
    val notDoneTasks: List<NotDoneTaskItem>,
    val goalSpotlight: GoalSpotlightData?,
    val tomorrowPreview: TomorrowPreview,
    val reflection: String,
    val generatedAt: Instant,
    val generationTimeMs: Long,
    val isDayClosed: Boolean
)

data class CompletedTaskItem(
    val id: Long,
    val title: String
)

data class NotDoneTaskItem(
    val id: Long,
    val title: String,
    val quadrant: EisenhowerQuadrant,
    val selectedAction: NotDoneAction = NotDoneAction.MOVE_TO_TOMORROW
)

enum class NotDoneAction {
    MOVE_TO_TOMORROW,
    RESCHEDULE,
    DROP
}

data class TomorrowPreview(
    val topPriorityTitle: String?,
    val meetingCount: Int,
    val taskCount: Int,
    val carryOverCount: Int
)

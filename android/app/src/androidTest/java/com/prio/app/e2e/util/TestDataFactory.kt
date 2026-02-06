package com.prio.app.e2e.util

import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.common.model.GoalCategory
import com.prio.core.common.model.RecurrencePattern
import com.prio.core.data.local.entity.DailyAnalyticsEntity
import com.prio.core.data.local.entity.GoalEntity
import com.prio.core.data.local.entity.MeetingEntity
import com.prio.core.data.local.entity.MilestoneEntity
import com.prio.core.data.local.entity.TaskEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Factory for creating test data entities.
 *
 * Provides builder-style methods for each Room entity with sensible defaults.
 * All timestamps default to [Clock.System.now()] for consistency.
 *
 * Usage:
 * ```
 * val task = TestDataFactory.task(
 *     title = "Buy groceries",
 *     quadrant = EisenhowerQuadrant.SCHEDULE,
 *     dueDate = TestDataFactory.hoursFromNow(4)
 * )
 * taskDao.insert(task)
 * ```
 */
object TestDataFactory {

    private val now: Instant get() = Clock.System.now()
    private val today: LocalDate
        get() = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

    // =========================================================================
    // Timestamp helpers
    // =========================================================================

    fun hoursFromNow(hours: Int): Instant = now.plus(hours.hours)
    fun daysFromNow(days: Int): Instant = now.plus(days.days)
    fun hoursAgo(hours: Int): Instant = now.minus(hours.hours)
    fun daysAgo(days: Int): Instant = now.minus(days.days)
    fun daysFromToday(days: Int): LocalDate = today.plus(days, DateTimeUnit.DAY)
    fun daysBeforeToday(days: Int): LocalDate = today.minus(days, DateTimeUnit.DAY)

    // =========================================================================
    // TaskEntity builders
    // =========================================================================

    fun task(
        id: Long = 0,
        title: String = "Test Task",
        notes: String? = null,
        dueDate: Instant? = null,
        quadrant: EisenhowerQuadrant = EisenhowerQuadrant.ELIMINATE,
        goalId: Long? = null,
        parentTaskId: Long? = null,
        isRecurring: Boolean = false,
        recurrencePattern: RecurrencePattern? = null,
        urgencyScore: Float = 0f,
        aiExplanation: String? = "AI classified this task",
        aiConfidence: Float = 0.85f,
        isCompleted: Boolean = false,
        completedAt: Instant? = null,
        createdAt: Instant = now,
        updatedAt: Instant = now,
        position: Int = 0
    ) = TaskEntity(
        id = id,
        title = title,
        notes = notes,
        dueDate = dueDate,
        quadrant = quadrant,
        goalId = goalId,
        parentTaskId = parentTaskId,
        isRecurring = isRecurring,
        recurrencePattern = recurrencePattern,
        urgencyScore = urgencyScore,
        aiExplanation = aiExplanation,
        aiConfidence = aiConfidence,
        isCompleted = isCompleted,
        completedAt = completedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        position = position
    )

    /** Urgent task due today, Do First quadrant. */
    fun urgentTask(
        title: String = "Urgent: Deadline today",
        dueDate: Instant = hoursFromNow(2)
    ) = task(
        title = title,
        quadrant = EisenhowerQuadrant.DO_FIRST,
        dueDate = dueDate,
        urgencyScore = 0.95f,
        aiExplanation = "Has deadline today - marked as urgent and important"
    )

    /** Overdue task — due date in the past, not completed. */
    fun overdueTask(
        title: String = "Overdue: Should have been done",
        dueDate: Instant = daysAgo(2)
    ) = task(
        title = title,
        quadrant = EisenhowerQuadrant.DO_FIRST,
        dueDate = dueDate,
        urgencyScore = 1.0f,
        aiExplanation = "Overdue task requiring immediate attention"
    )

    /** Recurring task with specified pattern. */
    fun recurringTask(
        title: String = "Daily standup",
        pattern: RecurrencePattern = RecurrencePattern.DAILY,
        quadrant: EisenhowerQuadrant = EisenhowerQuadrant.SCHEDULE
    ) = task(
        title = title,
        quadrant = quadrant,
        isRecurring = true,
        recurrencePattern = pattern,
        dueDate = hoursFromNow(12)
    )

    /** Completed task. */
    fun completedTask(
        title: String = "Completed task",
        quadrant: EisenhowerQuadrant = EisenhowerQuadrant.DO_FIRST
    ) = task(
        title = title,
        quadrant = quadrant,
        isCompleted = true,
        completedAt = hoursAgo(1)
    )

    /** Subtask linked to a parent. */
    fun subtask(
        title: String = "Subtask",
        parentTaskId: Long
    ) = task(
        title = title,
        parentTaskId = parentTaskId,
        quadrant = EisenhowerQuadrant.ELIMINATE
    )

    // =========================================================================
    // GoalEntity builders
    // =========================================================================

    fun goal(
        id: Long = 0,
        title: String = "Test Goal",
        description: String? = "A test goal for E2E",
        originalInput: String? = null,
        category: GoalCategory = GoalCategory.PERSONAL,
        targetDate: LocalDate? = daysFromToday(30),
        progress: Int = 0,
        isCompleted: Boolean = false,
        completedAt: Instant? = null,
        createdAt: Instant = now,
        updatedAt: Instant = now
    ) = GoalEntity(
        id = id,
        title = title,
        description = description,
        originalInput = originalInput,
        category = category,
        targetDate = targetDate,
        progress = progress,
        isCompleted = isCompleted,
        completedAt = completedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    /** Goal that's on track (25-75% progress). */
    fun onTrackGoal(
        title: String = "On Track Goal",
        progress: Int = 50,
        category: GoalCategory = GoalCategory.CAREER
    ) = goal(
        title = title,
        progress = progress,
        category = category
    )

    /** Goal that's at risk (low progress, near target date). */
    fun atRiskGoal(
        title: String = "At Risk Goal",
        progress: Int = 10,
        category: GoalCategory = GoalCategory.HEALTH
    ) = goal(
        title = title,
        progress = progress,
        category = category,
        targetDate = daysFromToday(3) // Near deadline, low progress
    )

    /** Completed goal. */
    fun completedGoal(
        title: String = "Completed Goal",
        category: GoalCategory = GoalCategory.LEARNING
    ) = goal(
        title = title,
        progress = 100,
        isCompleted = true,
        completedAt = hoursAgo(24),
        category = category
    )

    // =========================================================================
    // MilestoneEntity builders
    // =========================================================================

    fun milestone(
        id: Long = 0,
        goalId: Long,
        title: String = "Test Milestone",
        targetDate: LocalDate? = daysFromToday(15),
        isCompleted: Boolean = false,
        completedAt: Instant? = null,
        position: Int = 0,
        createdAt: Instant = now,
        updatedAt: Instant = now
    ) = MilestoneEntity(
        id = id,
        goalId = goalId,
        title = title,
        targetDate = targetDate,
        isCompleted = isCompleted,
        completedAt = completedAt,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    // =========================================================================
    // MeetingEntity builders
    // =========================================================================

    fun meeting(
        id: Long = 0,
        calendarEventId: String? = null,
        title: String = "Test Meeting",
        description: String? = null,
        location: String? = null,
        startTime: Instant = hoursFromNow(1),
        endTime: Instant = hoursFromNow(2),
        attendees: String? = null,
        notes: String? = null,
        actionItems: String? = null,
        agenda: String? = null,
        createdAt: Instant = now,
        updatedAt: Instant = now
    ) = MeetingEntity(
        id = id,
        calendarEventId = calendarEventId,
        title = title,
        description = description,
        location = location,
        startTime = startTime,
        endTime = endTime,
        attendees = attendees,
        notes = notes,
        actionItems = actionItems,
        agenda = agenda,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    /** Ongoing meeting (started 30 min ago, ends in 30 min). */
    fun ongoingMeeting(title: String = "Ongoing Sync") = meeting(
        title = title,
        startTime = now.minus(30.toLong().hours / 60),
        endTime = now.plus(30.toLong().hours / 60)
    )

    /** Meeting in the past. */
    fun pastMeeting(title: String = "Yesterday's Retro") = meeting(
        title = title,
        startTime = daysAgo(1),
        endTime = daysAgo(1).plus(1.hours)
    )

    // =========================================================================
    // DailyAnalyticsEntity builders
    // =========================================================================

    fun analytics(
        id: Long = 0,
        date: LocalDate = today,
        tasksCreated: Int = 3,
        tasksCompleted: Int = 2,
        q1Completed: Int = 1,
        q2Completed: Int = 1,
        q3Completed: Int = 0,
        q4Completed: Int = 0,
        goalsProgressed: Int = 1,
        aiClassifications: Int = 3,
        aiOverrides: Int = 0,
        briefingOpened: Boolean = false,
        summaryOpened: Boolean = false
    ) = DailyAnalyticsEntity(
        id = id,
        date = date,
        tasksCreated = tasksCreated,
        tasksCompleted = tasksCompleted,
        q1Completed = q1Completed,
        q2Completed = q2Completed,
        q3Completed = q3Completed,
        q4Completed = q4Completed,
        goalsProgressed = goalsProgressed,
        aiClassifications = aiClassifications,
        aiOverrides = aiOverrides,
        briefingOpened = briefingOpened,
        summaryOpened = summaryOpened
    )

    // =========================================================================
    // Batch data for common test scenarios
    // =========================================================================

    /** Mixed set of tasks across all quadrants for task list tests. */
    fun mixedTaskSet(): List<TaskEntity> = listOf(
        urgentTask(title = "Prepare presentation"),
        task(title = "Call dentist", quadrant = EisenhowerQuadrant.SCHEDULE, dueDate = daysFromNow(1)),
        task(title = "Reply to emails", quadrant = EisenhowerQuadrant.DELEGATE),
        task(title = "Clean desk", quadrant = EisenhowerQuadrant.ELIMINATE),
        overdueTask(title = "Submit report"),
        recurringTask(title = "Daily standup"),
        completedTask(title = "Buy groceries")
    )

    /** A week of analytics data for insights tests. */
    fun weekOfAnalytics(): List<DailyAnalyticsEntity> = (0..6).map { daysBack ->
        analytics(
            date = daysBeforeToday(daysBack),
            tasksCreated = (2..5).random(),
            tasksCompleted = (1..4).random(),
            q1Completed = (0..2).random(),
            q2Completed = (0..2).random()
        )
    }
}

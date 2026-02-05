package com.prio.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prio.core.common.model.RecurrencePattern
import com.prio.core.data.local.entity.TaskEntity
import com.prio.core.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber

/**
 * WorkManager Worker for creating the next occurrence of a recurring task.
 * 
 * Implements TM-008 from 0.3.2_task_management_user_stories.md:
 * - When recurring task is completed, next instance is created automatically
 * - Recurring tasks remember quadrant for new instances
 * - Support for Daily, Weekly, Monthly, Yearly patterns
 * 
 * Usage:
 * This worker is triggered when a recurring task is completed. It calculates
 * the next due date based on the recurrence pattern and creates a new task.
 */
@HiltWorker
class RecurringTaskWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val clock: Clock
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_COMPLETED_TASK_ID = "completed_task_id"
        const val TAG = "RecurringTaskWorker"
        
        /**
         * Default hour for recurring tasks without specific time (9 AM).
         */
        const val DEFAULT_HOUR = 9
        const val DEFAULT_MINUTE = 0
    }

    override suspend fun doWork(): Result {
        val completedTaskId = inputData.getLong(KEY_COMPLETED_TASK_ID, -1L)
        
        if (completedTaskId == -1L) {
            Timber.e("$TAG: No task ID provided")
            return Result.failure()
        }

        return try {
            val completedTask = taskRepository.getTaskById(completedTaskId)
            
            if (completedTask == null) {
                Timber.e("$TAG: Task $completedTaskId not found")
                return Result.failure()
            }

            val recurrencePattern = completedTask.recurrencePattern
            if (!completedTask.isRecurring || recurrencePattern == null) {
                Timber.w("$TAG: Task $completedTaskId is not recurring")
                return Result.success()
            }

            val nextDueDate = calculateNextDueDate(
                currentDueDate = completedTask.dueDate,
                pattern = recurrencePattern,
                completedAt = completedTask.completedAt ?: clock.now()
            )

            val now = clock.now()
            val nextTask = TaskEntity(
                title = completedTask.title,
                notes = completedTask.notes,
                dueDate = nextDueDate,
                quadrant = completedTask.quadrant, // Remember quadrant per TM-008
                goalId = completedTask.goalId,
                parentTaskId = null, // Recurring tasks don't have parents
                isRecurring = true,
                recurrencePattern = completedTask.recurrencePattern,
                urgencyScore = 0f, // Will be recalculated by repository
                aiExplanation = completedTask.aiExplanation,
                aiConfidence = completedTask.aiConfidence,
                isCompleted = false,
                completedAt = null,
                createdAt = now,
                updatedAt = now,
                position = completedTask.position
            )

            val newTaskId = taskRepository.insertTask(nextTask)
            Timber.i("$TAG: Created next occurrence for recurring task. Original: $completedTaskId, New: $newTaskId, Next due: $nextDueDate")

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to create next occurrence for task $completedTaskId")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Calculates the next due date based on recurrence pattern.
     * 
     * Per TM-008 acceptance criteria:
     * - Daily: Next day
     * - Weekly: Same day next week
     * - Monthly: Same date next month
     * - Yearly: Same date next year
     * - Weekdays: Next weekday (Mon-Fri)
     * - Custom: Based on interval (future enhancement)
     */
    internal fun calculateNextDueDate(
        currentDueDate: Instant?,
        pattern: RecurrencePattern,
        completedAt: Instant
    ): Instant {
        val timeZone = TimeZone.currentSystemDefault()
        
        // If no current due date, use completion time as reference
        val referenceDate = currentDueDate?.toLocalDateTime(timeZone)?.date
            ?: completedAt.toLocalDateTime(timeZone).date

        // If current due date had a time, preserve it; otherwise use default
        val referenceTime = currentDueDate?.toLocalDateTime(timeZone)?.time
            ?: kotlinx.datetime.LocalTime(DEFAULT_HOUR, DEFAULT_MINUTE)

        val nextDate: LocalDate = when (pattern) {
            RecurrencePattern.DAILY -> {
                referenceDate.plus(1, DateTimeUnit.DAY)
            }
            
            RecurrencePattern.WEEKLY -> {
                referenceDate.plus(1, DateTimeUnit.WEEK)
            }
            
            RecurrencePattern.MONTHLY -> {
                referenceDate.plus(1, DateTimeUnit.MONTH)
            }
            
            RecurrencePattern.YEARLY -> {
                referenceDate.plus(1, DateTimeUnit.YEAR)
            }
            
            RecurrencePattern.WEEKDAYS -> {
                // Find next weekday (Mon-Fri)
                var next = referenceDate.plus(1, DateTimeUnit.DAY)
                while (next.dayOfWeek == DayOfWeek.SATURDAY || 
                       next.dayOfWeek == DayOfWeek.SUNDAY) {
                    next = next.plus(1, DateTimeUnit.DAY)
                }
                next
            }
            
            RecurrencePattern.CUSTOM -> {
                // Default to daily for custom (will be enhanced in v1.1)
                referenceDate.plus(1, DateTimeUnit.DAY)
            }
        }

        return nextDate.atTime(referenceTime).toInstant(timeZone)
    }
}

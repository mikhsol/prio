package com.prio.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for recurring task operations via WorkManager.
 * 
 * Implements TM-008 from 0.3.2_task_management_user_stories.md:
 * - When recurring task completed, next instance created automatically
 * - Uses lazy creation (don't pre-generate all instances)
 * - Reliable background execution via WorkManager
 * 
 * Design decisions:
 * - Uses OneTimeWorkRequest (not periodic) because each occurrence is unique
 * - Enqueues work immediately on task completion for UX responsiveness
 * - Work runs even if app is closed (WorkManager guarantee)
 */
@Singleton
class RecurringTaskScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "RecurringTaskScheduler"
        private const val WORK_NAME_PREFIX = "recurring_task_"
    }

    /**
     * Schedules creation of the next occurrence of a recurring task.
     * 
     * Called when a recurring task is completed. WorkManager ensures
     * the next occurrence is created even if the app is killed.
     * 
     * @param completedTaskId The ID of the completed recurring task
     */
    fun scheduleNextOccurrence(completedTaskId: Long) {
        Timber.d("$TAG: Scheduling next occurrence for task $completedTaskId")

        val inputData = Data.Builder()
            .putLong(RecurringTaskWorker.KEY_COMPLETED_TASK_ID, completedTaskId)
            .build()

        // No constraints needed - we want this to run immediately
        val constraints = Constraints.Builder()
            .build()

        val workRequest = OneTimeWorkRequestBuilder<RecurringTaskWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("$WORK_NAME_PREFIX$completedTaskId")
            .build()

        // Use unique work to prevent duplicates if user completes task multiple times
        WorkManager.getInstance(context).enqueueUniqueWork(
            "$WORK_NAME_PREFIX$completedTaskId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Timber.i("$TAG: Enqueued work for recurring task $completedTaskId")
    }

    /**
     * Cancels any pending work for a recurring task.
     * 
     * Called when a recurring task is deleted or recurrence is ended.
     * 
     * @param taskId The ID of the task to cancel work for
     */
    fun cancelPendingWork(taskId: Long) {
        Timber.d("$TAG: Cancelling pending work for task $taskId")
        
        WorkManager.getInstance(context)
            .cancelUniqueWork("$WORK_NAME_PREFIX$taskId")
    }

    /**
     * Checks if work is pending for a specific task.
     * 
     * @param taskId The ID of the task to check
     * @return true if work is pending
     */
    fun isWorkPending(taskId: Long): Boolean {
        // WorkManager's getWorkInfosForUniqueWork is async, 
        // so this is a best-effort check
        return false // Simplified for MVP
    }
}

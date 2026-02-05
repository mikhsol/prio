package com.prio.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.prio.core.data.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * BroadcastReceiver for handling reminder notification actions.
 * 
 * Implements TM-009 from 0.3.2_task_management_user_stories.md:
 * - Complete: Marks task as completed directly from notification
 * - Snooze: Reschedules reminder (15min, 1hr, tomorrow)
 * 
 * Actions are processed asynchronously with CoroutineScope.
 */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {
    
    companion object {
        const val TAG = "ReminderActionReceiver"
        const val ACTION_COMPLETE = "com.prio.app.COMPLETE_TASK"
        const val ACTION_SNOOZE_15MIN = "com.prio.app.SNOOZE_15MIN"
        const val ACTION_SNOOZE_1HR = "com.prio.app.SNOOZE_1HR"
        const val ACTION_SNOOZE_TOMORROW = "com.prio.app.SNOOZE_TOMORROW"
    }

    @Inject
    lateinit var taskRepository: TaskRepository
    
    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderWorker.KEY_TASK_ID, -1L)
        
        if (taskId == -1L) {
            Timber.e("$TAG: No task ID in intent")
            return
        }

        // Dismiss notification
        NotificationManagerCompat.from(context).cancel(taskId.toInt())

        when (intent.action) {
            ACTION_COMPLETE -> handleComplete(taskId)
            ACTION_SNOOZE_15MIN -> handleSnooze(taskId, 15)
            ACTION_SNOOZE_1HR -> handleSnooze(taskId, 60)
            ACTION_SNOOZE_TOMORROW -> handleSnoozeTomorrow(taskId)
            else -> Timber.w("$TAG: Unknown action ${intent.action}")
        }
    }

    private fun handleComplete(taskId: Long) {
        scope.launch {
            try {
                Timber.d("$TAG: Completing task $taskId from notification")
                taskRepository.completeTask(taskId)
                
                // If recurring, schedule next occurrence
                val task = taskRepository.getTaskById(taskId)
                if (task?.isRecurring == true && task.recurrencePattern != null) {
                    // Note: The RecurringTaskScheduler should handle this
                    Timber.d("$TAG: Task is recurring, next occurrence will be scheduled")
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Failed to complete task $taskId")
            }
        }
    }

    private fun handleSnooze(taskId: Long, minutes: Int) {
        scope.launch {
            try {
                Timber.d("$TAG: Snoozing task $taskId for $minutes minutes")
                reminderScheduler.scheduleSnoozeReminder(taskId, minutes)
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Failed to snooze task $taskId")
            }
        }
    }

    private fun handleSnoozeTomorrow(taskId: Long) {
        scope.launch {
            try {
                Timber.d("$TAG: Snoozing task $taskId until tomorrow morning")
                reminderScheduler.scheduleTomorrowMorningReminder(taskId)
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Failed to snooze task $taskId until tomorrow")
            }
        }
    }
}

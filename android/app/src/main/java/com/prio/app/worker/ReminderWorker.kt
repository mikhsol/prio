package com.prio.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prio.app.MainActivity
import com.prio.app.R
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber

/**
 * WorkManager Worker for sending task reminder notifications.
 * 
 * Implements TM-009 from 0.3.2_task_management_user_stories.md:
 * - Notifications at configurable intervals (1d, 3d before deadline)
 * - Notification shows task title and due time
 * - Tap notification opens task directly
 * - Snooze support (15min, 1hr, tomorrow)
 * - No reminders for Q4 tasks unless explicitly set
 * - Quiet hours respected (default 10pm-7am)
 * 
 * Notification Channels:
 * - URGENT: For Q1 tasks and imminent deadlines
 * - IMPORTANT: For Q2 tasks
 * - NORMAL: For other reminders
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val clock: Clock
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_REMINDER_TYPE = "reminder_type"
        const val TAG = "ReminderWorker"
        
        // Notification channel IDs
        const val CHANNEL_URGENT = "prio_reminders_urgent"
        const val CHANNEL_IMPORTANT = "prio_reminders_important"
        const val CHANNEL_NORMAL = "prio_reminders_normal"
        
        // Quiet hours (10 PM to 7 AM by default)
        const val QUIET_HOUR_START = 22
        const val QUIET_HOUR_END = 7
        
        // Request codes for pending intents
        private const val REQUEST_CODE_OPEN = 1000
        private const val REQUEST_CODE_COMPLETE = 2000
        private const val REQUEST_CODE_SNOOZE_15MIN = 3000
        private const val REQUEST_CODE_SNOOZE_1HR = 4000
    }

    enum class ReminderType {
        DEADLINE_TODAY,       // Due today
        DEADLINE_TOMORROW,    // Due tomorrow
        DEADLINE_IN_3_DAYS,   // Due in 3 days
        CUSTOM,               // User-set reminder
        OVERDUE               // Past due
    }

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        val reminderTypeOrdinal = inputData.getInt(KEY_REMINDER_TYPE, ReminderType.CUSTOM.ordinal)
        val reminderType = ReminderType.entries.getOrNull(reminderTypeOrdinal) ?: ReminderType.CUSTOM
        
        if (taskId == -1L) {
            Timber.e("$TAG: No task ID provided")
            return Result.failure()
        }

        return try {
            val task = taskRepository.getTaskById(taskId)
            
            if (task == null) {
                Timber.e("$TAG: Task $taskId not found")
                return Result.failure()
            }

            // Don't send reminders for completed tasks
            if (task.isCompleted) {
                Timber.d("$TAG: Task $taskId is completed, skipping reminder")
                return Result.success()
            }

            // Don't send reminders for Q4 tasks (per TM-009)
            if (task.quadrant == EisenhowerQuadrant.ELIMINATE) {
                Timber.d("$TAG: Task $taskId is Q4, skipping reminder per TM-009")
                return Result.success()
            }

            // Check quiet hours
            if (isInQuietHours()) {
                Timber.d("$TAG: In quiet hours, rescheduling reminder")
                // Reschedule for end of quiet hours
                return Result.retry()
            }

            // Send notification
            sendNotification(taskId, task.title, task.quadrant, task.dueDate, reminderType)
            
            Timber.i("$TAG: Sent reminder for task $taskId (${reminderType.name})")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to send reminder for task $taskId")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Check if current time is within quiet hours.
     */
    private fun isInQuietHours(): Boolean {
        val now = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentHour = now.hour
        
        return if (QUIET_HOUR_START > QUIET_HOUR_END) {
            // Quiet hours span midnight (e.g., 22:00 to 07:00)
            currentHour >= QUIET_HOUR_START || currentHour < QUIET_HOUR_END
        } else {
            currentHour in QUIET_HOUR_START until QUIET_HOUR_END
        }
    }

    /**
     * Send the reminder notification.
     */
    private fun sendNotification(
        taskId: Long,
        title: String,
        quadrant: EisenhowerQuadrant,
        dueDate: kotlinx.datetime.Instant?,
        reminderType: ReminderType
    ) {
        // Ensure notification channels exist
        createNotificationChannels()
        
        // Determine channel based on urgency
        val channelId = when {
            reminderType == ReminderType.OVERDUE || reminderType == ReminderType.DEADLINE_TODAY -> CHANNEL_URGENT
            quadrant == EisenhowerQuadrant.DO_FIRST -> CHANNEL_URGENT
            quadrant == EisenhowerQuadrant.SCHEDULE -> CHANNEL_IMPORTANT
            else -> CHANNEL_NORMAL
        }

        // Build notification content
        val contentText = buildContentText(reminderType, dueDate)
        
        // Intent to open task detail
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("task_id", taskId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN + taskId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Complete task
        val completeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_COMPLETE
            putExtra(KEY_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_COMPLETE + taskId.toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 15 minutes
        val snooze15Intent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE_15MIN
            putExtra(KEY_TASK_ID, taskId)
        }
        val snooze15PendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SNOOZE_15MIN + taskId.toInt(),
            snooze15Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(
                if (channelId == CHANNEL_URGENT) NotificationCompat.PRIORITY_HIGH 
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check, "Complete", completePendingIntent)
            .addAction(R.drawable.ic_snooze, "Snooze", snooze15PendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.w("$TAG: Notification permission not granted")
                return
            }
        }

        NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
    }

    /**
     * Build content text based on reminder type.
     */
    private fun buildContentText(
        reminderType: ReminderType,
        dueDate: kotlinx.datetime.Instant?
    ): String {
        return when (reminderType) {
            ReminderType.OVERDUE -> "⚠️ This task is overdue!"
            ReminderType.DEADLINE_TODAY -> {
                dueDate?.let { 
                    val time = it.toLocalDateTime(TimeZone.currentSystemDefault()).time
                    "Due today at ${formatTime(time.hour, time.minute)}"
                } ?: "Due today"
            }
            ReminderType.DEADLINE_TOMORROW -> "Due tomorrow"
            ReminderType.DEADLINE_IN_3_DAYS -> "Due in 3 days"
            ReminderType.CUSTOM -> {
                dueDate?.let {
                    val dateTime = it.toLocalDateTime(TimeZone.currentSystemDefault())
                    "Due ${dateTime.date}"
                } ?: "Don't forget!"
            }
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return if (minute > 0) {
            "$displayHour:${minute.toString().padStart(2, '0')} $period"
        } else {
            "$displayHour $period"
        }
    }

    /**
     * Create notification channels for different reminder priorities.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            
            // Urgent channel (Q1, overdue, due today)
            val urgentChannel = NotificationChannel(
                CHANNEL_URGENT,
                "Urgent Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent task reminders for deadlines today and overdue items"
                enableVibration(true)
                enableLights(true)
            }

            // Important channel (Q2 tasks)
            val importantChannel = NotificationChannel(
                CHANNEL_IMPORTANT,
                "Important Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for important tasks"
            }

            // Normal channel (other reminders)
            val normalChannel = NotificationChannel(
                CHANNEL_NORMAL,
                "Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "General task reminders"
            }

            notificationManager.createNotificationChannels(
                listOf(urgentChannel, importantChannel, normalChannel)
            )
        }
    }
}

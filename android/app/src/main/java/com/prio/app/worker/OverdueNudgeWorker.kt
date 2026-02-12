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
import com.prio.core.data.preferences.UserPreferencesRepository
import com.prio.core.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber

/**
 * WorkManager Worker for periodic overdue task nudge notifications.
 *
 * Part of Milestone 4.2: Smart nudge system.
 * Scans for tasks that are past their due date and not completed,
 * then sends a summary notification nudging the user to take action.
 *
 * Respects:
 * - notifications_enabled master toggle
 * - overdue_alerts_enabled per-type toggle
 * - quiet hours preferences
 *
 * Scheduled by [OverdueNudgeScheduler] to run periodically (default: every 4 hours).
 */
@HiltWorker
class OverdueNudgeWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val clock: Clock
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "OverdueNudgeWorker"

        // Notification channel
        const val CHANNEL_OVERDUE = "prio_overdue_nudge"

        // Fixed notification ID (replaces previous overdue nudge)
        const val NOTIFICATION_ID_OVERDUE = 8001

        private const val REQUEST_CODE_OVERDUE = 8100
    }

    override suspend fun doWork(): Result {
        return try {
            // Check if notifications and overdue alerts are enabled
            val notificationsEnabled = userPreferencesRepository.notificationsEnabled.first()
            val overdueAlertsEnabled = userPreferencesRepository.overdueAlertsEnabled.first()

            if (!notificationsEnabled || !overdueAlertsEnabled) {
                Timber.d("$TAG: Overdue alerts disabled, skipping")
                return Result.success()
            }

            // Check quiet hours
            if (isInQuietHours()) {
                Timber.d("$TAG: In quiet hours, skipping overdue nudge")
                return Result.success()
            }

            // Find overdue tasks
            val now = clock.now()
            val allActive = taskRepository.getAllActiveTasksSync()
            val overdueTasks = allActive.filter { task ->
                task.dueDate != null &&
                    task.dueDate!! < now &&
                    !task.isCompleted &&
                    task.quadrant != EisenhowerQuadrant.ELIMINATE
            }

            if (overdueTasks.isEmpty()) {
                Timber.d("$TAG: No overdue tasks, dismissing any existing notification")
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_OVERDUE)
                return Result.success()
            }

            sendOverdueNotification(overdueTasks.size, overdueTasks.firstOrNull()?.title)

            Timber.i("$TAG: Sent overdue nudge for ${overdueTasks.size} task(s)")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to check overdue tasks")
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private suspend fun isInQuietHours(): Boolean {
        val quietHoursEnabled = userPreferencesRepository.quietHoursEnabled.first()
        if (!quietHoursEnabled) return false

        val quietStart = userPreferencesRepository.quietHoursStart.first()
        val quietEnd = userPreferencesRepository.quietHoursEnd.first()

        val now = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentHour = now.hour

        return if (quietStart > quietEnd) {
            currentHour >= quietStart || currentHour < quietEnd
        } else {
            currentHour in quietStart until quietEnd
        }
    }

    private fun sendOverdueNotification(count: Int, topTaskTitle: String?) {
        createNotificationChannel()

        val contentText = buildString {
            append("⚠️ You have $count overdue task${if (count != 1) "s" else ""}")
            if (topTaskTitle != null) {
                append(". Top: \"$topTaskTitle\"")
            }
            append(" — review and reprioritize")
        }

        // Intent to open Tasks screen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "tasks")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OVERDUE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_OVERDUE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Overdue tasks need attention")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
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

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_OVERDUE, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_OVERDUE,
                "Overdue Task Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for tasks past their due date"
                enableVibration(true)
                enableLights(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}

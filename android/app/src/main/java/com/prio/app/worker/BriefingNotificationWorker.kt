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
import com.prio.core.data.preferences.UserPreferencesRepository
import com.prio.core.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber

/**
 * WorkManager Worker for daily briefing notifications (task 3.4.4).
 *
 * Per CB-001 from 0.3.4_calendar_briefings_user_stories.md:
 * - Morning briefing notification at user-configured time (default 7:00 AM)
 * - Evening summary notification at user-configured time (default 6:00 PM)
 * - Notification content includes task count and top priority
 * - Tap opens corresponding briefing screen
 * - Respects quiet hours and user opt-out (briefing_enabled preference)
 *
 * Notification Channels:
 * - CHANNEL_BRIEFING_MORNING: Morning briefing prompts
 * - CHANNEL_BRIEFING_EVENING: Evening summary prompts
 *
 * @see BriefingScheduler for scheduling logic
 * @see ReminderWorker for task-specific notification pattern reference
 */
@HiltWorker
class BriefingNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val clock: Clock
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_BRIEFING_TYPE = "briefing_type"
        const val TAG = "BriefingNotificationWorker"

        // Notification channel IDs
        const val CHANNEL_BRIEFING_MORNING = "prio_briefing_morning"
        const val CHANNEL_BRIEFING_EVENING = "prio_briefing_evening"

        // Notification IDs (fixed per type to replace previous)
        const val NOTIFICATION_ID_MORNING = 9001
        const val NOTIFICATION_ID_EVENING = 9002

        // Request codes
        private const val REQUEST_CODE_MORNING = 9100
        private const val REQUEST_CODE_EVENING = 9200
    }

    enum class BriefingType {
        MORNING,
        EVENING
    }

    override suspend fun doWork(): Result {
        val typeOrdinal = inputData.getInt(KEY_BRIEFING_TYPE, BriefingType.MORNING.ordinal)
        val briefingType = BriefingType.entries.getOrNull(typeOrdinal) ?: BriefingType.MORNING

        return try {
            // Check if notifications are globally enabled
            val notificationsEnabled = userPreferencesRepository.notificationsEnabled.first()
            if (!notificationsEnabled) {
                Timber.d("$TAG: Notifications disabled, skipping ${briefingType.name}")
                return Result.success()
            }

            // Check per-type preference
            when (briefingType) {
                BriefingType.MORNING -> {
                    val briefingEnabled = userPreferencesRepository.briefingEnabled.first()
                    if (!briefingEnabled) {
                        Timber.d("$TAG: Morning briefing disabled, skipping")
                        return Result.success()
                    }
                }
                BriefingType.EVENING -> {
                    val eveningEnabled = userPreferencesRepository.eveningSummaryEnabled.first()
                    if (!eveningEnabled) {
                        Timber.d("$TAG: Evening summary disabled, skipping")
                        return Result.success()
                    }
                }
            }

            // Gather quick stats for notification content
            val timeZone = TimeZone.currentSystemDefault()
            val now = clock.now()
            val today = now.toLocalDateTime(timeZone).date
            val todayStart = today.atStartOfDayIn(timeZone)
            val tomorrowStart = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)

            when (briefingType) {
                BriefingType.MORNING -> sendMorningNotification(todayStart, tomorrowStart)
                BriefingType.EVENING -> sendEveningNotification(todayStart, tomorrowStart)
            }

            Timber.i("$TAG: Sent ${briefingType.name} briefing notification")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to send ${briefingType.name} briefing notification")
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private suspend fun sendMorningNotification(
        todayStart: kotlinx.datetime.Instant,
        tomorrowStart: kotlinx.datetime.Instant
    ) {
        createNotificationChannels()

        // Get task count for today
        val activeTasks = taskRepository.getAllActiveTasksSync()
        val todayTasks = activeTasks.filter { task ->
            task.dueDate?.let { it >= todayStart && it < tomorrowStart } ?: false
        }
        val overdueTasks = activeTasks.filter { task ->
            task.dueDate?.let { it < todayStart } ?: false
        }

        val totalCount = todayTasks.size + overdueTasks.size
        val topPriority = todayTasks
            .filter { it.quadrant == com.prio.core.common.model.EisenhowerQuadrant.DO_FIRST }
            .firstOrNull()

        val contentText = buildString {
            append("☀️ You have $totalCount task${if (totalCount != 1) "s" else ""} today")
            if (overdueTasks.isNotEmpty()) {
                append(" (${overdueTasks.size} overdue)")
            }
            if (topPriority != null) {
                append(". Top: \"${topPriority.title}\"")
            }
        }

        // Intent to open morning briefing
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "morning_briefing")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_MORNING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BRIEFING_MORNING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Good morning! Your day is ready ☀️")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        sendNotificationSafely(NOTIFICATION_ID_MORNING, notification)
    }

    private suspend fun sendEveningNotification(
        todayStart: kotlinx.datetime.Instant,
        tomorrowStart: kotlinx.datetime.Instant
    ) {
        createNotificationChannels()

        // Get completion stats
        val completedToday = taskRepository.getTasksCompletedOnDate(todayStart.toEpochMilliseconds())
        val activeTasks = taskRepository.getAllActiveTasksSync()
        val notDoneToday = activeTasks.filter { task ->
            task.dueDate?.let { it >= todayStart && it < tomorrowStart } ?: false
        }.size

        val contentText = buildString {
            append("🌙 You completed $completedToday task${if (completedToday != 1) "s" else ""} today")
            if (notDoneToday > 0) {
                append(". $notDoneToday still open — review & close your day")
            } else {
                append(". All done! Review your day")
            }
        }

        // Intent to open evening summary
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "evening_summary")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_EVENING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BRIEFING_EVENING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to wrap up! Here's your day 🌙")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        sendNotificationSafely(NOTIFICATION_ID_EVENING, notification)
    }

    private fun sendNotificationSafely(notificationId: Int, notification: android.app.Notification) {
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
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    /**
     * Create notification channels for briefing notifications.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val morningChannel = NotificationChannel(
                CHANNEL_BRIEFING_MORNING,
                "Morning Briefing",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily morning briefing with your day's priorities"
            }

            val eveningChannel = NotificationChannel(
                CHANNEL_BRIEFING_EVENING,
                "Evening Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily evening summary to review and close your day"
            }

            notificationManager.createNotificationChannels(
                listOf(morningChannel, eveningChannel)
            )
        }
    }
}

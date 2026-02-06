package com.prio.app.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.prio.core.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for daily briefing notifications (task 3.4.4).
 *
 * Per CB-001 from 0.3.4_calendar_briefings_user_stories.md:
 * - Schedules morning briefing at user-configured time (default 07:00)
 * - Schedules evening summary at user-configured time (default 18:00)
 * - Respects briefing_enabled preference
 * - Re-schedules when user changes briefing time in settings
 *
 * Uses WorkManager for reliable scheduling across app restarts and device reboots.
 * Pattern follows existing [ReminderScheduler] approach.
 *
 * @see BriefingNotificationWorker for the worker implementation
 * @see UserPreferencesRepository for briefing time preferences
 */
@Singleton
class BriefingScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val clock: Clock
) {
    companion object {
        private const val TAG = "BriefingScheduler"

        // Unique work names
        private const val WORK_MORNING_DAILY = "briefing_morning_daily"
        private const val WORK_EVENING_DAILY = "briefing_evening_daily"
        private const val WORK_MORNING_NEXT = "briefing_morning_next"
        private const val WORK_EVENING_NEXT = "briefing_evening_next"

        // Repeat interval
        private const val REPEAT_INTERVAL_HOURS = 24L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Initialize briefing schedule on app startup.
     *
     * Reads user preferences and schedules both morning and evening briefings.
     * Should be called from Application.onCreate() or an initializer.
     */
    fun initialize() {
        scope.launch {
            try {
                val briefingEnabled = userPreferencesRepository.briefingEnabled.first()
                if (!briefingEnabled) {
                    Timber.d("$TAG: Briefings disabled, cancelling all")
                    cancelAll()
                    return@launch
                }

                val morningTime = userPreferencesRepository.morningBriefingTime.first()
                val eveningTime = userPreferencesRepository.eveningSummaryTime.first()

                scheduleMorningBriefing(morningTime)
                scheduleEveningBriefing(eveningTime)

                Timber.i("$TAG: Initialized briefing schedule (morning=$morningTime, evening=$eveningTime)")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Failed to initialize briefing schedule")
            }
        }
    }

    /**
     * Schedule morning briefing notification.
     *
     * @param timeStr Time string in "HH:mm" format (default "07:00")
     */
    fun scheduleMorningBriefing(timeStr: String = "07:00") {
        val targetTime = parseTime(timeStr) ?: LocalTime(7, 0)
        scheduleNextBriefing(
            briefingType = BriefingNotificationWorker.BriefingType.MORNING,
            targetTime = targetTime,
            uniqueName = WORK_MORNING_NEXT
        )
        Timber.d("$TAG: Scheduled morning briefing at $timeStr")
    }

    /**
     * Schedule evening briefing notification.
     *
     * @param timeStr Time string in "HH:mm" format (default "18:00")
     */
    fun scheduleEveningBriefing(timeStr: String = "18:00") {
        val targetTime = parseTime(timeStr) ?: LocalTime(18, 0)
        scheduleNextBriefing(
            briefingType = BriefingNotificationWorker.BriefingType.EVENING,
            targetTime = targetTime,
            uniqueName = WORK_EVENING_NEXT
        )
        Timber.d("$TAG: Scheduled evening briefing at $timeStr")
    }

    /**
     * Schedule the next occurrence of a briefing notification.
     *
     * Calculates delay from now to the next target time. If the target time
     * has already passed today, schedules for tomorrow.
     */
    private fun scheduleNextBriefing(
        briefingType: BriefingNotificationWorker.BriefingType,
        targetTime: LocalTime,
        uniqueName: String
    ) {
        val timeZone = TimeZone.currentSystemDefault()
        val now = clock.now()
        val todayLocal = now.toLocalDateTime(timeZone).date

        // Calculate target instant for today
        val todayTarget = todayLocal.atTime(targetTime).toInstant(timeZone)

        // If target time already passed today, schedule for tomorrow
        val scheduledInstant = if (todayTarget > now) {
            todayTarget
        } else {
            todayLocal.plus(1, DateTimeUnit.DAY).atTime(targetTime).toInstant(timeZone)
        }

        val delayMillis = (scheduledInstant - now).inWholeMilliseconds

        val inputData = Data.Builder()
            .putInt(
                BriefingNotificationWorker.KEY_BRIEFING_TYPE,
                briefingType.ordinal
            )
            .build()

        val workRequest = OneTimeWorkRequestBuilder<BriefingNotificationWorker>()
            .setInputData(inputData)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(uniqueName)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Timber.d("$TAG: ${briefingType.name} briefing scheduled in ${delayMillis / 1000}s (${scheduledInstant})")
    }

    /**
     * Reschedule briefings when user changes settings.
     *
     * Call this from Settings screen when briefing time changes.
     */
    fun reschedule() {
        initialize()
    }

    /**
     * Cancel all briefing notifications.
     */
    fun cancelAll() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_MORNING_NEXT)
        workManager.cancelUniqueWork(WORK_EVENING_NEXT)
        workManager.cancelUniqueWork(WORK_MORNING_DAILY)
        workManager.cancelUniqueWork(WORK_EVENING_DAILY)
        Timber.d("$TAG: Cancelled all briefing notifications")
    }

    /**
     * Cancel only morning briefing.
     */
    fun cancelMorningBriefing() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_MORNING_NEXT)
    }

    /**
     * Cancel only evening briefing.
     */
    fun cancelEveningBriefing() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_EVENING_NEXT)
    }

    /**
     * Parse "HH:mm" string to LocalTime.
     */
    private fun parseTime(timeStr: String): LocalTime? {
        return try {
            val parts = timeStr.split(":")
            if (parts.size == 2) {
                LocalTime(parts[0].toInt(), parts[1].toInt())
            } else null
        } catch (e: Exception) {
            Timber.w("$TAG: Failed to parse time '$timeStr'")
            null
        }
    }
}

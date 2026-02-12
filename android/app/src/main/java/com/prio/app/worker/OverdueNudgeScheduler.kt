package com.prio.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for periodic overdue task nudge notifications (Milestone 4.2).
 *
 * Enqueues a periodic [OverdueNudgeWorker] that checks for overdue tasks
 * and sends a summary notification. Runs every 4 hours by default.
 *
 * The worker itself respects user preferences (notifications_enabled,
 * overdue_alerts_enabled, quiet hours) and will no-op if disabled.
 *
 * Should be initialized from [PrioApplication.onCreate].
 */
@Singleton
class OverdueNudgeScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "OverdueNudgeScheduler"
        private const val WORK_NAME = "overdue_nudge_periodic"
        private const val REPEAT_INTERVAL_HOURS = 4L
    }

    /**
     * Initialize the periodic overdue nudge check.
     *
     * Uses KEEP policy so the existing schedule survives app restarts
     * without resetting the timer.
     */
    fun initialize() {
        val workRequest = PeriodicWorkRequestBuilder<OverdueNudgeWorker>(
            REPEAT_INTERVAL_HOURS, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        Timber.i("$TAG: Initialized periodic overdue nudge check (every ${REPEAT_INTERVAL_HOURS}h)")
    }

    /**
     * Cancel the periodic overdue nudge check.
     */
    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Timber.d("$TAG: Cancelled periodic overdue nudge check")
    }
}

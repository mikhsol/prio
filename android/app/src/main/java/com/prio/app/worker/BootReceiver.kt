package com.prio.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Receiver for BOOT_COMPLETED to reschedule reminders after device reboot.
 *
 * Per GAP-C02: The RECEIVE_BOOT_COMPLETED permission was declared but no
 * receiver was registered. Without this, all scheduled reminders and alarms
 * are lost when the device reboots.
 *
 * Implements TM-009: Smart Reminders must survive device reboot.
 * Also re-initializes briefing schedule and overdue nudge checks.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var briefingScheduler: BriefingScheduler

    @Inject
    lateinit var overdueNudgeScheduler: OverdueNudgeScheduler

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Timber.d("BootReceiver: Device rebooted, rescheduling all notifications")

        val pendingResult = goAsync()
        scope.launch {
            try {
                reminderScheduler.rescheduleAllReminders()
                briefingScheduler.initialize()
                overdueNudgeScheduler.initialize()
                Timber.d("BootReceiver: All notifications rescheduled successfully")
            } catch (e: Exception) {
                Timber.e(e, "BootReceiver: Failed to reschedule notifications")
            } finally {
                pendingResult.finish()
            }
        }
    }
}

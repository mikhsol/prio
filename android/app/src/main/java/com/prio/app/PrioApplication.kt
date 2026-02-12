package com.prio.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.prio.app.worker.BriefingScheduler
import com.prio.app.worker.OverdueNudgeScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Prio Application class with Hilt dependency injection and WorkManager.
 * 
 * Your Private Productivity AI
 */
@HiltAndroidApp
class PrioApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var briefingScheduler: BriefingScheduler

    @Inject
    lateinit var overdueNudgeScheduler: OverdueNudgeScheduler
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG 
                else android.util.Log.INFO
            )
            .build()
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Plant production tree that logs to Crashlytics
            Timber.plant(CrashReportingTree())
        }
        
        Timber.d("Prio Application started")

        // Initialize notification schedulers
        briefingScheduler.initialize()
        overdueNudgeScheduler.initialize()
        Timber.d("Notification schedulers initialized")
    }
    
    /**
     * Production logging tree that reports to Firebase Crashlytics.
     * Per GAP-C01: Crash reporting must be active in production builds.
     */
    private class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                    .log("${tag ?: "Prio"}: $message")
                t?.let {
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                        .recordException(it)
                }
            }
        }
    }
}

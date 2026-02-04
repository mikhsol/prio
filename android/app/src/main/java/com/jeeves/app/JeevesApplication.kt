package com.jeeves.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Jeeves Application class with Hilt dependency injection.
 * 
 * Your Private Productivity AI
 */
@HiltAndroidApp
class JeevesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Plant production tree that logs to Crashlytics
            Timber.plant(CrashReportingTree())
        }
        
        Timber.d("Jeeves Application started")
    }
    
    /**
     * Production logging tree that reports to Firebase Crashlytics.
     */
    private class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Log errors to Crashlytics
            if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
                // Firebase Crashlytics logging will be added when configured
                // FirebaseCrashlytics.getInstance().log("$tag: $message")
                // t?.let { FirebaseCrashlytics.getInstance().recordException(it) }
            }
        }
    }
}

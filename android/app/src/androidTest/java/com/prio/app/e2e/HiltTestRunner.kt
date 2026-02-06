package com.prio.app.e2e

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom AndroidJUnitRunner for Hilt-based E2E tests.
 *
 * Replaces the application class with [HiltTestApplication] so that
 * Hilt can inject test dependencies into E2E test classes.
 *
 * Configure in build.gradle.kts:
 * ```
 * defaultConfig {
 *     testInstrumentationRunner = "com.prio.app.e2e.HiltTestRunner"
 * }
 * ```
 *
 * OR keep the default runner and use @HiltAndroidTest + HiltAndroidRule
 * which auto-generates the test application.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}

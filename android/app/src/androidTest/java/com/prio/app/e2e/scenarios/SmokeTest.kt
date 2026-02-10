package com.prio.app.e2e.scenarios

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ActivityScenario
import com.prio.app.MainActivity
import com.prio.core.data.preferences.UserPreferencesRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import javax.inject.Inject

/**
 * Minimal smoke test to verify the Compose hierarchy is accessible.
 * Uses createEmptyComposeRule + manual ActivityScenario.launch to control timing.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SmokeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            userPreferencesRepository.setOnboardingCompleted(true)
        }
    }

    @Test
    fun composeHierarchyExists() {
        Log.d("PrioTest", "=== SmokeTest: launching Activity ===")
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Log.d("PrioTest", "=== SmokeTest: Activity launched, waitForIdle ===")
        composeRule.waitForIdle()
        Log.d("PrioTest", "=== SmokeTest: waitForIdle done, checking root ===")
        composeRule.onRoot().assertIsDisplayed()
        Log.d("PrioTest", "=== SmokeTest: SUCCESS! ===")
        scenario.close()
    }
}

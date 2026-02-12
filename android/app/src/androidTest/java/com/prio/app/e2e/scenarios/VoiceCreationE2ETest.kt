package com.prio.app.e2e.scenarios

import android.Manifest
import com.prio.app.e2e.BaseE2ETest
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

/**
 * E2E regression test for the voice creation bug fix.
 *
 * **Bug**: Tapping the microphone button shows "Getting ready..." indefinitely.
 * The SpeechRecognizer hangs because:
 * 1. EXTRA_PREFER_OFFLINE = true with no offline model downloaded
 * 2. createOnDeviceSpeechRecognizer() stalls when the model isn't ready
 *
 * **Fix**: Added a 5-second initialization timeout (INIT_TIMEOUT_MS) in
 * VoiceInputManager.startListening(). If onReadyForSpeech doesn't fire
 * within the timeout, the manager falls back to the default (cloud-capable)
 * SpeechRecognizer without EXTRA_PREFER_OFFLINE.
 *
 * **Test strategy**: These tests verify the voice input flow transitions
 * past the "Getting ready..." (Initializing) state within a reasonable
 * timeout. The actual recognition result depends on device capabilities,
 * so we verify state transitions rather than speech content.
 *
 * RECORD_AUDIO permission is auto-granted via [GrantPermissionRule].
 */
@HiltAndroidTest
class VoiceCreationE2ETest : BaseE2ETest() {

    @get:Rule(order = 3)
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Test
    fun voiceInput_doesNotStickOnGettingReady() {
        // GIVEN: user is on the task list and opens quick capture
        nav.goToTasks()
        nav.tapFab()
        quickCapture.assertSheetVisible()

        // WHEN: user taps the microphone button to start voice input
        quickCapture.tapVoiceInput()
        composeRule.waitForIdle()

        // THEN: "Getting ready..." appears briefly but transitions
        //       to either Listening or Error within 15 seconds.
        //       Before the fix, it would stay on "Getting ready..." forever.
        quickCapture.assertNotStuckOnGettingReady(timeoutMs = 15_000)

        // Cleanup: dismiss the voice overlay and sheet
        quickCapture.dismiss()
        quickCapture.assertSheetDismissed()
    }

    @Test
    fun voiceInput_canRecoverFromError() {
        // GIVEN: user opens quick capture and taps mic
        nav.goToTasks()
        nav.tapFab()
        quickCapture.assertSheetVisible()

        // WHEN: voice input is activated
        quickCapture.tapVoiceInput()
        composeRule.waitForIdle()

        // THEN: voice moves past Initializing (either Listening or Error)
        quickCapture.assertNotStuckOnGettingReady(timeoutMs = 15_000)

        // AND: if an error occurred, user can tap "Type Instead" to
        //      fall back to keyboard input
        val hasError = composeRule.onAllNodes(
            hasText("Try Again", substring = true)
        ).fetchSemanticsNodes().isNotEmpty()

        if (hasError) {
            quickCapture.tapTypeInstead()
            quickCapture.assertVoiceOverlayDismissed()

            // Verify text input still works after voice error
            quickCapture.typeTaskText("Fallback task from voice error")
            quickCapture.submitInput()
            quickCapture.waitForAiClassification()
            quickCapture.tapCreateTask()
            quickCapture.assertSheetDismissed()
            taskList.assertTaskDisplayed("Fallback task from voice error")
        } else {
            // Voice is working (Listening state) — just dismiss
            quickCapture.dismiss()
            quickCapture.assertSheetDismissed()
        }
    }

    @Test
    fun voiceInput_canRetryAfterError() {
        // GIVEN: user opens quick capture and taps mic
        nav.goToTasks()
        nav.tapFab()
        quickCapture.assertSheetVisible()

        // WHEN: voice input is activated
        quickCapture.tapVoiceInput()
        composeRule.waitForIdle()

        // Wait for state to move past Initializing
        quickCapture.assertNotStuckOnGettingReady(timeoutMs = 15_000)

        // THEN: if error, user can retry and should not get stuck again
        val hasError = composeRule.onAllNodes(
            hasText("Try Again", substring = true)
        ).fetchSemanticsNodes().isNotEmpty()

        if (hasError) {
            // Tap "Try Again" — should not get stuck on "Getting ready..." again
            composeRule.onAllNodes(
                hasText("Try Again", substring = true)
            ).onFirst().performClick()
            composeRule.waitForIdle()

            // The retry should also transition past Initializing
            quickCapture.assertNotStuckOnGettingReady(timeoutMs = 15_000)
        }

        // Cleanup
        quickCapture.dismiss()
        quickCapture.assertSheetDismissed()
    }

    @Test
    fun voiceInput_dismissDuringInitDoesNotCrash() {
        // GIVEN: user opens quick capture
        nav.goToTasks()
        nav.tapFab()
        quickCapture.assertSheetVisible()

        // WHEN: user taps mic and immediately dismisses
        quickCapture.tapVoiceInput()
        composeRule.waitForIdle()

        // Don't wait for transition — dismiss immediately during "Getting ready..."
        quickCapture.dismiss()

        // THEN: sheet dismisses cleanly without crash
        quickCapture.assertSheetDismissed()

        // AND: app is still responsive — can reopen quick capture
        nav.tapFab()
        quickCapture.assertSheetVisible()
        quickCapture.dismiss()
        quickCapture.assertSheetDismissed()
    }
}

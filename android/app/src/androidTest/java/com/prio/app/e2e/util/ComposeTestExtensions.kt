package com.prio.app.e2e.util

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.prio.app.MainActivity

/**
 * Custom Compose test extensions for Prio E2E tests.
 *
 * Provides:
 * - Retry logic for flaky emulator interactions
 * - Wait-for-element helpers
 * - Assertion chaining
 */

/**
 * Wait until a node matching this interaction is displayed.
 * Useful for elements that appear after async operations (AI classification, DB writes).
 *
 * @param timeoutMs Maximum wait time in milliseconds
 * @throws AssertionError if element is not displayed within timeout
 */
fun SemanticsNodeInteraction.waitUntilDisplayed(
    rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>,
    timeoutMs: Long = 5_000L
): SemanticsNodeInteraction {
    rule.waitUntil(timeoutMillis = timeoutMs) {
        try {
            assertIsDisplayed()
            true
        } catch (_: AssertionError) {
            false
        }
    }
    return this
}

/**
 * Assert that the node exists and is displayed, with a descriptive message on failure.
 */
fun SemanticsNodeInteraction.assertDisplayedWithMessage(
    message: String
): SemanticsNodeInteraction {
    try {
        assertIsDisplayed()
    } catch (e: AssertionError) {
        throw AssertionError("$message: ${e.message}", e)
    }
    return this
}

/**
 * Assert that a node is displayed and enabled (tappable).
 */
fun SemanticsNodeInteraction.assertDisplayedAndEnabled(): SemanticsNodeInteraction {
    assertIsDisplayed()
    assertIsEnabled()
    return this
}

/**
 * Assert that a node is displayed but disabled (not tappable).
 */
fun SemanticsNodeInteraction.assertDisplayedAndDisabled(): SemanticsNodeInteraction {
    assertIsDisplayed()
    assertIsNotEnabled()
    return this
}

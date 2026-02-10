package com.prio.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo

/**
 * Robot for briefing screens: MorningBriefingScreen and EveningSummaryScreen.
 *
 * MorningBriefingScreen:
 * - Title: "Morning Briefing"
 * - Back: contentDescription "Back"
 * - Hero card: contentDescription "Morning briefing for {date}. You have {n} urgent tasks..."
 * - Start My Day CTA: contentDescription "Start my day. Double tap to dismiss briefing."
 *   - Text: "Start My Day →"
 *
 * EveningSummaryScreen:
 * - Title: "Evening Summary"
 * - Back: contentDescription "Back"
 * - Header card: contentDescription "Evening summary. You completed {n} of {n} tasks."
 * - Close Day CTA: contentDescription "Close day and plan tomorrow. Double tap to confirm."
 *   - Text: "✓ Close Day & Plan Tomorrow" / "✓ Day closed"
 * - Settings icon: contentDescription "Settings"
 * - Day closed state: "DAY COMPLETE", "Day Closed!", "See you tomorrow morning ☀️"
 * - Incomplete task actions: "Move to tomorrow", "Reschedule", "Drop"
 * - Disconnect message: "🏠 Time to disconnect! You've earned your rest."
 *
 * Known issues:
 * - DEF-016: EveningSummary "Close Day" animation has no exit path
 * - DEF-014: MorningBriefing hardcoded colors break dark mode
 * - DEF-015: MorningBriefing collectAsState() should be collectAsStateWithLifecycle()
 */
class BriefingRobot(
    private val rule: ComposeTestRule
) {

    // =========================================================================
    // Morning Briefing actions
    // =========================================================================

    fun tapStartMyDay() {
        rule.onNodeWithText("Start My Day", substring = true)
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    // =========================================================================
    // Evening Summary actions
    // =========================================================================

    fun tapCloseDay() {
        rule.onNodeWithText("Close Day", substring = true)
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    fun tapMoveTomorrow(taskTitle: String) {
        // First find the task, then tap its "Move to tomorrow" action
        rule.onNodeWithText(taskTitle, substring = true)
            .performScrollTo()
        rule.onNodeWithText("Move to tomorrow")
            .performClick()
        rule.waitForIdle()
    }

    fun tapReschedule(taskTitle: String) {
        rule.onNodeWithText(taskTitle, substring = true)
            .performScrollTo()
        rule.onNodeWithText("Reschedule")
            .performClick()
        rule.waitForIdle()
    }

    fun tapDrop(taskTitle: String) {
        rule.onNodeWithText(taskTitle, substring = true)
            .performScrollTo()
        rule.onNodeWithText("Drop")
            .performClick()
        rule.waitForIdle()
    }

    fun tapSettings() {
        rule.onNodeWithContentDescription("Settings")
            .performClick()
        rule.waitForIdle()
    }

    fun tapBack() {
        rule.onNodeWithContentDescription("Back")
            .performClick()
        rule.waitForIdle()
    }

    // =========================================================================
    // Assertions
    // =========================================================================

    fun assertMorningBriefingVisible() {
        rule.onNodeWithText("Morning Briefing")
            .assertIsDisplayed()
    }

    fun assertEveningSummaryVisible() {
        rule.onNodeWithText("Evening Summary")
            .assertIsDisplayed()
    }

    fun assertStartMyDayVisible() {
        rule.onNodeWithText("Start My Day", substring = true)
            .assertIsDisplayed()
    }

    fun assertCloseDayVisible() {
        rule.onNodeWithText("Close Day", substring = true)
            .assertIsDisplayed()
    }

    fun assertDayClosed() {
        rule.onNodeWithText("DAY COMPLETE", substring = true)
            .assertIsDisplayed()
    }

    fun assertDisconnectMessage() {
        rule.onNodeWithText("Time to disconnect", substring = true)
            .assertIsDisplayed()
    }

    fun assertCompletionCount(completed: Int, total: Int) {
        rule.onNodeWithContentDescription(
            "You completed $completed of $total tasks",
            substring = true
        ).assertIsDisplayed()
    }

    fun assertHeroCard() {
        rule.onNodeWithContentDescription("Morning briefing for", substring = true)
            .assertIsDisplayed()
    }

    fun assertUrgentTaskCount(count: Int) {
        rule.onNodeWithContentDescription("$count urgent", substring = true)
            .assertIsDisplayed()
    }
}

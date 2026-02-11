package com.prio.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo

/**
 * Robot for the CalendarScreen.
 *
 * UI elements:
 * - Title: "Calendar"
 * - Go to today: contentDescription "Go to today"
 * - Previous/Next week: contentDescription "Previous week" / "Next week"
 * - Day chips: contentDescription "{dayName}, {month} {dayNumber}. Has events" / "No events"
 * - Timeline event blocks: contentDescription "{title}. {startTime} to {endTime}. ..."
 * - Untimed task cards: contentDescription "{quadrantEmoji} {title}. {dueText}"
 * - "Schedule" section header
 * - "Tasks Without Time" section
 * - Calendar connect CTA: "Connect Your Calendar" / "Connect" / "Skip for Now"
 * - "Read-only access · Data stays on device"
 *
 * MeetingCard (in Calendar):
 * - contentDescription: "{title}. From {startTime} to {endTime}. ..."
 * - "NOW" badge for ongoing meetings
 */
class CalendarRobot(
    private val rule: ComposeTestRule
) {

    // =========================================================================
    // Actions
    // =========================================================================

    fun goToToday() {
        rule.onNodeWithContentDescription("Go to today")
            .performClick()
        rule.waitForIdle()
    }

    fun goToPreviousWeek() {
        rule.onNodeWithContentDescription("Previous week")
            .performClick()
        rule.waitForIdle()
    }

    fun goToNextWeek() {
        rule.onNodeWithContentDescription("Next week")
            .performClick()
        rule.waitForIdle()
    }

    fun selectDay(dayName: String) {
        rule.onNodeWithContentDescription(dayName, substring = true)
            .performClick()
        rule.waitForIdle()
    }

    fun tapMeeting(title: String) {
        rule.onNodeWithContentDescription(title, substring = true)
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    fun tapConnectCalendar() {
        rule.onNodeWithText("Connect")
            .performClick()
        rule.waitForIdle()
    }

    fun tapSkipCalendarConnect() {
        rule.onNodeWithText("Skip for Now")
            .performClick()
        rule.waitForIdle()
    }

    // =========================================================================
    // Assertions
    // =========================================================================

    fun assertScreenVisible() {
        // Calendar screen has unique week navigation controls
        rule.onNodeWithContentDescription("Previous week")
            .assertIsDisplayed()
    }

    fun assertDaySelected(dayName: String) {
        rule.onNodeWithContentDescription(dayName, substring = true)
            .assertIsDisplayed()
    }

    fun assertMeetingDisplayed(title: String) {
        rule.onNodeWithContentDescription(title, substring = true)
            .assertIsDisplayed()
    }

    fun assertOngoingMeeting() {
        rule.onNodeWithText("NOW")
            .assertIsDisplayed()
    }

    fun assertCalendarConnectPrompt() {
        rule.onNodeWithText("Connect Your Calendar")
            .assertIsDisplayed()
    }

    fun assertNoEvents() {
        rule.onNodeWithText("No events", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    fun assertUntimedTasksSection() {
        rule.onNodeWithText("Tasks Without Time", substring = true)
            .assertIsDisplayed()
    }

    fun assertPrivacyNote() {
        rule.onNodeWithText("Read-only access", substring = true)
            .assertIsDisplayed()
    }

    // =========================================================================
    // View Mode Switching
    // =========================================================================

    fun switchToDayView() {
        rule.onNodeWithContentDescription("Day view")
            .performClick()
        rule.waitForIdle()
    }

    fun switchToWeekView() {
        rule.onNodeWithContentDescription("Week view")
            .performClick()
        rule.waitForIdle()
    }

    fun switchToMonthView() {
        rule.onNodeWithContentDescription("Month view")
            .performClick()
        rule.waitForIdle()
    }

    fun assertViewModeSwitcherVisible() {
        rule.onNodeWithContentDescription("Day view")
            .assertIsDisplayed()
        rule.onNodeWithContentDescription("Week view")
            .assertIsDisplayed()
        rule.onNodeWithContentDescription("Month view")
            .assertIsDisplayed()
    }

    fun assertWeekViewVisible() {
        // Week view shows day summary cards with meeting/task counts or "No events".
        // Wait for content to load — check for any of the expected texts.
        rule.waitUntil(timeoutMillis = 5_000) {
            val hasMeetingText = rule.onAllNodesWithText("meeting", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
            val hasTaskText = rule.onAllNodesWithText("task", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
            val hasNoEvents = rule.onAllNodesWithText("No events", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
            hasMeetingText || hasTaskText || hasNoEvents
        }
    }

    fun assertMonthViewVisible() {
        // Month mode shows month navigation bar with Previous/Next month buttons.
        // Wait for the month navigation to render after mode switch.
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("Previous month", substring = false)
                .fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithContentDescription("Previous month")
                    .fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun goToPreviousMonth() {
        rule.onNodeWithContentDescription("Previous month")
            .performClick()
        rule.waitForIdle()
    }

    fun goToNextMonth() {
        rule.onNodeWithContentDescription("Next month")
            .performClick()
        rule.waitForIdle()
    }
}

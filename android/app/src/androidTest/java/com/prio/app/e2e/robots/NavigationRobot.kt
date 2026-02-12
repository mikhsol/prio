package com.prio.app.e2e.robots

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso

/**
 * Robot for navigating between main app screens via bottom navigation.
 *
 * Matches the PrioBottomNavigation component which uses defaultNavItems:
 * - Today (route: "today")
 * - Tasks (route: "tasks")
 * - Goals (route: "goals")
 * - Calendar (route: "calendar")
 *
 * Nav item contentDescription pattern: "{Label}. Selected" / "{Label}. Not selected"
 * We use the ". " suffix pattern to disambiguate from other nodes containing the label text.
 *
 * FAB is centered in bottom nav with contentDescription = "Add new task".
 * Since TaskListScreen's duplicate FAB was removed (Bug 5 fix), there is now
 * only one "Add new task" FAB on screen at a time.
 */
class NavigationRobot(
    private val rule: ComposeTestRule
) {

    // =========================================================================
    // Navigation actions
    // =========================================================================

    fun goToToday() {
        rule.onAllNodesWithContentDescription("Today. ", substring = true)
            .onFirst()
            .performClick()
        rule.waitForIdle()
    }

    fun goToTasks() {
        rule.onAllNodesWithContentDescription("Tasks. ", substring = true)
            .onFirst()
            .performClick()
        rule.waitForIdle()
    }

    fun goToGoals() {
        rule.onAllNodesWithContentDescription("Goals. ", substring = true)
            .onFirst()
            .performClick()
        rule.waitForIdle()
    }

    fun goToCalendar() {
        rule.onAllNodesWithContentDescription("Calendar. ", substring = true)
            .onFirst()
            .performClick()
        rule.waitForIdle()
    }

    fun goToSettings() {
        rule.onAllNodesWithContentDescription("Settings")
            .onFirst()
            .performClick()
        rule.waitForIdle()
    }

    fun tapFab() {
        // Use onLast() to get the PrioBottomNavigation center FAB,
        // which directly sets showQuickCapture=true (no ViewModel indirection).
        // The TaskListScreen FAB (first in tree) goes through a coroutine chain
        // that may not complete within waitForIdle().
        rule.onAllNodesWithContentDescription("Add new task")
            .onLast()
            .performClick()
        rule.waitForIdle()
    }

    fun pressBack() {
        Espresso.pressBack()
        rule.waitForIdle()
    }

    // =========================================================================
    // Assertions
    // =========================================================================

    fun assertBottomNavVisible() {
        rule.onAllNodesWithContentDescription("Today. ", substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    fun assertBottomNavHidden() {
        val nodes = rule.onAllNodesWithContentDescription("Today. ", substring = true)
            .fetchSemanticsNodes()
        assert(nodes.isEmpty()) {
            "Bottom nav should be hidden but found ${nodes.size} node(s)"
        }
    }

    fun assertOnTodayScreen() {
        rule.onAllNodesWithContentDescription("Today. Selected")
            .onFirst()
            .assertIsDisplayed()
    }

    fun assertOnTasksScreen() {
        rule.onAllNodesWithContentDescription("Tasks. Selected")
            .onFirst()
            .assertIsDisplayed()
    }

    fun assertOnGoalsScreen() {
        rule.onAllNodesWithContentDescription("Goals. Selected")
            .onFirst()
            .assertIsDisplayed()
    }

    fun assertOnCalendarScreen() {
        rule.onAllNodesWithContentDescription("Calendar. Selected")
            .onFirst()
            .assertIsDisplayed()
    }

    fun assertFabVisible() {
        rule.onAllNodesWithContentDescription("Add new task")
            .onFirst()
            .assertIsDisplayed()
    }

    fun assertSettingsButtonVisible() {
        rule.onAllNodesWithContentDescription("Settings")
            .onFirst()
            .assertIsDisplayed()
    }

    fun assertOnMoreScreen() {
        rule.onNodeWithText("More")
            .assertIsDisplayed()
    }

    // =========================================================================
    // Snackbar assertions (Scaffold-level)
    // =========================================================================

    /**
     * Assert that a snackbar with the given message is currently displayed.
     */
    fun assertSnackbarDisplayed(message: String) {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(message, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(message, substring = true)
            .assertIsDisplayed()
    }

    /**
     * Assert that a snackbar with the given message auto-dismisses
     * within the specified timeout. Material3 SnackbarDuration.Short is ~4s.
     *
     * @param message    text expected in the snackbar
     * @param timeoutMs  maximum wait time for the snackbar to disappear
     */
    fun assertSnackbarAutoDismisses(message: String, timeoutMs: Long = 10_000L) {
        rule.waitUntil(timeoutMillis = timeoutMs) {
            rule.onAllNodesWithText(message, substring = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }
}

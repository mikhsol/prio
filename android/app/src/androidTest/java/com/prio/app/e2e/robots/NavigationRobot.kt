package com.prio.app.e2e.robots

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
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
 * Note: TaskListScreen also has an "Add new task" FAB, so we use onAllNodes().onFirst().
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
}

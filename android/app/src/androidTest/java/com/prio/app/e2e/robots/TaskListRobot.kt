package com.prio.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.prio.app.MainActivity

/**
 * Robot for the Tasks list screen (TaskListScreen).
 *
 * Screen structure:
 * - TopAppBar: "Tasks"
 * - Search toggle (contentDescription: "Search tasks" / "Close search")
 * - Show completed toggle (contentDescription: "Show/Hide completed tasks")
 * - Filter chips (contentDescription: "{filter.name} filter")
 * - LazyColumn with TaskSection headers + TaskCard items
 * - FAB is in PrioBottomNavigation, not directly on this screen
 *
 * TaskCard contentDescription:
 *   "{title}. Priority: {quadrant.label}. Completed. Overdue. Due: {dueText}. Reminder set. Goal: {goalName}"
 *
 * TaskCard checkbox contentDescription:
 *   "Mark {task.title} as complete" / "Completed. Double tap to mark incomplete"
 */
class TaskListRobot(
    private val rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
) {

    // =========================================================================
    // Actions
    // =========================================================================

    fun openSearch() {
        rule.onNodeWithContentDescription("Search tasks")
            .performClick()
        rule.waitForIdle()
    }

    fun closeSearch() {
        rule.onNodeWithContentDescription("Close search")
            .performClick()
        rule.waitForIdle()
    }

    fun toggleShowCompleted() {
        rule.onNode(
            hasContentDescription("Show completed tasks") or
                hasContentDescription("Hide completed tasks")
        ).performClick()
        rule.waitForIdle()
    }

    fun selectFilter(filterName: String) {
        rule.onNodeWithContentDescription("$filterName filter", substring = true)
            .performClick()
        rule.waitForIdle()
    }

    fun tapTask(title: String) {
        rule.onNodeWithText(title, substring = true)
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    fun completeTask(title: String) {
        rule.onNodeWithContentDescription("Mark $title as complete")
            .performClick()
        rule.waitForIdle()
    }

    fun swipeTaskLeft(title: String) {
        rule.onNodeWithText(title, substring = true)
            .performTouchInput { swipeLeft() }
        rule.waitForIdle()
    }

    fun swipeTaskRight(title: String) {
        rule.onNodeWithText(title, substring = true)
            .performTouchInput { swipeRight() }
        rule.waitForIdle()
    }

    // =========================================================================
    // Assertions
    // =========================================================================

    fun assertScreenVisible() {
        // Use Search tasks icon which is unique to TaskListScreen
        // (avoids matching the 'Tasks' bottom nav label)
        rule.onNodeWithContentDescription("Search tasks")
            .assertIsDisplayed()
    }

    fun assertTaskDisplayed(title: String) {
        rule.onNodeWithText(title, substring = true)
            .assertIsDisplayed()
    }

    fun assertTaskNotDisplayed(title: String) {
        rule.onNodeWithText(title, substring = true)
            .assertDoesNotExist()
    }

    fun assertEmptyState() {
        // EmptyState component with headline text
        rule.onNode(
            hasText("No tasks yet", substring = true, ignoreCase = true) or
                hasText("Add your first task", substring = true, ignoreCase = true)
        ).assertIsDisplayed()
    }

    fun assertSectionVisible(sectionTitle: String) {
        rule.onNodeWithText(sectionTitle, substring = true)
            .assertIsDisplayed()
    }

    fun assertOverdueIndicator(title: String) {
        rule.onNodeWithContentDescription("$title", substring = true)
            .assertIsDisplayed()
        // Overdue should appear in the card's content description
        rule.onNode(
            hasContentDescription("Overdue", substring = true)
        ).assertIsDisplayed()
    }

    fun assertTaskCount(sectionTitle: String, expectedCount: Int) {
        // Section headers show count in parentheses
        rule.onNodeWithText("$sectionTitle ($expectedCount)", substring = true)
            .assertIsDisplayed()
    }

    fun assertFilterSelected(filterName: String) {
        rule.onNodeWithContentDescription("$filterName filter, selected", substring = true)
            .assertIsDisplayed()
    }

    fun assertErrorState() {
        rule.onNodeWithText("Couldn't load your tasks", substring = true)
            .assertIsDisplayed()
    }
}

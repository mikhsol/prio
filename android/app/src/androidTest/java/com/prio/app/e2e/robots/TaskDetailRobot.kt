package com.prio.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.prio.app.MainActivity

/**
 * Robot for the TaskDetailSheet modal.
 *
 * Opened by tapping a task card from TaskListScreen.
 *
 * UI elements:
 * - Checkbox: contentDescription "Mark as complete" / "Completed. Double tap to mark incomplete"
 * - Overflow menu: contentDescription "More options"
 *   - Menu items: "Edit", "Done Editing", "Duplicate", "Copy to Clipboard", "Delete"
 * - Task title: placeholder "Task title"
 * - Subtasks section: "Subtasks" prefix
 *   - Add subtask: contentDescription "Add subtask"
 *   - Delete subtask: contentDescription "Delete subtask"
 * - Drag handle: contentDescription "Drag to reorder" (ReorderableList)
 *
 * Note: "Add subtask" button currently does nothing (known defect DEF-011).
 */
class TaskDetailRobot(
    private val rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
) {

    // =========================================================================
    // Actions
    // =========================================================================

    fun tapComplete() {
        rule.onNode(
            hasContentDescription("Mark as complete") or
                hasContentDescription("Completed", substring = true)
        ).performClick()
        rule.waitForIdle()
    }

    fun openOverflowMenu() {
        rule.onNodeWithContentDescription("More options")
            .performClick()
        rule.waitForIdle()
    }

    fun tapEdit() {
        rule.onNodeWithText("Edit")
            .performClick()
        rule.waitForIdle()
    }

    fun tapDoneEditing() {
        rule.onNodeWithText("Done Editing")
            .performClick()
        rule.waitForIdle()
    }

    fun tapDuplicate() {
        rule.onNodeWithText("Duplicate")
            .performClick()
        rule.waitForIdle()
    }

    fun tapCopyToClipboard() {
        rule.onNodeWithText("Copy to Clipboard")
            .performClick()
        rule.waitForIdle()
    }

    fun tapDelete() {
        rule.onAllNodesWithText("Delete")
            .onFirst()
            .performClick()
        rule.waitForIdle()
    }

    /**
     * Confirm deletion in the PrioConfirmDialog.
     * Called after [tapDelete] which opens the dialog.
     */
    fun confirmDelete() {
        // PrioConfirmDialog has a "Delete" confirm button
        // Wait for dialog to appear, then tap the confirm button
        rule.waitUntil(timeoutMillis = 3_000) {
            rule.onAllNodesWithText("Delete")
                .fetchSemanticsNodes().size >= 1
        }
        rule.onAllNodesWithText("Delete")
            .onFirst()
            .performClick()
        rule.waitForIdle()
    }

    fun editTitle(newTitle: String) {
        rule.onNodeWithText("Task title", substring = true)
            .performTextClearance()
        rule.onNodeWithText("Task title", substring = true)
            .performTextInput(newTitle)
        rule.waitForIdle()
    }

    fun tapAddSubtask() {
        rule.onNodeWithContentDescription("Add subtask")
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    // =========================================================================
    // Assertions
    // =========================================================================

    fun assertSheetVisible() {
        rule.onNodeWithContentDescription("More options")
            .assertIsDisplayed()
    }

    fun assertTaskTitle(title: String) {
        rule.onNodeWithText(title, substring = true)
            .assertIsDisplayed()
    }

    fun assertCompleted() {
        rule.onNodeWithContentDescription("Completed", substring = true)
            .assertIsDisplayed()
    }

    fun assertNotCompleted() {
        rule.onNodeWithContentDescription("Mark as complete")
            .assertIsDisplayed()
    }

    fun assertSubtasksSection() {
        rule.onNodeWithText("Subtasks", substring = true)
            .assertIsDisplayed()
    }

    fun assertAiExplanation(text: String) {
        rule.onNodeWithText(text, substring = true)
            .assertIsDisplayed()
    }
}

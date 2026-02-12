package com.prio.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput

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
    private val rule: ComposeTestRule
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
        // Wait for PrioConfirmDialog to appear (title: "Delete Task?")
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("Delete Task?")
                .fetchSemanticsNodes().isNotEmpty()
        }
        // The dialog has "Cancel" and "Delete" buttons.
        // Find all "Delete" text nodes — the dialog confirm button is the last one
        // (after the dialog title "Delete Task?" and potentially the menu item).
        rule.onAllNodesWithText("Delete")
            .onLast()
            .performClick()
        rule.waitForIdle()
    }

    fun editTitle(newTitle: String, currentTitle: String? = null) {
        // In edit mode, the OutlinedTextField shows current title (not placeholder).
        // Match on current title text within an editable field.
        if (currentTitle != null) {
            rule.onNode(
                hasText(currentTitle, substring = true) and hasSetTextAction()
            )
                .performScrollTo()
                .performTextClearance()
        } else {
            rule.onNodeWithText("Task title", substring = true)
                .performScrollTo()
                .performTextClearance()
        }
        rule.waitForIdle()
        // After clearing, the same node (OutlinedTextField) still has SetTextAction.
        // The placeholder "Task title" becomes visible.
        rule.onNode(
            hasText("Task title", substring = true) and hasSetTextAction()
        )
            .performScrollTo()
            .performTextInput(newTitle)
        rule.waitForIdle()
    }

    fun tapAddSubtask() {
        rule.onNodeWithContentDescription("Add subtask")
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    /**
     * Fill in and confirm the Add Subtask dialog.
     */
    fun addSubtaskViaDialog(subtaskTitle: String) {
        tapAddSubtask()
        // Wait for AlertDialog to appear ("Add Subtask" title)
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("Add Subtask")
                .fetchSemanticsNodes().isNotEmpty()
        }
        // Type into the "Subtask title" placeholder field
        rule.onNode(hasText("Subtask title") and hasSetTextAction())
            .performTextInput(subtaskTitle)
        rule.waitForIdle()
        rule.onNodeWithText("Add")
            .performClick()
        rule.waitForIdle()
    }

    /**
     * Tap the task title text to enter edit mode (Bug 4 fix).
     */
    fun tapTitleToEdit(title: String) {
        rule.onNodeWithText(title, substring = true)
            .performClick()
        rule.waitForIdle()
    }

    /**
     * Tap the "Save" button (Bug 1 fix — replaces "Complete" in edit mode).
     */
    fun tapSave() {
        rule.onNodeWithText("Save")
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    /**
     * Tap a property row by its label text to trigger the "Change" action.
     * The entire PropertyRow is clickable (not just the "Change" TextButton).
     *
     * Note: We click the label text directly because there are multiple "Change"
     * TextButtons in the sheet (QuadrantSection also has one), so matching
     * by "Change" text is ambiguous.
     */
    fun tapChangeForProperty(propertyLabel: String) {
        rule.onNodeWithText(propertyLabel, substring = true)
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    /**
     * Type into the notes field.
     */
    fun typeNotes(notes: String) {
        rule.onNode(
            hasText("Add notes", substring = true) and hasSetTextAction()
        )
            .performScrollTo()
            .performTextInput(notes)
        rule.waitForIdle()
    }

    /**
     * Clear and type into the notes field (for editing existing notes).
     */
    fun editNotes(currentNotes: String, newNotes: String) {
        rule.onNode(
            hasText(currentNotes, substring = true) and hasSetTextAction()
        )
            .performScrollTo()
            .performTextClearance()
        rule.waitForIdle()
        rule.onNode(
            hasText("Add notes", substring = true) and hasSetTextAction()
        )
            .performScrollTo()
            .performTextInput(newNotes)
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

    fun assertSaveButtonVisible() {
        rule.onNodeWithText("Save")
            .performScrollTo()
            .assertIsDisplayed()
    }

    fun assertCompleteButtonVisible() {
        rule.onNodeWithText("Complete")
            .performScrollTo()
            .assertIsDisplayed()
    }

    fun assertSaveButtonNotVisible() {
        val nodes = rule.onAllNodesWithText("Save").fetchSemanticsNodes()
        assert(nodes.isEmpty()) {
            "Save button should not be visible but found ${nodes.size} node(s)"
        }
    }

    fun assertInEditMode() {
        // In edit mode, the title OutlinedTextField is displayed
        rule.onNode(hasSetTextAction() and hasText("Task title", substring = true))
            .assertIsDisplayed()
    }

    fun assertTitleEditable(currentTitle: String) {
        // Title field is an OutlinedTextField with SetTextAction containing the current title
        rule.onNode(hasSetTextAction() and hasText(currentTitle, substring = true))
            .assertIsDisplayed()
    }

    fun assertNotesContain(text: String) {
        rule.onNodeWithText(text, substring = true)
            .assertIsDisplayed()
    }

    fun assertDatePickerVisible() {
        // Material3 DatePickerDialog has "OK" and "Cancel" buttons
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("OK")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun assertGoalPickerVisible() {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("Link Goal")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Assert the goal picker subtitle is shown (rich bottom sheet).
     */
    fun assertGoalPickerSubtitle() {
        rule.onNodeWithText("Linking tasks to goals helps track progress", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Assert a goal row shows category and progress in the goal picker.
     */
    fun assertGoalRowDetails(goalTitle: String, progress: String) {
        rule.onNodeWithText(goalTitle, substring = true)
            .assertIsDisplayed()
        rule.onNodeWithText(progress, substring = true)
            .assertIsDisplayed()
    }

    /**
     * Assert the "Remove goal link" option is visible in the goal picker.
     */
    fun assertRemoveGoalLinkVisible() {
        rule.onNodeWithText("Remove goal link", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Tap "Remove goal link" in the goal picker to unlink the goal.
     */
    fun tapRemoveGoalLink() {
        rule.onNodeWithText("Remove goal link", substring = true)
            .performClick()
        rule.waitForIdle()
    }

    /**
     * Assert the goal picker empty state is shown.
     */
    fun assertGoalPickerEmpty() {
        rule.onNodeWithText("No active goals yet", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Assert the linked goal is displayed on the task detail property row
     * with category info (rich format).
     */
    fun assertLinkedGoalDisplayed(goalTitle: String) {
        rule.onNodeWithText(goalTitle, substring = true)
            .assertIsDisplayed()
    }

    fun assertSnackbarMessage(text: String) {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(text, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(text, substring = true)
            .assertIsDisplayed()
    }

    fun assertSubtaskDisplayed(title: String) {
        rule.onNodeWithText(title, substring = true)
            .assertIsDisplayed()
    }

    /**
     * Dismiss the date picker by tapping Cancel.
     */
    fun dismissDatePicker() {
        rule.onNodeWithText("Cancel")
            .performClick()
        rule.waitForIdle()
    }

    /**
     * Confirm the date picker by tapping OK.
     */
    fun confirmDatePicker() {
        rule.onNodeWithText("OK")
            .performClick()
        rule.waitForIdle()
    }

    /**
     * Dismiss the goal picker bottom sheet by pressing back.
     * The goal picker is a ModalBottomSheet (not an AlertDialog),
     * so we dismiss by pressing the back button.
     */
    fun dismissGoalPicker() {
        androidx.test.espresso.Espresso.pressBack()
        rule.waitForIdle()
    }

    /**
     * Select a goal from the goal picker by title.
     */
    fun selectGoalInPicker(goalTitle: String) {
        rule.onNodeWithText(goalTitle, substring = true)
            .performClick()
        rule.waitForIdle()
    }

    // =========================================================================
    // Time Picker (two-step date → time flow)
    // =========================================================================

    /**
     * Assert that the time picker dialog is visible after selecting a date.
     * The time picker is shown in an AlertDialog with title "Set Time".
     */
    fun assertTimePickerVisible() {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("Set Time")
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Set Time").assertIsDisplayed()
    }

    /**
     * Confirm the time picker by tapping OK (saves date + time).
     */
    fun confirmTimePicker() {
        rule.onNodeWithText("OK")
            .performClick()
        rule.waitForIdle()
    }

    /**
     * Skip the time picker by tapping Skip (saves date only).
     */
    fun skipTimePicker() {
        rule.onNodeWithText("Skip")
            .performClick()
        rule.waitForIdle()
    }

    /**
     * Assert that the date picker shows "Next" button (step 1 of two-step flow).
     */
    fun assertDatePickerHasNextButton() {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("Next")
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Next").assertIsDisplayed()
    }

    /**
     * Tap "Next" in the date picker to proceed to time picker.
     */
    fun tapDatePickerNext() {
        rule.onNodeWithText("Next")
            .performClick()
        rule.waitForIdle()
    }
}

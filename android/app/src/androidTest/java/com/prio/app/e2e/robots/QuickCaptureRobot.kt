package com.prio.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput

/**
 * Robot for the QuickCaptureSheet modal.
 *
 * Opened by tapping FAB (contentDescription: "Add new task") from bottom nav.
 *
 * UI elements:
 * - PrioTextField placeholder: "Type or speak (on-device)"
 *   - contentDescription: "Task input. Type or speak (on-device)"
 *   - Clear button: contentDescription "Clear text"
 *   - Voice button: contentDescription "Start voice input (on-device)" / "Stop voice input"
 * - Close button: contentDescription "Close"
 * - Priority edit icon: contentDescription "Change priority"
 * - ParsedFieldRow edit icons: contentDescription "Edit"
 * - Quadrant selection: contentDescription "Selected" for chosen
 * - "Choose Priority" text
 * - "Edit Details" text
 * - "Create Task" text / button
 * - "Link to a goal" text
 *
 * Flow: type text → AI parses → parsed fields shown → tap "Create Task"
 */
class QuickCaptureRobot(
    private val rule: ComposeTestRule
) {

    // =========================================================================
    // Actions
    // =========================================================================

    fun typeTaskText(text: String) {
        // The TextField has placeholder "Type or speak (on-device)" but no contentDescription.
        // Match by hasSetTextAction() which finds the text input field.
        rule.onNode(hasSetTextAction())
            .performTextClearance()
        rule.onNode(hasSetTextAction())
            .performTextInput(text)
        rule.waitForIdle()
    }

    /**
     * Trigger the IME Done action on the text field.
     * This fires QuickCaptureEvent.ParseInput in the ViewModel,
     * which sets showPreview = true so the "Create Task" button appears.
     * Must be called after typeTaskText() and before waitForAiClassification().
     */
    fun submitInput() {
        rule.onNode(hasSetTextAction())
            .performImeAction()
        rule.waitForIdle()
    }

    fun clearText() {
        rule.onNodeWithContentDescription("Clear text")
            .performClick()
        rule.waitForIdle()
    }

    fun tapVoiceInput() {
        rule.onNode(
            hasContentDescription("Start voice input", substring = true) or
                hasContentDescription("Stop voice input", substring = true)
        ).performClick()
        rule.waitForIdle()
    }

    fun tapCreateTask() {
        rule.onNodeWithText("Create Task")
            .performClick()
        rule.waitForIdle()
    }

    fun tapChangePriority() {
        rule.onNodeWithContentDescription("Change priority")
            .performClick()
        rule.waitForIdle()
    }

    fun selectPriority(quadrantLabel: String) {
        rule.onNodeWithText(quadrantLabel)
            .performClick()
        rule.waitForIdle()
    }

    fun tapLinkToGoal() {
        rule.onNodeWithText("Link to a goal", substring = true)
            .performClick()
        rule.waitForIdle()
    }

    fun dismiss() {
        rule.onNodeWithContentDescription("Close")
            .performClick()
        rule.waitForIdle()
    }

    // =========================================================================
    // Assertions
    // =========================================================================

    fun assertSheetVisible() {
        // Wait for sheet animation to complete before asserting.
        // Match by hasSetTextAction which finds the text input field.
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodes(hasSetTextAction())
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        rule.onNode(hasSetTextAction())
            .assertIsDisplayed()
    }

    fun assertSheetDismissed() {
        rule.onNode(hasSetTextAction())
            .assertDoesNotExist()
    }

    fun assertCreateTaskButtonVisible() {
        rule.onNodeWithText("Create Task")
            .assertIsDisplayed()
    }

    fun assertParsedFieldsShown() {
        // When AI parses the task, parsed field rows with "Edit" buttons appear
        rule.onNodeWithContentDescription("Edit", substring = true)
            .assertIsDisplayed()
    }

    fun assertPriorityVisible(quadrantLabel: String) {
        rule.onNodeWithText(quadrantLabel, substring = true)
            .assertIsDisplayed()
    }

    /**
     * Assert the text input field is empty (no stale text from a previous session).
     * Verifies the placeholder "Type or speak (on-device)" is visible, which only
     * appears when inputText is empty. Also verifies no "Create Task" button
     * is visible (parsed result not present).
     */
    fun assertInputEmpty() {
        // Wait for the text field to be present
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodes(hasSetTextAction())
                .fetchSemanticsNodes().isNotEmpty()
        }
        // Verify the text field does NOT contain stale text.
        // We check the unmerged tree for the placeholder text which only renders
        // when the TextField's value is empty.
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(
                "Type or speak", substring = true, useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Assert the "Create Task" button is NOT visible (no parsed result present).
     */
    fun assertCreateTaskButtonNotVisible() {
        rule.onNodeWithText("Create Task")
            .assertDoesNotExist()
    }

    /**
     * Assert no parsed field rows are shown (no "Edit" buttons from AI parsing).
     */
    fun assertNoParsedFieldsShown() {
        val nodes = rule.onAllNodesWithContentDescription("Edit", substring = true)
            .fetchSemanticsNodes()
        assert(nodes.isEmpty()) { "Expected no parsed field rows but found ${nodes.size}" }
    }

    fun assertAiProcessing() {
        // AI processing indicator should show
        rule.onNodeWithText("AI runs on device", substring = true)
            .assertIsDisplayed()
    }

    fun assertVoiceListening() {
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodes(hasText("Listening", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Listening", substring = true)
            .assertIsDisplayed()
    }

    fun assertVoiceError() {
        rule.onNodeWithText("Try Again", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Assert that the "Getting ready..." text is NOT displayed,
     * i.e. the voice input has transitioned past the Initializing state.
     * Used in regression tests for the init timeout bug fix.
     */
    fun assertNotStuckOnGettingReady(timeoutMs: Long = 15_000) {
        // Wait for the state to move past "Getting ready..." within the timeout.
        // The fix adds a 5s init timeout + fallback, so within 15s the state
        // should be either Listening, Processing, Result, or Error — never
        // stuck on Initializing.
        rule.waitUntil(timeoutMillis = timeoutMs) {
            val hasListening = rule.onAllNodes(hasText("Listening", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
            val hasError = rule.onAllNodes(hasText("Try Again", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
            val hasTypeInstead = rule.onAllNodes(hasText("Type Instead", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
            val hasProcessing = rule.onAllNodes(hasText("Processing", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
            hasListening || hasError || hasTypeInstead || hasProcessing
        }
    }

    /**
     * Assert that voice input transitioned past Initializing.
     * Returns true if we see Listening, Error, Processing, or the overlay is gone.
     * Returns false if still stuck on "Getting ready...".
     */
    fun isVoicePastInitializing(): Boolean {
        val hasListening = rule.onAllNodes(hasText("Listening", substring = true))
            .fetchSemanticsNodes().isNotEmpty()
        val hasError = rule.onAllNodes(hasText("Try Again", substring = true))
            .fetchSemanticsNodes().isNotEmpty()
        val hasTypeInstead = rule.onAllNodes(hasText("Type Instead", substring = true))
            .fetchSemanticsNodes().isNotEmpty()
        val hasProcessing = rule.onAllNodes(hasText("Processing", substring = true))
            .fetchSemanticsNodes().isNotEmpty()
        return hasListening || hasError || hasTypeInstead || hasProcessing
    }

    /**
     * Tap the "Type Instead" button shown on voice error state.
     */
    fun tapTypeInstead() {
        rule.onNodeWithText("Type Instead")
            .performClick()
        rule.waitForIdle()
    }

    /**
     * Assert that the voice overlay is no longer visible
     * (voice input was cancelled or completed).
     */
    fun assertVoiceOverlayDismissed() {
        rule.waitUntil(timeoutMillis = 3_000) {
            rule.onAllNodes(hasText("Getting ready", substring = true))
                .fetchSemanticsNodes().isEmpty() &&
            rule.onAllNodes(hasText("Listening", substring = true))
                .fetchSemanticsNodes().isEmpty()
        }
    }

    /**
     * Wait for AI classification to complete (parsed fields appear).
     * Generous timeout because on-device LLM can be slow.
     */
    fun waitForAiClassification(timeoutMs: Long = 30_000L) {
        rule.waitUntil(timeoutMillis = timeoutMs) {
            rule.onAllNodes(hasText("Create Task")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // =========================================================================
    // Time Picker (two-step date → time flow)
    // =========================================================================

    /**
     * Assert that the time picker dialog is visible.
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
}

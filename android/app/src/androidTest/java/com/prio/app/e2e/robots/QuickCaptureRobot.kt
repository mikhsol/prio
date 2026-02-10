package com.prio.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    fun assertAiProcessing() {
        // AI processing indicator should show
        rule.onNodeWithText("AI runs on device", substring = true)
            .assertIsDisplayed()
    }

    fun assertVoiceListening() {
        rule.onNodeWithText("Listening", substring = true)
            .assertIsDisplayed()
    }

    fun assertVoiceError() {
        rule.onNodeWithText("Try Again", substring = true)
            .assertIsDisplayed()
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
}

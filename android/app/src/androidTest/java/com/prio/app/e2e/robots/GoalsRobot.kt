package com.prio.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso

/**
 * Robot for the Goals screens: GoalsListScreen, CreateGoalScreen, GoalDetailScreen.
 *
 * GoalsListScreen:
 * - Title: "My Goals"
 * - FAB: contentDescription "Create new goal" / "Maximum goals reached"
 * - Overview card: contentDescription "Goals overview: {n} active, {n} on track, {n} at risk"
 * - Section collapse/expand: contentDescription "Collapse" / "Expand"
 * - Empty state: "No Goals Yet" / "Create First Goal" button
 * - GoalCard: contentDescription "{title}. Category: {category.label}. {n} percent complete..."
 *   - Overflow: contentDescription "More options"
 *
 * CreateGoalScreen:
 * - Title: "Create Goal"
 * - Back: contentDescription "Back"
 * - Input: "What do you want to achieve?" / placeholder "My goal is to…"
 * - "Refine with AI" button
 * - "Next: Timeline" / "Back" / "Create Goal 🎯" buttons
 * - "Skip AI — I'll write my own"
 * - Milestones: "Add milestone" icon, "Remove" icon
 * - "📅 Target Date", "🏁 Milestones", "🔔 Weekly Check-in Reminder"
 *
 * GoalDetailScreen:
 * - Progress ring: contentDescription "{n} percent complete, status: {status.displayName}"
 */
class GoalsRobot(
    private val rule: ComposeTestRule
) {

    // =========================================================================
    // Goals List actions
    // =========================================================================

    fun tapCreateGoalFab() {
        rule.onNodeWithContentDescription("Create new goal")
            .performClick()
        rule.waitForIdle()
    }

    fun tapGoal(title: String) {
        rule.onNodeWithText(title, substring = true)
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    fun tapGoalOverflowMenu(title: String) {
        // Find the More options button in the goal card context
        // Since multiple "More options" may exist, we target by the goal title first
        rule.onNodeWithText(title, substring = true)
            .performScrollTo()
        rule.onNodeWithContentDescription("More options")
            .performClick()
        rule.waitForIdle()
    }

    fun tapCreateFirstGoal() {
        rule.onNodeWithText("Create First Goal", useUnmergedTree = true)
            .performClick()
        rule.waitForIdle()
    }

    // =========================================================================
    // Create Goal actions
    // =========================================================================

    fun typeGoalTitle(title: String) {
        rule.onNodeWithText("My goal is to", substring = true)
            .performTextInput(title)
        rule.waitForIdle()
    }

    fun tapRefineWithAi() {
        rule.onNodeWithText("Refine with AI")
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    fun tapSkipAi() {
        rule.onNodeWithText("Skip AI", substring = true)
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    fun tapNextTimeline() {
        rule.onNodeWithText("Next: Timeline")
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    fun tapBack() {
        rule.onNodeWithContentDescription("Back")
            .performClick()
        rule.waitForIdle()
    }

    fun tapCreateGoalButton() {
        rule.onNodeWithText("Create Goal \uD83C\uDFAF")
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    fun tapAddMilestone() {
        rule.onNodeWithContentDescription("Add milestone")
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    // =========================================================================
    // Celebration Overlay (post-creation)
    // =========================================================================

    fun assertCelebrationVisible() {
        rule.onNodeWithText("Goal Created!")
            .assertIsDisplayed()
    }

    fun tapBackToGoals() {
        rule.onNodeWithText("Back to Goals")
            .performClick()
        rule.waitForIdle()
    }

    fun tapAddFirstTask() {
        rule.onNodeWithText("Add First Task", substring = true)
            .performClick()
        rule.waitForIdle()
    }

    fun tapViewGoalDetails() {
        rule.onNodeWithText("View Goal Details")
            .performClick()
        rule.waitForIdle()
    }

    // =========================================================================
    // Assertions
    // =========================================================================

    fun assertListScreenVisible() {
        // Wait for goals list to load — check for the top bar title "Goals"
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("Goals", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun assertFabVisible() {
        rule.onNodeWithContentDescription("Create new goal")
            .assertIsDisplayed()
    }

    fun assertFabNotVisible() {
        rule.onNodeWithContentDescription("Create new goal")
            .assertDoesNotExist()
    }

    fun assertCreateScreenVisible() {
        rule.onNodeWithText("Create Goal")
            .assertIsDisplayed()
    }

    fun assertEmptyState() {
        rule.onNodeWithText("No Goals Yet")
            .assertIsDisplayed()
    }

    fun assertGoalDisplayed(title: String) {
        rule.onNodeWithText(title, substring = true)
            .assertIsDisplayed()
    }

    fun assertGoalNotDisplayed(title: String) {
        rule.onNodeWithText(title, substring = true)
            .assertDoesNotExist()
    }

    fun assertOverviewCard() {
        rule.onNodeWithContentDescription("Goals overview", substring = true)
            .assertIsDisplayed()
    }

    fun assertMaxGoalsReached() {
        rule.onNodeWithContentDescription("Maximum goals reached")
            .assertIsDisplayed()
    }

    fun assertProgressRing(percentText: String) {
        // Match "N percent complete" to target only the progress ring,
        // not the breakdown row ("Progress breakdown: milestones X percent, tasks Y percent").
        rule.onNodeWithContentDescription("$percentText complete", substring = true)
            .assertIsDisplayed()
    }

    fun assertGoalCount(expectedActive: Int) {
        rule.onNodeWithContentDescription("$expectedActive active", substring = true)
            .assertIsDisplayed()
    }

    // =========================================================================
    // Create Goal — Milestone Input helpers
    // =========================================================================

    /**
     * Type a milestone title into the milestone input field and tap the Add button.
     * Used on the CreateGoalScreen Step 3 (Timeline & Milestones).
     */
    fun typeMilestoneAndAdd(title: String) {
        // Wait for the input field to be available (may recompose after previous add)
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodes(hasTestTag("milestone_input"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(300) // allow recomposition to settle
        val field = rule.onNodeWithTag("milestone_input")
        field.performScrollTo()
        field.performTextReplacement(title)
        rule.waitForIdle()
        // Close the software keyboard so it doesn't obstruct the Add button
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        Thread.sleep(300)
        rule.onNodeWithContentDescription("Add milestone")
            .performClick()
        rule.waitForIdle()
    }

    /**
     * Assert a milestone chip with the given title is displayed on the CreateGoalScreen.
     */
    fun assertMilestoneChip(title: String) {
        rule.onNodeWithText(title, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Type a milestone title into the input field WITHOUT tapping the Add button.
     * Used to reproduce Bug 3 (secondary): pending input text should be
     * auto-saved when the user taps "Create Goal" directly.
     */
    fun typeMilestoneWithoutAdd(title: String) {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodes(hasTestTag("milestone_input"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(300)
        val field = rule.onNodeWithTag("milestone_input")
        field.performScrollTo()
        field.performTextReplacement(title)
        rule.waitForIdle()
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
    }

    /**
     * Remove all existing milestone chips (e.g. AI-suggested ones) on the CreateGoalScreen.
     * Taps the "Remove" icon on each chip until none remain.
     */
    fun clearAllMilestoneChips() {
        var chips = rule.onAllNodesWithContentDescription("Remove").fetchSemanticsNodes()
        while (chips.isNotEmpty()) {
            rule.onAllNodesWithContentDescription("Remove")[0]
                .performScrollTo()
                .performClick()
            rule.waitForIdle()
            Thread.sleep(200)
            chips = rule.onAllNodesWithContentDescription("Remove").fetchSemanticsNodes()
        }
    }

    // =========================================================================
    // Goal Detail — Tab and Milestone helpers
    // =========================================================================

    /**
     * Select a tab on the GoalDetailScreen (e.g. "📋 Tasks", "🏁 Milestones", "📊 Analytics").
     */
    fun tapDetailTab(tabLabel: String) {
        rule.onNodeWithText(tabLabel, substring = true)
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }

    /**
     * Assert that a milestone with the given title is visible in the detail Milestones tab.
     * Uses hasClickAction() to disambiguate from the AI insight card which may
     * also contain the milestone title in its suggestion text.
     */
    fun assertMilestoneDisplayed(title: String) {
        rule.onNode(hasText(title, substring = true) and hasClickAction())
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Toggle (complete/uncomplete) a milestone in the detail Milestones tab by clicking its row.
     * Uses hasClickAction() to disambiguate from the AI insight card which may
     * also contain the milestone title in its suggestion text.
     */
    fun tapMilestoneToggle(title: String) {
        rule.onNode(hasText(title, substring = true) and hasClickAction())
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
    }
}

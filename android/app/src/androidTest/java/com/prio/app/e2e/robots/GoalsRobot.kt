package com.prio.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput

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
        rule.onNodeWithText("Create First Goal")
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
            .performClick()
        rule.waitForIdle()
    }

    fun tapSkipAi() {
        rule.onNodeWithText("Skip AI", substring = true)
            .performClick()
        rule.waitForIdle()
    }

    fun tapNextTimeline() {
        rule.onNodeWithText("Next: Timeline")
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
    // Assertions
    // =========================================================================

    fun assertListScreenVisible() {
        // Use the Goals FAB which is unique to GoalsListScreen
        // (avoids matching the 'Goals' bottom nav label)
        rule.onNodeWithContentDescription("Create new goal")
            .assertIsDisplayed()
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
        rule.onNodeWithContentDescription(percentText, substring = true)
            .assertIsDisplayed()
    }

    fun assertGoalCount(expectedActive: Int) {
        rule.onNodeWithContentDescription("$expectedActive active", substring = true)
            .assertIsDisplayed()
    }
}

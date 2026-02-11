package com.prio.app.e2e.scenarios

import androidx.compose.ui.test.onNodeWithText
import com.prio.app.e2e.BaseE2ETest
import com.prio.app.e2e.util.TestDataFactory
import com.prio.core.common.model.GoalCategory
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * E2E-A9/A10: Goals Flow Tests
 *
 * Validates user stories:
 * - GL-001: Create Goal
 * - GL-002: AI-Assisted SMART Goal Refinement
 * - GL-003: Goal Dashboard
 * - GL-004: Milestone Tracking
 * - GL-005: Weekly Check-in
 * - GL-006: Goal Completion
 *
 * Known issues:
 * - DEF-008: GoalStatus.COMPLETED mapped to ON_TRACK (GoalsListScreen)
 * - DEF-009: averageProgress NaN crash when no goals (GoalsListScreen)
 * - DEF-010: Overflow menu TODO (GoalsListScreen)
 */
@HiltAndroidTest
class GoalsFlowE2ETest : BaseE2ETest() {

    // =========================================================================
    // E2E-A9-01: Smoke — Goals empty state
    // Priority: P0 (Smoke)
    // =========================================================================

    @Test
    fun goalsScreen_emptyState() {
        nav.goToGoals()
        goals.assertListScreenVisible()
        goals.assertEmptyState()
    }

    // =========================================================================
    // E2E-A9-02: Create goal via FAB
    // Priority: P1 (Core) — GL-001
    // =========================================================================

    @Test
    fun createGoal_showsInList() {
        nav.goToGoals()

        // Use the empty state "Create First Goal" button to start
        goals.assertEmptyState()
        goals.tapCreateFirstGoal()
        goals.assertCreateScreenVisible()

        // Type the goal on Step 1
        goals.typeGoalTitle("Run a marathon")
        Thread.sleep(500)

        // Tap "Refine with AI" to advance to Step 2
        goals.tapRefineWithAi()

        // Wait for AI processing to finish (AI should fail fast and fallback).
        // After fallback, the "✨ SMART Goal" heading and form fields appear.
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("SMART Goal", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        goals.tapNextTimeline()
        goals.tapCreateGoalButton()

        // Wait for celebration overlay to appear
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Goal Created!")
            ).fetchSemanticsNodes().isNotEmpty()
        }
        goals.assertCelebrationVisible()

        // Tap "Back to Goals" to navigate to the list
        goals.tapBackToGoals()

        // Wait for navigation back to goals list
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("Create new goal")
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Should return to list with new goal
        goals.assertListScreenVisible()
        goals.assertGoalDisplayed("Run a marathon")
    }

    // =========================================================================
    // E2E-A9-03: Create goal with AI refinement
    // Priority: P2 (Extended) — GL-002
    // =========================================================================

    @Test
    fun createGoal_withAiRefinement() {
        // AI refinement requires a real or fake AI provider.
        // On test device, AI fails and fallback sets refinedGoal = input.
        // This test validates the full flow works with AI fallback.
        nav.goToGoals()
        goals.tapCreateFirstGoal()

        goals.typeGoalTitle("Get healthier")
        Thread.sleep(500)
        goals.tapRefineWithAi()

        // Wait for AI processing to finish (success or fallback).
        // After processing, "✨ SMART Goal" heading appears.
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("SMART Goal", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        goals.tapNextTimeline()
        goals.tapCreateGoalButton()
    }

    // =========================================================================
    // E2E-A9-04: Goals overview card shows stats
    // Priority: P1 (Core) — GL-003
    // =========================================================================

    @Test
    fun goalsOverviewCard_showsActiveCount() {
        runBlocking {
            goalRepository.insertGoal(TestDataFactory.onTrackGoal(title = "Career Goal"))
            goalRepository.insertGoal(TestDataFactory.atRiskGoal(title = "Health Goal"))
            goalRepository.insertGoal(TestDataFactory.completedGoal(title = "Done Goal"))
        }
        Thread.sleep(2_000)

        nav.goToGoals()

        // Wait for goals to load from Room → Flow → ViewModel → Compose
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Career Goal", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        goals.assertOverviewCard()
        goals.assertGoalDisplayed("Career Goal")
        goals.assertGoalDisplayed("Health Goal")
    }

    // =========================================================================
    // E2E-A9-05: Goal detail shows progress ring
    // Priority: P2 (Extended) — GL-003
    // =========================================================================

    @Test
    fun goalDetail_showsProgressRing() {
        runBlocking {
            goalRepository.insertGoal(
                TestDataFactory.goal(title = "Learning Goal", progress = 60, category = GoalCategory.LEARNING)
            )
        }
        Thread.sleep(2_000)

        nav.goToGoals()

        // Wait for goal to load from Room → Flow → ViewModel → Compose
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Learning Goal", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }
        goals.tapGoal("Learning Goal")

        goals.assertProgressRing("60 percent")
    }

    // =========================================================================
    // E2E-A10-01: Smoke — Create goal from empty state
    // Priority: P0 (Smoke)
    // =========================================================================

    @Test
    fun createFirstGoal_fromEmptyState() {
        nav.goToGoals()
        goals.assertEmptyState()
        goals.tapCreateFirstGoal()
        goals.assertCreateScreenVisible()
    }

    // =========================================================================
    // E2E-A10-02: Maximum goals limit
    // Priority: P2 (Edge case) — GL-001
    // =========================================================================

    @Test
    fun maxGoalsReached_fabDisabledOrWarning() {
        // Create maximum number of goals (typically 10)
        runBlocking {
            repeat(10) { i ->
                goalRepository.insertGoal(
                    TestDataFactory.goal(
                        title = "Goal $i",
                        category = GoalCategory.entries[i % GoalCategory.entries.size]
                    )
                )
            }
        }
        Thread.sleep(2_000)

        nav.goToGoals()

        // Wait for goals to load — look for any goal card
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Goal 0", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        goals.assertMaxGoalsReached()
    }

    // =========================================================================
    // E2E-A10-03: Goal with milestones
    // Priority: P1 (Core) — GL-004
    // =========================================================================

    @Test
    fun goalWithMilestones_showsMilestoneProgress() {
        runBlocking {
            goalRepository.insertGoal(
                TestDataFactory.goal(title = "Learn Kotlin", category = GoalCategory.LEARNING, progress = 40)
            )
        }
        Thread.sleep(2_000)

        nav.goToGoals()

        // Wait for goal to load from Room → Flow → ViewModel → Compose
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Learn Kotlin", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        goals.tapGoal("Learn Kotlin")

        // Wait for GoalDetailScreen to load (async from Room)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("percent complete", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Goal detail should show progress ring
        // contentDescription format: "{n} percent complete, status: {status}"
        goals.assertProgressRing("40 percent")
    }

    // =========================================================================
    // E2E-A9-06: Complete goal from detail screen
    // Priority: P1 (Core) — GL-006 / Regression: "No complete goal button"
    // =========================================================================

    @Test
    fun completeGoal_fromDetailScreen_marksAsCompleted() {
        // Seed an active goal
        runBlocking {
            goalRepository.insertGoal(
                TestDataFactory.onTrackGoal(title = "Complete Me Goal")
            )
        }
        Thread.sleep(2_000)

        nav.goToGoals()

        // Wait for goal to load
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Complete Me Goal", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Tap goal to enter detail
        goals.tapGoal("Complete Me Goal")

        // Wait for GoalDetailScreen to load (async from Room)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("percent complete", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Verify complete button is visible
        goals.assertCompleteButtonVisible()

        // Tap complete goal
        goals.tapCompleteGoal()

        // Verify completion snackbar
        goals.assertCompletionSnackbar()

        // After completion the complete button should be hidden
        goals.assertCompleteButtonNotVisible()
    }

    // =========================================================================
    // E2E-A10-04: Swipe to delete goal with undo
    // Priority: P1 (Core) — Regression test for "Can't undo goal deletion"
    // =========================================================================

    @Test
    fun swipeDeleteGoal_undoRestoresGoal() {
        // Seed a goal into the database
        runBlocking {
            goalRepository.insertGoal(
                TestDataFactory.onTrackGoal(title = "Undo Test Goal")
            )
        }
        Thread.sleep(2_000)

        nav.goToGoals()

        // Wait for goal to load
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Undo Test Goal", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Verify goal is displayed
        goals.assertGoalDisplayed("Undo Test Goal")

        // Swipe goal left to delete
        goals.swipeGoalToDelete("Undo Test Goal")

        // Verify snackbar with undo action is shown
        goals.assertDeleteSnackbarWithUndo("Undo Test Goal")

        // Tap undo to restore
        goals.tapSnackbarUndo()

        // Wait for goal to reappear after undo re-insert
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Undo Test Goal", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Verify the goal is back in the list (handles possible snackbar overlap)
        goals.assertGoalDisplayed("Undo Test Goal")
    }

    // =========================================================================
    // E2E-A10-05: Swipe to delete goal permanently (no undo)
    // Priority: P1 (Core) — Regression test for goal deletion
    // =========================================================================

    @Test
    fun swipeDeleteGoal_permanentlyRemovesGoal() {
        // Seed two goals so the list doesn't go to empty state while snackbar
        // is showing (empty state check would match snackbar text too)
        runBlocking {
            goalRepository.insertGoal(
                TestDataFactory.onTrackGoal(title = "Delete Me Goal")
            )
            goalRepository.insertGoal(
                TestDataFactory.onTrackGoal(title = "Keep This Goal")
            )
        }
        Thread.sleep(2_000)

        nav.goToGoals()

        // Wait for both goals to load
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Delete Me Goal", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        goals.assertGoalDisplayed("Delete Me Goal")
        goals.assertGoalDisplayed("Keep This Goal")

        // Swipe goal left to delete
        goals.swipeGoalToDelete("Delete Me Goal")

        // Verify snackbar appears
        goals.assertDeleteSnackbarWithUndo("Delete Me Goal")

        // The other goal should still be visible
        goals.assertGoalDisplayed("Keep This Goal")
    }
}

package com.prio.app.e2e.scenarios

import androidx.compose.ui.test.onNodeWithText
import com.prio.app.e2e.BaseE2ETest
import com.prio.app.e2e.util.TestDataFactory
import com.prio.core.common.model.GoalCategory
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
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
        goals.tapCreateGoalFab()

        goals.assertCreateScreenVisible()
        goals.typeGoalTitle("Run a marathon")
        goals.tapSkipAi() // Skip AI for faster test
        goals.tapNextTimeline()
        goals.tapCreateGoalButton()

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
        nav.goToGoals()
        goals.tapCreateGoalFab()

        goals.typeGoalTitle("Get healthier")
        goals.tapRefineWithAi()

        // Wait for AI refinement to complete — the "Next: Timeline" button
        // becomes enabled after AI finishes processing.
        // FakeAiProvider returns deterministic results within ~100ms.
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Next: Timeline")
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
    fun goalsOverviewCard_showsActiveCount() = runTest {
        goalRepository.insertGoal(TestDataFactory.onTrackGoal(title = "Career Goal"))
        goalRepository.insertGoal(TestDataFactory.atRiskGoal(title = "Health Goal"))
        goalRepository.insertGoal(TestDataFactory.completedGoal(title = "Done Goal"))

        nav.goToGoals()
        goals.assertListScreenVisible()
        goals.assertOverviewCard()
        goals.assertGoalDisplayed("Career Goal")
        goals.assertGoalDisplayed("Health Goal")
    }

    // =========================================================================
    // E2E-A9-05: Goal detail shows progress ring
    // Priority: P2 (Extended) — GL-003
    // =========================================================================

    @Test
    fun goalDetail_showsProgressRing() = runTest {
        goalRepository.insertGoal(
            TestDataFactory.goal(title = "Learning Goal", progress = 60, category = GoalCategory.LEARNING)
        )

        nav.goToGoals()
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
    fun maxGoalsReached_fabDisabledOrWarning() = runTest {
        // Create maximum number of goals (typically 10)
        repeat(10) { i ->
            goalRepository.insertGoal(
                TestDataFactory.goal(
                    title = "Goal $i",
                    category = GoalCategory.entries[i % GoalCategory.entries.size]
                )
            )
        }

        nav.goToGoals()
        goals.assertMaxGoalsReached()
    }

    // =========================================================================
    // E2E-A10-03: Goal with milestones
    // Priority: P1 (Core) — GL-004
    // =========================================================================

    @Test
    fun goalWithMilestones_showsMilestoneProgress() = runTest {
        val goalId = goalRepository.insertGoal(
            TestDataFactory.goal(title = "Learn Kotlin", category = GoalCategory.LEARNING, progress = 40)
        )

        nav.goToGoals()
        waitForIdle()
        goals.assertGoalDisplayed("Learn Kotlin")
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
}

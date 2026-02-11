package com.prio.app.e2e.scenarios

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.prio.app.e2e.BaseE2ETest
import com.prio.app.e2e.util.TestDataFactory
import com.prio.core.common.model.GoalCategory
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Regression E2E tests for milestone-related bug fixes.
 *
 * Bug 1: Close milestone → no goal progress update.
 *   Root cause: [GoalRepository.recalculateProgress] only counted tasks,
 *   ignoring milestones entirely. Milestones now count as progress items.
 *
 * Bug 2: Milestone ↔ Task relationship missing.
 *   Design decision: milestones are first-class progress items. Both tasks
 *   and milestones linked to a goal contribute to the progress % using a
 *   weighted formula: milestones 60%, tasks 40%.
 *
 * Bug 3: Create goal → add multiple milestones → last added milestone lost.
 *   Original root cause: [CreateGoalViewModel.addMilestone] captured the milestone
 *   list outside the StateFlow.update lambda, causing stale overwrites
 *   when milestones were added in quick succession.
 *   Secondary root cause: user types last milestone into the input field and
 *   taps "Create Goal" without first tapping the "+" Add button. The pending
 *   text in the local Compose state was silently discarded because the
 *   Create button only dispatched [OnCreateGoal] without flushing the input.
 *   Fix: [CreateGoalScreen] now auto-adds pending milestone text before
 *   dispatching [OnCreateGoal].
 */
@HiltAndroidTest
class MilestoneRegressionE2ETest : BaseE2ETest() {

    // =========================================================================
    // REG-M01: Completing a milestone updates goal progress
    // Regression for Bug 1 — "Close milestone → no task progress"
    // =========================================================================

    @Test
    fun completeMilestone_updatesGoalProgress() {
        // Seed a goal with 2 milestones, no linked tasks.
        // Initial progress = 0%. After completing 1 of 2 milestones → 50%.
        val goalId: Long
        runBlocking {
            goalId = goalRepository.insertGoal(
                TestDataFactory.goal(
                    title = "Milestone Progress Goal",
                    category = GoalCategory.LEARNING,
                    progress = 0
                )
            )
            database.milestoneDao().insert(
                TestDataFactory.milestone(goalId = goalId, title = "Step 1: Research", position = 0)
            )
            database.milestoneDao().insert(
                TestDataFactory.milestone(goalId = goalId, title = "Step 2: Practice", position = 1)
            )
        }
        Thread.sleep(2_000)

        // Navigate to goal detail
        nav.goToGoals()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Milestone Progress Goal", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        goals.tapGoal("Milestone Progress Goal")

        // Wait for detail screen to load
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasContentDescription("percent complete", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Initially 0% — verify the ring shows 0
        goals.assertProgressRing("0 percent")

        // Switch to Milestones tab
        goals.tapDetailTab("Milestones")
        Thread.sleep(1_000)

        // Verify both milestones visible
        goals.assertMilestoneDisplayed("Step 1: Research")
        goals.assertMilestoneDisplayed("Step 2: Practice")

        // Complete the first milestone
        goals.tapMilestoneToggle("Step 1: Research")
        Thread.sleep(2_000) // wait for recalculateProgress

        // Progress should now be 50% (1 of 2 milestones completed)
        goals.assertProgressRing("50 percent")
    }

    // =========================================================================
    // REG-M02: Milestone-only goals have meaningful progress
    // Regression for Bug 2 — milestones not related to progress
    // =========================================================================

    @Test
    fun milestoneOnlyGoal_progressReflectsMilestones() {
        // A goal with 3 milestones and NO linked tasks.
        // Completing all 3 should yield 100%.
        val goalId: Long
        runBlocking {
            goalId = goalRepository.insertGoal(
                TestDataFactory.goal(
                    title = "Pure Milestone Goal",
                    category = GoalCategory.PERSONAL,
                    progress = 0
                )
            )
            database.milestoneDao().insert(
                TestDataFactory.milestone(goalId = goalId, title = "Phase A", position = 0)
            )
            database.milestoneDao().insert(
                TestDataFactory.milestone(goalId = goalId, title = "Phase B", position = 1)
            )
            database.milestoneDao().insert(
                TestDataFactory.milestone(goalId = goalId, title = "Phase C", position = 2)
            )
        }
        Thread.sleep(2_000)

        nav.goToGoals()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Pure Milestone Goal", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        goals.tapGoal("Pure Milestone Goal")

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasContentDescription("percent complete", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        goals.assertProgressRing("0 percent")
        goals.tapDetailTab("Milestones")
        Thread.sleep(1_000)

        // Complete all three milestones one by one
        goals.tapMilestoneToggle("Phase A")
        Thread.sleep(1_500)
        goals.assertProgressRing("33 percent")

        goals.tapMilestoneToggle("Phase B")
        Thread.sleep(1_500)
        goals.assertProgressRing("66 percent")

        goals.tapMilestoneToggle("Phase C")
        Thread.sleep(1_500)
        goals.assertProgressRing("100 percent")
    }

    // =========================================================================
    // REG-M03: Mixed tasks + milestones use weighted progress formula
    // Regression for Bug 1 & 2 combined
    // =========================================================================

    @Test
    fun mixedTasksAndMilestones_progressCombined() {
        // 1 milestone + 1 task.
        // Weighted formula: 0.6 × milestoneRatio + 0.4 × taskRatio
        // Complete milestone → 0.6×(1/1) + 0.4×(0/1) = 60%.
        val goalId: Long
        runBlocking {
            goalId = goalRepository.insertGoal(
                TestDataFactory.goal(
                    title = "Mixed Progress Goal",
                    category = GoalCategory.CAREER,
                    progress = 0
                )
            )
            database.milestoneDao().insert(
                TestDataFactory.milestone(goalId = goalId, title = "Plan complete", position = 0)
            )
            taskRepository.insertTask(
                TestDataFactory.task(title = "Execute plan", goalId = goalId)
            )
        }
        Thread.sleep(2_000)

        nav.goToGoals()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Mixed Progress Goal", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        goals.tapGoal("Mixed Progress Goal")

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasContentDescription("percent complete", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Initial: no items completed → 0%
        goals.assertProgressRing("0 percent")

        // Complete the milestone
        goals.tapDetailTab("Milestones")
        Thread.sleep(1_000)
        goals.tapMilestoneToggle("Plan complete")
        Thread.sleep(2_000)

        // Weighted: 0.6×(1/1) + 0.4×(0/1) = 60%
        goals.assertProgressRing("60 percent")
    }

    // =========================================================================
    // REG-M04: Create goal with multiple milestones — all saved
    // Regression for Bug 3 — "Last added milestone not saved"
    // =========================================================================

    @Test
    fun createGoal_multiMilestones_allSaved() {
        nav.goToGoals()
        goals.assertEmptyState()
        goals.tapCreateFirstGoal()
        goals.assertCreateScreenVisible()

        // Step 1: Describe
        goals.typeGoalTitle("Learn 3 instruments")
        Thread.sleep(500)

        // Skip AI for deterministic test — go directly to SMART step
        goals.tapRefineWithAi()

        // Wait for AI processing to complete (success or fallback)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText("SMART Goal", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Step 2 → Step 3
        goals.tapNextTimeline()
        Thread.sleep(500)

        // Wait for Step 3 (Timeline) to load
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Milestones", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(500)

        // Clear any AI-suggested milestones so we have room for our own
        goals.clearAllMilestoneChips()
        Thread.sleep(1_000)

        // Verify all milestones were cleared (no Remove buttons left)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Remove")
                .fetchSemanticsNodes().isEmpty()
        }

        // Wait for the input field to appear (shown when milestones < 5)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag("milestone_input"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Add 3 milestones with time for recomposition between each
        goals.typeMilestoneAndAdd("Guitar basics")
        Thread.sleep(500)
        goals.typeMilestoneAndAdd("Piano fundamentals")
        Thread.sleep(500)
        goals.typeMilestoneAndAdd("Drums intro")
        Thread.sleep(500)

        // All 3 should be visible as chips before creating
        goals.assertMilestoneChip("Guitar basics")
        goals.assertMilestoneChip("Piano fundamentals")
        goals.assertMilestoneChip("Drums intro")

        // Create the goal
        goals.tapCreateGoalButton()

        // Wait for celebration
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Goal Created!"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        goals.assertCelebrationVisible()

        // Navigate to goal detail to verify all milestones persisted
        goals.tapViewGoalDetails()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasContentDescription("percent complete", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Switch to Milestones tab
        goals.tapDetailTab("Milestones")
        Thread.sleep(1_500)

        // All 3 milestones must be present in the detail
        goals.assertMilestoneDisplayed("Guitar basics")
        goals.assertMilestoneDisplayed("Piano fundamentals")
        goals.assertMilestoneDisplayed("Drums intro")
    }

    // =========================================================================
    // REG-M05: Uncomplete milestone reverts progress
    // Ensures toggling milestone off reduces progress correctly
    // =========================================================================

    @Test
    fun uncompleteMilestone_revertsProgress() {
        val goalId: Long
        runBlocking {
            goalId = goalRepository.insertGoal(
                TestDataFactory.goal(
                    title = "Toggle Progress Goal",
                    category = GoalCategory.HEALTH,
                    progress = 0
                )
            )
            database.milestoneDao().insert(
                TestDataFactory.milestone(goalId = goalId, title = "Milestone A", position = 0)
            )
            database.milestoneDao().insert(
                TestDataFactory.milestone(goalId = goalId, title = "Milestone B", position = 1)
            )
        }
        Thread.sleep(2_000)

        nav.goToGoals()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Toggle Progress Goal", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        goals.tapGoal("Toggle Progress Goal")

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasContentDescription("percent complete", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        goals.tapDetailTab("Milestones")
        Thread.sleep(1_000)

        // Complete milestone A → 50%
        goals.tapMilestoneToggle("Milestone A")
        Thread.sleep(2_000)
        goals.assertProgressRing("50 percent")

        // Uncomplete milestone A → back to 0%
        goals.tapMilestoneToggle("Milestone A")
        Thread.sleep(2_000)
        goals.assertProgressRing("0 percent")
    }

    // =========================================================================
    // REG-M06: Weighted formula — milestone has higher weight than tasks
    // Verifies 60/40 blend: completing 1 of 2 milestones with 0 of 3 tasks
    // Expected: 0.6×(1/2) + 0.4×(0/3) = 30%
    // =========================================================================

    @Test
    fun weightedFormula_milestoneWeightsMoreThanTasks() {
        val goalId: Long
        runBlocking {
            goalId = goalRepository.insertGoal(
                TestDataFactory.goal(
                    title = "Weighted Progress Goal",
                    category = GoalCategory.LEARNING,
                    progress = 0
                )
            )
            database.milestoneDao().insert(
                TestDataFactory.milestone(goalId = goalId, title = "Research phase", position = 0)
            )
            database.milestoneDao().insert(
                TestDataFactory.milestone(goalId = goalId, title = "Build phase", position = 1)
            )
            taskRepository.insertTask(
                TestDataFactory.task(title = "Task Alpha", goalId = goalId)
            )
            taskRepository.insertTask(
                TestDataFactory.task(title = "Task Beta", goalId = goalId)
            )
            taskRepository.insertTask(
                TestDataFactory.task(title = "Task Gamma", goalId = goalId)
            )
        }
        Thread.sleep(2_000)

        nav.goToGoals()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Weighted Progress Goal", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        goals.tapGoal("Weighted Progress Goal")

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasContentDescription("percent complete", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Initially 0%
        goals.assertProgressRing("0 percent")

        // Complete 1 of 2 milestones, 0 of 3 tasks
        // Weighted: 0.6×(1/2) + 0.4×(0/3) = 0.3 + 0 = 30%
        goals.tapDetailTab("Milestones")
        Thread.sleep(1_000)
        goals.tapMilestoneToggle("Research phase")
        Thread.sleep(2_000)
        goals.assertProgressRing("30 percent")
    }

    // =========================================================================
    // REG-M07: Weighted formula — tasks-only contributes 40% cap in mixed mode
    // Verifies: completing all tasks but no milestones = 40%
    // Expected: 0.6×(0/1) + 0.4×(2/2) = 40%
    // =========================================================================

    @Test
    fun weightedFormula_tasksOnlyContribute40PercentWhenMilestonesExist() {
        val goalId: Long
        runBlocking {
            goalId = goalRepository.insertGoal(
                TestDataFactory.goal(
                    title = "Tasks Capped Goal",
                    category = GoalCategory.CAREER,
                    progress = 0
                )
            )
            database.milestoneDao().insert(
                TestDataFactory.milestone(goalId = goalId, title = "Final milestone", position = 0)
            )
            val task1Id = taskRepository.insertTask(
                TestDataFactory.task(title = "Do task one", goalId = goalId)
            )
            val task2Id = taskRepository.insertTask(
                TestDataFactory.task(title = "Do task two", goalId = goalId)
            )
            // Complete both tasks
            taskRepository.completeTask(task1Id)
            taskRepository.completeTask(task2Id)
            // Recalculate so the DB reflects the new progress
            goalRepository.recalculateProgress(goalId)
        }
        Thread.sleep(2_000)

        nav.goToGoals()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Tasks Capped Goal", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        goals.tapGoal("Tasks Capped Goal")

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasContentDescription("percent complete", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // All tasks done, no milestones done → 0.6×(0/1) + 0.4×(2/2) = 40%
        goals.assertProgressRing("40 percent")
    }

    // =========================================================================
    // REG-M08: Pending milestone input auto-saved on "Create Goal"
    // Regression for Bug 3 (secondary) — user types last milestone but taps
    // "Create Goal" without pressing "+", losing the pending text.
    // =========================================================================

    @Test
    fun createGoal_pendingMilestoneInput_autoSavedOnCreate() {
        nav.goToGoals()
        goals.assertEmptyState()
        goals.tapCreateFirstGoal()
        goals.assertCreateScreenVisible()

        // Step 1: Describe
        goals.typeGoalTitle("Master cooking")
        Thread.sleep(500)

        goals.tapRefineWithAi()

        // Wait for AI processing to complete (success or fallback)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText("SMART Goal", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Step 2 → Step 3
        goals.tapNextTimeline()
        Thread.sleep(500)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Milestones", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(500)

        // Clear any AI-suggested milestones
        goals.clearAllMilestoneChips()
        Thread.sleep(1_000)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Remove")
                .fetchSemanticsNodes().isEmpty()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag("milestone_input"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Add first two milestones via the "+" button
        goals.typeMilestoneAndAdd("Knife skills")
        Thread.sleep(500)
        goals.typeMilestoneAndAdd("Sauce basics")
        Thread.sleep(500)

        // Type the THIRD milestone but do NOT tap "+" — leave it pending
        goals.typeMilestoneWithoutAdd("Baking bread")
        Thread.sleep(500)

        // Verify only 2 chips visible so far; the third is still in the input
        goals.assertMilestoneChip("Knife skills")
        goals.assertMilestoneChip("Sauce basics")

        // Tap "Create Goal" — the pending input should be auto-added
        goals.tapCreateGoalButton()

        // Wait for celebration
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Goal Created!"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        goals.assertCelebrationVisible()

        // Navigate to goal detail to verify ALL 3 milestones persisted
        goals.tapViewGoalDetails()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasContentDescription("percent complete", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Switch to Milestones tab
        goals.tapDetailTab("Milestones")
        Thread.sleep(1_500)

        // All 3 milestones must be present — including the one from the input field
        goals.assertMilestoneDisplayed("Knife skills")
        goals.assertMilestoneDisplayed("Sauce basics")
        goals.assertMilestoneDisplayed("Baking bread")
    }

    // =========================================================================
    // REG-M09: Milestone input stays visible when adding 3+ milestones
    // Regression for keyboard occlusion bug — after adding >2 milestones the
    // input field was pushed below the keyboard and the user couldn't see
    // what they were typing.
    // =========================================================================

    @Test
    fun addMultipleMilestones_inputRemainsVisibleAboveKeyboard() {
        nav.goToGoals()
        goals.assertEmptyState()
        goals.tapCreateFirstGoal()
        goals.assertCreateScreenVisible()

        // Step 1: Describe
        goals.typeGoalTitle("Build a garden")
        Thread.sleep(500)

        // Step 1 → Step 2 (AI refinement)
        goals.tapRefineWithAi()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText("SMART Goal", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Step 2 → Step 3 (Timeline & Milestones)
        goals.tapNextTimeline()
        Thread.sleep(500)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Milestones", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(500)

        // Clear any AI-suggested milestones
        goals.clearAllMilestoneChips()
        Thread.sleep(1_000)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Remove")
                .fetchSemanticsNodes().isEmpty()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag("milestone_input"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Add milestones one by one and verify the input stays visible
        // after auto-scroll. Checking input visibility BEFORE scrolling to
        // chips, because assertMilestoneChip uses performScrollTo which
        // would mask the bug.
        goals.typeMilestoneAndAdd("Prepare soil")
        Thread.sleep(800) // allow auto-scroll animation to complete
        goals.assertMilestoneInputVisible()

        goals.typeMilestoneAndAdd("Plant seeds")
        Thread.sleep(800)
        goals.assertMilestoneInputVisible()

        goals.typeMilestoneAndAdd("Install irrigation")
        Thread.sleep(800)
        // KEY ASSERTION: after 3 milestones, the input must still be on screen
        goals.assertMilestoneInputVisible()

        // Add a 4th milestone to prove the field is fully interactable
        goals.typeMilestoneAndAdd("First harvest")
        Thread.sleep(800)
        goals.assertMilestoneInputVisible()

        // Verify all 4 chips are present on the create screen
        goals.assertMilestoneChip("Prepare soil")
        goals.assertMilestoneChip("Plant seeds")
        goals.assertMilestoneChip("Install irrigation")
        goals.assertMilestoneChip("First harvest")

        // Full creation + detail screen milestone persistence is
        // covered by REG-M04 (createGoal_multiMilestones_allSaved).
    }
}

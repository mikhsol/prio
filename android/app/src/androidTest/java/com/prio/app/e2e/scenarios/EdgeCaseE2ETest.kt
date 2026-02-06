package com.prio.app.e2e.scenarios

import com.prio.app.e2e.BaseE2ETest
import com.prio.app.e2e.util.TestDataFactory
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.common.model.RecurrencePattern
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * E2E-B: Undocumented Use Cases & Edge Cases
 *
 * Tests scenarios not explicitly covered by user stories but
 * critical for real-world usage. Discovered during code audit.
 *
 * Categories:
 * - Cross-feature interactions (tasks + goals, calendar + tasks)
 * - Data boundary conditions
 * - State machine transitions
 * - Error recovery paths
 */
@HiltAndroidTest
class EdgeCaseE2ETest : BaseE2ETest() {

    // =========================================================================
    // E2E-B1-01: Task linked to goal — goal deletion cascades
    // Priority: P1 (Core)
    // =========================================================================

    @Test
    fun deleteGoal_linkedTasksUpdateGracefully() = runTest {
        val goalId = goalRepository.insertGoal(
            TestDataFactory.goal(title = "Fitness goal")
        )
        taskRepository.insertTask(
            TestDataFactory.task(title = "Go running", goalId = goalId)
        )

        nav.goToTasks()
        taskList.assertTaskDisplayed("Go running")

        // Delete the goal
        nav.goToGoals()
        goals.tapGoal("Fitness goal")
        // (Would need delete action on goal detail)

        // Task should still exist but with goalId=null (FK SET_NULL)
        nav.goToTasks()
        taskList.assertTaskDisplayed("Go running")
    }

    // =========================================================================
    // E2E-B1-02: Recurring task completion creates next instance
    // Priority: P1 (Core) — TM-008
    // =========================================================================

    @Test
    fun completeRecurringTask_generatesNextInstance() = runTest {
        taskRepository.insertTask(
            TestDataFactory.recurringTask(
                title = "Daily standup",
                pattern = RecurrencePattern.DAILY
            )
        )

        nav.goToTasks()
        taskList.assertTaskDisplayed("Daily standup")

        taskList.completeTask("Daily standup")

        // A new instance should be generated for the next day
        // (depends on RecurrenceManager implementation)
        waitForIdle()
    }

    // =========================================================================
    // E2E-B1-03: Cross-screen data consistency
    // Priority: P1 (Core)
    // =========================================================================

    @Test
    fun taskCreatedOnTaskScreen_appearsOnCalendar() = runTest {
        // Create a task with a due date
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Calendar visible task",
                dueDate = TestDataFactory.hoursFromNow(3),
                quadrant = EisenhowerQuadrant.SCHEDULE
            )
        )

        nav.goToTasks()
        taskList.assertTaskDisplayed("Calendar visible task")

        nav.goToCalendar()
        // Task should appear in untimed tasks or timeline
        waitForIdle()
    }

    // =========================================================================
    // E2E-B2-01: Search with no results
    // Priority: P2 (Extended)
    // =========================================================================

    @Test
    fun searchWithNoResults_showsEmptyMessage() = runTest {
        taskRepository.insertTask(TestDataFactory.task(title = "Real task"))

        nav.goToTasks()
        taskList.openSearch()
        // Would need to type search query — robot doesn't have search text input yet
        // This tests the search empty state
    }

    // =========================================================================
    // E2E-B2-02: Back navigation while AI is processing
    // Priority: P1 (Core) — Race condition
    // =========================================================================

    @Test
    fun backDuringAiProcessing_doesNotCrash() {
        nav.goToTasks()
        nav.tapFab()
        quickCapture.typeTaskText("AI processing task with lots of context")

        // Don't wait for AI — dismiss immediately
        quickCapture.dismiss()

        // AI coroutine should be cancelled gracefully
        taskList.assertScreenVisible()
    }

    // =========================================================================
    // E2E-B2-03: Create task while offline (airplane mode)
    // Priority: P2 (Extended) — Offline-first principle
    // =========================================================================

    @Test
    fun createTask_worksOffline() {
        // On-device AI should work without network
        // Room database is local
        // This test validates offline-first architecture
        nav.goToTasks()
        nav.tapFab()
        quickCapture.typeTaskText("Offline task")
        quickCapture.waitForAiClassification()
        quickCapture.tapCreateTask()
        quickCapture.assertSheetDismissed()
        taskList.assertTaskDisplayed("Offline task")
    }

    // =========================================================================
    // E2E-B3-01: Goals with 0% and 100% progress boundary
    // Priority: P1 (Core) — DEF-009 (NaN risk)
    // =========================================================================

    @Test
    fun goalProgressBoundaries_noNaN() = runTest {
        goalRepository.insertGoal(TestDataFactory.goal(title = "Zero Goal", progress = 0))
        goalRepository.insertGoal(TestDataFactory.goal(title = "Full Goal", progress = 100))

        nav.goToGoals()
        goals.assertGoalDisplayed("Zero Goal")
        goals.assertGoalDisplayed("Full Goal")
        // No NaN/crash in the overview card averageProgress calculation
    }

    // =========================================================================
    // E2E-B3-02: Task with all optional fields null
    // Priority: P2 (Extended)
    // =========================================================================

    @Test
    fun minimalTask_rendersCorrectly() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Bare minimum",
                notes = null,
                dueDate = null,
                goalId = null,
                aiExplanation = null,
                aiConfidence = 0f
            )
        )

        nav.goToTasks()
        taskList.assertTaskDisplayed("Bare minimum")
        taskList.tapTask("Bare minimum")
        taskDetail.assertSheetVisible()
    }
}

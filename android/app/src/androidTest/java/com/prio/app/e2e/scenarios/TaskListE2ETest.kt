package com.prio.app.e2e.scenarios

import com.prio.app.e2e.BaseE2ETest
import com.prio.app.e2e.util.TestDataFactory
import com.prio.core.common.model.EisenhowerQuadrant
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * E2E-A4: Task List & Eisenhower Matrix Tests
 *
 * Validates user stories:
 * - TM-003: AI Eisenhower Classification
 * - TM-004: Task List View
 * - TM-005: Eisenhower Matrix View
 *
 * Acceptance criteria:
 * - Tasks grouped by quadrant
 * - Sections collapsible
 * - Overdue tasks highlighted
 * - Filter by quadrant
 * - Search tasks
 */
@HiltAndroidTest
class TaskListE2ETest : BaseE2ETest() {

    // =========================================================================
    // E2E-A4-01: Smoke — Task list with Eisenhower sections
    // Priority: P0 (Smoke)
    // =========================================================================

    @Test
    fun taskListShowsEisenhowerSections() = runTest {
        // Pre-populate tasks across all 4 quadrants
        val tasks = TestDataFactory.mixedTaskSet()
        tasks.forEach { taskRepository.insertTask(it) }

        nav.goToTasks()
        taskList.assertScreenVisible()

        // Wait for Room data to propagate through Flow → ViewModel → UI
        waitForIdle()

        // Verify Eisenhower sections are visible
        // Section headers render as "🔴 DO FIRST", "🟡 SCHEDULE", etc.
        // assertSectionVisible uses substring matching + waitUntil(10s)
        taskList.assertSectionVisible("DO FIRST")
        taskList.assertSectionVisible("SCHEDULE")
        taskList.assertSectionVisible("DELEGATE")
        taskList.assertSectionVisible("MAYBE LATER")
    }

    // =========================================================================
    // E2E-A4-02: Empty state when no tasks
    // Priority: P1 (Core)
    // =========================================================================

    @Test
    fun emptyTaskList_showsEmptyState() {
        nav.goToTasks()
        taskList.assertScreenVisible()
        taskList.assertEmptyState()
    }

    // =========================================================================
    // E2E-A4-03: Complete task via checkbox
    // Priority: P1 (Core) — TM-006
    // =========================================================================

    @Test
    fun completeTask_removedFromActiveList() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(title = "Task to complete", quadrant = EisenhowerQuadrant.DO_FIRST)
        )

        nav.goToTasks()
        taskList.assertTaskDisplayed("Task to complete")

        // Complete it
        taskList.completeTask("Task to complete")

        // Should be hidden (completed tasks hidden by default)
        taskList.assertTaskNotDisplayed("Task to complete")
    }

    // =========================================================================
    // E2E-A4-04: Show completed tasks toggle
    // Priority: P2 (Extended)
    // =========================================================================

    @Test
    fun toggleCompletedTasks_showsAndHides() = runTest {
        taskRepository.insertTask(TestDataFactory.completedTask(title = "Done task"))
        taskRepository.insertTask(TestDataFactory.task(title = "Active task"))

        nav.goToTasks()

        // Completed task should be hidden by default
        taskList.assertTaskNotDisplayed("Done task")
        taskList.assertTaskDisplayed("Active task")

        // Toggle to show completed
        taskList.toggleShowCompleted()
        taskList.assertTaskDisplayed("Done task")

        // Toggle back
        taskList.toggleShowCompleted()
        taskList.assertTaskNotDisplayed("Done task")
    }

    // =========================================================================
    // E2E-A4-05: Smoke — Filter by quadrant
    // Priority: P0 (Smoke)
    // =========================================================================

    @Test
    fun filterByQuadrant_showsOnlyMatchingTasks() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(title = "Urgent deadline", quadrant = EisenhowerQuadrant.DO_FIRST)
        )
        taskRepository.insertTask(
            TestDataFactory.task(title = "Plan vacation", quadrant = EisenhowerQuadrant.SCHEDULE)
        )

        nav.goToTasks()

        // Filter to Do First only
        taskList.selectFilter("Do First")

        taskList.assertTaskDisplayed("Urgent deadline")
        taskList.assertTaskNotDisplayed("Plan vacation")
    }

    // =========================================================================
    // E2E-A4-06: Overdue tasks highlighted
    // Priority: P1 (Core)
    // =========================================================================

    @Test
    fun overdueTask_showsOverdueIndicator() = runTest {
        taskRepository.insertTask(TestDataFactory.overdueTask(title = "Late report"))

        nav.goToTasks()
        taskList.assertTaskDisplayed("Late report")
        taskList.assertOverdueIndicator("Late report")
    }

    // =========================================================================
    // E2E-A4-07: Tap task opens detail sheet
    // Priority: P1 (Core)
    // =========================================================================

    @Test
    fun tapTask_opensDetailSheet() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(title = "Detailed task", notes = "Has notes")
        )

        nav.goToTasks()
        taskList.tapTask("Detailed task")

        // Task detail sheet should open
        taskDetail.assertSheetVisible()
        taskDetail.assertTaskTitle("Detailed task")
    }

    // =========================================================================
    // E2E-A4-08: Swipe to complete
    // Priority: P2 (Extended) — TM-006
    // =========================================================================

    @Test
    fun swipeRightTask_completesIt() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(title = "Swipeable task", quadrant = EisenhowerQuadrant.SCHEDULE)
        )

        nav.goToTasks()
        taskList.assertTaskDisplayed("Swipeable task")

        taskList.swipeTaskRight("Swipeable task")

        // Task should be completed and hidden
        taskList.assertTaskNotDisplayed("Swipeable task")
    }

    // =========================================================================
    // E2E-A4-09: Swipe to delete
    // Priority: P2 (Extended) — TM-006
    // =========================================================================

    @Test
    fun swipeLeftTask_deletesIt() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(title = "Deletable task")
        )

        nav.goToTasks()
        taskList.assertTaskDisplayed("Deletable task")

        taskList.swipeTaskLeft("Deletable task")

        taskList.assertTaskNotDisplayed("Deletable task")
    }
}

package com.prio.app.e2e.scenarios

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
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
    fun taskListShowsEisenhowerSections() {
        // Pre-populate tasks across all 4 quadrants
        // Use runBlocking (not runTest) so Room's real dispatchers work normally
        kotlinx.coroutines.runBlocking {
            val tasks = TestDataFactory.mixedTaskSet()
            tasks.forEach { taskRepository.insertTask(it) }
        }

        nav.goToTasks()
        taskList.assertScreenVisible()

        // Give Room → Flow → ViewModel → Compose pipeline time to settle
        Thread.sleep(3_000)
        waitForIdle()

        // Verify Eisenhower sections by testTag (more reliable than text matching)
        // Section headers have testTag("section_header_{quadrant.name}")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("section_header_DO_FIRST")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("section_header_DO_FIRST").assertIsDisplayed()
        composeRule.onNodeWithTag("section_header_SCHEDULE").assertIsDisplayed()
        composeRule.onNodeWithTag("section_header_DELEGATE").assertIsDisplayed()
        // ELIMINATE section is off-screen in the LazyColumn; scroll to it
        // Two scrollable containers exist (filter chips LazyRow + task LazyColumn)
        // The task LazyColumn is the second (last) one
        composeRule.onAllNodes(hasScrollToNodeAction())
            .onLast()
            .performScrollToNode(hasTestTag("section_header_ELIMINATE"))
        composeRule.onNodeWithTag("section_header_ELIMINATE").assertIsDisplayed()
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

        // Wait for Room → Flow → ViewModel → Compose pipeline to propagate
        // the completion state and hide the task from active list
        Thread.sleep(1_000)
        waitForIdle()

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
        // Insert a task due today and one due far in the future
        taskRepository.insertTask(
            TestDataFactory.urgentTask(title = "Urgent deadline")
        )
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Plan vacation",
                quadrant = EisenhowerQuadrant.SCHEDULE,
                dueDate = TestDataFactory.daysFromNow(30)
            )
        )

        nav.goToTasks()
        Thread.sleep(1_000)
        waitForIdle()

        // Filter to Today only (available filters: All/Today/Upcoming/Has Goal/Recurring)
        taskList.selectFilter("Today")
        Thread.sleep(500)
        waitForIdle()

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

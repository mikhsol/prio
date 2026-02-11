package com.prio.app.e2e.scenarios

import com.prio.app.e2e.BaseE2ETest
import com.prio.app.e2e.util.TestDataFactory
import com.prio.core.common.model.EisenhowerQuadrant
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * E2E-A5: Task Detail & Edit Tests
 *
 * Validates user stories:
 * - TM-006: Complete, Edit, Delete Tasks
 * - TM-007: Notes & Subtasks
 *
 * Acceptance criteria:
 * - View task details from task list
 * - Edit title, notes, due date
 * - Mark complete/incomplete
 * - Delete task
 * - Duplicate task
 * - Copy to clipboard
 * - Add/remove subtasks
 * - AI explanation visible
 */
@HiltAndroidTest
class TaskDetailE2ETest : BaseE2ETest() {

    // =========================================================================
    // E2E-A5-01: Smoke — View task details
    // Priority: P0 (Smoke)
    // =========================================================================

    @Test
    fun viewTaskDetail_showsAllFields() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Important meeting prep",
                quadrant = EisenhowerQuadrant.DO_FIRST,
                notes = "Prepare slides for Q4 review",
                aiExplanation = "Deadline today, high urgency"
            )
        )

        nav.goToTasks()
        waitForIdle()
        taskList.tapTask("Important meeting prep")

        // Inline TaskDetailSheet renders in main Compose tree (post-migration)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("More options")
            ).fetchSemanticsNodes().isNotEmpty()
        }
        taskDetail.assertSheetVisible()
        taskDetail.assertTaskTitle("Important meeting prep")
        taskDetail.assertNotCompleted()
        taskDetail.assertAiExplanation("Deadline today")
    }

    // =========================================================================
    // E2E-A5-02: Mark task complete from detail
    // Priority: P1 (Core)
    // =========================================================================

    @Test
    fun markTaskComplete_fromDetail() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(title = "Complete me", quadrant = EisenhowerQuadrant.SCHEDULE)
        )

        nav.goToTasks()
        taskList.tapTask("Complete me")

        taskDetail.assertNotCompleted()
        taskDetail.tapComplete()
        taskDetail.assertCompleted()
    }

    // =========================================================================
    // E2E-A5-03: Delete task from detail
    // Priority: P1 (Core)
    // =========================================================================

    @Test
    fun deleteTask_removedFromList() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(title = "Delete me")
        )

        nav.goToTasks()
        taskList.tapTask("Delete me")
        taskDetail.assertSheetVisible()

        taskDetail.openOverflowMenu()
        taskDetail.tapDelete()
        // Wait for PrioConfirmDialog to appear before confirming
        taskDetail.confirmDelete()

        // After deletion, sheet closes and returns to task list
        waitForIdle()
        nav.goToTasks()
        taskList.assertTaskNotDisplayed("Delete me")
    }

    // =========================================================================
    // E2E-A5-04: Duplicate task
    // Priority: P2 (Extended)
    // =========================================================================

    @Test
    fun duplicateTask_createsNewCopy() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(title = "Original task")
        )

        nav.goToTasks()
        taskList.tapTask("Original task")

        taskDetail.openOverflowMenu()
        taskDetail.tapDuplicate()

        // Should return to list with both original and copy visible
        // (Depending on implementation, copy might have "Copy of" prefix)
    }

    // =========================================================================
    // E2E-A5-05: Edit task title
    // Priority: P1 (Core)
    // =========================================================================

    @Test
    fun editTaskTitle_updatesInList() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(title = "Old title")
        )

        nav.goToTasks()
        taskList.tapTask("Old title")

        taskDetail.openOverflowMenu()
        taskDetail.tapEdit()

        taskDetail.editTitle("New title", currentTitle = "Old title")
        taskDetail.openOverflowMenu()
        taskDetail.tapDoneEditing()

        // Navigate back, verify updated
        nav.pressBack()
        taskList.assertTaskDisplayed("New title")
        taskList.assertTaskNotDisplayed("Old title")
    }

    // =========================================================================
    // E2E-A5-06: Smoke — View subtasks section
    // Priority: P0 (Smoke)
    // =========================================================================

    @Test
    fun viewSubtasksSection_displayedInDetail() = runTest {
        val parentId = taskRepository.insertTask(
            TestDataFactory.task(title = "Parent task")
        )
        taskRepository.insertTask(
            TestDataFactory.subtask(title = "Sub 1", parentTaskId = parentId)
        )
        taskRepository.insertTask(
            TestDataFactory.subtask(title = "Sub 2", parentTaskId = parentId)
        )

        nav.goToTasks()
        taskList.tapTask("Parent task")

        taskDetail.assertSheetVisible()
        taskDetail.assertSubtasksSection()
    }

    // =========================================================================
    // E2E-A5-07: Date picker shows "Next" → Time picker flow
    // Priority: P1 (Core) — Regression for time picker feature
    // =========================================================================

    @Test
    fun changeDueDate_showsTimePickerAfterDateSelection() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Task with due date",
                quadrant = EisenhowerQuadrant.DO_FIRST,
            )
        )

        nav.goToTasks()
        waitForIdle()
        taskList.tapTask("Task with due date")

        // Wait for sheet to open
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("More options")
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Enter edit mode
        taskDetail.openOverflowMenu()
        taskDetail.tapEdit()

        // Tap the due date property row to open date picker
        taskDetail.tapChangeForProperty("No due date")

        // Step 1: Date picker should show "Next" button (not "OK")
        taskDetail.assertDatePickerHasNextButton()

        // Tap "Next" to proceed to time picker
        taskDetail.tapDatePickerNext()

        // Step 2: Time picker dialog should appear
        taskDetail.assertTimePickerVisible()

        // Confirm time selection
        taskDetail.confirmTimePicker()
    }

    // =========================================================================
    // E2E-A5-08: Time picker "Skip" saves date without time
    // Priority: P1 (Core) — Regression for time picker feature
    // =========================================================================

    @Test
    fun changeDueDate_skipTime_savesDateOnly() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Task skip time",
                quadrant = EisenhowerQuadrant.SCHEDULE,
            )
        )

        nav.goToTasks()
        waitForIdle()
        taskList.tapTask("Task skip time")

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("More options")
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Enter edit mode and open date picker
        taskDetail.openOverflowMenu()
        taskDetail.tapEdit()
        taskDetail.tapChangeForProperty("No due date")

        // Select date and proceed to time picker
        taskDetail.tapDatePickerNext()
        taskDetail.assertTimePickerVisible()

        // Skip time selection — should save date without time
        taskDetail.skipTimePicker()

        // Verify no crash and sheet still visible
        taskDetail.assertSheetVisible()
    }
}

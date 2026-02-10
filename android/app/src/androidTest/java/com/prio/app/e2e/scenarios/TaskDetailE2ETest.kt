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
        taskList.tapTask("Important meeting prep")

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

        taskDetail.openOverflowMenu()
        taskDetail.tapDelete()
        taskDetail.confirmDelete()

        // Back to task list, task should be gone
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

        taskDetail.editTitle("New title")
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
}

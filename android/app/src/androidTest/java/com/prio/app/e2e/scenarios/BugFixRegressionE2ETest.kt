package com.prio.app.e2e.scenarios

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import com.prio.app.e2e.BaseE2ETest
import com.prio.app.e2e.util.TestDataFactory
import com.prio.core.common.model.EisenhowerQuadrant
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * E2E Regression Tests for Bug Fixes
 *
 * Validates that the following fixed bugs do not regress:
 *
 * Bug 1 — Save button in edit mode (was: Complete only, had to use overflow menu)
 * Bug 2 — "Change" buttons work for date/goal/recurrence/reminder fields
 * Bug 3 — Notes preserved when adding subtask (was: notes lost on reload)
 * Bug 4 — Tap task name to enter edit mode (was: only via overflow menu)
 * Bug 5 — Single FAB for add task (was: duplicate FAB on Tasks screen)
 *
 * Test naming: regression_{bugNumber}_{scenario}
 */
@HiltAndroidTest
class BugFixRegressionE2ETest : BaseE2ETest() {

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Navigate to task detail for a pre-created task.
     * Waits for the inline TaskDetailSheet to render.
     */
    private fun navigateToTaskDetail(taskTitle: String) {
        nav.goToTasks()
        waitForIdle()
        taskList.tapTask(taskTitle)
        // Wait for inline TaskDetailSheet to render (overflow menu appears)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasContentDescription("More options")
            ).fetchSemanticsNodes().isNotEmpty()
        }
        taskDetail.assertSheetVisible()
    }

    /**
     * Enter edit mode via overflow menu.
     */
    private fun enterEditMode() {
        taskDetail.openOverflowMenu()
        taskDetail.tapEdit()
    }

    // =========================================================================
    // Bug 1: Save button in edit mode
    // Was: Only "Complete" + "Delete" buttons. To save, user had to open
    //      overflow menu → "Done Editing".
    // Fix: In edit mode, "Complete" button is replaced with "Save" button.
    // =========================================================================

    @Test
    fun regression_bug1_saveButtonVisibleInEditMode() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Save button test",
                quadrant = EisenhowerQuadrant.SCHEDULE
            )
        )

        navigateToTaskDetail("Save button test")

        // Before edit mode: "Complete" button visible, no "Save" button
        taskDetail.assertCompleteButtonVisible()
        taskDetail.assertSaveButtonNotVisible()

        // Enter edit mode
        enterEditMode()

        // In edit mode: "Save" button visible instead of "Complete"
        taskDetail.assertSaveButtonVisible()
    }

    @Test
    fun regression_bug1_saveButtonPersistsChanges() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Persist via save",
                quadrant = EisenhowerQuadrant.DO_FIRST
            )
        )

        navigateToTaskDetail("Persist via save")
        enterEditMode()

        // Edit the title
        taskDetail.editTitle("Saved new title", currentTitle = "Persist via save")

        // Tap Save button
        taskDetail.tapSave()
        waitForIdle()

        // Navigate back and verify the change persisted
        nav.pressBack()
        waitForIdle()
        Thread.sleep(1_000) // Room → Flow pipeline
        taskList.assertTaskDisplayed("Saved new title")
        taskList.assertTaskNotDisplayed("Persist via save")
    }

    @Test
    fun regression_bug1_completeButtonRestoredAfterSave() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Complete button restore",
                quadrant = EisenhowerQuadrant.SCHEDULE
            )
        )

        navigateToTaskDetail("Complete button restore")
        enterEditMode()

        // "Save" is visible in edit mode
        taskDetail.assertSaveButtonVisible()

        // Tap Save → exits edit mode
        taskDetail.tapSave()
        waitForIdle()

        // "Complete" button should be restored
        taskDetail.assertCompleteButtonVisible()
        taskDetail.assertSaveButtonNotVisible()
    }

    // =========================================================================
    // Bug 2: "Change" buttons not working for detail fields
    // Was: Tapping "Change" on date/goal/recurrence/reminder did nothing
    //      (effect handlers were TODO stubs).
    // Fix: DatePickerDialog for date, AlertDialog for goal picker,
    //      snackbar "coming soon" for recurrence/reminder.
    // =========================================================================

    @Test
    fun regression_bug2_dateChangeOpensDatePicker() {
        runBlocking {
            taskRepository.insertTask(
                TestDataFactory.task(
                    title = "Date picker test",
                    quadrant = EisenhowerQuadrant.SCHEDULE
                )
            )
        }

        navigateToTaskDetail("Date picker test")

        // Tap the due date row ("No due date") — entire row is clickable
        taskDetail.tapChangeForProperty("No due date")

        // DatePickerDialog should appear with OK/Cancel buttons
        taskDetail.assertDatePickerVisible()

        // Dismiss it
        taskDetail.dismissDatePicker()
    }

    @Test
    fun regression_bug2_goalChangeOpensGoalPicker() {
        runBlocking {
            // Pre-create a goal so the picker has content
            goalRepository.insertGoal(
                TestDataFactory.goal(title = "Learn Kotlin")
            )
            taskRepository.insertTask(
                TestDataFactory.task(
                    title = "Goal picker test",
                    quadrant = EisenhowerQuadrant.SCHEDULE
                )
            )
        }

        navigateToTaskDetail("Goal picker test")

        // Tap the goal row ("No goal linked") — entire row is clickable
        taskDetail.tapChangeForProperty("No goal linked")

        // Goal picker dialog should appear
        taskDetail.assertGoalPickerVisible()

        // Dismiss it
        taskDetail.dismissGoalPicker()
    }

    @Test
    fun regression_bug2_goalPickerLinksGoal() {
        runBlocking {
            goalRepository.insertGoal(
                TestDataFactory.goal(title = "Ship MVP")
            )
            taskRepository.insertTask(
                TestDataFactory.task(
                    title = "Link goal test",
                    quadrant = EisenhowerQuadrant.DO_FIRST
                )
            )
        }

        navigateToTaskDetail("Link goal test")
        taskDetail.tapChangeForProperty("No goal linked")
        taskDetail.assertGoalPickerVisible()

        // Select the goal
        taskDetail.selectGoalInPicker("Ship MVP")

        // Snackbar confirms "Goal linked"
        taskDetail.assertSnackbarMessage("Goal linked")
    }

    @Test
    fun regression_bug2_recurrenceChangeShowsComingSoon() {
        runBlocking {
            taskRepository.insertTask(
                TestDataFactory.task(
                    title = "Recurrence test",
                    quadrant = EisenhowerQuadrant.DELEGATE
                )
            )
        }

        navigateToTaskDetail("Recurrence test")

        // Tap the recurrence row ("Does not repeat") — entire row is clickable
        taskDetail.tapChangeForProperty("Does not repeat")

        // Should see "coming soon" snackbar
        taskDetail.assertSnackbarMessage("coming soon")
    }

    @Test
    fun regression_bug2_reminderChangeShowsComingSoon() {
        runBlocking {
            taskRepository.insertTask(
                TestDataFactory.task(
                    title = "Reminder test",
                    quadrant = EisenhowerQuadrant.ELIMINATE
                )
            )
        }

        navigateToTaskDetail("Reminder test")

        // Tap the reminder row ("No reminder") — entire row is clickable
        taskDetail.tapChangeForProperty("No reminder")

        // Should see "coming soon" snackbar
        taskDetail.assertSnackbarMessage("coming soon")
    }

    // =========================================================================
    // Bug 3: Notes lost when adding subtask
    // Was: addSubtask() called loadTask() which reloaded from DB, discarding
    //      unsaved notes from the UI state.
    // Fix: Auto-save in-flight edits before reload, then restore editing state.
    // =========================================================================

    @Test
    fun regression_bug3_notesPreservedAfterAddingSubtask() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Notes survive subtask",
                quadrant = EisenhowerQuadrant.DO_FIRST
            )
        )

        navigateToTaskDetail("Notes survive subtask")
        enterEditMode()

        // Type some notes
        taskDetail.typeNotes("Important meeting notes that must not be lost")
        waitForIdle()

        // Add a subtask (this triggers the bug scenario: loadTask reload)
        taskDetail.addSubtaskViaDialog("Prepare slides")
        Thread.sleep(2_000) // Wait for DB write + reload

        // Notes should still be visible after the subtask reload
        taskDetail.assertNotesContain("Important meeting notes")

        // Subtask should also be visible
        taskDetail.assertSubtaskDisplayed("Prepare slides")
    }

    @Test
    fun regression_bug3_notesAndTitlePreservedAfterSubtaskAdd() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Multi-field preserve",
                quadrant = EisenhowerQuadrant.SCHEDULE
            )
        )

        navigateToTaskDetail("Multi-field preserve")
        enterEditMode()

        // Edit both title and notes
        taskDetail.editTitle("Renamed task", currentTitle = "Multi-field preserve")
        taskDetail.typeNotes("My important notes")
        waitForIdle()

        // Add subtask — triggers the reload
        taskDetail.addSubtaskViaDialog("Check references")
        Thread.sleep(2_000)

        // Notes should be preserved (this is the critical Bug 3 assertion)
        taskDetail.assertNotesContain("My important notes")
        taskDetail.assertSubtaskDisplayed("Check references")
    }

    // =========================================================================
    // Bug 4: Tap task name to enter edit mode
    // Was: Title was a non-clickable Text composable; edit mode only accessible
    //      via overflow menu → "Edit".
    // Fix: Title Text has .clickable { onEditClick() } to toggle edit mode.
    // =========================================================================

    @Test
    fun regression_bug4_tapTitleEntersEditMode() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Tap me to edit",
                quadrant = EisenhowerQuadrant.DO_FIRST
            )
        )

        navigateToTaskDetail("Tap me to edit")

        // Tap the title text directly (not via overflow menu)
        taskDetail.tapTitleToEdit("Tap me to edit")

        // Should now be in edit mode — title becomes an OutlinedTextField
        taskDetail.assertTitleEditable("Tap me to edit")

        // Save button should appear (Bug 1 fix)
        taskDetail.assertSaveButtonVisible()
    }

    @Test
    fun regression_bug4_tapTitleAndEditInPlace() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Old task name",
                quadrant = EisenhowerQuadrant.SCHEDULE
            )
        )

        navigateToTaskDetail("Old task name")

        // Tap title to enter edit mode
        taskDetail.tapTitleToEdit("Old task name")

        // Edit the title
        taskDetail.editTitle("New task name", currentTitle = "Old task name")

        // Save
        taskDetail.tapSave()
        waitForIdle()

        // Navigate back and verify
        nav.pressBack()
        waitForIdle()
        Thread.sleep(1_000)
        taskList.assertTaskDisplayed("New task name")
        taskList.assertTaskNotDisplayed("Old task name")
    }

    // =========================================================================
    // Bug 5: Two FAB buttons nearby to add task
    // Was: TaskListScreen.Scaffold had its own FAB + PrioBottomNavigation had
    //      a center FAB. Both said "Add new task", causing visual clutter and
    //      confusion.
    // Fix: Removed FAB from TaskListScreen.Scaffold. PrioBottomNavigation's
    //      center FAB is the single entry point.
    // =========================================================================

    @Test
    fun regression_bug5_singleFabOnTasksScreen() {
        nav.goToTasks()
        taskList.assertScreenVisible()

        // Count all "Add new task" FABs — should be exactly 1
        val fabNodes = composeRule.onAllNodesWithContentDescription("Add new task")
            .fetchSemanticsNodes()
        assert(fabNodes.size == 1) {
            "Expected exactly 1 'Add new task' FAB but found ${fabNodes.size}"
        }
    }

    @Test
    fun regression_bug5_singleFabOpensQuickCapture() {
        nav.goToTasks()
        taskList.assertScreenVisible()

        // Tap the single FAB
        nav.tapFab()

        // Quick capture should open
        quickCapture.assertSheetVisible()
    }

    @Test
    fun regression_bug5_fabOnAllScreensIsSingle() {
        // Verify each screen has exactly 1 FAB

        // Today screen
        nav.goToToday()
        waitForIdle()
        val todayFabs = composeRule.onAllNodesWithContentDescription("Add new task")
            .fetchSemanticsNodes()
        assert(todayFabs.size == 1) {
            "Today: Expected 1 FAB, found ${todayFabs.size}"
        }

        // Tasks screen (this was the buggy screen)
        nav.goToTasks()
        waitForIdle()
        val tasksFabs = composeRule.onAllNodesWithContentDescription("Add new task")
            .fetchSemanticsNodes()
        assert(tasksFabs.size == 1) {
            "Tasks: Expected 1 FAB, found ${tasksFabs.size}"
        }

        // Goals screen
        nav.goToGoals()
        waitForIdle()
        val goalsFabs = composeRule.onAllNodesWithContentDescription("Add new task")
            .fetchSemanticsNodes()
        assert(goalsFabs.size == 1) {
            "Goals: Expected 1 FAB, found ${goalsFabs.size}"
        }

        // Calendar screen
        nav.goToCalendar()
        waitForIdle()
        val calendarFabs = composeRule.onAllNodesWithContentDescription("Add new task")
            .fetchSemanticsNodes()
        assert(calendarFabs.size == 1) {
            "Calendar: Expected 1 FAB, found ${calendarFabs.size}"
        }
    }
}

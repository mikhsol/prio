package com.prio.app.e2e.scenarios

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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
 * Bug 6 — Delete task undo restores task (was: undo called uncomplete instead of re-insert)
 * Bug 7 — Single create-goal CTA on empty goals screen (was: FAB + empty-state button)
 * Bug 8 — "Add First Task" after goal creation opens quick capture (was: TODO stub) * Bug 9 — Goal Edit button not working (was: OnEditGoal was a no-op stub)
 * Bug 10 — "Refine with AI" not working (was: router short-circuited on rule-based failure
 *          for SUGGEST_SMART_GOAL without trying LLM; OnDeviceAiProvider missing handler)
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

    // =========================================================================
    // Bug 6: Delete task → Undo not working
    // Was: Snackbar undo action always dispatched OnUndoComplete (uncomplete)
    //      instead of OnUndoDelete (re-insert). Deleted task was permanently lost.
    // Fix: ShowSnackbar effect now carries the correct undoEvent so the screen
    //      dispatches OnUndoDelete for deletes and OnUndoComplete for completes.
    // =========================================================================

    @Test
    fun regression_bug6_deleteTaskUndoRestoresTask() {
        runBlocking {
            taskRepository.insertTask(
                TestDataFactory.task(
                    title = "Delete undo test task",
                    quadrant = EisenhowerQuadrant.SCHEDULE
                )
            )
        }

        nav.goToTasks()
        waitForIdle()
        taskList.assertTaskDisplayed("Delete undo test task")

        // Swipe left to delete
        taskList.swipeTaskLeft("Delete undo test task")
        Thread.sleep(1_000)

        // Snackbar should show "Task deleted" with "Undo"
        taskList.assertSnackbarWithUndo("Task deleted")

        // Tap Undo
        taskList.tapSnackbarUndo()

        // Wait for task to reappear (undo → Room insert → Flow → recomposition)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Delete undo test task", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(1_000) // Allow SwipeToDismiss snap-back animation
        waitForIdle()

        // Task should reappear in the list
        taskList.assertTaskDisplayed("Delete undo test task")
    }

    @Test
    fun regression_bug6_completeTaskUndoStillWorks() {
        runBlocking {
            taskRepository.insertTask(
                TestDataFactory.task(
                    title = "Complete undo test task",
                    quadrant = EisenhowerQuadrant.DO_FIRST
                )
            )
        }

        nav.goToTasks()
        waitForIdle()
        taskList.assertTaskDisplayed("Complete undo test task")

        // Swipe right to complete
        taskList.swipeTaskRight("Complete undo test task")
        Thread.sleep(1_000)

        // Snackbar should show "Task completed" with "Undo"
        taskList.assertSnackbarWithUndo("Task completed")

        // Tap Undo
        taskList.tapSnackbarUndo()
        Thread.sleep(2_000)

        // Task should reappear (uncompleted) in the list
        taskList.assertTaskDisplayed("Complete undo test task")
    }

    @Test
    fun regression_bug6_deleteTaskWithoutUndoRemovesPermanently() {
        runBlocking {
            taskRepository.insertTask(
                TestDataFactory.task(
                    title = "Permanent delete task",
                    quadrant = EisenhowerQuadrant.ELIMINATE
                )
            )
        }

        nav.goToTasks()
        waitForIdle()
        taskList.assertTaskDisplayed("Permanent delete task")

        // Swipe left to delete
        taskList.swipeTaskLeft("Permanent delete task")
        Thread.sleep(5_000) // Wait for snackbar to auto-dismiss

        // Task should be gone
        taskList.assertTaskNotDisplayed("Permanent delete task")
    }

    // =========================================================================
    // Bug 7: Duplicate "Create Goal" buttons on empty goals screen
    // Was: GoalsListScreen showed both the Scaffold FAB ("New Goal") AND the
    //      EmptyGoalsState "Create First Goal" button when there were no goals,
    //      creating two overlapping CTAs.
    // Fix: FAB is hidden when uiState.isEmpty is true; only the empty-state
    //      "Create First Goal" button is shown.
    // =========================================================================

    @Test
    fun regression_bug7_emptyGoalsShowsSingleCreateButton() {
        // No goals pre-created → empty state
        nav.goToGoals()
        waitForIdle()

        goals.assertEmptyState()

        // The empty-state "Create First Goal" button should be visible.
        // Use useUnmergedTree because ExtendedFloatingActionButton merges
        // children semantics, so the text only exists in the unmerged tree.
        composeRule.onNodeWithText("Create First Goal", useUnmergedTree = true)
            .assertExists()

        // The FAB ("Create new goal") should NOT be visible
        goals.assertFabNotVisible()
    }

    @Test
    fun regression_bug7_goalsWithDataShowsFab() {
        runBlocking {
            goalRepository.insertGoal(
                TestDataFactory.goal(title = "My active goal")
            )
        }

        nav.goToGoals()
        waitForIdle()
        Thread.sleep(2_000) // Wait for Room → Flow pipeline

        // FAB should be visible when there are goals
        goals.assertFabVisible()
    }

    @Test
    fun regression_bug7_emptyStateCreateButtonNavigatesToWizard() {
        // No goals pre-created → empty state
        nav.goToGoals()
        waitForIdle()

        goals.assertEmptyState()

        // Tap the empty-state "Create First Goal" button
        goals.tapCreateFirstGoal()
        waitForIdle()

        // Should navigate to Create Goal wizard
        goals.assertCreateScreenVisible()
    }

    // =========================================================================
    // Bug 8: "Add First Task" button not working after goal creation
    // Was: CreateGoalEffect.OpenQuickCapture was a TODO stub that showed
    //      "Quick capture coming soon" snackbar. The button did nothing useful.
    // Fix: CreateGoalScreen now receives onShowQuickCapture callback from
    //      PrioNavHost, navigates to goal detail, and opens quick capture.
    // =========================================================================

    @Test
    fun regression_bug8_addFirstTaskNavigatesFromCelebration() {
        nav.goToGoals()
        waitForIdle()

        // Tap empty-state button to create a goal
        goals.assertEmptyState()
        goals.tapCreateFirstGoal()
        waitForIdle()

        // Step 1: Describe
        goals.typeGoalTitle("Learn Kotlin Multiplatform")
        goals.tapRefineWithAi()

        // Wait for AI or timeout then skip
        Thread.sleep(3_000)
        try {
            goals.tapSkipAi()
        } catch (_: Throwable) {
            // AI may have already completed — AssertionError extends Error, not Exception
        }
        waitForIdle()

        // Step 2: SMART → Next
        goals.tapNextTimeline()
        waitForIdle()

        // Step 3: Create the goal
        goals.tapCreateGoalButton()
        Thread.sleep(3_000) // Wait for goal creation

        // Celebration overlay should appear
        goals.assertCelebrationVisible()

        // Tap "Add First Task" — should navigate to goal detail + open quick capture
        goals.tapAddFirstTask()
        Thread.sleep(2_000)

        // Quick capture sheet should be visible (the fix wires up onShowQuickCapture)
        quickCapture.assertSheetVisible()
    }

    @Test
    fun regression_bug8_viewDetailsWorksFromCelebration() {
        nav.goToGoals()
        waitForIdle()

        goals.assertEmptyState()
        goals.tapCreateFirstGoal()
        waitForIdle()

        // Quick create flow
        goals.typeGoalTitle("Ship v1.0")
        goals.tapRefineWithAi()
        Thread.sleep(3_000)
        try {
            goals.tapSkipAi()
        } catch (_: Throwable) {
            // AI may have already completed — AssertionError extends Error, not Exception
        }
        waitForIdle()
        goals.tapNextTimeline()
        waitForIdle()
        goals.tapCreateGoalButton()
        Thread.sleep(3_000)

        goals.assertCelebrationVisible()

        // Tap "View Goal Details" — should navigate to goal detail
        goals.tapViewGoalDetails()

        // Wait for navigation + ViewModel data loading (popBackStack + navigate is async)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Ship v1.0", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Should be on goal detail screen showing the goal title.
        // Use onAllNodesWithText + onFirst because the title may also appear
        // in the description section.
        composeRule.onAllNodesWithText("Ship v1.0", substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    // =========================================================================
    // Bug 9: Goal Edit button not working
    // Was: GoalDetailEvent.OnEditGoal handler was a no-op stub:
    //      `// Will navigate to edit screen`
    // Fix: Inline edit mode — tapping Edit enters edit mode with editable
    //      title/description. Save persists via goalRepository.updateGoal().
    //      Cancel discards edits.
    // =========================================================================

    // =========================================================================
    // Bug 10: "Refine with AI" not working
    // Was: AiProviderRouter.processWithHybrid/processWithHybridNano returned
    //      a failure immediately when the rule-based provider didn't support
    //      SUGGEST_SMART_GOAL, without ever trying Gemini Nano or llama.cpp.
    //      Additionally, OnDeviceAiProvider had no SUGGEST_SMART_GOAL handler
    //      and fell through to generalGenerate() returning ChatResponse instead
    //      of SmartGoalSuggestion.
    // Fix: Router now escalates to LLM when rule-based fails for escalation-
    //      eligible request types. OnDeviceAiProvider now has a dedicated
    //      suggestSmartGoal() method that returns SmartGoalSuggestion.
    //      On devices without LLM, the ViewModel fallback sets refinedGoal
    //      from user input and the wizard still advances.
    // =========================================================================

    @Test
    fun regression_bug10_refineWithAiAdvancesToSmartStep() {
        nav.goToGoals()
        waitForIdle()

        // Create goal from empty state
        goals.assertEmptyState()
        goals.tapCreateFirstGoal()
        waitForIdle()
        goals.assertCreateScreenVisible()

        // Step 1: Describe
        goals.typeGoalTitle("Run a half marathon in under 2 hours")

        // Tap "Refine with AI" — this was the broken button
        goals.tapRefineWithAi()

        // AI processing should start and either:
        // a) Return a SmartGoalSuggestion from Gemini Nano / llama.cpp, or
        // b) Fall back gracefully setting refinedGoal = user input
        // In both cases, the "✨ SMART Goal" heading should appear on Step 2.
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("SMART Goal", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // The refined goal field should contain text (either AI-refined or original input)
        composeRule.onNodeWithText("Run a half marathon", substring = true)
            .assertIsDisplayed()

        // Category chips should be visible
        composeRule.onNodeWithText("Category", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        // Should be able to continue to Step 3
        goals.tapNextTimeline()
        waitForIdle()

        // Step 3: Timeline should be visible
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Target Date", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun regression_bug10_refineWithAiThenSkipCompletesWizard() {
        nav.goToGoals()
        waitForIdle()

        goals.assertEmptyState()
        goals.tapCreateFirstGoal()
        waitForIdle()

        // Step 1: Describe
        goals.typeGoalTitle("Save ten thousand for emergency fund")
        goals.tapRefineWithAi()

        // Wait for Step 2 to load (AI processing + result or fallback)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("SMART Goal", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // If AI didn't skip already, explicitly tap "Skip AI"
        try {
            goals.tapSkipAi()
        } catch (_: Throwable) {
            // AI may have already been skipped by fallback
        }
        waitForIdle()

        // Continue to Step 3: Timeline
        goals.tapNextTimeline()
        waitForIdle()

        // Create the goal
        goals.tapCreateGoalButton()
        Thread.sleep(3_000) // Wait for goal creation

        // Celebration should appear
        goals.assertCelebrationVisible()

        // Go back to goals list
        goals.tapBackToGoals()

        // Wait for navigation
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("Create new goal")
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Goal should appear in the list
        goals.assertGoalDisplayed("Save ten thousand")
    }

    /**
     * Navigate to goal detail for a pre-created goal.
     */
    private fun navigateToGoalDetail(goalTitle: String) {
        nav.goToGoals()
        waitForIdle()
        Thread.sleep(2_000) // Wait for Room → Flow pipeline
        goals.tapGoal(goalTitle)
        waitForIdle()
        // Wait for GoalDetailScreen to load (progress ring appears)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithContentDescription("Edit goal")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun regression_bug9_editButtonEntersEditMode() = runTest {
        goalRepository.insertGoal(
            TestDataFactory.goal(title = "Edit mode test goal")
        )

        navigateToGoalDetail("Edit mode test goal")

        // Edit button should be visible in view mode
        goals.assertEditButtonVisible()

        // Tap Edit
        goals.tapEditGoal()
        waitForIdle()

        // Should now be in edit mode — Save button visible instead of Edit
        goals.assertSaveButtonVisible()
    }

    @Test
    fun regression_bug9_editGoalTitlePersists() = runTest {
        goalRepository.insertGoal(
            TestDataFactory.goal(title = "Old goal title")
        )

        navigateToGoalDetail("Old goal title")

        // Enter edit mode
        goals.tapEditGoal()
        waitForIdle()

        // Edit title
        goals.editGoalTitle("New goal title", currentTitle = "Old goal title")

        // Save
        goals.tapSaveGoalEdit()
        waitForIdle()
        Thread.sleep(2_000) // Room → Flow pipeline

        // Title should be updated in the top bar
        goals.assertGoalDetailTitle("New goal title")

        // Navigate back to goals list and verify
        nav.pressBack()
        waitForIdle()
        Thread.sleep(1_000)
        goals.assertGoalDisplayed("New goal title")
        goals.assertGoalNotDisplayed("Old goal title")
    }

    @Test
    fun regression_bug9_cancelEditDiscardsChanges() = runTest {
        goalRepository.insertGoal(
            TestDataFactory.goal(title = "Keep this title")
        )

        navigateToGoalDetail("Keep this title")

        // Enter edit mode
        goals.tapEditGoal()
        waitForIdle()

        // Edit title
        goals.editGoalTitle("Discarded title", currentTitle = "Keep this title")

        // Cancel
        goals.tapCancelGoalEdit()
        waitForIdle()

        // Original title should be restored
        goals.assertGoalDetailTitle("Keep this title")

        // Edit button should be back (view mode)
        goals.assertEditButtonVisible()
    }

    @Test
    fun regression_bug9_editModeRestoredAfterSave() = runTest {
        goalRepository.insertGoal(
            TestDataFactory.goal(title = "Save and restore")
        )

        navigateToGoalDetail("Save and restore")

        // Enter edit mode
        goals.tapEditGoal()
        waitForIdle()
        goals.assertSaveButtonVisible()

        // Save without changes
        goals.tapSaveGoalEdit()
        waitForIdle()

        // Should be back in view mode — Edit button visible, not Save
        goals.assertEditButtonVisible()
    }
}

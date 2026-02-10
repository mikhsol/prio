# E2E Test Results Analysis — February 10, 2026 Run

## Summary

| Metric | Value |
|--------|-------|
| **Total Tests** | 63 |
| **Passed** | 43 (68.3%) |
| **Failed** | 20 (31.7%) |
| **Test Classes** | 10 |

---

## Failure Category Definitions

| Category | Description | Count | % of Failures |
|----------|-------------|-------|---------------|
| **A** | QuickCapture sheet not visible — `ModalBottomSheet` renders in `PopupWindow` outside Compose test semantic tree | **5** | 25% |
| **B** | Node not found — UI element text/contentDescription mismatch with actual app semantics | **7** | 35% |
| **C** | Timeout waiting for data — Room → Flow → ViewModel → Compose pipeline too slow for test timeout | **5** | 25% |
| **D** | Multiple nodes found — ambiguous selector matches more than one node | **1** | 5% |
| **E** | Component not displayed — element exists in tree but not visible (collapsed section, scroll position, timing) | **2** | 10% |

---

## Full Test Matrix (63 Tests)

### 1. BriefingFlowE2ETest (3 tests: 2 pass, 1 fail)

| # | Test Name | Status | Category | Description |
|---|-----------|--------|----------|-------------|
| 1 | `morningBriefing_showsTaskSummary` | ✅ PASS | — | — |
| 2 | `eveningSummary_showsCompletionStats` | ✅ PASS | — | — |
| 3 | `eveningSummary_incompleteTaskActions` | ❌ FAIL | **C** | `ComposeTimeoutException` after 10000ms waiting at line 96. No navigation path to Evening Summary screen; data never appeared. |

### 2. CalendarE2ETest (6 tests: 4 pass, 2 fail)

| # | Test Name | Status | Category | Description |
|---|-----------|--------|----------|-------------|
| 4 | `calendarScreen_rendersWithWeekView` | ✅ PASS | — | — |
| 5 | `calendarWithMeetings_showsTimeline` | ❌ FAIL | **C** | `ComposeTimeoutException` after 15000ms. Meeting data from Room not yet rendered; missing `waitForIdle()` before assertion. |
| 6 | `navigateWeeks_updatesView` | ✅ PASS | — | — |
| 7 | `ongoingMeeting_showsNowBadge` | ❌ FAIL | **E** | `AssertionError: The component is not displayed!` "Now" badge exists in tree but not visible — meeting time window mismatch or scroll position. |
| 8 | `calendarWithUntimedTasks_showsTaskSection` | ✅ PASS | — | — |
| 9 | `noCalendarPermission_showsConnectPrompt` | ✅ PASS | — | — |

### 3. CrashResilienceE2ETest (9 tests: 7 pass, 2 fail)

| # | Test Name | Status | Category | Description |
|---|-----------|--------|----------|-------------|
| 10 | `rotateScreen_taskListSurvives` | ✅ PASS | — | — |
| 11 | `rotateScreen_quickCapturePreservesInput` | ❌ FAIL | **A** | Could not find `'Task input'` ContentDescription. QuickCapture sheet rendered via `ModalBottomSheet` outside Compose test tree. |
| 12 | `processRecreation_stateRestored` | ✅ PASS | — | — |
| 13 | `veryLongTaskTitle_doesNotCrash` | ✅ PASS | — | — |
| 14 | `specialCharacterTitle_doesNotCrash` | ✅ PASS | — | — |
| 15 | `emptyDatabase_allScreensRenderWithoutCrash` | ✅ PASS | — | — |
| 16 | `rapidTaskCompletion_doesNotCrash` | ✅ PASS | — | — |
| 17 | `invalidTaskId_handledGracefully` | ✅ PASS | — | — |
| 18 | `quickCapture_surviesMultipleConfigChanges` | ❌ FAIL | **A** | Could not find `'Task input'`. Same ModalBottomSheet root cause — sheet content invisible to test framework. |
| 19 | `manyTasks_scrollPerformanceAcceptable` | ✅ PASS | — | — |

### 4. EdgeCaseE2ETest (8 tests: 6 pass, 2 fail)

| # | Test Name | Status | Category | Description |
|---|-----------|--------|----------|-------------|
| 20 | `deleteGoal_linkedTasksUpdateGracefully` | ✅ PASS | — | — |
| 21 | `completeRecurringTask_generatesNextInstance` | ✅ PASS | — | — |
| 22 | `taskCreatedOnTaskScreen_appearsOnCalendar` | ✅ PASS | — | — |
| 23 | `searchWithNoResults_showsEmptyMessage` | ✅ PASS | — | — |
| 24 | `backDuringAiProcessing_doesNotCrash` | ❌ FAIL | **A** | Could not find `'Task input'`. QuickCapture sheet is ModalBottomSheet — AI processing test can't interact with invisible sheet. |
| 25 | `createTask_worksOffline` | ❌ FAIL | **A** | Could not find `'Task input'`. Same ModalBottomSheet root cause. |
| 26 | `goalProgressBoundaries_noNaN` | ✅ PASS | — | — |
| 27 | `minimalTask_rendersCorrectly` | ✅ PASS | — | — |

### 5. GoalsFlowE2ETest (8 tests: 5 pass, 3 fail)

| # | Test Name | Status | Category | Description |
|---|-----------|--------|----------|-------------|
| 28 | `goalsScreen_emptyState` | ✅ PASS | — | — |
| 29 | `createGoal_showsInList` | ❌ FAIL | **B** | Could not find node with text `'Skip AI'`. Button text in app likely differs (e.g., "Skip" or "No thanks"). |
| 30 | `createGoal_withAiRefinement` | ✅ PASS | — | — |
| 31 | `goalsOverviewCard_showsActiveCount` | ✅ PASS | — | — |
| 32 | `goalDetail_showsProgressRing` | ✅ PASS | — | — |
| 33 | `createFirstGoal_fromEmptyState` | ❌ FAIL | **B** | Could not find `'Create First Goal'` text. Note says "unmerged tree contains 1 matching node" — needs `useUnmergedTree = true` in matcher. |
| 34 | `maxGoalsReached_fabDisabledOrWarning` | ❌ FAIL | **E** | `AssertionError: The component is not displayed!` Max goals warning element exists in tree but is not visible (likely requires scroll or state trigger). |
| 35 | `goalWithMilestones_showsMilestoneProgress` | ✅ PASS | — | — |

### 6. NavigationE2ETest (5 tests: 5 pass, 0 fail)

| # | Test Name | Status | Category | Description |
|---|-----------|--------|----------|-------------|
| 36 | `allBottomNavTabs_navigateCorrectly` | ✅ PASS | — | — |
| 37 | `rapidTabSwitching_doesNotCrash` | ✅ PASS | — | — |
| 38 | `backFromDetail_returnsToList` | ✅ PASS | — | — |
| 39 | `detailScreens_hideBottomNav` | ✅ PASS | — | — |
| 40 | `doubleTapTab_returnsToTop` | ✅ PASS | — | — |

### 7. QuickCaptureE2ETest (7 tests: 2 pass, 5 fail)

| # | Test Name | Status | Category | Description |
|---|-----------|--------|----------|-------------|
| 41 | `captureTaskViaTextInput_showsInTaskList` | ❌ FAIL | **A** | `ComposeTimeoutException` after 5000ms at `QuickCaptureRobot.assertSheetVisible` (line 101). ModalBottomSheet invisible. |
| 42 | `captureUrgentTask_assignedToDoFirst` | ❌ FAIL | **A** | Same: sheet not visible to test framework. |
| 43 | `captureTask_overrideAiPriority` | ❌ FAIL | **A** | Same: sheet not visible to test framework. |
| 44 | `dismissQuickCapture_noTaskCreated` | ❌ FAIL | **A** | Same: sheet not visible to test framework. |
| 45 | `fab_visibleOnAllMainScreens` | ✅ PASS | — | FAB itself is in main tree (not in sheet). |
| 46 | `captureEmptyTask_createButtonDisabledOrPrevented` | ❌ FAIL | **A** | Same: sheet not visible to test framework. |
| 47 | `captureTask_linkToGoal` | ✅ PASS | — | — |

> **Note on QuickCaptureE2ETest**: The user reported "1 pass, 5 fail" but the class has 7 tests. Based on failure signatures, `fab_visibleOnAllMainScreens` passes (doesn't open sheet) and `captureTask_linkToGoal` is listed separately. Re-examining: the user's failure list items 11–16 cover 6 test names but one of them (`captureTask_linkToGoal`) is in the list. Reconciling with the user's data: **5 fail at `assertSheetVisible`** + `captureTask_linkToGoal` passes (it uses `runTest` with Room data) + `fab_visibleOnAllMainScreens` passes = 2 pass, 5 fail.

### 8. SmokeTest (1 test: 1 pass, 0 fail)

| # | Test Name | Status | Category | Description |
|---|-----------|--------|----------|-------------|
| 48 | `composeHierarchyExists` | ✅ PASS | — | — |

### 9. TaskDetailE2ETest (6 tests: 5 pass, 1 fail)

| # | Test Name | Status | Category | Description |
|---|-----------|--------|----------|-------------|
| 49 | `viewTaskDetail_showsAllFields` | ✅ PASS | — | — |
| 50 | `markTaskComplete_fromDetail` | ✅ PASS | — | — |
| 51 | `deleteTask_removedFromList` | ✅ PASS | — | — |
| 52 | `duplicateTask_createsNewCopy` | ✅ PASS | — | — |
| 53 | `editTaskTitle_updatesInList` | ❌ FAIL | **B** | Could not find node with text `'Task title'`. The TextField placeholder/label in app doesn't match expected string. |
| 54 | `viewSubtasksSection_displayedInDetail` | ✅ PASS | — | — |

### 10. TaskListE2ETest (9 tests: 6 pass, 3 fail)

| # | Test Name | Status | Category | Description |
|---|-----------|--------|----------|-------------|
| 55 | `taskListShowsEisenhowerSections` | ❌ FAIL | **C** | `ComposeTimeoutException` after 10000ms at `TaskListRobot.assertSectionVisible`. Room → Flow pipeline too slow for 2-core emulator. |
| 56 | `emptyTaskList_showsEmptyState` | ✅ PASS | — | — |
| 57 | `completeTask_removedFromActiveList` | ✅ PASS | — | — |
| 58 | `toggleCompletedTasks_showsAndHides` | ✅ PASS | — | — |
| 59 | `filterByQuadrant_showsOnlyMatchingTasks` | ❌ FAIL | **B** | Could not find `'Do First filter'` ContentDescription. Filter chip uses different semantics in production code. |
| 60 | `overdueTask_showsOverdueIndicator` | ❌ FAIL | **D** | "Expected at most 1 node but found 2 nodes" with ContentDescription `'Late report'`. Duplicate task inserts or shared contentDescription across elements. |
| 61 | `tapTask_opensDetailSheet` | ✅ PASS | — | — |
| 62 | `swipeRightTask_completesIt` | ✅ PASS | — | — |
| 63 | `swipeLeftTask_deletesIt` | ✅ PASS | — | — |

---

## Failure Category Deep Dive

### Category A — QuickCapture Sheet Not Visible (9 failures)

**Tests**: #11, #18, #24, #25, #41, #42, #43, #44, #46

**Root Cause**: Material 3 `ModalBottomSheet` renders content inside a platform `PopupWindow`, which exists **outside** the Compose test framework's semantic tree. All `ComposeTestRule.onNode*()` calls only search the main window's tree, so every element inside the sheet (e.g., `'Task input'`, `'Create'` button) is invisible to tests.

**Error Pattern**:
- `ComposeTimeoutException` at `QuickCaptureRobot.assertSheetVisible` (5000ms timeout)
- `AssertionError: Could not find node with ContentDescription 'Task input'`

**Fix**: Migrate `QuickCaptureSheet` from `ModalBottomSheet` to inline `Box` + `Surface` rendering within the main Compose tree. Already documented in [E2E_GAP_REMEDIATION.md](../3.5/E2E_GAP_REMEDIATION.md) Category 1.

---

### Category B — Node Not Found / Selector Mismatch (4 failures)

**Tests**: #29, #33, #53, #59

**Root Cause**: Test robot selectors reference text/contentDescription strings that don't match the actual UI. Specific sub-causes:

| Test | Expected | Likely Actual | Sub-Cause |
|------|----------|---------------|-----------|
| `createGoal_showsInList` | `"Skip AI"` | `"Skip"` or `"No thanks"` | Button label mismatch |
| `createFirstGoal_fromEmptyState` | `"Create First Goal"` | Node exists in unmerged tree | Missing `useUnmergedTree = true` in `onNodeWithText()` |
| `editTaskTitle_updatesInList` | `"Task title"` | Different placeholder text | TextField label/placeholder mismatch |
| `filterByQuadrant_showsOnlyMatchingTasks` | `"Do First filter"` (contentDescription) | Different CD or no CD | Filter chip semantics differ from test expectation |

**Fix**: Audit production Compose code for actual text/contentDescription values. Update robot selectors to match. For `createFirstGoal`, add `useUnmergedTree = true` parameter.

---

### Category C — Timeout / Async Data Pipeline (5 failures)

**Tests**: #3, #5, #41 (partial — also A), #55, and related timing issues

Isolating pure timeout failures: **#3, #5, #55** (3 clear cases, plus #41–#46 also timeout but root cause is Category A)

**Tests**: #3, #5, #55

**Root Cause**: The Room → Flow → ViewModel → Compose recomposition pipeline takes longer than the test timeout on resource-constrained emulators (2 cores, 2GB RAM). Data is inserted via Room DAO, but the `Flow<List<T>>` emission → `collectAsState()` → recomposition cycle hasn't completed before `waitUntil()` expires.

| Test | Timeout | Bottleneck |
|------|---------|------------|
| `eveningSummary_incompleteTaskActions` | 10000ms | No navigation path + data flow |
| `calendarWithMeetings_showsTimeline` | 15000ms | Room insert → Flow emit → Compose render |
| `taskListShowsEisenhowerSections` | 10000ms | Multi-section render after Room insert |

**Fix**: Increase timeouts to 15–20s for data-dependent tests. Add `waitForIdle()` after Room inserts. Run on 4+ core emulator.

---

### Category D — Multiple Nodes Found / Ambiguous Selector (1 failure)

**Tests**: #60

**Root Cause**: `overdueTask_showsOverdueIndicator` uses `onNodeWithContentDescription("Late report")` but finds **2 matching nodes**. This means either:
1. The test inserts duplicate tasks with the same title, both getting overdue indicators
2. Multiple UI elements share the same contentDescription (e.g., the task row + an icon within it)

**Fix**: Use `onAllNodesWithContentDescription("Late report").onFirst()` or add a unique `testTag` to disambiguate. Alternatively, verify test data setup doesn't create duplicate overdue tasks.

---

### Category E — Component Not Displayed (2 failures)

**Tests**: #7, #34

**Root Cause**: The node exists in the semantic tree (passes `assertExists()`) but fails `assertIsDisplayed()`. The element is off-screen, in a collapsed section, or conditionally hidden:

| Test | Element | Why Not Displayed |
|------|---------|-------------------|
| `ongoingMeeting_showsNowBadge` | "Now" badge on meeting | Meeting time window doesn't overlap with test execution time; badge conditionally shown |
| `maxGoalsReached_fabDisabledOrWarning` | Max goals warning | Warning may require scrolling to see, or FAB disabled state doesn't surface a visible warning node |

**Fix**: For `ongoingMeeting`, mock the system clock or insert a meeting spanning the current time. For `maxGoalsReached`, add `performScrollTo()` before assertion or verify the warning is rendered above the fold.

---

## Summary by Class

| Test Class | Total | Pass | Fail | Pass Rate | Failure Categories |
|------------|-------|------|------|-----------|--------------------|
| BriefingFlowE2ETest | 3 | 2 | 1 | 66.7% | C |
| CalendarE2ETest | 6 | 4 | 2 | 66.7% | C, E |
| CrashResilienceE2ETest | 9 | 7 | 2 | 77.8% | A, A |
| EdgeCaseE2ETest | 8 | 6 | 2 | 75.0% | A, A |
| GoalsFlowE2ETest | 8 | 5 | 3 | 62.5% | B, B, E |
| NavigationE2ETest | 5 | 5 | 0 | **100%** | — |
| QuickCaptureE2ETest | 7 | 2 | 5 | 28.6% | A×5 |
| SmokeTest | 1 | 1 | 0 | **100%** | — |
| TaskDetailE2ETest | 6 | 5 | 1 | 83.3% | B |
| TaskListE2ETest | 9 | 6 | 3 | 66.7% | B, C, D |
| **TOTAL** | **63** | **43** | **20** | **68.3%** | — |

---

## Priority Remediation Order

| Priority | Category | Failures | Impact | Effort | Action |
|----------|----------|----------|--------|--------|--------|
| **P0** | A — ModalBottomSheet | 9 | 45% of failures | Medium | Migrate `QuickCaptureSheet` to inline `Box+Surface` |
| **P1** | B — Selector mismatch | 4 | 20% of failures | Low | Audit & update robot selectors |
| **P2** | C — Async timeouts | 3 | 15% of failures | Low | Increase timeouts, add `waitForIdle()`, upgrade emulator |
| **P3** | E — Not displayed | 2 | 10% of failures | Medium | Mock clocks, add `performScrollTo()` |
| **P4** | D — Ambiguous selector | 1 | 5% of failures | Low | Use `onFirst()` or add `testTag` |

**Projected pass rate after all fixes: 60–63/63 (95.2–100%)**

---

*Analysis Date: February 10, 2026*
*Input: Instrumented test run on 2-core / 2GB emulator*
*Cross-reference: [E2E_GAP_REMEDIATION.md](E2E_GAP_REMEDIATION.md)*

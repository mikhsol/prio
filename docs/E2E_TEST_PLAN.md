# Prio — End-to-End Test Plan

## Document Metadata

| Field | Value |
|-------|-------|
| **Created** | February 6, 2026 |
| **Last Updated** | February 10, 2026 |
| **Phase** | Phase 3 Gap Analysis + E2E Test Planning |
| **Authors** | Product Manager, UX Designer, Android Developer |
| **Status** | Gap Remediation Complete — Ready for E2E Execution |
| **Target** | Local emulator (primary), Physical device (on demand) |

---

## 1. Executive Summary

This document is the result of a cross-functional analysis of **Phase 3: Feature Plugins** implementation against the original requirements (22 user stories across TM, GL, CB families), UX design specs (1.1.x series), and Android best practices. It defines a comprehensive E2E test plan that covers:

1. **All documented user scenarios** (22 user stories)
2. **Undocumented edge cases** (inferred from code analysis)
3. **UI/UX problems** (identified via source code audit)
4. **Typical Android crash scenarios** (platform-specific failure modes)

**Key Finding**: Phase 3 feature code is ~95% complete but has significant **test coverage gaps**, **dead UI paths**, and **unhandled edge cases** that must be validated before Phase 4/5.

> **📋 Update (Feb 10, 2026)**: All **5 Critical** and **10 High** gaps have been fixed. Additionally **5 Medium** and **3 Low** gaps were resolved. The ModalBottomSheet test-framework blocker was eliminated by migrating to inline bottom sheets. E2E test robots and scenarios were updated to match corrected selectors. See §2.2 for per-gap status.

---

## 2. Phase 3 Gap Analysis

### 2.1 Implementation vs. Requirements Matrix

| User Story | Priority | Implemented | Gaps / Issues |
|------------|----------|-------------|---------------|
| **TM-001** Quick Task Capture | P0 | ✅ | Voice input works; FAB visible on all tabs ✅ |
| **TM-002** NL Task Parsing | P0 | ✅ | ~~`error` state defined but **never displayed**~~ → ✅ FIXED (GAP-H09): error Card + Retry in QuickCaptureSheet |
| **TM-003** AI Eisenhower Classification | P0 | ✅ | AI explanation shows in TaskDetail ✅ |
| **TM-004** Task List with Priority | P0 | ✅ | ~~No explicit **error state**~~ → ✅ FIXED (GAP-M01): error state + Retry on TaskList, Goals, Calendar |
| **TM-005** Deadline Urgency Scoring | P0 | ✅ | Urgency auto-updates ✅; notification at 1d/3d ✅ |
| **TM-006** Complete/Edit/Delete | P0 | ✅ | ~~Snackbar "View" action **does nothing**~~ → ✅ FIXED (GAP-H02): navigates to task detail |
| **TM-007** Subtasks/Checklist | P1 | ✅ | ~~"Add subtask" button **does nothing**~~ → ✅ FIXED (GAP-H03): AddSubtaskDialog wired |
| **TM-008** Recurring Tasks | P1 | ✅ | WorkManager auto-creation ✅; date calculation tested ✅ |
| **TM-009** Smart Reminders | P1 | ✅ | 3 channels, snooze, quiet hours ✅; ~~No `BOOT_COMPLETED` receiver~~ → ✅ FIXED (GAP-C02): BootReceiver added |
| **TM-010** Override AI Classification | P0 | ✅ | Quadrant selector in TaskDetail ✅; override tracking TODO (P3) |
| **GL-001** Create Goal + AI | P0 | ✅ | 3-step wizard, SMART suggestions, max 10 goals ✅ |
| **GL-002** Goal Progress Viz | P0 | ✅ | Progress ring, color status ✅; ~~`COMPLETED` maps to `ON_TRACK`~~ → ✅ FIXED (GAP-C03): COMPLETED enum added |
| **GL-003** Link Tasks to Goals | P0 | ✅ | GoalPicker in QuickCapture + TaskDetail ✅ |
| **GL-004** Goal Milestones | P1 | ✅ | Timeline, 0-5 milestones, check-off ✅ |
| **GL-005** Goal Dashboard | P0 | ✅ | Stats, category filters, at-risk sorting ✅ |
| **GL-006** Goal Analytics | P1 | ⚠️ Partial | Analytics tab exists; no export functionality |
| **CB-001** Morning Briefing | P0 | ✅ | Greeting, top 3, schedule, goal spotlight, AI insight ✅ |
| **CB-002** Calendar Read Integration | P0 | ✅ | ContentProvider, multi-calendar, color-coded ✅ |
| **CB-003** Evening Summary | P1 | ✅ | ~~`ShowError` effect **silently dropped**; "Close Day" animation has **no exit path**~~ → ✅ FIXED (GAP-H04): Done button + 4s auto-dismiss |
| **CB-004** Meeting Notes + AI | P1 | ✅ | Notes, action items, accept/reject ✅ |
| **CB-005** Calendar Day View | P0 | ✅ | Timeline, events, tasks, NOW indicator ✅ |
| **CB-006** Smart Briefing Insights | P1 | ✅ | Rule-based insights, no-repeat 7d dedup ✅ |

### 2.2 Critical Gaps Identified

#### 🔴 Critical (Crash / Data Loss Risk) — ✅ ALL 5 FIXED

| ID | Gap | Source | Impact | Status |
|----|-----|--------|--------|--------|
| GAP-C01 | **CrashReportingTree is a no-op** — zero crash reporting in production | `PrioApplication.kt` | Crashes in production are invisible | ✅ FIXED — Uncommented FirebaseCrashlytics calls |
| GAP-C02 | **`RECEIVE_BOOT_COMPLETED`** permission declared, **no BootReceiver registered** | `AndroidManifest.xml` | Reminders/alarms lost after device reboot | ✅ FIXED — Created `BootReceiver.kt` + manifest entry |
| GAP-C03 | **`GoalStatus.COMPLETED` maps to `ON_TRACK`** in GoalsList UI | `GoalsListScreen.kt` | Completed goals show wrong status indicator | ✅ FIXED — Added `COMPLETED` to UI GoalStatus enum |
| GAP-C04 | **`averageProgress` can be `NaN`** (0/0 division) → crash in `CircularProgressIndicator` | `GoalsListScreen.kt` | App crash with 0 active goals | ✅ FIXED — Guarded with `.takeIf { it.isFinite() } ?: 0f` |
| GAP-C05 | **Navigation ID fallback to `0L`** for missing arguments | `PrioNavHost.kt` | Silent bad state — detail screens show wrong/empty data | ✅ FIXED — Changed to `?: error("...required")` |

#### 🟠 High (Broken UX / Lost Functionality) — ✅ ALL 10 FIXED

| ID | Gap | Source | Impact | Status |
|----|-----|--------|--------|--------|
| GAP-H01 | **TodayScreen is entirely hardcoded placeholder** | `TodayScreen.kt` | Home tab shows fake data — all click handlers are empty TODOs | ✅ FIXED — Rewrote with `TodayViewModel` + live data |
| GAP-H02 | **Snackbar "View" action does nothing** after task creation | `PrioAppShell.kt` | User taps "View" → nothing happens | ✅ FIXED — Navigates to `NavRoutes.taskDetail(id)` |
| GAP-H03 | **"Add subtask" button does nothing** in TaskDetail | `TaskDetailSheet.kt` | Feature appears available but is non-functional | ✅ FIXED — Added `AddSubtaskDialog` composable |
| GAP-H04 | **Evening Summary "Close Day" animation has no exit** | `EveningSummaryScreen.kt` | User stuck on "Day Closed!" overlay forever | ✅ FIXED — "Done" button + 4s auto-dismiss |
| GAP-H05 | **"More" tab maps to "Today"** in bottom navigation | `PrioAppShell.kt` | "More" tab can never appear selected | ✅ FIXED — Routes to `NavRoutes.MORE` |
| GAP-H06 | **No deep link handling** — notification taps cannot route to screens | `MainActivity.kt` | Briefing/reminder notifications open the default screen | ✅ FIXED — `parseDeepLink()` + `deepLinkRoute` param |
| GAP-H07 | **Onboarding hardcoded to `false`** — first launch goes straight to app | `MainActivity.kt` | New users skip onboarding entirely | ✅ FIXED — Reads `UserPreferencesRepository` |
| GAP-H08 | **`collectAsState()` instead of `collectAsStateWithLifecycle()`** | Briefing screens | Unnecessary data collection when app is backgrounded | ✅ FIXED — Both briefing screens updated |
| GAP-H09 | **QuickCapture `error` state is never displayed** | `QuickCaptureSheet.kt` | If AI parsing fails, user sees no feedback | ✅ FIXED — Error Card + "Retry" button |
| GAP-H10 | **`RECORD_AUDIO` permanent denial** — no settings redirect | `PrioAppShell.kt` | User permanently denied mic with no way to fix | ✅ FIXED — Settings redirect snackbar |

#### 🟡 Medium (Polish / Edge Cases) — 5 FIXED, 5 DEFERRED

| ID | Gap | Source | Impact | Status |
|----|-----|--------|--------|--------|
| GAP-M01 | **No error state** in TaskListScreen, GoalsListScreen, CalendarScreen | Multiple | Failed data load = blank screen, no retry | ✅ FIXED — Error state + Retry on all 3 screens |
| GAP-M02 | **Hardcoded colors** in Briefing screens don't respect dark mode | Briefing screens | Dark mode shows light-colored cards | ✅ FIXED — All `Color(0xFF...)` → theme tokens (0 remaining) |
| GAP-M03 | **All UI strings hardcoded** — not in string resources | All screens | No i18n/l10n support possible | ⏭ DEFERRED — Massive refactor, no functional impact for MVP |
| GAP-M04 | **Nested scroll conflicts** — LazyColumn inside ModalBottomSheet | TaskDetail, QuickCapture | Gesture collision between sheet drag and list scroll | ⏭ DEFERRED — Mitigated by inline bottom sheet migration |
| GAP-M05 | **Calendar timeline RTL breaks** — hardcoded pixel offsets | `CalendarScreen.kt` | RTL languages have broken layout | ⏭ DEFERRED — Low priority for MVP market |
| GAP-M06 | **DatePicker not pre-populated** with existing due date | `QuickCaptureSheet.kt` | User loses previous date when editing | ✅ FIXED — `initialSelectedDateMillis` from parsed Instant |
| GAP-M07 | **Force-unwrap `!!`** on nullable state in Briefing screens | Briefing screens | Potential NPE under race conditions | ✅ FIXED — `?: return@Scaffold` safe pattern |
| GAP-M08 | **Dual navigation systems** (PrioRoute + NavRoutes) — dead code drift | `PrioNavigation.kt` | Maintenance burden, potential sync bugs | ⏭ DEFERRED — Cosmetic, existing approach works |
| GAP-M09 | **Version "1.0.0" hardcoded** in About screen | `PrioNavHost.kt` | Will become stale | ✅ FIXED — Uses `BuildConfig.VERSION_NAME` |
| GAP-M10 | **No confetti animation** on task/goal completion | Multiple | TODO markers — visual celebration missing | ⏭ DEFERRED — Post-MVP feature (see TODO.md #15) |

#### 🔵 Low (Accessibility / Best Practice)

| ID | Gap | Source | Impact | Status |
|----|-----|--------|--------|--------|
| GAP-L01 | **Missing content descriptions** on many interactive elements | All screens | Screen reader users cannot use key features | ⏭ DEFERRED — Incremental improvement, not blocking |
| GAP-L02 | **Swipe background icons** have `contentDescription = null` | `TaskListScreen.kt` | No TalkBack feedback on swipe | ✅ FIXED — "Delete task" / "Complete task" descriptions |
| GAP-L03 | **Filter chip** uses enum name instead of `displayName` | `TaskListScreen.kt` | TalkBack reads "HasGoal" not "Has Goal" | ✅ FIXED — Uses `filter.displayName` |
| GAP-L04 | **"Edit" content description** is identical for all ParsedFieldRow buttons | `QuickCaptureSheet.kt` | Ambiguous — should be "Edit title", "Edit date" | ✅ FIXED — "Edit title", "Edit due date", "Edit goal" |
| GAP-L05 | **Touch targets < 48dp** on some "See All" / "Full View" text buttons | Briefing screens | WCAG touch target failure | ⏭ DEFERRED — Requires design system audit |

### 2.3 Missing Test Coverage

| Area | Unit Tests | UI Tests | E2E Tests | Gap |
|------|-----------|----------|-----------|-----|
| EisenhowerEngine | 80 ✅ | — | — | No integration test with real TaskRepository |
| TaskList Screen | 9 (model only) | 0 | 0 | **No rendered UI tests** — 9 tests verify data classes only |
| QuickCapture | 9 (voice) | 0 | 0 | **No capture flow test** |
| TaskDetail | 0 | 0 | 0 | **Zero test coverage** |
| Goals feature | 18 (ViewModel) | 0 | 0 | **No UI tests** |
| Calendar feature | 26 (ViewModel) | 0 | 0 | **No UI tests** |
| Briefings | 0 | 0 | 0 | **Zero test coverage** for BriefingGenerator + ViewModels |
| Analytics | 36 (ViewModel+Repo) | 0 | 0 | No UI tests |
| Navigation | 0 | 0 | 0 | **Zero navigation tests** |
| Cross-feature flows | 0 | 0 | 0 | **No E2E tests exist** |

---

## 3. E2E Test Strategy

### 3.1 Framework Selection

| Framework | Purpose | Rationale |
|-----------|---------|-----------|
| **Compose UI Testing** (primary) | All E2E tests | Native Compose support, Hilt DI integration, CI-ready, no external tools |
| **JUnit 5 + MockK** | Test orchestration + mocking | Already in project, consistent with unit tests |
| **Turbine** | Flow assertion | Already in project for ViewModel testing |

> **Decision**: Maestro was considered (per ACTION_PLAN) but rejected for this phase because:
> 1. All screens are Compose — native test API gives better control
> 2. No Maestro infrastructure exists (zero flows, no CI setup)
> 3. Compose tests can run in emulator AND physical device identically
> 4. Better assertion capabilities for Compose-specific behaviors

### 3.2 Test Environment

| Platform | Configuration | Purpose |
|----------|---------------|---------|
| **Emulator** | Pixel 6, API 34, x86_64 | Primary execution target |
| **Emulator** | Pixel 4a, API 29, x86_64 | Min SDK validation |
| **Physical** | Any ARM64 device, API 29+ | On-demand performance verification |

### 3.3 Resource Constraints

```
CPU limit: 2 cores max
Strategy:
  - Gradle: org.gradle.workers.max=2
  - Emulator: -cores 2
  - Tests: NOT parallelized (sequential execution)
  - Sharding: NOT used (single device)
```

### 3.4 Test Configuration

```properties
# gradle.properties additions
android.testInstrumentationRunnerArguments.numShards=1
org.gradle.workers.max=2
org.gradle.parallel=false
```

```bash
# Emulator launch (2 CPU limit)
emulator -avd Pixel_6_API_34 -cores 2 -memory 4096 -no-snapshot -no-audio -no-window
```

```bash
# Test execution
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.prio.app.e2e \
  --no-parallel \
  -Dorg.gradle.workers.max=2
```

---

## 4. E2E Test Scenarios

### 4.1 Category A — User Story Coverage (Product Manager + Android Developer)

These tests validate every P0 and P1 user story acceptance criterion from Phase 3.

#### A1. Task Quick Capture (TM-001)

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-A1-01 | **Happy path: FAB → type → save** | 1. Launch app 2. Tap Tasks tab 3. Tap FAB 4. Type "Buy groceries" 5. Tap "Create Task" | QuickCapture opens; task appears in task list | P0 |
| E2E-A1-02 | **FAB accessible from all tabs** | 1. Navigate to each tab (Today, Tasks, Goals, Calendar) 2. Verify FAB is visible and tappable | FAB visible and opens QuickCapture from every tab | P0 |
| E2E-A1-03 | **Quick capture timing** | 1. Tap FAB 2. Measure until text field is focused | Focus within 200ms (spec: 100ms) | P0 |
| E2E-A1-04 | **Voice input happy path** | 1. Tap FAB 2. Tap mic icon 3. Grant permission 4. Speak "Call dentist tomorrow" 5. Wait for result | Speech recognized, text populated, AI parsing triggered | P1 |
| E2E-A1-05 | **Quick suggestions** | 1. Tap FAB 2. Verify suggestions appear | Context-appropriate suggestions visible based on time of day | P1 |
| E2E-A1-06 | **Cancel capture** | 1. Tap FAB 2. Type some text 3. Tap "Cancel" / press back | QuickCapture dismissed, no task created | P0 |
| E2E-A1-07 | **Empty input rejected** | 1. Tap FAB 2. Leave text field empty 3. Tap "Create Task" | Task not created; field validation shown or button disabled | P0 |

#### A2. Natural Language Parsing (TM-002)

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-A2-01 | **Date extraction** | 1. Open QuickCapture 2. Type "Call mom tomorrow at 5pm" | Title: "Call mom", Due: tomorrow 5:00 PM parsed and shown | P0 |
| E2E-A2-02 | **Priority signal extraction** | 1. Type "urgent: finish report by Friday" | Title: "finish report", Due: Friday, Priority: Q1 or high | P0 |
| E2E-A2-03 | **Plain text fallback** | 1. Type "buy groceries" | Title: "buy groceries", no date, default quadrant | P0 |
| E2E-A2-04 | **Parsed preview is editable** | 1. Type "Meeting notes tomorrow" 2. Tap date in parsed preview 3. Change date | Date updates in parsed result | P1 |
| E2E-A2-05 | **Quadrant override in preview** | 1. Type "check emails" 2. AI suggests Q3 3. Tap quadrant pills 4. Select Q1 | Quadrant changes to Q1 in preview | P0 |
| E2E-A2-06 | **Goal linking in capture** | 1. Create a goal first 2. Open QuickCapture 3. Tap "Link to Goal" 4. Select a goal | Task saved with goal link visible | P1 |
| E2E-A2-07 | **Date picker in capture** | 1. Open QuickCapture 2. Type task 3. Tap date field 4. Select date from DatePicker | Selected date reflected in parsed result | P1 |

#### A3. AI Eisenhower Classification (TM-003)

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-A3-01 | **Deadline-based Q1 classification** | 1. Create task "Finish report" due today | Task classified as Q1 (Do First / Red badge) | P0 |
| E2E-A3-02 | **Keyword-based Q2 classification** | 1. Create task "Learn Spanish" due next month | Task classified as Q2 (Schedule / Amber badge) | P0 |
| E2E-A3-03 | **Q4 classification** | 1. Create task "Reorganize bookshelf" no deadline | Task classified as Q4 (Eliminate / Gray badge) | P0 |
| E2E-A3-04 | **AI explanation displayed** | 1. Create task 2. Open task detail | One-sentence AI explanation visible in detail | P0 |
| E2E-A3-05 | **Classification speed** | 1. Time from input to badge display | < 2 seconds (rule-based < 100ms) | P0 |

#### A4. Task List View (TM-004)

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-A4-01 | **Priority-sorted sections** | 1. Create tasks in Q1, Q2, Q3, Q4 2. View task list | Tasks grouped by quadrant: DO FIRST → SCHEDULE → DELEGATE → ELIMINATE | P0 |
| E2E-A4-02 | **Quadrant badge colors** | 1. View task list with multi-quadrant tasks | Q1=Red, Q2=Amber, Q3=Orange, Q4=Gray badges displayed | P0 |
| E2E-A4-03 | **Overdue task indicator** | 1. Create task with past due date 2. View list | Task shows overdue warning (left border indicator) | P0 |
| E2E-A4-04 | **Completed toggle** | 1. Complete a task 2. Tap "Show completed" filter | Completed tasks appear with strikethrough | P0 |
| E2E-A4-05 | **Empty state** | 1. Start with no tasks 2. View task list | Encouraging empty state message: "No tasks yet" | P0 |
| E2E-A4-06 | **Section collapse/expand** | 1. Tap section header | Section collapses/expands with animation | P1 |
| E2E-A4-07 | **Search tasks** | 1. Tap search 2. Type partial task name | Matching tasks displayed, non-matching hidden | P1 |
| E2E-A4-08 | **Filter chips** | 1. Tap "Today" filter 2. Tap "Has Goal" filter | List filters to matching tasks only | P1 |
| E2E-A4-09 | **60fps scroll** | 1. Create 50+ tasks 2. Scroll rapidly | No visible frame drops (jank) | P1 |

#### A5. Task CRUD (TM-006)

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-A5-01 | **Complete via checkbox** | 1. Tap checkbox on a task | Task animates to completed, moves to completed section | P0 |
| E2E-A5-02 | **Complete via swipe right** | 1. Swipe right on a task | Green background shown, task completed | P0 |
| E2E-A5-03 | **Delete via swipe left** | 1. Swipe left on a task | Red background shown, task deleted, undo snackbar appears | P0 |
| E2E-A5-04 | **Undo delete within 5s** | 1. Delete task 2. Tap "Undo" on snackbar within 5s | Task restored to list | P0 |
| E2E-A5-05 | **Edit in detail sheet** | 1. Tap a task to open detail 2. Tap "Edit" 3. Change title 4. Tap "Done Editing" | Title updated in detail and list | P0 |
| E2E-A5-06 | **Open task detail** | 1. Tap a task in list | TaskDetailSheet opens with correct task data | P0 |
| E2E-A5-07 | **Overflow menu actions** | 1. Open task detail 2. Tap overflow (⋮) 3. Tap "Duplicate" | Duplicate task created | P1 |

#### A6. Recurring Tasks (TM-008)

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-A6-01 | **Set daily recurrence** | 1. Create task 2. Set to daily recurrence 3. Complete | Next occurrence auto-created for tomorrow | P1 |
| E2E-A6-02 | **Recurrence indicator** | 1. View recurring task in list | Recurrence icon (🔁) visible on card | P1 |
| E2E-A6-03 | **Monthly end-of-month** | 1. Create monthly task on Jan 31 2. Complete | Next: Feb 28 (handles short month) | P1 |

#### A7. Smart Reminders (TM-009)

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-A7-01 | **Reminder notification fires** | 1. Create task with due date + reminder 2. Wait for trigger time | Notification appears with task title | P1 |
| E2E-A7-02 | **Snooze from notification** | 1. Receive reminder 2. Tap "Snooze 1hr" | Notification dismissed, reappears in 1hr | P1 |
| E2E-A7-03 | **Complete from notification** | 1. Receive reminder 2. Tap "Complete" | Task marked complete, notification dismissed | P1 |
| E2E-A7-04 | **Quiet hours** | 1. Set quiet hours 10pm-7am 2. Trigger reminder at 11pm | Notification deferred until 7am | P1 |
| E2E-A7-05 | **Q4 tasks no reminders** | 1. Create Q4 task with due date | No reminder auto-set (unless user explicitly sets one) | P1 |

#### A8. Quadrant Override (TM-010)

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-A8-01 | **Override classification** | 1. Open task detail 2. Tap quadrant badge 3. Select different quadrant | Quadrant changes immediately, badge updates | P0 |
| E2E-A8-02 | **Override persists** | 1. Override quadrant 2. Close detail 3. Reopen | Overridden quadrant persisted | P0 |

#### A9. Goals Feature (GL-001 through GL-005)

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-A9-01 | **Create goal with AI** | 1. Tap Goals tab 2. Tap "+" 3. Enter "Learn Spanish" 4. Wait for AI 5. Accept SMART suggestion 6. Set date + milestones 7. Save | Goal created with AI-refined title, target date, milestones | P0 |
| E2E-A9-02 | **Goal progress from tasks** | 1. Create goal 2. Link 3 tasks 3. Complete 1 task | Goal progress = 33% (1/3) | P0 |
| E2E-A9-03 | **Goal status colors** | 1. Create on-track goal 2. Create behind goal 3. Create at-risk goal | Green, Yellow, Red status indicators | P0 |
| E2E-A9-04 | **Max 10 goals enforcement** | 1. Create 10 goals 2. Try to create 11th | Snackbar: "Maximum 10 active goals reached" | P0 |
| E2E-A9-05 | **Goal milestones** | 1. Create goal with 3 milestones 2. Complete milestone | Milestone checked off, progress updates | P1 |
| E2E-A9-06 | **Goal detail tabs** | 1. Open goal detail 2. Tap Tasks tab 3. Tap Milestones tab 4. Tap Analytics tab | Each tab shows correct content | P1 |
| E2E-A9-07 | **Goal category filter** | 1. Create goals in different categories 2. Tap category filter chip | List filters to selected category | P1 |
| E2E-A9-08 | **Bidirectional goal-task linking** | 1. Link task to goal 2. Open goal detail → Tasks tab | Task appears in goal's linked tasks list | P0 |
| E2E-A9-09 | **100% completion celebration** | 1. Complete all linked tasks for a goal | Confetti animation / celebration shown | P1 |
| E2E-A9-10 | **Goals overview stats** | 1. Create multiple goals 2. View goals list header | Overview card shows: active count, on-track count, at-risk count | P0 |

#### A10. Calendar Feature (CB-002, CB-004, CB-005)

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-A10-01 | **Calendar permission flow** | 1. Tap Calendar tab 2. See permission prompt 3. Grant permission | Calendar events appear in day view | P0 |
| E2E-A10-02 | **Day view with events** | 1. Ensure device has calendar events 2. View calendar | Events show in timeline with title, time, color by source | P0 |
| E2E-A10-03 | **Navigate days** | 1. Swipe left on day view | Next day's events displayed | P0 |
| E2E-A10-04 | **Meeting detail read-only** | 1. Tap a calendar event | Event detail shows (read-only): title, time, location, attendees | P0 |
| E2E-A10-05 | **Meeting notes** | 1. Open meeting detail 2. Tap "Add Notes" 3. Type notes 4. Wait | Notes auto-saved (debounce 1s) | P1 |
| E2E-A10-06 | **AI action item extraction** | 1. Add meeting notes with action items 2. Save | AI extracts action items, shows as task suggestions | P1 |
| E2E-A10-07 | **Accept extracted action item** | 1. Extract action items 2. Tap "Accept" on one | Task created with meeting link | P1 |
| E2E-A10-08 | **Privacy messaging** | 1. View calendar permission dialog | "Your calendar data stays on your device" message visible | P0 |
| E2E-A10-09 | **NOW indicator** | 1. View today's calendar | Red "NOW" line at current time position | P1 |
| E2E-A10-10 | **Permission denied gracefully** | 1. Deny calendar permission | Empty state with "Connect Calendar" prompt, no crash | P0 |

#### A11. Daily Briefings (CB-001, CB-003)

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-A11-01 | **Morning briefing content** | 1. Navigate to Today tab 2. Open morning briefing | Greeting, top 3 priorities, schedule preview, goal spotlight, AI insight | P0 |
| E2E-A11-02 | **"Start My Day" CTA** | 1. Open morning briefing 2. Tap "Start My Day" | Briefing dismissed, dashboard shown | P0 |
| E2E-A11-03 | **Evening summary content** | 1. Open evening summary | Completed tasks, not-done tasks, goal progress, tomorrow preview, AI reflection | P0 |
| E2E-A11-04 | **Move task to tomorrow** | 1. Open evening summary 2. Select "Move to tomorrow" on a not-done task 3. Tap "Close Day" | Task due date updated to tomorrow | P0 |
| E2E-A11-05 | **"Close Day" CTA** | 1. Open evening summary 2. Tap "Close Day" | Close day animation plays + app returns to usable state | P0 |
| E2E-A11-06 | **Briefing offline** | 1. Disable network 2. Open briefing | Briefing generates from local data (no weather) | P0 |
| E2E-A11-07 | **End-of-day nudge** | 1. Configure end-of-day nudge 2. Wait for trigger | "Time to disconnect!" nudge appears | P1 |

#### A12. Analytics (Milestone 3.5)

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-A12-01 | **Analytics screen loads** | 1. Navigate to Insights (from More tab) | Weekly stats, completion chart, streaks, goals summary visible | P1 |
| E2E-A12-02 | **Data reflects actions** | 1. Complete 3 tasks 2. Navigate to Insights | Completed count includes the 3 tasks | P1 |
| E2E-A12-03 | **Streak tracking** | 1. Complete at least 1 task per day for 3 days 2. Check streak | Current streak shows 3 | P1 |

---

### 4.2 Category B — Undocumented Application Use Cases (Product Manager + Android Developer)

These test edge cases and real-world usage patterns that are not covered by user stories.

#### B1. Cross-Feature Interactions

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-B1-01 | **Task→Goal progress chain** | 1. Create goal 2. Create 5 tasks linked to goal 3. Complete 3 tasks | Goal progress = 60%, status updates accordingly | P0 |
| E2E-B1-02 | **Delete goal with linked tasks** | 1. Create goal 2. Link 3 tasks 3. Delete goal | Tasks remain (unlinked), goal removed from list | P0 |
| E2E-B1-03 | **Briefing reflects today's data** | 1. Create 5 tasks (2 Q1, 3 Q2) 2. Open morning briefing | Briefing shows the 2 Q1 + top Q2 task as top 3 | P0 |
| E2E-B1-04 | **Evening summary reflects day's work** | 1. Complete 3 tasks 2. Leave 2 incomplete 3. Open evening summary | Shows 3 completed, 2 not done with action options | P0 |
| E2E-B1-05 | **Calendar event → meeting notes → task** | 1. View calendar event 2. Add notes 3. AI extracts action 4. Accept 5. View in task list | Task in list linked to meeting | P0 |
| E2E-B1-06 | **Quick capture → auto-classify → goal-link** | 1. Create goal "Get fit" 2. Quick capture "Go to gym tomorrow" | AI classifies (Q2), potentially suggests "Get fit" goal link | P1 |
| E2E-B1-07 | **Analytics updates after task operations** | 1. Check insights 2. Complete a task 3. Re-check insights | Completion count incremented | P1 |

#### B2. Data Integrity Scenarios

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-B2-01 | **Rapid task creation (10 tasks)** | 1. Create 10 tasks quickly via QuickCapture | All 10 tasks appear in list, correctly classified | P0 |
| E2E-B2-02 | **Create-delete-undo-create cycle** | 1. Create task A 2. Delete task A 3. Undo 4. Create task B | Both A and B present in list | P0 |
| E2E-B2-03 | **Concurrent edit scenario** | 1. Open task detail 2. Simultaneously trigger reminder worker | Task data remains consistent (no race corruption) | P1 |
| E2E-B2-04 | **Database migration survival** | 1. Insert data 2. Simulate schema migration | Data preserved after migration | P1 |
| E2E-B2-05 | **Large dataset: 500 tasks** | 1. Pre-populate 500 tasks 2. Open task list 3. Search | App doesn't ANR; search returns results within 1s | P1 |
| E2E-B2-06 | **Large dataset: 500 tasks scroll** | 1. Pre-populate 500 tasks 2. Scroll to bottom | Smooth scrolling, no OOM | P1 |

#### B3. User Workflow Scenarios

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-B3-01 | **Full day workflow** | 1. Open morning briefing 2. Create 3 tasks 3. Complete 2 4. Check analytics 5. Open evening summary 6. Close day | Complete flow works end-to-end | P0 |
| E2E-B3-02 | **Goal achievement journey** | 1. Create goal with milestones 2. Create linked tasks 3. Complete tasks 4. Complete milestones 5. Achieve 100% | Progress tracks from 0→100%, celebration at finish | P0 |
| E2E-B3-03 | **Privacy-conscious user (Maya)** | 1. Skip all account prompts 2. Deny network 3. Use all features | App fully functional without network, no data leaves device | P0 |
| E2E-B3-04 | **Power user multi-tab workflow** | 1. Task list → create task 2. Goals → link task 3. Calendar → add notes 4. Today → check briefing | Navigation maintains state across all tabs | P1 |

---

### 4.3 Category C — UI/UX Edge Cases (Product Manager + UX Designer + Android Developer)

#### C1. Input Edge Cases

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-C1-01 | **Very long task title (500 chars)** | 1. Create task with 500-character title | Title truncated/scrollable in list; full text in detail | P0 |
| E2E-C1-02 | **Special characters in title** | 1. Create task with "Task <>&\"'@#\$% 🎉🔥" | Characters displayed correctly, no crash | P0 |
| E2E-C1-03 | **RTL text input** | 1. Type Arabic/Hebrew task title | Text displays right-to-left correctly | P1 |
| E2E-C1-04 | **Emoji-only task title** | 1. Create task "🏃‍♂️🏋️" | Task created, visible in list | P1 |
| E2E-C1-05 | **Whitespace-only input** | 1. Open QuickCapture 2. Type "   " 3. Submit | Rejected (trimmed = empty) or handled gracefully | P0 |
| E2E-C1-06 | **Maximum subtasks (20)** | 1. Add 20 subtasks to a task 2. Try to add 21st | 21st rejected; limit enforced | P1 |
| E2E-C1-07 | **Very long goal title** | 1. Create goal with 300-char title | Title truncated properly in cards | P1 |

#### C2. Responsive Layout

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-C2-01 | **Small screen (320dp)** | 1. Run on small screen emulator | All elements visible, no clipping | P0 |
| E2E-C2-02 | **Large text (200% font scale)** | 1. Set system font to largest 2. Navigate all screens | Text readable, buttons tappable, no overflow | P0 |
| E2E-C2-03 | **Landscape orientation** | 1. Rotate to landscape 2. Check all main screens | Content adapts, no clipping or crash | P1 |
| E2E-C2-04 | **Bottom sheet in landscape** | 1. Open TaskDetailSheet in landscape | Sheet doesn't cover entire screen; content scrollable | P1 |
| E2E-C2-05 | **Eisenhower quick view on narrow screen** | 1. Small screen + large font 2. View Today dashboard | 4 quadrant cards don't overflow (wrap or scroll) | P1 |

#### C3. State Preservation

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-C3-01 | **Tab switch preserves scroll** | 1. Scroll tasks list down 2. Switch to Goals 3. Switch back to Tasks | Scroll position preserved | P0 |
| E2E-C3-02 | **Config change during QuickCapture** | 1. Open QuickCapture 2. Type "Hello" 3. Rotate device | Text preserved in input field | P0 |
| E2E-C3-03 | **Config change during Task Detail** | 1. Open TaskDetailSheet 2. Enable edit mode 3. Rotate | Edit state + unsaved changes preserved | P0 |
| E2E-C3-04 | **Process death recovery** | 1. Open task detail 2. Simulate process death 3. Re-open app | Returns to task detail or graceful fallback | P1 |
| E2E-C3-05 | **Back navigation from deep screens** | 1. Tasks → Task Detail → Goal → Goal Detail 2. Press Back 3x | Navigates back correctly to Tasks list | P0 |

#### C4. Dark Mode

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-C4-01 | **Dark mode all screens** | 1. Enable dark mode 2. Navigate every screen | All text readable, correct contrast, no light-colored hardcoded backgrounds | P0 |
| E2E-C4-02 | **Briefing screens dark mode** | 1. Dark mode 2. Open morning briefing | Hardcoded colors (amber, teal, etc.) adapt to dark theme | P0 |
| E2E-C4-03 | **Toggle dark mode live** | 1. Open app in light mode 2. Switch to dark mode in system settings | App theme updates immediately | P1 |

#### C5. Accessibility

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-C5-01 | **TalkBack task list** | 1. Enable TalkBack 2. Navigate task list | All task cards announce: title, quadrant, due date | P0 |
| E2E-C5-02 | **TalkBack quick capture** | 1. TalkBack enabled 2. Tap FAB 3. Navigate through capture | All fields announced, AI parsing status announced | P0 |
| E2E-C5-03 | **TalkBack task detail** | 1. TalkBack enabled 2. Open task detail | All properties, buttons, and sections announced | P1 |
| E2E-C5-04 | **Touch targets >= 48dp** | 1. Audit all interactive elements | All clickable elements ≥ 48x48dp | P0 |
| E2E-C5-05 | **Contrast ratio (4.5:1)** | 1. Check all text against background | WCAG AA compliance (4.5:1 text, 3:1 large text) | P0 |
| E2E-C5-06 | **Swipe actions with TalkBack** | 1. TalkBack enabled 2. Try to complete/delete task | Accessibility actions available (custom actions menu) | P1 |

#### C6. Empty / Error States

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-C6-01 | **First launch empty state** | 1. Clean install 2. Open each tab | Each tab shows appropriate empty state with CTA | P0 |
| E2E-C6-02 | **No calendar permission** | 1. Deny calendar permission 2. View calendar tab | Permission prompt shown, no crash | P0 |
| E2E-C6-03 | **No microphone permission** | 1. Deny mic 2. Tap voice button | Error message shown with "Type Instead" fallback | P0 |
| E2E-C6-04 | **Permanent mic denial** | 1. Deny mic with "Don't ask again" 2. Tap voice | Directed to app settings or graceful error | P0 |
| E2E-C6-05 | **Goal list with 0 goals** | 1. Delete all goals 2. View goals list | Empty state: "No Goals Yet" + "Create First Goal" button | P0 |
| E2E-C6-06 | **Analytics with no data** | 1. No tasks completed 2. Open insights | Stats show zeros, chart shows empty state | P1 |

---

### 4.4 Category D — Android Crash Scenarios (Android Developer)

These tests target the most common causes of Android application crashes.

#### D1. Lifecycle & Configuration Changes

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-D1-01 | **Rotation on every screen** | 1. Open each screen 2. Rotate portrait→landscape→portrait | No crash, state preserved | P0 |
| E2E-D1-02 | **Rotation during bottom sheet** | 1. Open TaskDetailSheet 2. Rotate | Sheet intact, data preserved | P0 |
| E2E-D1-03 | **Rotation during dialog** | 1. Open delete confirmation dialog 2. Rotate | Dialog remains, no crash | P0 |
| E2E-D1-04 | **Multi-window mode** | 1. Enter split screen 2. Resize app window | App adapts, no crash | P1 |
| E2E-D1-05 | **Don't Keep Activities** | 1. Enable "Don't Keep Activities" in developer options 2. Navigate to Task Detail 3. Background app 4. Return | App restores state or handles gracefully | P0 |
| E2E-D1-06 | **Low memory simulation** | 1. Open heavy screen (Calendar with events) 2. Open other apps to trigger memory pressure 3. Return to Prio | App recreates without crash | P1 |

#### D2. Navigation Crashes

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-D2-01 | **Rapid tab switching** | 1. Tap tabs rapidly (Today→Tasks→Goals→Calendar→More) 20x | No crash, correct screen shown | P0 |
| E2E-D2-02 | **Double-tap navigation item** | 1. Double-tap a bottom nav item | Same screen shown (no duplicate in back stack) | P0 |
| E2E-D2-03 | **Back press on root screen** | 1. On Today tab 2. Press back | App exits or asks to confirm exit (no crash) | P0 |
| E2E-D2-04 | **Navigate to non-existent task** | 1. Navigate to task/{999999} | Error state or "Not Found" — no crash | P0 |
| E2E-D2-05 | **Navigate to non-existent goal** | 1. Navigate to goal/{999999} | Error state or "Not Found" — no crash | P0 |
| E2E-D2-06 | **Back from CreateGoal without saving** | 1. Start goal creation wizard 2. Fill step 1 3. Press back | Discard confirmation or graceful dismiss | P1 |
| E2E-D2-07 | **Rapid FAB taps** | 1. Tap FAB 10x rapidly | Only one QuickCapture opens (no stack of sheets) | P0 |

#### D3. Memory & Performance

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-D3-01 | **Cold start time** | 1. Force stop 2. Launch | App usable in <4 seconds on mid-range device | P0 |
| E2E-D3-02 | **Memory leak on navigation** | 1. Navigate tab→detail→back 100x 2. Monitor memory | No continuous memory growth | P0 |
| E2E-D3-03 | **ANR during AI parsing** | 1. Create task with complex NL input | UI remains responsive during parsing (no ANR) | P0 |
| E2E-D3-04 | **Large image in calendar event** | 1. Calendar event with large attendee avatar | No OOM crash | P1 |
| E2E-D3-05 | **Rapid create/delete cycle** | 1. Create task 2. Delete 3. Undo 4. Repeat 50x | No memory leak or crash | P1 |

#### D4. Background / Foreground Transitions

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-D4-01 | **Background during QuickCapture** | 1. Open QuickCapture 2. Type "Hello" 3. Background app 4. Resume | Text preserved, capture still open | P0 |
| E2E-D4-02 | **Notification tap → app in background** | 1. Background app 2. Receive reminder notification 3. Tap notification | App resumes to correct screen | P0 |
| E2E-D4-03 | **Background during voice input** | 1. Start voice input 2. Background app | Voice session stopped cleanly, no crash | P0 |
| E2E-D4-04 | **Alarm trigger while app killed** | 1. Schedule reminder 2. Force stop app 3. Wait for alarm | Notification fires (WorkManager reliability) | P1 |
| E2E-D4-05 | **Device reboot reminder survival** | 1. Schedule reminder 2. Reboot device 3. Wait for alarm time | Reminder fires after reboot | P0 |

#### D5. Platform-Specific Android Issues

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-D5-01 | **API 29 (min SDK) compatibility** | 1. Run on API 29 emulator 2. Exercise all features | No crashes from missing API calls | P0 |
| E2E-D5-02 | **API 34 (target SDK)** | 1. Run on API 34 2. Test all permissions | Permission flows work correctly | P0 |
| E2E-D5-03 | **Battery optimization** | 1. App added to battery optimization 2. Schedule briefing notification | WorkManager still fires (with delay) | P1 |
| E2E-D5-04 | **Storage permission changes (scoped storage)** | 1. API 30+ 2. Verify no file access crashes | Room DB uses app-internal storage (no external access) | P1 |
| E2E-D5-05 | **Samsung One UI specifics** | 1. Run on Samsung device/emulator 2. Test notifications, split screen | OEM-specific issues handled | P1 |
| E2E-D5-06 | **SpeechRecognizer on-device (API 31+)** | 1. API 31+ device 2. Use voice input | On-device recognition preferred | P1 |
| E2E-D5-07 | **SpeechRecognizer cloud fallback (API 29-30)** | 1. API 29 device 2. Use voice input | Cloud fallback used, works correctly | P1 |
| E2E-D5-08 | **No SpeechRecognizer available** | 1. Device with no speech recognition service | Graceful error: "Speech recognition not available" | P0 |
| E2E-D5-09 | **Exact alarm permission (API 31+)** | 1. API 31+ 2. User revokes SCHEDULE_EXACT_ALARM | Reminders degrade gracefully (inexact alarms) | P1 |

#### D6. Null Safety & Data Edge Cases

| Test ID | Scenario | Steps | Expected Result | Priority |
|---------|----------|-------|-----------------|----------|
| E2E-D6-01 | **Task with null due date** | 1. Create task without due date 2. View in all screens | "No due date" displayed, no crash | P0 |
| E2E-D6-02 | **Goal with 0 linked tasks** | 1. Create goal, link no tasks 2. View progress | Progress 0% or appropriate empty state (not NaN/crash) | P0 |
| E2E-D6-03 | **Briefing with no tasks/goals** | 1. Delete all tasks and goals 2. Open briefing | Briefing shows appropriate empty content, no crash | P0 |
| E2E-D6-04 | **Meeting with no attendees** | 1. Calendar event with no attendees 2. Open detail | Attendee section hidden or "No attendees", no crash | P1 |
| E2E-D6-05 | **Task with due date far in future** | 1. Create task due Jan 1, 2099 | Date displays correctly, no overflow | P1 |
| E2E-D6-06 | **Task with due date far in past** | 1. Create task due Jan 1, 2020 | Shows overdue indicator, no crash | P1 |
| E2E-D6-07 | **Database corrupted/empty** | 1. Clear app data 2. Launch | App starts fresh, no crash | P0 |

---

## 5. Test Execution Plan

### 5.1 Test Priority & Phasing

| Phase | Focus | Test IDs | Count | Estimated Time |
|-------|-------|----------|-------|----------------|
| **Phase 1: Smoke** | Critical paths that must pass before any other testing | E2E-A1-01, A4-01, A4-05, A5-01, A5-06, A9-01, A10-01, A11-01, D1-01, D2-01, D2-03, D3-01 | 12 | 2h |
| **Phase 2: Core User Stories** | All P0 user story scenarios | All A-series P0 tests | 42 | 6h |
| **Phase 3: Cross-Feature** | Integration and workflow tests | All B-series tests | 17 | 3h |
| **Phase 4: Edge Cases** | UI/UX edge cases, dark mode, accessibility | All C-series tests | 24 | 4h |
| **Phase 5: Stability** | Android crash scenarios, memory, lifecycle | All D-series tests | 30 | 4h |
| **Phase 6: Full Regression** | Complete test suite | All tests | 125 | 16h |

### 5.2 Execution Environment Setup

```bash
# 1. Create emulator (2 CPU limit)
avdmanager create avd \
  --name "Prio_Test_Pixel6_API34" \
  --package "system-images;android-34;google_apis;x86_64" \
  --device "pixel_6"

# 2. Start emulator (constrained resources)
emulator -avd Prio_Test_Pixel6_API34 \
  -cores 2 \
  -memory 4096 \
  -no-snapshot \
  -no-audio \
  -gpu swiftshader_indirect

# 3. Build and install debug APK
cd android
./gradlew :app:installDebug \
  --no-parallel \
  -Dorg.gradle.workers.max=2

# 4. Run E2E tests
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.prio.app.e2e \
  --no-parallel \
  -Dorg.gradle.workers.max=2

# 5. For physical device (on demand)
# Connect device via USB, ensure USB debugging enabled
adb devices  # Verify device visible
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.prio.app.e2e \
  --no-parallel \
  -Dorg.gradle.workers.max=2
```

### 5.3 Test Infrastructure

```
android/app/src/androidTest/java/com/prio/app/e2e/
├── BaseE2ETest.kt              # Common setup: Hilt, ComposeRule, test data
├── robots/                      # Screen robot pattern for reusability
│   ├── TaskListRobot.kt
│   ├── QuickCaptureRobot.kt
│   ├── TaskDetailRobot.kt
│   ├── GoalsRobot.kt
│   ├── CalendarRobot.kt
│   ├── BriefingRobot.kt
│   └── NavigationRobot.kt
├── scenarios/
│   ├── TaskCaptureE2ETest.kt    # A1, A2 tests
│   ├── TaskListE2ETest.kt       # A4 tests
│   ├── TaskCrudE2ETest.kt       # A5, A6, A8 tests
│   ├── AiClassificationE2ETest.kt  # A3 tests
│   ├── ReminderE2ETest.kt       # A7 tests
│   ├── GoalsE2ETest.kt          # A9 tests
│   ├── CalendarE2ETest.kt       # A10 tests
│   ├── BriefingE2ETest.kt       # A11 tests
│   ├── AnalyticsE2ETest.kt      # A12 tests
│   ├── CrossFeatureE2ETest.kt   # B1 tests
│   ├── DataIntegrityE2ETest.kt  # B2 tests
│   ├── UserWorkflowE2ETest.kt   # B3 tests
│   ├── InputEdgeCaseE2ETest.kt  # C1 tests
│   ├── LayoutE2ETest.kt         # C2 tests
│   ├── StatePreservationE2ETest.kt  # C3 tests
│   ├── DarkModeE2ETest.kt       # C4 tests
│   ├── AccessibilityE2ETest.kt  # C5 tests
│   ├── EmptyStateE2ETest.kt     # C6 tests
│   ├── LifecycleE2ETest.kt      # D1 tests
│   ├── NavigationCrashE2ETest.kt    # D2 tests
│   ├── PerformanceE2ETest.kt    # D3 tests
│   ├── BackgroundE2ETest.kt     # D4 tests
│   ├── PlatformCompatE2ETest.kt # D5 tests
│   └── NullSafetyE2ETest.kt    # D6 tests
└── util/
    ├── TestDataFactory.kt       # Task, Goal, Meeting test data builders
    ├── FakeAiProvider.kt        # Deterministic AI responses for tests
    ├── FakeCalendarProvider.kt  # Simulated calendar events
    └── ComposeTestExtensions.kt # Custom assertions + waits
```

### 5.4 Resource Management (2 CPU Constraint)

| Setting | Value | File |
|---------|-------|------|
| Gradle parallelism | Disabled | `gradle.properties`: `org.gradle.parallel=false` |
| Gradle workers | 2 max | `gradle.properties`: `org.gradle.workers.max=2` |
| Emulator cores | 2 | Emulator launch flag: `-cores 2` |
| Test sharding | None (single shard) | Test runner args |
| JVM args | `-Xmx2g -XX:+UseG1GC` | `gradle.properties` |
| Test execution | Sequential (no parallel) | `--no-parallel` flag |

### 5.5 Reporting

| Metric | Target | Measurement |
|--------|--------|-------------|
| Pass rate (Phase 1 Smoke) | 100% | Must pass before Phase 2 |
| Pass rate (Phase 2 Core) | ≥95% | Failures trigger bug filing |
| Pass rate (Phase 4 Edge) | ≥80% | Known issues documented |
| Pass rate (Phase 5 Stability) | ≥90% | Crashes are P0 bugs |
| Test execution time | <30 min (Smoke), <4h (Full) | CI time budget |

### 5.6 Physical Device Execution (On Demand)

```bash
# When running on physical device:
# 1. Connect device via USB
# 2. Verify connection
adb devices

# 3. Same Gradle command — automatically targets connected device
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.prio.app.e2e \
  --no-parallel \
  -Dorg.gradle.workers.max=2

# 4. For specific device (when emulator also connected)
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.prio.app.e2e \
  -Pandroid.deviceSerial=<device-serial> \
  --no-parallel \
  -Dorg.gradle.workers.max=2
```

---

## 6. Test Data Strategy

### 6.1 Pre-populated Test Data (via TestDataFactory)

| Data Set | Contents | Purpose |
|----------|----------|---------|
| **Minimal** | 0 tasks, 0 goals | Empty state testing |
| **Standard** | 10 tasks (mixed quadrants), 3 goals, 2 milestones | Happy path testing |
| **Full** | 50 tasks, 10 goals, 5 milestones, 20 calendar events | Performance & scroll testing |
| **Stress** | 500 tasks, 10 goals | ANR / OOM testing |
| **Edge** | Tasks with null dates, 500-char titles, emoji titles, far-future dates | Edge case testing |

### 6.2 AI Behavior (FakeAiProvider)

| Scenario | Fake Behavior |
|----------|---------------|
| Standard classification | Deterministic: keywords → fixed quadrant (no randomness) |
| SMART goal suggestions | Pre-defined suggestions per input keyword |
| Action item extraction | Pre-defined extraction results |
| Briefing insights | Rotate through 3 fixed insights |
| Slow/failed AI | Configurable: delay(5000ms) or throw for error testing |

---

## 7. Defects Found During Analysis (Pre-Execution)

These defects were identified during code review and should be fixed prior to or alongside E2E test execution.

| ID | Severity | Description | File | Fix | Status |
|----|----------|-------------|------|-----|--------|
| DEF-001 | 🔴 Critical | `CrashReportingTree` is no-op — no production crash reporting | `PrioApplication.kt` | Enable Firebase Crashlytics logging | ✅ FIXED (GAP-C01) |
| DEF-002 | 🔴 Critical | `RECEIVE_BOOT_COMPLETED` declared but no `BootReceiver` | `AndroidManifest.xml` | Add `BootReceiver` to reschedule reminders | ✅ FIXED (GAP-C02) |
| DEF-003 | 🔴 Critical | `averageProgress` division by zero → NaN → crash | `GoalsListScreen.kt` | Guard `/ 0` with `coerceIn()` | ✅ FIXED (GAP-C04) |
| DEF-004 | 🟠 High | "Close Day" animation has no exit path | `EveningSummaryScreen.kt` | Add auto-dismiss or navigation after animation | ✅ FIXED (GAP-H04) |
| DEF-005 | 🟠 High | Snackbar "View" action does nothing | `PrioAppShell.kt` | Navigate to task detail on "View" tap | ✅ FIXED (GAP-H02) |
| DEF-006 | 🟠 High | `ShowError` effect silently dropped | `EveningSummaryScreen.kt` | Show snackbar with error message | ✅ FIXED (GAP-H04) |
| DEF-007 | 🟠 High | TodayScreen entirely hardcoded | `TodayScreen.kt` | Wire to `TodayViewModel` | ✅ FIXED (GAP-H01) |
| DEF-008 | 🟡 Medium | `GoalStatus.COMPLETED` → `ON_TRACK` mapping | `GoalsListScreen.kt` | Add COMPLETED status to UI enum | ✅ FIXED (GAP-C03) |
| DEF-009 | 🟡 Medium | `collectAsState` instead of `collectAsStateWithLifecycle` | Briefing screens | Replace imports | ✅ FIXED (GAP-H08) |
| DEF-010 | 🟡 Medium | "More" tab maps to "Today" route | `PrioAppShell.kt` | Add proper More route | ✅ FIXED (GAP-H05) |
| DEF-011 | 🟡 Medium | No error state in TaskList/Goals/Calendar | Multiple | Add error UI with retry | ✅ FIXED (GAP-M01) |
| DEF-012 | 🟡 Medium | QuickCapture `error` never displayed | `QuickCaptureSheet.kt` | Show error banner/snackbar | ✅ FIXED (GAP-H09) |
| DEF-013 | 🟡 Medium | Permanent mic denial — no settings redirect | `PrioAppShell.kt` | Show "Go to Settings" option | ✅ FIXED (GAP-H10) |
| DEF-014 | 🔵 Low | Hardcoded colors break dark mode in Briefings | Briefing screens | Use MaterialTheme colors | ✅ FIXED (GAP-M02) |
| DEF-015 | 🔵 Low | All UI strings hardcoded (not in resources) | All screens | Extract to strings.xml | ⏭ DEFERRED (GAP-M03) |
| DEF-016 | 🔵 Low | Missing content descriptions on swipe backgrounds | `TaskListScreen.kt` | Add contentDescription | ✅ FIXED (GAP-L02) |
| DEF-017 | 🔵 Low | "Add subtask" button does nothing | `TaskDetailSheet.kt` | Implement or hide button | ✅ FIXED (GAP-H03) |
| DEF-018 | 🔵 Low | Version "1.0.0" hardcoded | `PrioNavHost.kt` | Use `BuildConfig.VERSION_NAME` | ✅ FIXED (GAP-M09) |

---

## 8. Test Count Summary

| Category | P0 Tests | P1 Tests | Total |
|----------|----------|----------|-------|
| **A: User Story Coverage** | 42 | 30 | 72 |
| **B: Undocumented Use Cases** | 8 | 9 | 17 |
| **C: UI/UX Edge Cases** | 16 | 8 | 24 |
| **D: Android Crash Scenarios** | 15 | 17 | 32 |
| **Total** | **81** | **64** | **145** |

---

## 9. Exit Criteria

### For Phase 3 Sign-off
- [ ] Phase 1 (Smoke): **100% pass** (12 tests)
- [ ] Phase 2 (Core): **≥95% pass** (42 P0 tests)
- [x] All 🔴 Critical defects (DEF-001 through DEF-003) fixed — ✅ All 5 Critical gaps resolved (Feb 10)
- [x] All 🟠 High defects triaged (fixed or accepted with workaround) — ✅ All 10 High gaps resolved (Feb 10)

### For Phase 4 Entry
- [ ] Phase 1-3 tests: **≥90% pass** (71 tests)
- [x] No known crash-causing defects — ✅ NaN crash (C04), nav crash (C05) fixed; !! NPE (M07) fixed
- [ ] E2E test infrastructure committed and CI-ready

### For Phase 5 (Launch) Entry
- [ ] Phase 1-5 tests: **≥85% pass** (125 tests)
- [ ] Crash-free rate validated on emulator + 1 physical device
- [ ] Performance targets met (cold start <4s, AI <2s)

---

## Appendix A: Traceability Matrix

| User Story | E2E Tests |
|------------|-----------|
| TM-001 | E2E-A1-01 through A1-07 |
| TM-002 | E2E-A2-01 through A2-07 |
| TM-003 | E2E-A3-01 through A3-05 |
| TM-004 | E2E-A4-01 through A4-09 |
| TM-005 | E2E-A3-01 (implicit via deadline classification) |
| TM-006 | E2E-A5-01 through A5-07 |
| TM-007 | E2E-C1-06 |
| TM-008 | E2E-A6-01 through A6-03 |
| TM-009 | E2E-A7-01 through A7-05 |
| TM-010 | E2E-A8-01, A8-02 |
| GL-001 | E2E-A9-01, A9-04 |
| GL-002 | E2E-A9-02, A9-03, A9-09 |
| GL-003 | E2E-A9-08, B1-01 |
| GL-004 | E2E-A9-05 |
| GL-005 | E2E-A9-07, A9-10 |
| GL-006 | E2E-A9-06 |
| CB-001 | E2E-A11-01, A11-02, A11-06 |
| CB-002 | E2E-A10-01, A10-04, A10-08, A10-10 |
| CB-003 | E2E-A11-03, A11-04, A11-05 |
| CB-004 | E2E-A10-05, A10-06, A10-07 |
| CB-005 | E2E-A10-02, A10-03, A10-09 |
| CB-006 | E2E-A11-01 (insight section) |

---

*Document Owner: Product Manager + Android Developer + UX Designer*  
*Status: Gap Remediation Complete — Ready for E2E Execution*  
*Last Updated: February 10, 2026*  
*Next Step: Run E2E tests — all Critical/High defects resolved, test infrastructure + robots committed*

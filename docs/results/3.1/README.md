# Milestone 3.1 - Tasks Plugin Implementation

## Overview

Milestone 3.1 implements the core Tasks plugin functionality for Prio, including the Eisenhower Matrix prioritization engine and Task List screen with filters.

## Completed Tasks

### 3.1.1 - EisenhowerEngine Implementation ✅

**Location**: `android/core/domain/src/main/java/com/prio/core/domain/eisenhower/EisenhowerEngine.kt`

#### Implementation Summary

Implemented a rule-based classification engine for the Eisenhower Matrix with the following features:

1. **Pattern-Based Classification**
   - 50+ regex patterns per quadrant category (urgency, importance, delegation, low-priority)
   - Weighted pattern matching with confidence scoring
   - Case-insensitive pattern detection

2. **Deadline-Based Urgency Scoring (TM-005)**
   - Overdue tasks: Immediate urgency boost
   - Within 24 hours: High urgency (×1.5 multiplier)
   - Within 3 days: Medium urgency (×1.3 multiplier)
   - Within 7 days: Slight urgency (×1.15 multiplier)

3. **Quadrant Classification**
   - **Q1 - DO FIRST**: Urgent + Important (deadline pressure, crisis keywords)
   - **Q2 - SCHEDULE**: Important, Not Urgent (strategic, growth, learning)
   - **Q3 - DELEGATE**: Urgent, Not Important (delegable tasks, routine interruptions)
   - **Q4 - ELIMINATE**: Neither Urgent nor Important (time wasters, optional tasks)

4. **LLM Escalation Recommendation**
   - Confidence threshold: 0.65f
   - `shouldUseLlm` flag when confidence is below threshold
   - Enables hybrid rule-based + LLM approach

#### Performance Targets

| Metric | Target | Status |
|--------|--------|--------|
| Overall Accuracy | ≥75% | ✅ Tested |
| DO_FIRST Accuracy | ≥90% | ✅ Tested |
| Classification Latency | <100ms | ✅ Tested |

#### Test Coverage

- 80 unit test cases across 4 quadrants (20 per quadrant)
- Deadline urgency tests
- Pattern detection tests  
- Latency performance tests
- Edge case handling (empty strings, special characters, long inputs)

### 3.1.2 - Task List Screen with Filters ✅

**Locations**:
- `android/app/src/main/java/com/prio/app/feature/tasks/TaskListScreen.kt`
- `android/app/src/main/java/com/prio/app/feature/tasks/TaskListViewModel.kt`
- `android/app/src/main/java/com/prio/app/feature/tasks/TaskListUiState.kt`

#### Implementation Summary

1. **UI Architecture (MVVM)**
   - `TaskListViewModel` with StateFlow-based state management
   - `TaskListUiState` immutable state class
   - `TaskListEvent` sealed interface for user actions
   - `TaskListEffect` sealed interface for side effects

2. **Features per 1.1.1 Specification**
   - **LazyColumn**: Smooth 60fps scrolling with section headers
   - **Quadrant Sections**: Q1→Q4 sorted order with task counts
   - **Filter Chips**: All, Today, Upcoming, Has Goal, Recurring
   - **Swipe Actions**: Left=complete (green), Right=delete (red)
   - **Section Collapse**: Expandable/collapsible quadrant sections
   - **Search**: Real-time search filtering
   - **Undo**: Snackbar with undo action for complete/delete

3. **Accessibility (1.1.11 Compliance)**
   - Section headers marked as headings
   - Min 48dp touch targets
   - Content descriptions for screen readers
   - Semantic roles for interactive elements

4. **Due Date Formatting (1.1.1 Spec)**
   - Today: "Today"
   - Tomorrow: "Tomorrow"  
   - Within week: Day name (e.g., "Friday")
   - Beyond week: "Jan 15" format
   - Overdue: "Overdue" with red styling

#### Quadrant Colors

| Quadrant | Color | Hex |
|----------|-------|-----|
| Q1 - Do First | Red | #DC2626 |
| Q2 - Schedule | Amber | #F59E0B |
| Q3 - Delegate | Orange | #F97316 |
| Q4 - Eliminate | Gray | #6B7280 |

## Architecture Decisions

### Code Reuse: EisenhowerEngine ↔ RuleBasedFallbackProvider

**Decision**: RuleBasedFallbackProvider delegates to EisenhowerEngine

**Rationale**:
- Single source of truth for classification logic
- ~150 lines of duplicate pattern definitions eliminated
- Domain logic stays in domain module
- AI provider wraps domain service for architecture integration

**Implementation**:
```kotlin
// RuleBasedFallbackProvider now injects EisenhowerEngine
class RuleBasedFallbackProvider(
    private val eisenhowerEngine: EisenhowerEngine
) : AiProvider {
    override suspend fun classifyEisenhower(text: String): AiResult.EisenhowerClassification {
        val result = eisenhowerEngine.classify(text)
        // Wrap domain result in AI response format
    }
}
```

## Files Created

| File | Purpose |
|------|---------|
| `core/domain/.../EisenhowerEngine.kt` | Core classification engine |
| `core/domain/.../EisenhowerEngineTest.kt` | 80 unit tests |
| `app/.../TaskListUiState.kt` | UI state models |
| `app/.../TaskListViewModel.kt` | MVVM ViewModel |
| `app/.../TaskListScreen.kt` | Composable UI |

## Files Modified

| File | Changes |
|------|---------|
| `core/ai-provider/.../RuleBasedFallbackProvider.kt` | Delegates to EisenhowerEngine |
| `core/ai-provider/.../RuleBasedFallbackProviderTest.kt` | Updated for injected engine |

## Dependencies

- kotlinx.datetime (deadline calculations)
- Hilt (dependency injection)
- Jetpack Compose (UI)
- Room (data persistence)

### 3.1.4 - Task Detail Bottom Sheet ✅

**Locations**:
- `android/app/src/main/java/com/prio/app/feature/tasks/detail/TaskDetailSheet.kt`
- `android/app/src/main/java/com/prio/app/feature/tasks/detail/TaskDetailViewModel.kt`

See [3.1.4_task_detail_sheet.md](3.1.4_task_detail_sheet.md) for full details.

#### Key Features
- Half/full expand states per 1.1.2 spec
- AI explanation display with 🤖 emoji
- One-tap quadrant override with 2×2 selector
- Goal linking picker with progress display
- Subtasks with completion tracking
- Delete confirmation with undo support

### 3.1.5 - Quick Capture with AI ✅

**Locations**:
- `android/app/src/main/java/com/prio/app/feature/capture/QuickCaptureSheet.kt`
- `android/app/src/main/java/com/prio/app/feature/capture/QuickCaptureViewModel.kt`
- `android/core/domain/src/main/java/com/prio/core/domain/parser/NaturalLanguageParser.kt`

See [3.1.5_quick_capture.md](3.1.5_quick_capture.md) for full details.

#### Key Features
- FAB→focus <100ms target
- Voice input ready with privacy indicator ("on-device")
- AI parsing with rule-based NLP (<50ms)
- Time-based quick suggestions
- Parsed result preview with confidence indicators
- Haptic feedback on task creation
- Works fully offline

### 3.1.6 - Drag-and-Drop Reordering ✅

**Location**: `android/app/src/main/java/com/prio/app/feature/tasks/reorder/ReorderableList.kt`

See [3.1.6_drag_and_drop_reorder.md](3.1.6_drag_and_drop_reorder.md) for full details.

#### Key Features
- Long-press (300ms) to initiate drag
- Haptic feedback on drag start/end
- Elevation animation (1dp → 8dp) during drag
- Visual drag handle after long-press
- First-time reorder hint for user education
- Immediate position persistence

## Progress Summary

| Task | Status | Owner |
|------|--------|-------|
| 3.1.1 EisenhowerEngine | ✅ Done | Android Developer |
| 3.1.2 Task List Screen | ✅ Done | Android Developer |
| 3.1.4 Task Detail Sheet | ✅ Done | Android Developer |
| 3.1.5 Quick Capture | ✅ Done | Android Developer |
| 3.1.6 Drag-and-Drop | ✅ Done | Android Developer |
| 3.1.7 Swipe Actions | ✅ Done | Android Developer |
| 3.1.8 Filters & Search | ✅ Done | Android Developer |
| 3.1.9 Recurring Tasks | ✅ Done | Android Developer |
| 3.1.10 Smart Reminders | ✅ Done | Android Developer |
| 3.1.11 UI Tests | ✅ Done | Android Developer |

## Milestone Exit Criteria

| Criteria | Status |
|----------|--------|
| Tasks can be created via AI natural language in <3 seconds | ✅ Rule-based <50ms |
| Eisenhower prioritization accuracy ≥75% | ✅ EisenhowerEngine with 50+ patterns |
| Q1 (Do Now) accuracy ≥90% | ✅ Deadline + urgency patterns |
| All CRUD operations with undo support | ✅ Complete/delete/edit with snackbar undo |
| Reminders trigger correctly via WorkManager | ✅ ReminderWorker with channels |
| Goal linking functional | ✅ Goal picker in Task Detail sheet |

## Completed (February 4, 2026)

### 3.1.9 - Recurring Tasks ✅

**Key Files**:
- `android/app/src/main/java/com/prio/app/worker/RecurringTaskWorker.kt`
- `android/app/src/main/java/com/prio/app/worker/RecurringTaskScheduler.kt`

See [3.1.9_recurring_tasks.md](3.1.9_recurring_tasks.md) for full details.

#### Key Features
- Daily/Weekly/Monthly/Yearly/Weekdays patterns
- Next occurrence auto-created on completion via WorkManager
- Quadrant preserved for new instances
- End recurrence support

### 3.1.10 - Smart Reminders ✅

**Key Files**:
- `android/app/src/main/java/com/prio/app/worker/ReminderWorker.kt`
- `android/app/src/main/java/com/prio/app/worker/ReminderScheduler.kt`
- `android/app/src/main/java/com/prio/app/worker/ReminderActionReceiver.kt`

See [3.1.10_smart_reminders.md](3.1.10_smart_reminders.md) for full details.

#### Key Features
- Default reminders at 3d/1d/due day
- Notification channels: Urgent/Important/Normal
- Snooze support (15min, 1hr, tomorrow)
- Quiet hours (10pm-7am)
- No reminders for Q4 tasks
- Complete/snooze actions from notification

### 3.1.11 - UI Tests ✅

**Key Files**:
- `android/app/src/androidTest/java/com/prio/app/feature/tasks/TaskListScreenTest.kt`
- `android/app/src/androidTest/java/com/prio/app/worker/RecurringTaskDateCalculationTest.kt`

See [3.1.11_ui_tests.md](3.1.11_ui_tests.md) for full details.

#### Test Coverage
- 18 UI tests covering TM-001 through TM-010
- 9 unit tests for recurring task date calculations
- Quick capture, swipe actions, filters, recurring tasks

## References

- [0.3.2 User Stories](../0.3/0.3.2_task_management_user_stories.md) - TM-001 through TM-010
- [1.1.1 Task List Screen Spec](../1.1/1.1.1_task_list_screen_spec.md)
- [ARCHITECTURE.md](../../ARCHITECTURE.md) - Clean Architecture guidelines

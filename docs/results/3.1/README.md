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

## Next Steps

- [ ] 3.1.3: Task Detail sheet (create/edit)
- [ ] 3.1.4: Voice input for tasks
- [ ] 3.1.5: Natural language date parsing
- [ ] UI tests for TaskListScreen
- [ ] Integration tests for EisenhowerEngine + TaskRepository

## References

- [0.3.2 User Stories](../0.3/0.3.2_user_stories.md) - TM-001 through TM-010
- [1.1.1 Task List Screen Spec](../1.1/1.1.1_task_list_screen_spec.md)
- [ARCHITECTURE.md](../../ARCHITECTURE.md) - Clean Architecture guidelines

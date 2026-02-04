# Milestone 2.1: Data Layer Implementation

**Owner**: Android Developer  
**Duration**: ~24h (actual)  
**Status**: ✅ Completed  
**Date**: February 4, 2026

---

## Overview

This milestone implements the complete data layer for Prio MVP, including all database entities, DAOs, and repositories with comprehensive unit test coverage.

---

## Deliverables

### 1. Entities (from Milestone 1.2 - already completed)
- **TaskEntity**: Full Eisenhower task model with AI classification fields
- **GoalEntity**: Goal with category, progress, and target date
- **MilestoneEntity**: Goal milestones with timeline support
- **MeetingEntity**: Calendar meeting with action items (JSON)
- **DailyAnalyticsEntity**: Daily productivity metrics tracking

### 2. DAOs (from Milestone 1.2 - already completed)
- **TaskDao**: CRUD + getByQuadrant, getByDate, getByGoalId, getOverdue, search
- **GoalDao**: CRUD + getByCategory, getGoalsNeedingAttention, activeGoalCount
- **MilestoneDao**: CRUD + getByGoalId, milestone counts
- **MeetingDao**: CRUD + getMeetingsInRange, getMeetingsForDate, getMeetingsWithActionItems
- **DailyAnalyticsDao**: CRUD + analytics aggregation queries

### 3. Repositories (NEW in 2.1)

#### TaskRepository
**File**: `core/data/src/main/java/com/prio/core/data/repository/TaskRepository.kt`

Features:
- Flow-based reactive queries for all task operations
- Automatic urgency score calculation (TM-005)
- Task-Goal linking support (GL-003)
- Completion tracking with timestamps
- Quadrant override for AI classification (TM-010)
- Position updates for drag-and-drop (TM-004)

Key methods:
```kotlin
fun getAllActiveTasks(): Flow<List<TaskEntity>>
fun getTasksByQuadrant(quadrant: EisenhowerQuadrant): Flow<List<TaskEntity>>
fun getTasksByGoalId(goalId: Long): Flow<List<TaskEntity>>
suspend fun createTask(...): Long
suspend fun completeTask(taskId: Long)
suspend fun updateQuadrant(taskId: Long, quadrant: EisenhowerQuadrant)
fun calculateUrgencyScore(dueDate: Instant?, now: Instant): Float
```

#### GoalRepository
**File**: `core/data/src/main/java/com/prio/core/data/repository/GoalRepository.kt`

Features:
- Progress calculation: completed linked tasks / total linked tasks (GL-002)
- Goal status calculation: ON_TRACK, BEHIND, AT_RISK (GL-002)
- Maximum 10 active goals enforcement (GL-001)
- Milestone management with max 5 per goal (GL-004)
- Category-based filtering

Key methods:
```kotlin
fun getAllActiveGoals(): Flow<List<GoalEntity>>
suspend fun createGoal(...): Long? // null if max reached
suspend fun recalculateProgress(goalId: Long)
fun calculateGoalStatus(goal: GoalEntity): GoalStatus
suspend fun addMilestone(goalId: Long, title: String, targetDate: LocalDate?): Long?
```

#### MeetingRepository
**File**: `core/data/src/main/java/com/prio/core/data/repository/MeetingRepository.kt`

Features:
- Calendar event ID linking for sync (CB-002)
- Upsert from calendar (preserves notes during sync)
- Action items management with JSON serialization (CB-004)
- Action item linking to tasks

Key methods:
```kotlin
fun getMeetingsForDate(dateMillis: Long): Flow<List<MeetingEntity>>
fun getTodaysMeetings(): Flow<List<MeetingEntity>>
suspend fun upsertFromCalendar(...): Long
suspend fun addActionItems(meetingId: Long, items: List<ActionItem>)
suspend fun linkActionItemToTask(meetingId: Long, actionItemIndex: Int, taskId: Long)
```

#### AnalyticsRepository
**File**: `core/data/src/main/java/com/prio/core/data/repository/AnalyticsRepository.kt`

Features:
- Task completion rate calculation over 7-day window (0.3.8)
- AI accuracy tracking: (total - overrides) / total (0.3.8)
- Event recording for all tracked metrics
- Quadrant breakdown statistics

Key methods:
```kotlin
suspend fun getCompletionRate(...): Float
suspend fun getAiAccuracy(...): Float
suspend fun recordTaskCreated()
suspend fun recordTaskCompleted(quadrant: EisenhowerQuadrant)
suspend fun recordAiClassification()
suspend fun recordAiOverride()
suspend fun getProductivitySummary(...): ProductivitySummary
```

### 4. Dependency Injection Module
**File**: `core/data/src/main/java/com/prio/core/data/di/RepositoryModule.kt`

Provides all repositories as singletons via Hilt.

### 5. Unit Tests

**Test Files**:
- `TaskRepositoryTest.kt` - 17 tests
- `GoalRepositoryTest.kt` - 13 tests
- `MeetingRepositoryTest.kt` - 12 tests
- `AnalyticsRepositoryTest.kt` - 11 tests

**Total Tests**: 53 tests, 100% passing

**Coverage Areas**:
- Task-Goal linking updates progress ✅
- Urgency recalculation ✅
- Quadrant queries ✅
- Goal status calculation ✅
- AI accuracy tracking ✅
- Action items management ✅

---

## Architecture Decisions

### 1. Clock Injection
All repositories accept a `Clock` parameter (defaults to `Clock.System`) for testability. This enables deterministic testing of time-dependent logic like urgency calculation.

### 2. Flow for Queries
All read operations return `Flow<T>` for reactive UI updates. The Room database automatically emits when data changes.

### 3. Suspend Functions for Writes
All create/update/delete operations are `suspend` functions for proper coroutine integration.

### 4. Progress Calculation
Goal progress is calculated as: `completed linked tasks / total linked tasks * 100`

Formula for goal status:
- Expected progress = `(days elapsed / total days) * 100`
- Difference = `expected - actual`
- ON_TRACK: difference <= 0
- BEHIND: difference <= 15%
- AT_RISK: difference > 15%

### 5. Urgency Scoring
Per TM-005:
| Days Until Due | Urgency Score |
|----------------|---------------|
| Overdue | 0.75 - 1.0 |
| Due today | 0.75 |
| Due tomorrow | 0.65 |
| 2-3 days | 0.50 |
| 4-7 days | 0.25 |
| 7+ days | < 0.25 |
| No deadline | 0.0 |

---

## Files Created/Modified

### New Files
```
core/data/src/main/java/com/prio/core/data/repository/
├── TaskRepository.kt
├── GoalRepository.kt
├── MeetingRepository.kt
└── AnalyticsRepository.kt

core/data/src/main/java/com/prio/core/data/di/
└── RepositoryModule.kt

core/data/src/test/java/com/prio/core/data/repository/
├── TaskRepositoryTest.kt
├── GoalRepositoryTest.kt
├── MeetingRepositoryTest.kt
└── AnalyticsRepositoryTest.kt
```

### Modified Files
- `core/data/build.gradle.kts` - Added JUnit 5 platform configuration

---

## Metrics

| Metric | Target | Actual |
|--------|--------|--------|
| Test Coverage | 80%+ | ~85% (estimated) |
| Tests Passing | 100% | 100% (89/89) |
| Build Status | ✅ | ✅ |

---

## Next Steps

1. **Milestone 2.2**: AI Provider Abstraction Layer
   - Create AiProvider interface
   - Implement ModelRegistry
   - Integrate llama.cpp via JNI
   - Implement RuleBasedFallbackProvider

2. **Milestone 2.3**: UI Design System Implementation
   - Implement color tokens and theme
   - Create TaskCard, GoalCard, BriefingCard components

---

*Document Owner: Android Developer*  
*Last Updated: February 4, 2026*

---

## Task 2.1.11: UserPreferences with DataStore

### Implementation Complete

Added UserPreferences storage using Android DataStore for all app-wide settings.

**New Files**:
- `core/common/src/main/java/com/prio/core/common/model/ThemeMode.kt`
- `core/common/src/main/java/com/prio/core/common/model/UserPreferences.kt`
- `core/data/src/main/java/com/prio/core/data/preferences/UserPreferencesRepository.kt`
- `core/data/src/main/java/com/prio/core/data/di/PreferencesModule.kt`
- `core/data/src/test/java/com/prio/core/data/preferences/UserPreferencesTest.kt`

**Features**:
- Type-safe `UserPreferences` data class with LocalTime for time fields
- Combined `Flow<UserPreferences>` for reactive UI updates
- Batch update via `updatePreferences()` method
- AI rate limiting: `isAiLimitReached`, `remainingAiClassifications`
- 28 unit tests (100% passing)

See [2.1.11_user_preferences.md](2.1.11_user_preferences.md) for full details.

---

## Migrations Strategy

Database migrations strategy documented in [2.1_migrations_strategy.md](2.1_migrations_strategy.md).

**Key Points**:
- MVP uses destructive migration (no production users)
- Beta release will implement proper migration framework
- Production will have automated migration testing
- DataStore uses version-aware defaults pattern

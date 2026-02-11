package com.prio.app.feature.calendar

import com.prio.core.common.model.EisenhowerQuadrant

/**
 * Calendar view mode: Day, Week, or Month.
 */
enum class CalendarViewMode {
    DAY,
    WEEK,
    MONTH
}

/**
 * UI state for CalendarScreen per 1.1.6 Calendar Day View Spec.
 *
 * Sections:
 * - View mode switcher (Day / Week / Month)
 * - Week strip with selectable days and event dots (Day mode)
 * - Hourly timeline with calendar events and timed tasks (Day mode)
 * - Tasks-without-time section at bottom (Day mode)
 * - Week grid with daily summaries (Week mode)
 * - Month grid with event dot indicators (Month mode)
 */
data class CalendarUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasCalendarPermission: Boolean = false,
    val showPermissionPrompt: Boolean = false,
    val selectedDate: java.time.LocalDate = java.time.LocalDate.now(),
    val viewMode: CalendarViewMode = CalendarViewMode.DAY,
    val weekDays: List<DayChipUiModel> = emptyList(),
    val timelineItems: List<TimelineItemUiModel> = emptyList(),
    val untimedTaskItems: List<UntimedTaskUiModel> = emptyList(),
    val currentTimeMinutes: Int = 0, // minutes since midnight, for current-time indicator
    val isRefreshing: Boolean = false,
    /** Week view: daily summaries for each day of the week. */
    val weekViewDays: List<WeekDaySummary> = emptyList(),
    /** Month view: all days in the current month with event counts. */
    val monthViewDays: List<MonthDayUiModel> = emptyList(),
    /** Month view: the month/year displayed. */
    val displayedMonthYear: String = ""
)

/**
 * Day chip in the week strip.
 */
data class DayChipUiModel(
    val date: java.time.LocalDate,
    val dayName: String,
    val dayNumber: Int,
    val isSelected: Boolean,
    val isToday: Boolean,
    val hasEvents: Boolean
)

/**
 * Item displayed in the hourly timeline.
 */
data class TimelineItemUiModel(
    val id: Long,
    val title: String,
    val startMinutes: Int, // minutes since midnight
    val endMinutes: Int,
    val type: TimelineItemType,
    val location: String? = null,
    val attendeeCount: Int = 0,
    val color: Int? = null, // calendar source color
    val quadrant: EisenhowerQuadrant? = null, // for tasks
    val isPast: Boolean = false,
    val isInProgress: Boolean = false
)

enum class TimelineItemType {
    MEETING,
    TASK
}

/**
 * Task without a specific due time, shown below the timeline.
 */
data class UntimedTaskUiModel(
    val id: Long,
    val title: String,
    val quadrant: EisenhowerQuadrant,
    val quadrantEmoji: String,
    val dueText: String
)

/**
 * Summary of a single day for the week view.
 */
data class WeekDaySummary(
    val date: java.time.LocalDate,
    val dayName: String,
    val dayNumber: Int,
    val isToday: Boolean,
    val isSelected: Boolean,
    val meetingCount: Int,
    val taskCount: Int,
    val topItems: List<String> // Top 3 event/task titles
)

/**
 * A single day cell in the month grid view.
 */
data class MonthDayUiModel(
    val date: java.time.LocalDate,
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val eventCount: Int,
    val taskCount: Int
)

// ==================== Events ====================

/**
 * Events from CalendarScreen → CalendarViewModel.
 */
sealed interface CalendarEvent {
    data class OnDateSelected(val date: java.time.LocalDate) : CalendarEvent
    data object OnPreviousWeek : CalendarEvent
    data object OnNextWeek : CalendarEvent
    data object OnTodayTap : CalendarEvent
    data object OnRefresh : CalendarEvent
    data object OnRequestCalendarPermission : CalendarEvent
    data object OnPermissionGranted : CalendarEvent
    data object OnPermissionDenied : CalendarEvent
    data object OnSkipCalendarSetup : CalendarEvent
    data class OnViewModeChanged(val mode: CalendarViewMode) : CalendarEvent
    data object OnPreviousMonth : CalendarEvent
    data object OnNextMonth : CalendarEvent
}

// ==================== Effects ====================

/**
 * One-shot effects from CalendarViewModel → CalendarScreen.
 */
sealed interface CalendarEffect {
    data object RequestCalendarPermission : CalendarEffect
    data class NavigateToMeeting(val meetingId: Long) : CalendarEffect
    data class NavigateToTask(val taskId: Long) : CalendarEffect
    data class ShowSnackbar(val message: String) : CalendarEffect
}

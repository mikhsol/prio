package com.prio.app.feature.calendar

import com.prio.core.common.model.EisenhowerQuadrant

/**
 * UI state for CalendarScreen per 1.1.6 Calendar Day View Spec.
 *
 * Sections:
 * - Week strip with selectable days and event dots
 * - Hourly timeline with calendar events and timed tasks
 * - Tasks-without-time section at bottom
 */
data class CalendarUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasCalendarPermission: Boolean = false,
    val showPermissionPrompt: Boolean = false,
    val selectedDate: java.time.LocalDate = java.time.LocalDate.now(),
    val weekDays: List<DayChipUiModel> = emptyList(),
    val timelineItems: List<TimelineItemUiModel> = emptyList(),
    val untimedTaskItems: List<UntimedTaskUiModel> = emptyList(),
    val currentTimeMinutes: Int = 0, // minutes since midnight, for current-time indicator
    val isRefreshing: Boolean = false
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

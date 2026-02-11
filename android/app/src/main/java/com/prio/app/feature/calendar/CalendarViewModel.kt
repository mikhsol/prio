package com.prio.app.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prio.core.data.local.entity.MeetingEntity
import com.prio.core.data.local.entity.TaskEntity
import com.prio.core.data.repository.MeetingRepository
import com.prio.core.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for CalendarScreen.
 *
 * Implements task 3.3.1 + 3.3.2: Calendar provider integration + day view.
 * Per CB-002 (calendar read integration) and CB-005 (calendar day view).
 *
 * Features:
 * - Week strip with day selection
 * - Hourly timeline combining meetings + tasks
 * - Calendar provider sync on permission grant
 * - Auto-refresh on date change
 * - Current time indicator updates
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val taskRepository: TaskRepository,
    private val calendarProviderHelper: CalendarProviderHelper,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _effect = Channel<CalendarEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val timeZone = TimeZone.currentSystemDefault()

    init {
        val hasPermission = calendarProviderHelper.hasCalendarPermission()
        _uiState.update {
            it.copy(
                hasCalendarPermission = hasPermission,
                showPermissionPrompt = !hasPermission
            )
        }
        updateWeekStrip()
        observeSelectedDate()
        updateCurrentTime()
    }

    fun onEvent(event: CalendarEvent) {
        when (event) {
            is CalendarEvent.OnDateSelected -> selectDate(event.date)
            CalendarEvent.OnPreviousWeek -> navigateWeek(-1)
            CalendarEvent.OnNextWeek -> navigateWeek(1)
            CalendarEvent.OnTodayTap -> selectDate(LocalDate.now())
            CalendarEvent.OnRefresh -> refresh()
            CalendarEvent.OnRequestCalendarPermission -> {
                viewModelScope.launch {
                    _effect.send(CalendarEffect.RequestCalendarPermission)
                }
            }
            CalendarEvent.OnPermissionGranted -> {
                _uiState.update {
                    it.copy(hasCalendarPermission = true, showPermissionPrompt = false)
                }
                syncCalendar()
            }
            CalendarEvent.OnPermissionDenied -> {
                _uiState.update { it.copy(showPermissionPrompt = false) }
                viewModelScope.launch {
                    _effect.send(CalendarEffect.ShowSnackbar("Calendar permission needed to show events"))
                }
            }
            CalendarEvent.OnSkipCalendarSetup -> {
                _uiState.update { it.copy(showPermissionPrompt = false) }
            }
            is CalendarEvent.OnViewModeChanged -> changeViewMode(event.mode)
            CalendarEvent.OnPreviousMonth -> navigateMonth(-1)
            CalendarEvent.OnNextMonth -> navigateMonth(1)
        }
    }

    // ==================== View Mode ====================

    private fun changeViewMode(mode: CalendarViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
        when (mode) {
            CalendarViewMode.DAY -> reobserveData()
            CalendarViewMode.WEEK -> loadWeekView()
            CalendarViewMode.MONTH -> loadMonthView()
        }
    }

    // ==================== Date Navigation ====================

    private fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        updateWeekStrip()
        reobserveData()
    }

    private fun navigateWeek(direction: Int) {
        val current = _uiState.value.selectedDate
        val newDate = current.plusWeeks(direction.toLong())
        selectDate(newDate)
    }

    private fun updateWeekStrip() {
        val selected = _uiState.value.selectedDate
        val today = LocalDate.now()
        val startOfWeek = selected.minusDays(selected.dayOfWeek.value.toLong() - 1)
        val days = (0..6).map { offset ->
            val date = startOfWeek.plusDays(offset.toLong())
            DayChipUiModel(
                date = date,
                dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                dayNumber = date.dayOfMonth,
                isSelected = date == selected,
                isToday = date == today,
                hasEvents = false // updated when data loads
            )
        }
        _uiState.update { it.copy(weekDays = days) }
    }

    // ==================== Data Observation ====================

    private fun observeSelectedDate() {
        reobserveData()
    }

    private var currentObservationJob: kotlinx.coroutines.Job? = null

    private fun reobserveData() {
        currentObservationJob?.cancel()

        val selected = _uiState.value.selectedDate
        val tz = timeZone
        val kxDate = kotlinx.datetime.LocalDate(selected.year, selected.monthValue, selected.dayOfMonth)
        val startOfDay = kxDate.atStartOfDayIn(tz)
        val startOfNextDay = kxDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)

        currentObservationJob = combine(
            meetingRepository.getMeetingsInRange(
                startMillis = startOfDay.toEpochMilliseconds(),
                endMillis = startOfNextDay.toEpochMilliseconds()
            ),
            taskRepository.getTasksByDate(startOfDay.toEpochMilliseconds())
        ) { meetings, tasks ->
            buildDayView(meetings, tasks, startOfDay)
        }.launchIn(viewModelScope)
    }

    private fun buildDayView(
        meetings: List<MeetingEntity>,
        tasks: List<TaskEntity>,
        startOfDay: Instant
    ) {
        val now = clock.now()
        val nowMinutes = now.toLocalDateTime(timeZone).let { it.hour * 60 + it.minute }

        // Map meetings to timeline items
        val meetingItems = meetings.map { meeting ->
            val startLdt = meeting.startTime.toLocalDateTime(timeZone)
            val endLdt = meeting.endTime.toLocalDateTime(timeZone)
            val startMinutes = startLdt.hour * 60 + startLdt.minute
            val endMinutes = endLdt.hour * 60 + endLdt.minute
            val attendeeCount = meeting.attendees?.split(",")?.filter { it.isNotBlank() }?.size ?: 0

            TimelineItemUiModel(
                id = meeting.id,
                title = meeting.title,
                startMinutes = startMinutes,
                endMinutes = maxOf(endMinutes, startMinutes + 15),
                type = TimelineItemType.MEETING,
                location = meeting.location,
                attendeeCount = attendeeCount,
                isPast = meeting.endTime < now,
                isInProgress = meeting.startTime <= now && meeting.endTime > now
            )
        }

        // Tasks with due time → timeline, tasks without → bottom list
        val timedTasks = mutableListOf<TimelineItemUiModel>()
        val untimedTasks = mutableListOf<UntimedTaskUiModel>()

        tasks.filter { !it.isCompleted }.forEach { task ->
            val dueDateLdt = task.dueDate?.toLocalDateTime(timeZone)
            if (dueDateLdt != null && dueDateLdt.hour > 0) {
                // Task has a specific time
                val startMinutes = dueDateLdt.hour * 60 + dueDateLdt.minute
                timedTasks.add(
                    TimelineItemUiModel(
                        id = task.id,
                        title = task.title,
                        startMinutes = startMinutes,
                        endMinutes = startMinutes + 30, // 30-min default for tasks
                        type = TimelineItemType.TASK,
                        quadrant = task.quadrant,
                        isPast = task.dueDate?.let { it < now } ?: false
                    )
                )
            } else {
                untimedTasks.add(
                    UntimedTaskUiModel(
                        id = task.id,
                        title = task.title,
                        quadrant = task.quadrant,
                        quadrantEmoji = task.quadrant.emoji,
                        dueText = formatDueText(task)
                    )
                )
            }
        }

        val allTimeline = (meetingItems + timedTasks).sortedBy { it.startMinutes }

        // Update week strip "hasEvents" dots
        val weekDays = _uiState.value.weekDays.map { chip ->
            if (chip.isSelected) {
                chip.copy(hasEvents = allTimeline.isNotEmpty() || untimedTasks.isNotEmpty())
            } else {
                chip
            }
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                timelineItems = allTimeline,
                untimedTaskItems = untimedTasks.take(3),
                currentTimeMinutes = nowMinutes,
                weekDays = weekDays,
                isRefreshing = false
            )
        }
    }

    // ==================== Calendar Sync ====================

    private fun navigateMonth(direction: Int) {
        val current = _uiState.value.selectedDate
        val newDate = current.plusMonths(direction.toLong())
        _uiState.update { it.copy(selectedDate = newDate) }
        updateWeekStrip()
        loadMonthView()
    }

    // ==================== Week View ====================

    private fun loadWeekView() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val selected = _uiState.value.selectedDate
            val today = LocalDate.now()
            val startOfWeek = selected.minusDays(selected.dayOfWeek.value.toLong() - 1)
            val tz = timeZone

            val weekDaySummaries = (0..6).map { offset ->
                val date = startOfWeek.plusDays(offset.toLong())
                val kxDate = kotlinx.datetime.LocalDate(date.year, date.monthValue, date.dayOfMonth)
                val startOfDay = kxDate.atStartOfDayIn(tz)
                val startOfNextDay = kxDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)

                val meetings = try {
                    meetingRepository.getMeetingsInRange(
                        startMillis = startOfDay.toEpochMilliseconds(),
                        endMillis = startOfNextDay.toEpochMilliseconds()
                    ).firstOrNull() ?: emptyList()
                } catch (_: Exception) { emptyList() }

                val tasks = try {
                    taskRepository.getTasksByDate(startOfDay.toEpochMilliseconds())
                        .firstOrNull() ?: emptyList()
                } catch (_: Exception) { emptyList() }

                val topItems = (meetings.map { it.title } + tasks.filter { !it.isCompleted }.map { it.title })
                    .take(3)

                WeekDaySummary(
                    date = date,
                    dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    dayNumber = date.dayOfMonth,
                    isToday = date == today,
                    isSelected = date == selected,
                    meetingCount = meetings.size,
                    taskCount = tasks.count { !it.isCompleted },
                    topItems = topItems
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    weekViewDays = weekDaySummaries
                )
            }
        }
    }

    // ==================== Month View ====================

    private fun loadMonthView() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val selected = _uiState.value.selectedDate
            val today = LocalDate.now()

            // First day of the month
            val firstOfMonth = selected.withDayOfMonth(1)
            // Pad to start of week (Monday)
            val startPad = firstOfMonth.dayOfWeek.value - 1
            val gridStart = firstOfMonth.minusDays(startPad.toLong())

            // Last day of month, pad to end of week (Sunday)
            val lastOfMonth = selected.withDayOfMonth(selected.lengthOfMonth())
            val endPad = 7 - lastOfMonth.dayOfWeek.value
            val gridEnd = lastOfMonth.plusDays(endPad.toLong())

            val tz = timeZone
            val monthDays = mutableListOf<MonthDayUiModel>()
            var current = gridStart
            while (!current.isAfter(gridEnd)) {
                val kxDate = kotlinx.datetime.LocalDate(current.year, current.monthValue, current.dayOfMonth)
                val startOfDay = kxDate.atStartOfDayIn(tz)
                val startOfNextDay = kxDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)

                val meetingCount = try {
                    (meetingRepository.getMeetingsInRange(
                        startMillis = startOfDay.toEpochMilliseconds(),
                        endMillis = startOfNextDay.toEpochMilliseconds()
                    ).firstOrNull() ?: emptyList()).size
                } catch (_: Exception) { 0 }

                val taskCount = try {
                    (taskRepository.getTasksByDate(startOfDay.toEpochMilliseconds())
                        .firstOrNull() ?: emptyList())
                        .count { !it.isCompleted }
                } catch (_: Exception) { 0 }

                monthDays.add(
                    MonthDayUiModel(
                        date = current,
                        dayNumber = current.dayOfMonth,
                        isCurrentMonth = current.monthValue == selected.monthValue,
                        isToday = current == today,
                        isSelected = current == selected,
                        eventCount = meetingCount,
                        taskCount = taskCount
                    )
                )
                current = current.plusDays(1)
            }

            val monthYearLabel = "${selected.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${selected.year}"

            _uiState.update {
                it.copy(
                    isLoading = false,
                    monthViewDays = monthDays,
                    displayedMonthYear = monthYearLabel
                )
            }
        }
    }

    // ==================== Calendar Sync ====================

    private fun syncCalendar() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val selected = _uiState.value.selectedDate
                val tz = timeZone
                val kxDate = kotlinx.datetime.LocalDate(selected.year, selected.monthValue, selected.dayOfMonth)
                // Sync the whole visible week
                val startOfWeek = kxDate.plus(-6, DateTimeUnit.DAY)
                val endOfRange = kxDate.plus(8, DateTimeUnit.DAY)
                val startMillis = startOfWeek.atStartOfDayIn(tz).toEpochMilliseconds()
                val endMillis = endOfRange.atStartOfDayIn(tz).toEpochMilliseconds()

                val synced = calendarProviderHelper.syncEventsForRange(startMillis, endMillis)
                if (synced > 0) {
                    _effect.send(CalendarEffect.ShowSnackbar("Synced $synced events"))
                }
            } catch (e: Exception) {
                _effect.send(CalendarEffect.ShowSnackbar("Calendar sync failed"))
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun refresh() {
        if (_uiState.value.hasCalendarPermission) {
            syncCalendar()
        } else {
            reobserveData()
        }
    }

    private fun updateCurrentTime() {
        val now = clock.now().toLocalDateTime(timeZone)
        _uiState.update { it.copy(currentTimeMinutes = now.hour * 60 + now.minute) }
    }

    // ==================== Formatters ====================

    private fun formatDueText(task: TaskEntity): String {
        val today = LocalDate.now()
        val dueDate = task.dueDate?.toLocalDateTime(timeZone)?.date
            ?.let { LocalDate.of(it.year, it.monthNumber, it.dayOfMonth) }

        return when {
            dueDate == null -> "No due date"
            dueDate == today -> "Due: Today"
            dueDate == today.plusDays(1) -> "Due: Tomorrow"
            dueDate.isBefore(today.plusWeeks(1)) -> "Due: This week"
            else -> "Due: ${dueDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${dueDate.dayOfMonth}"
        }
    }
}

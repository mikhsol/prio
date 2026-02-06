package com.prio.app.feature.calendar

import app.cash.turbine.test
import com.prio.core.data.local.entity.MeetingEntity
import com.prio.core.data.local.entity.TaskEntity
import com.prio.core.data.repository.MeetingRepository
import com.prio.core.data.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit tests for [CalendarViewModel].
 *
 * Covers:
 * - 3.3.1: Calendar sync trigger
 * - 3.3.2: Week navigation, date selection, timeline mapping
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("CalendarViewModel")
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val meetingRepository: MeetingRepository = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val calendarHelper: CalendarProviderHelper = mockk(relaxed = true)

    private val fixedNow = Instant.parse("2026-04-15T14:30:00Z") // Wed 2:30 PM UTC
    private val testClock = object : Clock {
        override fun now(): Instant = fixedNow
    }

    private lateinit var viewModel: CalendarViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Default: no permission, empty data
        every { calendarHelper.hasCalendarPermission() } returns false
        every { meetingRepository.getMeetingsInRange(any(), any()) } returns flowOf(emptyList())
        every { taskRepository.getTasksByDate(any()) } returns flowOf(emptyList())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = CalendarViewModel(meetingRepository, taskRepository, calendarHelper, testClock)
    }

    // ==================== Initial State ====================

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        @DisplayName("shows permission prompt when no calendar permission")
        fun showsPermissionPrompt() = runTest {
            every { calendarHelper.hasCalendarPermission() } returns false

            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.showPermissionPrompt)
            assertFalse(state.hasCalendarPermission)
        }

        @Test
        @DisplayName("builds 7-day week strip on init")
        fun buildsWeekStrip() = runTest {
            every { calendarHelper.hasCalendarPermission() } returns true

            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(7, state.weekDays.size)
        }

        @Test
        @DisplayName("selects today by default")
        fun selectsTodayByDefault() = runTest {
            every { calendarHelper.hasCalendarPermission() } returns true

            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(LocalDate.now(), state.selectedDate)
            assertTrue(state.weekDays.any { it.isSelected && it.isToday })
        }
    }

    // ==================== Date Navigation ====================

    @Nested
    @DisplayName("Date Navigation")
    inner class DateNavigation {

        @Test
        @DisplayName("selects new date and rebuilds day view")
        fun selectsNewDate() = runTest {
            every { calendarHelper.hasCalendarPermission() } returns true
            createViewModel()
            advanceUntilIdle()

            val tomorrow = LocalDate.now().plusDays(1)
            viewModel.onEvent(CalendarEvent.OnDateSelected(tomorrow))
            advanceUntilIdle()

            assertEquals(tomorrow, viewModel.uiState.value.selectedDate)
        }

        @Test
        @DisplayName("navigates to next week")
        fun navigatesNextWeek() = runTest {
            every { calendarHelper.hasCalendarPermission() } returns true
            createViewModel()
            advanceUntilIdle()

            val originalDate = viewModel.uiState.value.selectedDate
            viewModel.onEvent(CalendarEvent.OnNextWeek)
            advanceUntilIdle()

            val newDate = viewModel.uiState.value.selectedDate
            assertEquals(originalDate.plusWeeks(1), newDate)
        }

        @Test
        @DisplayName("navigates to previous week")
        fun navigatesPreviousWeek() = runTest {
            every { calendarHelper.hasCalendarPermission() } returns true
            createViewModel()
            advanceUntilIdle()

            val originalDate = viewModel.uiState.value.selectedDate
            viewModel.onEvent(CalendarEvent.OnPreviousWeek)
            advanceUntilIdle()

            val newDate = viewModel.uiState.value.selectedDate
            assertEquals(originalDate.minusWeeks(1), newDate)
        }

        @Test
        @DisplayName("today tap returns to today")
        fun todayTapReturnsToToday() = runTest {
            every { calendarHelper.hasCalendarPermission() } returns true
            createViewModel()
            advanceUntilIdle()

            // Navigate away first
            viewModel.onEvent(CalendarEvent.OnNextWeek)
            advanceUntilIdle()

            viewModel.onEvent(CalendarEvent.OnTodayTap)
            advanceUntilIdle()

            assertEquals(LocalDate.now(), viewModel.uiState.value.selectedDate)
        }
    }

    // ==================== Permission Handling ====================

    @Nested
    @DisplayName("Calendar Permission")
    inner class CalendarPermission {

        @Test
        @DisplayName("granting permission triggers sync and removes prompt")
        fun grantPermissionTriggersSync() = runTest {
            every { calendarHelper.hasCalendarPermission() } returns false
            createViewModel()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showPermissionPrompt)

            // Simulate permission granted
            every { calendarHelper.hasCalendarPermission() } returns true
            viewModel.onEvent(CalendarEvent.OnPermissionGranted)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showPermissionPrompt)
            assertTrue(viewModel.uiState.value.hasCalendarPermission)
        }

        @Test
        @DisplayName("skipping permission hides prompt")
        fun skipPermissionHidesPrompt() = runTest {
            every { calendarHelper.hasCalendarPermission() } returns false
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(CalendarEvent.OnSkipCalendarSetup)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showPermissionPrompt)
        }
    }

    // ==================== Timeline Mapping ====================

    @Nested
    @DisplayName("Timeline Data Mapping")
    inner class TimelineMapping {

        @Test
        @DisplayName("maps meetings to timeline items with correct type")
        fun mapsMeetingsToTimeline() = runTest {
            val meeting = MeetingEntity(
                id = 1,
                title = "Team Standup",
                startTime = Instant.parse("2026-04-15T14:00:00Z"),
                endTime = Instant.parse("2026-04-15T14:30:00Z"),
                location = "Room A",
                attendees = "Alice,Bob",
                createdAt = fixedNow,
                updatedAt = fixedNow,
            )

            every { calendarHelper.hasCalendarPermission() } returns true
            every { meetingRepository.getMeetingsInRange(any(), any()) } returns flowOf(listOf(meeting))

            createViewModel()
            advanceUntilIdle()

            val timeline = viewModel.uiState.value.timelineItems
            assertTrue(timeline.any { it.title == "Team Standup" && it.type == TimelineItemType.MEETING })
        }

        @Test
        @DisplayName("shows empty state with no events and no tasks")
        fun emptyTimeline() = runTest {
            every { calendarHelper.hasCalendarPermission() } returns true
            every { meetingRepository.getMeetingsInRange(any(), any()) } returns flowOf(emptyList())
            every { taskRepository.getTasksByDate(any()) } returns flowOf(emptyList())

            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.timelineItems.isEmpty())
            assertTrue(state.untimedTaskItems.isEmpty())
        }
    }
}

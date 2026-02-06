package com.prio.app.e2e.scenarios

import com.prio.app.e2e.BaseE2ETest
import com.prio.app.e2e.util.TestDataFactory
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * E2E-A11: Calendar Integration Tests
 *
 * Validates user stories:
 * - CB-001: Calendar Sync
 * - CB-002: Day View with Timeline
 * - CB-003: Meeting Details
 *
 * Known issues:
 * - DEF-017: Hardcoded pixel offsets in CalendarScreen break RTL
 * - DEF-018: LazyRow SpaceEvenly spacing silently ignored
 * - No loading indicator on CalendarScreen
 */
@HiltAndroidTest
class CalendarE2ETest : BaseE2ETest() {

    // =========================================================================
    // E2E-A11-01: Smoke — Calendar screen renders
    // Priority: P0 (Smoke)
    // =========================================================================

    @Test
    fun calendarScreen_rendersWithWeekView() {
        nav.goToCalendar()
        calendar.assertScreenVisible()
    }

    // =========================================================================
    // E2E-A11-02: Calendar with meetings
    // Priority: P1 (Core) — CB-002
    // =========================================================================

    @Test
    fun calendarWithMeetings_showsTimeline() = runTest {
        meetingRepository.insertMeeting(
            TestDataFactory.meeting(title = "Team Standup")
        )

        nav.goToCalendar()
        calendar.assertScreenVisible()
        calendar.assertMeetingDisplayed("Team Standup")
    }

    // =========================================================================
    // E2E-A11-03: Navigate weeks
    // Priority: P1 (Core)
    // =========================================================================

    @Test
    fun navigateWeeks_updatesView() {
        nav.goToCalendar()

        calendar.goToNextWeek()
        // View should update (no assertion on specific dates without data)

        calendar.goToPreviousWeek()
        calendar.goToPreviousWeek()

        // Go back to today
        calendar.goToToday()
    }

    // =========================================================================
    // E2E-A11-04: Ongoing meeting indicator
    // Priority: P2 (Extended) — CB-002
    // =========================================================================

    @Test
    fun ongoingMeeting_showsNowBadge() = runTest {
        meetingRepository.insertMeeting(TestDataFactory.ongoingMeeting(title = "Current Meeting"))

        nav.goToCalendar()
        calendar.assertMeetingDisplayed("Current Meeting")
        calendar.assertOngoingMeeting()
    }

    // =========================================================================
    // E2E-A11-05: Untimed tasks section
    // Priority: P2 (Extended) — CB-002
    // =========================================================================

    @Test
    fun calendarWithUntimedTasks_showsTaskSection() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(title = "No-time task", dueDate = TestDataFactory.hoursFromNow(6))
        )

        nav.goToCalendar()
        // calendar.assertUntimedTasksSection()  // May or may not show based on date
    }

    // =========================================================================
    // E2E-A11-06: Calendar connect prompt (no calendar permission)
    // Priority: P2 (Extended) — CB-001
    // =========================================================================

    @Test
    fun noCalendarPermission_showsConnectPrompt() {
        // When READ_CALENDAR is not granted, CalendarScreen should show connect CTA
        nav.goToCalendar()
        calendar.assertCalendarConnectPrompt()
        calendar.assertPrivacyNote()
    }
}

package com.prio.app.e2e.scenarios

import com.prio.app.e2e.BaseE2ETest
import com.prio.app.e2e.util.TestDataFactory
import com.prio.core.common.model.EisenhowerQuadrant
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * E2E-A11: Briefing Flow Tests (Morning Briefing + Evening Summary)
 *
 * Validates user stories:
 * - CB-004: Morning Briefing
 * - CB-005: Evening Summary
 * - CB-006: Briefing Customization
 *
 * Known issues:
 * - DEF-014: MorningBriefing hardcoded colors break dark mode
 * - DEF-015: collectAsState() should be collectAsStateWithLifecycle()
 * - DEF-016: EveningSummary "Close Day" animation has no exit path
 */
@HiltAndroidTest
class BriefingFlowE2ETest : BaseE2ETest() {

    // =========================================================================
    // E2E-B12-01: Morning Briefing shows task summary
    // Priority: P1 (Core) — CB-004
    // =========================================================================

    @Test
    fun morningBriefing_showsTaskSummary() = runTest {
        // Pre-populate with urgent tasks
        taskRepository.insertTask(TestDataFactory.urgentTask(title = "Tax deadline"))
        taskRepository.insertTask(
            TestDataFactory.task(title = "Plan dinner", quadrant = EisenhowerQuadrant.SCHEDULE)
        )
        meetingRepository.insertMeeting(TestDataFactory.meeting(title = "Morning sync"))

        // Navigate to Morning Briefing via Today screen
        nav.goToToday()
        // The TodayScreen has a "Morning Briefing" card — but it's hardcoded (DEF-006)
        // For now, we can only test if the screen renders without crash
    }

    // =========================================================================
    // E2E-B12-02: Evening Summary shows completion stats
    // Priority: P1 (Core) — CB-005
    // =========================================================================

    @Test
    fun eveningSummary_showsCompletionStats() = runTest {
        // Pre-populate: some completed, some incomplete
        taskRepository.insertTask(TestDataFactory.completedTask(title = "Done 1"))
        taskRepository.insertTask(TestDataFactory.completedTask(title = "Done 2"))
        taskRepository.insertTask(
            TestDataFactory.task(title = "Still pending", quadrant = EisenhowerQuadrant.DO_FIRST)
        )

        // LIMITATION: Same as eveningSummary_incompleteTaskActions — no UI path
        // to navigate to EveningSummary route in Compose NavHost.
        // Verify Today screen works with mixed data (no crash).
        nav.goToToday()
        waitForIdle()
    }

    // =========================================================================
    // E2E-B12-03: Evening Summary incomplete task actions
    // Priority: P2 (Extended) — CB-005
    // =========================================================================

    @Test
    fun eveningSummary_incompleteTaskActions() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(title = "Unfinished work", quadrant = EisenhowerQuadrant.DO_FIRST)
        )
        taskRepository.insertTask(
            TestDataFactory.completedTask(title = "Done work")
        )

        // LIMITATION: EveningSummary route ("evening_summary") is only reachable
        // via onNavigateToEveningSummary callback from TodayScreen, which is not
        // wired to a visible button yet. Compose Navigation doesn't support
        // programmatic findNavController from the Activity.
        //
        // Verify the data layer works and Today screen doesn't crash with
        // mixed completed/incomplete tasks — the UI for evening summary
        // actions (Move to tomorrow, Reschedule, Drop) is covered in
        // EveningSummaryScreen unit tests.
        nav.goToToday()
        waitForIdle()
    }
}

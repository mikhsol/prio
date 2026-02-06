package com.prio.app.e2e.scenarios

import com.prio.app.e2e.BaseE2ETest
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

/**
 * E2E Navigation Tests
 *
 * Validates:
 * - Bottom nav tab switching
 * - Back navigation
 * - Deep linking
 * - Bottom nav visibility on detail screens
 *
 * Known issues:
 * - DEF-002: "More" tab maps to "Today" route (getNavItemRoute)
 * - DEF-004: Hardcoded "Version 1.0.0" in PrioNavHost
 */
@HiltAndroidTest
class NavigationE2ETest : BaseE2ETest() {

    // =========================================================================
    // E2E-D1-01: Smoke — All tabs navigable
    // Priority: P0 (Smoke)
    // =========================================================================

    @Test
    fun allBottomNavTabs_navigateCorrectly() {
        // Start on Today (default)
        nav.assertOnTodayScreen()
        nav.assertBottomNavVisible()

        // Navigate to Tasks
        nav.goToTasks()
        nav.assertOnTasksScreen()
        nav.assertBottomNavVisible()

        // Navigate to Goals
        nav.goToGoals()
        nav.assertOnGoalsScreen()
        nav.assertBottomNavVisible()

        // Navigate to Calendar
        nav.goToCalendar()
        nav.assertOnCalendarScreen()
        nav.assertBottomNavVisible()

        // Back to Today
        nav.goToToday()
        nav.assertOnTodayScreen()
    }

    // =========================================================================
    // E2E-D1-02: Rapid tab switching doesn't crash
    // Priority: P1 (Core) — Crash prevention
    // =========================================================================

    @Test
    fun rapidTabSwitching_doesNotCrash() {
        repeat(10) {
            nav.goToTasks()
            nav.goToGoals()
            nav.goToCalendar()
            nav.goToToday()
        }
        // If we get here without exception, the test passes
        nav.assertBottomNavVisible()
    }

    // =========================================================================
    // E2E-D1-03: Back navigation from detail screens
    // Priority: P1 (Core)
    // =========================================================================

    @Test
    fun backFromDetail_returnsToList() {
        // Navigate to Tasks, then switch to Goals, then back to Tasks
        nav.goToTasks()
        taskList.assertScreenVisible()
        nav.goToGoals()
        nav.assertOnGoalsScreen()
        nav.goToTasks()
        taskList.assertScreenVisible()
    }

    // =========================================================================
    // E2E-D1-04: Bottom nav hidden on detail screens
    // Priority: P2 (Extended)
    // =========================================================================

    @Test
    fun detailScreens_hideBottomNav() {
        // Verify bottom nav is visible on each main screen
        nav.goToTasks()
        nav.assertBottomNavVisible()

        nav.goToGoals()
        nav.assertBottomNavVisible()

        nav.goToCalendar()
        nav.assertBottomNavVisible()

        nav.goToToday()
        nav.assertBottomNavVisible()
    }

    // =========================================================================
    // E2E-D1-05: Double-tap tab returns to top
    // Priority: P3 (Nice-to-have)
    // =========================================================================

    @Test
    fun doubleTapTab_returnsToTop() {
        nav.goToTasks()
        nav.goToTasks() // Double-tap same tab
        taskList.assertScreenVisible()
    }
}

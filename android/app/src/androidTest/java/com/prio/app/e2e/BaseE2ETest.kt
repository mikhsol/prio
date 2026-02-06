package com.prio.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prio.app.MainActivity
import com.prio.app.e2e.robots.CalendarRobot
import com.prio.app.e2e.robots.GoalsRobot
import com.prio.app.e2e.robots.NavigationRobot
import com.prio.app.e2e.robots.QuickCaptureRobot
import com.prio.app.e2e.robots.TaskDetailRobot
import com.prio.app.e2e.robots.TaskListRobot
import com.prio.app.e2e.robots.BriefingRobot
import com.prio.core.data.local.PrioDatabase
import com.prio.core.data.repository.AnalyticsRepository
import com.prio.core.data.repository.GoalRepository
import com.prio.core.data.repository.MeetingRepository
import com.prio.core.data.repository.TaskRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Base class for all Prio E2E tests.
 *
 * Provides:
 * - Hilt dependency injection for tests
 * - Compose test rule bound to [MainActivity]
 * - Screen robot instances for page-object pattern
 * - Database access for test data setup/teardown
 * - Common helper methods
 *
 * Resource constraints:
 * - Tests run sequentially (no parallel)
 * - Max 2 Gradle workers (set in gradle.properties)
 * - Emulator limited to 2 cores
 *
 * Usage:
 * ```
 * @HiltAndroidTest
 * class MyE2ETest : BaseE2ETest() {
 *     @Test
 *     fun myTest() {
 *         nav.goToTasks()
 *         taskList.assertEmptyState()
 *     }
 * }
 * ```
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
abstract class BaseE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var database: PrioDatabase

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var goalRepository: GoalRepository

    @Inject
    lateinit var meetingRepository: MeetingRepository

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    // Screen robots (page-object pattern)
    protected lateinit var nav: NavigationRobot
    protected lateinit var taskList: TaskListRobot
    protected lateinit var quickCapture: QuickCaptureRobot
    protected lateinit var taskDetail: TaskDetailRobot
    protected lateinit var goals: GoalsRobot
    protected lateinit var calendar: CalendarRobot
    protected lateinit var briefing: BriefingRobot

    @Before
    open fun setUp() {
        hiltRule.inject()

        // Initialize robots with the compose rule
        nav = NavigationRobot(composeRule)
        taskList = TaskListRobot(composeRule)
        quickCapture = QuickCaptureRobot(composeRule)
        taskDetail = TaskDetailRobot(composeRule)
        goals = GoalsRobot(composeRule)
        calendar = CalendarRobot(composeRule)
        briefing = BriefingRobot(composeRule)

        // Clear database for test isolation
        clearTestData()
    }

    @After
    open fun tearDown() {
        clearTestData()
    }

    /**
     * Clears all data from the database for test isolation.
     */
    private fun clearTestData() {
        database.clearAllTables()
    }

    /**
     * Wait for idle state in Compose, UI thread, and background work.
     */
    protected fun waitForIdle() {
        composeRule.waitForIdle()
    }

    /**
     * Wait until a condition is met, with timeout.
     * Useful for async operations (AI parsing, database writes).
     */
    protected fun waitUntil(
        timeoutMs: Long = 5_000L,
        conditionDescription: String = "condition",
        condition: () -> Boolean
    ) {
        composeRule.waitUntil(timeoutMillis = timeoutMs) {
            condition()
        }
    }
}

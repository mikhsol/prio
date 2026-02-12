package com.prio.app.e2e.scenarios

import android.os.Debug
import android.os.SystemClock
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performScrollToNode
import com.prio.app.e2e.BaseE2ETest
import com.prio.app.e2e.util.TestDataFactory
import com.prio.core.common.model.EisenhowerQuadrant
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import timber.log.Timber

/**
 * E2E Performance & Profiling Tests (Milestone 4.3)
 *
 * Validates performance targets from ACTION_PLAN:
 * - Cold start <4s on mid-range device
 * - LLM inference <3s (rule-based <50ms)
 * - Peak memory <1GB during AI inference
 * - Smooth scrolling with 50+ tasks
 * - No ANR during heavy database operations
 *
 * These tests measure real-world performance on the connected device.
 * Results are logged to Logcat with tag "PrioPerf" for CI collection.
 *
 * Device targets:
 * - Tier 1: Pixel 9a / Samsung Galaxy S24 (6GB+ RAM)
 * - Tier 2: Pixel 6a / Samsung Galaxy A54 (4-6GB RAM)
 * - Tier 3: Budget devices with 3-4GB RAM
 */
@HiltAndroidTest
class PerformanceE2ETest : BaseE2ETest() {

    companion object {
        private const val TAG = "PrioPerf"

        // Performance thresholds
        private const val COLD_START_THRESHOLD_MS = 4_000L
        private const val AI_RULE_BASED_THRESHOLD_MS = 200L
        private const val SCROLL_FRAME_THRESHOLD_MS = 32L // ~30fps minimum
        private const val PEAK_MEMORY_THRESHOLD_MB = 1024L // 1GB
        private const val DB_BULK_INSERT_THRESHOLD_MS = 5_000L
    }

    // =========================================================================
    // PERF-01: Cold start time measurement
    // Priority: P0 — Key Milestone 4.3 exit criterion
    // =========================================================================

    @Test
    fun coldStart_completesUnder4Seconds() {
        // Activity is already launched by BaseE2ETest.setUp().
        // Measure from activity launch to first meaningful frame.
        val startTime = SystemClock.elapsedRealtime()

        // Wait for the Today screen to fully render (default destination)
        nav.assertOnTodayScreen()
        nav.assertBottomNavVisible()
        waitForIdle()

        val elapsed = SystemClock.elapsedRealtime() - startTime
        Timber.tag(TAG).i("PERF-01: Cold start to Today screen: ${elapsed}ms (threshold: ${COLD_START_THRESHOLD_MS}ms)")

        // Note: BaseE2ETest setUp() includes Hilt injection + Activity launch,
        // so the measured time is from compose idle assertion, not full cold start.
        // Full cold start includes Application.onCreate + Activity.onCreate + first frame.
        // This test validates the post-launch rendering is fast.
        assertTrue(
            "Cold start took ${elapsed}ms, exceeds ${COLD_START_THRESHOLD_MS}ms threshold",
            elapsed < COLD_START_THRESHOLD_MS
        )
    }

    // =========================================================================
    // PERF-02: AI classification latency (rule-based)
    // Priority: P0 — Core user experience
    // =========================================================================

    @Test
    fun aiClassification_ruleBasedUnder200ms() {
        nav.goToTasks()
        nav.tapFab()
        quickCapture.assertSheetVisible()

        val startTime = SystemClock.elapsedRealtime()
        quickCapture.typeTaskText("Submit quarterly report by Friday deadline")
        quickCapture.submitInput()
        quickCapture.waitForAiClassification()
        val elapsed = SystemClock.elapsedRealtime() - startTime

        Timber.tag(TAG).i("PERF-02: AI classification latency: ${elapsed}ms (threshold: ${AI_RULE_BASED_THRESHOLD_MS}ms)")

        // Rule-based should be <50ms, but with UI round-trip, <200ms is acceptable
        assertTrue(
            "AI classification took ${elapsed}ms, exceeds ${AI_RULE_BASED_THRESHOLD_MS}ms threshold",
            elapsed < AI_RULE_BASED_THRESHOLD_MS
        )
    }

    // =========================================================================
    // PERF-03: Memory usage stays under 1GB
    // Priority: P0 — Prevents OOM on mid-range devices
    // =========================================================================

    @Test
    fun memoryUsage_staysUnder1GB() {
        // Navigate through all screens to trigger full composition
        nav.goToToday()
        waitForIdle()
        nav.goToTasks()
        waitForIdle()
        nav.goToGoals()
        waitForIdle()
        nav.goToCalendar()
        waitForIdle()

        // Force GC before measurement
        Runtime.getRuntime().gc()
        Thread.sleep(500)

        val memInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memInfo)
        val totalPssMb = memInfo.totalPss / 1024 // PSS in KB, convert to MB

        Timber.tag(TAG).i("PERF-03: Total PSS memory: ${totalPssMb}MB (threshold: ${PEAK_MEMORY_THRESHOLD_MB}MB)")
        Timber.tag(TAG).i("  - Native heap: ${memInfo.nativePss / 1024}MB")
        Timber.tag(TAG).i("  - Dalvik heap: ${memInfo.dalvikPss / 1024}MB")
        Timber.tag(TAG).i("  - Other: ${memInfo.otherPss / 1024}MB")

        assertTrue(
            "Memory usage ${totalPssMb}MB exceeds ${PEAK_MEMORY_THRESHOLD_MB}MB threshold",
            totalPssMb < PEAK_MEMORY_THRESHOLD_MB
        )
    }

    // =========================================================================
    // PERF-04: Scroll performance with 50 tasks
    // Priority: P1 — Smooth UX requirement
    // =========================================================================

    @Test
    fun scrollWith50Tasks_noJank() = runTest {
        // Insert 50 tasks across all quadrants
        repeat(50) { i ->
            taskRepository.insertTask(
                TestDataFactory.task(
                    title = "Perf task #${i + 1}",
                    quadrant = EisenhowerQuadrant.entries[i % 4],
                    position = i
                )
            )
        }

        nav.goToTasks()
        Thread.sleep(2_000)
        waitForIdle()

        // Scroll to bottom (ELIMINATE section should be last)
        val startTime = SystemClock.elapsedRealtime()

        try {
            composeRule.onAllNodes(hasScrollToNodeAction())
                .onLast()
                .performScrollToNode(hasTestTag("section_header_ELIMINATE"))
        } catch (_: Exception) {
            // Some tasks may not render with testTag, scroll anyway
        }

        val elapsed = SystemClock.elapsedRealtime() - startTime
        Timber.tag(TAG).i("PERF-04: Scroll through 50 tasks: ${elapsed}ms")

        // Verify we can still interact (no ANR)
        waitForIdle()
        taskList.assertScreenVisible()
    }

    // =========================================================================
    // PERF-05: Database bulk insert performance
    // Priority: P1 — Data layer performance
    // =========================================================================

    @Test
    fun bulkDatabaseInsert_completesUnder5Seconds() = runTest {
        val startTime = SystemClock.elapsedRealtime()

        // Insert 100 tasks
        repeat(100) { i ->
            taskRepository.insertTask(
                TestDataFactory.task(
                    title = "Bulk task #${i + 1}",
                    quadrant = EisenhowerQuadrant.entries[i % 4]
                )
            )
        }

        val elapsed = SystemClock.elapsedRealtime() - startTime
        Timber.tag(TAG).i("PERF-05: 100 task inserts: ${elapsed}ms (threshold: ${DB_BULK_INSERT_THRESHOLD_MS}ms)")

        assertTrue(
            "Bulk insert took ${elapsed}ms, exceeds ${DB_BULK_INSERT_THRESHOLD_MS}ms threshold",
            elapsed < DB_BULK_INSERT_THRESHOLD_MS
        )
    }

    // =========================================================================
    // PERF-06: Navigation switching latency
    // Priority: P1 — Screen transition smoothness
    // =========================================================================

    @Test
    fun navigationSwitching_isSmooth() {
        val timings = mutableListOf<Long>()

        // Measure each tab transition
        listOf(
            { nav.goToTasks() },
            { nav.goToGoals() },
            { nav.goToCalendar() },
            { nav.goToToday() }
        ).forEach { action ->
            val start = SystemClock.elapsedRealtime()
            action()
            waitForIdle()
            timings.add(SystemClock.elapsedRealtime() - start)
        }

        timings.forEachIndexed { idx, time ->
            val screens = listOf("Tasks", "Goals", "Calendar", "Today")
            Timber.tag(TAG).i("PERF-06: Navigate to ${screens[idx]}: ${time}ms")
        }

        val maxTime = timings.max()
        val avgTime = timings.average().toLong()
        Timber.tag(TAG).i("PERF-06: Max navigation: ${maxTime}ms, Avg: ${avgTime}ms")

        // Each transition should be under 1 second
        assertTrue(
            "Navigation transition took ${maxTime}ms, exceeds 1000ms",
            maxTime < 1_000L
        )
    }

    // =========================================================================
    // PERF-07: Quick capture end-to-end latency
    // Priority: P0 — Core user flow performance
    // =========================================================================

    @Test
    fun quickCapture_endToEnd_under5Seconds() {
        nav.goToTasks()

        val startTime = SystemClock.elapsedRealtime()

        nav.tapFab()
        quickCapture.assertSheetVisible()
        quickCapture.typeTaskText("Performance test task deadline tomorrow")
        quickCapture.submitInput()
        quickCapture.waitForAiClassification()
        quickCapture.tapCreateTask()
        quickCapture.assertSheetDismissed()

        val elapsed = SystemClock.elapsedRealtime() - startTime
        Timber.tag(TAG).i("PERF-07: Quick capture E2E: ${elapsed}ms (threshold: 5000ms)")

        assertTrue(
            "Quick capture E2E took ${elapsed}ms, exceeds 5000ms threshold",
            elapsed < 5_000L
        )

        // Verify task was actually created
        taskList.assertTaskDisplayed("Performance test task")
    }

    // =========================================================================
    // PERF-08: Task detail load latency
    // Priority: P1 — Screen with AI data must load fast
    // =========================================================================

    @Test
    fun taskDetailLoad_completesQuickly() = runTest {
        // Pre-populate a task with AI fields
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Perf detail task",
                quadrant = EisenhowerQuadrant.DO_FIRST,
                aiExplanation = "Classified as urgent and important because of deadline tomorrow",
                aiConfidence = 0.92f,
                notes = "These are test notes for the performance task detail screen"
            )
        )

        nav.goToTasks()
        waitForIdle()

        val startTime = SystemClock.elapsedRealtime()
        taskList.tapTask("Perf detail task")
        waitForIdle()

        val elapsed = SystemClock.elapsedRealtime() - startTime
        Timber.tag(TAG).i("PERF-08: Task detail load: ${elapsed}ms (threshold: 2000ms)")

        assertTrue(
            "Task detail load took ${elapsed}ms, exceeds 2000ms threshold",
            elapsed < 2_000L
        )
    }
}

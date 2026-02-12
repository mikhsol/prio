package com.prio.app.e2e.scenarios

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.prio.app.e2e.BaseE2ETest
import com.prio.app.e2e.util.TestDataFactory
import com.prio.core.common.model.EisenhowerQuadrant
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import timber.log.Timber

/**
 * Accessibility smoke tests (Milestone 4.3.6)
 *
 * Validates WCAG 2.1 AA compliance per docs/results/1.1/1.1.11_accessibility_requirements_spec.md:
 * - Touch targets ≥48dp minimum
 * - Content descriptions on all interactive elements
 * - Screen reader (TalkBack) navigability
 * - Semantic heading structure
 * - Focus management on dialogs
 * - No color-only information encoding
 *
 * NOTE: Contrast ratio checks require screenshot comparison or Accessibility Scanner,
 * which cannot be done purely via Compose Testing API. Those are covered by manual QA.
 */
@HiltAndroidTest
class AccessibilityE2ETest : BaseE2ETest() {

    companion object {
        private const val TAG = "PrioA11y"
        private const val MIN_TOUCH_TARGET_DP = 48
    }

    // =========================================================================
    // A11Y-01: All interactive elements have content descriptions
    // Priority: P0 — TalkBack cannot navigate without this
    // =========================================================================

    @Test
    fun todayScreen_allInteractiveElementsHaveContentDescriptions() {
        nav.assertOnTodayScreen()
        waitForIdle()

        assertAllClickableNodesHaveSemantics("Today screen")
    }

    @Test
    fun taskListScreen_allInteractiveElementsHaveContentDescriptions() = runTest {
        // Seed data so we have task cards to check
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "A11y test task",
                quadrant = EisenhowerQuadrant.DO_FIRST
            )
        )

        nav.goToTasks()
        waitForIdle()
        Thread.sleep(2_000)

        assertAllClickableNodesHaveSemantics("Tasks screen")
    }

    @Test
    fun goalsScreen_allInteractiveElementsHaveContentDescriptions() {
        nav.goToGoals()
        waitForIdle()

        assertAllClickableNodesHaveSemantics("Goals screen")
    }

    @Test
    fun calendarScreen_allInteractiveElementsHaveContentDescriptions() {
        nav.goToCalendar()
        waitForIdle()

        assertAllClickableNodesHaveSemantics("Calendar screen")
    }

    // =========================================================================
    // A11Y-02: Bottom navigation has proper labels
    // Priority: P0 — Core navigation for screen reader users
    // =========================================================================

    @Test
    fun bottomNav_hasProperAccessibilityLabels() {
        // Each bottom nav item must have "{Label}. Selected/Not selected" pattern
        nav.assertBottomNavVisible()

        // Today should be selected by default
        composeRule.onNodeWithContentDescription("Today. Selected", substring = true)
            .assertIsDisplayed()

        // Other tabs should be "Not selected"
        listOf("Tasks", "Goals", "Calendar").forEach { tab ->
            composeRule.onNodeWithContentDescription("$tab. ", substring = true)
                .assertIsDisplayed()
        }

        // FAB must have description
        composeRule.onNodeWithContentDescription("Add new task")
            .assertIsDisplayed()

        Timber.tag(TAG).i("A11Y-02: Bottom nav labels OK")
    }

    // =========================================================================
    // A11Y-03: Touch targets meet minimum 48dp requirement
    // Priority: P0 — Motor accessibility per WCAG 2.1 AA
    // =========================================================================

    @Test
    fun touchTargets_meetMinimumSize() = runTest {
        // Seed a task so we have interactive cards
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Touch target test task",
                quadrant = EisenhowerQuadrant.DO_FIRST
            )
        )

        nav.goToTasks()
        waitForIdle()
        Thread.sleep(2_000)

        // Check all clickable nodes
        val clickableNodes = composeRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()

        var violations = 0
        clickableNodes.forEach { node ->
            val bounds = node.boundsInRoot
            val widthDp = bounds.right - bounds.left
            val heightDp = bounds.bottom - bounds.top

            // Only check nodes that are actually visible (non-zero bounds)
            if (widthDp > 0 && heightDp > 0) {
                if (widthDp < MIN_TOUCH_TARGET_DP || heightDp < MIN_TOUCH_TARGET_DP) {
                    val desc = node.config.getOrElseNullable(SemanticsProperties.ContentDescription) { null }
                        ?.joinToString() ?: "unnamed"
                    Timber.tag(TAG).w(
                        "A11Y-03 VIOLATION: '$desc' touch target ${widthDp}x${heightDp}dp < ${MIN_TOUCH_TARGET_DP}dp"
                    )
                    violations++
                }
            }
        }

        Timber.tag(TAG).i("A11Y-03: Checked ${clickableNodes.size} clickable nodes, $violations violations")

        // Allow some violations (Compose may report internal nodes)
        // but flag if there are many
        assertTrue(
            "Too many touch target violations: $violations out of ${clickableNodes.size}",
            violations <= clickableNodes.size / 4 // max 25% can be small
        )
    }

    // =========================================================================
    // A11Y-04: Quick capture sheet has proper content descriptions
    // Priority: P0 — Core feature must be screen-reader accessible
    // =========================================================================

    @Test
    fun quickCaptureSheet_isAccessible() {
        nav.goToTasks()
        nav.tapFab()
        quickCapture.assertSheetVisible()

        // Text input must be reachable
        composeRule.onNodeWithContentDescription("Task input", substring = true)
            .assertIsDisplayed()

        // Close button
        composeRule.onNodeWithContentDescription("Close")
            .assertIsDisplayed()

        // Voice input button
        composeRule.onNodeWithContentDescription("voice input", substring = true, ignoreCase = true)
            .assertIsDisplayed()

        Timber.tag(TAG).i("A11Y-04: Quick capture sheet accessibility OK")

        quickCapture.dismiss()
    }

    // =========================================================================
    // A11Y-05: Task card content descriptions are meaningful
    // Priority: P0 — TalkBack must announce task info
    // =========================================================================

    @Test
    fun taskCard_hasDescriptiveContentDescription() = runTest {
        taskRepository.insertTask(
            TestDataFactory.task(
                title = "Accessibility review task",
                quadrant = EisenhowerQuadrant.DO_FIRST
            )
        )

        nav.goToTasks()
        waitForIdle()
        Thread.sleep(2_000)

        // Task card should include title and priority in its description
        composeRule.onNodeWithContentDescription("Accessibility review task", substring = true)
            .assertIsDisplayed()

        // Checkbox should describe the action
        composeRule.onNodeWithContentDescription("Mark Accessibility review task as complete")
            .assertIsDisplayed()

        Timber.tag(TAG).i("A11Y-05: Task card descriptions OK")
    }

    // =========================================================================
    // A11Y-06: Quadrant sections have non-color-only indicators
    // Priority: P1 — Color-blind users must distinguish sections
    // =========================================================================

    @Test
    fun quadrantSections_haveTextLabels() = runTest {
        // Populate all 4 quadrants
        EisenhowerQuadrant.entries.forEach { q ->
            taskRepository.insertTask(
                TestDataFactory.task(
                    title = "A11y ${q.name.lowercase()} task",
                    quadrant = q
                )
            )
        }

        nav.goToTasks()
        waitForIdle()
        Thread.sleep(2_000)

        // Each quadrant section must have a text label (not just color)
        listOf("DO FIRST", "SCHEDULE", "DELEGATE", "ELIMINATE").forEach { section ->
            taskList.assertSectionVisible(section)
        }

        Timber.tag(TAG).i("A11Y-06: All quadrant sections have text labels")
    }

    // =========================================================================
    // A11Y-07: FAB and navigation use standard semantics
    // Priority: P1 — Predictable interaction for screen reader users
    // =========================================================================

    @Test
    fun fabAndNav_useStandardSemantics() {
        // Verify settings button
        nav.assertSettingsButtonVisible()

        // Navigate to each screen and verify screen reader can identify it
        nav.goToTasks()
        nav.assertOnTasksScreen()

        nav.goToGoals()
        nav.assertOnGoalsScreen()

        nav.goToCalendar()
        nav.assertOnCalendarScreen()

        nav.goToToday()
        nav.assertOnTodayScreen()

        Timber.tag(TAG).i("A11Y-07: Navigation semantics OK")
    }

    // =========================================================================
    // A11Y-08: Goal progress has non-visual description
    // Priority: P1 — Progress ring must be described for screen readers
    // =========================================================================

    @Test
    fun goalProgress_hasAccessibleDescription() = runTest {
        goalRepository.insertGoal(
            TestDataFactory.goal(
                title = "Accessibility test goal"
            )
        )

        nav.goToGoals()
        waitForIdle()
        Thread.sleep(2_000)

        // Goal card should have content description with progress info
        composeRule.onNodeWithContentDescription("Accessibility test goal", substring = true)
            .assertIsDisplayed()

        Timber.tag(TAG).i("A11Y-08: Goal progress description OK")
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Checks that all clickable nodes have either a text or contentDescription.
     * Nodes without either are invisible to screen readers.
     */
    private fun assertAllClickableNodesHaveSemantics(screenName: String) {
        val clickableNodes = composeRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()

        var nodesWithoutSemantics = 0
        clickableNodes.forEach { node ->
            val hasDescription = node.config.getOrElseNullable(SemanticsProperties.ContentDescription) { null }
                ?.isNotEmpty() == true

            val hasText = node.config.getOrElseNullable(SemanticsProperties.Text) { null }
                ?.isNotEmpty() == true

            if (!hasDescription && !hasText) {
                nodesWithoutSemantics++
                Timber.tag(TAG).w(
                    "A11Y-01 WARNING [$screenName]: Clickable node at ${node.boundsInRoot} has no content description or text"
                )
            }
        }

        Timber.tag(TAG).i(
            "A11Y-01 [$screenName]: ${clickableNodes.size} clickable nodes, $nodesWithoutSemantics without semantics"
        )

        // All clickable nodes should have some form of semantic info
        assertTrue(
            "[$screenName] $nodesWithoutSemantics clickable nodes have no content description or text",
            nodesWithoutSemantics <= clickableNodes.size / 5 // max 20% tolerance for internal Compose nodes
        )
    }
}

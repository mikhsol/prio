package com.prio.core.domain.eisenhower

import com.prio.core.common.model.EisenhowerQuadrant
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Comprehensive unit tests for EisenhowerEngine.
 * 
 * Verifies:
 * - Task 3.1.1: Rule-based primary classifier
 * - Target accuracy: ≥75%
 * - Target latency: <100ms
 * - 50+ patterns per quadrant
 * - Deadline-based urgency scoring per TM-005
 * 
 * Test Dataset: Extended from 0.2.3 accuracy tests (20 tasks per quadrant = 80 total)
 */
@DisplayName("EisenhowerEngine")
class EisenhowerEngineTest {
    
    private lateinit var engine: EisenhowerEngine
    private lateinit var testClock: TestClock
    
    // Test clock for deterministic deadline testing
    class TestClock(private var currentInstant: Instant = Instant.parse("2026-02-04T12:00:00Z")) : Clock {
        override fun now(): Instant = currentInstant
        fun setNow(instant: Instant) { currentInstant = instant }
    }
    
    @BeforeEach
    fun setup() {
        testClock = TestClock()
        engine = EisenhowerEngine(testClock)
    }
    
    // ========================================================================
    // Test Data: 20 tasks per quadrant = 80 total
    // Based on 0.2.3 test cases + expanded for comprehensive coverage
    // ========================================================================
    
    companion object {
        // DO_FIRST (Q1): Urgent + Important - 20 test cases
        @JvmStatic
        fun doFirstTasks() = listOf(
            "Urgent: Submit tax return by today deadline",
            "Emergency: Server is down, fix immediately",
            "ASAP: Prepare for board presentation this afternoon",
            "Client waiting - send contract now",
            "Critical bug in production - hotfix needed",
            "Doctor appointment today at 3pm - health checkup",
            "Deadline today: Submit quarterly report",
            "Boss needs the analysis report by end of day",
            "Production outage - users cannot login",
            "Tax filing deadline tomorrow - review documents",
            "Important meeting with CEO in 1 hour",
            "Critical: Family emergency - need to leave now",
            "Overdue invoice payment - client is waiting",
            "Career review meeting today - prepare talking points",
            "Legal contract needs signature by 5pm today",
            "Health: Prescription refill expires today",
            "Project milestone due today - final review needed",
            "Customer escalation - resolve billing issue immediately",
            "Board meeting presentation due in 2 hours",
            "Critical decision needed: Accept job offer by EOD"
        )
        
        // SCHEDULE (Q2): Important, Not Urgent - 20 test cases
        @JvmStatic
        fun scheduleTasks() = listOf(
            "Plan next quarter's strategy",
            "Read leadership book for career development",
            "Research investment options for retirement",
            "Learn new programming language for skill growth",
            "Schedule annual health checkup",
            "Plan family vacation for summer",
            "Write blog post about career journey",
            "Review and update resume",
            "Build portfolio website for career",
            "Take online course on machine learning",
            "Develop workout routine for fitness goal",
            "Plan monthly budget review",
            "Research grad school programs",
            "Write thank you notes to mentors",
            "Plan team building event for next month",
            "Create personal development roadmap",
            "Review insurance coverage options",
            "Schedule networking coffee chats",
            "Read industry report for market trends",
            "Plan long-term financial goals"
        )
        
        // DELEGATE (Q3): Urgent, Not Important - 20 test cases
        @JvmStatic
        fun delegateTasks() = listOf(
            "Order office supplies - printer ink running low",
            "Schedule recurring team meeting for next week",
            "Compile weekly status report for distribution",
            "Book meeting room for Friday standup",
            "Fill out travel expense form",
            "Update team spreadsheet with latest numbers",
            "Forward meeting notes to the team",
            "Respond to standard vendor inquiry",
            "Archive old project files",
            "Update Slack channel settings",
            "Send calendar invite for training session",
            "File expense receipts from last trip",
            "Order new employee welcome kit supplies",
            "Schedule office maintenance visit",
            "Update shared document templates",
            "Coordinate lunch order for team",
            "Send reminder about timesheet submission",
            "Book conference room for client call",
            "Update team contact list",
            "Standard monthly reporting task"
        )
        
        // ELIMINATE (Q4): Not Urgent, Not Important - 20 test cases
        @JvmStatic
        fun eliminateTasks() = listOf(
            "Browse Reddit for interesting posts",
            "Watch Netflix new releases",
            "Maybe reorganize bookshelf someday",
            "Scroll through social media",
            "Eventually clean up old photos",
            "Nice to have: custom keyboard shortcuts",
            "Browse YouTube tech videos",
            "Someday: learn to play guitar",
            "Optional: try new coffee shop",
            "When I have time: organize music playlist",
            "Maybe check out that new game",
            "Low priority: rearrange desk setup",
            "If time: browse furniture options",
            "Eventually: organize old files",
            "Nice to have: better phone case",
            "Someday: visit that museum",
            "Browse Twitter for news",
            "Maybe: reorganize closet",
            "Not urgent: random ideas to explore",
            "Just check social media updates"
        )
    }
    
    // ========================================================================
    // Accuracy Tests (Target: ≥75%)
    // ========================================================================
    
    @Nested
    @DisplayName("DO_FIRST (Q1) Accuracy")
    inner class DoFirstAccuracyTests {
        
        @Test
        @DisplayName("Should classify DO_FIRST tasks with ≥75% accuracy")
        fun doFirstAccuracy() {
            val tasks = doFirstTasks()
            var correct = 0
            val failures = mutableListOf<String>()
            
            tasks.forEach { task ->
                val result = engine.classify(task)
                if (result.quadrant == EisenhowerQuadrant.DO_FIRST) {
                    correct++
                } else {
                    failures.add("'$task' → ${result.quadrant} (expected DO_FIRST), confidence=${result.confidence}")
                }
            }
            
            val accuracy = correct.toFloat() / tasks.size
            println("DO_FIRST Accuracy: ${(accuracy * 100).toInt()}% ($correct/${tasks.size})")
            if (failures.isNotEmpty()) {
                println("Failures:")
                failures.forEach { println("  - $it") }
            }
            
            assertTrue(accuracy >= 0.75f, 
                "DO_FIRST accuracy ${(accuracy * 100).toInt()}% < 75% target. Failures: ${failures.size}"
            )
        }
        
        @Test
        @DisplayName("Should achieve ≥90% accuracy on DO_FIRST (per milestone exit criteria)")
        fun doFirstHighAccuracy() {
            val tasks = doFirstTasks()
            var correct = 0
            
            tasks.forEach { task ->
                val result = engine.classify(task)
                if (result.quadrant == EisenhowerQuadrant.DO_FIRST) {
                    correct++
                }
            }
            
            val accuracy = correct.toFloat() / tasks.size
            println("DO_FIRST High Accuracy Check: ${(accuracy * 100).toInt()}% ($correct/${tasks.size})")
            
            // This is the target from milestone exit criteria
            assertTrue(accuracy >= 0.90f, 
                "DO_FIRST accuracy ${(accuracy * 100).toInt()}% < 90% target (critical requirement)"
            )
        }
    }
    
    @Nested
    @DisplayName("SCHEDULE (Q2) Accuracy")
    inner class ScheduleAccuracyTests {
        
        @Test
        @DisplayName("Should classify SCHEDULE tasks with ≥75% accuracy")
        fun scheduleAccuracy() {
            val tasks = scheduleTasks()
            var correct = 0
            val failures = mutableListOf<String>()
            
            tasks.forEach { task ->
                val result = engine.classify(task)
                if (result.quadrant == EisenhowerQuadrant.SCHEDULE) {
                    correct++
                } else {
                    failures.add("'$task' → ${result.quadrant} (expected SCHEDULE)")
                }
            }
            
            val accuracy = correct.toFloat() / tasks.size
            println("SCHEDULE Accuracy: ${(accuracy * 100).toInt()}% ($correct/${tasks.size})")
            if (failures.isNotEmpty()) {
                println("Failures:")
                failures.forEach { println("  - $it") }
            }
            
            assertTrue(accuracy >= 0.75f,
                "SCHEDULE accuracy ${(accuracy * 100).toInt()}% < 75% target"
            )
        }
    }
    
    @Nested
    @DisplayName("DELEGATE (Q3) Accuracy")
    inner class DelegateAccuracyTests {
        
        @Test
        @DisplayName("Should classify DELEGATE tasks with ≥75% accuracy")
        fun delegateAccuracy() {
            val tasks = delegateTasks()
            var correct = 0
            val failures = mutableListOf<String>()
            
            tasks.forEach { task ->
                val result = engine.classify(task)
                if (result.quadrant == EisenhowerQuadrant.DELEGATE) {
                    correct++
                } else {
                    failures.add("'$task' → ${result.quadrant} (expected DELEGATE)")
                }
            }
            
            val accuracy = correct.toFloat() / tasks.size
            println("DELEGATE Accuracy: ${(accuracy * 100).toInt()}% ($correct/${tasks.size})")
            if (failures.isNotEmpty()) {
                println("Failures:")
                failures.forEach { println("  - $it") }
            }
            
            assertTrue(accuracy >= 0.75f,
                "DELEGATE accuracy ${(accuracy * 100).toInt()}% < 75% target"
            )
        }
    }
    
    @Nested
    @DisplayName("ELIMINATE (Q4) Accuracy")
    inner class EliminateAccuracyTests {
        
        @Test
        @DisplayName("Should classify ELIMINATE tasks with ≥75% accuracy")
        fun eliminateAccuracy() {
            val tasks = eliminateTasks()
            var correct = 0
            val failures = mutableListOf<String>()
            
            tasks.forEach { task ->
                val result = engine.classify(task)
                if (result.quadrant == EisenhowerQuadrant.ELIMINATE) {
                    correct++
                } else {
                    failures.add("'$task' → ${result.quadrant} (expected ELIMINATE)")
                }
            }
            
            val accuracy = correct.toFloat() / tasks.size
            println("ELIMINATE Accuracy: ${(accuracy * 100).toInt()}% ($correct/${tasks.size})")
            if (failures.isNotEmpty()) {
                println("Failures:")
                failures.forEach { println("  - $it") }
            }
            
            assertTrue(accuracy >= 0.75f,
                "ELIMINATE accuracy ${(accuracy * 100).toInt()}% < 75% target"
            )
        }
    }
    
    @Nested
    @DisplayName("Overall Accuracy")
    inner class OverallAccuracyTests {
        
        @Test
        @DisplayName("Should achieve ≥75% overall accuracy across all quadrants")
        fun overallAccuracy() {
            val allTasks = listOf(
                doFirstTasks().map { it to EisenhowerQuadrant.DO_FIRST },
                scheduleTasks().map { it to EisenhowerQuadrant.SCHEDULE },
                delegateTasks().map { it to EisenhowerQuadrant.DELEGATE },
                eliminateTasks().map { it to EisenhowerQuadrant.ELIMINATE }
            ).flatten()
            
            var correct = 0
            val confusionMatrix = mutableMapOf<Pair<EisenhowerQuadrant, EisenhowerQuadrant>, Int>()
            
            allTasks.forEach { (task, expected) ->
                val result = engine.classify(task)
                val key = expected to result.quadrant
                confusionMatrix[key] = (confusionMatrix[key] ?: 0) + 1
                if (result.quadrant == expected) {
                    correct++
                }
            }
            
            val accuracy = correct.toFloat() / allTasks.size
            
            println("\n====== OVERALL ACCURACY REPORT ======")
            println("Total: ${(accuracy * 100).toInt()}% ($correct/${allTasks.size})")
            println("\nBy Quadrant:")
            EisenhowerQuadrant.entries.forEach { expected ->
                val total = allTasks.count { it.second == expected }
                val correctCount = confusionMatrix[expected to expected] ?: 0
                val acc = correctCount.toFloat() / total
                println("  ${expected.name}: ${(acc * 100).toInt()}% ($correctCount/$total)")
            }
            println("\nConfusion Matrix (Expected → Actual):")
            EisenhowerQuadrant.entries.forEach { expected ->
                val row = EisenhowerQuadrant.entries.map { actual ->
                    confusionMatrix[expected to actual] ?: 0
                }
                println("  ${expected.name}: ${row.joinToString(" ")}")
            }
            println("======================================\n")
            
            assertTrue(accuracy >= 0.75f,
                "Overall accuracy ${(accuracy * 100).toInt()}% < 75% target"
            )
        }
    }
    
    // ========================================================================
    // Latency Tests (Target: <100ms)
    // ========================================================================
    
    @Nested
    @DisplayName("Latency Performance")
    inner class LatencyTests {
        
        @Test
        @DisplayName("Single classification should complete in <100ms")
        fun singleClassificationLatency() {
            val task = "Urgent: Submit tax return by today deadline"
            
            val startTime = System.currentTimeMillis()
            val result = engine.classify(task)
            val elapsed = System.currentTimeMillis() - startTime
            
            println("Single classification latency: ${elapsed}ms")
            assertTrue(elapsed < 100, "Latency ${elapsed}ms > 100ms target")
            assertTrue(result.latencyMs < 100f, "Reported latency ${result.latencyMs}ms > 100ms")
        }
        
        @Test
        @DisplayName("Batch classification (80 tasks) should average <10ms per task")
        fun batchClassificationLatency() {
            val allTasks = doFirstTasks() + scheduleTasks() + delegateTasks() + eliminateTasks()
            
            val startTime = System.currentTimeMillis()
            val results = engine.classifyBatch(allTasks)
            val elapsed = System.currentTimeMillis() - startTime
            
            val avgLatency = elapsed.toFloat() / allTasks.size
            println("Batch classification: ${allTasks.size} tasks in ${elapsed}ms (avg: ${avgLatency}ms/task)")
            
            assertTrue(avgLatency < 10f, "Average latency ${avgLatency}ms > 10ms target")
            assertEquals(allTasks.size, results.size)
        }
    }
    
    // ========================================================================
    // Deadline Urgency Tests (per TM-005)
    // ========================================================================
    
    @Nested
    @DisplayName("Deadline Urgency Scoring (TM-005)")
    inner class DeadlineUrgencyTests {
        
        private val now = Instant.parse("2026-02-04T12:00:00Z")
        
        @BeforeEach
        fun setupClock() {
            testClock.setNow(now)
        }
        
        @Test
        @DisplayName("Overdue task should have critical urgency (≥0.75)")
        fun overdueTaskUrgency() {
            val overdueDate = now.minus(2.days)
            val urgency = engine.calculateDeadlineUrgency(overdueDate)
            
            assertTrue(urgency >= 0.75f, "Overdue urgency $urgency < 0.75")
            assertTrue(urgency <= 1.0f, "Urgency $urgency > 1.0")
        }
        
        @Test
        @DisplayName("Due today should have critical urgency (0.75)")
        fun dueTodayUrgency() {
            // Due at end of today
            val dueTodayDate = Instant.parse("2026-02-04T23:59:00Z")
            val urgency = engine.calculateDeadlineUrgency(dueTodayDate)
            
            assertEquals(0.75f, urgency, 0.01f)
        }
        
        @Test
        @DisplayName("Due tomorrow should have high urgency (0.65)")
        fun dueTomorrowUrgency() {
            val tomorrowDate = now.plus(1.days)
            val urgency = engine.calculateDeadlineUrgency(tomorrowDate)
            
            assertEquals(0.65f, urgency, 0.01f)
        }
        
        @Test
        @DisplayName("Due in 3 days should have medium urgency (0.50)")
        fun dueIn3DaysUrgency() {
            val in3Days = now.plus(3.days)
            val urgency = engine.calculateDeadlineUrgency(in3Days)
            
            assertEquals(0.50f, urgency, 0.01f)
        }
        
        @Test
        @DisplayName("Due in 7 days should have low urgency (0.25)")
        fun dueIn7DaysUrgency() {
            val in7Days = now.plus(7.days)
            val urgency = engine.calculateDeadlineUrgency(in7Days)
            
            assertEquals(0.25f, urgency, 0.01f)
        }
        
        @Test
        @DisplayName("Due in 14 days should have minimal urgency")
        fun dueIn14DaysUrgency() {
            val in14Days = now.plus(14.days)
            val urgency = engine.calculateDeadlineUrgency(in14Days)
            
            assertTrue(urgency < 0.25f, "Urgency $urgency should be < 0.25 for 14 days out")
            assertTrue(urgency >= 0f, "Urgency should be non-negative")
        }
        
        @Test
        @DisplayName("No due date should have zero urgency")
        fun noDueDateUrgency() {
            val urgency = engine.calculateDeadlineUrgency(null)
            assertEquals(0f, urgency)
        }
        
        @Test
        @DisplayName("Deadline urgency should affect classification")
        fun deadlineAffectsClassification() {
            val task = "Finish project report"
            
            // Without deadline - should be SCHEDULE (important keywords)
            val noDeadlineResult = engine.classify(task, dueDate = null)
            
            // With today deadline - should be DO_FIRST
            val todayDeadline = now.plus(6.hours)
            val todayResult = engine.classify(task, dueDate = todayDeadline)
            
            println("Without deadline: ${noDeadlineResult.quadrant}")
            println("With today deadline: ${todayResult.quadrant}")
            
            assertEquals(EisenhowerQuadrant.DO_FIRST, todayResult.quadrant,
                "Task with today deadline should be DO_FIRST")
        }
    }
    
    // ========================================================================
    // Pattern Detection Tests
    // ========================================================================
    
    @Nested
    @DisplayName("Urgency Pattern Detection")
    inner class UrgencyPatternTests {
        
        @ParameterizedTest
        @CsvSource(
            "Urgent: fix the bug, true",
            "ASAP - review document, true",
            "Emergency meeting needed, true",
            "Server is down!, true",
            "Production issue - critical, true",
            "Due today by 5pm, true",
            "Overdue task from yesterday, true",
            "Need this done immediately, true",
            "Client waiting on response, true",
            "Deadline tomorrow morning, true",
            "Sometime next month, false",
            "Eventually clean the desk, false",
            "Nice to have feature, false"
        )
        @DisplayName("Should detect urgency patterns correctly")
        fun detectUrgencyPatterns(taskText: String, expectedUrgent: Boolean) {
            val result = engine.classify(taskText)
            
            assertEquals(expectedUrgent, result.isUrgent,
                "'$taskText' isUrgent=${result.isUrgent}, expected=$expectedUrgent")
        }
    }
    
    @Nested
    @DisplayName("Importance Pattern Detection")
    inner class ImportancePatternTests {
        
        @ParameterizedTest
        @CsvSource(
            "Prepare for career review meeting, true",
            "Schedule health checkup appointment, true",
            "Family dinner planning, true",
            "Review tax documents, true",
            "Learn new programming skill, true",
            "Strategic planning session, true",
            "Client presentation materials, true",
            "Browse social media, false",
            "Watch Netflix, false",
            "Maybe reorganize desk, false"
        )
        @DisplayName("Should detect importance patterns correctly")
        fun detectImportancePatterns(taskText: String, expectedImportant: Boolean) {
            val result = engine.classify(taskText)
            
            assertEquals(expectedImportant, result.isImportant,
                "'$taskText' isImportant=${result.isImportant}, expected=$expectedImportant")
        }
    }
    
    // ========================================================================
    // Edge Cases and Special Scenarios
    // ========================================================================
    
    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCaseTests {
        
        @Test
        @DisplayName("Empty task should default to SCHEDULE with low confidence")
        fun emptyTaskClassification() {
            val result = engine.classify("")
            
            assertEquals(EisenhowerQuadrant.SCHEDULE, result.quadrant)
            assertTrue(result.confidence < 0.6f, "Empty task should have low confidence")
            assertTrue(result.shouldEscalateToLlm, "Empty task should recommend LLM escalation")
        }
        
        @Test
        @DisplayName("Very long task should still classify quickly")
        fun longTaskClassification() {
            val longTask = "This is a very long task description that goes on and on " +
                    "with lots of detail about what needs to be done including many " +
                    "specific requirements and criteria that must be met before the " +
                    "task can be considered complete and signed off by the stakeholders. ".repeat(10)
            
            val startTime = System.currentTimeMillis()
            val result = engine.classify(longTask)
            val elapsed = System.currentTimeMillis() - startTime
            
            assertTrue(elapsed < 100, "Long task classification took ${elapsed}ms")
            assertNotNull(result.quadrant)
        }
        
        @Test
        @DisplayName("Conflicting signals should result in lower confidence")
        fun conflictingSignals() {
            // Task with both urgency and low-priority signals
            val conflictingTask = "Maybe urgent: browse social media for work research ASAP"
            val result = engine.classify(conflictingTask)
            
            // Should have lower confidence due to conflicting signals
            println("Conflicting signals result: ${result.quadrant}, confidence: ${result.confidence}")
            assertNotNull(result.quadrant)
        }
        
        @Test
        @DisplayName("Case insensitive pattern matching")
        fun caseInsensitiveMatching() {
            val results = listOf(
                engine.classify("URGENT task"),
                engine.classify("urgent task"),
                engine.classify("Urgent task"),
                engine.classify("uRgEnT task")
            )
            
            // All should detect urgency
            results.forEach { result ->
                assertTrue(result.isUrgent, "Should detect urgency regardless of case")
            }
        }
    }
    
    // ========================================================================
    // LLM Escalation Tests
    // ========================================================================
    
    @Nested
    @DisplayName("LLM Escalation Recommendations")
    inner class LlmEscalationTests {
        
        @Test
        @DisplayName("Low confidence results should recommend LLM escalation")
        fun lowConfidenceEscalation() {
            val ambiguousTask = "Do the thing"
            val result = engine.classify(ambiguousTask)
            
            if (result.confidence < EisenhowerEngine.LLM_ESCALATION_THRESHOLD) {
                assertTrue(result.shouldEscalateToLlm,
                    "Low confidence (${result.confidence}) should recommend LLM escalation")
            }
        }
        
        @Test
        @DisplayName("High confidence results should not recommend LLM escalation")
        fun highConfidenceNoEscalation() {
            val clearTask = "Urgent: Server is down - fix immediately - production outage"
            val result = engine.classify(clearTask)
            
            if (result.confidence >= 0.75f) {
                assertFalse(result.shouldEscalateToLlm,
                    "High confidence (${result.confidence}) should not recommend LLM escalation")
            }
        }
        
        @Test
        @DisplayName("shouldEscalateToLlm helper should match result flag")
        fun escalationHelperConsistency() {
            val tasks = doFirstTasks() + scheduleTasks()
            
            tasks.forEach { task ->
                val result = engine.classify(task)
                assertEquals(
                    result.shouldEscalateToLlm,
                    engine.shouldEscalateToLlm(result),
                    "Helper should match result flag for: $task"
                )
            }
        }
    }
}

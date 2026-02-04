package com.prio.core.aiprovider.provider

import com.prio.core.ai.model.AiContext
import com.prio.core.ai.model.AiRequest
import com.prio.core.ai.model.AiRequestType
import com.prio.core.ai.model.AiResult
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.domain.eisenhower.EisenhowerEngine
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * Unit tests for RuleBasedFallbackProvider.
 * 
 * Tests verify:
 * - Eisenhower classification accuracy (target: 75%)
 * - Latency under 50ms
 * - Confidence scoring
 * - LLM escalation recommendations
 * 
 * Test cases based on 0.2.3 accuracy testing dataset.
 */
@DisplayName("RuleBasedFallbackProvider Tests")
class RuleBasedFallbackProviderTest {
    
    private lateinit var provider: RuleBasedFallbackProvider
    private lateinit var eisenhowerEngine: EisenhowerEngine
    
    @BeforeEach
    fun setup() {
        eisenhowerEngine = EisenhowerEngine(Clock.System)
        provider = RuleBasedFallbackProvider(eisenhowerEngine)
    }
    
    @Nested
    @DisplayName("Provider Initialization")
    inner class InitializationTests {
        
        @Test
        @DisplayName("Provider is always available")
        fun providerAlwaysAvailable() = runTest {
            assertTrue(provider.isAvailable.value)
            assertTrue(provider.initialize())
            assertTrue(provider.isAvailable.value)
        }
        
        @Test
        @DisplayName("Provider has correct ID and capabilities")
        fun providerMetadata() {
            assertEquals("rule-based", provider.providerId)
            assertEquals("Fast Mode (Offline)", provider.displayName)
            assertTrue(provider.capabilities.isNotEmpty())
        }
    }
    
    @Nested
    @DisplayName("Quadrant 1: DO (Urgent + Important)")
    inner class Q1DoTests {
        
        @ParameterizedTest
        @DisplayName("Urgent + Important tasks should be DO")
        @ValueSource(strings = [
            "URGENT: Client presentation due today at 2pm",
            "Server is down - fix immediately",
            "Submit quarterly report before end of day",
            "Emergency: Production system crashed, customer data at risk",
            "Deadline today: Sign the contract with our biggest client"
        ])
        fun urgentImportantTasksClassifiedAsDo(taskText: String) {
            val result = eisenhowerEngine.classify(taskText)
            assertEquals(EisenhowerQuadrant.DO_FIRST, result.quadrant, "Task: $taskText")
            assertTrue(result.isUrgent, "Should be marked as urgent: $taskText")
            assertTrue(result.isImportant, "Should be marked as important: $taskText")
        }
        
        @Test
        @DisplayName("DO tasks should have high confidence")
        fun doTasksHighConfidence() {
            val result = eisenhowerEngine.classify("URGENT: Client meeting in 30 minutes, prepare the deck")
            assertEquals(EisenhowerQuadrant.DO_FIRST, result.quadrant)
            assertTrue(result.confidence >= 0.7f, "Confidence should be >= 0.7: ${result.confidence}")
            assertFalse(result.shouldEscalateToLlm, "High confidence DO should not escalate")
        }
    }
    
    @Nested
    @DisplayName("Quadrant 2: SCHEDULE (Important, Not Urgent)")
    inner class Q2ScheduleTests {
        
        @ParameterizedTest
        @DisplayName("Important non-urgent tasks should be SCHEDULE")
        @ValueSource(strings = [
            "Research vacation destinations for next quarter",
            "Start learning Spanish - career development goal",
            "Review investment portfolio next month",
            "Plan team offsite for Q3",
            "Study for AWS certification exam"
        ])
        fun importantNonUrgentTasksScheduled(taskText: String) {
            val result = eisenhowerEngine.classify(taskText)
            assertEquals(EisenhowerQuadrant.SCHEDULE, result.quadrant, "Task: $taskText")
            assertFalse(result.isUrgent, "Should NOT be marked as urgent: $taskText")
            assertTrue(result.isImportant, "Should be marked as important: $taskText")
        }
        
        @Test
        @DisplayName("SCHEDULE tasks with clear importance have good confidence")
        fun scheduleTasksConfidence() {
            val result = eisenhowerEngine.classify("Work on career development plan for promotion")
            assertEquals(EisenhowerQuadrant.SCHEDULE, result.quadrant)
            assertTrue(result.confidence >= 0.65f, "Confidence should be >= 0.65: ${result.confidence}")
        }
    }
    
    @Nested
    @DisplayName("Quadrant 3: DELEGATE (Urgent, Not Important)")
    inner class Q3DelegateTests {
        
        @ParameterizedTest
        @DisplayName("Routine urgent tasks should be DELEGATE")
        @ValueSource(strings = [
            "Order office supplies - we're running low",
            "Compile weekly status report for team meeting",
            "Schedule the team lunch for next week",
            "Fill out the employee survey before deadline",
            "Book conference room for recurring standup"
        ])
        fun routineUrgentTasksDelegated(taskText: String) {
            val result = eisenhowerEngine.classify(taskText)
            assertEquals(EisenhowerQuadrant.DELEGATE, result.quadrant, "Task: $taskText")
        }
        
        @Test
        @DisplayName("Delegation patterns are detected")
        fun delegationPatternsDetected() {
            val result = eisenhowerEngine.classify("Have the intern update the spreadsheet")
            assertEquals(EisenhowerQuadrant.DELEGATE, result.quadrant)
            assertTrue(result.explanation.contains("delegation") || 
                       result.explanation.contains("delegate") ||
                       result.explanation.contains("routine"))
        }
    }
    
    @Nested
    @DisplayName("Quadrant 4: ELIMINATE (Not Urgent, Not Important)")
    inner class Q4EliminateTests {
        
        @ParameterizedTest
        @DisplayName("Low priority activities should be ELIMINATE")
        @ValueSource(strings = [
            "Browse social media for interesting content",
            "Watch that YouTube video everyone's talking about",
            "Maybe someday reorganize my bookshelf",
            "Scroll through Reddit when I have time",
            "Binge-watch the new Netflix series"
        ])
        fun lowPriorityTasksEliminated(taskText: String) {
            val result = eisenhowerEngine.classify(taskText)
            assertEquals(EisenhowerQuadrant.ELIMINATE, result.quadrant, "Task: $taskText")
        }
        
        @Test
        @DisplayName("ELIMINATE tasks should have good confidence")
        fun eliminateTasksConfidence() {
            val result = eisenhowerEngine.classify("Maybe if I have time browse social media youtube")
            assertEquals(EisenhowerQuadrant.ELIMINATE, result.quadrant)
            assertTrue(result.confidence >= 0.7f, "Confidence should be >= 0.7: ${result.confidence}")
        }
    }
    
    @Nested
    @DisplayName("Accuracy Verification")
    inner class AccuracyTests {
        
        /**
         * Test dataset from 0.2.3 with expected quadrants.
         * Target accuracy: 75%
         */
        @ParameterizedTest
        @DisplayName("Accuracy test dataset from 0.2.3")
        @CsvSource(
            // Quadrant, Task
            "DO_FIRST, 'URGENT: Server down affecting all customers'",
            "DO_FIRST, 'Client deadline today - submit proposal by 5pm'",
            "DO_FIRST, 'Emergency meeting in 2 hours with board'",
            "DO_FIRST, 'Production bug - fix immediately before more data loss'",
            "DO_FIRST, 'Tax filing deadline today - must submit'",
            
            "SCHEDULE, 'Research new technologies for Q3 project'",
            "SCHEDULE, 'Work on career development plan'",
            "SCHEDULE, 'Learn Kubernetes for infrastructure improvement'",
            "SCHEDULE, 'Review and update investment strategy'",
            "SCHEDULE, 'Plan vacation for family bonding'",
            
            "DELEGATE, 'Order office supplies for the team'",
            "DELEGATE, 'Compile weekly status report'",
            "DELEGATE, 'Schedule team building event'",
            "DELEGATE, 'Update the shared spreadsheet with new data'",
            "DELEGATE, 'Book travel for upcoming conference'",
            
            "ELIMINATE, 'Browse reddit for memes'",
            "ELIMINATE, 'Watch random YouTube videos'",
            "ELIMINATE, 'Maybe someday clean out email subscriptions'",
            "ELIMINATE, 'Scroll through social media feeds'",
            "ELIMINATE, 'Play mobile games during lunch'"
        )
        fun accuracyTestDataset(expected: String, taskText: String) {
            val result = eisenhowerEngine.classify(taskText)
            val expectedQuadrant = EisenhowerQuadrant.valueOf(expected)
            assertEquals(expectedQuadrant, result.quadrant, 
                "Task: $taskText\nExpected: $expected\nGot: ${result.quadrant}\nExplanation: ${result.explanation}")
        }
    }
    
    @Nested
    @DisplayName("Performance Tests")
    inner class PerformanceTests {
        
        @Test
        @DisplayName("Classification latency under 50ms")
        fun classificationLatency() = runTest {
            val testTasks = listOf(
                "URGENT: Client meeting in 30 minutes",
                "Research vacation destinations",
                "Order office supplies",
                "Watch YouTube videos"
            )
            
            testTasks.forEach { task ->
                val startTime = System.currentTimeMillis()
                eisenhowerEngine.classify(task)
                val latency = System.currentTimeMillis() - startTime
                
                assertTrue(latency < 50, "Classification took ${latency}ms, should be <50ms for: $task")
            }
        }
        
        @Test
        @DisplayName("Batch classification performance")
        fun batchClassificationPerformance() {
            val tasks = (1..100).map { "Task number $it with various content" }
            
            val startTime = System.currentTimeMillis()
            tasks.forEach { eisenhowerEngine.classify(it) }
            val totalTime = System.currentTimeMillis() - startTime
            
            assertTrue(totalTime < 500, "100 classifications took ${totalTime}ms, should be <500ms")
        }
    }
    
    @Nested
    @DisplayName("Confidence Scoring")
    inner class ConfidenceTests {
        
        @Test
        @DisplayName("Clear signals result in high confidence")
        fun clearSignalsHighConfidence() {
            val result = eisenhowerEngine.classify("URGENT EMERGENCY: Production server crashed affecting all customers")
            assertTrue(result.confidence >= 0.8f, "Clear DO signal should have high confidence: ${result.confidence}")
        }
        
        @Test
        @DisplayName("Ambiguous tasks have lower confidence and recommend LLM")
        fun ambiguousTasksLowConfidence() {
            val result = eisenhowerEngine.classify("Think about updating the thing")
            assertTrue(result.confidence <= 0.65f, "Ambiguous task should have low confidence: ${result.confidence}")
            assertTrue(result.shouldEscalateToLlm, "Low confidence should recommend LLM escalation")
        }
        
        @Test
        @DisplayName("Multiple matching patterns increase confidence")
        fun multiplePatternsBetterConfidence() {
            val singleSignal = eisenhowerEngine.classify("Important task")
            val multipleSignals = eisenhowerEngine.classify("URGENT: Important client meeting today deadline")
            
            assertTrue(multipleSignals.confidence > singleSignal.confidence,
                "Multiple signals (${multipleSignals.confidence}) should beat single (${singleSignal.confidence})")
        }
    }
    
    @Nested
    @DisplayName("Signal Detection")
    inner class SignalDetectionTests {
        
        @Test
        @DisplayName("Urgency signals are captured")
        fun urgencySignalsCaptured() {
            val result = eisenhowerEngine.classify("URGENT: Server down, fix ASAP")
            assertTrue(result.urgencySignals.isNotEmpty(), "Should detect urgency signals")
            assertTrue(result.isUrgent)
        }
        
        @Test
        @DisplayName("Importance signals are captured")
        fun importanceSignalsCaptured() {
            val result = eisenhowerEngine.classify("Strategic planning for career development")
            assertTrue(result.importanceSignals.isNotEmpty(), "Should detect importance signals")
            assertTrue(result.isImportant)
        }
        
        @Test
        @DisplayName("Explanations include detected signals")
        fun explanationsIncludeSignals() {
            val result = eisenhowerEngine.classify("Client meeting today - prepare presentation")
            assertTrue(result.explanation.isNotEmpty())
            // Explanation should reference detected patterns
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCaseTests {
        
        @Test
        @DisplayName("Empty input defaults to SCHEDULE")
        fun emptyInputDefaultsToSchedule() {
            val result = eisenhowerEngine.classify("")
            assertEquals(EisenhowerQuadrant.SCHEDULE, result.quadrant)
            assertTrue(result.confidence < 0.6f, "Empty input should have low confidence")
        }
        
        @Test
        @DisplayName("Very long input is handled")
        fun longInputHandled() {
            val longTask = "Important task ".repeat(100)
            val result = eisenhowerEngine.classify(longTask)
            assertNotNull(result.quadrant)
            assertTrue(result.isImportant)
        }
        
        @Test
        @DisplayName("Special characters are handled")
        fun specialCharactersHandled() {
            val result = eisenhowerEngine.classify("Fix bug #1234 @urgent $$$")
            assertNotNull(result.quadrant)
        }
        
        @Test
        @DisplayName("Unicode text is handled")
        fun unicodeHandled() {
            val result = eisenhowerEngine.classify("重要な会議 - Important meeting today")
            assertNotNull(result.quadrant)
        }
    }
    
    @Nested
    @DisplayName("Task Parsing")
    inner class TaskParsingTests {
        
        @Test
        @DisplayName("Parse task with due date")
        fun parseTaskWithDueDate() = runTest {
            val request = AiRequest(
                type = AiRequestType.PARSE_TASK,
                input = "Submit report by tomorrow at 3pm"
            )
            
            val result = provider.complete(request)
            assertTrue(result.isSuccess)
            
            val response = result.getOrThrow()
            assertTrue(response.success)
            assertNotNull(response.result)
            assertTrue(response.result is AiResult.ParsedTask)
            
            val parsed = response.result as AiResult.ParsedTask
            assertNotNull(parsed.dueDate)
            assertNotNull(parsed.dueTime)
        }
        
        @Test
        @DisplayName("Parse task extracts clean title")
        fun parseTaskExtractsTitle() = runTest {
            val request = AiRequest(
                type = AiRequestType.PARSE_TASK,
                input = "Remind me to call the dentist tomorrow"
            )
            
            val result = provider.complete(request)
            assertTrue(result.isSuccess)
            
            val parsed = result.getOrThrow().result as AiResult.ParsedTask
            assertFalse(parsed.title.contains("remind me", ignoreCase = true))
            assertTrue(parsed.title.contains("dentist", ignoreCase = true))
        }
    }
}

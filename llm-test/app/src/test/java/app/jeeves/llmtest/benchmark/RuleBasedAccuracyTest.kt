package app.jeeves.llmtest.benchmark

import app.jeeves.llmtest.engine.EisenhowerQuadrant
import app.jeeves.llmtest.engine.RuleBasedClassifier
import org.junit.Assert.*
import org.junit.Test

/**
 * Standalone accuracy test for rule-based classifier.
 * This test can run without an Android device and provides
 * real accuracy metrics against the 20 ground truth test cases.
 */
class RuleBasedAccuracyTest {
    
    /**
     * 20 sample tasks with ground truth Eisenhower quadrant labels.
     */
    private val testCases = listOf(
        // Q1: DO (Urgent + Important) - 5 cases
        TestData(
            id = 1,
            task = "Respond to client email about project deadline tomorrow",
            groundTruth = EisenhowerQuadrant.DO
        ),
        TestData(
            id = 2,
            task = "Server is down, customers can't access the app",
            groundTruth = EisenhowerQuadrant.DO
        ),
        TestData(
            id = 3,
            task = "Complete tax filing before April 15 deadline (today is April 10)",
            groundTruth = EisenhowerQuadrant.DO
        ),
        TestData(
            id = 4,
            task = "Prepare presentation for board meeting in 2 hours",
            groundTruth = EisenhowerQuadrant.DO
        ),
        TestData(
            id = 5,
            task = "Handle urgent support ticket from VIP customer",
            groundTruth = EisenhowerQuadrant.DO
        ),
        
        // Q2: SCHEDULE (Important + Not Urgent) - 5 cases
        TestData(
            id = 6,
            task = "Plan next quarter's marketing strategy",
            groundTruth = EisenhowerQuadrant.SCHEDULE
        ),
        TestData(
            id = 7,
            task = "Read professional development book for career growth",
            groundTruth = EisenhowerQuadrant.SCHEDULE
        ),
        TestData(
            id = 8,
            task = "Schedule annual health checkup",
            groundTruth = EisenhowerQuadrant.SCHEDULE
        ),
        TestData(
            id = 9,
            task = "Research new project management tools for team",
            groundTruth = EisenhowerQuadrant.SCHEDULE
        ),
        TestData(
            id = 10,
            task = "Write documentation for the new feature",
            groundTruth = EisenhowerQuadrant.SCHEDULE
        ),
        
        // Q3: DELEGATE (Urgent + Not Important) - 5 cases
        TestData(
            id = 11,
            task = "Respond to routine HR policy survey by end of day",
            groundTruth = EisenhowerQuadrant.DELEGATE
        ),
        TestData(
            id = 12,
            task = "Order office supplies that are running low",
            groundTruth = EisenhowerQuadrant.DELEGATE
        ),
        TestData(
            id = 13,
            task = "Schedule team's vacation calendar for next month",
            groundTruth = EisenhowerQuadrant.DELEGATE
        ),
        TestData(
            id = 14,
            task = "Answer phone call about lunch meeting location",
            groundTruth = EisenhowerQuadrant.DELEGATE
        ),
        TestData(
            id = 15,
            task = "Compile weekly status report from team updates",
            groundTruth = EisenhowerQuadrant.DELEGATE
        ),
        
        // Q4: ELIMINATE (Not Urgent + Not Important) - 5 cases
        TestData(
            id = 16,
            task = "Browse social media during lunch break",
            groundTruth = EisenhowerQuadrant.ELIMINATE
        ),
        TestData(
            id = 17,
            task = "Reorganize email folders for the third time this month",
            groundTruth = EisenhowerQuadrant.ELIMINATE
        ),
        TestData(
            id = 18,
            task = "Watch YouTube video about productivity hacks",
            groundTruth = EisenhowerQuadrant.ELIMINATE
        ),
        TestData(
            id = 19,
            task = "Attend optional company picnic planning meeting",
            groundTruth = EisenhowerQuadrant.ELIMINATE
        ),
        TestData(
            id = 20,
            task = "Clean up old files on desktop (no deadline)",
            groundTruth = EisenhowerQuadrant.ELIMINATE
        )
    )
    
    data class TestData(
        val id: Int,
        val task: String,
        val groundTruth: EisenhowerQuadrant
    )
    
    @Test
    fun `calculate rule-based accuracy against ground truth`() {
        var correct = 0
        val quadrantStats = mutableMapOf<EisenhowerQuadrant, Pair<Int, Int>>() // (correct, total)
        
        EisenhowerQuadrant.values().forEach { q ->
            quadrantStats[q] = Pair(0, 0)
        }
        
        println("\n=== Rule-Based Classifier Accuracy Test ===\n")
        
        for (testCase in testCases) {
            val result = RuleBasedClassifier.classify(testCase.task)
            val isCorrect = result.quadrant == testCase.groundTruth
            
            if (isCorrect) correct++
            
            val (prevCorrect, prevTotal) = quadrantStats[testCase.groundTruth]!!
            quadrantStats[testCase.groundTruth] = Pair(
                prevCorrect + if (isCorrect) 1 else 0,
                prevTotal + 1
            )
            
            val status = if (isCorrect) "✓" else "✗"
            println("$status [${testCase.id}] ${testCase.task.take(50)}...")
            println("   Expected: ${testCase.groundTruth}, Got: ${result.quadrant} (${(result.confidence * 100).toInt()}%)")
        }
        
        val accuracy = correct.toDouble() / testCases.size * 100
        
        println("\n=== Summary ===")
        println("Overall Accuracy: $correct/${testCases.size} = ${accuracy.toInt()}%\n")
        
        println("Per-Quadrant Accuracy:")
        for ((quadrant, stats) in quadrantStats) {
            val (c, t) = stats
            val pct = if (t > 0) (c.toDouble() / t * 100).toInt() else 0
            println("  $quadrant: $c/$t = $pct%")
        }
        
        // Assert minimum accuracy threshold
        assertTrue(
            "Rule-based classifier should achieve at least 60% accuracy, got ${accuracy.toInt()}%",
            accuracy >= 60.0
        )
    }
    
    @Test
    fun `print classification details for all test cases`() {
        println("\n=== Detailed Classification Analysis ===\n")
        
        for (testCase in testCases) {
            val result = RuleBasedClassifier.classify(testCase.task)
            println("Task: ${testCase.task}")
            println("  Ground Truth: ${testCase.groundTruth}")
            println("  Prediction:   ${result.quadrant}")
            println("  Confidence:   ${(result.confidence * 100).toInt()}%")
            println("  Reasoning:    ${result.reasoning}")
            println("  Match:        ${if (result.quadrant == testCase.groundTruth) "✓ CORRECT" else "✗ WRONG"}")
            println()
        }
    }
}

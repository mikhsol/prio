package app.jeeves.llmtest.engine

import org.junit.Test

/**
 * Accuracy computation test that prints detailed results.
 */
class AccuracyComputationTest {
    
    private val testCases = listOf(
        // Q1: DO (Urgent + Important) - 5 cases
        Triple(1, "Respond to client email about project deadline tomorrow", EisenhowerQuadrant.DO),
        Triple(2, "Server is down, customers can't access the app", EisenhowerQuadrant.DO),
        Triple(3, "Complete tax filing before April 15 deadline (today is April 10)", EisenhowerQuadrant.DO),
        Triple(4, "Prepare presentation for board meeting in 2 hours", EisenhowerQuadrant.DO),
        Triple(5, "Handle urgent support ticket from VIP customer", EisenhowerQuadrant.DO),
        
        // Q2: SCHEDULE (Important + Not Urgent) - 5 cases
        Triple(6, "Plan next quarter's marketing strategy", EisenhowerQuadrant.SCHEDULE),
        Triple(7, "Read professional development book for career growth", EisenhowerQuadrant.SCHEDULE),
        Triple(8, "Schedule annual health checkup", EisenhowerQuadrant.SCHEDULE),
        Triple(9, "Research new project management tools for team", EisenhowerQuadrant.SCHEDULE),
        Triple(10, "Write documentation for the new feature", EisenhowerQuadrant.SCHEDULE),
        
        // Q3: DELEGATE (Urgent + Not Important) - 5 cases
        Triple(11, "Respond to routine HR policy survey by end of day", EisenhowerQuadrant.DELEGATE),
        Triple(12, "Order office supplies that are running low", EisenhowerQuadrant.DELEGATE),
        Triple(13, "Schedule team's vacation calendar for next month", EisenhowerQuadrant.DELEGATE),
        Triple(14, "Answer phone call about lunch meeting location", EisenhowerQuadrant.DELEGATE),
        Triple(15, "Compile weekly status report from team updates", EisenhowerQuadrant.DELEGATE),
        
        // Q4: ELIMINATE (Not Urgent + Not Important) - 5 cases
        Triple(16, "Browse social media during lunch break", EisenhowerQuadrant.ELIMINATE),
        Triple(17, "Reorganize email folders for the third time this month", EisenhowerQuadrant.ELIMINATE),
        Triple(18, "Watch YouTube video about productivity hacks", EisenhowerQuadrant.ELIMINATE),
        Triple(19, "Attend optional company picnic planning meeting", EisenhowerQuadrant.ELIMINATE),
        Triple(20, "Clean up old files on desktop (no deadline)", EisenhowerQuadrant.ELIMINATE)
    )
    
    @Test
    fun computeAccuracy() {
        var correct = 0
        val quadrantResults = mutableMapOf<EisenhowerQuadrant, MutableList<Boolean>>()
        EisenhowerQuadrant.values().forEach { quadrantResults[it] = mutableListOf() }
        
        val sb = StringBuilder()
        sb.appendLine("\n" + "=".repeat(60))
        sb.appendLine("RULE-BASED CLASSIFIER ACCURACY TEST")
        sb.appendLine("=".repeat(60))
        
        for ((id, task, expected) in testCases) {
            val result = RuleBasedClassifier.classify(task)
            val isCorrect = result.quadrant == expected
            if (isCorrect) correct++
            quadrantResults[expected]?.add(isCorrect)
            
            val marker = if (isCorrect) "✓" else "✗"
            sb.appendLine("$marker [$id] ${task.take(50)}...")
            if (!isCorrect) {
                sb.appendLine("      Expected: $expected, Got: ${result.quadrant}")
            }
        }
        
        val accuracy = correct * 100.0 / testCases.size
        
        sb.appendLine("\n" + "-".repeat(60))
        sb.appendLine("SUMMARY")
        sb.appendLine("-".repeat(60))
        sb.appendLine("Overall Accuracy: $correct/20 = ${"%.1f".format(accuracy)}%")
        sb.appendLine()
        sb.appendLine("Per-Quadrant Accuracy:")
        for ((q, results) in quadrantResults) {
            val c = results.count { it }
            val t = results.size
            val pct = if (t > 0) c * 100.0 / t else 0.0
            sb.appendLine("  ${q.name.padEnd(10)}: $c/$t = ${"%.0f".format(pct)}%")
        }
        sb.appendLine("=".repeat(60))
        
        // Print to stdout so it shows in test output
        System.out.println(sb.toString())
        
        // Also write to a file for reference
        java.io.File("accuracy_results.txt").writeText(sb.toString())
    }
}

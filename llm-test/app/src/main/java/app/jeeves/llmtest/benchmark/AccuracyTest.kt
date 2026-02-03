package app.jeeves.llmtest.benchmark

import app.jeeves.llmtest.engine.ClassificationResult
import app.jeeves.llmtest.engine.EisenhowerClassifier
import app.jeeves.llmtest.engine.EisenhowerQuadrant
import app.jeeves.llmtest.engine.RuleBasedClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Accuracy test runner for Eisenhower classification.
 * 
 * Task 0.2.3: Test task categorization accuracy with 20 sample prompts
 * 
 * Tests both LLM-based and rule-based classifiers against ground truth labels.
 */
class AccuracyTest(private val classifier: EisenhowerClassifier) {
    
    companion object {
        /**
         * 20 sample tasks with ground truth Eisenhower quadrant labels.
         * Curated to cover all quadrants and common edge cases.
         */
        val TEST_CASES = listOf(
            // Q1: DO (Urgent + Important) - 5 cases
            TestCase(
                id = 1,
                task = "Respond to client email about project deadline tomorrow",
                groundTruth = EisenhowerQuadrant.DO,
                rationale = "Client deadline is urgent and important"
            ),
            TestCase(
                id = 2,
                task = "Server is down, customers can't access the app",
                groundTruth = EisenhowerQuadrant.DO,
                rationale = "Production outage is critical crisis"
            ),
            TestCase(
                id = 3,
                task = "Complete tax filing before April 15 deadline (today is April 10)",
                groundTruth = EisenhowerQuadrant.DO,
                rationale = "Legal deadline approaching, cannot be delayed"
            ),
            TestCase(
                id = 4,
                task = "Prepare presentation for board meeting in 2 hours",
                groundTruth = EisenhowerQuadrant.DO,
                rationale = "Imminent important meeting"
            ),
            TestCase(
                id = 5,
                task = "Handle urgent support ticket from VIP customer",
                groundTruth = EisenhowerQuadrant.DO,
                rationale = "VIP customer issue is both urgent and important"
            ),
            
            // Q2: SCHEDULE (Important + Not Urgent) - 5 cases
            TestCase(
                id = 6,
                task = "Plan next quarter's marketing strategy",
                groundTruth = EisenhowerQuadrant.SCHEDULE,
                rationale = "Strategic planning is important but not time-bound"
            ),
            TestCase(
                id = 7,
                task = "Read professional development book for career growth",
                groundTruth = EisenhowerQuadrant.SCHEDULE,
                rationale = "Personal development without deadline"
            ),
            TestCase(
                id = 8,
                task = "Schedule annual health checkup",
                groundTruth = EisenhowerQuadrant.SCHEDULE,
                rationale = "Health is important, no immediate urgency"
            ),
            TestCase(
                id = 9,
                task = "Research new project management tools for team",
                groundTruth = EisenhowerQuadrant.SCHEDULE,
                rationale = "Improvement initiative without deadline"
            ),
            TestCase(
                id = 10,
                task = "Write documentation for the new feature",
                groundTruth = EisenhowerQuadrant.SCHEDULE,
                rationale = "Important for quality, no stated deadline"
            ),
            
            // Q3: DELEGATE (Urgent + Not Important) - 5 cases
            TestCase(
                id = 11,
                task = "Respond to routine HR policy survey by end of day",
                groundTruth = EisenhowerQuadrant.DELEGATE,
                rationale = "Deadline exists but low personal importance"
            ),
            TestCase(
                id = 12,
                task = "Order office supplies that are running low",
                groundTruth = EisenhowerQuadrant.DELEGATE,
                rationale = "Administrative task suitable for delegation"
            ),
            TestCase(
                id = 13,
                task = "Schedule team's vacation calendar for next month",
                groundTruth = EisenhowerQuadrant.DELEGATE,
                rationale = "Coordination task can be handled by admin"
            ),
            TestCase(
                id = 14,
                task = "Answer phone call about lunch meeting location",
                groundTruth = EisenhowerQuadrant.DELEGATE,
                rationale = "Interruption that doesn't require senior attention"
            ),
            TestCase(
                id = 15,
                task = "Compile weekly status report from team updates",
                groundTruth = EisenhowerQuadrant.DELEGATE,
                rationale = "Routine aggregation task"
            ),
            
            // Q4: ELIMINATE (Not Urgent + Not Important) - 5 cases
            TestCase(
                id = 16,
                task = "Browse social media during lunch break",
                groundTruth = EisenhowerQuadrant.ELIMINATE,
                rationale = "Time-wasting activity with no value"
            ),
            TestCase(
                id = 17,
                task = "Reorganize email folders for the third time this month",
                groundTruth = EisenhowerQuadrant.ELIMINATE,
                rationale = "Busy work, diminishing returns"
            ),
            TestCase(
                id = 18,
                task = "Watch YouTube video about productivity hacks",
                groundTruth = EisenhowerQuadrant.ELIMINATE,
                rationale = "Procrastination disguised as learning"
            ),
            TestCase(
                id = 19,
                task = "Attend optional company picnic planning meeting",
                groundTruth = EisenhowerQuadrant.ELIMINATE,
                rationale = "Optional social activity"
            ),
            TestCase(
                id = 20,
                task = "Clean up old files on desktop (no deadline)",
                groundTruth = EisenhowerQuadrant.ELIMINATE,
                rationale = "Nice-to-have with no impact"
            )
        )
    }
    
    /**
     * Run accuracy test using LLM classifier.
     */
    suspend fun runLlmTest(): AccuracyReport = withContext(Dispatchers.Default) {
        val results = mutableListOf<TestResult>()
        
        for (testCase in TEST_CASES) {
            val classification = classifier.classify(testCase.task)
            val isCorrect = classification.quadrant == testCase.groundTruth
            
            results.add(
                TestResult(
                    testCase = testCase,
                    predicted = classification.quadrant,
                    confidence = classification.confidence,
                    isCorrect = isCorrect,
                    source = classification.source.name
                )
            )
        }
        
        generateReport(results, "LLM")
    }
    
    /**
     * Run accuracy test using rule-based classifier only.
     */
    fun runRuleBasedTest(): AccuracyReport {
        val results = mutableListOf<TestResult>()
        
        for (testCase in TEST_CASES) {
            val classification = RuleBasedClassifier.classify(testCase.task)
            val isCorrect = classification.quadrant == testCase.groundTruth
            
            results.add(
                TestResult(
                    testCase = testCase,
                    predicted = classification.quadrant,
                    confidence = classification.confidence,
                    isCorrect = isCorrect,
                    source = "RULE_BASED"
                )
            )
        }
        
        return generateReport(results, "Rule-Based")
    }
    
    private fun generateReport(results: List<TestResult>, classifierName: String): AccuracyReport {
        val correct = results.count { it.isCorrect }
        val total = results.size
        val accuracy = correct.toFloat() / total
        
        // Per-quadrant metrics
        val quadrantMetrics = EisenhowerQuadrant.entries.map { quadrant ->
            val relevant = results.filter { it.testCase.groundTruth == quadrant }
            val truePositives = relevant.count { it.isCorrect }
            val predicted = results.filter { it.predicted == quadrant }
            val falsePositives = predicted.count { !it.isCorrect }
            
            val precision = if (predicted.isNotEmpty()) {
                truePositives.toFloat() / predicted.size
            } else 0f
            
            val recall = if (relevant.isNotEmpty()) {
                truePositives.toFloat() / relevant.size
            } else 0f
            
            val f1 = if (precision + recall > 0) {
                2 * precision * recall / (precision + recall)
            } else 0f
            
            QuadrantMetrics(
                quadrant = quadrant,
                total = relevant.size,
                correct = truePositives,
                precision = precision,
                recall = recall,
                f1Score = f1
            )
        }
        
        // Identify errors
        val errors = results.filter { !it.isCorrect }
        
        return AccuracyReport(
            classifierName = classifierName,
            totalTests = total,
            correctPredictions = correct,
            accuracy = accuracy,
            quadrantMetrics = quadrantMetrics,
            errors = errors,
            results = results
        )
    }
}

data class TestCase(
    val id: Int,
    val task: String,
    val groundTruth: EisenhowerQuadrant,
    val rationale: String
)

data class TestResult(
    val testCase: TestCase,
    val predicted: EisenhowerQuadrant,
    val confidence: Float,
    val isCorrect: Boolean,
    val source: String
)

data class QuadrantMetrics(
    val quadrant: EisenhowerQuadrant,
    val total: Int,
    val correct: Int,
    val precision: Float,
    val recall: Float,
    val f1Score: Float
)

data class AccuracyReport(
    val classifierName: String,
    val totalTests: Int,
    val correctPredictions: Int,
    val accuracy: Float,
    val quadrantMetrics: List<QuadrantMetrics>,
    val errors: List<TestResult>,
    val results: List<TestResult>
) {
    fun toMarkdown(): String = buildString {
        appendLine("# Eisenhower Classification Accuracy Report")
        appendLine()
        appendLine("**Classifier**: $classifierName")
        appendLine("**Total Tests**: $totalTests")
        appendLine("**Accuracy**: ${String.format("%.1f", accuracy * 100)}%")
        appendLine()
        
        appendLine("## Per-Quadrant Performance")
        appendLine("| Quadrant | Tests | Correct | Precision | Recall | F1 |")
        appendLine("|----------|-------|---------|-----------|--------|-----|")
        for (qm in quadrantMetrics) {
            appendLine("| ${qm.quadrant} | ${qm.total} | ${qm.correct} | ${String.format("%.2f", qm.precision)} | ${String.format("%.2f", qm.recall)} | ${String.format("%.2f", qm.f1Score)} |")
        }
        appendLine()
        
        if (errors.isNotEmpty()) {
            appendLine("## Errors (${errors.size})")
            appendLine("| # | Task | Expected | Predicted | Confidence |")
            appendLine("|---|------|----------|-----------|------------|")
            for (err in errors) {
                val truncatedTask = if (err.testCase.task.length > 40) {
                    err.testCase.task.take(37) + "..."
                } else {
                    err.testCase.task
                }
                appendLine("| ${err.testCase.id} | $truncatedTask | ${err.testCase.groundTruth} | ${err.predicted} | ${String.format("%.2f", err.confidence)} |")
            }
            appendLine()
        }
        
        appendLine("## All Results")
        appendLine("| # | Task | Expected | Predicted | Correct |")
        appendLine("|---|------|----------|-----------|---------|")
        for (result in results) {
            val truncatedTask = if (result.testCase.task.length > 40) {
                result.testCase.task.take(37) + "..."
            } else {
                result.testCase.task
            }
            val status = if (result.isCorrect) "✅" else "❌"
            appendLine("| ${result.testCase.id} | $truncatedTask | ${result.testCase.groundTruth} | ${result.predicted} | $status |")
        }
    }
    
    val passesTarget: Boolean
        get() = accuracy >= 0.80f
}

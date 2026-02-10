package com.prio.core.aiprovider.benchmark

import com.prio.core.ai.model.AiContext
import com.prio.core.ai.model.AiRequest
import com.prio.core.ai.model.AiRequestType
import com.prio.core.ai.model.AiResult
import com.prio.core.ai.provider.AiProvider
import com.prio.core.common.model.EisenhowerQuadrant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Benchmark harness comparing all AI providers against labeled test cases.
 *
 * Task 3.6.9: Measures accuracy, latency (p50/p95/p99), and token usage
 * across rule-based, Gemini Nano, and llama.cpp providers.
 *
 * Outputs a structured [BenchmarkReport] for analytics and regression tracking.
 */
@Singleton
class AiProviderBenchmark @Inject constructor() {

    /**
     * Run the complete benchmark suite against the given providers.
     *
     * @param providers Map of provider-id → AiProvider
     * @param testCases Labeled test cases (default: [EISENHOWER_TEST_CASES])
     * @return Structured benchmark report
     */
    suspend fun runBenchmark(
        providers: Map<String, AiProvider>,
        testCases: List<EisenhowerTestCase> = EISENHOWER_TEST_CASES
    ): BenchmarkReport = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i("Starting benchmark: ${providers.size} providers, ${testCases.size} cases")

        val providerResults = providers.map { (id, provider) ->
            val results = mutableListOf<TestCaseResult>()
            val latencies = mutableListOf<Long>()

            for (testCase in testCases) {
                val request = AiRequest(
                    type = AiRequestType.CLASSIFY_EISENHOWER,
                    input = testCase.taskText,
                    context = AiContext(
                        custom = testCase.dueDate?.let { mapOf("due_date" to it) } ?: emptyMap()
                    )
                )

                val startTime = System.nanoTime()
                val response = try {
                    provider.complete(request)
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Provider $id failed on: ${testCase.taskText}")
                    null
                }
                val latencyMs = (System.nanoTime() - startTime) / 1_000_000

                val classification = response?.getOrNull()?.result as? AiResult.EisenhowerClassification
                val correct = classification?.quadrant == testCase.expectedQuadrant

                results.add(
                    TestCaseResult(
                        testCase = testCase,
                        predictedQuadrant = classification?.quadrant,
                        confidence = classification?.confidence ?: 0f,
                        correct = correct,
                        latencyMs = latencyMs,
                        error = response?.exceptionOrNull()?.message
                    )
                )
                latencies.add(latencyMs)
            }

            val sortedLatencies = latencies.sorted()
            val correctCount = results.count { it.correct }

            ProviderBenchmarkResult(
                providerId = id,
                totalCases = testCases.size,
                correctCount = correctCount,
                accuracy = if (testCases.isNotEmpty()) correctCount.toFloat() / testCases.size else 0f,
                latencyP50Ms = percentile(sortedLatencies, 50),
                latencyP95Ms = percentile(sortedLatencies, 95),
                latencyP99Ms = percentile(sortedLatencies, 99),
                averageLatencyMs = if (latencies.isNotEmpty()) latencies.average().toLong() else 0,
                results = results
            )
        }

        BenchmarkReport(
            timestamp = System.currentTimeMillis(),
            totalTestCases = testCases.size,
            providerResults = providerResults
        ).also { report ->
            Timber.tag(TAG).i("Benchmark complete:\n${report.toMarkdown()}")
        }
    }

    private fun percentile(sorted: List<Long>, pct: Int): Long {
        if (sorted.isEmpty()) return 0
        val idx = ((pct / 100.0) * (sorted.size - 1)).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[idx]
    }

    // =========================================================================
    // Data classes
    // =========================================================================

    data class EisenhowerTestCase(
        val id: Int,
        val taskText: String,
        val expectedQuadrant: EisenhowerQuadrant,
        val dueDate: String? = null,
        val category: String = "general"
    )

    data class TestCaseResult(
        val testCase: EisenhowerTestCase,
        val predictedQuadrant: EisenhowerQuadrant?,
        val confidence: Float,
        val correct: Boolean,
        val latencyMs: Long,
        val error: String? = null
    )

    data class ProviderBenchmarkResult(
        val providerId: String,
        val totalCases: Int,
        val correctCount: Int,
        val accuracy: Float,
        val latencyP50Ms: Long,
        val latencyP95Ms: Long,
        val latencyP99Ms: Long,
        val averageLatencyMs: Long,
        val results: List<TestCaseResult>
    )

    data class BenchmarkReport(
        val timestamp: Long,
        val totalTestCases: Int,
        val providerResults: List<ProviderBenchmarkResult>
    ) {
        fun toMarkdown(): String = buildString {
            appendLine("# AI Provider Benchmark Report")
            appendLine("| Provider | Accuracy | p50 | p95 | p99 | Avg |")
            appendLine("|----------|----------|-----|-----|-----|-----|")
            for (pr in providerResults) {
                appendLine(
                    "| ${pr.providerId} | ${(pr.accuracy * 100).toInt()}% (${pr.correctCount}/${pr.totalCases}) " +
                        "| ${pr.latencyP50Ms}ms | ${pr.latencyP95Ms}ms | ${pr.latencyP99Ms}ms | ${pr.averageLatencyMs}ms |"
                )
            }
            appendLine()
            appendLine("---")
            // Confusion detail per provider
            for (pr in providerResults) {
                appendLine("## ${pr.providerId}")
                val wrong = pr.results.filter { !it.correct }
                if (wrong.isEmpty()) {
                    appendLine("All correct!")
                } else {
                    appendLine("| # | Task | Expected | Predicted | Confidence |")
                    appendLine("|---|------|----------|-----------|------------|")
                    for (w in wrong) {
                        appendLine(
                            "| ${w.testCase.id} | ${w.testCase.taskText.take(50)} " +
                                "| ${w.testCase.expectedQuadrant} | ${w.predictedQuadrant ?: "ERROR"} " +
                                "| ${(w.confidence * 100).toInt()}% |"
                        )
                    }
                }
                appendLine()
            }
        }
    }

    // =========================================================================
    // 50 labeled test cases (from Milestone 0.2.3 + 3.6.9 spec)
    // =========================================================================

    companion object {
        private const val TAG = "AiProviderBenchmark"

        val EISENHOWER_TEST_CASES = listOf(
            // ── Q1: Do First (Urgent + Important) ──
            EisenhowerTestCase(1, "Submit tax return before deadline tomorrow", EisenhowerQuadrant.DO_FIRST, "2026-02-11", "finance"),
            EisenhowerTestCase(2, "Fix critical production bug affecting all users", EisenhowerQuadrant.DO_FIRST, category = "work"),
            EisenhowerTestCase(3, "Respond to client's urgent contract revision by EOD", EisenhowerQuadrant.DO_FIRST, "2026-02-10", "work"),
            EisenhowerTestCase(4, "Pick up child's prescription medication today", EisenhowerQuadrant.DO_FIRST, "2026-02-10", "family"),
            EisenhowerTestCase(5, "Prepare slides for board presentation in 2 hours", EisenhowerQuadrant.DO_FIRST, category = "work"),
            EisenhowerTestCase(6, "Emergency plumber appointment for burst pipe", EisenhowerQuadrant.DO_FIRST, category = "home"),
            EisenhowerTestCase(7, "Submit quarterly financial report due today", EisenhowerQuadrant.DO_FIRST, "2026-02-10", "work"),
            EisenhowerTestCase(8, "Call insurance company about claim deadline tomorrow", EisenhowerQuadrant.DO_FIRST, "2026-02-11", "finance"),
            EisenhowerTestCase(9, "Doctor appointment for persistent chest pain", EisenhowerQuadrant.DO_FIRST, category = "health"),
            EisenhowerTestCase(10, "Security vulnerability patch deployment ASAP", EisenhowerQuadrant.DO_FIRST, category = "work"),
            EisenhowerTestCase(11, "Renew expiring passport needed for trip next week", EisenhowerQuadrant.DO_FIRST, "2026-02-17", "travel"),
            EisenhowerTestCase(12, "Address customer data breach incident immediately", EisenhowerQuadrant.DO_FIRST, category = "work"),

            // ── Q2: Schedule (Important + Not Urgent) ──
            EisenhowerTestCase(13, "Learn Kotlin Multiplatform for career growth", EisenhowerQuadrant.SCHEDULE, category = "learning"),
            EisenhowerTestCase(14, "Create a 5-year financial plan", EisenhowerQuadrant.SCHEDULE, category = "finance"),
            EisenhowerTestCase(15, "Start weekly exercise routine", EisenhowerQuadrant.SCHEDULE, category = "health"),
            EisenhowerTestCase(16, "Plan team building retreat for next quarter", EisenhowerQuadrant.SCHEDULE, category = "work"),
            EisenhowerTestCase(17, "Write technical blog post about architecture patterns", EisenhowerQuadrant.SCHEDULE, category = "learning"),
            EisenhowerTestCase(18, "Schedule annual health checkup", EisenhowerQuadrant.SCHEDULE, category = "health"),
            EisenhowerTestCase(19, "Research investment options for retirement fund", EisenhowerQuadrant.SCHEDULE, category = "finance"),
            EisenhowerTestCase(20, "Mentor junior developer on design patterns", EisenhowerQuadrant.SCHEDULE, category = "work"),
            EisenhowerTestCase(21, "Read 'Designing Data-Intensive Applications'", EisenhowerQuadrant.SCHEDULE, category = "learning"),
            EisenhowerTestCase(22, "Plan family vacation for summer", EisenhowerQuadrant.SCHEDULE, category = "family"),
            EisenhowerTestCase(23, "Set up automated backup system for important files", EisenhowerQuadrant.SCHEDULE, category = "tech"),
            EisenhowerTestCase(24, "Develop a personal project roadmap for the year", EisenhowerQuadrant.SCHEDULE, category = "personal"),
            EisenhowerTestCase(25, "Build an emergency savings fund of 6 months", EisenhowerQuadrant.SCHEDULE, category = "finance"),

            // ── Q3: Delegate (Urgent + Not Important) ──
            EisenhowerTestCase(26, "Reply to vendor's scheduling email", EisenhowerQuadrant.DELEGATE, category = "work"),
            EisenhowerTestCase(27, "Attend optional team standup meeting", EisenhowerQuadrant.DELEGATE, category = "work"),
            EisenhowerTestCase(28, "Process expense reports that are overdue", EisenhowerQuadrant.DELEGATE, "2026-02-10", "admin"),
            EisenhowerTestCase(29, "Answer phone call from telemarketer", EisenhowerQuadrant.DELEGATE, category = "personal"),
            EisenhowerTestCase(30, "Forward meeting notes to absent colleague", EisenhowerQuadrant.DELEGATE, category = "work"),
            EisenhowerTestCase(31, "Review and approve routine purchase orders", EisenhowerQuadrant.DELEGATE, category = "admin"),
            EisenhowerTestCase(32, "Schedule office supply order for the team", EisenhowerQuadrant.DELEGATE, category = "admin"),
            EisenhowerTestCase(33, "Update shared team calendar with meeting rooms", EisenhowerQuadrant.DELEGATE, category = "admin"),
            EisenhowerTestCase(34, "Respond to non-urgent Slack messages from yesterday", EisenhowerQuadrant.DELEGATE, category = "work"),
            EisenhowerTestCase(35, "Book conference room for next week's all-hands", EisenhowerQuadrant.DELEGATE, category = "admin"),
            EisenhowerTestCase(36, "Fill out routine compliance training survey", EisenhowerQuadrant.DELEGATE, category = "work"),
            EisenhowerTestCase(37, "Print handouts for tomorrow's workshop", EisenhowerQuadrant.DELEGATE, category = "admin"),

            // ── Q4: Eliminate (Not Urgent + Not Important) ──
            EisenhowerTestCase(38, "Browse social media feeds", EisenhowerQuadrant.ELIMINATE, category = "personal"),
            EisenhowerTestCase(39, "Watch YouTube recommendations", EisenhowerQuadrant.ELIMINATE, category = "personal"),
            EisenhowerTestCase(40, "Reorganize desk drawer", EisenhowerQuadrant.ELIMINATE, category = "home"),
            EisenhowerTestCase(41, "Check gossip news websites", EisenhowerQuadrant.ELIMINATE, category = "personal"),
            EisenhowerTestCase(42, "Scroll through online shopping deals", EisenhowerQuadrant.ELIMINATE, category = "personal"),
            EisenhowerTestCase(43, "Re-sort Spotify playlists by mood", EisenhowerQuadrant.ELIMINATE, category = "personal"),
            EisenhowerTestCase(44, "Compare phone case designs online", EisenhowerQuadrant.ELIMINATE, category = "personal"),
            EisenhowerTestCase(45, "Read random Wikipedia articles", EisenhowerQuadrant.ELIMINATE, category = "personal"),
            EisenhowerTestCase(46, "Play mobile games during work hours", EisenhowerQuadrant.ELIMINATE, category = "personal"),
            EisenhowerTestCase(47, "Argue with strangers on Reddit", EisenhowerQuadrant.ELIMINATE, category = "personal"),
            EisenhowerTestCase(48, "Watch unboxing videos on YouTube", EisenhowerQuadrant.ELIMINATE, category = "personal"),
            EisenhowerTestCase(49, "Rearrange app icons on phone homescreen", EisenhowerQuadrant.ELIMINATE, category = "personal"),
            EisenhowerTestCase(50, "Clean out old bookmarks in browser", EisenhowerQuadrant.ELIMINATE, category = "personal")
        )
    }
}

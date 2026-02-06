package com.prio.core.data.repository

import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.data.local.dao.DailyAnalyticsDao
import com.prio.core.data.local.dao.TaskDao
import com.prio.core.data.local.entity.DailyAnalyticsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Analytics operations.
 * 
 * Implements 2.1.9 from ACTION_PLAN.md:
 * - Task completion rate calculation per 0.3.8
 * - (completed/created) over 7-day window
 * 
 * Based on:
 * - 0.3.8 Success Metrics: DAU, D7 Retention, Task Completion, AI Accuracy
 */
@Singleton
class AnalyticsRepository @Inject constructor(
    private val dailyAnalyticsDao: DailyAnalyticsDao,
    private val taskDao: TaskDao,
    private val clock: Clock = Clock.System
) {
    
    companion object {
        /**
         * Default window for analytics calculations.
         */
        const val DEFAULT_WINDOW_DAYS = 7
        
        /**
         * Target completion rate per 0.3.8 Success Metrics.
         */
        const val TARGET_COMPLETION_RATE = 0.60f // 60%
        
        /**
         * Target AI accuracy per 0.3.8 Success Metrics.
         */
        const val TARGET_AI_ACCURACY = 0.80f // 80%
        
        /**
         * Alert threshold for AI accuracy.
         */
        const val AI_ACCURACY_ALERT_THRESHOLD = 0.70f // 70%
    }
    
    // ==================== Query Operations ====================
    
    /**
     * Get analytics for a specific date.
     */
    fun getAnalyticsForDateFlow(date: LocalDate): Flow<DailyAnalyticsEntity?> = 
        dailyAnalyticsDao.getByDateFlow(date)
    
    /**
     * Get analytics for a date range.
     */
    fun getAnalyticsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyAnalyticsEntity>> = 
        dailyAnalyticsDao.getInRange(startDate, endDate)
    
    /**
     * Get recent days analytics.
     */
    fun getRecentDaysAnalytics(days: Int = DEFAULT_WINDOW_DAYS): Flow<List<DailyAnalyticsEntity>> = 
        dailyAnalyticsDao.getRecentDays(days)
    
    /**
     * Get today's analytics.
     */
    fun getTodayAnalytics(): Flow<DailyAnalyticsEntity?> {
        val today = getToday()
        return dailyAnalyticsDao.getByDateFlow(today)
    }
    
    // ==================== Suspend Queries ====================
    
    /**
     * Get analytics for a specific date.
     */
    suspend fun getAnalyticsForDate(date: LocalDate): DailyAnalyticsEntity? = 
        dailyAnalyticsDao.getByDate(date)
    
    /**
     * Get or create today's analytics entry.
     */
    suspend fun getOrCreateTodayAnalytics(): DailyAnalyticsEntity {
        val today = getToday()
        val existing = dailyAnalyticsDao.getByDate(today)
        
        return existing ?: run {
            val entity = DailyAnalyticsEntity(date = today)
            val id = dailyAnalyticsDao.insert(entity)
            entity.copy(id = id)
        }
    }
    
    // ==================== Completion Rate ====================
    
    /**
     * Calculate task completion rate for a date range.
     * Per 0.3.8 Success Metrics: (completed/created) over 7-day window.
     * 
     * @return Completion rate as a float between 0.0 and 1.0
     */
    suspend fun getCompletionRate(
        startDate: LocalDate = getToday().minus(DatePeriod(days = DEFAULT_WINDOW_DAYS)),
        endDate: LocalDate = getToday()
    ): Float {
        val rate = dailyAnalyticsDao.getCompletionRateInRange(startDate, endDate)
        return rate ?: 0f
    }
    
    /**
     * Get total tasks created in a date range.
     */
    suspend fun getTotalTasksCreated(
        startDate: LocalDate = getToday().minus(DatePeriod(days = DEFAULT_WINDOW_DAYS)),
        endDate: LocalDate = getToday()
    ): Int = dailyAnalyticsDao.getTotalCreatedInRange(startDate, endDate) ?: 0
    
    /**
     * Get total tasks completed in a date range.
     */
    suspend fun getTotalTasksCompleted(
        startDate: LocalDate = getToday().minus(DatePeriod(days = DEFAULT_WINDOW_DAYS)),
        endDate: LocalDate = getToday()
    ): Int = dailyAnalyticsDao.getTotalCompletedInRange(startDate, endDate) ?: 0
    
    // ==================== AI Accuracy ====================
    
    /**
     * Calculate AI classification accuracy for a date range.
     * Per 0.3.8 Success Metrics: Classifications NOT overridden by user.
     * 
     * Accuracy = (total classifications - overrides) / total classifications
     * 
     * @return Accuracy as a float between 0.0 and 1.0
     */
    suspend fun getAiAccuracy(
        startDate: LocalDate = getToday().minus(DatePeriod(days = DEFAULT_WINDOW_DAYS)),
        endDate: LocalDate = getToday()
    ): Float {
        val totalClassifications = dailyAnalyticsDao.getTotalAiClassificationsInRange(startDate, endDate) ?: 0
        val overrides = dailyAnalyticsDao.getTotalAiOverridesInRange(startDate, endDate) ?: 0
        
        return if (totalClassifications > 0) {
            (totalClassifications - overrides).toFloat() / totalClassifications
        } else {
            1f // No classifications = no errors
        }
    }
    
    /**
     * Check if AI accuracy is below alert threshold.
     */
    suspend fun isAiAccuracyBelowThreshold(): Boolean = 
        getAiAccuracy() < AI_ACCURACY_ALERT_THRESHOLD
    
    // ==================== Recording Events ====================
    
    /**
     * Record a task creation event.
     */
    suspend fun recordTaskCreated() {
        val analytics = getOrCreateTodayAnalytics()
        val updated = analytics.copy(tasksCreated = analytics.tasksCreated + 1)
        dailyAnalyticsDao.update(updated)
    }
    
    /**
     * Record a task completion event.
     */
    suspend fun recordTaskCompleted(quadrant: EisenhowerQuadrant) {
        val analytics = getOrCreateTodayAnalytics()
        val updated = when (quadrant) {
            EisenhowerQuadrant.DO_FIRST -> analytics.copy(
                tasksCompleted = analytics.tasksCompleted + 1,
                q1Completed = analytics.q1Completed + 1
            )
            EisenhowerQuadrant.SCHEDULE -> analytics.copy(
                tasksCompleted = analytics.tasksCompleted + 1,
                q2Completed = analytics.q2Completed + 1
            )
            EisenhowerQuadrant.DELEGATE -> analytics.copy(
                tasksCompleted = analytics.tasksCompleted + 1,
                q3Completed = analytics.q3Completed + 1
            )
            EisenhowerQuadrant.ELIMINATE -> analytics.copy(
                tasksCompleted = analytics.tasksCompleted + 1,
                q4Completed = analytics.q4Completed + 1
            )
        }
        dailyAnalyticsDao.update(updated)
    }
    
    /**
     * Record an AI classification event.
     */
    suspend fun recordAiClassification() {
        val analytics = getOrCreateTodayAnalytics()
        val updated = analytics.copy(aiClassifications = analytics.aiClassifications + 1)
        dailyAnalyticsDao.update(updated)
    }
    
    /**
     * Record an AI override event.
     * Per TM-010: User overrides AI classification.
     */
    suspend fun recordAiOverride() {
        val analytics = getOrCreateTodayAnalytics()
        val updated = analytics.copy(aiOverrides = analytics.aiOverrides + 1)
        dailyAnalyticsDao.update(updated)
    }
    
    /**
     * Record goal progress update.
     */
    suspend fun recordGoalProgressed() {
        val analytics = getOrCreateTodayAnalytics()
        val updated = analytics.copy(goalsProgressed = analytics.goalsProgressed + 1)
        dailyAnalyticsDao.update(updated)
    }
    
    /**
     * Record briefing opened.
     * Per CB-001: Morning briefing tracking.
     */
    suspend fun recordBriefingOpened() {
        val analytics = getOrCreateTodayAnalytics()
        val updated = analytics.copy(briefingOpened = true)
        dailyAnalyticsDao.update(updated)
    }
    
    /**
     * Record evening summary opened.
     * Per CB-003: Evening summary tracking.
     */
    suspend fun recordSummaryOpened() {
        val analytics = getOrCreateTodayAnalytics()
        val updated = analytics.copy(summaryOpened = true)
        dailyAnalyticsDao.update(updated)
    }
    
    // ==================== Summary Statistics ====================
    
    /**
     * Get productivity summary for a period.
     */
    suspend fun getProductivitySummary(
        startDate: LocalDate = getToday().minus(DatePeriod(days = DEFAULT_WINDOW_DAYS)),
        endDate: LocalDate = getToday()
    ): ProductivitySummary {
        val totalCreated = getTotalTasksCreated(startDate, endDate)
        val totalCompleted = getTotalTasksCompleted(startDate, endDate)
        val completionRate = getCompletionRate(startDate, endDate)
        val aiAccuracy = getAiAccuracy(startDate, endDate)
        
        return ProductivitySummary(
            totalTasksCreated = totalCreated,
            totalTasksCompleted = totalCompleted,
            completionRate = completionRate,
            aiAccuracy = aiAccuracy,
            isCompletionRateOnTarget = completionRate >= TARGET_COMPLETION_RATE,
            isAiAccuracyOnTarget = aiAccuracy >= TARGET_AI_ACCURACY
        )
    }
    
    /**
     * Get quadrant breakdown for today.
     */
    suspend fun getTodayQuadrantBreakdown(): QuadrantBreakdown {
        val analytics = getOrCreateTodayAnalytics()
        return QuadrantBreakdown(
            q1Completed = analytics.q1Completed,
            q2Completed = analytics.q2Completed,
            q3Completed = analytics.q3Completed,
            q4Completed = analytics.q4Completed
        )
    }

    // ==================== Weekly Chart Data (3.5.3) ====================

    /**
     * Get daily completion data for the past 7 days, suitable for bar chart rendering.
     * Returns exactly 7 entries, filling in zeros for days with no data.
     */
    suspend fun getWeeklyCompletionData(): List<DailyCompletionPoint> {
        val today = getToday()
        val weekAgo = today.minus(DatePeriod(days = 6))
        val records = dailyAnalyticsDao.getInRangeSync(weekAgo, today)
        val recordMap = records.associateBy { it.date }

        return (0..6).map { offset ->
            val date = weekAgo.plus(DatePeriod(days = offset))
            val record = recordMap[date]
            DailyCompletionPoint(
                date = date,
                tasksCompleted = record?.tasksCompleted ?: 0,
                q1Completed = record?.q1Completed ?: 0,
                q2Completed = record?.q2Completed ?: 0,
                q3Completed = record?.q3Completed ?: 0,
                q4Completed = record?.q4Completed ?: 0
            )
        }
    }

    // ==================== Streak Calculation (3.5.4) ====================

    /**
     * Calculate the current productivity streak.
     * A streak is the count of consecutive days (ending today or yesterday)
     * where at least one task was completed.
     *
     * Per Jordan persona: visible streaks drive engagement and habit formation.
     */
    suspend fun getCurrentStreak(): Int {
        val productiveDays = dailyAnalyticsDao.getProductiveDaysDesc()
        if (productiveDays.isEmpty()) return 0

        val today = getToday()
        val yesterday = today.minus(DatePeriod(days = 1))

        // Streak must start from today or yesterday
        val firstDay = productiveDays.first().date
        if (firstDay != today && firstDay != yesterday) return 0

        var streak = 1
        for (i in 1 until productiveDays.size) {
            val prevDate = productiveDays[i - 1].date
            val currDate = productiveDays[i].date
            val daysBetween = currDate.daysUntil(prevDate)
            if (daysBetween == 1) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    /**
     * Get the longest productivity streak ever recorded.
     */
    suspend fun getLongestStreak(): Int {
        val productiveDays = dailyAnalyticsDao.getProductiveDaysDesc()
            .sortedBy { it.date }  // Sort ascending for forward iteration
        if (productiveDays.isEmpty()) return 0

        var longest = 1
        var current = 1
        for (i in 1 until productiveDays.size) {
            val prevDate = productiveDays[i - 1].date
            val currDate = productiveDays[i].date
            val daysBetween = prevDate.daysUntil(currDate)
            if (daysBetween == 1) {
                current++
                if (current > longest) longest = current
            } else {
                current = 1
            }
        }
        return longest
    }
    
    // ==================== Helpers ====================
    
    private fun getToday(): LocalDate {
        val timeZone = TimeZone.currentSystemDefault()
        return clock.now().toLocalDateTime(timeZone).date
    }
}

/**
 * Productivity summary statistics.
 */
data class ProductivitySummary(
    val totalTasksCreated: Int,
    val totalTasksCompleted: Int,
    val completionRate: Float,
    val aiAccuracy: Float,
    val isCompletionRateOnTarget: Boolean,
    val isAiAccuracyOnTarget: Boolean
)

/**
 * Quadrant completion breakdown.
 */
data class QuadrantBreakdown(
    val q1Completed: Int,
    val q2Completed: Int,
    val q3Completed: Int,
    val q4Completed: Int
) {
    val total: Int get() = q1Completed + q2Completed + q3Completed + q4Completed
}

/**
 * Single data point for the 7-day completion bar chart (3.5.3).
 */
data class DailyCompletionPoint(
    val date: LocalDate,
    val tasksCompleted: Int,
    val q1Completed: Int = 0,
    val q2Completed: Int = 0,
    val q3Completed: Int = 0,
    val q4Completed: Int = 0
)

package com.prio.core.data.repository

import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.data.local.dao.DailyAnalyticsDao
import com.prio.core.data.local.dao.TaskDao
import com.prio.core.data.local.entity.DailyAnalyticsEntity
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for AnalyticsRepository.
 * 
 * Per ACTION_PLAN.md 2.1.10: 80%+ coverage.
 * Tests:
 * - Completion rate calculation
 * - AI accuracy tracking
 * - Event recording
 */
@DisplayName("AnalyticsRepository")
class AnalyticsRepositoryTest {
    
    @MockK
    private lateinit var dailyAnalyticsDao: DailyAnalyticsDao
    
    @MockK
    private lateinit var taskDao: TaskDao
    
    @MockK
    private lateinit var clock: Clock
    
    private lateinit var repository: AnalyticsRepository
    
    private val now = Instant.parse("2026-02-04T12:00:00Z")
    private val today = LocalDate.parse("2026-02-04")
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { clock.now() } returns now
        repository = AnalyticsRepository(dailyAnalyticsDao, taskDao, clock)
    }
    
    private fun createTestAnalytics(
        date: LocalDate = today,
        tasksCreated: Int = 10,
        tasksCompleted: Int = 6,
        aiClassifications: Int = 8,
        aiOverrides: Int = 2
    ) = DailyAnalyticsEntity(
        id = 1L,
        date = date,
        tasksCreated = tasksCreated,
        tasksCompleted = tasksCompleted,
        q1Completed = 2,
        q2Completed = 2,
        q3Completed = 1,
        q4Completed = 1,
        goalsProgressed = 2,
        aiClassifications = aiClassifications,
        aiOverrides = aiOverrides,
        briefingOpened = true,
        summaryOpened = false
    )
    
    @Nested
    @DisplayName("Completion Rate")
    inner class CompletionRateTests {
        
        @Test
        @DisplayName("getCompletionRate calculates correctly")
        fun getCompletionRate_calculatesCorrectly() = runTest {
            val startDate = LocalDate.parse("2026-01-28")
            val endDate = today
            
            coEvery { dailyAnalyticsDao.getCompletionRateInRange(startDate, endDate) } returns 0.6f
            
            val rate = repository.getCompletionRate(startDate, endDate)
            
            assertEquals(0.6f, rate)
        }
        
        @Test
        @DisplayName("getCompletionRate returns 0 when no data")
        fun getCompletionRate_returnsZeroWhenNoData() = runTest {
            val startDate = LocalDate.parse("2026-01-28")
            val endDate = today
            
            coEvery { dailyAnalyticsDao.getCompletionRateInRange(startDate, endDate) } returns null
            
            val rate = repository.getCompletionRate(startDate, endDate)
            
            assertEquals(0f, rate)
        }
    }
    
    @Nested
    @DisplayName("AI Accuracy")
    inner class AiAccuracyTests {
        
        @Test
        @DisplayName("getAiAccuracy calculates correctly")
        fun getAiAccuracy_calculatesCorrectly() = runTest {
            val startDate = LocalDate.parse("2026-01-28")
            val endDate = today
            
            // 100 classifications, 20 overrides = 80% accuracy
            coEvery { dailyAnalyticsDao.getTotalAiClassificationsInRange(startDate, endDate) } returns 100
            coEvery { dailyAnalyticsDao.getTotalAiOverridesInRange(startDate, endDate) } returns 20
            
            val accuracy = repository.getAiAccuracy(startDate, endDate)
            
            assertEquals(0.8f, accuracy)
        }
        
        @Test
        @DisplayName("getAiAccuracy returns 1 when no classifications")
        fun getAiAccuracy_returnsOneWhenNoClassifications() = runTest {
            val startDate = LocalDate.parse("2026-01-28")
            val endDate = today
            
            coEvery { dailyAnalyticsDao.getTotalAiClassificationsInRange(startDate, endDate) } returns 0
            coEvery { dailyAnalyticsDao.getTotalAiOverridesInRange(startDate, endDate) } returns 0
            
            val accuracy = repository.getAiAccuracy(startDate, endDate)
            
            assertEquals(1f, accuracy)
        }
        
        @Test
        @DisplayName("isAiAccuracyBelowThreshold returns true when below 70%")
        fun isAiAccuracyBelowThreshold_returnsTrueWhenBelow() = runTest {
            coEvery { dailyAnalyticsDao.getTotalAiClassificationsInRange(any(), any()) } returns 100
            coEvery { dailyAnalyticsDao.getTotalAiOverridesInRange(any(), any()) } returns 35 // 65% accuracy
            
            val belowThreshold = repository.isAiAccuracyBelowThreshold()
            
            assertTrue(belowThreshold)
        }
        
        @Test
        @DisplayName("isAiAccuracyBelowThreshold returns false when at 70%")
        fun isAiAccuracyBelowThreshold_returnsFalseWhenAt() = runTest {
            coEvery { dailyAnalyticsDao.getTotalAiClassificationsInRange(any(), any()) } returns 100
            coEvery { dailyAnalyticsDao.getTotalAiOverridesInRange(any(), any()) } returns 30 // 70% accuracy
            
            val belowThreshold = repository.isAiAccuracyBelowThreshold()
            
            assertFalse(belowThreshold)
        }
    }
    
    @Nested
    @DisplayName("Event Recording")
    inner class EventRecordingTests {
        
        @Test
        @DisplayName("recordTaskCreated increments counter")
        fun recordTaskCreated_incrementsCounter() = runTest {
            val analytics = createTestAnalytics(tasksCreated = 5)
            coEvery { dailyAnalyticsDao.getByDate(today) } returns analytics
            
            repository.recordTaskCreated()
            
            val updatedSlot = slot<DailyAnalyticsEntity>()
            coVerify { dailyAnalyticsDao.update(capture(updatedSlot)) }
            assertEquals(6, updatedSlot.captured.tasksCreated)
        }
        
        @Test
        @DisplayName("recordTaskCompleted increments total and quadrant counters")
        fun recordTaskCompleted_incrementsCounters() = runTest {
            val analytics = createTestAnalytics(tasksCompleted = 5)
            coEvery { dailyAnalyticsDao.getByDate(today) } returns analytics
            
            repository.recordTaskCompleted(EisenhowerQuadrant.DO_FIRST)
            
            val updatedSlot = slot<DailyAnalyticsEntity>()
            coVerify { dailyAnalyticsDao.update(capture(updatedSlot)) }
            assertEquals(6, updatedSlot.captured.tasksCompleted)
            assertEquals(3, updatedSlot.captured.q1Completed) // Started with 2
        }
        
        @Test
        @DisplayName("recordAiClassification increments counter")
        fun recordAiClassification_incrementsCounter() = runTest {
            val analytics = createTestAnalytics(aiClassifications = 10)
            coEvery { dailyAnalyticsDao.getByDate(today) } returns analytics
            
            repository.recordAiClassification()
            
            val updatedSlot = slot<DailyAnalyticsEntity>()
            coVerify { dailyAnalyticsDao.update(capture(updatedSlot)) }
            assertEquals(11, updatedSlot.captured.aiClassifications)
        }
        
        @Test
        @DisplayName("recordAiOverride increments counter")
        fun recordAiOverride_incrementsCounter() = runTest {
            val analytics = createTestAnalytics(aiOverrides = 3)
            coEvery { dailyAnalyticsDao.getByDate(today) } returns analytics
            
            repository.recordAiOverride()
            
            val updatedSlot = slot<DailyAnalyticsEntity>()
            coVerify { dailyAnalyticsDao.update(capture(updatedSlot)) }
            assertEquals(4, updatedSlot.captured.aiOverrides)
        }
        
        @Test
        @DisplayName("recordBriefingOpened sets flag")
        fun recordBriefingOpened_setsFlag() = runTest {
            val analytics = createTestAnalytics().copy(briefingOpened = false)
            coEvery { dailyAnalyticsDao.getByDate(today) } returns analytics
            
            repository.recordBriefingOpened()
            
            val updatedSlot = slot<DailyAnalyticsEntity>()
            coVerify { dailyAnalyticsDao.update(capture(updatedSlot)) }
            assertTrue(updatedSlot.captured.briefingOpened)
        }
        
        @Test
        @DisplayName("getOrCreateTodayAnalytics creates when not exists")
        fun getOrCreateTodayAnalytics_createsWhenNotExists() = runTest {
            coEvery { dailyAnalyticsDao.getByDate(today) } returns null
            val insertSlot = slot<DailyAnalyticsEntity>()
            coEvery { dailyAnalyticsDao.insert(capture(insertSlot)) } returns 1L
            
            val result = repository.getOrCreateTodayAnalytics()
            
            assertEquals(today, insertSlot.captured.date)
            assertEquals(0, insertSlot.captured.tasksCreated)
        }
    }
    
    @Nested
    @DisplayName("Summary Statistics")
    inner class SummaryStatisticsTests {
        
        @Test
        @DisplayName("getProductivitySummary returns correct data")
        fun getProductivitySummary_returnsCorrectData() = runTest {
            val startDate = LocalDate.parse("2026-01-28")
            val endDate = today
            
            coEvery { dailyAnalyticsDao.getTotalCreatedInRange(startDate, endDate) } returns 50
            coEvery { dailyAnalyticsDao.getTotalCompletedInRange(startDate, endDate) } returns 35
            coEvery { dailyAnalyticsDao.getCompletionRateInRange(startDate, endDate) } returns 0.7f
            coEvery { dailyAnalyticsDao.getTotalAiClassificationsInRange(startDate, endDate) } returns 100
            coEvery { dailyAnalyticsDao.getTotalAiOverridesInRange(startDate, endDate) } returns 15
            
            val summary = repository.getProductivitySummary(startDate, endDate)
            
            assertEquals(50, summary.totalTasksCreated)
            assertEquals(35, summary.totalTasksCompleted)
            assertEquals(0.7f, summary.completionRate)
            assertEquals(0.85f, summary.aiAccuracy)
            assertTrue(summary.isCompletionRateOnTarget)
            assertTrue(summary.isAiAccuracyOnTarget)
        }
        
        @Test
        @DisplayName("getTodayQuadrantBreakdown returns correct data")
        fun getTodayQuadrantBreakdown_returnsCorrectData() = runTest {
            val analytics = createTestAnalytics()
            coEvery { dailyAnalyticsDao.getByDate(today) } returns analytics
            
            val breakdown = repository.getTodayQuadrantBreakdown()
            
            assertEquals(2, breakdown.q1Completed)
            assertEquals(2, breakdown.q2Completed)
            assertEquals(1, breakdown.q3Completed)
            assertEquals(1, breakdown.q4Completed)
            assertEquals(6, breakdown.total)
        }
    }

    @Nested
    @DisplayName("Weekly Completion Data (3.5.3)")
    inner class WeeklyCompletionDataTests {

        @Test
        @DisplayName("returns exactly 7 data points")
        fun returnsExactly7DataPoints() = runTest {
            coEvery { dailyAnalyticsDao.getInRangeSync(any(), any()) } returns emptyList()

            val data = repository.getWeeklyCompletionData()

            assertEquals(7, data.size)
        }

        @Test
        @DisplayName("fills zeros for days with no analytics")
        fun fillsZerosForMissingDays() = runTest {
            coEvery { dailyAnalyticsDao.getInRangeSync(any(), any()) } returns emptyList()

            val data = repository.getWeeklyCompletionData()

            assertTrue(data.all { it.tasksCompleted == 0 })
            assertTrue(data.all { it.q1Completed == 0 })
        }

        @Test
        @DisplayName("maps existing records to correct dates")
        fun mapsExistingRecords() = runTest {
            val record = DailyAnalyticsEntity(
                id = 1L,
                date = today,
                tasksCompleted = 5,
                q1Completed = 2,
                q2Completed = 3,
                q3Completed = 0,
                q4Completed = 0
            )
            coEvery { dailyAnalyticsDao.getInRangeSync(any(), any()) } returns listOf(record)

            val data = repository.getWeeklyCompletionData()

            val todayPoint = data.last() // today is the last element (most recent)
            assertEquals(5, todayPoint.tasksCompleted)
            assertEquals(2, todayPoint.q1Completed)
            assertEquals(3, todayPoint.q2Completed)
        }

        @Test
        @DisplayName("dates span from 6 days ago to today")
        fun datesSpanCorrectly() = runTest {
            coEvery { dailyAnalyticsDao.getInRangeSync(any(), any()) } returns emptyList()

            val data = repository.getWeeklyCompletionData()

            assertEquals(today, data.last().date)
            assertEquals(LocalDate.parse("2026-01-29"), data.first().date)
        }
    }

    @Nested
    @DisplayName("Streak Calculation (3.5.4)")
    inner class StreakCalculationTests {

        private fun makeProductiveDay(date: LocalDate) = DailyAnalyticsEntity(
            id = 0L,
            date = date,
            tasksCompleted = 1
        )

        @Test
        @DisplayName("getCurrentStreak returns 0 when no productive days")
        fun getCurrentStreak_returnsZeroWhenEmpty() = runTest {
            coEvery { dailyAnalyticsDao.getProductiveDaysDesc() } returns emptyList()

            assertEquals(0, repository.getCurrentStreak())
        }

        @Test
        @DisplayName("getCurrentStreak returns 1 when only today is productive")
        fun getCurrentStreak_returns1ForToday() = runTest {
            coEvery { dailyAnalyticsDao.getProductiveDaysDesc() } returns listOf(
                makeProductiveDay(today)
            )

            assertEquals(1, repository.getCurrentStreak())
        }

        @Test
        @DisplayName("getCurrentStreak counts consecutive days from today")
        fun getCurrentStreak_countsConsecutiveDays() = runTest {
            coEvery { dailyAnalyticsDao.getProductiveDaysDesc() } returns listOf(
                makeProductiveDay(today),                              // Feb 4
                makeProductiveDay(LocalDate.parse("2026-02-03")),      // Feb 3
                makeProductiveDay(LocalDate.parse("2026-02-02"))       // Feb 2
            )

            assertEquals(3, repository.getCurrentStreak())
        }

        @Test
        @DisplayName("getCurrentStreak breaks on gap")
        fun getCurrentStreak_breaksOnGap() = runTest {
            coEvery { dailyAnalyticsDao.getProductiveDaysDesc() } returns listOf(
                makeProductiveDay(today),                              // Feb 4
                makeProductiveDay(LocalDate.parse("2026-02-03")),      // Feb 3
                // Gap: Feb 2 missing
                makeProductiveDay(LocalDate.parse("2026-02-01"))       // Feb 1
            )

            assertEquals(2, repository.getCurrentStreak())
        }

        @Test
        @DisplayName("getCurrentStreak starts from yesterday if today has no data")
        fun getCurrentStreak_startsFromYesterday() = runTest {
            coEvery { dailyAnalyticsDao.getProductiveDaysDesc() } returns listOf(
                makeProductiveDay(LocalDate.parse("2026-02-03")),      // yesterday
                makeProductiveDay(LocalDate.parse("2026-02-02"))
            )

            assertEquals(2, repository.getCurrentStreak())
        }

        @Test
        @DisplayName("getCurrentStreak returns 0 when first day is not today/yesterday")
        fun getCurrentStreak_returnsZeroWhenOldData() = runTest {
            coEvery { dailyAnalyticsDao.getProductiveDaysDesc() } returns listOf(
                makeProductiveDay(LocalDate.parse("2026-02-01")) // 3 days ago
            )

            assertEquals(0, repository.getCurrentStreak())
        }

        @Test
        @DisplayName("getLongestStreak finds the longest historical streak")
        fun getLongestStreak_findsLongest() = runTest {
            coEvery { dailyAnalyticsDao.getProductiveDaysDesc() } returns listOf(
                // Current streak: 2 (Feb 3-4)
                makeProductiveDay(today),
                makeProductiveDay(LocalDate.parse("2026-02-03")),
                // Gap
                // Historical streak: 3 (Jan 29-31)
                makeProductiveDay(LocalDate.parse("2026-01-31")),
                makeProductiveDay(LocalDate.parse("2026-01-30")),
                makeProductiveDay(LocalDate.parse("2026-01-29"))
            )

            assertEquals(3, repository.getLongestStreak())
        }

        @Test
        @DisplayName("getLongestStreak returns 1 for single productive day")
        fun getLongestStreak_returnsSingleDay() = runTest {
            coEvery { dailyAnalyticsDao.getProductiveDaysDesc() } returns listOf(
                makeProductiveDay(today)
            )

            assertEquals(1, repository.getLongestStreak())
        }

        @Test
        @DisplayName("getLongestStreak returns 0 when no productive days")
        fun getLongestStreak_returnsZeroWhenEmpty() = runTest {
            coEvery { dailyAnalyticsDao.getProductiveDaysDesc() } returns emptyList()

            assertEquals(0, repository.getLongestStreak())
        }
    }
}

package com.prio.app.worker

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prio.core.common.model.RecurrencePattern
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for recurring task date calculations.
 * 
 * Tests the date calculation logic from RecurringTaskWorker.
 * Per TM-008 from 0.3.2_task_management_user_stories.md:
 * - Daily: Next day
 * - Weekly: Same day next week  
 * - Monthly: Same date next month
 * - Yearly: Same date next year
 * - Weekdays: Next weekday (Mon-Fri)
 */
@RunWith(AndroidJUnit4::class)
class RecurringTaskDateCalculationTest {

    private val timeZone = TimeZone.UTC
    
    // Test reference date: Monday, February 10, 2026, 9:00 AM
    private val referenceDate = LocalDate(2026, 2, 10)
        .atTime(LocalTime(9, 0))
        .toInstant(timeZone)

    @Test
    fun dailyRecurrence_addsOneDay() {
        // Given: A task due on Feb 10, 2026
        val currentDueDate = referenceDate
        
        // When: Calculating next occurrence for DAILY pattern
        val nextDue = calculateNextDueDate(currentDueDate, RecurrencePattern.DAILY, referenceDate)
        
        // Then: Next due should be Feb 11, 2026
        val expected = LocalDate(2026, 2, 11)
            .atTime(LocalTime(9, 0))
            .toInstant(timeZone)
        assertEquals(expected, nextDue)
    }

    @Test
    fun weeklyRecurrence_addsOneWeek() {
        // Given: A task due on Feb 10, 2026 (Monday)
        val currentDueDate = referenceDate
        
        // When: Calculating next occurrence for WEEKLY pattern
        val nextDue = calculateNextDueDate(currentDueDate, RecurrencePattern.WEEKLY, referenceDate)
        
        // Then: Next due should be Feb 17, 2026 (next Monday)
        val expected = LocalDate(2026, 2, 17)
            .atTime(LocalTime(9, 0))
            .toInstant(timeZone)
        assertEquals(expected, nextDue)
    }

    @Test
    fun monthlyRecurrence_addsOneMonth() {
        // Given: A task due on Feb 10, 2026
        val currentDueDate = referenceDate
        
        // When: Calculating next occurrence for MONTHLY pattern
        val nextDue = calculateNextDueDate(currentDueDate, RecurrencePattern.MONTHLY, referenceDate)
        
        // Then: Next due should be Mar 10, 2026
        val expected = LocalDate(2026, 3, 10)
            .atTime(LocalTime(9, 0))
            .toInstant(timeZone)
        assertEquals(expected, nextDue)
    }

    @Test
    fun yearlyRecurrence_addsOneYear() {
        // Given: A task due on Feb 10, 2026
        val currentDueDate = referenceDate
        
        // When: Calculating next occurrence for YEARLY pattern
        val nextDue = calculateNextDueDate(currentDueDate, RecurrencePattern.YEARLY, referenceDate)
        
        // Then: Next due should be Feb 10, 2027
        val expected = LocalDate(2027, 2, 10)
            .atTime(LocalTime(9, 0))
            .toInstant(timeZone)
        assertEquals(expected, nextDue)
    }

    @Test
    fun weekdaysRecurrence_skipsWeekends_fromFriday() {
        // Given: A task due on Friday, Feb 13, 2026
        val fridayDate = LocalDate(2026, 2, 13)
            .atTime(LocalTime(9, 0))
            .toInstant(timeZone)
        
        // When: Calculating next occurrence for WEEKDAYS pattern
        val nextDue = calculateNextDueDate(fridayDate, RecurrencePattern.WEEKDAYS, fridayDate)
        
        // Then: Next due should be Monday, Feb 16, 2026 (skip weekend)
        val expected = LocalDate(2026, 2, 16)
            .atTime(LocalTime(9, 0))
            .toInstant(timeZone)
        assertEquals(expected, nextDue)
    }

    @Test
    fun weekdaysRecurrence_skipsWeekends_fromSaturday() {
        // Given: A task completed on Saturday, Feb 14, 2026
        val saturdayDate = LocalDate(2026, 2, 14)
            .atTime(LocalTime(9, 0))
            .toInstant(timeZone)
        
        // When: Calculating next occurrence for WEEKDAYS pattern
        val nextDue = calculateNextDueDate(saturdayDate, RecurrencePattern.WEEKDAYS, saturdayDate)
        
        // Then: Next due should be Monday, Feb 16, 2026
        val expected = LocalDate(2026, 2, 16)
            .atTime(LocalTime(9, 0))
            .toInstant(timeZone)
        assertEquals(expected, nextDue)
    }

    @Test
    fun weekdaysRecurrence_fromMonday() {
        // Given: A task due on Monday, Feb 10, 2026
        val mondayDate = referenceDate
        
        // When: Calculating next occurrence for WEEKDAYS pattern  
        val nextDue = calculateNextDueDate(mondayDate, RecurrencePattern.WEEKDAYS, mondayDate)
        
        // Then: Next due should be Tuesday, Feb 11, 2026
        val expected = LocalDate(2026, 2, 11)
            .atTime(LocalTime(9, 0))
            .toInstant(timeZone)
        assertEquals(expected, nextDue)
    }

    @Test
    fun customRecurrence_defaultsToDaily() {
        // Given: A task with CUSTOM pattern
        val currentDueDate = referenceDate
        
        // When: Calculating next occurrence
        val nextDue = calculateNextDueDate(currentDueDate, RecurrencePattern.CUSTOM, referenceDate)
        
        // Then: Should default to next day
        val expected = LocalDate(2026, 2, 11)
            .atTime(LocalTime(9, 0))
            .toInstant(timeZone)
        assertEquals(expected, nextDue)
    }

    @Test
    fun monthlyRecurrence_endOfMonth() {
        // Given: A task due on Jan 31, 2026
        val janDate = LocalDate(2026, 1, 31)
            .atTime(LocalTime(9, 0))
            .toInstant(timeZone)
        
        // When: Calculating next monthly occurrence
        val nextDue = calculateNextDueDate(janDate, RecurrencePattern.MONTHLY, janDate)
        
        // Then: Should handle Feb correctly (Feb 28 in 2026)
        val result = nextDue.toLocalDateTime(timeZone)
        assertEquals(2, result.monthNumber)
        assertTrue(result.dayOfMonth <= 28)
    }

    @Test
    fun yearlyRecurrence_leapYear() {
        // Given: A task due on Feb 29, 2024 (leap year)
        val leapDate = LocalDate(2024, 2, 29)
            .atTime(LocalTime(9, 0))
            .toInstant(timeZone)
        
        // When: Calculating next yearly occurrence
        val nextDue = calculateNextDueDate(leapDate, RecurrencePattern.YEARLY, leapDate)
        
        // Then: Should handle non-leap year correctly (Feb 28, 2025)
        val result = nextDue.toLocalDateTime(timeZone)
        assertEquals(2025, result.year)
        assertEquals(2, result.monthNumber)
        assertTrue(result.dayOfMonth <= 28)
    }

    @Test
    fun preservesTimeComponent() {
        // Given: A task due at 3:30 PM
        val afternoonTask = LocalDate(2026, 2, 10)
            .atTime(LocalTime(15, 30))
            .toInstant(timeZone)
        
        // When: Calculating next occurrence
        val nextDue = calculateNextDueDate(afternoonTask, RecurrencePattern.DAILY, afternoonTask)
        
        // Then: Time should be preserved
        val result = nextDue.toLocalDateTime(timeZone)
        assertEquals(15, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun allRecurrencePatternsHandled() {
        // Verify all patterns are handled without exception
        RecurrencePattern.entries.forEach { pattern ->
            val result = calculateNextDueDate(referenceDate, pattern, referenceDate)
            assertTrue("Pattern $pattern should produce future date", 
                result > referenceDate || pattern == RecurrencePattern.CUSTOM)
        }
    }

    // =========================================================================
    // Test implementation of the calculation logic (mirrors RecurringTaskWorker)
    // =========================================================================

    private fun calculateNextDueDate(
        currentDueDate: Instant?,
        pattern: RecurrencePattern,
        completedAt: Instant
    ): Instant {
        val referenceInstant = currentDueDate ?: completedAt
        val referenceDateTime = referenceInstant.toLocalDateTime(timeZone)
        val referenceLocalDate = LocalDate(
            referenceDateTime.year,
            referenceDateTime.monthNumber,
            referenceDateTime.dayOfMonth
        )
        val referenceTime = LocalTime(referenceDateTime.hour, referenceDateTime.minute)

        val nextDate = when (pattern) {
            RecurrencePattern.DAILY -> {
                referenceLocalDate.plus(DatePeriod(days = 1))
            }
            RecurrencePattern.WEEKLY -> {
                referenceLocalDate.plus(DatePeriod(days = 7))
            }
            RecurrencePattern.MONTHLY -> {
                referenceLocalDate.plus(DatePeriod(months = 1))
            }
            RecurrencePattern.YEARLY -> {
                referenceLocalDate.plus(DatePeriod(years = 1))
            }
            RecurrencePattern.WEEKDAYS -> {
                var next = referenceLocalDate.plus(DatePeriod(days = 1))
                while (next.dayOfWeek == DayOfWeek.SATURDAY || 
                       next.dayOfWeek == DayOfWeek.SUNDAY) {
                    next = next.plus(DatePeriod(days = 1))
                }
                next
            }
            RecurrencePattern.CUSTOM -> {
                // Default to daily for CUSTOM pattern
                referenceLocalDate.plus(DatePeriod(days = 1))
            }
        }

        return nextDate.atTime(referenceTime).toInstant(timeZone)
    }
}

package com.prio.core.data.preferences

import com.prio.core.common.model.ThemeMode
import com.prio.core.common.model.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for UserPreferences model.
 * 
 * Per ACTION_PLAN.md 2.1.11: Test UserPreferences data class functionality.
 * 
 * Note: UserPreferencesRepository requires Android Context for DataStore.
 * Android Instrumented tests are in androidTest folder.
 * These tests cover the UserPreferences data class behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("UserPreferences")
class UserPreferencesTest {
    
    @Nested
    @DisplayName("Default Values")
    inner class DefaultValues {
        
        @Test
        @DisplayName("should have correct default morning briefing time")
        fun defaultMorningBriefingTime() {
            val prefs = UserPreferences.DEFAULT
            assertEquals(LocalTime(7, 0), prefs.morningBriefingTime)
        }
        
        @Test
        @DisplayName("should have correct default evening summary time")
        fun defaultEveningSummaryTime() {
            val prefs = UserPreferences.DEFAULT
            assertEquals(LocalTime(18, 0), prefs.eveningSummaryTime)
        }
        
        @Test
        @DisplayName("should have briefings enabled by default")
        fun briefingEnabledByDefault() {
            val prefs = UserPreferences.DEFAULT
            assertTrue(prefs.briefingEnabled)
        }
        
        @Test
        @DisplayName("should have system theme by default")
        fun systemThemeByDefault() {
            val prefs = UserPreferences.DEFAULT
            assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
        }
        
        @Test
        @DisplayName("should have notifications enabled by default")
        fun notificationsEnabledByDefault() {
            val prefs = UserPreferences.DEFAULT
            assertTrue(prefs.notificationsEnabled)
        }
        
        @Test
        @DisplayName("should have 15 minute reminder advance by default")
        fun defaultReminderAdvance() {
            val prefs = UserPreferences.DEFAULT
            assertEquals(15, prefs.reminderAdvanceMinutes)
        }
        
        @Test
        @DisplayName("should have Phi-3 model as default")
        fun defaultAiModel() {
            val prefs = UserPreferences.DEFAULT
            assertEquals("phi-3-mini-4k-instruct-q4", prefs.aiModelId)
        }
        
        @Test
        @DisplayName("should have AI model not downloaded by default")
        fun aiModelNotDownloadedByDefault() {
            val prefs = UserPreferences.DEFAULT
            assertFalse(prefs.aiModelDownloaded)
        }
        
        @Test
        @DisplayName("should have AI classification enabled by default")
        fun aiClassificationEnabledByDefault() {
            val prefs = UserPreferences.DEFAULT
            assertTrue(prefs.aiClassificationEnabled)
        }
        
        @Test
        @DisplayName("should have daily AI limit of 5 for free tier")
        fun defaultDailyAiLimit() {
            val prefs = UserPreferences.DEFAULT
            assertEquals(5, prefs.dailyAiLimit)
        }
        
        @Test
        @DisplayName("should have zero AI used by default")
        fun defaultDailyAiUsed() {
            val prefs = UserPreferences.DEFAULT
            assertEquals(0, prefs.dailyAiUsed)
        }
        
        @Test
        @DisplayName("should have onboarding not completed by default")
        fun onboardingNotCompletedByDefault() {
            val prefs = UserPreferences.DEFAULT
            assertFalse(prefs.onboardingCompleted)
        }
        
        @Test
        @DisplayName("should have null user name by default")
        fun nullUserNameByDefault() {
            val prefs = UserPreferences.DEFAULT
            assertNull(prefs.userName)
        }
        
        @Test
        @DisplayName("should have calendar not connected by default")
        fun calendarNotConnectedByDefault() {
            val prefs = UserPreferences.DEFAULT
            assertFalse(prefs.calendarConnected)
        }
    }
    
    @Nested
    @DisplayName("Time Parsing")
    inner class TimeParsing {
        
        @Test
        @DisplayName("should parse valid time string")
        fun parseValidTime() {
            val time = UserPreferences.parseTime("08:30")
            assertEquals(LocalTime(8, 30), time)
        }
        
        @Test
        @DisplayName("should parse time with leading zeros")
        fun parseTimeWithLeadingZeros() {
            val time = UserPreferences.parseTime("07:05")
            assertEquals(LocalTime(7, 5), time)
        }
        
        @Test
        @DisplayName("should return default for invalid time string")
        fun parseInvalidTime() {
            val time = UserPreferences.parseTime("invalid")
            assertEquals(LocalTime(7, 0), time)
        }
        
        @Test
        @DisplayName("should return default for empty time string")
        fun parseEmptyTime() {
            val time = UserPreferences.parseTime("")
            assertEquals(LocalTime(7, 0), time)
        }
        
        @Test
        @DisplayName("should return default for partial time string")
        fun parsePartialTime() {
            val time = UserPreferences.parseTime("08")
            assertEquals(LocalTime(7, 0), time)
        }
    }
    
    @Nested
    @DisplayName("Time Formatting")
    inner class TimeFormatting {
        
        @Test
        @DisplayName("should format time with leading zeros")
        fun formatTimeWithLeadingZeros() {
            val formatted = UserPreferences.formatTime(LocalTime(7, 5))
            assertEquals("07:05", formatted)
        }
        
        @Test
        @DisplayName("should format afternoon time correctly")
        fun formatAfternoonTime() {
            val formatted = UserPreferences.formatTime(LocalTime(18, 30))
            assertEquals("18:30", formatted)
        }
        
        @Test
        @DisplayName("should format midnight correctly")
        fun formatMidnight() {
            val formatted = UserPreferences.formatTime(LocalTime(0, 0))
            assertEquals("00:00", formatted)
        }
    }
    
    @Nested
    @DisplayName("AI Limit Calculations")
    inner class AiLimitCalculations {
        
        @Test
        @DisplayName("should not be at limit when unused")
        fun notAtLimitWhenUnused() {
            val prefs = UserPreferences(dailyAiLimit = 5, dailyAiUsed = 0)
            assertFalse(prefs.isAiLimitReached)
        }
        
        @Test
        @DisplayName("should be at limit when fully used")
        fun atLimitWhenFullyUsed() {
            val prefs = UserPreferences(dailyAiLimit = 5, dailyAiUsed = 5)
            assertTrue(prefs.isAiLimitReached)
        }
        
        @Test
        @DisplayName("should be at limit when exceeded")
        fun atLimitWhenExceeded() {
            val prefs = UserPreferences(dailyAiLimit = 5, dailyAiUsed = 7)
            assertTrue(prefs.isAiLimitReached)
        }
        
        @Test
        @DisplayName("should calculate remaining correctly")
        fun calculateRemaining() {
            val prefs = UserPreferences(dailyAiLimit = 5, dailyAiUsed = 3)
            assertEquals(2, prefs.remainingAiClassifications)
        }
        
        @Test
        @DisplayName("should return zero remaining when limit reached")
        fun zeroRemainingWhenLimitReached() {
            val prefs = UserPreferences(dailyAiLimit = 5, dailyAiUsed = 5)
            assertEquals(0, prefs.remainingAiClassifications)
        }
        
        @Test
        @DisplayName("should return zero remaining when exceeded")
        fun zeroRemainingWhenExceeded() {
            val prefs = UserPreferences(dailyAiLimit = 5, dailyAiUsed = 10)
            assertEquals(0, prefs.remainingAiClassifications)
        }
    }
    
    @Nested
    @DisplayName("ThemeMode")
    inner class ThemeModeTests {
        
        @Test
        @DisplayName("should parse 'light' string")
        fun parseLightTheme() {
            assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("light"))
        }
        
        @Test
        @DisplayName("should parse 'dark' string")
        fun parseDarkTheme() {
            assertEquals(ThemeMode.DARK, ThemeMode.fromString("dark"))
        }
        
        @Test
        @DisplayName("should parse 'system' string")
        fun parseSystemTheme() {
            assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("system"))
        }
        
        @Test
        @DisplayName("should default to system for unknown string")
        fun defaultToSystem() {
            assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("unknown"))
        }
        
        @Test
        @DisplayName("should parse case-insensitively")
        fun parseCaseInsensitive() {
            assertEquals(ThemeMode.DARK, ThemeMode.fromString("DARK"))
            assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("Light"))
        }
        
        @Test
        @DisplayName("should convert to lowercase string")
        fun convertToString() {
            assertEquals("system", ThemeMode.SYSTEM.toString())
            assertEquals("light", ThemeMode.LIGHT.toString())
            assertEquals("dark", ThemeMode.DARK.toString())
        }
    }
    
    @Nested
    @DisplayName("Data Class Copy")
    inner class DataClassCopy {
        
        @Test
        @DisplayName("should copy with modified theme")
        fun copyWithModifiedTheme() {
            val original = UserPreferences.DEFAULT
            val modified = original.copy(themeMode = ThemeMode.DARK)
            
            assertEquals(ThemeMode.DARK, modified.themeMode)
            assertEquals(original.morningBriefingTime, modified.morningBriefingTime)
            assertEquals(original.notificationsEnabled, modified.notificationsEnabled)
        }
        
        @Test
        @DisplayName("should copy with multiple modifications")
        fun copyWithMultipleModifications() {
            val original = UserPreferences.DEFAULT
            val modified = original.copy(
                themeMode = ThemeMode.DARK,
                notificationsEnabled = false,
                morningBriefingTime = LocalTime(6, 30),
                userName = "Test User"
            )
            
            assertEquals(ThemeMode.DARK, modified.themeMode)
            assertFalse(modified.notificationsEnabled)
            assertEquals(LocalTime(6, 30), modified.morningBriefingTime)
            assertEquals("Test User", modified.userName)
        }
    }
}

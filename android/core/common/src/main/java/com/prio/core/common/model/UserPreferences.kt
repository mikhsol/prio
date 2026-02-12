package com.prio.core.common.model

import kotlinx.datetime.LocalTime

/**
 * User preferences data class.
 * 
 * Consolidates all user settings into a single immutable data class
 * for type-safe access throughout the application.
 * 
 * Based on CB-001 from 0.3.4_calendar_briefings_user_stories.md
 */
data class UserPreferences(
    // Briefing settings
    val morningBriefingTime: LocalTime = LocalTime(7, 0),
    val eveningSummaryTime: LocalTime = LocalTime(18, 0),
    val briefingEnabled: Boolean = true,
    
    // Theme
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    
    // Notifications
    val notificationsEnabled: Boolean = true,
    val reminderAdvanceMinutes: Int = 15,
    val eveningSummaryEnabled: Boolean = true,
    val taskRemindersEnabled: Boolean = true,
    val overdueAlertsEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 7,
    
    // AI settings
    val aiModelId: String = "phi-3-mini-4k-instruct-q4",
    val aiModelDownloaded: Boolean = false,
    val aiClassificationEnabled: Boolean = true,
    val dailyAiLimit: Int = 5, // Free tier: 5/day
    val dailyAiUsed: Int = 0,
    
    // Onboarding
    val onboardingCompleted: Boolean = false,
    
    // User profile (local-only mode)
    val userName: String? = null,
    
    // Calendar integration
    val calendarConnected: Boolean = false
) {
    companion object {
        /**
         * Default preferences for first-time users.
         */
        val DEFAULT = UserPreferences()
        
        /**
         * Parse time string in "HH:mm" format to LocalTime.
         */
        fun parseTime(timeString: String): LocalTime {
            return try {
                val parts = timeString.split(":")
                if (parts.size == 2) {
                    LocalTime(parts[0].toInt(), parts[1].toInt())
                } else {
                    LocalTime(7, 0)
                }
            } catch (e: Exception) {
                LocalTime(7, 0)
            }
        }
        
        /**
         * Format LocalTime to "HH:mm" string.
         */
        fun formatTime(time: LocalTime): String {
            return "%02d:%02d".format(time.hour, time.minute)
        }
    }
    
    /**
     * Check if AI usage limit has been reached for the day.
     */
    val isAiLimitReached: Boolean
        get() = dailyAiUsed >= dailyAiLimit
    
    /**
     * Remaining AI classifications for the day.
     */
    val remainingAiClassifications: Int
        get() = (dailyAiLimit - dailyAiUsed).coerceAtLeast(0)
}

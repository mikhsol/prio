package com.prio.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prio.core.common.model.ThemeMode
import com.prio.core.common.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prio_preferences")

/**
 * User preferences stored in DataStore.
 * 
 * Based on CB-001 from 0.3.4_calendar_briefings_user_stories.md
 * and 1.1.9 Settings Screens Spec.
 * 
 * Fields:
 * - morning_briefing_time
 * - evening_summary_time
 * - theme
 * - notification_enabled
 * - AI model settings
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val context: Context
) {
    
    private object PreferencesKeys {
        // Briefing times (stored as "HH:mm")
        val MORNING_BRIEFING_TIME = stringPreferencesKey("morning_briefing_time")
        val EVENING_SUMMARY_TIME = stringPreferencesKey("evening_summary_time")
        val BRIEFING_ENABLED = booleanPreferencesKey("briefing_enabled")
        
        // Theme
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
        
        // Notifications
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_ADVANCE_MINUTES = intPreferencesKey("reminder_advance_minutes")
        
        // AI Settings
        val AI_MODEL_ID = stringPreferencesKey("ai_model_id")
        val AI_MODEL_DOWNLOADED = booleanPreferencesKey("ai_model_downloaded")
        val AI_CLASSIFICATION_ENABLED = booleanPreferencesKey("ai_classification_enabled")
        val DAILY_AI_LIMIT = intPreferencesKey("daily_ai_limit")
        val DAILY_AI_USED = intPreferencesKey("daily_ai_used")
        
        // Onboarding
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FIRST_LAUNCH_DATE = stringPreferencesKey("first_launch_date")
        
        // User profile (local-only mode)
        val USER_NAME = stringPreferencesKey("user_name")
        
        // Calendar integration
        val CALENDAR_CONNECTED = booleanPreferencesKey("calendar_connected")
        val SELECTED_CALENDAR_IDS = stringPreferencesKey("selected_calendar_ids")
    }
    
    // Briefing settings
    val morningBriefingTime: Flow<String> = context.dataStore.data
        .map { it[PreferencesKeys.MORNING_BRIEFING_TIME] ?: "07:00" }
    
    val eveningSummaryTime: Flow<String> = context.dataStore.data
        .map { it[PreferencesKeys.EVENING_SUMMARY_TIME] ?: "18:00" }
    
    val briefingEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.BRIEFING_ENABLED] ?: true }
    
    suspend fun setMorningBriefingTime(time: String) {
        context.dataStore.edit { it[PreferencesKeys.MORNING_BRIEFING_TIME] = time }
    }
    
    suspend fun setEveningSummaryTime(time: String) {
        context.dataStore.edit { it[PreferencesKeys.EVENING_SUMMARY_TIME] = time }
    }
    
    suspend fun setBriefingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BRIEFING_ENABLED] = enabled }
    }
    
    // Theme settings
    val themeMode: Flow<String> = context.dataStore.data
        .map { it[PreferencesKeys.THEME_MODE] ?: "system" }
    
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode }
    }
    
    // Notification settings
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true }
    
    val reminderAdvanceMinutes: Flow<Int> = context.dataStore.data
        .map { it[PreferencesKeys.REMINDER_ADVANCE_MINUTES] ?: 15 }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled }
    }
    
    suspend fun setReminderAdvanceMinutes(minutes: Int) {
        context.dataStore.edit { it[PreferencesKeys.REMINDER_ADVANCE_MINUTES] = minutes }
    }
    
    // AI settings
    val aiModelId: Flow<String> = context.dataStore.data
        .map { it[PreferencesKeys.AI_MODEL_ID] ?: "phi-3-mini-4k-instruct-q4" }
    
    val aiModelDownloaded: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.AI_MODEL_DOWNLOADED] ?: false }
    
    val aiClassificationEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.AI_CLASSIFICATION_ENABLED] ?: true }
    
    val dailyAiLimit: Flow<Int> = context.dataStore.data
        .map { it[PreferencesKeys.DAILY_AI_LIMIT] ?: 5 } // Free tier: 5/day
    
    val dailyAiUsed: Flow<Int> = context.dataStore.data
        .map { it[PreferencesKeys.DAILY_AI_USED] ?: 0 }
    
    suspend fun setAiModelId(modelId: String) {
        context.dataStore.edit { it[PreferencesKeys.AI_MODEL_ID] = modelId }
    }
    
    suspend fun setAiModelDownloaded(downloaded: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AI_MODEL_DOWNLOADED] = downloaded }
    }
    
    suspend fun setAiClassificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AI_CLASSIFICATION_ENABLED] = enabled }
    }
    
    suspend fun incrementDailyAiUsed() {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.DAILY_AI_USED] ?: 0
            prefs[PreferencesKeys.DAILY_AI_USED] = current + 1
        }
    }
    
    suspend fun resetDailyAiUsed() {
        context.dataStore.edit { it[PreferencesKeys.DAILY_AI_USED] = 0 }
    }
    
    // Onboarding
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.ONBOARDING_COMPLETED] ?: false }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.ONBOARDING_COMPLETED] = completed }
    }
    
    // User profile
    val userName: Flow<String?> = context.dataStore.data
        .map { it[PreferencesKeys.USER_NAME] }
    
    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[PreferencesKeys.USER_NAME] = name }
    }
    
    // Calendar integration
    val calendarConnected: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.CALENDAR_CONNECTED] ?: false }
    
    suspend fun setCalendarConnected(connected: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CALENDAR_CONNECTED] = connected }
    }
    
    /**
     * Combined Flow of all user preferences as a single data class.
     * 
     * This provides type-safe access to all preferences and automatically
     * emits updates when any preference changes.
     */
    val userPreferences: Flow<UserPreferences> = context.dataStore.data
        .map { prefs ->
            UserPreferences(
                morningBriefingTime = UserPreferences.parseTime(
                    prefs[PreferencesKeys.MORNING_BRIEFING_TIME] ?: "07:00"
                ),
                eveningSummaryTime = UserPreferences.parseTime(
                    prefs[PreferencesKeys.EVENING_SUMMARY_TIME] ?: "18:00"
                ),
                briefingEnabled = prefs[PreferencesKeys.BRIEFING_ENABLED] ?: true,
                themeMode = ThemeMode.fromString(
                    prefs[PreferencesKeys.THEME_MODE] ?: "system"
                ),
                notificationsEnabled = prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                reminderAdvanceMinutes = prefs[PreferencesKeys.REMINDER_ADVANCE_MINUTES] ?: 15,
                aiModelId = prefs[PreferencesKeys.AI_MODEL_ID] ?: "phi-3-mini-4k-instruct-q4",
                aiModelDownloaded = prefs[PreferencesKeys.AI_MODEL_DOWNLOADED] ?: false,
                aiClassificationEnabled = prefs[PreferencesKeys.AI_CLASSIFICATION_ENABLED] ?: true,
                dailyAiLimit = prefs[PreferencesKeys.DAILY_AI_LIMIT] ?: 5,
                dailyAiUsed = prefs[PreferencesKeys.DAILY_AI_USED] ?: 0,
                onboardingCompleted = prefs[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
                userName = prefs[PreferencesKeys.USER_NAME],
                calendarConnected = prefs[PreferencesKeys.CALENDAR_CONNECTED] ?: false
            )
        }
    
    /**
     * Update multiple preferences at once.
     * 
     * This is more efficient than calling multiple set methods
     * as it only triggers one DataStore write.
     */
    suspend fun updatePreferences(update: (UserPreferences) -> UserPreferences) {
        context.dataStore.edit { prefs ->
            val current = UserPreferences(
                morningBriefingTime = UserPreferences.parseTime(
                    prefs[PreferencesKeys.MORNING_BRIEFING_TIME] ?: "07:00"
                ),
                eveningSummaryTime = UserPreferences.parseTime(
                    prefs[PreferencesKeys.EVENING_SUMMARY_TIME] ?: "18:00"
                ),
                briefingEnabled = prefs[PreferencesKeys.BRIEFING_ENABLED] ?: true,
                themeMode = ThemeMode.fromString(
                    prefs[PreferencesKeys.THEME_MODE] ?: "system"
                ),
                notificationsEnabled = prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                reminderAdvanceMinutes = prefs[PreferencesKeys.REMINDER_ADVANCE_MINUTES] ?: 15,
                aiModelId = prefs[PreferencesKeys.AI_MODEL_ID] ?: "phi-3-mini-4k-instruct-q4",
                aiModelDownloaded = prefs[PreferencesKeys.AI_MODEL_DOWNLOADED] ?: false,
                aiClassificationEnabled = prefs[PreferencesKeys.AI_CLASSIFICATION_ENABLED] ?: true,
                dailyAiLimit = prefs[PreferencesKeys.DAILY_AI_LIMIT] ?: 5,
                dailyAiUsed = prefs[PreferencesKeys.DAILY_AI_USED] ?: 0,
                onboardingCompleted = prefs[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
                userName = prefs[PreferencesKeys.USER_NAME],
                calendarConnected = prefs[PreferencesKeys.CALENDAR_CONNECTED] ?: false
            )
            
            val updated = update(current)
            
            prefs[PreferencesKeys.MORNING_BRIEFING_TIME] = UserPreferences.formatTime(updated.morningBriefingTime)
            prefs[PreferencesKeys.EVENING_SUMMARY_TIME] = UserPreferences.formatTime(updated.eveningSummaryTime)
            prefs[PreferencesKeys.BRIEFING_ENABLED] = updated.briefingEnabled
            prefs[PreferencesKeys.THEME_MODE] = updated.themeMode.toString()
            prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] = updated.notificationsEnabled
            prefs[PreferencesKeys.REMINDER_ADVANCE_MINUTES] = updated.reminderAdvanceMinutes
            prefs[PreferencesKeys.AI_MODEL_ID] = updated.aiModelId
            prefs[PreferencesKeys.AI_MODEL_DOWNLOADED] = updated.aiModelDownloaded
            prefs[PreferencesKeys.AI_CLASSIFICATION_ENABLED] = updated.aiClassificationEnabled
            prefs[PreferencesKeys.DAILY_AI_LIMIT] = updated.dailyAiLimit
            prefs[PreferencesKeys.DAILY_AI_USED] = updated.dailyAiUsed
            prefs[PreferencesKeys.ONBOARDING_COMPLETED] = updated.onboardingCompleted
            updated.userName?.let { prefs[PreferencesKeys.USER_NAME] = it }
            prefs[PreferencesKeys.CALENDAR_CONNECTED] = updated.calendarConnected
        }
    }
    
    /**
     * Clear all preferences (for logout/reset).
     */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}

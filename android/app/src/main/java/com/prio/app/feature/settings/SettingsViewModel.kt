package com.prio.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prio.core.common.model.ThemeMode
import com.prio.core.common.model.UserPreferences
import com.prio.core.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences.DEFAULT,
    val isLoading: Boolean = true
)

sealed interface SettingsEvent {
    // Theme
    data class SetThemeMode(val mode: ThemeMode) : SettingsEvent

    // Notifications
    data class SetNotificationsEnabled(val enabled: Boolean) : SettingsEvent
    data class SetReminderAdvanceMinutes(val minutes: Int) : SettingsEvent

    // Briefings
    data class SetBriefingEnabled(val enabled: Boolean) : SettingsEvent
    data class SetMorningBriefingTime(val time: String) : SettingsEvent
    data class SetEveningSummaryTime(val time: String) : SettingsEvent

    // AI
    data class SetAiClassificationEnabled(val enabled: Boolean) : SettingsEvent

    // Data
    data object ExportAllData : SettingsEvent
    data object DeleteAllData : SettingsEvent
    data object ResetOnboarding : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userPreferences.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs, isLoading = false) }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsEvent.SetThemeMode -> {
                    userPreferencesRepository.setThemeMode(event.mode.toString())
                }

                is SettingsEvent.SetNotificationsEnabled -> {
                    userPreferencesRepository.setNotificationsEnabled(event.enabled)
                }

                is SettingsEvent.SetReminderAdvanceMinutes -> {
                    userPreferencesRepository.setReminderAdvanceMinutes(event.minutes)
                }

                is SettingsEvent.SetBriefingEnabled -> {
                    userPreferencesRepository.setBriefingEnabled(event.enabled)
                }

                is SettingsEvent.SetMorningBriefingTime -> {
                    userPreferencesRepository.setMorningBriefingTime(event.time)
                }

                is SettingsEvent.SetEveningSummaryTime -> {
                    userPreferencesRepository.setEveningSummaryTime(event.time)
                }

                is SettingsEvent.SetAiClassificationEnabled -> {
                    userPreferencesRepository.setAiClassificationEnabled(event.enabled)
                }

                is SettingsEvent.ExportAllData -> {
                    Timber.d("Export all data requested")
                    // TODO: Implement data export
                }

                is SettingsEvent.DeleteAllData -> {
                    Timber.d("Delete all data requested")
                    userPreferencesRepository.clearAll()
                }

                is SettingsEvent.ResetOnboarding -> {
                    userPreferencesRepository.setOnboardingCompleted(false)
                }
            }
        }
    }
}

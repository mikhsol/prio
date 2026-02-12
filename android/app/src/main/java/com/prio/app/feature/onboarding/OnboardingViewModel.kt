package com.prio.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prio.core.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 5,
    val permissionNotificationsGranted: Boolean = false,
    val permissionCalendarGranted: Boolean = false,
    val permissionMicrophoneGranted: Boolean = false,
    val firstTaskText: String = "",
    val isCreatingTask: Boolean = false,
    val taskCreated: Boolean = false,
    val createdTaskQuadrant: String? = null
)

sealed interface OnboardingEvent {
    data object NextPage : OnboardingEvent
    data object PreviousPage : OnboardingEvent
    data class GoToPage(val page: Int) : OnboardingEvent
    data object SkipOnboarding : OnboardingEvent
    data object CompleteOnboarding : OnboardingEvent
    data class UpdateFirstTaskText(val text: String) : OnboardingEvent
    data object CreateFirstTask : OnboardingEvent
    data object SkipFirstTask : OnboardingEvent
    data class NotificationPermissionResult(val granted: Boolean) : OnboardingEvent
    data class CalendarPermissionResult(val granted: Boolean) : OnboardingEvent
    data class MicrophonePermissionResult(val granted: Boolean) : OnboardingEvent
}

sealed interface OnboardingEffect {
    data object NavigateToDashboard : OnboardingEffect
    data object RequestNotificationPermission : OnboardingEffect
    data object RequestCalendarPermission : OnboardingEffect
    data object RequestMicrophonePermission : OnboardingEffect
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effect = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.NextPage -> {
                _uiState.update { state ->
                    if (state.currentPage < state.totalPages - 1) {
                        state.copy(currentPage = state.currentPage + 1)
                    } else {
                        state
                    }
                }
            }

            is OnboardingEvent.PreviousPage -> {
                _uiState.update { state ->
                    if (state.currentPage > 0) {
                        state.copy(currentPage = state.currentPage - 1)
                    } else {
                        state
                    }
                }
            }

            is OnboardingEvent.GoToPage -> {
                _uiState.update { it.copy(currentPage = event.page.coerceIn(0, it.totalPages - 1)) }
            }

            is OnboardingEvent.SkipOnboarding -> {
                completeOnboarding()
            }

            is OnboardingEvent.CompleteOnboarding -> {
                completeOnboarding()
            }

            is OnboardingEvent.UpdateFirstTaskText -> {
                _uiState.update { it.copy(firstTaskText = event.text) }
            }

            is OnboardingEvent.CreateFirstTask -> {
                createFirstTask()
            }

            is OnboardingEvent.SkipFirstTask -> {
                completeOnboarding()
            }

            is OnboardingEvent.NotificationPermissionResult -> {
                _uiState.update { it.copy(permissionNotificationsGranted = event.granted) }
            }

            is OnboardingEvent.CalendarPermissionResult -> {
                _uiState.update { it.copy(permissionCalendarGranted = event.granted) }
            }

            is OnboardingEvent.MicrophonePermissionResult -> {
                _uiState.update { it.copy(permissionMicrophoneGranted = event.granted) }
            }
        }
    }

    private fun createFirstTask() {
        val text = _uiState.value.firstTaskText.trim()
        if (text.isEmpty()) return

        _uiState.update { it.copy(isCreatingTask = true) }

        viewModelScope.launch {
            try {
                // Simulate task creation — the actual task creation uses QuickCaptureViewModel
                // For onboarding, we just show the celebration and mark complete
                kotlinx.coroutines.delay(500)
                _uiState.update {
                    it.copy(
                        isCreatingTask = false,
                        taskCreated = true,
                        createdTaskQuadrant = "DO FIRST"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create first task")
                _uiState.update { it.copy(isCreatingTask = false) }
            }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(true)
            _effect.send(OnboardingEffect.NavigateToDashboard)
        }
    }
}

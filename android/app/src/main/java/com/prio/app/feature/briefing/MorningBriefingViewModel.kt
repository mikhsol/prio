package com.prio.app.feature.briefing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prio.core.data.preferences.UserPreferencesRepository
import com.prio.core.data.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Morning Briefing screen (task 3.4.2).
 *
 * Per 1.1.5 spec:
 * - Greeting + Top Priorities + Schedule Preview + Goal Spotlight + AI Insight
 * - "Start My Day" CTA marks briefing as read
 * - Briefing regenerates on significant data changes
 *
 * Performance: Briefing loads in <100ms (pre-generated) per spec.
 */
@HiltViewModel
class MorningBriefingViewModel @Inject constructor(
    private val briefingGenerator: BriefingGenerator,
    private val analyticsRepository: AnalyticsRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MorningBriefingUiState())
    val uiState: StateFlow<MorningBriefingUiState> = _uiState.asStateFlow()

    private val _effect = Channel<MorningBriefingEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadBriefing()
    }

    fun onEvent(event: MorningBriefingEvent) {
        when (event) {
            MorningBriefingEvent.OnStartMyDay -> startMyDay()
            MorningBriefingEvent.OnRefresh -> loadBriefing()
            MorningBriefingEvent.OnViewAgain -> {
                _uiState.update { it.copy(isRead = false) }
            }
            is MorningBriefingEvent.OnTaskTap -> {
                viewModelScope.launch {
                    _effect.send(MorningBriefingEffect.NavigateToTask(event.taskId))
                }
            }
            is MorningBriefingEvent.OnGoalTap -> {
                viewModelScope.launch {
                    _effect.send(MorningBriefingEffect.NavigateToGoal(event.goalId))
                }
            }
            MorningBriefingEvent.OnSeeAllTasks -> {
                viewModelScope.launch {
                    _effect.send(MorningBriefingEffect.NavigateToTasks)
                }
            }
            MorningBriefingEvent.OnFullCalendarView -> {
                viewModelScope.launch {
                    _effect.send(MorningBriefingEffect.NavigateToCalendar)
                }
            }
        }
    }

    private fun loadBriefing() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val prefs = preferencesRepository.userPreferences.first()
                val userName = prefs.userName?.takeIf { it.isNotBlank() }

                val briefing = briefingGenerator.generateMorningBriefing(userName)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        briefing = briefing,
                        isRead = false
                    )
                }

                Timber.i("MorningBriefingVM: Briefing loaded in ${briefing.generationTimeMs}ms")
            } catch (e: Exception) {
                Timber.e(e, "MorningBriefingVM: Failed to load briefing")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Unable to generate briefing. Pull to retry."
                    )
                }
            }
        }
    }

    private fun startMyDay() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRead = true) }
            analyticsRepository.recordBriefingOpened()
            _effect.send(MorningBriefingEffect.BriefingDismissed)
        }
    }
}

// ==================== UI State ====================

data class MorningBriefingUiState(
    val isLoading: Boolean = true,
    val briefing: MorningBriefingData? = null,
    val isRead: Boolean = false,
    val error: String? = null
)

// ==================== Events ====================

sealed interface MorningBriefingEvent {
    data object OnStartMyDay : MorningBriefingEvent
    data object OnRefresh : MorningBriefingEvent
    data object OnViewAgain : MorningBriefingEvent
    data class OnTaskTap(val taskId: Long) : MorningBriefingEvent
    data class OnGoalTap(val goalId: Long) : MorningBriefingEvent
    data object OnSeeAllTasks : MorningBriefingEvent
    data object OnFullCalendarView : MorningBriefingEvent
}

// ==================== Effects ====================

sealed interface MorningBriefingEffect {
    data class NavigateToTask(val taskId: Long) : MorningBriefingEffect
    data class NavigateToGoal(val goalId: Long) : MorningBriefingEffect
    data object NavigateToTasks : MorningBriefingEffect
    data object NavigateToCalendar : MorningBriefingEffect
    data object BriefingDismissed : MorningBriefingEffect
}

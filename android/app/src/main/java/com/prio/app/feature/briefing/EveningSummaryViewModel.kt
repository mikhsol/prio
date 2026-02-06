package com.prio.app.feature.briefing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.data.repository.AnalyticsRepository
import com.prio.core.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Evening Summary screen (task 3.4.3).
 *
 * Per 1.1.7 Evening Summary spec:
 * - Accomplishments list
 * - Not Done with Move/Reschedule/Drop actions
 * - Goal progress delta
 * - Tomorrow preview
 * - AI reflection
 * - "Close Day & Plan Tomorrow" CTA
 *
 * Close Day flow:
 * 1. Apply task decisions (move to tomorrow, reschedule, drop)
 * 2. Animation + haptic feedback
 * 3. Mark summary as read
 */
@HiltViewModel
class EveningSummaryViewModel @Inject constructor(
    private val briefingGenerator: BriefingGenerator,
    private val taskRepository: TaskRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(EveningSummaryUiState())
    val uiState: StateFlow<EveningSummaryUiState> = _uiState.asStateFlow()

    private val _effect = Channel<EveningSummaryEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadSummary()
    }

    fun onEvent(event: EveningSummaryEvent) {
        when (event) {
            EveningSummaryEvent.OnCloseDay -> closeDay()
            EveningSummaryEvent.OnRefresh -> loadSummary()
            is EveningSummaryEvent.OnTaskActionChanged -> updateTaskAction(event.taskId, event.action)
            is EveningSummaryEvent.OnTaskTap -> {
                viewModelScope.launch {
                    _effect.send(EveningSummaryEffect.NavigateToTask(event.taskId))
                }
            }
            is EveningSummaryEvent.OnGoalTap -> {
                viewModelScope.launch {
                    _effect.send(EveningSummaryEffect.NavigateToGoal(event.goalId))
                }
            }
        }
    }

    private fun loadSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val summary = briefingGenerator.generateEveningSummary()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        summary = summary
                    )
                }
                Timber.i("EveningSummaryVM: Summary loaded in ${summary.generationTimeMs}ms")
            } catch (e: Exception) {
                Timber.e(e, "EveningSummaryVM: Failed to load summary")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Unable to generate summary. Pull to retry."
                    )
                }
            }
        }
    }

    /**
     * Update the action for a not-done task.
     * Per 1.1.7 spec: Move to tomorrow (default), Reschedule, Drop.
     */
    private fun updateTaskAction(taskId: Long, action: NotDoneAction) {
        _uiState.update { state ->
            val updated = state.summary?.copy(
                notDoneTasks = state.summary.notDoneTasks.map { task ->
                    if (task.id == taskId) task.copy(selectedAction = action) else task
                }
            )
            state.copy(summary = updated)
        }
    }

    /**
     * Close Day: apply all task decisions and mark day as closed.
     *
     * Per 1.1.7 Close Day flow:
     * 1. Move selected tasks to tomorrow
     * 2. Reschedule as specified
     * 3. Mark dropped tasks as Q4
     * 4. Animation + haptic
     * 5. Dashboard shows "Day closed ✓"
     */
    private fun closeDay() {
        viewModelScope.launch {
            val summary = _uiState.value.summary ?: return@launch

            _uiState.update { it.copy(isClosingDay = true) }

            try {
                val timeZone = TimeZone.currentSystemDefault()
                val now = clock.now()
                val today = now.toLocalDateTime(timeZone).date
                val tomorrowStart = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)

                for (task in summary.notDoneTasks) {
                    when (task.selectedAction) {
                        NotDoneAction.MOVE_TO_TOMORROW -> {
                            // Move due date to tomorrow, same time or start of day
                            taskRepository.updateTaskDueDate(task.id, tomorrowStart)
                            Timber.d("Moved task ${task.id} to tomorrow")
                        }
                        NotDoneAction.RESCHEDULE -> {
                            // For now, also move to tomorrow (full date picker in future)
                            taskRepository.updateTaskDueDate(task.id, tomorrowStart)
                            Timber.d("Rescheduled task ${task.id} to tomorrow")
                        }
                        NotDoneAction.DROP -> {
                            // Move to Q4 (Maybe Later) with no date per spec
                            taskRepository.updateTaskQuadrant(task.id, EisenhowerQuadrant.ELIMINATE)
                            taskRepository.updateTaskDueDate(task.id, null)
                            Timber.d("Dropped task ${task.id} to Q4")
                        }
                    }
                }

                // Record analytics
                analyticsRepository.recordSummaryOpened()

                // Update state
                _uiState.update {
                    it.copy(
                        isClosingDay = false,
                        summary = it.summary?.copy(isDayClosed = true)
                    )
                }

                // Trigger animation effect
                _effect.send(EveningSummaryEffect.DayClosedAnimation)

                Timber.i("EveningSummaryVM: Day closed, ${summary.notDoneTasks.size} tasks processed")
            } catch (e: Exception) {
                Timber.e(e, "EveningSummaryVM: Failed to close day")
                _uiState.update { it.copy(isClosingDay = false) }
                _effect.send(EveningSummaryEffect.ShowError("Failed to close day. Try again."))
            }
        }
    }
}

// ==================== UI State ====================

data class EveningSummaryUiState(
    val isLoading: Boolean = true,
    val summary: EveningSummaryData? = null,
    val isClosingDay: Boolean = false,
    val error: String? = null
)

// ==================== Events ====================

sealed interface EveningSummaryEvent {
    data object OnCloseDay : EveningSummaryEvent
    data object OnRefresh : EveningSummaryEvent
    data class OnTaskActionChanged(val taskId: Long, val action: NotDoneAction) : EveningSummaryEvent
    data class OnTaskTap(val taskId: Long) : EveningSummaryEvent
    data class OnGoalTap(val goalId: Long) : EveningSummaryEvent
}

// ==================== Effects ====================

sealed interface EveningSummaryEffect {
    data class NavigateToTask(val taskId: Long) : EveningSummaryEffect
    data class NavigateToGoal(val goalId: Long) : EveningSummaryEffect
    data object DayClosedAnimation : EveningSummaryEffect
    data class ShowError(val message: String) : EveningSummaryEffect
}

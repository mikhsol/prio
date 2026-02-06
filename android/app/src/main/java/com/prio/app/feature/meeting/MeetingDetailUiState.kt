package com.prio.app.feature.meeting

import com.prio.core.data.local.entity.ActionItem

/**
 * UI state for MeetingDetailScreen.
 * 
 * Covers tasks 3.3.3 (detail), 3.3.4 (notes), 3.3.5 (action items), 3.3.6 (agenda).
 */
data class MeetingDetailUiState(
    val isLoading: Boolean = true,
    val meetingId: Long = 0,
    val title: String = "",
    val description: String? = null,
    val location: String? = null,
    val startTimeFormatted: String = "",
    val endTimeFormatted: String = "",
    val dateFormatted: String = "",
    val durationFormatted: String = "",
    val attendees: List<String> = emptyList(),
    val isInProgress: Boolean = false,
    val isPast: Boolean = false,

    // 3.3.4 Notes
    val notes: String = "",
    val isNotesEditing: Boolean = false,
    val notesSaved: Boolean = true,

    // 3.3.5 Action items
    val actionItems: List<ActionItemUiModel> = emptyList(),
    val isExtractingActionItems: Boolean = false,

    // 3.3.6 Agenda
    val agenda: String = "",
    val isAgendaEditing: Boolean = false,
    val agendaSaved: Boolean = true,
)

/**
 * UI model for a single action item.
 */
data class ActionItemUiModel(
    val index: Int,
    val description: String,
    val assignee: String?,
    val isCompleted: Boolean,
    val linkedTaskId: Long?,
)

// ==================== Events ====================

sealed interface MeetingDetailEvent {
    // Notes
    data class OnNotesChanged(val notes: String) : MeetingDetailEvent
    data object OnSaveNotes : MeetingDetailEvent
    data object OnToggleNotesEdit : MeetingDetailEvent

    // Action items
    data object OnExtractActionItems : MeetingDetailEvent
    data class OnToggleActionItemComplete(val index: Int) : MeetingDetailEvent
    data class OnConvertActionItemToTask(val index: Int) : MeetingDetailEvent

    // Agenda
    data class OnAgendaChanged(val agenda: String) : MeetingDetailEvent
    data object OnSaveAgenda : MeetingDetailEvent
    data object OnToggleAgendaEdit : MeetingDetailEvent

    // Navigation
    data object OnNavigateBack : MeetingDetailEvent
}

// ==================== Effects ====================

sealed interface MeetingDetailEffect {
    data object NavigateBack : MeetingDetailEffect
    data class ShowSnackbar(val message: String) : MeetingDetailEffect
    data class NavigateToTask(val taskId: Long) : MeetingDetailEffect
}

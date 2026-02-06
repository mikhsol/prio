package com.prio.app.feature.meeting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prio.core.data.local.entity.ActionItem
import com.prio.core.data.local.entity.MeetingEntity
import com.prio.core.data.repository.MeetingRepository
import com.prio.core.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for Meeting Detail screen.
 *
 * Handles:
 * - 3.3.3: Meeting detail display
 * - 3.3.4: Notes editing with auto-save (debounce 1s)
 * - 3.3.5: AI action item extraction (rule-based)
 * - 3.3.6: Agenda editing
 */
@HiltViewModel
class MeetingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val meetingRepository: MeetingRepository,
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) : ViewModel() {

    private val meetingId: Long = savedStateHandle["meetingId"] ?: 0L

    private val _uiState = MutableStateFlow(MeetingDetailUiState(meetingId = meetingId))
    val uiState: StateFlow<MeetingDetailUiState> = _uiState.asStateFlow()

    private val _effect = Channel<MeetingDetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var notesAutoSaveJob: Job? = null
    private var agendaAutoSaveJob: Job? = null

    init {
        loadMeeting()
    }

    fun onEvent(event: MeetingDetailEvent) {
        when (event) {
            // Notes
            is MeetingDetailEvent.OnNotesChanged -> onNotesChanged(event.notes)
            MeetingDetailEvent.OnSaveNotes -> saveNotes()
            MeetingDetailEvent.OnToggleNotesEdit -> toggleNotesEdit()

            // Action items
            MeetingDetailEvent.OnExtractActionItems -> extractActionItems()
            is MeetingDetailEvent.OnToggleActionItemComplete -> toggleActionItemComplete(event.index)
            is MeetingDetailEvent.OnConvertActionItemToTask -> convertToTask(event.index)

            // Agenda
            is MeetingDetailEvent.OnAgendaChanged -> onAgendaChanged(event.agenda)
            MeetingDetailEvent.OnSaveAgenda -> saveAgenda()
            MeetingDetailEvent.OnToggleAgendaEdit -> toggleAgendaEdit()

            // Nav
            MeetingDetailEvent.OnNavigateBack -> {
                viewModelScope.launch { _effect.send(MeetingDetailEffect.NavigateBack) }
            }
        }
    }

    // ==================== Load ====================

    private fun loadMeeting() {
        viewModelScope.launch {
            meetingRepository.getMeetingByIdFlow(meetingId).collect { entity ->
                if (entity != null) {
                    updateStateFromEntity(entity)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun updateStateFromEntity(entity: MeetingEntity) {
        val tz = TimeZone.currentSystemDefault()
        val now = clock.now()

        val startLocal = entity.startTime.toLocalDateTime(tz)
        val endLocal = entity.endTime.toLocalDateTime(tz)

        val dateStr = "${startLocal.monthNumber}/${startLocal.dayOfMonth}/${startLocal.year}"
        val startStr = formatTime(startLocal.hour, startLocal.minute)
        val endStr = formatTime(endLocal.hour, endLocal.minute)

        val durationMinutes = ((entity.endTime - entity.startTime).inWholeMinutes).toInt()
        val durationStr = if (durationMinutes >= 60) {
            "${durationMinutes / 60}h ${durationMinutes % 60}m"
        } else {
            "${durationMinutes}m"
        }

        val isInProgress = now >= entity.startTime && now < entity.endTime
        val isPast = now >= entity.endTime

        val attendeesList = entity.attendees
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        val actionItems = entity.actionItems?.let { json ->
            try {
                ActionItem.listFromJson(json).mapIndexed { idx, item ->
                    ActionItemUiModel(
                        index = idx,
                        description = item.description,
                        assignee = item.assignee,
                        isCompleted = item.isCompleted,
                        linkedTaskId = item.linkedTaskId,
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        } ?: emptyList()

        _uiState.update { current ->
            current.copy(
                isLoading = false,
                title = entity.title,
                description = entity.description,
                location = entity.location,
                startTimeFormatted = startStr,
                endTimeFormatted = endStr,
                dateFormatted = dateStr,
                durationFormatted = durationStr,
                attendees = attendeesList,
                isInProgress = isInProgress,
                isPast = isPast,
                notes = if (!current.isNotesEditing) entity.notes ?: "" else current.notes,
                actionItems = actionItems,
                agenda = if (!current.isAgendaEditing) entity.agenda ?: "" else current.agenda,
            )
        }
    }

    // ==================== Notes (3.3.4) ====================

    private fun toggleNotesEdit() {
        _uiState.update { it.copy(isNotesEditing = !it.isNotesEditing) }
    }

    private fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes, notesSaved = false) }
        // Auto-save with 1 second debounce
        notesAutoSaveJob?.cancel()
        notesAutoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DEBOUNCE_MS)
            saveNotesInternal()
        }
    }

    private fun saveNotes() {
        notesAutoSaveJob?.cancel()
        viewModelScope.launch { saveNotesInternal() }
    }

    private suspend fun saveNotesInternal() {
        val notes = _uiState.value.notes
        meetingRepository.updateNotes(meetingId, notes)
        _uiState.update { it.copy(notesSaved = true) }
    }

    // ==================== Action Items (3.3.5) ====================

    /**
     * Rule-based action item extraction from meeting notes.
     *
     * Parses notes for sentences starting with action verbs ("action:", "todo:",
     * "follow up:", dash/bullet items with action verbs, assignments like "@Name").
     * Results are stored as [ActionItem] in the meeting entity.
     */
    private fun extractActionItems() {
        val notes = _uiState.value.notes
        if (notes.isBlank()) {
            viewModelScope.launch {
                _effect.send(MeetingDetailEffect.ShowSnackbar("Add notes first to extract action items."))
            }
            return
        }

        _uiState.update { it.copy(isExtractingActionItems = true) }

        viewModelScope.launch {
            val extracted = extractActionItemsFromText(notes)
            if (extracted.isEmpty()) {
                _effect.send(MeetingDetailEffect.ShowSnackbar("No action items found in notes."))
                _uiState.update { it.copy(isExtractingActionItems = false) }
                return@launch
            }

            meetingRepository.addActionItems(meetingId, extracted)
            _effect.send(
                MeetingDetailEffect.ShowSnackbar("Extracted ${extracted.size} action item(s).")
            )
            _uiState.update { it.copy(isExtractingActionItems = false) }
        }
    }

    private fun toggleActionItemComplete(index: Int) {
        viewModelScope.launch {
            val items = meetingRepository.getActionItems(meetingId).toMutableList()
            if (index in items.indices) {
                items[index] = items[index].copy(isCompleted = !items[index].isCompleted)
                meetingRepository.updateActionItems(meetingId, items)
            }
        }
    }

    /**
     * Convert an action item to a Prio task.
     * Links the task ID back to the action item for traceability.
     */
    private fun convertToTask(actionItemIndex: Int) {
        viewModelScope.launch {
            val items = meetingRepository.getActionItems(meetingId)
            if (actionItemIndex !in items.indices) return@launch

            val item = items[actionItemIndex]
            val existingTaskId = item.linkedTaskId
            if (existingTaskId != null) {
                _effect.send(MeetingDetailEffect.NavigateToTask(existingTaskId))
                return@launch
            }

            val taskId = taskRepository.createTask(
                title = item.description,
                notes = "From meeting: ${_uiState.value.title}",
            )
            meetingRepository.linkActionItemToTask(meetingId, actionItemIndex, taskId)
            _effect.send(MeetingDetailEffect.ShowSnackbar("Task created from action item."))
        }
    }

    // ==================== Agenda (3.3.6) ====================

    private fun toggleAgendaEdit() {
        _uiState.update { it.copy(isAgendaEditing = !it.isAgendaEditing) }
    }

    private fun onAgendaChanged(agenda: String) {
        _uiState.update { it.copy(agenda = agenda, agendaSaved = false) }
        agendaAutoSaveJob?.cancel()
        agendaAutoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DEBOUNCE_MS)
            saveAgendaInternal()
        }
    }

    private fun saveAgenda() {
        agendaAutoSaveJob?.cancel()
        viewModelScope.launch { saveAgendaInternal() }
    }

    private suspend fun saveAgendaInternal() {
        val agenda = _uiState.value.agenda
        meetingRepository.updateAgenda(meetingId, agenda)
        _uiState.update { it.copy(agendaSaved = true) }
    }

    // ==================== Rule-Based Action Item Extraction ====================

    companion object {
        /** Auto-save debounce in milliseconds. */
        private const val AUTO_SAVE_DEBOUNCE_MS = 1_000L

        /**
         * Action verb patterns for rule-based extraction.
         * Each line starting with these (after trimming) is an action item.
         */
        private val ACTION_PREFIXES = listOf(
            "action:", "action item:", "todo:", "to-do:", "to do:",
            "follow up:", "follow-up:", "followup:",
            "task:", "assigned:", "assign:",
        )

        /** Bullet/dash line action verbs that indicate an action item. */
        private val ACTION_VERBS = setOf(
            "call", "email", "send", "create", "update", "review", "schedule",
            "prepare", "write", "draft", "complete", "finish", "submit", "fix",
            "contact", "notify", "remind", "follow up", "investigate", "research",
            "set up", "organize", "book", "arrange", "confirm", "check",
        )

        /**
         * Extract action items from free-form meeting notes using heuristics.
         *
         * Rules:
         * 1. Lines prefixed with "Action:", "TODO:", "Follow up:" etc.
         * 2. Bullet/dash lines starting with an action verb
         * 3. "@Name" in a line → assignee
         */
        fun extractActionItemsFromText(text: String): List<ActionItem> {
            val items = mutableListOf<ActionItem>()

            for (rawLine in text.lines()) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue

                val lower = line.lowercase()

                // Rule 1: Explicit action prefixes
                val matchedPrefix = ACTION_PREFIXES.firstOrNull { lower.startsWith(it) }
                if (matchedPrefix != null) {
                    val desc = line.substring(matchedPrefix.length).trim()
                    if (desc.isNotEmpty()) {
                        items += ActionItem(
                            description = desc,
                            assignee = extractAssignee(line),
                        )
                    }
                    continue
                }

                // Rule 2: Bullet/dash lines with action verbs
                val stripped = line.removePrefix("-").removePrefix("*").removePrefix("•").trim()
                val strippedLower = stripped.lowercase()
                if (stripped != line && ACTION_VERBS.any { strippedLower.startsWith(it) }) {
                    items += ActionItem(
                        description = stripped,
                        assignee = extractAssignee(stripped),
                    )
                }
            }

            return items
        }

        /** Extract @Name assignee from text. */
        private fun extractAssignee(text: String): String? {
            val atMatch = Regex("@(\\w+)").find(text) ?: return null
            return atMatch.groupValues[1]
        }
    }

    // ==================== Formatting ====================

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return if (minute == 0) "$h $amPm" else "$h:${minute.toString().padStart(2, '0')} $amPm"
    }
}

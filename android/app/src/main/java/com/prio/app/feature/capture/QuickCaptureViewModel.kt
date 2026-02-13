package com.prio.app.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prio.app.feature.capture.voice.VoiceInputState
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.data.repository.GoalRepository
import com.prio.core.data.repository.TaskRepository
import com.prio.core.domain.eisenhower.EisenhowerEngine
import com.prio.core.domain.parser.NaturalLanguageParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * ViewModel for QuickCaptureSheet.
 * 
 * Implements 3.1.5 from ACTION_PLAN.md:
 * - Natural language parsing with AI
 * - Rule-based classification (primary, <50ms)
 * - Voice input support
 * - <5s total flow target
 * - Offline-capable
 */
@HiltViewModel
class QuickCaptureViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository,
    private val eisenhowerEngine: EisenhowerEngine,
    private val naturalLanguageParser: NaturalLanguageParser,
    private val clock: Clock
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(QuickCaptureUiState())
    val uiState: StateFlow<QuickCaptureUiState> = _uiState.asStateFlow()
    
    private val _effect = Channel<QuickCaptureEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    
    // Debounce job for parsing - longer delay to not interrupt typing
    private var parseJob: Job? = null
    private val PARSE_DEBOUNCE_MS = 800L
    
    /**
     * Handle UI events.
     */
    fun onEvent(event: QuickCaptureEvent) {
        when (event) {
            is QuickCaptureEvent.UpdateInput -> updateInput(event.text)
            is QuickCaptureEvent.StartVoiceInput -> startVoiceInput()
            is QuickCaptureEvent.StopVoiceInput -> stopVoiceInput()
            is QuickCaptureEvent.RetryVoiceInput -> retryVoiceInput()
            is QuickCaptureEvent.CancelVoiceInput -> cancelVoiceInput()
            is QuickCaptureEvent.ParseInput -> parseInput()
            is QuickCaptureEvent.UpdateParsedTitle -> updateParsedTitle(event.title)
            is QuickCaptureEvent.UpdateParsedQuadrant -> updateParsedQuadrant(event.quadrant)
            is QuickCaptureEvent.UpdateParsedDueDate -> updateParsedDueDate(event.dateMillis)
            is QuickCaptureEvent.ToggleDatePicker -> toggleDatePicker()
            is QuickCaptureEvent.ToggleTimePicker -> toggleTimePicker()
            is QuickCaptureEvent.SetPendingDate -> setPendingDate(event.dateMillis)
            is QuickCaptureEvent.UpdateParsedDueDateTime -> updateParsedDueDateTime(event.dateMillis, event.hour, event.minute)
            is QuickCaptureEvent.ToggleGoalPicker -> toggleGoalPicker()
            is QuickCaptureEvent.SelectGoal -> selectGoal(event.goalId, event.goalTitle)
            is QuickCaptureEvent.AddSuggestionToInput -> addSuggestion(event.suggestion)
            is QuickCaptureEvent.CreateTask -> createTask()
            is QuickCaptureEvent.OpenEditDetails -> openEditDetails()
            is QuickCaptureEvent.Dismiss -> dismiss()
            is QuickCaptureEvent.Reset -> reset()
        }
    }
    
    private fun updateInput(text: String) {
        // Collapse the preview card when the user resumes typing.
        // This prevents the parsed-result + action-buttons content from
        // pushing the input field off-screen when the keyboard is open.
        _uiState.update { it.copy(inputText = text, showPreview = false) }
        
        // Debounce parsing - cancel previous and wait for typing to stop
        parseJob?.cancel()
        if (text.isNotBlank()) {
            parseJob = viewModelScope.launch {
                delay(PARSE_DEBOUNCE_MS)
                // Only parse if text hasn't changed during delay (user stopped typing)
                if (_uiState.value.inputText == text) {
                    // Inline silent parsing (no separate coroutine launch)
                    performParsing(text, silent = true)
                }
            }
        } else {
            _uiState.update { it.copy(parsedResult = null, showPreview = false) }
        }
    }
    
    private fun startVoiceInput() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isVoiceInputActive = true,
                    voiceState = VoiceInputState.Initializing
                ) 
            }
            _effect.send(QuickCaptureEffect.StartVoiceRecognition)
        }
    }
    
    private fun stopVoiceInput() {
        _uiState.update { 
            it.copy(
                isVoiceInputActive = false,
                voiceState = VoiceInputState.Idle
            ) 
        }
    }
    
    /**
     * Retry voice input after an error.
     * Resets voice state and re-triggers recognition.
     */
    private fun retryVoiceInput() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isVoiceInputActive = true,
                    voiceState = VoiceInputState.Initializing
                )
            }
            _effect.send(QuickCaptureEffect.StartVoiceRecognition)
        }
    }
    
    /**
     * Cancel voice mode and return to keyboard typing.
     */
    private fun cancelVoiceInput() {
        _uiState.update { 
            it.copy(
                isVoiceInputActive = false,
                voiceState = VoiceInputState.Idle
            )
        }
    }
    
    /**
     * Called when voice recognition produces text.
     */
    fun onVoiceResult(text: String) {
        _uiState.update { 
            it.copy(
                inputText = text,
                isVoiceInputActive = false,
                voiceState = VoiceInputState.Idle
            )
        }
        parseInput()
    }
    
    /**
     * Update the voice state from the VoiceInputManager.
     * Called by the hosting composable that manages the VoiceInputManager lifecycle.
     */
    fun updateVoiceState(voiceState: VoiceInputState) {
        _uiState.update { it.copy(voiceState = voiceState) }
        
        // Auto-handle terminal states
        when (voiceState) {
            is VoiceInputState.Result -> {
                onVoiceResult(voiceState.text)
            }
            is VoiceInputState.Error -> {
                // Keep voice active so the error UI shows with retry/type-instead buttons
                _uiState.update { it.copy(isVoiceInputActive = true) }
            }
            else -> { /* Non-terminal states: just update UI */ }
        }
    }
    
    /**
     * Parse input with loading indicator - used for explicit parse action (Done button).
     */
    private fun parseInput() {
        val input = _uiState.value.inputText.trim()
        if (input.isBlank()) return
        
        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            _uiState.update { it.copy(isAiParsing = true) }
            performParsing(input)
        }
    }
    
    /**
     * Common parsing logic used by parseInput and debounced updateInput.
     *
     * @param input The text to parse
     * @param silent When true (background debounce), stores the result but does NOT
     *   expand the preview card. This prevents the bottom sheet from resizing while
     *   the user is still typing with the keyboard open.
     */
    private suspend fun performParsing(input: String, silent: Boolean = false) {
        try {
            // Parse natural language input
            val parsed = naturalLanguageParser.parse(input)
            
            // Classify with Eisenhower engine (rule-based, <50ms)
            val classification = eisenhowerEngine.classify(
                taskText = parsed.title,
                dueDate = parsed.dueDate
            )
            
            // Find suggested goal based on content, but preserve preselected goal
            val currentState = _uiState.value
            val suggestedGoal = findSuggestedGoal(parsed.title)
                ?: currentState.parsedResult?.suggestedGoal  // preserve preselected/previous goal
            
            val result = ParsedTaskResult(
                title = parsed.title,
                dueDate = parsed.dueDate?.toString(),
                dueDateFormatted = parsed.dueDate?.let { formatDueDate(it) },
                dueTime = parsed.dueTime,
                quadrant = classification.quadrant,
                aiExplanation = classification.explanation,
                suggestedGoal = suggestedGoal,
                confidence = classification.confidence
            )
            
            // Record AI classification event for analytics (Milestone 3.5.1)
            try { taskRepository.recordAiClassification() } catch (_: Exception) {}
            
            _uiState.update { 
                it.copy(
                    isAiParsing = false,
                    parsedResult = result,
                    showPreview = if (silent) it.showPreview else true
                )
            }
        } catch (e: Exception) {
            // Fallback: use input as title, default quadrant
            val fallbackResult = ParsedTaskResult(
                title = input,
                quadrant = EisenhowerQuadrant.ELIMINATE,
                aiExplanation = "AI will classify in background"
            )
            
            _uiState.update {
                it.copy(
                    isAiParsing = false,
                    parsedResult = fallbackResult,
                    showPreview = if (silent) it.showPreview else true
                )
            }
        }
    }
    
    private suspend fun findSuggestedGoal(title: String): SuggestedGoal? {
        return try {
            // Get active goals and find best match
            val goals = goalRepository.getAllActiveGoals().firstOrNull() ?: emptyList()
            
            // Simple keyword matching (can be enhanced with ML)
            val keywords = title.lowercase().split(" ")
            
            goals.firstOrNull { goal ->
                keywords.any { keyword ->
                    goal.title.lowercase().contains(keyword) ||
                    goal.description?.lowercase()?.contains(keyword) == true
                }
            }?.let { goal ->
                SuggestedGoal(
                    id = goal.id,
                    title = goal.title,
                    reason = "Contains '${keywords.find { goal.title.lowercase().contains(it) }}'"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun updateParsedTitle(title: String) {
        _uiState.update { current ->
            current.copy(
                parsedResult = current.parsedResult?.copy(title = title)
            )
        }
    }
    
    private fun updateParsedQuadrant(quadrant: EisenhowerQuadrant) {
        _uiState.update { current ->
            current.copy(
                parsedResult = current.parsedResult?.copy(
                    quadrant = quadrant,
                    aiExplanation = "Manually set by user"
                )
            )
        }
        // Record AI override event when user changes AI-suggested quadrant (Milestone 3.5.1)
        viewModelScope.launch {
            try { taskRepository.recordAiOverride() } catch (_: Exception) {}
        }
    }

    /**
     * Toggle date picker visibility (3.1.5.B.4).
     */
    private fun toggleDatePicker() {
        _uiState.update { it.copy(showDatePicker = !it.showDatePicker) }
    }

    /**
     * Toggle time picker visibility (step 2 after date picker).
     */
    private fun toggleTimePicker() {
        _uiState.update { it.copy(showTimePicker = !it.showTimePicker) }
    }

    /**
     * Store pending date millis while time picker is shown.
     */
    private fun setPendingDate(dateMillis: Long?) {
        _uiState.update { it.copy(pendingDateMillis = dateMillis) }
    }

    /**
     * Update parsed due date+time from combined date and time pickers.
     * Combines the date millis with hour/minute offset.
     */
    private fun updateParsedDueDateTime(dateMillis: Long?, hour: Int, minute: Int) {
        _uiState.update { current ->
            if (dateMillis == null) {
                current.copy(
                    parsedResult = current.parsedResult?.copy(
                        dueDate = null,
                        dueDateFormatted = null,
                        dueTime = null
                    ),
                    pendingDateMillis = null
                )
            } else {
                val timeOffsetMillis = (hour * 3600_000L) + (minute * 60_000L)
                val combinedMillis = dateMillis + timeOffsetMillis
                val instant = Instant.fromEpochMilliseconds(combinedMillis)
                val formatted = formatDueDate(instant)
                val timeStr = String.format("%02d:%02d", hour, minute)
                current.copy(
                    parsedResult = current.parsedResult?.copy(
                        dueDate = instant.toString(),
                        dueDateFormatted = formatted,
                        dueTime = timeStr
                    ),
                    pendingDateMillis = null
                )
            }
        }
    }

    /**
     * Update parsed due date from date picker selection (3.1.5.B.4).
     * Converts epoch millis to formatted date string.
     */
    private fun updateParsedDueDate(dateMillis: Long?) {
        _uiState.update { current ->
            if (dateMillis == null) {
                current.copy(
                    parsedResult = current.parsedResult?.copy(
                        dueDate = null,
                        dueDateFormatted = null
                    )
                )
            } else {
                val instant = Instant.fromEpochMilliseconds(dateMillis)
                val formatted = formatDueDate(instant)
                current.copy(
                    parsedResult = current.parsedResult?.copy(
                        dueDate = instant.toString(),
                        dueDateFormatted = formatted
                    )
                )
            }
        }
    }

    /**
     * Toggle goal picker visibility and load goals (3.1.5.B.5).
     */
    private fun toggleGoalPicker() {
        val willShow = !_uiState.value.showGoalPicker
        if (willShow) {
            viewModelScope.launch {
                val goals = goalRepository.getAllActiveGoals().firstOrNull() ?: emptyList()
                val pickerItems = goals.map { goal ->
                    GoalPickerItem(
                        id = goal.id,
                        title = goal.title,
                        progress = goal.progress,
                        category = goal.category.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        emoji = "\uD83C\uDFAF"
                    )
                }
                _uiState.update {
                    it.copy(
                        showGoalPicker = true,
                        availableGoals = pickerItems
                    )
                }
            }
        } else {
            _uiState.update { it.copy(showGoalPicker = false) }
        }
    }

    /**
     * Select a goal to link to the parsed task (3.1.5.B.5).
     */
    private fun selectGoal(goalId: Long?, goalTitle: String?) {
        _uiState.update { current ->
            val suggestedGoal = if (goalId != null && goalTitle != null) {
                SuggestedGoal(
                    id = goalId,
                    title = goalTitle,
                    reason = "Selected by user"
                )
            } else {
                null
            }
            current.copy(
                parsedResult = current.parsedResult?.copy(
                    suggestedGoal = suggestedGoal
                )
            )
        }
    }
    
    private fun addSuggestion(suggestion: String) {
        val currentInput = _uiState.value.inputText
        val newInput = if (currentInput.isBlank()) {
            suggestion
        } else {
            "$currentInput $suggestion"
        }
        _uiState.update { it.copy(inputText = newInput) }
    }
    
    private fun createTask() {
        val result = _uiState.value.parsedResult ?: return
        val preselectedGoalId = _uiState.value.preselectedGoalId
        
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            
            try {
                // Parse due date if present
                val dueDate = result.dueDate?.let { parseDueDate(it) }
                
                // Use suggestedGoal if available, fall back to preselectedGoalId
                val goalId = result.suggestedGoal?.id ?: preselectedGoalId

                // Create task
                val taskId = taskRepository.createTask(
                    title = result.title,
                    notes = null,
                    dueDate = dueDate,
                    quadrant = result.quadrant,
                    aiExplanation = result.aiExplanation,
                    goalId = goalId
                )
                
                _uiState.update { 
                    it.copy(
                        isCreating = false,
                        isCreated = true,
                        // Clear form fields so stale data doesn't show on next open
                        inputText = "",
                        parsedResult = null,
                        showPreview = false
                    )
                }
                
                _effect.send(QuickCaptureEffect.TaskCreated(taskId))
                _effect.send(QuickCaptureEffect.ShowSnackbar(
                    message = "Task created",
                    actionLabel = "View",
                    taskId = taskId
                ))
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isCreating = false,
                        error = "Failed to create task: ${e.message}"
                    )
                }
                _effect.send(QuickCaptureEffect.ShowError("Failed to create task"))
            }
        }
    }
    
    private fun openEditDetails() {
        viewModelScope.launch {
            val result = _uiState.value.parsedResult ?: return@launch
            val preselectedGoalId = _uiState.value.preselectedGoalId
            
            try {
                // Create task first, then open detail
                val dueDate = result.dueDate?.let { parseDueDate(it) }
                
                // Use suggestedGoal if available, fall back to preselectedGoalId
                val goalId = result.suggestedGoal?.id ?: preselectedGoalId

                val taskId = taskRepository.createTask(
                    title = result.title,
                    notes = null,
                    dueDate = dueDate,
                    quadrant = result.quadrant,
                    aiExplanation = result.aiExplanation,
                    goalId = goalId
                )
                
                _effect.send(QuickCaptureEffect.OpenTaskDetail(taskId))
            } catch (e: Exception) {
                _effect.send(QuickCaptureEffect.ShowError("Failed to create task"))
            }
        }
    }
    
    private fun dismiss() {
        reset()
    }
    
    /**
     * Pre-select a goal for linking when QuickCapture is opened from a Goal Detail screen.
     * Fetches the goal title from the repository and sets it as the suggestedGoal
     * so the created task will be automatically linked to this goal.
     */
    fun preselectGoal(goalId: Long) {
        viewModelScope.launch {
            try {
                val goals = goalRepository.getAllActiveGoals().firstOrNull() ?: emptyList()
                val goal = goals.find { it.id == goalId }
                if (goal != null) {
                    val suggested = SuggestedGoal(
                        id = goal.id,
                        title = goal.title,
                        reason = "Linked from goal detail"
                    )
                    _uiState.update { current ->
                        current.copy(
                            parsedResult = current.parsedResult?.copy(suggestedGoal = suggested)
                                ?: ParsedTaskResult(
                                    title = "",
                                    suggestedGoal = suggested
                                ),
                            preselectedGoalId = goalId
                        )
                    }
                }
            } catch (e: Exception) {
                // Silently fail — goal linking is a convenience, not critical
            }
        }
    }
    
    private fun reset() {
        parseJob?.cancel()
        parseJob = null
        _uiState.update { QuickCaptureUiState() }  // voiceState defaults to Idle
    }
    
    private fun formatDueDate(instant: Instant): String {
        val now = clock.now()
        val tz = TimeZone.currentSystemDefault()
        val localDateTime = instant.toLocalDateTime(tz)
        val localDate = localDateTime.date
        val today = now.toLocalDateTime(tz).date
        val tomorrow = today.plus(1, DateTimeUnit.DAY)

        val hasTime = localDateTime.hour > 0 || localDateTime.minute > 0
        val timeSuffix = if (hasTime) {
            val hour = localDateTime.hour
            val minute = localDateTime.minute.toString().padStart(2, '0')
            val amPm = if (hour < 12) "AM" else "PM"
            val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            " at $hour12:$minute $amPm"
        } else ""
        
        return when (localDate) {
            today -> "Today$timeSuffix"
            tomorrow -> "Tomorrow$timeSuffix"
            else -> {
                val month = localDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                "$month ${localDate.dayOfMonth}$timeSuffix"
            }
        }
    }
    
    private fun parseDueDate(dateString: String): Instant? {
        return try {
            Instant.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Side effects from QuickCaptureViewModel.
 */
sealed interface QuickCaptureEffect {
    data class TaskCreated(val taskId: Long) : QuickCaptureEffect
    data class OpenTaskDetail(val taskId: Long) : QuickCaptureEffect
    data class ShowSnackbar(val message: String, val actionLabel: String? = null, val taskId: Long? = null) : QuickCaptureEffect
    data class ShowError(val message: String) : QuickCaptureEffect
    object StartVoiceRecognition : QuickCaptureEffect
    object Dismiss : QuickCaptureEffect
}

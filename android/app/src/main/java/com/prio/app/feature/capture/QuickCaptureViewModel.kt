package com.prio.app.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    
    // Debounce job for parsing
    private var parseJob: Job? = null
    private val PARSE_DEBOUNCE_MS = 500L
    
    /**
     * Handle UI events.
     */
    fun onEvent(event: QuickCaptureEvent) {
        when (event) {
            is QuickCaptureEvent.UpdateInput -> updateInput(event.text)
            is QuickCaptureEvent.StartVoiceInput -> startVoiceInput()
            is QuickCaptureEvent.StopVoiceInput -> stopVoiceInput()
            is QuickCaptureEvent.ParseInput -> parseInput()
            is QuickCaptureEvent.UpdateParsedTitle -> updateParsedTitle(event.title)
            is QuickCaptureEvent.UpdateParsedQuadrant -> updateParsedQuadrant(event.quadrant)
            is QuickCaptureEvent.AddSuggestionToInput -> addSuggestion(event.suggestion)
            is QuickCaptureEvent.CreateTask -> createTask()
            is QuickCaptureEvent.OpenEditDetails -> openEditDetails()
            is QuickCaptureEvent.Dismiss -> dismiss()
            is QuickCaptureEvent.Reset -> reset()
        }
    }
    
    private fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text, showPreview = false) }
        
        // Debounce parsing
        parseJob?.cancel()
        if (text.isNotBlank()) {
            parseJob = viewModelScope.launch {
                delay(PARSE_DEBOUNCE_MS)
                parseInput()
            }
        } else {
            _uiState.update { it.copy(parsedResult = null) }
        }
    }
    
    private fun startVoiceInput() {
        viewModelScope.launch {
            _uiState.update { it.copy(isVoiceInputActive = true) }
            _effect.send(QuickCaptureEffect.StartVoiceRecognition)
        }
    }
    
    private fun stopVoiceInput() {
        _uiState.update { it.copy(isVoiceInputActive = false) }
    }
    
    /**
     * Called when voice recognition produces text.
     */
    fun onVoiceResult(text: String) {
        _uiState.update { 
            it.copy(
                inputText = text,
                isVoiceInputActive = false
            )
        }
        parseInput()
    }
    
    private fun parseInput() {
        val input = _uiState.value.inputText.trim()
        if (input.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isAiParsing = true) }
            
            try {
                // Parse natural language input
                val parsed = naturalLanguageParser.parse(input)
                
                // Classify with Eisenhower engine (rule-based, <50ms)
                val classification = eisenhowerEngine.classify(
                    taskText = parsed.title,
                    dueDate = parsed.dueDate
                )
                
                // Find suggested goal based on content
                val suggestedGoal = findSuggestedGoal(parsed.title)
                
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
                
                _uiState.update { 
                    it.copy(
                        isAiParsing = false,
                        parsedResult = result,
                        showPreview = true
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
                        showPreview = true
                    )
                }
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
        
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            
            try {
                // Parse due date if present
                val dueDate = result.dueDate?.let { parseDueDate(it) }
                
                // Create task
                val taskId = taskRepository.createTask(
                    title = result.title,
                    notes = null,
                    dueDate = dueDate,
                    quadrant = result.quadrant,
                    aiExplanation = result.aiExplanation,
                    goalId = result.suggestedGoal?.id
                )
                
                _uiState.update { 
                    it.copy(
                        isCreating = false,
                        isCreated = true
                    )
                }
                
                _effect.send(QuickCaptureEffect.TaskCreated(taskId))
                _effect.send(QuickCaptureEffect.ShowSnackbar(
                    message = "Task created",
                    actionLabel = "View"
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
            
            try {
                // Create task first, then open detail
                val dueDate = result.dueDate?.let { parseDueDate(it) }
                
                val taskId = taskRepository.createTask(
                    title = result.title,
                    notes = null,
                    dueDate = dueDate,
                    quadrant = result.quadrant,
                    aiExplanation = result.aiExplanation,
                    goalId = result.suggestedGoal?.id
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
    
    private fun reset() {
        _uiState.update { QuickCaptureUiState() }
    }
    
    private fun formatDueDate(instant: Instant): String {
        val now = clock.now()
        val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        
        return when (localDate) {
            today -> "Today"
            tomorrow -> "Tomorrow"
            else -> {
                val month = localDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                "$month ${localDate.dayOfMonth}"
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
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : QuickCaptureEffect
    data class ShowError(val message: String) : QuickCaptureEffect
    object StartVoiceRecognition : QuickCaptureEffect
    object Dismiss : QuickCaptureEffect
}

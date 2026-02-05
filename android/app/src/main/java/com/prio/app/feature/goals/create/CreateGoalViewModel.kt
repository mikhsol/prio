package com.prio.app.feature.goals.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prio.core.ai.model.AiContext
import com.prio.core.ai.model.AiRequest
import com.prio.core.ai.model.AiRequestType
import com.prio.core.ai.model.AiResult
import com.prio.core.ai.provider.AiProvider
import com.prio.core.aiprovider.di.MainAiProvider
import com.prio.core.common.model.GoalCategory
import com.prio.core.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * ViewModel for the Create Goal wizard.
 *
 * Implements task 3.2.3 from ACTION_PLAN.md:
 * 3-step wizard: Describe → AI SMART Refinement → Timeline & Milestones
 *
 * AI Integration:
 * - Uses SUGGEST_SMART_GOAL request type for AiResult.SmartGoalSuggestion
 * - Provides existing goals as context for relevance
 * - Graceful fallback: "Skip AI" always available
 *
 * Constraints per GL-001:
 * - Maximum 10 active goals enforced
 * - 0-5 milestones per goal
 */
@HiltViewModel
class CreateGoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    @MainAiProvider private val aiProvider: AiProvider,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGoalUiState())
    val uiState: StateFlow<CreateGoalUiState> = _uiState.asStateFlow()

    private val _effect = Channel<CreateGoalEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        checkGoalLimit()
    }

    /**
     * Check whether the user can create a new goal (max 10 active).
     */
    private fun checkGoalLimit() {
        viewModelScope.launch {
            val canCreate = goalRepository.canCreateNewGoal()
            val count = goalRepository.getActiveGoalCount()
            _uiState.update {
                it.copy(
                    canCreateGoal = canCreate,
                    activeGoalCount = count
                )
            }
            if (!canCreate) {
                _effect.send(CreateGoalEffect.ShowSnackbar(
                    "Maximum 10 active goals reached. Complete or remove a goal first."
                ))
            }
        }
    }

    /**
     * Process UI events from the wizard screen.
     */
    fun onEvent(event: CreateGoalEvent) {
        when (event) {
            // Step 1: Describe
            is CreateGoalEvent.OnGoalInputChange -> updateGoalInput(event.input)
            is CreateGoalEvent.OnExampleSelect -> selectExample(event.example)
            is CreateGoalEvent.OnNextFromDescribe -> advanceFromDescribe()

            // Step 2: AI SMART
            is CreateGoalEvent.OnRequestAiRefinement -> requestAiRefinement()
            is CreateGoalEvent.OnSkipAi -> skipAi()
            is CreateGoalEvent.OnRefinedGoalChange -> updateRefinedGoal(event.text)
            is CreateGoalEvent.OnCategorySelect -> selectCategory(event.category)
            is CreateGoalEvent.OnNextFromSmart -> advanceFromSmart()

            // Step 3: Timeline & Milestones
            is CreateGoalEvent.OnTargetDateSelect -> selectTargetDate(event.dateMillis)
            is CreateGoalEvent.OnAddMilestone -> addMilestone(event.title)
            is CreateGoalEvent.OnRemoveMilestone -> removeMilestone(event.index)
            is CreateGoalEvent.OnToggleWeeklyReminder -> toggleReminder(event.enabled)
            is CreateGoalEvent.OnCreateGoal -> createGoal()

            // Navigation
            is CreateGoalEvent.OnBack -> navigateBack()
            is CreateGoalEvent.OnPreviousStep -> goToPreviousStep()

            // Post-creation
            is CreateGoalEvent.OnAddFirstTask -> openQuickCapture()
            is CreateGoalEvent.OnViewDetails -> viewGoalDetails()
            is CreateGoalEvent.OnBackToGoals -> backToGoals()
        }
    }

    // ==================== Step 1: Describe ====================

    private fun updateGoalInput(input: String) {
        _uiState.update { it.copy(goalInput = input) }
    }

    private fun selectExample(example: String) {
        _uiState.update { it.copy(goalInput = example) }
    }

    private fun advanceFromDescribe() {
        val input = _uiState.value.goalInput.trim()
        if (input.isBlank()) {
            viewModelScope.launch {
                _effect.send(CreateGoalEffect.ShowSnackbar("Please describe your goal first"))
            }
            return
        }

        _uiState.update { it.copy(currentStep = CreateGoalStep.AI_SMART) }

        // Auto-trigger AI refinement
        requestAiRefinement()
    }

    // ==================== Step 2: AI SMART ====================

    private fun requestAiRefinement() {
        val input = _uiState.value.goalInput.trim()
        if (input.isBlank()) return

        _uiState.update { it.copy(isAiProcessing = true, error = null) }

        viewModelScope.launch {
            try {
                val request = AiRequest(
                    type = AiRequestType.SUGGEST_SMART_GOAL,
                    input = input,
                    context = AiContext(
                        currentTime = clock.now().toString(),
                        userTimezone = TimeZone.currentSystemDefault().id
                    )
                )

                val response = aiProvider.complete(request)

                response.fold(
                    onSuccess = { aiResponse ->
                        val suggestion = aiResponse.result as? AiResult.SmartGoalSuggestion
                        if (suggestion != null) {
                            _uiState.update {
                                it.copy(
                                    isAiProcessing = false,
                                    refinedGoal = suggestion.refinedGoal,
                                    smartSpecific = suggestion.specific,
                                    smartMeasurable = suggestion.measurable,
                                    smartAchievable = suggestion.achievable,
                                    smartRelevant = suggestion.relevant,
                                    smartTimeBound = suggestion.timeBound,
                                    suggestedMilestones = suggestion.suggestedMilestones,
                                    aiSkipped = false
                                )
                            }
                        } else {
                            // AI returned unexpected result type — fall back
                            fallbackToManualEntry()
                        }
                    },
                    onFailure = { error ->
                        fallbackToManualEntry()
                        _effect.send(CreateGoalEffect.ShowSnackbar(
                            "AI unavailable — you can refine your goal manually"
                        ))
                    }
                )
            } catch (e: Exception) {
                fallbackToManualEntry()
                _effect.send(CreateGoalEffect.ShowSnackbar(
                    "AI unavailable — you can refine your goal manually"
                ))
            }
        }
    }

    private fun fallbackToManualEntry() {
        val input = _uiState.value.goalInput.trim()
        _uiState.update {
            it.copy(
                isAiProcessing = false,
                refinedGoal = input,
                aiSkipped = true
            )
        }
    }

    private fun skipAi() {
        val input = _uiState.value.goalInput.trim()
        _uiState.update {
            it.copy(
                isAiProcessing = false,
                refinedGoal = input,
                aiSkipped = true
            )
        }
    }

    private fun updateRefinedGoal(text: String) {
        _uiState.update { it.copy(refinedGoal = text) }
    }

    private fun selectCategory(category: GoalCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    private fun advanceFromSmart() {
        val refined = _uiState.value.refinedGoal.trim()
        if (refined.isBlank()) {
            viewModelScope.launch {
                _effect.send(CreateGoalEffect.ShowSnackbar("Please provide a goal title"))
            }
            return
        }

        // Pre-populate milestones from AI suggestions
        val state = _uiState.value
        val milestones = if (state.milestones.isEmpty() && state.suggestedMilestones.isNotEmpty()) {
            state.suggestedMilestones.map { title ->
                MilestoneInput(title = title, isAiSuggested = true)
            }
        } else {
            state.milestones
        }

        _uiState.update {
            it.copy(
                currentStep = CreateGoalStep.TIMELINE,
                milestones = milestones
            )
        }
    }

    // ==================== Step 3: Timeline & Milestones ====================

    private fun selectTargetDate(dateMillis: Long) {
        val instant = Instant.fromEpochMilliseconds(dateMillis)
        val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val formatted = "${localDate.monthNumber}/${localDate.dayOfMonth}/${localDate.year}"

        _uiState.update {
            it.copy(
                targetDateMillis = dateMillis,
                targetDateText = formatted
            )
        }
    }

    private fun addMilestone(title: String) {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return

        val current = _uiState.value.milestones
        if (current.size >= GoalRepository.MAX_MILESTONES_PER_GOAL) {
            viewModelScope.launch {
                _effect.send(CreateGoalEffect.ShowSnackbar(
                    "Maximum ${GoalRepository.MAX_MILESTONES_PER_GOAL} milestones per goal"
                ))
            }
            return
        }

        _uiState.update {
            it.copy(milestones = current + MilestoneInput(title = trimmed))
        }
    }

    private fun removeMilestone(index: Int) {
        val current = _uiState.value.milestones
        if (index in current.indices) {
            _uiState.update {
                it.copy(milestones = current.toMutableList().apply { removeAt(index) })
            }
        }
    }

    private fun toggleReminder(enabled: Boolean) {
        _uiState.update { it.copy(enableWeeklyReminder = enabled) }
    }

    // ==================== Goal Creation ====================

    private fun createGoal() {
        val state = _uiState.value
        if (state.isLoading) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // Build description from SMART fields
                val description = buildSmartDescription(state)

                // Determine target date
                val targetDate: LocalDate? = state.targetDateMillis?.let { millis ->
                    Instant.fromEpochMilliseconds(millis)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date
                }

                // Create the goal
                val goalId = goalRepository.createGoal(
                    title = state.refinedGoal.ifBlank { state.goalInput }.trim(),
                    description = description,
                    originalInput = state.goalInput.trim(),
                    category = state.selectedCategory,
                    targetDate = targetDate
                )

                if (goalId != null) {
                    // Add milestones
                    state.milestones.forEachIndexed { index, milestone ->
                        goalRepository.addMilestone(
                            goalId = goalId,
                            title = milestone.title,
                            targetDate = parseMilestoneDate(milestone.targetDate)
                        )
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            createdGoalId = goalId,
                            showCelebration = true
                        )
                    }

                    _effect.send(CreateGoalEffect.ShowCelebration)
                } else {
                    // Max goals limit reached
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Maximum 10 active goals reached"
                        )
                    }
                    _effect.send(CreateGoalEffect.ShowSnackbar(
                        "Maximum 10 active goals reached. Complete or remove a goal first."
                    ))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to create goal: ${e.localizedMessage}"
                    )
                }
                _effect.send(CreateGoalEffect.ShowSnackbar(
                    "Failed to create goal. Please try again."
                ))
            }
        }
    }

    /**
     * Build structured description from SMART fields.
     */
    private fun buildSmartDescription(state: CreateGoalUiState): String {
        if (state.aiSkipped) {
            return state.refinedGoal
        }

        return buildString {
            if (state.smartSpecific.isNotBlank()) {
                appendLine("📌 Specific: ${state.smartSpecific}")
            }
            if (state.smartMeasurable.isNotBlank()) {
                appendLine("📊 Measurable: ${state.smartMeasurable}")
            }
            if (state.smartAchievable.isNotBlank()) {
                appendLine("✅ Achievable: ${state.smartAchievable}")
            }
            if (state.smartRelevant.isNotBlank()) {
                appendLine("🎯 Relevant: ${state.smartRelevant}")
            }
            if (state.smartTimeBound.isNotBlank()) {
                appendLine("⏰ Time-bound: ${state.smartTimeBound}")
            }
        }.trimEnd()
    }

    private fun parseMilestoneDate(dateText: String?): LocalDate? {
        if (dateText.isNullOrBlank()) return null
        return try {
            // Simple MM/DD/YYYY parse
            val parts = dateText.split("/")
            if (parts.size == 3) {
                LocalDate(
                    year = parts[2].toInt(),
                    monthNumber = parts[0].toInt(),
                    dayOfMonth = parts[1].toInt()
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    // ==================== Navigation ====================

    private fun navigateBack() {
        viewModelScope.launch {
            _effect.send(CreateGoalEffect.NavigateBack)
        }
    }

    private fun goToPreviousStep() {
        val currentStep = _uiState.value.currentStep
        val previousStep = when (currentStep) {
            CreateGoalStep.DESCRIBE -> {
                viewModelScope.launch {
                    _effect.send(CreateGoalEffect.NavigateBack)
                }
                return
            }
            CreateGoalStep.AI_SMART -> CreateGoalStep.DESCRIBE
            CreateGoalStep.TIMELINE -> CreateGoalStep.AI_SMART
        }
        _uiState.update { it.copy(currentStep = previousStep) }
    }

    private fun openQuickCapture() {
        viewModelScope.launch {
            _effect.send(CreateGoalEffect.OpenQuickCapture)
        }
    }

    private fun viewGoalDetails() {
        val goalId = _uiState.value.createdGoalId ?: return
        viewModelScope.launch {
            _effect.send(CreateGoalEffect.NavigateToGoalDetail(goalId))
        }
    }

    private fun backToGoals() {
        viewModelScope.launch {
            _effect.send(CreateGoalEffect.NavigateToGoalsList)
        }
    }
}

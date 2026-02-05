package com.prio.app.feature.goals.create

import com.prio.core.common.model.GoalCategory

/**
 * UI state for the Create Goal wizard.
 *
 * Implements task 3.2.3: Goal Creation wizard per 1.1.4 Goals Screens Spec.
 * Per GL-001: "Create goal with AI SMART suggestions"
 *
 * 3-step wizard:
 * 1. Describe: Natural language input + examples
 * 2. AI SMART Refinement: AI suggestion + SMART breakdown + category + "Skip AI"
 * 3. Timeline & Milestones: Target date + AI-suggested milestones + custom add
 */
data class CreateGoalUiState(
    val currentStep: CreateGoalStep = CreateGoalStep.DESCRIBE,
    val isLoading: Boolean = false,
    val error: String? = null,

    // Step 1: Describe
    val goalInput: String = "",
    val inputExamples: List<String> = listOf(
        "Get promoted to Senior Engineer by December",
        "Read 12 books this year",
        "Run a half marathon in under 2 hours",
        "Save $10,000 for emergency fund",
        "Learn Spanish to conversational level"
    ),

    // Step 2: AI SMART Refinement
    val refinedGoal: String = "",
    val smartSpecific: String = "",
    val smartMeasurable: String = "",
    val smartAchievable: String = "",
    val smartRelevant: String = "",
    val smartTimeBound: String = "",
    val selectedCategory: GoalCategory = GoalCategory.PERSONAL,
    val isAiProcessing: Boolean = false,
    val aiSkipped: Boolean = false,

    // Step 3: Timeline & Milestones
    val targetDateText: String = "",
    val targetDateMillis: Long? = null,
    val suggestedMilestones: List<String> = emptyList(),
    val milestones: List<MilestoneInput> = emptyList(),
    val enableWeeklyReminder: Boolean = true,

    // Completion
    val createdGoalId: Long? = null,
    val showCelebration: Boolean = false,

    // Limits
    val canCreateGoal: Boolean = true,
    val activeGoalCount: Int = 0
)

/**
 * Wizard step for goal creation.
 */
enum class CreateGoalStep(val number: Int, val label: String) {
    DESCRIBE(1, "Describe Your Goal"),
    AI_SMART(2, "AI SMART Refinement"),
    TIMELINE(3, "Timeline & Milestones")
}

/**
 * Input model for a milestone during creation.
 */
data class MilestoneInput(
    val title: String,
    val targetDate: String? = null,
    val isAiSuggested: Boolean = false
)

/**
 * Events from CreateGoalScreen to ViewModel.
 */
sealed interface CreateGoalEvent {
    // Step 1
    data class OnGoalInputChange(val input: String) : CreateGoalEvent
    data class OnExampleSelect(val example: String) : CreateGoalEvent
    data object OnNextFromDescribe : CreateGoalEvent

    // Step 2
    data object OnRequestAiRefinement : CreateGoalEvent
    data object OnSkipAi : CreateGoalEvent
    data class OnRefinedGoalChange(val text: String) : CreateGoalEvent
    data class OnCategorySelect(val category: GoalCategory) : CreateGoalEvent
    data object OnNextFromSmart : CreateGoalEvent

    // Step 3
    data class OnTargetDateSelect(val dateMillis: Long) : CreateGoalEvent
    data class OnAddMilestone(val title: String) : CreateGoalEvent
    data class OnRemoveMilestone(val index: Int) : CreateGoalEvent
    data class OnToggleWeeklyReminder(val enabled: Boolean) : CreateGoalEvent
    data object OnCreateGoal : CreateGoalEvent

    // Navigation
    data object OnBack : CreateGoalEvent
    data object OnPreviousStep : CreateGoalEvent

    // Post-creation
    data object OnAddFirstTask : CreateGoalEvent
    data object OnViewDetails : CreateGoalEvent
    data object OnBackToGoals : CreateGoalEvent
}

/**
 * One-time effects from ViewModel to UI.
 */
sealed interface CreateGoalEffect {
    data object NavigateBack : CreateGoalEffect
    data class NavigateToGoalDetail(val goalId: Long) : CreateGoalEffect
    data object NavigateToGoalsList : CreateGoalEffect
    data object OpenQuickCapture : CreateGoalEffect
    data class ShowSnackbar(val message: String) : CreateGoalEffect
    data object ShowCelebration : CreateGoalEffect
}

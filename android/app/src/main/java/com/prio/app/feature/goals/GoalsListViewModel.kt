package com.prio.app.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prio.core.common.model.GoalCategory
import com.prio.core.common.model.GoalStatus
import com.prio.core.data.local.entity.GoalEntity
import com.prio.core.data.repository.GoalRepository
import com.prio.core.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for GoalsListScreen.
 *
 * Implements task 3.2.1: Goals List screen per 1.1.4 Goals Screens Spec.
 * Per GL-005: "Goals list accessible from main navigation"
 *
 * Features:
 * - Overview card with stats (Active, On Track, At Risk) + average progress ring
 * - Category filter chips (horizontal scroll, single-select, "All" default)
 * - Goal cards grouped by status: At Risk → Slightly Behind → On Track
 * - Empty state per 1.1.10
 * - Max 10 active goals enforcement per GL-001
 */
@HiltViewModel
class GoalsListViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsListUiState())
    val uiState: StateFlow<GoalsListUiState> = _uiState.asStateFlow()

    private val _effect = Channel<GoalsListEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // For undo delete
    private var lastDeletedGoal: GoalEntity? = null

    // Filter state
    private val categoryFilterFlow = MutableStateFlow<GoalCategory?>(null)

    // Section collapse state
    private val sectionCollapseState = MutableStateFlow(
        mapOf(
            GoalStatus.AT_RISK to true,
            GoalStatus.BEHIND to true,
            GoalStatus.ON_TRACK to true,
            GoalStatus.COMPLETED to true
        )
    )

    init {
        observeGoals()
    }

    /**
     * Observe goals from repository and transform to UI state.
     * Combines goal data with filter state.
     */
    private fun observeGoals() {
        combine(
            goalRepository.getAllActiveGoals(),
            goalRepository.getActiveGoalCountFlow(),
            categoryFilterFlow,
            sectionCollapseState
        ) { goals, activeCount, categoryFilter, collapseState ->
            transformToUiState(goals, activeCount, categoryFilter, collapseState)
        }.launchIn(viewModelScope)
    }

    /**
     * Transform goal entities to UI state with sections.
     * Groups by calculated status, sorted: At Risk → Behind → On Track.
     */
    private suspend fun transformToUiState(
        goals: List<GoalEntity>,
        activeCount: Int,
        categoryFilter: GoalCategory?,
        collapseState: Map<GoalStatus, Boolean>
    ) {
        val filteredGoals = if (categoryFilter != null) {
            goals.filter { it.category == categoryFilter }
        } else {
            goals
        }

        // Map entities to UI models with calculated status
        val goalUiModels = filteredGoals.map { entity ->
            mapToUiModel(entity)
        }

        // Calculate overview stats from ALL active goals (not filtered)
        val allGoalModels = goals.map { mapToUiModel(it) }
        val overviewStats = calculateOverviewStats(allGoalModels)

        // Group by status in priority order: At Risk → Behind → On Track
        val statusOrder = listOf(GoalStatus.AT_RISK, GoalStatus.BEHIND, GoalStatus.ON_TRACK)
        val sections = statusOrder.map { status ->
            GoalSection(
                status = status,
                goals = goalUiModels.filter { it.status == status },
                isExpanded = collapseState[status] ?: true
            )
        }.filter { it.goals.isNotEmpty() }

        _uiState.update { current ->
            current.copy(
                isLoading = false,
                error = null,
                sections = sections,
                overviewStats = overviewStats,
                selectedCategoryFilter = categoryFilter,
                activeGoalCount = activeCount,
                canCreateNewGoal = activeCount < GoalRepository.MAX_ACTIVE_GOALS
            )
        }
    }

    /**
     * Map GoalEntity to GoalUiModel with calculated fields.
     */
    private suspend fun mapToUiModel(entity: GoalEntity): GoalUiModel {
        val status = goalRepository.calculateGoalStatus(entity)
        val (milestonesCompleted, milestonesTotal) = goalRepository.getMilestoneProgress(entity.id)

        // Count linked tasks
        val activeTasks = taskRepository.getActiveTasksByGoalId(entity.id)
        val completedTasks = taskRepository.getCompletedTasksByGoalId(entity.id)
        val linkedTasksCount = activeTasks.size + completedTasks.size

        // Format target date
        val targetDateText = entity.targetDate?.let { date ->
            try {
                val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
                javaDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))
            } catch (e: Exception) {
                date.toString()
            }
        }

        return GoalUiModel(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            category = entity.category,
            progress = entity.progress / 100f,  // Convert 0-100 to 0.0-1.0
            status = status,
            targetDate = targetDateText,
            milestonesCompleted = milestonesCompleted,
            milestonesTotal = milestonesTotal,
            linkedTasksCount = linkedTasksCount,
            isCompleted = entity.isCompleted
        )
    }

    /**
     * Calculate overview statistics for dashboard card.
     * Per 1.1.4: Active, On Track, At Risk + average progress ring.
     */
    private fun calculateOverviewStats(goals: List<GoalUiModel>): GoalOverviewStats {
        val activeGoals = goals.filter { !it.isCompleted }
        return GoalOverviewStats(
            activeCount = activeGoals.size,
            onTrackCount = activeGoals.count { it.status == GoalStatus.ON_TRACK },
            atRiskCount = activeGoals.count { it.status == GoalStatus.AT_RISK },
            behindCount = activeGoals.count { it.status == GoalStatus.BEHIND },
            averageProgress = if (activeGoals.isNotEmpty()) {
                activeGoals.map { it.progress }.average().toFloat()
            } else 0f
        )
    }

    /**
     * Handle UI events.
     */
    fun onEvent(event: GoalsListEvent) {
        when (event) {
            is GoalsListEvent.OnGoalClick -> {
                viewModelScope.launch {
                    _effect.send(GoalsListEffect.NavigateToGoalDetail(event.goalId))
                }
            }
            is GoalsListEvent.OnCategoryFilterSelect -> {
                categoryFilterFlow.value = event.category
            }
            is GoalsListEvent.OnSectionToggle -> {
                sectionCollapseState.update { current ->
                    current.toMutableMap().apply {
                        this[event.status] = !(this[event.status] ?: true)
                    }
                }
            }
            is GoalsListEvent.OnGoalDelete -> {
                deleteGoal(event.goalId)
            }
            is GoalsListEvent.OnGoalComplete -> {
                completeGoal(event.goalId)
            }
            GoalsListEvent.OnCreateGoalClick -> {
                viewModelScope.launch {
                    if (_uiState.value.canCreateNewGoal) {
                        _effect.send(GoalsListEffect.NavigateToCreateGoal)
                    } else {
                        _effect.send(GoalsListEffect.ShowMaxGoalsWarning)
                    }
                }
            }
            GoalsListEvent.OnRefresh -> {
                _uiState.update { it.copy(isLoading = true) }
                // Goals are observed via Flow, state will auto-update
                _uiState.update { it.copy(isLoading = false) }
            }
            GoalsListEvent.OnUndoDelete -> {
                undoDelete()
            }
        }
    }

    private fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            val goal = goalRepository.getGoalById(goalId)
            if (goal != null) {
                lastDeletedGoal = goal
                goalRepository.deleteGoalById(goalId)
                _effect.send(
                    GoalsListEffect.ShowSnackbar(
                        message = "\"${goal.title}\" deleted",
                        actionLabel = "Undo"
                    )
                )
            }
        }
    }

    private fun undoDelete() {
        viewModelScope.launch {
            lastDeletedGoal?.let { goal ->
                goalRepository.insertGoal(goal)
                lastDeletedGoal = null
            }
        }
    }

    private fun completeGoal(goalId: Long) {
        viewModelScope.launch {
            goalRepository.completeGoal(goalId)
            _effect.send(GoalsListEffect.ShowCompletionConfetti)
        }
    }
}

package com.prio.app.feature.tasks

import com.prio.core.common.model.EisenhowerQuadrant

/**
 * UI state for the Task List screen.
 * 
 * Implements task 3.1.2: Task List screen with filters
 * Per 1.1.1 Task List Screen Specification.
 */
data class TaskListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val sections: List<TaskSection> = emptyList(),
    val selectedFilter: TaskFilter = TaskFilter.All,
    val showCompletedTasks: Boolean = false,
    val searchQuery: String = "",
    val viewMode: TaskViewMode = TaskViewMode.LIST,
    val isSearchActive: Boolean = false,
    val totalActiveCount: Int = 0,
    val doFirstCount: Int = 0
) {
    val isEmpty: Boolean
        get() = sections.all { it.tasks.isEmpty() }
    
    val isAllCompleted: Boolean
        get() = totalActiveCount == 0 && sections.sumOf { it.tasks.size } > 0
    
    val hasNoResults: Boolean
        get() = isEmpty && (searchQuery.isNotBlank() || selectedFilter != TaskFilter.All)
}

/**
 * A section in the task list, grouped by quadrant.
 * Per 1.1.1: DO FIRST, SCHEDULE, DELEGATE, MAYBE LATER sections.
 */
data class TaskSection(
    val quadrant: EisenhowerQuadrant,
    val tasks: List<TaskUiModel>,
    val isExpanded: Boolean = true
) {
    val title: String
        get() = when (quadrant) {
            EisenhowerQuadrant.DO_FIRST -> "DO FIRST"
            EisenhowerQuadrant.SCHEDULE -> "SCHEDULE"
            EisenhowerQuadrant.DELEGATE -> "DELEGATE"
            EisenhowerQuadrant.ELIMINATE -> "MAYBE LATER"
        }
    
    val count: Int
        get() = tasks.size
    
    val emoji: String
        get() = quadrant.emoji
}

/**
 * Task UI model for display in the list.
 * Mapped from domain TaskEntity.
 */
data class TaskUiModel(
    val id: Long,
    val title: String,
    val quadrant: EisenhowerQuadrant,
    val isCompleted: Boolean = false,
    val isOverdue: Boolean = false,
    val dueText: String? = null,
    val goalName: String? = null,
    val aiExplanation: String? = null,
    val hasSubtasks: Boolean = false,
    val hasNotes: Boolean = false,
    val hasReminder: Boolean = false,
    val isRecurring: Boolean = false
)

/**
 * Task filter options.
 * Per 1.1.1 filter chips spec.
 */
enum class TaskFilter(val displayName: String) {
    All("All"),
    Today("Today"),
    Upcoming("Upcoming"),
    HasGoal("Has Goal"),
    Recurring("Recurring")
}

/**
 * View mode for task list.
 * Per 1.1.1 view switcher spec.
 */
enum class TaskViewMode {
    LIST,       // Grouped by quadrant (default)
    FOCUS,      // One task at a time
    MATRIX      // 2x2 Eisenhower grid
}

/**
 * Events from TaskListScreen to ViewModel.
 */
sealed interface TaskListEvent {
    data class OnTaskClick(val taskId: Long) : TaskListEvent
    data class OnTaskCheckboxClick(val taskId: Long) : TaskListEvent
    data class OnTaskQuadrantChange(val taskId: Long, val newQuadrant: EisenhowerQuadrant) : TaskListEvent
    data class OnTaskSwipeComplete(val taskId: Long) : TaskListEvent
    data class OnTaskSwipeDelete(val taskId: Long) : TaskListEvent
    data class OnSectionToggle(val quadrant: EisenhowerQuadrant) : TaskListEvent
    data class OnFilterSelect(val filter: TaskFilter) : TaskListEvent
    data class OnSearchQueryChange(val query: String) : TaskListEvent
    data class OnViewModeChange(val mode: TaskViewMode) : TaskListEvent
    data object OnToggleShowCompleted : TaskListEvent
    data object OnSearchToggle : TaskListEvent
    data object OnFabClick : TaskListEvent
    data object OnRefresh : TaskListEvent
    data object OnUndoComplete : TaskListEvent
    data object OnUndoDelete : TaskListEvent
    data object OnClearFilters : TaskListEvent
}

/**
 * One-time effects from ViewModel to UI.
 */
sealed interface TaskListEffect {
    data class NavigateToTaskDetail(val taskId: Long) : TaskListEffect
    data object NavigateToQuickCapture : TaskListEffect
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val undoEvent: TaskListEvent? = null
    ) : TaskListEffect
    data object ShowCompleteConfetti : TaskListEffect
}

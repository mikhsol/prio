package com.prio.app.feature.tasks

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.ui.components.Quadrant
import com.prio.core.ui.components.TaskCard
import com.prio.core.ui.components.TaskCardData
import com.prio.core.ui.theme.PrioTheme
import com.prio.core.ui.theme.QuadrantColors
import kotlinx.coroutines.flow.collectLatest

/**
 * Task List Screen implementing requirement 3.1.2.
 * 
 * Features per 1.1.1 Task List Screen Specification:
 * - LazyColumn with quadrant sections (Q1→Q4 order)
 * - Filter chips (All, Today, Upcoming, Has Goal, Recurring)
 * - Swipe actions (left=complete, right=delete)
 * - Section collapse/expand
 * - Search functionality
 * - 60fps scroll performance
 * - Undo via snackbar
 * 
 * Accessibility:
 * - Section headers are headings
 * - Min 48dp touch targets
 * - Screen reader descriptions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel = hiltViewModel(),
    onNavigateToTaskDetail: (Long) -> Unit = {},
    onNavigateToAddTask: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle effects (snackbar, navigation)
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is TaskListEffect.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = effect.message,
                        actionLabel = effect.actionLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(TaskListEvent.OnUndoComplete)
                    }
                }
                is TaskListEffect.NavigateToTaskDetail -> {
                    onNavigateToTaskDetail(effect.taskId)
                }
                is TaskListEffect.NavigateToQuickCapture -> {
                    onNavigateToAddTask()
                }
                is TaskListEffect.ShowCompleteConfetti -> {
                    // TODO: Show confetti animation
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TaskListTopBar(
                isSearchActive = state.isSearchActive,
                searchQuery = state.searchQuery,
                showCompletedTasks = state.showCompletedTasks,
                onSearchQueryChange = { viewModel.onEvent(TaskListEvent.OnSearchQueryChange(it)) },
                onSearchToggle = { viewModel.onEvent(TaskListEvent.OnSearchToggle) },
                onToggleShowCompleted = { viewModel.onEvent(TaskListEvent.OnToggleShowCompleted) }
            )
        },
        // FAB removed: PrioBottomNavigation already provides a center "Add" FAB
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips row
            FilterChipsRow(
                currentFilter = state.selectedFilter,
                onFilterSelected = { viewModel.onEvent(TaskListEvent.OnFilterSelect(it)) }
            )
            
            // Task list content
            when {
                state.isLoading -> {
                    LoadingState()
                }
                state.error != null -> {
                    ErrorState(
                        message = state.error ?: "Something went wrong",
                        onRetry = { viewModel.onEvent(TaskListEvent.OnRefresh) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                state.isEmpty -> {
                    EmptyState(
                        filter = state.selectedFilter,
                        onAddTask = { viewModel.onEvent(TaskListEvent.OnFabClick) }
                    )
                }
                else -> {
                    TaskListContent(
                        sections = state.sections,
                        onTaskClick = { viewModel.onEvent(TaskListEvent.OnTaskClick(it)) },
                        onTaskComplete = { viewModel.onEvent(TaskListEvent.OnTaskSwipeComplete(it)) },
                        onTaskDelete = { viewModel.onEvent(TaskListEvent.OnTaskSwipeDelete(it)) },
                        onSectionToggle = { viewModel.onEvent(TaskListEvent.OnSectionToggle(it)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    showCompletedTasks: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onToggleShowCompleted: () -> Unit
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                // Use OutlinedTextField instead of SearchBar for better fit in TopAppBar
                androidx.compose.material3.OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search tasks...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            } else {
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        actions = {
            IconButton(
                onClick = onSearchToggle,
                modifier = Modifier.semantics {
                    contentDescription = if (isSearchActive) "Close search" else "Search tasks"
                }
            ) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = null
                )
            }
            IconButton(
                onClick = onToggleShowCompleted,
                modifier = Modifier.semantics {
                    contentDescription = if (showCompletedTasks) "Hide completed tasks" else "Show completed tasks"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = if (showCompletedTasks) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    currentFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(TaskFilter.entries) { filter ->
            FilterChip(
                selected = filter == currentFilter,
                onClick = { onFilterSelected(filter) },
                label = { 
                    Text(
                        text = filter.displayName,
                        maxLines = 1
                    ) 
                },
                leadingIcon = if (filter == currentFilter) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.semantics {
                    contentDescription = "${filter.displayName} filter" + 
                        if (filter == currentFilter) ", selected" else ""
                }
            )
        }
    }
}

@Composable
private fun TaskListContent(
    sections: List<TaskSection>,
    onTaskClick: (Long) -> Unit,
    onTaskComplete: (Long) -> Unit,
    onTaskDelete: (Long) -> Unit,
    onSectionToggle: (EisenhowerQuadrant) -> Unit
) {
    val listState = rememberLazyListState()
    
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        sections.forEach { section ->
            // Section header
            item(key = "header_${section.quadrant.name}") {
                SectionHeader(
                    section = section,
                    onToggle = { onSectionToggle(section.quadrant) }
                )
            }
            
            // Section tasks (animated visibility)
            if (section.isExpanded) {
                items(
                    items = section.tasks,
                    key = { it.id }
                ) { task ->
                    SwipeableTaskCard(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                        onComplete = { onTaskComplete(task.id) },
                        onDelete = { onTaskDelete(task.id) }
                    )
                }
            }
        }
        
        // Bottom spacer for FAB
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    section: TaskSection,
    onToggle: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (!section.isExpanded) -90f else 0f,
        label = "arrow_rotation"
    )
    
    val quadrantColor = when (section.quadrant) {
        EisenhowerQuadrant.DO_FIRST -> QuadrantColors.doFirst
        EisenhowerQuadrant.SCHEDULE -> QuadrantColors.schedule
        EisenhowerQuadrant.DELEGATE -> QuadrantColors.delegate
        EisenhowerQuadrant.ELIMINATE -> QuadrantColors.eliminate
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .semantics { heading() }
            .testTag("section_header_${section.quadrant.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color indicator dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(quadrantColor)
            )
            
            // Section title with emoji
            Text(
                text = "${section.emoji} ${section.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Task count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = section.count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Expand/collapse arrow
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (!section.isExpanded) "Expand section" else "Collapse section",
            modifier = Modifier.rotate(rotationAngle),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskCard(
    task: TaskUiModel,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onComplete()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )
    
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            SwipeBackground(
                targetValue = dismissState.targetValue
            )
        },
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = true,
        modifier = Modifier.animateContentSize()
    ) {
        TaskCard(
            task = task.toCardData(),
            onTap = onClick,
            onCheckboxTap = onComplete
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(
    targetValue: SwipeToDismissBoxValue
) {
    val color = when (targetValue) {
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }
    
    val icon = when (targetValue) {
        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Check
        SwipeToDismissBoxValue.Settled -> null
    }
    
    val iconDescription = when (targetValue) {
        SwipeToDismissBoxValue.EndToStart -> "Delete task"
        SwipeToDismissBoxValue.StartToEnd -> "Complete task"
        SwipeToDismissBoxValue.Settled -> null
    }
    
    val alignment = when (targetValue) {
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.Settled -> Alignment.Center
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = iconDescription,
                tint = Color.White
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading tasks...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠️",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyState(
    filter: TaskFilter,
    onAddTask: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📋",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = when (filter) {
                TaskFilter.All -> "No tasks yet"
                TaskFilter.Today -> "No tasks for today"
                TaskFilter.Upcoming -> "No upcoming tasks"
                TaskFilter.HasGoal -> "No tasks linked to goals"
                TaskFilter.Recurring -> "No recurring tasks"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Tap + to create your first task",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Extension function to convert UI model to card data
private fun TaskUiModel.toCardData(): TaskCardData {
    return TaskCardData(
        id = id.toString(),
        title = title,
        quadrant = when (quadrant) {
            EisenhowerQuadrant.DO_FIRST -> Quadrant.DO_FIRST
            EisenhowerQuadrant.SCHEDULE -> Quadrant.SCHEDULE
            EisenhowerQuadrant.DELEGATE -> Quadrant.DELEGATE
            EisenhowerQuadrant.ELIMINATE -> Quadrant.ELIMINATE
        },
        isCompleted = isCompleted,
        isOverdue = isOverdue,
        dueText = dueText,
        goalName = goalName,
        hasReminder = hasReminder
    )
}

// ============ Previews ============

@Preview(showBackground = true)
@Composable
private fun TaskListScreenPreview() {
    PrioTheme {
        // For preview, we'd need mock data
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Task List Screen Preview")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    PrioTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(
                section = TaskSection(
                    quadrant = EisenhowerQuadrant.DO_FIRST,
                    tasks = emptyList(),
                    isExpanded = true
                ),
                onToggle = {}
            )
            SectionHeader(
                section = TaskSection(
                    quadrant = EisenhowerQuadrant.SCHEDULE,
                    tasks = emptyList(),
                    isExpanded = false
                ),
                onToggle = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FilterChipsRowPreview() {
    PrioTheme {
        FilterChipsRow(
            currentFilter = TaskFilter.All,
            onFilterSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    PrioTheme {
        EmptyState(
            filter = TaskFilter.All,
            onAddTask = {}
        )
    }
}

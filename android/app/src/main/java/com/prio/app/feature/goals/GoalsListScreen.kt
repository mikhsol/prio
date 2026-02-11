package com.prio.app.feature.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prio.core.common.model.GoalCategory
import com.prio.core.common.model.GoalStatus
import com.prio.core.ui.components.GoalCard
import com.prio.core.ui.components.GoalCardData
import com.prio.core.ui.theme.PrioTheme
import com.prio.core.ui.theme.SemanticColors

/**
 * Goals List Screen per 1.1.4 Goals Screens Spec.
 *
 * Implements GL-005: "Goals list accessible from main navigation"
 *
 * Features:
 * - Overview card with Active/On Track/At Risk stats + average progress ring
 * - Category filter chips (horizontal scroll, single-select, "All" default)
 * - Goal cards grouped by status: ⚠️ At Risk → ⏳ Slightly Behind → ✅ On Track
 * - Empty state with 🎯 emoji + "Create First Goal" CTA
 * - Max 10 active goals enforcement per GL-001
 *
 * @param onNavigateToGoalDetail Navigate to goal detail screen
 * @param onNavigateToCreateGoal Navigate to create goal screen
 * @param viewModel Injected via Hilt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsListScreen(
    onNavigateToGoalDetail: (Long) -> Unit = {},
    onNavigateToCreateGoal: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GoalsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle one-time effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is GoalsListEffect.NavigateToGoalDetail -> {
                    onNavigateToGoalDetail(effect.goalId)
                }
                GoalsListEffect.NavigateToCreateGoal -> {
                    onNavigateToCreateGoal()
                }
                is GoalsListEffect.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = effect.message,
                        actionLabel = effect.actionLabel
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(GoalsListEvent.OnUndoDelete)
                    }
                }
                GoalsListEffect.ShowMaxGoalsWarning -> {
                    snackbarHostState.showSnackbar(
                        message = "Maximum 10 active goals reached. Complete or delete a goal first."
                    )
                }
                GoalsListEffect.ShowCompletionConfetti -> {
                    // TODO: Trigger confetti animation (milestone 3.2.6)
                    snackbarHostState.showSnackbar(message = "🎉 Goal completed!")
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Goals",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.activeGoalCount} active goals",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // Only show FAB when goals exist; empty state has its own CTA button
            if (!uiState.isEmpty) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.onEvent(GoalsListEvent.OnCreateGoalClick) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Goal") },
                    modifier = Modifier.semantics {
                        contentDescription = if (uiState.canCreateNewGoal) {
                            "Create new goal"
                        } else {
                            "Maximum goals reached"
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
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
                        text = uiState.error ?: "Something went wrong",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.onEvent(GoalsListEvent.OnRefresh) }) {
                        Text("Retry")
                    }
                }
            }
            uiState.isEmpty -> {
                EmptyGoalsState(
                    onCreateGoal = { viewModel.onEvent(GoalsListEvent.OnCreateGoalClick) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                GoalsListContent(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun GoalsListContent(
    uiState: GoalsListUiState,
    onEvent: (GoalsListEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Overview card per 1.1.4
        item(key = "overview") {
            GoalsOverviewCard(stats = uiState.overviewStats)
        }

        // Category filter chips per 1.1.4
        item(key = "filters") {
            CategoryFilterChips(
                selectedCategory = uiState.selectedCategoryFilter,
                onCategorySelected = { category ->
                    onEvent(GoalsListEvent.OnCategoryFilterSelect(category))
                }
            )
        }

        // Sections grouped by status per 1.1.4: At Risk → Behind → On Track
        uiState.sections.forEach { section ->
            item(key = "section_header_${section.status.name}") {
                SectionHeader(
                    section = section,
                    onToggle = { onEvent(GoalsListEvent.OnSectionToggle(section.status)) }
                )
            }

            if (section.isExpanded) {
                items(
                    items = section.goals,
                    key = { "goal_${it.id}" }
                ) { goal ->
                    SwipeableGoalCard(
                        goal = goal,
                        onTap = { onEvent(GoalsListEvent.OnGoalClick(goal.id)) },
                        onDelete = { onEvent(GoalsListEvent.OnGoalDelete(goal.id)) }
                    )
                }
            }
        }

        // Bottom spacing for FAB
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Swipeable GoalCard with end-to-start swipe to delete.
 * Follows the same SwipeToDismissBox pattern used in TaskListScreen.
 *
 * The LaunchedEffect(goal.id) resets dismiss state when the card
 * re-enters composition after an undo re-insert.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableGoalCard(
    goal: GoalUiModel,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                // Only delete swipe supported for goals
                SwipeToDismissBoxValue.StartToEnd -> false
                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )

    // Reset dismiss state when card re-enters composition (e.g. after undo).
    // rememberSwipeToDismissBoxState uses rememberSaveable, so LazyList's
    // SaveableStateHolder may restore a dismissed position when the same key
    // reappears. Snap back to Settled to show the goal card properly.
    LaunchedEffect(goal.id) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete goal",
                        tint = Color.White
                    )
                }
            }
        },
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = false,
        modifier = Modifier.animateContentSize()
    ) {
        GoalCard(
            goal = GoalCardData(
                id = goal.id.toString(),
                title = goal.title,
                category = com.prio.core.ui.components.GoalCategory.valueOf(goal.category.name),
                progress = goal.progress,
                status = mapToUiStatus(goal.status),
                targetDate = goal.targetDate,
                milestonesCompleted = goal.milestonesCompleted,
                milestonesTotal = goal.milestonesTotal,
                linkedTasksCount = goal.linkedTasksCount
            ),
            onTap = onTap
        )
    }
}

/**
 * Map common model GoalStatus to UI component GoalStatus.
 */
private fun mapToUiStatus(status: GoalStatus): com.prio.core.ui.components.GoalStatus {
    return when (status) {
        GoalStatus.ON_TRACK -> com.prio.core.ui.components.GoalStatus.ON_TRACK
        GoalStatus.BEHIND -> com.prio.core.ui.components.GoalStatus.SLIGHTLY_BEHIND
        GoalStatus.AT_RISK -> com.prio.core.ui.components.GoalStatus.AT_RISK
        GoalStatus.COMPLETED -> com.prio.core.ui.components.GoalStatus.COMPLETED
    }
}

/**
 * Overview card with summary statistics.
 * Per 1.1.4: Stats boxes (Active, On Track, At Risk) + average progress ring.
 */
@Composable
private fun GoalsOverviewCard(
    stats: GoalOverviewStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Goals overview: ${stats.activeCount} active, " +
                    "${stats.onTrackCount} on track, ${stats.atRiskCount} at risk"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Average progress ring
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { stats.averageProgress.takeIf { it.isFinite() } ?: 0f },
                    modifier = Modifier.size(56.dp),
                    color = SemanticColors.onTrack,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                    strokeWidth = 6.dp
                )
                Text(
                    text = "${(stats.averageProgress.takeIf { it.isFinite() }?.times(100)?.toInt() ?: 0)}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            StatItem(
                value = stats.activeCount.toString(),
                label = "Active",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            StatItem(
                value = stats.onTrackCount.toString(),
                label = "On Track",
                color = SemanticColors.onTrack
            )
            StatItem(
                value = stats.atRiskCount.toString(),
                label = "At Risk",
                color = SemanticColors.atRisk
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

/**
 * Horizontal filter chips for goal categories.
 * Per 1.1.4: Single-select radio, "All" default.
 */
@Composable
private fun CategoryFilterChips(
    selectedCategory: GoalCategory?,
    onCategorySelected: (GoalCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        GoalCategory.entries.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    onCategorySelected(if (selectedCategory == category) null else category)
                },
                label = { Text("${category.emoji} ${category.displayName}") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

/**
 * Section header with status title, count, and expand/collapse toggle.
 */
@Composable
private fun SectionHeader(
    section: GoalSection,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${section.title} (${section.count})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() }
        )
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (section.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (section.isExpanded) "Collapse" else "Expand"
            )
        }
    }
}

/**
 * Empty state per 1.1.10: 🎯 emoji + explanatory text + "Create First Goal" CTA.
 */
@Composable
private fun EmptyGoalsState(
    onCreateGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "🎯",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Goals Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Set goals to connect your daily tasks\nto bigger outcomes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            ExtendedFloatingActionButton(
                onClick = onCreateGoal,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Create First Goal") }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyGoalsStatePreview() {
    PrioTheme {
        EmptyGoalsState(onCreateGoal = {})
    }
}

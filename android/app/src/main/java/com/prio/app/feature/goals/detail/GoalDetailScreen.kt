package com.prio.app.feature.goals.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prio.core.common.model.GoalStatus
import com.prio.core.ui.theme.SemanticColors

/**
 * Goal Detail Screen per 1.1.4 Goals Screens Spec.
 *
 * Implements GL-002 (Progress Visualization) and GL-006 (Goal Analytics).
 *
 * Features:
 * - Progress hero: 120dp circular ring, animated 800ms
 * - Tab Bar: 📋 Tasks | 🏁 Milestones | 📊 Analytics
 * - Linked tasks with quadrant badges
 * - Milestone timeline with completion checkoff
 * - AI insight card
 * - Confetti on 100% completion
 *
 * @param goalId The goal to display
 * @param onNavigateBack Back navigation
 * @param onNavigateToTask Navigate to task detail
 * @param onShowQuickCapture Show quick capture for adding linked task
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToTask: (Long) -> Unit = {},
    onShowQuickCapture: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GoalDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GoalDetailEffect.NavigateBack -> onNavigateBack()
                is GoalDetailEffect.NavigateToTask -> onNavigateToTask(effect.taskId)
                GoalDetailEffect.OpenQuickCapture -> onShowQuickCapture()
                is GoalDetailEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                GoalDetailEffect.ShowConfetti -> {
                    // TODO: Full confetti animation
                    snackbarHostState.showSnackbar("🎉 Congratulations! Goal completed!")
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
                    Text(
                        text = uiState.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(GoalDetailEvent.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(GoalDetailEvent.OnEditGoal) }) {
                        Icon(Icons.Default.Edit, "Edit goal")
                    }
                    IconButton(onClick = { viewModel.onEvent(GoalDetailEvent.OnDeleteGoal) }) {
                        Icon(Icons.Default.Delete, "Delete goal")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress Hero per 1.1.4
                item(key = "progress_hero") {
                    ProgressHero(
                        progress = uiState.progress,
                        status = uiState.status,
                        category = "${uiState.category.emoji} ${uiState.category.displayName}",
                        timeRemaining = uiState.timeRemaining,
                        targetDate = uiState.targetDate,
                        milestonesCompleted = uiState.milestonesCompleted,
                        milestonesTotal = uiState.milestonesTotal,
                        linkedTasksCount = uiState.linkedTasks.size + uiState.completedTasks.size,
                        milestoneContribution = uiState.milestoneContribution,
                        taskContribution = uiState.taskContribution
                    )
                }

                // AI Insight Card
                uiState.aiInsight?.let { insight ->
                    item(key = "ai_insight") {
                        AiInsightCard(insight = insight)
                    }
                }

                // Tab Bar per 1.1.4
                item(key = "tab_bar") {
                    TabRow(
                        selectedTabIndex = GoalDetailTab.entries.indexOf(uiState.selectedTab)
                    ) {
                        GoalDetailTab.entries.forEach { tab ->
                            Tab(
                                selected = uiState.selectedTab == tab,
                                onClick = {
                                    viewModel.onEvent(GoalDetailEvent.OnTabSelect(tab))
                                },
                                text = { Text("${tab.emoji} ${tab.label}") }
                            )
                        }
                    }
                }

                // Tab Content
                when (uiState.selectedTab) {
                    GoalDetailTab.TASKS -> {
                        // Active tasks
                        if (uiState.linkedTasks.isEmpty() && uiState.completedTasks.isEmpty()) {
                            item(key = "no_tasks") {
                                EmptyTabContent(
                                    message = "No tasks linked yet",
                                    actionLabel = "Add Task",
                                    onAction = { viewModel.onEvent(GoalDetailEvent.OnAddTask) }
                                )
                            }
                        } else {
                            item(key = "add_task_button") {
                                TextButton(
                                    onClick = { viewModel.onEvent(GoalDetailEvent.OnAddTask) }
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add linked task")
                                }
                            }

                            items(
                                items = uiState.linkedTasks,
                                key = { "task_${it.id}" }
                            ) { task ->
                                LinkedTaskCard(
                                    task = task,
                                    onClick = {
                                        viewModel.onEvent(GoalDetailEvent.OnTaskClick(task.id))
                                    }
                                )
                            }

                            // Completed tasks (collapsible)
                            if (uiState.completedTasks.isNotEmpty()) {
                                item(key = "completed_header") {
                                    TextButton(
                                        onClick = {
                                            viewModel.onEvent(GoalDetailEvent.OnToggleCompletedTasks)
                                        }
                                    ) {
                                        Text(
                                            "Completed (${uiState.completedTasks.size})",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (uiState.showCompletedTasks) {
                                    items(
                                        items = uiState.completedTasks,
                                        key = { "completed_task_${it.id}" }
                                    ) { task ->
                                        LinkedTaskCard(
                                            task = task,
                                            onClick = {
                                                viewModel.onEvent(GoalDetailEvent.OnTaskClick(task.id))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    GoalDetailTab.MILESTONES -> {
                        if (uiState.milestones.isEmpty()) {
                            item(key = "no_milestones") {
                                EmptyTabContent(
                                    message = "No milestones yet",
                                    actionLabel = "Add Milestone",
                                    onAction = { viewModel.onEvent(GoalDetailEvent.OnAddMilestone) }
                                )
                            }
                        } else {
                            items(
                                items = uiState.milestones,
                                key = { "milestone_${it.id}" }
                            ) { milestone ->
                                MilestoneTimelineItem(
                                    milestone = milestone,
                                    isLast = milestone == uiState.milestones.last(),
                                    onToggle = {
                                        viewModel.onEvent(GoalDetailEvent.OnMilestoneToggle(milestone.id))
                                    }
                                )
                            }

                            if (uiState.milestonesTotal < 5) {
                                item(key = "add_milestone") {
                                    TextButton(
                                        onClick = { viewModel.onEvent(GoalDetailEvent.OnAddMilestone) }
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add milestone (${uiState.milestonesTotal}/5)")
                                    }
                                }
                            }
                        }
                    }

                    GoalDetailTab.ANALYTICS -> {
                        item(key = "analytics_placeholder") {
                            AnalyticsContent(
                                progress = uiState.progress,
                                status = uiState.status,
                                linkedTasksCount = uiState.linkedTasks.size + uiState.completedTasks.size,
                                completedTasksCount = uiState.completedTasks.size,
                                milestonesCompleted = uiState.milestonesCompleted,
                                milestonesTotal = uiState.milestonesTotal,
                                milestoneContribution = uiState.milestoneContribution,
                                taskContribution = uiState.taskContribution
                            )
                        }
                    }
                }

                // Bottom spacer
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Add Milestone dialog (3.2.4)
    if (uiState.showAddMilestoneDialog) {
        AddMilestoneDialog(
            onConfirm = { title ->
                viewModel.onEvent(GoalDetailEvent.OnConfirmAddMilestone(title))
            },
            onDismiss = {
                viewModel.onEvent(GoalDetailEvent.OnDismissAddMilestoneDialog)
            }
        )
    }
}

/**
 * Dialog for adding a new milestone to a goal.
 * Per GL-004: Simple title input, limited to 5 milestones per goal.
 */
@Composable
private fun AddMilestoneDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var milestoneTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Milestone") },
        text = {
            Column {
                Text(
                    text = "Define a checkpoint for your goal progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = milestoneTitle,
                    onValueChange = { milestoneTitle = it },
                    placeholder = { Text("e.g., Complete first module") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(milestoneTitle) },
                enabled = milestoneTitle.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Progress Hero section per 1.1.4.
 * 120dp circular progress ring, animated 800ms decelerate.
 */
@Composable
private fun ProgressHero(
    progress: Float,
    status: GoalStatus,
    category: String,
    timeRemaining: String?,
    targetDate: String?,
    milestonesCompleted: Int,
    milestonesTotal: Int,
    linkedTasksCount: Int,
    milestoneContribution: Float = 0f,
    taskContribution: Float = 0f,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "progress_hero_animation"
    )

    val statusColor = when (status) {
        GoalStatus.ON_TRACK -> SemanticColors.onTrack
        GoalStatus.BEHIND -> SemanticColors.behind
        GoalStatus.AT_RISK -> SemanticColors.atRisk
        GoalStatus.COMPLETED -> SemanticColors.onTrack
    }

    val progressPercent = (progress * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Category label
            Text(
                text = category,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Circular progress ring (120dp per spec)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .semantics {
                        contentDescription = "$progressPercent percent complete, status: ${status.displayName}"
                    }
            ) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2

                    // Background circle
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        radius = radius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    drawArc(
                        color = statusColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$progressPercent%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = status.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time remaining
            timeRemaining?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                targetDate?.let {
                    Text(
                        text = "📅 $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (milestonesTotal > 0) {
                    Text(
                        text = "✅ $milestonesCompleted/$milestonesTotal milestones",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "📋 $linkedTasksCount tasks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Weighted progress breakdown (only when both milestones and tasks exist)
            if (milestonesTotal > 0 && linkedTasksCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Progress breakdown: " +
                                "milestones ${(milestoneContribution * 100).toInt()} percent, " +
                                "tasks ${(taskContribution * 100).toInt()} percent"
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏁 ${(milestoneContribution * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " + ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "📋 ${(taskContribution * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = " = ${progressPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * AI Insight Card per 1.1.4.
 * Context-aware suggestions based on goal status.
 */
@Composable
private fun AiInsightCard(
    insight: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "✨",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AI Insight",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

/**
 * Linked task card in goal detail.
 */
@Composable
private fun LinkedTaskCard(
    task: LinkedTaskUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (task.isCompleted) {
                    SemanticColors.onTrack
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                task.dueText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (task.isOverdue) {
                            SemanticColors.atRisk
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Text(
                text = task.quadrantEmoji,
                fontSize = 16.sp
            )
        }
    }
}

/**
 * Milestone timeline item per 1.1.4.
 * Vertical timeline with ✅ completed / ⏳ in-progress / ○ upcoming / ⚠️ overdue states.
 */
@Composable
private fun MilestoneTimelineItem(
    milestone: MilestoneUiModel,
    isLast: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Text(
                text = milestone.state.emoji,
                fontSize = 20.sp
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = Color.LightGray,
                            start = center.copy(y = 0f),
                            end = center.copy(y = size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = milestone.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (milestone.state == MilestoneState.IN_PROGRESS) FontWeight.SemiBold else FontWeight.Normal,
                textDecoration = if (milestone.isCompleted) TextDecoration.LineThrough else null,
                color = if (milestone.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            milestone.targetDate?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (milestone.state == MilestoneState.OVERDUE) {
                        SemanticColors.atRisk
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            milestone.completedAt?.let {
                Text(
                    text = "Completed $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = SemanticColors.onTrack
                )
            }
        }

        // Checkbox
        Checkbox(
            checked = milestone.isCompleted,
            onCheckedChange = { onToggle() }
        )
    }
}

/**
 * Simple analytics content for Goal Detail.
 * Per GL-006: Basic per-goal analytics tab.
 */
@Composable
private fun AnalyticsContent(
    progress: Float,
    status: GoalStatus,
    linkedTasksCount: Int,
    completedTasksCount: Int,
    milestonesCompleted: Int,
    milestonesTotal: Int,
    milestoneContribution: Float = 0f,
    taskContribution: Float = 0f,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Goal Analytics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        // Progress summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticRow("Overall Progress", "${(progress * 100).toInt()}%")
                AnalyticRow("Status", status.displayName)
                AnalyticRow("Tasks Completed", "$completedTasksCount / $linkedTasksCount")
                if (milestonesTotal > 0) {
                    AnalyticRow("Milestones", "$milestonesCompleted / $milestonesTotal")
                }
                // Weighted breakdown (only when both milestones and tasks exist)
                if (milestonesTotal > 0 && linkedTasksCount > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    AnalyticRow(
                        "🏁 Milestone Contribution",
                        "${(milestoneContribution * 100).toInt()}% (×60% weight)"
                    )
                    AnalyticRow(
                        "📋 Task Contribution",
                        "${(taskContribution * 100).toInt()}% (×40% weight)"
                    )
                }
                val completionRate = if (linkedTasksCount > 0) {
                    (completedTasksCount.toFloat() / linkedTasksCount * 100).toInt()
                } else 0
                AnalyticRow("Task Completion Rate", "$completionRate%")
            }
        }
    }
}

@Composable
private fun AnalyticRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Empty state for tab content.
 */
@Composable
private fun EmptyTabContent(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onAction) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(actionLabel)
        }
    }
}

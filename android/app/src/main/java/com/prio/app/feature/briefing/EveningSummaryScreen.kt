package com.prio.app.feature.briefing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Evening Summary Screen per 1.1.7 Evening Summary spec.
 *
 * Task 3.4.3: Full evening summary view with:
 * - Accomplishments (completed tasks list)
 * - Not Done tasks with action selection (move/reschedule/drop)
 * - Goal Progress delta
 * - Tomorrow's Preview
 * - AI Reflection
 * - End-of-day nudge (Maya persona)
 * - "Close Day & Plan Tomorrow" CTA
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EveningSummaryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTask: (Long) -> Unit = {},
    onNavigateToGoal: (Long) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: EveningSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClosedAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is EveningSummaryEffect.NavigateToTask -> onNavigateToTask(effect.taskId)
                is EveningSummaryEffect.NavigateToGoal -> onNavigateToGoal(effect.goalId)
                EveningSummaryEffect.DayClosedAnimation -> {
                    showClosedAnimation = true
                }
                is EveningSummaryEffect.ShowError -> {
                    // TODO: Show snackbar
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evening Summary") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // Day Closed overlay animation per 1.1.7 spec
        if (showClosedAnimation) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = showClosedAnimation,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Day Closed!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "See you tomorrow morning ☀️",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            return@Scaffold
        }

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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            uiState.summary != null -> {
                EveningSummaryContent(
                    summary = uiState.summary!!,
                    isClosingDay = uiState.isClosingDay,
                    onCloseDay = { viewModel.onEvent(EveningSummaryEvent.OnCloseDay) },
                    onTaskActionChanged = { taskId, action ->
                        viewModel.onEvent(EveningSummaryEvent.OnTaskActionChanged(taskId, action))
                    },
                    onTaskTap = { taskId -> viewModel.onEvent(EveningSummaryEvent.OnTaskTap(taskId)) },
                    onGoalTap = { goalId -> viewModel.onEvent(EveningSummaryEvent.OnGoalTap(goalId)) },
                    onSettingsTap = onNavigateToSettings,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun EveningSummaryContent(
    summary: EveningSummaryData,
    isClosingDay: Boolean,
    onCloseDay: () -> Unit,
    onTaskActionChanged: (Long, NotDoneAction) -> Unit,
    onTaskTap: (Long) -> Unit,
    onGoalTap: (Long) -> Unit,
    onSettingsTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eveningGradient = listOf(
        Color(0xFFE0E7FF), // Indigo-100
        Color(0xFFC7D2FE)  // Indigo-200
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==================== Header ====================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Evening summary. You completed ${summary.tasksCompletedCount} of ${summary.totalTodayTasks} tasks."
                    },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush = Brush.verticalGradient(eveningGradient))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🌙", fontSize = 32.sp)
                        Text(
                            text = "DAY COMPLETE",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = summary.date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4B5563)
                        )
                    }
                }
            }
        }

        // ==================== Accomplishments ====================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✅ ACCOMPLISHMENTS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = summary.completionMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (summary.completedTasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        summary.completedTasks.forEach { task ->
                            Text(
                                text = "✓ ${task.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF10B981),
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .clickable { onTaskTap(task.id) }
                            )
                        }
                        if (summary.tasksCompletedCount > summary.completedTasks.size) {
                            Text(
                                text = "... and ${summary.tasksCompletedCount - summary.completedTasks.size} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==================== Not Done ====================
        if (summary.notDoneTasks.isNotEmpty()) {
            item {
                Text(
                    text = "📋 NOT DONE (${summary.notDoneTasks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            summary.notDoneTasks.forEach { task ->
                item(key = "notdone_${task.id}") {
                    NotDoneTaskCard(
                        task = task,
                        onActionChanged = { action -> onTaskActionChanged(task.id, action) },
                        onTap = { onTaskTap(task.id) }
                    )
                }
            }
        }

        // ==================== Goal Progress ====================
        if (summary.goalSpotlight != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGoalTap(summary.goalSpotlight.id) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎯 GOAL PROGRESS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${summary.goalSpotlight.title}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { summary.goalSpotlight.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${summary.goalSpotlight.progress}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        // ==================== Tomorrow Preview ====================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "👀 TOMORROW'S PREVIEW",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (summary.tomorrowPreview.topPriorityTitle != null) {
                        Text(
                            text = "🔴 Top Priority: \"${summary.tomorrowPreview.topPriorityTitle}\"",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "📅 ${summary.tomorrowPreview.meetingCount} meeting${if (summary.tomorrowPreview.meetingCount != 1) "s" else ""} scheduled",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "📋 ${summary.tomorrowPreview.taskCount} task${if (summary.tomorrowPreview.taskCount != 1) "s" else ""}" +
                            if (summary.tomorrowPreview.carryOverCount > 0) " (${summary.tomorrowPreview.carryOverCount} carried over)" else "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // ==================== AI Reflection ====================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0F9FF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💭 AI REFLECTION",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = summary.reflection,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF374151)
                    )
                }
            }
        }

        // ==================== End-of-Day Nudge (Maya persona) - Task 3.4.5 ====================
        item {
            EndOfDayNudge(onSettingsTap = onSettingsTap)
        }

        // ==================== Close Day CTA ====================
        item {
            if (!summary.isDayClosed) {
                Button(
                    onClick = onCloseDay,
                    enabled = !isClosingDay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics { contentDescription = "Close day and plan tomorrow. Double tap to confirm." },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5) // Indigo
                    )
                ) {
                    if (isClosingDay) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "✓ Close Day & Plan Tomorrow",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "✓ Day closed",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ==================== Not Done Task Card ====================

@Composable
private fun NotDoneTaskCard(
    task: NotDoneTaskItem,
    onActionChanged: (NotDoneAction) -> Unit,
    onTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "○ ${task.title}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NotDoneActionOption(
                    label = "Move to tomorrow",
                    isSelected = task.selectedAction == NotDoneAction.MOVE_TO_TOMORROW,
                    onClick = { onActionChanged(NotDoneAction.MOVE_TO_TOMORROW) }
                )
                NotDoneActionOption(
                    label = "Reschedule",
                    isSelected = task.selectedAction == NotDoneAction.RESCHEDULE,
                    onClick = { onActionChanged(NotDoneAction.RESCHEDULE) }
                )
                NotDoneActionOption(
                    label = "Drop",
                    isSelected = task.selectedAction == NotDoneAction.DROP,
                    onClick = { onActionChanged(NotDoneAction.DROP) }
                )
            }
        }
    }
}

@Composable
private fun NotDoneActionOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .selectable(selected = isSelected, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== End-of-Day Nudge (Maya persona - Task 3.4.5) ====================

/**
 * End-of-day nudge per 1.1.7 Maya's End-of-Day Nudge spec.
 *
 * Reminds users to disconnect and sets work-life boundaries.
 * Configurable end-of-day time via Settings.
 */
@Composable
private fun EndOfDayNudge(
    endOfDayTime: String = "6:00 PM",
    onSettingsTap: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEF3C7).copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Color(0xFF92400E),
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "🏠 Time to disconnect! You've earned your rest.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF92400E)
                    )
                    Text(
                        text = "End-of-day set for $endOfDayTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB45309)
                    )
                }
            }
            IconButton(onClick = onSettingsTap) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFFB45309),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

package com.prio.app.feature.briefing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
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
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.ui.theme.QuadrantColors
import com.prio.core.ui.theme.SemanticColors

/**
 * Morning Briefing Screen per 1.1.5 Today Dashboard & Morning Briefing spec.
 *
 * Task 3.4.2: Full briefing detail view with:
 * - Greeting + date header
 * - Today's Focus summary
 * - Top 3 priority tasks (Q1 + Q2)
 * - Schedule overview
 * - Goal spotlight
 * - AI insight
 * - "Start My Day" CTA
 *
 * Accessibility: Full TalkBack support per 1.1.5 accessibility section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningBriefingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTask: (Long) -> Unit = {},
    onNavigateToGoal: (Long) -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    viewModel: MorningBriefingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MorningBriefingEffect.NavigateToTask -> onNavigateToTask(effect.taskId)
                is MorningBriefingEffect.NavigateToGoal -> onNavigateToGoal(effect.goalId)
                MorningBriefingEffect.NavigateToTasks -> onNavigateToTasks()
                MorningBriefingEffect.NavigateToCalendar -> onNavigateToCalendar()
                MorningBriefingEffect.BriefingDismissed -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Morning Briefing") },
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
            uiState.briefing != null -> {
                val briefing = uiState.briefing ?: return@Scaffold
                MorningBriefingContent(
                    briefing = briefing,
                    isRead = uiState.isRead,
                    onStartMyDay = { viewModel.onEvent(MorningBriefingEvent.OnStartMyDay) },
                    onTaskTap = { taskId -> viewModel.onEvent(MorningBriefingEvent.OnTaskTap(taskId)) },
                    onGoalTap = { goalId -> viewModel.onEvent(MorningBriefingEvent.OnGoalTap(goalId)) },
                    onSeeAllTasks = { viewModel.onEvent(MorningBriefingEvent.OnSeeAllTasks) },
                    onFullCalendarView = { viewModel.onEvent(MorningBriefingEvent.OnFullCalendarView) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun MorningBriefingContent(
    briefing: MorningBriefingData,
    isRead: Boolean,
    onStartMyDay: () -> Unit,
    onTaskTap: (Long) -> Unit,
    onGoalTap: (Long) -> Unit,
    onSeeAllTasks: () -> Unit,
    onFullCalendarView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val morningGradient = listOf(
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.tertiary
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==================== Briefing Hero Card ====================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = buildString {
                            append("Morning briefing for ${briefing.date}. ")
                            append("You have ${briefing.quadrantCounts.doFirst} urgent tasks ")
                            append("and ${briefing.meetingCount} meetings. ")
                            append("Top task: ${briefing.topPriorities.firstOrNull()?.title ?: "none"}.")
                        }
                    },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush = Brush.verticalGradient(morningGradient))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header with privacy badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "☀️", fontSize = 20.sp)
                                Text(
                                    text = "YOUR MORNING BRIEFING",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Private",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = briefing.date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Today's Focus summary
                        Text(
                            text = "📊 Today's Focus",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val focusSummary = buildString {
                            append("You have ${briefing.quadrantCounts.doFirst} urgent task")
                            if (briefing.quadrantCounts.doFirst != 1) append("s")
                            append(" and ${briefing.meetingCount} meeting")
                            if (briefing.meetingCount != 1) append("s")
                            append(".")
                            if (briefing.topPriorities.isNotEmpty()) {
                                append("\nYour most important task: \"${briefing.topPriorities.first().title}\"")
                            }
                        }
                        Text(
                            text = focusSummary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // ==================== Top Priorities ====================
        item {
            SectionHeader(
                emoji = "🔴",
                title = "DO FIRST (${briefing.topPriorities.size})",
                actionLabel = "See All",
                onAction = onSeeAllTasks
            )
        }

        if (briefing.topPriorities.isEmpty()) {
            item {
                EmptySection(
                    message = "No urgent tasks today! Great time for goal work.",
                    icon = "✨"
                )
            }
        } else {
            briefing.topPriorities.forEachIndexed { index, task ->
                item(key = "priority_${task.id}") {
                    PriorityTaskRow(
                        index = index + 1,
                        task = task,
                        onClick = { onTaskTap(task.id) }
                    )
                }
            }
        }

        // ==================== Schedule Overview ====================
        item {
            SectionHeader(
                emoji = "📅",
                title = "SCHEDULE OVERVIEW",
                actionLabel = "Full View",
                onAction = onFullCalendarView
            )
        }

        if (briefing.schedulePreview.isEmpty()) {
            item {
                EmptySection(
                    message = "No meetings scheduled. Perfect for deep work!",
                    icon = "🎯"
                )
            }
        } else {
            briefing.schedulePreview.forEach { event ->
                item(key = "schedule_${event.time}") {
                    ScheduleRow(event = event)
                }
            }
            if (briefing.focusHoursAvailable > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "░░░", color = SemanticColors.success)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${"%.0f".format(briefing.focusHoursAvailable)} hours focus time available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SemanticColors.success,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ==================== Goal Spotlight ====================
        if (briefing.goalSpotlight != null) {
            item {
                SectionHeader(emoji = "🎯", title = "GOAL SPOTLIGHT")
            }
            item {
                GoalSpotlightCard(
                    goal = briefing.goalSpotlight,
                    onClick = { onGoalTap(briefing.goalSpotlight.id) }
                )
            }
        }

        // ==================== AI Insight ====================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "💡 AI Insight",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = briefing.insight,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ==================== Start My Day CTA ====================
        item {
            AnimatedVisibility(visible = !isRead, enter = fadeIn(), exit = fadeOut()) {
                Button(
                    onClick = onStartMyDay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics { contentDescription = "Start my day. Double tap to dismiss briefing." },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Start My Day →",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = isRead) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "✓ Briefing read",
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

// ==================== Section Components ====================

@Composable
private fun SectionHeader(
    emoji: String,
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$emoji $title",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

@Composable
private fun PriorityTaskRow(
    index: Int,
    task: TopPriorityItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val quadrantColor = when (task.quadrant) {
                EisenhowerQuadrant.DO_FIRST -> QuadrantColors.doFirst
                EisenhowerQuadrant.SCHEDULE -> QuadrantColors.schedule
                EisenhowerQuadrant.DELEGATE -> QuadrantColors.delegate
                EisenhowerQuadrant.ELIMINATE -> QuadrantColors.eliminate
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(quadrantColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = task.dueText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScheduleRow(event: SchedulePreviewItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = event.time,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(48.dp)
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        )
        Column {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${event.durationMinutes} min",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalSpotlightCard(
    goal: GoalSpotlightData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = goal.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { goal.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val statusColor = if (goal.isAtRisk) SemanticColors.error else SemanticColors.success
                val statusText = if (goal.isAtRisk) "🔴 At risk" else "🟢 On track"
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
                Text(
                    text = "${goal.progress}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (goal.nextAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = goal.nextAction,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptySection(message: String, icon: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

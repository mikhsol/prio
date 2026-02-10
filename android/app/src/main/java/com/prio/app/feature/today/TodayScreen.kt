package com.prio.app.feature.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prio.core.ui.theme.PrioTheme
import com.prio.core.ui.theme.QuadrantColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Today / Dashboard Screen per 1.1.5 Today Dashboard Briefing Spec.
 * 
 * Entry point for CB-001: Morning Daily Briefing.
 * 
 * Features (connected to [TodayViewModel]):
 * - Morning/Evening Briefing Card (prominent at top)
 * - Eisenhower Quick View (live quadrant counts)
 * - Today's Top 3 Priorities (Q1/Q2 tasks)
 * - Calendar Timeline (upcoming events)
 * - Goal Progress Highlights
 * 
 * GAP-H01: Replaced hardcoded placeholder with ViewModel-backed live data.
 * 
 * @param onNavigateToTask Navigate to task detail
 * @param onNavigateToGoal Navigate to goal detail
 * @param onNavigateToMeeting Navigate to meeting detail
 * @param onNavigateToTasks Navigate to full tasks list
 * @param onNavigateToMorningBriefing Navigate to morning briefing
 * @param onNavigateToEveningSummary Navigate to evening summary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onNavigateToTask: (Long) -> Unit = {},
    onNavigateToGoal: (Long) -> Unit = {},
    onNavigateToMeeting: (Long) -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToMorningBriefing: () -> Unit = {},
    onNavigateToEveningSummary: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val formattedDate = today.format(DateTimeFormatter.ofPattern("MMMM d"))

    // Consume navigation effects
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TodayEffect.NavigateToTask -> onNavigateToTask(effect.taskId)
                is TodayEffect.NavigateToGoal -> onNavigateToGoal(effect.goalId)
                is TodayEffect.NavigateToMeeting -> onNavigateToMeeting(effect.meetingId)
                TodayEffect.NavigateToTasks -> onNavigateToTasks()
                TodayEffect.NavigateToMorningBriefing -> onNavigateToMorningBriefing()
                TodayEffect.NavigateToEveningSummary -> onNavigateToEveningSummary()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.greeting,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$dayOfWeek, $formattedDate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "Something went wrong",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pull down to retry",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Briefing Card — morning or evening
                    item {
                        BriefingCard(
                            title = if (uiState.isMorning) "Morning Briefing" else "Evening Summary",
                            subtitle = uiState.briefingSubtitle.ifEmpty { "Tap to view your briefing" },
                            icon = if (uiState.isMorning) Icons.Default.WbSunny else Icons.Default.Nightlight,
                            onClick = { viewModel.onEvent(TodayEvent.OnBriefingCardTap) }
                        )
                    }

                    // Eisenhower Quick View — live quadrant counts
                    item {
                        Text(
                            text = "Task Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    item {
                        EisenhowerQuickView(
                            doCount = uiState.quadrantCounts.doFirst,
                            scheduleCount = uiState.quadrantCounts.schedule,
                            delegateCount = uiState.quadrantCounts.delegate,
                            eliminateCount = uiState.quadrantCounts.eliminate,
                            onQuadrantClick = { viewModel.onEvent(TodayEvent.OnQuadrantTap) }
                        )
                    }

                    // Today's Top Priorities — real tasks
                    if (uiState.topPriorities.isNotEmpty()) {
                        item {
                            Text(
                                text = "Today's Focus",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        item {
                            TopPrioritiesCard(
                                priorities = uiState.topPriorities,
                                onTaskClick = { taskId ->
                                    viewModel.onEvent(TodayEvent.OnTaskTap(taskId))
                                }
                            )
                        }
                    }

                    // Goal Progress — real goals
                    if (uiState.goalSummaries.isNotEmpty()) {
                        item {
                            Text(
                                text = "Goal Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        item {
                            GoalProgressRow(
                                goals = uiState.goalSummaries,
                                onGoalClick = { goalId ->
                                    viewModel.onEvent(TodayEvent.OnGoalTap(goalId))
                                }
                            )
                        }
                    }

                    // Upcoming Events — real meetings
                    if (uiState.upcomingEvents.isNotEmpty()) {
                        item {
                            Text(
                                text = "Upcoming",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        item {
                            UpcomingEventsCard(
                                events = uiState.upcomingEvents,
                                onEventClick = { meetingId ->
                                    viewModel.onEvent(TodayEvent.OnMeetingTap(meetingId))
                                }
                            )
                        }
                    }

                    // Empty state when no data at all
                    if (uiState.topPriorities.isEmpty() && uiState.goalSummaries.isEmpty() && uiState.upcomingEvents.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "🎯",
                                        style = MaterialTheme.typography.headlineLarge
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Your day is clear!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Create tasks or goals to get started",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Bottom spacing for FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefingCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun EisenhowerQuickView(
    doCount: Int,
    scheduleCount: Int,
    delegateCount: Int,
    eliminateCount: Int,
    onQuadrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuadrantCard(
            label = "Do",
            count = doCount,
            color = QuadrantColors.doFirst,
            modifier = Modifier.weight(1f),
            onClick = onQuadrantClick
        )
        QuadrantCard(
            label = "Schedule",
            count = scheduleCount,
            color = QuadrantColors.schedule,
            modifier = Modifier.weight(1f),
            onClick = onQuadrantClick
        )
        QuadrantCard(
            label = "Delegate",
            count = delegateCount,
            color = QuadrantColors.delegate,
            modifier = Modifier.weight(1f),
            onClick = onQuadrantClick
        )
        QuadrantCard(
            label = "Later",
            count = eliminateCount,
            color = QuadrantColors.eliminate,
            modifier = Modifier.weight(1f),
            onClick = onQuadrantClick
        )
    }
}

@Composable
private fun QuadrantCard(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopPrioritiesCard(
    priorities: List<TopPriorityUi>,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            priorities.forEachIndexed { index, priority ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTaskClick(priority.id) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(QuadrantColors.doFirst),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = priority.title,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        priority.dueTime?.let { time ->
                            Text(
                                text = "Due $time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalProgressRow(
    goals: List<GoalSummaryUi>,
    onGoalClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(goals.size) { index ->
            val goal = goals[index]
            GoalProgressCard(
                name = goal.title,
                progress = goal.progress,
                onClick = { onGoalClick(goal.id) }
            )
        }
    }
}

@Composable
private fun GoalProgressCard(
    name: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class EventType {
    MEETING, TASK, BREAK
}

@Composable
private fun UpcomingEventsCard(
    events: List<UpcomingEventUi>,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            events.forEach { event ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEventClick(event.id) }
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Text(
                        text = event.time,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    PrioTheme {
        TodayScreen()
    }
}

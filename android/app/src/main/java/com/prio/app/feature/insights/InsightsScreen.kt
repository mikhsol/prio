package com.prio.app.feature.insights

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.FlagCircle
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prio.core.common.model.GoalStatus
import com.prio.core.ui.theme.PrioTheme
import com.prio.core.ui.theme.QuadrantColors
import com.prio.core.ui.theme.SemanticColors

/**
 * Insights (Analytics) Screen — Milestone 3.5.
 *
 * Combines:
 * - 3.5.2: Simple stats (weekly summary cards)
 * - 3.5.3: Task completion chart (7-day bar chart with quadrant colors)
 * - 3.5.4: Goal progress trend (Jordan persona: streak counter, goal progress arrows)
 *
 * Design follows Material 3 guidelines and Prio UX Design System.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGoal: (Long) -> Unit = {},
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is InsightsEffect.NavigateToGoal -> onNavigateToGoal(effect.goalId)
                is InsightsEffect.ShowSnackbar -> { /* Snackbar handling */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
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
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 3.5.2 — Weekly Summary Stats
                item {
                    SectionHeader(title = "This Week", icon = Icons.Filled.Timeline)
                }
                item {
                    WeeklyStatsRow(uiState)
                }

                // 3.5.3 — Task Completion Chart
                item {
                    SectionHeader(title = "Completion Chart", icon = Icons.Filled.TaskAlt)
                }
                item {
                    CompletionChart(chartData = uiState.chartData)
                }

                // Quadrant Breakdown
                item {
                    QuadrantBreakdownCard(breakdown = uiState.quadrantBreakdown)
                }

                // 3.5.4 — Streaks (Jordan persona)
                item {
                    SectionHeader(
                        title = "Streaks & Goals",
                        icon = Icons.Filled.LocalFireDepartment
                    )
                }
                item {
                    StreakCard(
                        currentStreak = uiState.currentStreak,
                        longestStreak = uiState.longestStreak
                    )
                }

                // Goal progress
                item {
                    GoalsSummaryCard(
                        activeGoals = uiState.activeGoals,
                        onTrack = uiState.onTrackGoals,
                        atRisk = uiState.atRiskGoals,
                        completedThisMonth = uiState.completedGoalsThisMonth
                    )
                }

                if (uiState.goalProgressItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "Goal Progress",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(
                        items = uiState.goalProgressItems,
                        key = { it.id }
                    ) { goal ->
                        GoalProgressRow(
                            goal = goal,
                            onClick = { viewModel.onEvent(InsightsEvent.OnGoalClick(goal.id)) }
                        )
                    }
                }

                // AI Accuracy
                item {
                    SectionHeader(title = "AI Performance", icon = Icons.Filled.Psychology)
                }
                item {
                    AiAccuracyCard(
                        accuracy = uiState.aiAccuracy,
                        isOnTarget = uiState.isAiAccuracyOnTarget
                    )
                }

                // Bottom spacing
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

// ============================================================
// Section Header
// ============================================================

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ============================================================
// 3.5.2 — Weekly Stats Row
// ============================================================

@Composable
private fun WeeklyStatsRow(state: InsightsUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "Completed",
            value = state.weeklyTasksCompleted.toString(),
            subtitle = "of ${state.weeklyTasksCreated} created",
            color = SemanticColors.success,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Today",
            value = state.todayTasksCompleted.toString(),
            subtitle = "${state.todayTasksCreated} created",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Rate",
            value = "${(state.completionRate * 100).toInt()}%",
            subtitle = if (state.isCompletionOnTarget) "On target" else "Below 60%",
            color = if (state.isCompletionOnTarget) SemanticColors.success else SemanticColors.warning,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================
// 3.5.3 — Task Completion Chart (7-day bar chart)
// ============================================================

@Composable
private fun CompletionChart(chartData: List<DayChartPoint>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (chartData.isEmpty() || chartData.all { it.tasksCompleted == 0 }) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.TaskAlt,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Complete tasks to see your chart",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                val maxValue = remember(chartData) {
                    (chartData.maxOfOrNull { it.tasksCompleted } ?: 1).coerceAtLeast(1)
                }

                // Bar chart
                val q1Color = QuadrantColors.doFirst
                val q2Color = QuadrantColors.schedule
                val q3Color = QuadrantColors.delegate
                val q4Color = QuadrantColors.eliminate
                val todayHighlight = MaterialTheme.colorScheme.primaryContainer
                val barCorner = 6f

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .semantics {
                            contentDescription = "Bar chart showing tasks completed over the past 7 days"
                        }
                ) {
                    val barCount = chartData.size
                    val spacing = 12.dp.toPx()
                    val totalSpacing = spacing * (barCount + 1)
                    val barWidth = (size.width - totalSpacing) / barCount
                    val chartHeight = size.height - 24.dp.toPx() // Reserve space for labels

                    chartData.forEachIndexed { index, point ->
                        val x = spacing + index * (barWidth + spacing)

                        // Highlight today background
                        if (point.isToday) {
                            drawRoundRect(
                                color = todayHighlight,
                                topLeft = Offset(x - 4.dp.toPx(), 0f),
                                size = Size(barWidth + 8.dp.toPx(), chartHeight + 20.dp.toPx()),
                                cornerRadius = CornerRadius(8.dp.toPx())
                            )
                        }

                        if (point.tasksCompleted > 0) {
                            val totalBarHeight =
                                (point.tasksCompleted.toFloat() / maxValue) * chartHeight

                            // Draw stacked bar: Q1 on bottom, Q4 on top
                            var yOffset = chartHeight - totalBarHeight
                            val segments = listOf(
                                point.q1 to q1Color,
                                point.q2 to q2Color,
                                point.q3 to q3Color,
                                point.q4 to q4Color
                            )
                            for ((count, color) in segments) {
                                if (count > 0) {
                                    val segHeight =
                                        (count.toFloat() / point.tasksCompleted) * totalBarHeight
                                    drawRoundRect(
                                        color = color,
                                        topLeft = Offset(x, yOffset),
                                        size = Size(barWidth, segHeight),
                                        cornerRadius = CornerRadius(barCorner)
                                    )
                                    yOffset += segHeight
                                }
                            }
                        }
                    }
                }

                // Day labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    chartData.forEach { point ->
                        Text(
                            text = point.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (point.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (point.isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ChartLegendItem(color = QuadrantColors.doFirst, label = "Do First")
                    ChartLegendItem(color = QuadrantColors.schedule, label = "Schedule")
                    ChartLegendItem(color = QuadrantColors.delegate, label = "Delegate")
                    ChartLegendItem(color = QuadrantColors.eliminate, label = "Later")
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// Quadrant Breakdown Card
// ============================================================

@Composable
private fun QuadrantBreakdownCard(breakdown: QuadrantBreakdownUi) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quadrant Breakdown (7 days)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuadrantStatItem("🔴", "Do First", breakdown.q1)
                QuadrantStatItem("🟡", "Schedule", breakdown.q2)
                QuadrantStatItem("🟠", "Delegate", breakdown.q3)
                QuadrantStatItem("⚪", "Later", breakdown.q4)
            }
        }
    }
}

@Composable
private fun QuadrantStatItem(emoji: String, label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 20.sp)
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// 3.5.4 — Streak Card (Jordan persona)
// ============================================================

@Composable
private fun StreakCard(currentStreak: Int, longestStreak: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (currentStreak > 0) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fire icon with streak count
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = "Current streak",
                    tint = if (currentStreak > 0) Color(0xFFFF6B35) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$currentStreak",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (currentStreak > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "day streak",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .height(48.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Longest streak
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = "Longest streak",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$longestStreak",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "longest",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================
// Goals Summary Card
// ============================================================

@Composable
private fun GoalsSummaryCard(
    activeGoals: Int,
    onTrack: Int,
    atRisk: Int,
    completedThisMonth: Int
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GoalStatItem(
                value = activeGoals,
                label = "Active",
                color = MaterialTheme.colorScheme.primary
            )
            GoalStatItem(
                value = onTrack,
                label = "On Track",
                color = SemanticColors.onTrack
            )
            GoalStatItem(
                value = atRisk,
                label = "At Risk",
                color = SemanticColors.atRisk
            )
            GoalStatItem(
                value = completedThisMonth,
                label = "Done",
                color = SemanticColors.success
            )
        }
    }
}

@Composable
private fun GoalStatItem(value: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
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

// ============================================================
// Goal Progress Row
// ============================================================

@Composable
private fun GoalProgressRow(
    goal: GoalProgressUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (goal.status) {
                            GoalStatus.ON_TRACK -> SemanticColors.onTrack
                            GoalStatus.BEHIND -> SemanticColors.behind
                            GoalStatus.AT_RISK -> SemanticColors.atRisk
                            GoalStatus.COMPLETED -> SemanticColors.success
                        }
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Progress bar
                val animatedProgress by animateFloatAsState(
                    targetValue = goal.progress,
                    animationSpec = tween(600),
                    label = "goal_progress"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when (goal.status) {
                        GoalStatus.ON_TRACK -> SemanticColors.onTrack
                        GoalStatus.BEHIND -> SemanticColors.behind
                        GoalStatus.AT_RISK -> SemanticColors.atRisk
                        GoalStatus.COMPLETED -> SemanticColors.success
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(goal.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    goal.targetDateText?.let { date ->
                        Text(
                            text = "Target: $date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Weekly delta arrow
            if (goal.weeklyDelta != 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (goal.weeklyDelta > 0) "↑${goal.weeklyDelta}%" else "↓${-goal.weeklyDelta}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (goal.weeklyDelta > 0) SemanticColors.success else SemanticColors.error
                )
            }
        }
    }
}

// ============================================================
// AI Accuracy Card
// ============================================================

@Composable
private fun AiAccuracyCard(accuracy: Float, isOnTarget: Boolean) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Psychology,
                contentDescription = "AI accuracy",
                tint = if (isOnTarget) SemanticColors.success else SemanticColors.warning,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Eisenhower AI Accuracy",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isOnTarget) {
                        "Classifications match your preferences"
                    } else {
                        "The AI is still learning your style"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${(accuracy * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isOnTarget) SemanticColors.success else SemanticColors.warning
            )
        }
    }
}

// ============================================================
// Previews
// ============================================================

@Preview(showBackground = true)
@Composable
private fun InsightsScreenPreview() {
    PrioTheme {
        // Preview with mock data would require a full ViewModel mock
        // Using standalone component previews instead
        StatCard(
            label = "Completed",
            value = "12",
            subtitle = "of 18 created",
            color = SemanticColors.success
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StreakCardPreview() {
    PrioTheme {
        StreakCard(currentStreak = 5, longestStreak = 12)
    }
}

@Preview(showBackground = true)
@Composable
private fun GoalProgressRowPreview() {
    PrioTheme {
        GoalProgressRow(
            goal = GoalProgressUiModel(
                id = 1,
                title = "Get promoted to Senior Engineer",
                progress = 0.65f,
                status = GoalStatus.ON_TRACK,
                targetDateText = "Jun 2026",
                weeklyDelta = 5
            ),
            onClick = {}
        )
    }
}

package com.prio.app.feature.goals

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Rocket
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prio.core.ui.theme.PrioTheme

/**
 * Goals List Screen per 1.1.4 Goals Screens Spec.
 * 
 * Implements GL-005: "Goals list accessible from main navigation"
 * 
 * Features:
 * - Active goals with progress cards
 * - Visual progress indicators
 * - Goal categories/tags
 * - Add new goal FAB
 * - Goal quick stats
 * 
 * This is a PLACEHOLDER implementation for Milestone 3.1.5.
 * Full implementation in Milestone 3.2 (Goals Plugin).
 * 
 * @param onNavigateToGoalDetail Navigate to goal detail screen
 * @param onNavigateToCreateGoal Navigate to create goal screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsListScreen(
    onNavigateToGoalDetail: (Long) -> Unit = {},
    onNavigateToCreateGoal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Sample data - will be replaced with real data from ViewModel
    val goals = remember {
        listOf(
            GoalData(
                id = 1L,
                title = "Launch Prio MVP",
                description = "Ship first version to Play Store",
                progress = 0.75f,
                linkedTasks = 12,
                completedTasks = 9,
                icon = Icons.Outlined.Rocket
            ),
            GoalData(
                id = 2L,
                title = "Read 12 Books This Year",
                description = "One book per month",
                progress = 0.33f,
                linkedTasks = 12,
                completedTasks = 4,
                icon = Icons.Outlined.EmojiEvents
            ),
            GoalData(
                id = 3L,
                title = "Get Promoted to Senior",
                description = "Demonstrate leadership and technical growth",
                progress = 0.5f,
                linkedTasks = 8,
                completedTasks = 4,
                icon = Icons.Outlined.TrendingUp
            ),
            GoalData(
                id = 4L,
                title = "Exercise 4x per Week",
                description = "Build consistent workout habit",
                progress = 0.6f,
                linkedTasks = 4,
                completedTasks = 2,
                icon = Icons.Outlined.EmojiEvents
            )
        )
    }
    
    Scaffold(
        modifier = modifier,
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
                            text = "${goals.size} active goals",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (goals.isEmpty()) {
            EmptyGoalsState(
                onCreateGoal = onNavigateToCreateGoal,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary card
                item {
                    GoalsSummaryCard(
                        totalGoals = goals.size,
                        averageProgress = goals.map { it.progress }.average().toFloat(),
                        totalTasks = goals.sumOf { it.linkedTasks }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Active Goals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                
                items(goals) { goal ->
                    GoalCard(
                        goal = goal,
                        onClick = { onNavigateToGoalDetail(goal.id) }
                    )
                }
                
                // Bottom spacing for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

data class GoalData(
    val id: Long,
    val title: String,
    val description: String,
    val progress: Float,
    val linkedTasks: Int,
    val completedTasks: Int,
    val icon: ImageVector = Icons.Default.Flag
)

@Composable
private fun GoalsSummaryCard(
    totalGoals: Int,
    averageProgress: Float,
    totalTasks: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = totalGoals.toString(),
                label = "Goals"
            )
            StatItem(
                value = "${(averageProgress * 100).toInt()}%",
                label = "Avg Progress"
            )
            StatItem(
                value = totalTasks.toString(),
                label = "Tasks"
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
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
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun GoalCard(
    goal: GoalData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics { contentDescription = "Goal: ${goal.title}, ${(goal.progress * 100).toInt()}% complete" },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = goal.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.size(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = goal.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Text(
                    text = "${(goal.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${goal.completedTasks}/${goal.linkedTasks} tasks completed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun GoalsListScreenPreview() {
    PrioTheme {
        GoalsListScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyGoalsStatePreview() {
    PrioTheme {
        EmptyGoalsState(onCreateGoal = {})
    }
}

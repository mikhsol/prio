package com.prio.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prio.core.ui.theme.PrioTheme
import com.prio.core.ui.theme.SemanticColors

/**
 * Goal category with icon.
 */
enum class GoalCategory(val label: String, val emoji: String) {
    CAREER("Career", "💼"),
    HEALTH("Health", "💪"),
    PERSONAL("Personal", "🎯"),
    FINANCIAL("Financial", "💰"),
    LEARNING("Learning", "📚"),
    RELATIONSHIPS("Relationships", "❤️")
}

/**
 * Goal status with color coding per 1.1.4 spec.
 * 
 * - On Track: Green #10B981 (≥0% behind)
 * - Slightly Behind: Yellow #F59E0B (<15% behind)  
 * - At Risk: Red #EF4444 (≥15% behind)
 */
enum class GoalStatus(
    val label: String,
    val color: Color,
    val emoji: String
) {
    ON_TRACK("On Track", SemanticColors.onTrack, "🟢"),
    SLIGHTLY_BEHIND("Slightly Behind", SemanticColors.behind, "🟡"),
    AT_RISK("At Risk", SemanticColors.atRisk, "🔴")
}

/**
 * Progress display variant.
 */
enum class ProgressVariant {
    /** Linear progress bar (default for cards) */
    LINEAR,
    /** Circular progress (for detail hero) */
    CIRCULAR
}

/**
 * Goal data for display in GoalCard.
 */
data class GoalCardData(
    val id: String,
    val title: String,
    val category: GoalCategory,
    val progress: Float, // 0.0 to 1.0
    val status: GoalStatus,
    val targetDate: String? = null,
    val milestonesCompleted: Int = 0,
    val milestonesTotal: Int = 0,
    val linkedTasksCount: Int = 0
)

/**
 * GoalCard component per 1.1.13 Component Specifications.
 * 
 * Displays a goal with:
 * - Category icon
 * - Title
 * - Progress bar (linear or circular)
 * - Status indicator (on track / behind / at risk)
 * - Metadata: target date, milestones, linked tasks
 * 
 * Accessibility:
 * - Touch target ≥48dp per 1.1.11
 * - Screen reader announces progress percentage and status
 * 
 * @param goal Goal data to display
 * @param onTap Called when card is tapped (opens detail)
 * @param onOverflowTap Optional - called when overflow menu is tapped
 * @param showMetadata Whether to show target date and milestone info
 * @param progressVariant LINEAR or CIRCULAR progress display
 * @param modifier Modifier for customization
 */
@Composable
fun GoalCard(
    goal: GoalCardData,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    onOverflowTap: (() -> Unit)? = null,
    showMetadata: Boolean = true,
    progressVariant: ProgressVariant = ProgressVariant.LINEAR
) {
    val progressPercent = (goal.progress * 100).toInt()
    
    val cardContentDescription = buildString {
        append(goal.title)
        append(". Category: ${goal.category.label}")
        append(". $progressPercent percent complete")
        append(". Status: ${goal.status.label}")
        goal.targetDate?.let { append(". Target: $it") }
        if (goal.milestonesTotal > 0) {
            append(". ${goal.milestonesCompleted} of ${goal.milestonesTotal} milestones")
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = cardContentDescription }
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = goal.category.emoji,
                        fontSize = 16.sp
                    )
                    Text(
                        text = goal.category.label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Overflow menu
                if (onOverflowTap != null) {
                    IconButton(
                        onClick = onOverflowTap,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Title
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress
            when (progressVariant) {
                ProgressVariant.LINEAR -> LinearProgress(
                    progress = goal.progress,
                    progressPercent = progressPercent,
                    status = goal.status
                )
                ProgressVariant.CIRCULAR -> CircularProgress(
                    progress = goal.progress,
                    progressPercent = progressPercent,
                    status = goal.status
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.status.emoji,
                    fontSize = 12.sp
                )
                Text(
                    text = goal.status.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = goal.status.color
                )
            }
            
            // Metadata
            if (showMetadata && (goal.targetDate != null || goal.milestonesTotal > 0 || goal.linkedTasksCount > 0)) {
                Spacer(modifier = Modifier.height(8.dp))
                MetadataRow(
                    targetDate = goal.targetDate,
                    milestonesCompleted = goal.milestonesCompleted,
                    milestonesTotal = goal.milestonesTotal,
                    linkedTasksCount = goal.linkedTasksCount
                )
            }
        }
    }
}

@Composable
private fun LinearProgress(
    progress: Float,
    progressPercent: Int,
    status: GoalStatus
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "progress_animation"
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = status.color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = "$progressPercent%",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CircularProgress(
    progress: Float,
    progressPercent: Int,
    status: GoalStatus
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "progress_animation"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(80.dp)
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            
            // Background circle
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                radius = radius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Progress arc
            drawArc(
                color = status.color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        Text(
            text = "$progressPercent%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MetadataRow(
    targetDate: String?,
    milestonesCompleted: Int,
    milestonesTotal: Int,
    linkedTasksCount: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
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
        
        if (linkedTasksCount > 0) {
            Text(
                text = "📋 $linkedTasksCount tasks",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============ Previews ============

@Preview(showBackground = true)
@Composable
private fun GoalCardPreview() {
    PrioTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GoalCard(
                goal = GoalCardData(
                    id = "1",
                    title = "Get promoted to Senior PM",
                    category = GoalCategory.CAREER,
                    progress = 0.65f,
                    status = GoalStatus.ON_TRACK,
                    targetDate = "Jun 2026",
                    milestonesCompleted = 5,
                    milestonesTotal = 12,
                    linkedTasksCount = 3
                ),
                onTap = {},
                onOverflowTap = {}
            )
            
            GoalCard(
                goal = GoalCardData(
                    id = "2",
                    title = "Run a half marathon",
                    category = GoalCategory.HEALTH,
                    progress = 0.35f,
                    status = GoalStatus.SLIGHTLY_BEHIND,
                    targetDate = "May 2026"
                ),
                onTap = {}
            )
            
            GoalCard(
                goal = GoalCardData(
                    id = "3",
                    title = "Save $10,000 emergency fund",
                    category = GoalCategory.FINANCIAL,
                    progress = 0.15f,
                    status = GoalStatus.AT_RISK,
                    targetDate = "Dec 2026"
                ),
                onTap = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GoalCardCircularPreview() {
    PrioTheme {
        GoalCard(
            goal = GoalCardData(
                id = "1",
                title = "Complete online certification",
                category = GoalCategory.LEARNING,
                progress = 0.72f,
                status = GoalStatus.ON_TRACK
            ),
            onTap = {},
            progressVariant = ProgressVariant.CIRCULAR,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F2937)
@Composable
private fun GoalCardDarkPreview() {
    PrioTheme(darkTheme = true) {
        GoalCard(
            goal = GoalCardData(
                id = "1",
                title = "Get promoted to Senior PM",
                category = GoalCategory.CAREER,
                progress = 0.65f,
                status = GoalStatus.ON_TRACK,
                targetDate = "Jun 2026"
            ),
            onTap = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

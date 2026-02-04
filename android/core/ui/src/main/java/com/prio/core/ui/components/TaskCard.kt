package com.prio.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prio.core.ui.theme.PrioTheme
import com.prio.core.ui.theme.SemanticColors

/**
 * Task data for display in TaskCard.
 * 
 * This is a UI model, not domain model.
 */
data class TaskCardData(
    val id: String,
    val title: String,
    val quadrant: Quadrant,
    val isCompleted: Boolean = false,
    val isOverdue: Boolean = false,
    val dueText: String? = null,
    val goalName: String? = null,
    val aiExplanation: String? = null
)

/**
 * TaskCard component per 1.1.13 Component Specifications.
 * 
 * Displays a task with:
 * - Quadrant badge (color-coded priority)
 * - Checkbox for completion
 * - Title (max 2 lines)
 * - Due date and goal linking
 * - Overdue indicator (red left border)
 * 
 * Accessibility:
 * - Min height 72dp per spec
 * - Touch target ≥48dp per 1.1.11
 * - Screen reader support
 * 
 * @param task Task data to display
 * @param onTap Called when card is tapped (opens detail)
 * @param onCheckboxTap Called when checkbox is tapped (toggles completion)
 * @param onQuadrantTap Optional - called when quadrant badge is tapped (changes priority)
 * @param isSelected Whether card is in selected state
 * @param showMetadata Whether to show due date and goal info
 * @param modifier Modifier for customization
 */
@Composable
fun TaskCard(
    task: TaskCardData,
    onTap: () -> Unit,
    onCheckboxTap: () -> Unit,
    modifier: Modifier = Modifier,
    onQuadrantTap: (() -> Unit)? = null,
    isSelected: Boolean = false,
    showMetadata: Boolean = true
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "card_bg_color"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.6f else 1f,
        label = "content_alpha"
    )
    
    val overdueColor = SemanticColors.error
    
    val cardContentDescription = buildString {
        append(task.title)
        append(". Priority: ${task.quadrant.label}")
        if (task.isCompleted) append(". Completed")
        if (task.isOverdue) append(". Overdue")
        task.dueText?.let { append(". Due: $it") }
        task.goalName?.let { append(". Goal: $it") }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .semantics { contentDescription = cardContentDescription }
            .clickable(onClick = onTap)
            .then(
                if (task.isOverdue && !task.isCompleted) {
                    Modifier.drawBehind {
                        drawLine(
                            color = overdueColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quadrant Badge
            QuadrantBadge(
                quadrant = task.quadrant,
                size = BadgeSize.COMPACT,
                onTap = onQuadrantTap
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Checkbox (min 48dp touch target)
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onCheckboxTap() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = if (task.isCompleted) {
                            "Completed. Double tap to mark incomplete"
                        } else {
                            "Mark ${task.title} as complete"
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Metadata row
                if (showMetadata && (task.isOverdue || task.dueText != null || task.goalName != null)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    MetadataRow(
                        isOverdue = task.isOverdue && !task.isCompleted,
                        dueText = task.dueText,
                        goalName = task.goalName
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(
    isOverdue: Boolean,
    dueText: String?,
    goalName: String?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isOverdue) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = SemanticColors.error
                )
                Text(
                    text = "Overdue",
                    style = MaterialTheme.typography.labelSmall,
                    color = SemanticColors.error
                )
            }
        } else if (dueText != null) {
            Text(
                text = "Due: $dueText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (goalName != null) {
            if (isOverdue || dueText != null) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "🎯 $goalName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============ Previews ============

@Preview(showBackground = true)
@Composable
private fun TaskCardDefaultPreview() {
    PrioTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskCard(
                task = TaskCardData(
                    id = "1",
                    title = "Submit quarterly report",
                    quadrant = Quadrant.DO_FIRST,
                    dueText = "Today",
                    goalName = "Career Growth"
                ),
                onTap = {},
                onCheckboxTap = {}
            )
            
            TaskCard(
                task = TaskCardData(
                    id = "2",
                    title = "Research vacation destinations for summer trip",
                    quadrant = Quadrant.SCHEDULE,
                    dueText = "This week"
                ),
                onTap = {},
                onCheckboxTap = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskCardStatesPreview() {
    PrioTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Overdue
            TaskCard(
                task = TaskCardData(
                    id = "1",
                    title = "Call dentist about appointment",
                    quadrant = Quadrant.DO_FIRST,
                    isOverdue = true,
                    dueText = "Yesterday"
                ),
                onTap = {},
                onCheckboxTap = {}
            )
            
            // Completed
            TaskCard(
                task = TaskCardData(
                    id = "2",
                    title = "Send email to team",
                    quadrant = Quadrant.DELEGATE,
                    isCompleted = true
                ),
                onTap = {},
                onCheckboxTap = {}
            )
            
            // Selected
            TaskCard(
                task = TaskCardData(
                    id = "3",
                    title = "Review PRs",
                    quadrant = Quadrant.SCHEDULE
                ),
                onTap = {},
                onCheckboxTap = {},
                isSelected = true
            )
            
            // Eliminate quadrant
            TaskCard(
                task = TaskCardData(
                    id = "4",
                    title = "Reorganize bookshelf",
                    quadrant = Quadrant.ELIMINATE
                ),
                onTap = {},
                onCheckboxTap = {},
                showMetadata = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F2937)
@Composable
private fun TaskCardDarkPreview() {
    PrioTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskCard(
                task = TaskCardData(
                    id = "1",
                    title = "Submit quarterly report",
                    quadrant = Quadrant.DO_FIRST,
                    dueText = "Today"
                ),
                onTap = {},
                onCheckboxTap = {}
            )
            
            TaskCard(
                task = TaskCardData(
                    id = "2",
                    title = "Overdue task example",
                    quadrant = Quadrant.DO_FIRST,
                    isOverdue = true
                ),
                onTap = {},
                onCheckboxTap = {}
            )
        }
    }
}

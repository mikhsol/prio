package com.prio.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prio.core.ui.theme.PrioTheme
import com.prio.core.ui.theme.SemanticColors

/**
 * Empty state presets per 1.1.10 spec.
 */
enum class EmptyStateType(
    val icon: String,
    val headline: String,
    val message: String,
    val ctaLabel: String?
) {
    TASKS(
        icon = "📋",
        headline = "No tasks yet!",
        message = "Tap + to add your first task and let Prio help you prioritize.",
        ctaLabel = "Add First Task"
    ),
    TASKS_FILTERED(
        icon = "🔍",
        headline = "No tasks match your filters",
        message = "Try adjusting your filters or search.",
        ctaLabel = "Clear Filters"
    ),
    TASKS_ALL_DONE(
        icon = "🎉",
        headline = "All caught up!",
        message = "You've completed all your tasks. Amazing! Take a break or add new ones.",
        ctaLabel = "View Completed"
    ),
    GOALS(
        icon = "🎯",
        headline = "No goals yet!",
        message = "Set your first goal and Prio will help you break it down into achievable steps.",
        ctaLabel = "Create First Goal"
    ),
    GOALS_COMPLETED(
        icon = "🏆",
        headline = "No completed goals yet",
        message = "Complete a goal to see it here!",
        ctaLabel = "View Active Goals"
    ),
    CALENDAR_NOT_CONNECTED(
        icon = "📅",
        headline = "Calendar not connected",
        message = "Connect your calendar to see your schedule alongside tasks.",
        ctaLabel = "Connect Calendar"
    ),
    CALENDAR_NO_EVENTS(
        icon = "📅",
        headline = "No events today",
        message = "Your day is clear! Perfect for deep work.",
        ctaLabel = null
    ),
    SEARCH_NO_RESULTS(
        icon = "🔍",
        headline = "No results found",
        message = "Try different keywords.",
        ctaLabel = "Clear Search"
    ),
    GOAL_NO_TASKS(
        icon = "📋",
        headline = "No tasks linked yet",
        message = "Add tasks to track progress toward this goal.",
        ctaLabel = "Add Task"
    ),
    GOAL_NO_MILESTONES(
        icon = "🏁",
        headline = "No milestones yet",
        message = "Break your goal into milestones to track progress.",
        ctaLabel = "Add Milestone"
    )
}

/**
 * EmptyState component per 1.1.10 Error/Empty States spec.
 * 
 * Displays when a list or section has no content.
 * 
 * @param type Preset empty state type
 * @param onCtaTap Called when CTA is tapped
 * @param modifier Modifier for customization
 */
@Composable
fun EmptyState(
    type: EmptyStateType,
    onCtaTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = type.icon,
        headline = type.headline,
        message = type.message,
        ctaLabel = type.ctaLabel,
        onCtaTap = onCtaTap,
        modifier = modifier
    )
}

/**
 * EmptyState component with custom content.
 * 
 * @param icon Emoji icon (64dp size)
 * @param headline Main headline
 * @param message Descriptive message (max 2 lines)
 * @param ctaLabel Optional call-to-action button label
 * @param onCtaTap Called when CTA is tapped
 * @param modifier Modifier for customization
 */
@Composable
fun EmptyState(
    icon: String,
    headline: String,
    message: String,
    ctaLabel: String? = null,
    onCtaTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val contentDesc = "$headline. $message"
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp)
            .semantics { contentDescription = contentDesc },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Text(
            text = icon,
            fontSize = 64.sp,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Headline
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Message
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
        
        // CTA
        if (ctaLabel != null && onCtaTap != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onCtaTap) {
                Text(text = ctaLabel)
            }
        }
    }
}

/**
 * Error state presets per 1.1.10 spec.
 */
enum class ErrorStateType(
    val icon: String,
    val headline: String,
    val message: String,
    val primaryAction: String,
    val secondaryAction: String?
) {
    DATABASE_READ(
        icon = "⚠️",
        headline = "Couldn't load your tasks",
        message = "Something went wrong. Your data is safe.",
        primaryAction = "Try Again",
        secondaryAction = "Restart App"
    ),
    DATABASE_WRITE(
        icon = "⚠️",
        headline = "Couldn't save your changes",
        message = "Your changes weren't saved. Try again.",
        primaryAction = "Try Again",
        secondaryAction = "Cancel"
    ),
    AI_MODEL_LOAD(
        icon = "🤖",
        headline = "AI couldn't start",
        message = "Using basic mode instead. AI features limited.",
        primaryAction = "Retry",
        secondaryAction = "Continue without AI"
    ),
    AI_CLASSIFICATION(
        icon = "🤖",
        headline = "Couldn't classify task",
        message = "We'll try again in background.",
        primaryAction = "OK",
        secondaryAction = null
    ),
    CALENDAR_SYNC(
        icon = "📅",
        headline = "Couldn't read calendar",
        message = "Check calendar permissions in Settings.",
        primaryAction = "Open Settings",
        secondaryAction = "Skip Calendar"
    ),
    PERMISSION_DENIED(
        icon = "🔐",
        headline = "Permission needed",
        message = "This feature requires additional permissions.",
        primaryAction = "Grant Permission",
        secondaryAction = "Not Now"
    ),
    GENERIC(
        icon = "⚠️",
        headline = "Something went wrong",
        message = "Please try again. If the problem persists, restart the app.",
        primaryAction = "Try Again",
        secondaryAction = null
    )
}

/**
 * ErrorState component per 1.1.10 spec.
 * 
 * Displays when an error occurs.
 * 
 * @param type Preset error type
 * @param onPrimaryAction Called when primary action is tapped
 * @param onSecondaryAction Called when secondary action is tapped
 * @param modifier Modifier for customization
 */
@Composable
fun ErrorState(
    type: ErrorStateType,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ErrorState(
        icon = type.icon,
        headline = type.headline,
        message = type.message,
        primaryActionLabel = type.primaryAction,
        secondaryActionLabel = type.secondaryAction,
        onPrimaryAction = onPrimaryAction,
        onSecondaryAction = onSecondaryAction,
        modifier = modifier
    )
}

/**
 * ErrorState component with custom content.
 */
@Composable
fun ErrorState(
    icon: String,
    headline: String,
    message: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val contentDesc = "$headline. $message"
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp)
            .semantics { contentDescription = contentDesc },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Text(
            text = icon,
            fontSize = 64.sp,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Headline
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Message
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Primary action
        Button(
            onClick = onPrimaryAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = primaryActionLabel)
        }
        
        // Secondary action
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(onClick = onSecondaryAction) {
                Text(
                    text = secondaryActionLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============ Previews ============

@Preview(showBackground = true)
@Composable
private fun EmptyStateTasksPreview() {
    PrioTheme {
        EmptyState(
            type = EmptyStateType.TASKS,
            onCtaTap = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStateAllDonePreview() {
    PrioTheme {
        EmptyState(
            type = EmptyStateType.TASKS_ALL_DONE,
            onCtaTap = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStateGoalsPreview() {
    PrioTheme {
        EmptyState(
            type = EmptyStateType.GOALS,
            onCtaTap = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStateDatabasePreview() {
    PrioTheme {
        ErrorState(
            type = ErrorStateType.DATABASE_READ,
            onPrimaryAction = {},
            onSecondaryAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStateAiPreview() {
    PrioTheme {
        ErrorState(
            type = ErrorStateType.AI_MODEL_LOAD,
            onPrimaryAction = {},
            onSecondaryAction = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F2937)
@Composable
private fun EmptyStateDarkPreview() {
    PrioTheme(darkTheme = true) {
        EmptyState(
            type = EmptyStateType.TASKS,
            onCtaTap = {}
        )
    }
}

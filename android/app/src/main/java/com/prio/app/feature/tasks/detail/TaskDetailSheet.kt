package com.prio.app.feature.tasks.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.ui.components.PrioConfirmDialog
import com.prio.core.ui.components.QuadrantBadge
import com.prio.core.ui.components.Quadrant
import com.prio.core.ui.theme.PrioTheme
import com.prio.core.ui.theme.QuadrantColors
import com.prio.core.ui.theme.SemanticColors

/**
 * Task Detail UI state for the bottom sheet.
 * 
 * Per 1.1.2 Task Detail Sheet Specification.
 */
data class TaskDetailUiState(
    val id: Long = 0,
    val title: String = "",
    val notes: String? = null,
    val quadrant: EisenhowerQuadrant = EisenhowerQuadrant.ELIMINATE,
    val aiExplanation: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: String? = null,
    val dueDate: String? = null,
    val dueDateFormatted: String = "No due date",
    val linkedGoal: LinkedGoalInfo? = null,
    val recurrenceText: String = "Does not repeat",
    val reminderText: String = "No reminder",
    val subtasks: List<SubtaskUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isAiClassifying: Boolean = false
)

/**
 * Linked goal info for display.
 */
data class LinkedGoalInfo(
    val id: Long,
    val title: String,
    val progress: Int,
    val category: String
)

/**
 * Subtask UI model.
 */
data class SubtaskUiModel(
    val id: Long,
    val title: String,
    val isCompleted: Boolean
)

/**
 * Events from TaskDetailSheet.
 */
sealed interface TaskDetailEvent {
    data class UpdateTitle(val title: String) : TaskDetailEvent
    data class UpdateNotes(val notes: String) : TaskDetailEvent
    data class UpdateQuadrant(val quadrant: EisenhowerQuadrant) : TaskDetailEvent
    object ToggleComplete : TaskDetailEvent
    object Delete : TaskDetailEvent
    object ConfirmDelete : TaskDetailEvent
    object DismissDelete : TaskDetailEvent
    object OpenDueDatePicker : TaskDetailEvent
    object OpenGoalPicker : TaskDetailEvent
    object OpenRecurrencePicker : TaskDetailEvent
    object OpenReminderPicker : TaskDetailEvent
    data class ToggleSubtask(val subtaskId: Long) : TaskDetailEvent
    data class DeleteSubtask(val subtaskId: Long) : TaskDetailEvent
    data class AddSubtask(val title: String) : TaskDetailEvent
    object Dismiss : TaskDetailEvent
    object Save : TaskDetailEvent
    /** Toggle inline editing mode via overflow menu. */
    object ToggleEditing : TaskDetailEvent
    /** Duplicate current task via overflow menu. */
    object DuplicateTask : TaskDetailEvent
    /** Copy task title + notes to clipboard via overflow menu. */
    object CopyToClipboard : TaskDetailEvent
}

/**
 * Task Detail Bottom Sheet.
 * 
 * Implements 3.1.4 from ACTION_PLAN.md per 1.1.2 Task Detail Sheet Specification:
 * - Half/full expand states
 * - AI explanation display with robot emoji
 * - Goal linking picker
 * - Quadrant override pills
 * - Delete with 5s undo
 * 
 * @param state Current UI state
 * @param onEvent Event handler for user interactions
 * @param onDismiss Called when sheet is dismissed
 * @param sheetState Bottom sheet state for expand/collapse control
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    state: TaskDetailUiState,
    onEvent: (TaskDetailEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showQuadrantSelector by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ) {}
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Title section with checkbox
            item {
                TitleSection(
                    title = state.title,
                    isCompleted = state.isCompleted,
                    isEditing = state.isEditing,
                    onTitleChange = { onEvent(TaskDetailEvent.UpdateTitle(it)) },
                    onCheckboxClick = { onEvent(TaskDetailEvent.ToggleComplete) },
                    onEditClick = { onEvent(TaskDetailEvent.ToggleEditing) },
                    onDuplicateClick = { onEvent(TaskDetailEvent.DuplicateTask) },
                    onCopyClick = { onEvent(TaskDetailEvent.CopyToClipboard) },
                    onDeleteClick = { showDeleteDialog = true }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Quadrant section with AI explanation
            item {
                QuadrantSection(
                    quadrant = state.quadrant,
                    aiExplanation = state.aiExplanation,
                    isAiClassifying = state.isAiClassifying,
                    onChangeClick = { showQuadrantSelector = true }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Property rows
            item {
                PropertyRow(
                    icon = Icons.Default.CalendarToday,
                    label = state.dueDateFormatted,
                    onClick = { onEvent(TaskDetailEvent.OpenDueDatePicker) }
                )
            }
            
            item {
                PropertyRow(
                    icon = Icons.Default.Flag,
                    label = state.linkedGoal?.let { 
                        "${it.title} (${it.progress}%)" 
                    } ?: "No goal linked",
                    onClick = { onEvent(TaskDetailEvent.OpenGoalPicker) }
                )
            }
            
            item {
                PropertyRow(
                    icon = Icons.Default.Repeat,
                    label = state.recurrenceText,
                    onClick = { onEvent(TaskDetailEvent.OpenRecurrencePicker) }
                )
            }
            
            item {
                PropertyRow(
                    icon = Icons.Default.Notifications,
                    label = state.reminderText,
                    onClick = { onEvent(TaskDetailEvent.OpenReminderPicker) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Notes section (only in expanded/edit mode)
            if (state.isEditing || state.notes != null) {
                item {
                    NotesSection(
                        notes = state.notes ?: "",
                        isEditing = state.isEditing,
                        onNotesChange = { onEvent(TaskDetailEvent.UpdateNotes(it)) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Subtasks section (only in expanded/edit mode)
            if (state.isEditing || state.subtasks.isNotEmpty()) {
                item {
                    SubtasksHeader(
                        completedCount = state.subtasks.count { it.isCompleted },
                        totalCount = state.subtasks.size,
                        onAddClick = { /* Show add subtask dialog */ }
                    )
                }
                
                items(state.subtasks, key = { it.id }) { subtask ->
                    SubtaskRow(
                        subtask = subtask,
                        onToggle = { onEvent(TaskDetailEvent.ToggleSubtask(subtask.id)) },
                        onDelete = { onEvent(TaskDetailEvent.DeleteSubtask(subtask.id)) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Completed state info
            if (state.isCompleted && state.completedAt != null) {
                item {
                    CompletedInfo(completedAt = state.completedAt)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Action buttons
            item {
                ActionButtons(
                    isCompleted = state.isCompleted,
                    onCompleteClick = { onEvent(TaskDetailEvent.ToggleComplete) },
                    onDeleteClick = { showDeleteDialog = true }
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        PrioConfirmDialog(
            title = "Delete Task?",
            message = "\"${state.title}\" will be moved to trash. You can restore it within 30 days.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = { 
                showDeleteDialog = false
                onEvent(TaskDetailEvent.ConfirmDelete) 
            },
            onDismiss = { showDeleteDialog = false },
            isDestructive = true
        )
    }
    
    // Quadrant selector dialog
    if (showQuadrantSelector) {
        QuadrantSelectorSheet(
            currentQuadrant = state.quadrant,
            aiExplanation = state.aiExplanation,
            onSelect = { quadrant ->
                showQuadrantSelector = false
                onEvent(TaskDetailEvent.UpdateQuadrant(quadrant))
            },
            onDismiss = { showQuadrantSelector = false }
        )
    }
}

@Composable
private fun TitleSection(
    title: String,
    isCompleted: Boolean,
    isEditing: Boolean,
    onTitleChange: (String) -> Unit,
    onCheckboxClick: () -> Unit,
    onEditClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 0.6f else 1f,
        label = "title_alpha"
    )
    
    var showOverflowMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onCheckboxClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SemanticColors.success,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.semantics {
                    contentDescription = if (isCompleted) {
                        "Completed. Double tap to mark incomplete"
                    } else {
                        "Mark as complete"
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Title
        if (isEditing) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier
                    .weight(1f)
                    .alpha(contentAlpha),
                textStyle = MaterialTheme.typography.headlineSmall,
                singleLine = true,
                placeholder = { Text("Task title") }
            )
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier
                    .weight(1f)
                    .alpha(contentAlpha)
            )
        }
        
        // Overflow menu
        Box {
            IconButton(
                onClick = { showOverflowMenu = true },
                modifier = Modifier.semantics { contentDescription = "More options" }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null
                )
            }
            
            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (isEditing) "Done Editing" else "Edit") },
                    onClick = {
                        showOverflowMenu = false
                        onEditClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Duplicate") },
                    onClick = {
                        showOverflowMenu = false
                        onDuplicateClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FileCopy,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy to Clipboard") },
                    onClick = {
                        showOverflowMenu = false
                        onCopyClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                Divider()
                DropdownMenuItem(
                    text = {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        showOverflowMenu = false
                        onDeleteClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun QuadrantSection(
    quadrant: EisenhowerQuadrant,
    aiExplanation: String?,
    isAiClassifying: Boolean,
    onChangeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = quadrant.emoji,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = quadrant.displayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = quadrant.color
                    )
                }
                
                TextButton(onClick = onChangeClick) {
                    Text("Change")
                }
            }
            
            // AI Explanation
            if (isAiClassifying) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Analyzing task...",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (aiExplanation != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "🤖",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "\"$aiExplanation\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        TextButton(onClick = onClick) {
            Text("Change")
        }
    }
}

@Composable
private fun NotesSection(
    notes: String,
    isEditing: Boolean,
    onNotesChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Notes",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("Add notes...") },
            enabled = isEditing,
            maxLines = 6
        )
    }
}

@Composable
private fun SubtasksHeader(
    completedCount: Int,
    totalCount: Int,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Subtasks ($completedCount/$totalCount)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        IconButton(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add subtask"
            )
        }
    }
}

@Composable
private fun SubtaskRow(
    subtask: SubtaskUiModel,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (subtask.isCompleted) 0.6f else 1f,
        label = "subtask_alpha"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = subtask.isCompleted,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .alpha(contentAlpha),
            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None
        )
        
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete subtask",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CompletedInfo(completedAt: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SemanticColors.success.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = SemanticColors.success
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = "Completed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SemanticColors.success
                )
                Text(
                    text = completedAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    isCompleted: Boolean,
    onCompleteClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Complete/Mark Incomplete button
        Button(
            onClick = onCompleteClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCompleted) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.AccessTime else Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isCompleted) "Mark Incomplete" else "Complete")
        }
        
        // Delete button
        OutlinedButton(
            onClick = onDeleteClick,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete")
        }
    }
}

/**
 * Quadrant selector bottom sheet / dialog.
 * Per 1.1.2 spec: Shows all 4 quadrants with current selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuadrantSelectorSheet(
    currentQuadrant: EisenhowerQuadrant,
    aiExplanation: String?,
    onSelect: (EisenhowerQuadrant) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Change Priority",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // AI explanation if available
            if (aiExplanation != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "🤖", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "AI classified this as:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "\"$aiExplanation\"",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Quadrant options in 2x2 grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuadrantOption(
                        quadrant = EisenhowerQuadrant.DO_FIRST,
                        isSelected = currentQuadrant == EisenhowerQuadrant.DO_FIRST,
                        onClick = { onSelect(EisenhowerQuadrant.DO_FIRST) },
                        modifier = Modifier.weight(1f)
                    )
                    QuadrantOption(
                        quadrant = EisenhowerQuadrant.SCHEDULE,
                        isSelected = currentQuadrant == EisenhowerQuadrant.SCHEDULE,
                        onClick = { onSelect(EisenhowerQuadrant.SCHEDULE) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuadrantOption(
                        quadrant = EisenhowerQuadrant.DELEGATE,
                        isSelected = currentQuadrant == EisenhowerQuadrant.DELEGATE,
                        onClick = { onSelect(EisenhowerQuadrant.DELEGATE) },
                        modifier = Modifier.weight(1f)
                    )
                    QuadrantOption(
                        quadrant = EisenhowerQuadrant.ELIMINATE,
                        isSelected = currentQuadrant == EisenhowerQuadrant.ELIMINATE,
                        onClick = { onSelect(EisenhowerQuadrant.ELIMINATE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Learning hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "💡", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your change helps Prio learn your preferences",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuadrantOption(
    quadrant: EisenhowerQuadrant,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        quadrant.color.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    
    val borderColor = if (isSelected) quadrant.color else Color.Transparent
    
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, borderColor)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = quadrant.emoji,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = quadrant.displayLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) quadrant.color else MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "(current)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Extension properties for EisenhowerQuadrant display
private val EisenhowerQuadrant.displayLabel: String
    get() = when (this) {
        EisenhowerQuadrant.DO_FIRST -> "DO FIRST"
        EisenhowerQuadrant.SCHEDULE -> "SCHEDULE"
        EisenhowerQuadrant.DELEGATE -> "DELEGATE"
        EisenhowerQuadrant.ELIMINATE -> "LATER"
    }

private val EisenhowerQuadrant.color: Color
    get() = when (this) {
        EisenhowerQuadrant.DO_FIRST -> QuadrantColors.doFirst
        EisenhowerQuadrant.SCHEDULE -> QuadrantColors.schedule
        EisenhowerQuadrant.DELEGATE -> QuadrantColors.delegate
        EisenhowerQuadrant.ELIMINATE -> QuadrantColors.eliminate
    }

private val EisenhowerQuadrant.emoji: String
    get() = when (this) {
        EisenhowerQuadrant.DO_FIRST -> "🔴"
        EisenhowerQuadrant.SCHEDULE -> "🟡"
        EisenhowerQuadrant.DELEGATE -> "🟠"
        EisenhowerQuadrant.ELIMINATE -> "⚪"
    }

@Preview(showBackground = true)
@Composable
private fun TaskDetailSheetPreview() {
    PrioTheme {
        Surface {
            // Preview content would go here
            // Note: ModalBottomSheet requires special handling in previews
        }
    }
}

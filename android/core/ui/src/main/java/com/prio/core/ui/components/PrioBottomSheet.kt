package com.prio.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.prio.core.ui.theme.PrioTheme
import com.prio.core.ui.theme.SemanticColors

/**
 * PrioBottomSheet per 1.1.13 Component Specifications.
 * 
 * Features:
 * - Drag handle (32dp × 4dp, centered)
 * - Corner radius: 28dp (top corners only)
 * - Initial state: Half-expanded (50% screen)
 * - Full state: 90% screen height
 * - Scrim: Black 32% opacity
 * 
 * @param onDismiss Called when sheet is dismissed
 * @param sheetState Sheet state for controlling expand/collapse
 * @param content Sheet content
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrioBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    content: @Composable ColumnScope.() -> Unit
) {
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
        },
        content = content
    )
}

/**
 * PrioDialog for confirmation dialogs.
 * 
 * Standard confirmation dialog with:
 * - Title
 * - Message
 * - Primary and secondary actions
 * 
 * @param title Dialog title
 * @param message Dialog message
 * @param confirmLabel Primary button label
 * @param dismissLabel Secondary button label
 * @param onConfirm Called when confirm is tapped
 * @param onDismiss Called when dismiss is tapped or dialog closed
 * @param isDestructive Whether confirm action is destructive (changes button color)
 */
@Composable
fun PrioConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            if (isDestructive) {
                Button(
                    onClick = onConfirm,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = SemanticColors.error
                    )
                ) {
                    Text(confirmLabel)
                }
            } else {
                Button(onClick = onConfirm) {
                    Text(confirmLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * PrioInfoDialog for information-only dialogs.
 */
@Composable
fun PrioInfoDialog(
    title: String,
    message: String,
    buttonLabel: String = "OK",
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(buttonLabel)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * PrioFullScreenDialog for complex forms.
 */
@Composable
fun PrioFullScreenDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = "Save",
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    
                    Button(
                        onClick = onConfirm,
                        enabled = confirmEnabled
                    ) {
                        Text(confirmLabel)
                    }
                }
                
                // Content
                content()
            }
        }
    }
}

// ============ Previews ============

@Preview(showBackground = true)
@Composable
private fun PrioConfirmDialogPreview() {
    var showDialog by remember { mutableStateOf(true) }
    
    PrioTheme {
        if (showDialog) {
            PrioConfirmDialog(
                title = "Delete Task?",
                message = "This action cannot be undone. The task will be permanently deleted.",
                confirmLabel = "Delete",
                dismissLabel = "Cancel",
                onConfirm = { showDialog = false },
                onDismiss = { showDialog = false },
                isDestructive = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrioConfirmDialogNonDestructivePreview() {
    var showDialog by remember { mutableStateOf(true) }
    
    PrioTheme {
        if (showDialog) {
            PrioConfirmDialog(
                title = "Complete Goal?",
                message = "Mark \"Get promoted\" as complete? This will move it to your completed goals.",
                confirmLabel = "Complete",
                dismissLabel = "Not Yet",
                onConfirm = { showDialog = false },
                onDismiss = { showDialog = false },
                isDestructive = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrioInfoDialogPreview() {
    var showDialog by remember { mutableStateOf(true) }
    
    PrioTheme {
        if (showDialog) {
            PrioInfoDialog(
                title = "AI Classification",
                message = "This task was classified as DO FIRST because it has a deadline today and mentions \"urgent\" in the title.",
                onDismiss = { showDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun PrioBottomSheetPreview() {
    var showSheet by remember { mutableStateOf(true) }
    
    PrioTheme {
        if (showSheet) {
            PrioBottomSheet(onDismiss = { showSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Task Detail",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Submit quarterly report",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "AI classified this as DO FIRST because the deadline is today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete")
                        }
                        
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark Complete")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

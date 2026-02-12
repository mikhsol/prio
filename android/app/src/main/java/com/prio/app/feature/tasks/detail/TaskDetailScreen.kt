package com.prio.app.feature.tasks.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

/**
 * Full-screen wrapper for TaskDetailSheet.
 *
 * Wires navigation-supplied [taskId] into [TaskDetailViewModel] and
 * handles side-effects (snackbars, dismiss → back, delete → back).
 *
 * Implements task 3.1.5.B.1 from ACTION_PLAN.md:
 * - Replaces PlaceholderDetailScreen in NavHost
 * - Loads task by ID via ViewModel (SavedStateHandle)
 * - Forwards navigation events
 *
 * @param taskId ID of the task to display
 * @param onNavigateBack Called when user dismisses or deletes task
 * @param onNavigateToGoal Called when user taps linked goal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToGoal: (Long) -> Unit = {},
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }
    var showGoalPicker by remember { mutableStateOf(false) }

    // Handle side effects
    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is TaskDetailEffect.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = effect.message,
                        actionLabel = effect.actionLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // Undo action — reload task
                        viewModel.loadTask(taskId)
                    }
                }
                is TaskDetailEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is TaskDetailEffect.Dismiss -> {
                    onNavigateBack()
                }
                is TaskDetailEffect.TaskDeleted -> {
                    onNavigateBack()
                }
                is TaskDetailEffect.CopyToClipboard -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Prio Task", effect.text))
                }
                is TaskDetailEffect.OpenDatePicker -> {
                    showDatePicker = true
                }
                is TaskDetailEffect.OpenGoalPicker -> {
                    showGoalPicker = true
                }
                is TaskDetailEffect.OpenRecurrencePicker -> {
                    // Future: recurrence picker
                    snackbarHostState.showSnackbar(
                        message = "Recurrence settings coming soon",
                        duration = SnackbarDuration.Short
                    )
                }
                is TaskDetailEffect.OpenReminderPicker -> {
                    // Future: reminder picker
                    snackbarHostState.showSnackbar(
                        message = "Reminders coming soon",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Loading state
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Show the actual bottom sheet content as a full-screen detail
    // The TaskDetailSheet is a ModalBottomSheet, but for navigation we
    // present it as a full composable. It auto-shows as a bottom sheet
    // overlay on entry.
    Box(modifier = Modifier.fillMaxSize()) {
        TaskDetailSheet(
            state = state,
            onEvent = viewModel::onEvent,
            onDismiss = onNavigateBack
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Date picker dialog — step 1: pick date
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog — step 2: pick time
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = {
                // User dismissed time picker — save date without time
                viewModel.onEvent(TaskDetailEvent.UpdateDueDate(pendingDateMillis))
                showTimePicker = false
                pendingDateMillis = null
            },
            title = { Text("Set Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    // Combine date + time millis
                    val dateMillis = pendingDateMillis
                    val combinedMillis = if (dateMillis != null) {
                        val timeOffsetMillis =
                            (timePickerState.hour * 3600_000L) + (timePickerState.minute * 60_000L)
                        // DatePicker returns midnight UTC for the selected date;
                        // add the chosen hour/minute offset
                        dateMillis + timeOffsetMillis
                    } else {
                        null
                    }
                    viewModel.onEvent(TaskDetailEvent.UpdateDueDate(combinedMillis))
                    showTimePicker = false
                    pendingDateMillis = null
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Skip time — save date only
                    viewModel.onEvent(TaskDetailEvent.UpdateDueDate(pendingDateMillis))
                    showTimePicker = false
                    pendingDateMillis = null
                }) {
                    Text("Skip")
                }
            }
        )
    }

    // Goal picker bottom sheet
    if (showGoalPicker) {
        val goals by viewModel.availableGoals.collectAsStateWithLifecycle()
        GoalPickerBottomSheet(
            goals = goals,
            currentGoalId = state.linkedGoal?.id,
            onSelectGoal = { goalId ->
                viewModel.onEvent(TaskDetailEvent.UpdateGoalLink(goalId))
                showGoalPicker = false
            },
            onClearGoal = {
                viewModel.onEvent(TaskDetailEvent.UpdateGoalLink(null))
                showGoalPicker = false
            },
            onDismiss = { showGoalPicker = false }
        )
    }
}

/**
 * Material 3 bottom sheet goal picker, consistent with the QuickCaptureSheet
 * goal picker design.
 *
 * Shows category emoji, goal title, and category + progress for each goal.
 * Includes a "No Goal" option to clear the link, a subtitle explaining the
 * feature, and a friendly empty state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalPickerBottomSheet(
    goals: List<LinkedGoalInfo>,
    currentGoalId: Long?,
    onSelectGoal: (Long) -> Unit,
    onClearGoal: () -> Unit,
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
                text = "Link Goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Linking tasks to goals helps track progress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // "No Goal" option to clear the link
            if (currentGoalId != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClearGoal)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "❌",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Remove goal link",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            if (goals.isEmpty()) {
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
                            text = "No active goals yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(goals, key = { it.id }) { goal ->
                        GoalPickerRow(
                            goal = goal,
                            onClick = { onSelectGoal(goal.id) }
                        )
                        if (goal != goals.last()) {
                            Divider(modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Single row in the goal picker bottom sheet.
 *
 * Displays category emoji, goal title, and a subtitle with the category name
 * and progress percentage, matching the QuickCaptureSheet's GoalPickerRow design.
 */
@Composable
private fun GoalPickerRow(
    goal: LinkedGoalInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = goal.emoji,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = goal.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = goal.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${goal.progress}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

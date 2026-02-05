package com.prio.app.feature.tasks.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                    // TODO: Integrate date picker (3.1.5.B.4 handled inline)
                }
                is TaskDetailEffect.OpenGoalPicker -> {
                    // Goal picker opens inline via TaskDetailSheet
                }
                is TaskDetailEffect.OpenRecurrencePicker -> {
                    // Future: recurrence picker
                }
                is TaskDetailEffect.OpenReminderPicker -> {
                    // Future: reminder picker
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
}

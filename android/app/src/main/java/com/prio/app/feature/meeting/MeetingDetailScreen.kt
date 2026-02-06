package com.prio.app.feature.meeting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Meeting Detail Screen per tasks 3.3.3 - 3.3.6.
 *
 * Sections:
 * 1. Header — title, time, location, attendees
 * 2. Notes — editable text with auto-save (3.3.4)
 * 3. Action Items — AI extraction + manual toggle + convert to task (3.3.5)
 * 4. Agenda — editable checklist (3.3.6)
 *
 * WCAG 2.1 AA — all interactive elements have semantic descriptions.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MeetingDetailScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToTask: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MeetingDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                MeetingDetailEffect.NavigateBack -> onNavigateBack()
                is MeetingDetailEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is MeetingDetailEffect.NavigateToTask -> onNavigateToTask(effect.taskId)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.title.ifEmpty { "Meeting" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(MeetingDetailEvent.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                // ==================== Header Section ====================
                MeetingHeaderSection(uiState)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // ==================== Notes Section (3.3.4) ====================
                NotesSection(
                    notes = uiState.notes,
                    isEditing = uiState.isNotesEditing,
                    isSaved = uiState.notesSaved,
                    onToggleEdit = { viewModel.onEvent(MeetingDetailEvent.OnToggleNotesEdit) },
                    onNotesChanged = { viewModel.onEvent(MeetingDetailEvent.OnNotesChanged(it)) },
                    onSave = { viewModel.onEvent(MeetingDetailEvent.OnSaveNotes) },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // ==================== Action Items Section (3.3.5) ====================
                ActionItemsSection(
                    actionItems = uiState.actionItems,
                    isExtracting = uiState.isExtractingActionItems,
                    onExtract = { viewModel.onEvent(MeetingDetailEvent.OnExtractActionItems) },
                    onToggleComplete = { viewModel.onEvent(MeetingDetailEvent.OnToggleActionItemComplete(it)) },
                    onConvertToTask = { viewModel.onEvent(MeetingDetailEvent.OnConvertActionItemToTask(it)) },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // ==================== Agenda Section (3.3.6) ====================
                AgendaSection(
                    agenda = uiState.agenda,
                    isEditing = uiState.isAgendaEditing,
                    isSaved = uiState.agendaSaved,
                    onToggleEdit = { viewModel.onEvent(MeetingDetailEvent.OnToggleAgendaEdit) },
                    onAgendaChanged = { viewModel.onEvent(MeetingDetailEvent.OnAgendaChanged(it)) },
                    onSave = { viewModel.onEvent(MeetingDetailEvent.OnSaveAgenda) },
                )

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// ==================== Header ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeetingHeaderSection(uiState: MeetingDetailUiState) {
    Column {
        // Title
        Text(
            text = uiState.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Time
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${uiState.dateFormatted} \u00B7 ${uiState.startTimeFormatted} \u2013 ${uiState.endTimeFormatted} (${uiState.durationFormatted})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Status badge
        if (uiState.isInProgress) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\uD83D\uDFE2 In progress",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Location
        uiState.location?.let { loc ->
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = loc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Attendees
        if (uiState.attendees.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    uiState.attendees.forEach { attendee ->
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = attendee,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
            }
        }

        // Description
        uiState.description?.let { desc ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ==================== Notes Section (3.3.4) ====================

@Composable
private fun NotesSection(
    notes: String,
    isEditing: Boolean,
    isSaved: Boolean,
    onToggleEdit: () -> Unit,
    onNotesChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\uD83D\uDCDD Notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Row {
                if (isEditing && !isSaved) {
                    Text(
                        text = "Unsaved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                IconButton(onClick = if (isEditing) onSave else onToggleEdit) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Save notes" else "Edit notes",
                    )
                }
            }
        }

        if (isEditing) {
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                placeholder = { Text("Add meeting notes...") },
                shape = RoundedCornerShape(12.dp),
            )
        } else {
            if (notes.isNotEmpty()) {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                Text(
                    text = "No notes yet. Tap the edit icon to add notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

// ==================== Action Items Section (3.3.5) ====================

@Composable
private fun ActionItemsSection(
    actionItems: List<ActionItemUiModel>,
    isExtracting: Boolean,
    onExtract: () -> Unit,
    onToggleComplete: (Int) -> Unit,
    onConvertToTask: (Int) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\u2705 Action Items (${actionItems.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            if (isExtracting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                TextButton(onClick = onExtract) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Extract from Notes")
                }
            }
        }

        if (actionItems.isEmpty()) {
            Text(
                text = "No action items yet. Write notes and tap \"Extract\" to find them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        } else {
            actionItems.forEach { item ->
                ActionItemRow(
                    item = item,
                    onToggleComplete = { onToggleComplete(item.index) },
                    onConvertToTask = { onConvertToTask(item.index) },
                )
            }
        }
    }
}

@Composable
private fun ActionItemRow(
    item: ActionItemUiModel,
    onToggleComplete: () -> Unit,
    onConvertToTask: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Checkbox icon
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = if (item.isCompleted) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Outlined.CheckCircle
                    },
                    contentDescription = if (item.isCompleted) "Mark incomplete" else "Mark complete",
                    tint = if (item.isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                )
                item.assignee?.let { assignee ->
                    Text(
                        text = "@$assignee",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Convert to task button
            if (item.linkedTaskId != null) {
                TextButton(onClick = onConvertToTask) {
                    Text("View Task", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                IconButton(
                    onClick = onConvertToTask,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Convert to task",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

// ==================== Agenda Section (3.3.6) ====================

@Composable
private fun AgendaSection(
    agenda: String,
    isEditing: Boolean,
    isSaved: Boolean,
    onToggleEdit: () -> Unit,
    onAgendaChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\uD83D\uDCCB Agenda",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Row {
                if (isEditing && !isSaved) {
                    Text(
                        text = "Unsaved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                IconButton(onClick = if (isEditing) onSave else onToggleEdit) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Save agenda" else "Edit agenda",
                    )
                }
            }
        }

        if (isEditing) {
            OutlinedTextField(
                value = agenda,
                onValueChange = onAgendaChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = { Text("Add agenda items, one per line...") },
                shape = RoundedCornerShape(12.dp),
            )
        } else {
            if (agenda.isNotEmpty()) {
                agenda.lines().filter { it.isNotBlank() }.forEach { line ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(
                            text = line.trim(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                Text(
                    text = "No agenda set. Tap the edit icon to add topics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

package com.prio.app.feature.capture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.ui.theme.PrioTheme
import com.prio.core.ui.theme.QuadrantColors
import kotlinx.coroutines.delay

/**
 * Quick Capture UI state.
 * 
 * Per 1.1.3 Quick Capture Flow Specification.
 */
data class QuickCaptureUiState(
    val inputText: String = "",
    val isVoiceInputActive: Boolean = false,
    val isAiParsing: Boolean = false,
    val parsedResult: ParsedTaskResult? = null,
    val showPreview: Boolean = false,
    val isCreating: Boolean = false,
    val isCreated: Boolean = false,
    val error: String? = null
)

/**
 * Parsed task result from AI.
 */
data class ParsedTaskResult(
    val title: String,
    val dueDate: String? = null,
    val dueDateFormatted: String? = null,
    val dueTime: String? = null,
    val quadrant: EisenhowerQuadrant = EisenhowerQuadrant.ELIMINATE,
    val aiExplanation: String? = null,
    val suggestedGoal: SuggestedGoal? = null,
    val confidence: Float = 0.8f
)

/**
 * Suggested goal for linking.
 */
data class SuggestedGoal(
    val id: Long,
    val title: String,
    val reason: String
)

/**
 * Quick Capture events.
 */
sealed interface QuickCaptureEvent {
    data class UpdateInput(val text: String) : QuickCaptureEvent
    object StartVoiceInput : QuickCaptureEvent
    object StopVoiceInput : QuickCaptureEvent
    object ParseInput : QuickCaptureEvent
    data class UpdateParsedTitle(val title: String) : QuickCaptureEvent
    data class UpdateParsedQuadrant(val quadrant: EisenhowerQuadrant) : QuickCaptureEvent
    data class AddSuggestionToInput(val suggestion: String) : QuickCaptureEvent
    object CreateTask : QuickCaptureEvent
    object OpenEditDetails : QuickCaptureEvent
    object Dismiss : QuickCaptureEvent
    object Reset : QuickCaptureEvent
}

/**
 * Quick Capture Bottom Sheet.
 * 
 * Implements 3.1.5 from ACTION_PLAN.md per 1.1.3 Quick Capture Flow Specification:
 * - FAB→focus <100ms target
 * - Voice input with on-device processing
 * - AI parsing preview
 * - Haptic confirm on task creation
 * - <5s total flow target
 * - Works offline
 * 
 * @param state Current UI state
 * @param onEvent Event handler
 * @param onDismiss Called when sheet dismissed
 * @param onOpenDetails Called when user wants full edit mode
 * @param onTaskCreated Called when task successfully created
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureSheet(
    state: QuickCaptureUiState,
    onEvent: (QuickCaptureEvent) -> Unit,
    onDismiss: () -> Unit,
    onOpenDetails: (Long) -> Unit = {},
    onTaskCreated: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    
    // Request focus immediately when opened (target: <100ms)
    LaunchedEffect(Unit) {
        delay(50) // Small delay for sheet animation
        focusRequester.requestFocus()
    }
    
    // Handle task created state
    LaunchedEffect(state.isCreated) {
        if (state.isCreated) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(500) // Show checkmark briefly
            onDismiss()
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = {
            onEvent(QuickCaptureEvent.Dismiss)
            onDismiss()
        },
        sheetState = sheetState,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Created success animation
            AnimatedVisibility(
                visible = state.isCreated,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Created",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Main content (hidden during success animation)
            AnimatedVisibility(
                visible = !state.isCreated,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    // Input section
                    InputSection(
                        inputText = state.inputText,
                        isVoiceActive = state.isVoiceInputActive,
                        isParsing = state.isAiParsing,
                        focusRequester = focusRequester,
                        onInputChange = { onEvent(QuickCaptureEvent.UpdateInput(it)) },
                        onVoiceClick = {
                            if (state.isVoiceInputActive) {
                                onEvent(QuickCaptureEvent.StopVoiceInput)
                            } else {
                                onEvent(QuickCaptureEvent.StartVoiceInput)
                            }
                        },
                        onClose = {
                            onEvent(QuickCaptureEvent.Dismiss)
                            onDismiss()
                        },
                        onDone = { onEvent(QuickCaptureEvent.ParseInput) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Quick suggestions
                    AnimatedVisibility(
                        visible = !state.showPreview && state.inputText.isEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        QuickSuggestions(
                            onSuggestionClick = { onEvent(QuickCaptureEvent.AddSuggestionToInput(it)) }
                        )
                    }
                    
                    // AI Parsing indicator
                    AnimatedVisibility(
                        visible = state.isAiParsing,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        AiParsingIndicator()
                    }
                    
                    // Parsed result preview
                    AnimatedVisibility(
                        visible = state.parsedResult != null && !state.isAiParsing,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                    ) {
                        state.parsedResult?.let { result ->
                            ParsedResultPreview(
                                result = result,
                                onTitleChange = { onEvent(QuickCaptureEvent.UpdateParsedTitle(it)) },
                                onQuadrantChange = { onEvent(QuickCaptureEvent.UpdateParsedQuadrant(it)) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action buttons (only show when we have parsed result)
                    AnimatedVisibility(
                        visible = state.parsedResult != null && !state.isAiParsing,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ActionButtons(
                            isCreating = state.isCreating,
                            onEditDetails = { onEvent(QuickCaptureEvent.OpenEditDetails) },
                            onCreate = { onEvent(QuickCaptureEvent.CreateTask) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InputSection(
    inputText: String,
    isVoiceActive: Boolean,
    isParsing: Boolean,
    focusRequester: FocusRequester,
    onInputChange: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice input button with privacy indicator
            IconButton(
                onClick = onVoiceClick,
                modifier = Modifier.semantics {
                    contentDescription = if (isVoiceActive) "Stop voice input" else "Start voice input (on-device)"
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isVoiceActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    // Privacy indicator (lock emoji) per spec
                    Text(
                        text = "🔒",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            // Text input
            TextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { 
                    Text(
                        text = "Type or speak (on-device)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                enabled = !isParsing
            )
            
            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.semantics { contentDescription = "Close" }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickSuggestions(
    onSuggestionClick: (String) -> Unit
) {
    // Time-based suggestions per spec
    val currentHour = remember { java.time.LocalTime.now().hour }
    
    val suggestions = remember(currentHour) {
        when {
            currentHour in 6..11 -> listOf("Tomorrow 9am", "This afternoon", "This week")
            currentHour in 12..16 -> listOf("Tomorrow", "End of day", "This week")
            else -> listOf("Tomorrow morning", "This weekend", "Next week")
        }
    }
    
    Column {
        Text(
            text = "Quick suggestions:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { suggestion ->
                AssistChip(
                    onClick = { onSuggestionClick(suggestion) },
                    label = { Text(suggestion) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            
            // Urgent chip
            item {
                AssistChip(
                    onClick = { onSuggestionClick("Urgent") },
                    label = { Text("Urgent") },
                    leadingIcon = {
                        Text("🔴", style = MaterialTheme.typography.labelSmall)
                    }
                )
            }
        }
    }
}

@Composable
private fun AiParsingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Understanding...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun ParsedResultPreview(
    result: ParsedTaskResult,
    onTitleChange: (String) -> Unit,
    onQuadrantChange: (EisenhowerQuadrant) -> Unit
) {
    var showQuadrantPicker by remember { mutableStateOf(false) }
    
    Column {
        Divider()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "AI Interpretation:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title row
                ParsedFieldRow(
                    emoji = "📋",
                    label = result.title,
                    onEditClick = { /* TODO: Inline edit */ }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Due date row
                if (result.dueDateFormatted != null) {
                    ParsedFieldRow(
                        emoji = "📅",
                        label = result.dueDateFormatted,
                        confidence = result.confidence,
                        onEditClick = { /* TODO: Date picker */ }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Quadrant row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showQuadrantPicker = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = result.quadrant.emoji,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.quadrant.displayLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = result.quadrant.color
                        )
                        if (result.aiExplanation != null) {
                            Text(
                                text = "\"${result.aiExplanation}\"",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { showQuadrantPicker = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change priority",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                // Suggested goal (optional)
                result.suggestedGoal?.let { goal ->
                    Spacer(modifier = Modifier.height(12.dp))
                    ParsedFieldRow(
                        emoji = "🎯",
                        label = goal.title,
                        sublabel = "AI Suggestion: \"${goal.reason}\"",
                        onEditClick = { /* TODO: Goal picker */ }
                    )
                }
            }
        }
    }
    
    // Quadrant picker
    if (showQuadrantPicker) {
        QuadrantPickerDialog(
            currentQuadrant = result.quadrant,
            onSelect = { 
                onQuadrantChange(it)
                showQuadrantPicker = false
            },
            onDismiss = { showQuadrantPicker = false }
        )
    }
}

@Composable
private fun ParsedFieldRow(
    emoji: String,
    label: String,
    sublabel: String? = null,
    confidence: Float = 1f,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            sublabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Confidence indicator for low confidence
        if (confidence < 0.7f) {
            ConfidenceIndicator(confidence)
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        IconButton(
            onClick = onEditClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ConfidenceIndicator(confidence: Float) {
    val dots = when {
        confidence >= 0.7f -> 3
        confidence >= 0.4f -> 2
        else -> 1
    }
    
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < dots) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun ActionButtons(
    isCreating: Boolean,
    onEditDetails: () -> Unit,
    onCreate: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onEditDetails,
            enabled = !isCreating,
            modifier = Modifier.weight(1f)
        ) {
            Text("Edit Details")
        }
        
        Button(
            onClick = onCreate,
            enabled = !isCreating,
            modifier = Modifier.weight(1f)
        ) {
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Task")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuadrantPickerDialog(
    currentQuadrant: EisenhowerQuadrant,
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
                text = "Choose Priority",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            EisenhowerQuadrant.entries.forEach { quadrant ->
                QuadrantOptionRow(
                    quadrant = quadrant,
                    isSelected = quadrant == currentQuadrant,
                    onClick = { onSelect(quadrant) }
                )
                
                if (quadrant != EisenhowerQuadrant.entries.last()) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuadrantOptionRow(
    quadrant: EisenhowerQuadrant,
    isSelected: Boolean,
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
            text = quadrant.emoji,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quadrant.displayLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) quadrant.color else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = quadrant.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = quadrant.color
            )
        }
    }
}

// Extension properties
private val EisenhowerQuadrant.displayLabel: String
    get() = when (this) {
        EisenhowerQuadrant.DO_FIRST -> "DO FIRST"
        EisenhowerQuadrant.SCHEDULE -> "SCHEDULE"
        EisenhowerQuadrant.DELEGATE -> "DELEGATE"
        EisenhowerQuadrant.ELIMINATE -> "MAYBE LATER"
    }

private val EisenhowerQuadrant.description: String
    get() = when (this) {
        EisenhowerQuadrant.DO_FIRST -> "Urgent + Important"
        EisenhowerQuadrant.SCHEDULE -> "Important, Not Urgent"
        EisenhowerQuadrant.DELEGATE -> "Urgent, Not Important"
        EisenhowerQuadrant.ELIMINATE -> "Neither Urgent nor Important"
    }

private val EisenhowerQuadrant.emoji: String
    get() = when (this) {
        EisenhowerQuadrant.DO_FIRST -> "🔴"
        EisenhowerQuadrant.SCHEDULE -> "🟡"
        EisenhowerQuadrant.DELEGATE -> "🟠"
        EisenhowerQuadrant.ELIMINATE -> "⚪"
    }

private val EisenhowerQuadrant.color: Color
    get() = when (this) {
        EisenhowerQuadrant.DO_FIRST -> QuadrantColors.doFirst
        EisenhowerQuadrant.SCHEDULE -> QuadrantColors.schedule
        EisenhowerQuadrant.DELEGATE -> QuadrantColors.delegate
        EisenhowerQuadrant.ELIMINATE -> QuadrantColors.eliminate
    }

@Preview(showBackground = true)
@Composable
private fun QuickCapturePreview() {
    PrioTheme {
        // Preview would require special handling for bottom sheet
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quick Capture Preview")
            }
        }
    }
}

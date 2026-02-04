package com.prio.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prio.core.ui.theme.PrioTheme

/**
 * AI processing state for Quick Capture.
 */
enum class AiProcessingState {
    /** No AI processing */
    IDLE,
    /** AI is analyzing input */
    PROCESSING,
    /** AI classification complete */
    COMPLETE
}

/**
 * PrioTextField component per 1.1.3 Quick Capture Flow spec.
 * 
 * Enhanced text field with:
 * - AI processing indicator
 * - Voice input button
 * - On-device privacy indicator
 * - Clear button
 * 
 * @param value Current text value
 * @param onValueChange Called when text changes
 * @param placeholder Placeholder text when empty
 * @param onSubmit Called when user submits (keyboard done)
 * @param onVoiceInput Called when voice input button is tapped
 * @param aiState Current AI processing state
 * @param isVoiceRecording Whether voice recording is active
 * @param showPrivacyIndicator Whether to show 🔒 on-device indicator
 * @param modifier Modifier for customization
 */
@Composable
fun PrioTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Add a new task...",
    onSubmit: (() -> Unit)? = null,
    onVoiceInput: (() -> Unit)? = null,
    onVoiceStop: (() -> Unit)? = null,
    aiState: AiProcessingState = AiProcessingState.IDLE,
    isVoiceRecording: Boolean = false,
    showPrivacyIndicator: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Privacy indicator
        if (showPrivacyIndicator) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI runs on device",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Main input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text field
            Box(modifier = Modifier.weight(1f)) {
                // Placeholder
                if (value.isEmpty() && !isVoiceRecording) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                // Voice recording indicator
                if (isVoiceRecording) {
                    VoiceRecordingIndicator()
                }
                
                // Actual text field
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .semantics {
                            contentDescription = "Task input. $placeholder"
                        },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onSubmit?.invoke() }
                    ),
                    singleLine = false,
                    maxLines = 3
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // AI processing indicator
            AnimatedVisibility(
                visible = aiState == AiProcessingState.PROCESSING,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AiProcessingIndicator()
            }
            
            // Clear button
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear text",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Voice input button
            if (onVoiceInput != null) {
                IconButton(
                    onClick = {
                        if (isVoiceRecording) {
                            onVoiceStop?.invoke()
                        } else {
                            onVoiceInput()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isVoiceRecording) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                ) {
                    Icon(
                        imageVector = if (isVoiceRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isVoiceRecording) "Stop recording" else "Start voice input",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AiProcessingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_processing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = "AI",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun VoiceRecordingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_recording")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = alpha))
        )
        Text(
            text = "Listening...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error.copy(alpha = alpha)
        )
    }
}

// ============ Previews ============

@Preview(showBackground = true)
@Composable
private fun PrioTextFieldPreview() {
    var text by remember { mutableStateOf("") }
    
    PrioTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Empty state
            PrioTextField(
                value = "",
                onValueChange = {},
                placeholder = "Add a new task...",
                onVoiceInput = {}
            )
            
            // With text
            PrioTextField(
                value = "Call dentist about appointment",
                onValueChange = {},
                onVoiceInput = {}
            )
            
            // Processing
            PrioTextField(
                value = "Submit quarterly report tomorrow",
                onValueChange = {},
                aiState = AiProcessingState.PROCESSING,
                onVoiceInput = {}
            )
            
            // Voice recording
            PrioTextField(
                value = "",
                onValueChange = {},
                isVoiceRecording = true,
                onVoiceInput = {},
                onVoiceStop = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F2937)
@Composable
private fun PrioTextFieldDarkPreview() {
    PrioTheme(darkTheme = true) {
        PrioTextField(
            value = "Submit quarterly report",
            onValueChange = {},
            aiState = AiProcessingState.PROCESSING,
            onVoiceInput = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

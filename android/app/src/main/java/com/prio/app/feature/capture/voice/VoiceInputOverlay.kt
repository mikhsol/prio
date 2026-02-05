package com.prio.app.feature.capture.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Voice input overlay UI for QuickCaptureSheet.
 *
 * Per 1.1.3 Quick Capture Flow Specification - Voice Input Mode:
 * - Large 🎤 icon displayed
 * - Audio visualization waveform: ▓▓▓▓▒▒░░▒▒▓▓▓▓
 * - "Listening..." label
 * - Live transcription area shows text in real-time
 * - [■ Stop] button
 *
 * Implements 3.1.5.B.3 from ACTION_PLAN.md.
 *
 * @param voiceState Current voice input state
 * @param onStopListening Called when user taps Stop
 * @param onRetry Called when user taps Try Again after error
 * @param onTypeInstead Called when user taps Type Instead after error
 */
@Composable
fun VoiceInputOverlay(
    voiceState: VoiceInputState,
    onStopListening: () -> Unit,
    onRetry: () -> Unit,
    onTypeInstead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (voiceState) {
                is VoiceInputState.Idle -> { /* Should not be shown */ }

                is VoiceInputState.Initializing -> {
                    InitializingContent()
                }

                is VoiceInputState.Listening -> {
                    ListeningContent(
                        partialText = voiceState.partialText,
                        audioLevel = voiceState.audioLevel,
                        onStop = onStopListening
                    )
                }

                is VoiceInputState.Processing -> {
                    ProcessingContent(partialText = voiceState.partialText)
                }

                is VoiceInputState.Result -> {
                    // Result is typically handled by parent; show briefly
                    ResultContent(text = voiceState.text)
                }

                is VoiceInputState.Error -> {
                    ErrorContent(
                        error = voiceState,
                        onRetry = onRetry,
                        onTypeInstead = onTypeInstead
                    )
                }
            }
        }
    }
}

// ========================================================================
// Sub-composables for each voice state
// ========================================================================

@Composable
private fun InitializingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pulsing mic icon
        val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "mic_scale"
        )

        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Initializing voice input",
            modifier = Modifier
                .size(64.dp)
                .scale(scale),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Getting ready...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ListeningContent(
    partialText: String,
    audioLevel: Float,
    onStop: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large animated mic icon
        val animatedLevel by animateFloatAsState(
            targetValue = audioLevel,
            animationSpec = spring(stiffness = 300f),
            label = "audio_level"
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.semantics {
                contentDescription = "Voice input active. Listening for speech."
            }
        ) {
            // Background pulse circle based on audio level
            Box(
                modifier = Modifier
                    .size((64 + animatedLevel * 32).dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.15f + animatedLevel * 0.15f
                        )
                    )
            )

            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Audio waveform visualization
        AudioWaveform(
            audioLevel = animatedLevel,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // "Listening..." label
        Text(
            text = "Listening...",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Live transcription area
        AnimatedVisibility(
            visible = partialText.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = partialText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stop button per spec: [■ Stop]
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onStop()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.semantics {
                contentDescription = "Stop listening"
            }
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Stop")
        }
    }
}

@Composable
private fun ProcessingContent(partialText: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Processing...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (partialText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = partialText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ResultContent(text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Error content with retry/fallback buttons.
 *
 * Per 1.1.3 spec fallback:
 * Shows: "Couldn't understand that" with [Try Again] and [Type Instead] buttons.
 */
@Composable
private fun ErrorContent(
    error: VoiceInputState.Error,
    onRetry: () -> Unit,
    onTypeInstead: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MicOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = error.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons per spec: [Try Again] and [Type Instead]
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error.errorType != VoiceErrorType.NOT_AVAILABLE &&
                error.errorType != VoiceErrorType.PERMISSION_DENIED
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.semantics {
                        contentDescription = "Try voice input again"
                    }
                ) {
                    Text("Try Again")
                }
            }

            Button(
                onClick = onTypeInstead,
                modifier = Modifier.semantics {
                    contentDescription = "Switch to typing"
                }
            ) {
                Text("Type Instead")
            }
        }
    }
}

// ========================================================================
// Audio waveform visualization
// ========================================================================

/**
 * Simple audio waveform visualization that responds to audio level.
 *
 * Renders a row of bars at varying heights driven by the audio level,
 * creating a visual representation per spec: ▓▓▓▓▒▒░░▒▒▓▓▓▓
 */
@Composable
private fun AudioWaveform(
    audioLevel: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 20
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    // Phase offset for wave animation
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val barPhase = phase + (index * Math.PI.toFloat() / barCount * 2f)
            // Sine wave modulated by audio level for organic waveform feel
            val barHeight = remember(audioLevel, barPhase) {
                val base = 0.2f
                val wave = (kotlin.math.sin(barPhase) + 1f) / 2f
                (base + wave * audioLevel * 0.8f).coerceIn(0.1f, 1f)
            }

            val animatedHeight by animateFloatAsState(
                targetValue = barHeight,
                animationSpec = spring(stiffness = 400f),
                label = "bar_$index"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((animatedHeight * 32).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.4f + animatedHeight * 0.6f
                        )
                    )
            )
        }
    }
}

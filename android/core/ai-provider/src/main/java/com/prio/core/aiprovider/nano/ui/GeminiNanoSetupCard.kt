package com.prio.core.aiprovider.nano.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prio.core.aiprovider.nano.GeminiNanoAvailability.FeatureStatus

/**
 * Reusable Compose component showing Gemini Nano status with download controls.
 *
 * Task 3.6.7: Used in Settings > AI Model section.
 * Task 3.6.8: Used in Onboarding Screen 4 (AI Model Setup).
 *
 * @param state Current [GeminiNanoUiState]
 * @param onDownloadClick Trigger model download
 * @param onSkipClick Skip (onboarding only, nullable)
 * @param variant Visual variant: SETTINGS (compact) or ONBOARDING (expanded with illustration)
 */
@Composable
fun GeminiNanoSetupCard(
    state: GeminiNanoUiState,
    onDownloadClick: () -> Unit,
    onSkipClick: (() -> Unit)? = null,
    variant: GeminiNanoCardVariant = GeminiNanoCardVariant.SETTINGS,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Gemini Nano AI model setup" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                state.isReady -> MaterialTheme.colorScheme.primaryContainer
                state.isSupported -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        state.isReady -> Icons.Outlined.CheckCircle
                        state.isSupported -> Icons.Outlined.AutoAwesome
                        else -> Icons.Outlined.Psychology
                    },
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = when {
                        state.isReady -> MaterialTheme.colorScheme.primary
                        state.isSupported -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = when {
                            state.isReady -> "Gemini Nano Active"
                            state.isSupported -> "Built-in AI Available"
                            else -> "AI Model Setup"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = state.statusSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Onboarding variant: expanded description
            if (variant == GeminiNanoCardVariant.ONBOARDING) {
                Text(
                    text = when {
                        state.isReady -> "Your device has built-in AI capabilities. " +
                            "No download needed! All AI runs privately on your device."
                        state.isSupported -> "Your device supports Google's built-in Gemini Nano AI. " +
                            "Enable it for instant, private AI — no 2.3 GB download required."
                        else -> "Your device doesn't support built-in AI. " +
                            "You can download a 2.3 GB AI model, or use the fast rule-based engine."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Download progress bar
            AnimatedVisibility(
                visible = state.downloadProgress != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    LinearProgressIndicator(
                        progress = { (state.downloadProgress ?: 0) / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Downloading Gemini Nano… ${state.downloadProgress ?: 0}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Loading indicator
            if (state.isLoading && state.downloadProgress == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking availability…", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Error/success message
            state.message?.let { msg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (msg.startsWith("Error") || msg.contains("failed"))
                            Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (msg.startsWith("Error") || msg.contains("failed"))
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (msg.startsWith("Error") || msg.contains("failed"))
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Action buttons
            if (!state.isReady && !state.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.isSupported) {
                        Button(
                            onClick = onDownloadClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (state.promptStatus) {
                                    is FeatureStatus.Available -> "Activate"
                                    is FeatureStatus.Downloadable -> "Download & Enable"
                                    else -> "Enable"
                                }
                            )
                        }
                    }

                    onSkipClick?.let { skip ->
                        OutlinedButton(onClick = skip) {
                            Text(if (state.isSupported) "Later" else "Skip")
                        }
                    }
                }
            }
        }
    }
}

enum class GeminiNanoCardVariant {
    /** Compact card for Settings > AI Model section */
    SETTINGS,
    /** Expanded card with description for Onboarding Screen 4 */
    ONBOARDING
}

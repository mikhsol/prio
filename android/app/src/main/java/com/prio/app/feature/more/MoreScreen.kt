package com.prio.app.feature.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prio.core.ui.theme.PrioTheme

/**
 * More Screen per 1.1.9 Settings Screens Spec.
 * 
 * Entry point for settings and additional options.
 * 
 * Features:
 * - Quick access to Insights/Analytics
 * - Settings categories (Notifications, Appearance, AI, etc.)
 * - Privacy & About sections
 * 
 * This is a PLACEHOLDER implementation for Milestone 3.1.5.
 * Full settings implementation in Milestone 4.1 (Onboarding & Settings).
 * 
 * @param onNavigateToSettings Navigate to main settings
 * @param onNavigateToInsights Navigate to insights/analytics
 * @param onNavigateToPrivacyPolicy Navigate to privacy policy
 * @param onNavigateToAbout Navigate to about screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "More",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Insights Card (Prominent)
            item {
                InsightsCard(onClick = onNavigateToInsights)
            }
            
            // Settings Section
            item {
                SectionHeader(title = "Settings")
            }
            
            item {
                SettingsGroup(
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Outlined.Notifications,
                            title = "Notifications",
                            subtitle = "Reminders, alerts, and daily briefings",
                            onClick = onNavigateToSettings
                        ),
                        SettingsItem(
                            icon = Icons.Outlined.DarkMode,
                            title = "Appearance",
                            subtitle = "Theme, colors, and display options",
                            onClick = onNavigateToSettings
                        ),
                        SettingsItem(
                            icon = Icons.Outlined.Psychology,
                            title = "AI & Classification",
                            subtitle = "Eisenhower settings, AI model preferences",
                            onClick = onNavigateToSettings
                        ),
                        SettingsItem(
                            icon = Icons.Outlined.Sync,
                            title = "Calendar Sync",
                            subtitle = "Connected calendars and sync settings",
                            onClick = onNavigateToSettings
                        ),
                        SettingsItem(
                            icon = Icons.Outlined.Backup,
                            title = "Backup & Export",
                            subtitle = "Data backup, export, and restore",
                            onClick = onNavigateToSettings
                        )
                    )
                )
            }
            
            // About Section
            item {
                SectionHeader(title = "About")
            }
            
            item {
                SettingsGroup(
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Outlined.Policy,
                            title = "Privacy Policy",
                            subtitle = "Your data stays on your device",
                            onClick = onNavigateToPrivacyPolicy
                        ),
                        SettingsItem(
                            icon = Icons.AutoMirrored.Outlined.HelpOutline,
                            title = "Help & Support",
                            subtitle = "FAQs, contact support",
                            onClick = onNavigateToAbout
                        ),
                        SettingsItem(
                            icon = Icons.Outlined.Info,
                            title = "About Prio",
                            subtitle = "Version 1.0.0",
                            onClick = onNavigateToAbout
                        )
                    )
                )
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun InsightsCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Analytics,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Productivity Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Track your progress and patterns",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .padding(vertical = 8.dp)
            .semantics { heading() }
    )
}

data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
private fun SettingsGroup(
    items: List<SettingsItem>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column {
            items.forEachIndexed { index, item ->
                SettingsRow(
                    icon = item.icon,
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = item.onClick
                )
                
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
            .semantics { contentDescription = "$title: $subtitle" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MoreScreenPreview() {
    PrioTheme {
        MoreScreen()
    }
}

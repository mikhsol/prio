package com.prio.app.feature.settings

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prio.core.ui.theme.PrioTheme

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBriefings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToAiModel: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onNavigateToBriefings = onNavigateToBriefings,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToAppearance = onNavigateToAppearance,
        onNavigateToAiModel = onNavigateToAiModel,
        onNavigateToAbout = onNavigateToAbout,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onNavigateBack: () -> Unit,
    onNavigateToBriefings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToAiModel: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
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
            // Profile section
            item {
                SettingsSectionCard(
                    items = listOf(
                        SettingsRowData(
                            icon = null,
                            title = "👤  Profile",
                            subtitle = state.preferences.userName ?: "Not signed in",
                            onClick = {}
                        )
                    )
                )
            }

            // Preferences section
            item {
                SectionHeader(title = "PREFERENCES")
            }

            item {
                SettingsSectionCard(
                    items = listOf(
                        SettingsRowData(
                            icon = Icons.Outlined.WbSunny,
                            title = "Daily Briefings",
                            subtitle = "Morning: ${formatTime(state.preferences.morningBriefingTime.hour, state.preferences.morningBriefingTime.minute)} • Evening: ${formatTime(state.preferences.eveningSummaryTime.hour, state.preferences.eveningSummaryTime.minute)}",
                            onClick = onNavigateToBriefings
                        ),
                        SettingsRowData(
                            icon = Icons.Outlined.Notifications,
                            title = "Notifications",
                            subtitle = if (state.preferences.notificationsEnabled) "Enabled" else "Disabled",
                            onClick = onNavigateToNotifications
                        ),
                        SettingsRowData(
                            icon = Icons.Outlined.DarkMode,
                            title = "Appearance",
                            subtitle = state.preferences.themeMode.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            onClick = onNavigateToAppearance
                        )
                    )
                )
            }

            // AI & Data section
            item {
                SectionHeader(title = "AI & DATA")
            }

            item {
                SettingsSectionCard(
                    items = listOf(
                        SettingsRowData(
                            icon = Icons.Outlined.SmartToy,
                            title = "AI Model",
                            subtitle = if (state.preferences.aiModelDownloaded) "Installed" else "Not installed",
                            onClick = onNavigateToAiModel
                        ),
                        SettingsRowData(
                            icon = Icons.Outlined.CalendarMonth,
                            title = "Calendar",
                            subtitle = if (state.preferences.calendarConnected) "Connected" else "Not connected",
                            onClick = {}
                        ),
                        SettingsRowData(
                            icon = Icons.Outlined.Backup,
                            title = "Backup & Export",
                            subtitle = "Local backup",
                            onClick = {}
                        )
                    )
                )
            }

            // About section
            item {
                SectionHeader(title = "ABOUT")
            }

            item {
                SettingsSectionCard(
                    items = listOf(
                        SettingsRowData(
                            icon = Icons.Outlined.Policy,
                            title = "Privacy Policy",
                            subtitle = null,
                            onClick = {}
                        ),
                        SettingsRowData(
                            icon = Icons.Outlined.Info,
                            title = "About Prio",
                            subtitle = "Version 1.0.0",
                            onClick = onNavigateToAbout
                        )
                    )
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
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
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(start = 4.dp)
            .semantics { heading() }
    )
}

data class SettingsRowData(
    val icon: ImageVector?,
    val title: String,
    val subtitle: String?,
    val onClick: () -> Unit
)

@Composable
private fun SettingsSectionCard(
    items: List<SettingsRowData>,
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { item.onClick() }
                        .padding(16.dp)
                        .semantics {
                            contentDescription = "${item.title}${item.subtitle?.let { ": $it" } ?: ""}"
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.subtitle != null) {
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = if (item.icon != null) 56.dp else 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return "$h:%02d $period".format(minute)
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PrioTheme {
        SettingsContent(
            state = SettingsUiState(isLoading = false),
            onNavigateBack = {},
            onNavigateToBriefings = {},
            onNavigateToNotifications = {},
            onNavigateToAppearance = {},
            onNavigateToAiModel = {},
            onNavigateToAbout = {}
        )
    }
}

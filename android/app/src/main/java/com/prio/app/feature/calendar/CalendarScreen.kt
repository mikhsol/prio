package com.prio.app.feature.calendar

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.ui.theme.QuadrantColors
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendar Day View per 1.1.6 Calendar Day View Spec.
 *
 * Features:
 * - Week date strip with event indicator dots
 * - Hourly timeline (60dp/hour) with current-time indicator (NOW line)
 * - Calendar events as colored blocks with location & attendee count
 * - Tasks with Eisenhower quadrant badges in timeline
 * - Collapsible tasks-without-time section at bottom
 * - Calendar permission prompt (privacy-first per Maya persona)
 * - Pull-to-refresh for calendar sync
 * - WCAG 2.1 AA accessibility with full semantics
 *
 * @param onNavigateToMeeting Navigate to meeting detail by ID
 * @param onNavigateToTask Navigate to task detail by ID
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToMeeting: (Long) -> Unit = {},
    onNavigateToTask: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Runtime permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.onEvent(CalendarEvent.OnPermissionGranted)
        } else {
            viewModel.onEvent(CalendarEvent.OnPermissionDenied)
        }
    }

    // Collect one-shot effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                CalendarEffect.RequestCalendarPermission -> {
                    permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                }

                is CalendarEffect.NavigateToMeeting -> onNavigateToMeeting(effect.meetingId)
                is CalendarEffect.NavigateToTask -> onNavigateToTask(effect.taskId)
                is CalendarEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    val monthYear = uiState.selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = monthYear,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(CalendarEvent.OnTodayTap) }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Go to today")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Week strip navigation
            WeekNavigationBar(
                onPreviousWeek = { viewModel.onEvent(CalendarEvent.OnPreviousWeek) },
                onNextWeek = { viewModel.onEvent(CalendarEvent.OnNextWeek) },
            )

            WeekDateStrip(
                days = uiState.weekDays,
                onDateSelected = { viewModel.onEvent(CalendarEvent.OnDateSelected(it)) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Main content area
            when {
                // First-time permission prompt
                uiState.showPermissionPrompt -> {
                    CalendarPermissionPrompt(
                        onConnect = { viewModel.onEvent(CalendarEvent.OnRequestCalendarPermission) },
                        onSkip = { viewModel.onEvent(CalendarEvent.OnSkipCalendarSetup) },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    )
                }

                // Empty state
                uiState.timelineItems.isEmpty() &&
                    uiState.untimedTaskItems.isEmpty() &&
                    !uiState.isLoading -> {
                    EmptyDayContent(
                        hasCalendarPermission = uiState.hasCalendarPermission,
                        onConnectCalendar = {
                            viewModel.onEvent(CalendarEvent.OnRequestCalendarPermission)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    )
                }

                // Main timeline + tasks
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    ) {
                        DayContent(
                            timelineItems = uiState.timelineItems,
                            untimedTasks = uiState.untimedTaskItems,
                            currentTimeMinutes = uiState.currentTimeMinutes,
                            onMeetingClick = onNavigateToMeeting,
                            onTaskClick = onNavigateToTask,
                        )
                    }
                }
            }
        }
    }
}

// ==================== Week Navigation ====================

@Composable
private fun WeekNavigationBar(
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week")
        }
        Text(
            text = "This Week",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onNextWeek) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next week")
        }
    }
}

@Composable
private fun WeekDateStrip(
    days: List<DayChipUiModel>,
    onDateSelected: (java.time.LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        items(days) { chip ->
            DateChip(
                chip = chip,
                onClick = { onDateSelected(chip.date) },
            )
        }
    }
}

@Composable
private fun DateChip(
    chip: DayChipUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            chip.isSelected -> MaterialTheme.colorScheme.primary
            chip.isToday -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        label = "dateBg",
    )
    val textColor = when {
        chip.isSelected -> MaterialTheme.colorScheme.onPrimary
        chip.isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val cd = buildString {
        append(chip.dayName)
        append(", ")
        append(chip.date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
        append(" ")
        append(chip.dayNumber)
        if (chip.hasEvents) append(". Has events") else append(". No events")
    }

    Surface(
        modifier = modifier
            .padding(4.dp)
            .clickable { onClick() }
            .semantics { contentDescription = cd },
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (chip.isSelected) 4.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = chip.dayName,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chip.dayNumber.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
            if (chip.hasEvents) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (chip.isSelected) textColor.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.primary,
                        ),
                )
            }
        }
    }
}

// ==================== Day Content (Timeline + Untimed Tasks) ====================

private const val TIMELINE_START_HOUR = 6
private const val TIMELINE_END_HOUR = 22

@Composable
private fun DayContent(
    timelineItems: List<TimelineItemUiModel>,
    untimedTasks: List<UntimedTaskUiModel>,
    currentTimeMinutes: Int,
    onMeetingClick: (Long) -> Unit,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hourHeight: Dp = 60.dp

    // Auto-scroll to current time area on first composition
    val listState = rememberLazyListState()
    val currentHourIndex =
        (currentTimeMinutes / 60 - TIMELINE_START_HOUR).coerceIn(0, TIMELINE_END_HOUR - TIMELINE_START_HOUR)
    LaunchedEffect(Unit) {
        listState.scrollToItem(maxOf(0, currentHourIndex - 1))
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        // Section header
        item(key = "timeline_header") {
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { heading() },
            )
        }

        // Hour rows (6 AM – 10 PM)
        items(TIMELINE_END_HOUR - TIMELINE_START_HOUR) { index ->
            val hour = TIMELINE_START_HOUR + index
            val hourStartMinutes = hour * 60
            val hourEndMinutes = hourStartMinutes + 60

            // Events whose block starts in this hour slot
            val eventsStartingHere = timelineItems.filter { item ->
                item.startMinutes in hourStartMinutes until hourEndMinutes
            }

            // Current-time indicator
            val showCurrentTime = currentTimeMinutes in hourStartMinutes until hourEndMinutes
            val currentTimeOffsetFraction = if (showCurrentTime) {
                (currentTimeMinutes - hourStartMinutes).toFloat() / 60f
            } else {
                0f
            }

            HourRow(
                hour = hour,
                height = hourHeight,
                events = eventsStartingHere,
                showCurrentTimeIndicator = showCurrentTime,
                currentTimeOffsetFraction = currentTimeOffsetFraction,
                primaryColor = primaryColor,
                outlineColor = outlineColor,
                onMeetingClick = onMeetingClick,
                onTaskClick = onTaskClick,
            )
        }

        // Tasks-without-time section
        if (untimedTasks.isNotEmpty()) {
            item(key = "untimed_divider") {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item(key = "untimed_header") {
                Text(
                    text = "\uD83D\uDCCB Tasks Without Time (${untimedTasks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics { heading() },
                )
            }

            items(untimedTasks, key = { "untimed_${it.id}" }) { task ->
                UntimedTaskCard(
                    task = task,
                    onClick = { onTaskClick(task.id) },
                )
            }
        }
    }
}

// ==================== Hour Row ====================

@Composable
private fun HourRow(
    hour: Int,
    height: Dp,
    events: List<TimelineItemUiModel>,
    showCurrentTimeIndicator: Boolean,
    currentTimeOffsetFraction: Float,
    primaryColor: Color,
    outlineColor: Color,
    onMeetingClick: (Long) -> Unit,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = formatHourLabel(hour)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                // Horizontal grid line at top of row
                drawLine(
                    color = outlineColor,
                    start = Offset(120f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1f,
                )
                // NOW indicator
                if (showCurrentTimeIndicator) {
                    val y = size.height * currentTimeOffsetFraction
                    drawCircle(color = primaryColor, radius = 6f, center = Offset(120f, y))
                    drawLine(
                        color = primaryColor,
                        start = Offset(126f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f,
                    )
                }
            },
    ) {
        // Hour label (left gutter)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(48.dp)
                .padding(start = 8.dp, top = 2.dp),
        )

        // Event blocks starting in this hour
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 16.dp, top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            events.forEach { item ->
                TimelineEventBlock(
                    item = item,
                    onClick = {
                        when (item.type) {
                            TimelineItemType.MEETING -> onMeetingClick(item.id)
                            TimelineItemType.TASK -> onTaskClick(item.id)
                        }
                    },
                )
            }
        }
    }
}

// ==================== Event Block ====================

@Composable
private fun TimelineEventBlock(
    item: TimelineItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = resolveEventBackground(item)
    val borderColor = resolveEventBorder(item)
    val alphaVal = if (item.isPast && !item.isInProgress) 0.6f else 1f

    val durationText = run {
        val mins = item.endMinutes - item.startMinutes
        if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
    }

    val cd = buildString {
        append(item.title)
        append(". ")
        append(formatTimeOfDay(item.startMinutes))
        append(" to ")
        append(formatTimeOfDay(item.endMinutes))
        append(". ")
        append(durationText)
        item.location?.let { append(". at $it") }
        if (item.attendeeCount > 0) append(". ${item.attendeeCount} attendees")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alphaVal)
            .clickable { onClick() }
            .semantics { contentDescription = cd },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            // Left accent line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (item.type == TimelineItemType.TASK) 32.dp else 40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(borderColor),
            )
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.location?.let { loc ->
                        Text(
                            text = "\uD83D\uDCCD $loc",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (item.attendeeCount > 0) {
                        Text(
                            text = "\uD83D\uDC65 ${item.attendeeCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun resolveEventBackground(item: TimelineItemUiModel): Color = when (item.type) {
    TimelineItemType.MEETING -> MaterialTheme.colorScheme.primaryContainer
    TimelineItemType.TASK -> {
        quadrantColor(item.quadrant).copy(alpha = 0.15f)
    }
}

@Composable
private fun resolveEventBorder(item: TimelineItemUiModel): Color = when (item.type) {
    TimelineItemType.MEETING -> {
        if (item.isInProgress) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    }

    TimelineItemType.TASK -> quadrantColor(item.quadrant)
}

private fun quadrantColor(q: EisenhowerQuadrant?): Color = when (q) {
    EisenhowerQuadrant.DO_FIRST -> QuadrantColors.doFirst
    EisenhowerQuadrant.SCHEDULE -> QuadrantColors.schedule
    EisenhowerQuadrant.DELEGATE -> QuadrantColors.delegate
    EisenhowerQuadrant.ELIMINATE -> QuadrantColors.eliminate
    else -> Color.Gray
}

// ==================== Untimed Task Card ====================

@Composable
private fun UntimedTaskCard(
    task: UntimedTaskUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
            .semantics {
                contentDescription = "${task.quadrantEmoji} ${task.title}. ${task.dueText}"
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = task.quadrantEmoji, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = task.dueText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ==================== Permission & Empty States ====================

/**
 * Privacy-first calendar permission prompt per 1.1.6 spec.
 * Messaging aligned with Maya persona: "Read-only access · Data stays on device."
 */
@Composable
private fun CalendarPermissionPrompt(
    onConnect: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Connect Your Calendar",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "See your schedule alongside tasks for better planning.\n" +
                        "Your calendar data stays on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Read-only access \u00B7 Data stays on device",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Connect")
                }

                TextButton(onClick = onSkip) {
                    Text("Skip for Now")
                }
            }
        }
    }
}

@Composable
private fun EmptyDayContent(
    hasCalendarPermission: Boolean,
    onConnectCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Events Today",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (hasCalendarPermission) {
                    "Your day is free! Schedule tasks\nor enjoy the break."
                } else {
                    "Connect your calendar to see your schedule\nalongside your tasks."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (!hasCalendarPermission) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onConnectCalendar) {
                    Text("Connect Calendar")
                }
            }
        }
    }
}

// ==================== Formatting Helpers ====================

private fun formatHourLabel(hour: Int): String = when {
    hour == 0 -> "12 AM"
    hour < 12 -> "$hour AM"
    hour == 12 -> "12 PM"
    else -> "${hour - 12} PM"
}

private fun formatTimeOfDay(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val amPm = if (h < 12) "AM" else "PM"
    val hour12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return if (m == 0) "$hour12 $amPm" else "$hour12:${m.toString().padStart(2, '0')} $amPm"
}

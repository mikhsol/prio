package com.prio.core.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prio.core.ui.theme.PrioTheme

/**
 * Meeting data for display in MeetingCard.
 */
data class MeetingCardData(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val attendeesCount: Int = 0,
    val actionItemsCount: Int = 0,
    val hasNotes: Boolean = false,
    val calendarColor: Color? = null,
    val isOngoing: Boolean = false
)

/**
 * MeetingCard component per 1.1.6 Calendar Day View spec.
 * 
 * Displays a meeting/calendar event with:
 * - Time range
 * - Title
 * - Attendee count
 * - Action items count
 * - Notes indicator
 * - Optional calendar color accent
 * 
 * @param meeting Meeting data to display
 * @param onTap Called when card is tapped (opens meeting detail)
 * @param modifier Modifier for customization
 */
@Composable
fun MeetingCard(
    meeting: MeetingCardData,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = meeting.calendarColor ?: MaterialTheme.colorScheme.primary
    
    val cardContentDescription = buildString {
        append(meeting.title)
        append(". From ${meeting.startTime} to ${meeting.endTime}")
        if (meeting.attendeesCount > 0) {
            append(". ${meeting.attendeesCount} attendees")
        }
        if (meeting.actionItemsCount > 0) {
            append(". ${meeting.actionItemsCount} action items")
        }
        if (meeting.isOngoing) {
            append(". Currently ongoing")
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = cardContentDescription }
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (meeting.isOngoing) 4.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (meeting.isOngoing) {
                accentColor.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Time column
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(52.dp)
            ) {
                Text(
                    text = meeting.startTime,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
                Text(
                    text = meeting.endTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Vertical divider accent
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .width(3.dp)
                    .height(48.dp)
            ) {
                drawRoundRect(
                    color = accentColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title with ongoing indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = meeting.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (meeting.isOngoing) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (meeting.isOngoing) {
                        Text(
                            text = "NOW",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Metadata row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attendees
                    if (meeting.attendeesCount > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = meeting.attendeesCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Action items
                    if (meeting.actionItemsCount > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${meeting.actionItemsCount} action items",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Notes indicator
                    if (meeting.hasNotes) {
                        Text(
                            text = "📝 Notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ============ Previews ============

@Preview(showBackground = true)
@Composable
private fun MeetingCardPreview() {
    PrioTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MeetingCard(
                meeting = MeetingCardData(
                    id = "1",
                    title = "Weekly Team Standup",
                    startTime = "9:00 AM",
                    endTime = "9:30 AM",
                    attendeesCount = 8,
                    actionItemsCount = 3,
                    hasNotes = true,
                    isOngoing = true
                ),
                onTap = {}
            )
            
            MeetingCard(
                meeting = MeetingCardData(
                    id = "2",
                    title = "Product Strategy Review with Leadership",
                    startTime = "2:00 PM",
                    endTime = "3:00 PM",
                    attendeesCount = 5,
                    calendarColor = Color(0xFF3B82F6) // Blue
                ),
                onTap = {}
            )
            
            MeetingCard(
                meeting = MeetingCardData(
                    id = "3",
                    title = "1:1 with Manager",
                    startTime = "4:00 PM",
                    endTime = "4:30 PM",
                    attendeesCount = 2,
                    hasNotes = true,
                    calendarColor = Color(0xFF10B981) // Green
                ),
                onTap = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F2937)
@Composable
private fun MeetingCardDarkPreview() {
    PrioTheme(darkTheme = true) {
        MeetingCard(
            meeting = MeetingCardData(
                id = "1",
                title = "Weekly Team Standup",
                startTime = "9:00 AM",
                endTime = "9:30 AM",
                attendeesCount = 8,
                isOngoing = true
            ),
            onTap = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

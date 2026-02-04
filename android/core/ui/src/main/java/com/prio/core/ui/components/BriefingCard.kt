package com.prio.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prio.core.ui.theme.PrioTheme

/**
 * Briefing type (morning or evening).
 */
enum class BriefingType(
    val emoji: String,
    val title: String,
    val gradientColors: List<Color>
) {
    MORNING(
        emoji = "☀️",
        title = "YOUR MORNING BRIEFING",
        gradientColors = listOf(
            Color(0xFFFEF3C7), // Amber-100
            Color(0xFFFDE68A)  // Amber-200
        )
    ),
    EVENING(
        emoji = "🌙",
        title = "YOUR EVENING SUMMARY",
        gradientColors = listOf(
            Color(0xFFE0E7FF), // Indigo-100
            Color(0xFFC7D2FE)  // Indigo-200
        )
    )
}

/**
 * A collapsible section in the briefing.
 */
data class BriefingSection(
    val title: String,
    val content: @Composable () -> Unit
)

/**
 * Briefing data for display.
 */
data class BriefingCardData(
    val type: BriefingType,
    val date: String,
    val summaryText: String,
    val sections: List<BriefingSection> = emptyList(),
    val isPrivate: Boolean = true, // On-device indicator
    val ctaLabel: String? = null
)

/**
 * BriefingCard component per 1.1.13 Component Specifications.
 * 
 * Displays daily briefing (morning or evening) with:
 * - Gradient background
 * - Header with type icon and date
 * - Summary text
 * - Expandable sections
 * - Optional CTA button
 * - Privacy indicator (🔒 for on-device processing)
 * 
 * @param briefing Briefing data to display
 * @param onCtaTap Called when CTA is tapped
 * @param onDismiss Called when briefing is dismissed (read) - reserved for future use
 * @param modifier Modifier for customization
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun BriefingCard(
    briefing: BriefingCardData,
    onCtaTap: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardContentDescription = buildString {
        append("${briefing.type.title} for ${briefing.date}")
        append(". ${briefing.summaryText}")
        if (briefing.isPrivate) {
            append(". Generated on device, completely private")
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = cardContentDescription },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = briefing.type.gradientColors
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = briefing.type.emoji,
                            fontSize = 20.sp
                        )
                        Text(
                            text = briefing.type.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    }
                    
                    // Privacy indicator
                    if (briefing.isPrivate) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFF4B5563)
                            )
                            Text(
                                text = "Private",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4B5563)
                            )
                        }
                    }
                }
                
                // Date
                Text(
                    text = briefing.date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4B5563)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                
                // Summary
                Text(
                    text = briefing.summaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1F2937)
                )
                
                // Expandable sections
                if (briefing.sections.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    briefing.sections.forEach { section ->
                        ExpandableSection(
                            title = section.title,
                            content = section.content
                        )
                    }
                }
                
                // CTA
                if (briefing.ctaLabel != null && onCtaTap != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onCtaTap,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = briefing.ctaLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
            
            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF4B5563)
                )
            }
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                content()
            }
        }
    }
}

// ============ Previews ============

@Preview(showBackground = true)
@Composable
private fun BriefingCardMorningPreview() {
    PrioTheme {
        BriefingCard(
            briefing = BriefingCardData(
                type = BriefingType.MORNING,
                date = "Tuesday, February 4",
                summaryText = "Good morning! You have 5 tasks today with 3 in DO FIRST. " +
                        "Your highest priority is \"Submit quarterly report\" due at 5pm.",
                sections = listOf(
                    BriefingSection("Today's Top 3") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("1. Submit quarterly report", style = MaterialTheme.typography.bodyMedium)
                            Text("2. Review team PRs", style = MaterialTheme.typography.bodyMedium)
                            Text("3. Call dentist", style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    BriefingSection("Calendar Preview") {
                        Text("3 meetings today starting at 10am", style = MaterialTheme.typography.bodyMedium)
                    }
                ),
                ctaLabel = "Start My Day →"
            ),
            onCtaTap = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BriefingCardEveningPreview() {
    PrioTheme {
        BriefingCard(
            briefing = BriefingCardData(
                type = BriefingType.EVENING,
                date = "Tuesday, February 4",
                summaryText = "Great work today! You completed 4 of 5 tasks (80%). " +
                        "Your \"Get promoted\" goal is 65% complete.",
                sections = listOf(
                    BriefingSection("Completed Today") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("✅ Submit quarterly report", style = MaterialTheme.typography.bodyMedium)
                            Text("✅ Review team PRs", style = MaterialTheme.typography.bodyMedium)
                            Text("✅ Call dentist", style = MaterialTheme.typography.bodyMedium)
                            Text("✅ Team standup", style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    BriefingSection("Moved to Tomorrow") {
                        Text("📋 Research vacation destinations", style = MaterialTheme.typography.bodyMedium)
                    }
                ),
                ctaLabel = "Close My Day"
            ),
            onCtaTap = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BriefingCardMinimalPreview() {
    PrioTheme {
        BriefingCard(
            briefing = BriefingCardData(
                type = BriefingType.MORNING,
                date = "Tuesday, February 4",
                summaryText = "Your schedule is clear today. Perfect for deep work!",
                isPrivate = true
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

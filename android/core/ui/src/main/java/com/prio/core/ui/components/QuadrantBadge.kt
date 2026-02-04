package com.prio.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prio.core.ui.theme.PrioTheme
import com.prio.core.ui.theme.QuadrantColors

/**
 * Eisenhower Matrix quadrant.
 * 
 * Based on 1.1.13 Component Specifications.
 */
enum class Quadrant(
    val label: String,
    val shortLabel: String,
    val emoji: String,
    val description: String,
    val color: Color,
    val backgroundColor: Color,
    val backgroundColorDark: Color
) {
    DO_FIRST(
        label = "DO FIRST",
        shortLabel = "Do",
        emoji = "🔴",
        description = "Urgent + Important",
        color = QuadrantColors.doFirst,
        backgroundColor = QuadrantColors.doFirstBg,
        backgroundColorDark = QuadrantColors.doFirstBgDark
    ),
    SCHEDULE(
        label = "SCHEDULE",
        shortLabel = "Schedule",
        emoji = "🟡",
        description = "Important, Not Urgent",
        color = QuadrantColors.schedule,
        backgroundColor = QuadrantColors.scheduleBg,
        backgroundColorDark = QuadrantColors.scheduleBgDark
    ),
    DELEGATE(
        label = "DELEGATE",
        shortLabel = "Delegate",
        emoji = "🟠",
        description = "Urgent, Not Important",
        color = QuadrantColors.delegate,
        backgroundColor = QuadrantColors.delegateBg,
        backgroundColorDark = QuadrantColors.delegateBgDark
    ),
    ELIMINATE(
        label = "MAYBE LATER",
        shortLabel = "Later",
        emoji = "⚪",
        description = "Neither Urgent nor Important",
        color = QuadrantColors.eliminate,
        backgroundColor = QuadrantColors.eliminateBg,
        backgroundColorDark = QuadrantColors.eliminateBgDark
    )
}

/**
 * Badge size variants per 1.1.13 spec.
 */
enum class BadgeSize {
    /** 24dp × 24dp, emoji only - for compact task cards */
    COMPACT,
    /** Height: 28dp, with label - for lists */
    STANDARD,
    /** Height: 48dp, with description - for selectors */
    LARGE
}

/**
 * QuadrantBadge component per 1.1.13 Component Specifications.
 * 
 * Displays Eisenhower Matrix quadrant indicator in three size variants:
 * - Compact (24dp): emoji only, for task cards
 * - Standard (28dp): emoji + label, for lists  
 * - Large (48dp): emoji + label + description, for selectors
 * 
 * @param quadrant The Eisenhower quadrant to display
 * @param size Badge size variant
 * @param onTap Optional click handler (makes badge tappable)
 * @param modifier Modifier for customization
 */
@Composable
fun QuadrantBadge(
    quadrant: Quadrant,
    modifier: Modifier = Modifier,
    size: BadgeSize = BadgeSize.COMPACT,
    onTap: (() -> Unit)? = null
) {
    val contentDesc = buildString {
        append(quadrant.label)
        if (onTap != null) {
            append(". Double tap to change priority.")
        }
    }
    
    val baseModifier = modifier
        .clip(RoundedCornerShape(if (size == BadgeSize.COMPACT) 6.dp else 8.dp))
        .background(quadrant.backgroundColor)
        .semantics {
            contentDescription = contentDesc
            if (onTap != null) {
                role = Role.Button
            }
        }
        .then(
            if (onTap != null) {
                Modifier.clickable(onClick = onTap)
            } else {
                Modifier
            }
        )
    
    when (size) {
        BadgeSize.COMPACT -> CompactBadge(quadrant, baseModifier)
        BadgeSize.STANDARD -> StandardBadge(quadrant, baseModifier)
        BadgeSize.LARGE -> LargeBadge(quadrant, baseModifier)
    }
}

@Composable
private fun CompactBadge(
    quadrant: Quadrant,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = quadrant.emoji,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun StandardBadge(
    quadrant: Quadrant,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .widthIn(min = 80.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = quadrant.emoji,
            fontSize = 12.sp
        )
        Text(
            text = quadrant.label,
            style = MaterialTheme.typography.labelMedium,
            color = quadrant.color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LargeBadge(
    quadrant: Quadrant,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .widthIn(min = 160.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = quadrant.emoji,
            fontSize = 20.sp
        )
        Column {
            Text(
                text = quadrant.label,
                style = MaterialTheme.typography.labelLarge,
                color = quadrant.color,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = quadrant.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============ Previews ============

@Preview(showBackground = true)
@Composable
private fun QuadrantBadgeCompactPreview() {
    PrioTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Quadrant.entries.forEach { quadrant ->
                QuadrantBadge(
                    quadrant = quadrant,
                    size = BadgeSize.COMPACT
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuadrantBadgeStandardPreview() {
    PrioTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Quadrant.entries.forEach { quadrant ->
                QuadrantBadge(
                    quadrant = quadrant,
                    size = BadgeSize.STANDARD
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuadrantBadgeLargePreview() {
    PrioTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Quadrant.entries.forEach { quadrant ->
                QuadrantBadge(
                    quadrant = quadrant,
                    size = BadgeSize.LARGE,
                    onTap = {}
                )
            }
        }
    }
}

package com.prio.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prio.core.ui.theme.PrioTheme
import com.prio.core.ui.theme.SemanticColors

/**
 * Navigation item data.
 */
data class PrioNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

/**
 * Default navigation items per 1.1.12 Wireframes navigation map.
 */
val defaultNavItems = listOf(
    PrioNavItem(
        route = "today",
        label = "Today",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    PrioNavItem(
        route = "tasks",
        label = "Tasks",
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle
    ),
    // Center item is FAB (handled separately)
    PrioNavItem(
        route = "goals",
        label = "Goals",
        selectedIcon = Icons.Filled.Flag,
        unselectedIcon = Icons.Outlined.Flag
    ),
    PrioNavItem(
        route = "calendar",
        label = "Calendar",
        selectedIcon = Icons.Filled.CalendarToday,
        unselectedIcon = Icons.Outlined.CalendarToday
    )
)

/**
 * PrioBottomNavigation per 1.1.12 Wireframes navigation map.
 * 
 * Features:
 * - 4 nav items + center FAB
 * - Badges for overdue/urgent items
 * - Animated selection state
 * - FAB integration
 * 
 * @param items Navigation items (max 4, FAB is center)
 * @param selectedRoute Currently selected route
 * @param onItemSelected Called when nav item is selected
 * @param onFabClick Called when FAB is clicked
 * @param fabBadgeCount Optional badge on FAB
 * @param modifier Modifier for customization
 */
@Composable
fun PrioBottomNavigation(
    items: List<PrioNavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
    fabBadgeCount: Int = 0
) {
    require(items.size <= 4) { "Maximum 4 nav items supported (FAB is center)" }
    
    // Use Box with clip disabled to allow FAB to extend above the surface
    // Height = nav bar (80dp) + FAB overhang (28dp = half of 56dp FAB)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp) // 80dp nav bar + 28dp FAB overhang
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Background surface for the nav bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            // Navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // First half of items
                items.take(items.size / 2).forEach { item ->
                    NavItem(
                        item = item,
                        isSelected = selectedRoute == item.route,
                        onClick = { onItemSelected(item.route) }
                    )
                }
                
                // Space for FAB
                Spacer(modifier = Modifier.width(72.dp))
                
                // Second half of items
                items.drop(items.size / 2).forEach { item ->
                    NavItem(
                        item = item,
                        isSelected = selectedRoute == item.route,
                        onClick = { onItemSelected(item.route) }
                    )
                }
            }
        }
        
        // FAB (centered, elevated above bar) - outside Surface to avoid clipping
        FloatingActionButton(
            onClick = onFabClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (80.dp - 56.dp) / 2 - 20.dp) // Position FAB to overlap top of nav bar
                .size(56.dp)
                .semantics {
                    contentDescription = "Add new task"
                },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp
            )
        ) {
            Box {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
                
                // Badge on FAB
                if (fabBadgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(SemanticColors.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (fabBadgeCount > 9) "9+" else fabBadgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    item: PrioNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "nav_scale"
    )
    
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "nav_color"
    )
    
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp)
            .semantics {
                contentDescription = "${item.label}. ${if (isSelected) "Selected" else "Not selected"}" +
                        if (item.badgeCount > 0) ". ${item.badgeCount} items" else ""
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                modifier = Modifier
                    .size(if (isSelected) 28.dp else 24.dp)
                    .scale(animatedScale),
                tint = animatedColor
            )
            
            // Badge
            if (item.badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-2).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(SemanticColors.error),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (item.badgeCount > 9) "9+" else item.badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = animatedColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ============ Previews ============

@Preview(showBackground = true)
@Composable
private fun PrioBottomNavigationPreview() {
    var selectedRoute by remember { mutableStateOf("today") }
    
    val itemsWithBadges = defaultNavItems.mapIndexed { index, item ->
        when (index) {
            1 -> item.copy(badgeCount = 3) // Tasks has urgent items
            else -> item
        }
    }
    
    PrioTheme {
        PrioBottomNavigation(
            items = itemsWithBadges,
            selectedRoute = selectedRoute,
            onItemSelected = { selectedRoute = it },
            onFabClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrioBottomNavigationTasksSelectedPreview() {
    PrioTheme {
        PrioBottomNavigation(
            items = defaultNavItems,
            selectedRoute = "tasks",
            onItemSelected = {},
            onFabClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F2937)
@Composable
private fun PrioBottomNavigationDarkPreview() {
    var selectedRoute by remember { mutableStateOf("goals") }
    
    PrioTheme(darkTheme = true) {
        PrioBottomNavigation(
            items = defaultNavItems,
            selectedRoute = selectedRoute,
            onItemSelected = { selectedRoute = it },
            onFabClick = {}
        )
    }
}

package com.prio.app.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for Prio app.
 * 
 * Based on navigation map from 1.1.12 Wireframes Spec.
 */
sealed interface PrioRoute {
    
    // Main navigation destinations (Bottom Nav)
    @Serializable
    data object Today : PrioRoute
    
    @Serializable
    data object Tasks : PrioRoute
    
    @Serializable
    data object Goals : PrioRoute
    
    @Serializable
    data object Calendar : PrioRoute
    
    @Serializable
    data object More : PrioRoute
    
    // Task routes
    @Serializable
    data class TaskDetail(val taskId: Long) : PrioRoute
    
    @Serializable
    data object QuickCapture : PrioRoute
    
    // Goal routes
    @Serializable
    data class GoalDetail(val goalId: Long) : PrioRoute
    
    @Serializable
    data object CreateGoal : PrioRoute
    
    // Calendar routes
    @Serializable
    data class MeetingDetail(val meetingId: Long) : PrioRoute
    
    // Briefing routes
    @Serializable
    data object MorningBriefing : PrioRoute
    
    @Serializable
    data object EveningSummary : PrioRoute
    
    // Settings routes
    @Serializable
    data object Settings : PrioRoute
    
    @Serializable
    data object DailyBriefingSettings : PrioRoute
    
    @Serializable
    data object NotificationSettings : PrioRoute
    
    @Serializable
    data object AppearanceSettings : PrioRoute
    
    @Serializable
    data object AiModelSettings : PrioRoute
    
    @Serializable
    data object CalendarSettings : PrioRoute
    
    @Serializable
    data object BackupExportSettings : PrioRoute
    
    @Serializable
    data object PrivacyPolicy : PrioRoute
    
    @Serializable
    data object TermsOfService : PrioRoute
    
    @Serializable
    data object About : PrioRoute
    
    // Onboarding routes
    @Serializable
    data object Onboarding : PrioRoute
    
    // Insights routes
    @Serializable
    data object Insights : PrioRoute
}

/**
 * Bottom navigation items.
 */
enum class BottomNavItem(
    val route: PrioRoute,
    val label: String,
    val icon: String,
    val selectedIcon: String
) {
    TODAY(
        route = PrioRoute.Today,
        label = "Today",
        icon = "sun",
        selectedIcon = "sun_filled"
    ),
    TASKS(
        route = PrioRoute.Tasks,
        label = "Tasks",
        icon = "checklist",
        selectedIcon = "checklist_filled"
    ),
    GOALS(
        route = PrioRoute.Goals,
        label = "Goals",
        icon = "target",
        selectedIcon = "target_filled"
    ),
    CALENDAR(
        route = PrioRoute.Calendar,
        label = "Calendar",
        icon = "calendar",
        selectedIcon = "calendar_filled"
    ),
    MORE(
        route = PrioRoute.More,
        label = "More",
        icon = "menu",
        selectedIcon = "menu_filled"
    )
}

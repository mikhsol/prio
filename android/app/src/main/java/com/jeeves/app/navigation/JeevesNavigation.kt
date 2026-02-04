package com.jeeves.app.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for Jeeves app.
 * 
 * Based on navigation map from 1.1.12 Wireframes Spec.
 */
sealed interface JeevesRoute {
    
    // Main navigation destinations (Bottom Nav)
    @Serializable
    data object Today : JeevesRoute
    
    @Serializable
    data object Tasks : JeevesRoute
    
    @Serializable
    data object Goals : JeevesRoute
    
    @Serializable
    data object Calendar : JeevesRoute
    
    @Serializable
    data object More : JeevesRoute
    
    // Task routes
    @Serializable
    data class TaskDetail(val taskId: Long) : JeevesRoute
    
    @Serializable
    data object QuickCapture : JeevesRoute
    
    // Goal routes
    @Serializable
    data class GoalDetail(val goalId: Long) : JeevesRoute
    
    @Serializable
    data object CreateGoal : JeevesRoute
    
    // Calendar routes
    @Serializable
    data class MeetingDetail(val meetingId: Long) : JeevesRoute
    
    // Briefing routes
    @Serializable
    data object MorningBriefing : JeevesRoute
    
    @Serializable
    data object EveningSummary : JeevesRoute
    
    // Settings routes
    @Serializable
    data object Settings : JeevesRoute
    
    @Serializable
    data object DailyBriefingSettings : JeevesRoute
    
    @Serializable
    data object NotificationSettings : JeevesRoute
    
    @Serializable
    data object AppearanceSettings : JeevesRoute
    
    @Serializable
    data object AiModelSettings : JeevesRoute
    
    @Serializable
    data object CalendarSettings : JeevesRoute
    
    @Serializable
    data object BackupExportSettings : JeevesRoute
    
    @Serializable
    data object PrivacyPolicy : JeevesRoute
    
    @Serializable
    data object TermsOfService : JeevesRoute
    
    @Serializable
    data object About : JeevesRoute
    
    // Onboarding routes
    @Serializable
    data object Onboarding : JeevesRoute
    
    // Insights routes
    @Serializable
    data object Insights : JeevesRoute
}

/**
 * Bottom navigation items.
 */
enum class BottomNavItem(
    val route: JeevesRoute,
    val label: String,
    val icon: String,
    val selectedIcon: String
) {
    TODAY(
        route = JeevesRoute.Today,
        label = "Today",
        icon = "sun",
        selectedIcon = "sun_filled"
    ),
    TASKS(
        route = JeevesRoute.Tasks,
        label = "Tasks",
        icon = "checklist",
        selectedIcon = "checklist_filled"
    ),
    GOALS(
        route = JeevesRoute.Goals,
        label = "Goals",
        icon = "target",
        selectedIcon = "target_filled"
    ),
    CALENDAR(
        route = JeevesRoute.Calendar,
        label = "Calendar",
        icon = "calendar",
        selectedIcon = "calendar_filled"
    ),
    MORE(
        route = JeevesRoute.More,
        label = "More",
        icon = "menu",
        selectedIcon = "menu_filled"
    )
}

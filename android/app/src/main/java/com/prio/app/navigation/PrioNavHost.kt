package com.prio.app.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.prio.app.feature.calendar.CalendarScreen
import com.prio.app.feature.briefing.EveningSummaryScreen
import com.prio.app.feature.briefing.MorningBriefingScreen
import com.prio.app.feature.goals.GoalsListScreen
import com.prio.app.feature.goals.create.CreateGoalScreen
import com.prio.app.feature.goals.detail.GoalDetailScreen
import com.prio.app.feature.insights.InsightsScreen
import com.prio.app.feature.meeting.MeetingDetailScreen
import com.prio.app.feature.more.MoreScreen
import com.prio.app.feature.tasks.TaskListScreen
import com.prio.app.feature.tasks.detail.TaskDetailScreen
import com.prio.app.feature.today.TodayScreen

/**
 * Navigation route constants.
 * 
 * String-based routes for navigation-compose 2.7.x compatibility.
 */
object NavRoutes {
    // Main bottom nav destinations
    const val TODAY = "today"
    const val TASKS = "tasks"
    const val GOALS = "goals"
    const val CALENDAR = "calendar"
    const val MORE = "more"
    
    // Detail destinations with arguments
    const val TASK_DETAIL = "task/{taskId}"
    const val GOAL_DETAIL = "goal/{goalId}"
    const val MEETING_DETAIL = "meeting/{meetingId}"
    
    // Settings destinations
    const val SETTINGS = "settings"
    const val INSIGHTS = "insights"
    const val PRIVACY_POLICY = "privacy_policy"
    const val ABOUT = "about"
    
    // Briefing routes
    const val MORNING_BRIEFING = "morning_briefing"
    const val EVENING_SUMMARY = "evening_summary"
    const val CREATE_GOAL = "create_goal"
    
    // Helper functions for navigation with arguments
    fun taskDetail(taskId: Long) = "task/$taskId"
    fun goalDetail(goalId: Long) = "goal/$goalId"
    fun meetingDetail(meetingId: Long) = "meeting/$meetingId"
}

/**
 * Main navigation host for Prio app.
 * 
 * Implements navigation graph per 1.1.12 Wireframes Navigation Map.
 * 
 * Architecture:
 * ```
 * PrioNavHost
 * ├── composable(Today)      - Dashboard / Today view
 * ├── composable(Tasks)      - Task list with Eisenhower grouping
 * ├── composable(Goals)      - Goals list with progress
 * ├── composable(Calendar)   - Calendar day/week view
 * ├── composable(More)       - Settings and additional options
 * └── Nested routes:
 *     ├── TaskDetail(taskId)
 *     ├── GoalDetail(goalId)
 *     ├── MeetingDetail(meetingId)
 *     └── Settings sub-screens
 * ```
 * 
 * @param navController Navigation controller
 * @param contentPadding Padding from scaffold (for bottom nav)
 * @param onShowQuickCapture Callback to show quick capture sheet
 * @param modifier Modifier for customization
 */
@Composable
fun PrioNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    onShowQuickCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.TODAY,
        modifier = modifier.padding(contentPadding),
        enterTransition = { defaultEnterTransition() },
        exitTransition = { defaultExitTransition() },
        popEnterTransition = { defaultPopEnterTransition() },
        popExitTransition = { defaultPopExitTransition() }
    ) {
        // =====================================================
        // Main Bottom Navigation Destinations
        // =====================================================
        
        // Today / Dashboard - Entry point per CB-001
        composable(
            route = NavRoutes.TODAY,
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() }
        ) {
            TodayScreen(
                onNavigateToTask = { taskId ->
                    navController.navigate(NavRoutes.taskDetail(taskId))
                },
                onNavigateToGoal = { goalId ->
                    navController.navigate(NavRoutes.goalDetail(goalId))
                },
                onNavigateToMeeting = { meetingId ->
                    navController.navigate(NavRoutes.meetingDetail(meetingId))
                },
                onNavigateToTasks = {
                    navController.navigate(NavRoutes.TASKS)
                },
                onNavigateToMorningBriefing = {
                    navController.navigate(NavRoutes.MORNING_BRIEFING)
                },
                onNavigateToEveningSummary = {
                    navController.navigate(NavRoutes.EVENING_SUMMARY)
                }
            )
        }
        
        // Tasks - Task list per TM-001, TM-006
        composable(
            route = NavRoutes.TASKS,
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() }
        ) {
            TaskListScreen(
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(NavRoutes.taskDetail(taskId))
                },
                onNavigateToAddTask = onShowQuickCapture
            )
        }
        
        // Goals - Goals list per GL-005
        composable(
            route = NavRoutes.GOALS,
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() }
        ) {
            GoalsListScreen(
                onNavigateToGoalDetail = { goalId ->
                    navController.navigate(NavRoutes.goalDetail(goalId))
                },
                onNavigateToCreateGoal = {
                    navController.navigate(NavRoutes.CREATE_GOAL)
                }
            )
        }
        
        // Calendar - Calendar view per calendar user stories
        composable(
            route = NavRoutes.CALENDAR,
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() }
        ) {
            CalendarScreen(
                onNavigateToMeeting = { meetingId ->
                    navController.navigate(NavRoutes.meetingDetail(meetingId))
                },
                onNavigateToTask = { taskId ->
                    navController.navigate(NavRoutes.taskDetail(taskId))
                }
            )
        }
        
        // More - Settings and additional options
        composable(
            route = NavRoutes.MORE,
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() }
        ) {
            MoreScreen(
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                },
                onNavigateToInsights = {
                    navController.navigate(NavRoutes.INSIGHTS)
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate(NavRoutes.PRIVACY_POLICY)
                },
                onNavigateToAbout = {
                    navController.navigate(NavRoutes.ABOUT)
                }
            )
        }
        
        // =====================================================
        // Nested Destinations - Task Details
        // =====================================================
        
        composable(
            route = NavRoutes.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId")
                ?: error("taskId argument is required for TaskDetail route")
            TaskDetailScreen(
                taskId = taskId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGoal = { goalId ->
                    navController.navigate(NavRoutes.goalDetail(goalId))
                }
            )
        }
        
        // =====================================================
        // Nested Destinations - Goal Details
        // =====================================================
        
        composable(
            route = NavRoutes.GOAL_DETAIL,
            arguments = listOf(navArgument("goalId") { type = NavType.LongType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getLong("goalId")
                ?: error("goalId argument is required for GoalDetail route")
            GoalDetailScreen(
                goalId = goalId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTask = { taskId ->
                    navController.navigate(NavRoutes.taskDetail(taskId))
                },
                onShowQuickCapture = onShowQuickCapture
            )
        }
        
        composable(route = NavRoutes.CREATE_GOAL) {
            CreateGoalScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGoalDetail = { goalId ->
                    navController.popBackStack()
                    navController.navigate(NavRoutes.goalDetail(goalId))
                },
                onNavigateToGoalsList = {
                    navController.popBackStack(route = NavRoutes.GOALS, inclusive = false)
                }
            )
        }
        
        // =====================================================
        // Nested Destinations - Meeting Details
        // =====================================================
        
        composable(
            route = NavRoutes.MEETING_DETAIL,
            arguments = listOf(navArgument("meetingId") { type = NavType.LongType })
        ) {
            MeetingDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTask = { taskId ->
                    navController.navigate(NavRoutes.taskDetail(taskId))
                }
            )
        }
        
        // =====================================================
        // Settings & More Destinations
        // =====================================================
        
        composable(route = NavRoutes.SETTINGS) {
            PlaceholderDetailScreen(
                title = "Settings",
                subtitle = "Configure Prio",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(route = NavRoutes.INSIGHTS) {
            InsightsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGoal = { goalId ->
                    navController.navigate(NavRoutes.goalDetail(goalId))
                }
            )
        }
        
        composable(route = NavRoutes.PRIVACY_POLICY) {
            PlaceholderDetailScreen(
                title = "Privacy Policy",
                subtitle = "Your data stays on device",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(route = NavRoutes.ABOUT) {
            PlaceholderDetailScreen(
                title = "About Prio",
                subtitle = "Version ${com.prio.app.BuildConfig.VERSION_NAME}",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Briefing routes
        composable(route = NavRoutes.MORNING_BRIEFING) {
            MorningBriefingScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTask = { taskId ->
                    navController.navigate(NavRoutes.taskDetail(taskId))
                },
                onNavigateToGoal = { goalId ->
                    navController.navigate(NavRoutes.goalDetail(goalId))
                },
                onNavigateToTasks = {
                    navController.navigate(NavRoutes.TASKS)
                },
                onNavigateToCalendar = {
                    navController.navigate(NavRoutes.CALENDAR)
                }
            )
        }
        
        composable(route = NavRoutes.EVENING_SUMMARY) {
            EveningSummaryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTask = { taskId ->
                    navController.navigate(NavRoutes.taskDetail(taskId))
                },
                onNavigateToGoal = { goalId ->
                    navController.navigate(NavRoutes.goalDetail(goalId))
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                }
            )
        }
    }
}

// =====================================================
// Animation Transitions
// =====================================================

/**
 * Default enter transition for nested screens.
 * Slides in from right with fade.
 */
private fun defaultEnterTransition(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(durationMillis = 300)
    ) + fadeIn(animationSpec = tween(durationMillis = 300))
}

/**
 * Default exit transition for nested screens.
 * Slides out to left with fade.
 */
private fun defaultExitTransition(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 4 },
        animationSpec = tween(durationMillis = 300)
    ) + fadeOut(animationSpec = tween(durationMillis = 300))
}

/**
 * Pop enter transition (back navigation).
 * Slides in from left.
 */
private fun defaultPopEnterTransition(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth / 4 },
        animationSpec = tween(durationMillis = 300)
    ) + fadeIn(animationSpec = tween(durationMillis = 300))
}

/**
 * Pop exit transition (back navigation).
 * Slides out to right.
 */
private fun defaultPopExitTransition(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(durationMillis = 300)
    ) + fadeOut(animationSpec = tween(durationMillis = 300))
}

/**
 * Tab switch enter transition.
 * Simple fade for bottom nav tabs.
 */
private fun tabEnterTransition(): EnterTransition {
    return fadeIn(animationSpec = tween(durationMillis = 200))
}

/**
 * Tab switch exit transition.
 * Simple fade for bottom nav tabs.
 */
private fun tabExitTransition(): ExitTransition {
    return fadeOut(animationSpec = tween(durationMillis = 200))
}

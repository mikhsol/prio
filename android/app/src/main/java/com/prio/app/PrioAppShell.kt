package com.prio.app

import android.Manifest
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.prio.app.feature.capture.QuickCaptureEffect
import com.prio.app.feature.capture.QuickCaptureSheet
import com.prio.app.feature.capture.QuickCaptureViewModel
import com.prio.app.feature.capture.voice.VoiceInputManager
import com.prio.app.feature.capture.voice.VoiceInputState
import com.prio.app.navigation.NavRoutes
import com.prio.app.navigation.PrioNavHost
import com.prio.core.ui.components.PrioBottomNavigation
import com.prio.core.ui.components.defaultNavItems
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main app shell that provides the scaffold with bottom navigation.
 * 
 * Architecture per 3.1.12 Navigation Integration Analysis:
 * ```
 * PrioAppShell
 * ├── Scaffold
 * │   ├── content: PrioNavHost
 * │   └── bottomBar: PrioBottomNavigation
 * └── QuickCaptureSheet (modal overlay)
 * ```
 * 
 * This composable:
 * - Manages the NavController
 * - Handles bottom nav visibility (hidden on detail screens)
 * - Manages QuickCapture modal state
 * - Provides consistent scaffold structure across all screens
 * 
 * @param onFirstLaunchComplete Callback when onboarding completes
 * @param showOnboarding Whether to show onboarding flow
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PrioAppShell(
    modifier: Modifier = Modifier,
    showOnboarding: Boolean = false,
    deepLinkRoute: String? = null,
    onFirstLaunchComplete: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle deep link navigation on first composition
    LaunchedEffect(deepLinkRoute) {
        deepLinkRoute?.let { route ->
            Timber.d("Navigating to deep link route: $route")
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }
    
    // Track quick capture state
    var showQuickCapture by rememberSaveable { mutableStateOf(false) }
    // Track which goal to pre-link when opening QuickCapture from a goal detail screen
    var pendingGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    
    // RECORD_AUDIO runtime permission (required for voice input)
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    // VoiceInputManager lifecycle (created when QuickCapture is visible)
    var voiceInputManager by remember { mutableStateOf<VoiceInputManager?>(null) }
    
    // Determine if bottom nav should be visible
    // Hide on detail screens and onboarding
    val showBottomNav = remember(currentRoute) {
        isMainDestination(currentRoute) && currentRoute != NavRoutes.ONBOARDING
    }
    
    // Map current route to nav item route string for selection
    val selectedNavRoute = remember(currentRoute) {
        getNavItemRoute(currentRoute)
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomNav) {
                PrioBottomNavigation(
                    items = defaultNavItems,
                    selectedRoute = selectedNavRoute,
                    onItemSelected = { route ->
                        navigateToTab(navController, route)
                    },
                    onFabClick = {
                        pendingGoalId = null
                        showQuickCapture = true
                    }
                )
            }
        }
    ) { contentPadding ->
        PrioNavHost(
            navController = navController,
            contentPadding = contentPadding,
            onShowQuickCapture = { goalId ->
                pendingGoalId = goalId
                showQuickCapture = true
            },
            showOnboarding = showOnboarding,
            onOnboardingComplete = onFirstLaunchComplete
        )
    }
    
    // Counter incremented each time QuickCapture is opened, used as a key
    // for LaunchedEffect to guarantee the Reset event fires on every open.
    var quickCaptureOpenCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(showQuickCapture) {
        if (showQuickCapture) quickCaptureOpenCount++
    }

    // Quick Capture modal overlay - accessible from any screen
    // Per TM-001: "FAB visible on all main screens"
    if (showQuickCapture) {
        val viewModel: QuickCaptureViewModel = hiltViewModel()

        // Reset ViewModel state synchronously every time the sheet opens.
        // hiltViewModel() returns the same activity-scoped instance, so stale
        // data from a previous capture (title, priority, etc.) would show if
        // we don't clear it on each open.  The counter key guarantees the
        // reset runs on every open, not just the first one.
        LaunchedEffect(quickCaptureOpenCount) {
            viewModel.onEvent(com.prio.app.feature.capture.QuickCaptureEvent.Reset)
            pendingGoalId?.let { goalId ->
                viewModel.preselectGoal(goalId)
            }
        }

        val state by viewModel.uiState.collectAsStateWithLifecycle()
        
        // Create VoiceInputManager when QuickCapture is visible
        DisposableEffect(Unit) {
            voiceInputManager = VoiceInputManager(context)
            onDispose {
                voiceInputManager?.destroy()
                voiceInputManager = null
            }
        }
        
        // Consume ViewModel effects (StartVoiceRecognition, snackbars, etc.)
        LaunchedEffect(viewModel) {
            viewModel.effect.collectLatest { effect ->
                when (effect) {
                    is QuickCaptureEffect.StartVoiceRecognition -> {
                        // Check permission before starting
                        if (audioPermissionState.status.isGranted) {
                            startVoiceRecognition(voiceInputManager, viewModel)
                        } else if (audioPermissionState.status.shouldShowRationale) {
                            // User previously denied — show rationale then request again
                            Timber.d("RECORD_AUDIO denied before, showing rationale and re-requesting")
                            audioPermissionState.launchPermissionRequest()
                        } else {
                            // First time asking OR permanently denied.
                            // On Android, shouldShowRationale is false in both cases.
                            // We attempt the system permission dialog first.
                            // If user previously selected "Don't ask again", the
                            // system dialog will be a no-op and the LaunchedEffect
                            // below (which watches isGranted) won't fire — the
                            // voice stays on Initializing until the 5s timeout
                            // triggers an error with PERMISSION_DENIED, which
                            // shows the error overlay with "Type Instead".
                            Timber.d("RECORD_AUDIO not granted, launching permission request")
                            audioPermissionState.launchPermissionRequest()
                        }
                    }
                    is QuickCaptureEffect.TaskCreated -> {
                        // Task created — snackbar shown via ShowSnackbar effect
                    }
                    is QuickCaptureEffect.ShowSnackbar -> {
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = effect.message,
                                actionLabel = effect.actionLabel,
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                // Navigate to the created task detail
                                effect.taskId?.let { id ->
                                    navController.navigate(NavRoutes.taskDetail(id))
                                }
                            }
                        }
                    }
                    is QuickCaptureEffect.ShowError -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(effect.message)
                        }
                    }
                    is QuickCaptureEffect.OpenTaskDetail -> {
                        showQuickCapture = false
                        navController.navigate(NavRoutes.taskDetail(effect.taskId))
                    }
                    is QuickCaptureEffect.Dismiss -> {
                        showQuickCapture = false
                    }
                }
            }
        }
        
        // Handle permission result: if just granted after request, start voice.
        // If denied permanently (shouldShowRationale = false after denial),
        // show a snackbar directing user to app Settings.
        LaunchedEffect(audioPermissionState.status.isGranted, audioPermissionState.status.shouldShowRationale) {
            if (audioPermissionState.status.isGranted && state.isVoiceInputActive) {
                startVoiceRecognition(voiceInputManager, viewModel)
            } else if (!audioPermissionState.status.isGranted
                && !audioPermissionState.status.shouldShowRationale
                && state.isVoiceInputActive
            ) {
                // Permanently denied — guide user to Settings
                Timber.d("RECORD_AUDIO permanently denied, directing to settings")
                viewModel.updateVoiceState(
                    VoiceInputState.Error(
                        errorType = com.prio.app.feature.capture.voice.VoiceErrorType.PERMISSION_DENIED,
                        message = "Microphone permission denied. Enable in Settings."
                    )
                )
            }
        }
        
        QuickCaptureSheet(
            state = state,
            onEvent = viewModel::onEvent,
            onDismiss = { 
                voiceInputManager?.cancel()
                viewModel.onEvent(com.prio.app.feature.capture.QuickCaptureEvent.Reset)
                pendingGoalId = null
                showQuickCapture = false 
            },
            onTaskCreated = { taskId ->
                viewModel.onEvent(com.prio.app.feature.capture.QuickCaptureEvent.Reset)
                pendingGoalId = null
                showQuickCapture = false
            }
        )
    }
}

/**
 * Navigate to a bottom navigation tab.
 * Uses single top and clears back stack to tab root.
 */
private fun navigateToTab(
    navController: androidx.navigation.NavController,
    route: String
) {
    navController.navigate(route) {
        // Pop up to the start destination of the graph to avoid
        // building up a large stack of destinations when selecting items
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        // Avoid multiple copies of the same destination
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = true
    }
}

/**
 * Check if current route is a main bottom nav destination.
 * Used to determine bottom nav visibility.
 */
private fun isMainDestination(route: String?): Boolean {
    if (route == null) return true
    
    // Main destinations where bottom nav should be visible
    val mainRoutes = setOf(
        NavRoutes.TODAY,
        NavRoutes.TASKS,
        NavRoutes.GOALS,
        NavRoutes.CALENDAR,
        NavRoutes.MORE
    )
    
    return route in mainRoutes
}

/**
 * Map current route to nav item route string.
 */
private fun getNavItemRoute(route: String?): String {
    return when (route) {
        NavRoutes.TODAY -> NavRoutes.TODAY
        NavRoutes.TASKS -> NavRoutes.TASKS
        NavRoutes.GOALS -> NavRoutes.GOALS
        NavRoutes.CALENDAR -> NavRoutes.CALENDAR
        NavRoutes.MORE -> NavRoutes.MORE
        else -> NavRoutes.TODAY
    }
}

/**
 * Start voice recognition via VoiceInputManager and pipe states to ViewModel.
 * 
 * Collects [VoiceInputState] emissions from the manager's flow and forwards
 * them to the ViewModel for UI state updates. The ViewModel handles terminal
 * states (Result → onVoiceResult, Error → show error overlay).
 *
 * Per 3.1.5.B.3: Android SpeechRecognizer, on-device processing, real-time transcription.
 */
private suspend fun startVoiceRecognition(
    voiceInputManager: VoiceInputManager?,
    viewModel: QuickCaptureViewModel
) {
    val manager = voiceInputManager ?: run {
        Timber.e("VoiceInputManager is null, cannot start recognition")
        viewModel.updateVoiceState(
            VoiceInputState.Error(
                errorType = com.prio.app.feature.capture.voice.VoiceErrorType.NOT_AVAILABLE,
                message = "Voice input not available"
            )
        )
        return
    }

    if (!manager.isAvailable()) {
        Timber.e("SpeechRecognizer not available on this device")
        viewModel.updateVoiceState(
            VoiceInputState.Error(
                errorType = com.prio.app.feature.capture.voice.VoiceErrorType.NOT_AVAILABLE,
                message = "Speech recognition is not available on this device"
            )
        )
        return
    }

    Timber.d("Starting voice recognition (on-device preferred: ${manager.isOnDeviceAvailable()})")
    
    manager.startListening().collect { state ->
        viewModel.updateVoiceState(state)
    }
}

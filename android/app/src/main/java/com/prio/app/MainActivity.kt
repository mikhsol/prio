package com.prio.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.prio.core.data.preferences.UserPreferencesRepository
import com.prio.core.ui.theme.PrioTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Main activity for Prio app.
 * 
 * Entry point that sets up:
 * - Splash screen
 * - Edge-to-edge display
 * - Compose UI with Material 3 theme
 * - Navigation via PrioAppShell
 * - Deep link handling for notifications (GAP-H06)
 * - First-launch onboarding check (GAP-H07)
 * 
 * Architecture per 3.1.12 Navigation Integration Analysis:
 * ```
 * MainActivity
 * └── PrioAppShell
 *     ├── Scaffold
 *     │   ├── PrioNavHost (content)
 *     │   └── PrioBottomNavigation (bottom bar)
 *     └── QuickCaptureSheet (modal overlay)
 * ```
 * 
 * @see PrioAppShell Main app shell with navigation
 * @see com.prio.app.navigation.PrioNavHost Navigation host with all routes
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        // Determine onboarding state and deep link from intent
        lifecycleScope.launch {
            val hasCompletedOnboarding = try {
                userPreferencesRepository.hasCompletedOnboarding.first()
            } catch (e: Exception) {
                Timber.w(e, "Failed to read onboarding preference, defaulting to false")
                false
            }
            val deepLinkRoute = parseDeepLink(intent)

            setContent {
                PrioTheme {
                    PrioAppShell(
                        showOnboarding = !hasCompletedOnboarding,
                        deepLinkRoute = deepLinkRoute,
                        onFirstLaunchComplete = {
                            lifecycleScope.launch {
                                userPreferencesRepository.setOnboardingComplete()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep links from notifications when activity is already running
        val route = parseDeepLink(intent)
        if (route != null) {
            Timber.d("Deep link received via onNewIntent: $route")
            // The route will be consumed by PrioAppShell via the intent extras
        }
    }

    /**
     * Parse deep link route from notification intent.
     * 
     * Notifications include extras:
     * - "route" → NavRoutes path string (e.g., "task/123", "morning_briefing")
     */
    private fun parseDeepLink(intent: Intent?): String? {
        return intent?.getStringExtra("route")?.also {
            Timber.d("Parsed deep link route: $it")
        }
    }
}

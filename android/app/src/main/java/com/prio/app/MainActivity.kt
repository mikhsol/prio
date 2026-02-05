package com.prio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.prio.core.ui.theme.PrioTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for Prio app.
 * 
 * Entry point that sets up:
 * - Splash screen
 * - Edge-to-edge display
 * - Compose UI with Material 3 theme
 * - Navigation via PrioAppShell
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            PrioTheme {
                // Main app shell with navigation
                // Per Milestone 3.1.5: Wire navigation components together
                PrioAppShell(
                    showOnboarding = false, // TODO: Check first launch in Milestone 4.1
                    onFirstLaunchComplete = {
                        // TODO: Save onboarding complete flag in Milestone 4.1
                    }
                )
            }
        }
    }
}

package com.prio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.prio.core.ui.components.ComponentShowcase
import com.prio.core.ui.theme.PrioTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for Prio app.
 * 
 * Entry point that sets up:
 * - Splash screen
 * - Edge-to-edge display
 * - Compose UI with Material 3 theme
 * - Navigation
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
                // TODO: Remove showShowcase flag after development - for component preview only
                var showShowcase by rememberSaveable { mutableStateOf(true) }
                
                if (showShowcase) {
                    // Component showcase for design review
                    ComponentShowcase(
                        onBackClick = { showShowcase = false }
                    )
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        // Placeholder content - will be replaced with navigation
                        WelcomeScreen(
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier) {
    Text(
        text = "Welcome to Prio\nYour Private Productivity AI",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    PrioTheme {
        WelcomeScreen()
    }
}

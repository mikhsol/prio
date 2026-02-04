package com.jeeves.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Jeeves color tokens.
 * 
 * Based on 1.1.13 Component Specifications.
 */

// Quadrant Colors (Eisenhower Matrix)
object QuadrantColors {
    // Foreground (badges, text)
    val doFirst = Color(0xFFDC2626)      // Red-600
    val schedule = Color(0xFFF59E0B)     // Amber-500
    val delegate = Color(0xFFF97316)     // Orange-500
    val eliminate = Color(0xFF6B7280)    // Gray-500
    
    // Backgrounds (cards, sections)
    val doFirstBg = Color(0xFFFEF2F2)    // Red-50
    val scheduleBg = Color(0xFFFFFBEB)   // Amber-50
    val delegateBg = Color(0xFFFFF7ED)   // Orange-50
    val eliminateBg = Color(0xFFF9FAFB)  // Gray-50
    
    // Dark mode backgrounds
    val doFirstBgDark = Color(0xFF450A0A)    // Red-950
    val scheduleBgDark = Color(0xFF451A03)   // Amber-950
    val delegateBgDark = Color(0xFF431407)   // Orange-950
    val eliminateBgDark = Color(0xFF111827)  // Gray-900
}

// Semantic Colors
object SemanticColors {
    val success = Color(0xFF10B981)      // Green-500
    val warning = Color(0xFFF59E0B)      // Amber-500
    val error = Color(0xFFEF4444)        // Red-500
    val info = Color(0xFF3B82F6)         // Blue-500
    
    // Goal status colors (from 1.1.4)
    val onTrack = Color(0xFF10B981)      // Green-500
    val behind = Color(0xFFF59E0B)       // Amber-500
    val atRisk = Color(0xFFEF4444)       // Red-500
}

// Brand Colors
object BrandColors {
    val primary = Color(0xFF0D9488)      // Teal-600
    val primaryVariant = Color(0xFF14B8A6) // Teal-500
    val primaryDark = Color(0xFF0F766E)  // Teal-700
    
    val secondary = Color(0xFF6366F1)    // Indigo-500
    val secondaryVariant = Color(0xFF4F46E5) // Indigo-600
}

// Light Theme Colors
val md_theme_light_primary = Color(0xFF0D9488)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFF99F6E4)
val md_theme_light_onPrimaryContainer = Color(0xFF134E4A)
val md_theme_light_secondary = Color(0xFF6366F1)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFE0E7FF)
val md_theme_light_onSecondaryContainer = Color(0xFF312E81)
val md_theme_light_tertiary = Color(0xFF8B5CF6)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFEDE9FE)
val md_theme_light_onTertiaryContainer = Color(0xFF4C1D95)
val md_theme_light_error = Color(0xFFEF4444)
val md_theme_light_errorContainer = Color(0xFFFEE2E2)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF7F1D1D)
val md_theme_light_background = Color(0xFFF9FAFB)
val md_theme_light_onBackground = Color(0xFF111827)
val md_theme_light_surface = Color(0xFFFFFFFF)
val md_theme_light_onSurface = Color(0xFF111827)
val md_theme_light_surfaceVariant = Color(0xFFF3F4F6)
val md_theme_light_onSurfaceVariant = Color(0xFF4B5563)
val md_theme_light_outline = Color(0xFF9CA3AF)
val md_theme_light_outlineVariant = Color(0xFFE5E7EB)
val md_theme_light_inverseOnSurface = Color(0xFFF9FAFB)
val md_theme_light_inverseSurface = Color(0xFF1F2937)
val md_theme_light_inversePrimary = Color(0xFF5EEAD4)
val md_theme_light_surfaceTint = Color(0xFF0D9488)
val md_theme_light_scrim = Color(0xFF000000)

// Dark Theme Colors
val md_theme_dark_primary = Color(0xFF5EEAD4)
val md_theme_dark_onPrimary = Color(0xFF134E4A)
val md_theme_dark_primaryContainer = Color(0xFF0F766E)
val md_theme_dark_onPrimaryContainer = Color(0xFFCCFBF1)
val md_theme_dark_secondary = Color(0xFFA5B4FC)
val md_theme_dark_onSecondary = Color(0xFF312E81)
val md_theme_dark_secondaryContainer = Color(0xFF4338CA)
val md_theme_dark_onSecondaryContainer = Color(0xFFE0E7FF)
val md_theme_dark_tertiary = Color(0xFFC4B5FD)
val md_theme_dark_onTertiary = Color(0xFF4C1D95)
val md_theme_dark_tertiaryContainer = Color(0xFF6D28D9)
val md_theme_dark_onTertiaryContainer = Color(0xFFEDE9FE)
val md_theme_dark_error = Color(0xFFFCA5A5)
val md_theme_dark_errorContainer = Color(0xFF991B1B)
val md_theme_dark_onError = Color(0xFF7F1D1D)
val md_theme_dark_onErrorContainer = Color(0xFFFEE2E2)
val md_theme_dark_background = Color(0xFF111827)
val md_theme_dark_onBackground = Color(0xFFF9FAFB)
val md_theme_dark_surface = Color(0xFF1F2937)
val md_theme_dark_onSurface = Color(0xFFF9FAFB)
val md_theme_dark_surfaceVariant = Color(0xFF374151)
val md_theme_dark_onSurfaceVariant = Color(0xFFD1D5DB)
val md_theme_dark_outline = Color(0xFF6B7280)
val md_theme_dark_outlineVariant = Color(0xFF374151)
val md_theme_dark_inverseOnSurface = Color(0xFF111827)
val md_theme_dark_inverseSurface = Color(0xFFF9FAFB)
val md_theme_dark_inversePrimary = Color(0xFF0D9488)
val md_theme_dark_surfaceTint = Color(0xFF5EEAD4)
val md_theme_dark_scrim = Color(0xFF000000)

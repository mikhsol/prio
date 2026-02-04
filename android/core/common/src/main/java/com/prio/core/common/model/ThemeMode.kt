package com.prio.core.common.model

/**
 * Theme mode setting for the application.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;
    
    companion object {
        fun fromString(value: String): ThemeMode {
            return when (value.lowercase()) {
                "light" -> LIGHT
                "dark" -> DARK
                else -> SYSTEM
            }
        }
    }
    
    override fun toString(): String = name.lowercase()
}

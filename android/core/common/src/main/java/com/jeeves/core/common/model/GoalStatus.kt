package com.jeeves.core.common.model

import kotlinx.serialization.Serializable

/**
 * Goal status based on progress relative to time elapsed.
 * Per GL-002 from 0.3.3_goals_user_stories.md and 1.1.4 Goals Screens Spec.
 * 
 * Status colors:
 * - ON_TRACK: Green #10B981
 * - BEHIND: Yellow #F59E0B (within 15%)
 * - AT_RISK: Red #EF4444 (behind by 15%+)
 */
@Serializable
enum class GoalStatus(
    val displayName: String,
    val colorHex: Long
) {
    ON_TRACK(
        displayName = "On Track",
        colorHex = 0xFF10B981
    ),
    BEHIND(
        displayName = "Behind",
        colorHex = 0xFFF59E0B
    ),
    AT_RISK(
        displayName = "At Risk",
        colorHex = 0xFFEF4444
    ),
    COMPLETED(
        displayName = "Completed",
        colorHex = 0xFF10B981
    )
}

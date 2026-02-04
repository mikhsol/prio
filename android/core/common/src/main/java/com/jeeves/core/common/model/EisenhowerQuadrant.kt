package com.jeeves.core.common.model

import kotlinx.serialization.Serializable

/**
 * Eisenhower Matrix quadrant for task prioritization.
 * 
 * Based on the Eisenhower Matrix methodology:
 * - Q1 (DO): Urgent + Important - Do immediately
 * - Q2 (SCHEDULE): Important, Not Urgent - Schedule for later  
 * - Q3 (DELEGATE): Urgent, Not Important - Consider delegating
 * - Q4 (ELIMINATE): Not Urgent, Not Important - Consider eliminating
 *
 * Colors per 1.1.13 Component Specifications:
 * - Q1: Red #DC2626
 * - Q2: Amber #F59E0B
 * - Q3: Orange #F97316
 * - Q4: Gray #6B7280
 */
@Serializable
enum class EisenhowerQuadrant(
    val displayName: String,
    val description: String,
    val colorHex: Long,
    val emoji: String
) {
    DO_FIRST(
        displayName = "Do First",
        description = "Urgent + Important",
        colorHex = 0xFFDC2626,
        emoji = "🔴"
    ),
    SCHEDULE(
        displayName = "Schedule",
        description = "Important, Not Urgent",
        colorHex = 0xFFF59E0B,
        emoji = "🟡"
    ),
    DELEGATE(
        displayName = "Delegate",
        description = "Urgent, Not Important",
        colorHex = 0xFFF97316,
        emoji = "🟠"
    ),
    ELIMINATE(
        displayName = "Later",
        description = "Not Urgent, Not Important",
        colorHex = 0xFF6B7280,
        emoji = "⚪"
    );

    companion object {
        /**
         * Get quadrant from urgency and importance flags.
         */
        fun fromFlags(isUrgent: Boolean, isImportant: Boolean): EisenhowerQuadrant {
            return when {
                isUrgent && isImportant -> DO_FIRST
                !isUrgent && isImportant -> SCHEDULE
                isUrgent && !isImportant -> DELEGATE
                else -> ELIMINATE
            }
        }
    }
}

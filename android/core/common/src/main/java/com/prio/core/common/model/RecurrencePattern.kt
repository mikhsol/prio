package com.prio.core.common.model

import kotlinx.serialization.Serializable

/**
 * Recurrence pattern for recurring tasks.
 * Per TM-008 from 0.3.2_task_management_user_stories.md
 */
@Serializable
enum class RecurrencePattern(
    val displayName: String
) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    WEEKDAYS("Weekdays"),
    CUSTOM("Custom")
}

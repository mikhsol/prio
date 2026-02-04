package com.prio.core.common.model

import kotlinx.serialization.Serializable

/**
 * Goal categories for organization and analytics.
 * Based on GL-001 user story from 0.3.3_goals_user_stories.md
 */
@Serializable
enum class GoalCategory(
    val displayName: String,
    val emoji: String
) {
    CAREER(
        displayName = "Career",
        emoji = "💼"
    ),
    HEALTH(
        displayName = "Health",
        emoji = "💪"
    ),
    PERSONAL(
        displayName = "Personal",
        emoji = "🏠"
    ),
    FINANCIAL(
        displayName = "Financial",
        emoji = "💰"
    ),
    LEARNING(
        displayName = "Learning",
        emoji = "📚"
    ),
    RELATIONSHIPS(
        displayName = "Relationships",
        emoji = "❤️"
    )
}

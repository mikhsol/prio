package com.prio.core.data.repository

import com.prio.core.common.model.GoalCategory
import com.prio.core.common.model.GoalStatus
import com.prio.core.data.local.dao.GoalDao
import com.prio.core.data.local.dao.MilestoneDao
import com.prio.core.data.local.dao.TaskDao
import com.prio.core.data.local.entity.GoalEntity
import com.prio.core.data.local.entity.MilestoneEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Goal and Milestone operations with progress calculation.
 * 
 * Implements 2.1.7 from ACTION_PLAN.md:
 * - Progress = completed_linked_tasks / total_linked_tasks per GL-002
 * - Milestone tracking per GL-004
 * 
 * Based on user stories:
 * - GL-001: Create goal with AI assistance
 * - GL-002: Goal progress visualization
 * - GL-003: Link tasks to goals
 * - GL-004: Goal milestones
 * - GL-005: Goal dashboard
 */
@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val milestoneDao: MilestoneDao,
    private val taskDao: TaskDao,
    private val clock: Clock = Clock.System
) {
    
    companion object {
        /**
         * Maximum number of active goals allowed.
         * Per GL-001: Maximum 10 active goals (prevent overwhelm).
         */
        const val MAX_ACTIVE_GOALS = 10
        
        /**
         * Maximum milestones per goal.
         * Per GL-004: 0-5 milestones per goal.
         */
        const val MAX_MILESTONES_PER_GOAL = 5
        
        /**
         * Threshold for "at risk" status (behind by 15%+).
         * Per GL-002: Red = behind by 15%+.
         */
        const val AT_RISK_THRESHOLD = 0.15f
        
        /**
         * Threshold for "slightly behind" status (within 15%).
         * Per GL-002: Yellow = within 15%.
         */
        const val SLIGHTLY_BEHIND_THRESHOLD = 0.15f
    }
    
    // ==================== Goal Query Operations ====================
    
    /**
     * Get all active goals sorted by target date.
     */
    fun getAllActiveGoals(): Flow<List<GoalEntity>> = 
        goalDao.getAllActiveGoals()
    
    /**
     * Get all goals including completed.
     */
    fun getAllGoals(): Flow<List<GoalEntity>> = 
        goalDao.getAllGoals()
    
    /**
     * Get a single goal by ID as Flow.
     */
    fun getGoalByIdFlow(goalId: Long): Flow<GoalEntity?> = 
        goalDao.getByIdFlow(goalId)
    
    /**
     * Get goals by category.
     */
    fun getGoalsByCategory(category: GoalCategory): Flow<List<GoalEntity>> = 
        goalDao.getByCategory(category)
    
    /**
     * Get completed goals.
     */
    fun getCompletedGoals(): Flow<List<GoalEntity>> = 
        goalDao.getCompletedGoals()
    
    /**
     * Get goals needing attention (lowest progress).
     * Per GL-005: At-risk first, then by deadline.
     */
    fun getGoalsNeedingAttention(limit: Int = 3): Flow<List<GoalEntity>> = 
        goalDao.getGoalsNeedingAttention(limit)
    
    /**
     * Get active goal count as Flow.
     * Per GL-001: Maximum 10 active goals check.
     */
    fun getActiveGoalCountFlow(): Flow<Int> = 
        goalDao.getActiveGoalCountFlow()
    
    // ==================== Goal Suspend Queries ====================
    
    /**
     * Get a single goal by ID.
     */
    suspend fun getGoalById(goalId: Long): GoalEntity? = 
        goalDao.getById(goalId)
    
    /**
     * Check if user can create a new goal.
     * Per GL-001: Maximum 10 active goals.
     */
    suspend fun canCreateNewGoal(): Boolean = 
        goalDao.getActiveGoalCount() < MAX_ACTIVE_GOALS
    
    /**
     * Get active goal count.
     */
    suspend fun getActiveGoalCount(): Int = 
        goalDao.getActiveGoalCount()
    
    // ==================== Goal Insert Operations ====================
    
    /**
     * Create a new goal.
     * Returns the generated goal ID, or null if max goals reached.
     * 
     * Per GL-001: Create goal with AI assistance.
     */
    suspend fun createGoal(
        title: String,
        description: String? = null,
        originalInput: String? = null,
        category: GoalCategory,
        targetDate: LocalDate? = null
    ): Long? {
        // Check max goals limit
        if (!canCreateNewGoal()) {
            return null
        }
        
        val now = clock.now()
        val goal = GoalEntity(
            title = title,
            description = description,
            originalInput = originalInput,
            category = category,
            targetDate = targetDate,
            progress = 0,
            createdAt = now,
            updatedAt = now
        )
        
        return goalDao.insert(goal)
    }
    
    /**
     * Insert a complete goal entity.
     */
    suspend fun insertGoal(goal: GoalEntity): Long = 
        goalDao.insert(goal)
    
    // ==================== Goal Update Operations ====================
    
    /**
     * Update a goal.
     */
    suspend fun updateGoal(goal: GoalEntity) {
        val updatedGoal = goal.copy(updatedAt = clock.now())
        goalDao.update(updatedGoal)
    }
    
    /**
     * Update goal progress based on linked tasks.
     * Per GL-002: Progress = completed linked tasks / total linked tasks.
     */
    suspend fun recalculateProgress(goalId: Long) {
        val activeTasks = taskDao.getActiveByGoalId(goalId)
        val completedTasks = taskDao.getCompletedByGoalId(goalId)
        
        val totalTasks = activeTasks.size + completedTasks.size
        val progress = if (totalTasks > 0) {
            ((completedTasks.size.toFloat() / totalTasks) * 100).toInt()
        } else {
            0
        }
        
        goalDao.updateProgress(
            goalId = goalId,
            progress = progress,
            updatedAt = clock.now()
        )
    }
    
    /**
     * Mark a goal as completed.
     * Per GL-002: Celebrate completion with animation.
     */
    suspend fun completeGoal(goalId: Long) {
        val now = clock.now()
        goalDao.updateCompletionStatus(
            goalId = goalId,
            isCompleted = true,
            completedAt = now,
            updatedAt = now
        )
    }
    
    /**
     * Mark a goal as not completed (undo).
     */
    suspend fun uncompleteGoal(goalId: Long) {
        val now = clock.now()
        goalDao.updateCompletionStatus(
            goalId = goalId,
            isCompleted = false,
            completedAt = null,
            updatedAt = now
        )
    }
    
    // ==================== Goal Delete Operations ====================
    
    /**
     * Delete a goal and its milestones.
     * Note: Tasks linked to this goal will have goal_id set to NULL (FK CASCADE).
     */
    suspend fun deleteGoal(goal: GoalEntity) {
        // Milestones are deleted via CASCADE in database
        goalDao.delete(goal)
    }
    
    /**
     * Delete a goal by ID.
     */
    suspend fun deleteGoalById(goalId: Long) {
        goalDao.deleteById(goalId)
    }
    
    // ==================== Milestone Operations ====================
    
    /**
     * Get milestones for a goal.
     * Per GL-004: Milestones appear in goal detail as timeline.
     */
    fun getMilestonesByGoalId(goalId: Long): Flow<List<MilestoneEntity>> = 
        milestoneDao.getByGoalId(goalId)
    
    /**
     * Add a milestone to a goal.
     * Per GL-004: 0-5 milestones per goal.
     */
    suspend fun addMilestone(
        goalId: Long,
        title: String,
        targetDate: LocalDate? = null
    ): Long? {
        // Check max milestones limit
        val currentCount = milestoneDao.getMilestoneCountForGoal(goalId)
        if (currentCount >= MAX_MILESTONES_PER_GOAL) {
            return null
        }
        
        val now = clock.now()
        val milestone = MilestoneEntity(
            goalId = goalId,
            title = title,
            targetDate = targetDate,
            position = currentCount,
            createdAt = now,
            updatedAt = now
        )
        
        return milestoneDao.insert(milestone)
    }
    
    /**
     * Update a milestone.
     */
    suspend fun updateMilestone(milestone: MilestoneEntity) {
        val updated = milestone.copy(updatedAt = clock.now())
        milestoneDao.update(updated)
    }
    
    /**
     * Complete a milestone.
     * Per GL-004: Check off milestones when completed.
     */
    suspend fun completeMilestone(milestoneId: Long) {
        val now = clock.now()
        milestoneDao.updateCompletionStatus(
            milestoneId = milestoneId,
            isCompleted = true,
            completedAt = now,
            updatedAt = now
        )
    }
    
    /**
     * Uncomplete a milestone (undo).
     */
    suspend fun uncompleteMilestone(milestoneId: Long) {
        milestoneDao.updateCompletionStatus(
            milestoneId = milestoneId,
            isCompleted = false,
            completedAt = null,
            updatedAt = clock.now()
        )
    }
    
    /**
     * Delete a milestone.
     */
    suspend fun deleteMilestone(milestone: MilestoneEntity) = 
        milestoneDao.delete(milestone)
    
    /**
     * Delete a milestone by ID.
     */
    suspend fun deleteMilestoneById(milestoneId: Long) = 
        milestoneDao.deleteById(milestoneId)
    
    // ==================== Goal Status Calculation ====================
    
    /**
     * Calculate goal status based on progress vs time elapsed.
     * 
     * Per GL-002:
     * - Green: On track (progress >= time elapsed %)
     * - Yellow: Slightly behind (within 15%)
     * - Red: At risk (behind by 15%+)
     */
    fun calculateGoalStatus(goal: GoalEntity): GoalStatus {
        if (goal.isCompleted) {
            return GoalStatus.COMPLETED
        }
        
        val targetDate = goal.targetDate ?: return GoalStatus.ON_TRACK
        
        val timeZone = TimeZone.currentSystemDefault()
        val now = clock.now().toLocalDateTime(timeZone).date
        val createdDate = goal.createdAt.toLocalDateTime(timeZone).date
        
        // Calculate expected progress based on time elapsed
        val totalDays = createdDate.daysUntil(targetDate)
        if (totalDays <= 0) {
            // Target date is today or in the past
            return if (goal.progress >= 100) GoalStatus.COMPLETED else GoalStatus.AT_RISK
        }
        
        val elapsedDays = createdDate.daysUntil(now)
        if (elapsedDays < 0) {
            // Not started yet (shouldn't happen)
            return GoalStatus.ON_TRACK
        }
        
        val expectedProgress = (elapsedDays.toFloat() / totalDays * 100).toInt()
        val actualProgress = goal.progress
        val difference = expectedProgress - actualProgress
        
        return when {
            difference <= 0 -> GoalStatus.ON_TRACK // Ahead or on track
            difference <= (AT_RISK_THRESHOLD * 100) -> GoalStatus.BEHIND // Within threshold
            else -> GoalStatus.AT_RISK // Behind threshold
        }
    }
    
    /**
     * Get milestone completion progress for a goal.
     * Returns pair of (completed, total).
     */
    suspend fun getMilestoneProgress(goalId: Long): Pair<Int, Int> {
        val total = milestoneDao.getMilestoneCountForGoal(goalId)
        val completed = milestoneDao.getCompletedMilestoneCountForGoal(goalId)
        return completed to total
    }
    
    // ==================== Dashboard Stats ====================
    
    /**
     * Get dashboard statistics for goals.
     * Per GL-005: Summary stats for dashboard.
     *
     * Calculates on-track/at-risk counts by iterating active goals
     * and applying [calculateGoalStatus]. Completed-this-month uses
     * the first day of the current month as the cutoff instant.
     */
    suspend fun getDashboardStats(): GoalDashboardStats {
        val activeGoals = goalDao.getActiveGoalsList()
        val activeCount = activeGoals.size

        var onTrackCount = 0
        var atRiskCount = 0
        for (goal in activeGoals) {
            when (calculateGoalStatus(goal)) {
                GoalStatus.ON_TRACK -> onTrackCount++
                GoalStatus.AT_RISK -> atRiskCount++
                GoalStatus.BEHIND -> { /* not displayed in stats */ }
                GoalStatus.COMPLETED -> { /* shouldn't appear in active list */ }
            }
        }

        // First day of current month at midnight for "completed this month" query
        val timeZone = TimeZone.currentSystemDefault()
        val today = clock.now().toLocalDateTime(timeZone).date
        val firstOfMonth = LocalDate(today.year, today.monthNumber, 1)
        val firstOfMonthInstant = firstOfMonth.atStartOfDayIn(timeZone)
        val completedThisMonth = goalDao.getCompletedGoalCountSince(firstOfMonthInstant)

        return GoalDashboardStats(
            activeGoals = activeCount,
            onTrackCount = onTrackCount,
            atRiskCount = atRiskCount,
            completedThisMonth = completedThisMonth
        )
    }
}

/**
 * Dashboard statistics for goals.
 * Per GL-005: Summary stats.
 */
data class GoalDashboardStats(
    val activeGoals: Int,
    val onTrackCount: Int,
    val atRiskCount: Int,
    val completedThisMonth: Int
)

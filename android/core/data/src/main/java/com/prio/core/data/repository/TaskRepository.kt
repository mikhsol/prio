package com.prio.core.data.repository

import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.data.local.dao.DailyAnalyticsDao
import com.prio.core.data.local.dao.GoalDao
import com.prio.core.data.local.dao.MilestoneDao
import com.prio.core.data.local.dao.TaskDao
import com.prio.core.data.local.entity.DailyAnalyticsEntity
import com.prio.core.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Repository for Task operations with urgency recalculation.
 * 
 * Implements 2.1.6 from ACTION_PLAN.md:
 * - Flow<List<Task>> for reactive updates
 * - Urgency recalculation based on TM-005
 * - All CRUD operations
 * 
 * Based on user stories:
 * - TM-001: Quick task capture
 * - TM-003: AI Eisenhower classification
 * - TM-005: Deadline-based urgency scoring
 * - TM-006: Complete, edit, delete tasks
 */
@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val goalDao: GoalDao,
    private val milestoneDao: MilestoneDao,
    private val dailyAnalyticsDao: DailyAnalyticsDao,
    private val clock: Clock = Clock.System
) {
    
    // ==================== Query Operations (Flow) ====================
    
    /**
     * Get all active tasks sorted by quadrant and position.
     * Active = not completed.
     */
    fun getAllActiveTasks(): Flow<List<TaskEntity>> = 
        taskDao.getAllActiveTasks()

    /**
     * Get all active tasks as a suspend call (non-Flow).
     * Used by BriefingGenerator and notification workers for one-shot reads.
     */
    suspend fun getAllActiveTasksSync(): List<TaskEntity> =
        taskDao.getAllActiveTasksSync()
    
    /**
     * Get all tasks including completed.
     */
    fun getAllTasks(): Flow<List<TaskEntity>> = 
        taskDao.getAllTasks()
    
    /**
     * Get a single task by ID as Flow for reactive updates.
     */
    fun getTaskByIdFlow(taskId: Long): Flow<TaskEntity?> = 
        taskDao.getByIdFlow(taskId)
    
    /**
     * Get tasks by Eisenhower quadrant.
     * Per TM-004: View tasks by quadrant.
     */
    fun getTasksByQuadrant(quadrant: EisenhowerQuadrant): Flow<List<TaskEntity>> = 
        taskDao.getByQuadrant(quadrant)
    
    /**
     * Get tasks linked to a specific goal.
     * Per GL-003: Task-Goal linking.
     */
    fun getTasksByGoalId(goalId: Long): Flow<List<TaskEntity>> = 
        taskDao.getByGoalId(goalId)
    
    /**
     * Get subtasks for a parent task.
     * Per TM-007: Task subtasks.
     */
    fun getSubtasks(parentTaskId: Long): Flow<List<TaskEntity>> = 
        taskDao.getSubtasks(parentTaskId)
    
    /**
     * Get overdue tasks (due date in the past, not completed).
     * Per TM-005: Urgency scoring.
     */
    fun getOverdueTasks(): Flow<List<TaskEntity>> = 
        taskDao.getOverdue(clock.now().toEpochMilliseconds())
    
    /**
     * Get tasks due on a specific date.
     * Per CB-005: Calendar day view.
     */
    fun getTasksByDate(dateMillis: Long): Flow<List<TaskEntity>> = 
        taskDao.getByDate(dateMillis)
    
    /**
     * Get tasks due within a date range.
     */
    fun getTasksDueInRange(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>> = 
        taskDao.getDueInRange(startMillis, endMillis)
    
    /**
     * Search tasks by title or notes.
     * Per TM-004: Search functionality.
     */
    fun searchTasks(query: String): Flow<List<TaskEntity>> = 
        taskDao.search(query)
    
    /**
     * Get recurring tasks.
     * Per TM-008: Recurring tasks.
     */
    fun getRecurringTasks(): Flow<List<TaskEntity>> = 
        taskDao.getRecurringTasks()
    
    // ==================== Suspend Query Operations ====================
    
    /**
     * Get a single task by ID.
     */
    suspend fun getTaskById(taskId: Long): TaskEntity? = 
        taskDao.getById(taskId)
    
    /**
     * Get active tasks linked to a goal (for progress calculation).
     * Per GL-002: Goal progress = completed linked tasks / total linked tasks.
     */
    suspend fun getActiveTasksByGoalId(goalId: Long): List<TaskEntity> = 
        taskDao.getActiveByGoalId(goalId)
    
    /**
     * Get completed tasks linked to a goal (for progress calculation).
     */
    suspend fun getCompletedTasksByGoalId(goalId: Long): List<TaskEntity> = 
        taskDao.getCompletedByGoalId(goalId)
    
    // ==================== Insert Operations ====================
    
    /**
     * Create a new task with automatic urgency calculation.
     * Returns the generated task ID.
     * 
     * Per TM-001: Quick task capture.
     * Per TM-003: AI Eisenhower classification.
     */
    suspend fun createTask(
        title: String,
        notes: String? = null,
        dueDate: Instant? = null,
        quadrant: EisenhowerQuadrant = EisenhowerQuadrant.ELIMINATE,
        goalId: Long? = null,
        parentTaskId: Long? = null,
        aiExplanation: String? = null,
        aiConfidence: Float = 0f
    ): Long {
        val now = clock.now()
        val urgencyScore = calculateUrgencyScore(dueDate, now)
        
        val task = TaskEntity(
            title = title,
            notes = notes,
            dueDate = dueDate,
            quadrant = quadrant,
            goalId = goalId,
            parentTaskId = parentTaskId,
            urgencyScore = urgencyScore,
            aiExplanation = aiExplanation,
            aiConfidence = aiConfidence,
            createdAt = now,
            updatedAt = now,
            position = 0 // Will be updated if needed
        )
        
        val id = taskDao.insert(task)
        recordAnalyticsEvent { it.copy(tasksCreated = it.tasksCreated + 1) }
        return id
    }
    
    /**
     * Insert a complete task entity.
     */
    suspend fun insertTask(task: TaskEntity): Long = 
        taskDao.insert(task)
    
    /**
     * Insert multiple tasks.
     */
    suspend fun insertAllTasks(tasks: List<TaskEntity>) = 
        taskDao.insertAll(tasks)
    
    // ==================== Update Operations ====================
    
    /**
     * Update a task.
     */
    suspend fun updateTask(task: TaskEntity) {
        val updatedTask = task.copy(
            updatedAt = clock.now(),
            urgencyScore = calculateUrgencyScore(task.dueDate, clock.now())
        )
        taskDao.update(updatedTask)
    }
    
    /**
     * Mark a task as completed.
     * Per TM-006: Complete tasks.
     */
    suspend fun completeTask(taskId: Long) {
        val now = clock.now()
        val task = taskDao.getById(taskId)
        taskDao.updateCompletionStatus(
            taskId = taskId,
            isCompleted = true,
            completedAt = now,
            updatedAt = now
        )
        // Record analytics: task completion with quadrant breakdown
        task?.let { t ->
            recordAnalyticsEvent { analytics ->
                when (t.quadrant) {
                    EisenhowerQuadrant.DO_FIRST -> analytics.copy(
                        tasksCompleted = analytics.tasksCompleted + 1,
                        q1Completed = analytics.q1Completed + 1
                    )
                    EisenhowerQuadrant.SCHEDULE -> analytics.copy(
                        tasksCompleted = analytics.tasksCompleted + 1,
                        q2Completed = analytics.q2Completed + 1
                    )
                    EisenhowerQuadrant.DELEGATE -> analytics.copy(
                        tasksCompleted = analytics.tasksCompleted + 1,
                        q3Completed = analytics.q3Completed + 1
                    )
                    EisenhowerQuadrant.ELIMINATE -> analytics.copy(
                        tasksCompleted = analytics.tasksCompleted + 1,
                        q4Completed = analytics.q4Completed + 1
                    )
                }
            }
        }
        // Recalculate goal progress if task is linked to a goal (GL-002)
        task?.goalId?.let { goalId ->
            recalculateGoalProgress(goalId)
        }
    }
    
    /**
     * Mark a task as not completed (undo).
     * Per TM-006: 5-second undo.
     */
    suspend fun uncompleteTask(taskId: Long) {
        val now = clock.now()
        val task = taskDao.getById(taskId)
        taskDao.updateCompletionStatus(
            taskId = taskId,
            isCompleted = false,
            completedAt = null,
            updatedAt = now
        )
        // Recalculate goal progress if task is linked to a goal (GL-002)
        task?.goalId?.let { goalId ->
            recalculateGoalProgress(goalId)
        }
    }
    
    /**
     * Override AI quadrant classification.
     * Per TM-010: Override AI classification.
     * 
     * Note: This should trigger analytics tracking for AI accuracy.
     */
    suspend fun updateQuadrant(taskId: Long, quadrant: EisenhowerQuadrant) {
        taskDao.updateQuadrant(
            taskId = taskId,
            quadrant = quadrant,
            updatedAt = clock.now()
        )
    }
    
    /**
     * Update task position for drag-and-drop reordering.
     * Per TM-004: Drag-and-drop reordering.
     */
    suspend fun updatePosition(taskId: Long, position: Int) {
        taskDao.updatePosition(
            taskId = taskId,
            position = position,
            updatedAt = clock.now()
        )
    }

    /**
     * Update task due date.
     * Per CB-005: Close Day flow — move tasks to tomorrow or clear date.
     *
     * @param taskId The task ID
     * @param dueDate New due date, or null to clear
     */
    suspend fun updateTaskDueDate(taskId: Long, dueDate: Instant?) {
        taskDao.updateDueDate(
            taskId = taskId,
            dueDate = dueDate,
            updatedAt = clock.now()
        )
    }

    /**
     * Update task quadrant.
     * Alias for [updateQuadrant] used by Close Day flow (CB-005).
     */
    suspend fun updateTaskQuadrant(taskId: Long, quadrant: EisenhowerQuadrant) {
        updateQuadrant(taskId, quadrant)
    }
    
    // ==================== Delete Operations ====================
    
    /**
     * Delete a task.
     * Per TM-006: Delete with undo (soft delete handled at UI layer).
     */
    suspend fun deleteTask(task: TaskEntity) = 
        taskDao.delete(task)
    
    /**
     * Delete a task by ID.
     */
    suspend fun deleteTaskById(taskId: Long) = 
        taskDao.deleteById(taskId)
    
    // ==================== Urgency Calculation ====================
    
    /**
     * Calculate urgency score based on deadline.
     * 
     * Per TM-005 from user stories:
     * - 7+ days away: Low urgency (0.0-0.25)
     * - 2-6 days: Medium urgency (0.25-0.5)
     * - 1 day: High urgency (0.5-0.75)
     * - Past due: Critical urgency (0.75-1.0)
     * 
     * Returns 0.0 if no deadline set.
     */
    fun calculateUrgencyScore(dueDate: Instant?, now: Instant = clock.now()): Float {
        if (dueDate == null) return 0f
        
        val timeZone = TimeZone.currentSystemDefault()
        val nowLocal = now.toLocalDateTime(timeZone).date
        val dueLocal = dueDate.toLocalDateTime(timeZone).date
        
        val daysUntilDue = nowLocal.daysUntil(dueLocal)
        
        return when {
            daysUntilDue < 0 -> {
                // Overdue: 0.75 to 1.0 based on how overdue
                val daysOverdue = -daysUntilDue
                min(1f, 0.75f + (daysOverdue * 0.05f))
            }
            daysUntilDue == 0 -> 0.75f // Due today
            daysUntilDue == 1 -> 0.65f // Due tomorrow
            daysUntilDue <= 3 -> 0.5f  // Within 3 days
            daysUntilDue <= 7 -> 0.25f // Within a week
            else -> max(0f, 0.25f - (daysUntilDue - 7) * 0.01f) // Beyond a week
        }
    }
    
    /**
     * Recalculate urgency scores for all active tasks.
     * Should be called daily via WorkManager.
     */
    suspend fun recalculateAllUrgencyScores() {
        val now = clock.now()
        val activeTasks = taskDao.getAllActiveTasksSync()
        
        // MVP: Update individually. This is functional but not optimal.
        // TODO: Optimize with batch update in v1.1 (see POST_MVP_ROADMAP.md)
        activeTasks.forEach { task ->
            val newScore = calculateUrgencyScore(task.dueDate, now)
            if (task.urgencyScore != newScore) {
                taskDao.updateUrgencyScore(task.id, newScore, now)
            }
        }
    }
    
    // ==================== Analytics Helpers ====================
    
    /**
     * Get count of tasks created on a specific date.
     */
    suspend fun getTasksCreatedOnDate(dateMillis: Long): Int = 
        taskDao.getTasksCreatedOnDate(dateMillis)
    
    /**
     * Get count of tasks completed on a specific date.
     */
    suspend fun getTasksCompletedOnDate(dateMillis: Long): Int = 
        taskDao.getTasksCompletedOnDate(dateMillis)
    
    /**
     * Get count of tasks completed by quadrant on a specific date.
     */
    suspend fun getTasksCompletedByQuadrantOnDate(
        dateMillis: Long, 
        quadrant: EisenhowerQuadrant
    ): Int = taskDao.getTasksCompletedByQuadrantOnDate(dateMillis, quadrant)

    /**
     * Get completed task entities within a date range.
     * Used by BriefingGenerator for displaying completed task titles.
     */
    suspend fun getTasksCompletedInRange(startMillis: Long, endMillis: Long): List<TaskEntity> =
        taskDao.getCompletedInRange(startMillis, endMillis)

    // ==================== Analytics Recording ====================

    /**
     * Record an AI classification event.
     * Called when the Eisenhower Engine classifies a task.
     * Per 0.3.8 Success Metrics: AI Accuracy tracking.
     */
    suspend fun recordAiClassification() {
        recordAnalyticsEvent { it.copy(aiClassifications = it.aiClassifications + 1) }
    }

    /**
     * Record an AI override event (user changes AI-suggested quadrant).
     * Per 0.3.8 Success Metrics: AI Accuracy = (classifications - overrides) / classifications.
     */
    suspend fun recordAiOverride() {
        recordAnalyticsEvent { it.copy(aiOverrides = it.aiOverrides + 1) }
    }

    // ==================== Goal Progress Recalculation ====================

    /**
     * Recalculate goal progress after a linked task is completed or uncompleted.
     *
     * Uses the same weighted formula as [GoalRepository.recalculateProgress]:
     * - Both milestones + tasks: 60% milestone ratio + 40% task ratio
     * - Milestones only: completedMilestones / totalMilestones
     * - Tasks only: completedTasks / totalTasks
     *
     * This avoids a circular dependency between TaskRepository and GoalRepository
     * by accessing GoalDao and MilestoneDao directly.
     */
    private suspend fun recalculateGoalProgress(goalId: Long) {
        try {
            val activeTasks = taskDao.getActiveByGoalId(goalId)
            val completedTasks = taskDao.getCompletedByGoalId(goalId)

            val totalMilestones = milestoneDao.getMilestoneCountForGoal(goalId)
            val completedMilestones = milestoneDao.getCompletedMilestoneCountForGoal(goalId)

            val totalTasks = activeTasks.size + completedTasks.size
            val hasTasks = totalTasks > 0
            val hasMilestones = totalMilestones > 0

            val progress = when {
                hasMilestones && hasTasks -> {
                    val milestoneRatio = completedMilestones.toFloat() / totalMilestones
                    val taskRatio = completedTasks.size.toFloat() / totalTasks
                    ((MILESTONE_WEIGHT * milestoneRatio + TASK_WEIGHT * taskRatio) * 100).toInt()
                }
                hasMilestones -> {
                    ((completedMilestones.toFloat() / totalMilestones) * 100).toInt()
                }
                hasTasks -> {
                    ((completedTasks.size.toFloat() / totalTasks) * 100).toInt()
                }
                else -> 0
            }

            goalDao.updateProgress(
                goalId = goalId,
                progress = progress,
                updatedAt = clock.now()
            )
        } catch (e: Exception) {
            // Goal progress recalculation is best-effort; never crash the app
        }
    }

    companion object {
        /** Weight for milestone contribution to overall goal progress. */
        const val MILESTONE_WEIGHT = 0.6f
        /** Weight for task contribution to overall goal progress. */
        const val TASK_WEIGHT = 0.4f
    }

    // ==================== Analytics Recording ====================

    /**
     * Lightweight analytics event recorder.
     * Ensures a DailyAnalyticsEntity exists for today, then applies the update.
     * Uses DailyAnalyticsDao directly to avoid circular dependency with AnalyticsRepository.
     */
    private suspend fun recordAnalyticsEvent(
        update: (DailyAnalyticsEntity) -> DailyAnalyticsEntity
    ) {
        try {
            val timeZone = TimeZone.currentSystemDefault()
            val today = clock.now().toLocalDateTime(timeZone).date
            val existing = dailyAnalyticsDao.getByDate(today)
            val analytics = existing ?: run {
                val entity = DailyAnalyticsEntity(date = today)
                val id = dailyAnalyticsDao.insert(entity)
                entity.copy(id = id)
            }
            dailyAnalyticsDao.update(update(analytics))
        } catch (e: Exception) {
            // Analytics recording is best-effort; never crash the app
        }
    }
}

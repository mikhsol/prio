package com.prio.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Data Access Object for Task operations.
 * 
 * Based on TM-004 from 0.3.2_task_management_user_stories.md:
 * - getByQuadrant, getByDate, getByGoalId, getOverdue, search
 */
@Dao
interface TaskDao {
    
    // Insert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)
    
    // Update operations
    @Update
    suspend fun update(task: TaskEntity)
    
    @Query("UPDATE tasks SET is_completed = :isCompleted, completed_at = :completedAt, updated_at = :updatedAt WHERE id = :taskId")
    suspend fun updateCompletionStatus(taskId: Long, isCompleted: Boolean, completedAt: Instant?, updatedAt: Instant)
    
    @Query("UPDATE tasks SET quadrant = :quadrant, updated_at = :updatedAt WHERE id = :taskId")
    suspend fun updateQuadrant(taskId: Long, quadrant: EisenhowerQuadrant, updatedAt: Instant)
    
    @Query("UPDATE tasks SET position = :position, updated_at = :updatedAt WHERE id = :taskId")
    suspend fun updatePosition(taskId: Long, position: Int, updatedAt: Instant)
    
    @Query("UPDATE tasks SET urgency_score = :urgencyScore, updated_at = :updatedAt WHERE id = :taskId")
    suspend fun updateUrgencyScore(taskId: Long, urgencyScore: Float, updatedAt: Instant)
    
    // Delete operations
    @Delete
    suspend fun delete(task: TaskEntity)
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)
    
    // Query operations - Flow for reactive updates
    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY quadrant ASC, position ASC")
    fun getAllActiveTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY quadrant ASC, position ASC")
    suspend fun getAllActiveTasksSync(): List<TaskEntity>
    
    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getById(taskId: Long): TaskEntity?
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getByIdFlow(taskId: Long): Flow<TaskEntity?>
    
    @Query("SELECT * FROM tasks WHERE quadrant = :quadrant AND is_completed = 0 ORDER BY position ASC")
    fun getByQuadrant(quadrant: EisenhowerQuadrant): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE goal_id = :goalId ORDER BY position ASC")
    fun getByGoalId(goalId: Long): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE goal_id = :goalId AND is_completed = 0")
    suspend fun getActiveByGoalId(goalId: Long): List<TaskEntity>
    
    @Query("SELECT * FROM tasks WHERE goal_id = :goalId AND is_completed = 1")
    suspend fun getCompletedByGoalId(goalId: Long): List<TaskEntity>
    
    @Query("SELECT * FROM tasks WHERE parent_task_id = :parentId ORDER BY position ASC")
    fun getSubtasks(parentId: Long): Flow<List<TaskEntity>>
    
    // Date-based queries
    @Query("SELECT * FROM tasks WHERE date(due_date / 1000, 'unixepoch') = date(:dateMillis / 1000, 'unixepoch') AND is_completed = 0 ORDER BY quadrant ASC")
    fun getByDate(dateMillis: Long): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE due_date IS NOT NULL AND due_date < :now AND is_completed = 0 ORDER BY due_date ASC")
    fun getOverdue(now: Long): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE due_date IS NOT NULL AND due_date >= :startMillis AND due_date < :endMillis AND is_completed = 0 ORDER BY due_date ASC")
    fun getDueInRange(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>>
    
    // Search
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun search(query: String): Flow<List<TaskEntity>>
    
    // Analytics queries
    @Query("SELECT COUNT(*) FROM tasks WHERE date(created_at / 1000, 'unixepoch') = date(:dateMillis / 1000, 'unixepoch')")
    suspend fun getTasksCreatedOnDate(dateMillis: Long): Int
    
    @Query("SELECT COUNT(*) FROM tasks WHERE date(completed_at / 1000, 'unixepoch') = date(:dateMillis / 1000, 'unixepoch')")
    suspend fun getTasksCompletedOnDate(dateMillis: Long): Int
    
    @Query("SELECT COUNT(*) FROM tasks WHERE date(completed_at / 1000, 'unixepoch') = date(:dateMillis / 1000, 'unixepoch') AND quadrant = :quadrant")
    suspend fun getTasksCompletedByQuadrantOnDate(dateMillis: Long, quadrant: EisenhowerQuadrant): Int
    
    // Recurring tasks
    @Query("SELECT * FROM tasks WHERE is_recurring = 1 AND is_completed = 0")
    fun getRecurringTasks(): Flow<List<TaskEntity>>
}

package com.prio.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.prio.core.common.model.GoalCategory
import com.prio.core.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Data Access Object for Goal operations.
 * 
 * Based on GL-001 through GL-006 from 0.3.3_goals_user_stories.md
 */
@Dao
interface GoalDao {
    
    // Insert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long
    
    // Update operations
    @Update
    suspend fun update(goal: GoalEntity)
    
    @Query("UPDATE goals SET progress = :progress, updated_at = :updatedAt WHERE id = :goalId")
    suspend fun updateProgress(goalId: Long, progress: Int, updatedAt: Instant)
    
    @Query("UPDATE goals SET is_completed = :isCompleted, completed_at = :completedAt, updated_at = :updatedAt WHERE id = :goalId")
    suspend fun updateCompletionStatus(goalId: Long, isCompleted: Boolean, completedAt: Instant?, updatedAt: Instant)
    
    // Delete operations
    @Delete
    suspend fun delete(goal: GoalEntity)
    
    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteById(goalId: Long)
    
    // Query operations
    @Query("SELECT * FROM goals WHERE is_completed = 0 ORDER BY target_date ASC")
    fun getAllActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE is_completed = 0 ORDER BY target_date ASC")
    suspend fun getAllActiveGoalsSync(): List<GoalEntity>
    
    @Query("SELECT * FROM goals ORDER BY created_at DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>
    
    @Query("SELECT * FROM goals WHERE id = :goalId")
    suspend fun getById(goalId: Long): GoalEntity?
    
    @Query("SELECT * FROM goals WHERE id = :goalId")
    fun getByIdFlow(goalId: Long): Flow<GoalEntity?>
    
    @Query("SELECT * FROM goals WHERE category = :category AND is_completed = 0 ORDER BY target_date ASC")
    fun getByCategory(category: GoalCategory): Flow<List<GoalEntity>>
    
    @Query("SELECT * FROM goals WHERE is_completed = 1 ORDER BY completed_at DESC")
    fun getCompletedGoals(): Flow<List<GoalEntity>>
    
    // Progress queries for GL-002
    @Query("SELECT * FROM goals WHERE is_completed = 0 ORDER BY progress ASC LIMIT :limit")
    fun getGoalsNeedingAttention(limit: Int = 3): Flow<List<GoalEntity>>
    
    // Count queries for GL-001 (max 10 active goals)
    @Query("SELECT COUNT(*) FROM goals WHERE is_completed = 0")
    suspend fun getActiveGoalCount(): Int
    
    @Query("SELECT COUNT(*) FROM goals WHERE is_completed = 0")
    fun getActiveGoalCountFlow(): Flow<Int>

    // Suspend list query for dashboard stats calculation (GL-005)
    @Query("SELECT * FROM goals WHERE is_completed = 0 ORDER BY target_date ASC")
    suspend fun getActiveGoalsList(): List<GoalEntity>

    // Completed goals within date range (GL-005)
    @Query("SELECT COUNT(*) FROM goals WHERE is_completed = 1 AND completed_at >= :since")
    suspend fun getCompletedGoalCountSince(since: Instant): Int
}

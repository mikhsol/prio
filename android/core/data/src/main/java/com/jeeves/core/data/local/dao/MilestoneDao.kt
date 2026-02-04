package com.jeeves.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jeeves.core.data.local.entity.MilestoneEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Data Access Object for Milestone operations.
 * 
 * Based on GL-004 from 0.3.3_goals_user_stories.md
 */
@Dao
interface MilestoneDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(milestone: MilestoneEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(milestones: List<MilestoneEntity>)
    
    @Update
    suspend fun update(milestone: MilestoneEntity)
    
    @Query("UPDATE milestones SET is_completed = :isCompleted, completed_at = :completedAt, updated_at = :updatedAt WHERE id = :milestoneId")
    suspend fun updateCompletionStatus(milestoneId: Long, isCompleted: Boolean, completedAt: Instant?, updatedAt: Instant)
    
    @Delete
    suspend fun delete(milestone: MilestoneEntity)
    
    @Query("DELETE FROM milestones WHERE id = :milestoneId")
    suspend fun deleteById(milestoneId: Long)
    
    @Query("DELETE FROM milestones WHERE goal_id = :goalId")
    suspend fun deleteByGoalId(goalId: Long)
    
    @Query("SELECT * FROM milestones WHERE goal_id = :goalId ORDER BY position ASC")
    fun getByGoalId(goalId: Long): Flow<List<MilestoneEntity>>
    
    @Query("SELECT * FROM milestones WHERE id = :milestoneId")
    suspend fun getById(milestoneId: Long): MilestoneEntity?
    
    @Query("SELECT COUNT(*) FROM milestones WHERE goal_id = :goalId")
    suspend fun getMilestoneCountForGoal(goalId: Long): Int
    
    @Query("SELECT COUNT(*) FROM milestones WHERE goal_id = :goalId AND is_completed = 1")
    suspend fun getCompletedMilestoneCountForGoal(goalId: Long): Int
}

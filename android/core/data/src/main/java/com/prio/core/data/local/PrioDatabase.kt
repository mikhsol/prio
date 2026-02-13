package com.prio.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.prio.core.data.local.converter.PrioTypeConverters
import com.prio.core.data.local.dao.DailyAnalyticsDao
import com.prio.core.data.local.dao.GoalDao
import com.prio.core.data.local.dao.MeetingDao
import com.prio.core.data.local.dao.MilestoneDao
import com.prio.core.data.local.dao.TaskDao
import com.prio.core.data.local.entity.DailyAnalyticsEntity
import com.prio.core.data.local.entity.GoalEntity
import com.prio.core.data.local.entity.MeetingEntity
import com.prio.core.data.local.entity.MilestoneEntity
import com.prio.core.data.local.entity.TaskEntity

/**
 * Main Room database for Prio application.
 * 
 * Entities based on ACTION_PLAN.md Milestone 2.1 data model requirements:
 * - TaskEntity: title, due_date, quadrant (Q1-Q4), goal_id (FK), notes, etc.
 * - GoalEntity: title, description, category, target_date, progress
 * - MilestoneEntity: goal_id (FK), title, target_date, is_complete
 * - MeetingEntity: calendar_event_id, notes, action_items (JSON)
 * - DailyAnalyticsEntity: daily productivity metrics
 */
@Database(
    entities = [
        TaskEntity::class,
        GoalEntity::class,
        MilestoneEntity::class,
        MeetingEntity::class,
        DailyAnalyticsEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(PrioTypeConverters::class)
abstract class PrioDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun goalDao(): GoalDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun meetingDao(): MeetingDao
    abstract fun dailyAnalyticsDao(): DailyAnalyticsDao
    
    companion object {
        const val DATABASE_NAME = "prio_database"
    }
}

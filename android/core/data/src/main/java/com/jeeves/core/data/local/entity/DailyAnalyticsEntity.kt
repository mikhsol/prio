package com.jeeves.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

/**
 * Daily analytics entity for tracking productivity metrics.
 * 
 * Based on 0.3.8 Success Metrics document.
 * Tracks: tasks_created, tasks_completed, quadrant_breakdown
 */
@Entity(
    tableName = "daily_analytics",
    indices = [
        Index(value = ["date"], unique = true)
    ]
)
data class DailyAnalyticsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "date")
    val date: LocalDate,
    
    @ColumnInfo(name = "tasks_created")
    val tasksCreated: Int = 0,
    
    @ColumnInfo(name = "tasks_completed")
    val tasksCompleted: Int = 0,
    
    @ColumnInfo(name = "q1_completed")
    val q1Completed: Int = 0,
    
    @ColumnInfo(name = "q2_completed")
    val q2Completed: Int = 0,
    
    @ColumnInfo(name = "q3_completed")
    val q3Completed: Int = 0,
    
    @ColumnInfo(name = "q4_completed")
    val q4Completed: Int = 0,
    
    @ColumnInfo(name = "goals_progressed")
    val goalsProgressed: Int = 0,
    
    @ColumnInfo(name = "ai_classifications")
    val aiClassifications: Int = 0,
    
    @ColumnInfo(name = "ai_overrides")
    val aiOverrides: Int = 0,
    
    @ColumnInfo(name = "briefing_opened")
    val briefingOpened: Boolean = false,
    
    @ColumnInfo(name = "summary_opened")
    val summaryOpened: Boolean = false
)

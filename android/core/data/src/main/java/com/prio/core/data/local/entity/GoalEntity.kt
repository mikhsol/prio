package com.prio.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.prio.core.common.model.GoalCategory
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Goal entity for Room database.
 * 
 * Based on data model requirements from:
 * - ACTION_PLAN.md Milestone 2.1
 * - 0.3.3 Goals User Stories (GL-001 through GL-006)
 */
@Entity(
    tableName = "goals",
    indices = [
        Index(value = ["category"]),
        Index(value = ["target_date"]),
        Index(value = ["is_completed"]),
        Index(value = ["is_archived"])
    ]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "original_input")
    val originalInput: String? = null,
    
    @ColumnInfo(name = "category")
    val category: GoalCategory,
    
    @ColumnInfo(name = "target_date")
    val targetDate: LocalDate? = null,
    
    @ColumnInfo(name = "progress")
    val progress: Int = 0,
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    
    @ColumnInfo(name = "completed_at")
    val completedAt: Instant? = null,
    
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
    
    @ColumnInfo(name = "archived_at")
    val archivedAt: Instant? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)

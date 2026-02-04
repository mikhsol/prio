package com.jeeves.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jeeves.core.common.model.EisenhowerQuadrant
import com.jeeves.core.common.model.RecurrencePattern
import kotlinx.datetime.Instant

/**
 * Task entity for Room database.
 * 
 * Based on data model requirements from:
 * - ACTION_PLAN.md Milestone 2.1
 * - 0.3.2 Task Management User Stories (TM-001 through TM-010)
 * - 0.3.3 Goals User Stories (GL-003 task-goal linking)
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["goal_id"]),
        Index(value = ["parent_task_id"]),
        Index(value = ["quadrant"]),
        Index(value = ["due_date"]),
        Index(value = ["is_completed"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "due_date")
    val dueDate: Instant? = null,
    
    @ColumnInfo(name = "quadrant")
    val quadrant: EisenhowerQuadrant = EisenhowerQuadrant.ELIMINATE,
    
    @ColumnInfo(name = "goal_id")
    val goalId: Long? = null,
    
    @ColumnInfo(name = "parent_task_id")
    val parentTaskId: Long? = null,
    
    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean = false,
    
    @ColumnInfo(name = "recurrence_pattern")
    val recurrencePattern: RecurrencePattern? = null,
    
    @ColumnInfo(name = "urgency_score")
    val urgencyScore: Float = 0f,
    
    @ColumnInfo(name = "ai_explanation")
    val aiExplanation: String? = null,
    
    @ColumnInfo(name = "ai_confidence")
    val aiConfidence: Float = 0f,
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    
    @ColumnInfo(name = "completed_at")
    val completedAt: Instant? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    
    @ColumnInfo(name = "position")
    val position: Int = 0
)

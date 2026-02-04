package com.jeeves.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Meeting entity for Room database.
 * 
 * Based on data model requirements from:
 * - ACTION_PLAN.md Milestone 2.1
 * - 0.3.4 Calendar & Briefings User Stories (CB-004)
 */
@Entity(
    tableName = "meetings",
    indices = [
        Index(value = ["calendar_event_id"]),
        Index(value = ["start_time"]),
        Index(value = ["end_time"])
    ]
)
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "calendar_event_id")
    val calendarEventId: String? = null,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "location")
    val location: String? = null,
    
    @ColumnInfo(name = "start_time")
    val startTime: Instant,
    
    @ColumnInfo(name = "end_time")
    val endTime: Instant,
    
    @ColumnInfo(name = "attendees")
    val attendees: String? = null, // JSON array of attendee names
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "action_items")
    val actionItems: String? = null, // JSON array of ActionItem
    
    @ColumnInfo(name = "agenda")
    val agenda: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)

/**
 * Action item extracted from meeting notes.
 */
@Serializable
data class ActionItem(
    val description: String,
    val assignee: String? = null,
    val isCompleted: Boolean = false,
    val linkedTaskId: Long? = null
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        
        fun listToJson(items: List<ActionItem>): String = json.encodeToString(items)
        
        fun listFromJson(jsonString: String): List<ActionItem> = 
            json.decodeFromString(jsonString)
    }
}

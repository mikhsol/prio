package com.prio.core.data.repository

import com.prio.core.data.local.dao.MeetingDao
import com.prio.core.data.local.entity.ActionItem
import com.prio.core.data.local.entity.MeetingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

/**
 * Repository for Meeting operations with calendar event linking.
 * 
 * Implements 2.1.8 from ACTION_PLAN.md:
 * - Calendar event linking
 * - Action item extraction storage
 * 
 * Based on user stories:
 * - CB-002: Calendar read integration
 * - CB-004: Meeting notes + action items
 */
@Singleton
class MeetingRepository @Inject constructor(
    private val meetingDao: MeetingDao,
    private val clock: Clock = Clock.System
) {
    
    // ==================== Query Operations (Flow) ====================
    
    /**
     * Get a meeting by ID as Flow.
     */
    fun getMeetingByIdFlow(meetingId: Long): Flow<MeetingEntity?> = 
        meetingDao.getByIdFlow(meetingId)
    
    /**
     * Get meetings within a time range.
     * Used for calendar views.
     */
    fun getMeetingsInRange(startMillis: Long, endMillis: Long): Flow<List<MeetingEntity>> = 
        meetingDao.getMeetingsInRange(startMillis, endMillis)

    /**
     * Get meetings within a time range as a suspend call (non-Flow).
     * Used by BriefingGenerator and Dashboard for one-shot reads.
     */
    suspend fun getMeetingsForDateSync(
        startTime: kotlinx.datetime.Instant,
        endTime: kotlinx.datetime.Instant
    ): List<MeetingEntity> =
        meetingDao.getMeetingsInRangeSync(
            startMillis = startTime.toEpochMilliseconds(),
            endMillis = endTime.toEpochMilliseconds()
        )
    
    /**
     * Get meetings for a specific date.
     * Per CB-005: Calendar day view.
     */
    fun getMeetingsForDate(dateMillis: Long): Flow<List<MeetingEntity>> = 
        meetingDao.getMeetingsForDate(dateMillis)
    
    /**
     * Get today's meetings.
     */
    fun getTodaysMeetings(): Flow<List<MeetingEntity>> {
        val now = clock.now()
        val timeZone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(timeZone).date
        val startOfDay = today.atStartOfDayIn(timeZone)
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        val startOfNextDay = tomorrow.atStartOfDayIn(timeZone)
        
        return meetingDao.getMeetingsInRange(
            startMillis = startOfDay.toEpochMilliseconds(),
            endMillis = startOfNextDay.toEpochMilliseconds()
        )
    }
    
    /**
     * Get meetings with pending action items.
     * Per CB-004: Track meetings with action items.
     */
    fun getMeetingsWithActionItems(): Flow<List<MeetingEntity>> = 
        meetingDao.getMeetingsWithActionItems()
    
    /**
     * Get recent meetings.
     */
    fun getRecentMeetings(limit: Int = 10): Flow<List<MeetingEntity>> = 
        meetingDao.getRecentMeetings(limit)
    
    // ==================== Suspend Query Operations ====================
    
    /**
     * Get a meeting by ID.
     */
    suspend fun getMeetingById(meetingId: Long): MeetingEntity? = 
        meetingDao.getById(meetingId)
    
    /**
     * Get meeting by calendar event ID.
     * For linking with system calendar events.
     */
    suspend fun getMeetingByCalendarEventId(calendarEventId: String): MeetingEntity? = 
        meetingDao.getByCalendarEventId(calendarEventId)
    
    // ==================== Insert Operations ====================
    
    /**
     * Create a new meeting.
     * Returns the generated meeting ID.
     */
    suspend fun createMeeting(
        title: String,
        startTime: Instant,
        endTime: Instant,
        description: String? = null,
        location: String? = null,
        calendarEventId: String? = null,
        attendees: List<String>? = null,
        agenda: String? = null
    ): Long {
        val now = clock.now()
        val attendeesJson = attendees?.joinToString(separator = ",")
        
        val meeting = MeetingEntity(
            title = title,
            startTime = startTime,
            endTime = endTime,
            description = description,
            location = location,
            calendarEventId = calendarEventId,
            attendees = attendeesJson,
            agenda = agenda,
            createdAt = now,
            updatedAt = now
        )
        
        return meetingDao.insert(meeting)
    }
    
    /**
     * Insert a complete meeting entity.
     */
    suspend fun insertMeeting(meeting: MeetingEntity): Long = 
        meetingDao.insert(meeting)
    
    /**
     * Insert or update meeting from calendar sync.
     * Uses calendarEventId to find existing meeting.
     */
    suspend fun upsertFromCalendar(
        calendarEventId: String,
        title: String,
        startTime: Instant,
        endTime: Instant,
        description: String? = null,
        location: String? = null,
        attendees: List<String>? = null
    ): Long {
        val existing = getMeetingByCalendarEventId(calendarEventId)
        val now = clock.now()
        val attendeesJson = attendees?.joinToString(separator = ",")
        
        return if (existing != null) {
            // Update existing meeting, preserving notes and action items
            val updated = existing.copy(
                title = title,
                startTime = startTime,
                endTime = endTime,
                description = description,
                location = location,
                attendees = attendeesJson,
                updatedAt = now
            )
            meetingDao.update(updated)
            existing.id
        } else {
            // Create new meeting
            createMeeting(
                title = title,
                startTime = startTime,
                endTime = endTime,
                description = description,
                location = location,
                calendarEventId = calendarEventId,
                attendees = attendees
            )
        }
    }
    
    /**
     * Insert multiple meetings (for bulk calendar sync).
     */
    suspend fun insertAllMeetings(meetings: List<MeetingEntity>) = 
        meetingDao.insertAll(meetings)
    
    // ==================== Update Operations ====================
    
    /**
     * Update a meeting.
     */
    suspend fun updateMeeting(meeting: MeetingEntity) {
        val updated = meeting.copy(updatedAt = clock.now())
        meetingDao.update(updated)
    }
    
    /**
     * Update meeting notes.
     * Per CB-004: Take notes during/after meetings.
     */
    suspend fun updateNotes(meetingId: Long, notes: String) {
        val meeting = getMeetingById(meetingId) ?: return
        val updated = meeting.copy(
            notes = notes,
            updatedAt = clock.now()
        )
        meetingDao.update(updated)
    }
    
    /**
     * Update meeting agenda.
     */
    suspend fun updateAgenda(meetingId: Long, agenda: String) {
        val meeting = getMeetingById(meetingId) ?: return
        val updated = meeting.copy(
            agenda = agenda,
            updatedAt = clock.now()
        )
        meetingDao.update(updated)
    }
    
    // ==================== Action Items ====================
    
    /**
     * Add action items to a meeting.
     * Per CB-004: Extract action items from notes.
     */
    suspend fun addActionItems(meetingId: Long, items: List<ActionItem>) {
        val meeting = getMeetingById(meetingId) ?: return
        
        // Parse existing action items
        val existingItems = meeting.actionItems?.let { 
            try {
                ActionItem.listFromJson(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
        
        // Combine and save
        val allItems = existingItems + items
        val updated = meeting.copy(
            actionItems = ActionItem.listToJson(allItems),
            updatedAt = clock.now()
        )
        meetingDao.update(updated)
    }
    
    /**
     * Update action items for a meeting.
     */
    suspend fun updateActionItems(meetingId: Long, items: List<ActionItem>) {
        val meeting = getMeetingById(meetingId) ?: return
        val updated = meeting.copy(
            actionItems = if (items.isEmpty()) null else ActionItem.listToJson(items),
            updatedAt = clock.now()
        )
        meetingDao.update(updated)
    }
    
    /**
     * Get action items for a meeting.
     */
    suspend fun getActionItems(meetingId: Long): List<ActionItem> {
        val meeting = getMeetingById(meetingId) ?: return emptyList()
        return meeting.actionItems?.let {
            try {
                ActionItem.listFromJson(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }
    
    /**
     * Mark an action item as completed.
     */
    suspend fun completeActionItem(meetingId: Long, actionItemIndex: Int) {
        val items = getActionItems(meetingId).toMutableList()
        if (actionItemIndex in items.indices) {
            items[actionItemIndex] = items[actionItemIndex].copy(isCompleted = true)
            updateActionItems(meetingId, items)
        }
    }
    
    /**
     * Link an action item to a task.
     * Per CB-004: Convert action items to tasks.
     */
    suspend fun linkActionItemToTask(meetingId: Long, actionItemIndex: Int, taskId: Long) {
        val items = getActionItems(meetingId).toMutableList()
        if (actionItemIndex in items.indices) {
            items[actionItemIndex] = items[actionItemIndex].copy(linkedTaskId = taskId)
            updateActionItems(meetingId, items)
        }
    }
    
    // ==================== Delete Operations ====================
    
    /**
     * Delete a meeting.
     */
    suspend fun deleteMeeting(meeting: MeetingEntity) = 
        meetingDao.delete(meeting)
    
    /**
     * Delete a meeting by ID.
     */
    suspend fun deleteMeetingById(meetingId: Long) = 
        meetingDao.deleteById(meetingId)
}

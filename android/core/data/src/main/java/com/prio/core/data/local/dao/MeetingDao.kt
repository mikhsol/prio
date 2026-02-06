package com.prio.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.prio.core.data.local.entity.MeetingEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Meeting operations.
 * 
 * Based on CB-004 from 0.3.4_calendar_briefings_user_stories.md
 */
@Dao
interface MeetingDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meeting: MeetingEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(meetings: List<MeetingEntity>)
    
    @Update
    suspend fun update(meeting: MeetingEntity)
    
    @Delete
    suspend fun delete(meeting: MeetingEntity)
    
    @Query("DELETE FROM meetings WHERE id = :meetingId")
    suspend fun deleteById(meetingId: Long)
    
    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    suspend fun getById(meetingId: Long): MeetingEntity?
    
    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    fun getByIdFlow(meetingId: Long): Flow<MeetingEntity?>
    
    @Query("SELECT * FROM meetings WHERE calendar_event_id = :calendarEventId")
    suspend fun getByCalendarEventId(calendarEventId: String): MeetingEntity?
    
    @Query("SELECT * FROM meetings WHERE start_time >= :startMillis AND start_time < :endMillis ORDER BY start_time ASC")
    fun getMeetingsInRange(startMillis: Long, endMillis: Long): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings WHERE start_time >= :startMillis AND start_time < :endMillis ORDER BY start_time ASC")
    suspend fun getMeetingsInRangeSync(startMillis: Long, endMillis: Long): List<MeetingEntity>
    
    @Query("SELECT * FROM meetings WHERE date(start_time / 1000, 'unixepoch') = date(:dateMillis / 1000, 'unixepoch') ORDER BY start_time ASC")
    fun getMeetingsForDate(dateMillis: Long): Flow<List<MeetingEntity>>
    
    @Query("SELECT * FROM meetings WHERE action_items IS NOT NULL AND action_items != '[]' ORDER BY start_time DESC")
    fun getMeetingsWithActionItems(): Flow<List<MeetingEntity>>
    
    @Query("SELECT * FROM meetings ORDER BY start_time DESC LIMIT :limit")
    fun getRecentMeetings(limit: Int = 10): Flow<List<MeetingEntity>>
}

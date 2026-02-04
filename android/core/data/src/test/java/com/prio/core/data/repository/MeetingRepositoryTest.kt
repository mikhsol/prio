package com.prio.core.data.repository

import app.cash.turbine.test
import com.prio.core.data.local.dao.MeetingDao
import com.prio.core.data.local.entity.ActionItem
import com.prio.core.data.local.entity.MeetingEntity
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours

/**
 * Unit tests for MeetingRepository.
 * 
 * Per ACTION_PLAN.md 2.1.10: 80%+ coverage.
 * Tests:
 * - Meeting CRUD operations
 * - Calendar event linking
 * - Action items management
 */
@DisplayName("MeetingRepository")
class MeetingRepositoryTest {
    
    @MockK
    private lateinit var meetingDao: MeetingDao
    
    @MockK
    private lateinit var clock: Clock
    
    private lateinit var repository: MeetingRepository
    
    private val now = Instant.parse("2026-02-04T12:00:00Z")
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { clock.now() } returns now
        repository = MeetingRepository(meetingDao, clock)
    }
    
    private fun createTestMeeting(
        id: Long = 1L,
        title: String = "Test Meeting",
        calendarEventId: String? = "calendar_123",
        actionItems: String? = null
    ) = MeetingEntity(
        id = id,
        title = title,
        startTime = now,
        endTime = now + 1.hours,
        calendarEventId = calendarEventId,
        actionItems = actionItems,
        createdAt = now,
        updatedAt = now
    )
    
    @Nested
    @DisplayName("Query Operations")
    inner class QueryOperations {
        
        @Test
        @DisplayName("getMeetingByIdFlow returns Flow from DAO")
        fun getMeetingByIdFlow_returnsFlowFromDao() = runTest {
            val meeting = createTestMeeting()
            every { meetingDao.getByIdFlow(1L) } returns flowOf(meeting)
            
            val flow = repository.getMeetingByIdFlow(1L)
            
            flow.test {
                val result = awaitItem()
                assertEquals("Test Meeting", result?.title)
                cancelAndConsumeRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("getMeetingsForDate returns meetings for date")
        fun getMeetingsForDate_returnsMeetings() = runTest {
            val meetings = listOf(createTestMeeting(1), createTestMeeting(2))
            every { meetingDao.getMeetingsForDate(any()) } returns flowOf(meetings)
            
            val flow = repository.getMeetingsForDate(now.toEpochMilliseconds())
            
            flow.test {
                val result = awaitItem()
                assertEquals(2, result.size)
                cancelAndConsumeRemainingEvents()
            }
        }
    }
    
    @Nested
    @DisplayName("Create Operations")
    inner class CreateOperations {
        
        @Test
        @DisplayName("createMeeting inserts meeting correctly")
        fun createMeeting_inserts() = runTest {
            val meetingSlot = slot<MeetingEntity>()
            coEvery { meetingDao.insert(capture(meetingSlot)) } returns 1L
            
            val result = repository.createMeeting(
                title = "New Meeting",
                startTime = now,
                endTime = now + 1.hours,
                location = "Room 101",
                attendees = listOf("Alice", "Bob")
            )
            
            assertEquals(1L, result)
            assertEquals("New Meeting", meetingSlot.captured.title)
            assertEquals("Room 101", meetingSlot.captured.location)
            assertEquals("Alice,Bob", meetingSlot.captured.attendees)
        }
        
        @Test
        @DisplayName("upsertFromCalendar updates existing meeting")
        fun upsertFromCalendar_updatesExisting() = runTest {
            val calendarId = "cal_456"
            val existing = createTestMeeting(id = 5L, calendarEventId = calendarId)
            coEvery { meetingDao.getByCalendarEventId(calendarId) } returns existing
            
            val result = repository.upsertFromCalendar(
                calendarEventId = calendarId,
                title = "Updated Meeting Title",
                startTime = now,
                endTime = now + 2.hours
            )
            
            assertEquals(5L, result)
            val updatedSlot = slot<MeetingEntity>()
            coVerify { meetingDao.update(capture(updatedSlot)) }
            assertEquals("Updated Meeting Title", updatedSlot.captured.title)
        }
        
        @Test
        @DisplayName("upsertFromCalendar creates new meeting")
        fun upsertFromCalendar_createsNew() = runTest {
            val calendarId = "cal_789"
            coEvery { meetingDao.getByCalendarEventId(calendarId) } returns null
            coEvery { meetingDao.insert(any()) } returns 10L
            
            val result = repository.upsertFromCalendar(
                calendarEventId = calendarId,
                title = "New Calendar Meeting",
                startTime = now,
                endTime = now + 1.hours
            )
            
            assertEquals(10L, result)
        }
    }
    
    @Nested
    @DisplayName("Action Items")
    inner class ActionItemsTests {
        
        @Test
        @DisplayName("addActionItems appends to existing items")
        fun addActionItems_appendsToExisting() = runTest {
            val existingItems = listOf(ActionItem("Existing item"))
            val meeting = createTestMeeting(actionItems = ActionItem.listToJson(existingItems))
            coEvery { meetingDao.getById(1L) } returns meeting
            
            val newItems = listOf(ActionItem("New item"))
            repository.addActionItems(1L, newItems)
            
            val updatedSlot = slot<MeetingEntity>()
            coVerify { meetingDao.update(capture(updatedSlot)) }
            
            val savedItems = ActionItem.listFromJson(updatedSlot.captured.actionItems!!)
            assertEquals(2, savedItems.size)
            assertEquals("Existing item", savedItems[0].description)
            assertEquals("New item", savedItems[1].description)
        }
        
        @Test
        @DisplayName("getActionItems returns parsed items")
        fun getActionItems_returnsParsedItems() = runTest {
            val items = listOf(
                ActionItem("Task 1", assignee = "Alice"),
                ActionItem("Task 2", isCompleted = true)
            )
            val meeting = createTestMeeting(actionItems = ActionItem.listToJson(items))
            coEvery { meetingDao.getById(1L) } returns meeting
            
            val result = repository.getActionItems(1L)
            
            assertEquals(2, result.size)
            assertEquals("Task 1", result[0].description)
            assertEquals("Alice", result[0].assignee)
            assertTrue(result[1].isCompleted)
        }
        
        @Test
        @DisplayName("completeActionItem marks item as completed")
        fun completeActionItem_marksCompleted() = runTest {
            val items = listOf(
                ActionItem("Task 1"),
                ActionItem("Task 2")
            )
            val meeting = createTestMeeting(actionItems = ActionItem.listToJson(items))
            coEvery { meetingDao.getById(1L) } returns meeting
            
            repository.completeActionItem(1L, 0)
            
            val updatedSlot = slot<MeetingEntity>()
            coVerify { meetingDao.update(capture(updatedSlot)) }
            
            val savedItems = ActionItem.listFromJson(updatedSlot.captured.actionItems!!)
            assertTrue(savedItems[0].isCompleted)
        }
        
        @Test
        @DisplayName("linkActionItemToTask sets task ID")
        fun linkActionItemToTask_setsTaskId() = runTest {
            val items = listOf(ActionItem("Task to link"))
            val meeting = createTestMeeting(actionItems = ActionItem.listToJson(items))
            coEvery { meetingDao.getById(1L) } returns meeting
            
            repository.linkActionItemToTask(1L, 0, taskId = 100L)
            
            val updatedSlot = slot<MeetingEntity>()
            coVerify { meetingDao.update(capture(updatedSlot)) }
            
            val savedItems = ActionItem.listFromJson(updatedSlot.captured.actionItems!!)
            assertEquals(100L, savedItems[0].linkedTaskId)
        }
    }
    
    @Nested
    @DisplayName("Update Operations")
    inner class UpdateOperations {
        
        @Test
        @DisplayName("updateNotes updates meeting notes")
        fun updateNotes_updatesNotes() = runTest {
            val meeting = createTestMeeting()
            coEvery { meetingDao.getById(1L) } returns meeting
            
            repository.updateNotes(1L, "New notes content")
            
            val updatedSlot = slot<MeetingEntity>()
            coVerify { meetingDao.update(capture(updatedSlot)) }
            assertEquals("New notes content", updatedSlot.captured.notes)
        }
        
        @Test
        @DisplayName("updateAgenda updates meeting agenda")
        fun updateAgenda_updatesAgenda() = runTest {
            val meeting = createTestMeeting()
            coEvery { meetingDao.getById(1L) } returns meeting
            
            repository.updateAgenda(1L, "1. Intro\n2. Discussion\n3. Wrap-up")
            
            val updatedSlot = slot<MeetingEntity>()
            coVerify { meetingDao.update(capture(updatedSlot)) }
            assertEquals("1. Intro\n2. Discussion\n3. Wrap-up", updatedSlot.captured.agenda)
        }
    }
}

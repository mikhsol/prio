package com.prio.core.data.repository

import app.cash.turbine.test
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.common.model.RecurrencePattern
import com.prio.core.data.local.dao.DailyAnalyticsDao
import com.prio.core.data.local.dao.GoalDao
import com.prio.core.data.local.dao.MilestoneDao
import com.prio.core.data.local.dao.TaskDao
import com.prio.core.data.local.entity.TaskEntity
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Unit tests for TaskRepository.
 * 
 * Per ACTION_PLAN.md 2.1.10: 80%+ coverage.
 * Tests:
 * - Task-Goal linking updates progress
 * - Urgency recalculation
 * - Quadrant queries
 */
@DisplayName("TaskRepository")
class TaskRepositoryTest {
    
    @MockK
    private lateinit var taskDao: TaskDao
    
    @MockK
    private lateinit var goalDao: GoalDao
    
    @MockK
    private lateinit var milestoneDao: MilestoneDao
    
    @MockK
    private lateinit var dailyAnalyticsDao: DailyAnalyticsDao
    
    @MockK
    private lateinit var clock: Clock
    
    private lateinit var repository: TaskRepository
    
    private val now = Instant.parse("2026-02-04T12:00:00Z")
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { clock.now() } returns now
        repository = TaskRepository(taskDao, goalDao, milestoneDao, dailyAnalyticsDao, clock)
    }
    
    private fun createTestTask(
        id: Long = 1L,
        title: String = "Test Task",
        dueDate: Instant? = null,
        quadrant: EisenhowerQuadrant = EisenhowerQuadrant.ELIMINATE,
        goalId: Long? = null,
        isCompleted: Boolean = false
    ) = TaskEntity(
        id = id,
        title = title,
        dueDate = dueDate,
        quadrant = quadrant,
        goalId = goalId,
        isCompleted = isCompleted,
        createdAt = now,
        updatedAt = now
    )
    
    @Nested
    @DisplayName("Query Operations")
    inner class QueryOperations {
        
        @Test
        @DisplayName("getAllActiveTasks returns Flow from DAO")
        fun getAllActiveTasks_returnsFlowFromDao() = runTest {
            val tasks = listOf(createTestTask(1), createTestTask(2))
            every { taskDao.getAllActiveTasks() } returns flowOf(tasks)
            
            val flow = repository.getAllActiveTasks()
            
            flow.test {
                val result = awaitItem()
                assertEquals(2, result.size)
                cancelAndConsumeRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("getTasksByQuadrant returns tasks for specific quadrant")
        fun getTasksByQuadrant_returnsCorrectTasks() = runTest {
            val q1Tasks = listOf(
                createTestTask(1, "Urgent Task 1", quadrant = EisenhowerQuadrant.DO_FIRST),
                createTestTask(2, "Urgent Task 2", quadrant = EisenhowerQuadrant.DO_FIRST)
            )
            every { taskDao.getByQuadrant(EisenhowerQuadrant.DO_FIRST) } returns flowOf(q1Tasks)
            
            val flow = repository.getTasksByQuadrant(EisenhowerQuadrant.DO_FIRST)
            
            flow.test {
                val result = awaitItem()
                assertEquals(2, result.size)
                assertTrue(result.all { it.quadrant == EisenhowerQuadrant.DO_FIRST })
                cancelAndConsumeRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("getTasksByGoalId returns tasks linked to goal")
        fun getTasksByGoalId_returnsLinkedTasks() = runTest {
            val goalId = 100L
            val linkedTasks = listOf(
                createTestTask(1, "Task for Goal", goalId = goalId),
                createTestTask(2, "Another Task for Goal", goalId = goalId)
            )
            every { taskDao.getByGoalId(goalId) } returns flowOf(linkedTasks)
            
            val flow = repository.getTasksByGoalId(goalId)
            
            flow.test {
                val result = awaitItem()
                assertEquals(2, result.size)
                assertTrue(result.all { it.goalId == goalId })
                cancelAndConsumeRemainingEvents()
            }
        }
    }
    
    @Nested
    @DisplayName("Create Operations")
    inner class CreateOperations {
        
        @Test
        @DisplayName("createTask inserts task with calculated urgency")
        fun createTask_insertsWithUrgency() = runTest {
            val taskSlot = slot<TaskEntity>()
            coEvery { taskDao.insert(capture(taskSlot)) } returns 1L
            
            val tomorrow = now + 1.days
            val result = repository.createTask(
                title = "New Task",
                dueDate = tomorrow,
                quadrant = EisenhowerQuadrant.DO_FIRST
            )
            
            assertEquals(1L, result)
            assertEquals("New Task", taskSlot.captured.title)
            assertEquals(EisenhowerQuadrant.DO_FIRST, taskSlot.captured.quadrant)
            assertTrue(taskSlot.captured.urgencyScore > 0f) // Due tomorrow should have urgency
            assertEquals(now, taskSlot.captured.createdAt)
        }
        
        @Test
        @DisplayName("createTask with goal links task correctly")
        fun createTask_withGoal_linksCorrectly() = runTest {
            val taskSlot = slot<TaskEntity>()
            coEvery { taskDao.insert(capture(taskSlot)) } returns 1L
            
            val goalId = 100L
            repository.createTask(
                title = "Task for Goal",
                goalId = goalId
            )
            
            assertEquals(goalId, taskSlot.captured.goalId)
        }
    }
    
    @Nested
    @DisplayName("Update Operations")
    inner class UpdateOperations {
        
        @Test
        @DisplayName("completeTask updates completion status")
        fun completeTask_updatesStatus() = runTest {
            coEvery { taskDao.getById(1L) } returns null

            repository.completeTask(1L)
            
            coVerify { 
                taskDao.updateCompletionStatus(
                    taskId = 1L,
                    isCompleted = true,
                    completedAt = now,
                    updatedAt = now
                )
            }
        }
        
        @Test
        @DisplayName("uncompleteTask clears completion status")
        fun uncompleteTask_clearsStatus() = runTest {
            coEvery { taskDao.getById(1L) } returns null

            repository.uncompleteTask(1L)
            
            coVerify {
                taskDao.updateCompletionStatus(
                    taskId = 1L,
                    isCompleted = false,
                    completedAt = null,
                    updatedAt = now
                )
            }
        }
        
        @Test
        @DisplayName("updateQuadrant changes quadrant")
        fun updateQuadrant_changesQuadrant() = runTest {
            repository.updateQuadrant(1L, EisenhowerQuadrant.SCHEDULE)
            
            coVerify {
                taskDao.updateQuadrant(
                    taskId = 1L,
                    quadrant = EisenhowerQuadrant.SCHEDULE,
                    updatedAt = now
                )
            }
        }
    }
    
    @Nested
    @DisplayName("Goal Progress Recalculation")
    inner class GoalProgressRecalculation {
        
        @Test
        @DisplayName("completeTask recalculates goal progress when task is linked")
        fun completeTask_recalculatesGoalProgress() = runTest {
            val goalId = 100L
            val task = createTestTask(1L, "Task for Goal", goalId = goalId)
            coEvery { taskDao.getById(1L) } returns task
            
            // After completion: 1 completed, 2 active => 1/3 = 33%
            coEvery { taskDao.getActiveByGoalId(goalId) } returns listOf(
                createTestTask(2L, goalId = goalId),
                createTestTask(3L, goalId = goalId)
            )
            coEvery { taskDao.getCompletedByGoalId(goalId) } returns listOf(
                createTestTask(1L, goalId = goalId, isCompleted = true)
            )
            coEvery { milestoneDao.getMilestoneCountForGoal(goalId) } returns 0
            coEvery { milestoneDao.getCompletedMilestoneCountForGoal(goalId) } returns 0
            
            repository.completeTask(1L)
            
            coVerify {
                goalDao.updateProgress(goalId, 33, now)
            }
        }
        
        @Test
        @DisplayName("completeTask does not recalculate when task has no goal")
        fun completeTask_noGoal_skipsRecalculation() = runTest {
            val task = createTestTask(1L, "No Goal Task", goalId = null)
            coEvery { taskDao.getById(1L) } returns task
            
            repository.completeTask(1L)
            
            coVerify(exactly = 0) {
                goalDao.updateProgress(any(), any(), any())
            }
        }
        
        @Test
        @DisplayName("uncompleteTask recalculates goal progress when task is linked")
        fun uncompleteTask_recalculatesGoalProgress() = runTest {
            val goalId = 100L
            val task = createTestTask(1L, "Task for Goal", goalId = goalId, isCompleted = true)
            coEvery { taskDao.getById(1L) } returns task
            
            // After uncompletion: 2 active (including the uncompleted one), 1 completed => 1/3 = 33%
            coEvery { taskDao.getActiveByGoalId(goalId) } returns listOf(
                createTestTask(1L, goalId = goalId),
                createTestTask(2L, goalId = goalId)
            )
            coEvery { taskDao.getCompletedByGoalId(goalId) } returns listOf(
                createTestTask(3L, goalId = goalId, isCompleted = true)
            )
            coEvery { milestoneDao.getMilestoneCountForGoal(goalId) } returns 0
            coEvery { milestoneDao.getCompletedMilestoneCountForGoal(goalId) } returns 0
            
            repository.uncompleteTask(1L)
            
            coVerify {
                goalDao.updateProgress(goalId, 33, now)
            }
        }
        
        @Test
        @DisplayName("uncompleteTask does not recalculate when task has no goal")
        fun uncompleteTask_noGoal_skipsRecalculation() = runTest {
            val task = createTestTask(1L, "No Goal Task", goalId = null)
            coEvery { taskDao.getById(1L) } returns task
            
            repository.uncompleteTask(1L)
            
            coVerify(exactly = 0) {
                goalDao.updateProgress(any(), any(), any())
            }
        }
        
        @Test
        @DisplayName("completeTask recalculates with milestones and tasks weighted formula")
        fun completeTask_withMilestones_usesWeightedFormula() = runTest {
            val goalId = 100L
            val task = createTestTask(1L, "Task for Goal", goalId = goalId)
            coEvery { taskDao.getById(1L) } returns task
            
            // 2 milestones, 1 completed = 50% milestone ratio
            // 1 completed task out of 2 total = 50% task ratio
            // Weighted: 0.6 * 0.5 + 0.4 * 0.5 = 0.5 => 50%
            coEvery { taskDao.getActiveByGoalId(goalId) } returns listOf(
                createTestTask(2L, goalId = goalId)
            )
            coEvery { taskDao.getCompletedByGoalId(goalId) } returns listOf(
                createTestTask(1L, goalId = goalId, isCompleted = true)
            )
            coEvery { milestoneDao.getMilestoneCountForGoal(goalId) } returns 2
            coEvery { milestoneDao.getCompletedMilestoneCountForGoal(goalId) } returns 1
            
            repository.completeTask(1L)
            
            coVerify {
                goalDao.updateProgress(goalId, 50, now)
            }
        }
        
        @Test
        @DisplayName("completeTask with all tasks completed updates to 100%")
        fun completeTask_allCompleted_updatesTo100() = runTest {
            val goalId = 100L
            val task = createTestTask(1L, "Last Task", goalId = goalId)
            coEvery { taskDao.getById(1L) } returns task
            
            // All tasks completed
            coEvery { taskDao.getActiveByGoalId(goalId) } returns emptyList()
            coEvery { taskDao.getCompletedByGoalId(goalId) } returns listOf(
                createTestTask(1L, goalId = goalId, isCompleted = true),
                createTestTask(2L, goalId = goalId, isCompleted = true)
            )
            coEvery { milestoneDao.getMilestoneCountForGoal(goalId) } returns 0
            coEvery { milestoneDao.getCompletedMilestoneCountForGoal(goalId) } returns 0
            
            repository.completeTask(1L)
            
            coVerify {
                goalDao.updateProgress(goalId, 100, now)
            }
        }
    }
    
    @Nested
    @DisplayName("Urgency Calculation")
    inner class UrgencyCalculation {
        
        @Test
        @DisplayName("calculateUrgencyScore returns 0 for no deadline")
        fun noDeadline_returnsZero() {
            val score = repository.calculateUrgencyScore(null, now)
            assertEquals(0f, score)
        }
        
        @Test
        @DisplayName("calculateUrgencyScore returns high urgency for overdue")
        fun overdue_returnsHighUrgency() {
            val yesterday = now - 1.days
            val score = repository.calculateUrgencyScore(yesterday, now)
            assertTrue(score >= 0.75f, "Overdue should have score >= 0.75")
        }
        
        @Test
        @DisplayName("calculateUrgencyScore returns high urgency for today")
        fun dueToday_returnsHighUrgency() {
            val score = repository.calculateUrgencyScore(now, now)
            assertEquals(0.75f, score)
        }
        
        @Test
        @DisplayName("calculateUrgencyScore returns medium urgency for tomorrow")
        fun dueTomorrow_returnsMediumUrgency() {
            val tomorrow = now + 1.days
            val score = repository.calculateUrgencyScore(tomorrow, now)
            assertEquals(0.65f, score)
        }
        
        @Test
        @DisplayName("calculateUrgencyScore returns lower urgency for next week")
        fun nextWeek_returnsLowerUrgency() {
            val nextWeek = now + 7.days
            val score = repository.calculateUrgencyScore(nextWeek, now)
            assertEquals(0.25f, score)
        }
        
        @Test
        @DisplayName("calculateUrgencyScore returns low urgency for far future")
        fun farFuture_returnsLowUrgency() {
            val farFuture = now + 30.days
            val score = repository.calculateUrgencyScore(farFuture, now)
            assertTrue(score < 0.25f, "Far future should have score < 0.25")
        }
    }
}

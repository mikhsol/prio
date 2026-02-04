package com.prio.core.data.repository

import app.cash.turbine.test
import com.prio.core.common.model.GoalCategory
import com.prio.core.common.model.GoalStatus
import com.prio.core.data.local.dao.GoalDao
import com.prio.core.data.local.dao.MilestoneDao
import com.prio.core.data.local.dao.TaskDao
import com.prio.core.data.local.entity.GoalEntity
import com.prio.core.data.local.entity.MilestoneEntity
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
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for GoalRepository.
 * 
 * Per ACTION_PLAN.md 2.1.10: 80%+ coverage.
 * Tests:
 * - Task-Goal linking updates progress
 * - Progress calculation
 * - Goal status calculation
 */
@DisplayName("GoalRepository")
class GoalRepositoryTest {
    
    @MockK
    private lateinit var goalDao: GoalDao
    
    @MockK
    private lateinit var milestoneDao: MilestoneDao
    
    @MockK
    private lateinit var taskDao: TaskDao
    
    @MockK
    private lateinit var clock: Clock
    
    private lateinit var repository: GoalRepository
    
    private val now = Instant.parse("2026-02-04T12:00:00Z")
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { clock.now() } returns now
        repository = GoalRepository(goalDao, milestoneDao, taskDao, clock)
    }
    
    private fun createTestGoal(
        id: Long = 1L,
        title: String = "Test Goal",
        category: GoalCategory = GoalCategory.CAREER,
        targetDate: LocalDate? = LocalDate.parse("2026-06-01"),
        progress: Int = 0,
        isCompleted: Boolean = false
    ) = GoalEntity(
        id = id,
        title = title,
        category = category,
        targetDate = targetDate,
        progress = progress,
        isCompleted = isCompleted,
        createdAt = now,
        updatedAt = now
    )
    
    private fun createTestTask(
        id: Long = 1L,
        goalId: Long? = null,
        isCompleted: Boolean = false
    ) = TaskEntity(
        id = id,
        title = "Task",
        goalId = goalId,
        isCompleted = isCompleted,
        createdAt = now,
        updatedAt = now
    )
    
    @Nested
    @DisplayName("Query Operations")
    inner class QueryOperations {
        
        @Test
        @DisplayName("getAllActiveGoals returns Flow from DAO")
        fun getAllActiveGoals_returnsFlowFromDao() = runTest {
            val goals = listOf(createTestGoal(1), createTestGoal(2))
            every { goalDao.getAllActiveGoals() } returns flowOf(goals)
            
            val flow = repository.getAllActiveGoals()
            
            flow.test {
                val result = awaitItem()
                assertEquals(2, result.size)
                cancelAndConsumeRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("getGoalsByCategory returns goals for specific category")
        fun getGoalsByCategory_returnsCorrectGoals() = runTest {
            val healthGoals = listOf(
                createTestGoal(1, "Health Goal 1", GoalCategory.HEALTH),
                createTestGoal(2, "Health Goal 2", GoalCategory.HEALTH)
            )
            every { goalDao.getByCategory(GoalCategory.HEALTH) } returns flowOf(healthGoals)
            
            val flow = repository.getGoalsByCategory(GoalCategory.HEALTH)
            
            flow.test {
                val result = awaitItem()
                assertEquals(2, result.size)
                assertTrue(result.all { it.category == GoalCategory.HEALTH })
                cancelAndConsumeRemainingEvents()
            }
        }
    }
    
    @Nested
    @DisplayName("Create Operations")
    inner class CreateOperations {
        
        @Test
        @DisplayName("createGoal fails when max goals reached")
        fun createGoal_failsWhenMaxReached() = runTest {
            coEvery { goalDao.getActiveGoalCount() } returns GoalRepository.MAX_ACTIVE_GOALS
            
            val result = repository.createGoal(
                title = "New Goal",
                category = GoalCategory.CAREER
            )
            
            assertNull(result)
        }
        
        @Test
        @DisplayName("createGoal succeeds when under limit")
        fun createGoal_succeedsWhenUnderLimit() = runTest {
            coEvery { goalDao.getActiveGoalCount() } returns 5
            val goalSlot = slot<GoalEntity>()
            coEvery { goalDao.insert(capture(goalSlot)) } returns 1L
            
            val result = repository.createGoal(
                title = "New Goal",
                description = "Description",
                category = GoalCategory.CAREER,
                targetDate = LocalDate.parse("2026-12-31")
            )
            
            assertEquals(1L, result)
            assertEquals("New Goal", goalSlot.captured.title)
            assertEquals(GoalCategory.CAREER, goalSlot.captured.category)
            assertEquals(0, goalSlot.captured.progress)
        }
    }
    
    @Nested
    @DisplayName("Progress Calculation")
    inner class ProgressCalculation {
        
        @Test
        @DisplayName("recalculateProgress calculates correctly")
        fun recalculateProgress_calculatesCorrectly() = runTest {
            val goalId = 1L
            val activeTasks = listOf(createTestTask(1, goalId), createTestTask(2, goalId))
            val completedTasks = listOf(createTestTask(3, goalId, isCompleted = true))
            
            coEvery { taskDao.getActiveByGoalId(goalId) } returns activeTasks
            coEvery { taskDao.getCompletedByGoalId(goalId) } returns completedTasks
            
            repository.recalculateProgress(goalId)
            
            // 1 completed / 3 total = 33%
            coVerify {
                goalDao.updateProgress(goalId, 33, now)
            }
        }
        
        @Test
        @DisplayName("recalculateProgress handles no tasks")
        fun recalculateProgress_handlesNoTasks() = runTest {
            val goalId = 1L
            coEvery { taskDao.getActiveByGoalId(goalId) } returns emptyList()
            coEvery { taskDao.getCompletedByGoalId(goalId) } returns emptyList()
            
            repository.recalculateProgress(goalId)
            
            coVerify {
                goalDao.updateProgress(goalId, 0, now)
            }
        }
        
        @Test
        @DisplayName("recalculateProgress 100% when all completed")
        fun recalculateProgress_100WhenAllCompleted() = runTest {
            val goalId = 1L
            val completedTasks = listOf(
                createTestTask(1, goalId, isCompleted = true),
                createTestTask(2, goalId, isCompleted = true)
            )
            
            coEvery { taskDao.getActiveByGoalId(goalId) } returns emptyList()
            coEvery { taskDao.getCompletedByGoalId(goalId) } returns completedTasks
            
            repository.recalculateProgress(goalId)
            
            coVerify {
                goalDao.updateProgress(goalId, 100, now)
            }
        }
    }
    
    @Nested
    @DisplayName("Goal Status Calculation")
    inner class StatusCalculation {
        
        @Test
        @DisplayName("calculateGoalStatus returns COMPLETED for completed goals")
        fun completedGoal_returnsCompleted() {
            val goal = createTestGoal(isCompleted = true)
            val status = repository.calculateGoalStatus(goal)
            assertEquals(GoalStatus.COMPLETED, status)
        }
        
        @Test
        @DisplayName("calculateGoalStatus returns ON_TRACK when no target date")
        fun noTargetDate_returnsOnTrack() {
            val goal = createTestGoal(targetDate = null, progress = 50)
            val status = repository.calculateGoalStatus(goal)
            assertEquals(GoalStatus.ON_TRACK, status)
        }
        
        @Test
        @DisplayName("calculateGoalStatus returns ON_TRACK when ahead")
        fun aheadOfSchedule_returnsOnTrack() {
            // Created today, target in 10 days, 50% progress (way ahead)
            val goal = createTestGoal(
                targetDate = LocalDate.parse("2026-02-14"),
                progress = 50
            )
            val status = repository.calculateGoalStatus(goal)
            assertEquals(GoalStatus.ON_TRACK, status)
        }
        
        @Test
        @DisplayName("calculateGoalStatus returns AT_RISK when overdue with low progress")
        fun overdueWithLowProgress_returnsAtRisk() {
            // Target date is today, only 10% progress
            val goal = createTestGoal(
                targetDate = LocalDate.parse("2026-02-04"),
                progress = 10
            )
            val status = repository.calculateGoalStatus(goal)
            assertEquals(GoalStatus.AT_RISK, status)
        }
    }
    
    @Nested
    @DisplayName("Milestone Operations")
    inner class MilestoneOperations {
        
        @Test
        @DisplayName("addMilestone fails when max reached")
        fun addMilestone_failsWhenMaxReached() = runTest {
            val goalId = 1L
            coEvery { milestoneDao.getMilestoneCountForGoal(goalId) } returns GoalRepository.MAX_MILESTONES_PER_GOAL
            
            val result = repository.addMilestone(goalId, "New Milestone")
            
            assertNull(result)
        }
        
        @Test
        @DisplayName("addMilestone succeeds when under limit")
        fun addMilestone_succeedsUnderLimit() = runTest {
            val goalId = 1L
            coEvery { milestoneDao.getMilestoneCountForGoal(goalId) } returns 2
            val milestoneSlot = slot<MilestoneEntity>()
            coEvery { milestoneDao.insert(capture(milestoneSlot)) } returns 1L
            
            val result = repository.addMilestone(goalId, "New Milestone")
            
            assertEquals(1L, result)
            assertEquals("New Milestone", milestoneSlot.captured.title)
            assertEquals(goalId, milestoneSlot.captured.goalId)
            assertEquals(2, milestoneSlot.captured.position) // Appended after existing 2
        }
        
        @Test
        @DisplayName("getMilestoneProgress returns correct counts")
        fun getMilestoneProgress_returnsCorrectCounts() = runTest {
            val goalId = 1L
            coEvery { milestoneDao.getMilestoneCountForGoal(goalId) } returns 5
            coEvery { milestoneDao.getCompletedMilestoneCountForGoal(goalId) } returns 3
            
            val (completed, total) = repository.getMilestoneProgress(goalId)
            
            assertEquals(3, completed)
            assertEquals(5, total)
        }
    }
}

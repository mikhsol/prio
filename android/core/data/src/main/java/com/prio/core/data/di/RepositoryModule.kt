package com.prio.core.data.di

import com.prio.core.data.local.dao.DailyAnalyticsDao
import com.prio.core.data.local.dao.GoalDao
import com.prio.core.data.local.dao.MeetingDao
import com.prio.core.data.local.dao.MilestoneDao
import com.prio.core.data.local.dao.TaskDao
import com.prio.core.data.repository.AnalyticsRepository
import com.prio.core.data.repository.GoalRepository
import com.prio.core.data.repository.MeetingRepository
import com.prio.core.data.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.datetime.Clock
import javax.inject.Singleton

/**
 * Hilt module for repository dependency injection.
 * 
 * Provides all repositories for the data layer.
 * Per ACTION_PLAN.md Milestone 2.1.6-2.1.9.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System
    
    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        goalDao: GoalDao,
        milestoneDao: MilestoneDao,
        dailyAnalyticsDao: DailyAnalyticsDao,
        clock: Clock
    ): TaskRepository = TaskRepository(taskDao, goalDao, milestoneDao, dailyAnalyticsDao, clock)
    
    @Provides
    @Singleton
    fun provideGoalRepository(
        goalDao: GoalDao,
        milestoneDao: MilestoneDao,
        taskDao: TaskDao,
        dailyAnalyticsDao: DailyAnalyticsDao,
        clock: Clock
    ): GoalRepository = GoalRepository(goalDao, milestoneDao, taskDao, dailyAnalyticsDao, clock)
    
    @Provides
    @Singleton
    fun provideMeetingRepository(
        meetingDao: MeetingDao,
        clock: Clock
    ): MeetingRepository = MeetingRepository(meetingDao, clock)
    
    @Provides
    @Singleton
    fun provideAnalyticsRepository(
        dailyAnalyticsDao: DailyAnalyticsDao,
        taskDao: TaskDao,
        clock: Clock
    ): AnalyticsRepository = AnalyticsRepository(dailyAnalyticsDao, taskDao, clock)
}

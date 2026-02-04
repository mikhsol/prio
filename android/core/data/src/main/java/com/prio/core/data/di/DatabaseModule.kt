package com.prio.core.data.di

import android.content.Context
import androidx.room.Room
import com.prio.core.data.local.PrioDatabase
import com.prio.core.data.local.dao.DailyAnalyticsDao
import com.prio.core.data.local.dao.GoalDao
import com.prio.core.data.local.dao.MeetingDao
import com.prio.core.data.local.dao.MilestoneDao
import com.prio.core.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependency injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun providePrioDatabase(
        @ApplicationContext context: Context
    ): PrioDatabase {
        return Room.databaseBuilder(
            context,
            PrioDatabase::class.java,
            PrioDatabase.DATABASE_NAME
        )
            // TODO: Add migrations before v1.1
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideTaskDao(database: PrioDatabase): TaskDao = database.taskDao()
    
    @Provides
    @Singleton
    fun provideGoalDao(database: PrioDatabase): GoalDao = database.goalDao()
    
    @Provides
    @Singleton
    fun provideMilestoneDao(database: PrioDatabase): MilestoneDao = database.milestoneDao()
    
    @Provides
    @Singleton
    fun provideMeetingDao(database: PrioDatabase): MeetingDao = database.meetingDao()
    
    @Provides
    @Singleton
    fun provideDailyAnalyticsDao(database: PrioDatabase): DailyAnalyticsDao = database.dailyAnalyticsDao()
}

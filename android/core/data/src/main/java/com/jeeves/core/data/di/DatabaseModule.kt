package com.jeeves.core.data.di

import android.content.Context
import androidx.room.Room
import com.jeeves.core.data.local.JeevesDatabase
import com.jeeves.core.data.local.dao.DailyAnalyticsDao
import com.jeeves.core.data.local.dao.GoalDao
import com.jeeves.core.data.local.dao.MeetingDao
import com.jeeves.core.data.local.dao.MilestoneDao
import com.jeeves.core.data.local.dao.TaskDao
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
    fun provideJeevesDatabase(
        @ApplicationContext context: Context
    ): JeevesDatabase {
        return Room.databaseBuilder(
            context,
            JeevesDatabase::class.java,
            JeevesDatabase.DATABASE_NAME
        )
            // TODO: Add migrations before v1.1
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideTaskDao(database: JeevesDatabase): TaskDao = database.taskDao()
    
    @Provides
    @Singleton
    fun provideGoalDao(database: JeevesDatabase): GoalDao = database.goalDao()
    
    @Provides
    @Singleton
    fun provideMilestoneDao(database: JeevesDatabase): MilestoneDao = database.milestoneDao()
    
    @Provides
    @Singleton
    fun provideMeetingDao(database: JeevesDatabase): MeetingDao = database.meetingDao()
    
    @Provides
    @Singleton
    fun provideDailyAnalyticsDao(database: JeevesDatabase): DailyAnalyticsDao = database.dailyAnalyticsDao()
}

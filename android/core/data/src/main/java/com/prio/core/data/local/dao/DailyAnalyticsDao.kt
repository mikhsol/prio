package com.prio.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.prio.core.data.local.entity.DailyAnalyticsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Data Access Object for DailyAnalytics operations.
 * 
 * Based on 0.3.8 Success Metrics
 */
@Dao
interface DailyAnalyticsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analytics: DailyAnalyticsEntity): Long
    
    @Update
    suspend fun update(analytics: DailyAnalyticsEntity)
    
    @Query("SELECT * FROM daily_analytics WHERE date = :date")
    suspend fun getByDate(date: LocalDate): DailyAnalyticsEntity?
    
    @Query("SELECT * FROM daily_analytics WHERE date = :date")
    fun getByDateFlow(date: LocalDate): Flow<DailyAnalyticsEntity?>
    
    @Query("SELECT * FROM daily_analytics WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyAnalyticsEntity>>
    
    @Query("SELECT * FROM daily_analytics ORDER BY date DESC LIMIT :days")
    fun getRecentDays(days: Int = 7): Flow<List<DailyAnalyticsEntity>>
    
    // Analytics aggregation queries
    @Query("SELECT SUM(tasks_completed) FROM daily_analytics WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalCompletedInRange(startDate: LocalDate, endDate: LocalDate): Int?
    
    @Query("SELECT SUM(tasks_created) FROM daily_analytics WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalCreatedInRange(startDate: LocalDate, endDate: LocalDate): Int?
    
    @Query("SELECT AVG(CAST(tasks_completed AS FLOAT) / NULLIF(tasks_created, 0)) FROM daily_analytics WHERE date >= :startDate AND date <= :endDate")
    suspend fun getCompletionRateInRange(startDate: LocalDate, endDate: LocalDate): Float?
    
    // AI accuracy tracking
    @Query("SELECT SUM(ai_classifications) FROM daily_analytics WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalAiClassificationsInRange(startDate: LocalDate, endDate: LocalDate): Int?
    
    @Query("SELECT SUM(ai_overrides) FROM daily_analytics WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalAiOverridesInRange(startDate: LocalDate, endDate: LocalDate): Int?
}

package com.jeeves.core.data.local.converter

import androidx.room.TypeConverter
import com.jeeves.core.common.model.EisenhowerQuadrant
import com.jeeves.core.common.model.GoalCategory
import com.jeeves.core.common.model.RecurrencePattern
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Room type converters for custom types.
 */
class JeevesTypeConverters {
    
    // Instant converters
    @TypeConverter
    fun instantToLong(instant: Instant?): Long? = instant?.toEpochMilliseconds()
    
    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }
    
    // LocalDate converters
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.toString()
    
    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }
    
    // EisenhowerQuadrant converters
    @TypeConverter
    fun quadrantToString(quadrant: EisenhowerQuadrant?): String? = quadrant?.name
    
    @TypeConverter
    fun stringToQuadrant(value: String?): EisenhowerQuadrant? = 
        value?.let { EisenhowerQuadrant.valueOf(it) }
    
    // GoalCategory converters
    @TypeConverter
    fun categoryToString(category: GoalCategory?): String? = category?.name
    
    @TypeConverter
    fun stringToCategory(value: String?): GoalCategory? = 
        value?.let { GoalCategory.valueOf(it) }
    
    // RecurrencePattern converters
    @TypeConverter
    fun recurrenceToString(pattern: RecurrencePattern?): String? = pattern?.name
    
    @TypeConverter
    fun stringToRecurrence(value: String?): RecurrencePattern? = 
        value?.let { RecurrencePattern.valueOf(it) }
}

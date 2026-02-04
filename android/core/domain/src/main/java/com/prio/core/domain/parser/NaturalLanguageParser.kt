package com.prio.core.domain.parser

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of natural language parsing.
 */
data class ParsedTask(
    val title: String,
    val dueDate: Instant? = null,
    val dueTime: String? = null,
    val isUrgent: Boolean = false,
    val keywords: List<String> = emptyList()
)

/**
 * Natural language parser for task input.
 * 
 * Extracts:
 * - Task title (main action)
 * - Due date (temporal expressions)
 * - Time (specific times)
 * - Urgency indicators
 * 
 * Works offline with rule-based parsing.
 */
interface NaturalLanguageParser {
    /**
     * Parse natural language input into structured task data.
     * 
     * @param input Raw user input (text or voice transcription)
     * @return Parsed task data
     */
    suspend fun parse(input: String): ParsedTask
}

/**
 * Rule-based implementation of NaturalLanguageParser.
 * 
 * Per 3.1.5 requirements:
 * - Works offline
 * - <500ms parsing time
 * - Extracts temporal expressions
 */
@Singleton
class RuleBasedNaturalLanguageParser @Inject constructor(
    private val clock: Clock
) : NaturalLanguageParser {
    
    // Temporal patterns
    private val todayPattern = Regex("\\b(today)\\b", RegexOption.IGNORE_CASE)
    private val tomorrowPattern = Regex("\\b(tomorrow)\\b", RegexOption.IGNORE_CASE)
    private val nextWeekPattern = Regex("\\b(next\\s+week)\\b", RegexOption.IGNORE_CASE)
    private val thisWeekPattern = Regex("\\b(this\\s+week)\\b", RegexOption.IGNORE_CASE)
    private val thisWeekendPattern = Regex("\\b(this\\s+weekend)\\b", RegexOption.IGNORE_CASE)
    private val nextMonthPattern = Regex("\\b(next\\s+month)\\b", RegexOption.IGNORE_CASE)
    
    // Day of week patterns
    private val mondayPattern = Regex("\\b(monday|mon)\\b", RegexOption.IGNORE_CASE)
    private val tuesdayPattern = Regex("\\b(tuesday|tue|tues)\\b", RegexOption.IGNORE_CASE)
    private val wednesdayPattern = Regex("\\b(wednesday|wed)\\b", RegexOption.IGNORE_CASE)
    private val thursdayPattern = Regex("\\b(thursday|thu|thurs)\\b", RegexOption.IGNORE_CASE)
    private val fridayPattern = Regex("\\b(friday|fri)\\b", RegexOption.IGNORE_CASE)
    private val saturdayPattern = Regex("\\b(saturday|sat)\\b", RegexOption.IGNORE_CASE)
    private val sundayPattern = Regex("\\b(sunday|sun)\\b", RegexOption.IGNORE_CASE)
    
    // Time patterns
    private val timePattern = Regex("\\b(at\\s+)?(\\d{1,2})(:\\d{2})?\\s*(am|pm)?\\b", RegexOption.IGNORE_CASE)
    private val morningPattern = Regex("\\b(morning|in the morning)\\b", RegexOption.IGNORE_CASE)
    private val afternoonPattern = Regex("\\b(afternoon|in the afternoon)\\b", RegexOption.IGNORE_CASE)
    private val eveningPattern = Regex("\\b(evening|tonight|in the evening)\\b", RegexOption.IGNORE_CASE)
    private val eodPattern = Regex("\\b(end of day|eod)\\b", RegexOption.IGNORE_CASE)
    
    // Relative patterns
    private val inDaysPattern = Regex("\\bin\\s+(\\d+)\\s+days?\\b", RegexOption.IGNORE_CASE)
    private val inWeeksPattern = Regex("\\bin\\s+(\\d+)\\s+weeks?\\b", RegexOption.IGNORE_CASE)
    private val inHoursPattern = Regex("\\bin\\s+(\\d+)\\s+hours?\\b", RegexOption.IGNORE_CASE)
    
    // Urgency patterns
    private val urgentPattern = Regex("\\b(urgent|asap|immediately|critical|emergency|now)\\b", RegexOption.IGNORE_CASE)
    private val byPattern = Regex("\\b(by|before|due|deadline)\\s+", RegexOption.IGNORE_CASE)
    
    override suspend fun parse(input: String): ParsedTask {
        val trimmedInput = input.trim()
        var workingInput = trimmedInput
        var extractedDate: Instant? = null
        var extractedTime: String? = null
        val keywords = mutableListOf<String>()
        
        val now = clock.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        // Extract urgency
        val isUrgent = urgentPattern.containsMatchIn(workingInput)
        if (isUrgent) {
            keywords.add("urgent")
        }
        
        // Extract and remove temporal expressions
        when {
            todayPattern.containsMatchIn(workingInput) -> {
                extractedDate = today.atTime(17, 0).toInstant(TimeZone.currentSystemDefault())
                workingInput = todayPattern.replace(workingInput, "")
            }
            tomorrowPattern.containsMatchIn(workingInput) -> {
                val tomorrow = today.plus(DatePeriod(days = 1))
                extractedDate = tomorrow.atTime(9, 0).toInstant(TimeZone.currentSystemDefault())
                workingInput = tomorrowPattern.replace(workingInput, "")
            }
            nextWeekPattern.containsMatchIn(workingInput) -> {
                val nextWeek = today.plus(DatePeriod(days = 7))
                extractedDate = nextWeek.atTime(9, 0).toInstant(TimeZone.currentSystemDefault())
                workingInput = nextWeekPattern.replace(workingInput, "")
            }
            thisWeekPattern.containsMatchIn(workingInput) -> {
                // This week = Friday
                val daysUntilFriday = (5 - today.dayOfWeek.ordinal + 7) % 7
                val friday = today.plus(DatePeriod(days = if (daysUntilFriday == 0) 7 else daysUntilFriday))
                extractedDate = friday.atTime(17, 0).toInstant(TimeZone.currentSystemDefault())
                workingInput = thisWeekPattern.replace(workingInput, "")
            }
            thisWeekendPattern.containsMatchIn(workingInput) -> {
                // Weekend = Saturday
                val daysUntilSaturday = (6 - today.dayOfWeek.ordinal + 7) % 7
                val saturday = today.plus(DatePeriod(days = if (daysUntilSaturday == 0) 7 else daysUntilSaturday))
                extractedDate = saturday.atTime(10, 0).toInstant(TimeZone.currentSystemDefault())
                workingInput = thisWeekendPattern.replace(workingInput, "")
            }
            nextMonthPattern.containsMatchIn(workingInput) -> {
                val nextMonth = today.plus(DatePeriod(months = 1))
                extractedDate = nextMonth.atTime(9, 0).toInstant(TimeZone.currentSystemDefault())
                workingInput = nextMonthPattern.replace(workingInput, "")
            }
        }
        
        // Extract day of week
        val dayPatterns = listOf(
            mondayPattern to kotlinx.datetime.DayOfWeek.MONDAY,
            tuesdayPattern to kotlinx.datetime.DayOfWeek.TUESDAY,
            wednesdayPattern to kotlinx.datetime.DayOfWeek.WEDNESDAY,
            thursdayPattern to kotlinx.datetime.DayOfWeek.THURSDAY,
            fridayPattern to kotlinx.datetime.DayOfWeek.FRIDAY,
            saturdayPattern to kotlinx.datetime.DayOfWeek.SATURDAY,
            sundayPattern to kotlinx.datetime.DayOfWeek.SUNDAY
        )
        
        for ((pattern, targetDay) in dayPatterns) {
            if (pattern.containsMatchIn(workingInput) && extractedDate == null) {
                val daysUntil = (targetDay.ordinal - today.dayOfWeek.ordinal + 7) % 7
                val targetDate = today.plus(DatePeriod(days = if (daysUntil == 0) 7 else daysUntil))
                extractedDate = targetDate.atTime(9, 0).toInstant(TimeZone.currentSystemDefault())
                workingInput = pattern.replace(workingInput, "")
                break
            }
        }
        
        // Extract specific time
        val timeMatch = timePattern.find(workingInput)
        if (timeMatch != null && extractedDate != null) {
            val hour = timeMatch.groupValues[2].toIntOrNull() ?: 9
            val minutes = timeMatch.groupValues[3].removePrefix(":").toIntOrNull() ?: 0
            val ampm = timeMatch.groupValues[4].lowercase()
            
            val adjustedHour = when {
                ampm == "pm" && hour < 12 -> hour + 12
                ampm == "am" && hour == 12 -> 0
                ampm.isEmpty() && hour < 8 -> hour + 12 // Assume PM for low hours without AM/PM
                else -> hour
            }
            
            val dateTime = extractedDate.toLocalDateTime(TimeZone.currentSystemDefault())
            extractedDate = LocalDate(dateTime.year, dateTime.month, dateTime.dayOfMonth)
                .atTime(adjustedHour, minutes)
                .toInstant(TimeZone.currentSystemDefault())
            extractedTime = "${adjustedHour % 12}:${minutes.toString().padStart(2, '0')} ${if (adjustedHour >= 12) "PM" else "AM"}"
            workingInput = workingInput.replace(timeMatch.value, "")
        }
        
        // Extract time of day
        when {
            morningPattern.containsMatchIn(workingInput) && extractedDate != null -> {
                extractedTime = "9:00 AM"
                workingInput = morningPattern.replace(workingInput, "")
            }
            afternoonPattern.containsMatchIn(workingInput) && extractedDate != null -> {
                extractedTime = "2:00 PM"
                workingInput = afternoonPattern.replace(workingInput, "")
            }
            eveningPattern.containsMatchIn(workingInput) && extractedDate != null -> {
                extractedTime = "6:00 PM"
                workingInput = eveningPattern.replace(workingInput, "")
            }
            eodPattern.containsMatchIn(workingInput) -> {
                if (extractedDate == null) {
                    extractedDate = today.atTime(17, 0).toInstant(TimeZone.currentSystemDefault())
                }
                extractedTime = "5:00 PM"
                workingInput = eodPattern.replace(workingInput, "")
            }
        }
        
        // Extract relative days/weeks
        inDaysPattern.find(workingInput)?.let { match ->
            val days = match.groupValues[1].toIntOrNull() ?: 1
            val targetDate = today.plus(DatePeriod(days = days))
            extractedDate = targetDate.atTime(9, 0).toInstant(TimeZone.currentSystemDefault())
            workingInput = workingInput.replace(match.value, "")
        }
        
        inWeeksPattern.find(workingInput)?.let { match ->
            val weeks = match.groupValues[1].toIntOrNull() ?: 1
            val targetDate = today.plus(DatePeriod(days = weeks * 7))
            extractedDate = targetDate.atTime(9, 0).toInstant(TimeZone.currentSystemDefault())
            workingInput = workingInput.replace(match.value, "")
        }
        
        // Clean up "by", "due", etc.
        workingInput = byPattern.replace(workingInput, "")
        workingInput = urgentPattern.replace(workingInput, "")
        
        // Clean up title: remove extra spaces and common filler words at start
        val title = workingInput
            .replace(Regex("\\s+"), " ")
            .trim()
            .replaceFirst(Regex("^(i need to|i have to|i want to|i should|need to|have to|want to|should|please|remind me to)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        
        return ParsedTask(
            title = title.ifBlank { trimmedInput },
            dueDate = extractedDate,
            dueTime = extractedTime,
            isUrgent = isUrgent,
            keywords = keywords
        )
    }
}

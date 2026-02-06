package com.prio.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prio.app.feature.briefing.BriefingGenerator
import com.prio.app.feature.briefing.GoalSpotlightData
import com.prio.app.feature.briefing.MorningBriefingData
import com.prio.app.feature.briefing.SchedulePreviewItem
import com.prio.app.feature.briefing.TopPriorityItem
import com.prio.core.common.model.EisenhowerQuadrant
import com.prio.core.data.preferences.UserPreferencesRepository
import com.prio.core.data.repository.GoalRepository
import com.prio.core.data.repository.MeetingRepository
import com.prio.core.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Today / Dashboard screen (task 3.4.6).
 *
 * Replaces the hardcoded placeholder data in TodayScreen with real
 * data from repositories and BriefingGenerator.
 *
 * Per 1.1.5 Today Dashboard & Briefing Spec:
 * - Eisenhower Quick View (live quadrant counts)
 * - Today's Top Priorities (Q1/Q2 tasks due today)
 * - Goal Progress (active goals with %)
 * - Upcoming Events (today's meetings)
 * - Briefing card (morning/evening based on time of day)
 */
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository,
    private val meetingRepository: MeetingRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val briefingGenerator: BriefingGenerator,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _effect = Channel<TodayEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadDashboard()
    }

    fun onEvent(event: TodayEvent) {
        when (event) {
            TodayEvent.OnRefresh -> loadDashboard()
            TodayEvent.OnBriefingCardTap -> {
                viewModelScope.launch {
                    val hour = clock.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault()).hour
                    if (hour < 15) {
                        _effect.send(TodayEffect.NavigateToMorningBriefing)
                    } else {
                        _effect.send(TodayEffect.NavigateToEveningSummary)
                    }
                }
            }
            is TodayEvent.OnTaskTap -> {
                viewModelScope.launch {
                    _effect.send(TodayEffect.NavigateToTask(event.taskId))
                }
            }
            is TodayEvent.OnGoalTap -> {
                viewModelScope.launch {
                    _effect.send(TodayEffect.NavigateToGoal(event.goalId))
                }
            }
            is TodayEvent.OnMeetingTap -> {
                viewModelScope.launch {
                    _effect.send(TodayEffect.NavigateToMeeting(event.meetingId))
                }
            }
            TodayEvent.OnViewAllTasks -> {
                viewModelScope.launch {
                    _effect.send(TodayEffect.NavigateToTasks)
                }
            }
            TodayEvent.OnQuadrantTap -> {
                viewModelScope.launch {
                    _effect.send(TodayEffect.NavigateToTasks)
                }
            }
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val timeZone = TimeZone.currentSystemDefault()
                val now = clock.now()
                val today = now.toLocalDateTime(timeZone).date
                val todayStart = today.atStartOfDayIn(timeZone)
                val tomorrowStart = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)

                // Load all active tasks
                val activeTasks = taskRepository.getAllActiveTasksSync()

                // Quadrant counts
                val quadrantCounts = QuadrantCountsUi(
                    doFirst = activeTasks.count { it.quadrant == EisenhowerQuadrant.DO_FIRST },
                    schedule = activeTasks.count { it.quadrant == EisenhowerQuadrant.SCHEDULE },
                    delegate = activeTasks.count { it.quadrant == EisenhowerQuadrant.DELEGATE },
                    eliminate = activeTasks.count { it.quadrant == EisenhowerQuadrant.ELIMINATE }
                )

                // Top priorities: Q1/Q2 tasks due today, sorted by urgency
                val todayTasks = activeTasks.filter { task ->
                    task.dueDate?.let { it >= todayStart && it < tomorrowStart } ?: false
                }.sortedByDescending { it.urgencyScore }

                val topPriorities = todayTasks.take(3).map { task ->
                    TopPriorityUi(
                        id = task.id,
                        title = task.title,
                        quadrant = task.quadrant,
                        dueTime = task.dueDate?.let { dueDate ->
                            val local = dueDate.toLocalDateTime(timeZone)
                            if (local.hour > 0 || local.minute > 0) {
                                val period = if (local.hour < 12) "AM" else "PM"
                                val displayHour = when {
                                    local.hour == 0 -> 12
                                    local.hour > 12 -> local.hour - 12
                                    else -> local.hour
                                }
                                "$displayHour:${local.minute.toString().padStart(2, '0')} $period"
                            } else null
                        }
                    )
                }

                // Goals
                val activeGoals = goalRepository.getAllActiveGoalsSync()
                val goalSummaries = activeGoals.take(4).map { goal ->
                    GoalSummaryUi(
                        id = goal.id,
                        title = goal.title,
                        progress = goal.progress / 100f
                    )
                }

                // Meetings
                val meetings = meetingRepository.getMeetingsForDateSync(todayStart, tomorrowStart)
                val upcomingEvents = meetings.take(5).map { meeting ->
                    UpcomingEventUi(
                        id = meeting.id,
                        title = meeting.title,
                        time = meeting.startTime.toLocalDateTime(timeZone).let { local ->
                            val period = if (local.hour < 12) "AM" else "PM"
                            val displayHour = when {
                                local.hour == 0 -> 12
                                local.hour > 12 -> local.hour - 12
                                else -> local.hour
                            }
                            "$displayHour:${local.minute.toString().padStart(2, '0')} $period"
                        }
                    )
                }

                // Briefing card info
                val hour = now.toLocalDateTime(timeZone).hour
                val overdueTasks = activeTasks.filter { task ->
                    task.dueDate?.let { it < todayStart } ?: false
                }
                val briefingSubtitle = buildString {
                    val urgentCount = todayTasks.count { it.quadrant == EisenhowerQuadrant.DO_FIRST }
                    if (urgentCount > 0) append("$urgentCount urgent task${if (urgentCount > 1) "s" else ""}")
                    if (overdueTasks.isNotEmpty()) {
                        if (isNotEmpty()) append(", ")
                        append("${overdueTasks.size} overdue")
                    }
                    if (meetings.isNotEmpty()) {
                        if (isNotEmpty()) append(", ")
                        append("${meetings.size} meeting${if (meetings.size > 1) "s" else ""}")
                    }
                    if (isEmpty()) append("Tap to view your briefing")
                }

                val userName = try {
                    userPreferencesRepository.userName.first()
                } catch (_: Exception) { null }

                _uiState.update {
                    TodayUiState(
                        isLoading = false,
                        greeting = buildGreeting(hour, userName),
                        briefingSubtitle = briefingSubtitle,
                        isMorning = hour < 15,
                        quadrantCounts = quadrantCounts,
                        topPriorities = topPriorities,
                        goalSummaries = goalSummaries,
                        upcomingEvents = upcomingEvents,
                        totalTodayTasks = todayTasks.size,
                        overdueTasks = overdueTasks.size
                    )
                }

                Timber.d("TodayVM: Dashboard loaded (${todayTasks.size} tasks, ${meetings.size} meetings)")
            } catch (e: Exception) {
                Timber.e(e, "TodayVM: Failed to load dashboard")
                _uiState.update {
                    it.copy(isLoading = false, error = "Unable to load dashboard")
                }
            }
        }
    }

    private fun buildGreeting(hour: Int, name: String?): String {
        val timeGreeting = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Hello"
        }
        return if (!name.isNullOrBlank()) "$timeGreeting, $name" else timeGreeting
    }
}

// ==================== UI State ====================

data class TodayUiState(
    val isLoading: Boolean = true,
    val greeting: String = "Good morning",
    val briefingSubtitle: String = "",
    val isMorning: Boolean = true,
    val quadrantCounts: QuadrantCountsUi = QuadrantCountsUi(),
    val topPriorities: List<TopPriorityUi> = emptyList(),
    val goalSummaries: List<GoalSummaryUi> = emptyList(),
    val upcomingEvents: List<UpcomingEventUi> = emptyList(),
    val totalTodayTasks: Int = 0,
    val overdueTasks: Int = 0,
    val error: String? = null
)

data class QuadrantCountsUi(
    val doFirst: Int = 0,
    val schedule: Int = 0,
    val delegate: Int = 0,
    val eliminate: Int = 0
)

data class TopPriorityUi(
    val id: Long,
    val title: String,
    val quadrant: EisenhowerQuadrant,
    val dueTime: String? = null
)

data class GoalSummaryUi(
    val id: Long,
    val title: String,
    val progress: Float
)

data class UpcomingEventUi(
    val id: Long,
    val title: String,
    val time: String
)

// ==================== Events ====================

sealed interface TodayEvent {
    data object OnRefresh : TodayEvent
    data object OnBriefingCardTap : TodayEvent
    data class OnTaskTap(val taskId: Long) : TodayEvent
    data class OnGoalTap(val goalId: Long) : TodayEvent
    data class OnMeetingTap(val meetingId: Long) : TodayEvent
    data object OnViewAllTasks : TodayEvent
    data object OnQuadrantTap : TodayEvent
}

// ==================== Effects ====================

sealed interface TodayEffect {
    data class NavigateToTask(val taskId: Long) : TodayEffect
    data class NavigateToGoal(val goalId: Long) : TodayEffect
    data class NavigateToMeeting(val meetingId: Long) : TodayEffect
    data object NavigateToTasks : TodayEffect
    data object NavigateToMorningBriefing : TodayEffect
    data object NavigateToEveningSummary : TodayEffect
}

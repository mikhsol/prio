package com.prio.app.feature.meeting

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.prio.core.data.local.entity.ActionItem
import com.prio.core.data.local.entity.MeetingEntity
import com.prio.core.data.repository.MeetingRepository
import com.prio.core.data.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [MeetingDetailViewModel].
 *
 * Covers:
 * - 3.3.3: Meeting detail loading
 * - 3.3.4: Notes editing and auto-save
 * - 3.3.5: Action item extraction (rule-based)
 * - 3.3.6: Agenda editing
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("MeetingDetailViewModel")
class MeetingDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val meetingRepository: MeetingRepository = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)

    private val fixedNow = Instant.parse("2026-04-15T14:30:00Z")
    private val testClock = object : Clock {
        override fun now(): Instant = fixedNow
    }

    private val testMeeting = MeetingEntity(
        id = 1L,
        title = "Sprint Planning",
        description = "Plan next sprint",
        location = "Room B",
        startTime = Instant.parse("2026-04-15T14:00:00Z"),
        endTime = Instant.parse("2026-04-15T15:00:00Z"),
        attendees = "Alice,Bob,Charlie",
        notes = "Action: review PRs\nTodo: update docs @Bob",
        actionItems = null,
        agenda = "1. Review\n2. Plan\n3. Assign",
        createdAt = fixedNow,
        updatedAt = fixedNow,
    )

    private lateinit var viewModel: MeetingDetailViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { meetingRepository.getMeetingByIdFlow(1L) } returns flowOf(testMeeting)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(meetingId: Long = 1L) {
        val savedState = SavedStateHandle(mapOf("meetingId" to meetingId))
        viewModel = MeetingDetailViewModel(savedState, meetingRepository, taskRepository, testClock)
    }

    // ==================== Meeting Detail (3.3.3) ====================

    @Nested
    @DisplayName("Meeting Detail Loading")
    inner class MeetingDetailLoading {

        @Test
        @DisplayName("loads meeting details on init")
        fun loadsMeetingDetails() = runTest {
            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals("Sprint Planning", state.title)
            assertEquals("Room B", state.location)
            assertEquals(3, state.attendees.size)
            assertTrue(state.attendees.contains("Alice"))
        }

        @Test
        @DisplayName("displays formatted time and duration")
        fun displaysFormattedTime() = runTest {
            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.durationFormatted.contains("1h") || state.durationFormatted.contains("60m"))
            assertTrue(state.startTimeFormatted.isNotEmpty())
            assertTrue(state.endTimeFormatted.isNotEmpty())
        }

        @Test
        @DisplayName("handles missing meeting gracefully")
        fun handlesMissingMeeting() = runTest {
            every { meetingRepository.getMeetingByIdFlow(99L) } returns flowOf(null)

            createViewModel(meetingId = 99L)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals("", viewModel.uiState.value.title)
        }
    }

    // ==================== Notes (3.3.4) ====================

    @Nested
    @DisplayName("Notes Editing")
    inner class NotesEditing {

        @Test
        @DisplayName("toggles notes editing mode")
        fun togglesNotesEditing() = runTest {
            createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isNotesEditing)

            viewModel.onEvent(MeetingDetailEvent.OnToggleNotesEdit)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isNotesEditing)

            viewModel.onEvent(MeetingDetailEvent.OnToggleNotesEdit)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isNotesEditing)
        }

        @Test
        @DisplayName("updates notes text in state")
        fun updatesNotesText() = runTest {
            createViewModel()
            advanceUntilIdle()

            // Enter editing mode first so repo flow doesn't overwrite
            viewModel.onEvent(MeetingDetailEvent.OnToggleNotesEdit)
            viewModel.onEvent(MeetingDetailEvent.OnNotesChanged("New notes content"))
            // Don't advanceUntilIdle — that triggers auto-save debounce

            assertEquals("New notes content", viewModel.uiState.value.notes)
            // After changing notes but before auto-save fires, notesSaved should be false
            assertFalse(viewModel.uiState.value.notesSaved)
        }

        @Test
        @DisplayName("saves notes to repository")
        fun savesNotesToRepository() = runTest {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(MeetingDetailEvent.OnSaveNotes)
            advanceUntilIdle()

            coVerify { meetingRepository.updateNotes(1L, any()) }
        }
    }

    // ==================== Action Items (3.3.5) ====================

    @Nested
    @DisplayName("Action Item Extraction")
    inner class ActionItemExtraction {

        @Test
        @DisplayName("extracts action items from notes text (rule-based)")
        fun extractsActionItemsRuleBased() {
            val text = """
                Action: Review PRs before merge
                Todo: Update documentation @Bob
                - Send meeting summary to team
                Regular note line
                Follow up: Schedule next planning session
            """.trimIndent()

            val items = MeetingDetailViewModel.extractActionItemsFromText(text)

            assertTrue(items.isNotEmpty(), "Should extract at least 1 action item")
            assertTrue(
                items.any { it.description.contains("Review PRs") },
                "Should find 'Action:' prefix",
            )
            assertTrue(
                items.any { it.assignee == "Bob" },
                "Should extract @Bob assignee",
            )
        }

        @Test
        @DisplayName("returns empty list for notes without action verbs")
        fun returnsEmptyForNoActionItems() {
            val text = "Just a regular meeting note.\nNothing to do here."
            val items = MeetingDetailViewModel.extractActionItemsFromText(text)
            assertTrue(items.isEmpty())
        }

        @Test
        @DisplayName("handles bullet items with action verbs")
        fun handlesBulletItems() {
            val text = """
                - Send report to management
                - Review budget proposal
                * Email client about timeline
                - This is not an action
            """.trimIndent()

            val items = MeetingDetailViewModel.extractActionItemsFromText(text)
            assertTrue(items.size >= 3, "Should extract bullet action items")
        }

        @Test
        @DisplayName("shows snackbar when notes are empty")
        fun showsSnackbarForEmptyNotes() = runTest {
            val emptyMeeting = testMeeting.copy(notes = "")
            every { meetingRepository.getMeetingByIdFlow(1L) } returns flowOf(emptyMeeting)

            createViewModel()
            advanceUntilIdle()

            viewModel.effect.test {
                viewModel.onEvent(MeetingDetailEvent.OnExtractActionItems)
                advanceUntilIdle()

                val effect = awaitItem()
                assertTrue(effect is MeetingDetailEffect.ShowSnackbar)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("toggles action item completion")
        fun togglesActionItemComplete() = runTest {
            val items = listOf(ActionItem("Do something", isCompleted = false))
            coEvery { meetingRepository.getActionItems(1L) } returns items

            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(MeetingDetailEvent.OnToggleActionItemComplete(0))
            advanceUntilIdle()

            coVerify { meetingRepository.updateActionItems(1L, any()) }
        }

        @Test
        @DisplayName("converts action item to task")
        fun convertsActionItemToTask() = runTest {
            val items = listOf(ActionItem("Write docs", assignee = "Alice"))
            coEvery { meetingRepository.getActionItems(1L) } returns items
            coEvery { taskRepository.createTask(any(), notes = any()) } returns 42L

            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(MeetingDetailEvent.OnConvertActionItemToTask(0))
            advanceUntilIdle()

            coVerify { taskRepository.createTask("Write docs", notes = any()) }
            coVerify { meetingRepository.linkActionItemToTask(1L, 0, 42L) }
        }
    }

    // ==================== Agenda (3.3.6) ====================

    @Nested
    @DisplayName("Agenda Editing")
    inner class AgendaEditing {

        @Test
        @DisplayName("toggles agenda editing mode")
        fun togglesAgendaEditing() = runTest {
            createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isAgendaEditing)

            viewModel.onEvent(MeetingDetailEvent.OnToggleAgendaEdit)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isAgendaEditing)
        }

        @Test
        @DisplayName("saves agenda to repository")
        fun savesAgendaToRepository() = runTest {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(MeetingDetailEvent.OnSaveAgenda)
            advanceUntilIdle()

            coVerify { meetingRepository.updateAgenda(1L, any()) }
        }

        @Test
        @DisplayName("loads existing agenda from meeting")
        fun loadsExistingAgenda() = runTest {
            createViewModel()
            advanceUntilIdle()

            assertEquals("1. Review\n2. Plan\n3. Assign", viewModel.uiState.value.agenda)
        }
    }
}

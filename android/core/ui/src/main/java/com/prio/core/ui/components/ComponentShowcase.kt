package com.prio.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prio.core.ui.theme.PrioTheme

/**
 * Component Showcase Screen
 * 
 * Displays all Prio design system components in all their states.
 * Used for design review and development reference.
 * 
 * Per 2.3.12: Preview composables for all components in all states.
 * 
 * @param onBackClick Called when user taps back button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentShowcase(
    onBackClick: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    PrioTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = { 
                        Text("🎨 Component Showcase")
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { innerPadding ->
            ComponentShowcaseContent(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

/**
 * Component Showcase Content
 * 
 * The scrollable content displaying all components.
 */
@Composable
private fun ComponentShowcaseContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ===== QUADRANT BADGES =====
        SectionHeader("QuadrantBadge")
        
        Text("Compact (24dp)", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Quadrant.entries.forEach { quadrant ->
                QuadrantBadge(quadrant = quadrant, size = BadgeSize.COMPACT)
            }
        }
        
        Text("Standard (28dp)", style = MaterialTheme.typography.labelMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Quadrant.entries.forEach { quadrant ->
                QuadrantBadge(quadrant = quadrant, size = BadgeSize.STANDARD)
            }
        }
        
        Text("Large (48dp) - Tappable", style = MaterialTheme.typography.labelMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Quadrant.entries.forEach { quadrant ->
                QuadrantBadge(quadrant = quadrant, size = BadgeSize.LARGE, onTap = {})
            }
        }
        
        SectionDivider()
        
        // ===== TASK CARDS =====
        SectionHeader("TaskCard")
        
        Text("Default State", style = MaterialTheme.typography.labelMedium)
        TaskCard(
            task = TaskCardData(
                id = "1",
                title = "Submit quarterly report",
                quadrant = Quadrant.DO_FIRST,
                dueText = "Today",
                goalName = "Career Growth"
            ),
            onTap = {},
            onCheckboxTap = {}
        )
        
        Text("Overdue State (red left border)", style = MaterialTheme.typography.labelMedium)
        TaskCard(
            task = TaskCardData(
                id = "2",
                title = "Call dentist about appointment",
                quadrant = Quadrant.DO_FIRST,
                        isOverdue = true,
                        dueText = "Yesterday"
                    ),
                    onTap = {},
                    onCheckboxTap = {}
                )
                
                Text("Completed State (strikethrough, 60% opacity)", style = MaterialTheme.typography.labelMedium)
                TaskCard(
                    task = TaskCardData(
                        id = "3",
                        title = "Send email to team",
                        quadrant = Quadrant.DELEGATE,
                        isCompleted = true
                    ),
                    onTap = {},
                    onCheckboxTap = {}
                )
                
                Text("Selected State", style = MaterialTheme.typography.labelMedium)
                TaskCard(
                    task = TaskCardData(
                        id = "4",
                        title = "Review PRs",
                        quadrant = Quadrant.SCHEDULE
                    ),
                    onTap = {},
                    onCheckboxTap = {},
                    isSelected = true
                )
                
                Text("All Quadrants", style = MaterialTheme.typography.labelMedium)
                Quadrant.entries.forEach { quadrant ->
                    TaskCard(
                        task = TaskCardData(
                            id = quadrant.name,
                            title = "Task with ${quadrant.label} priority",
                            quadrant = quadrant,
                            dueText = "Tomorrow"
                        ),
                        onTap = {},
                        onCheckboxTap = {}
                    )
                }
                
                SectionDivider()
                
                // ===== GOAL CARDS =====
                SectionHeader("GoalCard")
                
                Text("On Track (Green)", style = MaterialTheme.typography.labelMedium)
                GoalCard(
                    goal = GoalCardData(
                        id = "1",
                        title = "Get promoted to Senior PM",
                        category = GoalCategory.CAREER,
                        progress = 0.65f,
                        status = GoalStatus.ON_TRACK,
                        targetDate = "Jun 2026",
                        milestonesCompleted = 5,
                        milestonesTotal = 12,
                        linkedTasksCount = 3
                    ),
                    onTap = {},
                    onOverflowTap = {}
                )
                
                Text("Slightly Behind (Yellow)", style = MaterialTheme.typography.labelMedium)
                GoalCard(
                    goal = GoalCardData(
                        id = "2",
                        title = "Run a half marathon",
                        category = GoalCategory.HEALTH,
                        progress = 0.35f,
                        status = GoalStatus.SLIGHTLY_BEHIND,
                        targetDate = "May 2026"
                    ),
                    onTap = {}
                )
                
                Text("At Risk (Red)", style = MaterialTheme.typography.labelMedium)
                GoalCard(
                    goal = GoalCardData(
                        id = "3",
                        title = "Save $10,000 emergency fund",
                        category = GoalCategory.FINANCIAL,
                        progress = 0.15f,
                        status = GoalStatus.AT_RISK,
                        targetDate = "Dec 2026"
                    ),
                    onTap = {}
                )
                
                Text("All Categories", style = MaterialTheme.typography.labelMedium)
                GoalCategory.entries.forEach { category ->
                    GoalCard(
                        goal = GoalCardData(
                            id = category.name,
                            title = "Goal in ${category.label} category",
                            category = category,
                            progress = 0.5f,
                            status = GoalStatus.ON_TRACK
                        ),
                        onTap = {},
                        showMetadata = false
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                SectionDivider()
                
                // ===== MEETING CARDS =====
                SectionHeader("MeetingCard")
                
                Text("Ongoing Meeting", style = MaterialTheme.typography.labelMedium)
                MeetingCard(
                    meeting = MeetingCardData(
                        id = "1",
                        title = "Weekly Team Standup",
                        startTime = "9:00 AM",
                        endTime = "9:30 AM",
                        attendeesCount = 8,
                        actionItemsCount = 3,
                        hasNotes = true,
                        isOngoing = true
                    ),
                    onTap = {}
                )
                
                Text("Standard Meeting", style = MaterialTheme.typography.labelMedium)
                MeetingCard(
                    meeting = MeetingCardData(
                        id = "2",
                        title = "Product Strategy Review with Leadership",
                        startTime = "2:00 PM",
                        endTime = "3:00 PM",
                        attendeesCount = 5,
                        calendarColor = Color(0xFF3B82F6)
                    ),
                    onTap = {}
                )
                
                Text("With Notes", style = MaterialTheme.typography.labelMedium)
                MeetingCard(
                    meeting = MeetingCardData(
                        id = "3",
                        title = "1:1 with Manager",
                        startTime = "4:00 PM",
                        endTime = "4:30 PM",
                        attendeesCount = 2,
                        hasNotes = true,
                        calendarColor = Color(0xFF10B981)
                    ),
                    onTap = {}
                )
                
                SectionDivider()
                
                // ===== BRIEFING CARDS =====
                SectionHeader("BriefingCard")
                
                Text("Morning Briefing", style = MaterialTheme.typography.labelMedium)
                BriefingCard(
                    briefing = BriefingCardData(
                        type = BriefingType.MORNING,
                        date = "Tuesday, February 4",
                        summaryText = "Good morning! You have 5 tasks today with 3 in DO FIRST.",
                        ctaLabel = "Start My Day →"
                    ),
                    onCtaTap = {}
                )
                
                Text("Evening Summary", style = MaterialTheme.typography.labelMedium)
                BriefingCard(
                    briefing = BriefingCardData(
                        type = BriefingType.EVENING,
                        date = "Tuesday, February 4",
                        summaryText = "Great work today! You completed 4 of 5 tasks (80%).",
                        ctaLabel = "Close My Day"
                    ),
                    onCtaTap = {}
                )
                
                SectionDivider()
                
                // ===== TEXT FIELD =====
                SectionHeader("PrioTextField")
                
                var text by remember { mutableStateOf("") }
                
                Text("Empty State", style = MaterialTheme.typography.labelMedium)
                PrioTextField(
                    value = "",
                    onValueChange = {},
                    onVoiceInput = {}
                )
                
                Text("With Text", style = MaterialTheme.typography.labelMedium)
                PrioTextField(
                    value = "Call dentist tomorrow",
                    onValueChange = {},
                    onVoiceInput = {}
                )
                
                Text("AI Processing", style = MaterialTheme.typography.labelMedium)
                PrioTextField(
                    value = "Submit report urgent",
                    onValueChange = {},
                    aiState = AiProcessingState.PROCESSING,
                    onVoiceInput = {}
                )
                
                Text("Voice Recording", style = MaterialTheme.typography.labelMedium)
                PrioTextField(
                    value = "",
                    onValueChange = {},
                    isVoiceRecording = true,
                    onVoiceInput = {},
                    onVoiceStop = {}
                )
                
                SectionDivider()
                
                // ===== EMPTY STATES =====
                SectionHeader("EmptyState")
                
                Text("Tasks Empty", style = MaterialTheme.typography.labelMedium)
                EmptyState(type = EmptyStateType.TASKS, onCtaTap = {})
                
                Text("All Done", style = MaterialTheme.typography.labelMedium)
                EmptyState(type = EmptyStateType.TASKS_ALL_DONE, onCtaTap = {})
                
                Text("Goals Empty", style = MaterialTheme.typography.labelMedium)
                EmptyState(type = EmptyStateType.GOALS, onCtaTap = {})
                
                SectionDivider()
                
                // ===== ERROR STATES =====
                SectionHeader("ErrorState")
                
                Text("Database Error", style = MaterialTheme.typography.labelMedium)
                ErrorState(
                    type = ErrorStateType.DATABASE_READ,
                    onPrimaryAction = {},
                    onSecondaryAction = {}
                )
                
                Text("AI Error", style = MaterialTheme.typography.labelMedium)
                ErrorState(
                    type = ErrorStateType.AI_MODEL_LOAD,
                    onPrimaryAction = {},
                    onSecondaryAction = {}
                )
                
                Spacer(modifier = Modifier.height(100.dp))
    }
}

@Preview(showBackground = true, heightDp = 3000)
@Composable
fun ComponentShowcasePreview() {
    ComponentShowcase()
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(modifier = Modifier.height(16.dp))
}

// ===== Dark Mode Preview =====

@Preview(showBackground = true, heightDp = 2000, backgroundColor = 0xFF1F2937)
@Composable
fun ComponentShowcaseDarkPreview() {
    PrioTheme(darkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SectionHeader("Dark Mode Components")
                
                // Task Cards
                TaskCard(
                    task = TaskCardData(
                        id = "1",
                        title = "Submit quarterly report",
                        quadrant = Quadrant.DO_FIRST,
                        dueText = "Today"
                    ),
                    onTap = {},
                    onCheckboxTap = {}
                )
                
                TaskCard(
                    task = TaskCardData(
                        id = "2",
                        title = "Overdue task",
                        quadrant = Quadrant.DO_FIRST,
                        isOverdue = true
                    ),
                    onTap = {},
                    onCheckboxTap = {}
                )
                
                // Goal Card
                GoalCard(
                    goal = GoalCardData(
                        id = "1",
                        title = "Career Growth Goal",
                        category = GoalCategory.CAREER,
                        progress = 0.65f,
                        status = GoalStatus.ON_TRACK
                    ),
                    onTap = {}
                )
                
                // Meeting Card
                MeetingCard(
                    meeting = MeetingCardData(
                        id = "1",
                        title = "Team Meeting",
                        startTime = "9:00 AM",
                        endTime = "10:00 AM",
                        isOngoing = true
                    ),
                    onTap = {}
                )
                
                // Text Field
                PrioTextField(
                    value = "Dark mode input",
                    onValueChange = {},
                    onVoiceInput = {}
                )
                
                // Empty State
                EmptyState(type = EmptyStateType.TASKS, onCtaTap = {})
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

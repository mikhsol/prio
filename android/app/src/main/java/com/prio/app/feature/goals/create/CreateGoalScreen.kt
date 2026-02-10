package com.prio.app.feature.goals.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prio.core.common.model.GoalCategory

/**
 * Create Goal Wizard Screen.
 *
 * Task 3.2.3: 3-step wizard per 1.1.4 Goals Screens Spec:
 * 1. Describe: Natural language input with examples
 * 2. AI SMART Refinement: AI-powered SMART breakdown + category selection
 * 3. Timeline & Milestones: Target date + milestones (AI-suggested + custom)
 *
 * Design:
 * - Progress bar at top showing step 1/2/3
 * - AnimatedContent for step transitions
 * - Material 3 styling consistent with design system
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGoalDetail: (Long) -> Unit,
    onNavigateToGoalsList: () -> Unit,
    onShowQuickCapture: () -> Unit = {},
    viewModel: CreateGoalViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect one-time effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateGoalEffect.NavigateBack -> onNavigateBack()
                is CreateGoalEffect.NavigateToGoalDetail -> onNavigateToGoalDetail(effect.goalId)
                is CreateGoalEffect.NavigateToGoalsList -> onNavigateToGoalsList()
                is CreateGoalEffect.OpenQuickCapture -> {
                    // Navigate to goal detail first, then open quick capture
                    state.createdGoalId?.let { goalId ->
                        onNavigateToGoalDetail(goalId)
                    }
                    onShowQuickCapture()
                }
                is CreateGoalEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is CreateGoalEffect.ShowCelebration -> {
                    // Celebration handled by UI state (showCelebration)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Goal",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(CreateGoalEvent.OnBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.ime
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Step progress indicator
            StepProgressBar(
                currentStep = state.currentStep,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Wizard content with animated transitions
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    val direction = if (targetState.number > initialState.number) 1 else -1
                    (slideInHorizontally { fullWidth -> direction * fullWidth } + fadeIn(
                        tween(300)
                    )).togetherWith(
                        slideOutHorizontally { fullWidth -> -direction * fullWidth } + fadeOut(
                            tween(300)
                        )
                    )
                },
                label = "WizardStep",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    CreateGoalStep.DESCRIBE -> DescribeStepContent(
                        state = state,
                        onEvent = viewModel::onEvent,
                        modifier = Modifier.fillMaxSize()
                    )

                    CreateGoalStep.AI_SMART -> AiSmartStepContent(
                        state = state,
                        onEvent = viewModel::onEvent,
                        modifier = Modifier.fillMaxSize()
                    )

                    CreateGoalStep.TIMELINE -> TimelineStepContent(
                        state = state,
                        onEvent = viewModel::onEvent,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Celebration overlay
    if (state.showCelebration && state.createdGoalId != null) {
        CelebrationOverlay(
            onAddFirstTask = { viewModel.onEvent(CreateGoalEvent.OnAddFirstTask) },
            onViewDetails = { viewModel.onEvent(CreateGoalEvent.OnViewDetails) },
            onBackToGoals = { viewModel.onEvent(CreateGoalEvent.OnBackToGoals) }
        )
    }
}

// =====================================================
// Step Progress Bar
// =====================================================

@Composable
private fun StepProgressBar(
    currentStep: CreateGoalStep,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = currentStep.number / 3f,
        animationSpec = tween(durationMillis = 400),
        label = "StepProgress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Step labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CreateGoalStep.entries.forEach { step ->
                val isActive = step.number <= currentStep.number
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        if (step.number < currentStep.number) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = "${step.number}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isActive) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (step != CreateGoalStep.entries.last()) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            strokeCap = StrokeCap.Round,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// =====================================================
// Step 1: Describe Your Goal
// =====================================================

@Composable
private fun DescribeStepContent(
    state: CreateGoalUiState,
    onEvent: (CreateGoalEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .imePadding()
    ) {
        // Header
        Text(
            text = "🎯",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "What do you want to achieve?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Describe your goal in your own words — our AI will help you make it SMART.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Input field
        OutlinedTextField(
            value = state.goalInput,
            onValueChange = { onEvent(CreateGoalEvent.OnGoalInputChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("My goal is to…") },
            placeholder = { Text("e.g., Get promoted to Senior Engineer by December") },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Example goals
        Text(
            text = "💡 Need inspiration?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        state.inputExamples.forEach { example ->
            ExampleGoalChip(
                text = example,
                isSelected = state.goalInput == example,
                onClick = { onEvent(CreateGoalEvent.OnExampleSelect(example)) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Next button
        Button(
            onClick = { onEvent(CreateGoalEvent.OnNextFromDescribe) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.goalInput.isNotBlank(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refine with AI")
        }
    }
}

@Composable
private fun ExampleGoalChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = if (!isSelected) FontStyle.Italic else FontStyle.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

// =====================================================
// Step 2: AI SMART Refinement
// =====================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiSmartStepContent(
    state: CreateGoalUiState,
    onEvent: (CreateGoalEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .imePadding()
    ) {
        if (state.isAiProcessing) {
            // AI Processing state
            AiProcessingCard()
        } else {
            // Refined goal title
            Text(
                text = "✨ SMART Goal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.refinedGoal,
                onValueChange = { onEvent(CreateGoalEvent.OnRefinedGoalChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Goal Title") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SMART breakdown (read-only display if AI provided, editable if skipped)
            if (!state.aiSkipped && state.smartSpecific.isNotBlank()) {
                SmartBreakdownCard(state)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Category selection
            Text(
                text = "📂 Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GoalCategory.entries.forEach { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { onEvent(CreateGoalEvent.OnCategorySelect(category)) },
                        label = { Text("${category.emoji} ${category.displayName}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onEvent(CreateGoalEvent.OnPreviousStep) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = { onEvent(CreateGoalEvent.OnNextFromSmart) },
                    modifier = Modifier.weight(1f),
                    enabled = state.refinedGoal.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Next: Timeline")
                }
            }

            if (!state.aiSkipped) {
                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { onEvent(CreateGoalEvent.OnSkipAi) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Skip AI — I'll write my own",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AiProcessingCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "✨ AI is analyzing your goal…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Making it Specific, Measurable, Achievable, Relevant & Time-bound",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SmartBreakdownCard(
    state: CreateGoalUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SMART Breakdown",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            SmartRow(label = "📌 Specific", value = state.smartSpecific)
            SmartRow(label = "📊 Measurable", value = state.smartMeasurable)
            SmartRow(label = "✅ Achievable", value = state.smartAchievable)
            SmartRow(label = "🎯 Relevant", value = state.smartRelevant)
            SmartRow(label = "⏰ Time-bound", value = state.smartTimeBound)
        }
    }
}

@Composable
private fun SmartRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    if (value.isBlank()) return

    Column(modifier = modifier.padding(bottom = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =====================================================
// Step 3: Timeline & Milestones
// =====================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineStepContent(
    state: CreateGoalUiState,
    onEvent: (CreateGoalEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var milestoneInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .imePadding()
    ) {
        // Target date
        Text(
            text = "📅 Target Date",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "When do you want to achieve this goal?",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.targetDateText.isNotBlank()) state.targetDateText
                else "Select target date (optional)"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Milestones section
        Text(
            text = "🏁 Milestones",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Break your goal into smaller checkpoints (max 5)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Existing milestones
        state.milestones.forEachIndexed { index, milestone ->
            MilestoneChipRow(
                milestone = milestone,
                onRemove = { onEvent(CreateGoalEvent.OnRemoveMilestone(index)) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Add milestone input
        if (state.milestones.size < 5) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = milestoneInput,
                    onValueChange = { milestoneInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { testTag = "milestone_input" },
                    placeholder = { Text("Add a milestone…") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (milestoneInput.isNotBlank()) {
                            onEvent(CreateGoalEvent.OnAddMilestone(milestoneInput))
                            milestoneInput = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add milestone",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Weekly reminder toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🔔 Weekly Check-in Reminder",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Get reminded to review your progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.enableWeeklyReminder,
                onCheckedChange = { onEvent(CreateGoalEvent.OnToggleWeeklyReminder(it)) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onEvent(CreateGoalEvent.OnPreviousStep) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back")
            }

            Button(
                onClick = { onEvent(CreateGoalEvent.OnCreateGoal) },
                modifier = Modifier.weight(1f),
                enabled = !state.isLoading && state.canCreateGoal,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Create Goal 🎯")
            }
        }

        // Error message
        state.error?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.targetDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onEvent(CreateGoalEvent.OnTargetDateSelect(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun MilestoneChipRow(
    milestone: MilestoneInput,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = if (milestone.isAiSuggested) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .background(
                if (milestone.isAiSuggested) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (milestone.isAiSuggested) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "AI Suggested",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Text(
            text = milestone.title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =====================================================
// Celebration Overlay
// =====================================================

@Composable
private fun CelebrationOverlay(
    onAddFirstTask: () -> Unit,
    onViewDetails: () -> Unit,
    onBackToGoals: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            .clickable(enabled = false) { /* consume clicks */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                Text(
                    text = "🎉",
                    style = MaterialTheme.typography.displayMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Goal Created!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Great start! Add your first task to make progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onAddFirstTask,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("➕ Add First Task")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("View Goal Details")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onBackToGoals,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Goals")
                }
            }
        }
    }
}

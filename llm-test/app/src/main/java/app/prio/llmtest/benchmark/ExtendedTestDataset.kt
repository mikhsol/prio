package app.prio.llmtest.benchmark

import app.prio.llmtest.engine.EisenhowerQuadrant

/**
 * Extended test dataset with 50 diverse tasks for comprehensive benchmarking.
 * 
 * Milestone 0.2.6 Task 0.2.6.5: Benchmark optimized prompts on 50 diverse tasks
 * 
 * Distribution: 12-13 tasks per quadrant covering various edge cases:
 * - Clear-cut cases (obvious classification)
 * - Edge cases (ambiguous signals)
 * - Domain variety (work, personal, health, admin)
 * - Temporal signals (deadlines, urgency words)
 */
object ExtendedTestDataset {
    
    val TEST_CASES_50: List<TestCase> = listOf(
        // =====================================================
        // DO (Urgent + Important) - 13 cases
        // =====================================================
        TestCase(
            id = 1,
            task = "Server is down, customers cannot access the app",
            groundTruth = EisenhowerQuadrant.DO,
            rationale = "Active crisis affecting customers - immediate action required"
        ),
        TestCase(
            id = 2,
            task = "Critical production bug causing data loss",
            groundTruth = EisenhowerQuadrant.DO,
            rationale = "Data loss is critical - requires immediate fix"
        ),
        TestCase(
            id = 3,
            task = "Tax filing deadline is tomorrow",
            groundTruth = EisenhowerQuadrant.DO,
            rationale = "Legal deadline within 24 hours with consequences"
        ),
        TestCase(
            id = 4,
            task = "Board presentation starts in 2 hours",
            groundTruth = EisenhowerQuadrant.DO,
            rationale = "Imminent important meeting with executives"
        ),
        TestCase(
            id = 5,
            task = "VIP customer support ticket marked urgent",
            groundTruth = EisenhowerQuadrant.DO,
            rationale = "VIP customer issue requires immediate attention"
        ),
        TestCase(
            id = 6,
            task = "Child's school called - needs to be picked up sick",
            groundTruth = EisenhowerQuadrant.DO,
            rationale = "Family emergency requiring immediate response"
        ),
        TestCase(
            id = 7,
            task = "Contract expires in 24 hours, needs signature",
            groundTruth = EisenhowerQuadrant.DO,
            rationale = "Legal deadline with business impact"
        ),
        TestCase(
            id = 8,
            task = "Major investor meeting in 30 minutes, deck not ready",
            groundTruth = EisenhowerQuadrant.DO,
            rationale = "Imminent high-stakes meeting"
        ),
        TestCase(
            id = 9,
            task = "Security breach detected - unauthorized access",
            groundTruth = EisenhowerQuadrant.DO,
            rationale = "Security incident requires immediate response"
        ),
        TestCase(
            id = 10,
            task = "Payment processing failing, customers can't checkout",
            groundTruth = EisenhowerQuadrant.DO,
            rationale = "Revenue-affecting system failure"
        ),
        TestCase(
            id = 11,
            task = "Flight leaves in 3 hours, haven't packed",
            groundTruth = EisenhowerQuadrant.DO,
            rationale = "Imminent travel deadline"
        ),
        TestCase(
            id = 12,
            task = "Quarterly report due by end of day to the CEO",
            groundTruth = EisenhowerQuadrant.DO,
            rationale = "Executive deadline today"
        ),
        TestCase(
            id = 13,
            task = "Website SSL certificate expires in 2 hours",
            groundTruth = EisenhowerQuadrant.DO,
            rationale = "Imminent technical failure affecting all users"
        ),
        
        // =====================================================
        // SCHEDULE (Important + Not Urgent) - 13 cases
        // =====================================================
        TestCase(
            id = 14,
            task = "Plan next quarter's marketing strategy",
            groundTruth = EisenhowerQuadrant.SCHEDULE,
            rationale = "Strategic planning without immediate deadline"
        ),
        TestCase(
            id = 15,
            task = "Read leadership book for career growth",
            groundTruth = EisenhowerQuadrant.SCHEDULE,
            rationale = "Personal development without deadline"
        ),
        TestCase(
            id = 16,
            task = "Schedule annual health checkup",
            groundTruth = EisenhowerQuadrant.SCHEDULE,
            rationale = "Health maintenance - important but flexible timing"
        ),
        TestCase(
            id = 17,
            task = "Research new project management tools for team",
            groundTruth = EisenhowerQuadrant.SCHEDULE,
            rationale = "Team improvement initiative without deadline"
        ),
        TestCase(
            id = 18,
            task = "Write documentation for the new feature",
            groundTruth = EisenhowerQuadrant.SCHEDULE,
            rationale = "Quality improvement without stated deadline"
        ),
        TestCase(
            id = 19,
            task = "Set up retirement savings account",
            groundTruth = EisenhowerQuadrant.SCHEDULE,
            rationale = "Financial planning - important for future"
        ),
        TestCase(
            id = 20,
            task = "Learn a new programming language for career advancement",
            groundTruth = EisenhowerQuadrant.SCHEDULE,
            rationale = "Skill development for long-term growth"
        ),
        TestCase(
            id = 21,
            task = "Create a personal budget spreadsheet",
            groundTruth = EisenhowerQuadrant.SCHEDULE,
            rationale = "Financial planning - important but not urgent"
        ),
        TestCase(
            id = 22,
            task = "Review and update the team's coding standards",
            groundTruth = EisenhowerQuadrant.SCHEDULE,
            rationale = "Process improvement without deadline"
        ),
        TestCase(
            id = 23,
            task = "Build relationships with new team members",
            groundTruth = EisenhowerQuadrant.SCHEDULE,
            rationale = "Relationship building - important for team cohesion"
        ),
        TestCase(
            id = 24,
            task = "Create a 5-year career development plan",
            groundTruth = EisenhowerQuadrant.SCHEDULE,
            rationale = "Long-term planning without immediate pressure"
        ),
        TestCase(
            id = 25,
            task = "Refactor legacy code to improve maintainability",
            groundTruth = EisenhowerQuadrant.SCHEDULE,
            rationale = "Technical debt reduction - important but not urgent"
        ),
        TestCase(
            id = 26,
            task = "Start an exercise routine for better health",
            groundTruth = EisenhowerQuadrant.SCHEDULE,
            rationale = "Health improvement - important for wellbeing"
        ),
        
        // =====================================================
        // DELEGATE (Urgent + Not Important) - 12 cases
        // =====================================================
        TestCase(
            id = 27,
            task = "Respond to routine HR survey by end of day",
            groundTruth = EisenhowerQuadrant.DELEGATE,
            rationale = "Has deadline but low personal value"
        ),
        TestCase(
            id = 28,
            task = "Order office supplies that are running low",
            groundTruth = EisenhowerQuadrant.DELEGATE,
            rationale = "Administrative task - could be handled by others"
        ),
        TestCase(
            id = 29,
            task = "Schedule team's vacation calendar for next month",
            groundTruth = EisenhowerQuadrant.DELEGATE,
            rationale = "Coordination task suitable for admin delegation"
        ),
        TestCase(
            id = 30,
            task = "Answer phone call about meeting room booking",
            groundTruth = EisenhowerQuadrant.DELEGATE,
            rationale = "Interruption that doesn't require senior attention"
        ),
        TestCase(
            id = 31,
            task = "Compile weekly status report from team updates",
            groundTruth = EisenhowerQuadrant.DELEGATE,
            rationale = "Routine aggregation task"
        ),
        TestCase(
            id = 32,
            task = "Book travel arrangements for upcoming conference",
            groundTruth = EisenhowerQuadrant.DELEGATE,
            rationale = "Logistics task suitable for delegation"
        ),
        TestCase(
            id = 33,
            task = "Update team contact list in company directory",
            groundTruth = EisenhowerQuadrant.DELEGATE,
            rationale = "Administrative update task"
        ),
        TestCase(
            id = 34,
            task = "Print and distribute meeting agenda for tomorrow",
            groundTruth = EisenhowerQuadrant.DELEGATE,
            rationale = "Routine prep task with deadline"
        ),
        TestCase(
            id = 35,
            task = "Respond to sales cold call asking for decision maker",
            groundTruth = EisenhowerQuadrant.DELEGATE,
            rationale = "Interruption with no strategic value"
        ),
        TestCase(
            id = 36,
            task = "Fill out expense report for last month's travel",
            groundTruth = EisenhowerQuadrant.DELEGATE,
            rationale = "Routine admin task with deadline"
        ),
        TestCase(
            id = 37,
            task = "Fix broken link on internal wiki page",
            groundTruth = EisenhowerQuadrant.DELEGATE,
            rationale = "Minor fix that could be handled by anyone"
        ),
        TestCase(
            id = 38,
            task = "Schedule recurring team sync meetings",
            groundTruth = EisenhowerQuadrant.DELEGATE,
            rationale = "Calendar coordination task"
        ),
        
        // =====================================================
        // ELIMINATE (Not Urgent + Not Important) - 12 cases
        // =====================================================
        TestCase(
            id = 39,
            task = "Browse social media during lunch break",
            groundTruth = EisenhowerQuadrant.ELIMINATE,
            rationale = "Time-wasting activity with no value"
        ),
        TestCase(
            id = 40,
            task = "Reorganize email folders for the third time this month",
            groundTruth = EisenhowerQuadrant.ELIMINATE,
            rationale = "Busy work with diminishing returns"
        ),
        TestCase(
            id = 41,
            task = "Watch YouTube videos about productivity tips",
            groundTruth = EisenhowerQuadrant.ELIMINATE,
            rationale = "Procrastination disguised as learning"
        ),
        TestCase(
            id = 42,
            task = "Attend optional company picnic planning meeting",
            groundTruth = EisenhowerQuadrant.ELIMINATE,
            rationale = "Optional social activity with no strategic value"
        ),
        TestCase(
            id = 43,
            task = "Clean up old desktop files (no deadline)",
            groundTruth = EisenhowerQuadrant.ELIMINATE,
            rationale = "Nice-to-have with no real impact"
        ),
        TestCase(
            id = 44,
            task = "Read random news articles about celebrities",
            groundTruth = EisenhowerQuadrant.ELIMINATE,
            rationale = "Entertainment with no value"
        ),
        TestCase(
            id = 45,
            task = "Check email for the fifth time this hour",
            groundTruth = EisenhowerQuadrant.ELIMINATE,
            rationale = "Compulsive checking, time waster"
        ),
        TestCase(
            id = 46,
            task = "Customize IDE theme colors again",
            groundTruth = EisenhowerQuadrant.ELIMINATE,
            rationale = "Procrastination through customization"
        ),
        TestCase(
            id = 47,
            task = "Debate endlessly in Slack about code formatting",
            groundTruth = EisenhowerQuadrant.ELIMINATE,
            rationale = "Bikeshedding - no real value"
        ),
        TestCase(
            id = 48,
            task = "Watch competitor's marketing video out of curiosity",
            groundTruth = EisenhowerQuadrant.ELIMINATE,
            rationale = "Curiosity without actionable intent"
        ),
        TestCase(
            id = 49,
            task = "Rearrange apps on phone home screen",
            groundTruth = EisenhowerQuadrant.ELIMINATE,
            rationale = "Trivial activity with no impact"
        ),
        TestCase(
            id = 50,
            task = "Join optional virtual happy hour",
            groundTruth = EisenhowerQuadrant.ELIMINATE,
            rationale = "Optional social activity - nice but not important"
        )
    )
    
    /**
     * Original 20-case test set for comparison with 0.2.3 baseline.
     */
    val TEST_CASES_20: List<TestCase> = TEST_CASES_50.take(20)
    
    /**
     * Get test cases grouped by quadrant for analysis.
     */
    fun getByQuadrant(): Map<EisenhowerQuadrant, List<TestCase>> {
        return TEST_CASES_50.groupBy { it.groundTruth }
    }
    
    /**
     * Get edge cases that are particularly challenging.
     */
    val EDGE_CASES: List<TestCase> = listOf(
        // Tasks with mixed signals
        TestCase(
            id = 101,
            task = "Attend optional but useful training session tomorrow",
            groundTruth = EisenhowerQuadrant.SCHEDULE, // Important for growth, optional = not urgent
            rationale = "Edge case: 'optional' suggests not urgent, but 'useful training' is important"
        ),
        TestCase(
            id = 102,
            task = "Reply to team lead's email about project update",
            groundTruth = EisenhowerQuadrant.DO, // Team lead implies some urgency/importance
            rationale = "Edge case: No explicit deadline but team lead implies priority"
        ),
        TestCase(
            id = 103,
            task = "Review pull request from junior developer",
            groundTruth = EisenhowerQuadrant.DELEGATE, // Could be done by other seniors
            rationale = "Edge case: Has implicit urgency (blocking someone) but could be delegated"
        ),
        TestCase(
            id = 104,
            task = "Update LinkedIn profile when you have time",
            groundTruth = EisenhowerQuadrant.ELIMINATE, // 'when you have time' = low priority
            rationale = "Edge case: Career-related but 'when you have time' signals low priority"
        ),
        TestCase(
            id = 105,
            task = "Prepare for next week's performance review",
            groundTruth = EisenhowerQuadrant.SCHEDULE, // Important for career, next week = not urgent now
            rationale = "Edge case: Important for career but not due immediately"
        )
    )
}

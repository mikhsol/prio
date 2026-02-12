## Ideas for future.
2. Agent specific to specific goal.
3. Personal/Work/etc... 
4. Identify duplicates.
5. Go through all reports and collect list of next steps/items which should be implemented later.
6. Tags for tasks/goals.
7. Pomodoro/flowtime.
8. Limit number of DO_IT tasks to 3.
9. Limint number of In Progress tasks to X.
10. Reasses tasks if it's more than Y in lists (do in background, suggest to user).
11. Notifications on/off in some time period.
12. Set up notification to specific interval (5-mins every hour/etc...)
13. Productivity recommendations.
14. Generate Videos on how to use app. Learn about marketing part of it (releases/how often/how to make it useful).
15. Beautiful animation on task/goal completion.
16. Time tracking.
17. iOS/Desktop app.
18. Completed tasks archived not deleted. Yearly/monthly/weekly summary on achieved tasks/goals, reflections on missed deadlines weekly.
19. 200 test cases for each topic (work/health/etc..) to classify tasks, educate classifier.
20. Export goals/tasks.
21. Soft delete/archive tasks/goals.
23. Delegate task to agent.
24. Email integration, suggest response.
25. Sync up notes from Notion.
26. Telegram/WhatsApp mini app/bot -> Send message to todo.
27. Separate (isolated) personal to work spaces/user profies?
28. Integration with other apps.
29. Spell checker and auto formatting of the text fields.
30. Just random notes which can be imported to task. Or creation task from clipboard.
31. T-shape task size & [RETHINK] progress update accordingly. Goal -> Milestone -> Task
                                                          -> Task
    Progress for goal -> Progress for each milestones.
32. Design MONETISATION model!!!
33. Add donation in crypto screen.

## Bugs:

* [FIXED][GOAL] Can't undo goal deletion.
* [FIXED][GOAL] Refine with AI not working. Router short-circuited on rule-based failure for SUGGEST_SMART_GOAL without trying LLM; OnDeviceAiProvider was missing a SUGGEST_SMART_GOAL handler.
* [FIXED][GOAL] Complete task -> goal progress not updated
* [FIXED][GOAL] No complete goal button.

* [FIXED][TASK] Create task -> "Task created" caption at the botton don't disappears

* [FIXED][TASK] No time picker when choose date.
* [FIXED][CALENDAR] Month/Week view missing.

* [FIXED][GOAL] Adding milestone (more than 2), new milestone hidden under keyboard and you don't see what are you typing.
* [FIXED][TASK] Voice creation not working. "Getting ready..." appears on the screen but nothing happens.

* [FIXED][GOAL/TASK] Create goal -> Add first task -> You see the last task created (title, priority...), not a clean placeholder.
* [FIXED][GOAL] Ugly UI to change goal for task. Replaced AlertDialog with rich Material 3 ModalBottomSheet: category emoji, title, category + progress subtitle, helper text, and proper empty state.

* [TASK] Recurrent tasks not implemented.
* [TASK] Reminder not implemented.

* [APP] Swipe left while task creation -> leaving app, not closing create task dialog. How to cancel task creation? Left swipe will create it. Is it good UX?
* [TASK] Create task -> Edit -> You can create task by swipe left, but same time can complete it.

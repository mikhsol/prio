## Ideas for future.
5. Go through all reports and collect list of next steps/items which should be implemented later.
32. Design MONETISATION model!!!
17. iOS/Desktop app.
33. Add donation in crypto screen.

35. [GENERAL FEATURE] Gemini nano only AI refinement. 
8. [GENERAL FEATURE] Limit number of DO_IT tasks to 3 - you can't have a lot urgent and important tasks.
9. [GENERAL FEATURE] Limint number of In Progress tasks to X days.
11. [GEENERAL FEATURE] Notifications on/off in some time period.
15. [GEENERAL FEATURE] Beautiful animation on task/goal completion.
19. [GEENERAL FEATURE] 200 test cases for each topic (work/health/etc..) to classify tasks, educate classifier.
29. [GENERAL FEATURE] Spell checker and auto formatting of the text fields.
30. [GENERAL FEATURE] Just random notes which can be imported to task. Or creation task from clipboard.

12. [???PAID/GENERAL FEATURE]Set up notification to specific interval (5-mins every hour/etc...)

36. [PAID FEATURE] On-device AI like phi3, llama4, etc... models.
10. [PAID FEATURE] Reasses tasks if it's more than Y days in lists (do in background, suggest to user).
2. [PAID FEATURE] Agent specific to specific goal, additional prompt refining by user
3. [PAID FEATURE] Personal/Work/etc... 
4. [PAID FEATURE] Identify duplicates.
6. [PAID FEATURE] Tags for tasks/goals.
7. [PAID FEATURE] Pomodoro/flowtime.
13. [PAID FEATURE] Productivity recommendations.
34. [PAID FEATURE] Backup/restore from back up (google drive, dropbox, locally on device (PRO); on prio storage in cloud(PRO+))
16. [PAID FEATURE] Time tracking.
18. [PAID FEATURE] Completed tasks archived not deleted. Yearly/monthly/weekly summary on achieved tasks/goals, reflections on missed deadlines weekly. Soft delete/archive tasks/goals.
23. [PAID FEATURE] Delegate task to agent.
24. [PAID FEATURE] Email integration, suggest response.
25. [PAID FEATURE] Sync up notes from Notion.
26. [PAID FEATURE] Telegram/WhatsApp mini app/bot -> Send message to todo.
27. [PAID FEATURE] Separate (isolated) personal to work spaces/user profies?
28. [PAID FEATURE] Integration with other apps (Teams/Slack, etc..., think about it).
31. [PAID FEATURE] T-shape task size & [RETHINK] progress update accordingly. Goal -> Milestone -> Task
                                                          -> Task
    Progress for goal -> Progress for each milestones.


14. [MARKETING] Generate Videos on how to use app. Learn about marketing part of it (releases/how often/how to make it useful).


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

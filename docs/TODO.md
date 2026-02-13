# TODO

## BUGS

### TO VERIFY
* [FIXED][GOAL] Refine with AI not working. Router short-circuited on rule-based failure for SUGGEST_SMART_GOAL without trying LLM; OnDeviceAiProvider was missing a SUGGEST_SMART_GOAL handler.
* [FIXED][GOAL] Complete task -> goal progress not updated
* [FIXED][GOAL] No complete goal button.
* [FIXED][TASK] Create task -> "Task created" caption at the botton don't disappears
* [FIXED][TASK] No time picker when choose date.
* [FIXED][CALENDAR] Month/Week view missing.
* [FIXED][GOAL] Adding milestone (more than 2), new milestone hidden under keyboard and you don't see what are you typing.
* [FIXED][TASK] Voice creation not working. "Getting ready..." appears on the screen but nothing happens.
* [FIXED][TASK] Voice creation permission never requested. shouldShowRationale=false on first install treated as "permanently denied" instead of "never asked".
* [FIXED][GOAL/TASK] Create goal -> Add first task -> You see the last task created (title, priority...), not a clean placeholder.
* [FIXED][GOAL] Ugly UI to change goal for task. Replaced AlertDialog with rich Material 3 ModalBottomSheet: category emoji, title, category + progress subtitle, helper text, and proper empty state.
* [FIXED][CALENDAR] Month/Week view missing.
* [FIXED][CALENDAR] Week view navigation broken. Previous/Next week buttons did not update week view content because navigateWeek() always loaded DAY mode data.

### TO FIX
* [GOAL] Undo goal, snackbar permanently shown on screen. Pressing on the goal do nothing, I can't go to the goal screen. Instead of goal/task deletion implement archive functionality, so it can be viewed later. Add button to show archived goals
* [TASK] Task overview not updated after new task was added.
* [APP] Swipe left while task creation -> leaving app, not closing create task dialog. How to cancel task creation? Left swipe will create it. Is it good UX?
* [TASK] Create task -> Edit -> You can create task by swipe left, but same time can complete it.
* [SETTINGS][AI] Download AI Model button not working
* [SETTINGS][AI] Verify that "Use AI for Classification Toggle" actually works.
* [SETTINGS][AI] Verify that "Reset AI Learning" actually works.
* [SETTINGS][NOTIFICATIONS] Quiet Hours From/TO hardcoded
* [SETTINGS][NOTIFICATIONS] Verify each toggle actually do something
* [SETTINGS][APPEARANCE] Display - can't change formats
* [SETTINGS][CALENDAR SYNC] Lead to Settings screen with profile
* [SETTINGS][CALENDAR SYNC] Profile should be hidden
* [SETTINGS][CALENDAR SYNC] Backup & Export not working
* [SETTINGS][CALENDAR SYNC] Privacy Policy - empty
* [SETTINGS][CALENDAR SYNC] About prio:
    * Remove Website
    * Remove Contact Support
    * Remove Open Source Licenses
* [SETTINGS] Help And Support Lead to About Prio
* [TASK] Voice input fill task partially. It should recognise full phrase, use AI to summarize title and create title and add the actual text into notes.
* [GOAL] NO option to edit suggested milestones. Target date not automatically catched from the Goal if it exists.
* [TASKS] Can't easily move tasks between categories (Do fist, etc...)
* [TODAY]Task overview not udated
[PRODUCTIVITY INSIGHTS] Not updating:
    * Eisenhower AI Accuracy always 100%
    * Quadrant breakdown not updated
* [TASK] Recurrent tasks not implemented.
* [TASK] Reminder not implemented.

## DO NEXT
0. Disable AI Model setting. Use only android embeded geminin nano for first release
0. Go through all reports and collect list of next steps/items which should be implemented later.
0. How migration would be done (app updates via google play)
0. Design MONETISATION model!!!
0. iOS/Desktop app.
0. Add donation in crypto screen.

## FREE FEATURES
0. Protection from prompt injections.
0. Gemini nano only AI refinement. 
0. Limit number of DO_IT tasks to 3 - you can't have a lot urgent and important tasks.
0. Limint number of In Progress tasks to X days.
0. Notifications on/off in some time period.
0. Beautiful animation on task/goal completion.
0. 200 test cases for each topic (work/health/etc..) to classify tasks, educate classifier.
0. Spell checker and auto formatting of the text fields.
0. Just random notes which can be imported to task. Or creation task from clipboard.
0. Asses GOAL on SMARTness and propose improvements.
0. Asses Task, is it defined properly?? Suggest improvement.

## SHOULD BE PAID?
0. Set up notification to specific interval (5-mins every hour/etc...)

## PAID FEATURES
0. Advance analytics (monthly/yearly, weekly can be generated on the general level).
0. On-device AI like phi3, llama4, etc... models.
0. Reasses tasks if it's more than Y days in lists (do in background, suggest to user).
0. Agent specific to specific goal, additional prompt refining by user
0. [BUSINESS FEATURE] Personal/Work/etc... 
0. Identify duplicates.
0. Tags for tasks/goals.
0. Pomodoro/flowtime.
0. Productivity recommendations.
0. Backup/restore from back up (google drive, dropbox, locally on device (PRO); on prio storage in cloud(PRO+))
0. Time tracking.
0. Completed tasks archived not deleted. Yearly/monthly/weekly summary on achieved tasks/goals, reflections on missed deadlines weekly. Soft delete/archive tasks/goals.
0. Delegate task to agent.
0. Email integration, suggest response.
0. Sync up notes from Notion.
0. Telegram/WhatsApp mini app/bot -> Send message to todo.
0. Separate (isolated) personal to work spaces/user profies?
0. Integration with other apps (Teams/Slack, etc..., think about it).
0. T-shape task size & [RETHINK] progress update accordingly. Goal -> Milestone -> Task
                                                          -> Task
    Progress for goal -> Progress for each milestones.
0. Checkin for flights
0. Connect to openclaw
0. Think about finance features?

## MARKETING
0. Generate Videos on how to use app. Learn about marketing part of it (releases/how often/how to make it useful).

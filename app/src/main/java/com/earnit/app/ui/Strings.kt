package com.earnit.app.ui

object Strings {
    const val APP_NAME = "Earn It"

    fun appTitle(nickname: String) = if (nickname.isNotBlank()) "Earn It, $nickname!" else "Earn It!"

    const val ABOUT_TEASER = "the idea, support and feedback options"
    const val ABOUT_THE_IDEA_TITLE = "The idea"
    const val ABOUT_THE_IDEA_BODY =
        "We all have those things we really want but don't \"need\" and a list of dreaded chores. " +
            "EarnIt is here to help you treat yourself guilt-free by crushing those \"ew\" tasks.\n\n" +
            "I kept the app simple: use it alongside your main never-ending to-do list to stay motivated. Use a widget as a reminder and to see your progress. " +
            "I take your privacy seriously, so all your data stays right here on your phone.\n\n" +
            "Enjoy the company of my mascots, Pugsly and Tabby, and if you have a great quote to share, I'd love to hear it!"

    const val NAME_INFO =
        "Your name appears in the greeting at the top of the home screen. " +
            "To generate a fun random nickname, tap the sparkle icon. " +
            "To switch back to a custom name, start typing."

    const val REWARDS_INFO =
        "Going for too many rewards makes it hard to stay focused. " +
            "Try finishing one before adding more."

    const val TASKS_NOTES_INFO =
        "When enabled, every log entry must include a note. " +
            "Useful for tracking and retaining history."

    const val CLEANUP_DISCLAIMER =
        "All clean up actions are permanent. Once deleted, there is no way to recover your data."

    const val MAX_REWARD_BANNER =
        "Max number of in progress rewards reached!"

    const val MASCOT_NONE_LABEL = "None"
    const val MASCOT_SECTION_TITLE = "Mascot"
    const val MASCOT_UNLOCK_ACTION = "Mascots"
    const val MASCOT_PICKER_TITLE = "Show mascot"
    const val MASCOT_SETTINGS_BADGE = "!"

    fun mascotUnlocked(name: String) = "You unlocked $name!"

    const val ADD_TASK_EMPTY = "No tasks yet."
    const val ADD_TASK_CREATE = "Create your own"
    const val ADD_TASK_BROWSE = "browse the Library"

    // CleanUpScreen dialogs
    const val CLEANUP_DIALOG_LOGS_TITLE = "Clear all logs?"
    const val CLEANUP_DIALOG_TASKS_TITLE = "Clear all tasks?"
    const val CLEANUP_DIALOG_REWARDS_TITLE = "Clear all rewards?"
    const val CLEANUP_DIALOG_ALL_TITLE = "Wipe everything?"
    const val CLEANUP_DIALOG_LOGS_BODY = "All logs and claimed reward history will be permanently deleted. Rewards and tasks are not affected."
    const val CLEANUP_DIALOG_TASKS_BODY = "All tasks will be permanently deleted and removed from all rewards."
    const val CLEANUP_DIALOG_REWARDS_BODY = "All active rewards and their logged progress will be permanently deleted."
    const val CLEANUP_DIALOG_ALL_BODY = "This will permanently delete all tasks, rewards, logs, and history. The app will be completely empty."

    // CleanUpScreen cards
    const val CLEANUP_CARD_LOGS = "Removes all logs and claimed reward history. Rewards and tasks stay."
    const val CLEANUP_CARD_TASKS = "Removes all tasks and unlinks them from rewards."
    const val CLEANUP_CARD_REWARDS = "Removes all active rewards and their progress logs."
    const val CLEANUP_CARD_ALL = "Wipes everything — all tasks, rewards, logs, and history."

    // CleanUpScreen buttons and snackbars
    const val CLEANUP_BTN_LOGS = "Clear logs"
    const val CLEANUP_BTN_TASKS = "Clear tasks"
    const val CLEANUP_BTN_REWARDS = "Clear rewards"
    const val CLEANUP_BTN_ALL = "Wipe everything"
    const val CLEANUP_SNACKBAR_LOGS = "Logs cleared"
    const val CLEANUP_SNACKBAR_TASKS = "Tasks cleared"
    const val CLEANUP_SNACKBAR_REWARDS = "Rewards cleared"
    const val CLEANUP_SNACKBAR_ALL = "Everything wiped"

    // About screen
    const val ABOUT_RATE_LABEL = "Rate the app"
    const val ABOUT_RATE_SUBTITLE = "Enjoying EarnIt? A review means a lot."
    const val ABOUT_CONTACT_LABEL = "Get in touch"
    const val ABOUT_CONTACT_SUBTITLE = "Feedback, bugs, or just want to say hi."
    const val ABOUT_CONTACT_EMAIL = "hello@secondmondaystudios.com"
    const val ABOUT_TIP_TITLE = "Support the developer"
    const val ABOUT_TIP_COPY =
        "EarnIt is free and ad-free. If it's been useful, a tip keeps the coffee flowing and the updates coming."
    const val TIP_SUCCESS = "Thank you so much!"
    const val TIP_ERROR = "Something went wrong. Please try again."
    const val TIP_LOAD_ERROR = "Couldn't load tip options. Please try again later."

    // Task Library screen
    const val LIBRARY_TITLE = "Task Library"

    fun libraryTaskCount(n: Int) = "$n tasks"

    fun libraryGroupHint(
        icon: String,
        name: String,
    ) = "Tasks will be added to group: $icon $name"

    fun libraryAddButton(count: Int) = "ADD $count TASKS"

    fun librarySkippedTitle(count: Int) = "$count task${if (count == 1) "" else "s"} skipped"

    fun librarySkippedBody(names: List<String>) = "The following tasks already exist and were not added:\n\n${names.joinToString("\n") { "• $it" }}"

    // Tasks screen
    const val TASKS_EMPTY_TITLE = "No tasks yet"
    const val TASKS_EMPTY_BODY = "Tap + to add your first task\nor browse the Library"
    const val TASKS_GROUP_OTHER = "Other"
    const val TASKS_GROUP_ASSIGN_HINT = "Assign a group in task edit to organise your list"
    const val TASKS_SEARCH_HINT = "Search tasks…"
    const val TASKS_LIBRARY_BTN = "Library"

    // Settings screen — navigation rows
    const val SETTINGS_DATA_TITLE = "Data & Backup"
    const val SETTINGS_DATA_SUBTITLE = "Export and import your data"
    const val SETTINGS_CLEANUP_ROW = "Permanently remove logs, tasks, or rewards"

    // Home screen
    const val HOME_QUOTE_SECTION = "QUOTE OF THE DAY"
    const val HOME_EMPTY_REWARDS = "No rewards yet.\nAdd one, then earn points toward it by completing tasks."
    const val HOME_ADD_TASKS_BTN = "+ ADD TASKS"
    const val LOG_BTN = "+ LOG"
    const val NEW_REWARD_DESC = "New Reward"

    // History screen
    const val HISTORY_TAB_TASKS = "Completed Tasks"
    const val HISTORY_TAB_REWARDS = "Claimed Rewards"
    const val HISTORY_NO_TASKS = "No completed tasks yet."
    const val HISTORY_NO_REWARDS = "No claimed rewards yet."
    const val HISTORY_EARN_AGAIN = "Earn Again"

    fun historySection(count: Int) = "History ($count)"

    // Task edit screen
    const val TASK_EDIT_NEW = "Add task"
    const val TASK_EDIT_EXISTING = "Edit task"
    const val TASK_DELETE_TITLE = "Delete task?"

    fun taskDeleteBody(name: String) = "\"$name\" will be permanently deleted. Logged activity won't be affected."

    const val TASK_NAME_LABEL = "Task name"
    const val TASK_NAME_PLACEHOLDER = "e.g. \"Run 30 minutes\""

    fun taskDuplicateError(name: String) = "A task named \"$name\" already exists."

    const val TASK_GROUP_PLACEHOLDER = "New group..."
    const val TASK_GROUP_OPTIONAL = "Group (optional)"

    fun taskGroupLabel(g: String) = "Group · $g"

    const val TASK_AUTO_POINTS_TOGGLE = "Assign points automatically"
    const val TASK_SLIDER_TIME = "Time"
    const val TASK_SLIDER_DIFFICULTY = "Difficulty"
    const val TASK_SLIDER_PREPARATION = "Preparation"
    const val TASK_POINTS_TOTAL = "Total points:"
    const val TASK_POINTS_LABEL = "Points"

    fun taskUsedIn(rewardName: String) = "Will be added to: $rewardName"

    const val TASK_USE_TO_GET = "Use to get:"
    const val TASK_MANDATORY_DESC = "Required"
    const val TASK_OPTIONAL_DESC = "Optional"
    const val TASK_REPEATABLE_DESC = "Repeatable"
    const val TASK_ONCE_DESC = "Once per cycle"
    const val TASK_SAVED = "Task saved"
    const val TASK_ICON_PICKER_TITLE = "Choose icon"
    const val BACK_DESC = "Back"
    const val DELETE_TASK_DESC = "Delete task"
    const val NEW_TASK_DESC = "New Task"
    const val CLEAR_DESC = "Clear"
    const val TASKS_VIEW_ALL_DESC = "Switch to all tasks view"
    const val TASKS_VIEW_GROUP_DESC = "Switch to group view"

    // Reward edit screen
    const val REWARD_EDIT_NEW = "Add reward"
    const val REWARD_EDIT_EXISTING = "Edit reward"
    const val REWARD_DELETE_TITLE = "Delete reward?"

    fun rewardDeleteBody(name: String) = "\"$name\" will be permanently deleted along with all logged progress toward it."

    const val REWARD_NAME_LABEL = "Reward name"

    fun rewardDuplicateError(name: String) = "A reward named \"$name\" already exists."

    const val REWARD_COST_LABEL = "Point cost"
    const val REWARD_DESC_LABEL = "Description (optional)"
    const val REWARD_DESC_PLACEHOLDER = "What will motivate you to earn this?"
    const val REWARD_TASKS_SECTION = "Tasks"
    const val REWARD_ADD_TASK_BTN = "Add task"
    const val REWARD_MANDATORY_DESC = "Mandatory"
    const val REWARD_OPTIONAL_DESC = "Optional"
    const val REWARD_NOT_REPEATABLE_DESC = "Not repeatable"
    const val REWARD_REPEATABLE_DESC = "Repeatable"
    const val REWARD_SAVED = "Reward saved"
    const val DELETE_REWARD_DESC = "Delete reward"

    // Task detail screen
    const val TASK_DETAIL_POINTS_LABEL = "Points:"
    const val TASK_DETAIL_NO_REWARDS = "Not used in any rewards yet.\nLink it to a reward to start earning points with it."
    const val NO_ACTIVITY = "No activity yet."

    // Reward detail screen
    const val REWARD_DETAIL_CLAIM_BTN = "CLAIM"
    const val REWARD_DETAIL_NO_TASKS = "No tasks added yet.\nAdd tasks below — completing them earns points toward this reward."
    const val REWARD_MANDATORY_TASKS_HINT = "Point goal reached, complete the ★ tasks to claim."

    fun rewardEarnTasksTitle(count: Int) = if (count > 0) "Complete to earn points ($count)" else "Complete to earn points"

    const val REWARD_ROAD_TO_GLORY = "Road to Glory"
    const val REWARD_RECENT_ACTIVITY = "Recent activity"

    // Log / claim dialogs (SharedDialogs.kt)
    const val LOG_DIALOG_TITLE = "Log"
    const val LOG_SEARCH_LABEL = "Search tasks"
    const val DIALOG_LOG_BTN = "LOG"
    const val DIALOG_CANCEL = "CANCEL"

    fun claimDialogTitle(name: String) = "Claim \"$name\""

    fun logNoMatch(q: String) = "No tasks match \"$q\""

    const val LOG_NOTE_LABEL = "Note"
    const val LOG_NOTE_MANDATORY_LABEL = "Note*"
    const val CLAIM_DIALOG_SUBTITLE = "Choose how to handle this reward after claiming."
    const val CLAIM_START_OVER_BTN = "Archive & Start Over"
    const val CLAIM_ARCHIVE_BTN = "Archive Only"
    const val CLAIM_CANCEL = "Cancel"
    const val ADD_TASK_DIALOG_TITLE = "Add task"
    const val ADD_TASK_GROUP_VIEW_DESC = "Show all tasks"
    const val ADD_TASK_ALL_VIEW_DESC = "Show by group"
    const val ADD_TASK_CREATE_BTN = "CREATE NEW TASK"
    const val ADD_TASK_CONFIRM_BTN = "ADD SELECTED"

    // Tasks screen (log dialog strings)
    fun tasksLogTitle(taskName: String) = "Log: $taskName"

    const val TASKS_LOG_SELECT_REWARD = "Select reward:"

    fun tasksLogSingleReward(rewardName: String) = "Reward: $rewardName"

    const val TASKS_LOG_NOTE_LABEL = "Note (optional)"

    // Data screen
    const val DATA_EXPORT_TITLE = "Export backup"
    const val DATA_EXPORT_SUBTITLE = "Save all your data as a JSON file"
    const val DATA_IMPORT_TITLE = "Restore backup"
    const val DATA_IMPORT_SUBTITLE = "Pick a backup file to restore your data"
    const val DATA_EXPORT_SUCCESS = "Backup saved ✓"
    const val DATA_EXPORT_FAIL = "Export failed ✗"
    const val DATA_IMPORT_SUCCESS = "Replaced ✓"
    const val DATA_IMPORT_MERGE_SUCCESS = "Merged ✓"
    const val DATA_IMPORT_FAIL = "Import failed ✗"
    const val IMPORT_ERROR_TOO_LARGE = "File is too large (max 10 MB)"
    const val IMPORT_ERROR_WRONG_TYPE = "This doesn't look like a backup file"
    const val IMPORT_ERROR_INVALID_JSON = "File is not valid JSON"
    const val IMPORT_ERROR_WRONG_SCHEMA = "This doesn't look like an EarnIt backup"
    const val IMPORT_ERROR_UNREADABLE = "Couldn't open the file"

    // Clean Up screen
    const val CLEANUP_SCREEN_TITLE = "Clean Up"

    // Settings screen
    const val SETTINGS_SECTION_ABOUT = "ABOUT"
    const val SETTINGS_SECTION_APPEARANCE = "APPEARANCE"
    const val SETTINGS_SECTION_REWARDS = "REWARD LIMITS"
    const val SETTINGS_SECTION_TASKS = "TASKS"
    const val SETTINGS_SECTION_DATA = "DATA & BACKUP"
    const val SETTINGS_SECTION_CLEANUP = "CLEAN UP"
    const val SETTINGS_NAME_LABEL = "Name"
    const val SETTINGS_NAME_PLACEHOLDER = "you can leave this empty"
    const val SETTINGS_QUOTE_TOGGLE = "Show daily quote"
    const val SETTINGS_OPTIMAL_LABEL = "Optimal"
    const val SETTINGS_MAX_LABEL = "Maximum"
    const val SETTINGS_NOTES_TOGGLE = "Notes required"
    const val SETTINGS_NOTES_DESC = "Notes required"
    const val THEME_WARM_GOLD = "Warm Gold"
    const val THEME_OCEAN_BLUE = "Ocean Blue"
    const val THEME_FOREST = "Forest"

    // Widget activities
    const val WIDGET_TASK_ALL_LOGGED = "All tasks logged!"
    const val WIDGET_TASK_PICKER_HINT = "Tap a task to log it"
    const val WIDGET_NOTE_HINT = "Add a note (optional)"
    const val WIDGET_NOTE_PLACEHOLDER = "What did you do?"
    const val WIDGET_LOG_TASK_BTN = "Log task"
    const val WIDGET_BACK_BTN = "Back"
    const val WIDGET_NOTIF_CHANNEL_NAME = "Widget task logs"

    fun widgetLoggedNotif(pts: Int) = "Logged! +$pts pts"

    const val WIDGET_CONFIG_TITLE = "Choose a reward"
    const val WIDGET_CONFIG_SUBTITLE = "Select which reward this widget will track."
    const val WIDGET_CONFIG_EMPTY = "No active rewards yet.\nCreate one in the app first."
    const val WIDGET_LABEL_TITLE = "Widget label"
    const val WIDGET_LABEL_HINT = "The label shown on your home screen widget. Change it to something neutral if you want privacy."
    const val WIDGET_ADD_BTN = "ADD WIDGET"

    fun widgetRewardName(name: String) = "Reward: $name"

    // Widget (Glance) — EarnItWidget.kt
    // Keep short and single-line. The widget box has a fixed, non-scrolling height;
    // on narrow (e.g. 3-column) layouts a wrapped second line overflows the box and
    // clips the progress bar beneath it (see fix/widget-hint-overflow).
    const val WIDGET_MANDATORY_HINT = "Finish mandatory tasks to claim"
}

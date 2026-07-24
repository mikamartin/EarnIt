package com.earnit.app.widget

/**
 * Glance testTag values shared between EarnItWidget's composables and their tests
 * (WidgetContentTest), so a typo can't silently desync a hasTestTag() matcher from the tag it's
 * meant to find — assertDoesNotExist() would falsely pass instead of failing loudly.
 */
internal object WidgetTestTags {
    const val REWARD_NAME = "widget_reward_name"
    const val MANDATORY_HINT = "widget_mandatory_hint"
    const val ALL_TASKS_LOGGED_HINT = "widget_all_tasks_logged_hint"
    const val CLAIM_BUTTON = "widget_claim_button"
    const val LOG_BUTTON = "widget_log_button"
    const val LOG_BUTTON_DISABLED = "widget_log_button_disabled"
    const val ADD_TASK_BUTTON = "widget_add_task_button"
    const val PROGRESS_CURRENT = "widget_progress_current"
    const val FLASH_CHECK = "widget_flash_check"
    const val FLASH_MESSAGE = "widget_flash_message"
    const val EMPTY_TITLE = "widget_empty_title"
    const val EMPTY_SUBTITLE = "widget_empty_subtitle"
    const val CLAIMED_NAME = "widget_claimed_name"
    const val CLAIMED_SUBTITLE = "widget_claimed_subtitle"
}

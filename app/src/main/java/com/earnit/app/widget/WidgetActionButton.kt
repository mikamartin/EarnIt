package com.earnit.app.widget

import com.earnit.app.data.RewardProgress

enum class WidgetActionButton { CLAIM, LOG, LOG_DISABLED, ADD_TASK }

fun widgetActionButtonFor(progress: RewardProgress): WidgetActionButton =
    when {
        progress.canClaim -> WidgetActionButton.CLAIM
        progress.loggableTasks.isNotEmpty() -> WidgetActionButton.LOG
        progress.allTasks.isEmpty() -> WidgetActionButton.ADD_TASK
        else -> WidgetActionButton.LOG_DISABLED
    }

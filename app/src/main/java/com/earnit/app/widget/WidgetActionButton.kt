package com.earnit.app.widget

import com.earnit.app.data.RewardProgress

enum class WidgetActionButton { CLAIM, LOG, ADD_TASK, NONE }

fun widgetActionButtonFor(progress: RewardProgress): WidgetActionButton {
    val completedIds = progress.activeLogs.map { it.taskId }.toSet()
    val taskRefsMap = progress.taskRefs.associateBy { it.taskId }
    val hasTasks =
        progress.allTasks.any { task ->
            val ref = taskRefsMap[task.id]
            ref?.isRepeatable == true || task.id !in completedIds
        }
    val noTasks = progress.allTasks.isEmpty()
    return when {
        progress.canClaim -> WidgetActionButton.CLAIM
        hasTasks -> WidgetActionButton.LOG
        noTasks -> WidgetActionButton.ADD_TASK
        else -> WidgetActionButton.NONE
    }
}

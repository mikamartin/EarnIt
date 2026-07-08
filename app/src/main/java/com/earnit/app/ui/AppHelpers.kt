package com.earnit.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.earnit.app.data.TaskEntity

/** Standard text style for all primary action and cancel buttons. */
internal val buttonLabelStyle: TextStyle
    @Composable get() =
        MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.ExtraBold,
        )

internal fun android.view.View.hapticTap() = performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

internal fun formatTimestamp(ts: Long): String {
    val currentYear =
        java.util.Calendar
            .getInstance()
            .get(java.util.Calendar.YEAR)
    val entryYear =
        java.util.Calendar
            .getInstance()
            .also { it.timeInMillis = ts }
            .get(java.util.Calendar.YEAR)
    val pattern = if (entryYear != currentYear) "MMM d, yyyy" else "MMM d"
    return java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(java.util.Date(ts))
}

internal fun formatLogTime(ts: Long): String {
    val now = java.util.Calendar.getInstance()
    val cal =
        java.util.Calendar
            .getInstance()
            .also { it.timeInMillis = ts }
    val isToday =
        cal.get(java.util.Calendar.DATE) == now.get(java.util.Calendar.DATE) &&
            cal.get(java.util.Calendar.MONTH) == now.get(java.util.Calendar.MONTH) &&
            cal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR)
    return if (isToday) {
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
    } else {
        java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(ts))
    }
}

internal fun formatDate(ts: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(ts))
}

internal fun TaskEntity.displayPoints(): String = "+${effectivePoints()}"

internal const val NOTE_MAX_CHARS = 200
internal const val REWARD_NAME_MAX_CHARS = 40
internal const val TASK_NAME_MAX_CHARS = 56
internal const val REWARD_DESC_MAX_CHARS = 200

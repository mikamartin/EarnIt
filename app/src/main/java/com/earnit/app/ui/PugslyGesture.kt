package com.earnit.app.ui

// Secret gesture on the Pugsly mascot: 3 quick taps, a deliberate pause, then 4 quick taps.
object PugslyGesture {
    const val GROUP_GAP_MS = 350L
    val PAUSE_MS = 600L..2000L
    const val PATTERN_LENGTH = 7

    fun nextState(
        timestamps: List<Long>,
        now: Long,
    ): List<Long> {
        val n = timestamps.size
        val gapMs = timestamps.lastOrNull()?.let { now - it }
        val validNextTap =
            when {
                n == 0 -> true
                n in 1..2 -> gapMs != null && gapMs <= GROUP_GAP_MS
                n == 3 -> gapMs != null && gapMs in PAUSE_MS
                n in 4..6 -> gapMs != null && gapMs <= GROUP_GAP_MS
                else -> false
            }
        return if (validNextTap) timestamps + now else listOf(now)
    }

    fun isComplete(timestamps: List<Long>): Boolean = timestamps.size == PATTERN_LENGTH
}

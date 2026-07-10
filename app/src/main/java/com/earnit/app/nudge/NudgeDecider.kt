package com.earnit.app.nudge

sealed interface NudgeDecision {
    data object NoOp : NudgeDecision

    data class Reset(
        val newAnchor: Long,
    ) : NudgeDecision

    data class Send(
        val stage: Int,
        val newAnchor: Long,
    ) : NudgeDecision
}

object NudgeDecider {
    const val FIRST_THRESHOLD_HOURS = 48
    const val SECOND_THRESHOLD_HOURS = 96
    const val FIRST_THRESHOLD_MS = FIRST_THRESHOLD_HOURS * 60 * 60 * 1000L
    const val SECOND_THRESHOLD_MS = SECOND_THRESHOLD_HOURS * 60 * 60 * 1000L

    fun decide(
        now: Long,
        lastLogTimestamp: Long?,
        hasActiveReward: Boolean,
        currentStage: Int,
        anchorTimestamp: Long,
    ): NudgeDecision {
        if (lastLogTimestamp == null || !hasActiveReward) return NudgeDecision.NoOp

        // A log landed after the last nudge was recorded — the idle streak broke; start fresh.
        if (currentStage != 0 && lastLogTimestamp != anchorTimestamp) {
            return NudgeDecision.Reset(newAnchor = lastLogTimestamp)
        }

        val idleMs = now - lastLogTimestamp
        return when {
            currentStage == 0 && idleMs >= FIRST_THRESHOLD_MS ->
                NudgeDecision.Send(stage = 1, newAnchor = lastLogTimestamp)
            currentStage == 1 && idleMs >= SECOND_THRESHOLD_MS ->
                NudgeDecision.Send(stage = 2, newAnchor = lastLogTimestamp)
            else -> NudgeDecision.NoOp
        }
    }
}

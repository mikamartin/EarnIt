package com.earnit.app.nudge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NudgeDeciderTest {
    private val hour = 60 * 60 * 1000L
    private val now = 1_000_000_000_000L

    @Test
    fun `never logged returns NoOp`() {
        val decision =
            NudgeDecider.decide(
                now = now,
                lastLogTimestamp = null,
                hasActiveReward = true,
                currentStage = 0,
                anchorTimestamp = 0L,
            )
        assertEquals(NudgeDecision.NoOp, decision)
    }

    @Test
    fun `no active reward returns NoOp even when idle`() {
        val decision =
            NudgeDecider.decide(
                now = now,
                lastLogTimestamp = now - 200 * hour,
                hasActiveReward = false,
                currentStage = 0,
                anchorTimestamp = 0L,
            )
        assertEquals(NudgeDecision.NoOp, decision)
    }

    @Test
    fun `idle under 48h returns NoOp`() {
        val decision =
            NudgeDecider.decide(
                now = now,
                lastLogTimestamp = now - 47 * hour,
                hasActiveReward = true,
                currentStage = 0,
                anchorTimestamp = 0L,
            )
        assertEquals(NudgeDecision.NoOp, decision)
    }

    @Test
    fun `idle at least 48h with stage 0 sends first nudge`() {
        val lastLog = now - 48 * hour
        val decision =
            NudgeDecider.decide(
                now = now,
                lastLogTimestamp = lastLog,
                hasActiveReward = true,
                currentStage = 0,
                anchorTimestamp = 0L,
            )
        assertEquals(NudgeDecision.Send(stage = 1, newAnchor = lastLog), decision)
    }

    @Test
    fun `idle past 48h but under 96h with stage 1 already sent returns NoOp`() {
        val lastLog = now - 60 * hour
        val decision =
            NudgeDecider.decide(
                now = now,
                lastLogTimestamp = lastLog,
                hasActiveReward = true,
                currentStage = 1,
                anchorTimestamp = lastLog,
            )
        assertEquals(NudgeDecision.NoOp, decision)
    }

    @Test
    fun `idle at least 96h with stage 1 sends second nudge`() {
        val lastLog = now - 96 * hour
        val decision =
            NudgeDecider.decide(
                now = now,
                lastLogTimestamp = lastLog,
                hasActiveReward = true,
                currentStage = 1,
                anchorTimestamp = lastLog,
            )
        assertEquals(NudgeDecision.Send(stage = 2, newAnchor = lastLog), decision)
    }

    @Test
    fun `stage 2 never sends again regardless of idle duration`() {
        val lastLog = now - 500 * hour
        val decision =
            NudgeDecider.decide(
                now = now,
                lastLogTimestamp = lastLog,
                hasActiveReward = true,
                currentStage = 2,
                anchorTimestamp = lastLog,
            )
        assertEquals(NudgeDecision.NoOp, decision)
    }

    @Test
    fun `a new log after stage 1 resets the streak`() {
        val newLog = now - 1 * hour
        val decision =
            NudgeDecider.decide(
                now = now,
                lastLogTimestamp = newLog,
                hasActiveReward = true,
                currentStage = 1,
                anchorTimestamp = now - 60 * hour,
            )
        assertEquals(NudgeDecision.Reset(newAnchor = newLog), decision)
    }

    @Test
    fun `a new log after stage 2 resets the streak`() {
        val newLog = now - 1 * hour
        val decision =
            NudgeDecider.decide(
                now = now,
                lastLogTimestamp = newLog,
                hasActiveReward = true,
                currentStage = 2,
                anchorTimestamp = now - 200 * hour,
            )
        assertEquals(NudgeDecision.Reset(newAnchor = newLog), decision)
    }

    @Test
    fun `reset only triggers when stage is nonzero and anchor differs`() {
        // Stage 0 with a stale anchor (never actually used) should still evaluate thresholds normally.
        val lastLog = now - 10 * hour
        val decision =
            NudgeDecider.decide(
                now = now,
                lastLogTimestamp = lastLog,
                hasActiveReward = true,
                currentStage = 0,
                anchorTimestamp = now - 999 * hour,
            )
        assertTrue(decision is NudgeDecision.NoOp)
    }
}

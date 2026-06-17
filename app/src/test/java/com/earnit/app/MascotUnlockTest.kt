package com.earnit.app

import com.earnit.app.data.MascotId
import com.earnit.app.data.Mascots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotUnlockTest {
    private val defaultUnlocked = setOf(MascotId.PUGSLY, MascotId.TABBY)

    // ── ClaimsReached ─────────────────────────────────────────────────────────

    @Test
    fun `ClaimsReached unlocks when totalClaims equals threshold`() {
        // MASCOT_3 requires ClaimsReached(1). Caller passes totalClaims = historySize + 1.
        val result =
            Mascots.computeNewlyUnlocked(
                totalClaims = 1,
                totalPoints = 0,
                totalTasks = 0,
                alreadyUnlocked = defaultUnlocked,
            )
        assertTrue(MascotId.MASCOT_3 in result)
    }

    @Test
    fun `ClaimsReached does not unlock when totalClaims is below threshold`() {
        val result =
            Mascots.computeNewlyUnlocked(
                totalClaims = 0,
                totalPoints = 0,
                totalTasks = 0,
                alreadyUnlocked = defaultUnlocked,
            )
        assertFalse(MascotId.MASCOT_3 in result)
    }

    // ── PointsReached ─────────────────────────────────────────────────────────

    @Test
    fun `PointsReached unlocks at exact threshold`() {
        // MASCOT_4 requires PointsReached(100)
        val result =
            Mascots.computeNewlyUnlocked(
                totalClaims = 0,
                totalPoints = 100,
                totalTasks = 0,
                alreadyUnlocked = defaultUnlocked,
            )
        assertTrue(MascotId.MASCOT_4 in result)
    }

    @Test
    fun `PointsReached does not unlock one point below threshold`() {
        val result =
            Mascots.computeNewlyUnlocked(
                totalClaims = 0,
                totalPoints = 99,
                totalTasks = 0,
                alreadyUnlocked = defaultUnlocked,
            )
        assertFalse(MascotId.MASCOT_4 in result)
    }

    // ── TasksCompleted ────────────────────────────────────────────────────────

    @Test
    fun `TasksCompleted unlocks at exact threshold`() {
        // MASCOT_5 requires TasksCompleted(25)
        val result =
            Mascots.computeNewlyUnlocked(
                totalClaims = 0,
                totalPoints = 0,
                totalTasks = 25,
                alreadyUnlocked = defaultUnlocked,
            )
        assertTrue(MascotId.MASCOT_5 in result)
    }

    @Test
    fun `TasksCompleted does not unlock one task below threshold`() {
        val result =
            Mascots.computeNewlyUnlocked(
                totalClaims = 0,
                totalPoints = 0,
                totalTasks = 24,
                alreadyUnlocked = defaultUnlocked,
            )
        assertFalse(MascotId.MASCOT_5 in result)
    }

    // ── Already unlocked ──────────────────────────────────────────────────────

    @Test
    fun `already unlocked mascots are not returned again`() {
        val alreadyAll = defaultUnlocked + MascotId.MASCOT_3 + MascotId.MASCOT_4
        val result =
            Mascots.computeNewlyUnlocked(
                totalClaims = 10,
                totalPoints = 500,
                totalTasks = 200,
                alreadyUnlocked = alreadyAll,
            )
        assertFalse(MascotId.MASCOT_3 in result)
        assertFalse(MascotId.MASCOT_4 in result)
    }

    // ── Multiple unlock at once ───────────────────────────────────────────────

    @Test
    fun `multiple mascots crossing threshold at once are all returned`() {
        // High stats cross ClaimsReached(1), PointsReached(100), and TasksCompleted(25) simultaneously
        val result =
            Mascots.computeNewlyUnlocked(
                totalClaims = 1,
                totalPoints = 100,
                totalTasks = 25,
                alreadyUnlocked = defaultUnlocked,
            )
        assertTrue(MascotId.MASCOT_3 in result)
        assertTrue(MascotId.MASCOT_4 in result)
        assertTrue(MascotId.MASCOT_5 in result)
        assertEquals(3, result.size)
    }
}

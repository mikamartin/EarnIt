package com.earnit.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PugslyGestureTest {
    private val start = 1_000_000_000_000L

    @Test
    fun `first tap always starts a new sequence`() {
        val state = PugslyGesture.nextState(emptyList(), start)
        assertEquals(listOf(start), state)
    }

    @Test
    fun `tap exactly at the group gap boundary counts as same burst`() {
        val timestamps = listOf(start)
        val state = PugslyGesture.nextState(timestamps, start + PugslyGesture.GROUP_GAP_MS)
        assertEquals(listOf(start, start + PugslyGesture.GROUP_GAP_MS), state)
    }

    @Test
    fun `tap one ms past the group gap resets to a single tap`() {
        val timestamps = listOf(start)
        val now = start + PugslyGesture.GROUP_GAP_MS + 1
        val state = PugslyGesture.nextState(timestamps, now)
        assertEquals(listOf(now), state)
    }

    @Test
    fun `pause one ms short of the minimum resets to a single tap`() {
        val burstOne = listOf(start, start + 100, start + 200)
        val now = burstOne.last() + PugslyGesture.PAUSE_MS.first - 1
        val state = PugslyGesture.nextState(burstOne, now)
        assertEquals(listOf(now), state)
    }

    @Test
    fun `pause at exactly the minimum is accepted`() {
        val burstOne = listOf(start, start + 100, start + 200)
        val now = burstOne.last() + PugslyGesture.PAUSE_MS.first
        val state = PugslyGesture.nextState(burstOne, now)
        assertEquals(burstOne + now, state)
    }

    @Test
    fun `pause at exactly the maximum is accepted`() {
        val burstOne = listOf(start, start + 100, start + 200)
        val now = burstOne.last() + PugslyGesture.PAUSE_MS.last
        val state = PugslyGesture.nextState(burstOne, now)
        assertEquals(burstOne + now, state)
    }

    @Test
    fun `pause one ms past the maximum resets to a single tap`() {
        val burstOne = listOf(start, start + 100, start + 200)
        val now = burstOne.last() + PugslyGesture.PAUSE_MS.last + 1
        val state = PugslyGesture.nextState(burstOne, now)
        assertEquals(listOf(now), state)
    }

    @Test
    fun `full valid pattern reaches seven taps and is complete`() {
        var taps = listOf<Long>()
        var now = start
        // Burst of 3.
        repeat(3) {
            taps = PugslyGesture.nextState(taps, now)
            now += 50
        }
        // Deliberate pause.
        now = taps.last() + 1000
        // Burst of 4.
        repeat(4) {
            taps = PugslyGesture.nextState(taps, now)
            now += 50
        }
        assertEquals(PugslyGesture.PATTERN_LENGTH, taps.size)
        assertTrue(PugslyGesture.isComplete(taps))
    }

    @Test
    fun `an extra tap before the pause is not treated as complete`() {
        var taps = listOf<Long>()
        var now = start
        repeat(4) {
            taps = PugslyGesture.nextState(taps, now)
            now += 50
        }
        assertFalse(PugslyGesture.isComplete(taps))
    }

    @Test
    fun `a slow tap mid second burst resets before completion`() {
        var taps = listOf<Long>()
        var now = start
        repeat(3) {
            taps = PugslyGesture.nextState(taps, now)
            now += 50
        }
        now = taps.last() + 1000
        taps = PugslyGesture.nextState(taps, now)
        now += PugslyGesture.GROUP_GAP_MS + 1
        taps = PugslyGesture.nextState(taps, now)
        assertEquals(listOf(now), taps)
    }
}

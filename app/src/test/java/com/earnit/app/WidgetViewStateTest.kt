package com.earnit.app

import com.earnit.app.data.RewardEntity
import com.earnit.app.data.RewardProgress
import com.earnit.app.widget.WidgetViewState
import com.earnit.app.widget.resolveWidgetViewState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers resolveWidgetViewState as a pure function, independent of provideGlance's Hilt/Room/Glance
 * pipeline. In particular, rewardId != 0L with no matching progress is ambiguous between "claimed"
 * and "deleted without being claimed" — rewardExists is what tells those two apart.
 */
class WidgetViewStateTest {
    private val progress = RewardProgress(RewardEntity(id = 1L, name = "Movie Night", cost = 10), emptyList(), emptyList(), emptyList(), emptyList())

    @Test
    fun progressPresentAndFlashing_resolvesToFlash() {
        assertEquals(
            WidgetViewState.FLASH,
            resolveWidgetViewState(progress = progress, rewardId = 1L, rewardExists = true, showFlash = true),
        )
    }

    @Test
    fun progressPresentNotFlashing_resolvesToStandard() {
        assertEquals(
            WidgetViewState.STANDARD,
            resolveWidgetViewState(progress = progress, rewardId = 1L, rewardExists = true, showFlash = false),
        )
    }

    @Test
    fun noProgressRewardIdSetAndRewardExists_resolvesToClaimed() {
        assertEquals(
            WidgetViewState.CLAIMED,
            resolveWidgetViewState(progress = null, rewardId = 1L, rewardExists = true, showFlash = false),
        )
    }

    @Test
    fun noProgressRewardIdSetButRewardGone_resolvesToRewardRemoved() {
        assertEquals(
            WidgetViewState.REWARD_REMOVED,
            resolveWidgetViewState(progress = null, rewardId = 1L, rewardExists = false, showFlash = false),
        )
    }

    @Test
    fun noProgressAndNoRewardIdSet_resolvesToEmpty() {
        assertEquals(
            WidgetViewState.EMPTY,
            resolveWidgetViewState(progress = null, rewardId = 0L, rewardExists = false, showFlash = false),
        )
    }
}

package com.earnit.app.ui

import com.earnit.app.ui.onboarding.OnboardingField
import com.earnit.app.ui.onboarding.OnboardingLogic
import com.earnit.app.ui.onboarding.OnboardingStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStepTest {
    @Test
    fun `intro step can always advance`() {
        assertTrue(OnboardingLogic.canAdvance(OnboardingStep.Intro, name = "", cost = "", linkedTaskCount = 0))
    }

    @Test
    fun `name spotlight blocks advance until name is non-blank`() {
        val step = OnboardingStep.Spotlight(OnboardingField.NAME)
        assertFalse(OnboardingLogic.canAdvance(step, name = "  ", cost = "", linkedTaskCount = 0))
        assertTrue(OnboardingLogic.canAdvance(step, name = "Trip", cost = "", linkedTaskCount = 0))
    }

    @Test
    fun `cost spotlight blocks advance until cost is a positive number`() {
        val step = OnboardingStep.Spotlight(OnboardingField.COST)
        assertFalse(OnboardingLogic.canAdvance(step, name = "Trip", cost = "", linkedTaskCount = 0))
        assertFalse(OnboardingLogic.canAdvance(step, name = "Trip", cost = "0", linkedTaskCount = 0))
        assertFalse(OnboardingLogic.canAdvance(step, name = "Trip", cost = "not a number", linkedTaskCount = 0))
        assertTrue(OnboardingLogic.canAdvance(step, name = "Trip", cost = "50", linkedTaskCount = 0))
    }

    @Test
    fun `task spotlight blocks advance until at least one task is linked`() {
        val step = OnboardingStep.Spotlight(OnboardingField.TASKS)
        assertFalse(OnboardingLogic.canAdvance(step, name = "Trip", cost = "50", linkedTaskCount = 0))
        assertTrue(OnboardingLogic.canAdvance(step, name = "Trip", cost = "50", linkedTaskCount = 1))
    }

    @Test
    fun `awaiting save and outro can always advance`() {
        assertTrue(OnboardingLogic.canAdvance(OnboardingStep.AwaitingSave, name = "Trip", cost = "50", linkedTaskCount = 1))
        assertTrue(OnboardingLogic.canAdvance(OnboardingStep.Outro, name = "Trip", cost = "50", linkedTaskCount = 1))
    }

    @Test
    fun `next steps through the flow in order`() {
        assertEquals(OnboardingStep.Spotlight(OnboardingField.NAME), OnboardingLogic.next(OnboardingStep.Intro))
        assertEquals(
            OnboardingStep.Spotlight(OnboardingField.COST),
            OnboardingLogic.next(OnboardingStep.Spotlight(OnboardingField.NAME)),
        )
        assertEquals(
            OnboardingStep.Spotlight(OnboardingField.TASKS),
            OnboardingLogic.next(OnboardingStep.Spotlight(OnboardingField.COST)),
        )
        assertEquals(
            OnboardingStep.AwaitingSave,
            OnboardingLogic.next(OnboardingStep.Spotlight(OnboardingField.TASKS)),
        )
        assertEquals(OnboardingStep.Outro, OnboardingLogic.next(OnboardingStep.AwaitingSave))
    }

    @Test
    fun `outro is a terminal state`() {
        assertEquals(OnboardingStep.Outro, OnboardingLogic.next(OnboardingStep.Outro))
    }

    @Test
    fun `previous steps back through the flow in order`() {
        assertEquals(
            OnboardingStep.Intro,
            OnboardingLogic.previous(OnboardingStep.Spotlight(OnboardingField.NAME)),
        )
        assertEquals(
            OnboardingStep.Spotlight(OnboardingField.NAME),
            OnboardingLogic.previous(OnboardingStep.Spotlight(OnboardingField.COST)),
        )
        assertEquals(
            OnboardingStep.Spotlight(OnboardingField.COST),
            OnboardingLogic.previous(OnboardingStep.Spotlight(OnboardingField.TASKS)),
        )
        assertEquals(
            OnboardingStep.Spotlight(OnboardingField.TASKS),
            OnboardingLogic.previous(OnboardingStep.AwaitingSave),
        )
    }

    @Test
    fun `intro and outro have no previous state to back into`() {
        assertEquals(OnboardingStep.Intro, OnboardingLogic.previous(OnboardingStep.Intro))
        assertEquals(OnboardingStep.Outro, OnboardingLogic.previous(OnboardingStep.Outro))
    }

    @Test
    fun `next and previous are inverses, except into the terminal Outro state`() {
        // AwaitingSave -> next -> Outro is excluded: Outro is terminal for previous() too
        // (the underlying reward is already saved by then, so there's nothing to back into).
        val steps =
            listOf(
                OnboardingStep.Spotlight(OnboardingField.NAME),
                OnboardingStep.Spotlight(OnboardingField.COST),
                OnboardingStep.Spotlight(OnboardingField.TASKS),
            )
        steps.forEach { step ->
            assertEquals(step, OnboardingLogic.previous(OnboardingLogic.next(step)))
        }
    }

    @Test
    fun `fromIndex round-trips every step's index`() {
        val steps =
            listOf(
                OnboardingStep.Intro,
                OnboardingStep.Spotlight(OnboardingField.NAME),
                OnboardingStep.Spotlight(OnboardingField.COST),
                OnboardingStep.Spotlight(OnboardingField.TASKS),
                OnboardingStep.AwaitingSave,
                OnboardingStep.Outro,
            )
        steps.forEach { step ->
            assertEquals(step, OnboardingStep.fromIndex(step.index))
        }
    }
}

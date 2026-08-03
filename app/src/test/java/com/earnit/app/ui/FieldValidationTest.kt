package com.earnit.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldValidationTest {
    @Test
    fun `acceptWithinLimit returns incoming when under the max`() {
        assertEquals("ab", acceptWithinLimit(current = "a", incoming = "ab", max = 5))
    }

    @Test
    fun `acceptWithinLimit returns incoming when exactly at the max`() {
        assertEquals("abc", acceptWithinLimit(current = "ab", incoming = "abc", max = 3))
    }

    @Test
    fun `acceptWithinLimit returns current unchanged when incoming exceeds the max`() {
        assertEquals("abc", acceptWithinLimit(current = "abc", incoming = "abcd", max = 3))
    }

    @Test
    fun `acceptWithinLimit accepts a same-length replacement at the max`() {
        assertEquals("xyz", acceptWithinLimit(current = "abc", incoming = "xyz", max = 3))
    }

    @Test
    fun `digitsOnly strips non-digit characters from mixed input`() {
        assertEquals("1234", "12ab34".digitsOnly())
    }

    @Test
    fun `digitsOnly leaves an all-digit string unchanged`() {
        assertEquals("1234", "1234".digitsOnly())
    }

    @Test
    fun `digitsOnly returns empty for a string with no digits`() {
        assertEquals("", "abcd".digitsOnly())
    }

    @Test
    fun `nicknameFieldEdit disables random nickname when an accepted edit is typed while it's on`() {
        val edit = nicknameFieldEdit(current = "", incoming = "Mika", useRandomNickname = true)

        assertEquals("Mika", edit.text)
        assertTrue(edit.shouldDisableRandomNickname)
    }

    @Test
    fun `nicknameFieldEdit does not disable random nickname when it's already off`() {
        val edit = nicknameFieldEdit(current = "", incoming = "Mika", useRandomNickname = false)

        assertEquals("Mika", edit.text)
        assertFalse(edit.shouldDisableRandomNickname)
    }

    @Test
    fun `nicknameFieldEdit does not disable random nickname when the edit is rejected for exceeding the cap`() {
        val current = "a".repeat(NICKNAME_MAX_CHARS)
        val edit = nicknameFieldEdit(current = current, incoming = current + "x", useRandomNickname = true)

        assertEquals(current, edit.text)
        assertFalse(edit.shouldDisableRandomNickname)
    }

    @Test
    fun `taskGroupFieldEdit signals clearing the selected group when text becomes non-blank`() {
        val edit = taskGroupFieldEdit(current = "", incoming = "Errands")

        assertEquals("Errands", edit.text)
        assertTrue(edit.shouldClearSelectedGroup)
    }

    @Test
    fun `taskGroupFieldEdit does not signal clearing the selected group when text is cleared back to blank`() {
        val edit = taskGroupFieldEdit(current = "E", incoming = "")

        assertEquals("", edit.text)
        assertFalse(edit.shouldClearSelectedGroup)
    }

    @Test
    fun `withIncludedSetTo false resets mandatory and repeatable regardless of prior state`() {
        val state = TaskEditState(included = true, isMandatory = true, isRepeatable = true)

        val result = state.withIncludedSetTo(false)

        assertFalse(result.included)
        assertFalse(result.isMandatory)
        assertFalse(result.isRepeatable)
    }

    @Test
    fun `withIncludedSetTo true sets included without touching mandatory or repeatable`() {
        val state = TaskEditState(included = false, isMandatory = false, isRepeatable = false)

        val result = state.withIncludedSetTo(true)

        assertTrue(result.included)
        assertFalse(result.isMandatory)
        assertFalse(result.isRepeatable)
    }
}

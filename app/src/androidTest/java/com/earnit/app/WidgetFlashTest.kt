package com.earnit.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.widget.WidgetFlash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetFlashTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clearPrefs() {
        context
            .getSharedPreferences("widget_flash", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `set then isActive returns true for same reward`() {
        WidgetFlash.set(context, rewardId = 1L)
        assertTrue(WidgetFlash.isActive(context, 1L))
    }

    @Test
    fun `isActive returns false for different reward id`() {
        WidgetFlash.set(context, rewardId = 1L)
        assertFalse(WidgetFlash.isActive(context, 2L))
    }

    @Test
    fun `isActive returns false when flash has expired`() {
        WidgetFlash.set(context, rewardId = 1L, durationMs = -1000L)
        assertFalse(WidgetFlash.isActive(context, 1L))
    }

    @Test
    fun `isActive returns false when nothing has been set`() {
        assertFalse(WidgetFlash.isActive(context, 1L))
    }

    @Test
    fun `remainingMs returns positive value when flash is active`() {
        WidgetFlash.set(context, rewardId = 1L, durationMs = 3000L)
        val remaining = WidgetFlash.remainingMs(context, 1L)
        assertTrue(remaining > 0L)
        assertTrue(remaining <= 3000L)
    }

    @Test
    fun `remainingMs returns zero after expiry`() {
        WidgetFlash.set(context, rewardId = 1L, durationMs = -1000L)
        assertEquals(0L, WidgetFlash.remainingMs(context, 1L))
    }

    @Test
    fun `remainingMs returns zero for different reward id`() {
        WidgetFlash.set(context, rewardId = 1L)
        assertEquals(0L, WidgetFlash.remainingMs(context, 2L))
    }
}

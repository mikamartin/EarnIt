package com.earnit.app

import com.earnit.app.data.AppColorScheme
import com.earnit.app.ui.theme.ColorSchemes
import com.earnit.app.widget.widgetColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * `widgetColors()` maps an `AppColorScheme` (plus the device's light/dark mode) to the widget's
 * `WidgetColors`. Unlike `StandardContent`/`WidgetColors` itself, nothing exercised this mapping
 * directly before — WidgetContentTest always builds its own hardcoded `WidgetColors` fixture and
 * passes it straight to `StandardContent`. Covers specifically the two things that can silently
 * regress here: the `notification` accent diverging per scheme (Ocean Blue uses amber, not red,
 * unlike the other two), and the light/dark branch actually changing anything.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetColorsTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `notification color matches ColorSchemes accents for Warm Gold`() {
        val expected = ColorSchemes.accents(AppColorScheme.WARM_GOLD).notification
        val actual = widgetColors(context, AppColorScheme.WARM_GOLD).notification.getColor(context)
        assertEquals(expected, actual)
    }

    @Test
    fun `notification color matches ColorSchemes accents for Forest`() {
        val expected = ColorSchemes.accents(AppColorScheme.FOREST).notification
        val actual = widgetColors(context, AppColorScheme.FOREST).notification.getColor(context)
        assertEquals(expected, actual)
    }

    @Test
    fun `notification color diverges to amber for Ocean Blue`() {
        val expected = ColorSchemes.accents(AppColorScheme.OCEAN_BLUE).notification
        val actual = widgetColors(context, AppColorScheme.OCEAN_BLUE).notification.getColor(context)
        assertEquals(expected, actual)

        val warmGold = widgetColors(context, AppColorScheme.WARM_GOLD).notification.getColor(context)
        assertNotEquals(
            "Ocean Blue's notification color is intentionally amber, not the red the other schemes share",
            warmGold,
            actual,
        )
    }

    @Test
    fun `dark mode changes primary color from light mode for the same scheme`() {
        val lightPrimary = widgetColors(context, AppColorScheme.WARM_GOLD).primary.getColor(context)

        RuntimeEnvironment.setQualifiers("+night")
        val darkPrimary = widgetColors(context, AppColorScheme.WARM_GOLD).primary.getColor(context)

        assertNotEquals(
            "widgetColors should read the dark-mode ColorScheme once night mode is active",
            lightPrimary,
            darkPrimary,
        )
    }
}

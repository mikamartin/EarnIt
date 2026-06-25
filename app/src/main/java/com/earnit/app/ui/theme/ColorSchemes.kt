package com.earnit.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.earnit.app.data.AppColorScheme

data class EarnItAccents(
    val gradientStart: Color,
    val gradientEnd: Color,
    val cardPalette: List<Color>,
    val notification: Color,
)

val LocalEarnItAccents =
    staticCompositionLocalOf {
        EarnItAccents(
            gradientStart = Color(0xFFFFBF00),
            gradientEnd = Color(0xFFE07B00),
            cardPalette = listOf(Color(0xFFE8A000), Color(0xFF2A9D8F), Color(0xFF8E7CC3)),
            notification = Color(0xFFE53935),
        )
    }

object ColorSchemes {
    fun lightColors(scheme: AppColorScheme): ColorScheme =
        when (scheme) {
            AppColorScheme.WARM_GOLD -> warmGoldLight
            AppColorScheme.OCEAN_BLUE -> oceanBlueLight
            AppColorScheme.FOREST -> forestLight
        }

    fun darkColors(scheme: AppColorScheme): ColorScheme =
        when (scheme) {
            AppColorScheme.WARM_GOLD -> warmGoldDark
            AppColorScheme.OCEAN_BLUE -> oceanBlueDark
            AppColorScheme.FOREST -> forestDark
        }

    fun accents(scheme: AppColorScheme): EarnItAccents =
        when (scheme) {
            AppColorScheme.WARM_GOLD ->
                EarnItAccents(
                    gradientStart = Color(0xFFFFBF00),
                    gradientEnd = Color(0xFFE07B00),
                    cardPalette = listOf(Color(0xFFE8A000), Color(0xFF2A9D8F), Color(0xFF8E7CC3)),
                    notification = Color(0xFFE53935),
                )
            AppColorScheme.OCEAN_BLUE ->
                EarnItAccents(
                    gradientStart = Color(0xFF42A5F5),
                    gradientEnd = Color(0xFF1565C0),
                    cardPalette = listOf(Color(0xFF1976D2), Color(0xFF0097A7), Color(0xFF546E7A)),
                    notification = Color(0xFFE8A000),
                )
            AppColorScheme.FOREST ->
                EarnItAccents(
                    gradientStart = Color(0xFF66BB6A),
                    gradientEnd = Color(0xFF2E7D32),
                    cardPalette = listOf(Color(0xFF2E7D32), Color(0xFF795548), Color(0xFF0097A7)),
                    notification = Color(0xFFE53935),
                )
        }

    // ── Warm Gold ─────────────────────────────────────────────────────────────

    private val warmGoldLight =
        lightColorScheme(
            primary = Color(0xFFE8A000),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFEFAA),
            onPrimaryContainer = Color(0xFF261A00),
            secondary = Color(0xFF2A9D8F),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFB2DFDB),
            onSecondaryContainer = Color(0xFF00201E),
            surface = Color(0xFFFFFBF0),
            onSurface = Color(0xFF1C1B18),
            surfaceVariant = Color(0xFFEEE8D5),
            onSurfaceVariant = Color(0xFF4A4639),
            background = Color(0xFFFFFBF0),
            onBackground = Color(0xFF1C1B18),
        )

    private val warmGoldDark =
        darkColorScheme(
            primary = Color(0xFFFFD060),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF574400),
            onPrimaryContainer = Color(0xFFFFE082),
            secondary = Color(0xFF4DB6AC),
            onSecondary = Color(0xFF00201E),
            secondaryContainer = Color(0xFF1A5C56),
            onSecondaryContainer = Color(0xFFB2DFDB),
            surface = Color(0xFF1E1C18),
            onSurface = Color(0xFFEDE7D9),
            surfaceVariant = Color(0xFF2D2B25),
            onSurfaceVariant = Color(0xFFCDC6B4),
            background = Color(0xFF1E1C18),
            onBackground = Color(0xFFEDE7D9),
        )

    // ── Ocean Blue ────────────────────────────────────────────────────────────

    private val oceanBlueLight =
        lightColorScheme(
            primary = Color(0xFF1976D2),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFCCE5FF),
            onPrimaryContainer = Color(0xFF001D36),
            secondary = Color(0xFF0097A7),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFB2EBF2),
            onSecondaryContainer = Color(0xFF00202A),
            surface = Color(0xFFF2F7FF),
            onSurface = Color(0xFF181C20),
            surfaceVariant = Color(0xFFDDE8F5),
            onSurfaceVariant = Color(0xFF3A4555),
            background = Color(0xFFF2F7FF),
            onBackground = Color(0xFF181C20),
        )

    private val oceanBlueDark =
        darkColorScheme(
            primary = Color(0xFF64B5F6),
            onPrimary = Color(0xFF001D36),
            primaryContainer = Color(0xFF004B87),
            onPrimaryContainer = Color(0xFFCCE5FF),
            secondary = Color(0xFF4DD0E1),
            onSecondary = Color(0xFF00202A),
            secondaryContainer = Color(0xFF00525F),
            onSecondaryContainer = Color(0xFFB2EBF2),
            surface = Color(0xFF0D1B2A),
            onSurface = Color(0xFFD8E4F0),
            surfaceVariant = Color(0xFF1A2A3A),
            onSurfaceVariant = Color(0xFFB0C4D8),
            background = Color(0xFF0D1B2A),
            onBackground = Color(0xFFD8E4F0),
        )

    // ── Forest ────────────────────────────────────────────────────────────────

    private val forestLight =
        lightColorScheme(
            primary = Color(0xFF2E7D32),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFB8E6BA),
            onPrimaryContainer = Color(0xFF002105),
            secondary = Color(0xFF795548),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFD7CCC8),
            onSecondaryContainer = Color(0xFF1A0F0A),
            surface = Color(0xFFF6F3EE),
            onSurface = Color(0xFF1A1C18),
            surfaceVariant = Color(0xFFE6DFD4),
            onSurfaceVariant = Color(0xFF45483E),
            background = Color(0xFFF6F3EE),
            onBackground = Color(0xFF1A1C18),
        )

    private val forestDark =
        darkColorScheme(
            primary = Color(0xFF81C784),
            onPrimary = Color(0xFF002105),
            primaryContainer = Color(0xFF1B5E20),
            onPrimaryContainer = Color(0xFFB8E6BA),
            secondary = Color(0xFFBCAAA4),
            onSecondary = Color(0xFF1A0F0A),
            secondaryContainer = Color(0xFF4E342E),
            onSecondaryContainer = Color(0xFFD7CCC8),
            surface = Color(0xFF1A2318),
            onSurface = Color(0xFFE0E8D8),
            surfaceVariant = Color(0xFF252E22),
            onSurfaceVariant = Color(0xFFC0C8B4),
            background = Color(0xFF1A2318),
            onBackground = Color(0xFFE0E8D8),
        )
}

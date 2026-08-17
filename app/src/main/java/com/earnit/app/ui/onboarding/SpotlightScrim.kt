package com.earnit.app.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.unit.dp

/**
 * Full-screen dimming scrim with a rounded cutout around [targetRect] (in the same local
 * coordinate space as this composable). A null [targetRect] dims the whole screen.
 */
@Composable
fun SpotlightScrim(
    targetRect: Rect?,
    modifier: Modifier = Modifier,
    // Low enough that the rest of the real screen stays legible for context — this is meant to
    // draw the eye, not black out the app the user is trying to learn.
    scrimColor: Color = Color.Black.copy(alpha = 0.4f),
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (targetRect == null) {
            drawRect(scrimColor)
            return@Canvas
        }
        val padding = 10.dp.toPx()
        val cutout =
            Rect(
                left = targetRect.left - padding,
                top = targetRect.top - padding,
                right = targetRect.right + padding,
                bottom = targetRect.bottom + padding,
            )
        val fullScreenPath = Path().apply { addRect(Rect(Offset.Zero, size)) }
        val holePath =
            Path().apply {
                addRoundRect(RoundRect(cutout, cornerRadius = CornerRadius(16.dp.toPx())))
            }
        val scrimPath = Path.combine(PathOperation.Difference, fullScreenPath, holePath)
        drawPath(scrimPath, scrimColor)
    }
}

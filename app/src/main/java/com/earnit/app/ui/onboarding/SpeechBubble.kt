package com.earnit.app.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.earnit.app.data.MascotId
import com.earnit.app.data.Mascots
import com.earnit.app.ui.EarnItOutlinedButton
import com.earnit.app.ui.EarnItPrimaryButton
import com.earnit.app.ui.Strings
import com.earnit.app.ui.theme.LocalEarnItAccents

/** Pugsly avatar + coaching text + Continue/Skip affordances, used by all spotlight steps. */
@Composable
fun OnboardingSpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    continueEnabled: Boolean = true,
    onContinue: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val accents = LocalEarnItAccents.current
    val mascotScale = remember { Animatable(1f) }
    LaunchedEffect(text) {
        mascotScale.animateTo(1.15f, tween(150, easing = FastOutSlowInEasing))
        mascotScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
    }
    val pugslyDrawable = remember { Mascots.all.find { it.id == MascotId.PUGSLY }?.drawable }

    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (pugslyDrawable != null) {
            Image(
                painter = painterResource(pugslyDrawable),
                contentDescription = null,
                modifier = Modifier.size(64.dp).scale(mascotScale.value),
            )
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(listOf(accents.gradientStart, accents.gradientEnd)),
                        shape = RoundedCornerShape(16.dp),
                    ).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = accents.gradientStart,
                modifier = Modifier.size(18.dp),
            )
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            extraContent?.invoke(this)
            if (onContinue != null || onSkip != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onSkip != null) {
                        EarnItOutlinedButton(text = Strings.ONBOARDING_SKIP, onClick = onSkip)
                    }
                    if (onContinue != null) {
                        EarnItPrimaryButton(
                            text = Strings.ONBOARDING_CONTINUE,
                            onClick = onContinue,
                            enabled = continueEnabled,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** Small dot row showing progress through the four in-form spotlight steps. */
@Composable
fun OnboardingProgressDots(
    currentIndex: Int,
    modifier: Modifier = Modifier,
    totalSteps: Int = 4,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 1..totalSteps) {
            val active = i == currentIndex
            Box(
                modifier =
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (active) Color.White else Color.White.copy(alpha = 0.4f)),
            )
        }
    }
}

package com.earnit.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.earnit.app.ui.Strings

/** Which real form field a [OnboardingStep.Spotlight] step is anchored to. */
enum class OnboardingField { NAME, COST, TASKS }

/**
 * The onboarding flow's state machine. [Spotlight] steps ride on top of the real Create Reward
 * screen; [AwaitingSave] hands off to the real Save button once a task is linked, rather than
 * duplicating that action inside the overlay.
 */
sealed class OnboardingStep(
    val index: Int,
) {
    object Intro : OnboardingStep(0)

    data class Spotlight(
        val field: OnboardingField,
    ) : OnboardingStep(
            when (field) {
                OnboardingField.NAME -> 1
                OnboardingField.COST -> 2
                OnboardingField.TASKS -> 3
            },
        )

    object AwaitingSave : OnboardingStep(4)

    object Outro : OnboardingStep(5)

    companion object {
        fun fromIndex(index: Int): OnboardingStep =
            when (index) {
                0 -> Intro
                1 -> Spotlight(OnboardingField.NAME)
                2 -> Spotlight(OnboardingField.COST)
                3 -> Spotlight(OnboardingField.TASKS)
                4 -> AwaitingSave
                else -> Outro
            }
    }
}

/** Pure transition/gating logic for [OnboardingStep] — no Compose dependency, unit-testable. */
object OnboardingLogic {
    fun canAdvance(
        step: OnboardingStep,
        name: String,
        cost: String,
        linkedTaskCount: Int,
    ): Boolean =
        when (step) {
            is OnboardingStep.Intro -> true
            is OnboardingStep.Spotlight ->
                when (step.field) {
                    OnboardingField.NAME -> name.isNotBlank()
                    OnboardingField.COST -> (cost.toIntOrNull() ?: 0) > 0
                    OnboardingField.TASKS -> linkedTaskCount > 0
                }
            is OnboardingStep.AwaitingSave -> true
            is OnboardingStep.Outro -> true
        }

    fun next(step: OnboardingStep): OnboardingStep =
        when (step) {
            is OnboardingStep.Intro -> OnboardingStep.Spotlight(OnboardingField.NAME)
            is OnboardingStep.Spotlight ->
                when (step.field) {
                    OnboardingField.NAME -> OnboardingStep.Spotlight(OnboardingField.COST)
                    OnboardingField.COST -> OnboardingStep.Spotlight(OnboardingField.TASKS)
                    OnboardingField.TASKS -> OnboardingStep.AwaitingSave
                }
            is OnboardingStep.AwaitingSave -> OnboardingStep.Outro
            is OnboardingStep.Outro -> OnboardingStep.Outro
        }

    /** The step to land on when the user backs out of [step] — lets a mistake be corrected. */
    fun previous(step: OnboardingStep): OnboardingStep =
        when (step) {
            is OnboardingStep.Intro -> OnboardingStep.Intro
            is OnboardingStep.Spotlight ->
                when (step.field) {
                    OnboardingField.NAME -> OnboardingStep.Intro
                    OnboardingField.COST -> OnboardingStep.Spotlight(OnboardingField.NAME)
                    OnboardingField.TASKS -> OnboardingStep.Spotlight(OnboardingField.COST)
                }
            is OnboardingStep.AwaitingSave -> OnboardingStep.Spotlight(OnboardingField.TASKS)
            is OnboardingStep.Outro -> OnboardingStep.Outro
        }
}

/** Real form-field bounds (window coordinates), captured via [captureOnboardingAnchor] on the real composables. */
class OnboardingAnchors {
    var name by mutableStateOf<Rect?>(null)
    var cost by mutableStateOf<Rect?>(null)
    var tasks by mutableStateOf<Rect?>(null)
    var save by mutableStateOf<Rect?>(null)
}

fun Modifier.captureOnboardingAnchor(onPositioned: (Rect) -> Unit): Modifier = this.onGloballyPositioned { onPositioned(it.boundsInWindow()) }

private val introLines =
    listOf(
        Strings.ONBOARDING_INTRO_LINE_1,
        Strings.ONBOARDING_INTRO_LINE_2,
        Strings.ONBOARDING_INTRO_LINE_3,
    )

/**
 * One consistent presentation for every step — a light scrim (with a cutout around the real
 * field/button being taught, when there is one) plus a Pugsly speech bubble. Intro and Outro
 * used to be separate full-screen gold takeovers; that made them feel like a different app
 * layered on top rather than a guide to this one, so they now use the same bubble style as
 * every other step. The scrim+cutout is purely visual guidance, not a touch-blocker — every
 * real field stays fully interactive everywhere on screen. What actually enforces "do this step
 * before moving on" is the Continue button's enabled state (see [OnboardingLogic.canAdvance]).
 */
@Composable
fun OnboardingOverlay(
    step: OnboardingStep,
    anchors: OnboardingAnchors,
    name: String,
    onNameChange: (String) -> Unit,
    cost: String,
    linkedTaskCount: Int,
    onIntroFinished: () -> Unit,
    onSpotlightContinue: () -> Unit,
    onOutroFinished: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
) {
    var introLineIndex by rememberSaveable(step) { mutableStateOf(0) }
    val introBack: () -> Unit = { if (introLineIndex > 0) introLineIndex-- else onSkip() }
    val introContinue: () -> Unit = { if (introLineIndex < introLines.lastIndex) introLineIndex++ else onIntroFinished() }

    BackHandler {
        when (step) {
            is OnboardingStep.Outro -> onOutroFinished()
            is OnboardingStep.Intro -> introBack()
            else -> onBack()
        }
    }

    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    val targetWindow =
        when (step) {
            is OnboardingStep.Spotlight ->
                when (step.field) {
                    OnboardingField.NAME -> anchors.name
                    OnboardingField.COST -> anchors.cost
                    OnboardingField.TASKS -> anchors.tasks
                }
            is OnboardingStep.AwaitingSave -> anchors.save
            else -> null
        }
    val targetLocal = targetWindow?.translate(-overlayOrigin)
    val canAdvance =
        when (step) {
            is OnboardingStep.Intro -> true
            else -> OnboardingLogic.canAdvance(step, name, cost, linkedTaskCount)
        }
    val bubbleText =
        when (step) {
            is OnboardingStep.Intro -> introLines[introLineIndex]
            is OnboardingStep.Outro -> Strings.ONBOARDING_OUTRO_LINE
            is OnboardingStep.Spotlight ->
                when (step.field) {
                    OnboardingField.NAME -> Strings.ONBOARDING_NAME_LINE
                    OnboardingField.COST -> Strings.ONBOARDING_COST_LINE
                    OnboardingField.TASKS -> Strings.ONBOARDING_TASK_LINE
                }
            is OnboardingStep.AwaitingSave -> Strings.ONBOARDING_READY_TO_SAVE
        }
    // AwaitingSave and Outro have no overlay-owned Continue: AwaitingSave hands off to the real
    // Save button, and Outro's "continue" is the whole flow finishing.
    val onContinue: (() -> Unit)? =
        when (step) {
            is OnboardingStep.Intro -> introContinue
            is OnboardingStep.Outro -> onOutroFinished
            is OnboardingStep.AwaitingSave -> null
            is OnboardingStep.Spotlight -> onSpotlightContinue
        }
    val effectiveOnBack: (() -> Unit)? =
        when (step) {
            is OnboardingStep.Intro -> introBack
            is OnboardingStep.Outro -> null
            else -> onBack
        }
    val showSkip = step !is OnboardingStep.Outro

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { overlayOrigin = it.positionInWindow() },
    ) {
        val screenHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        // Keep the bubble on the opposite half of the screen from whatever it's spotlighting,
        // so it never visually covers the field/button being taught.
        val bubbleAtTop = targetLocal != null && targetLocal.center.y > screenHeightPx / 2f

        SpotlightScrim(targetRect = targetLocal, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {
            if (bubbleAtTop) {
                OnboardingCoachContent(
                    step,
                    bubbleText,
                    canAdvance,
                    name,
                    onNameChange,
                    onContinue,
                    effectiveOnBack,
                    if (showSkip) onSkip else null,
                )
                Box(modifier = Modifier.weight(1f))
            } else {
                Box(modifier = Modifier.weight(1f))
                OnboardingCoachContent(
                    step,
                    bubbleText,
                    canAdvance,
                    name,
                    onNameChange,
                    onContinue,
                    effectiveOnBack,
                    if (showSkip) onSkip else null,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.OnboardingCoachContent(
    step: OnboardingStep,
    bubbleText: String,
    canAdvance: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    onContinue: (() -> Unit)?,
    onBack: (() -> Unit)?,
    onSkip: (() -> Unit)?,
) {
    if (onBack != null || onSkip != null) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Strings.ONBOARDING_BACK_DESC,
                        tint = Color.White,
                    )
                }
            }
            if (onSkip != null) {
                Text(
                    Strings.ONBOARDING_SKIP,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(onClick = onSkip).padding(8.dp),
                )
            }
        }
    }
    if (step is OnboardingStep.Spotlight) {
        OnboardingProgressDots(
            currentIndex = step.index,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp),
        )
    }
    OnboardingSpeechBubble(
        text = bubbleText,
        continueEnabled = canAdvance,
        onContinue = onContinue,
        extraContent =
            if (step == OnboardingStep.Spotlight(OnboardingField.NAME) && name.isBlank()) {
                { StarterChipsRow(onChipSelected = onNameChange) }
            } else {
                null
            },
    )
}

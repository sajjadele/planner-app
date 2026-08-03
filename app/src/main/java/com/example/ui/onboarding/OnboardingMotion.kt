package com.example.ui.onboarding

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * Motion tokens for the onboarding journey, derived from Emil Kowalski's
 * design-engineering principles (see .claude/skills/emil-design-eng + review-animations).
 *
 * - Strong ease-out for enter/exit (never ease-in on UI).
 * - UI transitions stay under 300ms.
 * - Honors the system "Remove animations" accessibility setting as a reduced-motion proxy.
 */
object OnboardingMotion {
    val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    const val STEP_DURATION = 240

    /** Calm ambient motion (welcome sun pulse, connector draw) — slow, tranquil. */
    val CalmEase = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    const val CALM_PULSE_MS = 2400
    const val CONNECT_DRAW_MS = 700
}

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Subtle press feedback (scale ~0.97) on any pressable element, per the
 * "Buttons must feel responsive" rule. Pair with the element's InteractionSource.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(120, easing = OnboardingMotion.EaseOut),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

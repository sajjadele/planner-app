package com.example.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import kotlin.math.sin

/**
 * Step 1 — Welcome. Introduces the Goal-first philosophy with a calm, pulsing sun.
 * Presentational only; Canvas-only, no graph-domain dependency.
 */
@Composable
fun OnboardingWelcomeScreen(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = rememberReduceMotion()

    val transition = rememberInfiniteTransition(label = "welcomeSun")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            tween(
                OnboardingMotion.CALM_PULSE_MS,
                easing = OnboardingMotion.CalmEase
            ),
            RepeatMode.Reverse
        ),
        label = "welcomeSunPulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        CalmSun(scale = if (reduceMotion) 1f else pulse, modifier = Modifier.size(140.dp))

        Spacer(Modifier.height(36.dp))

        Text(
            text = "هر هدفِ بزرگی،",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "با یک قدمِ کوچک شروع می‌شود.",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.weight(1f))

        val interactionSource = remember { MutableInteractionSource() }
        Button(
            onClick = onNext,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .pressScale(interactionSource),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                "شروع کنیم",
                color = MaterialTheme.colorScheme.surface,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** A single calm sun: soft radial halo + gently breathing body. Decorative only. */
@Composable
private fun CalmSun(scale: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val baseR = size.minDimension * 0.21f
        val sunR = baseR * scale
        val haloR = sunR * 2.7f

        // Soft halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AccentGold.copy(alpha = 0.22f),
                    AccentGold.copy(alpha = 0.08f),
                    AccentGold.copy(alpha = 0f)
                ),
                center = c,
                radius = haloR
            ),
            radius = haloR,
            center = c
        )
        // Body
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF3D6), AccentGold, AccentGold),
                center = c,
                radius = sunR
            ),
            radius = sunR,
            center = c
        )
        // Gentle specular
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = sunR * 0.42f,
            center = c + Offset(-sunR * 0.24f, -sunR * 0.24f)
        )
        // Slow sine rim glow (calm amplification with the breathe)
        drawCircle(
            color = AccentGold.copy(alpha = 0.10f + 0.06f * sin(scale.toDouble() * Math.PI).toFloat()),
            radius = sunR * 1.25f,
            center = c
        )
    }
}
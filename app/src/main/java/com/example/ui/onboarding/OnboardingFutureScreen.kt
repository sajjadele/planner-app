package com.example.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import kotlin.math.cos
import kotlin.math.sin

/**
 * Step 4 — Future Features preview. Shows optional, complementary tools
 * (visual progress + Activity Feed) without implying they are required.
 * All visuals are decorative previews; no graph-domain dependency.
 */
@Composable
fun OnboardingFutureScreen(
    onFinish: () -> Unit,
    onBack: () -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier
) {
    val reduceMotion = rememberReduceMotion()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        OnboardingBackButton(onBack = onBack)
        Spacer(Modifier.height(8.dp))

        Text(
            text = "در مسیر، این ابزارها تو را همراهی می‌کنند.",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        // ── Preview 1: visual progress (mini solar system) ──
        MiniPreviewCard {
            Text(
                text = "منظومه پیشرفت",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "مسیر رشدت را به شکل بصری ببین.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            MiniSolarSystem(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                reduceMotion = reduceMotion
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Card 2: Activity Feed (optional) ──
        MiniPreviewCard {
            Text(
                text = "ثبت مسیر",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "برای قدم‌هایِ بزرگ‌تر، اگر خواستی\nمی‌توانی مسیرِ انجام کار را ثبت کنی.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(16.dp))
            StaticActivityFeedPreview()
        }

        Spacer(Modifier.weight(1f))

        val interactionSource = remember { MutableInteractionSource() }
        Button(
            onClick = onFinish,
            enabled = !isSubmitting,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .pressScale(interactionSource),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    "شروع استفاده از برنامه",
                    color = MaterialTheme.colorScheme.surface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Shared preview card shell. */
@Composable
private fun MiniPreviewCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/** Decorative mini solar system: gold sun + few slowly orbiting nodes. Preview only. */
@Composable
private fun MiniSolarSystem(
    modifier: Modifier = Modifier,
    reduceMotion: Boolean
) {
    val rotation = rememberInfiniteTransition(label = "miniSolarRotation")
    val angle by rotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(
                durationMillis = if (reduceMotion) 1 else 16000,
                easing = LinearEasing
            ),
            RepeatMode.Restart
        ),
        label = "miniSolarAngle"
    )

    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val sunR = size.minDimension * 0.115f
        val a = if (reduceMotion) 0f else angle

        // Halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AccentGold.copy(alpha = 0.24f), AccentGold.copy(alpha = 0f)),
                center = c,
                radius = sunR * 3f
            ),
            radius = sunR * 3f,
            center = c
        )
        drawCircle(color = AccentGold, radius = sunR, center = c)

        // Orbit rings
        val ringColors = listOf(AccentPurple, AccentBlue, AccentGreen)
        val ringFractions = listOf(0.30f, 0.46f, 0.66f)
        ringFractions.forEach { frac ->
            drawCircle(
                color = ringColors[ringFractions.indexOf(frac)].copy(alpha = 0.14f),
                radius = size.minDimension * frac,
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Orbiting nodes (very slow, decorative)
        ringFractions.forEachIndexed { i, frac ->
            val r = size.minDimension * frac
            val deg = a + i * 120f
            val rad = Math.toRadians(deg.toDouble())
            val pos = Offset(c.x + r * cos(rad).toFloat(), c.y + r * sin(rad).toFloat())
            drawCircle(
                color = ringColors[i].copy(alpha = 0.9f),
                radius = 6.dp.toPx(),
                center = pos
            )
        }
    }
}

/** Static, RTL-safe mock of the Activity Feed timeline. Decorative only. */
@Composable
private fun StaticActivityFeedPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ── Date header chip ──
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ) {
            Text(
                text = "${RTL}امروز",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
            )
        }

        // ── Bubble A: image + text + time ──
        ActivityBubble(hasImage = true)

        // ── Bubble B: text-only ──
        ActivityBubble(hasImage = false)

        // ── Optionality footnote ──
        Text(
            text = "فقط در صورت تمایل — لازم نیست هر روز بنویسی.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** Single fake message bubble — text-only or with image placeholder. */
@Composable
private fun ActivityBubble(hasImage: Boolean) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp, 10.dp, 14.dp, 10.dp)
        ) {
            if (hasImage) {
                // Row: image placeholder (start side in RTL) | text | timestamp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Image placeholder (emoji box)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${RTL}📷",
                                fontSize = 18.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${RTL}تمرین امروز انجام شد",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "15:40",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Text-only: text leading, timestamp trailing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${RTL}یک قدمِ کوچک جلو رفتم",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "09:15",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
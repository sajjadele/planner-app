package com.example.plugins.planner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * VisionClockFace — Premium Canvas clock for Vision Planner.
 *
 * Features:
 * - Outer gradient ring
 * - Soft glow on selected value
 * - Clock hands (hour + minute)
 * - Center identity with time display
 * - Neumorphic style
 * - Scale animation on selection
 */
@Composable
fun VisionClockFace(
    selectedHour: Int,
    selectedMinute: Int,
    isMinuteMode: Boolean,
    onHourTap: (Int) -> Unit,
    onMinuteTap: (Int) -> Unit,
    selectorColor: Color = MaterialTheme.colorScheme.primary,
    dialColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    selectedNumberColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedNumberColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Animated selection scale
    val selectionScale by animateFloatAsState(
        targetValue = 1.15f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "selection_scale"
    )

    // Numbers to display based on mode
    val numbers = remember(isMinuteMode) {
        if (isMinuteMode) {
            listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
        } else {
            listOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
        }
    }

    // Selected value based on mode
    val selectedValue = if (isMinuteMode) selectedMinute else selectedHour

    Canvas(
        modifier = modifier
            .size(240.dp)
            .pointerInput(numbers) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val radius = min(centerX, centerY) * 0.72f

                    numbers.forEachIndexed { index, num ->
                        val angle = (index * (360f / numbers.size) - 90f)
                        val rad = Math.toRadians(angle.toDouble())
                        val x = centerX + (radius * cos(rad)).toFloat()
                        val y = centerY + (radius * sin(rad)).toFloat()

                        val distance = kotlin.math.sqrt(
                            (offset.x - x) * (offset.x - x) +
                            (offset.y - y) * (offset.y - y)
                        )
                        if (distance < 28.dp.toPx()) {
                            if (isMinuteMode) {
                                onMinuteTap(num)
                            } else {
                                onHourTap(num)
                            }
                        }
                    }
                }
            }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val outerRadius = min(centerX, centerY) * 0.92f
        val innerRadius = min(centerX, centerY) * 0.72f
        val numberRadius = min(centerX, centerY) * 0.78f

        // ── Outer gradient ring ──
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    selectorColor.copy(alpha = 0.1f),
                    selectorColor.copy(alpha = 0.3f),
                    selectorColor.copy(alpha = 0.1f),
                    selectorColor.copy(alpha = 0.3f),
                    selectorColor.copy(alpha = 0.1f)
                )
            ),
            radius = outerRadius
        )

        // ── Inner dial background (neumorphic) ──
        drawCircle(
            color = dialColor,
            radius = innerRadius
        )

        // ── Inner shadow (neumorphic effect) ──
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                radius = innerRadius
            ),
            radius = innerRadius
        )

        // ── Center glow ──
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    selectorColor.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                radius = 40.dp.toPx()
            ),
            radius = 40.dp.toPx()
        )

        // ── Center dot ──
        drawCircle(
            color = selectorColor,
            radius = 5.dp.toPx()
        )

        // ── Clock hands ──
        // Hour hand
        val hourAngle = (selectedHour % 12) * 30f - 90f
        rotate(hourAngle, Offset(centerX, centerY)) {
            drawLine(
                color = selectorColor.copy(alpha = 0.6f),
                start = Offset(centerX, centerY),
                end = Offset(centerX + innerRadius * 0.4f, centerY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Minute hand
        val minuteAngle = selectedMinute * 6f - 90f
        rotate(minuteAngle, Offset(centerX, centerY)) {
            drawLine(
                color = selectorColor.copy(alpha = 0.4f),
                start = Offset(centerX, centerY),
                end = Offset(centerX + innerRadius * 0.6f, centerY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // ── Numbers around the dial ──
        numbers.forEachIndexed { index, num ->
            val angle = (index * (360f / numbers.size) - 90f)
            val rad = Math.toRadians(angle.toDouble())
            val x = centerX + (numberRadius * cos(rad)).toFloat()
            val y = centerY + (numberRadius * sin(rad)).toFloat()

            val isSelected = num == selectedValue
            val displayNum = String.format("%02d", num)

            // Draw glow behind selected number
            if (isSelected) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            selectorColor.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        radius = 24.dp.toPx()
                    ),
                    radius = 24.dp.toPx(),
                    center = Offset(x, y)
                )

                // Draw filled circle
                drawCircle(
                    color = selectorColor,
                    radius = 18.dp.toPx() * selectionScale,
                    center = Offset(x, y)
                )
            }

            // Draw number text
            val textStyle = TextStyle(
                fontSize = if (isSelected) 14.sp else 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) selectedNumberColor else unselectedNumberColor,
                textAlign = TextAlign.Center
            )

            val textLayoutResult = textMeasurer.measure(displayNum, textStyle)
            val textX = x - textLayoutResult.size.width / 2f
            val textY = y - textLayoutResult.size.height / 2f

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(textX, textY)
            )
        }
    }
}

package com.example.plugins.planner.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * VisionClockFace — Custom Canvas clock for time selection.
 *
 * Phase 2: Replaces CompactClockDial with a proper 12-hour clock face.
 *
 * Hour mode: 12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
 * Minute mode: 00, 05, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55
 *
 * Interaction:
 * - Tap hour → selectHour() → auto-advance to minute mode
 * - Tap minute → selectMinute() → stay in minute mode
 */
@Composable
fun VisionClockFace(
    selectedHour: Int,
    selectedMinute: Int,
    isMinuteMode: Boolean,
    onHourTap: (Int) -> Unit,
    onMinuteTap: (Int) -> Unit,
    selectorColor: Color = MaterialTheme.colorScheme.primary,
    dialColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    selectedNumberColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedNumberColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Animated selection for smooth transitions
    val selectionProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 250),
        label = "selection_anim"
    )

    // Numbers to display based on mode
    val numbers = remember(isMinuteMode) {
        if (isMinuteMode) {
            (0..55 step 5).toList() // 00, 05, 10, ..., 55
        } else {
            listOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11) // Clock face hours
        }
    }

    // Selected value based on mode
    val selectedValue = if (isMinuteMode) selectedMinute else selectedHour

    Canvas(
        modifier = modifier
            .size(220.dp)
            .pointerInput(numbers) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val radius = min(centerX, centerY) * 0.75f

                    // Find which number was tapped
                    numbers.forEachIndexed { index, num ->
                        val angle = (index * (360f / numbers.size) - 90f)
                        val rad = Math.toRadians(angle.toDouble())
                        val x = centerX + (radius * cos(rad)).toFloat()
                        val y = centerY + (radius * sin(rad)).toFloat()

                        // Check if tap is within the number's area
                        val distance = kotlin.math.sqrt(
                            (offset.x - x) * (offset.x - x) +
                            (offset.y - y) * (offset.y - y)
                        )
                        if (distance < 24.dp.toPx()) {
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
        val radius = min(centerX, centerY) * 0.75f

        // Draw dial background
        drawCircle(
            color = dialColor,
            radius = radius + 20.dp.toPx()
        )

        // Draw center dot
        drawCircle(
            color = selectorColor,
            radius = 4.dp.toPx(),
            center = Offset(centerX, centerY)
        )

        // Draw numbers
        numbers.forEachIndexed { index, num ->
            val angle = (index * (360f / numbers.size) - 90f)
            val rad = Math.toRadians(angle.toDouble())
            val x = centerX + (radius * cos(rad)).toFloat()
            val y = centerY + (radius * sin(rad)).toFloat()

            val isSelected = num == selectedValue

            // Draw selection circle
            if (isSelected) {
                drawCircle(
                    color = selectorColor,
                    radius = 16.dp.toPx(),
                    center = Offset(x, y),
                    alpha = selectionProgress
                )
            }

            // Draw number text
            val displayText = String.format("%02d", num)
            val textStyle = TextStyle(
                fontSize = if (isSelected) 14.sp else 12.sp,
                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold
                    else androidx.compose.ui.text.font.FontWeight.Normal,
                color = if (isSelected) selectedNumberColor else unselectedNumberColor,
                textAlign = TextAlign.Center
            )

            val textLayoutResult = textMeasurer.measure(displayText, textStyle)
            val textX = x - textLayoutResult.size.width / 2f
            val textY = y - textLayoutResult.size.height / 2f

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(textX, textY)
            )
        }
    }
}

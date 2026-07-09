package com.example.plugins.planner.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.constants.DateConstants
import com.example.plugins.planner.WeeklyInsightState
import com.example.ui.theme.*

@Composable
fun WeeklyInsightCard(
    state: WeeklyInsightState,
    modifier: Modifier = Modifier
) {
    NeumorphicSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 8,
        backgroundColor = SurfaceWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (state.hasData) {
                // Hero section: Progress ring + completion text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular progress ring
                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressRing(
                            progress = state.completionRate / 100f,
                            trackColor = NeumorphicBackground,
                            progressColor = AccentPurple,
                            strokeWidth = 10f
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${state.completionRate.toInt()}٪",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AccentPurple
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Completion text
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${state.completedCount} از ${state.createdCount} تسک تکمیل شد",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Collapsible secondary info
                var expanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeumorphicBackground.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (state.unorganizedCount > 0) {
                        CompactStatRow(
                            icon = "ℹ️",
                            text = "${state.unorganizedCount} تسک بدون دسته‌بندی",
                            iconTint = TextTertiary
                        )
                    }

                    // Expandable toggle for streak + best day
                    val hasSecondary = state.streakDays > 0 || state.bestDayIndex != null
                    if (hasSecondary) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (expanded) "کمتر" else "بیشتر",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = AccentPurple
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        AnimatedVisibility(visible = expanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (state.streakDays > 0) {
                                    CompactStatRow(
                                        icon = "🔥",
                                        text = "${state.streakDays} روز پیش‌سرهم تسک کامل شده",
                                        iconTint = AccentFire
                                    )
                                }

                                state.bestDayIndex?.let { dayIdx ->
                                    val dayName = DateConstants.persianDayNames.getOrElse(dayIdx) { "—" }
                                    CompactStatRow(
                                        icon = "📅",
                                        text = "بهترین روز: $dayName (${state.bestDayCount} تسک)",
                                        iconTint = AccentBlue
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Empty state
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "📊", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "هنوز داده‌ای برای این هفته ثبت نشده",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CircularProgressRing(
    progress: Float,
    trackColor: Color,
    progressColor: Color,
    strokeWidth: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasSize = size.minDimension
        val radius = (canvasSize - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val sweepAngle = 360f * progress.coerceIn(0f, 1f)

        // Track circle
        drawCircle(
            color = trackColor,
            center = center,
            radius = radius,
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )

        // Progress arc
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun CompactStatRow(
    icon: String,
    text: String,
    iconTint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}


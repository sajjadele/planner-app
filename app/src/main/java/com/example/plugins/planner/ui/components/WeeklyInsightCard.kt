package com.example.plugins.planner.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.core.constants.LifeAreas
import com.example.core.util.RTL
import com.example.core.util.isolated
import com.example.domain.insight.Velocity
import com.example.plugins.planner.ui.WeeklyInsightState
import com.example.ui.theme.*

@Composable
fun WeeklyInsightCard(
    state: WeeklyInsightState,
    onShowMore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    NeumorphicSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 8
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Crossfade(
                targetState = state.hasData,
                modifier = Modifier.fillMaxWidth(),
                // Gentle fade between empty and populated states (no abrupt pop).
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 350)
            ) { hasData ->
                if (hasData) {
                    // ── Compact row: Ring + key stats + expand trigger ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Progress ring
                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressRing(
                                progress = state.completionRate / 100f,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                progressColor = MaterialTheme.colorScheme.primary,
                                strokeWidth = 9f
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${state.completionRate.toInt().isolated()}٪",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Stats column
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${RTL}${state.completedCount.isolated()} از ${state.createdCount.isolated()} تسک تکمیل شد",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (state.streakDays > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${RTL}🔥 ${state.streakDays.isolated()} روز پیاپی",
                                    fontSize = 12.sp,
                                    color = AccentFire
                                )
                            }

                            state.bestDayIndex?.let { dayIdx ->
                                Spacer(modifier = Modifier.height(2.dp))
                                val dayName = DateConstants.persianDayNames.getOrElse(dayIdx) { "—" }
                                Text(
                                    text = "${RTL}📅 بهترین روز: $dayName (${state.bestDayCount.isolated()} تسک)",
                                    fontSize = 11.sp,
                                    color = AccentBlue
                                )
                            }
                        }

                        // "بیشتر" trigger button
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onShowMore() }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "جزئیات بیشتر",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "بیشتر",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────
// Bottom Sheet Content — all the detailed insight rows
// ────────────────────────────────────────────────────────────

@Composable
fun InsightDetailsSheetContent(
    state: WeeklyInsightState,
    onShowUncategorized: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!state.hasData) {
            Text(
                text = "هنوز داده‌ای برای این هفته ثبت نشده",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
            return
        }

        // ── Weekly Velocity ──
        val velocityLabel = when (state.weeklyVelocity) {
            Velocity.IMPROVING -> {
                val pct = state.weeklyVelocityPercent.toInt().isolated()
                "${RTL}عملکرد شما نسبت به هفته گذشته ${pct}٪ بهبود یافته 🚀"
            }
            Velocity.DECLINING -> {
                val pct = state.weeklyVelocityPercent.toInt().isolated()
                "${RTL}عملکرد شما نسبت به هفته گذشته ${pct}٪ کاهش یافته"
            }
            Velocity.STABLE -> "عملکرد شما نسبت به هفته گذشته ثابت است"
        }
        DetailSectionTitle(text = "روند هفتگی")
        DetailStatRow(
            icon = when (state.weeklyVelocity) {
                Velocity.IMPROVING -> "📈"
                Velocity.DECLINING -> "📉"
                Velocity.STABLE -> "➡️"
            },
            text = velocityLabel,
            iconTint = when (state.weeklyVelocity) {
                Velocity.IMPROVING -> AccentGreen
                Velocity.DECLINING -> MaterialTheme.colorScheme.error
                Velocity.STABLE -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        // ── Procrastination Alerts ──
        if (state.procrastinationAlerts.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            DetailSectionTitle(text = "هشدار به تعویق انداختن", color = Color(0xFFF97316))
            state.procrastinationAlerts.forEach { alert ->
                DetailStatRow(
                    icon = "⚠️",
                    text = "${RTL}\"${alert.taskTitle}\" ${alert.rescheduleCount.isolated()} بار به تعویق افتاده",
                    iconTint = Color(0xFFF97316)
                )
            }
        }

        // ── Neglected Goal ──
        state.neglectedGoalTitle?.let { goalTitle ->
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            DetailSectionTitle(text = "هدف مغفول")
            val rateText = state.neglectedGoalRate.toInt().isolated()
            DetailStatRow(
                icon = "📌",
                text = "${RTL}هدف \"$goalTitle\" کمترین نرخ تکمیل را دارد ($rateText٪)",
                iconTint = MaterialTheme.colorScheme.primary
            )
        }

        // ── Unorganized tasks ──
        if (state.unorganizedCount > 0) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            DetailSectionTitle(text = "بدون دسته‌بندی")
            DetailStatRow(
                icon = "ℹ️",
                text = "${RTL}${state.unorganizedCount.isolated()} تسک بدون دسته‌بندی",
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${RTL}مشاهده لیست",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onShowUncategorized() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // ── Life Area Breakdown ──
        state.lifeAreaBreakdown?.let { breakdown ->
            if (breakdown.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                DetailSectionTitle(text = "توزیع حوزه‌های زندگی")
                breakdown.forEach { item ->
                    DetailStatRow(
                        icon = LifeAreas.getIcon(item.lifeAreaId),
                        text = "${RTL}${LifeAreas.getName(item.lifeAreaId)}: ${item.count.isolated()} تسک",
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // ── Streak + Best Day (already shown compactly, but repeat for completeness) ──
        if (state.streakDays > 0 || state.bestDayIndex != null) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            DetailSectionTitle(text = "دستاوردها")
            if (state.streakDays > 0) {
                DetailStatRow(
                    icon = "🔥",
                    text = "${RTL}${state.streakDays.isolated()} روز پیش‌سرهم تسک کامل شده",
                    iconTint = AccentFire
                )
            }
            state.bestDayIndex?.let { dayIdx ->
                val dayName = DateConstants.persianDayNames.getOrElse(dayIdx) { "—" }
                DetailStatRow(
                    icon = "📅",
                    text = "${RTL}بهترین روز: $dayName (${state.bestDayCount.isolated()} تسک)",
                    iconTint = AccentBlue
                )
            }
        }
    }
}

// ── Shared composables ──

@Composable
private fun DetailSectionTitle(text: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun DetailStatRow(
    icon: String,
    text: String,
    iconTint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

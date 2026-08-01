package com.example.plugins.planner.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL

/**
 * VisionReminderTimePicker — Premium time picker for Vision Planner.
 *
 * Design:
 * - Title
 * - Digital time display (swipeable, animated)
 * - Clock face (Canvas, tap to select)
 * - Mode toggle (ساعت/دقیقه)
 * - Context text (انتخاب ساعت/دقیقه)
 * - Period toggle (صبح/عصر/شب)
 * - Action buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionReminderTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pickerState = remember {
        ReminderTimePickerState(
            ReminderTimeState.from24Hour(initialHour, initialMinute)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Title ──
            Text(
                text = "${RTL}زمان یادآوری",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Time Wheel Picker (iOS-style) ──
            VisionTimeWheel(
                hour = pickerState.timeState.hour,
                minute = pickerState.timeState.minute,
                onHourChange = { pickerState.selectHour(it) },
                onMinuteChange = { pickerState.selectMinute(it) },
                onModeAutoSwitch = {
                    // Auto-switch from hour to minute after scrolling stops
                    if (pickerState.mode == TimeSelectionMode.HOUR) {
                        pickerState.mode = TimeSelectionMode.MINUTE
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Context Text ──
            AnimatedContent(
                targetState = pickerState.mode,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "context_text"
            ) { mode ->
                Text(
                    text = "${RTL}${
                        if (mode == TimeSelectionMode.HOUR) "انتخاب ساعت" else "انتخاب دقیقه"
                    }",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Clock Face ──
            AnimatedContent(
                targetState = pickerState.mode,
                transitionSpec = {
                    fadeIn(tween(250)) + scaleIn(tween(250)) togetherWith
                    fadeOut(tween(250)) + scaleOut(tween(250))
                },
                label = "clock_mode"
            ) { mode ->
                VisionClockFace(
                    selectedHour = pickerState.timeState.hour,
                    selectedMinute = pickerState.timeState.minute,
                    isMinuteMode = mode == TimeSelectionMode.MINUTE,
                    onHourTap = { pickerState.selectHour(it) },
                    onMinuteTap = { pickerState.selectMinute(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Mode Toggle Chips ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PremiumModeChip(
                    label = "ساعت",
                    isSelected = pickerState.mode == TimeSelectionMode.HOUR,
                    onClick = { pickerState.mode = TimeSelectionMode.HOUR }
                )
                PremiumModeChip(
                    label = "دقیقه",
                    isSelected = pickerState.mode == TimeSelectionMode.MINUTE,
                    onClick = { pickerState.mode = TimeSelectionMode.MINUTE }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Period Toggle (قبل/بعد از ظهر) ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PeriodChip(
                    label = "قبل از ظهر",
                    icon = "🔵",
                    isSelected = pickerState.timeState.period == DayPeriod.AM,
                    onClick = {
                        if (pickerState.timeState.period != DayPeriod.AM) {
                            pickerState.togglePeriod()
                        }
                    }
                )
                PeriodChip(
                    label = "بعد از ظهر",
                    icon = "🟣",
                    isSelected = pickerState.timeState.period == DayPeriod.PM,
                    onClick = {
                        if (pickerState.timeState.period != DayPeriod.PM) {
                            pickerState.togglePeriod()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Action Buttons ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel — transparent/outlined
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        "${RTL}لغو",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Confirm — primary filled, larger
                Button(
                    onClick = {
                        val (h, m) = pickerState.confirmValue()
                        onConfirm(h, m)
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        "${RTL}تأیید",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

// ── Premium Mode Chip ──

@Composable
private fun PremiumModeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else
            Color.Transparent,
        animationSpec = tween(200),
        label = "chip_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        animationSpec = tween(200),
        label = "chip_border"
    )

    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = "${RTL}$label",
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ── Period Chip (صبح/عصر/شب) ──

@Composable
private fun PeriodChip(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else
            Color.Transparent,
        animationSpec = tween(200),
        label = "period_bg"
    )

    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = icon, fontSize = 14.sp)
            Text(
                text = "${RTL}$label",
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

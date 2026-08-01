package com.example.plugins.planner.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL

/**
 * VisionReminderTimePicker — Complete time picker for setting reminders.
 *
 * Phase 2: Replaces old TimePickerSheet with proper 12h + AM/PM architecture.
 *
 * Layout:
 * ┌──────────────────────────────┐
 * │      زمان یادآوری             │
 * │                              │
 * │         08:30                │
 * │         صبح                  │
 * │                              │
 * │      ┌──────────┐            │
 * │      │  Clock   │            │
 * │      │  Face    │            │
 * │      └──────────┘            │
 * │                              │
 * │    [ ساعت ] [ دقیقه ]        │
 * │                              │
 * │    [ صبح ] [ عصر ] [ شب ]   │
 * │                              │
 * │    [لغو]        [تایید]      │
 * └──────────────────────────────┘
 *
 * Interaction:
 * - Tap hour on clock → auto-advance to minute mode
 * - Tap minute on clock → stay in minute mode
 * - Swipe up/down on digital display → increment/decrement
 * - Tap AM/PM chips → toggle period
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

    // Single source of truth — converts from 24h input
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Digital Time Display (swipeable) ──
            DigitalTimeDisplay(
                hour = pickerState.timeState.hour,
                minute = pickerState.timeState.minute,
                period = pickerState.timeState.period,
                isMinuteMode = pickerState.mode == TimeSelectionMode.MINUTE,
                onSwipeUp = {
                    if (pickerState.mode == TimeSelectionMode.HOUR) {
                        pickerState.increaseHour()
                    } else {
                        pickerState.increaseMinute()
                    }
                },
                onSwipeDown = {
                    if (pickerState.mode == TimeSelectionMode.HOUR) {
                        pickerState.decreaseHour()
                    } else {
                        pickerState.decreaseMinute()
                    }
                }
            )

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

            Spacer(modifier = Modifier.height(12.dp))

            // ── Mode Toggle Chips ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ClockModeChip(
                    label = "ساعت",
                    isSelected = pickerState.mode == TimeSelectionMode.HOUR,
                    onClick = { pickerState.mode = TimeSelectionMode.HOUR },
                    selectedColor = MaterialTheme.colorScheme.primary
                )
                ClockModeChip(
                    label = "دقیقه",
                    isSelected = pickerState.mode == TimeSelectionMode.MINUTE,
                    onClick = { pickerState.mode = TimeSelectionMode.MINUTE },
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Period Toggle (AM/PM) ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PeriodChip(
                    label = "صبح",
                    isSelected = pickerState.timeState.period == DayPeriod.AM,
                    onClick = {
                        if (pickerState.timeState.period != DayPeriod.AM) {
                            pickerState.togglePeriod()
                        }
                    },
                    selectedColor = MaterialTheme.colorScheme.primary
                )
                PeriodChip(
                    label = "عصر",
                    isSelected = pickerState.timeState.period == DayPeriod.PM &&
                            pickerState.timeState.hour in 1..11,
                    onClick = {
                        if (pickerState.timeState.period != DayPeriod.PM ||
                            pickerState.timeState.hour == 12
                        ) {
                            pickerState.togglePeriod()
                        }
                    },
                    selectedColor = MaterialTheme.colorScheme.primary
                )
                PeriodChip(
                    label = "شب",
                    isSelected = pickerState.timeState.period == DayPeriod.PM &&
                            pickerState.timeState.hour in 12..12,
                    onClick = {
                        if (pickerState.timeState.period != DayPeriod.PM) {
                            pickerState.togglePeriod()
                        }
                    },
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Action Buttons ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("${RTL}لغو")
                }
                Button(
                    onClick = {
                        val (h, m) = pickerState.confirmValue()
                        onConfirm(h, m)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("${RTL}تأیید")
                }
            }
        }
    }
}

// ── Digital Time Display (swipeable) ──

@Composable
private fun DigitalTimeDisplay(
    hour: Int,
    minute: Int,
    period: DayPeriod,
    isMinuteMode: Boolean,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit
) {
    val periodLabel = when (period) {
        DayPeriod.AM -> "صبح"
        DayPeriod.PM -> "عصر"
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -10) onSwipeUp()
                    else if (dragAmount > 10) onSwipeDown()
                }
            }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Time digits
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%02d", hour),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMinuteMode)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.primary
            )
            Text(
                text = ":",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = String.format("%02d", minute),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMinuteMode)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        // Period label
        Text(
            text = "${RTL}$periodLabel",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Mode Chip ──

@Composable
private fun ClockModeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) selectedColor.copy(alpha = 0.15f)
            else androidx.compose.ui.graphics.Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, selectedColor) else null
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) selectedColor
                else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

// ── Period Chip (AM/PM) ──

@Composable
private fun PeriodChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) selectedColor.copy(alpha = 0.15f)
            else androidx.compose.ui.graphics.Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, selectedColor) else null
    ) {
        Text(
            text = "${RTL}$label",
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) selectedColor
                else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

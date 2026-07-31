package com.example.plugins.planner.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import com.example.core.util.isolated
import com.example.ui.theme.*

// ── Preset data model ──

data class ReminderPreset(
    val label: String,
    val icon: ImageVector,
    val hour: Int,
    val minute: Int
)

private val defaultPresets = listOf(
    ReminderPreset("صبح", Icons.Default.WbSunny, 8, 0),
    ReminderPreset("عصر", Icons.Default.WbCloudy, 14, 0),
    ReminderPreset("شب", Icons.Default.DarkMode, 21, 0)
)

// ── Reusable Reminder Section ──

@Composable
fun ReminderSection(
    reminderHour: Int?,
    reminderMinute: Int?,
    onSetReminder: (hour: Int, minute: Int) -> Unit,
    onClearReminder: () -> Unit,
    modifier: Modifier = Modifier,
    presets: List<ReminderPreset> = defaultPresets
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Header ──
        Text(
            text = "${RTL}یادآوری",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        // ── Preset chips row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = reminderHour == preset.hour && reminderMinute == preset.minute
                ReminderPresetChip(
                    preset = preset,
                    isSelected = isSelected,
                    onClick = {
                        if (isSelected) {
                            onClearReminder()
                        } else {
                            onSetReminder(preset.hour, preset.minute)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Divider ──
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Custom time row ──
        val isCustom = reminderHour != null && reminderMinute != null &&
                presets.none { it.hour == reminderHour && it.minute == reminderMinute }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { showCustomPicker = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = if (isCustom || (reminderHour != null && reminderMinute != null))
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isCustom)
                    "${RTL}${String.format("%02d:%02d", reminderHour, reminderMinute).isolated()}"
                else
                    "${RTL}زمان دلخواه",
                fontSize = 13.sp,
                color = if (isCustom)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (reminderHour != null && reminderMinute != null) {
                IconButton(
                    onClick = onClearReminder,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "حذف یادآوری",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // ── Custom time picker sheet ──
    if (showCustomPicker) {
        TimePickerSheet(
            initialHour = reminderHour ?: 8,
            initialMinute = reminderMinute ?: 0,
            onConfirm = { h, m ->
                onSetReminder(h, m)
                showCustomPicker = false
            },
            onDismiss = { showCustomPicker = false }
        )
    }
}

// ── Preset chip ──

@Composable
private fun ReminderPresetChip(
    preset: ReminderPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        label = "chip_border"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else
            Color.Transparent,
        label = "chip_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chip_content"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                border = BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = preset.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = preset.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
            Text(
                text = String.format("%02d:%02d", preset.hour, preset.minute).isolated(),
                fontSize = 10.sp,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

// ── Custom Time Picker Bottom Sheet ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

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
            Text(
                text = "${RTL}زمان یادآوری",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ── Wheel time display ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelTimeColumn(
                    label = "ساعت",
                    value = selectedHour,
                    range = 0..23,
                    onValueChange = { selectedHour = it },
                    modifier = Modifier.width(80.dp)
                )
                Text(
                    text = " : ",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                WheelTimeColumn(
                    label = "دقیقه",
                    value = selectedMinute,
                    range = 0..59,
                    onValueChange = { selectedMinute = it },
                    modifier = Modifier.width(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Clock dial for visual selection ──
            CompactClockDial(
                hour = selectedHour,
                minute = selectedMinute,
                onHourChange = { selectedHour = it },
                onMinuteChange = { selectedMinute = it },
                selectorColor = MaterialTheme.colorScheme.primary,
                dialColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                selectedNumberColor = MaterialTheme.colorScheme.onPrimary,
                unselectedNumberColor = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                    onClick = { onConfirm(selectedHour, selectedMinute) },
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

// ── Scrollable Wheel Column ──

private const val WHEEL_ITEM_HEIGHT = 40 // dp per row
private const val WHEEL_VISIBLE_ROWS = 3 // only 3 rows visible at once

@Composable
private fun WheelTimeColumn(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemCount = range.last - range.first + 1
    val initialIndex = (value - range.first).coerceIn(0, itemCount - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    // Derive the centred value from scroll position + pixel offset
    LaunchedEffect(listState) {
        snapshotFlow {
            val idx = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            // Each item is WHEEL_ITEM_HEIGHT dp; convert offset to item fraction
            val fraction = offset.toFloat() / (WHEEL_ITEM_HEIGHT * android.content.res.Resources.getSystem().displayMetrics.density)
            val raw = range.first + idx + fraction
            raw.roundToInt().coerceIn(range.first, range.last)
        }.collect { snapped ->
            if (snapped != value) onValueChange(snapped)
        }
    }

    // When value changes externally (e.g. from clock dial), scroll to it
    LaunchedEffect(value) {
        val target = (value - range.first).coerceIn(0, itemCount - 1)
        val current = listState.firstVisibleItemIndex
        if (current != target) {
            listState.animateScrollToItem(target)
        }
    }

    val wheelHeight = (WHEEL_ITEM_HEIGHT * WHEEL_VISIBLE_ROWS).dp
    val halfItem = WHEEL_ITEM_HEIGHT / 2

    Box(
        modifier = modifier
            .height(wheelHeight)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        // Highlight bar behind the centre row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WHEEL_ITEM_HEIGHT.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
        )

        // Fade edges — top & bottom 1.5 rows
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((WHEEL_ITEM_HEIGHT * 1.5).dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((WHEEL_ITEM_HEIGHT * 1.5).dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )

        // The scrollable list — padded so first/last items can scroll to centre
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = halfItem.dp),
            flingBehavior = rememberSnapFlingBehavior(listState)
        ) {
            items(itemCount) { index ->
                val itemValue = range.first + index
                val isSelected = itemValue == value
                val distance = kotlin.math.abs(
                    (listState.firstVisibleItemIndex + listState.firstVisibleItemScrollOffset.toFloat() /
                            (WHEEL_ITEM_HEIGHT * android.content.res.Resources.getSystem().displayMetrics.density)) - index
                ).toInt()
                val alpha = (1f - (distance * 0.30f)).coerceIn(0.35f, 1f)
                val textSize = if (isSelected) 24.sp else 16.sp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WHEEL_ITEM_HEIGHT.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", itemValue),
                        fontSize = textSize,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                    )
                }
            }
        }
    }
}

// ── Compact Clock Dial ──

@Composable
private fun CompactClockDial(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    selectorColor: Color,
    dialColor: Color,
    selectedNumberColor: Color,
    unselectedNumberColor: Color
) {
    var isMinuteMode by remember { mutableStateOf(false) }
    val numbers = if (isMinuteMode) (0..55 step 5).toList() else (0..11).toList()
    val totalSlots = 12

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Clock face
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(110.dp))
                .background(dialColor),
            contentAlignment = Alignment.Center
        ) {
            // Center dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(selectorColor)
            )

            // Numbers around the dial
            val dialRadius = 85.dp

            numbers.forEachIndexed { index, num ->
                val itemAngle = (index * (360f / totalSlots) - 90f)
                val itemRad = Math.toRadians(itemAngle.toDouble())

                val isSelected = when {
                    isMinuteMode -> (minute / 5) == index
                    else -> {
                        // Convert 24h to 12h clock position
                        val clockHour = when (hour) {
                            0 -> 12
                            in 1..12 -> hour
                            else -> hour - 12
                        }
                        clockHour == (if (index == 0) 12 else index)
                    }
                }

                val displayNum = if (!isMinuteMode && num == 0) 12 else num

                // Calculate position using polar coordinates
                val offsetX = with(LocalDensity.current) {
                    (dialRadius * kotlin.math.cos(itemRad).toFloat())
                }
                val offsetY = with(LocalDensity.current) {
                    (dialRadius * kotlin.math.sin(itemRad).toFloat())
                }

                Box(
                    modifier = Modifier
                        .offset(x = offsetX - 16.dp, y = offsetY - 16.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) selectorColor else Color.Transparent)
                        .clickable {
                            if (isMinuteMode) {
                                onMinuteChange(num)
                            } else {
                                // Clicking 12 (num=0) should set hour to 12, not 0
                                onHourChange(if (num == 0) 12 else num)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", displayNum),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) selectedNumberColor else unselectedNumberColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mode toggle chips (outside the clock)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ClockModeChip(
                label = "ساعت",
                isSelected = !isMinuteMode,
                onClick = { isMinuteMode = false },
                selectedColor = selectorColor
            )
            ClockModeChip(
                label = "دقیقه",
                isSelected = isMinuteMode,
                onClick = { isMinuteMode = true },
                selectedColor = selectorColor
            )
        }
    }
}

@Composable
private fun ClockModeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) selectedColor.copy(alpha = 0.15f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, selectedColor) else null
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

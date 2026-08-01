package com.example.plugins.planner.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import com.example.core.util.isolated
import com.example.ui.theme.*

// ── Preset data model ──

data class ReminderPreset(
    val label: String,
    val icon: String,
    val time: String,
    val hour: Int,
    val minute: Int
)

private val defaultPresets = listOf(
    ReminderPreset("صبح", "☀️", "08:00", 8, 0),
    ReminderPreset("عصر", "☁️", "14:00", 14, 0),
    ReminderPreset("شب", "🌙", "21:00", 21, 0)
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
        VisionReminderTimePicker(
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
            Text(
                text = preset.icon,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${RTL}${preset.label}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
            Text(
                text = preset.time.isolated(),
                fontSize = 10.sp,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

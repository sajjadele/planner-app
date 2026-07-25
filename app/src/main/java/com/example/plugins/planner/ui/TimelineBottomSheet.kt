package com.example.plugins.planner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.formatPersianTime
import com.example.core.util.JalaliDate
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityEventEntity
import com.example.plugins.planner.data.ActivityEventType
import com.example.plugins.planner.ui.components.NeumorphicSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineBottomSheet(
    visible: Boolean,
    selectedDate: Long,
    timelineStartDate: Long,
    timelineEndDate: Long,
    activities: List<ActivityEventEntity>,
    onDismiss: () -> Unit,
    onSelectDate: (Long) -> Unit,
    onGoToToday: () -> Unit,
    onMoveDate: (Int) -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        TimelineSheetContent(
            selectedDate = selectedDate,
            timelineStartDate = timelineStartDate,
            timelineEndDate = timelineEndDate,
            activities = activities,
            onGoToToday = onGoToToday,
            onMoveDate = onMoveDate
        )
    }
}

@Composable
private fun TimelineSheetContent(
    selectedDate: Long,
    timelineStartDate: Long,
    timelineEndDate: Long,
    activities: List<ActivityEventEntity>,
    onGoToToday: () -> Unit,
    onMoveDate: (Int) -> Unit
) {
    val activityGroups = remember(activities) { groupActivitiesByDay(activities) }
    val listState = rememberLazyListState()

    val canMovePrevious = selectedDate > timelineStartDate
    val canMoveNext = selectedDate < timelineEndDate

    val rangeDays = ((timelineEndDate - timelineStartDate) / 86_400_000L).toInt() + 1
    val currentDay = ((selectedDate - timelineStartDate) / 86_400_000L).toInt() + 1
    val showProgress = rangeDays > 7

    LaunchedEffect(selectedDate) {
        listState.animateScrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // ── Header ──
        Text(
            text = "${RTL}تاریخچه فعالیت",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // ── Date Navigation ──
        ActivityDateNavigation(
            selectedDate = selectedDate,
            onTodayClick = onGoToToday,
            onPreviousDay = { onMoveDate(-1) },
            onNextDay = { onMoveDate(1) },
            canMovePrevious = canMovePrevious,
            canMoveNext = canMoveNext
        )

        // ── Progress Indicator ──
        if (showProgress) {
            Text(
                text = "${RTL}روز $currentDay از $rangeDays",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Center
            )
        }

        // ── Divider ──
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Timeline List ──
        if (activities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📅", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${RTL}فعالیتی در این روز ثبت نشده",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${RTL}فعالیت‌های این تسک از امروز تا تاریخ انجام تسک قابل مشاهده هستند.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                activityGroups.forEach { group ->
                    item(key = "day_header_${group.dateKey}") {
                        ActivityDayHeader(dateKey = group.dateKey)
                    }
                    items(
                        items = group.events,
                        key = { it.id }
                    ) { event ->
                        ActivityEventItem(event = event, useTimeOnly = true)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ════════════════════════════════════════════════════════════════
// DATE NAVIGATION
// ════════════════════════════════════════════════════════════════

@Composable
private fun ActivityDateNavigation(
    selectedDate: Long,
    onTodayClick: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    canMovePrevious: Boolean,
    canMoveNext: Boolean
) {
    val jalali = remember(selectedDate) { JalaliDate.fromEpochMs(selectedDate) }
    val dateText = remember(jalali) {
        "${RTL}${jalali.day} ${JalaliDate.MONTH_NAMES[jalali.month - 1]}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AssistChip(
            onClick = onTodayClick,
            label = {
                Text(
                    text = "${RTL}امروز",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            shape = RoundedCornerShape(10.dp)
        )

        Text(
            text = dateText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row {
            IconButton(
                onClick = { if (canMovePrevious) onPreviousDay() },
                modifier = Modifier.size(32.dp),
                enabled = canMovePrevious
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "${RTL}روز قبل",
                    tint = if (canMovePrevious)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = 90f }
                )
            }
            IconButton(
                onClick = { if (canMoveNext) onNextDay() },
                modifier = Modifier.size(32.dp),
                enabled = canMoveNext
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "${RTL}روز بعد",
                    tint = if (canMoveNext)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = -90f }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// ACTIVITY GROUPING
// ════════════════════════════════════════════════════════════════

private data class ActivityGroup(
    val dateKey: Long,
    val events: List<ActivityEventEntity>
)

private fun groupActivitiesByDay(activities: List<ActivityEventEntity>): List<ActivityGroup> {
    val grouped = activities.groupBy {
        JalaliDate.toEpochMs(JalaliDate.fromEpochMs(it.timestamp))
    }
    return grouped.entries
        .map { ActivityGroup(it.key, it.value.sortedByDescending { e -> e.timestamp }) }
        .sortedByDescending { it.dateKey }
}

@Composable
private fun ActivityDayHeader(dateKey: Long) {
    val todayKey = remember { JalaliDate.toEpochMs(JalaliDate.today()) }
    val yesterdayDate = remember {
        val t = JalaliDate.today()
        JalaliDate.toEpochMs(JalaliDate(t.year, t.month, t.day - 1))
    }

    val label = when (dateKey) {
        todayKey -> "${RTL}امروز"
        yesterdayDate -> "${RTL}دیروز"
        else -> {
            val jDate = JalaliDate.fromEpochMs(dateKey)
            "${RTL}${jDate.day} ${JalaliDate.MONTH_NAMES[jDate.month - 1]} ${jDate.year}"
        }
    }

    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

// ════════════════════════════════════════════════════════════════
// ACTIVITY EVENT ITEM
// ════════════════════════════════════════════════════════════════

@Composable
private fun ActivityEventItem(event: ActivityEventEntity, useTimeOnly: Boolean = false) {
    val eventType = try {
        ActivityEventType.valueOf(event.eventType)
    } catch (_: Exception) {
        null
    }

    val title = when (eventType) {
        ActivityEventType.STEP_CREATED -> "${RTL}مرحله ایجاد شد"
        ActivityEventType.STEP_COMPLETED -> "${RTL}مرحله تکمیل شد"
        ActivityEventType.STEP_REOPENED -> "${RTL}مرحله بازگشایی شد"
        ActivityEventType.STEP_DELETED -> "${RTL}مرحله حذف شد"
        ActivityEventType.NOTE_ADDED -> "${RTL}یادداشت اضافه شد"
        ActivityEventType.FILE_ADDED -> "${RTL}فایل اضافه شد"
        null -> "${RTL}${event.eventType}"
    }

    val icon = when (eventType) {
        ActivityEventType.STEP_CREATED -> "＋"
        ActivityEventType.STEP_COMPLETED -> "✓"
        ActivityEventType.STEP_REOPENED -> "↻"
        ActivityEventType.STEP_DELETED -> "✕"
        ActivityEventType.NOTE_ADDED -> "📝"
        ActivityEventType.FILE_ADDED -> "📎"
        null -> "•"
    }

    val timeText = remember(event.timestamp) {
        if (useTimeOnly) {
            SimpleDateFormat("HH:mm", Locale.US).format(Date(event.timestamp))
        } else {
            formatPersianTime(event.timestamp)
        }
    }

    NeumorphicSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = icon,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            event.description?.let { desc ->
                if (desc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${RTL}$desc",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = timeText,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 22.dp)
            )
        }
    }
}

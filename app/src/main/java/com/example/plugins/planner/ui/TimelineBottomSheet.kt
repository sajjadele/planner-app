package com.example.plugins.planner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.calendar.PersianCalendarDialog
import com.example.core.util.JalaliDate
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityMessageModel
import com.example.plugins.planner.ui.components.ActivityMessageCard
import com.example.plugins.planner.ui.components.NeumorphicSurface

private const val DAY_MILLIS = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineBottomSheet(
    visible: Boolean,
    selectedDate: Long,
    timelineStartDate: Long,
    timelineEndDate: Long,
    messages: List<ActivityMessageModel>,
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
            messages = messages,
            onGoToToday = onGoToToday,
            onMoveDate = onMoveDate,
            onSelectDate = onSelectDate
        )
    }
}

@Composable
private fun TimelineSheetContent(
    selectedDate: Long,
    timelineStartDate: Long,
    timelineEndDate: Long,
    messages: List<ActivityMessageModel>,
    onGoToToday: () -> Unit,
    onMoveDate: (Int) -> Unit,
    onSelectDate: (Long) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val activityGroups = remember(messages) { groupMessagesByDay(messages) }
    val listState = rememberLazyListState()

    val canMovePrevious = selectedDate > timelineStartDate
    val canMoveNext = selectedDate < timelineEndDate

    val rangeDays = maxOf(1, ((timelineEndDate - timelineStartDate) / DAY_MILLIS).toInt() + 1)
    val currentDay = maxOf(1, ((selectedDate - timelineStartDate) / DAY_MILLIS).toInt() + 1)
    val showProgress = rangeDays > 7

    var showCalendarDialog by remember { mutableStateOf(false) }

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
            canMoveNext = canMoveNext,
            onOpenCalendar = { showCalendarDialog = true }
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
        if (messages.isEmpty()) {
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                activityGroups.forEach { group ->
                    item(key = "day_header_${group.dateKey}") {
                        ActivityDayHeader(dateKey = group.dateKey)
                    }
                    items(
                        items = group.messages,
                        key = { it.id }
                    ) { message ->
                        ActivityMessageCard(message = message)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showCalendarDialog) {
        PersianCalendarDialog(
            selectedDateEpochMs = selectedDate,
            onDateSelected = { date ->
                onSelectDate(date)
                showCalendarDialog = false
            },
            onDismiss = { showCalendarDialog = false },
            minSelectableDate = timelineStartDate,
            maxSelectableDate = timelineEndDate,
            confirmButtonText = "${RTL}انتخاب",
            showConfirmButton = true
        )
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
    canMoveNext: Boolean,
    onOpenCalendar: () -> Unit
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

        Row(
            modifier = Modifier.clickable(onClick = onOpenCalendar),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "${RTL}انتخاب تاریخ",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = dateText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

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
// MESSAGE GROUPING BY DAY
// ════════════════════════════════════════════════════════════════

private data class MessageGroup(
    val dateKey: Long,
    val messages: List<ActivityMessageModel>
)

private fun groupMessagesByDay(messages: List<ActivityMessageModel>): List<MessageGroup> {
    val grouped = messages.groupBy {
        JalaliDate.toEpochMs(JalaliDate.fromEpochMs(it.timestamp))
    }
    return grouped.entries
        .map { MessageGroup(it.key, it.value.sortedByDescending { m -> m.timestamp }) }
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

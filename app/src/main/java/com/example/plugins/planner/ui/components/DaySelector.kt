package com.example.plugins.planner.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.calendar.PersianCalendarDialog
import com.example.core.calendar.PersianCalendarGrid
import com.example.core.calendar.PersianCalendarState
import com.example.core.calendar.rememberPersianCalendarState
import com.example.core.data.HolidayRepository
import com.example.core.domain.CalendarDate
import com.example.core.domain.DayContext
import com.example.core.util.JalaliDate
import com.example.core.util.isolated
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.HolidayRed
import java.util.Calendar

// ────────────────────────────────────────────────────────────
// Public utilities — delegate to JalaliDate
// ────────────────────────────────────────────────────────────

/** Midnight epoch ms of the Saturday (شنبه) of the week containing [dateEpochMs]. */
fun getSaturdayOfWeek(dateEpochMs: Long): Long = JalaliDate.saturdayOfWeek(dateEpochMs)

/** Persian day index (0=Sat … 6=Fri) from epoch ms. */
fun persianDayIndex(dateEpochMs: Long): Int = JalaliDate.dayOfWeekIndex(dateEpochMs)

/** Today at local midnight. */
fun todayDateEpochMs(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** Epoch ms of the Saturday of the week at [offsetWeeks] from today. */
private fun saturdayAtWeekOffset(offsetWeeks: Int): Long {
    val today = todayDateEpochMs()
    val thisSaturday = JalaliDate.saturdayOfWeek(today)
    return thisSaturday + offsetWeeks * 7L * 86400000L
}

private const val WEEK_WINDOW = 1000

// ────────────────────────────────────────────────────────────
// Infinite Horizontal Week Row
// ────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfiniteWeekRow(
    selectedDateEpochMs: Long,
    onDateSelected: (Long) -> Unit,
    daysWithTasks: Set<Long> = emptySet(),
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState()
) {
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val todayEpochMs = remember { todayDateEpochMs() }

    val initialIndex = remember {
        val thisSaturday = JalaliDate.saturdayOfWeek(todayEpochMs)
        val selectedSaturday = JalaliDate.saturdayOfWeek(selectedDateEpochMs)
        ((selectedSaturday - thisSaturday) / (7L * 86400000L) + WEEK_WINDOW).toInt()
    }

    LaunchedEffect(Unit) {
        listState.scrollToItem(initialIndex)
    }

    LazyRow(
        state = listState,
        flingBehavior = snapBehavior,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        itemsIndexed(items = List(WEEK_WINDOW * 2) { it }, itemContent = { index, _ ->
            val saturdayEpoch = saturdayAtWeekOffset(index - WEEK_WINDOW)
            WeekRow(
                saturdayEpoch = saturdayEpoch,
                selectedDateEpochMs = selectedDateEpochMs,
                todayEpochMs = todayEpochMs,
                daysWithTasks = daysWithTasks,
                onDateSelected = onDateSelected
            )
        })
    }
}

@Composable
private fun WeekRow(
    saturdayEpoch: Long,
    selectedDateEpochMs: Long,
    todayEpochMs: Long,
    daysWithTasks: Set<Long> = emptySet(),
    onDateSelected: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (dayIndex in 0..6) {
            val dayEpochMs = saturdayEpoch + dayIndex * 86400000L
            DayCell(
                dayIndex = dayIndex,
                dayEpochMs = dayEpochMs,
                isSelected = dayEpochMs == selectedDateEpochMs,
                isToday = dayEpochMs == todayEpochMs,
                hasTasks = dayEpochMs in daysWithTasks,
                onClick = { onDateSelected(dayEpochMs) }
            )
        }
    }
}

@Composable
private fun DayCell(
    dayIndex: Int,
    dayEpochMs: Long,
    isSelected: Boolean,
    isToday: Boolean,
    hasTasks: Boolean = false,
    onClick: () -> Unit
) {
    val jalali = remember(dayEpochMs) { JalaliDate.fromEpochMs(dayEpochMs) }
    val dayNumber = jalali.day
    val dayLetter = JalaliDate.DAY_LETTERS[dayIndex]

    val circleSize = 40.dp
    val selectedColor = MaterialTheme.colorScheme.primary
    val todayBorderColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .then(
                    when {
                        isSelected -> Modifier
                            .clip(CircleShape)
                            .background(selectedColor)
                        isToday -> Modifier
                            .clip(CircleShape)
                            .border(2.dp, todayBorderColor, CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                        else -> Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayNumber.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = dayLetter,
            fontSize = 11.sp,
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (hasTasks && !isSelected) AccentGreen else Color.Transparent)
        )
    }
}

// ────────────────────────────────────────────────────────────
// Calendar Popup — uses core PersianCalendarGrid
// ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarPopup(
    selectedDateEpochMs: Long,
    daysWithTasks: Set<Long> = emptySet(),
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val holidayRepo = remember { HolidayRepository(context) }
    val state = rememberPersianCalendarState(selectedDateEpochMs)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        PersianCalendarGrid(
            state = state,
            onDateSelected = onDateSelected,
            onDismiss = onDismiss,
            holidayRepository = holidayRepo,
            daysWithIndicators = daysWithTasks,
            indicatorColor = AccentGreen,
            showIndicator = { it in daysWithTasks },
            confirmButtonText = "مشاهده برنامه‌های این روز",
            showConfirmButton = true,
            dayContextContent = { dayContext ->
                var showDetails by remember { mutableStateOf(false) }
                DayContextHeader(
                    calendarDate = dayContext.date,
                    isExpanded = showDetails,
                    onToggle = { showDetails = !showDetails }
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = showDetails,
                    enter = androidx.compose.animation.expandVertically(),
                    exit = androidx.compose.animation.shrinkVertically()
                ) {
                    DayContextDetails(context = dayContext)
                }
            }
        )
    }
}

package com.example.plugins.planner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.HolidayRepository
import com.example.core.domain.CalendarDate
import com.example.core.domain.DayContext
import com.example.core.util.JalaliDate
import java.util.Calendar
import java.util.Locale

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

private const val WEEK_WINDOW = 1000 // ±19 years — more than enough

// ────────────────────────────────────────────────────────────
// Infinite Horizontal Week Row
// ────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfiniteWeekRow(
    selectedDateEpochMs: Long,
    onDateSelected: (Long) -> Unit,
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
    onClick: () -> Unit
) {
    val jalali = remember(dayEpochMs) { JalaliDate.fromEpochMs(dayEpochMs) }
    val dayNumber = jalali.day
    val dayLetter = JalaliDate.DAY_LETTERS[dayIndex]

    val circleSize = 40.dp
    val borderColor = MaterialTheme.colorScheme.primary
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
    }
}

// ────────────────────────────────────────────────────────────
// Calendar Popup — True Jalali Grid
// ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarPopup(
    selectedDateEpochMs: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val holidayRepo = remember { HolidayRepository(context) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        CalendarGrid(
            selectedDateEpochMs = selectedDateEpochMs,
            onDateSelected = onDateSelected,
            onDone = onDismiss,
            holidayRepo = holidayRepo
        )
    }
}

@Composable
private fun CalendarGrid(
    selectedDateEpochMs: Long,
    onDateSelected: (Long) -> Unit,
    onDone: () -> Unit,
    holidayRepo: HolidayRepository? = null
) {
    // Local preview selection — does not apply globally until user confirms.
    val localSelectedEpochMs = remember { mutableStateOf(selectedDateEpochMs) }
    val selectedJalali = remember(localSelectedEpochMs.value) { JalaliDate.fromEpochMs(localSelectedEpochMs.value) }
    val todayJalali = remember { JalaliDate.today() }
    var monthOffset by remember { mutableIntStateOf(0) }

    val targetMonth = remember(monthOffset) {
        var m = selectedJalali.month + monthOffset
        var y = selectedJalali.year
        while (m > 12) { m -= 12; y++ }
        while (m < 1) { m += 12; y-- }
        y to m
    }
    val (jYear, jMonth) = targetMonth

    val daysInJalaliMonth = remember(jYear, jMonth) { JalaliDate.monthLength(jYear, jMonth) }

    // First day of month → day-of-week (0=Sat)
    val firstDayEpochMs = remember(jYear, jMonth) {
        JalaliDate.toEpochMs(JalaliDate(jYear, jMonth, 1))
    }
    val startDow = remember(firstDayEpochMs) { JalaliDate.dayOfWeekIndex(firstDayEpochMs) }

    // Is the selected date in this month?
    val isSelectedMonth = jYear == selectedJalali.year && jMonth == selectedJalali.month
    val isTodayMonth = jYear == todayJalali.year && jMonth == todayJalali.month

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // ── Month / Year header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { monthOffset-- }) {
                Text("‹", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
            }
            // Month/year text intentionally omitted — clean minimal header
            TextButton(onClick = { monthOffset++ }) {
                Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Weekday header row (Sat → Fri) ──
        Row(modifier = Modifier.fillMaxWidth()) {
            JalaliDate.DAY_NAMES.forEach { name ->
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Calendar grid ──
        val totalCells = startDow + daysInJalaliMonth
        val rows = (totalCells + 6) / 7
        var dayNum = 1
        val cellShape = RoundedCornerShape(12.dp)

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val hasDay = cellIndex >= startDow && dayNum <= daysInJalaliMonth
                    val thisDay = if (hasDay) dayNum else 0
                    if (hasDay) dayNum++

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasDay) {
                            val cellDate = remember(jYear, jMonth, thisDay) {
                                val epochMs = JalaliDate.toEpochMs(JalaliDate(jYear, jMonth, thisDay))
                                CalendarDate.fromEpochMs(epochMs)
                            }
                            val isDaySelected = isSelectedMonth && cellDate.jalaliDay == selectedJalali.day
                            val isDayToday = isTodayMonth && cellDate.isToday

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(4.dp)
                                    .clip(cellShape)
                                    .then(
                                        when {
                                            isDaySelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                                            isDayToday -> Modifier
                                                .border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, cellShape)
                                            else -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        }
                                    )
                                    .clickable {
                                        localSelectedEpochMs.value = cellDate.epochMs
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cellDate.jalaliDay.toString(),
                                    fontSize = 15.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = if (isDaySelected || isDayToday) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isDaySelected -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }

                }

            // ── Day Context panel (progressive disclosure) ──
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))

            var showDetails by remember { mutableStateOf(false) }
            val panelDate = remember(localSelectedEpochMs.value) {
                CalendarDate.fromEpochMs(localSelectedEpochMs.value)
            }
            val panelContext = remember(panelDate, holidayRepo) {
                val holidays = if (holidayRepo != null) {
                    holidayRepo.getHolidays(panelDate)
                } else emptyList()
                DayContext(date = panelDate, holidays = holidays)
            }

            DayContextHeader(
                calendarDate = panelDate,
                isExpanded = showDetails,
                onToggle = { showDetails = !showDetails }
            )

            AnimatedVisibility(
                visible = showDetails,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                DayContextDetails(context = panelContext)
            }

            // ── Confirm button ──
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onDateSelected(localSelectedEpochMs.value)
                    onDone()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("مشاهده برنامه‌های این روز")
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

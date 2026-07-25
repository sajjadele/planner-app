package com.example.core.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.data.HolidayRepository
import com.example.core.domain.CalendarDate
import com.example.core.domain.DayContext
import com.example.core.util.JalaliDate
import com.example.core.util.isolated
import com.example.ui.theme.HolidayRed

@Composable
fun PersianCalendarGrid(
    state: PersianCalendarState,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    holidayRepository: HolidayRepository? = null,
    daysWithIndicators: Set<Long> = emptySet(),
    indicatorColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent,
    showIndicator: (Long) -> Boolean = { false },
    confirmButtonText: String = "تأیید",
    showConfirmButton: Boolean = true,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    dayContextContent: (@Composable ColumnScope.(DayContext) -> Unit)? = null,
    minSelectableDate: Long? = null,
    maxSelectableDate: Long? = null
) {
    val (jYear, jMonth) = state.targetMonth()
    val selectedJalali = state.selectedJalali
    val todayJalali = state.todayJalali

    val gregorianMonthRange = remember(jYear, jMonth) {
        JalaliDate.getGregorianMonthRange(jYear, jMonth)
    }

    val daysInJalaliMonth = remember(jYear, jMonth) { JalaliDate.monthLength(jYear, jMonth) }

    val firstDayEpochMs = remember(jYear, jMonth) {
        JalaliDate.toEpochMs(JalaliDate(jYear, jMonth, 1))
    }
    val startDow = remember(firstDayEpochMs) { JalaliDate.dayOfWeekIndex(firstDayEpochMs) }

    val holidayDays = remember(jYear, jMonth, holidayRepository) {
        if (holidayRepository == null) emptySet()
        else {
            (1..daysInJalaliMonth).mapNotNull { d ->
                val date = CalendarDate.fromEpochMs(
                    JalaliDate.toEpochMs(JalaliDate(jYear, jMonth, d))
                )
                if (holidayRepository.isHoliday(date)) d else null
            }.toSet()
        }
    }

    val isSelectedMonth = jYear == selectedJalali.year && jMonth == selectedJalali.month
    val isTodayMonth = jYear == todayJalali.year && jMonth == todayJalali.month

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // Month/Year header with navigation
        var monthPickerVisible by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { state.previousMonth() }) {
                Text("‹", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
            }

            Box(contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { monthPickerVisible = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${JalaliDate.MONTH_NAMES[jMonth - 1]} ${jYear.isolated()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = gregorianMonthRange,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextButton(onClick = { state.nextMonth() }) {
                Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Month picker dialog
        if (monthPickerVisible) {
            MonthPickerDialog(
                jYear = jYear,
                jMonth = jMonth,
                onMonthSelected = { monthDelta -> state.jumpToMonth(monthDelta) },
                onDismiss = { monthPickerVisible = false }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Weekday header row (Sat → Fri)
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

        // Calendar grid
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
                            val isHoliday = hasDay && thisDay in holidayDays
                            val showDayIndicator = hasDay && showIndicator(cellDate.epochMs)
                            val isSelectable = state.isSelectable(cellDate.epochMs)

                            val cellAlpha = if (isSelectable || isDaySelected || isDayToday) 1f else 0.35f

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(4.dp)
                                    .clip(cellShape)
                                    .then(
                                        when {
                                            isDaySelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                                            isDayToday -> Modifier
                                                .border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, cellShape)
                                            else -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f * cellAlpha))
                                        }
                                    )
                                    .then(
                                        if (isSelectable)
                                            Modifier.clickable { state.selectDate(cellDate.epochMs) }
                                        else Modifier
                                    )
                            ) {
                                Text(
                                    text = cellDate.jalaliDay.toString(),
                                    fontSize = 15.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = if (isDaySelected || isDayToday) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isDaySelected -> MaterialTheme.colorScheme.onPrimary
                                        isHoliday -> HolidayRed.copy(alpha = cellAlpha)
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = cellAlpha)
                                    }
                                )
                                if ((isHoliday || showDayIndicator) && !isDaySelected) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isHoliday) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(HolidayRed)
                                            )
                                        }
                                        if (showDayIndicator) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(indicatorColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Day Context panel slot
        if (dayContextContent != null) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))

            var showDetails by remember { mutableStateOf(false) }
            val panelDate = remember(state.localSelectedEpochMs.value) {
                CalendarDate.fromEpochMs(state.localSelectedEpochMs.value)
            }
            val panelContext = remember(panelDate, holidayRepository) {
                val holidays = if (holidayRepository != null) {
                    holidayRepository.getHolidays(panelDate)
                } else emptyList()
                DayContext(date = panelDate, holidays = holidays)
            }

            dayContextContent(panelContext)
        }

        // Header content slot (e.g., confirm button area)
        if (headerContent != null) {
            headerContent()
        }

        // Confirm button
        if (showConfirmButton) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onDateSelected(state.localSelectedEpochMs.value)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmButtonText)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun MonthPickerDialog(
    jYear: Int,
    jMonth: Int,
    onMonthSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "انتخاب ماه",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val months = (0..11).chunked(3)
                months.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { idx ->
                            val monthNum = idx + 1
                            val isActive = monthNum == jMonth
                            val gReg = remember(jYear, monthNum) {
                                JalaliDate.getGregorianMonthRange(jYear, monthNum)
                                    .split(" ").first()
                            }
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.3f)
                                    .clickable {
                                        onMonthSelected(monthNum - jMonth)
                                        onDismiss()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = if (isActive) BorderStroke(
                                    1.5.dp, MaterialTheme.colorScheme.primary
                                ) else null
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = JalaliDate.MONTH_NAMES[idx],
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = gReg,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

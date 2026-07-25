package com.example.core.calendar

import androidx.compose.runtime.*
import com.example.core.util.JalaliDate

@Composable
fun rememberPersianCalendarState(
    initialDateEpochMs: Long = System.currentTimeMillis()
): PersianCalendarState {
    return remember { PersianCalendarState(initialDateEpochMs) }
}

class PersianCalendarState(
    initialDateEpochMs: Long
) {
    private val _localSelectedEpochMs = mutableStateOf(initialDateEpochMs)
    val localSelectedEpochMs: State<Long> = _localSelectedEpochMs

    private val _monthOffset = mutableIntStateOf(0)
    val monthOffset: State<Int> = _monthOffset

    val selectedJalali: JalaliDate
        @Composable get() = remember(_localSelectedEpochMs.value) {
            JalaliDate.fromEpochMs(_localSelectedEpochMs.value)
        }

    val todayJalali: JalaliDate
        @Composable get() = remember { JalaliDate.today() }

    @Composable
    fun targetMonth(): Pair<Int, Int> {
        val sel = selectedJalali
        val offset = _monthOffset.intValue
        return remember(sel, offset) {
            var m = sel.month + offset
            var y = sel.year
            while (m > 12) { m -= 12; y++ }
            while (m < 1) { m += 12; y-- }
            y to m
        }
    }

    fun selectDate(epochMs: Long) {
        _localSelectedEpochMs.value = epochMs
        _monthOffset.intValue = 0
    }

    fun previousMonth() {
        _monthOffset.intValue--
    }

    fun nextMonth() {
        _monthOffset.intValue++
    }

    fun jumpToMonth(monthDelta: Int) {
        _monthOffset.intValue += monthDelta
    }
}

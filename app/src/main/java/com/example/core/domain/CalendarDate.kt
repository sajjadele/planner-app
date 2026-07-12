package com.example.core.domain

import com.example.core.util.JalaliDate
import com.example.core.util.isolated
import java.util.Calendar

/**
 * Enriched calendar date value object.
 *
 * Precomputes Jalali and Gregorian fields from [epochMs] in one place,
 * so every consumer reads pre-resolved values instead of converting independently.
 *
 * All fields are computed eagerly in [fromEpochMs].
 */
data class CalendarDate(
    /** Epoch milliseconds (local midnight). */
    val epochMs: Long,
    /** Jalali year (e.g. 1405). */
    val jalaliYear: Int,
    /** Jalali month 1–12. */
    val jalaliMonth: Int,
    /** Jalali day 1–31. */
    val jalaliDay: Int,
    /** Gregorian year (e.g. 2026). */
    val gregorianYear: Int,
    /** Gregorian month 1–12. */
    val gregorianMonth: Int,
    /** Gregorian day 1–31. */
    val gregorianDay: Int,
    /** Day-of-week index 0=Saturday … 6=Friday. */
    val dayOfWeekIndex: Int,
    /** True if this date is the current real-world day. */
    val isToday: Boolean,
    /** Persian month name, e.g. "تیر". */
    val monthName: String,
    /** Persian day name, e.g. "شنبه". */
    val dayName: String
) {
    companion object {
        private val gMonthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        /**
         * Create a [CalendarDate] from epoch milliseconds (local midnight).
         */
        fun fromEpochMs(epochMs: Long): CalendarDate {
            val jalali = JalaliDate.fromEpochMs(epochMs)
            val (gYear, gMonth, gDay) = JalaliDate.toGregorian(jalali)
            val dowIndex = JalaliDate.dayOfWeekIndex(epochMs)
            val todayJalali = JalaliDate.fromEpochMs(todayEpochMs())
            val isToday = jalali.year == todayJalali.year &&
                jalali.month == todayJalali.month &&
                jalali.day == todayJalali.day

            return CalendarDate(
                epochMs = epochMs,
                jalaliYear = jalali.year,
                jalaliMonth = jalali.month,
                jalaliDay = jalali.day,
                gregorianYear = gYear,
                gregorianMonth = gMonth,
                gregorianDay = gDay,
                dayOfWeekIndex = dowIndex,
                isToday = isToday,
                monthName = JalaliDate.MONTH_NAMES[jalali.month - 1],
                dayName = JalaliDate.DAY_NAMES[dowIndex]
            )
        }

        /** Convenience: today's [CalendarDate]. */
        fun today(): CalendarDate = fromEpochMs(todayEpochMs())

        private fun todayEpochMs(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }

    /** Full Jalali date string for display, e.g. "شنبه 20 تیر 1405". */
    fun toJalaliDisplay(): String = "$dayName ${jalaliDay.isolated()} $monthName ${jalaliYear.isolated()}"

    /** Gregorian date string for display, e.g. "11 July 2026". */
    fun toGregorianDisplay(): String = "$gregorianDay ${gMonthNames[gregorianMonth - 1]} $gregorianYear"

    /** Dual date string for the top bar, e.g. "20 تیر 1405 | July 11, 2026". */
    fun toDualDisplay(): String = "${toJalaliDisplay()} | ${toGregorianDisplay()}"
}

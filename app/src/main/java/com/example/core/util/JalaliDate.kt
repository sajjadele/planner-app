package com.example.core.util

import java.util.Calendar

/**
 * Jalali (Persian / Solar Hijri) calendar date representation.
 *
 * Month lengths:
 *   Farvardin–Shahrivar (1–6): 31 days each
 *   Mehr–Bahman (7–11):       30 days each
 *   Esfand (12):               29 days (30 in leap years)
 *
 * Leap years follow a 33-year cycle: positions 1,5,9,13,17,22,26,30
 * within each 33-year block (8 leap years per cycle = 12053 days).
 *
 * Conversion uses Julian Day Numbers (JDN) as the intermediate, so both
 * directions are exact and round-trip safe.
 */
data class JalaliDate(
    val year: Int,
    val month: Int,  // 1–12
    val day: Int     // 1–31
) {
    companion object {
        private val MONTH_LENGTHS = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val MONTH_NAMES = arrayOf(
            "فروردین", "اردیبهشت", "خرداد",
            "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر",
            "دی", "بهمن", "اسفند"
        )

        /** Persian weekday names starting from Saturday. */
        val DAY_NAMES = arrayOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه")

        /** Persian weekday first letters (for compact display). */
        val DAY_LETTERS = arrayOf("ش", "ی", "د", "س", "چ", "پ", "ج")

        // JDN of 1 Farvardin 1 AH (the Jalali epoch, ~19 March 622 CE).
        private const val JALALI_EPOCH = 1948320

        // Length of one 33-year cycle: 33 × 365 + 8 leap days.
        private const val CYCLE_DAYS = 12053

        // 0-indexed positions within a 33-year cycle that are leap years.
        // 1-indexed leap years: 1, 5, 9, 13, 17, 22, 26, 30 → 8 per cycle
        private val LEAP_POSITIONS = booleanArrayOf(
            true,  false, false, false, true,   // 0-4     leap: 0, 4
            false, false, false, true,  false,  // 5-9     leap: 8
            false, false, true,  false, false,  // 10-14   leap: 12
            false, true,  false, false, false,  // 15-19   leap: 16
            false, false, true,  false, false,  // 20-24   leap: 21
            true,  false, false, false, true,   // 25-29   leap: 25, 29
            false, false, false                  // 30-32
        )

        /** True if [year] is a leap year in the Jalali 33-year cycle. */
        fun isLeapYear(year: Int): Boolean {
            val pos = ((year - 1) % 33 + 33) % 33
            return LEAP_POSITIONS[pos]
        }

        /** Number of days in a given Jalali month (1–12). */
        fun monthLength(year: Int, month: Int): Int {
            return if (month == 12 && isLeapYear(year)) 30
            else MONTH_LENGTHS[month - 1]
        }

        /** Days in a full Jalali year (365 or 366). */
        private fun yearLength(year: Int): Int = if (isLeapYear(year)) 366 else 365

        // ── JDN utilities ──────────────────────────────────────────

        /** JDN of a proleptic Gregorian date. */
        private fun gregorianJdn(year: Int, month: Int, day: Int): Int {
            val a = (14 - month) / 12
            val y = year + 4800 - a
            val m = month + 12 * a - 3
            return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        }

        /** Gregorian date (y,m,d) from a JDN. */
        private fun jdnToGregorian(jdn: Int): Triple<Int, Int, Int> {
            val a = jdn + 32044
            val b = (4 * a + 3) / 146097
            val c = a - (146097 * b) / 4
            val d = (4 * c + 3) / 1461
            val e = c - (1461 * d) / 4
            val m = (5 * e + 2) / 153
            val day = e - (153 * m + 2) / 5 + 1
            val month = m + 3 - 12 * (m / 10)
            val year = 100 * b + d - 4800 + (m / 10)
            return Triple(year, month, day)
        }

        /** Jalali date from a JDN. */
        private fun jdnToJalali(jdn: Int): JalaliDate {
            var days = jdn - JALALI_EPOCH
            // Full 33-year cycles.
            val cycles = days / CYCLE_DAYS
            days -= cycles * CYCLE_DAYS
            var jy = 1 + cycles * 33
            // Remaining years within the cycle — at most 32 iterations.
            while (true) {
                val yl = yearLength(jy)
                if (days < yl) break
                days -= yl
                jy++
            }
            // Remaining days → month and day.
            // Month 1–6: 31 days, 7–11: 30 days, 12: 29/30 days.
            var jm = 1
            while (days >= monthLength(jy, jm)) {
                days -= monthLength(jy, jm)
                jm++
            }
            val jd = days + 1
            return JalaliDate(jy, jm, jd)
        }

        // ── Public API ──────────────────────────────────────────────

        /** Today's Jalali date, derived from the system clock. */
        fun today(): JalaliDate = fromGregorian(
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH) + 1,
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        )

        /** Epoch milliseconds → Jalali date. */
        fun fromEpochMs(epochMs: Long): JalaliDate {
            val cal = Calendar.getInstance()
            cal.timeInMillis = epochMs
            return fromGregorian(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        /** Jalali date → epoch milliseconds (local midnight). */
        fun toEpochMs(jalali: JalaliDate): Long {
            val (gYear, gMonth, gDay) = toGregorian(jalali)
            val cal = Calendar.getInstance()
            cal.set(gYear, gMonth - 1, gDay, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        /** Gregorian → Jalali conversion. */
        fun fromGregorian(gYear: Int, gMonth: Int, gDay: Int): JalaliDate {
            val jdn = gregorianJdn(gYear, gMonth, gDay)
            return jdnToJalali(jdn)
        }

        /** Jalali → Gregorian conversion. */
        fun toGregorian(jalali: JalaliDate): Triple<Int, Int, Int> {
            val jdn = jalaliToJdn(jalali)
            return jdnToGregorian(jdn)
        }

        /** JDN for a given Jalali date. */
        private fun jalaliToJdn(jalali: JalaliDate): Int {
            var days = 0
            // Full 33-year cycles before this year.
            val cycles = (jalali.year - 1) / 33
            days += cycles * CYCLE_DAYS
            // Remaining whole years since the last cycle start.
            val startOfCycle = 1 + cycles * 33
            for (y in startOfCycle until jalali.year) {
                days += yearLength(y)
            }
            // Months of the current year.
            for (m in 1 until jalali.month) {
                days += monthLength(jalali.year, m)
            }
            days += jalali.day - 1
            return JALALI_EPOCH + days
        }

        /**
         * Persian day-of-week index (0=Saturday … 6=Friday) from epoch milliseconds.
         *
         * Calendar.DAY_OF_WEEK: Sun=1, Mon=2, Tue=3, Wed=4, Thu=5, Fri=6, Sat=7.
         * We map: Sat→0, Sun→1, …, Fri→6.
         * So formula: gDow % 7
         *   Sun(1)%7=1→Mon ✗   → we actually want Sat=0, Sun=1, Mon=2, Tue=3, Wed=4, Thu=5, Fri=6
         *   Wait: gDow=1(Sun)→1 ✓, Sat=7→0 ✓
         * Hmm, let me re-check: we need Sat=0. Calendar.SATURDAY=7. 7%7=0 ✓.
         * We need Sun=1. Calendar.SUNDAY=1. 1%7=1 ✓.
         * We need Fri=6. Calendar.FRIDAY=6. 6%7=6 ✓.
         * Correct!
         */
        fun dayOfWeekIndex(epochMs: Long): Int {
            val cal = Calendar.getInstance()
            cal.timeInMillis = epochMs
            return cal.get(Calendar.DAY_OF_WEEK) % 7 // Sun=1→1=Mon, Sat=7→0=Sat
        }

        /**
         * Epoch milliseconds of the Saturday (شنبه) of the week containing [epochMs].
         */
        fun saturdayOfWeek(epochMs: Long): Long {
            val dow = dayOfWeekIndex(epochMs) // 0=Saturday
            val adjusted = epochMs - dow * 86400000L
            val cal = Calendar.getInstance()
            cal.timeInMillis = adjusted
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        /**
         * Gregorian month range for a Jalali month, e.g. "June - July".
         */
        fun getGregorianMonthRange(jalaliYear: Int, jalaliMonth: Int): String {
            val firstGreg = toGregorian(JalaliDate(jalaliYear, jalaliMonth, 1))
            val lastDay = monthLength(jalaliYear, jalaliMonth)
            val lastGreg = toGregorian(JalaliDate(jalaliYear, jalaliMonth, lastDay))
            val months = arrayOf("January","February","March","April","May","June","July","August","September","October","November","December")
            val firstMonth = months[firstGreg.second - 1]
            val lastMonth = months[lastGreg.second - 1]
            return if (firstGreg.first == lastGreg.first) {
                if (firstGreg.second == lastGreg.second) "$firstMonth ${firstGreg.first}"
                else "$firstMonth - $lastMonth ${firstGreg.first}"
            } else {
                "$firstMonth ${firstGreg.first} - $lastMonth ${lastGreg.first}"
            }
        }
    }

    /** Full Persian date string with English digits, e.g. "1405/04/20". */
    fun toEnglishString(): String {
        return "$year/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}"
    }
}

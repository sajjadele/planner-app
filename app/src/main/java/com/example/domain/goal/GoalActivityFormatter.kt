package com.example.domain.goal

import com.example.core.util.RTL
import com.example.core.util.toEnglishDigits
import java.util.concurrent.TimeUnit

/**
 * Pure-Kotlin Persian text formatters for Goal activity/deadline display. No Android imports —
 * host-JVM testable. Vision Planner uses Persian UI with English numbers, so counts are rendered
 * with [toEnglishDigits]; RTL marks are applied so embedded numerals do not reorder inside Persian
 * (RTL) sentences.
 */
object GoalActivityFormatter {

    private val DAY_MS = TimeUnit.DAYS.toMillis(1)

    /** "12 روز فعالیت" — lifetime active-day count. */
    fun formatActiveDays(count: Int): String {
        if (count <= 0) return ""
        return "${RTL}${count.toEnglishDigits()} روز فعالیت"
    }

    /**
     * Relative last-activity wording:
     *  - today  -> "امروز"
     *  - yesterday -> "دیروز"
     *  - N days ago -> "۳ روز پیش"
     *  - null / unknown -> "" (caller hides the line)
     */
    fun formatLastActivity(timestamp: Long?, nowMillis: Long = System.currentTimeMillis()): String {
        if (timestamp == null) return ""
        val diffDays = ((nowMillis - timestamp) + DAY_MS - 1) / DAY_MS // ceil to whole days
        return when {
            diffDays <= 0L -> "امروز"
            diffDays == 1L -> "دیروز"
            else -> "${RTL}${diffDays.toEnglishDigits()} روز پیش"
        }
    }

    /**
     * Deadline wording. Only returns text when the deadline is within [showWithinDays] days
     * (default 7) so the card stays quiet for distant deadlines. Returns null otherwise.
     * Wording: "تا ۵ روز دیگر".
     */
    fun formatDeadline(
        deadlineEpochMs: Long?,
        nowMillis: Long = System.currentTimeMillis(),
        showWithinDays: Int = 7
    ): String? {
        if (deadlineEpochMs == null) return null
        val diffDays = (deadlineEpochMs - nowMillis) / DAY_MS
        if (diffDays < 0 || diffDays > showWithinDays) return null
        return "تا ${RTL}${diffDays.toEnglishDigits()} روز دیگر"
    }
}

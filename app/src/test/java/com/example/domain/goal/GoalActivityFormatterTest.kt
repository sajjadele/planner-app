package com.example.domain.goal

import com.example.core.util.RTL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoalActivityFormatterTest {

    private val NOW = 1_000_000_000_000L // arbitrary fixed "now"
    private val DAY = 86_400_000L

    @Test
    fun formatActiveDays_returnsEnglishDigits() {
        assertEquals("${RTL}12 روز فعالیت", GoalActivityFormatter.formatActiveDays(12))
    }

    @Test
    fun formatActiveDays_zero_isBlank() {
        assertEquals("", GoalActivityFormatter.formatActiveDays(0))
    }

    @Test
    fun formatLastActivity_today() {
        assertEquals("امروز", GoalActivityFormatter.formatLastActivity(NOW, NOW))
    }

    @Test
    fun formatLastActivity_yesterday() {
        assertEquals("دیروز", GoalActivityFormatter.formatLastActivity(NOW - DAY, NOW))
    }

    @Test
    fun formatLastActivity_threeDaysAgo() {
        assertEquals("${RTL}3 روز پیش", GoalActivityFormatter.formatLastActivity(NOW - 3 * DAY, NOW))
    }

    @Test
    fun formatLastActivity_null_isBlank() {
        assertEquals("", GoalActivityFormatter.formatLastActivity(null, NOW))
    }

    @Test
    fun formatDeadline_withinWindow_returnsEnglishDigits() {
        assertEquals("تا ${RTL}5 روز دیگر", GoalActivityFormatter.formatDeadline(NOW + 5 * DAY, NOW, showWithinDays = 7))
    }

    @Test
    fun formatDeadline_farInFuture_isNull() {
        assertNull(GoalActivityFormatter.formatDeadline(NOW + 30 * DAY, NOW, showWithinDays = 7))
    }

    @Test
    fun formatDeadline_past_isNull() {
        assertNull(GoalActivityFormatter.formatDeadline(NOW - DAY, NOW, showWithinDays = 7))
    }

    @Test
    fun formatDeadline_null_isNull() {
        assertNull(GoalActivityFormatter.formatDeadline(null, NOW))
    }
}

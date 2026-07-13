package com.example.domain.snapshot

import com.example.domain.insight.Velocity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class BehaviorCalculatorTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        val c = Calendar.getInstance()
        c.set(year, month, day, hour, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    @Test
    fun build_computesRateAndRescheduleRate() {
        val result = BehaviorCalculator.build(
            dateEpochMs = at(2026, Calendar.JULY, 13),
            completed = 5,
            created = 10,
            completedTimestamps = emptyList(),
            prevCompleted = 0,
            prevCreated = 0,
            rescheduleCount = 2
        )
        assertEquals(5, result.completed)
        assertEquals(10, result.created)
        assertEquals(0.2f, result.rescheduleRate)
        assertEquals(Velocity.IMPROVING, result.velocity) // 50% vs 0%
    }

    @Test
    fun build_zeroCreatedYieldsZeroRateAndZeroReschedule() {
        val result = BehaviorCalculator.build(
            dateEpochMs = at(2026, Calendar.JULY, 13),
            completed = 0,
            created = 0,
            completedTimestamps = emptyList(),
            prevCompleted = 0,
            prevCreated = 0,
            rescheduleCount = 5
        )
        assertEquals(0f, result.rescheduleRate)
        assertEquals(Velocity.STABLE, result.velocity) // 0 vs 0
    }

    @Test
    fun build_velocityImprovingWhenRateUpVsYesterday() {
        val result = BehaviorCalculator.build(
            dateEpochMs = at(2026, Calendar.JULY, 13),
            completed = 8,
            created = 10,
            completedTimestamps = emptyList(),
            prevCompleted = 5,
            prevCreated = 10,
            rescheduleCount = 0
        )
        assertEquals(Velocity.IMPROVING, result.velocity) // 80% vs 50%
    }

    @Test
    fun build_velocityDecliningWhenRateDownVsYesterday() {
        val result = BehaviorCalculator.build(
            dateEpochMs = at(2026, Calendar.JULY, 13),
            completed = 3,
            created = 10,
            completedTimestamps = emptyList(),
            prevCompleted = 8,
            prevCreated = 10,
            rescheduleCount = 0
        )
        assertEquals(Velocity.DECLINING, result.velocity) // 30% vs 80%
    }

    @Test
    fun build_streakAsOfDayIncludesToday() {
        val today = at(2026, Calendar.JULY, 13)
        val yesterday = at(2026, Calendar.JULY, 12)
        val result = BehaviorCalculator.build(
            dateEpochMs = today,
            completed = 1,
            created = 1,
            completedTimestamps = listOf(today, yesterday),
            prevCompleted = 0,
            prevCreated = 0,
            rescheduleCount = 0
        )
        // endOfDay(today) is "now"; streak counts today + yesterday = 2
        assertEquals(2, result.streak)
    }
}

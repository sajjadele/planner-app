package com.example.domain.insight

import com.example.plugins.planner.data.DayCompletion
import com.example.plugins.planner.data.GoalRateResult
import com.example.plugins.planner.data.TaskRescheduleWithTitle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class InsightCalculatorTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        val c = Calendar.getInstance()
        c.set(year, month, day, hour, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    @Test
    fun computeCompletionRate_returnsZeroWhenNothingCreated() {
        assertEquals(0f, InsightCalculator.computeCompletionRate(0, 0))
    }

    @Test
    fun computeCompletionRate_computesPercentage() {
        assertEquals(50f, InsightCalculator.computeCompletionRate(5, 10))
    }

    @Test
    fun computeVelocity_improvingWhenDiffAboveFive() {
        assertEquals(Velocity.IMPROVING, InsightCalculator.computeVelocity(80f, 70f))
    }

    @Test
    fun computeVelocity_decliningWhenDiffBelowMinusFive() {
        assertEquals(Velocity.DECLINING, InsightCalculator.computeVelocity(60f, 75f))
    }

    @Test
    fun computeVelocity_stableWithinFive() {
        assertEquals(Velocity.STABLE, InsightCalculator.computeVelocity(72f, 70f))
    }

    @Test
    fun findProcrastinationAlerts_filtersAndSortsAndFallsBackTitle() {
        val input = listOf(
            TaskRescheduleWithTitle(1, "A", 2),
            TaskRescheduleWithTitle(2, null, 5),
            TaskRescheduleWithTitle(3, "C", 4)
        )
        val result = InsightCalculator.findProcrastinationAlerts(input)
        assertEquals(2, result.size)
        // Highest count first; null title falls back to "تسک #id"
        assertEquals("تسک #2", result[0].taskTitle)
        assertEquals(5, result[0].rescheduleCount)
        assertEquals("C", result[1].taskTitle)
    }

    @Test
    fun findNeglectedGoal_firstBelowHundredWithTasks() {
        val input = listOf(
            GoalRateResult(1, "G1", 10, 10, 100f),
            GoalRateResult(2, "G2", 5, 2, 40f),
            GoalRateResult(3, "G3", 3, 0, 0f)
        )
        val neglected = InsightCalculator.findNeglectedGoal(input)
        assertEquals(2, neglected?.goalId)
    }

    @Test
    fun computeBestDay_emptyReturnsNull() {
        val (idx, count) = InsightCalculator.computeBestDay(emptyList())
        assertEquals(null, idx)
        assertEquals(0, count)
    }

    @Test
    fun computeBestDay_returnsFirst() {
        val (idx, count) = InsightCalculator.computeBestDay(listOf(DayCompletion(2, 7)))
        assertEquals(2, idx)
        assertEquals(7, count)
    }

    @Test
    fun computeStreak_emptyReturnsZero() {
        assertEquals(0, InsightCalculator.computeStreak(emptyList(), nowMillis = at(2026, Calendar.JULY, 13)))
    }

    @Test
    fun computeStreak_consecutiveDaysEndingYesterday() {
        val today = at(2026, Calendar.JULY, 13)
        val yesterday = at(2026, Calendar.JULY, 12)
        val twoDaysAgo = at(2026, Calendar.JULY, 11)
        val streak = InsightCalculator.computeStreak(
            listOf(yesterday, twoDaysAgo), nowMillis = today
        )
        assertEquals(2, streak)
    }

    @Test
    fun computeStreak_includesToday() {
        val today = at(2026, Calendar.JULY, 13)
        val yesterday = at(2026, Calendar.JULY, 12)
        val twoDaysAgo = at(2026, Calendar.JULY, 11)
        val streak = InsightCalculator.computeStreak(
            listOf(today, yesterday, twoDaysAgo), nowMillis = today
        )
        assertEquals(3, streak)
    }

    @Test
    fun computeStreak_brokenChainResets() {
        val today = at(2026, Calendar.JULY, 13)
        val twoDaysAgo = at(2026, Calendar.JULY, 11) // gap at yesterday
        val streak = InsightCalculator.computeStreak(
            listOf(today, twoDaysAgo), nowMillis = today
        )
        assertEquals(1, streak)
    }

    @Test
    fun getWeekRange_spansSevenDaysAndStartsOnSaturday() {
        val now = at(2026, Calendar.JULY, 13, 10) // a Monday
        val (start, end) = InsightCalculator.getWeekRange(0, nowMillis = now)
        assertEquals(7L * 24 * 60 * 60 * 1000, end - start)
        val startCal = Calendar.getInstance()
        startCal.timeInMillis = start
        assertEquals(Calendar.SATURDAY, startCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
    }
}

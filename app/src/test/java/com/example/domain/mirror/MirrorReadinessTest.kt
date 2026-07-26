package com.example.domain.mirror

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MirrorReadinessTest {

    private fun at(year: Int, month: Int, day: Int): Long {
        val c = Calendar.getInstance()
        c.set(year, month, day, 12, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun daysAgo(days: Int, nowMillis: Long = System.currentTimeMillis()): Long =
        nowMillis - (days.toLong() * 24 * 60 * 60 * 1000)

    @Test
    fun isEligible_false_forBrandNewGoal() {
        // Goal created seconds ago, even with a linked task.
        val createdAt = System.currentTimeMillis()
        assertFalse(
            "freshly created goal must not be eligible",
            MirrorReadiness.isEligible(createdAt, linkedTaskCount = 1, goalStatus = "active")
        )
    }

    @Test
    fun isEligible_false_whenYoungerThanSevenDays() {
        assertFalse(
            "goal younger than 7 days must not be eligible",
            MirrorReadiness.isEligible(daysAgo(3), linkedTaskCount = 5, goalStatus = "active")
        )
    }

    @Test
    fun isEligible_false_whenNoCreatedEvent() {
        // No creation timestamp available yet (null).
        assertFalse(
            "goal without a created event must not be eligible",
            MirrorReadiness.isEligible(null, linkedTaskCount = 5, goalStatus = "active")
        )
    }

    @Test
    fun isEligible_false_whenNoLinkedTasks() {
        // Old enough but no tasks yet → no behavioral history.
        assertFalse(
            "goal with zero linked tasks must not be eligible",
            MirrorReadiness.isEligible(daysAgo(14), linkedTaskCount = 0, goalStatus = "active")
        )
    }

    @Test
    fun isEligible_true_whenOldEnoughWithTasks() {
        assertTrue(
            "goal older than 7 days with linked tasks must be eligible",
            MirrorReadiness.isEligible(daysAgo(10), linkedTaskCount = 2, goalStatus = "active")
        )
    }

    @Test
    fun isEligible_true_exactlyAtThreshold() {
        assertTrue(
            "goal exactly 7 days old with tasks must be eligible",
            MirrorReadiness.isEligible(daysAgo(7), linkedTaskCount = 1, goalStatus = "active")
        )
    }

    @Test
    fun isEligible_false_whenGoalCompleted() {
        assertFalse(
            "completed goal must not be eligible",
            MirrorReadiness.isEligible(daysAgo(14), linkedTaskCount = 3, goalStatus = "completed")
        )
    }

    @Test
    fun isEligible_false_whenGoalAbandoned() {
        assertFalse(
            "abandoned goal must not be eligible",
            MirrorReadiness.isEligible(daysAgo(14), linkedTaskCount = 3, goalStatus = "abandoned")
        )
    }

    @Test
    fun isEligible_false_whenGoalPaused() {
        assertFalse(
            "paused goal must not be eligible",
            MirrorReadiness.isEligible(daysAgo(14), linkedTaskCount = 3, goalStatus = "paused")
        )
    }

    @Test
    fun isEligible_false_whenStatusEmpty() {
        assertFalse(
            "missing status must not be eligible",
            MirrorReadiness.isEligible(daysAgo(14), linkedTaskCount = 3, goalStatus = "")
        )
    }
}

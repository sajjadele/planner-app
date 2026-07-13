package com.example.domain.snapshot

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalProgressCalculatorTest {

    @Test
    fun build_computesRateFromCompletedAndTotal() {
        val result = GoalProgressCalculator.build(
            goalId = 7,
            dateEpochMs = 1_700_000_000_000L,
            completed = 4,
            total = 10
        )
        assertEquals(7, result.goalId)
        assertEquals(4, result.completed)
        assertEquals(10, result.total)
        assertEquals(40f, result.rate)
    }

    @Test
    fun build_zeroTotalYieldsZeroRate() {
        val result = GoalProgressCalculator.build(
            goalId = 1,
            dateEpochMs = 1_700_000_000_000L,
            completed = 0,
            total = 0
        )
        assertEquals(0f, result.rate)
    }

    @Test
    fun build_zeroCompletedYieldsZeroRate() {
        val result = GoalProgressCalculator.build(
            goalId = 2,
            dateEpochMs = 1_700_000_000_000L,
            completed = 0,
            total = 5
        )
        assertEquals(0f, result.rate)
    }
}

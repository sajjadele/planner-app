package com.example.domain.goal

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalProgressCalculatorTest {

    @Test
    fun completionOnly_zeroActivity_yieldsWeightedCompletion() {
        // 70% completion, 0 active days -> overall = 70 * 0.7 = 49
        val p = GoalProgressCalculator.compute(completionRate = 70f, activeDaysInWindow = 0)
        assertEquals(70f, p.completionRate)
        assertEquals(0f, p.activityMomentum)
        assertEquals(49f, p.overall, 0.001f)
    }

    @Test
    fun activityOnly_zeroCompletion_yieldsWeightedMomentum() {
        // 0% completion, 30 active days in 30-day window -> momentum 100 -> overall = 100 * 0.3 = 30
        val p = GoalProgressCalculator.compute(completionRate = 0f, activeDaysInWindow = 30, windowDays = 30)
        assertEquals(0f, p.completionRate)
        assertEquals(100f, p.activityMomentum)
        assertEquals(30f, p.overall, 0.001f)
    }

    @Test
    fun combined_example_matchesSpec() {
        // 70% completion + 30 active days/30 (momentum 100) -> 70*0.7 + 100*0.3 = 49 + 30 = 79
        val p = GoalProgressCalculator.compute(completionRate = 70f, activeDaysInWindow = 30, windowDays = 30)
        assertEquals(79f, p.overall, 0.001f)
    }

    @Test
    fun partialWindowMomentum_isFractional() {
        // 15 active days / 30 -> momentum 50 -> overall = 70*0.7 + 50*0.3 = 49 + 15 = 64
        val p = GoalProgressCalculator.compute(completionRate = 70f, activeDaysInWindow = 15, windowDays = 30)
        assertEquals(50f, p.activityMomentum, 0.001f)
        assertEquals(64f, p.overall, 0.001f)
    }

    @Test
    fun momentumClampsAt100() {
        val p = GoalProgressCalculator.compute(completionRate = 100f, activeDaysInWindow = 100, windowDays = 30)
        assertEquals(100f, p.activityMomentum)
        assertEquals(100f, p.overall, 0.001f)
    }

    @Test
    fun completionRateIsClampedTo100() {
        val p = GoalProgressCalculator.compute(completionRate = 140f, activeDaysInWindow = 0)
        assertEquals(100f, p.completionRate)
    }
}

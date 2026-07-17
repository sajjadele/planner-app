package com.example.domain.goal

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-domain tests for the Goal Detail progress composition:
 * 70% task completion + 30% 30-day window activity momentum.
 * Mirrors the contract used by GoalDetailViewModel.goalProgress.
 */
class GoalDetailProgressTest {

    @Test
    fun `all tasks done and full window activity yields 100 percent`() {
        val result = GoalProgressCalculator.compute(
            completionRate = 100f,
            activeDaysInWindow = 30,
            windowDays = 30
        )
        assertEquals(100f, result.overall, 0.001f)
    }

    @Test
    fun `no tasks and no activity yields zero`() {
        val result = GoalProgressCalculator.compute(
            completionRate = 0f,
            activeDaysInWindow = 0,
            windowDays = 30
        )
        assertEquals(0f, result.overall, 0.001f)
    }

    @Test
    fun `half tasks complete with full window activity yields 65 percent`() {
        // 50% tasks -> 35; 100% activity -> 30; total 65.
        val result = GoalProgressCalculator.compute(
            completionRate = 50f,
            activeDaysInWindow = 30,
            windowDays = 30
        )
        assertEquals(65f, result.overall, 0.001f)
    }

    @Test
    fun `no tasks but full window activity still shows momentum`() {
        // 0 tasks -> 0 task component; 100% activity -> 30 overall.
        val result = GoalProgressCalculator.compute(
            completionRate = 0f,
            activeDaysInWindow = 30,
            windowDays = 30
        )
        assertEquals(30f, result.overall, 0.001f)
    }

    @Test
    fun `partial window activity scales momentum component`() {
        // 0 tasks -> 0; 15/30 window -> 50% -> 15 overall.
        val result = GoalProgressCalculator.compute(
            completionRate = 0f,
            activeDaysInWindow = 15,
            windowDays = 30
        )
        assertEquals(15f, result.overall, 0.001f)
    }
}

package com.example.domain.goal

import kotlin.math.min

/**
 * Pure-Kotlin Goal progress model. Combines task completion with recent activity momentum so
 * progress reflects behavior, not just a checklist. No Android/Room imports — unit-testable on
 * the host JVM.
 *
 * Weighting (product decision): 70% task completion + 30% activity momentum. Momentum is measured
 * over a rolling window (default 30 days) of scheduled activity days, never lifetime — a goal with
 * a burst of activity a year ago should not look "active" today.
 *
 * Progress is meant to motivate, not judge: the score is always non-negative and clamps at 100.
 */
data class GoalProgress(
    /** Task completion rate, 0..100. */
    val completionRate: Float,
    /** Recent activity momentum, 0..100 (active days in window / window length). */
    val activityMomentum: Float,
    /** Weighted overall score, 0..100. */
    val overall: Float
)

object GoalProgressCalculator {

    /**
     * @param completionRate task completion percentage (0..100)
     * @param activeDaysInWindow distinct scheduled activity days within the rolling window
     * @param windowDays length of the rolling window in days (default 30)
     * @param completionWeight weight of completion (default 0.7)
     * @param activityWeight weight of momentum (default 0.3)
     */
    fun compute(
        completionRate: Float,
        activeDaysInWindow: Int,
        windowDays: Int = 30,
        completionWeight: Float = 0.7f,
        activityWeight: Float = 0.3f
    ): GoalProgress {
        val rate = completionRate.coerceIn(0f, 100f)
        val momentum = if (windowDays > 0) {
            min(100f, activeDaysInWindow.toFloat() / windowDays.toFloat() * 100f)
        } else 0f
        val overall = rate * completionWeight + momentum * activityWeight
        return GoalProgress(
            completionRate = rate,
            activityMomentum = momentum,
            overall = overall.coerceIn(0f, 100f)
        )
    }
}

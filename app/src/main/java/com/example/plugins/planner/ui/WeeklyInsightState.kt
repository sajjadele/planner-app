package com.example.plugins.planner.ui

import com.example.plugins.planner.data.DayCompletion
import com.example.plugins.planner.data.LifeAreaCompletion

data class WeeklyInsightState(
    // Core metrics
    val completedCount: Int = 0,
    val createdCount: Int = 0,
    val completionRate: Float = 0f,
    val lifeAreaBreakdown: List<LifeAreaCompletion>? = null,
    val unorganizedCount: Int = 0,
    val streakDays: Int = 0,
    val bestDayIndex: Int? = null,
    val bestDayCount: Int = 0,
    val hasData: Boolean = true,

    // Phase 3: Behavioral insights
    val procrastinationAlerts: List<ProcrastinationAlert> = emptyList(),
    val neglectedGoalTitle: String? = null,
    val neglectedGoalRate: Float = 0f,
    val weeklyVelocity: Velocity = Velocity.STABLE,
    val weeklyVelocityPercent: Float = 0f
)

/**
 * Procrastination alert: a task that has been rescheduled ≥ 3 times.
 * Signals avoidance behavior — the AI Coach will use this pattern.
 */
data class ProcrastinationAlert(
    val taskTitle: String,
    val rescheduleCount: Int
)

/**
 * Weekly velocity: is the user accelerating, maintaining, or declining?
 * Compared by completion rate (completed/created) of current vs previous week.
 */
enum class Velocity {
    IMPROVING,  // completing more than last week
    STABLE,     // roughly the same (±5%)
    DECLINING   // completing less than last week
}

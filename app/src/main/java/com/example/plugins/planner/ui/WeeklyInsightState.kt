package com.example.plugins.planner.ui

import com.example.domain.insight.ProcrastinationAlert
import com.example.domain.insight.Velocity
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

package com.example.plugins.planner

data class WeeklyInsightState(
    val completedCount: Int = 0,
    val createdCount: Int = 0,
    val completionRate: Float = 0f, // completed / created * 100
    val lifeAreaBreakdown: List<LifeAreaCompletion>? = null, // null = hide section
    val unorganizedCount: Int = 0,
    val streakDays: Int = 0,
    val bestDayIndex: Int? = null, // null = no completed tasks yet
    val bestDayCount: Int = 0,
    val hasData: Boolean = true // false = show "no data yet"
)
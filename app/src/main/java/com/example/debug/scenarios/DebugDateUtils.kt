package com.example.debug.scenarios

import java.util.Calendar

/**
 * Debug-only shared date utilities for ScenarioGenerator.
 *
 * Eliminates date helper duplication across QA tools.
 * All methods are pure functions with no side effects.
 */
object DebugDateUtils {

    private const val DAY_MS = 86_400_000L

    /**
     * Midnight epoch ms of N days ago.
     * Used for TaskEntity.dateEpochMs (scheduled day).
     */
    fun midnightDaysAgo(days: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Exact timestamp N days ago from now.
     * Used for TaskEventEntity.timestamp (event occurrence time).
     */
    fun timestampDaysAgo(days: Int): Long =
        System.currentTimeMillis() - days.toLong() * DAY_MS

    /**
     * Exact timestamp N days ago at a specific hour (local time).
     * Used for events that happen at a specific time of day.
     */
    fun timestampDaysAgoAtHour(days: Int, hour: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Midnight epoch ms for a specific day offset.
     * Alternative to midnightDaysAgo when you need explicit control.
     */
    fun midnightEpoch(daysAgo: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

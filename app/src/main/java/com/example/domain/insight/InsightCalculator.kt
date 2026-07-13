package com.example.domain.insight

import com.example.plugins.planner.data.DayCompletion
import com.example.plugins.planner.data.GoalRateResult
import com.example.plugins.planner.data.TaskRescheduleWithTitle
import java.util.Calendar

/**
 * Pure-Kotlin insight computations, extracted from [com.example.plugins.planner.ui.WeeklyInsightViewModel].
 *
 * No Android framework imports — every function is a plain JVM function and is unit-testable
 * on the host without Robolectric or an emulator. Time-dependent functions accept an explicit
 * `nowMillis` so tests are deterministic.
 */
object InsightCalculator {

    /**
     * Current streak: consecutive days (ending today or yesterday) with at least one
     * 'completed' event. Maps timestamps → (year, day-of-year) keys using device-local calendar.
     */
    fun computeStreak(
        completedTimestamps: List<Long>,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        if (completedTimestamps.isEmpty()) return 0

        val cal = Calendar.getInstance()
        val daySet = completedTimestamps.map { ts ->
            cal.timeInMillis = ts
            dayKey(cal)
        }.toSet()

        cal.timeInMillis = nowMillis
        if (!daySet.contains(dayKey(cal))) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        var streak = 0
        var key = dayKey(cal)
        while (daySet.contains(key)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            key = dayKey(cal)
        }
        return streak
    }

    /** Completion rate as a percentage (0 when nothing was created). */
    fun computeCompletionRate(completed: Int, created: Int): Float {
        return if (created > 0) (completed.toFloat() / created.toFloat()) * 100f else 0f
    }

    /**
     * Weekly velocity: compare current vs previous week completion rate.
     * ±5 percentage points is treated as STABLE.
     */
    fun computeVelocity(currentRate: Float, previousRate: Float): Velocity {
        val diff = currentRate - previousRate
        return when {
            diff > 5f -> Velocity.IMPROVING
            diff < -5f -> Velocity.DECLINING
            else -> Velocity.STABLE
        }
    }

    /**
     * Procrastination alerts: tasks rescheduled ≥ [threshold] times, sorted by count desc.
     * Titles are resolved by the SQL JOIN in the DAO, so no blocking lookup is needed here.
     */
    fun findProcrastinationAlerts(
        rescheduleCounts: List<TaskRescheduleWithTitle>,
        threshold: Int = 3
    ): List<ProcrastinationAlert> {
        return rescheduleCounts
            .filter { it.rescheduleCount >= threshold }
            .sortedByDescending { it.rescheduleCount }
            .map { alert ->
                ProcrastinationAlert(
                    taskTitle = alert.taskTitle ?: "تسک #${alert.taskId}",
                    rescheduleCount = alert.rescheduleCount
                )
            }
    }

    /**
     * Neglected goal: the goal with the lowest completion rate that still has ≥ 1 task.
     * Returns null when every goal is at 100% or has no tasks.
     */
    fun findNeglectedGoal(goalRates: List<GoalRateResult>): GoalRateResult? {
        return goalRates.firstOrNull { it.totalTasks > 0 && it.completionRate < 100f }
    }

    /**
     * Best day of the week: the (Persian) day index with the most completions.
     * Returns (dayIndex, count); dayIndex is null when there are no completions.
     */
    fun computeBestDay(dayCompletions: List<DayCompletion>): Pair<Int?, Int> {
        return if (dayCompletions.isNotEmpty()) {
            dayCompletions.first().dayIndex to dayCompletions.first().count
        } else {
            null to 0
        }
    }

    /**
     * Persian week range (Saturday → Friday).
     * @param offsetDays shift from the current week (e.g. -7 for the previous week).
     * @param nowMillis reference "now" (defaults to the real clock).
     * @return (startMillis, endMillis) where start = Saturday 00:00, end = next Saturday 00:00.
     */
    fun getWeekRange(
        offsetDays: Int = 0,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis

        if (offsetDays != 0) {
            cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        }

        // Calendar.DAY_OF_WEEK: Sun=1, Mon=2, ..., Sat=7
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = when (dayOfWeek) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }

        cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 7)
        val end = cal.timeInMillis

        return start to end
    }

    private fun dayKey(cal: Calendar): Int {
        return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }
}

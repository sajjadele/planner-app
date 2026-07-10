package com.example.plugins.planner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.plugins.planner.data.DayCompletion
import com.example.plugins.planner.data.GoalCompletion
import com.example.plugins.planner.data.GoalRateResult
import com.example.plugins.planner.data.InsightDao
import com.example.plugins.planner.data.LifeAreaCompletion
import com.example.plugins.planner.data.TaskDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

class WeeklyInsightViewModel(application: Application) : AndroidViewModel(application) {

    private val insightDao: InsightDao
    private val taskDao: TaskDao

    private val _insightState = MutableStateFlow(WeeklyInsightState(hasData = false))
    val insightState: StateFlow<WeeklyInsightState> = _insightState.asStateFlow()

    // Week ranges — computed once, fixed for the ViewModel lifetime
    private val currentWeek: Pair<Long, Long>
    private val previousWeek: Pair<Long, Long>

    init {
        val database = AppDatabase.getDatabase(application)
        insightDao = database.insightDao()
        taskDao = database.taskDao()
        currentWeek = getWeekRange(0)      // this week
        previousWeek = getWeekRange(-7)    // last week
        observeInsight()
    }

    /**
     * Reactive observation via Room Flow. When any row in task_events, tasks,
     * or goals changes, Room re-runs the queries and the combine block emits
     * a fresh WeeklyInsightState — no manual refresh needed.
     *
     * Phase 3: now also computes procrastination alerts, goal neglect,
     * and weekly velocity comparison.
     */
    private fun observeInsight() {
        val (curStart, curEnd) = currentWeek
        val (prevStart, prevEnd) = previousWeek

        viewModelScope.launch {
            combine(
                // Core metrics
                insightDao.observeCompletedCount(curStart, curEnd),
                insightDao.observeCreatedCount(curStart, curEnd),
                insightDao.observeCompletionByLifeArea(curStart, curEnd),
                insightDao.observeUnorganizedCount(curStart, curEnd),
                insightDao.observeCompletedTimestamps(),
                insightDao.observeCompletionByDay(curStart, curEnd),
                insightDao.observeCompletionByGoal(curStart, curEnd),
                // Phase 3: Behavioral
                insightDao.observeRescheduleCounts(),
                insightDao.observeGoalCompletionRates(),
                // Phase 3: Weekly velocity
                insightDao.observePreviousWeekCompletedCount(prevStart, prevEnd),
                insightDao.observePreviousWeekCreatedCount(prevStart, prevEnd)
            ) { results: Array<*> ->
                val completed = results[0] as Int
                val created = results[1] as Int
                val lifeArea = results[2] as List<LifeAreaCompletion>
                val unorganized = results[3] as Int
                val completedTs = results[4] as List<Long>
                val bestDay = results[5] as List<DayCompletion>
                @Suppress("UNCHECKED_CAST")
                val goalData = results[6] as List<GoalCompletion>
                @Suppress("UNCHECKED_CAST")
                val rescheduleCounts = results[7] as List<com.example.plugins.planner.data.TaskRescheduleCount>
                @Suppress("UNCHECKED_CAST")
                val goalRates = results[8] as List<GoalRateResult>
                val prevCompleted = results[9] as Int
                val prevCreated = results[10] as Int

                // --- Streak ---
                val streakDays = computeStreak(completedTs)

                // --- Best day ---
                val bestDayIndex = if (bestDay.isNotEmpty()) bestDay.first().dayIndex else null
                val bestDayCount = if (bestDay.isNotEmpty()) bestDay.first().count else 0

                // --- Completion rate ---
                val rate = if (created > 0) (completed.toFloat() / created.toFloat()) * 100f else 0f

                // --- Procrastination alerts (rescheduled ≥ 3 times) ---
                val alerts = rescheduleCounts
                    .filter { it.rescheduleCount >= 3 }
                    .sortedByDescending { it.rescheduleCount }

                // Resolve task titles for procrastination alerts
                val alertsWithTitles = alerts.map { alert ->
                    val task = taskDao.getTaskById(alert.taskId)
                    ProcrastinationAlert(
                        taskTitle = task?.title ?: "تسک #${alert.taskId}",
                        rescheduleCount = alert.rescheduleCount
                    )
                }

                // --- Neglected goal (lowest completion rate, with at least 1 task) ---
                val neglectedGoal = goalRates
                    .firstOrNull { it.totalTasks > 0 && it.completionRate < 100f }

                // --- Weekly velocity ---
                val prevRate = if (prevCreated > 0) (prevCompleted.toFloat() / prevCreated.toFloat()) * 100f else 0f
                val velocityDiff = rate - prevRate
                val velocity = when {
                    velocityDiff > 5f -> Velocity.IMPROVING
                    velocityDiff < -5f -> Velocity.DECLINING
                    else -> Velocity.STABLE
                }

                // --- Build state ---
                if (created == 0 && completed == 0 && prevCompleted == 0) {
                    WeeklyInsightState(hasData = false, streakDays = streakDays)
                } else {
                    WeeklyInsightState(
                        completedCount = completed,
                        createdCount = created,
                        completionRate = rate,
                        lifeAreaBreakdown = if (lifeArea.isEmpty()) null else lifeArea,
                        unorganizedCount = unorganized,
                        streakDays = streakDays,
                        bestDayIndex = bestDayIndex,
                        bestDayCount = bestDayCount,
                        hasData = true,
                        // Phase 3
                        procrastinationAlerts = alertsWithTitles,
                        neglectedGoalTitle = neglectedGoal?.goalTitle,
                        neglectedGoalRate = neglectedGoal?.completionRate ?: 0f,
                        weeklyVelocity = velocity,
                        weeklyVelocityPercent = velocityDiff
                    )
                }
            }.collect { _insightState.value = it }
        }
    }

    /**
     * Compute current streak: count consecutive days ending today (or yesterday
     * if nothing completed today yet) that have at least one 'completed' event.
     * Uses device-local calendar to map timestamps → day-of-year.
     */
    private fun computeStreak(completedTimestamps: List<Long>): Int {
        if (completedTimestamps.isEmpty()) return 0

        val cal = Calendar.getInstance()
        val daySet = completedTimestamps.map { ts ->
            cal.timeInMillis = ts
            val y = cal.get(Calendar.YEAR)
            val d = cal.get(Calendar.DAY_OF_YEAR)
            y * 1000 + d // unique day key
        }.toSet()

        // Start from today; if nothing completed today, start from yesterday
        cal.timeInMillis = System.currentTimeMillis()
        val todayKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        if (!daySet.contains(todayKey)) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        var streak = 0
        var key = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        while (daySet.contains(key)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            key = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        }
        return streak
    }

    /**
     * Calculate Persian week range (Saturday to Friday).
     * @param offsetDays shift from current week (e.g. -7 for previous week)
     * Returns (startMillis, endMillis) where start = Saturday 00:00, end = next Saturday 00:00.
     */
    private fun getWeekRange(offsetDays: Int = 0): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = System.currentTimeMillis()

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
}

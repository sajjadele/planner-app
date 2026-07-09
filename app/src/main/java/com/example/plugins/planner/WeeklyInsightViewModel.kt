package com.example.plugins.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class WeeklyInsightViewModel(application: Application) : AndroidViewModel(application) {

    private val taskEventDao: TaskEventDao

    private val _insightState = MutableStateFlow(WeeklyInsightState(hasData = false))
    val insightState: StateFlow<WeeklyInsightState> = _insightState.asStateFlow()

    // Week range is fixed for the lifetime of the ViewModel (same week)
    private val weekRange: Pair<Long, Long>

    init {
        val database = AppDatabase.getDatabase(application)
        taskEventDao = database.taskEventDao()
        weekRange = getWeekRange()
        observeInsight()
    }

    /**
     * Reactive observation via Room Flow. When any row in task_events or tasks
     * changes (insert/complete/delete), Room re-runs the queries and this
     * combine block emits a fresh WeeklyInsightState — no manual refresh needed.
     */
    private fun observeInsight() {
        val (start, end) = weekRange
        viewModelScope.launch {
            combine(
                taskEventDao.observeCompletedCount(start, end),
                taskEventDao.observeCreatedCount(start, end),
                taskEventDao.observeCompletionByLifeArea(start, end),
                taskEventDao.observeUnorganizedCount(start, end),
                taskEventDao.observeCompletedTimestamps(),
                taskEventDao.observeCompletionByDay(start, end)
            ) { results: Array<*> ->
                val completed = results[0] as Int
                val created = results[1] as Int
                val lifeArea = results[2] as List<LifeAreaCompletion>
                val unorganized = results[3] as Int
                val completedTs = results[4] as List<Long>
                val bestDay = results[5] as List<DayCompletion>

                val streakDays = computeStreak(completedTs)
                val bestDayIndex = if (bestDay.isNotEmpty()) bestDay.first().dayIndex else null
                val bestDayCount = if (bestDay.isNotEmpty()) bestDay.first().count else 0
                if (created == 0 && completed == 0) {
                    WeeklyInsightState(hasData = false, streakDays = streakDays)
                } else {
                    val rate = if (created > 0) {
                        (completed.toFloat() / created.toFloat()) * 100f
                    } else {
                        0f
                    }
                    val lifeAreaData = if (lifeArea.isEmpty()) null else lifeArea
                    WeeklyInsightState(
                        completedCount = completed,
                        createdCount = created,
                        completionRate = rate,
                        lifeAreaBreakdown = lifeAreaData,
                        unorganizedCount = unorganized,
                        streakDays = streakDays,
                        bestDayIndex = bestDayIndex,
                        bestDayCount = bestDayCount,
                        hasData = true
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
     * Calculate current Persian week range (Saturday to Friday).
     * Returns (startMillis, endMillis) where start = Saturday 00:00, end = next Saturday 00:00.
     */
    private fun getWeekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = System.currentTimeMillis()

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

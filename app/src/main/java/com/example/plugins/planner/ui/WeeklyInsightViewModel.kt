package com.example.plugins.planner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.domain.insight.InsightCalculator
import com.example.domain.insight.ProcrastinationAlert
import com.example.domain.insight.Velocity
import com.example.plugins.planner.data.DayCompletion
import com.example.plugins.planner.data.GoalRateResult
import com.example.plugins.planner.data.InsightRepository
import com.example.plugins.planner.data.LifeAreaCompletion
import com.example.plugins.planner.data.RoomInsightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class WeeklyInsightViewModel(application: Application) : AndroidViewModel(application) {

    private val insightRepository: InsightRepository

    private val _insightState = MutableStateFlow(WeeklyInsightState(hasData = false))
    val insightState: StateFlow<WeeklyInsightState> = _insightState.asStateFlow()

    // Week ranges — computed once, fixed for the ViewModel lifetime
    private val currentWeek: Pair<Long, Long>
    private val previousWeek: Pair<Long, Long>

    init {
        val database = AppDatabase.getDatabase(application)
        insightRepository = RoomInsightRepository(database.insightDao())
        currentWeek = InsightCalculator.getWeekRange(0)      // this week
        previousWeek = InsightCalculator.getWeekRange(-7)    // last week
        observeInsight()
    }

    /**
     * Reactive observation via Room Flow. When any row in task_events, tasks,
     * or goals changes, Room re-runs the queries and the combine block emits
     * a fresh WeeklyInsightState — no manual refresh needed.
     *
     * All insight queries now filter by dateEpochMs (scheduled day) rather than
     * timestamp (creation moment), so future-planned tasks don't distort the
     * current week's metrics. All derived metrics are computed by the pure-Kotlin
     * [InsightCalculator] (no Android-coupled logic in this ViewModel).
     */
    private fun observeInsight() {
        val (curStart, curEnd) = currentWeek
        val (prevStart, prevEnd) = previousWeek

        viewModelScope.launch {
            combine(
                insightRepository.observeCompletedCount(curStart, curEnd),
                insightRepository.observeCreatedCount(curStart, curEnd),
                insightRepository.observeCompletionByLifeArea(curStart, curEnd),
                insightRepository.observeUnorganizedCount(curStart, curEnd),
                insightRepository.observeCompletedTimestamps(),
                insightRepository.observeCompletionByDay(curStart, curEnd),
                insightRepository.observeRescheduleCounts(),
                insightRepository.observeGoalCompletionRates(),
                insightRepository.observePreviousWeekCompletedCount(prevStart, prevEnd),
                insightRepository.observePreviousWeekCreatedCount(prevStart, prevEnd)
            ) { results: Array<*> ->
                val completed = results[0] as Int
                val created = results[1] as Int
                val lifeArea = results[2] as List<LifeAreaCompletion>
                val unorganized = results[3] as Int
                val completedTs = results[4] as List<Long>
                val bestDay = results[5] as List<DayCompletion>
                @Suppress("UNCHECKED_CAST")
                val rescheduleCounts = results[6] as List<com.example.plugins.planner.data.TaskRescheduleWithTitle>
                @Suppress("UNCHECKED_CAST")
                val goalRates = results[7] as List<GoalRateResult>
                val prevCompleted = results[8] as Int
                val prevCreated = results[9] as Int

                // --- Streak ---
                val streakDays = InsightCalculator.computeStreak(completedTs)

                // --- Best day ---
                val (bestDayIndex, bestDayCount) = InsightCalculator.computeBestDay(bestDay)

                // --- Completion rate ---
                val rate = InsightCalculator.computeCompletionRate(completed, created)

                // --- Procrastination alerts (rescheduled >= 3 times) ---
                // Titles are already resolved by the SQL JOIN in observeRescheduleCounts,
                // so no blocking lookup is required.
                val alertsWithTitles: List<ProcrastinationAlert> =
                    InsightCalculator.findProcrastinationAlerts(rescheduleCounts)

                // --- Neglected goal (lowest completion rate, with at least 1 task) ---
                val neglectedGoal: GoalRateResult? = InsightCalculator.findNeglectedGoal(goalRates)

                // --- Weekly velocity ---
                val prevRate = InsightCalculator.computeCompletionRate(prevCompleted, prevCreated)
                val velocityDiff = rate - prevRate
                val velocity: Velocity = InsightCalculator.computeVelocity(rate, prevRate)

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
}

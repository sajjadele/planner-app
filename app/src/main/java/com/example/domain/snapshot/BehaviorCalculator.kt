package com.example.domain.snapshot

import com.example.domain.insight.InsightCalculator
import com.example.domain.insight.Velocity
import java.util.Calendar

/**
 * Pure-Kotlin result of a daily behavioral rollup.
 *
 * No Android/Room imports — unit-testable on the host JVM. `velocity` is the
 * [Velocity] enum; [com.example.core.snapshot.SnapshotAggregator] persists it as its name.
 */
data class BehaviorSnapshotData(
    val dateEpochMs: Long,
    val completed: Int,
    val created: Int,
    val streak: Int,
    val velocity: Velocity,
    val rescheduleRate: Float
)

/**
 * Computes the daily behavioral snapshot from raw signal.
 *
 * - `streak`: the completion streak *as of the end of the day* (so historical days carry their
 *   own streak value, making the snapshot a true time-series).
 * - `velocity`: day-over-day completion-rate change vs the previous day, via
 *   [InsightCalculator.computeVelocity].
 * - `rescheduleRate`: reschedules that day ÷ tasks created that day (0 when nothing created).
 *
 * Pure function — the aggregator supplies the day-scoped inputs and decides when to persist.
 */
object BehaviorCalculator {

    fun build(
        dateEpochMs: Long,
        completed: Int,
        created: Int,
        completedTimestamps: List<Long>,
        prevCompleted: Int,
        prevCreated: Int,
        rescheduleCount: Int
    ): BehaviorSnapshotData {
        val rate = InsightCalculator.computeCompletionRate(completed, created)
        val prevRate = InsightCalculator.computeCompletionRate(prevCompleted, prevCreated)
        val velocity = InsightCalculator.computeVelocity(rate, prevRate)
        val rescheduleRate = if (created > 0) rescheduleCount.toFloat() / created.toFloat() else 0f

        val endOfDay = endOfDayMillis(dateEpochMs)
        val streak = InsightCalculator.computeStreak(completedTimestamps, nowMillis = endOfDay)

        return BehaviorSnapshotData(
            dateEpochMs = dateEpochMs,
            completed = completed,
            created = created,
            streak = streak,
            velocity = velocity,
            rescheduleRate = rescheduleRate
        )
    }

    private fun endOfDayMillis(dateEpochMs: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dateEpochMs
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}

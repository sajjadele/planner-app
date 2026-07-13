package com.example.domain.snapshot

import com.example.domain.insight.InsightCalculator

/**
 * Pure-Kotlin result of a per-goal daily progress computation.
 *
 * No Android/Room imports — unit-testable on the host JVM. Mapped to
 * [com.example.core.snapshot.GoalProgressSnapshotEntity] by [com.example.core.snapshot.SnapshotAggregator].
 */
data class GoalProgressSnapshotData(
    val dateEpochMs: Long,
    val goalId: Int,
    val completed: Int,
    val total: Int,
    val rate: Float
)

/**
 * Computes a single goal's daily progress projection.
 *
 * `rate` reuses [InsightCalculator.computeCompletionRate] so the projection stays consistent
 * with the on-demand insight math. Pure function — given the same inputs it always returns the
 * same snapshot; the aggregator decides when to persist it.
 */
object GoalProgressCalculator {
    fun build(goalId: Int, dateEpochMs: Long, completed: Int, total: Int): GoalProgressSnapshotData {
        return GoalProgressSnapshotData(
            dateEpochMs = dateEpochMs,
            goalId = goalId,
            completed = completed,
            total = total,
            rate = InsightCalculator.computeCompletionRate(completed, total)
        )
    }
}

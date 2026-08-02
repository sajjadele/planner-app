package com.example.core.snapshot

import com.example.core.goal.GoalRepository
import com.example.domain.insight.InsightCalculator
import com.example.domain.snapshot.BehaviorCalculator
import com.example.domain.snapshot.BehaviorSnapshotData
import com.example.domain.snapshot.GoalProgressCalculator
import com.example.domain.snapshot.GoalProgressSnapshotData
import com.example.plugins.planner.data.InsightRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Phase 3 aggregation engine: turns raw `tasks`/`task_events` signal into the rebuildable
 * `behavior_snapshot` + `goal_progress_snapshot` projections.
 *
 * Design rules (see `docs/ADR-0003`):
 * - Snapshots are *projections* — always recomputable from events; never authoritative.
 * - Trigger strategy: real-time upsert of *today* on key events (`recordDay(today)`) plus an
 *   App-Launch Backfill Engine (`backfillIfNeeded`) — no `WorkManager`, no scheduler.
 * - Depends only on repository interfaces, so it is swappable/mockable and free of Android deps.
 */
class SnapshotAggregator(
    private val insightRepository: InsightRepository,
    private val snapshotRepository: SnapshotRepository,
    private val goalRepository: GoalRepository
) {

    /**
     * Recompute and persist both snapshots for a single day. Idempotent (REPLACE upsert) so it
     * is safe to call on every relevant event and during backfill.
     */
    suspend fun recordDay(dateEpochMs: Long) {
        val start = dateEpochMs
        val end = endOfDay(dateEpochMs)

        val completed = insightRepository.observeCompletedCount(start, end).first()
        val created = insightRepository.observeCreatedCount(start, end).first()
        // Cap streak input at ~400 days — any realistic streak is well within this window.
        val completedTimestamps = insightRepository.observeCompletedTimestamps(
            System.currentTimeMillis() - STREAK_LOOKBACK_DAYS * DAY_MS
        ).first()

        val prevStart = start - DAY_MS
        val prevEnd = end - DAY_MS
        val prevCompleted = insightRepository.observeCompletedCount(prevStart, prevEnd).first()
        val prevCreated = insightRepository.observeCreatedCount(prevStart, prevEnd).first()
        val rescheduleCount = insightRepository.observeRescheduleCountBetween(start, end).first()

        val behavior = BehaviorCalculator.build(
            dateEpochMs = dateEpochMs,
            completed = completed,
            created = created,
            completedTimestamps = completedTimestamps,
            prevCompleted = prevCompleted,
            prevCreated = prevCreated,
            rescheduleCount = rescheduleCount
        )
        snapshotRepository.recordBehavior(behavior.toEntity())

        val activeGoalIds = goalRepository.getActiveGoals().first().map { it.id }
        for (goalId in activeGoalIds) {
            val counts = insightRepository.getGoalDayCounts(goalId, start, end)
            val progress = GoalProgressCalculator.build(
                goalId = goalId,
                dateEpochMs = dateEpochMs,
                completed = counts.completed,
                total = counts.total
            )
            snapshotRepository.recordGoalProgress(progress.toEntity())
        }
    }

    /**
     * Fill any gaps in the projection from the earliest task day through today. Used by the
     * App-Launch Backfill Engine so historical days are present for trend/velocity queries.
     */
    suspend fun backfillMissing() {
        val covered = snapshotRepository.getCoveredDates().toSet()
        val earliest = insightRepository.getEarliestTaskDateEpochMs() ?: return
        val today = startOfToday()
        var day = earliest
        while (day <= today) {
            if (day !in covered) recordDay(day)
            day += DAY_MS
        }
    }

    /**
     * Backfill only when the projection is stale (no snapshot for today yet). Cheap on every
     * launch; the heavy historical fill runs once and then stays current via real-time upserts.
     */
    suspend fun backfillIfNeeded() {
        val latest = snapshotRepository.getLatestBehavior()
        if (latest == null || latest.dateEpochMs < startOfToday()) {
            backfillMissing()
        }
    }

    /**
     * Delete all behavior and goal-progress snapshots.
     * Used by Developer Lab cleanup to ensure stale debug data does not
     * interfere with subsequent scenario generation.
     */
    suspend fun clearAllSnapshots() {
        snapshotRepository.deleteAllBehaviors()
        snapshotRepository.deleteAllGoalProgress()
    }

    private fun BehaviorSnapshotData.toEntity() = BehaviorSnapshotEntity(
        dateEpochMs = dateEpochMs,
        completed = completed,
        created = created,
        streak = streak,
        velocity = velocity.name,
        rescheduleRate = rescheduleRate
    )

    private fun GoalProgressSnapshotData.toEntity() = GoalProgressSnapshotEntity(
        dateEpochMs = dateEpochMs,
        goalId = goalId,
        completed = completed,
        total = total,
        rate = rate
    )

    private fun endOfDay(dateEpochMs: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dateEpochMs
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        private const val DAY_MS = 86400000L
        /** Cap streak calculation input at ~400 days. Any realistic streak is within this window. */
        private const val STREAK_LOOKBACK_DAYS = 400L
    }
}

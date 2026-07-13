package com.example.core.snapshot

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Daily behavioral rollup for the whole app (task-focused, no goal-lifecycle coupling).
 *
 * A single row per day: completion counts, current streak (as of that day), day-over-day
 * velocity, and a reschedule rate. Rebuildable from `tasks` + `task_events`; the raw events
 * remain the source of truth. `behavior_snapshot` intentionally omits `lifeAreaId` (task-focused
 * per Phase 3 locked decisions) — per-life-area detail stays re-derivable via
 * `InsightDao.observeCompletionByLifeArea`. See `docs/ADR-0003`.
 */
@Entity(tableName = "behavior_snapshot")
data class BehaviorSnapshotEntity(
    @PrimaryKey val dateEpochMs: Long,
    val completed: Int,
    val created: Int,
    val streak: Int,
    val velocity: String,   // Velocity enum name: IMPROVING | STABLE | DECLINING
    val rescheduleRate: Float
)

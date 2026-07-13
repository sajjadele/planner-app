package com.example.core.snapshot

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Daily progress projection for a single goal.
 *
 * Rebuildable from `tasks` (the authoritative source of truth) — never treated as an
 * authoritative write. One row per (day, goal). Consumed by the future Graph View (per-goal
 * progress trend) and the AI Coach (Phase 3/5 read model). See `docs/ADR-0003`.
 */
@Entity(
    tableName = "goal_progress_snapshot",
    primaryKeys = ["dateEpochMs", "goalId"],
    foreignKeys = [
        ForeignKey(
            entity = com.example.core.goal.GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId")]
)
data class GoalProgressSnapshotEntity(
    val dateEpochMs: Long,
    val goalId: Int,
    val completed: Int,
    val total: Int,
    val rate: Float
)

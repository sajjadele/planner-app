package com.example.core.goal

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Append-only signal log for goal lifecycle transitions.
 *
 * Distinct from `task_events` (which tracks Task actions). Goal commitment
 * behavior (how often goals are paused/abandoned/resumed) is required later for
 * behavioral analysis (Phase 3) and the AI Coach (Phase 5). The status string on
 * [com.example.core.goal.GoalEntity] is the current state; this table is the audit trail.
 */
@Entity(
    tableName = "goal_events",
    indices = [Index("goalId")]
)
data class GoalEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Int,
    val eventType: String, // "created" | "completed" | "paused" | "resumed" | "abandoned"
    val timestamp: Long = System.currentTimeMillis()
)

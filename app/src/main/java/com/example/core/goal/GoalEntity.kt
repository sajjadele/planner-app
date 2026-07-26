package com.example.core.goal

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Root entity of the Vision Planner mental model.
 *
 * Goal represents a long-term objective. All Tasks optionally
 * cascade up to a Goal. The system tracks goal status over time
 * to enable future Insight generation and AI coaching.
 *
 * Mental model:
 *   Goal → Task → Event → Insight
 */
@Entity(
    tableName = "goals",
    indices = [Index("status")]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String? = null,
    val status: String = GoalStatus.ACTIVE, // "active" | "paused" | "completed" | "abandoned" | "archived"
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    // Phase 5.1: optional, low-friction context. Both nullable; creating a goal stays fast.
    val why: String? = null,
    val deadlineEpochMs: Long? = null
)

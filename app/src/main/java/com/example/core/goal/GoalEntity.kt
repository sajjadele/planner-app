package com.example.core.goal

import androidx.room.Entity
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
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String? = null,
    val status: String = "active", // "active" | "completed" | "paused" | "abandoned"
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

package com.example.plugins.planner.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.goal.GoalEntity

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("goalId")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    // Optional metadata — tasks must remain ultra-fast to create.
    val priority: String? = null, // "HIGH", "MEDIUM", "LOW"
    val isCompleted: Boolean = false,
    /** Midnight epoch ms of the scheduled day (local timezone). Replaces dayIndex. */
    val dateEpochMs: Long,
    /** Creation timestamp for ordering / insights fallback. */
    val timestamp: Long = System.currentTimeMillis(),
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    // Goal linkage — nullable foreign key to GoalEntity.
    val goalId: Int? = null,
    val lifeAreaId: Int? = null,
    // goalName removed: goal linkage is resolved exclusively via goalId (FK)
    val valueTag: String? = null
)

package com.example.plugins.planner

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    // Optional metadata — tasks must remain ultra-fast to create.
    val priority: String? = null, // "HIGH", "MEDIUM", "LOW"
    val isCompleted: Boolean = false,
    val dayIndex: Int, // 0 to 6 representing Saturday (0) to Friday (6)
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    // Lightweight semantic hooks for future optional organization.
    val goalId: Int? = null,
    val lifeAreaId: Int? = null,
    val goalName: String? = null, // free-text goal name (MVP: no Goal entity yet)
    val valueTag: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

package com.example.plugins.planner

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val priority: String, // "HIGH", "MEDIUM", "LOW"
    val isCompleted: Boolean = false,
    val dayIndex: Int, // 0 to 6 representing Monday (0) to Sunday (6)
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

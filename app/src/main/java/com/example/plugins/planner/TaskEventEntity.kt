package com.example.plugins.planner

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_events",
    indices = [Index("taskId")]
)
data class TaskEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Int,
    val eventType: String, // "created" | "completed" | "reopened" | "deleted"
    val timestamp: Long = System.currentTimeMillis()
)

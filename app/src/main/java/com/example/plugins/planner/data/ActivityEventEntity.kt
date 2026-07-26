package com.example.plugins.planner.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_events",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId"), Index("timestamp")]
)
data class ActivityEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val taskId: Int,

    val stepId: Int? = null,

    val eventType: String,

    val description: String? = null,

    val timestamp: Long = System.currentTimeMillis()
)

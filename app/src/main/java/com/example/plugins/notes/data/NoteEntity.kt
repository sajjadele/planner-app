package com.example.plugins.notes.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [Index("goalId"), Index("taskId")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    // Optional linkage — notes can optionally provide context to goals or tasks.
    val goalId: Int? = null,
    val taskId: Int? = null
)

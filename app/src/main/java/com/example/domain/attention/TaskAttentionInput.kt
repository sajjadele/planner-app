package com.example.domain.attention

/**
 * Minimal task projection for the Attention calculator.
 *
 * Keeps [domain.attention] free of Android/Room-typed entities.
 * A task that is completed is excluded upstream — this model is only
 * constructed for active (incomplete) tasks.
 */
data class TaskAttentionInput(
    val id: Int,
    val title: String,
    val dateEpochMs: Long?,
    val deadlineEpochMs: Long?,
    val lastMeaningfulInteractionMs: Long?,
    val rescheduleCount: Int
)

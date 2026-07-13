package com.example.domain.insight

/**
 * Procrastination alert: a task that has been rescheduled ≥ 3 times.
 * Signals avoidance behavior — the AI Coach will use this pattern.
 */
data class ProcrastinationAlert(
    val taskTitle: String,
    val rescheduleCount: Int
)

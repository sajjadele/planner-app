package com.example.domain.attention

/**
 * Pure-Kotlin attention provider for the Behavioral Solar System.
 *
 * Thin, testable boundary over [AttentionCalculator]: given active tasks and
 * supporting signals, returns taskId → [AttentionResult]. No Android deps.
 *
 * Callers recompute whenever task/signal inputs change (reactive provider).
 */
object AttentionProvider {

    /**
     * Compute attention for active (incomplete) tasks only.
     * Completed tasks must be filtered out by the caller.
     */
    fun compute(
        tasks: List<TaskAttentionInput>,
        nowMillis: Long = System.currentTimeMillis()
    ): Map<Int, AttentionResult> = AttentionCalculator.compute(tasks, nowMillis)
}

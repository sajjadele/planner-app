package com.example.core.goal

/**
 * Canonical goal status values. Status remains a plain [String] on [GoalEntity] for Room
 * compatibility (no enum migration); this object centralizes the allowed values and the
 * minimal valid transition rules so call sites stop hard-coding raw strings.
 *
 * Allowed transitions (no complex state machine):
 * - active    -> paused | completed | abandoned
 * - paused    -> active | completed | abandoned
 * - completed -> archived
 * - abandoned -> archived
 * - archived  -> (terminal)
 */
object GoalStatus {
    const val ACTIVE = "active"
    const val PAUSED = "paused"
    const val COMPLETED = "completed"
    const val ABANDONED = "abandoned"
    const val ARCHIVED = "archived"

    /** All currently valid status string values. */
    val ALL = listOf(ACTIVE, PAUSED, COMPLETED, ABANDONED, ARCHIVED)

    /**
     * Returns true when moving from [from] to [to] is permitted.
     * Self-transitions are ignored by callers (no-op), so they are treated as valid here.
     */
    fun canTransition(from: String, to: String): Boolean {
        if (from == to) return true
        return when (from) {
            ACTIVE -> to in listOf(PAUSED, COMPLETED, ABANDONED)
            PAUSED -> to in listOf(ACTIVE, COMPLETED, ABANDONED)
            COMPLETED -> to == ARCHIVED
            ABANDONED -> to == ARCHIVED
            ARCHIVED -> false
            else -> false
        }
    }

    /**
     * Throws [IllegalArgumentException] if [from] -> [to] is not a permitted transition.
     * Used by the repository before persisting a status change so invalid moves fail fast
     * rather than silently corrupting goal lifecycle state.
     */
    fun requireValidTransition(from: String, to: String) {
        if (!canTransition(from, to)) {
            throw IllegalArgumentException("Illegal goal status transition: $from -> $to")
        }
    }
}

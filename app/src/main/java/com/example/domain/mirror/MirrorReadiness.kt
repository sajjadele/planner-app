package com.example.domain.mirror

import java.util.Calendar

/**
 * Eligibility gate for Mirror analysis.
 *
 * Mirror must not produce feedback for goals that lack enough behavioral history.
 * A freshly created goal has no meaningful pattern yet, and showing feedback at
 * creation time violates the product rule "Mirror should become meaningful after
 * enough behavioral history (~7 days)".
 *
 * This gate is intentionally conservative: it sits ABOVE the individual heuristics
 * (which are unchanged) and short-circuits [RoomMirrorRepository.evaluate] to an
 * empty result when the goal is not eligible.
 *
 * Uses only existing data:
 * - [goalCreatedAtMs]: timestamp of the goal's `created` event (from goal_events)
 * - [linkedTaskCount]: number of tasks linked to the goal (from existing rate query)
 */
object MirrorReadiness {

    /** Minimum goal age (days) before any Mirror signal is allowed. */
    const val MIN_GOAL_AGE_DAYS = 7

    /** Minimum number of linked tasks before Mirror is considered meaningful. */
    const val MIN_LINKED_TASKS = 1

    /** Only ACTIVE goals receive Mirror feedback. */
    const val ACTIVE_STATUS = "active"

    fun isEligible(
        goalCreatedAtMs: Long?,
        linkedTaskCount: Int,
        goalStatus: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (goalStatus != ACTIVE_STATUS) return false
        val createdAt = goalCreatedAtMs ?: return false
        if (ageDays(createdAt, nowMillis) < MIN_GOAL_AGE_DAYS) return false
        if (linkedTaskCount < MIN_LINKED_TASKS) return false
        return true
    }

    private fun ageDays(fromMillis: Long, nowMillis: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = fromMillis
        val fromDay = cal.get(Calendar.DAY_OF_YEAR)
        val fromYear = cal.get(Calendar.YEAR)
        cal.timeInMillis = nowMillis
        val toDay = cal.get(Calendar.DAY_OF_YEAR)
        val toYear = cal.get(Calendar.YEAR)
        val raw = (toYear - fromYear) * 365 + (toDay - fromDay)
        return if (raw < 0) 0 else raw
    }
}

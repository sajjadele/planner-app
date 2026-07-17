package com.example.domain.goal

import com.example.core.goal.GoalEntity
import java.util.Comparator

/**
 * Pure-Kotlin ordering for the Goal Dashboard. No Android imports — host-JVM testable.
 *
 * Priority (product decision for Phase 5.2):
 *   1. Approaching deadline first (earlier future deadline wins; no deadline / past deadline last).
 *   2. Recent activity next (more recent last-activity wins; never active last).
 *   3. Engagement last (more active days / tasks wins).
 *
 * @param lastActivity map of goalId -> latest activity epoch ms (null = never).
 * @param activeDays map of goalId -> lifetime active day count (engagement signal).
 */
object GoalSort {

    fun sort(
        goals: List<GoalEntity>,
        lastActivity: Map<Int, Long?>,
        activeDays: Map<Int, Int>
    ): List<GoalEntity> {
        val now = System.currentTimeMillis()
        val deadlineComparator = Comparator<GoalEntity> { a, b ->
            rankDeadline(a.deadlineEpochMs, now).compareTo(rankDeadline(b.deadlineEpochMs, now))
        }
        val activityComparator = Comparator<GoalEntity> { a, b ->
            (lastActivity[b.id] ?: Long.MIN_VALUE).compareTo(lastActivity[a.id] ?: Long.MIN_VALUE)
        }
        val engagementComparator = Comparator<GoalEntity> { a, b ->
            (activeDays[b.id] ?: 0).compareTo(activeDays[a.id] ?: 0)
        }
        return goals.sortedWith(deadlineComparator.thenComparing(activityComparator).thenComparing(engagementComparator))
    }

    /**
     * Lower rank = sorted earlier. Future deadlines rank by their timestamp (earlier = lower).
     * No deadline or past deadline ranks last (highest).
     */
    private fun rankDeadline(deadlineEpochMs: Long?, now: Long): Long {
        if (deadlineEpochMs == null || deadlineEpochMs < now) return Long.MAX_VALUE
        return deadlineEpochMs
    }
}

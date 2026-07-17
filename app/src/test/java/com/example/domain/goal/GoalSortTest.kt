package com.example.domain.goal

import com.example.core.goal.GoalEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalSortTest {

    private val NOW = System.currentTimeMillis()
    private val DAY = 86_400_000L

    private fun goal(id: Int, deadline: Long? = null) = GoalEntity(id = id, title = "G$id", deadlineEpochMs = deadline)

    @Test
    fun approachingDeadline_sortsFirst() {
        val far = goal(1, deadline = NOW + 100L * DAY)
        val near = goal(2, deadline = NOW + 2L * DAY)
        val none = goal(3, deadline = null)

        val sorted = GoalSort.sort(listOf(far, near, none), emptyMap(), emptyMap())
        assertEquals(listOf(2, 1, 3), sorted.map { it.id })
    }

    @Test
    fun noDeadline_sortsLast() {
        val withDl = goal(1, deadline = NOW + 5L * DAY)
        val without = goal(2, deadline = null)
        val sorted = GoalSort.sort(listOf(without, withDl), emptyMap(), emptyMap())
        assertEquals(listOf(1, 2), sorted.map { it.id })
    }

    @Test
    fun sameDeadline_recentActivityWins() {
        val a = goal(1, deadline = NOW + 5L * DAY)
        val b = goal(2, deadline = NOW + 5L * DAY)
        val lastActivity = mapOf(1 to NOW - 10L * DAY, 2 to NOW - 1L * DAY)
        val sorted = GoalSort.sort(listOf(a, b), lastActivity, emptyMap())
        assertEquals(listOf(2, 1), sorted.map { it.id })
    }

    @Test
    fun sameDeadlineSameActivity_engagementWins() {
        val a = goal(1, deadline = NOW + 5L * DAY)
        val b = goal(2, deadline = NOW + 5L * DAY)
        val lastActivity = mapOf(1 to NOW - DAY, 2 to NOW - DAY)
        val activeDays = mapOf(1 to 3, 2 to 10)
        val sorted = GoalSort.sort(listOf(a, b), lastActivity, activeDays)
        assertEquals(listOf(2, 1), sorted.map { it.id })
    }

    @Test
    fun priority_order_deadlineThenActivityThenEngagement() {
        // a: nearest deadline; b: far deadline but recent activity; c: no deadline, recent activity
        val a = goal(1, deadline = NOW + 1L * DAY)
        val b = goal(2, deadline = NOW + 20L * DAY)
        val c = goal(3, deadline = null)
        val lastActivity = mapOf(1 to NOW - 100L * DAY, 2 to NOW, 3 to NOW)
        val sorted = GoalSort.sort(listOf(c, b, a), lastActivity, emptyMap())
        assertEquals(listOf(1, 2, 3), sorted.map { it.id })
    }
}

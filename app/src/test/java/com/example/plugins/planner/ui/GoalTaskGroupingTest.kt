package com.example.plugins.planner.ui

import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalStatus
import com.example.plugins.planner.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalTaskGroupingTest {

    private fun task(id: Int, goalId: Int?): TaskEntity =
        TaskEntity(id = id, title = "t$id", dateEpochMs = 0L, goalId = goalId)

    private fun goal(id: Int, title: String): GoalEntity =
        GoalEntity(id = id, title = title, status = GoalStatus.ACTIVE)

    @Test
    fun `tasks with the same goal are grouped together`() {
        val tasks = listOf(task(1, 10), task(2, 10), task(3, 10))
        val groups = groupTasksByGoal(tasks, listOf(goal(10, "AI")))
        assertEquals(1, groups.size)
        assertEquals("AI", groups[0].goal?.title)
        assertEquals(3, groups[0].tasks.size)
    }

    @Test
    fun `tasks with different goals are separated`() {
        val tasks = listOf(task(1, 10), task(2, 20))
        val groups = groupTasksByGoal(tasks, listOf(goal(10, "AI"), goal(20, "Uni")))
        assertEquals(2, groups.size)
        assertEquals("AI", groups[0].goal?.title)
        assertEquals("Uni", groups[1].goal?.title)
    }

    @Test
    fun `tasks without goal appear in a no-goal group`() {
        val tasks = listOf(task(1, null), task(2, null))
        val groups = groupTasksByGoal(tasks, emptyList())
        assertEquals(1, groups.size)
        assertEquals(null, groups[0].goal)
        assertEquals(2, groups[0].tasks.size)
    }

    @Test
    fun `no-goal group is appended last when mixed`() {
        val tasks = listOf(task(1, 10), task(2, null), task(3, 20))
        val groups = groupTasksByGoal(tasks, listOf(goal(10, "AI"), goal(20, "Uni")))
        assertEquals(3, groups.size)
        assertEquals("AI", groups[0].goal?.title)
        assertEquals("Uni", groups[1].goal?.title)
        assertEquals(null, groups[2].goal)
    }

    @Test
    fun `empty task list returns empty groups`() {
        val groups = groupTasksByGoal(emptyList(), listOf(goal(10, "AI")))
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `tasks whose goal is missing from list are kept under inactive-goal placeholder`() {
        // Sprint 5.2 (F2): a task whose goal (99) is not in the provided (active) goals must NOT be
        // dropped — it is kept under a placeholder "هدف غیرفعال" group so the task stays visible.
        // Here "AI" (goal 10) has no tasks, so only 2 groups result: placeholder + no-goal.
        val tasks = listOf(task(1, 99), task(2, null))
        val groups = groupTasksByGoal(tasks, listOf(goal(10, "AI")))
        assertEquals(2, groups.size)
        assertEquals("هدف غیرفعال", groups[0].goal?.title)
        assertEquals(1, groups[0].tasks.size)
        assertEquals(1, groups[0].tasks[0].id)
        assertEquals(null, groups[1].goal)
        assertEquals(1, groups[1].tasks.size)
        assertEquals(2, groups[1].tasks[0].id)
    }

    @Test
    fun `inactive-goal placeholder keeps task visible while active goals still group`() {
        val tasks = listOf(task(1, 10), task(2, 99))
        val groups = groupTasksByGoal(tasks, listOf(goal(10, "AI")))
        assertEquals(2, groups.size)
        assertEquals("AI", groups[0].goal?.title)
        assertEquals("هدف غیرفعال", groups[1].goal?.title)
        assertEquals(2, groups[1].tasks[0].id)
    }
}

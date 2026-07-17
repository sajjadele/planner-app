package com.example.plugins.planner.ui

import com.example.plugins.planner.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskDayKeysTest {

    private fun task(id: Int, dateEpochMs: Long): TaskEntity =
        TaskEntity(id = id, title = "t$id", dateEpochMs = dateEpochMs)

    @Test
    fun `distinct days become distinct keys`() {
        val tasks = listOf(
            task(1, 1_000L),
            task(2, 2_000L),
            task(3, 3_000L)
        )
        val keys = taskDayKeys(tasks)
        assertEquals(setOf(1_000L, 2_000L, 3_000L), keys)
    }

    @Test
    fun `tasks on same day collapse to one key`() {
        val tasks = listOf(
            task(1, 5_000L),
            task(2, 5_000L),
            task(3, 5_000L)
        )
        val keys = taskDayKeys(tasks)
        assertEquals(setOf(5_000L), keys)
        assertEquals(1, keys.size)
    }

    @Test
    fun `empty list yields empty set`() {
        assertTrue(taskDayKeys(emptyList()).isEmpty())
    }

    @Test
    fun `distinct raw dateEpochMs values are preserved as distinct keys`() {
        // taskDayKeys maps directly on TaskEntity.dateEpochMs (which is midnight-aligned in the
        // app), so two different day keys remain distinct.
        val tasks = listOf(
            task(1, 86_400_000L),
            task(2, 86_400_000L + 86_400_000L)
        )
        assertEquals(setOf(86_400_000L, 172_800_000L), taskDayKeys(tasks))
    }
}

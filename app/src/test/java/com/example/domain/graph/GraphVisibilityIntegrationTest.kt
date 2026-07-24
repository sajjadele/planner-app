package com.example.domain.graph

import com.example.domain.attention.AttentionProvider
import com.example.domain.attention.TaskAttentionInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integration-style JVM test: task list changes propagate through
 * AttentionProvider → VisibilityResolver → VisibleGraphModel.
 *
 * Proves the reactive attention path used by GoalDetailViewModel.graphSource
 * without Android/Room.
 */
class GraphVisibilityIntegrationTest {

    private data class FakeTask(
        val id: Int,
        val title: String,
        val isCompleted: Boolean,
        val deadlineEpochMs: Long?,
        val rescheduleCount: Int = 0
    )

    private val now = 1_700_000_000_000L
    private val tomorrow = now + 86_400_000L
    private val farFuture = now + 90L * 86_400_000L

    private fun resolveVisible(tasks: List<FakeTask>, level: VisibilityLevel = VisibilityLevel.OVERVIEW): VisibleGraphModel {
        val active = tasks.filter { !it.isCompleted }
        val attention = AttentionProvider.compute(
            active.map {
                TaskAttentionInput(
                    id = it.id,
                    title = it.title,
                    dateEpochMs = null,
                    deadlineEpochMs = it.deadlineEpochMs,
                    lastMeaningfulInteractionMs = null,
                    rescheduleCount = it.rescheduleCount
                )
            },
            now
        )
        return VisibilityResolver.resolve(
            VisibilityInput(
                activeTaskIds = active.map { it.id },
                attentionResults = attention,
                taskMetadata = active.associate {
                    it.id to TaskMetadata(
                        id = it.id,
                        title = it.title,
                        priority = null,
                        dateEpochMs = null,
                        deadlineEpochMs = it.deadlineEpochMs,
                        rescheduleCount = it.rescheduleCount
                    )
                },
                level = level,
                nowMillis = now
            )
        )
    }

    @Test
    fun `task list change propagates to visible graph selection`() = runTest {
        val tasksFlow = MutableStateFlow(
            listOf(
                FakeTask(1, "far", false, farFuture),
                FakeTask(2, "near", false, tomorrow),
                FakeTask(3, "also-far", false, farFuture)
            )
        )

        val visibleFlow = tasksFlow.map { resolveVisible(it) }

        val first = visibleFlow.first()
        assertEquals(3, first.tasks.size)
        // Near-deadline task should rank highest and appear first
        assertEquals(2, first.tasks.first().taskId)

        // Complete the near-deadline task → it must drop out of visibility
        tasksFlow.value = tasksFlow.value.map {
            if (it.id == 2) it.copy(isCompleted = true) else it
        }
        val second = visibleFlow.first()
        assertEquals(2, second.tasks.size)
        assertFalse(second.tasks.any { it.taskId == 2 })
        assertTrue(second.tasks.any { it.taskId == 1 })
        assertTrue(second.tasks.any { it.taskId == 3 })
    }

    @Test
    fun `adding high attention task updates overview selection`() = runTest {
        val tasksFlow = MutableStateFlow(
            listOf(
                FakeTask(1, "a", false, farFuture),
                FakeTask(2, "b", false, farFuture),
                FakeTask(3, "c", false, farFuture),
                FakeTask(4, "d", false, farFuture),
                FakeTask(5, "e", false, farFuture),
                FakeTask(6, "f", false, farFuture),
                FakeTask(7, "g", false, farFuture),
                FakeTask(8, "h", false, farFuture)
            )
        )
        val visibleFlow = tasksFlow.map { resolveVisible(it) }

        val before = visibleFlow.first()
        assertEquals(7, before.tasks.size)
        assertFalse(before.tasks.any { it.taskId == 8 }) // lowest of 8, dropped by overview max

        // Add a very high-attention overdue-like task via heavy reschedule + near deadline
        tasksFlow.value = tasksFlow.value + FakeTask(
            id = 99,
            title = "urgent",
            isCompleted = false,
            deadlineEpochMs = tomorrow,
            rescheduleCount = 5
        )
        val after = visibleFlow.first()
        assertEquals(7, after.tasks.size)
        assertTrue("New high-attention task must enter overview", after.tasks.any { it.taskId == 99 })
        assertEquals(99, after.tasks.first().taskId)
    }

    @Test
    fun `goalGraphBuilder no longer decides which tasks are visible`() {
        // Geometry-only sun scaffold has no task nodes regardless of task count.
        val sun = GoalGraphBuilder.buildSun(1, "G", null)
        assertEquals(1, sun.nodes.size)
        assertEquals(NodeKind.GOAL, sun.nodes.single().kind)
    }
}

package com.example.domain.graph

import com.example.domain.goal.GoalProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host-JVM unit tests for the Behavioral Solar System builder.
 * Pure Kotlin — no Android/Room dependencies. Mirrors GoalProgressCalculatorTest style.
 */
class GoalGraphBuilderTest {

    private fun task(
        id: Int,
        priority: String? = null,
        isCompleted: Boolean = false,
        deadlineEpochMs: Long? = null
    ) = GoalGraphBuilder.TaskInput(
        id = id,
        title = "task-$id",
        priority = priority,
        isCompleted = isCompleted,
        deadlineEpochMs = deadlineEpochMs
    )

    private val progress = GoalProgress(completionRate = 60f, activityMomentum = 30f, overall = 51f)

    // ── Lane ordering: HIGH sits closer to center than LOW ──
    @Test
    fun `HIGH lane has smaller radius than LOW lane`() {
        val graph = GoalGraphBuilder.build(
            goalId = 1,
            goalTitle = "Goal",
            tasks = listOf(task(1, "HIGH"), task(2, "LOW")),
            rescheduleCounts = emptyMap(),
            progress = progress
        )
        val high = graph.nodes.first { it.id == 1 }
        val low = graph.nodes.first { it.id == 2 }
        val highR = GraphGeometry.distance(graph.centerX, graph.centerY, high.cx, high.cy)
        val lowR = GraphGeometry.distance(graph.centerX, graph.centerY, low.cx, low.cy)
        assertTrue(highR < lowR)
    }

    // ── Even angle distribution within a lane ──
    @Test
    fun `two tasks in same lane split the circle symmetrically`() {
        val graph = GoalGraphBuilder.build(
            goalId = 1,
            goalTitle = "Goal",
            tasks = listOf(task(1, "HIGH"), task(2, "HIGH")),
            rescheduleCounts = emptyMap(),
            progress = progress
        )
        val a = graph.nodes.first { it.id == 1 }
        val b = graph.nodes.first { it.id == 2 }
        val angA = kotlin.math.atan2((a.cy - graph.centerY).toDouble(), (a.cx - graph.centerX).toDouble())
        val angB = kotlin.math.atan2((b.cy - graph.centerY).toDouble(), (b.cx - graph.centerX).toDouble())
        var delta = kotlin.math.abs(angA - angB)
        if (delta > kotlin.math.PI) delta = (2 * kotlin.math.PI - delta)
        // Two nodes → expected half-circle separation (~PI), allowing for the small id jitter.
        assertTrue(delta > 2.5f)
    }

    @Test
    fun `single task in lane is placed at a fixed angle`() {
        val g1 = GoalGraphBuilder.build(1, "G", listOf(task(7, "MEDIUM")), emptyMap(), progress)
        val g2 = GoalGraphBuilder.build(1, "G", listOf(task(7, "MEDIUM")), emptyMap(), progress)
        val n1 = g1.nodes.first { it.id == 7 }
        val n2 = g2.nodes.first { it.id == 7 }
        assertEquals(n1.cx, n2.cx)
        assertEquals(n1.cy, n2.cy)
    }

    // ── Strict determinism: identical inputs → identical coordinates ──
    @Test
    fun `same inputs produce identical coordinates`() {
        val tasks = listOf(
            task(1, "HIGH"), task(2, "HIGH"), task(3, "MEDIUM"),
            task(4, "LOW"), task(5, "LOW"), task(6, null)
        )
        val counts = mapOf(2 to 3, 4 to 2)
        val g1 = GoalGraphBuilder.build(1, "Goal", tasks, counts, progress)
        val g2 = GoalGraphBuilder.build(1, "Goal", tasks, counts, progress)
        assertEquals(g1.nodes.size, g2.nodes.size)
        g1.nodes.zip(g2.nodes).forEach { (n1, n2) ->
            assertEquals(n1.id, n2.id)
            assertEquals("cx mismatch for task ${n1.id}", n1.cx, n2.cx)
            assertEquals("cy mismatch for task ${n1.id}", n1.cy, n2.cy)
        }
        assertEquals(g1.edges, g2.edges)
    }

    // ── Priority sizing ──
    @Test
    fun `node size follows priority ordering HIGH greater than MEDIUM greater than LOW`() {
        val graph = GoalGraphBuilder.build(
            goalId = 1,
            goalTitle = "Goal",
            tasks = listOf(task(1, "HIGH"), task(2, "MEDIUM"), task(3, "LOW")),
            rescheduleCounts = emptyMap(),
            progress = progress
        )
        val high = graph.nodes.first { it.id == 1 }.size
        val medium = graph.nodes.first { it.id == 2 }.size
        val low = graph.nodes.first { it.id == 3 }.size
        assertTrue(high > medium)
        assertTrue(medium > low)
    }

    // ── Boulder flag mapping (threshold = 2) ──
    @Test
    fun `boulder flag set when reschedule count at or above threshold`() {
        val graph = GoalGraphBuilder.build(
            goalId = 1,
            goalTitle = "Goal",
            tasks = listOf(task(1, "HIGH"), task(2, "HIGH"), task(3, "HIGH")),
            rescheduleCounts = mapOf(1 to 1, 2 to 2, 3 to 5),
            progress = progress
        )
        assertFalse(graph.nodes.first { it.id == 1 }.isBoulder)
        assertTrue(graph.nodes.first { it.id == 2 }.isBoulder)
        assertTrue(graph.nodes.first { it.id == 3 }.isBoulder)
        assertEquals(ColorRole.BOULDER, graph.nodes.first { it.id == 2 }.colorRole)
    }

    // ── Completion mapping: faded outer edge points ──
    @Test
    fun `completed tasks are faded and orbit on the outer edge`() {
        val graph = GoalGraphBuilder.build(
            goalId = 1,
            goalTitle = "Goal",
            tasks = listOf(task(1, "HIGH"), task(2, "HIGH", isCompleted = true)),
            rescheduleCounts = emptyMap(),
            progress = progress
        )
        val completed = graph.nodes.first { it.id == 2 }
        assertTrue(completed.isCompleted)
        assertTrue(completed.alpha < 1f)
        val r = GraphGeometry.distance(graph.centerX, graph.centerY, completed.cx, completed.cy)
        assertTrue(r > graph.viewportRadius * 0.9f)
        assertEquals(ColorRole.COMPLETED, completed.colorRole)
        // edge connection to a completed node is faded too
        assertTrue(graph.edges.first { it.toId == 2 }.alpha < 1f)
    }

    // ── Ring Tide: overall progress surfaces to the graph ──
    @Test
    fun `goal progress overall drives ring tide value`() {
        val graph = GoalGraphBuilder.build(
            goalId = 1,
            goalTitle = "Goal",
            tasks = emptyList(),
            rescheduleCounts = emptyMap(),
            progress = progress
        )
        assertEquals(51f, graph.goalProgressOverall)
    }

    @Test
    fun `ring tide is zero when progress is null`() {
        val graph = GoalGraphBuilder.build(
            goalId = 1,
            goalTitle = "Goal",
            tasks = emptyList(),
            rescheduleCounts = emptyMap(),
            progress = null
        )
        assertEquals(0f, graph.goalProgressOverall)
        // exactly one node (the sun) when there are no tasks
        assertEquals(1, graph.nodes.size)
        assertEquals(NodeKind.GOAL, graph.nodes.first().kind)
    }

    // ── Duplicate-node regression: null-priority tasks must not be placed twice ──
    @Test
    fun `two null-priority active tasks render exactly two task nodes`() {
        val graph = GoalGraphBuilder.build(
            goalId = 1,
            goalTitle = "Goal",
            tasks = listOf(task(1, null), task(2, null)),
            rescheduleCounts = emptyMap(),
            progress = progress
        )
        val taskNodes = graph.nodes.filter { it.kind == NodeKind.TASK }
        assertEquals(2, taskNodes.size)
        // no duplicate ids
        assertEquals(taskNodes.map { it.id }.toSet().size, taskNodes.size)
    }
}

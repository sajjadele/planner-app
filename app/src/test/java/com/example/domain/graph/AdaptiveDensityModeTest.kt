package com.example.domain.graph

import com.example.domain.goal.GoalProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the Phase 6.5.6 adaptive density Behavioral Solar System.
 * No Android/Room dependencies. Mirrors GoalGraphBuilderTest style.
 */
class AdaptiveDensityModeTest {

    private fun task(
        id: Int,
        priority: String? = null,
        isCompleted: Boolean = false,
        dateEpochMs: Long? = null
    ) = GoalGraphBuilder.TaskInput(
        id = id,
        title = "task-$id",
        priority = priority,
        isCompleted = isCompleted,
        dateEpochMs = dateEpochMs
    )

    private val progress = GoalProgress(completionRate = 60f, activityMomentum = 30f, overall = 51f)

    // ── Density tier selection by active task count (SIMPLE ≤6, CLUSTERED ≤20, SUMMARY >20) ──
    @Test
    fun `5 active tasks renders SIMPLE density`() {
        val tasks = (1..5).map { task(it, "MEDIUM") }
        val graph = GoalGraphBuilder.build(1, "G", tasks, emptyMap(), progress)
        assertEquals(GraphDensityMode.SIMPLE, graph.densityMode)
        assertTrue(graph.clusters.isEmpty())
    }

    @Test
    fun `6 active tasks renders SIMPLE density`() {
        val tasks = (1..6).map { task(it, "MEDIUM") }
        val graph = GoalGraphBuilder.build(1, "G", tasks, emptyMap(), progress)
        assertEquals(GraphDensityMode.SIMPLE, graph.densityMode)
        assertTrue(graph.clusters.isEmpty())
    }

    @Test
    fun `7 active tasks renders CLUSTERED density`() {
        val tasks = (1..7).map { task(it, "MEDIUM") }
        val graph = GoalGraphBuilder.build(1, "G", tasks, emptyMap(), progress)
        assertEquals(GraphDensityMode.CLUSTERED, graph.densityMode)
        assertFalse(graph.clusters.isEmpty())
    }

    @Test
    fun `20 active tasks renders CLUSTERED density`() {
        val tasks = (1..20).map { task(it, "MEDIUM") }
        val graph = GoalGraphBuilder.build(1, "G", tasks, emptyMap(), progress)
        assertEquals(GraphDensityMode.CLUSTERED, graph.densityMode)
        assertFalse(graph.clusters.isEmpty())
    }

    @Test
    fun `21 active tasks renders SUMMARY density`() {
        val tasks = (1..21).map { task(it, "MEDIUM") }
        val graph = GoalGraphBuilder.build(1, "G", tasks, emptyMap(), progress)
        assertEquals(GraphDensityMode.SUMMARY, graph.densityMode)
        assertFalse(graph.clusters.isEmpty())
    }

    // ── Cluster contents ──
    @Test
    fun `CLUSTERED density groups high priority tasks into ACTIVE_HIGH cluster`() {
        val tasks = (1..9).map { task(it, "HIGH") }
        val graph = GoalGraphBuilder.build(1, "G", tasks, emptyMap(), progress)
        val high = graph.clusters.first { it.clusterType == ClusterType.ACTIVE_HIGH }
        assertEquals(9, high.taskCount)
        assertEquals("HIGH", high.priorityLevel)
        assertEquals((1..9).toSet(), high.memberIds.toSet())
    }

    @Test
    fun `CLUSTERED density groups medium priority tasks into ACTIVE_MEDIUM cluster`() {
        val tasks = (1..9).map { task(it, "MEDIUM") }
        val graph = GoalGraphBuilder.build(1, "G", tasks, emptyMap(), progress)
        val med = graph.clusters.first { it.clusterType == ClusterType.ACTIVE_MEDIUM }
        assertEquals(9, med.taskCount)
        assertEquals("MEDIUM", med.priorityLevel)
    }

    @Test
    fun `CLUSTERED density groups completed tasks into COMPLETED cluster`() {
        val active = (1..9).map { task(it, "LOW") }
        val done = (10..14).map { task(it, "LOW", isCompleted = true) }
        val graph = GoalGraphBuilder.build(1, "G", active + done, emptyMap(), progress)
        val completed = graph.clusters.firstOrNull { it.clusterType == ClusterType.COMPLETED }
        assertNotNull(completed)
        assertEquals(5, completed!!.taskCount)
        assertEquals((10..14).toSet(), completed.memberIds.toSet())
    }

    @Test
    fun `empty priority lanes produce no cluster for that type`() {
        // Only LOW active + no completed → no HIGH/MEDIUM/COMPLETED clusters.
        val tasks = (1..9).map { task(it, "LOW") }
        val graph = GoalGraphBuilder.build(1, "G", tasks, emptyMap(), progress)
        val types = graph.clusters.map { it.clusterType }
        assertEquals(listOf(ClusterType.ACTIVE_LOW), types)
    }

    // ── Deterministic cluster positions ──
    @Test
    fun `cluster positions are deterministic across builds`() {
        val tasks = (1..9).map { task(it, "HIGH") }
        val g1 = GoalGraphBuilder.build(1, "G", tasks, emptyMap(), progress)
        val g2 = GoalGraphBuilder.build(1, "G", tasks, emptyMap(), progress)
        g1.clusters.zip(g2.clusters).forEach { (a, b) ->
            assertEquals(a.clusterType, b.clusterType)
            assertEquals("cx mismatch for ${a.clusterType}", a.cx, b.cx)
            assertEquals("cy mismatch for ${a.clusterType}", a.cy, b.cy)
        }
    }

    @Test
    fun `cluster visualSize never exceeds sun size`() {
        // Many tasks → cluster size clamps below GOAL_SIZE (40f). SUMMARY tier.
        val tasks = (1..50).map { task(it, "MEDIUM") }
        val graph = GoalGraphBuilder.build(1, "G", tasks, emptyMap(), progress)
        graph.clusters.forEach { cluster ->
            assertTrue("cluster ${cluster.clusterType} too large", cluster.visualSize < 40f)
        }
    }

    @Test
    fun `SIMPLE density orders displayed tasks by priority rank`() {
        val tasks = listOf(
            task(1, "LOW"),
            task(2, "HIGH"),
            task(3, "MEDIUM"),
            task(4, "LOW"),
            task(5, "HIGH")
        )
        val graph = GoalGraphBuilder.build(1, "G", tasks, emptyMap(), progress)
        assertEquals(GraphDensityMode.SIMPLE, graph.densityMode)
        val active = graph.nodes.filter { it.kind == NodeKind.TASK && !it.isCompleted }
        // HIGH tasks should appear before LOW tasks in the node list (priority ordering).
        val firstHighIdx = active.indexOfFirst { it.priority == "HIGH" }
        val firstLowIdx = active.indexOfFirst { it.priority == "LOW" }
        assertTrue(firstHighIdx >= 0)
        assertTrue(firstLowIdx >= 0)
        assertTrue(firstHighIdx < firstLowIdx)
    }
}

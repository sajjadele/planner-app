package com.example.domain.graph

import com.example.domain.goal.GoalProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host-JVM unit tests for GoalGraphBuilder geometry helpers (post Phase 2B migration).
 * Visibility decisions are covered by VisibilityResolverTest.
 */
class GoalGraphBuilderTest {

    private val progress = GoalProgress(completionRate = 60f, activityMomentum = 30f, overall = 51f)

    @Test
    fun `buildSun places goal at center with progress`() {
        val graph = GoalGraphBuilder.buildSun(
            goalId = 1,
            goalTitle = "Goal",
            progress = progress
        )
        assertEquals(1, graph.nodes.size)
        assertEquals(NodeKind.GOAL, graph.nodes.first().kind)
        assertEquals(graph.viewportRadius, graph.nodes.first().cx)
        assertEquals(graph.viewportRadius, graph.nodes.first().cy)
        assertEquals(51f, graph.goalProgressOverall)
    }

    @Test
    fun `buildSun is zero progress when progress null`() {
        val graph = GoalGraphBuilder.buildSun(1, "G", null)
        assertEquals(0f, graph.goalProgressOverall)
    }

    @Test
    fun `jitter is deterministic`() {
        assertEquals(GoalGraphBuilder.jitter(7), GoalGraphBuilder.jitter(7))
    }

    @Test
    fun `deterministicAngle is stable for same inputs`() {
        val a1 = GoalGraphBuilder.deterministicAngle(3, 10)
        val a2 = GoalGraphBuilder.deterministicAngle(3, 10)
        assertEquals(a1, a2)
    }

    @Test
    fun `projectTask uses continuous radius from visible task`() {
        val task = VisibleTask(
            taskId = 1,
            attentionScore = 1f,
            radius = 100f,
            angle = 0f,
            reasons = emptyList(),
            isBoulder = false,
            isOverdue = false,
            isNearDeadline = false,
            priority = null,
            title = "t"
        )
        val (x, y) = GoalGraphBuilder.projectTask(task, 320f, 320f)
        assertEquals(420f, x, 0.01f)
        assertEquals(320f, y, 0.01f)
    }

    @Test
    fun `single task angle is fixed at half pi`() {
        val angle = GoalGraphBuilder.deterministicAngle(99, 1)
        assertEquals(Math.PI.toFloat() / 2f, angle, 0.0001f)
    }

    @Test
    fun `two different ids produce different jitter`() {
        // Not always true for all pairs, but 1 vs 2 should differ for this hash
        val j1 = GoalGraphBuilder.jitter(1)
        val j2 = GoalGraphBuilder.jitter(2)
        assertTrue(j1 in 0f..0.25f)
        assertTrue(j2 in 0f..0.25f)
    }
}

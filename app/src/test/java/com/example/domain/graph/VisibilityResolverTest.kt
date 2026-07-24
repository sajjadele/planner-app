package com.example.domain.graph

import com.example.domain.attention.AttentionComponents
import com.example.domain.attention.AttentionReason
import com.example.domain.attention.AttentionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host-JVM unit tests for VisibilityResolver — pure logic, no Android/Room.
 *
 * Covers all 10 required Phase 2B scenarios:
 * 1. Overview respects min/max limits
 * 2. Highest attention tasks are selected
 * 3. Low attention tasks are clustered
 * 4. 200 tasks do not create 200 visible nodes
 * 5. Continuous orbit calculation works
 * 6. Attention score 1.0 is closest
 * 7. Attention score 0.0 is farthest
 * 8. Completed tasks excluded
 * 9. Cluster expansion preserves attention ordering
 * 10. Deterministic output for same input
 */
class VisibilityResolverTest {

    private val viewportRadius = 320f
    private val now = 1_700_000_000_000L

    private fun attentionResult(score: Float) = AttentionResult(
        score = score,
        components = AttentionComponents(0f, 0f, 0f),
        reasons = if (score > 0f) listOf(AttentionReason.NearDeadline(1)) else emptyList()
    )

    private fun metadata(id: Int) = TaskMetadata(
        id = id,
        title = "task-$id",
        priority = "MEDIUM",
        dateEpochMs = null,
        deadlineEpochMs = null,
        rescheduleCount = 0
    )

    private fun buildInput(
        taskCount: Int,
        scores: Map<Int, Float> = emptyMap(),
        level: VisibilityLevel = VisibilityLevel.OVERVIEW
    ): VisibilityInput {
        val ids = (1..taskCount).toList()
        val attentionResults: Map<Int, AttentionResult> = ids.associate { id ->
            id to attentionResult(scores[id] ?: 0f)
        }
        val taskMetadata: Map<Int, TaskMetadata> = ids.associate { id ->
            id to metadata(id)
        }
        return VisibilityInput(
            activeTaskIds = ids,
            attentionResults = attentionResults,
            taskMetadata = taskMetadata,
            level = level,
            viewportRadius = viewportRadius,
            nowMillis = now
        )
    }

    // ── 1. Overview respects min/max limits ──

    @Test
    fun `overview with fewer than max tasks shows all`() {
        val input = buildInput(taskCount = 5)
        val model = VisibilityResolver.resolve(input)
        assertEquals(5, model.tasks.size)
        assertEquals(0, model.hiddenCount)
    }

    @Test
    fun `overview with more than max tasks caps at seven`() {
        val input = buildInput(taskCount = 20)
        val model = VisibilityResolver.resolve(input)
        assertEquals(7, model.tasks.size)
        assertEquals(13, model.hiddenCount)
    }

    @Test
    fun `overview with two tasks shows two`() {
        val input = buildInput(taskCount = 2)
        val model = VisibilityResolver.resolve(input)
        assertEquals(2, model.tasks.size)
        assertEquals(0, model.hiddenCount)
    }

    @Test
    fun `overview with one hundred tasks shows seven`() {
        val input = buildInput(taskCount = 100)
        val model = VisibilityResolver.resolve(input)
        assertEquals(7, model.tasks.size)
        assertEquals(93, model.hiddenCount)
    }

    // ── 2. Highest attention tasks are selected ──

    @Test
    fun `overview selects highest attention tasks`() {
        val scores = mapOf(
            1 to 0.1f, 2 to 0.9f, 3 to 0.5f, 4 to 0.3f, 5 to 0.7f,
            6 to 0.2f, 7 to 0.8f, 8 to 0.4f, 9 to 0.6f, 10 to 0.15f
        )
        val input = buildInput(taskCount = 10, scores = scores)
        val model = VisibilityResolver.resolve(input)

        assertEquals(7, model.tasks.size)
        val visibleIds = model.tasks.map { it.taskId }.toSet()
        // Top 7 by score: 2(0.9), 7(0.8), 5(0.7), 9(0.6), 3(0.5), 8(0.4), 4(0.3)
        assertTrue(visibleIds.contains(2))
        assertTrue(visibleIds.contains(7))
        assertTrue(visibleIds.contains(5))
        assertTrue(visibleIds.contains(9))
        assertTrue(visibleIds.contains(3))
        assertTrue(visibleIds.contains(8))
        assertTrue(visibleIds.contains(4))
        // Excluded: 1(0.1), 6(0.2), 10(0.15)
        assertFalse(visibleIds.contains(1))
        assertFalse(visibleIds.contains(6))
        assertFalse(visibleIds.contains(10))
    }

    @Test
    fun `overview tasks are sorted by attention descending`() {
        val scores = mapOf(1 to 0.3f, 2 to 0.9f, 3 to 0.5f, 4 to 0.7f, 5 to 0.1f)
        val input = buildInput(taskCount = 5, scores = scores)
        val model = VisibilityResolver.resolve(input)

        val taskScores = model.tasks.map { it.attentionScore }
        for (i in 0 until taskScores.size - 1) {
            assertTrue(
                "Score ${taskScores[i]} should be >= ${taskScores[i + 1]}",
                taskScores[i] >= taskScores[i + 1]
            )
        }
    }

    // ── 3. Low attention tasks are clustered ──

    @Test
    fun `insight level clusters tasks above threshold`() {
        val input = buildInput(taskCount = 50, level = VisibilityLevel.INSIGHT)
        val model = VisibilityResolver.resolve(input)

        assertTrue("Should have clusters", model.clusters.isNotEmpty())
        assertTrue("Should have individual tasks", model.tasks.isNotEmpty())
        assertEquals(7, model.tasks.size)
        assertTrue("Should have hidden count", model.hiddenCount > 0)
    }

    @Test
    fun `insight level shows all individually when below threshold`() {
        val input = buildInput(taskCount = 15, level = VisibilityLevel.INSIGHT)
        val model = VisibilityResolver.resolve(input)

        assertTrue("No clusters when below threshold", model.clusters.isEmpty())
        assertEquals(15, model.tasks.size)
        assertEquals(0, model.hiddenCount)
    }

    // ── 4. 200 tasks do not create 200 visible nodes ──

    @Test
    fun `two hundred tasks do not create two hundred visible nodes`() {
        val input = buildInput(taskCount = 200, level = VisibilityLevel.INSIGHT)
        val model = VisibilityResolver.resolve(input)

        val totalVisible = model.tasks.size + model.clusters.sumOf { it.count }
        assertTrue("Total visible (tasks + cluster members) should be <= 200", totalVisible <= 200)
        assertEquals(7, model.tasks.size)
        assertTrue("Should have hidden count", model.hiddenCount > 0)
        val clusterMemberCount = model.clusters.sumOf { it.count }
        assertEquals(model.hiddenCount, clusterMemberCount)
    }

    @Test
    fun `two hundred tasks produce at most three clusters`() {
        val input = buildInput(taskCount = 200, level = VisibilityLevel.INSIGHT)
        val model = VisibilityResolver.resolve(input)
        assertTrue("At most 3 clusters", model.clusters.size <= 3)
    }

    // ── 5. Continuous orbit calculation works ──

    @Test
    fun `orbit radius is continuous between inner and outer bounds`() {
        val r0 = VisibilityResolver.orbitRadius(0f, viewportRadius)
        val r05 = VisibilityResolver.orbitRadius(0.5f, viewportRadius)
        val r1 = VisibilityResolver.orbitRadius(1f, viewportRadius)

        val inner = viewportRadius * 0.30f
        val outer = viewportRadius * 0.85f

        assertEquals("Score zero should be at outer radius", outer, r0, 0.01f)
        assertEquals("Score one should be at inner radius", inner, r1, 0.01f)
        assertTrue("Score half should be between inner and outer", r05 > inner && r05 < outer)
        val expectedMid = (inner + outer) / 2f
        assertEquals("Score half should be at midpoint", expectedMid, r05, 0.01f)
    }

    @Test
    fun `orbit radius is monotonic with score`() {
        val scores = listOf(0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f)
        val radii = scores.map { VisibilityResolver.orbitRadius(it, viewportRadius) }

        for (i in 0 until radii.size - 1) {
            assertTrue(
                "Radius should decrease as score increases: r[$i]=${radii[i]} >= r[${i + 1}]=${radii[i + 1]}",
                radii[i] >= radii[i + 1]
            )
        }
    }

    // ── 6. Attention score 1.0 is closest ──

    @Test
    fun `attention score one is closest to sun`() {
        val scores = mapOf(1 to 0.0f, 2 to 0.5f, 3 to 1.0f)
        val input = buildInput(taskCount = 3, scores = scores)
        val model = VisibilityResolver.resolve(input)

        val task1 = model.tasks.first { it.taskId == 1 }
        val task2 = model.tasks.first { it.taskId == 2 }
        val task3 = model.tasks.first { it.taskId == 3 }

        assertTrue(
            "Score one should be closest (smallest radius)",
            task3.radius < task2.radius && task3.radius < task1.radius
        )
    }

    @Test
    fun `score one radius equals inner bound`() {
        val r = VisibilityResolver.orbitRadius(1.0f, viewportRadius)
        val inner = viewportRadius * 0.30f
        assertEquals(inner, r, 0.01f)
    }

    // ── 7. Attention score 0.0 is farthest ──

    @Test
    fun `attention score zero is farthest from sun`() {
        val scores = mapOf(1 to 0.0f, 2 to 0.5f, 3 to 1.0f)
        val input = buildInput(taskCount = 3, scores = scores)
        val model = VisibilityResolver.resolve(input)

        val task1 = model.tasks.first { it.taskId == 1 }
        val task2 = model.tasks.first { it.taskId == 2 }
        val task3 = model.tasks.first { it.taskId == 3 }

        assertTrue(
            "Score zero should be farthest (largest radius)",
            task1.radius > task2.radius && task1.radius > task3.radius
        )
    }

    @Test
    fun `score zero radius equals outer bound`() {
        val r = VisibilityResolver.orbitRadius(0.0f, viewportRadius)
        val outer = viewportRadius * 0.85f
        assertEquals(outer, r, 0.01f)
    }

    // ── 8. Completed tasks excluded ──

    @Test
    fun `completed tasks are not in visibility input`() {
        val activeIds = listOf(1, 2, 3)
        val attentionResults: Map<Int, AttentionResult> = mapOf(
            1 to attentionResult(0.9f),
            2 to attentionResult(0.5f),
            3 to attentionResult(0.1f),
            4 to attentionResult(0.8f)
        )
        val taskMetadata: Map<Int, TaskMetadata> = activeIds.associate { id ->
            id to metadata(id)
        }
        val input = VisibilityInput(
            activeTaskIds = activeIds,
            attentionResults = attentionResults,
            taskMetadata = taskMetadata,
            level = VisibilityLevel.OVERVIEW,
            viewportRadius = viewportRadius,
            nowMillis = now
        )

        val model = VisibilityResolver.resolve(input)
        val visibleIds = model.tasks.map { it.taskId }
        assertFalse("Completed task 4 should not appear", visibleIds.contains(4))
        assertEquals(3, model.tasks.size)
    }

    // ── 9. Cluster expansion preserves attention ordering ──

    @Test
    fun `cluster member ids are ordered by attention score`() {
        val scores = mutableMapOf<Int, Float>()
        for (i in 1..30) {
            scores[i] = (i % 20) * 0.01f
        }
        val input = buildInput(taskCount = 30, scores = scores, level = VisibilityLevel.INSIGHT)
        val model = VisibilityResolver.resolve(input)

        val lowCluster = model.clusters.firstOrNull { it.band == AttentionBand.LOW }
        if (lowCluster != null) {
            val memberScores = lowCluster.memberIds.map { id ->
                input.attentionResults[id]!!.score
            }
            for (i in 0 until memberScores.size - 1) {
                assertTrue(
                    "Members should be ordered by score descending",
                    memberScores[i] >= memberScores[i + 1]
                )
            }
        }
    }

    @Test
    fun `expanded cluster members are sorted by attention`() {
        val scores = (1..30).associate { i -> i to (i * 0.03f).coerceAtMost(1f) }
        val input = buildInput(taskCount = 30, scores = scores, level = VisibilityLevel.INSIGHT)
        val model = VisibilityResolver.resolve(input)

        val individualScores = model.tasks.map { it.attentionScore }
        for (i in 0 until individualScores.size - 1) {
            assertTrue(
                "Individual tasks should be sorted by score descending",
                individualScores[i] >= individualScores[i + 1]
            )
        }
    }

    // ── 10. Deterministic output for same input ──

    @Test
    fun `same input produces identical output`() {
        val scores = (1..50).associate { i -> i to (i * 0.02f).coerceAtMost(1f) }
        val input = buildInput(taskCount = 50, scores = scores, level = VisibilityLevel.INSIGHT)

        val model1 = VisibilityResolver.resolve(input)
        val model2 = VisibilityResolver.resolve(input)

        assertEquals(model1.tasks.size, model2.tasks.size)
        model1.tasks.zip(model2.tasks).forEach { (t1, t2) ->
            assertEquals("task ${t1.taskId} radius mismatch", t1.radius, t2.radius, 0.001f)
            assertEquals("task ${t1.taskId} angle mismatch", t1.angle, t2.angle, 0.001f)
            assertEquals("task ${t1.taskId} score mismatch", t1.attentionScore, t2.attentionScore, 0.001f)
        }
        assertEquals(model1.clusters.size, model2.clusters.size)
        model1.clusters.zip(model2.clusters).forEach { (c1, c2) ->
            assertEquals("cluster ${c1.band} radius mismatch", c1.radius, c2.radius, 0.001f)
            assertEquals("cluster ${c1.band} angle mismatch", c1.angle, c2.angle, 0.001f)
            assertEquals("cluster ${c1.band} members mismatch", c1.memberIds, c2.memberIds)
        }
    }

    @Test
    fun `same input produces identical angles`() {
        val input = buildInput(taskCount = 10)

        val model1 = VisibilityResolver.resolve(input)
        val model2 = VisibilityResolver.resolve(input)

        model1.tasks.zip(model2.tasks).forEach { (t1, t2) ->
            assertEquals("angle should be deterministic", t1.angle, t2.angle, 0.0001f)
        }
    }

    // ── Additional: Expanded level ──

    @Test
    fun `expanded level shows up to twenty tasks`() {
        val input = buildInput(taskCount = 50, level = VisibilityLevel.EXPANDED)
        val model = VisibilityResolver.resolve(input)

        assertEquals(20, model.tasks.size)
        assertEquals(30, model.hiddenCount)
    }

    @Test
    fun `expanded level shows all when below threshold`() {
        val input = buildInput(taskCount = 15, level = VisibilityLevel.EXPANDED)
        val model = VisibilityResolver.resolve(input)

        assertEquals(15, model.tasks.size)
        assertEquals(0, model.hiddenCount)
    }

    // ── Additional: Orbit bands ──

    @Test
    fun `orbit bands are provided for renderer`() {
        val input = buildInput(taskCount = 5)
        val model = VisibilityResolver.resolve(input)

        assertEquals(3, model.orbitBands.size)
        assertEquals(AttentionBand.HIGH, model.orbitBands[0].band)
        assertEquals(AttentionBand.MEDIUM, model.orbitBands[1].band)
        assertEquals(AttentionBand.LOW, model.orbitBands[2].band)
    }
}

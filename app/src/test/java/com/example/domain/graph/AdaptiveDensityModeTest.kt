package com.example.domain.graph

import com.example.domain.attention.AttentionComponents
import com.example.domain.attention.AttentionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Adaptive density / clustering thresholds now live on VisibilityResolver (Phase 2B).
 * These tests lock the MVP CLUSTER_THRESHOLD and level behavior.
 */
class AdaptiveDensityModeTest {

    private fun attention(score: Float) = AttentionResult(
        score = score,
        components = AttentionComponents(0f, 0f, 0f),
        reasons = emptyList()
    )

    private fun input(count: Int, level: VisibilityLevel): VisibilityInput {
        val ids = (1..count).toList()
        return VisibilityInput(
            activeTaskIds = ids,
            attentionResults = ids.associate { it to attention(0.1f) },
            taskMetadata = ids.associate {
                it to TaskMetadata(it, "t-$it", null, null, null, 0)
            },
            level = level,
            viewportRadius = 320f,
            nowMillis = 1_700_000_000_000L
        )
    }

    @Test
    fun `insight below cluster threshold shows individuals only`() {
        val model = VisibilityResolver.resolve(input(20, VisibilityLevel.INSIGHT))
        assertEquals(20, model.tasks.size)
        assertTrue(model.clusters.isEmpty())
    }

    @Test
    fun `insight above cluster threshold enables clusters`() {
        val model = VisibilityResolver.resolve(input(21, VisibilityLevel.INSIGHT))
        assertTrue(model.clusters.isNotEmpty())
        assertEquals(VisibilityResolver.OVERVIEW_MAX, model.tasks.size)
    }

    @Test
    fun `overview never exceeds max seven`() {
        val model = VisibilityResolver.resolve(input(50, VisibilityLevel.OVERVIEW))
        assertEquals(7, model.tasks.size)
    }

    @Test
    fun `expanded caps at cluster threshold`() {
        val model = VisibilityResolver.resolve(input(50, VisibilityLevel.EXPANDED))
        assertEquals(VisibilityResolver.CLUSTER_THRESHOLD, model.tasks.size)
    }

    @Test
    fun `cluster threshold constant is twenty`() {
        assertEquals(20, VisibilityResolver.CLUSTER_THRESHOLD)
    }
}

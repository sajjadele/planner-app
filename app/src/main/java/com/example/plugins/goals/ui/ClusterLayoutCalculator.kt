package com.example.plugins.goals.ui

import com.example.domain.graph.GraphGeometry
import com.example.domain.graph.VisibleCluster

/**
 * Pure-Kotlin cluster member position calculator.
 *
 * Single source of truth for where expanded cluster members are placed.
 * Both the renderer and hit-test consume this — no duplicated coordinate math.
 *
 * No Compose / DrawScope / Canvas dependencies. Host-JVM testable.
 */
object ClusterLayoutCalculator {

    /**
     * Deterministic jitter from task id (mirrors [com.example.domain.graph.GoalGraphBuilder.jitter]).
     * Returns a value in [0, 0.25) radians.
     */
    private fun jitter(id: Int): Float = ((id * 92821) % 1000) / 1000f * 0.25f

    /**
     * Calculate positions for all members of an expanded cluster.
     *
     * @param cluster the expanded cluster
     * @param centerX design-space center x (== viewportRadius)
     * @param centerY design-space center y (== viewportRadius)
     * @return list of member positions, one per member id, in the same order as [VisibleCluster.memberIds]
     */
    fun calculateClusterMembers(
        cluster: VisibleCluster,
        centerX: Float,
        centerY: Float
    ): List<ClusterMemberPosition> {
        val n = cluster.memberIds.size
        return cluster.memberIds.mapIndexed { i, taskId ->
            val angle = if (n == 1) cluster.angle
            else (i.toFloat() / n) * GraphGeometry.TWO_PI + jitter(taskId)
            val (px, py) = GraphGeometry.project(centerX, centerY, cluster.radius, angle)
            ClusterMemberPosition(
                taskId = taskId,
                cx = px,
                cy = py,
                angle = angle
            )
        }
    }
}

/**
 * Position of a single cluster member in design-space coordinates.
 *
 * @param taskId the task id
 * @param cx design-space x (before canvas scaling)
 * @param cy design-space y (before canvas scaling)
 * @param angle deterministic angle in radians
 */
data class ClusterMemberPosition(
    val taskId: Int,
    val cx: Float,
    val cy: Float,
    val angle: Float
)

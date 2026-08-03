package com.example.plugins.goals.ui

import androidx.compose.ui.text.TextLayoutResult
import com.example.domain.graph.GraphGeometry
import com.example.domain.graph.VisibleGraphModel
import com.example.domain.graph.VisibleTask

/**
 * Hit-test logic for the Behavioral Solar System canvas.
 *
 * Pure coordinate calculations — no Compose state mutation, no DrawScope.
 * Consumes [VisibleGraphModel] + tap offset → returns [VisibleHit].
 */
internal fun hitTestVisible(
    model: VisibleGraphModel,
    px: Float,
    py: Float,
    expandedClusterId: Int?,
    scale: Float,
    hiddenHintMeasured: TextLayoutResult?,
    density: Float
): VisibleHit {
    val cx = model.viewportRadius * scale
    val cy = model.viewportRadius * scale
    val tolerance = 12f * scale
    val sunR = SUN_SIZE * scale

    // Sun hit
    if (GraphGeometry.distance(px, py, cx, cy) <= sunR + tolerance) {
        return VisibleHit.Sun
    }

    // Clusters: expand when collapsed; re-tap expanded cluster collapses (toggle in host)
    if (model.clusters.isNotEmpty()) {
        model.clusters.forEach { cluster ->
            val (x, y) = clusterPosition(model, cluster)
            val nx = cx + (x - model.centerX) * scale
            val ny = cy + (y - model.centerY) * scale
            val visualSize = (20f + minOf(cluster.count, 20) * 0.7f).coerceAtMost(34f)
            val hitR = if (cluster.clusterId == expandedClusterId) {
                visualSize * scale * 1.5f + tolerance
            } else {
                visualSize * scale + tolerance
            }
            if (GraphGeometry.distance(px, py, nx, ny) <= hitR) {
                return VisibleHit.Cluster(cluster.clusterId)
            }
        }
    }

    // Tasks: use ClusterLayoutCalculator for expanded cluster members
    val tasksToHit = if (expandedClusterId != null) {
        val expanded = model.clusters.firstOrNull { it.clusterId == expandedClusterId }
        if (expanded != null) {
            val memberPositions = ClusterLayoutCalculator.calculateClusterMembers(
                expanded, model.centerX, model.centerY
            )
            memberPositions.map { pos ->
                VisibleTask(
                    taskId = pos.taskId,
                    attentionScore = 0f,
                    radius = expanded.radius,
                    angle = pos.angle,
                    reasons = emptyList(),
                    isBoulder = false,
                    isOverdue = false,
                    isNearDeadline = false,
                    priority = null,
                    title = ""
                )
            }
        } else {
            emptyList()
        }
    } else {
        model.tasks
    }

    var bestId: Int? = null
    var bestDist = Float.MAX_VALUE
    val visualR = SAT_SIZE * scale * SAT_SIZE_MUL
    tasksToHit.forEach { task ->
        val (x, y) = taskPosition(model, task)
        val nx = cx + (x - model.centerX) * scale
        val ny = cy + (y - model.centerY) * scale
        val d = GraphGeometry.distance(px, py, nx, ny)
        if (d <= visualR + tolerance && d < bestDist) {
            bestDist = d
            bestId = task.taskId
        }
    }
    if (bestId != null) return VisibleHit.Task(bestId)

    // Progressive disclosure chrome: "N more" — positioned below outermost orbit ring
    if (hiddenHintMeasured != null && model.hiddenCount > 0 && expandedClusterId == null) {
        val outermostRing = model.orbitBands.lastOrNull()?.radius ?: model.viewportRadius * 0.85f
        val chipGap = 20f * density
        val chipWidth = hiddenHintMeasured.size.width + 32f * density
        val chipHeight = hiddenHintMeasured.size.height + 16f * density
        val chipY = cy + outermostRing * scale + chipGap
        val chipX = cx - chipWidth / 2f

        val left = chipX
        val right = chipX + chipWidth
        val top = chipY
        val bottom = chipY + chipHeight

        if (px in left..right && py in top..bottom) {
            return VisibleHit.ShowMore
        }
    }

    return VisibleHit.None
}

/** Result types for solar system hit detection. */
internal sealed class VisibleHit {
    data class Cluster(val id: Int) : VisibleHit()
    data class Task(val id: Int) : VisibleHit()
    data object ShowMore : VisibleHit()
    data object Sun : VisibleHit()
    data object None : VisibleHit()
}

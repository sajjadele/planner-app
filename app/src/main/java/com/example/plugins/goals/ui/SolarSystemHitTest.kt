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

        // Generous hit area with padding for reliable touch detection
        val hitPad = 16f * density
        val left = chipX - hitPad
        val right = chipX + chipWidth + hitPad
        val top = chipY - hitPad
        val bottom = chipY + chipHeight + hitPad

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

// ── Zoomed view hit-test ──

/**
 * Hit-test for the "بازگشت" (back) button in zoomed cluster view.
 * Button is centered at bottom of canvas.
 */
internal fun hitTestBackButton(px: Float, py: Float, canvasSize: Float, density: Float): Boolean {
    val btnW = 100f * density
    val btnH = 36f * density
    val btnX = canvasSize / 2f - btnW / 2f
    val btnY = canvasSize - 50f * density
    val hitPad = 12f * density
    return px in (btnX - hitPad)..(btnX + btnW + hitPad) &&
        py in (btnY - hitPad)..(btnY + btnH + hitPad)
}

/**
 * Hit-test for individual members of a zoomed cluster.
 * Returns task ID if hit, null otherwise.
 */
internal fun hitTestZoomedMembers(
    model: VisibleGraphModel,
    clusterId: Int,
    px: Float,
    py: Float,
    scale: Float
): Int? {
    val cluster = model.clusters.firstOrNull { it.clusterId == clusterId } ?: return null
    val memberTasks = model.tasks.filter { it.taskId in cluster.memberIds }
    val activeBands = model.orbitBands.filter { it.band != null }
    val bandCount = activeBands.size.coerceAtLeast(1)
    val cx = model.viewportRadius * scale
    val cy = model.viewportRadius * scale
    val tolerance = 12f * scale
    val visualR = SAT_SIZE * scale * SAT_SIZE_MUL

    var bestId: Int? = null
    var bestDist = Float.MAX_VALUE

    memberTasks.forEachIndexed { index, task ->
        val bandIndex = index % bandCount
        val band = activeBands[bandIndex]
        val angle = (index.toFloat() / memberTasks.size) * GraphGeometry.TWO_PI +
            ((task.taskId * 92821) % 1000) / 1000f * 0.3f
        val pos = GraphGeometry.project(model.centerX, model.centerY, band.radius, angle)
        val nx = cx + (pos.first - model.centerX) * scale
        val ny = cy + (pos.second - model.centerY) * scale
        val d = GraphGeometry.distance(px, py, nx, ny)
        if (d <= visualR + tolerance && d < bestDist) {
            bestDist = d
            bestId = task.taskId
        }
    }
    return bestId
}

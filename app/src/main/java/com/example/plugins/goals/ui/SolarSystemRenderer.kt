package com.example.plugins.goals.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.util.RTL
import com.example.core.util.toEnglishDigits
import com.example.domain.graph.GraphGeometry
import com.example.domain.graph.VisibleCluster
import com.example.domain.graph.VisibleGraphModel
import com.example.domain.graph.VisibleTask
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRed
import kotlin.math.sin

// ── Rendering constants ──

internal const val SAT_SIZE = 16f
internal const val SAT_SIZE_MUL = 1.5f
/** Fixed design-space sun radius (identity object — not progress-scaled). */
internal const val SUN_SIZE = 40f
/** Decorative pulse amplitude: ~98%–102% of fixed sun size (not semantic). */
private const val SUN_PULSE_AMP = 0.02f
/** Soft identity halo scale relative to sun body (not progress-driven). */
private const val SUN_HALO_SCALE = 1.35f

// ── Coordinate helpers (shared with hit-test) ──

internal fun toCanvas(model: VisibleGraphModel, scale: Float, x: Float, y: Float): Offset {
    val cx = model.viewportRadius * scale
    val cy = model.viewportRadius * scale
    return Offset(cx + (x - model.centerX) * scale, cy + (y - model.centerY) * scale)
}

internal fun taskPosition(model: VisibleGraphModel, task: VisibleTask): Pair<Float, Float> =
    GraphGeometry.project(model.centerX, model.centerY, task.radius, task.angle)

internal fun clusterPosition(model: VisibleGraphModel, cluster: VisibleCluster): Pair<Float, Float> =
    GraphGeometry.project(model.centerX, model.centerY, cluster.radius, cluster.angle)

// ── Color utility ──

internal fun Color.lighten(amount: Float): Color =
    copy(red = red + (1f - red) * amount, green = green + (1f - green) * amount, blue = blue + (1f - blue) * amount)

// ── Main entry point ──

/**
 * Draw the complete Behavioral Solar System for a single goal.
 *
 * Delegates to focused sub-renderers for each visual layer.
 */
internal fun DrawScope.drawVisibleSolarSystem(
    model: VisibleGraphModel,
    scale: Float,
    time: Float,
    sunEntrance: Float,
    ringEntrance: Float,
    nodeEntrance: Float,
    expandProgress: Float,
    selectedId: Int?,
    expandedClusterId: Int?,
    titleMeasured: TextLayoutResult,
    clusterCountMap: Map<Int, TextLayoutResult>,
    hiddenHintMeasured: TextLayoutResult?,
    emptyHintMeasured: TextLayoutResult?,
    onSurface: Color,
    onSurfaceVariant: Color,
    primary: Color
) {
    val center = toCanvas(model, scale, model.centerX, model.centerY)
    val sunR = SUN_SIZE * scale

    drawOrbitRings(model, scale, ringEntrance, center, onSurfaceVariant)
    drawSun(center, sunR, sunEntrance, time)
    drawSunTitle(model, center, sunR, titleMeasured, sunEntrance)
    drawClusterLayer(model, scale, nodeEntrance, expandProgress, expandedClusterId, selectedId, clusterCountMap)
    drawTaskLayer(model, scale, time, nodeEntrance, expandedClusterId, selectedId)
    drawEmptyHint(emptyHintMeasured, center, model, nodeEntrance)
    drawShowMoreChip(hiddenHintMeasured, model, expandedClusterId, nodeEntrance, primary)
}

// ── Orbit rings ──

private fun DrawScope.drawOrbitRings(
    model: VisibleGraphModel,
    scale: Float,
    ringEntrance: Float,
    center: Offset,
    onSurfaceVariant: Color
) {
    val ringStroke = Stroke(width = 1.0f.dp.toPx())
    model.orbitBands.forEachIndexed { index, band ->
        val r = band.radius * scale
        val alpha = when (index) {
            0 -> 0.08f
            1 -> 0.06f
            2 -> 0.05f
            else -> 0.04f
        }
        drawCircle(
            color = onSurfaceVariant.copy(alpha = alpha * ringEntrance),
            radius = r,
            center = center,
            style = ringStroke
        )
    }
}

// ── Sun (goal identity) ──

private fun DrawScope.drawSun(center: Offset, sunR: Float, sunEntrance: Float, time: Float) {
    val pulse = 1f + SUN_PULSE_AMP * sin(time * 2 * Math.PI.toFloat())
    val sunBodyR = sunR * pulse
    val haloRadius = sunBodyR * SUN_HALO_SCALE

    // Halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                AccentGold.copy(alpha = 0.35f * sunEntrance),
                AccentGold.copy(alpha = 0.12f * sunEntrance),
                AccentGold.copy(alpha = 0.0f)
            ),
            center = center,
            radius = haloRadius
        ),
        radius = haloRadius,
        center = center
    )
    // Body
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFE08A).copy(alpha = sunEntrance),
                AccentGold.copy(alpha = sunEntrance),
                AccentGold.copy(alpha = 0.85f * sunEntrance)
            ),
            center = center,
            radius = sunBodyR
        ),
        radius = sunBodyR,
        center = center
    )
    // Specular highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.18f * sunEntrance),
        radius = sunBodyR * 0.42f,
        center = center
    )
}

// ── Sun title ──

private fun DrawScope.drawSunTitle(
    model: VisibleGraphModel,
    center: Offset,
    sunR: Float,
    titleMeasured: TextLayoutResult,
    sunEntrance: Float
) {
    val titleGap = 32.dp.toPx()
    val titleY = center.y - sunR - titleGap - titleMeasured.size.height
    drawText(
        textLayoutResult = titleMeasured,
        topLeft = Offset(center.x - titleMeasured.size.width / 2f, titleY),
        alpha = sunEntrance
    )
}

// ── Cluster layer ──

private fun DrawScope.drawClusterLayer(
    model: VisibleGraphModel,
    scale: Float,
    nodeEntrance: Float,
    expandProgress: Float,
    expandedClusterId: Int?,
    selectedId: Int?,
    clusterCountMap: Map<Int, TextLayoutResult>
) {
    if (model.clusters.isEmpty()) return

    if (expandedClusterId == null) {
        // All clusters collapsed — draw normally
        model.clusters.forEach { cluster ->
            drawClusterNode(cluster, model, scale, nodeEntrance, clusterCountMap[cluster.clusterId])
        }
    } else {
        // One cluster expanded — dim others, show expanded members
        val expanded = model.clusters.firstOrNull { it.clusterId == expandedClusterId }
        model.clusters.forEach { cluster ->
            if (cluster.clusterId != expandedClusterId) {
                drawClusterNode(cluster, model, scale, nodeEntrance * 0.35f, clusterCountMap[cluster.clusterId])
            }
        }
        // Expanded cluster members via ClusterLayoutCalculator
        if (expanded != null) {
            val memberPositions = ClusterLayoutCalculator.calculateClusterMembers(
                expanded, model.centerX, model.centerY
            )
            memberPositions.forEach { pos ->
                val memberTask = VisibleTask(
                    taskId = pos.taskId,
                    attentionScore = 0f,
                    radius = expanded.radius,
                    angle = pos.angle,
                    reasons = emptyList(),
                    isBoulder = false,
                    isOverdue = false,
                    isNearDeadline = false,
                    priority = null,
                    title = "task-${pos.taskId}"
                )
                drawSatellite(memberTask, model, scale, time = 0f, entrance = nodeEntrance * expandProgress, selectedId = selectedId)
            }
        }
    }
}

// ── Task layer ──

private fun DrawScope.drawTaskLayer(
    model: VisibleGraphModel,
    scale: Float,
    time: Float,
    nodeEntrance: Float,
    expandedClusterId: Int?,
    selectedId: Int?
) {
    if (expandedClusterId != null) return // tasks shown via cluster expansion
    model.tasks.forEach { task ->
        drawSatellite(task, model, scale, time, nodeEntrance, selectedId)
    }
}

// ── Empty hint ──

private fun DrawScope.drawEmptyHint(
    emptyHintMeasured: TextLayoutResult?,
    center: Offset,
    model: VisibleGraphModel,
    nodeEntrance: Float
) {
    if (emptyHintMeasured == null) return
    val scale = size.minDimension / (model.viewportRadius * 2f)
    // Position below the outermost orbit ring with comfortable padding
    val outerRingBottom = center.y + model.viewportRadius * 0.85f * scale
    val chipGap = 24.dp.toPx()
    val chipY = outerRingBottom + chipGap
    val chipW = emptyHintMeasured.size.width + 32.dp.toPx()
    val chipH = emptyHintMeasured.size.height + 16.dp.toPx()
    val chipX = center.x - chipW / 2f

    // Subtle background chip
    drawRoundRect(
        color = Color(0xFF3A3A3E).copy(alpha = 0.85f * nodeEntrance),
        topLeft = Offset(chipX, chipY),
        size = Size(chipW, chipH),
        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
    )
    // Text
    drawText(
        textLayoutResult = emptyHintMeasured,
        topLeft = Offset(
            chipX + 16.dp.toPx(),
            chipY + chipH / 2f - emptyHintMeasured.size.height / 2f
        ),
        alpha = nodeEntrance
    )
}

// ── Show-more chip ──

private fun DrawScope.drawShowMoreChip(
    hiddenHintMeasured: TextLayoutResult?,
    model: VisibleGraphModel,
    expandedClusterId: Int?,
    nodeEntrance: Float,
    primary: Color
) {
    if (hiddenHintMeasured == null || expandedClusterId != null) return
    if (model.hiddenCount <= 0) return

    val scale = size.minDimension / (model.viewportRadius * 2f)
    // Position below outermost orbit ring
    val outermostRing = model.orbitBands.lastOrNull()?.radius ?: model.viewportRadius * 0.85f
    val chipGap = 20.dp.toPx()
    val chipY = center.y + outermostRing * scale + chipGap
    val chipWidth = hiddenHintMeasured.size.width + 32.dp.toPx()
    val chipHeight = hiddenHintMeasured.size.height + 16.dp.toPx()
    val chipX = center.x - chipWidth / 2f

    drawRoundRect(
        color = primary.copy(alpha = 0.12f * nodeEntrance),
        topLeft = Offset(chipX, chipY),
        size = Size(chipWidth, chipHeight),
        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
    )
    drawText(
        textLayoutResult = hiddenHintMeasured,
        topLeft = Offset(
            chipX + 16.dp.toPx(),
            chipY + chipHeight / 2f - hiddenHintMeasured.size.height / 2f
        ),
        alpha = nodeEntrance
    )
}

// ── Individual satellite ──

private fun DrawScope.drawSatellite(
    task: VisibleTask,
    model: VisibleGraphModel,
    scale: Float,
    time: Float,
    entrance: Float,
    selectedId: Int?
) {
    val (px, py) = taskPosition(model, task)
    val wobble = if (task.isBoulder) {
        val phase = (task.taskId * 92821 % 1000) / 1000f * 2 * Math.PI.toFloat()
        2.dp.toPx() * sin(time * 2 * Math.PI.toFloat() * 0.5f + phase)
    } else 0f
    val base = toCanvas(model, scale, px, py)
    val pos = base + Offset(wobble, wobble * 0.6f)

    val isSelected = task.taskId == selectedId

    // Color by attention score (proximity to sun)
    val baseColor = when {
        task.attentionScore >= 0.7f -> Color(0xFFDC2626)  // red — high attention
        task.attentionScore >= 0.4f -> Color(0xFFF59E0B)  // amber — medium
        else -> AccentPurple                               // purple — low
    }

    val baseAlpha = entrance
    val visualR = SAT_SIZE * scale * SAT_SIZE_MUL
    val r = visualR

    // Shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.12f * entrance),
        radius = r * 1.15f,
        center = pos + Offset(x = 0f, y = 1.dp.toPx())
    )

    // Overdue badge (independent of base color)
    if (task.isOverdue) {
        val overdueDot = Color(0xFFDC2626).copy(alpha = 0.7f * entrance)
        drawCircle(color = overdueDot, radius = r * 0.35f, center = pos + Offset(r * 0.7f, -r * 0.7f))
    }

    // Boulder ring
    if (task.isBoulder) {
        drawCircle(
            color = AccentRed.copy(alpha = 0.18f * entrance),
            radius = r * 0.85f,
            center = pos,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }

    // Near-deadline ring
    if (task.isNearDeadline) {
        drawCircle(
            color = Color(0xFFF59E0B).copy(alpha = 0.35f * entrance),
            radius = r * 1.15f,
            center = pos,
            style = Stroke(width = 1.dp.toPx())
        )
    }

    // Planet body
    val core = baseColor.lighten(0.25f).copy(alpha = baseAlpha)
    val edge = baseColor.copy(alpha = baseAlpha)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(core, baseColor.copy(alpha = baseAlpha), edge),
            center = pos,
            radius = r
        ),
        radius = if (isSelected) r * 1.2f else r,
        center = pos
    )

    // Selection ring
    if (isSelected) {
        drawCircle(
            color = AccentPurple,
            radius = r * 1.2f,
            center = pos,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// ── Cluster node ──

private fun DrawScope.drawClusterNode(
    cluster: VisibleCluster,
    model: VisibleGraphModel,
    scale: Float,
    alpha: Float,
    countMeasured: TextLayoutResult?
) {
    val (px, py) = clusterPosition(model, cluster)
    val pos = toCanvas(model, scale, px, py)
    val color = AccentPurple
    val visualSize = (20f + minOf(cluster.count, 20) * 0.7f).coerceAtMost(34f)
    val r = visualSize * scale

    drawCircle(color = color.copy(alpha = 0.12f * alpha), radius = r * 1.5f, center = pos)
    drawCircle(color = color.copy(alpha = alpha), radius = r, center = pos)
    drawCircle(color = Color.White.copy(alpha = 0.10f * alpha), radius = r * 0.6f, center = pos)

    val measured = countMeasured ?: return
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(pos.x - measured.size.width / 2f, pos.y - measured.size.height / 2f),
        alpha = alpha
    )
}

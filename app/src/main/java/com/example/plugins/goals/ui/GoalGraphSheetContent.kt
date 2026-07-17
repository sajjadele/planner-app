package com.example.plugins.goals.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.core.util.RTL
import kotlin.math.sin
import com.example.domain.graph.ColorRole
import com.example.domain.graph.GoalGraph
import com.example.domain.graph.GoalGraphNode
import com.example.domain.graph.GraphGeometry
import com.example.domain.graph.NodeKind
import com.example.ui.theme.*

/**
 * Bottom-sheet body rendering the Behavioral Solar System for a single goal.
 *
 * Pure rendering of the precomputed [GoalGraph]: the sun (goal) with a Ring Tide halo scaled by
 * progress, three faint priority-lane orbit rings, gravity edges, and task satellites. Tapping a
 * satellite highlights it and shows a Persian label. "Boulder" nodes wobble deterministically.
 */
@Composable
fun GoalGraphSheetContent(graph: GoalGraph) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var selectedId by remember { mutableStateOf<Int?>(null) }

    val infinite = rememberInfiniteTransition(label = "solar-system")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(4000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "clock"
    )

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "منظومه رفتاری",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "هر تسک یک سیاره است؛ فاصله و اندازه نشان‌دهنده اولویت و وضعیت آن است.",
            fontSize = 12.sp,
            color = onSurfaceVariant,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(graph) {
                        detectTapGestures { offset ->
                            selectedId = hitTest(graph, offset.x, offset.y)
                        }
                    }
            ) {
                val scale = size.minDimension / (graph.viewportRadius * 2f)
                drawSolarSystem(
                    graph = graph,
                    scale = scale,
                    time = t,
                    selectedId = selectedId,
                    textMeasurer = textMeasurer,
                    density = density,
                    onSurface = onSurface,
                    onSurfaceVariant = onSurfaceVariant
                )
            }
        }
    }
}

/** Map graph design-space coords (centered at viewportRadius) into canvas pixels. */
private fun toCanvas(graph: GoalGraph, scale: Float, x: Float, y: Float): Offset {
    val cx = graph.viewportRadius * scale
    val cy = graph.viewportRadius * scale
    return Offset(cx + (x - graph.centerX) * scale, cy + (y - graph.centerY) * scale)
}

private fun DrawScope.drawSolarSystem(
    graph: GoalGraph,
    scale: Float,
    time: Float,
    selectedId: Int?,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    density: androidx.compose.ui.unit.Density,
    onSurface: Color,
    onSurfaceVariant: Color
) {
    val sun = graph.nodes.first { it.kind == NodeKind.GOAL }
    val center = toCanvas(graph, scale, sun.cx, sun.cy)

    // ── Priority lane orbit rings ──
    listOf(0.34f, 0.58f, 0.82f).forEach { frac ->
        val r = graph.viewportRadius * frac * scale
        drawCircle(
            color = onSurfaceVariant.copy(alpha = 0.12f),
            radius = r,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }

    // ── Gravity edges (goal → task) ──
    graph.edges.forEach { edge ->
        val node = graph.nodes.firstOrNull { it.id == edge.toId } ?: return@forEach
        val pos = toCanvas(graph, scale, node.cx, node.cy)
        val highlighted = node.id == selectedId
        drawLine(
            color = if (highlighted) AccentPurple else onSurfaceVariant,
            start = center,
            end = pos,
            alpha = if (highlighted) 0.9f else edge.alpha * 0.35f,
            strokeWidth = if (highlighted) 2.dp.toPx() else 1.dp.toPx()
        )
    }

    // ── Ring Tide: sun halo intensity scales with goal progress ──
    val tide = (graph.goalProgressOverall / 100f).coerceIn(0f, 1f)
    val pulse = 1f + 0.04f * sin(time * 2 * Math.PI.toFloat())
    val haloRadius = sun.size * scale * (1.6f + tide * 1.4f) * pulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                AccentPurple.copy(alpha = 0.35f + tide * 0.35f),
                AccentPurple.copy(alpha = 0.0f)
            ),
            center = center,
            radius = haloRadius
        ),
        radius = haloRadius,
        center = center
    )

    // ── Sun (goal node) ──
    drawCircle(color = AccentPurple, radius = sun.size * scale * pulse, center = center)
    drawCircle(
        color = Color.White.copy(alpha = 0.18f),
        radius = sun.size * scale * 0.6f * pulse,
        center = center
    )

    // ── Satellites (task nodes) ──
    graph.nodes.filter { it.kind == NodeKind.TASK }.forEach { node ->
        val wobble = if (node.isBoulder) {
            val phase = (node.id * 92821 % 1000) / 1000f * 2 * Math.PI.toFloat()
            4.dp.toPx() * sin(time * 2 * Math.PI.toFloat() * 2f + phase)
        } else 0f
        val base = toCanvas(graph, scale, node.cx, node.cy)
        val pos = base + Offset(wobble, wobble * 0.6f)
        val color = colorForRole(node.colorRole, onSurfaceVariant)
        val selected = node.id == selectedId

        if (node.isBoulder) {
            drawCircle(
                color = AccentRed.copy(alpha = 0.25f),
                radius = node.size * scale * 1.8f,
                center = pos
            )
        }
        drawCircle(
            color = color.copy(alpha = node.alpha),
            radius = if (selected) node.size * scale * 1.25f else node.size * scale,
            center = pos
        )
        if (selected) {
            drawCircle(
                color = AccentPurple,
                radius = node.size * scale * 1.25f,
                center = pos,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        if (selected) {
            val label = "${RTL}${node.label}"
            val measured = textMeasurer.measure(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = onSurface,
                    textAlign = TextAlign.Center
                )
            )
            val labelY = (pos.y - node.size * scale - 22.dp.toPx()).coerceAtLeast(4.dp.toPx())
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(pos.x - measured.size.width / 2f, labelY)
            )
        }
    }
}

/** Find the task node under a tap (canvas px), else null. */
private fun hitTest(graph: GoalGraph, px: Float, py: Float): Int? {
    val cx = graph.viewportRadius
    val cy = graph.viewportRadius
    var best: Int? = null
    var bestDist = Float.MAX_VALUE
    graph.nodes.filter { it.kind == NodeKind.TASK }.forEach { node ->
        val nx = cx + (node.cx - graph.centerX)
        val ny = cy + (node.cy - graph.centerY)
        val d = GraphGeometry.distance(px, py, nx, ny)
        if (d <= node.size + 12f && d < bestDist) {
            bestDist = d
            best = node.id
        }
    }
    return best
}

private fun colorForRole(role: ColorRole, onSurfaceVariant: Color): Color = when (role) {
    ColorRole.GOAL -> AccentPurple
    ColorRole.HIGH -> AccentRed
    ColorRole.MEDIUM -> AccentFire
    ColorRole.LOW -> AccentGreen
    ColorRole.BOULDER -> AccentRed
    ColorRole.COMPLETED -> onSurfaceVariant
}

package com.example.plugins.goals.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.core.util.RTL
import kotlin.math.sin
import com.example.domain.graph.ColorRole
import com.example.domain.graph.ClusterType
import com.example.domain.graph.GoalGraph
import com.example.domain.graph.GoalGraphNode
import com.example.domain.graph.GraphGeometry
import com.example.domain.graph.GraphMode
import com.example.domain.graph.NodeKind
import com.example.domain.graph.TaskClusterNode
import com.example.ui.theme.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info

/**
 * Bottom-sheet body rendering the Behavioral Solar System for a single goal.
 *
 * Pure rendering of the precomputed [GoalGraph]: the sun (goal) with a progress ring + Ring Tide
 * halo scaled by progress, three priority-lane orbit rings (weighted by visual importance),
 * gravity edges, and task satellites. Tapping a satellite highlights it and shows a Persian label.
 * "Boulder" nodes wobble deterministically. All values come from the domain/ViewModel — Compose
 * only displays.
 */
@Composable
fun GoalGraphSheetContent(graph: GoalGraph) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var selectedId by remember { mutableStateOf<Int?>(null) }
    var expandedClusterId by remember { mutableStateOf<Int?>(null) }
    var showLegend by remember { mutableStateOf(false) }

    // ── Phase 6.4 motion: staged entrance (sun → rings → satellites) + cluster expand ──
    // Staged one-shot entrance so the system "assembles" calmly rather than popping in.
    val sunEntrance = remember { Animatable(0f) }
    val ringEntrance = remember { Animatable(0f) }
    val nodeEntrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { sunEntrance.animateTo(1f, tween(280, easing = FastOutSlowInEasing)) }
        launch {
            delay(140)
            ringEntrance.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        }
        launch {
            delay(300)
            nodeEntrance.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
    }

    // Cluster expand/collapse progress (0 = overview, 1 = expanded). Calm fade+scale, no spring.
    val expandProgress = remember { Animatable(0f) }
    LaunchedEffect(expandedClusterId) {
        val target = if (expandedClusterId != null) 1f else 0f
        expandProgress.animateTo(target, tween(320, easing = FastOutSlowInEasing))
    }

    // Subtle, non-physics ambient pulse (Ring Tide breathing + Boulder wobble only).
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "منظومه رفتاری",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showLegend = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "راهنمای منظومه",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "وضعیت هدف در یک نگاه",
            fontSize = 12.sp,
            color = onSurfaceVariant,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (showLegend) {
            AlertDialog(
                onDismissRequest = { showLegend = false },
                confirmButton = {
                    TextButton(onClick = { showLegend = false }) {
                        Text("بستن")
                    }
                },
                title = { Text("راهنمای منظومه رفتاری") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendRow(
                            glyph = "☀",
                            title = "هدف",
                            body = "مرکز هدف و میزان پیشرفت"
                        )
                        LegendRow(
                            glyph = "🔥",
                            title = "High Priority",
                            body = "تسک‌های مهم‌تر نزدیک‌تر به هدف قرار می‌گیرند."
                        )
                        LegendRow(
                            glyph = "●",
                            title = "Active Task",
                            body = "تسک فعال"
                        )
                        LegendRow(
                            glyph = "○",
                            title = "Completed Task",
                            body = "مسیرهای طی شده"
                        )
                        LegendRow(
                            glyph = "⏳",
                            title = "نمایش زمانی",
                            body = "نمایش اهمیت زمانی در نسخه آینده اضافه خواهد شد",
                            enabled = false
                        )
                    }
                }
            )
        }

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
                        val hit = hitTest(graph, offset.x, offset.y, expandedClusterId)
                        when {
                            hit is HitResult.Cluster -> expandedClusterId = hit.id
                            hit is HitResult.Task -> {
                                // Tapping the sun (goal) collapses an expanded cluster.
                                if (hit.id == graph.nodes.first { it.kind == NodeKind.GOAL }.id) {
                                    expandedClusterId = null
                                }
                                selectedId = hit.id
                            }
                            hit is HitResult.None -> {
                                expandedClusterId = null
                                selectedId = null
                            }
                        }
                    }
                }
            ) {
                val scale = size.minDimension / (graph.viewportRadius * 2f)
                drawSolarSystem(
                    graph = graph,
                    scale = scale,
                    time = t,
                    sunEntrance = sunEntrance.value,
                    ringEntrance = ringEntrance.value,
                    nodeEntrance = nodeEntrance.value,
                    expandProgress = expandProgress.value,
                    selectedId = selectedId,
                    expandedClusterId = expandedClusterId,
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
    sunEntrance: Float,
    ringEntrance: Float,
    nodeEntrance: Float,
    expandProgress: Float,
    selectedId: Int?,
    expandedClusterId: Int?,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    density: androidx.compose.ui.unit.Density,
    onSurface: Color,
    onSurfaceVariant: Color
) {
    val sun = graph.nodes.first { it.kind == NodeKind.GOAL }
    val center = toCanvas(graph, scale, sun.cx, sun.cy)
    val sunR = sun.size * scale

    // ── Priority lane orbit rings (weighted: HIGH strongest → LOW weakest) ──
    val lanes = listOf(
        Triple(0.34f, 0.22f, 1.5f), // HIGH: brighter, thicker
        Triple(0.58f, 0.15f, 1f),   // MEDIUM
        Triple(0.82f, 0.08f, 1f)    // LOW: fainter
    )
    lanes.forEach { (frac, alpha, w) ->
        val r = graph.viewportRadius * frac * scale
        drawCircle(
            color = onSurfaceVariant.copy(alpha = alpha * ringEntrance),
            radius = r,
            center = center,
            style = Stroke(width = w.dp.toPx())
        )
    }

    // ── Gravity edges removed (Phase 6.4+): tasks float on their orbits with no connecting
    //    lines, per design review. `graph.edges` is no longer drawn. ──

    // ── Ring Tide: sun halo intensity scales with goal progress (breathing pulse) ──
    val tide = (graph.goalProgressOverall / 100f).coerceIn(0f, 1f)
    val pulse = 1f + 0.05f * sin(time * 2 * Math.PI.toFloat())
    val haloRadius = sunR * (1.6f + tide * 1.4f) * pulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                AccentGold.copy(alpha = (0.30f + tide * 0.40f) * sunEntrance),
                AccentGold.copy(alpha = 0.0f)
            ),
            center = center,
            radius = haloRadius
        ),
        radius = haloRadius,
        center = center
    )

    // ── Sun (goal node) — warm gold ──
    drawCircle(color = AccentGold.copy(alpha = sunEntrance), radius = sunR * pulse, center = center)
    drawCircle(
        color = Color.White.copy(alpha = 0.18f * sunEntrance),
        radius = sunR * 0.6f * pulse,
        center = center
    )

    // ── Progress ring around the sun (drawn from the domain's overall value) ──
    val progress = graph.goalProgressOverall.coerceIn(0f, 100f)
    val ringR = sunR * 1.35f * pulse
    val sweep = (progress / 100f) * 360f
    // track
    drawArc(
        color = AccentCyan.copy(alpha = 0.18f * sunEntrance),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(center.x - ringR, center.y - ringR),
        size = Size(ringR * 2, ringR * 2),
        style = Stroke(width = 4.dp.toPx())
    )
    // filled progress arc
    if (sweep > 0f) {
        drawArc(
            color = AccentCyan.copy(alpha = sunEntrance),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(center.x - ringR, center.y - ringR),
            size = Size(ringR * 2, ringR * 2),
            style = Stroke(width = 4.dp.toPx())
        )
    }

    // ── Sun labels: goal title ABOVE the sun, progress % centered INSIDE the sun ──
    val pct = "${progress.toInt()}%"
    val title = "${RTL}${sun.label}"
    val titleMeasured = textMeasurer.measure(
        text = title,
        style = TextStyle(
            fontSize = 12.sp,
            color = onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    )
    val pctMeasured = textMeasurer.measure(
        text = pct,
        style = TextStyle(
            fontSize = 13.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    )
    // Title sits fully above the sun disc (clamped so it can't overlap the halo/disc).
    val titleY = (center.y - haloRadius - titleMeasured.size.height - 6.dp.toPx())
        .coerceAtLeast(2.dp.toPx())
    drawText(
        textLayoutResult = titleMeasured,
        topLeft = Offset(center.x - titleMeasured.size.width / 2f, titleY),
        alpha = sunEntrance
    )
    // Percentage stays cleanly centered inside the sun.
    drawText(
        textLayoutResult = pctMeasured,
        topLeft = Offset(center.x - pctMeasured.size.width / 2f, center.y - pctMeasured.size.height / 2f),
        alpha = sunEntrance
    )

    // ── CLUSTER mode: overview clusters (or expanded member satellites) ──
    if (graph.mode == GraphMode.CLUSTER) {
        if (expandedClusterId == null) {
            // Overview: draw the four summary clusters with their counts.
            graph.clusters.forEach { cluster ->
                drawCluster(cluster, graph, scale, nodeEntrance, onSurfaceVariant, textMeasurer)
            }
        } else {
            // Detail exploration: expand the tapped cluster's members as individual satellites;
            // other clusters are dimmed to keep focus. Expansion happens in-view — no navigation.
            val expanded = graph.clusters.firstOrNull { it.id == expandedClusterId }
            graph.clusters.forEach { cluster ->
                if (cluster.id != expandedClusterId) {
                    drawCluster(cluster, graph, scale, nodeEntrance * 0.35f, onSurfaceVariant, textMeasurer)
                }
            }
            val memberIds = expanded?.memberIds?.toSet() ?: emptySet()
            graph.nodes.filter { it.kind == NodeKind.TASK && it.id in memberIds }.forEach { node ->
                drawSatellite(
                    node = node, graph = graph, scale = scale, time = time,
                    entrance = nodeEntrance * expandProgress, selectedId = selectedId,
                    onSurface = onSurface, onSurfaceVariant = onSurfaceVariant,
                    textMeasurer = textMeasurer
                )
            }
        }
    } else {
        // ── INDIVIDUAL mode: every task as its own satellite ──
        graph.nodes.filter { it.kind == NodeKind.TASK }.forEach { node ->
            drawSatellite(
                node = node, graph = graph, scale = scale, time = time,
                entrance = nodeEntrance, selectedId = selectedId,
                onSurface = onSurface, onSurfaceVariant = onSurfaceVariant,
                textMeasurer = textMeasurer
            )
        }
    }
}

/** Draw a single task satellite (shared by INDIVIDUAL mode and cluster expansion). */
private fun DrawScope.drawSatellite(
    node: GoalGraphNode,
    graph: GoalGraph,
    scale: Float,
    time: Float,
    entrance: Float,
    selectedId: Int?,
    onSurface: Color,
    onSurfaceVariant: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val wobble = if (node.isBoulder) {
        val phase = (node.id * 92821 % 1000) / 1000f * 2 * Math.PI.toFloat()
        4.dp.toPx() * sin(time * 2 * Math.PI.toFloat() * 2f + phase)
    } else 0f
    val base = toCanvas(graph, scale, node.cx, node.cy)
    val pos = base + Offset(wobble, wobble * 0.6f)
    val color = colorForRole(node.colorRole, onSurfaceVariant)
    val selected = node.id == selectedId
    val baseAlpha = node.alpha * entrance

    // ── Gentle breathing shimmer (Phase 6.4): subtle scale+alpha, deterministic per node,
    //    NO positional/orbital movement. Completed memory nodes stay calmer. ──
    val shimmerPhase = (node.id * 92821 % 1000) / 1000f * 2 * Math.PI.toFloat()
    val shimmerAmp = if (node.isCompleted) 0.015f else 0.03f
    val shimmer = 1f + shimmerAmp * sin(time * 2 * Math.PI.toFloat() + shimmerPhase)
    val alphaShimmer = if (node.isCompleted) 0f else 0.06f * sin(time * 2 * Math.PI.toFloat() + shimmerPhase)

    // Priority presence: HIGH gets a soft outer glow for stronger visual weight.
    if (node.colorRole == ColorRole.HIGH && !node.isCompleted) {
        drawCircle(
            color = color.copy(alpha = 0.18f * entrance),
            radius = node.size * scale * 1.9f * shimmer,
            center = pos
        )
    }
    if (node.isBoulder) {
        drawCircle(
            color = AccentRed.copy(alpha = 0.25f * entrance),
            radius = node.size * scale * 1.8f,
            center = pos
        )
    }
    drawCircle(
        color = color.copy(alpha = (baseAlpha + alphaShimmer).coerceIn(0f, 1f)),
        radius = (if (selected) node.size * scale * 1.25f else node.size * scale) * shimmer,
        center = pos
    )
    if (selected) {
        drawCircle(
            color = AccentPurple,
            radius = node.size * scale * 1.25f * shimmer,
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

/** Draw a summary cluster node with its task count label. */
private fun DrawScope.drawCluster(
    cluster: TaskClusterNode,
    graph: GoalGraph,
    scale: Float,
    alpha: Float,
    onSurfaceVariant: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val pos = toCanvas(graph, scale, cluster.cx, cluster.cy)
    val color = colorForCluster(cluster.clusterType, onSurfaceVariant)
    val r = cluster.visualSize * scale
    drawCircle(color = color.copy(alpha = 0.18f * alpha), radius = r * 1.5f, center = pos)
    drawCircle(color = color.copy(alpha = alpha), radius = r, center = pos)
    drawCircle(
        color = Color.White.copy(alpha = 0.15f * alpha),
        radius = r * 0.6f,
        center = pos
    )
    val count = cluster.taskCount.toString()
    val measured = textMeasurer.measure(
        text = count,
        style = TextStyle(
            fontSize = (if (cluster.taskCount >= 10) 13.sp else 15.sp),
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(pos.x - measured.size.width / 2f, pos.y - measured.size.height / 2f),
        alpha = alpha
    )
}

/** Map a cluster type to its UI color (keeps domain ColorRole-free). */
private fun colorForCluster(type: ClusterType, onSurfaceVariant: Color): Color = when (type) {
    ClusterType.ACTIVE_HIGH -> AccentRed
    ClusterType.ACTIVE_MEDIUM -> AccentFire
    ClusterType.ACTIVE_LOW -> AccentGreen
    ClusterType.COMPLETED -> onSurfaceVariant
}

/** Result of a tap hit-test: which element (cluster / task / nothing) was tapped. */
private sealed class HitResult {
    data class Cluster(val id: Int) : HitResult()
    data class Task(val id: Int) : HitResult()
    object None : HitResult()
}

/**
 * Hit-test a tap (canvas px). In CLUSTER mode, clusters take priority; when a cluster is expanded,
 * its member task satellites are tappable. In INDIVIDUAL mode, tasks are tappable directly.
 */
private fun hitTest(graph: GoalGraph, px: Float, py: Float, expandedClusterId: Int?): HitResult {
    val cx = graph.viewportRadius
    val cy = graph.viewportRadius

    if (graph.mode == GraphMode.CLUSTER) {
        if (expandedClusterId == null) {
            graph.clusters.forEach { cluster ->
                val nx = cx + (cluster.cx - graph.centerX)
                val ny = cy + (cluster.cy - graph.centerY)
                if (GraphGeometry.distance(px, py, nx, ny) <= cluster.visualSize + 12f) {
                    return HitResult.Cluster(cluster.id)
                }
            }
            return HitResult.None
        }
        val expanded = graph.clusters.firstOrNull { it.id == expandedClusterId } ?: return HitResult.None
        val memberIds = expanded.memberIds.toSet()
        graph.nodes.filter { it.kind == NodeKind.TASK && it.id in memberIds }.forEach { node ->
            val nx = cx + (node.cx - graph.centerX)
            val ny = cy + (node.cy - graph.centerY)
            if (GraphGeometry.distance(px, py, nx, ny) <= node.size + 12f) {
                return HitResult.Task(node.id)
            }
        }
        return HitResult.None
    }

    // INDIVIDUAL mode: tasks only.
    var bestId: Int? = null
    var bestDist = Float.MAX_VALUE
    graph.nodes.filter { it.kind == NodeKind.TASK }.forEach { node ->
        val nx = cx + (node.cx - graph.centerX)
        val ny = cy + (node.cy - graph.centerY)
        val d = GraphGeometry.distance(px, py, nx, ny)
        if (d <= node.size + 12f && d < bestDist) {
            bestDist = d
            bestId = node.id
        }
    }
    return if (bestId != null) HitResult.Task(bestId) else HitResult.None
}

private fun colorForRole(role: ColorRole, onSurfaceVariant: Color): Color = when (role) {
    ColorRole.GOAL -> AccentGold
    ColorRole.HIGH -> AccentRed
    ColorRole.MEDIUM -> AccentFire
    ColorRole.LOW -> AccentGreen
    ColorRole.BOULDER -> AccentRed
    ColorRole.COMPLETED -> onSurfaceVariant
}

/**
 * Single row in the Help/Legend dialog explaining the Behavioral Solar System metaphor.
 * [enabled] greys out deferred concepts (e.g. temporal/deadline view, arriving in a future version).
 */
@Composable
private fun LegendRow(
    glyph: String,
    title: String,
    body: String,
    enabled: Boolean = true
) {
    val color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = glyph,
            fontSize = 16.sp,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp)
        )
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = body,
                fontSize = 12.sp,
                color = color.copy(alpha = if (enabled) 0.8f else 0.5f),
                lineHeight = 17.sp
            )
        }
    }
}

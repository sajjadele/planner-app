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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.core.util.RTL
import com.example.core.util.toEnglishDigits
import kotlin.math.sin
import kotlin.math.roundToInt
import com.example.domain.graph.ColorRole
import com.example.domain.graph.ClusterType
import com.example.domain.graph.GoalGraph
import com.example.domain.graph.GoalGraphNode
import com.example.domain.graph.GraphGeometry
import com.example.domain.graph.GraphDensityMode
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
fun GoalGraphSheetContent(
    graph: GoalGraph,
    autoShowEducation: Boolean = false,
    onEducationDismissed: () -> Unit = {},
    onTaskTap: (Int) -> Unit = {}
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var selectedId by remember { mutableStateOf<Int?>(null) }
    var expandedClusterId by remember { mutableStateOf<Int?>(null) }
    var showLegend by remember { mutableStateOf(false) }
    // Phase 5.5: on first-ever open the legend auto-shows as the education surface.
    val wasEducation = remember { autoShowEducation }
    LaunchedEffect(Unit) {
        if (autoShowEducation) showLegend = true
    }

    // ── Phase 6.4 motion: staged entrance (sun → rings → satellites) + cluster expand ──
    // Staged one-shot entrance so the system "assembles" calmly rather than popping in.
    val sunEntrance = remember { Animatable(0f) }
    val ringEntrance = remember { Animatable(0f) }
    val nodeEntrance = remember { Animatable(0f) }

    // Phase 5.5: the ambient breathing pulse starts ONLY after the staged entrance completes,
    // so the sheet slide-in + assemble animation do not overlap the 60fps breathing. `entranceDone`
    // is set true only once the final entrance animation fully finishes (see below), so this gate
    // holds on cold starts too — not merely when the view is already warm.
    var entranceDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { sunEntrance.animateTo(1f, tween(280, easing = FastOutSlowInEasing)) }
        launch {
            delay(140)
            ringEntrance.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        }
        // The node entrance is the last (and longest) stage; await it directly so `entranceDone`
        // flips exactly when the staged entrance fully completes — not on a fixed heuristic delay.
        // This guarantees the breathing pulse never starts before the assemble animation ends,
        // on BOTH cold and warm starts.
        delay(300)
        nodeEntrance.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        entranceDone = true
    }

    // Cluster expand/collapse progress (0 = overview, 1 = expanded). Calm fade+scale, no spring.
    val expandProgress = remember { Animatable(0f) }
    LaunchedEffect(expandedClusterId) {
        val target = if (expandedClusterId != null) 1f else 0f
        expandProgress.animateTo(target, tween(320, easing = FastOutSlowInEasing))
    }

    // Phase 5.5: the ambient breathing pulse starts ONLY after the staged entrance completes,
    // so the sheet slide-in + assemble animation do not overlap the 60fps breathing. The pulse is
    // kept (no animation removed); before entranceDone the canvas renders statically (t = 0).
    val t = if (entranceDone) {
        val infinite = rememberInfiniteTransition(label = "solar-system")
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(4000, easing = LinearEasing),
                RepeatMode.Restart
            ),
            label = "clock"
        ).value
    } else {
        0f
    }

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // ── Phase 5.4 (ADR-0009) render optimization: cache graph-derived values that never change
    //    between frames. The 60fps breathing redraw must NOT re-scan the node list or re-run
    //    TextMeasurer every frame — so the (stable) lookups + text layouts are computed once here
    //    in the composable scope and passed into the Canvas draw. Only the pulse (`t`) varies. ──
    val sunNode = remember(graph) { graph.nodes.first { it.kind == NodeKind.GOAL } }
    val taskNodes = remember(graph) { graph.nodes.filter { it.kind == NodeKind.TASK } }

    val progressPct = (graph.goalProgressOverall).coerceIn(0f, 100f).toInt()
    val titleMeasured = remember(graph) {
        textMeasurer.measure(
            text = "${RTL}${sunNode.label}",
            style = TextStyle(
                fontSize = 13.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            constraints = Constraints(maxWidth = (200f * density.density).roundToInt())
        )
    }
    val pctMeasured = remember(graph) {
        textMeasurer.measure(
            text = "$progressPct%",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        )
    }
    // Cluster count labels are static per graph → measured once, keyed by taskCount.
    val clusterCountMap = remember(graph) {
        graph.clusters.associate { cluster ->
            cluster.id to textMeasurer.measure(
                text = cluster.taskCount.toString(),
                style = TextStyle(
                    fontSize = (if (cluster.taskCount >= 10) 13.sp else 15.sp),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

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
                onDismissRequest = {
                    showLegend = false
                    if (wasEducation) onEducationDismissed()
                },
                confirmButton = {
                    TextButton(onClick = {
                        showLegend = false
                        if (wasEducation) onEducationDismissed()
                    }) {
                        Text("بستن")
                    }
                },
                title = { Text("راهنمای منظومه رفتاری") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendRow(
                            glyph = "☀",
                            title = "هدف",
                            body = "مرکز منظومه است و میزان پیشرفت آن را نشان می‌دهد."
                        )
                        LegendRow(
                            glyph = "🔥",
                            title = "اولویت بالا",
                            body = "تسک‌های با اولویت بالاتر نزدیک‌تر به هدف قرار می‌گیرند."
                        )
                        LegendRow(
                            glyph = "⏰",
                            title = "موعد گذشته",
                            body = "تسک‌هایی که مهلت آن‌ها گذشته و هنوز انجام نشده‌اند با حلقه قرمز مشخص می‌شوند."
                        )
                        LegendRow(
                            glyph = "●",
                            title = "تسک فعال",
                            body = "هر نقطه نشان‌دهنده یک کار این هدف است."
                        )
                        LegendRow(
                            glyph = "○",
                            title = "انجام شده",
                            body = "تسک‌های انجام شده به عنوان سابقه نمایش داده می‌شوند."
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
                        val hitScale = size.width.toFloat() / (graph.viewportRadius * 2f)
                        val hit = hitTest(graph, offset.x, offset.y, expandedClusterId, hitScale)
                        when {
                            hit is HitResult.Cluster -> expandedClusterId = hit.id
                            hit is HitResult.Task -> {
                                // Resolve the tapped node by BOTH id and kind: goal and task id
                                // sequences are independent, so a task may share the goal's numeric
                                // id. Gate on the actual node kind, never on id equality.
                                val hitNode = graph.nodes.firstOrNull { it.id == hit.id && it.kind == hit.kind }
                                if (hitNode?.kind == NodeKind.GOAL) {
                                    // Tapping the sun (goal) collapses an expanded cluster.
                                    expandedClusterId = null
                                    selectedId = hit.id
                                } else {
                                    selectedId = hit.id
                                    // Real task satellites open a read-only preview.
                                    onTaskTap(hit.id)
                                }
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
                    sunNode = sunNode,
                    taskNodes = taskNodes,
                    scale = scale,
                    time = t,
                    sunEntrance = sunEntrance.value,
                    ringEntrance = ringEntrance.value,
                    nodeEntrance = nodeEntrance.value,
                    expandProgress = expandProgress.value,
                    selectedId = selectedId,
                    expandedClusterId = expandedClusterId,
                    titleMeasured = titleMeasured,
                    pctMeasured = pctMeasured,
                    clusterCountMap = clusterCountMap,
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

/** One concentric orbit ring: fraction of viewport radius (matches GoalGraphBuilder) + style. */
private data class OrbitRing(val frac: Float, val alpha: Float, val width: Float)

/**
 * Visual size multiplier for task satellites (Phase 6.5.7): enlarges every satellite while
 * preserving the HIGH/MEDIUM/LOW/COMPLETED ratio set in GoalGraphBuilder. Sized so the near-deadline
 * halo + overdue dot still have breathing room, and so SIMPLE lanes (≤6 tasks) never overlap.
 * Confined to the renderer — domain geometry/sizes are untouched.
 */
private const val SAT_SIZE_MUL = 1.5f

/** Render-space radius of a satellite node (design size × canvas scale × visual multiplier). */
private fun GoalGraphNode.visualRadius(scale: Float): Float = size * scale * SAT_SIZE_MUL

/** Lerp a color toward white by [amount] (0..1) for a soft, milder gradient core. */
private fun Color.lighten(amount: Float): Color =
    copy(red = red + (1f - red) * amount, green = green + (1f - green) * amount, blue = blue + (1f - blue) * amount)

private fun DrawScope.drawSolarSystem(
    graph: GoalGraph,
    sunNode: GoalGraphNode,
    taskNodes: List<GoalGraphNode>,
    scale: Float,
    time: Float,
    sunEntrance: Float,
    ringEntrance: Float,
    nodeEntrance: Float,
    expandProgress: Float,
    selectedId: Int?,
    expandedClusterId: Int?,
    titleMeasured: androidx.compose.ui.text.TextLayoutResult,
    pctMeasured: androidx.compose.ui.text.TextLayoutResult,
    clusterCountMap: Map<Int, androidx.compose.ui.text.TextLayoutResult>,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    density: androidx.compose.ui.unit.Density,
    onSurface: Color,
    onSurfaceVariant: Color
) {
    val sun = sunNode
    val center = toCanvas(graph, scale, sun.cx, sun.cy)
val sunR = sun.size * scale

    // ── Orbit rings: drawn ONLY for the three meaningful priority lanes (Phase 6.5.3 denoise).
    //    Undated (0.82, shares LOW orbit) and completed (0.97) tasks sit on their builder radii;
    //    completed ring is omitted to cut visual noise. Rings use the EXACT same fractions the
    //    domain builder places nodes on (no drift), and the identical
    //    `viewportRadius * frac * scale` formula as node mapping.
    //    Mirrors GoalGraphBuilder: HIGH 0.34, MEDIUM 0.58, LOW/undated 0.82.
    // Phase 6.3.1: orbit hierarchy — stronger HIGH presence with warm tint, MEDIUM balanced, LOW subtle calm.
    val orbits = listOf(
        OrbitRing(0.34f, 0.40f, 2.0f), // HIGH: warm red tint, strongest presence
        OrbitRing(0.58f, 0.18f, 1.5f), // MEDIUM: orange/fire tint, balanced
        OrbitRing(0.82f, 0.08f, 1.0f)  // LOW: green/calm tint, subtle
    )
    orbits.forEach { ring ->
        val r = graph.viewportRadius * ring.frac * scale
        val orbitColor = when (ring.frac) {
            0.34f -> AccentRed
            0.58f -> AccentFire
            0.82f -> AccentGreen
            else -> onSurfaceVariant
        }
        drawCircle(
            color = orbitColor.copy(alpha = ring.alpha * ringEntrance),
            radius = r,
            center = center,
            style = Stroke(width = ring.width.dp.toPx())
        )
    }

    // ── Gravity edges removed (Phase 6.4+): tasks float on their orbits with no connecting
    //    lines, per design review. `graph.edges` is no longer drawn. ──

    // ── Ring Tide: sun halo intensity scales with goal progress (breathing pulse) ──
    // Phase 6.5.2: make the sun dominant. The luminous body is enlarged and the halo is tighter
    // (smaller max radius) with a stronger peak so it reads as glow hugging the star, not a faint
    // wash. Progress still drives intensity — no fake values.
    val tide = (graph.goalProgressOverall / 100f).coerceIn(0f, 1f)
    val pulse = 1f + 0.05f * sin(time * 2 * Math.PI.toFloat())
    // Phase 6.3.1: sun body enlarged for stronger celestial presence; halo tightened to hug the disc.
    val sunBodyR = sunR * 1.75f * pulse
    val haloRadius = sunBodyR * (1.25f + tide * 0.7f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                AccentGold.copy(alpha = (0.45f + tide * 0.45f) * sunEntrance),
                AccentGold.copy(alpha = (0.18f + tide * 0.22f) * sunEntrance),
                AccentGold.copy(alpha = 0.0f)
            ),
            center = center,
            radius = haloRadius
        ),
        radius = haloRadius,
        center = center
    )

    // ── Sun (goal node) — warm gold, luminous body via layered radial gradient ──
    // Outer glow edge → bright warm core (sun-like), brighter than a flat disc.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFE08A).copy(alpha = sunEntrance), // hot inner highlight
                AccentGold.copy(alpha = sunEntrance),         // gold body
                AccentGold.copy(alpha = 0.85f * sunEntrance)  // slightly deeper edge
            ),
            center = center,
            radius = sunBodyR
        ),
        radius = sunBodyR,
        center = center
    )
    // Soft white-hot center for a stellar read.
    drawCircle(
        color = Color.White.copy(alpha = 0.22f * sunEntrance),
        radius = sunBodyR * 0.42f * pulse,
        center = center
    )

    // Phase 6.3.1: progress ring repositioned around the larger sun body, stronger stroke for clear progress read.
    val progress = graph.goalProgressOverall.coerceIn(0f, 100f)
    val ringR = sunBodyR * 1.35f * pulse
    val sweep = (progress / 100f) * 360f
    // track
    drawArc(
        color = AccentCyan.copy(alpha = 0.18f * sunEntrance),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(center.x - ringR, center.y - ringR),
        size = Size(ringR * 2, ringR * 2),
        style = Stroke(width = 5.dp.toPx())
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
            style = Stroke(width = 5.dp.toPx())
        )
    }

    // ── Sun labels: title ABOVE the sun, progress % centered INSIDE (Phase 6.3.1) ──
    // Text layouts are precomputed once per graph (passed in) — not re-measured every frame.
    // Title sits above the sun disc; % is vertically centered inside the body.
    // Phase 6.3.2: increased gap to 32dp for clear visual separation — title is independent identity.
    val titleGap = 32.dp.toPx()
    val titleY = center.y - sunBodyR - titleGap - titleMeasured.size.height
    drawText(
        textLayoutResult = titleMeasured,
        topLeft = Offset(center.x - titleMeasured.size.width / 2f, titleY),
        alpha = sunEntrance
    )
    // Progress % centered vertically inside the sun body.
    val pctX = center.x - pctMeasured.size.width / 2f
    val pctY = center.y - pctMeasured.size.height / 2f
    drawText(
        textLayoutResult = pctMeasured,
        topLeft = Offset(pctX, pctY),
        alpha = sunEntrance
    )

    // ── CLUSTERED / SUMMARY: overview clusters (or expanded member satellites). SIMPLE draws all. ──
    if (graph.densityMode != GraphDensityMode.SIMPLE) {
        if (expandedClusterId == null) {
            // Overview: draw the four summary clusters with their counts.
            graph.clusters.forEach { cluster ->
                drawCluster(cluster, graph, scale, nodeEntrance, onSurfaceVariant, clusterCountMap[cluster.id])
            }
        } else {
            // Detail exploration: expand the tapped cluster's members as individual satellites;
            // other clusters are dimmed to keep focus. Expansion happens in-view — no navigation.
            val expanded = graph.clusters.firstOrNull { it.id == expandedClusterId }
            graph.clusters.forEach { cluster ->
                if (cluster.id != expandedClusterId) {
                    drawCluster(cluster, graph, scale, nodeEntrance * 0.35f, onSurfaceVariant, clusterCountMap[cluster.id])
                }
            }
            val memberIds = expanded?.memberIds?.toSet() ?: emptySet()
            // L3 scalability (Phase 6.5.6): the SUMMARY tier caps expansion so a huge cluster doesn't
            // re-clutter. Sample the top members by priority (HIGH→MEDIUM→LOW→null) and show a
            // "و N بیشتر" hint for the rest. CLUSTERED expands ALL members (≤20 total, uncluttered).
            // Expansion stays in-view; picking order is deterministic (priority then id).
            val members = taskNodes.filter { it.id in memberIds }
            val L3_CAP = 12
            val applyL3 = graph.densityMode == GraphDensityMode.SUMMARY
            val (shown, hidden) = if (applyL3 && members.size > L3_CAP) {
                val ranked = members.sortedWith(
                    compareBy(
                        { prRank(it.priority) },
                        { it.id }
                    )
                )
                ranked.take(L3_CAP) to (members.size - L3_CAP)
            } else {
                members to 0
            }
            shown.forEach { node ->
                drawSatellite(
                    node = node, graph = graph, scale = scale, time = time,
                    entrance = nodeEntrance * expandProgress, selectedId = selectedId,
                    onSurface = onSurface, onSurfaceVariant = onSurfaceVariant,
                    textMeasurer = textMeasurer
                )
            }
            if (hidden > 0 && expanded != null) {
                val c = expanded
                val pos = toCanvas(graph, scale, c.cx, c.cy)
                val hint = "${RTL}و ${hidden.toEnglishDigits()} بیشتر"
                val measured = textMeasurer.measure(
                    text = hint,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                )
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(pos.x - measured.size.width / 2f, pos.y + c.visualSize * scale + 6.dp.toPx()),
                    alpha = nodeEntrance * expandProgress
                )
            }
        }
    } else {
        // ── INDIVIDUAL mode: every task as its own satellite ──
        taskNodes.forEach { node ->
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
    // Undated tasks (no priority, no deadline) now share the LOW orbit (0.82). To distinguish them
    // from real LOW-priority tasks, use a muted blue-gray tone with reduced alpha.
    val isUndated = node.priority == null && node.colorRole == ColorRole.LOW && !node.isCompleted
    val color = if (isUndated) AccentBlue.copy(alpha = 0.7f) else colorForRole(node.colorRole, onSurfaceVariant)
    val selected = node.id == selectedId
    val baseAlpha = if (isUndated) node.alpha * 0.75f * entrance else node.alpha * entrance

    // ── Gentle breathing shimmer (Phase 6.4): subtle scale+alpha, deterministic per node,
    //    NO positional/orbital movement. Completed memory nodes stay calmer. ──
    val shimmerPhase = (node.id * 92821 % 1000) / 1000f * 2 * Math.PI.toFloat()
    // Phase 6.3.1: LOW tasks quieter — reduced shimmer so they read as calm, not flickering.
    val shimmerAmp = if (node.isCompleted) 0.010f else if (node.colorRole == ColorRole.LOW) 0.012f else 0.03f
    val shimmer = 1f + shimmerAmp * sin(time * 2 * Math.PI.toFloat() + shimmerPhase)
    val alphaShimmer = if (node.isCompleted) 0f else 0.06f * sin(time * 2 * Math.PI.toFloat() + shimmerPhase)

    // Phase 6.3.1: HIGH satellites get a slight visual size boost for priority presence. Composed
    // on top of the global SAT_SIZE_MUL base via visualRadius().
    val visualSizeMul = if (node.colorRole == ColorRole.HIGH && !node.isCompleted) 1.15f else 1f

    // ── Priority presence (Phase 6.5.4) ──
    // HIGH: strong visual weight — tight bright glow ring + larger soft halo.
    if (node.colorRole == ColorRole.HIGH && !node.isCompleted) {
        drawCircle(
            color = color.copy(alpha = 0.28f * entrance),
            radius = node.visualRadius(scale) * visualSizeMul * 2.4f * shimmer,
            center = pos
        )
        drawCircle(
            color = color.copy(alpha = 0.42f * entrance),
            radius = node.visualRadius(scale) * visualSizeMul * 1.35f * shimmer,
            center = pos
        )
    }
    // MEDIUM: light presence — a single soft halo (quieter than HIGH).
    if (node.colorRole == ColorRole.MEDIUM && !node.isCompleted) {
        drawCircle(
            color = color.copy(alpha = 0.10f * entrance),
            radius = node.visualRadius(scale) * visualSizeMul * 1.9f * shimmer,
            center = pos
        )
    }
    if (node.isBoulder) {
        drawCircle(
            color = AccentRed.copy(alpha = 0.28f * entrance),
            radius = node.visualRadius(scale) * 1.8f,
            center = pos
        )
    }
    // ── Deadline cues (Feedback, not judgment) — overlays independent of priority color ──
    // NEAR DEADLINE: a soft amber halo + a tiny calm clock glyph. No harsh red, no blinking.
    if (node.isNearDeadline && !node.isCompleted) {
        val amber = Color(0xFFF59E0B)
        drawCircle(
            color = amber.copy(alpha = 0.22f * entrance),
            radius = node.visualRadius(scale) * 1.9f,
            center = pos
        )
        drawCircle(
            color = amber.copy(alpha = 0.45f * entrance),
            radius = node.visualRadius(scale) * 1.35f,
            center = pos,
            style = Stroke(width = 1.5.dp.toPx())
        )
        // Tiny clock glyph at top-right: small ring + two short hands.
        val clockR = node.visualRadius(scale) * 0.55f
        val clockC = pos + Offset(node.visualRadius(scale) * 1.1f, -node.visualRadius(scale) * 1.1f)
        drawCircle(
            color = amber.copy(alpha = 0.9f * entrance),
            radius = clockR,
            center = clockC,
            style = Stroke(width = 1.2.dp.toPx())
        )
        drawLine(
            color = amber.copy(alpha = 0.9f * entrance),
            start = clockC,
            end = clockC + Offset(0f, -clockR * 0.6f),
            strokeWidth = 1.2.dp.toPx()
        )
        drawLine(
            color = amber.copy(alpha = 0.9f * entrance),
            start = clockC,
            end = clockC + Offset(clockR * 0.5f, 0f),
            strokeWidth = 1.2.dp.toPx()
        )
    }
    if (node.isOverdue) {
        // OVERDUE: a gentle, faded red corner dot (not an aggressive full-satellite ring). Reads as
        // a quiet "past due" marker regardless of priority color — feedback, not alarm.
        val overdueDot = Color(0xFFDC2626).copy(alpha = 0.55f * entrance)
        val dotCenter = pos + Offset(node.visualRadius(scale) * 1.0f, -node.visualRadius(scale) * 1.0f)
        drawCircle(
            color = overdueDot,
            radius = node.visualRadius(scale) * 0.4f,
            center = dotCenter
        )
    }
    if (node.isCompleted) {
        // Ghosted memory object: faint gradient fill + outline ring, no glow, no shimmer.
        // Gradient kept milder than active nodes (subdued core lift) so completed reads as calm.
        val r = node.visualRadius(scale)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.lighten(0.18f).copy(alpha = (baseAlpha * 0.6f).coerceIn(0f, 1f)),
                    color.copy(alpha = (baseAlpha * 0.35f).coerceIn(0f, 1f))
                ),
                center = pos,
                radius = r
            ),
            radius = r,
            center = pos
        )
        drawCircle(
            color = color.copy(alpha = 0.5f * entrance),
            radius = r,
            center = pos,
            style = Stroke(width = 1.dp.toPx())
        )
    } else {
        // Active satellite: soft radial gradient (bright core → main priority color at the edge),
        // milder than the sun's gradient since these are satellites. Preserves priority tint,
        // undated blue-gray, and boulder/overdue/near-deadline color overlays.
        val r = node.visualRadius(scale) * visualSizeMul
        val fillAlpha = (baseAlpha + alphaShimmer).coerceIn(0f, 1f)
        val core = color.lighten(0.30f).copy(alpha = fillAlpha)
        val edge = color.copy(alpha = fillAlpha)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(core, color.copy(alpha = fillAlpha), edge),
                center = pos,
                radius = r * shimmer
            ),
            radius = (if (selected) r * 1.25f else r) * shimmer,
            center = pos
        )
    }
    if (selected) {
        drawCircle(
            color = AccentPurple,
            radius = node.visualRadius(scale) * visualSizeMul * 1.25f * shimmer,
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
    countMeasured: androidx.compose.ui.text.TextLayoutResult?
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
    // Cluster count label is precomputed once per graph in the composable scope (Phase 5.4 / ADR-0009).
    val measured = countMeasured ?: return
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
    data class Task(val id: Int, val kind: NodeKind) : HitResult()
    object None : HitResult()
}

/**
 * Hit-test a tap (canvas px). In CLUSTER mode, clusters take priority; when a cluster is expanded,
 * its member task satellites are tappable. In INDIVIDUAL mode, tasks are tappable directly.
 */
private fun hitTest(graph: GoalGraph, px: Float, py: Float, expandedClusterId: Int?, scale: Float): HitResult {
    // Tap coords (px, py) are in canvas pixels; node coords are in design space. Map design→canvas
    // with the SAME transform the renderer uses (see toCanvas) so hit-testing lines up on any size.
    val cx = graph.viewportRadius * scale
    val cy = graph.viewportRadius * scale
    val tolerance = 12f * scale

    if (graph.densityMode != GraphDensityMode.SIMPLE) {
        if (expandedClusterId == null) {
            graph.clusters.forEach { cluster ->
                val nx = cx + (cluster.cx - graph.centerX) * scale
                val ny = cy + (cluster.cy - graph.centerY) * scale
                if (GraphGeometry.distance(px, py, nx, ny) <= cluster.visualSize * scale + tolerance) {
                    return HitResult.Cluster(cluster.id)
                }
            }
            return HitResult.None
        }
        val expanded = graph.clusters.firstOrNull { it.id == expandedClusterId } ?: return HitResult.None
        val memberIds = expanded.memberIds.toSet()
        graph.nodes.filter { it.kind == NodeKind.TASK && it.id in memberIds }.forEach { node ->
            val nx = cx + (node.cx - graph.centerX) * scale
            val ny = cy + (node.cy - graph.centerY) * scale
            if (GraphGeometry.distance(px, py, nx, ny) <= node.visualRadius(scale) + tolerance) {
                return HitResult.Task(node.id, node.kind)
            }
        }
        return HitResult.None
    }

    // SIMPLE mode: tasks only.
    var bestId: Int? = null
    var bestDist = Float.MAX_VALUE
    graph.nodes.filter { it.kind == NodeKind.TASK }.forEach { node ->
        val nx = cx + (node.cx - graph.centerX) * scale
        val ny = cy + (node.cy - graph.centerY) * scale
        val d = GraphGeometry.distance(px, py, nx, ny)
        if (d <= node.visualRadius(scale) + tolerance && d < bestDist) {
            bestDist = d
            bestId = node.id
        }
    }
    return if (bestId != null) HitResult.Task(bestId, NodeKind.TASK) else HitResult.None
}

/** Deterministic priority rank for L3 expansion sampling (HIGH→MEDIUM→LOW→null). Mirrors builder. */
private fun prRank(priority: String?): Int = when (priority) {
    "HIGH" -> 0
    "MEDIUM" -> 1
    "LOW" -> 2
    else -> 3
}

private fun colorForRole(role: ColorRole, onSurfaceVariant: Color): Color = when (role) {
    ColorRole.GOAL -> AccentGold
    ColorRole.HIGH -> AccentRed
    ColorRole.MEDIUM -> AccentFire
    ColorRole.LOW -> AccentGreen
    ColorRole.BOULDER -> AccentRed
    ColorRole.OVERDUE -> Color(0xFFDC2626) // distinct overdue red (slightly deeper than AccentRed)
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

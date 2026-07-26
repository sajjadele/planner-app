package com.example.plugins.goals.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
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
import com.example.domain.graph.GraphGeometry
import com.example.domain.graph.VisibilityLevel
import com.example.domain.graph.VisibilityResolver
import com.example.domain.graph.VisibleCluster
import com.example.domain.graph.VisibleGraphModel
import com.example.domain.graph.VisibleTask
import com.example.ui.theme.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info

/**
 * Bottom-sheet body rendering the Behavioral Solar System for a single goal.
 *
 * Phase 2B: pure rendering of [VisibleGraphModel]. Visibility decisions live in
 * domain.graph.VisibilityResolver. Phase 2C.1: the Sun is Goal **identity only**
 * (name + decorative presence) — no progress, health, or attention on the sun.
 *
 * Phase 2C.4: progressive disclosure via "N more" chrome; cluster expand/collapse;
 * selection is ephemeral (cleared when preview dismisses).
 *
 * [expandedClusterId] is owned by the ViewModel (affects visible content).
 * [selectedId] is transient Compose UI state only.
 */
@Composable
fun GoalGraphSheetContent(
    visibleGraph: VisibleGraphModel,
    goalTitle: String,
    expandedClusterId: Int? = null,
    onExpandedClusterChange: (Int?) -> Unit = {},
    onToggleCluster: (Int) -> Unit = {},
    onRevealMore: () -> Unit = {},
    selectedTaskId: Int? = null,
    onSelectedTaskChange: (Int?) -> Unit = {},
    autoShowEducation: Boolean = false,
    onEducationDismissed: () -> Unit = {},
    onTaskTap: (Int) -> Unit = {}
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var showLegend by remember { mutableStateOf(false) }
    val wasEducation = remember { autoShowEducation }
    LaunchedEffect(Unit) {
        if (autoShowEducation) showLegend = true
    }

    val sunEntrance = remember { Animatable(0f) }
    val ringEntrance = remember { Animatable(0f) }
    val nodeEntrance = remember { Animatable(0f) }
    var entranceDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { sunEntrance.animateTo(1f, tween(280, easing = FastOutSlowInEasing)) }
        launch {
            delay(140)
            ringEntrance.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        }
        delay(300)
        nodeEntrance.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        entranceDone = true
    }

    val expandProgress = remember { Animatable(0f) }
    LaunchedEffect(expandedClusterId) {
        val target = if (expandedClusterId != null) 1f else 0f
        expandProgress.animateTo(target, tween(320, easing = FastOutSlowInEasing))
    }

    // Idle motion only when a boulder is visible (avoids continuous Canvas invalidation).
    val needsIdleMotion = remember(visibleGraph) {
        visibleGraph.tasks.any { it.isBoulder }
    }
    var clock by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(entranceDone, needsIdleMotion) {
        if (!entranceDone || !needsIdleMotion) {
            clock = 0f
            return@LaunchedEffect
        }
        val anim = Animatable(0f)
        while (isActive) {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(4000, easing = LinearEasing)) {
                clock = value
            }
        }
    }
    val t = if (entranceDone && needsIdleMotion) clock else 0f

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    val titleMeasured = remember(goalTitle) {
        textMeasurer.measure(
            text = "${RTL}${goalTitle}",
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
    val clusterCountMap = remember(visibleGraph) {
        visibleGraph.clusters.associate { cluster ->
            cluster.clusterId to textMeasurer.measure(
                text = cluster.count.toString(),
                style = TextStyle(
                    fontSize = (if (cluster.count >= 10) 13.sp else 15.sp),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
    // Progressive disclosure chrome only when a deeper level is still available.
    // Progressive disclosure chrome only when a deeper level is still available.
    val canRevealMore = remember(visibleGraph) {
        when (visibleGraph.level) {
            VisibilityLevel.OVERVIEW -> visibleGraph.hiddenCount > 0
            VisibilityLevel.EXPANDED ->
                visibleGraph.hiddenCount > 0 &&
                    (visibleGraph.tasks.size + visibleGraph.hiddenCount) > VisibilityResolver.CLUSTER_THRESHOLD
            VisibilityLevel.INSIGHT -> false
        }
    }
    val hiddenHintMeasured = remember(visibleGraph.hiddenCount, canRevealMore, primary) {
        if (!canRevealMore) null
        else textMeasurer.measure(
            text = "${RTL}نمایش ${visibleGraph.hiddenCount.toEnglishDigits()} مورد دیگر",
            style = TextStyle(
                fontSize = 12.sp,
                color = primary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        )
    }
    val emptyHintMeasured = remember(onSurfaceVariant) {
        textMeasurer.measure(
            text = "${RTL}تسک فعالی نیست",
            style = TextStyle(
                fontSize = 12.sp,
                color = onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        )
    }
    val isEmptyGraph = visibleGraph.tasks.isEmpty() && visibleGraph.clusters.isEmpty()

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
                            body = "مرکز منظومه و هویت هدف است؛ پیشرفت و توجه روی سیارهها نشان داده میشوند."
                        )
                        LegendRow(
                            glyph = "🔥",
                            title = "توجه بالا",
                            body = "تسکهایی که نیاز به توجه بیشتری دارند نزدیکتر به هدف قرار میگیرند."
                        )
                        LegendRow(
                            glyph = "⏰",
                            title = "موعد گذشته",
                            body = "تسکهایی که مهلت آنها گذشته و هنوز انجام نشدهاند با حلقه قرمز مشخص میشوند."
                        )
                        LegendRow(
                            glyph = "●",
                            title = "تسک فعال",
                            body = "هر نقطه نشاندهنده یک کار این هدف است."
                        )
                        LegendRow(
                            glyph = "○",
                            title = "خوشه توجه",
                            body = "وقتی تعداد تسکها زیاد است، تسکهای مشابه در خوشههای توجه گروهبندی میشوند."
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
                    .pointerInput(visibleGraph, expandedClusterId) {
                        detectTapGestures { offset ->
                            val hitScale = size.width.toFloat() / (visibleGraph.viewportRadius * 2f)
                            val hit = hitTestVisible(
                                visibleGraph,
                                offset.x,
                                offset.y,
                                expandedClusterId,
                                hitScale,
                                hiddenHintMeasured,
                                density.density
                            )
                            when (hit) {
                                is VisibleHit.Cluster -> onToggleCluster(hit.id)
                                is VisibleHit.Task -> {
                                    onSelectedTaskChange(hit.id)
                                    onTaskTap(hit.id)
                                }
                                is VisibleHit.ShowMore -> onRevealMore()
                                is VisibleHit.Sun -> {
                                    onExpandedClusterChange(null)
                                    onSelectedTaskChange(null)
                                }
                                VisibleHit.None -> {
                                    onExpandedClusterChange(null)
                                    onSelectedTaskChange(null)
                                }
                            }
                        }
                    }
            ) {
                val scale = size.minDimension / (visibleGraph.viewportRadius * 2f)
                drawVisibleSolarSystem(
                    model = visibleGraph,
                    scale = scale,
                    time = t,
                    sunEntrance = sunEntrance.value,
                    ringEntrance = ringEntrance.value,
                    nodeEntrance = nodeEntrance.value,
                    expandProgress = expandProgress.value,
                    selectedId = selectedTaskId,
                    expandedClusterId = expandedClusterId,
                    titleMeasured = titleMeasured,
                    clusterCountMap = clusterCountMap,
                    hiddenHintMeasured = hiddenHintMeasured,
                    emptyHintMeasured = if (isEmptyGraph) emptyHintMeasured else null,
                    onSurface = onSurface,
                    onSurfaceVariant = onSurfaceVariant,
                    primary = primary
                )
            }
        }
    }
}

private fun toCanvas(model: VisibleGraphModel, scale: Float, x: Float, y: Float): Offset {
    val cx = model.viewportRadius * scale
    val cy = model.viewportRadius * scale
    return Offset(cx + (x - model.centerX) * scale, cy + (y - model.centerY) * scale)
}

private fun taskPosition(model: VisibleGraphModel, task: VisibleTask): Pair<Float, Float> =
    GraphGeometry.project(model.centerX, model.centerY, task.radius, task.angle)

private fun clusterPosition(model: VisibleGraphModel, cluster: VisibleCluster): Pair<Float, Float> =
    GraphGeometry.project(model.centerX, model.centerY, cluster.radius, cluster.angle)

private const val SAT_SIZE = 16f
private const val SAT_SIZE_MUL = 1.5f
/** Fixed design-space sun radius (identity object — not progress-scaled). */
private const val SUN_SIZE = 40f
/** Decorative pulse amplitude: ~98%–102% of fixed sun size (not semantic). */
private const val SUN_PULSE_AMP = 0.02f
/** Soft identity halo scale relative to sun body (not progress-driven). */
private const val SUN_HALO_SCALE = 1.35f

private fun Color.lighten(amount: Float): Color =
    copy(red = red + (1f - red) * amount, green = green + (1f - green) * amount, blue = blue + (1f - blue) * amount)

private fun DrawScope.drawVisibleSolarSystem(
    model: VisibleGraphModel,
    scale: Float,
    time: Float,
    sunEntrance: Float,
    ringEntrance: Float,
    nodeEntrance: Float,
    expandProgress: Float,
    selectedId: Int?,
    expandedClusterId: Int?,
    titleMeasured: androidx.compose.ui.text.TextLayoutResult,
    clusterCountMap: Map<Int, androidx.compose.ui.text.TextLayoutResult>,
    hiddenHintMeasured: androidx.compose.ui.text.TextLayoutResult?,
    emptyHintMeasured: androidx.compose.ui.text.TextLayoutResult?,
    onSurface: Color,
    onSurfaceVariant: Color,
    primary: Color
) {
    val center = toCanvas(model, scale, model.centerX, model.centerY)
    val sunR = SUN_SIZE * scale

    // Orbit bands: faint astronomical guides only.
    // Attention is communicated solely by planet distance from the sun — not by ring style.
    val ringColor = onSurfaceVariant
    val ringStroke = Stroke(width = 1.0f.dp.toPx())
    model.orbitBands.forEachIndexed { index, band ->
        val r = band.radius * scale
        // Equal visual language; slight fade with distance for depth, never attention color
        val alpha = when (index) {
            0 -> 0.08f
            1 -> 0.06f
            2 -> 0.05f
            else -> 0.04f
        }
        drawCircle(
            color = ringColor.copy(alpha = alpha * ringEntrance),
            radius = r,
            center = center,
            style = ringStroke
        )
    }

    // ── Sun: Goal identity only (Phase 2C.1) ──
    // Fixed size + subtle decorative pulse. No progress, health, or attention.
    // Color: AccentGold fallback (GoalEntity has no user-selected color field yet).
    val pulse = 1f + SUN_PULSE_AMP * sin(time * 2 * Math.PI.toFloat())
    val sunBodyR = sunR * pulse
    val haloRadius = sunBodyR * SUN_HALO_SCALE
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
    drawCircle(
        color = Color.White.copy(alpha = 0.18f * sunEntrance),
        radius = sunBodyR * 0.42f,
        center = center
    )

    val titleGap = 32.dp.toPx()
    val titleY = center.y - sunBodyR - titleGap - titleMeasured.size.height
    drawText(
        textLayoutResult = titleMeasured,
        topLeft = Offset(center.x - titleMeasured.size.width / 2f, titleY),
        alpha = sunEntrance
    )

    // Clusters (Insight level) when not expanded
    if (model.clusters.isNotEmpty() && expandedClusterId == null) {
        model.clusters.forEach { cluster ->
            drawVisibleCluster(cluster, model, scale, nodeEntrance, onSurfaceVariant, clusterCountMap[cluster.clusterId])
        }
    } else if (model.clusters.isNotEmpty() && expandedClusterId != null) {
        val expanded = model.clusters.firstOrNull { it.clusterId == expandedClusterId }
        model.clusters.forEach { cluster ->
            if (cluster.clusterId != expandedClusterId) {
                drawVisibleCluster(cluster, model, scale, nodeEntrance * 0.35f, onSurfaceVariant, clusterCountMap[cluster.clusterId])
            }
        }
        // Expanded cluster members: look up from individual tasks by member id
        // Members may not be in model.tasks (they were clustered). Draw placeholders at band radius.
        val memberIds = expanded?.memberIds ?: emptyList()
        val shown = model.tasks.filter { it.taskId in memberIds }
        if (shown.isNotEmpty()) {
            shown.forEach { task ->
                drawVisibleSatellite(
                    task = task, model = model, scale = scale, time = time,
                    entrance = nodeEntrance * expandProgress, selectedId = selectedId
                )
            }
        } else if (expanded != null) {
            // Members not in visible individual set — place along cluster band by attention order
            expanded.memberIds.forEachIndexed { i, taskId ->
                val n = expanded.memberIds.size
                val angle = if (n == 1) expanded.angle
                else (i.toFloat() / n) * GraphGeometry.TWO_PI + ((taskId * 92821) % 1000) / 1000f * 0.25f
                val placeholder = VisibleTask(
                    taskId = taskId,
                    attentionScore = expanded.band.range.start,
                    radius = expanded.radius,
                    angle = angle,
                    reasons = emptyList(),
                    isBoulder = false,
                    isOverdue = false,
                    isNearDeadline = false,
                    priority = null,
                    title = "task-$taskId"
                )
                drawVisibleSatellite(
                    task = placeholder, model = model, scale = scale, time = time,
                    entrance = nodeEntrance * expandProgress, selectedId = selectedId
                )
            }
        }
    }

    // Individual visible tasks (Overview / Expanded / Insight top tasks)
    if (expandedClusterId == null) {
        model.tasks.forEach { task ->
            drawVisibleSatellite(
                task = task, model = model, scale = scale, time = time,
                entrance = nodeEntrance, selectedId = selectedId
            )
        }
    }

    // Empty graph guidance (sun remains identity only)
    if (emptyHintMeasured != null) {
        drawText(
            textLayoutResult = emptyHintMeasured,
            topLeft = Offset(
                center.x - emptyHintMeasured.size.width / 2f,
                center.y + model.viewportRadius * 0.72f * scale
            ),
            alpha = nodeEntrance
        )
    }

    // Show-more affordance: fixed bottom-left chip (outside solar system area)
    if (hiddenHintMeasured != null && expandedClusterId == null) {
        val horizontalPadding = 24.dp.toPx()
        val bottomPadding = 40.dp.toPx()
        val chipWidth = hiddenHintMeasured.size.width + 32.dp.toPx()
        val chipHeight = hiddenHintMeasured.size.height + 16.dp.toPx()
        // Fixed position: bottom-left corner, well outside orbit rings
        val chipX = horizontalPadding
        val chipY = size.height - bottomPadding - chipHeight
        
        // Background: primary blue with low opacity
        drawRoundRect(
            color = primary.copy(alpha = 0.12f * nodeEntrance),
            topLeft = Offset(chipX, chipY),
            size = Size(chipWidth, chipHeight),
            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
        )
        
        // Text
        drawText(
            textLayoutResult = hiddenHintMeasured,
            topLeft = Offset(
                chipX + 16.dp.toPx(),
                chipY + chipHeight / 2f - hiddenHintMeasured.size.height / 2f
            ),
            alpha = nodeEntrance
        )
    }
}

private fun DrawScope.drawVisibleSatellite(
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

    // Neutral planet color - attention is encoded by orbit position only
    // Categorical states only: boulder, overdue, near-deadline, selected
    val isSelected = task.taskId == selectedId
    val baseColor = when {
        task.isOverdue -> Color(0xFFDC2626)
        task.isBoulder -> AccentRed
        task.isNearDeadline -> Color(0xFFF59E0B)
        else -> AccentPurple // neutral planet color
    }

    val baseAlpha = entrance
    val visualR = SAT_SIZE * scale * SAT_SIZE_MUL
    val r = visualR // uniform size for all planets

    // Subtle elevation shadow (not attention-based glow)
    val shadowAlpha = 0.12f * entrance
    drawCircle(
        color = Color.Black.copy(alpha = shadowAlpha),
        radius = r * 1.15f,
        center = pos + Offset(x = 0f, y = 1.dp.toPx())
    )

    // Overdue: small badge indicator only (not full planet color)
    if (task.isOverdue && !baseColor.equals(Color(0xFFDC2626))) {
        val overdueDot = Color(0xFFDC2626).copy(alpha = 0.6f * entrance)
        drawCircle(color = overdueDot, radius = r * 0.35f, center = pos + Offset(r * 0.7f, -r * 0.7f))
    }

    // Boulder: subtle texture indicator (inner ring) - not aggressive glow
    if (task.isBoulder) {
        drawCircle(
            color = AccentRed.copy(alpha = 0.18f * entrance),
            radius = r * 0.85f,
            center = pos,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }

    // Near deadline: thin accent ring
    if (task.isNearDeadline) {
        val amber = Color(0xFFF59E0B)
        drawCircle(
            color = amber.copy(alpha = 0.35f * entrance),
            radius = r * 1.15f,
            center = pos,
            style = Stroke(width = 1.dp.toPx())
        )
    }

    // Main planet body - uniform size, neutral color for normal tasks
    val fillAlpha = baseAlpha
    val core = baseColor.lighten(0.25f).copy(alpha = fillAlpha)
    val edge = baseColor.copy(alpha = fillAlpha)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(core, baseColor.copy(alpha = fillAlpha), edge),
            center = pos,
            radius = r
        ),
        radius = if (isSelected) r * 1.2f else r,
        center = pos
    )

    // Selection ring only — dialog is the primary insight surface (no canvas title label)
    if (isSelected) {
        drawCircle(
            color = AccentPurple,
            radius = r * 1.2f,
            center = pos,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun DrawScope.drawVisibleCluster(
    cluster: VisibleCluster,
    model: VisibleGraphModel,
    scale: Float,
    alpha: Float,
    onSurfaceVariant: Color,
    countMeasured: androidx.compose.ui.text.TextLayoutResult?
) {
    val (px, py) = clusterPosition(model, cluster)
    val pos = toCanvas(model, scale, px, py)
    // Neutral cluster color - attention is encoded by orbit position, not color
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

private sealed class VisibleHit {
    data class Cluster(val id: Int) : VisibleHit()
    data class Task(val id: Int) : VisibleHit()
    data object ShowMore : VisibleHit()
    data object Sun : VisibleHit()
    data object None : VisibleHit()
}

private fun hitTestVisible(
    model: VisibleGraphModel,
    px: Float,
    py: Float,
    expandedClusterId: Int?,
    scale: Float,
    hiddenHintMeasured: androidx.compose.ui.text.TextLayoutResult?,
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

    val tasksToHit = if (expandedClusterId != null) {
        val expanded = model.clusters.firstOrNull { it.clusterId == expandedClusterId }
        val memberIds = expanded?.memberIds?.toSet() ?: emptySet()
        model.tasks.filter { it.taskId in memberIds }.ifEmpty {
            expanded?.memberIds?.mapIndexed { idx, id ->
                val n = expanded.memberIds.size
                val angle = if (n == 1) expanded.angle
                else (idx.toFloat() / n) * GraphGeometry.TWO_PI +
                    ((id * 92821) % 1000) / 1000f * 0.25f
                VisibleTask(
                    taskId = id,
                    attentionScore = 0f,
                    radius = expanded.radius,
                    angle = angle,
                    reasons = emptyList(),
                    isBoulder = false,
                    isOverdue = false,
                    isNearDeadline = false,
                    priority = null,
                    title = ""
                )
            } ?: emptyList()
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

    // Progressive disclosure chrome: "N more" - Fixed bottom-left position
    if (hiddenHintMeasured != null && model.hiddenCount > 0 && expandedClusterId == null) {
        val horizontalPadding = 24f * density
        val bottomPadding = 40f * density
        val chipWidth = hiddenHintMeasured.size.width + 32f * density
        val chipHeight = hiddenHintMeasured.size.height + 16f * density
        val chipX = horizontalPadding
        val chipY = (2f * model.viewportRadius * scale) - bottomPadding - chipHeight
        
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
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
            Text(
                text = body,
                fontSize = 12.sp,
                color = color.copy(alpha = if (enabled) 0.8f else 0.5f),
                lineHeight = 17.sp
            )
        }
    }
}

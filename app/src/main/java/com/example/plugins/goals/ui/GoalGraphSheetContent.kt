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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
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
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import com.example.core.util.RTL
import com.example.core.util.toEnglishDigits
import com.example.domain.graph.VisibilityLevel
import com.example.domain.graph.VisibilityResolver
import com.example.domain.graph.VisibleGraphModel
import com.example.ui.theme.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info

/**
 * Bottom-sheet body rendering the Behavioral Solar System for a single goal.
 *
 * Entry point only — rendering delegated to [SolarSystemRenderer],
 * hit-testing to [SolarSystemHitTest], cluster layout to [ClusterLayoutCalculator].
 *
 * Phase 2B: pure rendering of [VisibleGraphModel]. Visibility decisions live in
 * domain.graph.VisibilityResolver.
 *
 * Phase 4: extracted renderer/hit-test/cluster-layout for separation of concerns.
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

    // ── Entrance animations ──
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

    // ── Idle motion (boulder wobble) ──
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

    // ── Theme colors ──
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    // ── Text measurements ──
    val titleMeasured = remember(goalTitle) {
        textMeasurer.measure(
            text = "${RTL}${goalTitle}",
            style = TextStyle(
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            constraints = Constraints(maxWidth = (150f * density.density).roundToInt())
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

    // ── Progressive disclosure chrome ──
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
                fontSize = 13.sp,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        )
    }
    val isEmptyGraph = visibleGraph.tasks.isEmpty() && visibleGraph.clusters.isEmpty()

    // ── Layout ──
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
                        LegendRow(glyph = "☀", title = "هدف", body = "مرکز منظومه و هویت هدف است؛ پیشرفت و توجه روی سیارهها نشان داده میشوند.")
                        LegendRow(glyph = "🔥", title = "توجه بالا", body = "تسکهایی که نیاز به توجه بیشتری دارند نزدیکتر به هدف قرار میگیرند.")
                        LegendRow(glyph = "⏰", title = "موعد گذشته", body = "تسکهایی که مهلت آنها گذشته و هنوز انجام نشدهاند با حلقه قرمز مشخص میشوند.")
                        LegendRow(glyph = "●", title = "تسک فعال", body = "هر نقطه نشاندهنده یک کار این هدف است.")
                        LegendRow(glyph = "○", title = "خوشه توجه", body = "وقتی تعداد تسکها زیاد است، تسکهای مشابه در خوشههای توجه گروهبندی میشوند.")
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

// ── Legend ──

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

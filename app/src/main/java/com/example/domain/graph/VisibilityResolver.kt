package com.example.domain.graph

import com.example.domain.attention.AttentionResult
import kotlin.math.PI
import kotlin.math.min

/**
 * Pure-Kotlin Visibility Resolver for the Behavioral Solar System (Phase 2B).
 *
 * Decides WHAT should be visible based on attention scores:
 * - Overview selection (min 3, max 7 highest-attention tasks)
 * - Clustering (attention-band clusters for >20 active tasks)
 * - Continuous orbit radius (higher attention = closer to sun)
 * - Visibility limits and completed-task exclusion
 *
 * Attention and Visibility are strictly separated:
 * - [com.example.domain.attention.AttentionCalculator] computes WHY a task needs attention.
 * - VisibilityResolver decides WHAT should be visible.
 *
 * No Android dependencies — host-JVM testable.
 */
object VisibilityResolver {

    // ── Overview limits ──

    /** Minimum tasks shown in Overview level. */
    const val OVERVIEW_MIN = 3

    /** Maximum tasks shown in Overview level. */
    const val OVERVIEW_MAX = 7

    /** Active task count at/above which clustering is enabled. */
    const val CLUSTER_THRESHOLD = 20

    /** Maximum individual tasks shown in Expanded level. */
    const val EXPANDED_MAX = CLUSTER_THRESHOLD

    // ── Orbit radius constants ──

    /** Inner orbit radius (fraction of viewport) — score 1.0 maps here. */
    private const val INNER_RADIUS_FRAC = 0.30f

    /** Outer orbit radius (fraction of viewport) — score 0.0 maps here. */
    private const val OUTER_RADIUS_FRAC = 0.85f

    /** Window (ms) before a scheduled date within which an active task reads as "near due" (24h). */
    const val NEAR_DEADLINE_WINDOW_MS = 24L * 60L * 60L * 1000L

    /** Minimum angular gap as fraction of ideal spacing. */
    private const val RELAXATION_MIN_GAP_FRAC = 0.65f

    /**
     * Resolve visibility for the requested [VisibilityLevel].
     *
     * @param input all data needed for visibility decisions
     * @return [VisibleGraphModel] containing only what should be rendered
     */
    fun resolve(input: VisibilityInput): VisibleGraphModel {
        val cx = input.viewportRadius
        val cy = input.viewportRadius

        // Sort active tasks by attention score descending (highest first).
        // Tasks with no attention result default to score 0.0.
        val sorted = input.activeTaskIds
            .map { id ->
                val result = input.attentionResults[id] ?: AttentionResult(
                    score = 0f,
                    components = com.example.domain.attention.AttentionComponents(0f, 0f, 0f),
                    reasons = emptyList()
                )
                id to result
            }
            .sortedByDescending { it.second.score }

        val orbitBands = buildOrbitBands(input.viewportRadius)

        return when (input.level) {
            VisibilityLevel.OVERVIEW -> resolveOverview(
                sorted = sorted,
                input = input,
                cx = cx,
                cy = cy,
                orbitBands = orbitBands
            )
            VisibilityLevel.EXPANDED -> resolveExpanded(
                sorted = sorted,
                input = input,
                cx = cx,
                cy = cy,
                orbitBands = orbitBands
            )
            VisibilityLevel.INSIGHT -> resolveInsight(
                sorted = sorted,
                input = input,
                cx = cx,
                cy = cy,
                orbitBands = orbitBands
            )
        }
    }

    // ── Level: Overview ──

    /**
     * Overview: show the highest-attention tasks (min 3, max 7).
     *
     * If there are fewer than [OVERVIEW_MAX] tasks, show all of them.
     * If there are more, show the top [OVERVIEW_MAX] by attention score.
     * Never show fewer than [OVERVIEW_MIN] (unless fewer tasks exist).
     */
    private fun resolveOverview(
        sorted: List<Pair<Int, AttentionResult>>,
        input: VisibilityInput,
        cx: Float,
        cy: Float,
        orbitBands: List<OrbitBand>
    ): VisibleGraphModel {
        val total = sorted.size
        val visibleCount = when {
            total <= OVERVIEW_MAX -> total
            else -> OVERVIEW_MAX
        }
        val visible = sorted.take(visibleCount)
        val hiddenCount = total - visibleCount

        val tasks = relaxAngles(visible.mapIndexed { index, (id, result) ->
            toVisibleTask(id, result, input, cx, cy, index, visible.size)
        })

        return VisibleGraphModel(
            level = VisibilityLevel.OVERVIEW,
            tasks = tasks,
            clusters = emptyList(),
            hiddenCount = hiddenCount,
            orbitBands = orbitBands,
            viewportRadius = input.viewportRadius,
            centerX = cx,
            centerY = cy
        )
    }

    // ── Level: Expanded ──

    /**
     * Expanded: reveal more tasks (up to [EXPANDED_MAX]).
     *
     * If the total is within the individual-rendering threshold, show all.
     * Otherwise show up to [EXPANDED_MAX] highest-attention tasks.
     */
    private fun resolveExpanded(
        sorted: List<Pair<Int, AttentionResult>>,
        input: VisibilityInput,
        cx: Float,
        cy: Float,
        orbitBands: List<OrbitBand>
    ): VisibleGraphModel {
        val total = sorted.size
        val visibleCount = when {
            total <= EXPANDED_MAX -> total
            else -> EXPANDED_MAX
        }
        val visible = sorted.take(visibleCount)
        val hiddenCount = total - visibleCount

        val tasks = relaxAngles(visible.mapIndexed { index, (id, result) ->
            toVisibleTask(id, result, input, cx, cy, index, visible.size)
        })

        return VisibleGraphModel(
            level = VisibilityLevel.EXPANDED,
            tasks = tasks,
            clusters = emptyList(),
            hiddenCount = hiddenCount,
            orbitBands = orbitBands,
            viewportRadius = input.viewportRadius,
            centerX = cx,
            centerY = cy
        )
    }

    // ── Level: Insight ──

    /**
     * Insight: cluster tasks into attention bands when the collection is large.
     *
     * If total <= [CLUSTER_THRESHOLD], show all tasks individually (same as Expanded).
     * If total > [CLUSTER_THRESHOLD], group remaining tasks into attention-band clusters.
     *
     * The highest-attention tasks (up to [OVERVIEW_MAX]) are shown individually
     * for immediate readability; the rest are clustered.
     */
    private fun resolveInsight(
        sorted: List<Pair<Int, AttentionResult>>,
        input: VisibilityInput,
        cx: Float,
        cy: Float,
        orbitBands: List<OrbitBand>
    ): VisibleGraphModel {
        val total = sorted.size

        // Small collections: show all individually (same as Expanded).
        if (total <= CLUSTER_THRESHOLD) {
        val tasks = relaxAngles(sorted.mapIndexed { index, (id, result) ->
            toVisibleTask(id, result, input, cx, cy, index, sorted.size)
        })
            return VisibleGraphModel(
                level = VisibilityLevel.INSIGHT,
                tasks = tasks,
                clusters = emptyList(),
                hiddenCount = 0,
                orbitBands = orbitBands,
                viewportRadius = input.viewportRadius,
                centerX = cx,
                centerY = cy
            )
        }

        // Large collections: show top tasks individually, cluster the rest.
        val individualCount = min(OVERVIEW_MAX, total)
        val visibleIndividual = sorted.take(individualCount)
        val remainder = sorted.drop(individualCount)

        val tasks = relaxAngles(visibleIndividual.mapIndexed { index, (id, result) ->
            toVisibleTask(id, result, input, cx, cy, index, visibleIndividual.size)
        })

        val clusters = buildClusters(remainder, input, cx, cy)
        val hiddenCount = remainder.size

        return VisibleGraphModel(
            level = VisibilityLevel.INSIGHT,
            tasks = tasks,
            clusters = clusters,
            hiddenCount = hiddenCount,
            orbitBands = orbitBands,
            viewportRadius = input.viewportRadius,
            centerX = cx,
            centerY = cy
        )
    }

    // ── Task → VisibleTask ──

    /**
     * Convert a task id + attention result into a [VisibleTask] with continuous
     * orbit radius and deterministic angle.
     */
    private fun toVisibleTask(
        id: Int,
        result: AttentionResult,
        input: VisibilityInput,
        cx: Float,
        cy: Float,
        visibleIndex: Int = -1,
        visibleCount: Int = -1
    ): VisibleTask {
        val radius = orbitRadius(result.score, input.viewportRadius)
        val angle = if (visibleIndex >= 0 && visibleCount > 0) {
            // Post-visibility angle assignment: distribute visible subset evenly
            val baseAngle = (visibleIndex.toFloat() / visibleCount) * GraphGeometry.TWO_PI
            val jitter = GoalGraphBuilder.jitter(id)
            baseAngle + jitter
        } else {
            // Legacy: use global count (for clusters or pre-2C.4 compatibility)
            deterministicAngle(id, input.activeTaskCount)
        }
        val metadata = input.taskMetadata[id]

        val todayMidnight = input.nowMillis - (input.nowMillis % 86400000L)
        val isOverdue = metadata?.dateEpochMs != null && metadata.dateEpochMs < todayMidnight
        val isNearDeadline = metadata?.dateEpochMs != null && !isOverdue &&
            metadata.dateEpochMs >= input.nowMillis &&
            metadata.dateEpochMs < input.nowMillis + NEAR_DEADLINE_WINDOW_MS
        val isBoulder = (metadata?.rescheduleCount ?: 0) >= BOULDER_RESCHEDULE_THRESHOLD

        return VisibleTask(
            taskId = id,
            attentionScore = result.score,
            radius = radius,
            angle = angle,
            reasons = result.reasons,
            isBoulder = isBoulder,
            isOverdue = isOverdue,
            isNearDeadline = isNearDeadline,
            priority = metadata?.priority,
            title = metadata?.title ?: "task-$id"
        )
    }

    // ── Angle relaxation ──

    /**
     * Deterministic relaxation: ensure minimum angular separation between
     * neighboring planets. Prevents visual collisions caused by jitter
     * reducing angular distance below a readable threshold.
     *
     * Single forward pass + wrap-around fix. No Random, no animation.
     * Same input always produces same output.
     */
    private fun relaxAngles(tasks: List<VisibleTask>): List<VisibleTask> {
        if (tasks.size <= 1) return tasks

        val sorted = tasks.sortedBy { it.angle }
        val n = sorted.size
        val minGap = (GraphGeometry.TWO_PI / n.toFloat()) * RELAXATION_MIN_GAP_FRAC

        // Work with mutable angle array
        val angles = FloatArray(n) { sorted[it].angle }

        // Forward pass: enforce minimum gap between consecutive planets
        for (i in 1 until n) {
            var gap = angles[i] - angles[i - 1]
            if (gap < 0f) gap += GraphGeometry.TWO_PI
            if (gap < minGap) {
                angles[i] = angles[i - 1] + minGap
            }
        }

        // Wrap-around: last planet → first planet gap
        var wrapGap = angles[0] + GraphGeometry.TWO_PI - angles[n - 1]
        if (wrapGap < minGap) {
            // Evenly distribute the deficit across all planets
            val shift = (minGap - wrapGap) / n.toFloat()
            for (i in angles.indices) {
                angles[i] += shift * (n - 1 - i).toFloat()
            }
        }

        // Normalize to [0, TWO_PI)
        return sorted.mapIndexed { i, task ->
            task.copy(angle = ((angles[i] % GraphGeometry.TWO_PI) + GraphGeometry.TWO_PI) % GraphGeometry.TWO_PI)
        }
    }

    // ── Clustering ──

    /**
     * Group remainder tasks into attention-band clusters.
     *
     * Each non-empty band produces a [VisibleCluster] positioned at the band's
     * radius and a deterministic angle.
     */
    private fun buildClusters(
        remainder: List<Pair<Int, AttentionResult>>,
        input: VisibilityInput,
        cx: Float,
        cy: Float
    ): List<VisibleCluster> {
        val clusterAngleStep = (2 * PI.toFloat()) / AttentionBand.values().size
        return AttentionBand.values().mapIndexedNotNull { bandIndex, band ->
            // remainder is already sorted by score descending; preserve that order in memberIds
            val members = remainder.filter { band.range.contains(it.second.score) }
            if (members.isEmpty()) return@mapIndexedNotNull null

            val angle = bandIndex * clusterAngleStep
            val radius = input.viewportRadius * band.radius

            VisibleCluster(
                clusterId = CLUSTER_ID_BASE + band.ordinal,
                band = band,
                memberIds = members.map { it.first },
                count = members.size,
                radius = radius,
                angle = angle
            )
        }
    }

    // ── Continuous orbit radius ──

    /**
     * Continuous orbit radius from attention score.
     *
     * score 1.0 → innerRadius (closest to sun)
     * score 0.0 → outerRadius (farthest)
     *
     * radius = innerRadius + (outerRadius - innerRadius) * (1 - score)
     */
    fun orbitRadius(score: Float, viewportRadius: Float): Float {
        val inner = viewportRadius * INNER_RADIUS_FRAC
        val outer = viewportRadius * OUTER_RADIUS_FRAC
        return inner + (outer - inner) * (1f - score)
    }

    // ── Deterministic angle ──

    /**
     * Deterministic angle for a task — delegates to [GoalGraphBuilder] geometry.
     */
    fun deterministicAngle(id: Int, totalCount: Int): Float =
        GoalGraphBuilder.deterministicAngle(id, totalCount)

    // ── Orbit bands ──

    /**
     * Build the visual reference orbit bands for the renderer.
     *
     * These are visual guides only — individual task radii are continuous,
     * not snapped to these bands.
     */
    private fun buildOrbitBands(viewportRadius: Float): List<OrbitBand> {
        return AttentionBand.values().map { band ->
            OrbitBand(
                radius = viewportRadius * band.radius,
                band = band
            )
        }
    }
}

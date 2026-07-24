package com.example.domain.graph

import com.example.domain.attention.AttentionReason

/**
 * Pure-Kotlin visibility models for the Behavioral Solar System (Phase 2B).
 *
 * These models separate *what is visible* (VisibilityResolver) from *why a task
 * needs attention* (AttentionCalculator). The renderer consumes [VisibleGraphModel]
 * only — it never queries DAOs or makes visibility decisions.
 *
 * No Android / Room / Compose imports.
 */

/**
 * Progressive disclosure levels for the Solar System.
 *
 * - [OVERVIEW]: 3–7 highest-attention tasks, individual rendering.
 * - [EXPANDED]: more tasks revealed (up to 20), still individual.
 * - [INSIGHT]: clusters for >20 tasks (attention-band aggregation).
 */
enum class VisibilityLevel {
    OVERVIEW,
    EXPANDED,
    INSIGHT
}

/**
 * Attention bands used for clustering aggregation only.
 *
 * Bands are NOT fixed orbit positions — individual task radii are computed
 * continuously from [VisibleTask.attentionScore]. Bands only group tasks
 * into clusters when the collection is too large for individual rendering.
 *
 * @param range inclusive score range [0, 1]
 * @param label human-readable label for the cluster
 * @param radius fraction of viewport radius for the cluster's visual position
 * @param colorRole UI color mapping (domain stays color-free via enum)
 */
enum class AttentionBand(
    val range: ClosedFloatingPointRange<Float>,
    val label: String,
    val radius: Float,
    val colorRole: ColorRole
) {
    HIGH(0.5f..1.0f, "High Attention", 0.34f, ColorRole.HIGH),
    MEDIUM(0.2f..0.5f, "Medium Attention", 0.58f, ColorRole.MEDIUM),
    LOW(0.0f..0.2f, "Low Attention", 0.82f, ColorRole.LOW)
}

/**
 * Input to [VisibilityResolver.resolve].
 *
 * Contains everything the resolver needs to decide what is visible:
 * - active (incomplete) tasks with their attention results
 * - the requested visibility level
 * - viewport geometry
 * - clock for overdue/near-deadline computation
 *
 * Completed tasks are excluded upstream (completion gate in ViewModel).
 */
data class VisibilityInput(
    /** Active task ids in stable order (e.g. by id). */
    val activeTaskIds: List<Int>,
    /** taskId → AttentionResult for active tasks. */
    val attentionResults: Map<Int, com.example.domain.attention.AttentionResult>,
    /** Active task metadata needed for overdue/near-deadline flags. */
    val taskMetadata: Map<Int, TaskMetadata>,
    /** Requested progressive-disclosure level. */
    val level: VisibilityLevel = VisibilityLevel.OVERVIEW,
    /** Design-space viewport radius (default 320f). */
    val viewportRadius: Float = 320f,
    /** Clock source for overdue/near-deadline computation. */
    val nowMillis: Long = System.currentTimeMillis()
) {
    /** Number of active tasks. */
    val activeTaskCount: Int get() = activeTaskIds.size
}

/**
 * Minimal task metadata for visibility decisions (overdue, near-deadline, boulder).
 *
 * Pure Kotlin — no Android/Room types.
 */
data class TaskMetadata(
    val id: Int,
    val title: String,
    val priority: String?,
    val dateEpochMs: Long?,
    val deadlineEpochMs: Long?,
    val rescheduleCount: Int
)

/**
 * The resolved output of [VisibilityResolver].
 *
 * Contains everything the renderer needs to draw the Solar System at the
 * requested [level]. The renderer is a pure consumer — it never makes
 * visibility decisions.
 *
 * @param level the visibility level this model was resolved for
 * @param tasks visible tasks (individual satellites) at this level
 * @param clusters visible clusters (for [VisibilityLevel.INSIGHT])
 * @param hiddenCount tasks not shown at this level (for "N more" hints)
 * @param orbitBands reference band radii for visual orbit rings
 * @param viewportRadius design-space viewport radius
 * @param centerX center X (viewportRadius)
 * @param centerY center Y (viewportRadius)
 */
data class VisibleGraphModel(
    val level: VisibilityLevel,
    val tasks: List<VisibleTask>,
    val clusters: List<VisibleCluster>,
    val hiddenCount: Int,
    val orbitBands: List<OrbitBand>,
    val viewportRadius: Float = 320f,
    val centerX: Float = viewportRadius,
    val centerY: Float = viewportRadius
) {
    /** True when this model uses clusters instead of individual tasks. */
    val isClustered: Boolean get() = clusters.isNotEmpty()
}

/**
 * A single visible task (satellite) in the Solar System.
 *
 * @param taskId task id
 * @param attentionScore [0, 1] — higher = closer to sun
 * @param radius continuous orbit radius in design-space pixels
 * @param angle deterministic angle in radians (0 = +x axis)
 * @param reasons attention explainability reasons
 * @param isBoulder true when rescheduleCount >= threshold
 * @param isOverdue true when scheduled date is in the past
 * @param isNearDeadline true when due within 24h window
 * @param priority task priority (for renderer styling)
 * @param title task title (for labels)
 */
data class VisibleTask(
    val taskId: Int,
    val attentionScore: Float,
    val radius: Float,
    val angle: Float,
    val reasons: List<AttentionReason>,
    val isBoulder: Boolean,
    val isOverdue: Boolean,
    val isNearDeadline: Boolean,
    val priority: String?,
    val title: String
)

/**
 * A cluster summarizing multiple tasks of one attention band.
 *
 * Used when the active task count exceeds the individual-rendering threshold.
 *
 * @param clusterId stable synthetic id
 * @param band which attention band this cluster represents
 * @param memberIds task ids belonging to this cluster
 * @param count number of tasks summarized
 * @param radius design-space radius for the cluster's position
 * @param angle deterministic angle for the cluster's position
 */
data class VisibleCluster(
    val clusterId: Int,
    val band: AttentionBand,
    val memberIds: List<Int>,
    val count: Int,
    val radius: Float,
    val angle: Float
)

/**
 * A visual reference orbit band drawn by the renderer.
 *
 * Bands are visual guides only — individual task radii are continuous, not
 * snapped to these bands.
 *
 * @param radius fraction of viewport radius
 * @param band the attention band this reference represents (null for the sun)
 */
data class OrbitBand(
    val radius: Float,
    val band: AttentionBand?
)

/** Synthetic id base for clusters so they never collide with real task ids. */
const val CLUSTER_ID_BASE = 1_000_000

/** Boulder threshold — reschedule count at/above which a task is flagged. */
const val BOULDER_RESCHEDULE_THRESHOLD = 2

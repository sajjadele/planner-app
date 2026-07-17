package com.example.domain.graph

/**
 * Pure-Kotlin domain models for the "Behavioral Solar System" graph view.
 *
 * No Android / Room / Compose imports — this package is host-JVM testable and is the single
 * source of truth for graph geometry. UI renders these models; it never queries DAOs directly
 * (per ADR-0002 / ADR-0004). Coordinates are design-space pixels relative to a square viewport
 * whose center is (viewportRadius, viewportRadius).
 */

/** A node in the Behavioral Solar System. (cx, cy) are absolute canvas coords in design space. */
data class GoalGraphNode(
    val id: Int,
    val label: String,
    val kind: NodeKind,
    val cx: Float,
    val cy: Float,
    /** Visual radius. For tasks this encodes priority; for the goal node it is the sun size. */
    val size: Float,
    /** 1f = fully active, <1f = faded (e.g. completed "memory" points). */
    val alpha: Float,
    /** "HIGH" | "MEDIUM" | "LOW" | null. */
    val priority: String?,
    /** True when the task is a "Boulder" (rescheduled >= threshold) — drives the wobble halo. */
    val isBoulder: Boolean,
    val isCompleted: Boolean,
    /** Drives UI tint without leaking android.graphics.Color into the domain layer. */
    val colorRole: ColorRole
)

enum class NodeKind { GOAL, TASK }

enum class ColorRole { GOAL, HIGH, MEDIUM, LOW, COMPLETED, BOULDER }

/**
 * Rendering mode for the Behavioral Solar System. Decided by [com.example.domain.graph.GoalGraphBuilder]
 * from the active-task count against [com.example.domain.graph.GoalGraphBuilder.MAX_VISIBLE_TASKS].
 * - INDIVIDUAL: every active task is drawn as its own satellite (small goals).
 * - CLUSTER: tasks are summarized into priority/completion clusters (large goals).
 */
enum class GraphMode { INDIVIDUAL, CLUSTER }

/**
 * The four stable clusters an adaptive graph can show. Each maps to a priority lane (or the
 * completed memory ring). Pure domain concept — UI maps to colors.
 */
enum class ClusterType { ACTIVE_HIGH, ACTIVE_MEDIUM, ACTIVE_LOW, COMPLETED }

/**
 * Gravity line from the central goal node to a satellite task. [alpha] encodes connection
 * strength (1f active, faded for completed "memory" points).
 */
data class GoalGraphEdge(
    val fromId: Int,
    val toId: Int,
    val alpha: Float
)

/**
 * A fully computed Behavioral Solar System for a single goal.
 *
 * @param centerX center of the viewport (== viewportRadius)
 * @param centerY center of the viewport (== viewportRadius)
 * @param viewportRadius design-space radius; max orbit band sits just inside this
 * @param goalProgressOverall weighted progress 0..100 — drives the central "Ring Tide" glow
 * @param nodes goal sun + task satellites (in CLUSTER mode these are still computed for expansion;
 *              the renderer decides which to draw based on [mode]/expanded cluster)
 * @param edges goal→task or goal→cluster gravity lines (depends on [mode])
 * @param mode INDIVIDUAL (every task shown) or CLUSTER (tasks summarized into [clusters])
 * @param clusters task clusters shown in CLUSTER mode (empty in INDIVIDUAL mode)
 */
data class GoalGraph(
    val centerX: Float,
    val centerY: Float,
    val viewportRadius: Float,
    val goalProgressOverall: Float,
    val nodes: List<GoalGraphNode>,
    val edges: List<GoalGraphEdge>,
    val mode: GraphMode = GraphMode.INDIVIDUAL,
    val clusters: List<TaskClusterNode> = emptyList()
)

/**
 * A summary node representing multiple tasks of one [ClusterType] in the adaptive (CLUSTER) view.
 *
 * Pure-Kotlin, no Android/Compose imports. Carries its [memberIds] so the renderer can expand the
 * cluster in-view without altering [GoalGraphNode]. Position is deterministic (computed by the
 * builder from the cluster's lane radius + a fixed angle).
 *
 * @param id stable synthetic id (clusterType.ordinal + CLUSTER_ID_BASE)
 * @param clusterType which group this summarizes
 * @param taskCount number of tasks summarized
 * @param cx absolute design-space x
 * @param cy absolute design-space y
 * @param visualSize design-space radius — scales with [taskCount] but is clamped so it never
 *                   competes with the sun
 * @param priorityLevel "HIGH" | "MEDIUM" | "LOW" | null (COMPLETED)
 * @param memberIds task ids belonging to this cluster (used for in-view expansion)
 */
data class TaskClusterNode(
    val id: Int,
    val clusterType: ClusterType,
    val taskCount: Int,
    val cx: Float,
    val cy: Float,
    val visualSize: Float,
    val priorityLevel: String?,
    val memberIds: List<Int>
)

/**
 * Deterministic polar→cartesian projection utility. Pure math only.
 */
object GraphGeometry {
    const val TWO_PI: Float = 2f * PI_F

    /** Project a point at [radius] and [angleRad] (radians, 0 = +x axis) around (cx, cy). */
    fun project(cx: Float, cy: Float, radius: Float, angleRad: Float): Pair<Float, Float> =
        (cx + radius * kotlin.math.cos(angleRad)) to (cy + radius * kotlin.math.sin(angleRad))

    /** Euclidean distance between two points. */
    fun distance(ax: Float, ay: Float, bx: Float, by: Float): Float =
        kotlin.math.hypot(ax - bx, ay - by)
}

/** PI as a Float constant (const-valid literal). */
private const val PI_F = 3.1415927f

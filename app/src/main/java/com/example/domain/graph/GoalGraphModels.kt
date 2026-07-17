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
 * @param nodes goal sun + task satellites
 * @param edges goal→task gravity lines
 */
data class GoalGraph(
    val centerX: Float,
    val centerY: Float,
    val viewportRadius: Float,
    val goalProgressOverall: Float,
    val nodes: List<GoalGraphNode>,
    val edges: List<GoalGraphEdge>
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

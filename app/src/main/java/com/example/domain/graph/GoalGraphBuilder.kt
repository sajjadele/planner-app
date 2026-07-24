package com.example.domain.graph

import com.example.domain.goal.GoalProgress
import kotlin.math.PI

/**
 * Geometry utilities for the Behavioral Solar System (Phase 2B+ migration).
 *
 * Visibility decisions (what is shown, clustering, overview limits) live exclusively in
 * [VisibilityResolver]. This builder only:
 * - places the sun (goal) node
 * - projects [VisibleTask]/[VisibleCluster] polar data into absolute design-space coordinates
 * - exposes deterministic [jitter] / angle helpers
 *
 * Pure Kotlin, no Android/Room imports.
 */
object GoalGraphBuilder {

    const val GOAL_SIZE = 40f
    const val DEFAULT_VIEWPORT_RADIUS = 320f

    /** Window (ms) for near-deadline flag (shared with VisibilityResolver). */
    const val NEAR_DEADLINE_WINDOW_MS = VisibilityResolver.NEAR_DEADLINE_WINDOW_MS

    /**
     * Build sun-only scaffolding for Ring Tide / progress (goal metadata).
     * Task satellites come from [VisibleGraphModel], not from this method.
     */
    fun buildSun(
        goalId: Int,
        goalTitle: String,
        progress: GoalProgress?,
        viewportRadius: Float = DEFAULT_VIEWPORT_RADIUS
    ): GoalGraph {
        val cx = viewportRadius
        val cy = viewportRadius
        val sun = GoalGraphNode(
            id = goalId,
            label = goalTitle,
            kind = NodeKind.GOAL,
            cx = cx,
            cy = cy,
            size = GOAL_SIZE,
            alpha = 1f,
            priority = null,
            isBoulder = false,
            isCompleted = false,
            colorRole = ColorRole.GOAL
        )
        return GoalGraph(
            centerX = cx,
            centerY = cy,
            viewportRadius = viewportRadius,
            goalProgressOverall = progress?.overall ?: 0f,
            nodes = listOf(sun),
            edges = emptyList(),
            densityMode = GraphDensityMode.SIMPLE,
            clusters = emptyList()
        )
    }

    /**
     * Project a [VisibleTask] into absolute design-space (cx, cy).
     * Radius and angle are already decided by [VisibilityResolver].
     */
    fun projectTask(
        task: VisibleTask,
        centerX: Float,
        centerY: Float
    ): Pair<Float, Float> = GraphGeometry.project(centerX, centerY, task.radius, task.angle)

    /**
     * Project a [VisibleCluster] into absolute design-space (cx, cy).
     */
    fun projectCluster(
        cluster: VisibleCluster,
        centerX: Float,
        centerY: Float
    ): Pair<Float, Float> = GraphGeometry.project(centerX, centerY, cluster.radius, cluster.angle)

    /**
     * Deterministic tiny angle offset from task id (no Random).
     * Returns a value in [0, 0.25) radians.
     */
    fun jitter(id: Int): Float = ((id * 92821) % 1000) / 1000f * 0.25f

    /**
     * Deterministic angle for even distribution + jitter.
     */
    fun deterministicAngle(id: Int, totalCount: Int): Float {
        if (totalCount <= 0) return 0f
        if (totalCount == 1) return PI.toFloat() / 2f
        val baseAngle = (id % totalCount).toFloat() / totalCount * GraphGeometry.TWO_PI
        return baseAngle + jitter(id)
    }

    /**
     * Minimal task projection kept for tests / adapters.
     * Visibility no longer reads priority for layout decisions.
     */
    data class TaskInput(
        val id: Int,
        val title: String,
        val priority: String? = null,
        val isCompleted: Boolean = false,
        val dateEpochMs: Long? = null,
        val attentionScore: Float? = null
    )
}

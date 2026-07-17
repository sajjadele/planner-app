package com.example.domain.graph

import com.example.domain.goal.GoalProgress
import kotlin.math.PI

/**
 * Deterministic builder for the Behavioral Solar System graph (ADR-0004).
 *
 * Pure Kotlin, no Android/Room imports. Given a goal, its tasks, per-task reschedule counts,
 * and the goal's progress, it computes a stable polar layout:
 *
 * - Tasks are grouped into three concentric priority lanes (HIGH inner, MEDIUM middle, LOW outer).
 * - Within a lane, nodes are spread evenly around the circle; a deterministic id-based jitter
 *   de-aligns lanes so rings don't overlap radially. No [kotlin.random.Random] — same inputs
 *   always yield identical coordinates.
 * - Completed tasks become tiny, faded "memory" points on the outer edge ring.
 * - A "Boulder" (rescheduleCount >= [BOULDER_RESCHEDULE_THRESHOLD]) is flagged for the UI wobble.
 * - [GoalProgress.overall] is surfaced for the central "Ring Tide" glow.
 *
 * Urgency-by-deadline radius mapping is intentionally deferred to V2 (keep core orbits stable).
 */
object GoalGraphBuilder {

    /** Lane radii as fractions of [GoalGraph.viewportRadius]; inner = more urgent/important. */
    private val LANE_FRACTION = mapOf(
        "HIGH" to 0.34f,
        "MEDIUM" to 0.58f,
        "LOW" to 0.82f
    )
    /** Active tasks with no priority fall back to the outer LOW lane. */
    private const val NO_PRIORITY_LANE = "LOW"
    /** Undated active tasks without a goal priority are pushed to the very outer active ring. */
    private const val NO_DATE_FRACTION = 0.92f
    /** Completed tasks orbit on the faded outer edge. */
    private const val COMPLETED_FRACTION = 0.97f

    /** Reschedule count at/above which a task is flagged as a Boulder (matches MirrorHeuristics). */
    private const val BOULDER_RESCHEDULE_THRESHOLD = 2

    /** Task node radius by priority (design-space px at the default viewport). */
    private val PRIORITY_SIZE = mapOf(
        "HIGH" to 22f,
        "MEDIUM" to 16f,
        "LOW" to 12f
    )
    private const val DEFAULT_TASK_SIZE = 14f
    private const val GOAL_SIZE = 40f

    /**
     * @param goalId central goal id
     * @param goalTitle label for the sun
     * @param tasks task list already scoped to this goal
     * @param rescheduleCounts map of taskId -> reschedule event count
     * @param progress goal progress (overall drives Ring Tide); null-safe
     * @param viewportRadius design-space radius (default 320f)
     * @param nowMillis clock source for any future deadline math
     */
    fun build(
        goalId: Int,
        goalTitle: String,
        tasks: List<TaskInput>,
        rescheduleCounts: Map<Int, Int>,
        progress: GoalProgress?,
        viewportRadius: Float = 320f,
        @Suppress("UNUSED_PARAMETER") nowMillis: Long = System.currentTimeMillis()
    ): GoalGraph {
        val cx = viewportRadius
        val cy = viewportRadius
        val nodes = mutableListOf<GoalGraphNode>()
        val edges = mutableListOf<GoalGraphEdge>()

        // ── Sun (central Goal node) ──
        nodes += GoalGraphNode(
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

        val active = tasks.filter { !it.isCompleted }
        val completed = tasks.filter { it.isCompleted }

        // Group active tasks by lane (priority), defaulting null priority to the outer lane.
        val byLane = active.groupBy { it.priority ?: NO_PRIORITY_LANE }

        fun placeLane(laneTasks: List<TaskInput>, baseFraction: Float) {
            val n = laneTasks.size
            laneTasks.forEachIndexed { i, task ->
                val angle = if (n == 1) PI.toFloat() / 2f
                else (i.toFloat() / n) * GraphGeometry.TWO_PI + jitter(task.id)
                val r = viewportRadius * baseFraction
                val (x, y) = GraphGeometry.project(cx, cy, r, angle)
                val isBoulder = (rescheduleCounts[task.id] ?: 0) >= BOULDER_RESCHEDULE_THRESHOLD
                nodes += GoalGraphNode(
                    id = task.id,
                    label = task.title,
                    kind = NodeKind.TASK,
                    cx = x,
                    cy = y,
                    size = PRIORITY_SIZE[task.priority] ?: DEFAULT_TASK_SIZE,
                    alpha = 1f,
                    priority = task.priority,
                    isBoulder = isBoulder,
                    isCompleted = false,
                    colorRole = colorFor(task.priority, isBoulder)
                )
                edges += GoalGraphEdge(goalId, task.id, 1f)
            }
        }

        placeLane(byLane["HIGH"] ?: emptyList(), LANE_FRACTION["HIGH"]!!)
        placeLane(byLane["MEDIUM"] ?: emptyList(), LANE_FRACTION["MEDIUM"]!!)
        placeLane(byLane["LOW"] ?: emptyList(), LANE_FRACTION["LOW"]!!)

        // Undated active tasks without a priority are pushed to the outermost active ring so they
        // read as "far / low gravity" rather than cluttering a dated lane.
        val undated = active.filter { it.deadlineEpochMs == null && it.priority == null }
        placeLane(undated, NO_DATE_FRACTION)

        // ── Completed tasks: faded memory points on the outer edge ──
        val nc = completed.size
        completed.forEachIndexed { i, task ->
            val angle = if (nc == 1) 0f else (i.toFloat() / nc) * GraphGeometry.TWO_PI
            val r = viewportRadius * COMPLETED_FRACTION
            val (x, y) = GraphGeometry.project(cx, cy, r, angle)
            nodes += GoalGraphNode(
                id = task.id,
                label = task.title,
                kind = NodeKind.TASK,
                cx = x,
                cy = y,
                size = 6f,
                alpha = 0.28f,
                priority = task.priority,
                isBoulder = false,
                isCompleted = true,
                colorRole = ColorRole.COMPLETED
            )
            edges += GoalGraphEdge(goalId, task.id, 0.28f)
        }

        return GoalGraph(
            centerX = cx,
            centerY = cy,
            viewportRadius = viewportRadius,
            goalProgressOverall = progress?.overall ?: 0f,
            nodes = nodes,
            edges = edges
        )
    }

    /**
     * Deterministic tiny angle offset derived from the task id (no Random), so adjacent lanes
     * don't line up radially. Returns a value in [0, 0.25) radians.
     */
    private fun jitter(id: Int): Float = ((id * 92821) % 1000) / 1000f * 0.25f

    private fun colorFor(priority: String?, isBoulder: Boolean): ColorRole =
        if (isBoulder) ColorRole.BOULDER
        else when (priority) {
            "HIGH" -> ColorRole.HIGH
            "MEDIUM" -> ColorRole.MEDIUM
            "LOW" -> ColorRole.LOW
            else -> ColorRole.LOW
        }

    /**
     * Minimal task projection the builder consumes. Keeps [com.example.domain.graph] free of the
     * Android-typed TaskEntity (which lives under plugins.planner.data), protecting the domain
     * boundary. Deadline is retained for the deferred V2 urgency-radius mapping.
     */
    data class TaskInput(
        val id: Int,
        val title: String,
        val priority: String?,
        val isCompleted: Boolean,
        val deadlineEpochMs: Long?
    )
}

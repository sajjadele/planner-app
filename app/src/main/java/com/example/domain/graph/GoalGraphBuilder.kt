package com.example.domain.graph

import com.example.domain.goal.GoalProgress
import kotlin.math.PI
import kotlin.math.min

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

    /**
     * Adaptive threshold (Phase 6.3). When the number of *active* tasks exceeds this, the graph
     * renders priority/completion CLUSTERS instead of individual satellites, so a large goal
     * communicates health in under 2 seconds. Single tunable constant — raise/lower to change
     * when the overview kicks in.
     */
    const val MAX_VISIBLE_TASKS = 8

    /** Synthetic id base for clusters so they never collide with real task/goal ids. */
    private const val CLUSTER_ID_BASE = 1_000_000

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

    /** Priority ranking for displayed-task ordering (§4): HIGH first, then MEDIUM, then LOW/null. */
    private val PRIORITY_RANK = mapOf("HIGH" to 0, "MEDIUM" to 1, "LOW" to 2, null to 3)

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

        // Adaptive mode decision (Phase 6.3): overview clusters kick in once active tasks exceed
        // the tunable threshold. Individual nodes are always computed (needed for expansion and
        // for INDIVIDUAL mode), so the layout stays deterministic either way.
        val mode = if (active.size > MAX_VISIBLE_TASKS) GraphMode.CLUSTER else GraphMode.INDIVIDUAL

        // Group active tasks by lane (priority). IMPORTANT: keep null-priority tasks OUT of the
        // LOW lane — they are placed exactly once on the dedicated outer "undated" ring below.
        // (Mapping null→LOW here previously double-placed null-priority tasks, causing the
        // "2 tasks render as 4 circles" duplicate-node bug.)
        val byLane = active.groupBy { it.priority }

        // Deterministic, priority-ordered placement (§4): most meaningful tasks first.
        // Order within the active set: HIGH → MEDIUM → LOW → (no priority). This only affects the
        // angle at which each task lands; all tasks ≤ MAX_VISIBLE_TASKS are still shown.
        val orderedActive = active.sortedWith(
            compareBy({ PRIORITY_RANK[it.priority] ?: 3 }, { it.id })
        )

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
                if (mode == GraphMode.INDIVIDUAL) edges += GoalGraphEdge(goalId, task.id, 1f)
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
            if (mode == GraphMode.INDIVIDUAL) edges += GoalGraphEdge(goalId, task.id, 0.28f)
        }

        // ── Adaptive clusters (only meaningful in CLUSTER mode) ──
        val clusters = if (mode == GraphMode.CLUSTER) buildClusters(
            cx = cx, cy = cy, viewportRadius = viewportRadius,
            active = active, completed = completed, goalId = goalId, edges = edges
        ) else emptyList()

        return GoalGraph(
            centerX = cx,
            centerY = cy,
            viewportRadius = viewportRadius,
            goalProgressOverall = progress?.overall ?: 0f,
            nodes = nodes,
            edges = edges,
            mode = mode,
            clusters = clusters
        )
    }

    /**
     * Build the four stable clusters (ACTIVE_HIGH / ACTIVE_MEDIUM / ACTIVE_LOW / COMPLETED) for the
     * adaptive overview. Positions are deterministic: each cluster sits on its lane radius at a
     * fixed angle (no Random). [visualSize] grows with task count but is clamped so a cluster never
     * visually competes with the sun. Sun→cluster edges are appended to [edges].
     */
    private fun buildClusters(
        cx: Float,
        cy: Float,
        viewportRadius: Float,
        active: List<TaskInput>,
        completed: List<TaskInput>,
        goalId: Int,
        edges: MutableList<GoalGraphEdge>
    ): List<TaskClusterNode> {
        val specs = listOf(
            ClusterSpec(ClusterType.ACTIVE_HIGH, "HIGH", 0f, 0.34f),
            ClusterSpec(ClusterType.ACTIVE_MEDIUM, "MEDIUM", 2f * PI.toFloat() / 3f, 0.58f),
            ClusterSpec(ClusterType.ACTIVE_LOW, "LOW", 4f * PI.toFloat() / 3f, 0.82f),
            ClusterSpec(ClusterType.COMPLETED, null, PI.toFloat(), 0.95f)
        )
        return specs.mapNotNull { spec ->
            val members = when (spec.clusterType) {
                ClusterType.COMPLETED -> completed
                else -> active.filter { (it.priority ?: NO_PRIORITY_LANE) == spec.priority }
            }
            if (members.isEmpty()) return@mapNotNull null
            val (x, y) = GraphGeometry.project(cx, cy, viewportRadius * spec.fraction, spec.angle)
            // Clamp size: 20..34 design px, scaled by count but always < GOAL_SIZE (40).
            val size = (20f + minOf(members.size, 20) * 0.7f).coerceAtMost(34f)
            edges += GoalGraphEdge(goalId, CLUSTER_ID_BASE + spec.clusterType.ordinal, 0.6f)
            TaskClusterNode(
                id = CLUSTER_ID_BASE + spec.clusterType.ordinal,
                clusterType = spec.clusterType,
                taskCount = members.size,
                cx = x,
                cy = y,
                visualSize = size,
                priorityLevel = spec.priority,
                memberIds = members.map { it.id }
            )
        }
    }

    private data class ClusterSpec(
        val clusterType: ClusterType,
        val priority: String?,
        val angle: Float,
        val fraction: Float
    )

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

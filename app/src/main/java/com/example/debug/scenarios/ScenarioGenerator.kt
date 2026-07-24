package com.example.debug.scenarios

import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalEventEntity
import com.example.core.goal.GoalRepository
import com.example.core.snapshot.SnapshotAggregator
import com.example.plugins.notes.data.NoteEntity
import com.example.plugins.notes.data.NoteRepository
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskEventEntity
import com.example.plugins.planner.data.TaskRepository
import kotlinx.coroutines.flow.first

/**
 * Debug-only scenario generator for Developer Lab testing.
 *
 * Generates realistic multi-day user behavior histories that exercise
 * AttentionCalculator, VisibilityResolver, and MirrorEngine through
 * the normal data pipeline.
 *
 * Each scenario creates a Goal + Tasks + TaskEvents + Notes with timestamps
 * spread across a realistic timeline.
 *
 * After generation, calls SnapshotAggregator.backfillMissing() to
 * ensure behavior snapshots are available for Mirror evaluation.
 */
class ScenarioGenerator(
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val snapshotAggregator: SnapshotAggregator
) {

    /**
     * Generate a realistic user behavior scenario.
     *
     * @param type which persona to generate
     * @return result containing goalId, taskIds, and description
     */
    suspend fun generate(type: ScenarioType): GeneratedScenarioResult {
        return when (type) {
            ScenarioType.PRODUCTIVE_USER -> generateProductiveUser()
            ScenarioType.PROCRASTINATOR_USER -> generateProcrastinatorUser()
            ScenarioType.CHAOS_USER -> generateChaosUser()
            ScenarioType.RECOVERY_USER -> generateRecoveryUser()
        }
    }

    /**
     * Clear all generated debug data (goals/tasks/events/notes with "🧪 Lab" prefix).
     */
    suspend fun clearGeneratedScenarios() {
        val allGoals = goalRepository.getAllGoals().first()
        val labGoals = allGoals.filter { it.title.startsWith(GOAL_PREFIX) }

        labGoals.forEach { goal ->
            // Delete notes for goal's tasks first
            val tasks = taskRepository.getTasksByGoalId(goal.id).first()
            tasks.forEach { task ->
                val notes = noteRepository.getNotesByTaskId(task.id).first()
                notes.forEach { noteRepository.delete(it) }
            }
            taskRepository.deleteEventsForGoal(goal.id)
            taskRepository.deleteTasksForGoal(goal.id)
            goalRepository.deleteGoal(goal)
        }

        // Clear stale behavior snapshots to prevent contaminated signal detection
        snapshotAggregator.clearAllSnapshots()

        snapshotAggregator.backfillMissing()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Scenario A: Productive User
    //
    // Behavioral pattern:
    // - Tasks created gradually over 30 days
    // - Most tasks have realistic deadlines
    // - Completed tasks have deadlines before completion
    // - No overdue active tasks
    // - Recent activity ensures no GoalAttention signal
    //
    // Expected outcome:
    // - Low attention scores (0.0-0.2)
    // - No Boulder signals
    // - No GoalAttention signal
    // - Calm solar system
    // ═══════════════════════════════════════════════════════════════════════

    private suspend fun generateProductiveUser(): GeneratedScenarioResult {
        val goalId = createGoal("Learn Kotlin", ageDays = 30)
        val taskIds = mutableListOf<Int>()

        // 18 completed tasks (spread across days -28 to -3)
        // Pattern: created → note added → completed within deadline
        for (i in 1..18) {
            val createdDaysAgo = 28 - (i % 18)
            val deadlineDaysAgo = createdDaysAgo - 5 // Deadline 5 days after creation
            val completedDaysAgo = createdDaysAgo - 3 // Complete 3 days after creation (before deadline)

            val taskId = createTask(
                goalId = goalId,
                title = "Learn Topic $i",
                createdDaysAgo = createdDaysAgo,
                scheduledDaysAgo = createdDaysAgo,
                deadlineDaysAgo = deadlineDaysAgo
            )
            taskIds.add(taskId)

            // Created event
            insertEvent(taskId, "created", createdDaysAgo)

            // Add meaningful interaction (note) 1 day after creation
            insertNote(taskId, "Started working on topic $i", createdDaysAgo - 1)

            // Completed event (before deadline)
            insertEvent(taskId, "completed", completedDaysAgo)

            // Mark task as completed
            val task = taskRepository.getTasksByGoalId(goalId).first()
                .first { it.id == taskId }
            taskRepository.updateTask(task.copy(isCompleted = true))
        }

        // 2 active tasks (created recently, not completed)
        // These have future deadlines and recent interactions
        for (i in 19..20) {
            val taskId = createTask(
                goalId = goalId,
                title = "Active Task $i",
                createdDaysAgo = 5,
                scheduledDaysAgo = 2,
                deadlineDaysAgo = -3 // Deadline 3 days from now
            )
            taskIds.add(taskId)
            insertEvent(taskId, "created", 5)
            // Add recent interaction (note) 1 day ago
            insertNote(taskId, "Making progress on task $i", 1)
        }

        // Add recent goal activity to prevent GoalAttention signal
        // This ensures the last goal event is within 14 days
        goalRepository.insertGoalEvent(
            GoalEventEntity(
                goalId = goalId,
                eventType = "completed",
                timestamp = DebugDateUtils.timestampDaysAgo(2)
            )
        )

        snapshotAggregator.backfillMissing()

        return GeneratedScenarioResult(
            goalId = goalId,
            scenarioType = ScenarioType.PRODUCTIVE_USER,
            createdTaskIds = taskIds,
            description = "Productive user: 18 completed, 2 active, no reschedules. " +
                    "Expected: low attention, no boulders, calm solar system."
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Scenario B: Procrastinator User
    //
    // Behavioral pattern:
    // - Tasks created with deadlines that are frequently missed
    // - Many reschedule events
    // - No meaningful interactions (no notes)
    // - Overdue tasks with high avoidance
    //
    // Expected outcome:
    // - High attention scores (0.5-0.9)
    // - Multiple Boulder signals
    // - GoalAttention signal (old goal, no recent activity)
    // - InitiatorFinisher signal
    // ═══════════════════════════════════════════════════════════════════════

    private suspend fun generateProcrastinatorUser(): GeneratedScenarioResult {
        val goalId = createGoal("Launch App", ageDays = 30)
        val taskIds = mutableListOf<Int>()

        // 3 completed tasks (recent, with deadlines met)
        for (i in 1..3) {
            val taskId = createTask(
                goalId = goalId,
                title = "Quick Win $i",
                createdDaysAgo = 25,
                scheduledDaysAgo = 20,
                deadlineDaysAgo = 18
            )
            taskIds.add(taskId)
            insertEvent(taskId, "created", 25)
            insertEvent(taskId, "completed", 5 - i)

            val task = taskRepository.getTasksByGoalId(goalId).first()
                .first { it.id == taskId }
            taskRepository.updateTask(task.copy(isCompleted = true))
        }

        // 17 active tasks with reschedules (creating boulders and overdue)
        // Pattern: created → deadline missed → rescheduled multiple times → still active
        val procrastinatorTasks = listOf(
            // Heavy procrastination (3+ reschedules, old, overdue)
            // Created 25 days ago, deadline was 15 days ago, rescheduled 3 times
            ProcrastinatorTaskSpec("Critical Feature", 25, 15, listOf(20, 15, 7)),

            // Light procrastination (1 reschedule - won't qualify as Boulder)
            ProcrastinatorTaskSpec("Core Module", 22, 12, listOf(18)),

            // Light procrastination (1 reschedule each - won't qualify as Boulder)
            ProcrastinatorTaskSpec("Main Task", 20, 10, listOf(15)),
            ProcrastinatorTaskSpec("API Integration", 18, 8, listOf(12)),
            ProcrastinatorTaskSpec("UI Component", 16, 6, listOf(10)),
            ProcrastinatorTaskSpec("Database Layer", 15, 5, listOf(9)),

            // Light procrastination (1 reschedule)
            ProcrastinatorTaskSpec("Testing", 14, 4, listOf(8)),
            ProcrastinatorTaskSpec("Documentation", 13, 3, listOf(7)),
            ProcrastinatorTaskSpec("Refactoring", 12, 2, listOf(6)),

            // Very recent procrastination
            ProcrastinatorTaskSpec("Bug Fix", 10, 1, listOf(5)),
            ProcrastinatorTaskSpec("Feature A", 9, 0, listOf(4)),
            ProcrastinatorTaskSpec("Feature B", 8, -1, listOf(3)),

            // Recently created, no reschedule yet (deadline approaching)
            ProcrastinatorTaskSpec("Task New 1", 5, -2, emptyList()),
            ProcrastinatorTaskSpec("Task New 2", 4, -3, emptyList()),
            ProcrastinatorTaskSpec("Task New 3", 3, -4, emptyList()),
            ProcrastinatorTaskSpec("Task New 4", 2, -5, emptyList()),
            ProcrastinatorTaskSpec("Task New 5", 1, -6, emptyList())
        )

        for (spec in procrastinatorTasks) {
            val taskId = createTask(
                goalId = goalId,
                title = spec.title,
                createdDaysAgo = spec.createdDaysAgo,
                scheduledDaysAgo = spec.createdDaysAgo - 3,
                deadlineDaysAgo = spec.deadlineDaysAgo
            )
            taskIds.add(taskId)

            // Created event
            insertEvent(taskId, "created", spec.createdDaysAgo)

            // Reschedule events (no notes = high staleness)
            for (rescheduleDay in spec.rescheduleDays) {
                insertEvent(taskId, "rescheduled", rescheduleDay)
            }
        }

        snapshotAggregator.backfillMissing()

        return GeneratedScenarioResult(
            goalId = goalId,
            scenarioType = ScenarioType.PROCRASTINATOR_USER,
            createdTaskIds = taskIds,
            description = "Procrastinator: 3 completed, 17 active with reschedules. " +
                    "Expected: high attention, boulders, overdue, Mirror signals."
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Scenario C: Chaos User
    //
    // Behavioral pattern:
    // - Mixed task states (completed, stale, overdue, rescheduled, recent)
    // - Varied deadlines (some met, some missed, some future)
    // - Mixed interaction patterns (some tasks have notes, some don't)
    //
    // Expected outcome:
    // - Wide attention score distribution (0.1-0.9)
    // - Different orbit distances
    // - Possible clustering (50 tasks > threshold)
    // - Mixed Solar System appearance
    // ═══════════════════════════════════════════════════════════════════════

    private suspend fun generateChaosUser(): GeneratedScenarioResult {
        val goalId = createGoal("Side Project", ageDays = 45)
        val taskIds = mutableListOf<Int>()

        // 50 tasks with mixed behaviors
        for (i in 1..50) {
            val createdDaysAgo = 40 - (i % 40)

            val taskId = createTask(
                goalId = goalId,
                title = "Task $i",
                createdDaysAgo = createdDaysAgo,
                scheduledDaysAgo = createdDaysAgo - 2,
                deadlineDaysAgo = createdDaysAgo - 5
            )
            taskIds.add(taskId)

            // Created event
            insertEvent(taskId, "created", createdDaysAgo)

            when {
                // 15 completed tasks (spread across days, with notes)
                i <= 15 -> {
                    val completedDaysAgo = createdDaysAgo - 3
                    insertEvent(taskId, "completed", completedDaysAgo)
                    // Add interaction for completed tasks
                    insertNote(taskId, "Working on task $i", createdDaysAgo - 1)
                    val task = taskRepository.getTasksByGoalId(goalId).first()
                        .first { it.id == taskId }
                    taskRepository.updateTask(task.copy(isCompleted = true))
                }

                // 10 stale tasks (created early, no interaction, deadline passed)
                i <= 25 -> {
                    // No additional events - task remains stale with old deadline
                }

                // 10 overdue tasks (deadline passed, some with reschedules)
                i <= 35 -> {
                    if (i % 3 == 0) {
                        insertEvent(taskId, "rescheduled", createdDaysAgo - 5)
                    }
                }

                // 10 tasks with reschedules (varied count)
                i <= 45 -> {
                    val rescheduleCount = (i % 3) + 1
                    for (r in 1..rescheduleCount) {
                        insertEvent(taskId, "rescheduled", createdDaysAgo - r * 2)
                    }
                }

                // 5 recently created (active, fresh, future deadline)
                else -> {
                    // Recently created with future deadline
                }
            }
        }

        snapshotAggregator.backfillMissing()

        return GeneratedScenarioResult(
            goalId = goalId,
            scenarioType = ScenarioType.CHAOS_USER,
            createdTaskIds = taskIds,
            description = "Chaos user: 50 tasks with mixed states. " +
                    "Expected: wide attention range, varied orbits, possible clustering."
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Scenario D: Recovery User
    //
    // Behavioral pattern:
    // - First 15 days: bad behavior (reschedules, overdue, no interaction)
    // - Last 15 days: good behavior (completions, notes, met deadlines)
    //
    // Expected outcome:
    // - Attention trend decreases over time
    // - Early tasks: higher attention (staleness + avoidance)
    // - Recent tasks: lower attention (fresh, completed)
    // - No ConsistencyDecay signal (improvement detected)
    // ═══════════════════════════════════════════════════════════════════════

    private suspend fun generateRecoveryUser(): GeneratedScenarioResult {
        val goalId = createGoal("Fitness Routine", ageDays = 30)
        val taskIds = mutableListOf<Int>()

        // First 15 days: bad behavior (many reschedules, few completions, overdue)
        for (i in 1..10) {
            val createdDaysAgo = 28 - i // Days -28 to -19
            val deadlineDaysAgo = createdDaysAgo - 2 // Deadline 2 days after creation (easily missed)

            val taskId = createTask(
                goalId = goalId,
                title = "Early Task $i",
                createdDaysAgo = createdDaysAgo,
                scheduledDaysAgo = createdDaysAgo - 1,
                deadlineDaysAgo = deadlineDaysAgo
            )
            taskIds.add(taskId)

            insertEvent(taskId, "created", createdDaysAgo)

            // Most tasks get rescheduled (no notes = high staleness)
            if (i <= 7) {
                insertEvent(taskId, "rescheduled", createdDaysAgo - 3)
                if (i <= 4) {
                    insertEvent(taskId, "rescheduled", createdDaysAgo - 6)
                }
            }

            // Only 2 completions in first 15 days
            if (i == 1 || i == 3) {
                insertEvent(taskId, "completed", createdDaysAgo - 2)
                val task = taskRepository.getTasksByGoalId(goalId).first()
                    .first { it.id == taskId }
                taskRepository.updateTask(task.copy(isCompleted = true))
            }
        }

        // Last 15 days: good behavior (regular completions, no reschedules, notes)
        for (i in 1..10) {
            val createdDaysAgo = 15 - i // Days -14 to -5
            val deadlineDaysAgo = createdDaysAgo + 5 // Deadline 5 days after creation (realistic)

            val taskId = createTask(
                goalId = goalId,
                title = "Recent Task $i",
                createdDaysAgo = createdDaysAgo,
                scheduledDaysAgo = createdDaysAgo,
                deadlineDaysAgo = deadlineDaysAgo
            )
            taskIds.add(taskId)

            insertEvent(taskId, "created", createdDaysAgo)

            // Add meaningful interaction (note) for recent tasks
            insertNote(taskId, "Completed task $i on schedule", createdDaysAgo - 1)

            // Complete within 1-2 days (good behavior, before deadline)
            val completedDaysAgo = createdDaysAgo - 1
            insertEvent(taskId, "completed", completedDaysAgo)
            val task = taskRepository.getTasksByGoalId(goalId).first()
                .first { it.id == taskId }
            taskRepository.updateTask(task.copy(isCompleted = true))
        }

        // Add recent goal activity to prevent GoalAttention signal
        goalRepository.insertGoalEvent(
            GoalEventEntity(
                goalId = goalId,
                eventType = "completed",
                timestamp = DebugDateUtils.timestampDaysAgo(3)
            )
        )

        snapshotAggregator.backfillMissing()

        return GeneratedScenarioResult(
            goalId = goalId,
            scenarioType = ScenarioType.RECOVERY_USER,
            createdTaskIds = taskIds,
            description = "Recovery user: bad start (reschedules) → good finish (completions). " +
                    "Expected: decreasing attention, no ConsistencyDecay signal."
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════

    private suspend fun createGoal(title: String, ageDays: Int): Int {
        val goal = GoalEntity(
            title = "$GOAL_PREFIX $title",
            status = "active",
            createdAt = DebugDateUtils.timestampDaysAgo(ageDays)
        )
        val goalId = goalRepository.insertGoal(goal).toInt()

        goalRepository.insertGoalEvent(
            GoalEventEntity(
                goalId = goalId,
                eventType = "created",
                timestamp = DebugDateUtils.timestampDaysAgo(ageDays)
            )
        )

        return goalId
    }

    private suspend fun createTask(
        goalId: Int,
        title: String,
        createdDaysAgo: Int,
        scheduledDaysAgo: Int,
        deadlineDaysAgo: Int? = null
    ): Int {
        val task = TaskEntity(
            title = title,
            dateEpochMs = DebugDateUtils.midnightDaysAgo(scheduledDaysAgo),
            timestamp = DebugDateUtils.timestampDaysAgo(createdDaysAgo),
            goalId = goalId,
            deadlineEpochMs = deadlineDaysAgo?.let { DebugDateUtils.midnightDaysAgo(it) }
        )
        return taskRepository.insertTask(task).toInt()
    }

    private suspend fun insertEvent(taskId: Int, eventType: String, daysAgo: Int) {
        taskRepository.insertTaskEvent(
            TaskEventEntity(
                taskId = taskId,
                eventType = eventType,
                timestamp = DebugDateUtils.timestampDaysAgo(daysAgo)
            )
        )
    }

    private suspend fun insertNote(taskId: Int, content: String, daysAgo: Int) {
        noteRepository.insert(
            NoteEntity(
                content = content,
                timestamp = DebugDateUtils.timestampDaysAgo(daysAgo),
                taskId = taskId
            )
        )
    }

    companion object {
        const val GOAL_PREFIX = "🧪 Lab"
    }

    /**
     * Helper data class for procrastinator task specifications.
     */
    private data class ProcrastinatorTaskSpec(
        val title: String,
        val createdDaysAgo: Int,
        val deadlineDaysAgo: Int,
        val rescheduleDays: List<Int>
    )
}

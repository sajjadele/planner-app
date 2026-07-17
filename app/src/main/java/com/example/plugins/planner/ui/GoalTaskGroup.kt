package com.example.plugins.planner.ui

import com.example.core.goal.GoalEntity
import com.example.plugins.planner.data.TaskEntity

/**
 * UI model grouping today's (or the selected day's) tasks by their owning goal.
 * `goal == null` represents the "بدون هدف" (No Goal) section — tasks with no `goalId`.
 *
 * Pure data holder; grouping logic lives in [groupTasksByGoal].
 */
data class GoalTaskGroup(
    val goal: GoalEntity?,
    val tasks: List<TaskEntity>
)

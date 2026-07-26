package com.example.core.mirror

import com.example.domain.mirror.MirrorInsight
import com.example.domain.mirror.MirrorSignal
import kotlinx.coroutines.flow.Flow

interface MirrorRepository {
    fun observeRescheduleCounts(): Flow<List<RescheduleSummary>>
    fun observeGoalCompletionRates(): Flow<List<GoalMirrorRate>>
    fun observeGoalEvents(): Flow<List<GoalEventMirrorItem>>
    fun observeBehaviorRange(start: Long, end: Long): Flow<List<BehaviorMirrorItem>>
    suspend fun getEarliestTaskDateEpochMs(): Long?

    suspend fun evaluate(goalId: Int): List<MirrorSignal>
    suspend fun render(signals: List<MirrorSignal>): List<MirrorInsight>
}

data class RescheduleSummary(
    val taskId: Int,
    val taskTitle: String?,
    val rescheduleCount: Int,
    val createdAt: Long,
    val isCompleted: Boolean
)

data class GoalMirrorRate(
    val goalId: Int,
    val goalTitle: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val completionRate: Float
)

data class GoalEventMirrorItem(
    val goalId: Int,
    val eventType: String,
    val timestamp: Long
)

data class BehaviorMirrorItem(
    val dateEpochMs: Long,
    val completed: Int,
    val created: Int
)

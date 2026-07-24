package com.example.core.snapshot

import kotlinx.coroutines.flow.Flow

/**
 * Repository boundary for the Phase 3 progress/behavior projections.
 *
 * Extracted as an interface so the concrete [RoomSnapshotRepository] can be swapped or mocked
 * in tests; consumers (the [SnapshotAggregator], and later the Graph View / AI Coach) depend on
 * this interface, never on the DAO directly.
 */
interface SnapshotRepository {
    suspend fun recordGoalProgress(snapshot: GoalProgressSnapshotEntity)
    suspend fun recordBehavior(snapshot: BehaviorSnapshotEntity)
    fun observeGoalProgress(goalId: Int): Flow<List<GoalProgressSnapshotEntity>>
    fun observeBehaviorRange(start: Long, end: Long): Flow<List<BehaviorSnapshotEntity>>
    suspend fun getLatestBehavior(): BehaviorSnapshotEntity?
    suspend fun getCoveredDates(): List<Long>
    suspend fun deleteAllBehaviors()
    suspend fun deleteAllGoalProgress()
}

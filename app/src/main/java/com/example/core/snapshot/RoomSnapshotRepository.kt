package com.example.core.snapshot

import kotlinx.coroutines.flow.Flow

/**
 * Room-backed implementation of [SnapshotRepository]. Thin delegation over [SnapshotDao];
 * all SQL lives in the DAO, this class owns only the boundary.
 */
class RoomSnapshotRepository(private val snapshotDao: SnapshotDao) : SnapshotRepository {
    override suspend fun recordGoalProgress(snapshot: GoalProgressSnapshotEntity) =
        snapshotDao.upsertGoalProgress(snapshot)

    override suspend fun recordBehavior(snapshot: BehaviorSnapshotEntity) =
        snapshotDao.upsertBehavior(snapshot)

    override fun observeGoalProgress(goalId: Int): Flow<List<GoalProgressSnapshotEntity>> =
        snapshotDao.observeGoalProgress(goalId)

    override fun observeBehaviorRange(start: Long, end: Long): Flow<List<BehaviorSnapshotEntity>> =
        snapshotDao.observeBehaviorRange(start, end)

    override suspend fun getLatestBehavior(): BehaviorSnapshotEntity? =
        snapshotDao.getLatestBehavior()

    override suspend fun getCoveredDates(): List<Long> =
        snapshotDao.getCoveredDates()

    override suspend fun deleteAllBehaviors() =
        snapshotDao.deleteAllBehaviors()

    override suspend fun deleteAllGoalProgress() =
        snapshotDao.deleteAllGoalProgress()
}

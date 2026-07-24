package com.example.core.snapshot

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Persistence for the Phase 3 progress/behavior projections.
 *
 * Snapshots are rebuildable: writes use REPLACE (idempotent upsert) and explicit deletes exist
 * so a day can be recomputed from raw events without duplication. Reads are reactive Flows that
 * re-emit when the underlying projection changes.
 */
@Dao
interface SnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoalProgress(snapshot: GoalProgressSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBehavior(snapshot: BehaviorSnapshotEntity)

    @Query("""
        SELECT * FROM goal_progress_snapshot
        WHERE goalId = :goalId
        ORDER BY dateEpochMs DESC
    """)
    fun observeGoalProgress(goalId: Int): Flow<List<GoalProgressSnapshotEntity>>

    @Query("""
        SELECT * FROM behavior_snapshot
        WHERE dateEpochMs BETWEEN :start AND :end
        ORDER BY dateEpochMs ASC
    """)
    fun observeBehaviorRange(start: Long, end: Long): Flow<List<BehaviorSnapshotEntity>>

    @Query("SELECT * FROM behavior_snapshot ORDER BY dateEpochMs DESC LIMIT 1")
    suspend fun getLatestBehavior(): BehaviorSnapshotEntity?

    @Query("SELECT DISTINCT dateEpochMs FROM behavior_snapshot ORDER BY dateEpochMs ASC")
    suspend fun getCoveredDates(): List<Long>

    @Query("DELETE FROM behavior_snapshot WHERE dateEpochMs = :dateEpochMs")
    suspend fun deleteBehaviorForDay(dateEpochMs: Long)

    @Query("DELETE FROM goal_progress_snapshot WHERE dateEpochMs = :dateEpochMs")
    suspend fun deleteGoalProgressForDay(dateEpochMs: Long)

    @Query("DELETE FROM behavior_snapshot")
    suspend fun deleteAllBehaviors()

    @Query("DELETE FROM goal_progress_snapshot")
    suspend fun deleteAllGoalProgress()
}

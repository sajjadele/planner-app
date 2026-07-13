package com.example.core.goal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Persistence for [GoalEventEntity]. Read-only append model — events are never updated or
 * deleted, preserving the goal lifecycle audit trail.
 */
@Dao
interface GoalEventDao {
    @Insert
    suspend fun insertEvent(event: GoalEventEntity)

    @Query("SELECT * FROM goal_events ORDER BY timestamp DESC")
    fun observeAllEvents(): Flow<List<GoalEventEntity>>

    @Query("SELECT * FROM goal_events WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun observeEventsByGoal(goalId: Int): Flow<List<GoalEventEntity>>
}

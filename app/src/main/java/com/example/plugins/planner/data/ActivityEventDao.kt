package com.example.plugins.planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityEventDao {

    @Insert
    suspend fun insert(event: ActivityEventEntity)

    @Update
    suspend fun update(event: ActivityEventEntity)

    @Query(
        """
        SELECT * FROM activity_events
        WHERE taskId = :taskId
        ORDER BY timestamp ASC
        """
    )
    fun observeByTaskId(taskId: Int): Flow<List<ActivityEventEntity>>

    @Query(
        """
        SELECT * FROM activity_events
        WHERE id = :id
        """
    )
    fun observeById(id: Long): Flow<ActivityEventEntity?>

    @Query("SELECT * FROM activity_events WHERE id = :id")
    suspend fun getById(id: Long): ActivityEventEntity?

    @Query("DELETE FROM activity_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM activity_events WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: Int)

    @Query(
        """
        SELECT * FROM activity_events
        WHERE taskId = :taskId AND eventType = :eventType
        ORDER BY timestamp DESC
        LIMIT 1
        """
    )
    suspend fun findLatestEvent(taskId: Int, eventType: String): ActivityEventEntity?

    @Query(
        """
        SELECT * FROM activity_events
        WHERE stepId = :stepId
        ORDER BY timestamp DESC
        """
    )
    suspend fun getEventsByStepId(stepId: Int): List<ActivityEventEntity>
}

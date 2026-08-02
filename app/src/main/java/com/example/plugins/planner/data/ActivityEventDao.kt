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

    /**
     * Windowed feed query — returns only the most recent [limit] rows matching the
     * current feed scope (task, optional day range, optional step), newest first.
     * Used as the reactive "anchor window" for the Activity Feed (P0 windowed loading):
     * bounded to [limit] rows instead of loading the task's entire history.
     */
    @Query(
        """
        SELECT * FROM activity_events
        WHERE taskId = :taskId
          AND (:dayStart IS NULL OR timestamp >= :dayStart)
          AND (:dayEnd IS NULL OR timestamp < :dayEnd)
          AND (:stepId IS NULL OR stepId = :stepId)
        ORDER BY timestamp DESC, id DESC
        LIMIT :limit
        """
    )
    fun observeFeedWindow(
        taskId: Int,
        dayStart: Long?,
        dayEnd: Long?,
        stepId: Int?,
        limit: Int
    ): Flow<List<ActivityEventEntity>>

    /**
     * One-shot cursor-based page fetch for scroll-back pagination (P0 windowed loading).
     * Returns up to [limit] rows strictly older than the (timestamp, id) cursor,
     * newest first, within the current feed scope. Cursor ties broken by id to avoid
     * duplicates/skips across pages.
     */
    @Query(
        """
        SELECT * FROM activity_events
        WHERE taskId = :taskId
          AND (:dayStart IS NULL OR timestamp >= :dayStart)
          AND (:dayEnd IS NULL OR timestamp < :dayEnd)
          AND (:stepId IS NULL OR stepId = :stepId)
          AND (timestamp < :beforeTs OR (timestamp = :beforeTs AND id < :beforeId))
        ORDER BY timestamp DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun getFeedPageBefore(
        taskId: Int,
        dayStart: Long?,
        dayEnd: Long?,
        stepId: Int?,
        beforeTs: Long,
        beforeId: Int,
        limit: Int
    ): List<ActivityEventEntity>

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

    /**
     * Nullify stepId for all activity events referencing a given step.
     * Called before deleting a tag so activities are preserved but unlinked.
     */
    @Query("UPDATE activity_events SET stepId = NULL WHERE stepId = :stepId")
    suspend fun clearStepId(stepId: Int)
}

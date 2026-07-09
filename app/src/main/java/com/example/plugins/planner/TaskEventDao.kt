package com.example.plugins.planner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskEventDao {
    // TODO: needed for future Insight analytics (completion velocity, reopen rate)
    @Query("SELECT * FROM task_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<TaskEventEntity>>

    @Insert
    suspend fun insertEvent(event: TaskEventEntity)

    // --- Weekly Insight queries (reactive Flows) ---

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE isCompleted = 1 AND timestamp BETWEEN :start AND :end
    """)
    fun observeCompletedCount(start: Long, end: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE timestamp BETWEEN :start AND :end
    """)
    fun observeCreatedCount(start: Long, end: Long): Flow<Int>

    // Streak: raw timestamps of 'completed' events (day computed in Kotlin w/ device TZ)
    @Query("""
        SELECT timestamp FROM task_events
        WHERE eventType = 'completed'
    """)
    fun observeCompletedTimestamps(): Flow<List<Long>>

    @Query("""
        SELECT t.lifeAreaId as lifeAreaId, COUNT(*) as count
        FROM tasks t
        WHERE t.isCompleted = 1 AND t.timestamp BETWEEN :start AND :end
        AND t.lifeAreaId IS NOT NULL
        GROUP BY t.lifeAreaId
        ORDER BY count DESC
    """)
    fun observeCompletionByLifeArea(start: Long, end: Long): Flow<List<LifeAreaCompletion>>

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE timestamp BETWEEN :start AND :end
        AND lifeAreaId IS NULL AND goalId IS NULL AND goalName IS NULL
    """)
    fun observeUnorganizedCount(start: Long, end: Long): Flow<Int>

    // Best day of week: dayIndex with most completed tasks this week
    @Query("""
        SELECT dayIndex as dayIndex, COUNT(*) as count
        FROM tasks
        WHERE isCompleted = 1 AND timestamp BETWEEN :start AND :end
        GROUP BY dayIndex
        ORDER BY count DESC
    """)
    fun observeCompletionByDay(start: Long, end: Long): Flow<List<DayCompletion>>
}

data class LifeAreaCompletion(val lifeAreaId: Int, val count: Int)
data class DayCompletion(val dayIndex: Int, val count: Int)
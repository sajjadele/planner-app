package com.example.core.goal

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE status = 'active' ORDER BY createdAt DESC")
    fun getActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE status = :status ORDER BY createdAt DESC")
    fun getGoalsByStatus(status: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: Int): GoalEntity?

    @Query("SELECT * FROM goals WHERE id = :goalId")
    fun observeGoalById(goalId: Int): Flow<GoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: Int)

    @Query("SELECT COUNT(*) FROM goals WHERE status = 'active'")
    fun observeActiveGoalCount(): Flow<Int>

    /**
     * Latest scheduled activity day for a goal's tasks, derived from `tasks.dateEpochMs`.
     *
     * NOTE: this reflects *scheduled* activity days, not necessarily executed actions. A future
     * refinement may migrate the signal to `task_events`. Kept derived (never stored) so the
     * single source of truth remains `tasks`/`task_events`.
     */
    @Query("SELECT MAX(t.dateEpochMs) FROM tasks t WHERE t.goalId = :goalId")
    fun observeGoalLastActivity(goalId: Int): Flow<Long?>

    /**
     * Count of distinct scheduled days on which the goal has tasks, derived from `tasks.dateEpochMs`.
     *
     * NOTE: "active day" here means a day with at least one task scheduled for the goal. See the
     * note on [observeGoalLastActivity] — may later migrate to `task_events`. Derived, not stored.
     */
    @Query("""
        SELECT COUNT(DISTINCT CAST(t.dateEpochMs / 86400000 AS INTEGER))
        FROM tasks t WHERE t.goalId = :goalId
    """)
    fun observeGoalActiveDayCount(goalId: Int): Flow<Int>
}

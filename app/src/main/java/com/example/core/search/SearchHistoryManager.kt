package com.example.core.search

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * SearchHistoryManager — Tracks task/goal IDs clicked from search results.
 *
 * Stores last N accessed task IDs and goal IDs for the "recent searches"
 * feature. Task and goal histories are kept SEPARATE so an ID collision
 * between the two auto-increment tables cannot mix up a task with a goal.
 * Data is persisted via SharedPreferences.
 */
class SearchHistoryManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "search_history"
        private const val KEY_RECENT_TASKS = "recent_task_ids"
        private const val KEY_RECENT_GOALS = "recent_goal_ids"
        private const val MAX_HISTORY_SIZE = 10
        private const val SEPARATOR = ","
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _recentTaskIds = MutableStateFlow(loadHistory(KEY_RECENT_TASKS))
    val recentTaskIds: StateFlow<List<Int>> = _recentTaskIds
    
    private val _recentGoalIds = MutableStateFlow(loadHistory(KEY_RECENT_GOALS))
    val recentGoalIds: StateFlow<List<Int>> = _recentGoalIds
    
    private fun loadHistory(key: String): List<Int> {
        val raw = prefs.getString(key, "") ?: ""
        return if (raw.isBlank()) {
            emptyList()
        } else {
            raw.split(SEPARATOR)
                .mapNotNull { it.trim().toIntOrNull() }
                .take(MAX_HISTORY_SIZE)
        }
    }
    
    private fun saveHistory(key: String, ids: List<Int>) {
        val raw = ids.joinToString(SEPARATOR)
        prefs.edit().putString(key, raw).apply()
    }
    
    /**
     * Record that a task was accessed from search.
     * Moves it to the front of the list (most recent).
     */
    fun recordTaskAccess(taskId: Int) {
        _recentTaskIds.value = pushToFront(_recentTaskIds.value, taskId).also {
            saveHistory(KEY_RECENT_TASKS, it)
        }
    }
    
    /**
     * Record that a goal was accessed from search.
     * Moves it to the front of the list (most recent).
     */
    fun recordGoalAccess(goalId: Int) {
        _recentGoalIds.value = pushToFront(_recentGoalIds.value, goalId).also {
            saveHistory(KEY_RECENT_GOALS, it)
        }
    }
    
    /**
     * Remove id if present, add to front, trim to max size, persist.
     */
    private fun pushToFront(current: List<Int>, id: Int): List<Int> {
        val updated = current.toMutableList()
        updated.remove(id)     // Remove if already exists
        updated.add(0, id)     // Add to front
        return updated.take(MAX_HISTORY_SIZE)
    }
    
    /**
     * Get recent task IDs in order (most recent first).
     */
    fun getRecentTaskIds(): List<Int> = _recentTaskIds.value
    
    /**
     * Get recent goal IDs in order (most recent first).
     */
    fun getRecentGoalIds(): List<Int> = _recentGoalIds.value
}

package com.example.core.search

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * SearchHistoryManager — Tracks task IDs clicked from search results.
 *
 * Stores last N accessed task IDs for "recent searches" feature.
 * Data is persisted via SharedPreferences.
 */
class SearchHistoryManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "search_history"
        private const val KEY_RECENT_TASKS = "recent_task_ids"
        private const val MAX_HISTORY_SIZE = 10
        private const val SEPARATOR = ","
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _recentTaskIds = MutableStateFlow(loadHistory())
    val recentTaskIds: StateFlow<List<Int>> = _recentTaskIds
    
    private fun loadHistory(): List<Int> {
        val raw = prefs.getString(KEY_RECENT_TASKS, "") ?: ""
        return if (raw.isBlank()) {
            emptyList()
        } else {
            raw.split(SEPARATOR)
                .mapNotNull { it.trim().toIntOrNull() }
                .take(MAX_HISTORY_SIZE)
        }
    }
    
    private fun saveHistory(ids: List<Int>) {
        val raw = ids.joinToString(SEPARATOR)
        prefs.edit().putString(KEY_RECENT_TASKS, raw).apply()
    }
    
    /**
     * Record that a task was accessed from search.
     * Moves it to the front of the list (most recent).
     */
    fun recordAccess(taskId: Int) {
        val current = _recentTaskIds.value.toMutableList()
        current.remove(taskId)  // Remove if already exists
        current.add(0, taskId)  // Add to front
        
        // Trim to max size
        val trimmed = current.take(MAX_HISTORY_SIZE)
        _recentTaskIds.value = trimmed
        saveHistory(trimmed)
    }
    
    /**
     * Get recent task IDs in order (most recent first).
     */
    fun getRecentIds(): List<Int> = _recentTaskIds.value
}

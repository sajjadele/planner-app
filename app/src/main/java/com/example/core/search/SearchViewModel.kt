package com.example.core.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.constants.DateConstants
import com.example.core.database.AppDatabase
import com.example.plugins.planner.ui.components.persianDayIndex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed class SearchResult {
    data class TaskResult(
        val id: Int,
        val title: String,
        val priority: String?,
        val isCompleted: Boolean,
        val dateEpochMs: Long,
        val dayName: String
    ) : SearchResult()
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val searchHistory = SearchHistoryManager(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    /** Recent tasks based on search history (last clicked from search) */
    val recentTasks: StateFlow<List<SearchResult.TaskResult>> = combine(
        searchHistory.recentTaskIds,
        taskDao.getAllTasks()
    ) { recentIds, allTasks ->
        recentIds.mapNotNull { id ->
            allTasks.find { it.id == id }?.let { task ->
                val dayIdx = persianDayIndex(task.dateEpochMs)
                val dayName = DateConstants.persianDayNames.getOrElse(dayIdx) {
                    DateConstants.persianDayNames.first()
                }
                SearchResult.TaskResult(
                    id = task.id,
                    title = task.title,
                    priority = task.priority,
                    isCompleted = task.isCompleted,
                    dateEpochMs = task.dateEpochMs,
                    dayName = dayName
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val searchResults: StateFlow<List<SearchResult>> = combine(
        _searchQuery,
        taskDao.getAllTasks()
    ) { query, tasks ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val normalizedQuery = query.trim().lowercase()
            
            tasks.filter { 
                it.title.lowercase().contains(normalizedQuery)
            }.map { task ->
                val dayIdx = persianDayIndex(task.dateEpochMs)
                val dayName = DateConstants.persianDayNames.getOrElse(dayIdx) {
                    DateConstants.persianDayNames.first()
                }
                SearchResult.TaskResult(
                    id = task.id,
                    title = task.title,
                    priority = task.priority,
                    isCompleted = task.isCompleted,
                    dateEpochMs = task.dateEpochMs,
                    dayName = dayName
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Record that a task was accessed from search results.
     * Call this before navigating to the task.
     */
    fun recordTaskAccess(taskId: Int) {
        searchHistory.recordAccess(taskId)
    }
}

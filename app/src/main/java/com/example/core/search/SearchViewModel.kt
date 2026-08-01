package com.example.core.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.constants.DateConstants
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalDao
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
    
    data class GoalResult(
        val id: Int,
        val title: String,
        val status: String,
        val description: String?
    ) : SearchResult()
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val goalDao = database.goalDao()
    private val searchHistory = SearchHistoryManager(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    /** Recent tasks and goals based on search history (last clicked from search) */
    val recentTasks: StateFlow<List<SearchResult>> = combine(
        searchHistory.recentTaskIds,
        taskDao.getAllTasks(),
        goalDao.getAllGoals()
    ) { recentIds, allTasks, allGoals ->
        recentIds.mapNotNull { id ->
            // Try to find as task first
            allTasks.find { it.id == id }?.let { task ->
                val dayIdx = persianDayIndex(task.dateEpochMs)
                val dayName = DateConstants.persianDayNames.getOrElse(dayIdx) {
                    DateConstants.persianDayNames.first()
                }
                return@mapNotNull SearchResult.TaskResult(
                    id = task.id,
                    title = task.title,
                    priority = task.priority,
                    isCompleted = task.isCompleted,
                    dateEpochMs = task.dateEpochMs,
                    dayName = dayName
                )
            }
            // Try to find as goal
            allGoals.find { it.id == id }?.let { goal ->
                return@mapNotNull SearchResult.GoalResult(
                    id = goal.id,
                    title = goal.title,
                    status = goal.status,
                    description = goal.description
                )
            }
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val searchResults: StateFlow<List<SearchResult>> = combine(
        _searchQuery,
        taskDao.getAllTasks(),
        goalDao.getAllGoals()
    ) { query, tasks, goals ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val normalizedQuery = query.trim().lowercase()
            val results = mutableListOf<SearchResult>()
            
            // Search tasks
            tasks.filter { 
                it.title.lowercase().contains(normalizedQuery)
            }.forEach { task ->
                val dayIdx = persianDayIndex(task.dateEpochMs)
                val dayName = DateConstants.persianDayNames.getOrElse(dayIdx) {
                    DateConstants.persianDayNames.first()
                }
                results.add(SearchResult.TaskResult(
                    id = task.id,
                    title = task.title,
                    priority = task.priority,
                    isCompleted = task.isCompleted,
                    dateEpochMs = task.dateEpochMs,
                    dayName = dayName
                ))
            }
            
            // Search goals
            goals.filter {
                it.title.lowercase().contains(normalizedQuery)
            }.forEach { goal ->
                results.add(SearchResult.GoalResult(
                    id = goal.id,
                    title = goal.title,
                    status = goal.status,
                    description = goal.description
                ))
            }
            
            results
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

package com.example.core.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.constants.DateConstants
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalDao
import com.example.core.util.normalizeForSearch
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

enum class SearchFilter(val label: String) {
    ALL("همه"),
    TASKS("تسک‌ها"),
    GOALS("اهداف")
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val goalDao = database.goalDao()
    private val searchHistory = SearchHistoryManager(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter

    /** Recent tasks based on search history (last clicked from search) */
    private val recentTaskResults: StateFlow<List<SearchResult>> = combine(
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

    /** Recent goals based on search history (last clicked from search) */
    private val recentGoalResults: StateFlow<List<SearchResult>> = combine(
        searchHistory.recentGoalIds,
        goalDao.getAllGoals()
    ) { recentIds, allGoals ->
        recentIds.mapNotNull { id ->
            allGoals.find { it.id == id }?.let { goal ->
                SearchResult.GoalResult(
                    id = goal.id,
                    title = goal.title,
                    status = goal.status,
                    description = goal.description
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** Combined recent results for display: tasks first, then goals. */
    val recentResults: StateFlow<List<SearchResult>> = combine(
        recentTaskResults,
        recentGoalResults
    ) { tasks, goals ->
        tasks + goals
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val searchResults: StateFlow<List<SearchResult>> = combine(
        _searchQuery,
        _searchFilter,
        taskDao.getAllTasks(),
        goalDao.getAllGoals()
    ) { query, filter, tasks, goals ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val normalizedQuery = query.trim().normalizeForSearch()
            val results = mutableListOf<SearchResult>()
            
            // Search tasks (if filter allows)
            if (filter != SearchFilter.GOALS) {
                tasks.filter { task ->
                    task.title.normalizeForSearch().contains(normalizedQuery) ||
                        task.valueTag?.normalizeForSearch()?.contains(normalizedQuery) == true
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
            }
            
            // Search goals (if filter allows)
            if (filter != SearchFilter.TASKS) {
                goals.filter { goal ->
                    goal.title.normalizeForSearch().contains(normalizedQuery) ||
                        goal.description?.normalizeForSearch()?.contains(normalizedQuery) == true ||
                        goal.why?.normalizeForSearch()?.contains(normalizedQuery) == true
                }.forEach { goal ->
                    results.add(SearchResult.GoalResult(
                        id = goal.id,
                        title = goal.title,
                        status = goal.status,
                        description = goal.description
                    ))
                }
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

    fun updateFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    /**
     * Record that a task was accessed from search results.
     * Call this before navigating to the task.
     */
    fun recordTaskAccess(taskId: Int) {
        searchHistory.recordTaskAccess(taskId)
    }

    /**
     * Record that a goal was accessed from search results.
     * Call this before navigating to the goal.
     */
    fun recordGoalAccess(goalId: Int) {
        searchHistory.recordGoalAccess(goalId)
    }
}

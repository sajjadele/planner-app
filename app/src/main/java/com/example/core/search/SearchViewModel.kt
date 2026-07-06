package com.example.core.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.constants.DateConstants
import com.example.core.database.AppDatabase
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
        val dayIndex: Int,
        val dayName: String
    ) : SearchResult()

    data class NoteResult(
        val id: Int,
        val content: String,
        val timestamp: Long
    ) : SearchResult()
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val noteDao = database.noteDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery


    val searchResults: StateFlow<List<SearchResult>> = combine(
        _searchQuery,
        taskDao.getAllTasks(),
        noteDao.getAllNotes()
    ) { query, tasks, notes ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val normalizedQuery = query.trim().lowercase()
            
            val filteredTasks = tasks.filter { 
                it.title.lowercase().contains(normalizedQuery)
            }.map { task ->
                val dayName = DateConstants.persianDayNames.getOrElse(task.dayIndex) {
                    DateConstants.persianDayNames.first()
                }
                SearchResult.TaskResult(
                    id = task.id,
                    title = task.title,
                    priority = task.priority,
                    isCompleted = task.isCompleted,
                    dayIndex = task.dayIndex,
                    dayName = dayName
                )
            }

            val filteredNotes = notes.filter {
                it.content.lowercase().contains(normalizedQuery)
            }.map { note ->
                SearchResult.NoteResult(
                    id = note.id,
                    content = note.content,
                    timestamp = note.timestamp
                )
            }

            // Combine list, prioritizing tasks first then notes
            filteredTasks + filteredNotes
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateQuery(query: String) {
        _searchQuery.value = query
    }
}

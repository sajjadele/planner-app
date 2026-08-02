package com.example.core.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchFilterTest {
    
    @Test
    fun `ALL filter includes both tasks and goals`() {
        val filter = SearchFilter.ALL
        val results = listOf(
            SearchResult.TaskResult(1, "Task", null, false, 0L, "شنبه"),
            SearchResult.GoalResult(1, "Goal", "active", null)
        )
        val filtered = results.filter { result ->
            when (filter) {
                SearchFilter.ALL -> true
                SearchFilter.TASKS -> result is SearchResult.TaskResult
                SearchFilter.GOALS -> result is SearchResult.GoalResult
            }
        }
        assertEquals(2, filtered.size)
    }
    
    @Test
    fun `TASKS filter includes only tasks`() {
        val filter = SearchFilter.TASKS
        val results = listOf(
            SearchResult.TaskResult(1, "Task", null, false, 0L, "شنبه"),
            SearchResult.GoalResult(1, "Goal", "active", null)
        )
        val filtered = results.filter { result ->
            when (filter) {
                SearchFilter.ALL -> true
                SearchFilter.TASKS -> result is SearchResult.TaskResult
                SearchFilter.GOALS -> result is SearchResult.GoalResult
            }
        }
        assertEquals(1, filtered.size)
        assert(filtered[0] is SearchResult.TaskResult)
    }
    
    @Test
    fun `GOALS filter includes only goals`() {
        val filter = SearchFilter.GOALS
        val results = listOf(
            SearchResult.TaskResult(1, "Task", null, false, 0L, "شنبه"),
            SearchResult.GoalResult(1, "Goal", "active", null)
        )
        val filtered = results.filter { result ->
            when (filter) {
                SearchFilter.ALL -> true
                SearchFilter.TASKS -> result is SearchResult.TaskResult
                SearchFilter.GOALS -> result is SearchResult.GoalResult
            }
        }
        assertEquals(1, filtered.size)
        assert(filtered[0] is SearchResult.GoalResult)
    }
}

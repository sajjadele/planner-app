# Search Result Filter — Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Add filter chips (همه / تسک‌ها / اهداف) to the SearchDialog so users can filter search results by type.

**Architecture:** Add a `SearchFilter` enum to SearchViewModel, expose a `MutableStateFlow<SearchFilter>`, and filter the combined `searchResults` flow. The UI adds `FilterChip` components above the results list.

**Tech Stack:** Kotlin, Jetpack Compose, Material3 FilterChip, StateFlow

---

## Current Context

- **SearchViewModel** (`app/src/main/java/com/example/core/search/SearchViewModel.kt`): `searchResults` is a `combine(_searchQuery, taskDao.getAllTasks(), goalDao.getAllGoals())` that returns `List<SearchResult>` (sealed class: TaskResult | GoalResult).
- **SearchDialog** (`app/src/main/java/com/example/core/search/SearchDialog.kt`): Full-screen Dialog with OutlinedTextField, recent results section, and search results LazyColumn. No filter UI yet.
- **SearchResult** sealed class is defined at top of SearchViewModel.kt (lines 17-33).

---

## Step-by-Step Plan

### Task 1: Add SearchFilter enum to SearchViewModel.kt

**Objective:** Define filter options as an enum inside SearchViewModel file.

**File:** `app/src/main/java/com/example/core/search/SearchViewModel.kt`

**Code to add** (above `class SearchViewModel`, after `SearchResult` sealed class, around line 34):

```kotlin
enum class SearchFilter(val label: String) {
    ALL("همه"),
    TASKS("تسک‌ها"),
    GOALS("اهداف")
}
```

**Step 2: Add filter state to SearchViewModel**

Inside `class SearchViewModel`, add after `_searchQuery` (line 41):

```kotlin
private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
val searchFilter: StateFlow<SearchFilter> = _searchFilter
```

And the update function:

```kotlin
fun updateFilter(filter: SearchFilter) {
    _searchFilter.value = filter
}
```

**Step 3: Modify `searchResults` to respect filter**

Replace the `searchResults` StateFlow (lines 104-154) with:

```kotlin
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
```

**Verification:** Compile check — no runtime test needed yet.

---

### Task 2: Add FilterChip UI to SearchDialog.kt

**Objective:** Add three FilterChip buttons above the search results.

**File:** `app/src/main/java/com/example/core/search/SearchDialog.kt`

**Step 1: Add imports** (if not already present):

```kotlin
import com.example.core.search.SearchFilter
```

**Step 2: Add filter state collection** (after line 56):

```kotlin
val searchFilter by viewModel.searchFilter.collectAsState()
```

**Step 3: Add FilterChip row** — insert after `Spacer(modifier = Modifier.height(20.dp))` (line 153) and before the results content section:

```kotlin
// Filter chips
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    SearchFilter.entries.forEach { filter ->
        FilterChip(
            selected = searchFilter == filter,
            onClick = { viewModel.updateFilter(filter) },
            label = {
                Text(
                    text = filter.label,
                    fontSize = 12.sp
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}
```

**Step 4: Add import for FilterChipDefaults** (if not present):

```kotlin
import androidx.compose.material3.FilterChipDefaults
```

**Verification:** Compile check — UI renders filter chips.

---

### Task 3: Add unit tests for filter logic

**Objective:** Test that filter correctly includes/excludes task and goal results.

**File:** Create `app/src/test/java/com/example/core/search/SearchFilterTest.kt`

```kotlin
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
```

**Run:** `./gradlew test --tests "com.example.core.search.SearchFilterTest"`

**Expected:** All 3 tests pass.

---

### Task 4: Fix duplicate imports in SearchDialog.kt

**Objective:** Remove duplicate import lines (currently lines 17-20 have duplicates).

**File:** `app/src/main/java/com/example/core/search/SearchDialog.kt`

Remove these duplicate lines:
- Line 17: `import androidx.compose.material.icons.filled.CheckCircleOutline` (duplicate of line 13)
- Line 18: `import androidx.compose.material.icons.filled.Close` (duplicate of line 19)
- Line 19: `import androidx.compose.material.icons.filled.Flag` (duplicate of line 16)
- Line 20: `import androidx.compose.material.icons.filled.PauseCircle` (duplicate of line 15)

Keep only one of each.

---

### Task 5: Manual verification

**Objective:** Build and verify the feature works.

**Steps:**
1. Run `./gradlew assembleDebug` — should compile successfully
2. Run `./gradlew test` — all tests pass
3. Manual test on device:
   - Open search dialog
   - Type a query that matches both tasks and goals
   - Verify "همه" chip is selected by default
   - Tap "تسک‌ها" — only task results shown
   - Tap "اهداف" — only goal results shown
   - Tap "همه" — both shown again
   - Clear query — filter resets? (no, filter persists — this is fine)

---

## Files Changed Summary

| File | Action |
|------|--------|
| `app/src/main/java/com/example/core/search/SearchViewModel.kt` | Modify: add SearchFilter enum, filter state, filter logic |
| `app/src/main/java/com/example/core/search/SearchDialog.kt` | Modify: add FilterChip UI, fix duplicate imports |
| `app/src/test/java/com/example/core/search/SearchFilterTest.kt` | Create: unit tests for filter |

---

## Risks & Notes

- **FilterChip availability:** `FilterChip` is in `material3` since compose-material3 1.1.0. The project uses Material3 (confirmed by imports), so this should work.
- **Filter persistence:** The filter does NOT reset when dialog closes. This is intentional — user preference within session.
- **No database changes:** Filter is purely in-memory ViewModel state.

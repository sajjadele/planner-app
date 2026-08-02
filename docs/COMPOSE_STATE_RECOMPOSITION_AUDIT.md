# Compose State & Recomposition Audit

> **Date:** 2026-08-02
> **Type:** Read-only audit — no code changes
> **Project:** Vision Planner Android (Kotlin + Jetpack Compose + MVVM)

---

## 1. Current Architecture

### State Flow: ViewModel → StateFlow → Compose

```
ViewModel (AndroidViewModel)
  └── MutableStateFlow<T> / StateFlow<T>
        └── collectAsState() in Composables
              └── Recomposition on value change
```

### Key ViewModels Analyzed
- `PlannerViewModel` — task list, date selection, completion
- `TaskDetailViewModel` — task detail, activity feed, tag management
- `WeeklyInsightViewModel` — weekly insights, streaks, velocity
- `GoalViewModel` — goal dashboard
- `GoalDetailViewModel` — goal detail screen

### Key Composables Analyzed
- `PlannerScreen` — main task list with LazyColumn
- `TaskDetailScreen` — task detail with activity feed
- `GoalDashboardScreen` — goals by tab
- `GoalDetailScreen` — goal editing

---

## 2. Findings

### F1: `editableTitle` Stale After Task Reload — CRITICAL

**File:** `TaskDetailScreen.kt`
**Location:** Line 110

```kotlin
var editableTitle by remember(task) { mutableStateOf(task?.title ?: "") }
```

**Problem:** `editableTitle` is initialized once from `task?.title` and stored in `remember`. When `task` updates (e.g., after editing the title in another screen), `editableTitle` does NOT update because `remember(task)` only re-initializes when the `task` reference changes — but `task` is a `StateFlow<TaskEntity?>` and the same `TaskEntity` instance may be re-emitted with an updated title.

**Impact:** User edits title → saves → navigates back → title field still shows OLD title.

**Severity:** Critical

---

### F2: `LaunchedEffect(scrollToMessageId, filteredActivityMessages)` — Repeated Execution — HIGH

**File:** `TaskDetailScreen.kt`
**Location:** Line 210

```kotlin
LaunchedEffect(scrollToMessageId, filteredActivityMessages) {
    val targetId = scrollToMessageId ?: return@LaunchedEffect
    val index = filteredActivityMessages.indexOfFirst { it.id == targetId }
    ...
}
```

**Problem:** `filteredActivityMessages` changes on EVERY activity event insertion/deletion. The `LaunchedEffect` restarts frequently — even when `scrollToMessageId` is null. The `return@LaunchedEffect` early exit handles null, but `indexOfFirst` O(n) runs on every emission.

**Impact:** Unnecessary O(n) scans on every activity feed update.

**Severity:** High

---

### F3: `TaskDetailScreen` — 18 Independent `mutableStateOf` Variables — HIGH Recomposition Risk

**File:** `TaskDetailScreen.kt`
**Location:** Lines 111-134

18 independent `mutableStateOf` variables at the top level of the composable. Changing ANY one (e.g., typing in `logInput`) causes the ENTIRE `TaskDetailScreen` to recompose — including the LazyColumn, goal dropdown, tag menus, etc.

**Impact:** Jank on every keystroke in text fields.

**Severity:** High

---

### F4: `PlannerScreen` — Inline `MutableStateFlow` Fallback Creates Orphaned Collectors — MEDIUM

**File:** `PlannerScreen.kt`
**Location:** Lines 76, 86

```kotlin
val insightState by (resolvedInsightVm?.insightState
    ?: MutableStateFlow(WeeklyInsightState(hasData = false)).asStateFlow()).collectAsState()
```

**Problem:** When `resolvedInsightVm` is null, a new `MutableStateFlow` is created inline. `collectAsState()` subscribes to this new flow each recomposition. Old subscriptions are never explicitly cancelled.

**Impact:** Potential memory leak from orphaned flow collectors.

**Severity:** Medium

---

### F5: `TaskDetailViewModel` — `filteredActivityMessages` Recomputes on Every Activity Event — MEDIUM

**File:** `TaskDetailViewModel.kt`
**Location:** Lines 141-149

```kotlin
val filteredActivityMessages: StateFlow<List<ActivityMessageModel>> = combine(
    activityMessages, _filterState, _selectedActivityDate
) { messages, filter, selectedDate -> applyFilter(messages, filter, selectedDate) }
```

**Problem:** `combine` re-emits whenever `activityMessages` changes (every activity event). `applyFilter()` runs O(n) on every event, even if the filter hasn't changed.

**Impact:** Unnecessary filtering on every activity event.

**Severity:** Medium

---

### F6: `WeeklyInsightViewModel` — No Error Handling in `combine` — MEDIUM

**File:** `WeeklyInsightViewModel.kt`
**Location:** Lines 54-140

The `combine` block processes 10 Room flows. If any single flow emits an error, the entire coroutine crashes and insight updates stop until ViewModel recreation.

**Severity:** Medium

---

### F7: `TaskDetailViewModel` — `creationContext` Doesn't React to `steps` Changes — MEDIUM

**File:** `TaskDetailViewModel.kt`
**Location:** Lines 228-244

`creationContext` reads `steps.value` inside `.map()` but only re-emits when `_filterState` changes. If a tag is renamed, `creationContext` won't update until the user changes the filter.

**Severity:** Medium

---

### F8: `PlannerScreen` — `selectedDateEpochMs` Collected Twice — LOW

**File:** `PlannerScreen.kt`
**Location:** Lines 50, 120-130

`selectedDateEpochMs` is collected directly AND indirectly through `goalTaskGroups` (which also depends on it). Two separate downstream collectors.

**Severity:** Low

---

### F9: `PlannerViewModel` — `recordDayDebounced` Job Not Cancelled on ViewModel Clear — LOW

**File:** `PlannerViewModel.kt`
**Location:** Lines 44-52

`viewModelScope` handles cleanup automatically, but rapid date changes across ViewModel recreation could miss a snapshot.

**Severity:** Low

---

## 3. Recomposition Risks

| Risk | Location | Trigger | Impact |
|------|----------|---------|--------|
| R1 | `TaskDetailScreen` — 18 mutableState vars | Any state change | Full screen recomposition |
| R2 | `TaskDetailScreen` — `editableTitle` | Task reload | Stale title |
| R3 | `PlannerScreen` — `filteredActivityMessages` | Every activity event | Unnecessary filtering |
| R4 | `TaskDetailViewModel` — `filteredActivityMessages` | Every activity event | `applyFilter()` O(n) per event |
| R5 | `PlannerScreen` — inline `MutableStateFlow` | Recomposition | Orphaned collector |
| R6 | `WeeklyInsightViewModel` — no error handling | Single flow error | Insight UI stops |

---

## 4. State Management Issues

### Duplicate Sources of Truth
- `TaskDetailViewModel._filterState` and `creationContext` — `creationContext` reads `steps.value` directly instead of being a pure transform of `_filterState`

### Wrong Ownership
- `TaskDetailScreen` — `editableTitle` is business data stored in Compose `remember` — should be in ViewModel

### Lifecycle Risks
- `PlannerScreen` — `LaunchedEffect(Unit)` for snackbar collection runs for composable lifetime
- `WeeklyInsightViewModel` — `observeInsight()` coroutines have no error handling

---

## 5. Performance Risks

### Expensive Operations in Composables
- `TaskDetailScreen` line 212: `filteredActivityMessages.indexOfFirst` — O(n) scan on every emission
- `PlannerScreen` line 232-236: `JalaliDate.toGregorian()` — memoized with `remember`, OK

### Large Rendering Risks
- `PlannerScreen` — LazyColumn groups have no `key` on group headers, only on items
- `TaskDetailScreen` — 18 independent state triggers full recomposition

### Memory Concerns
- `PlannerScreen` — inline `MutableStateFlow` fallback may not be GC'd immediately

---

## 6. Recommended Improvements

### P0: Real Bugs / Crash Risks
1. **F1/F13:** Move `editableTitle` to ViewModel or sync with `task.title` using `derivedStateOf`
2. **F6:** Add `try/catch` around `combine` block in `WeeklyInsightViewModel`

### P1: Important UX / Performance Issues
3. **F12:** Extract independent state groups into separate composables to limit recomposition scope
4. **F2/F3:** Separate `scrollToMessageId` logic into its own `LaunchedEffect`
5. **F5:** Add `distinctUntilChanged` to `filteredActivityMessages`
6. **F4/F5:** Extract fallback flows to `remember` blocks

### P2: Nice Improvements
7. Deduplicate `selectedDateEpochMs` collection
8. Make `creationContext` react to `steps` changes
9. Cache `groupTasksByGoal` result per date
10. Add `buffer()` on `_selectedDateEpochMs` for rapid date switching

---

## 7. Files Reviewed

| File | Sections |
|------|----------|
| `PlannerViewModel.kt` | StateFlow declarations, debounce, goalTaskGroups |
| `PlannerScreen.kt` | collectAsState, LaunchedEffect, inline flows |
| `TaskDetailViewModel.kt` | MutableStateFlow, filteredActivityMessages, creationContext |
| `TaskDetailScreen.kt` | remember/mutableStateOf, LaunchedEffect keys |
| `WeeklyInsightViewModel.kt` | observeInsight, combine block, error handling |
| `GoalDashboardScreen.kt` | collectAsState, LaunchedEffect |
| `GoalDetailViewModel.kt` | StateFlow declarations |
| `GoalViewModel.kt` | StateFlow declarations |

---

## 8. Summary

| Severity | Count |
|----------|-------|
| Critical | 2 |
| High | 3 |
| Medium | 5 |
| Low | 6 |
| **Total** | **16** |

# Database Layer — Performance Audit Report (Comprehensive)

> **Date:** 2026-08-02
> **Scope:** DAO queries, Repository patterns, Snapshot system, Flow efficiency, Index coverage, Migration safety
> **Status:** Read-only audit — no code changes implemented yet

---

## 0. Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  PlannerViewModel · GoalDetailViewModel · GoalViewModel     │
│  WeeklyInsightViewModel · TaskDetailViewModel                │
└──────────────────────────┬──────────────────────────────────┘
                           │ StateFlow / combine
┌──────────────────────────┴──────────────────────────────────┐
│                     Repository Layer                         │
│  TaskRepository · GoalRepository · InsightRepository        │
│  SnapshotRepository · ActivityEventRepository                │
│  TaskStepRepository · NoteRepository                         │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────┴──────────────────────────────────┐
│                        DAO Layer                             │
│  TaskDao · GoalDao · TaskEventDao · InsightDao               │
│  SnapshotDao · ActivityEventDao · TaskStepDao                │
│  GoalEventDao · NoteDao · ModuleSettingsDao                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────┴──────────────────────────────────┐
│                   Room Database (v15)                        │
│  10 tables · 10 DAOs · Manual DI (no Hilt/Koin)            │
└─────────────────────────────────────────────────────────────┘
```

### Tables
| Table | Rows (6mo est.) | Growth Rate | Primary Use |
|-------|-----------------|-------------|-------------|
| `goals` | 10–30 | Slow | Goal lifecycle |
| `tasks` | 360–1,080 | 2–6/day | Daily execution |
| `task_events` | 540–1,620 | 3/task lifecycle | Behavioral signal |
| `activity_events` | 180–540 | 1–3/day | User activities |
| `task_steps` | 0–200 | Optional | Tags/metadata |
| `goal_events` | 20–60 | Per lifecycle change | Goal audit trail |
| `goal_progress_snapshot` | 180 × N goals | 1/day/goal | Progress projection |
| `behavior_snapshot` | 180 | 1/day | Behavior projection |
| `notes` | 100–500 | Variable | User notes |
| `module_settings` | 1–5 | Rare | App settings |

---

## 1. Index Audit

### 1.1 Existing Indexes (Verified)

Room Entity annotations auto-create indices at table creation. Migrations added indices for existing tables during upgrades. Both are correct.

| Table | Column | Source | Covers |
|-------|--------|--------|--------|
| `tasks` | `goalId` | `@Entity(indices)` ✅ | `getTasksByGoalId()`, bulk JOINs |
| `tasks` | `dateEpochMs` | `@Entity(indices)` + Migration v10→11 ✅ | Day-scoped queries, BETWEEN |
| `task_events` | `taskId` | `@Entity(indices)` ✅ | `deleteEventsForGoal()` subquery |
| `task_events` | `eventType` | `@Entity(indices)` + Migration v10→11 ✅ | Reschedule/completion filter |
| `activity_events` | `taskId` | `@Entity(indices)` + Migration v12→13 ✅ | `observeByTaskId()` |
| `activity_events` | `timestamp` | `@Entity(indices)` + Migration v12→13 ✅ | Timestamp range queries |
| `goals` | `status` | `@Entity(indices)` + Migration v10→11 ✅ | Dashboard status filter |
| `goal_events` | `goalId` | Migration v7→8 ✅ | Goal lifecycle events |
| `goal_progress_snapshot` | `goalId` | Migration v8→9 ✅ | Goal progress observation |
| `task_steps` | `taskId` | Migration v13→14 ✅ | Steps by task |

**Total: 10 indexes across 7 tables.**

### 1.2 Missing Indexes

#### `task_events(timestamp)` — 🟡 MEDIUM

**Not indexed.** `eventType` is indexed but `timestamp` is not.

**Affected queries:**
```sql
-- observeRescheduleCountBetween — filters on eventType + timestamp range
SELECT COUNT(*) FROM task_events
WHERE eventType = 'rescheduled' AND timestamp BETWEEN :start AND :end

-- observeCompletedTimestamps — no timestamp filter, but results are used for streak
SELECT timestamp FROM task_events WHERE eventType = 'completed'
```

**Impact:** `observeRescheduleCountBetween` is called by `SnapshotAggregator.recordDay()` on every user action. Without a timestamp index, the BETWEEN filter requires a full scan of matching eventType rows.

**Recommendation:** Add composite index `(eventType, timestamp)` — covers both eventType-only queries AND eventType+timestamp range queries.

```sql
CREATE INDEX IF NOT EXISTS index_task_events_type_timestamp
ON task_events(eventType, timestamp)
```

**Storage cost:** Minimal. 3 columns × ~1000 rows = negligible.

---

#### `activity_events(stepId)` — 🟢 LOW

**Not indexed.**

**Affected queries:**
```sql
-- getEventsByStepId
SELECT * FROM activity_events WHERE stepId = :stepId ORDER BY timestamp DESC

-- clearStepId
UPDATE activity_events SET stepId = NULL WHERE stepId = :stepId
```

**Impact:** Only triggered on tag management operations (rare). StepId is nullable and low-cardinality.

**Recommendation:** Defer. Add only if tag filtering becomes a common operation.

---

### 1.3 Duplicate Index Check

**No duplicates found.** Entity annotations and migrations are complementary:
- Entity annotations create indices for **new installs** (Room creates table + indices)
- Migrations add indices for **upgrading users** (table exists without indices)

Both are necessary and correct.

---

## 2. Unbounded Historical Queries

### 2.1 `observeCompletedTimestamps()` — 🔴 CRITICAL

**DAO:**
```sql
SELECT timestamp FROM task_events WHERE eventType = 'completed'
```

**No date filter. No LIMIT.** Loads ALL completion timestamps ever recorded.

**Consumers:**
1. `WeeklyInsightViewModel.observeInsight()` → streak calculation
2. `SnapshotAggregator.recordDay()` → streak calculation per day

**How streak works (InsightCalculator.computeStreak):**
```kotlin
fun computeStreak(completedTimestamps: List<Long>, nowMillis: Long): Int {
    val daySet = completedTimestamps.map { ts -> dayKey(cal.apply { timeInMillis = ts }) }.toSet()
    // walks backward from today counting consecutive days
}
```

The streak only walks backward from today. A streak can't exceed ~365 days in practice. **The algorithm only needs timestamps from the last ~400 days** (safety margin). Anything older is wasted memory and query time.

**Growth projection:**
| Duration | Tasks/Day | Rows | Memory (approx) |
|----------|-----------|------|------------------|
| 1 month | 3 | ~90 | ~1 KB |
| 6 months | 3 | ~540 | ~6 KB |
| 1 year | 3 | ~1,080 | ~12 KB |
| 2 years | 3 | ~2,160 | ~24 KB |

At 2 years, the list is still small in absolute terms but grows linearly with no bound.

**Proposed fix:**
```sql
-- Add fromEpochMs parameter (e.g., 400 days ago)
SELECT timestamp FROM task_events
WHERE eventType = 'completed' AND timestamp >= :fromEpochMs
```

Both consumers already have access to `System.currentTimeMillis()`, so computing `fromEpochMs = now - 400L * DAY_MS` is trivial.

**Risk:** Very low. The 400-day window covers any realistic streak. No behavioral data is lost — older timestamps remain in the table for future AI analysis.

---

### 2.2 `observeRescheduleCounts()` — 🟡 MEDIUM

**DAO:**
```sql
SELECT te.taskId, t.title, t.timestamp, COUNT(*) AS rescheduleCount
FROM task_events te
JOIN tasks t ON t.id = te.taskId
WHERE te.eventType = 'rescheduled'
GROUP BY te.taskId
```

**No date filter.** Loads ALL rescheduled tasks globally, including completed/archived ones.

**Consumer:** `WeeklyInsightViewModel.observeInsight()` — procrastination alerts.

**The insight screen shows "tasks with ≥3 reschedules."** Old completed tasks with high reschedule counts are noise — the user already dealt with them.

**Proposed fix:**
```sql
-- Filter to tasks created in last 90 days (active concern window)
WHERE te.eventType = 'rescheduled'
AND t.timestamp > :ninetyDaysAgo
```

**Risk:** Low. Procrastination alerts for tasks older than 90 days are not actionable.

---

### 2.3 `observeAllEvents()` in GoalEventDao — 🟢 LOW

**DAO:**
```sql
SELECT * FROM goal_events ORDER BY timestamp DESC
```

**Consumer:** `RoomGoalRepository.observeGoalEvents()` — used by GoalDetailViewModel for lifecycle display.

**Impact:** Goal events are low-volume (20–60 in 6 months). No fix needed.

---

### 2.4 `getAllTasks()` in TaskDao — 🟢 LOW

**DAO:**
```sql
SELECT * FROM tasks ORDER BY id DESC
```

**Consumer:** Not found in active ViewModel code. Likely debug-only.

**Recommendation:** Verify usage. Remove if unused, or add LIMIT.

---

## 3. Snapshot System Efficiency

### 3.1 `recordDay()` — Called on Every User Action

**Trigger points (PlannerViewModel):**
- `addTask()` → `recordDay(today)`
- `toggleTaskCompletion()` → `recordDay(today)`
- `undoLastComplete()` → `recordDay(today)`
- `deleteTask()` → `recordDay(today)`
- `rescheduleTask()` → `recordDay(today)`

**What each `recordDay()` call executes:**

```
recordDay(dateEpochMs):
  1. observeCompletedCount(start, end)          — 1 query
  2. observeCreatedCount(start, end)            — 1 query
  3. observeCompletedTimestamps()                — 1 query (UNBOUNDED!)
  4. observeCompletedCount(prevStart, prevEnd)   — 1 query
  5. observeCreatedCount(prevStart, prevEnd)     — 1 query
  6. observeRescheduleCountBetween(start, end)   — 1 query
  7. getActiveGoals()                            — 1 query
  8. FOR EACH active goal:
     getGoalDayCounts(goalId, start, end)        — 1 query per goal
```

**Total: 7 + N queries** (N = active goals count)

| Active Goals | Total Queries/Action |
|--------------|---------------------|
| 3 | 10 |
| 5 | 12 |
| 10 | 17 |

**Problem:** Rapid user actions (completing 5 tasks quickly) trigger 5 separate `recordDay()` calls = 50–85 queries in quick succession.

### 3.2 Debouncing Opportunity — 🔴 HIGH PRIORITY

**Current:** No debouncing. Each action triggers immediate `recordDay()`.

**Proposed:**
```kotlin
private var recordDayJob: Job? = null

fun recordDayDebounced(dateEpochMs: Long) {
    recordDayJob?.cancel()
    recordDayJob = viewModelScope.launch {
        delay(300) // coalesce rapid actions
        snapshotAggregator.recordDay(dateEpochMs)
    }
}
```

**Impact:** 5 rapid actions → 1 `recordDay()` call instead of 5. Reduces query count by ~80% during active use.

**Risk:** Low. The 300ms delay is imperceptible. Snapshot is a projection, not user-facing data.

---

### 3.3 `backfillMissing()` — Linear Growth

**Algorithm:**
```
FOR each day from earliest task to today:
    IF day NOT in coveredDates:
        recordDay(day)  — 7 + N queries
```

**Growth:**
| History | Days | Active Goals | Total Queries |
|---------|------|--------------|---------------|
| 1 month | 30 | 3 | ~300 |
| 6 months | 180 | 5 | ~2,160 |
| 1 year | 365 | 5 | ~4,380 |

**Trigger:** App launch when `latest.dateEpochMs < today`. First launch after DB upgrade or fresh install with historical data.

**Optimization opportunities:**
1. **Batch in transaction:** Wrap all `recordDay()` calls in a single Room `@Transaction` — reduces WAL overhead.
2. **Limit backfill window:** Backfill only last 90 days. Older snapshots can be regenerated on demand.
3. **Progressive backfill:** Backfill in chunks (e.g., 30 days per launch) to avoid startup lag.

**Recommendation:** Implement option 2 (limit to 90 days) as the simplest safe fix.

---

### 3.4 Snapshot Redundancy Check

**Question:** Do multiple observers trigger redundant snapshot calculations?

**Analysis:**
- `SnapshotAggregator.recordDay()` is called explicitly by ViewModel actions — NOT by Room Flow observation.
- Room Flows (`observeGoalProgress`, `observeBehaviorRange`) are READ-ONLY — they don't trigger writes.
- No double-write pattern found. ✅

**However:** The `recordDay()` call uses `Flow.first()` internally (one-shot reads):
```kotlin
val completed = insightRepository.observeCompletedCount(start, end).first()
```

Each `.first()` creates a one-shot subscription. **7 one-shot subscriptions per call.** This is fine for individual calls but adds up during rapid actions.

---

## 4. Flow / Repository Query Audit

### 4.1 `WeeklyInsightViewModel` — 10-Flow Combine

```kotlin
combine(
    observeCompletedCount,          // 1 — window-scoped ✅
    observeCreatedCount,            // 2 — window-scoped ✅
    observeCompletionByLifeArea,    // 3 — window-scoped ✅
    observeUnorganizedCount,        // 4 — window-scoped ✅
    observeCompletedTimestamps,     // 5 — UNBOUNDED 🔴
    observeCompletionByDay,         // 6 — window-scoped ✅
    observeRescheduleCounts,        // 7 — UNBOUNDED 🟡
    observeGoalCompletionRates,     // 8 — global (acceptable, low volume) ✅
    observePreviousWeekCompleted,   // 9 — window-scoped ✅
    observePreviousWeekCreated      // 10 — window-scoped ✅
)
```

**Problem:** Every change to `tasks` or `task_events` triggers ALL 10 queries to re-run. The insight screen shows weekly data that doesn't need real-time updates.

**Proposed fix:** Add `.debounce(300)` before `.collect`:
```kotlin
combine(/* 10 flows */) { results -> ... }
    .debounce(300)  // coalesce rapid changes
    .collect { _insightState.value = it }
```

**Impact:** Reduces re-computation during rapid user actions.

---

### 4.2 `GoalDetailViewModel` — Graph Pipeline

```kotlin
combine(goal, allTasks, goalProgress, _visibilityLevel) { g, ts, progress, level ->
    // Attention computation
    // VisibilityResolver
    // Triple(visibleGraph, title, progress)
}
```

**Analysis:** This combine runs on every `allTasks` emission (any task change for this goal). The graph is only visible when the sheet is open, and `startGraphComputation()` / `stopGraphComputation()` properly gate collection. ✅

**However:** The `graphSource()` flow uses `combine(goal, allTasks, goalProgress, _visibilityLevel)` — 4 upstream flows. When any one changes, the entire pipeline re-runs. This includes attention computation and visibility resolution.

**Optimization:** The graph sheet already has lazy computation. No immediate fix needed, but consider `.distinctUntilChanged()` on the output if recomputation becomes expensive.

---

### 4.3 `PlannerViewModel` — Task List

```kotlin
val tasks: StateFlow<List<TaskEntity>> = _selectedDateEpochMs
    .flatMapLatest { date -> repository.getTasksForDay(date) }
    .onEach { _isTasksLoaded.value = true }
    .stateIn(...)
```

**Analysis:** `flatMapLatest` properly cancels previous query when date changes. `getTasksForDay` is a single-day query (bounded). ✅

**No issues found.**

---

### 4.4 `GoalViewModel` — Dashboard

```kotlin
val goalsByTab: StateFlow<List<DashboardGoalItem>> = _selectedTab
    .flatMapLatest { status ->
        combine(
            observeGoalsByStatus(status),           // 1
            observeGoalCompletionRatesByStatus(status), // 2
            observeGoalActivityBulk(status),        // 3
            observeGoalActivityBulkInWindow(status, from, to) // 4
        ) { ... }
    }
```

**Analysis:** 4 bulk queries combined. All use `GROUP BY` — efficient. `flatMapLatest` cancels on tab switch. ✅

**No issues found.**

---

### 4.5 `TaskDetailViewModel` — Activity Feed

```kotlin
val activities: StateFlow<List<ActivityEventEntity>> = activityEventRepository
    .observeActivities(taskId)  // observeByTaskId
    .stateIn(...)

val activityMessages: StateFlow<List<ActivityMessageModel>> = activities
    .map { ActivityMessageMapper.toMessages(it) }
    .stateIn(...)
```

**Analysis:** `observeByTaskId` filters by `taskId` (indexed). Activity feed is per-task, not global. ✅

**However:** The filter is applied in-memory after loading all activities:
```kotlin
val filteredActivityMessages = combine(activityMessages, _filterState, _selectedActivityDate) {
    messages, filter, selectedDate -> applyFilter(messages, filter, selectedDate)
}
```

**For tasks with many activities (100+),** the in-memory filter re-runs on every filter change. This is acceptable because:
1. Activities are per-task (bounded by task lifetime)
2. Filter changes are user-initiated (not rapid)
3. The list is already loaded and cached in `activityMessages`

---

## 5. Large List Safety

### 5.1 Activity Feed (Global)

**Finding:** There is NO global `getAllActivities()` query. Activity feeds are always scoped by `taskId`. ✅

The `ActivityEventDao` has `observeByTaskId(taskId)` — always filtered. No unbounded global activity query exists.

### 5.2 Task Lists

| Query | Scope | Bounded? |
|-------|-------|----------|
| `getAllTasks()` | Global | ❌ No LIMIT — but not used in active UI |
| `getTasksForDay(date)` | Single day | ✅ |
| `getTasksBetween(start, end)` | Date range | ✅ |
| `getTasksByGoalId(goalId)` | Per goal | ✅ |
| `getRecentTasks(limit)` | Limited | ✅ |
| `getTaskDayKeysBetween(start, end)` | Projection only | ✅ |

### 5.3 Goal Lists

| Query | Scope | Bounded? |
|-------|-------|----------|
| `getAllGoals()` | Global | ❌ No LIMIT — but goals are low-volume (10–30) |
| `getActiveGoals()` | Status-filtered | ✅ |
| `getGoalsByStatus(status)` | Status-filtered | ✅ |

### 5.4 Snapshot Lists

| Query | Scope | Bounded? |
|-------|-------|----------|
| `observeGoalProgress(goalId)` | Per goal | ✅ |
| `observeBehaviorRange(start, end)` | Date range | ✅ |
| `getCoveredDates()` | All dates | ❌ No LIMIT — but used only in backfill check |

### 5.5 Summary

No dangerous unbounded lists in the active UI path. The only unbounded queries are:
1. `observeCompletedTimestamps()` — **fix proposed in §2.1**
2. `observeRescheduleCounts()` — **fix proposed in §2.2**
3. `getAllTasks()` — verify usage, remove if unused
4. `getCoveredDates()` — acceptable (low volume)

---

## 6. Migration Safety

### 6.1 Current Schema Version: 15

Migration chain: 5→6→7→8→9→10→11→12→13→14→15

All migrations are additive or non-destructive:
- v5→6: ADD COLUMN (dateEpochMs)
- v6→7: Recreate table (DROP COLUMN not supported)
- v7→8: CREATE TABLE (goal_events)
- v8→9: CREATE TABLE (snapshots)
- v9→10: ADD COLUMN (why, deadlineEpochMs)
- v10→11: CREATE INDEX (performance)
- v11→12: ADD COLUMN (deadlineEpochMs on tasks)
- v12→13: CREATE TABLE (activity_events)
- v13→14: CREATE TABLE (task_steps)
- v14→15: ADD COLUMN (colorHex)

### 6.2 Proposed v15→v16 Migration

**Change:** Add composite index on `task_events(eventType, timestamp)`

```sql
CREATE INDEX IF NOT EXISTS index_task_events_type_timestamp
ON task_events(eventType, timestamp)
```

**Risk assessment:**
- ✅ Additive only — no schema change, no data loss
- ✅ `CREATE INDEX IF NOT EXISTS` is idempotent
- ✅ Covers existing `eventType` index usage AND adds timestamp range support
- ✅ `fallbackToDestructiveMigration()` remains as safety net
- ✅ No entity annotation change needed (index is migration-only for backward compatibility)

**Rollback risk:** None. Index can be dropped in a future migration if needed.

### 6.3 Entity Annotation Consideration

The proposed composite index `(eventType, timestamp)` is NOT added to `@Entity(indices=...)` because:
1. The Entity already has `Index("taskId")` and `Index("eventType")`
2. Room creates indices from Entity annotations at table creation
3. The composite index is for optimization only — not required for correctness
4. Keeping it migration-only avoids changing the Entity definition

---

## 7. Recommended Implementation Plan

### Priority 1: Debounce `recordDay()` (High Impact, Low Risk)

**File:** `PlannerViewModel.kt`
**Change:** Replace direct `snapshotAggregator.recordDay()` calls with debounced version
**Impact:** ~80% reduction in snapshot queries during rapid user actions
**Risk:** Very low — snapshot is a projection, 300ms delay is imperceptible

### Priority 2: Bound `observeCompletedTimestamps()` (High Impact, Low Risk)

**File:** `InsightDao.kt`, `InsightRepository.kt`, `RoomInsightRepository.kt`, `SnapshotAggregator.kt`
**Change:** Add `fromEpochMs` parameter (400 days ago)
**Impact:** Caps streak calculation input at ~400 days
**Risk:** Very low — streak can't exceed 365 days

### Priority 3: Add Composite Index (Medium Impact, Zero Risk)

**File:** `AppDatabase.kt` (new migration v15→v16)
**Change:** `CREATE INDEX index_task_events_type_timestamp ON task_events(eventType, timestamp)`
**Impact:** Faster `observeRescheduleCountBetween()` and `observeCompletedTimestamps()`
**Risk:** None — additive only

### Priority 4: Bound `observeRescheduleCounts()` (Medium Impact, Low Risk)

**File:** `InsightDao.kt`, `InsightRepository.kt`, `RoomInsightRepository.kt`
**Change:** Filter to tasks created in last 90 days
**Impact:** Reduces procrastination alert computation
**Risk:** Low — old completed tasks with high reschedules are not actionable

### Priority 5: Debounce WeeklyInsightViewModel (Low Impact, Zero Risk)

**File:** `WeeklyInsightViewModel.kt`
**Change:** Add `.debounce(300)` before `.collect`
**Impact:** Reduces re-computation during rapid actions
**Risk:** None

### Priority 6: Verify `getAllTasks()` Usage (Cleanup)

**File:** `TaskDao.kt`
**Action:** Search for callers. Remove if unused.

---

## 8. What NOT to Change

- ✅ Do not remove event tables (`task_events`, `activity_events`, `goal_events`)
- ✅ Do not remove snapshot tables (`goal_progress_snapshot`, `behavior_snapshot`)
- ✅ Do not change business logic in calculators
- ✅ Do not change Entity definitions unless absolutely necessary
- ✅ Do not add pagination to per-task activity feeds (already bounded)
- ✅ Do not change the snapshot projection philosophy (rebuildable, not authoritative)
- ✅ Do not introduce WorkManager or network calls

---

## 9. Files Affected (Proposed Changes)

| File | Change | Priority |
|------|--------|----------|
| `PlannerViewModel.kt` | Debounce `recordDay()` | P1 |
| `GoalDetailViewModel.kt` | Use debounced `recordDay()` | P1 |
| `InsightDao.kt` | Add `fromEpochMs` to `observeCompletedTimestamps` | P2 |
| `InsightRepository.kt` | Update interface signature | P2 |
| `RoomInsightRepository.kt` | Update implementation | P2 |
| `SnapshotAggregator.kt` | Pass `fromEpochMs` to `observeCompletedTimestamps` | P2 |
| `AppDatabase.kt` | Add migration v15→v16 (composite index) | P3 |
| `InsightDao.kt` | Add `ninetyDaysAgo` to `observeRescheduleCounts` | P4 |
| `WeeklyInsightViewModel.kt` | Add `.debounce(300)` | P5 |

---

*Report complete. Awaiting review before implementation.*

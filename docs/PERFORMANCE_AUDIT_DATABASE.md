# Database Layer — Performance Audit Report

> **Date:** 2026-08-02
> **Scope:** Database queries, DAOs, indexes, snapshot system, ViewModel data flow
> **Status:** Read-only audit — no code changes

---

## Executive Summary

The database layer is **architecturally sound** — N+1 patterns were eliminated in Phase 5.4, bulk queries use `GROUP BY`, and ViewModel lazy computation is well-designed. However, with growing data volume, **3 categories of risk** will cause noticeable lag within 3–6 months of active use:

1. **Missing indexes** on frequently queried columns
2. **Unbounded queries** that load entire tables
3. **Snapshot backfill cost** that grows linearly with usage history

---

## 1. Current Index Inventory

| Table | Index | Added In | Covers |
|-------|-------|----------|--------|
| `tasks` | `dateEpochMs` | v10→v11 | Day-scoped queries, insight BETWEEN |
| `tasks` | — | — | Goal lookup, completion filter |
| `task_events` | `eventType` | v10→v11 | Reschedule/completion aggregation |
| `task_events` | — | — | Timestamp range, taskId subquery |
| `goals` | `status` | v10→v11 | Status-filtered dashboard loads |
| `goal_events` | `goalId` | v7→v8 | Goal lifecycle events |
| `goal_progress_snapshot` | `goalId` | v8→v9 | Goal progress observation |
| `activity_events` | `taskId` | v12→v13 | Activity by task |
| `activity_events` | `timestamp` | v12→v13 | Timestamp range queries |
| `task_steps` | `taskId` | v13→v14 | Steps by task |

**Total: 9 indexes across 5 tables.**

---

## 2. Missing Indexes (High Priority)

### 2.1 `tasks(goalId)` — 🔴 CRITICAL

**Impact:** Every Goal Detail screen load triggers `getTasksByGoalId()` which does a full table scan on `tasks`.

**Query:**
```sql
SELECT * FROM tasks WHERE goalId = :goalId ORDER BY id DESC
```

**Frequency:** Every time a user opens any Goal Detail screen. Also used by `allTasks` in `GoalDetailViewModel` which powers the graph, mirror, calendar, and task list.

**Growth:** 6 months → ~360 tasks. Full scan on 360 rows is fast, but at 2000+ rows (1–2 years of heavy use) it becomes noticeable.

**Fix:** Add in next migration (v15→v16):
```sql
CREATE INDEX IF NOT EXISTS index_tasks_goalId ON tasks(goalId)
```

---

### 2.2 `task_events(timestamp)` — 🟡 MEDIUM

**Impact:** `observeCompletedTimestamps()` and `observeRescheduleCountBetween()` filter by timestamp range without an index.

**Queries:**
```sql
-- observeCompletedTimestamps — NO timestamp filter, but used in streak calc
SELECT timestamp FROM task_events WHERE eventType = 'completed'

-- observeRescheduleCountBetween — timestamp BETWEEN
SELECT COUNT(*) FROM task_events
WHERE eventType = 'rescheduled' AND timestamp BETWEEN :start AND :end
```

**Frequency:** `observeRescheduleCountBetween` called by `SnapshotAggregator.recordDay()` on every user action. `observeCompletedTimestamps` called on every snapshot computation.

**Fix:** Add index:
```sql
CREATE INDEX IF NOT EXISTS index_task_events_timestamp ON task_events(timestamp)
```

---

### 2.3 `activity_events(stepId)` — 🟢 LOW

**Impact:** `getEventsByStepId()` and `clearStepId()` filter by stepId without index.

**Frequency:** Only on tag management operations (rare).

**Fix:** Add index:
```sql
CREATE INDEX IF NOT EXISTS index_activity_events_stepId ON activity_events(stepId)
```

---

### 2.4 Composite Index for Snapshot Queries — 🟡 MEDIUM

**Impact:** `observeRescheduleCountBetween()` filters on both `eventType` AND `timestamp`. A composite index would be more efficient than two separate indexes.

**Fix:**
```sql
CREATE INDEX IF NOT EXISTS index_task_events_type_time
ON task_events(eventType, timestamp)
```

This covers both the eventType-only queries AND the eventType+timestamp range queries.

---

## 3. Unbounded Queries

### 3.1 `observeCompletedTimestamps()` — 🔴 CRITICAL

**Current behavior:**
```sql
SELECT timestamp FROM task_events WHERE eventType = 'completed'
```

Loads **ALL** completion timestamps ever recorded. No LIMIT, no date filter.

**Growth projection:**
| Usage Duration | Tasks/Day | Rows Returned |
|----------------|-----------|---------------|
| 1 month | 3 | ~90 |
| 6 months | 3 | ~540 |
| 1 year | 3 | ~1,080 |
| 2 years | 3 | ~2,160 |

**Consumers:**
- `WeeklyInsightViewModel.observeInsight()` — streak calculation
- `SnapshotAggregator.recordDay()` — streak calculation per day
- Streak is computed from timestamps grouped by day

**The streak only needs the last ~60 days** (a streak can't be longer than 60 days in the window used). The rest of the data is waste.

**Proposed fix:**
```sql
-- Only load last 90 days of completion timestamps (covers any realistic streak)
SELECT timestamp FROM task_events
WHERE eventType = 'completed'
AND timestamp > :ninetyDaysAgo
```

Add a `ninetyDaysAgo` parameter calculated as `System.currentTimeMillis() - 90 * DAY_MS`.

---

### 3.2 `observeRescheduleCounts()` — 🟡 MEDIUM

**Current behavior:**
```sql
SELECT te.taskId, t.title, t.timestamp, COUNT(*) AS rescheduleCount
FROM task_events te
JOIN tasks t ON t.id = te.taskId
WHERE te.eventType = 'rescheduled'
GROUP BY te.taskId
```

Loads **ALL** rescheduled tasks globally. Used by WeeklyInsightViewModel.

**Growth projection:** Rescheduled tasks accumulate forever. Even completed/archived tasks remain in the result.

**Proposed fix:** Filter to recent tasks only (e.g., tasks created in last 90 days):
```sql
WHERE te.eventType = 'rescheduled'
AND t.timestamp > :ninetyDaysAgo
```

---

### 3.3 `getAllEvents()` and `getAllTasks()` — 🟢 LOW

**Current behavior:**
```sql
-- TaskEventDao
SELECT * FROM task_events ORDER BY timestamp DESC

-- TaskDao
SELECT * FROM tasks ORDER BY id DESC
```

These load **everything** but I couldn't find active UI consumers. They may be debug-only.

**Proposed fix:** Remove if unused, or add LIMIT.

---

## 4. Snapshot System Analysis

### 4.1 `recordDay()` Cost

Called on **every user action** (task add/toggle/delete/reschedule). Each call:

```
recordDay(dateEpochMs):
  1. observeCompletedCount(start, end)     — 1 query
  2. observeCreatedCount(start, end)       — 1 query
  3. observeCompletedTimestamps()           — 1 query (UNBOUNDED!)
  4. observeCompletedCount(prevDay)         — 1 query
  5. observeCreatedCount(prevDay)           — 1 query
  6. observeRescheduleCountBetween()        — 1 query
  7. getActiveGoals()                       — 1 query
  8. FOR EACH goal: getGoalDayCounts()     — N queries
```

**Total: 7 + N queries per user action** (where N = number of active goals).

For a user with 5 active goals: **12 queries per action**. For 10 goals: **17 queries**.

### 4.2 `backfillMissing()` Cost

```
backfillMissing():
  FOR each day from earliest task to today:
    recordDay(day)  — 7 + N queries per day
```

**Growth projection:**
| History | Days | Active Goals | Total Queries |
|---------|------|--------------|---------------|
| 1 month | 30 | 3 | ~300 |
| 6 months | 180 | 5 | ~2,160 |
| 1 year | 365 | 5 | ~4,380 |

First launch after DB upgrade triggers full backfill. 6 months of history = ~2,160 queries in a single suspend function.

### 4.3 `recordDay()` on Every Action — Batching Opportunity

Currently, rapid user actions (e.g., completing 5 tasks quickly) trigger 5 separate `recordDay()` calls, each doing 7+ queries. The snapshot for today is the same regardless of which action triggered it.

**Proposed fix:** Debounce `recordDay()` calls. Use a debounce window (e.g., 500ms) so rapid actions coalesce into a single snapshot refresh.

```kotlin
private var recordDayJob: Job? = null

fun recordDayDebounced(dateEpochMs: Long) {
    recordDayJob?.cancel()
    recordDayJob = viewModelScope.launch {
        delay(500) // debounce window
        recordDay(dateEpochMs)
    }
}
```

---

## 5. WeeklyInsightViewModel — 10-Flow Combine

### Current:
```kotlin
combine(
    observeCompletedCount,          // 1
    observeCreatedCount,            // 2
    observeCompletionByLifeArea,    // 3
    observeUnorganizedCount,        // 4
    observeCompletedTimestamps,     // 5 — UNBOUNDED
    observeCompletionByDay,         // 6
    observeRescheduleCounts,        // 7 — UNBOUNDED
    observeGoalCompletionRates,     // 8
    observePreviousWeekCompleted,   // 9
    observePreviousWeekCreated      // 10
)
```

### Problem:
Every change to `tasks` or `task_events` triggers ALL 10 queries to re-run. The screen shows weekly data that doesn't change that frequently.

### Proposed fix:
1. **Debounce the combine output** — add `.debounce(300)` before `.collect`
2. **Scope queries to current/previous week** where possible (some already are)
3. **Cap `observeCompletedTimestamps`** to last 90 days (see §3.1)

---

## 6. Data Volume Projections (6-Month Active Use)

| Table | Estimated Rows | Growth Rate |
|-------|----------------|-------------|
| `tasks` | 360–1,080 | 2–6/day |
| `task_events` | 540–1,620 | 3/task lifecycle |
| `activity_events` | 180–540 | 1–3/day |
| `task_steps` | 0–200 | Optional |
| `goal_progress_snapshot` | 180 × N goals | 1/day/goal |
| `behavior_snapshot` | 180 | 1/day |
| `goals` | 10–30 | Slow |
| `goal_events` | 20–60 | Per lifecycle change |

**At these volumes, the missing indexes and unbounded queries won't cause visible lag yet.** But at 1–2 years of heavy use (2,000+ tasks, 3,000+ events), the issues become noticeable.

---

## 7. Recommended Priority Order

### Priority 1 (Before Phase 7) — Schema Migration
Add missing indexes in a single v15→v16 migration:
```sql
CREATE INDEX IF NOT EXISTS index_tasks_goalId ON tasks(goalId);
CREATE INDEX IF NOT EXISTS index_task_events_timestamp ON task_events(timestamp);
CREATE INDEX IF NOT EXISTS index_task_events_type_time ON task_events(eventType, timestamp);
CREATE INDEX IF NOT EXISTS index_activity_events_stepId ON activity_events(stepId);
```

### Priority 2 (Before Phase 7) — Bound Unbounded Queries
Cap `observeCompletedTimestamps()` to last 90 days. This is the single highest-impact fix.

### Priority 3 (Before Phase 7) — Snapshot Debouncing
Debounce `recordDay()` to coalesce rapid user actions.

### Priority 4 (Nice to have) — WeeklyInsight Debounce
Add `.debounce(300)` to the 10-flow combine output.

### Priority 5 (Deferred) — Snapshot Backfill Optimization
Batch backfill by processing multiple days in a single transaction, or limit backfill to last 90 days.

---

## 8. What's Already Good (Don't Touch)

- ✅ Bulk GROUP BY queries in `observeGoalActivityBulk` / `observeGoalActivityBulkInWindow`
- ✅ Projection query `getTaskDayKeysBetween` (avoids full entity materialization)
- ✅ Lazy Graph/Mirror computation in `GoalDetailViewModel`
- ✅ `WhileSubscribed(5000)` on all StateFlows
- ✅ `flatMapLatest` for date-driven task queries (proper cancellation)
- ✅ `stateIn` with `emptyList()` initial values (no blocking)
- ✅ Repository abstraction layer (thin delegation, no logic duplication)

---

*This report covers Database Layer only. UI/Compose and Domain/Processing audits are separate.*

# ADR-0013 — GoalDetail Loading Path Performance Audit & Sprint 3 Plan

- **Status:** Accepted
- **Date:** 2026-07-19
- **Phase:** Performance Sprint 2 (Audit) → Performance Sprint 3 (Plan)
- **Relates:** ADR-0009 (Performance Audit 5.4), ADR-0012 (Sprint 1 Lazy Computation)

## Context

After Sprint 1 lazy computation (ADR-0012) removed eager Graph and Mirror work, the app is
noticeably better, but **opening Goal Detail still has a perceptible delay**. This audit traces the
complete Goal Detail loading path (Compose → ViewModel → Repository → Room DAO) to find the remaining
bottlenecks. The fix is split into Sprint 3 (this ADR defines it). No code is changed by this ADR.

## Current Loading Pipeline (as built after Sprint 1)

```
GoalDetailScreen opens (viewModel key="goal_detail_$goalId")
  └─ GoalDetailViewModel.init
       ├─ repositories wired (Goal/Tasks/Insight/Snapshot/Mirror/GraphPrefs)
       └─ debounced Mirror collector launched (gated by _showMirrorSheet → does nothing while closed)
  └─ 8 reactive StateFlows begin cold collection (stateIn, WhileSubscribed(5000)):
       goal            → GoalDao.observeGoalById                  (PK)
       tasks           → TaskDao.getTasksByGoalId  (SELECT *)     (index_tasks_goalId)
       goalRate        → InsightDao.observeGoalCompletionRate      (PK join)
       activeDaysInWindow → GoalDao.observeGoalActiveDayCountInWindow (index_tasks_dateEpochMs)
       activeDays      → GoalDao.observeGoalActiveDayCount         (index_tasks_goalId, all-time)
       lastActivity    → GoalDao.observeGoalLastActivity           (index_tasks_goalId)
       rescheduleCounts→ InsightDao.observeRescheduleCountsByGoal  (task_events join)
       goalProgress    → combine(goalRate, activeDaysInWindow)     (pure math)
  └─ mirrorState  = NotRequested   (lazy — no query on open)
  └─ goalGraphState = NotRequested (lazy — no graph build on open)
  └─ GoalDetailScreen collects 11 StateFlows; PlannerViewModel.daysWithTasks (±60d scan) also loads.
```

## Part 1 — ViewModel Flow Audit

- **StateFlows exposed by `GoalDetailViewModel`: 13.**
  8 are reactive open-path sources (`goal`, `tasks`, `goalRate`, `activeDaysInWindow`,
  `goalProgress`, `activeDays`, `lastActivity`, `rescheduleCounts`); 2 lazy states
  (`mirrorState`, `goalGraphState`); 3 UI flags (`showMirrorSheet`, `showGraphSheet`,
  `showGraphEducation`).
- **Collected in `GoalDetailScreen`: 11** (`goal`, `tasks`, `goalRate`, `goalProgress`,
  `activeDays`, `lastActivity`, `mirrorState`, `showMirrorSheet`, `goalGraphState`,
  `showGraphSheet`, `showGraphEducation`) plus `daysWithTasks` from `PlannerViewModel`.
- **Emit immediately on open:** the 8 reactive open-path sources each fire a cold query.
- **Recomposition-storm risk:** 11 top-level `collectAsState()` calls. Each emission re-runs the
  entire `GoalDetailScreen` composable. None are `derivedStateOf` or hoisted into sub-trees, so a
  single `tasks` toggle re-renders the whole screen function. Lazy states (`mirrorState`,
  `goalGraphState`) do not emit on open, so they add no initial cost.

## Part 2 — Room Query Audit

**7 DB queries fire on Goal Detail open** (concurrent — each `stateIn` subscriber triggers its own
cold Flow; Room runs them on its shared executor, so they serialize on the single Room query thread
but do not block the Main thread):

| # | Query | DAO | Index | Note |
|---|---|---|---|---|
| 1 | `observeGoalById` | GoalDao:26 | PK `id` | trivial |
| 2 | `getTasksByGoalId` (`SELECT *`) | TaskDao:32 | `index_tasks_goalId` ✅ | **returns full TaskEntity rows** |
| 3 | `observeGoalCompletionRate(goalId)` | InsightDao:147 | PK join | cheap GROUP BY |
| 4 | `observeGoalActiveDayCountInWindow` | GoalDao:73 | `index_tasks_dateEpochMs` ✅ | windowed COUNT DISTINCT |
| 5 | `observeGoalActiveDayCount` | GoalDao:60 | `index_tasks_goalId` ✅ | **all-time** COUNT DISTINCT |
| 6 | `observeGoalLastActivity` | GoalDao:51 | `index_tasks_goalId` ✅ | MAX |
| 7 | `observeRescheduleCountsByGoal` | InsightDao:113 | `task_events.eventType`✅ + `tasks.goalId`✅ | **join over task_events** |

- **Expensive / over-fetching:**
  - **#2** materializes full `TaskEntity` rows for the whole goal (only a few columns are rendered).
  - **#5** scans **all-time** task history for the goal (no window) — cost grows with goal age.
  - **#7** is the key waste: `rescheduleCounts` feeds **only** `graphSource`, which is collected
    solely when the Graph sheet opens. Yet it is a standalone `stateIn`, so it runs a `task_events`
    join on **every** Goal Detail open even though Graph is lazy.
- **Missing indexes:** none. `tasks.goalId`, `tasks.dateEpochMs`, `goals.status`,
  `task_events.eventType`, `task_events.taskId` all indexed.

## Part 3 — Compose Rendering Audit

- **Unnecessary recompositions:** 11 `collectAsState()` at the top of `GoalDetailScreen` → every
  individual flow emission re-runs the entire screen function. `LazyColumn` rows are keyed
  (`key = { it.id }`) so list items are stable, but the surrounding function (top bar, progress card,
  metadata) re-evaluates. No `derivedStateOf` / sub-composable state hoisting.
- **Unstable parameters:** `GoalDetailTaskRow`, `MetricCard`, `StatusChip`, `MirrorSheetContent`,
  `GoalProgressCard` all take stable types (data classes / primitives). `onToggle/onEdit/onDelete`
  lambdas are recreated per composition but consumed inside keyed `items` — acceptable.
- **Expensive first-render composables:** `NeumorphicSurface` (elevation shadow) wraps each task row
  and the progress card — shadow rendering is the heaviest draw; dominant first-frame cost at scale.
  `JalaliDate.fromEpochMs` called twice per row for deadlines (cheap).
- **Animations overlapping initial load:** none. Graph sheet animations run only when the sheet is
  open (lazy). No entrance animation competes with initial data load. Good.

## Part 4 — Architecture Recommendation

**P2 (merge always-on flows into a single `GoalDetailUiState`) is recommended as the MEDIUM-TERM
architecture direction**, but its full migration is explicitly **out of scope for Sprint 3** — it is
introduced incrementally (Phase 3.2 below) and only for the always-on subset.

```kotlin
data class GoalDetailUiState(
    val goal: GoalEntity?,
    val tasks: List<TaskEntity>,
    val progress: GoalProgress?,
    val completionRate: Float?,
    val activeDays: Int,
    val lastActivity: Long?
)
```

- **Pros:** one `collectAsState()` → one recomposition per change instead of up to 7; single combined
  `combine` lets us gate `rescheduleCounts`/`activeDaysInWindow` behind Graph/Mirror visibility;
  clearer single source of truth; easier snapshot testing.
- **Cons:** larger diff; must preserve `WhileSubscribed(5000)` caching; existing call sites
  (`GoalProgressCard`, `GoalDetailTaskRow`) need rewiring; risk to the working lazy graph/mirror gating.
- **Decision:** adopt `GoalDetailUiState` incrementally. **GraphState and MirrorState stay separate**
  — they are lazy, event-driven states (NotRequested → Loading → Ready), not continuous open-path
  data, and merging them would re-couple them to the open path we just decoupled.

## Priority Ranking (Sprint 3 plan follows)

| Rank | Fix | Target | Effort | Win |
|---|---|---|---|---|
| P1 | Defer `rescheduleCounts` to Graph-sheet open | #7 query + join off open path | Low | Removes 1 DB query + join on every open |
| P2 | Single `GoalDetailUiState` for always-on fields | Recomposition storms | Med | One recomposition instead of up to 7 |
| P3 | Localize recomposition boundaries (hoist sub-composables) | Recomps | Med | Smaller recomposition scope |
| P4 (later) | `getTasksByGoalId` projection instead of `SELECT *` | Over-fetch #2 | Low | Less entity materialization |
| P5 (later) | Bound `observeGoalActiveDayCount` to recent window | #5 all-time scan | Low | Cheaper stat for old goals |

---

# Sprint 3 Implementation Plan

**Goal:** eliminate the Goal Detail open delay by (1) removing the unnecessary eager `rescheduleCounts`
query, (2) merging always-on flows into a single `GoalDetailUiState`, and (3) localizing Compose
recomposition. Graph/Mirror lazy states remain untouched.

## Phase 3.1 — Defer `rescheduleCounts` until Graph sheet opens

**Objective:** remove `InsightDao.observeRescheduleCountsByGoal` from the Goal Detail initial load.

**Changes (`GoalDetailViewModel.kt`):**
- Remove the standalone `rescheduleCounts: StateFlow<Map<Int,Int>>` (`:163-166`) and its `stateIn`.
- In `startGraphComputation()` (called from `setGraphSheetVisible(true)`), before collecting
  `graphSource`, fetch the reschedule counts on demand and pass them into the graph build. Two options:
  - **(3.1.a)** Make `graphSource` a `Flow` that is built lazily by first collecting
    `insightRepository.observeRescheduleCountsByGoal(goalId)` once (`.first()`) and combining with
    `goal`/`tasks`/`goalProgress` only after the sheet opens.
  - **(3.1.b)** Capture `rescheduleCounts = insightRepository.observeRescheduleCountsByGoal(goalId).first()`
    inside `startGraphComputation()` and substitute it into `GoalGraphBuilder.build(..., rescheduleCounts = ...)`.
- Keep `graphSource` defined in terms of a `rescheduleCounts` parameter/flow that is **only subscribed
  when the Graph sheet opens**.

**Result:** Goal Detail open fires **6** DB queries instead of 7; the `task_events` join is deferred to
Graph open (where it is actually needed). `GoalGraphBuilder` and `goalGraphState` behavior unchanged.

**Files:** `GoalDetailViewModel.kt` only.

**Verification:**
- Unit test: `goalGraphState == NotRequested` on open; assert `observeRescheduleCountsByGoal` was
  **not** queried until `setGraphSheetVisible(true)`.
- Manual: open Goal Detail (confirm no graph reschedule join in DB logs); open Graph (graph renders
  correctly, Boulder flags present).

## Phase 3.2 — Introduce `GoalDetailUiState` incrementally (always-on only)

**Objective:** collapse the 6 always-on open-path flows into one `StateFlow<GoalDetailUiState>` to cut
recomposition count. Keep `GraphState`/`MirrorState` separate.

**New state holder (`GoalDetailViewModel.kt`):**
```kotlin
data class GoalDetailUiState(
    val goal: GoalEntity?,
    val tasks: List<TaskEntity>,
    val progress: GoalProgress?,
    val completionRate: Float?,
    val activeDays: Int,
    val lastActivity: Long?
)

private val _uiState = MutableStateFlow(GoalDetailUiState())
val uiState: StateFlow<GoalDetailUiState> = _uiState

init {
    viewModelScope.launch {
        combine(goal, tasks, goalProgress, goalRate, activeDays, lastActivity) {
            g, ts, prog, rate, days, last ->
            GoalDetailUiState(
                goal = g, tasks = ts, progress = prog,
                completionRate = rate?.completionRate,
                activeDays = days, lastActivity = last
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalDetailUiState())
         // feed _uiState
    }
}
```

**Screen wiring (`GoalDetailScreen.kt`):**
- Replace the 6 individual `collectAsState()` calls (`goal, tasks, goalRate, goalProgress,
  activeDays, lastActivity`) with a single `val ui by viewModel.uiState.collectAsState()`.
- Pass `ui.goal`, `ui.tasks`, `ui.goalProgress`, `ui.goalRate?.completionRate`, `ui.activeDays`,
  `ui.lastActivity` into existing `GoalProgressCard` / `GoalDetailTaskRow` / `StatusChip` etc.
- **Do NOT** move `mirrorState`, `goalGraphState`, `showGraphSheet`, `showMirrorSheet`,
  `showGraphEducation` into `uiState` — they remain as dedicated lazy/event states.

**Preserved invariants:** `WhileSubscribed(5000)` caching retained; Graph/Mirror lazy gating
untouched; `GoalGraphBuilder` and animation system untouched; tests for those unchanged.

**Result:** Goal Detail recomposes **once per change** (single `collectAsState`) instead of up to 7
times; the `rescheduleCounts`/`activeDaysInWindow` joins stay out of the open path (see 3.1).

**Files:** `GoalDetailViewModel.kt`, `GoalDetailScreen.kt`.

**Verification:**
- Unit test: single emission updates all 6 fields; toggling one task emits exactly one `uiState`
  change (not 6).
- Compose: open Goal Detail, toggle a task → only the affected list row + progress card update;
  no full-screen recomposition storm (verify via Layout Inspector / recomposition counts).

## Phase 3.3 — Localize Compose recomposition boundaries

**Objective:** shrink recomposition scope further so a task toggle does not re-run the top bar,
progress card, or metadata.

**Changes (`GoalDetailScreen.kt`):**
- Hoist `GoalProgressCard`, `MetadataBlock` (the collapsible "جزئیات بیشتر" section), and the
  top-bar `Row` into **stateful sub-composables** (`@Composable fun GoalProgressCard(state: GoalDetailUiState)`)
  that take the `uiState` slice as a stable parameter, so they only recompose when their slice changes
  (Compose structural equality on the data class).
- Wrap per-row handlers (`onToggle/onEdit/onDelete`) in `remember(task.id)` lambdas so they are not
  recreated per `LazyColumn` recomposition.
- Optionally wrap `goalProgress`/`activeDays` reads in `remember(uiState) { … }` / `derivedStateOf`
  where a sub-composable needs a derived value.

**Result:** a task toggle recomposes only the changed `GoalDetailTaskRow` (keyed) + the progress card
(if completion changed); top bar and metadata do not re-run.

**Files:** `GoalDetailScreen.kt` only.

**Verification:**
- Compose: toggle a task; confirm (via recomposition counter / Layout Inspector) that only the
  toggled row + progress card recompose, not the top bar or metadata.

## Sequencing & Risk

1. **3.1 first** (lowest risk, isolated to ViewModel, removes a query) — lands the biggest open-path win.
2. **3.2 next** (medium risk, touches screen wiring) — folds flows; keep lazy states separate.
3. **3.3 last** (low risk, Compose-only refactor) — localizes boundaries; safe, reversible.

**Out of scope (deferred):** `SELECT *` projection (P4), all-time `activeDays` windowing (P5),
merging `GraphState`/`MirrorState` into `uiState` (rejected — would re-couple lazy states to open path).

**Stop condition:** after 3.1–3.3 implemented and verified, Sprint 3 is complete. No DB schema change,
no navigation change, no Graph visual/animation change.

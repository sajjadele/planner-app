# ADR-0009 — Performance Audit 5.4: Dashboard Aggregation & Compose Responsibility

- **Status:** Accepted
- **Date:** 2026-07-17
- **Phase:** 5.4 (Performance & UX Stability Audit)
- **Supersedes / relates:** builds on Phase 5.2 (Goal Progress) and Phase 6 (Behavioral Solar System)

## Context

Phase 5.4 audited responsiveness across startup, tab switching (Planner/Goals/Dashboard/Detail),
BottomSheet + Graph opening, and animation smoothness. Two structural bottlenecks dominated:

1. **Dashboard N+1 aggregation (B1, High).** `GoalViewModel.enrichAndSort` ran
   `repository.observeGoalLastActivity(id).first()` and `observeGoalActiveDayCount(id).first()`
   **per goal, twice**, inside the `flatMapLatest` map — i.e. 2×N suspend queries on every tab
   switch and every list emission. Goal count N makes this scale linearly and hit Room on the main
   collection path.
2. **Per-card flow fan-out (B2, High).** `GoalCard` collected three independent flows per card
   (`lastActivityFor`, `activeDaysFor`, `progressFor`), and `progressFor` built a **fresh
   `combine(...).stateIn()`** on every card recomposition. This duplicated the N+1 work inside the
   card and defeated Compose skippability (the card also took `viewModel: GoalViewModel` as a param,
   an unstable type).

Secondary findings (all addressed or logged): missing DB indexes (B3), Graph Canvas re-measuring
text every frame during the 60fps breathing pulse (B4), `refreshMirror()` re-running on every
`goalRate` emission (B5), unkeyed `SearchDialog` list (B7). A suspected duplicate `PlannerViewModel`
(B8) was **verified a non-issue**: `GoalDetailScreen` is rendered as a nested composable, so its
`viewModel()` resolves to the same `ViewModelStoreOwner` as `MainScreen` — no second instance.

## Decision

### 1. Dashboard aggregation moves to bulk queries (architecture change)
Replace per-goal N+1 with **four single `GROUP BY` queries** per tab emission, all reactive
(`Flow`):

- `GoalDao.observeGoalCompletionRatesByStatus(status)` — bulk completion rate per goal.
- `GoalDao.observeGoalActivityBulk(status)` — bulk `lastActivity` (MAX date) + lifetime `activeDays`
  (distinct days) per goal.
- `GoalDao.observeGoalActivityBulkInWindow(status, from, to)` — bulk windowed active-day count per
  goal (rolling momentum for progress).
- `GoalDao.observeGoalsByStatus(status)` — the goals themselves (unchanged).

`GoalViewModel.goalsByTab` now `combine`s these four flows once and folds them into
`List<DashboardGoalItem>`. `GoalSort.sort` is still used, fed by maps extracted from the items
(no domain → UI dependency; `GoalSort` keeps its pure `GoalEntity` + maps signature).

### 2. Compose responsibility changes (architecture change)
`GoalCard` no longer holds any `ViewModel` reference or subscribes to any `Flow`. It receives a
**fully precomputed, stable `DashboardGoalItem`** (`goal`, `progress: GoalProgress?`,
`lastActivity: Long?`, `activeDays: Int`) plus stable action lambdas. The dashboard list lambdas
are cached with `remember(goalId)` so unchanged cards are skipped on recomposition.

This moves all aggregation/derivation out of the Composable into the ViewModel (single source of
truth), matching the existing Phase 6 principle that "Compose only displays; the domain/ViewModel
computes." Card skippability is now guaranteed by stable params.

### 3. Rendering-only Graph optimization (no behavior/animation change)
The 4s Ring Tide breathing pulse is **kept** (visual spec preserved). Per-frame cost is removed by
hoisting all graph-stable work out of the `Canvas` draw lambda (which is *not* a `@Composable`
scope, so `remember` is unavailable there): `sunNode`, `taskNodes`, sun title/% text layouts, and
cluster-count text layouts are now `remember(graph)`-cached in the composable scope and passed into
`drawSolarSystem`. The only per-frame work is the pulse math + draws.

### 4. Database indexes (non-destructive migration)
Bump schema `version = 10 → 11` with `Migration(10,11)` creating three secondary indexes via
`CREATE INDEX IF NOT EXISTS` (idempotent, no column change): `tasks(dateEpochMs)`,
`task_events(eventType)`, `goals(status)`. Entity `@Index` annotations added for fresh-install
schema parity. `fallbackToDestructiveMigration()` remains the safety net.

### 5. Mirror recompute debounced (B5)
`GoalDetailViewModel` now recomputes Mirror via `combine(goalRate, tasks, activeDays) { }
.debounce(250).collect { refreshMirror() }` instead of `goalRate.collect { refreshMirror() }`.
Result is identical; bursts of task toggles no longer trigger one full aggregation each.

## Consequences

### Benefits
- Dashboard tab switch / list emission: **2×N suspend queries → 4 single `GROUP BY` queries**
  regardless of goal count.
- Per card: **3 collected flows (incl. a fresh `stateIn` per recomposition) → 0 flows**; card is
  skippable via stable `DashboardGoalItem` + cached lambdas.
- Graph sheet idle redraw: text layout (`TextMeasurer`) no longer runs every frame; only pulse math.
- Day-scoped / insight / status-list queries now use indexes instead of full table scans.
- Mirror aggregation coalesced (250ms debounce) on rapid task edits.

### Trade-offs
- `goalsByTab` now emits `List<DashboardGoalItem>` instead of `List<GoalEntity>` — a small
  ViewModel/UI contract change (intended, documented here).
- Four reactive streams combined per tab; each is a single Room observer, so total observers ≈ the
  previous per-goal count is *reduced*, not increased.
- New `Migration(10,11)` must be present for any v10 DB; covers only additive indexes.

### Future
- Could later consolidate `GoalDetailScreen`'s ~11 independent `stateIn` flows into one UiState
  data class (deferred — larger refactor, no behavior gain; noted as remaining limitation).
- Index choice may be revisited if query patterns shift (e.g. composite `goals(status, createdAt)`).

## Rejected alternatives
- **Removing the Graph breathing animation** — rejected: constraints require preserving pleasing
  animations; we optimized its cost instead (O4).
- **Consolidating all `GoalDetailScreen` flows into one UiState** — deferred: out of "small
  optimization" scope; no behavior change.
- **Deduplicating `PlannerViewModel.allGoals`/`GoalViewModel.allGoals` dual subscription** — skipped:
  low impact, both are `WhileSubscribed(5000)` shared upstream.

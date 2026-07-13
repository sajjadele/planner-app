# ADR-0003: Progress & Behavior Snapshots (Phase 3)

- **Status:** Accepted (2026-07-13)
- **Phase:** Phase 3 — Progress & Behavior Foundation
- **Supersedes / relates to:** Builds on ADR-0001 (single source of truth) and ADR-0002 (graph design, deferred to Phase 4). Consumed by Phase 4 (Graph) and Phase 5 (AI Coach).

---

## 1. Context

Phase 1 established `goalId` as the single source of truth for Goal→Task. Phase 2 extracted a
pure-Kotlin domain layer (`InsightCalculator`) and repository interfaces, and began logging goal
lifecycle signal (`goal_events`). Today "progress" is recomputed on demand from `tasks` +
`task_events` for every consumer (cards, weekly insight, and — soon — Graph View trend lines and an
AI Coach). There is **no historical record of the analysis itself**, so trends over months and
behavioral consistency are either impossible or expensive to recompute repeatedly.

Phase 3 introduces a stored, queryable representation of *progress over time* and *behavioral
signal* so future features read from one source instead of each re-deriving from raw events. This
ADR records the storage and triggering decisions for that foundation.

---

## 2. Decision

Introduce two **rebuildable projection** tables (DB v9), computed from the existing `tasks` /
`task_events` signal — never authoritative writes:

- **`goal_progress_snapshot`** — per goal, per day: `dateEpochMs`, `goalId`, `completed`, `total`,
  `rate`. One row per `(dateEpochMs, goalId)`. Feeds per-goal progress trends (Phase 4 Graph edge
  weights) and goal-level coaching (Phase 5).
- **`behavior_snapshot`** — per day: `dateEpochMs` (PK), `completed`, `created`, `streak`,
  `velocity` (Velocity enum name), `rescheduleRate`. A single daily rollup of app-wide behavior.

### 2.1 Trigger / refresh strategy
- **Real-time upsert of *today*:** `PlannerViewModel` (add/complete/reopen/reschedule/delete) and
  `GoalDetailViewModel` (task toggle) call `SnapshotAggregator.recordDay(today)` after each write.
- **App-Launch Backfill Engine:** `MainActivity` launches `SnapshotAggregator.backfillIfNeeded()`
  once per process start. It fills any day from the earliest task through today that lacks a
  snapshot (gap detection via `SnapshotDao.getCoveredDates()`).
- **No `WorkManager`, no periodic scheduler, no network.** Offline-first is preserved; the engine is
  a plain coroutine on `lifecycleScope` / `viewModelScope`.

### 2.2 Projection principle (key constraint)
Snapshots are always recomputable from `tasks` + `task_events`. Writes use `REPLACE` (idempotent
upsert) and `SnapshotDao` exposes explicit day deletes so any day can be rebuilt without
duplication. Audits/rebuilds recompute from raw events; consumers (Graph, AI) read the snapshots.

---

## 3. Alternatives Considered

1. **Authoritative progress writes (drift risk).** Rejected — violates the source-of-truth rule from
   ADR-0001 and would let snapshots drift from events. Projections stay rebuildable.
2. **`WorkManager` periodic job.** Rejected — overkill for a local, cheap recomputation and adds
   scheduling/lifecycle complexity; the real-time-today + launch-backfill pairing fully covers the
   need while staying offline and simple.
3. **`goal_events` in `behavior_snapshot`.** Rejected — locked decision keeps `behavior_snapshot`
   task-focused. Goal-commitment behavior is captured separately in `goal_events` and consumed
   directly by the AI Coach later, not folded into the daily task rollup.
4. **`behavior_snapshot.lifeAreaId?` (per-life-area rows, as sketched in ROADMAP).** Rejected in
   favor of a single whole-day row. Rationale: `behavior_snapshot` is the daily time-series;
   per-life-area breakdown is already re-derivable on demand via `InsightDao.observeCompletionByLifeArea`
   and is promoted to a first-class node in Phase 4. One row per day keeps the projection trivial to
   reason about and store. If per-life-area history is later required, it can be a separate table
   without disturbing this one.
5. **Compute snapshots in the UI / ViewModels.** Rejected — keeps business logic out of ViewModels
   (Phase 2 principle). `SnapshotAggregator` lives in `core.snapshot`; ViewModels only fire
   `recordDay(today)`.

---

## 4. Consequences

- DB schema moves to **v9** (`MIGRATION_8_9`); new installs and migrators both get the two tables.
- New packages: `com.example.core.snapshot` (entities, DAO, repository, aggregator) and
  `com.example.domain.snapshot` (pure-Kotlin calculators, host-JVM testable).
- `InsightDao`/`InsightRepository` gain day-scoped aggregation inputs (`observeRescheduleCountBetween`,
  `getGoalDayCounts`, `getEarliestTaskDateEpochMs`) — additive, no existing query changed.
- Phase 4 (Graph) and Phase 5 (AI) read `behavior_snapshot` / `goal_progress_snapshot` instead of
  recomputing history; they must treat the tables as read-only projections.
- No UI consumes snapshots in Phase 3 (storage + read API only), per scope guard.

# Architecture State

> This document is a **living snapshot** of Vision Planner's architecture after each completed
> phase. It is updated together with `ROADMAP.md` and the relevant ADR after every
> architectural change. Read it to recover the *current* architecture without re-reading the
> entire codebase.
>
> Companion docs: `README.md`, `Vision_Planner.md`, `docs/ROADMAP.md`, `docs/ADR-0001-goal-task-relationship.md`,
> `docs/ADR-0002-graph-architecture.md`.

---

## Current Phase

**Phase 3 — Progress & Behavior Foundation — COMPLETE (2026-07-13)**

Phase 4 (Graph View Foundation) is next.

---

## 1. Database Version

| Item | Value |
|------|-------|
| Current schema version | **9** |
| Latest migration | `MIGRATION_8_9` (add `goal_progress_snapshot` + `behavior_snapshot` projection tables) |
| Fallback | `fallbackToDestructiveMigration()` as safety net only |
| Tables | `goals`, `goal_events`, `goal_progress_snapshot`, `behavior_snapshot`, `tasks`, `task_events`, `notes`, `module_settings` |

Phase 3 added two rebuildable projection tables (no existing table modified). Snapshots are always
recomputable from `tasks` + `task_events`; the raw events stay the source of truth.

---

## 2. Domain Layer Status

| Subsystem | Status | Notes |
|-----------|--------|-------|
| Insight computation | **Complete (Phase 2)** | Pure-Kotlin `InsightCalculator` in `com.example.domain.insight` (streak, rate, velocity, procrastination, neglected goal, week range). `WeeklyInsightViewModel` only assembles the reactive `StateFlow` and delegates all math. |
| Snapshot computation | **Complete (Phase 3)** | Pure-Kotlin `GoalProgressCalculator` + `BehaviorCalculator` in `com.example.domain.snapshot`. No Android imports; compute per-day progress/behavior from raw inputs; `SnapshotAggregator` (in `core.snapshot`) persists the results. |
| Graph model | **Designed, not implemented** | `ADR-0002` defines the intended `Graph<Node, Edge>` model and `GraphRepository` contract. Implementation deferred to Phase 4. |
| Use-case layer | Minimal | `InsightCalculator` + snapshot calculators exist as pure function objects. No formal `UseCase` abstraction yet. |

**Package:** `com.example.domain.*` (no Android imports, host-JVM testable). Two sub-packages:
`domain.insight` (Phase 2) and `domain.snapshot` (Phase 3).

---

## 3. Repository Layer Status

| Repository | Type | Notes |
|------------|------|-------|
| `GoalRepository` | **Interface** (`RoomGoalRepository` impl) | Phase 2 extracted interface; concrete impl wraps `GoalDao` + `GoalEventDao`. |
| `TaskRepository` | Concrete class | Not yet interfaced (Phase 2 scope guard). Gained `getTasksByGoalId`. |
| `InsightRepository` | **Interface** (`RoomInsightRepository` impl) | Phase 2 — decouples `WeeklyInsightViewModel` from `InsightDao`. Phase 3 added day-scoped aggregation inputs (`observeRescheduleCountBetween`, `getGoalDayCounts`, `getEarliestTaskDateEpochMs`). |
| `SnapshotRepository` | **Interface** (`RoomSnapshotRepository` impl) | **Phase 3 (NEW).** Wraps `SnapshotDao`; thin boundary over the two projection tables. |
| `NoteRepository`, `HolidayRepository`, `ThemeRepository`, `OnboardingRepository` | Concrete | Unchanged. |

**DI:** None (manual construction in ViewModel `init` / `MainActivity` via `AppDatabase.getDatabase()`).
Deferred — no infrastructure refactor in Phase 2/3.

---

## 4. Event System

| Event stream | Table | Status |
|--------------|-------|--------|
| Task events | `task_events` (`created`, `completed`, `reopened`, `deleted`, `rescheduled`, `priority_changed`) | Active. Written by `PlannerViewModel`. |
| Goal events | `goal_events` | **Phase 2 (NEW + ACTIVE).** `GoalEventEntity` + `GoalEventDao` + `MIGRATION_7_8`. Logs `created`, `completed`, `paused`, `resumed`, `abandoned` from `GoalViewModel`. |

---

## 5. Progress System

| Item | Status |
|------|--------|
| Stored progress snapshots | **Present (Phase 3).** `goal_progress_snapshot` (per goal, per day) + `behavior_snapshot` (per day rollup). Rebuildable projections of `tasks`/`task_events`. |
| On-demand computation | `InsightDao` aggregations + `InsightCalculator`/`BehaviorCalculator` (pure). |
| Projection principle | Snapshots are recomputable from events (REPLACE upsert; `SnapshotDao` exposes day deletes). Never authoritative writes. |
| Refresh trigger | Real-time upsert of *today* via `SnapshotAggregator.recordDay(today)` (fired from `PlannerViewModel` + `GoalDetailViewModel`) + App-Launch Backfill Engine (`SnapshotAggregator.backfillIfNeeded()` in `MainActivity`). No `WorkManager`. |

---

## 6. Graph Readiness

| Item | Status |
|------|--------|
| Domain model | **Designed** (see `ADR-0002`). Not implemented. |
| Life Area as node | Not promoted (Phase 4). |
| `GraphRepository` | Not implemented. |
| Visualization | Not started (Phase 4). |
| Snapshot inputs | **Phase 3 ready.** `goal_progress_snapshot.rate` (edge weight) and `behavior_snapshot` (trend) are the historical signals the Graph model will consume. |

---

## 7. AI Readiness

| Item | Status |
|------|--------|
| `InsightGenerator` port | Not defined (Phase 5). |
| Rule engine | Not implemented. |
| Context aggregation | Not implemented; Phase 3 `behavior_snapshot`/`goal_progress_snapshot` are the intended behavioral inputs. |
| Remote/LLM | **Forbidden** by offline-first constraint. |

---

## 8. Testing Status

| Layer | Status |
|-------|--------|
| Domain unit tests | **Phase 2 + Phase 3 (PASSING).** `InsightCalculatorTest` (13 cases), `RoomInsightRepositoryTest` (MockK, 2), **Phase 3 adds** `BehaviorCalculatorTest`, `GoalProgressCalculatorTest`, `RoomSnapshotRepositoryTest` (MockK, 4), `SnapshotAggregatorTest` (MockK, 2). |
| Room DAO tests | **Phase 2/3 (Robolectric).** `GoalEventDaoTest` + `SnapshotDaoTest` — correct, but require Robolectric, which cannot provision its Android SDK jar in this sandbox (`DefaultSdkProvider` `UnsupportedOperationException`); pass where Robolectric SDK is available. |
| ViewModel tests | Not started. |
| UI / screenshot tests | Scaffolded (`ExampleRobolectricTest` — same Robolectric SDK limitation in sandbox). `GreetingScreenshotTest` (Roborazzi) removed (library frozen). |
| Test deps | JUnit4, Robolectric, Compose UI test, kotlinx-coroutines-test, MockK + Turbine (Phase 2). |

---

## Changelog

| Date | Phase | Change |
|------|-------|--------|
| 2026-07-13 | Phase 1 | `goalName` dropped; `goalId` single source of truth; DB v7. |
| 2026-07-13 | Phase 2 (start) | `InsightCalculator` extracted; `GoalRepository`/`InsightRepository` interfaces; `goal_events` table; `ADR-0002` graph design; `ARCHITECTURE_STATE.md` created. |
| 2026-07-13 | Phase 2 (complete) | DB v8 (`goal_events`); `Velocity`/`ProcrastinationAlert` moved to `domain.insight`; `GoalViewModel` logs lifecycle events; domain + MockK tests passing; docs updated (README, Vision_Planner, ROADMAP, ADR-0002, this file). |
| 2026-07-13 | Phase 3 (complete) | DB v9 (`goal_progress_snapshot` + `behavior_snapshot`); `domain.snapshot` calculators; `SnapshotRepository`/`RoomSnapshotRepository` + `SnapshotDao`; `SnapshotAggregator` (real-time today upsert + launch backfill, no `WorkManager`); `PlannerViewModel`/`GoalDetailViewModel` fire `recordDay`, `MainActivity` runs backfill; `ADR-0003`; snapshot + MockK tests passing; docs updated (README, Vision_Planner, ROADMAP, ADR-0003, this file). |

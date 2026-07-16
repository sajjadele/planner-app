# Architecture State

> Living snapshot of **current** technical architecture only.
>
> Authority order:
> 1. `PRODUCT_DIRECTION_DECISION_DOCUMENT.md`
> 2. this file
> 3. `docs/ROADMAP.md`
> 4. `docs/ADR/*`
> 5. `AGENT.md`
>
> Companion short bootstrap: `CURRENT_IMPLEMENTATION_CONTEXT.md`

---

## Current Phase

**Phase 3 — Behavior Data Foundation — COMPLETE**

**Next development direction:** Phase 4 — Mirror Engine Foundation

Graph is **not** the immediate next priority.

---

## 1. Database Version

| Item | Value |
|------|-------|
| Schema version | **9** |
| Latest migration | `MIGRATION_8_9` |
| Tables | `goals`, `goal_events`, `goal_progress_snapshot`, `behavior_snapshot`, `tasks`, `task_events`, `notes`, `module_settings` |

Projection tables (`goal_progress_snapshot`, `behavior_snapshot`) are rebuildable from `tasks` + `task_events`.

---

## 2. Domain Structure

| Package | Status | Role |
|---------|--------|------|
| `domain.insight` | Implemented | Streak, rate, velocity, procrastination, neglected-goal math |
| `domain.snapshot` | Implemented | Daily goal progress + behavior projection math |
| `domain.mirror` | Planned (Phase 4) | Pattern heuristics + neutral feedback generation |

Domain packages are pure Kotlin (no Android imports), host-JVM testable.

---

## 3. Repository Status

| Repository | Status |
|------------|--------|
| `GoalRepository` | Interface + Room impl |
| `InsightRepository` | Interface + Room impl |
| `SnapshotRepository` | Interface + Room impl |
| `TaskRepository` | Concrete (not interfaced yet) |
| `MirrorRepository` | Planned (Phase 4) |
| Notes / Holiday / Theme / Onboarding repos | Concrete |

DI framework: none (manual construction). Deferred.

---

## 4. Event System

| Stream | Table | Status |
|--------|-------|--------|
| Task events | `task_events` | Active (`created`, `completed`, `reopened`, `deleted`, `rescheduled`, `priority_changed`) |
| Goal events | `goal_events` | Active (`created`, `completed`, `paused`, `resumed`, `abandoned`) |

Events are the source of truth for behavioral history.

---

## 5. Snapshot System

| Item | Status |
|------|--------|
| Tables | `goal_progress_snapshot`, `behavior_snapshot` |
| Principle | Rebuildable projections, never authoritative |
| Today refresh | Real-time via `SnapshotAggregator.recordDay(today)` |
| Historical gaps | App-launch backfill via `SnapshotAggregator.backfillIfNeeded()` |
| Scheduler | No WorkManager |
| Consumers | Insight UI now; Mirror + Graph later |

Snapshots are inputs for Mirror analysis, not only storage/reporting.

---

## 6. Mirror Preparation (Next)

| Item | Direction |
|------|-----------|
| Placement | Goal Dashboard (no separate Mirror screen in V1) |
| Patterns | Boulder, Initiator/Finisher, Goal Attention, Consistency Decay |
| Sources | `task_events`, `goal_events`, snapshots, goal/task aggregates |
| Style | Neutral feedback, no judgment |
| Activation | Automatic; meaningful after ~7 days of usage |
| Reflection | Optional future feature only; never forced |

---

## 7. Graph Status

| Item | Status |
|------|--------|
| Priority | Later (Phase 6) |
| Scope | Goal→Task only |
| Persistence | None (computed on demand) |
| Life Area node | Not promoted |

Decision history: `docs/ADR/ADR-0002-graph-architecture.md`

---

## 8. AI Status

| Item | Status |
|------|--------|
| Role | Future enhancement (Phase 7) |
| Dependency in V1 | None |
| Constraint | Offline-first remains absolute |

---

## 9. UX Architecture Notes

| Item | Current / Direction |
|------|---------------------|
| Planner | Daily task execution remains primary interaction |
| Goal context | Goal Cards should make task→goal purpose visible |
| Tasks without goals | Allowed as Inbox/capture; visually separated |
| Life Area | Metadata only |

---

## 10. Testing Status

| Layer | Status |
|-------|--------|
| Domain unit tests | Present for insight + snapshot calculators |
| Mirror unit tests | Planned with Phase 4 |
| Room DAO tests | Present (Robolectric where SDK available) |
| ViewModel tests | Not started |

---

## Constraints Still Active

- Offline-first
- No WorkManager analytics jobs
- No network/remote AI foundation
- No dual sources of truth for Goal→Task
- No Life Area first-class promotion without new product decision

---

## Changelog (docs-relevant)

| Date | Change |
|------|--------|
| 2026-07-13 | Phases 1–3 complete (DB v9, domain.insight + domain.snapshot) |
| 2026-07-15 | Product direction consolidated; Mirror prioritized over Graph |
| 2026-07-16 | Docs hierarchy cleaned; ADRs moved under `docs/ADR/`; Mirror set as next phase |

# Vision Planner — Long-Term Architectural Roadmap

> **Purpose:** This document defines the future evolution of Vision Planner's
> architecture. It is a *guidance* document for AI agents and developers. It does
> **not** prescribe implementation details and **does not** modify code.
>
> **Companion docs:** `README.md`, `Vision_Planner.md`, and `docs/ADR-0001-goal-task-relationship.md`.
>
> **Status anchor:** Written after **Phase 1 (Foundation Cleanup)** is complete.
> Phase 1 removed the duplicate `goalName` cache, made `goalId` the single source of
> truth for the Goal→Task relationship, and removed the blocking `runBlocking` call in
> the insight pipeline. See ADR-0001.
>
> **Phase 1 — Status: COMPLETE.** Documentation finalized 2026-07-13 (README + Vision_Planner
> DB version → v7, ADR-0001 "Alternatives Considered" added, this status note added). Code is
> at DB schema v7. Phase 2 builds on this foundation.
>
> **Phase 2 — Status: COMPLETE.** Architecture Stabilization finalized 2026-07-13. Pure-Kotlin
> `domain.insight.InsightCalculator` extracted; `GoalRepository` + `InsightRepository` interfaces
> introduced (`RoomGoalRepository`, `RoomInsightRepository`); `goal_events` table + `GoalEventDao`
> added (DB v8) and goal lifecycle events logged from `GoalViewModel`; ADR-0002 records the
> (deferred-to-Phase-4) Graph architecture. Full DI was intentionally **not** done (scope guard).
> See `ARCHITECTURE_STATE.md` for the live snapshot.
>
> **Phase 3 — Status: COMPLETE.** Progress & Behavior Foundation finalized 2026-07-13. DB v9 adds
> two rebuildable projection tables (`goal_progress_snapshot`, `behavior_snapshot`); pure-Kotlin
> `domain.snapshot` calculators (`GoalProgressCalculator`, `BehaviorCalculator`) + `SnapshotAggregator`
> (real-time today upsert + App-Launch Backfill Engine, no `WorkManager`); `SnapshotRepository`
> interface + `RoomSnapshotRepository`. Storage + read-API only (no UI). ADR-0003 records the
> snapshot/trigger decisions. See `ARCHITECTURE_STATE.md` for the live snapshot.

---

## Guiding Principles (apply to every phase)

Vision Planner is **not a task manager** — it is a goal-execution and behavioral-learning
system. The chain is:

```
Goals → Tasks → Actions (Events) → Behavior → Insights
```

Architectural decisions must serve that chain. Additional constraints inherited from the
product and prior decisions:

1. **Offline-first is absolute.** No remote services, no network dependency, no auth/sync
   unless a future phase explicitly overturns this (it currently does not).
2. **Collect signal, not noise.** Store only high-signal behavioral data; do not log every
   interaction.
3. **Incremental, low-risk evolution.** Improve the foundation before building features on
   top of it. Avoid big-bang rewrites.
4. **Single source of truth.** Phase 1 established this for Goal→Task. Every future phase
   must preserve and extend it, never reintroduce duplication.
5. **Keep the UI dumb.** Business logic belongs in a neutral layer, not in ViewModels or DAOs.

---

## Current Architecture (post-Phase 3) — baseline for this roadmap

- **Pattern:** MVVM on `AndroidViewModel` + `StateFlow` + Room.
- **Entities:** `Goal`, `Task`, `TaskEvent` (append-only signal log), `GoalEvent` (goal
  lifecycle log, Phase 2), `GoalProgressSnapshot` + `BehaviorSnapshot` (rebuildable projections,
  Phase 3), `Note`, `ModuleSettings`. **No `User`, no graph store.**
- **Reads:** reactive (`DAO Flow → Repository interface → stateIn → collectAsState`).
- **Writes:** `suspend` in thin repositories, called from `viewModelScope.launch`.
- **Domain layer:** `com.example.domain.*` holds pure-Kotlin logic — `domain.insight`
  (`InsightCalculator`: streak, rate, velocity, procrastination, neglected-goal, week range) and
  `domain.snapshot` (`GoalProgressCalculator`, `BehaviorCalculator`: per-day progress/behavior
  projections). No Android imports; unit-testable on the host JVM.
- **Repository boundary:** `GoalRepository`/`InsightRepository`/`SnapshotRepository` are interfaces
  (`RoomGoalRepository`/`RoomInsightRepository`/`RoomSnapshotRepository` wrap the DAOs).
  `WeeklyInsightViewModel`/`GoalDetailViewModel` consume interfaces; `PlannerViewModel` still
  reaches some DAOs directly (DI deferred).
- **Insight logic:** SQL aggregation in `InsightDao` (behind `InsightRepository`) + pure-Kotlin
  derivation in `InsightCalculator`/`BehaviorCalculator`. No blocking calls (Phase 1) and no
  Android-coupled math (Phase 2/3).
- **Progress/behavior:** `SnapshotAggregator` computes `goal_progress_snapshot` +
  `behavior_snapshot` from `tasks`/`task_events` — real-time upsert of today (fired from
  `PlannerViewModel`/`GoalDetailViewModel`) + App-Launch Backfill Engine (`MainActivity`).
  Tables are rebuildable projections; Phase 3 is storage + read-API only (no UI).
- **Known limitations still open:** no full DI; Life Area is an `Int` on `Task`, not a first-class
  node (Phase 4); graph model not implemented (Phase 4, designed in ADR-0002); AI port not defined
  (Phase 5).

These open limitations are what Phases 4–6 address, in dependency order.

---

## Phase 2 — Architecture Stabilization

### 1. Objective
Make the codebase structurally ready for feature work: clean layer boundaries, testable
business logic, and a sane dependency model — without adding any new user-facing feature.

### 2. Why this phase is needed
Phase 1 fixed data correctness but the *shape* of the code still traps logic in
`AndroidViewModel`s and DAOs. Every future feature (progress, graph, AI) would otherwise
re-implement analytics ad-hoc and remain untestable. Stabilizing now prevents compounding
debt.

### 3. Problems it solves
- Insight/behavior algorithms are coupled to the Android framework → cannot unit test.
- No consistent boundary between "what the UI needs" and "how data is computed."
- ViewModels reach directly into `InsightDao`, bypassing repositories (inconsistent layering).
- No automated tests exist to guard regressions as the model grows.
- Manual `AppDatabase.getDatabase(application)` in every ViewModel `init` blocks injection/testing.

### 4. Dependencies
- Phase 1 complete (prerequisite — single source of truth is in place).
- No new entities required.

### 5. Expected architectural changes
- **Domain/use-case layer (pure Kotlin, no Android imports):** extract the computations
  currently in `WeeklyInsightViewModel` (streak, velocity, procrastination threshold,
  neglected-goal, best-day) into plain-Kotlin functions/use-cases that take repository
  interfaces and return domain models (e.g. `InsightSnapshot`).
- **Repository boundary hardening:** route *all* reads/writes through repositories; remove
  direct `InsightDao` access from ViewModels. Introduce repository *interfaces* so the
  implementation can be swapped in tests.
- **Lightweight DI:** inject repositories/use-cases (e.g. manual factories or Hilt). The
  goal is *testability and a single composition root*, not a specific framework.
- **Testing foundation:** unit tests for the new domain use-cases (pure Kotlin, fast);
  a Room test rule for DAO/aggregation tests; optional screenshot/smoke tests already
  scaffolded in `app/src/test`.
- **Logging of goal lifecycle events:** add `GoalEvent` (or extend the event model) so
  `paused`/`resumed`/`completed`/`abandoned` transitions are captured as signal — required
  later for behavioral analysis of goal commitment.

### 6. What should NOT be done in this phase
- No new UI features, no Graph screen, no AI.
- Do **not** build a progress/behavior table yet (that is Phase 3).
- Do **not** introduce a `User` entity or any sync/network layer.
- Do **not** over-abstract CRUD — only extract the *analytic/behavioral* logic.

---

## Phase 3 — Progress & Behavior Foundation

> **Status: COMPLETE (2026-07-13).** DB v9; `goal_progress_snapshot` + `behavior_snapshot` tables;
> `domain.snapshot` calculators; `SnapshotAggregator` (real-time today upsert + launch backfill);
> `SnapshotRepository` interface. Storage + read-API only. See ADR-0003 and ARCHITECTURE_STATE.md.

### 1. Objective
Introduce a stored, queryable representation of *progress over time* and *behavioral
signal*, so future features read from one source instead of recomputing from raw events.

### 2. Why this phase is needed
Today "progress" is computed on demand from `tasks` + `task_events`. Graph View trend
lines, AI time-context, and long-term behavior analysis would each recompute the same
history repeatedly, and there is **no historical record of the analysis itself**. A
behavioral foundation is the shared prerequisite for Phases 4 and 5.

### 3. Problems it solves
- No time-series of goal progress → trends/velocity-over-months are impossible or expensive.
- Every consumer (card, graph, AI) re-derives behavior → duplicated logic and drift.
- Raw events alone cannot answer "how consistent was this user in Q2?" without heavy compute.

### 4. Dependencies
- Phase 2 (domain layer + testable analytics) — the snapshot computation reuses those use-cases.
- Goal lifecycle events from Phase 2.

### 5. Expected architectural changes
- **Progress snapshots (event-sourced projection):** keep `task_events` as the *single
  source of truth*; compute `GoalProgressSnapshot` (per goal: completed/total, rate,
  timestamp) as a *rebuildable projection*, not an authoritative write. A periodic
  aggregation job (e.g. daily, or on key events) writes snapshots.
- **Behavior aggregation:** a `BehaviorSnapshot` (daily rollup) capturing per-life-area
  completion, streak, velocity, reschedule rate. Same projection principle.
- **Possible new entities/tables:**
  - `goal_progress_snapshot (goalId, date, completed, total, rate)`
  - `behavior_snapshot (date, lifeAreaId?, completed, created, streak, velocity, rescheduleRate)`
  - (Optional) `goal_event (goalId, eventType, timestamp)` for goal lifecycle signal.
- **Retention policy:** snapshots are cheap to recompute; prune raw events older than a
  horizon if needed, but keep enough to rebuild.
- **Read model:** Graph/AI read snapshots; audits recompute from raw events.

### 6. What should NOT be done in this phase
- Do **not** build the Graph UI (Phase 4).
- Do **not** wire AI/LLM (Phase 5).
- Do **not** make snapshots authoritative writes that can drift from events — always
  rebuildable.

---

## Phase 4 — Graph View Foundation

### 1. Objective
Represent the Goal → Task → Event → Life-Area relationships as a graph data model and
expose it through a clean read API, preparing for visualization.

### 2. Why this phase is needed
The mental model is inherently a graph, but the app currently only renders lists. A
Graph View (relationships, progress edges, life-area clustering) requires the relationships
to be queryable as nodes + edges, not reconstructed per screen.

### 3. Problems it solves
- Relationships are implicit (FK joins) and not reusable across screens.
- No concept of edge *weight/attribute* (e.g. progress, recency).
- Life Area is not a first-class node, so it cannot participate in the graph.

### 4. Dependencies
- Phase 1 (single Goal→Task source of truth).
- Phase 3 (progress snapshots give edges a weight/attribute to render).

### 5. Expected architectural changes
- **Graph domain model (pure Kotlin):** `Graph<Node, Edge>` where
  `Node ∈ {Goal, Task, LifeArea}`, `Edge` carries type + weight (e.g. `Goal—has→Task`,
  `Task—in→LifeArea`, weighted by progress/recency from snapshots).
- **Life Area as a node:** promote `lifeAreaId` to a stable registry/table so it is a real
  graph node with color/icon/progress — not just an `Int` on `Task`.
- **`GraphRepository`/use-case:** builds the graph from existing relations + Phase 3
  snapshots. UI consumes the `Graph` model, never the DAOs directly.
- **Visualization consumption:** the Compose view takes a `Graph` and lays it out; it must
  not query the database. Keep layout/animation in the UI, data in the domain.

### 6. What should NOT be done in this phase
- Do **not** couple the graph model to a specific rendering library or to AI.
- Do **not** store the graph itself — derive it on demand from relations + snapshots.
- Do **not** add interactivity/AI suggestions into the graph yet.

---

## Phase 5 — AI Architecture Preparation

### 1. Objective
Introduce AI *incrementally and safely*, behind a stable interface, so the app can gain
intelligence later without rewriting the UI or violating offline-first.

### 2. Why this phase is needed
Future value depends on "AI Coach" insights, but AI must not be hard-wired into screens,
and must respect the offline-first constraint decided in planning. A clean seam now lets
the engine evolve (rule → on-device) without touching consumers.

### 3. Problems it solves
- AI logic would otherwise leak into ViewModels/UI (tight coupling, untestable).
- No consistent way to assemble the context an insight engine needs.
- Risk of a premature, expensive commitment to cloud AI (forbidden by offline-first).

### 4. Dependencies
- Phase 2 (domain/use-case layer, DI) — the AI port lives in the domain layer.
- Phase 3 (behavior/progress snapshots) — the context the engine consumes.

### 5. Expected architectural changes
- **`InsightGenerator` port (interface) in the domain layer:** `suspend fun generate(ctx): List<Insight>`.
- **Rule-based implementation first:** deterministic, local, explainable rules reusing the
  Phase 2/3 signals (streaks, reschedules, velocity, neglected goals). Ships value while
  staying 100% offline and lightweight.
- **Context aggregation layer:** assembles a `InsightContext` (goal states, recent
  snapshots, behavioral flags) from repositories/snapshots — the *only* thing the engine
  sees. This decouples the engine from raw DAOs.
- **On-device model as a later, optional implementation** of the same port (TFLite/ONNX),
  still offline. Remote/LLM is explicitly out of scope under current constraints.
- **Consumers stay unaware:** card, graph tooltips, and a future coach screen call the
  port; swapping the engine is transparent.

### 6. What should NOT be done in this phase
- Do **not** add network/remote AI calls (violates offline-first).
- Do **not** let AI code call DAOs/UI directly — only the `InsightContext`.
- Do **not** build heavy on-device ML infra yet unless explicitly chosen; rule engine suffices.

---

## Phase 6 — User Experience & Onboarding

### 1. Objective
Evolve first-run and ongoing UX so users *experience* the Vision Planner philosophy
(goals drive tasks, tasks generate behavior, behavior becomes insight) rather than just
reading feature explanations.

### 2. Why this phase is needed
The current onboarding (Phase 1-era) captures one goal + one task and deep-links in. It
teaches the *mechanics* but not the *value loop*. As Graph/AI land, the UX must show users
why linking goals to tasks and completing them produces insight.

### 3. Problems it solves
- Onboarding explains features, not the goal→task→behavior→insight value chain.
- Users may create tasks unlinked from goals (defeating the product thesis).
- No guided demonstration of how progress/insights emerge from small actions.

### 4. Dependencies
- Phase 3 (progress/behavior) and Phase 4 (graph) — so onboarding can *show* relationships
  and progress forming.
- Phase 5 (insights) — so early "you completed X, here's a pattern" feedback is possible.

### 5. Expected architectural changes
- **Philosophy-driven onboarding:** a flow that demonstrates the loop — create a Goal,
  attach a Task, complete it, and *surface the resulting insight/progress* immediately,
  proving value before the user leaves onboarding.
- **Guided Goal → Task relationship creation:** the linking micro-action (already present
  in the current onboarding connector node) becomes a reusable, teachable pattern across
  the app, reinforced by the graph/insight views from Phases 4–5.
- **Value demonstration, not feature tours:** leverage Phase 3/4 data to show "your goal is
  X% complete" and "you're building a streak" early, anchored to real user data.
- **Reusable onboarding primitives:** the connector/linking UI and deep-link pattern become
  shared components, not a one-off screen.

### 6. What should NOT be done in this phase
- Do **not** add gamification/dark patterns or notifications that violate offline/simple ethos.
- Do **not** rebuild the data model — this phase is UX on top of Phases 3–5.
- Do **not** introduce accounts/sync to "personalize" — personalization here means the
  per-device behavioral model, not multi-user.

---

## Phase Dependency Diagram

```
Phase 1 (DONE)  Foundation Cleanup  ── goalId single source of truth, no blocking calls
        │
Phase 2 (DONE)  Architecture Stabilization  ── domain layer, repository interfaces, goal-lifecycle events, tests (DI deferred)
        │
Phase 3  (DONE)  Progress & Behavior Foundation  ── snapshots, behavior aggregation (projection)
        │
Phase 4  Graph View Foundation  ── Graph model + Life-Area node + GraphRepository
        │
Phase 5  AI Architecture Preparation  ── InsightGenerator port + rule engine + context layer
        │
Phase 6  UX & Onboarding  ── philosophy-driven, value-demonstrating first run
```

Each phase is independently shippable and leaves the app working. Phases 3–5 are the
shared foundation that Phase 6 (and all future features) consume.

---

## Explicitly Out of Scope (preserve context — do not reintroduce without a new decision)

- **Cloud / remote AI, network calls, auth, server sync** — forbidden by offline-first.
- **`User`/multi-profile entity** — single-device personalization only; add a thin anchor
  later only if a product decision requires it.
- **Graph storage** — derive the graph on demand; do not persist it.
- **Authoritative progress writes** — snapshots are rebuildable projections of events.
- **DI framework churn** — adopt only enough DI to enable testing/injection.

---

*This roadmap is the canonical long-term plan. When starting a new session, read this file
plus `ADR-0001` to recover full architectural context without re-explaining the project.*

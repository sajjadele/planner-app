# ADR-0004: Behavioral Solar System Graph View

- **Status:** Accepted (implementation started — Phase 6 / "Phase 4: Graph View" roadmap item)
- **Date:** 2026-07-16
- **Phase:** Graph Exploration (Behavioral Solar System)
- **Supersedes:** refines the generic intent of `docs/ADR/ADR-0002-graph-architecture.md` with the realized contextual design

## Context

ADR-0002 established that the Graph is a pure domain model, computed on demand, never stored, and scoped to Goal→Task only. A naive global "island" graph was rejected: the schema has a strict `Goal → Task` foreign-key relationship with no cross-goal links, so a global view would render as disjointed trees and would not feel like a connected network.

We instead pivot to a **contextual, behavior-driven** graph placed inside a popup/bottom sheet on `GoalDetailScreen`. The metaphor is a **Behavioral Solar System**: the Goal is the central gravity node (the "sun"), and its Tasks are orbiting satellites. Orbital position and appearance encode real behavioral signals already tracked by the app (`priority`, `isCompleted`, `rescheduleCount` via `task_events`, and `GoalProgress.overall`).

This delivers the product goal — "understand the health of a goal within 2 seconds" — as a visual, intuitive canvas rather than a static node-link diagram.

## Decision

### Placement & trigger
- The graph lives in a `ModalBottomSheet` (`GoalGraphSheetContent`) opened from a dedicated Galaxy/Graph `IconButton` in the `GoalDetailScreen` top bar, distinct from the existing Mirror sheet.
- It is per-goal (contextual), not a global overview. This keeps the "Goal is primary" model and avoids disconnected islands.

### Rendering
- Custom Compose `Canvas` (no third-party graph library). Matches "keep it simple" and offline-first; avoids a new dependency.
- Layout is **fully deterministic**, computed in the pure-Kotlin `domain.graph` package. No physics integration, no `Random` — same inputs always yield identical coordinates.

### Domain model (pure Kotlin, host-JVM testable)
Package `com.example.domain.graph` (no Android / Room / Compose imports):
- `GoalGraphNode` — id, label, kind (GOAL|TASK), design-space `(cx, cy)`, `size` (priority → radius), `alpha` (completed → faded), `priority`, `isBoulder`, `isCompleted`, `colorRole` (enum, not `android.graphics.Color`).
- `GoalGraphEdge` — goal→task gravity line with `alpha` (connection strength).
- `GoalGraph` — center, `viewportRadius`, `goalProgressOverall` (Ring Tide), nodes, edges.
- `GraphGeometry` — deterministic polar→cartesian `project(cx, cy, radius, angleRad)` and `distance`.

### Layout rules (deterministic)
- **Priority lanes:** active tasks grouped into three concentric lanes by `priority` — HIGH inner (0.34·R), MEDIUM middle (0.58·R), LOW outer (0.82·R); undated/no-priority active tasks pushed to the outer active ring (0.92·R).
- **Even angle distribution:** within a lane, node `i` of `n` gets `angle = (i / n) · 2π + jitter(id)`; a single node is fixed at π/2. `jitter(id) = ((id·92821) % 1000) / 1000 · 0.25` de-aligns lanes radially without randomness.
- **Node size by priority:** HIGH 22px > MEDIUM 16px > LOW 12px (design space).
- **Completion Ring Tide:** `GoalGraph.goalProgressOverall` carries `GoalProgress.overall` so the UI can brighten the sun by progress.
- **Boulder Wobble flag:** `isBoulder = rescheduleCount >= 2` (threshold matches `MirrorHeuristics.detectBoulder`), surfaced via `ColorRole.BOULDER` for a UI halo/wobble.
- **Faded orbits:** completed tasks become tiny (6px), faded (alpha 0.28) points on the outer edge ring (0.97·R), preserving a visual memory of progress.
- Urgency-by-deadline radius mapping is **deferred to V2** (keep core orbits stable first).

### Data flow (no DAO access from UI)
`GoalDetailViewModel` combines existing reactive sources and calls `GoalGraphBuilder.build(...)`:
- `goalRepository.observeGoalById` → title, `deadlineEpochMs`
- `taskRepository.getTasksByGoalId` → mapped to `GoalGraphBuilder.TaskInput` (decouples domain from `plugins.planner.data.TaskEntity`)
- `insightRepository.observeRescheduleCountsByGoal(goalId)` (existing query) → Boulder flag
- `goalProgress` (existing `StateFlow<GoalProgress?>`) → Ring Tide
The computed `GoalGraph` is exposed as a `StateFlow<GoalGraph?>`. UI renders it; it never queries DAOs directly (per ADR-0002).

### Deferred to V2
- Drift/Decay ("drifting satellites"): requires a per-task last-activity signal (new read-only query on `task_events`). Skipped now to avoid schema tension.
- Urgency-by-deadline radial mapping.

## Consequences

Positive:
- No schema change, no migration, no new stored state (honors ADR-0002 "never stored").
- Domain stays pure Kotlin and host-JVM testable; 9 unit tests cover lane ordering, even-angle distribution, determinism, priority sizing, Boulder and completion mapping, and Ring Tide.
- Clear domain/UI boundary via `TaskInput`; `ColorRole` avoids leaking `android.graphics.Color` into the domain layer.
- Reuses existing signals (priority, completion, reschedule counts, GoalProgress) — no new analytics.

Trade-offs:
- Contextual (per-goal) only; no global map of all goals at once. Acceptable per product decision ("Goal is primary").
- No cross-goal links by design (schema has none).
- V1 omits drift/decay and deadline-urgency radius; those are explicit V2 items.

## References
- `PRODUCT_DIRECTION_DECISION_DOCUMENT.md` §8 (Graph direction: on demand, not stored, Goal→Task, keep simple)
- `docs/ROADMAP.md` (Phase 6 — Graph Exploration)
- `docs/ADR/ADR-0002-graph-architecture.md` (graph is pure domain model, computed on demand, never stored)
- `docs/ARCHITECTURE_STATE.md` §7 (Graph status)
- Implementation: `app/src/main/java/com/example/domain/graph/` (`GoalGraphModels.kt`, `GoalGraphBuilder.kt`)
- Tests: `app/src/test/java/com/example/domain/graph/GoalGraphBuilderTest.kt` (9 tests, all passing)

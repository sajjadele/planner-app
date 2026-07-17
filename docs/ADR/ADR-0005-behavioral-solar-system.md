# ADR-0005 — Behavioral Solar System (Goal-Centered Graph)

- **Status:** Accepted
- **Date:** 2026-07-17
- **Phase:** 6.1 (Foundation Audit & Alignment)
- **Supersedes / extends:** ADR-0002 (graph architecture), ADR-0004 (solar-system design)

## Context

Vision Planner has a stable Goal Experience (Phases 1–5). A "Graph Exploration" feature
was partially hand-built inside `GoalDetailScreen` before this ADR. Phase 6.1 audits that
implementation against the intended Behavioral Solar System architecture and aligns the
foundation before advanced features are added.

The graph is explicitly **not** a generic data-visualization or knowledge-graph tool. It is a
**behavioral understanding tool for a single Goal**, opened from that Goal's detail view.

### Why a global / cross-goal graph was rejected

- The product philosophy is Goal-first: the user reasons about *one* goal and its tasks, never
  about a network of goals. A global graph would surface cross-goal structure the user does not
  need and that the domain model does not own.
- Offline-first and simplicity constraints favor a small, contextual, on-demand view over a
  persisted, traversable graph store (which would add schema, migration, and sync surface area).
- Life Area is explicitly **not** a first-class graph node (ROADMAP out-of-scope; ADR-0002).
- Relationship is strictly `Goal → Task`. No cross-goal edges, no Life-Area node.

## Decision

Adopt the **Goal-centered Behavioral Solar System** model:

1. **Single-goal scope.** The graph is computed for exactly one goal and rendered in a
   `ModalBottomSheet` launched from `GoalDetailScreen`. There is no global graph.
2. **Goal = Sun (center node).** Tasks = orbiting objects (satellites).
3. **Priority controls distance:** HIGH → inner orbit, MEDIUM → middle orbit, LOW → outer orbit.
4. **Priority controls size:** HIGH → largest node, MEDIUM → medium, LOW → smallest.
5. **Deterministic layout.** No random physics. Coordinates are computed from priority lanes plus
   a deterministic id-based angle jitter (`(id * 92821) % 1000`). Identical inputs always yield
   identical coordinates (verified by `GoalGraphBuilderTest`).
6. **Computed on demand, never stored.** The graph is a projection built in `domain.graph` from
   existing reactive sources; nothing is persisted to Room.
7. **Domain purity.** `domain.graph` has zero Android / Room / Compose imports; it is host-JVM
   testable. The UI only renders the precomputed `GoalGraph` model and never accesses a DAO.
8. **Compose-only rendering.** A custom `Canvas` renderer (`GoalGraphSheetContent`) draws the sun,
   Ring Tide halo, lane rings, gravity edges, and satellites. No graph library.
9. **Mandatory Help/Legend.** The sheet header carries a Help `IconButton` opening an `AlertDialog`
   that explains the metaphor (☀ goal, ● high priority, ● deadline warning, ○ completed).

### Model naming — intentional divergence from the original spec

The Phase 6 spec described `TaskNode`, `OrbitPosition`, and `TaskVisualState`. The implemented
and accepted model uses a single `GoalGraphNode` (discriminated by `NodeKind.GOAL | TASK`) carrying
computed absolute `(cx, cy)` coordinates, plus `GoalGraphEdge`, `GoalGraph`, and `GraphGeometry`.

**Decision: keep the current model.** It is behaviorally equivalent, avoids a churn-heavy rename
across the builder, renderer, and tests, and the unified node type is simpler than separate
`TaskNode`/`OrbitPosition` types. The divergence is documented here and will not be revisited
unless a concrete need (e.g. richer orbit metadata) arises.

## Consequences

### Benefits
- Clean, testable, deterministic domain layer independent of Android.
- No new database tables or migrations.
- Small, contextual UX that matches the Goal-first product philosophy.
- Easy to extend (V2): deadline-aware radius, drift/decay, Boulder emphasis.

### Trade-offs
- No cross-goal insight (accepted — out of scope).
- Deadline Warning is shown only as a greyed V2 placeholder in the legend; no deadline-radius
  logic yet (TaskEntity currently has no `deadlineEpochMs` field; `TaskInput.deadlineEpochMs` is
  plumbed as `null` for future use).
- Drift/Decay and AI interpretation deferred to V2 / Phase 7.

## Implementation pointers
- `app/src/main/java/com/example/domain/graph/` — `GoalGraphModels.kt`, `GoalGraphBuilder.kt`, `GraphGeometry`
- `app/src/test/java/com/example/domain/graph/GoalGraphBuilderTest.kt` — 9 passing tests
- `app/src/main/java/com/example/plugins/goals/ui/GoalDetailViewModel.kt` — `goalGraph`, `showGraphSheet`
- `app/src/main/java/com/example/plugins/goals/ui/GoalGraphSheetContent.kt` — Canvas renderer + Help/Legend
- `app/src/main/java/com/example/plugins/goals/ui/GoalDetailScreen.kt` — Galaxy entry button + sheet

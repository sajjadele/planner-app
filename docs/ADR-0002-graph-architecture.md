# ADR-0002: Graph Domain Model (designed in Phase 2, implemented in Phase 4)

- **Status:** Accepted (design only — implementation deferred to Phase 4)
- **Date:** 2026-07-13
- **Deciders:** Vision Planner core engineering
- **Phase:** Phase 2 — Architecture Stabilization (architecture design record)
- **Companion:** `docs/ROADMAP.md` (Phase 4 — Graph View Foundation)

## Context

The Vision Planner mental model is inherently a graph:

```
Goals → Tasks → Actions (Events) → Behavior → Insights
```

Today the app only renders **lists** (goal dashboard, planner day list, insight cards). The
relationships between entities exist only as implicit foreign keys (`tasks.goalId → goals.id`,
`tasks.lifeAreaId`, `task_events.taskId`). There is no reusable, typed representation of a
relationship with an attribute (weight) such as progress or recency.

Future phases require graph-shaped data:
- **Phase 4 (Graph View):** visualize Goal—Task—Life-Area relationships with weighted edges.
- **Phase 5 (AI Coach):** reason over the relationship structure, not just flat aggregates.

Phase 2 is the right moment to **decide the graph shape** (nodes, edges, repository contract)
so that Phase 4 only has to build the model + UI, not re-litigate the design. The actual
classes are intentionally **not** written in Phase 2 — requirements (layout, interaction,
weight semantics) are clearer once Phase 3 (progress/behavior snapshots) lands and gives edges
a concrete weight to render.

## Decision

The graph is modeled as a **pure-Kotlin, framework-free domain type**, separate from Room and
Compose. Specifically:

### Nodes
- `Node` is identified by an `id` and a `NodeType ∈ { GOAL, TASK, LIFE_AREA }`.
- **Life Area becomes a first-class node** (promoted from an `Int` column on `Task` to a stable
  registry/table with color/icon/progress). This is a Phase 4 change; recorded here so the
  node model accounts for it from the start.

### Edges
- `Edge` connects a `sourceId` to a `targetId`, carries an `EdgeType`, and an optional
  `weight: Float` (e.g. completion rate for `Goal—has→Task`, recency/progress for
  `Task—in→LifeArea`).
- `EdgeType ∈ { HAS_TASK, BELONGS_TO_LIFE_AREA, PROGRESS_TO }` (open to extension).

### Graph container
- `Graph(nodes: Set<Node>, edges: Set<Edge>)` — an immutable value object, no persistence.

### Repository contract
- `GraphRepository` (interface) exposes:
  - `fun buildGraph(): Graph` — one-shot build from existing relations + Phase 3 snapshots.
  - `fun observeGraph(): Flow<Graph>` — reactive rebuild when underlying data changes.
- The graph is **derived on demand**; it is never stored (per ROADMAP "Graph storage" is
  explicitly out of scope).

### UI boundary
- The Compose view consumes a `Graph` and lays it out. It must **not** query the database or
  DAOs directly. Layout/animation stay in the UI; data assembly stays in the domain.

## Alternatives Considered

### Alt A — Keep relationships implicit (FK joins per screen)
Recompute relationships ad-hoc in each screen/ViewModel as needed.

- **Rejected.** Duplicates relationship logic across screens, cannot render a unified graph,
  and makes the AI Coach (Phase 5) re-derive structure repeatedly. Re-litigates the same
  "single source" mistake that Phase 1 fixed for Goal→Task.

### Alt B — Persist the graph as its own table
Store nodes/edges in Room.

- **Rejected** (ROADMAP: "Graph storage — derive the graph on demand; do not persist it").
  The graph is a *projection* of goals/tasks/snapshots; persisting it creates a second source
  of truth that can drift, exactly the failure mode Phase 1 eliminated.

### Alt C — Couple the graph model to a rendering library (e.g. a graph DSL)
Define the node/edge types in terms of a specific visualization library.

- **Rejected.** Locks Phase 4/5 to one renderer and violates "keep the UI dumb / data in the
  domain." The domain model must be render-agnostic.

### Alt D — Build the graph classes now (chosen-timing variant)
Implement `Graph`/`Node`/`Edge`/`GraphRepository` in Phase 2.

- **Rejected for timing.** Weight semantics (what "progress" means on an edge) depend on the
  Phase 3 snapshot model, which does not exist yet. Designing now and implementing in Phase 4
  avoids a half-specified model and a likely rewrite. **This ADR pins the shape; Phase 4 pins
  the weights.**

## Consequences

### Positive
- Single, typed representation of relationships — reusable by Graph View, AI Coach, and any
  future consumer.
- Framework-free domain type → unit-testable without Android.
- UI stays dumb; data assembly isolated in `GraphRepository`.
- Life Area promoted to a real node, enabling clustering/coloring in the graph.

### Negative / Trade-offs
- `GraphRepository.buildGraph()` must join goals, tasks, life areas, and (Phase 3) snapshots —
  a non-trivial query/assembly cost. Mitigated by building on demand and only when the Graph
  screen is visible.
- Life Area promotion (Int → node) is a schema/domain change that Phase 4 must execute; this
  ADR flags it as a prerequisite, not a Phase 2 action.

### Neutral
- No code is written in Phase 2 for this ADR. It is a contract/design anchor.

## Constraints Honored

- **Offline-first:** no network, no sync — the graph is local-only.
- **Single source of truth:** the graph is derived, never authoritative.
- **Keep the UI dumb:** domain owns the model; UI only renders.
- **Incremental evolution:** Phase 4 implements against this contract without rewriting it.

## References

- `docs/ROADMAP.md` — Phase 4 (Graph View Foundation) defines the implementation trigger.
- `docs/ARCHITECTURE_STATE.md` — "Graph Readiness" tracks design vs implementation status.
- `com.example.domain.insight.InsightCalculator` — Phase 2 precedent for a pure-Kotlin domain
  layer that this graph model will follow.

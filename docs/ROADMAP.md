# Vision Planner — Roadmap

> High-level milestones only. Implementation details live in code and ADRs.
>
> Authority order: `PRODUCT_DIRECTION_DECISION_DOCUMENT.md` → `docs/ARCHITECTURE_STATE.md` → this file → ADRs → `AGENT.md`

---

## Guiding Principles

1. Offline-first is absolute.
2. Goal is the primary entity; Task is the execution unit.
3. Events are the source of truth; snapshots are rebuildable projections.
4. Feedback over judgment.
5. AI is a future enhancement, not a V1 dependency.
6. Keep the UI dumb; keep domain logic pure and testable.

Core chain:

```
Goal → Task → Event → Snapshot → Mirror → Feedback
```

---

## Phase Overview

| Phase | Name | Status |
|-------|------|--------|
| 1 | Foundation | ✅ Complete |
| 2 | Goal System | ✅ Complete |
| 3 | Behavior Data Foundation | ✅ Complete |
| 4 | Mirror Engine Foundation | ✅ Complete |
| 5 | Goal Experience Evolution | ✅ Complete (5.1–5.4) |
| 6 | Behavioral Solar System | ✅ Complete (6.1–6.4) |
| 7 | AI Insight Layer | Planned |

---

## Phase 1 — Foundation

**Status:** Complete

- Goal→Task single source of truth (`goalId` only)
- Remove denormalized goal title cache
- Stabilize Room schema baseline

Decision record: `docs/ADR/ADR-0001-goal-task-relationship.md`

---

## Phase 2 — Goal System

**Status:** Complete

- Goal lifecycle events (`goal_events`)
- Domain insight extraction (`domain.insight`)
- Repository interfaces for goals/insights

---

## Phase 3 — Behavior Data Foundation

**Status:** Complete

- Rebuildable progress/behavior snapshots
- Real-time today upsert + app-launch backfill
- Pure-Kotlin snapshot calculators (`domain.snapshot`)

Decision record: `docs/ADR/ADR-0003-progress-behavior-snapshots.md`

---

## Phase 4 — Mirror Engine Foundation

**Status:** Complete

- Detect behavioral patterns from existing data
- Provide neutral feedback inside Goal Detail
- No forced reflection / journaling
- No separate Mirror screen in V1

Initial patterns (all implemented):
1. Boulder
2. Initiator vs Finisher
3. Goal Attention
4. Consistency Decay

Implemented architecture surfaces:
- `domain.mirror` — `MirrorEngine`, `MirrorHeuristics`, `MirrorSignal` / `MirrorSignalType` / `MirrorInsight`
- `core.mirror` — `MirrorRepository` interface + `RoomMirrorRepository`
- `MirrorFeedbackCard` in Goal Detail

---

## Phase 5 — Goal Experience Evolution

**Status:** Complete (5.1–5.4)

Goals of this phase:
- Strengthen Goal-first daily UX
- Keep daily task execution simple
- Visually separate Inbox/capture tasks from goal-linked tasks
- Improve Goal Card progress and feedback presentation

Sub-phases:
- **5.1–5.2:** Goal lifecycle, performance optimizations
- **5.3:** GoalDetail metrics, active-days window
- **5.4:** Performance & UX Stability Audit (indexes, deferred init)

Decision records: `docs/ADR/ADR-0009-performance-audit-5.4.md`

---

## Phase 6 — Behavioral Solar System

**Status:** Complete (6.1–6.4)

- Goal = Sun (center), Task = orbiting satellite / cluster
- Computed on demand, never stored
- Visual understanding tool, not a complex knowledge graph
- Attention-driven positioning

Sub-phases:
- **6.1:** Foundation audit & alignment, Help/Legend
- **6.2:** Individual task satellites
- **6.3:** Adaptive cluster visualization
- **6.4:** Solar System motion & animation

Decision records:
- `docs/ADR/ADR-0002-graph-architecture.md`
- `docs/ADR/ADR-0004-graph-solar-system.md`
- `docs/ADR/ADR-0005-behavioral-solar-system.md`
- `docs/ADR/ADR-0008-solar-system-motion.md`
- `docs/ATTENTION_ARCHITECTURE.md`
- `docs/GRAPH_VIEW_RETROSPECTIVE.md`

---

## Phase 7 — AI Insight Layer

**Status:** Planned

Goals of this phase:
- Use structured events, snapshots, and Mirror outputs
- Amplify Mirror, do not replace it
- Remain offline-first
- No remote/network AI dependency under current constraints

---

## Explicitly Out of Scope

- Cloud sync / auth / remote AI
- Forced daily reflection
- Life Area as first-class entity or graph node
- Graph persistence
- Authoritative progress writes outside projections

---

*Read this with `PRODUCT_DIRECTION_DECISION_DOCUMENT.md` and `docs/ARCHITECTURE_STATE.md` for full context.*

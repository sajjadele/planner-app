# ADR-0007 — Adaptive Behavioral Solar System

- **Status:** Accepted
- **Date:** 2026-07-17
- **Phase:** 6.3 (Adaptive Visualization)
- **Extends:** ADR-0004 (solar-system design), ADR-0005 (goal-centered model), ADR-0006 (visual language)

## Context

Phase 6.1/6.2 delivered a correct, readable Solar System, but it always renders **every** task as
an individual satellite. For small goals that is ideal; for goals with many tasks the sheet becomes
visually overloaded and the user can no longer read *goal health* at a glance. The product goal is
to communicate goal state in under 2 seconds — not to list every task.

## Decision

### Overview / Detail adaptive modes
A single tunable constant `MAX_VISIBLE_TASKS = 8` (in `GoalGraphBuilder`) decides the rendering
mode from the **active** task count:

- **`INDIVIDUAL`** (active ≤ 8): every task is its own satellite — unchanged behavior.
- **`CLUSTER`** (active > 8): tasks are summarized into four deterministic clusters:

  | Cluster | Meaning | Position |
  |---------|---------|----------|
  | `ACTIVE_HIGH` | high-priority active tasks | inner lane |
  | `ACTIVE_MEDIUM` | medium-priority active tasks | middle lane |
  | `ACTIVE_LOW` | low-priority active tasks | outer lane |
  | `COMPLETED` | finished tasks (memory ring) | outer edge |

The mode is computed by the **domain builder**, not the UI, so the ViewModel/flow is untouched and
the choice is deterministic and testable.

### Domain model extension (additive, pure Kotlin)
- `enum class GraphMode { INDIVIDUAL, CLUSTER }`
- `enum class ClusterType { ACTIVE_HIGH, ACTIVE_MEDIUM, ACTIVE_LOW, COMPLETED }`
- `data class TaskClusterNode(id, clusterType, taskCount, cx, cy, visualSize, priorityLevel, memberIds)`
- `GoalGraph` gains `mode` and `clusters` fields (defaulting to `INDIVIDUAL` / empty, so all prior
  callers/tests are unaffected).

`GoalGraphNode` is **preserved** (spec: do not remove it). Cluster→task mapping is carried on the
cluster via `memberIds`, so in-view expansion needs no change to the task node model. The domain
remains free of Android / Compose / `android.graphics.Color` imports.

### Rendering rules
- When active ≤ `MAX_VISIBLE_TASKS`, the renderer draws individual satellites exactly as 6.2.
- In `CLUSTER` mode (overview) the renderer draws the four `TaskClusterNode`s, each showing its
  **task count** (e.g. "5", "12") and tinted by its `ClusterType`. The sun stays the dominant
  element (clusters' `visualSize` is clamped below the sun size).
- **Tap a cluster → expand** it in-view: its `memberIds` are drawn as individual satellites on that
  cluster's lane; the other clusters dim. **Tap an expanded task → highlight only** (existing 6.1
  behavior). **Tap the sun / empty space → collapse.** No navigation away from the graph; no detail
  popup (Goal Detail owns task details).

### Satellite prioritization (§4)
In `INDIVIDUAL` mode the displayed active tasks are ordered by priority rank (HIGH → MEDIUM → LOW →
none) then id, so the most meaningful tasks land first on their lanes. All ≤ 8 are still shown.
Deadline-proximity / recency ordering is a documented future hook (uses `TaskInput.deadlineEpochMs`
once available) — not hiding tasks randomly.

### Visual hierarchy (§5)
Sun = largest/center/primary focus; clusters = intermediate; task satellites = secondary. No
satellite may visually compete with the sun (sizes clamped).

### Animation
Deferred to Phase 6.4. The one-shot entrance fade from 6.2 remains; expansion is immediate (no
motion yet).

## Consequences

### Benefits
- Large goals (9…50+ tasks) read as 4 clusters, not a wall of dots — goal health in < 2s.
- Zero architecture change; display-only + additive domain types. All 6.1/6.2 tests stay green.
- `MAX_VISIBLE_TASKS` is a single, easily tunable constant.

### Trade-offs
- In `CLUSTER` mode individual tasks are not visible until a cluster is tapped (by design — overview
  first, detail on demand).
- Cluster positions are fixed per type (not fanned by member count) — keeps the layout calm and
  deterministic; member count is conveyed by the label + cluster size, not by angle.

### Future phases (out of 6.3 scope)
- Phase 6.4: expansion/collapse animation, cluster micro-interactions.
- V2: deadline-aware radius, drift/decay, richer prioritization signals.

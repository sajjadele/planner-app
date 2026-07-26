# ADR-0011 — Graph Visual Foundation (Audit & Independent Visual Evolution)

- **Status:** Accepted (audit phase; 6.5.x implementation follow-up planned)
- **Date:** 2026-07-17
- **Phase:** 6.5 (Visual Foundation Audit)
- **Extends:** ADR-0004 (solar-system design), ADR-0005 (goal-centered), ADR-0006 (visual language),
  ADR-0007 (adaptive), ADR-0008 (motion), ADR-0009 (performance), ADR-0010 (education/swipe)

## Context

Phase 6.1–6.4 + 5.4/5.5 delivered a correct, adaptive, animated Behavioral Solar System. Before any
further visual work, a **read-only audit** compared the current implementation against the desired
"calm, beautiful, understandable solar system" and produced a safe refactor plan. This ADR records the
audit conclusions and the standing decision that the **visual layer may evolve independently of the
domain architecture**, which stays unchanged.

> Naming: the repo had already used "Phase 6.3" for Adaptive Visualization (ADR-0007). This work is
> re-labeled **6.5** to avoid collision.

## Decision

### 1. Current visual approach needs refinement — but the architecture does not
The audit found the **domain and data flow are sound** and require no change:
- `domain.graph` is pure Kotlin (zero Android/Room/Compose imports), deterministic, host-JVM testable.
- `GoalDetailViewModel.goalGraph = combine(goal, tasks, rescheduleCounts, goalProgress)` →
  `GoalGraphBuilder.build` → `StateFlow<GoalGraph?>`; UI renders and never touches a DAO; VM is
  activity-scoped (survives tab switch).
- All signals needed for a visual upgrade (priority, completion, boulder, progress) already exist in
  `GoalGraphNode` / `GoalGraph`.

The problems are **render-layer only**:
- **Sun dominance weak** — core ≈12.5% viewport radius; Ring Tide halo low alpha reads as a dot+glow.
- **Orbit noise** — 5 rings drawn (HIGH/MEDIUM/LOW/undated/completed); undated (0.92) and completed
  (0.97) overlap and add clutter; only the 3 priority lanes are meaningful.
- **Priority differentiation subtle** at small canvas sizes.
- **No overdue/urgent encoding** — `TaskInput.deadlineEpochMs` is plumbed but the VM passes `null`
  (`GoalDetailViewModel.kt:140`) because `TaskEntity` has no deadline yet. Blocked until V2; adding it
  later is a pure UI+VM mapping change (no schema change needed for the *signal* — the field already
  flows through `TaskInput`).

### 2. Domain architecture remains unchanged
No model changes are required for any 6.5.x visual fix. `GoalGraphNode`, `GoalGraphEdge`, `GoalGraph`,
`TaskClusterNode`, `GraphGeometry`, and `GoalGraphBuilder` stay as-is except for **one additive, optional**
change in 6.5.5 (a member-sample cap for L3 scalability, which adds data without breaking existing types).
Determinism (no `Random`), on-demand compute, and "never stored" (ADR-0002) are preserved.

### 3. Visual layer can evolve independently
The domain deliberately exposes only `ColorRole` (never `android.graphics.Color`) and precomputed
geometry. Therefore the Compose renderer (`GoalGraphSheetContent.kt`) and theme (`Color.kt`) can be tuned
freely — sun size, ring set, glow, shimmer, halo — **without touching `domain.graph` or any test there**.
This is the same boundary that allowed 6.2/6.4 to ship display-only changes.

### 4. Doc/code drift correction
ADR-0006:30 and `ARCHITECTURE_STATE.md` (§7, §12) stated the Sun = `AccentPurple`. The implementation
uses **`AccentGold`** (`GoalGraphSheetContent.kt:369,380`; `colorForRole` GOAL→AccentGold; `Color.kt:32`).
The code is internally consistent and gold better matches the "sun" metaphor. **Decision: `AccentGold`
is the source of truth**; ADR-0006 and ARCHITECTURE_STATE text are corrected to match (6.5.1, zero code).

### 5. Animation verdict
Keep: staged entrance (sun→rings→nodes), cluster expand/collapse, Ring Tide breathing (already gated
post-entrance in Phase 5.5), satellite breathing shimmer (lower amplitude for LOW/completed), boulder
wobble. No gravity edges (already removed). **No physics or orbital motion** (honors ADR-0006 non-gamified tone).

## Recommended implementation order (6.5.x)
1. **6.5.1** Doc correction — Sun=`AccentGold` recorded as truth (zero code).
2. **6.5.2** Sun dominance — enlarge core, stronger gradient, tighter halo falloff (render only).
3. **6.5.3** Orbit denoise — draw only 3 priority rings; drop undated/completed rings (keep the points).
4. **6.5.4** Priority presence — strengthen HIGH glow/ring, ghost COMPLETED, calm LOW.
5. **6.5.5** Scalability handoff — lower `MAX_VISIBLE_TASKS` (→6); L3 capped member sample (additive).
6. **6.5.6 (V2)** OVERDUE signal — requires TaskEntity deadline; UI+VM mapping only.

## Consequences

### Benefits
- Clear separation: visual polish never risks the verified domain/tests.
- Target visual (dominant sun, calm 3-lane system, clear priority hierarchy) reachable with render edits.
- Doc drift closed; future readers get the true sun color.

### Trade-offs
- 6.5.2–6.5.4 are subjective visual tuning; validated by eye, not unit tests (builder/adaptive tests
  remain green and unaffected).
- OVERDUE (6.5.6) is deferred until task deadlines exist in the schema.

### What must remain unchanged
- Domain purity, determinism, on-demand compute, no stored graph state.
- Data flow (`combine` → `StateFlow`); no Composable accesses a DAO.
- Per-goal contextual / Goal-centered / no global graph.
- Kept animations; no physics. Phase 5.5 education gate + swipe nav untouched.

## References
- Audit record: `docs/ARCHITECTURE_STATE.md` §13 (Phase 6.5).
- Implementation: `app/src/main/java/com/example/plugins/goals/ui/GoalGraphSheetContent.kt`,
  `app/src/main/java/com/example/ui/theme/Color.kt`,
  `app/src/main/java/com/example/domain/graph/GoalGraphBuilder.kt` (6.5.5 only).

## 6.5.6 — Adaptive Density System (implemented)
The earlier two-tier `GraphMode { INDIVIDUAL, CLUSTER }` was replaced by a cleaner three-tier
`GraphDensityMode { SIMPLE, CLUSTERED, SUMMARY }`, chosen by **active** task count
(`SIMPLE_MAX_ACTIVE = 6`, `CLUSTERED_MAX_ACTIVE = 20`). This formalizes the "task count" concern into an
explicit density contract: SIMPLE = every task as a satellite; CLUSTERED = priority/completion clusters
with full tap-expand; SUMMARY = same clusters but tap-expand is priority-capped (L3, top-12 + "و N بیشتر").
The change is a pure enum rename + additive thresholds in `domain.graph` plus matching renderer branches;
no schema, no VM, no data-flow change. Tests renamed `AdaptiveGraphModeTest` → `AdaptiveDensityModeTest`
with SIMPLE/CLUSTERED/SUMMARY boundary assertions. This confirms the ADR's standing decision: the visual
layer (and its density/rendering policy) evolves independently of the unchanged pure-Kotlin domain model.

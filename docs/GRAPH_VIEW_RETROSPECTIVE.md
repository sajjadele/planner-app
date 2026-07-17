# Behavioral Solar System — Graph View Retrospective (Phases 6.1 → 6.4)

- **Author:** engineering retrospective, compiled 2026-07-17
- **Last updated:** 2026-07-17 (live-screen bug-fix pass — see §9)
- **Scope:** the Goal-centered "Behavioral Solar System" graph view, end to end.
- **Purpose:** one document capturing everything built and every decision made, so the next
  direction can be chosen deliberately.

---

## 0. TL;DR

A complete, production-shaped **Goal-centered graph view** exists:

- Pure-Kotlin domain that computes a deterministic solar-system layout on demand.
- A Compose `Canvas` renderer with a dominant Goal "sun" (progress ring + glow), priority lanes,
  task satellites, an adaptive cluster overview for large goals, and a calm motion language.
- Zero new database tables, zero stored graph state, no graph library, no Android imports in the
  domain.
- 19 passing pure-JVM tests (9 builder + 10 adaptive).

What is **deliberately deferred**: deadline-aware radius, drift/decay, AI interpretation, global
graph, task detail popups, any new DB fields.

---

## 1. Product framing

The graph is **not** a generic data visualization. It is a *behavioral understanding tool for a
single Goal*, opened from that Goal's detail.

```
Goal Detail  ──(Galaxy icon)──▶  Behavioral Solar System (bottom sheet)
                                      Goal = Sun (center)
                                      Task = orbiting satellite / cluster
```

No global graph. No cross-goal network. Relationship is strictly `Goal → Task`.

---

## 2. Phases & what shipped

### Phase 6.1 — Foundation Audit & Alignment
- Audited the partially-hand-built graph; confirmed it already matched the architecture.
- Added the **mandatory Help/Legend** button + `AlertDialog` (metaphor explanation; deadline item
  shown greyed as a V2 placeholder).
- Wrote `ADR-0005` (goal-centered model; global graph rejected; naming divergence documented).
- Result: stable foundation, 9 builder tests green.

### Phase 6.2 — Visual Language Foundation
- **Goal Sun** redesigned: dominant purple disc + **progress ring** (arc filled by
  `GoalProgress.overall`, cyan tint) + goal **title + %** drawn on the sun + Ring Tide glow that
  strengthens with progress (no fake values).
- **Orbit lanes** weighted by visual importance (HIGH brightest/thickest → LOW faintest).
- **Satellites**: HIGH gets a soft outer glow; LOW quieter; completed stay faded outer "memory"
  points.
- **Color system** stabilized via `ColorRole` → UI colors (domain stays color-free):
  `GOAL=AccentPurple, HIGH=AccentRed, MEDIUM=AccentFire, LOW=AccentGreen,
  COMPLETED=onSurfaceVariant, BOULDER=AccentRed`.
- Header subtitle → "وضعیت هدف در یک نگاه"; legend copy updated.
- One-shot entrance fade; no physics/random motion.
- `ADR-0006` written.

### Phase 6.3 — Adaptive Visualization
- **`MAX_VISIBLE_TASKS = 8`** threshold (active task count): ≤8 → individual satellites; >8 →
  four deterministic **clusters** (ACTIVE_HIGH / ACTIVE_MEDIUM / ACTIVE_LOW / COMPLETED) with count
  labels.
- Domain extended **additively**: `GraphMode`, `ClusterType`, `TaskClusterNode` (carries
  `memberIds`); `GoalGraph` gains `mode` + `clusters`. `GoalGraphNode` preserved.
- **In-view expansion**: tap cluster → its members animate in as satellites, others dim; tap task →
  highlight; tap sun → collapse. No navigation, no detail popup.
- 10 new `AdaptiveGraphModeTest` cases. `ADR-0007` written.

### Phase 6.4 — Solar System Motion & Animation
- **Staged entrance**: sun → rings → satellites/clusters (3 `Animatable`s).
- **Gentle breathing shimmer** on satellites (±3% scale / ±0.06 alpha, deterministic per-`id`
  phase; completed nodes quieter). **No orbital movement** — position never changes.
- **Sun breathing pulse** (amplitude 0.04→0.05), still tied to progress.
- **Calm cluster expand/collapse** (`expandProgress` `Animatable`, 320ms fade+scale, no spring).
- Motion language in `ADR-0008`. Display-only; prior tests unaffected.

---

## 3. Architecture (as built)

### Data flow
```
TaskEntity / GoalEntity
        │  (Room DAOs, via repositories)
        ▼
GoalDetailViewModel
        │  combine(goal, tasks, rescheduleCounts, goalProgress)
        ▼
GoalGraphBuilder.build(...)   ← pure Kotlin, deterministic, host-JVM testable
        ▼
StateFlow<GoalGraph?>  ──▶  GoalGraphSheetContent (Compose Canvas, renders only)
```
- **No Composable touches a DAO.** UI only renders the precomputed `GoalGraph`.
- **Computed on demand, never stored.**

### Domain layer (`app/src/main/java/com/example/domain/graph/`)
| File | Role |
|------|------|
| `GoalGraphModels.kt` | `GoalGraphNode`, `GoalGraphEdge`, `GoalGraph`, `GraphGeometry`, `NodeKind`, `ColorRole`, `GraphMode`, `ClusterType`, `TaskClusterNode` |
| `GoalGraphBuilder.kt` | deterministic layout; `MAX_VISIBLE_TASKS=8`; `buildClusters()`; priority-ranked ordering; `TaskInput` projection |
| `GoalGraphBuilderTest.kt` | 9 tests (lanes, sizing, boulder, completion, ring tide, determinism) |
| `AdaptiveGraphModeTest.kt` | 10 tests (mode thresholds, grouping, deterministic cluster positions, size clamp) |

**Invariants (verified):**
- Zero `android.*` / `androidx.*` / `Room` / `Compose` imports in `domain.graph`.
- Same inputs → identical coordinates (no `Random`).
- Boulder threshold = `rescheduleCount >= 2` (matches `MirrorHeuristics`).
- `TaskInput.deadlineEpochMs` is plumbed but passed as `null` (TaskEntity has no deadline field).

### Renderer (`GoalGraphSheetContent.kt`)
- Canvas: sun + progress ring + Ring Tide halo + 3 weighted lanes + gravity edges + satellites/
  clusters + Help dialog.
- Interaction: tap satellite → highlight + Persian label; tap cluster → in-view expand; tap sun →
  collapse.
- Motion: staged entrance, breathing shimmer, sun pulse, calm expand/collapse.
- All animation state is local `@Composable` state (`Animatable`s); no architecture change.

### Entry point (`GoalDetailScreen.kt`)
- Galaxy `IconButton` (`Icons.Filled.AutoGraph`) in the top bar → `ModalBottomSheet` hosting the
  graph. Mirrors the existing Mirror sheet pattern.

---

## 4. Decisions log (what we chose, and why)

| # | Decision | Why / Trade-off |
|---|----------|-----------------|
| D1 | Goal-centered only; reject global graph | Product is Goal-first; avoids schema/sync surface; Life Area not a node |
| D2 | Compute on demand; never persist graph | No migrations, offline-first, simple |
| D3 | Domain purity (no Android/Compose imports) | Host-JVM testable; clean boundary |
| D4 | Keep `GoalGraphNode` + `(cx,cy)` instead of spec's `TaskNode`/`OrbitPosition` | Behaviorally equivalent; avoids churn (ADR-0005) |
| D5 | Sun uses `AccentPurple`, not strict theme `primary` (`AccentCyan`) | Avoids cyan-on-cyan with the cyan progress ring; documented (ADR-0006) |
| D6 | Keep tap-to-highlight (passive) despite "no interaction" wording | Already shipped in 6.1; useful, no detail card (ADR-0006) |
| D7 | Clusters carry `memberIds`; `GoalGraphNode` untouched | Non-breaking expansion; clean model (ADR-0007) |
| D8 | `MAX_VISIBLE_TASKS = 8` single tunable constant | Easy to retune overview threshold |
| D9 | Cluster positions fixed per type (not fanned by count) | Calm, deterministic; count shown via label + size |
| D10 | Motion = breathing shimmer, **no orbital movement** | Attention without distraction; honors ADR-0006 "no orbit" |
| D11 | Cluster expand = calm fade+scale (320ms), no spring | Focus-first; no gamification (ADR-0008) |
| D12 | Deadline shown only as greyed V2 placeholder in legend | Honest; no misleading visualization yet |

---

## 5. Test status
- `GoalGraphBuilderTest`: 9 cases, passing.
- `AdaptiveGraphModeTest`: 10 cases, passing.
- Full `:app:testDebugUnitTest --tests "com.example.domain.*"`: green.
- Pre-existing Robolectric Room DAO/repository tests are **env-blocked** in this sandbox
  (SDK36 needs JDK21; only JDK17 present) — unrelated to graph work, documented in
  `ARCHITECTURE_STATE.md` §10.

---

## 6. Known limitations / open threads
- **Deadline:** `TaskEntity` has no `deadlineEpochMs`; urgency-radius mapping and "Deadline Warning"
  visual are V2 only (legend placeholder now).
- **Drift/Decay:** not modeled.
- **Prioritization ordering:** active tasks ≤ 8 are ordered by priority rank; deadline-proximity /
  recency ordering is a documented future hook (uses `TaskInput.deadlineEpochMs` once available).
- **Motion amplitudes** are conservative/subjective; tunable constants.
- **No global/cross-goal** view by design.

---

## 7. What could be done next (decision menu)

> Pick one. Each is independent; none require re-architecting what exists.

### A. V2 visual depth (build on current foundation)
- Deadline-aware radius / "Deadline Warning" highlight (needs `TaskEntity.deadlineEpochMs`).
- Drift/Decay system (tasks cooling over time without activity).
- Richer prioritization signals (recency, momentum) feeding layout/color.

### B. Interaction depth (still Goal-centered)
- Tap a satellite → navigate to `TaskDetailScreen` (explicit, not a popup) instead of highlight.
- Cluster multi-select / "show me only HIGH" filtered view.
- Long-press cluster → quick stats (count, completed ratio).

### C. AI layer (Phase 7)
- Mirror/Graph insight synthesis ("this goal is stalling because 3 HIGH tasks are Boulders").
- Offline-first constraint must hold; amplify Mirror, don't replace it.

### D. Polish & hardening
- Screenshot/visual regression tests for the Canvas (currently untested visually).
- Accessibility: ensure progress % and cluster counts are reachable by screen readers.
- Per-device motion-reduce (`reduceMotion`) support.

### E. Stop here
- The graph foundation (6.1–6.4) is feature-complete for the stated scope. Move engineering
  effort to another area (Planner, Mirror, Life Area, etc.).

---

## 8. Document index
- `docs/ADR/ADR-0002-graph-architecture.md` — graph architecture (reject stored/global)
- `docs/ADR/ADR-0004-graph-solar-system.md` — solar-system design
- `docs/ADR/ADR-0005-behavioral-solar-system.md` — goal-centered model + naming divergence
- `docs/ADR/ADR-0006-graph-visual-language.md` — visual language (sun, color, no physics)
- `docs/ADR/ADR-0007-adaptive-solar-system.md` — adaptive clusters
- `docs/ADR/ADR-0008-solar-system-motion.md` — motion language
- `docs/ARCHITECTURE_STATE.md` §7 — live Graph Status
- `docs/ROADMAP.md` Phase 6 — status line

---

## 9. Live-Screen Bug-Fix Pass (2026-07-17)

A close review of the rendered screen found three visual defects that contradicted the design
principles. All fixed in `GoalGraphBuilder.kt`, `GoalGraphSheetContent.kt`, and `Color.kt`.
Domain purity and the on-demand computation model are unchanged. Full `:app:compileDebugKotlin`
and `:app:testDebugUnitTest --tests "com.example.domain.*"` pass (10 builder + 10 adaptive + the
new regression test).

### 9.1 Duplicate-node bug (2 tasks → 4 circles) — CRITICAL
- **Root cause:** in `GoalGraphBuilder.build`, `placeLane` was called for the HIGH/MEDIUM/LOW lanes
  **and** again for `undated` tasks. `groupBy { it.priority ?: NO_PRIORITY_LANE }` already mapped
  `null`-priority tasks into the `"LOW"` key, so a `null`-priority task was placed **twice** (once
  in the LOW lane, once on the outer `undated` ring) → 2 tasks rendered as 4 circles.
- **Fix:** `byLane` now groups by the **raw** priority (`groupBy { it.priority }`); only tasks with
  an explicit `"LOW"` priority go in the LOW lane, while `null`-priority tasks are placed exactly
  once on the dedicated outer `undated` ring. `NO_PRIORITY_LANE` is still used correctly inside
  `buildClusters` (null tasks belong in `ACTIVE_LOW` there).
- **Regression test added:** `GoalGraphBuilderTest.two null-priority active tasks render exactly
  two task nodes` asserts exactly 2 `TASK` nodes and no duplicate ids.

### 9.2 Sun redesign — gold color + title moved above
- **Color:** `ColorRole.GOAL` now maps to a warm **gold** (`AccentGold = 0xFFFFB300`, added to
  `ui/theme/Color.kt`). Both the sun disc and the Ring Tide halo use gold (previously purple). This
  **overrides decision D5** from §4 (sun was purple to avoid cyan-on-cyan with the progress ring;
  the product now wants a literal "sun" gold, and the progress ring stays cyan so there is still
  enough contrast).
- **Title position:** the goal title is now drawn **above** the sun (clamped outside the halo/disc
  so it cannot overlap), colored `onSurface` for readability. Only the **progress percentage**
  ("1%") is drawn centered **inside** the sun. This removes the title/percentage overlap.

### 9.3 Gravity edges removed
- All `GoalGraphEdge` drawing is removed from the Canvas (the `drawLine` loop in `drawSolarSystem`
  is gone). Tasks now float purely on their orbits with no connecting lines, matching the design
  review. `GoalGraph.edges` data is still computed by the builder (harmless, retained for possible
  future use) but is no longer rendered.

### 9.4 Decision-log amendments
- **D5 (sun color)** — superseded: sun is now gold (`AccentGold`), not purple.
- **New D13** — no gravity edges drawn; tasks float on orbits.
- **New D14** — goal title rendered above the sun; only progress % inside.

### 9.5 Files changed in this pass
- `domain/graph/GoalGraphBuilder.kt` — de-duplicated placement (raw-priority grouping).
- `domain/graph/GoalGraphBuilderTest.kt` — +1 regression test.
- `ui/theme/Color.kt` — +`AccentGold`.
- `plugins/goals/ui/GoalGraphSheetContent.kt` — gold sun/halo, title-above-sun, edges removed,
  `colorForRole` GOAL → gold.


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

**Phase 4 — Mirror Engine Foundation — COMPLETE**

**Phase 5 — Goal Experience Evolution — COMPLETE (5.1–5.4)**

**Phase 6 — Behavioral Solar System — COMPLETE**

**Phase 5.4 — Performance & UX Stability Audit — COMPLETE** (see §11 and `docs/ADR/ADR-0009-performance-audit-5.4.md`)

---

## 1. Database Version

| Item | Value |
|------|-------|
| Schema version | **11** |
| Latest migration | `MIGRATION_10_11` (Phase 5.4: additive indexes) |
| Tables | `goals`, `goal_events`, `goal_progress_snapshot`, `behavior_snapshot`, `tasks`, `task_events`, `notes`, `module_settings` |

Phase 5.4 added three secondary indexes (no column change, fully non-destructive):
`tasks(dateEpochMs)`, `task_events(eventType)`, `goals(status)`.

Projection tables (`goal_progress_snapshot`, `behavior_snapshot`) are rebuildable from `tasks` + `task_events`.

---

## 2. Domain Structure

| Package | Status | Role |
|---------|--------|------|
| `domain.insight` | Implemented | Streak, rate, velocity, procrastination, neglected-goal math |
| `domain.snapshot` | Implemented | Daily goal progress + behavior projection math |
| `domain.mirror` | Implemented | Pattern heuristics + neutral feedback generation |

Domain packages are pure Kotlin (no Android imports), host-JVM testable.

---

## 3. Repository Status

| Repository | Status |
|------------|--------|
| `GoalRepository` | Interface + Room impl |
| `InsightRepository` | Interface + Room impl |
| `SnapshotRepository` | Interface + Room impl |
| `TaskRepository` | Concrete (not interfaced yet) |
| `MirrorRepository` | Interface + Room impl |
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

## 6. Mirror Architecture (Implemented — Phase 4)

| Item | Direction |
|------|-----------|
| Placement | Goal Detail screen via `MirrorFeedbackCard` (no separate Mirror screen in V1) |
| Patterns | Boulder, Initiator/Finisher, Goal Attention, Consistency Decay |
| Style | Neutral feedback, no judgment |
| Activation | Automatic; meaningful after ~7 days of usage |
| Reflection | Optional future feature only; never forced |

**`domain.mirror`** (pure Kotlin, host-JVM testable):
- `MirrorEngine` — renders a `MirrorSignal` into a neutral `MirrorInsight`
- `MirrorHeuristics` — four detectors: `detectBoulder`, `detectGoalAttention`, `detectInitiatorFinisher`, `detectConsistencyDecay`
- `MirrorSignal`, `MirrorSignalType`, `MirrorInsight` — signal/feedback model types

**`core.mirror`**:
- `MirrorRepository` — interface: observes reschedule counts, goal completion rates, goal events, behavior ranges; `evaluate(goalId)` + `render(signals)`
- `RoomMirrorRepository` — wires `InsightRepository`, `GoalRepository`, `RoomSnapshotRepository` into `evaluate`

**Snapshots as Mirror inputs:** Mirror consumes `behavior_snapshot` (`observeBehaviorRange`) and goal completion rates / reschedule counts derived from `task_events` + `goal_events`. Snapshots are projection inputs for analysis, never authoritative writes.

---

## 7. Graph Status

| Item | Status |
|------|--------|
| Priority | Phase 6 — foundation aligned (6.1) — Behavioral Solar System |
| Scope | Goal→Task only, contextual per-goal (bottom sheet on Goal Detail) |
| Persistence | None (computed on demand in `domain.graph`) |
| Life Area node | Not promoted |
| Rendering | Custom Compose Canvas, no graph library |
| Entry point | Galaxy `IconButton` (`Icons.Filled.AutoGraph`) in Goal Detail top bar |
| Help/Legend | Mandatory Help button in sheet header → `AlertDialog` metaphor legend |
| Drift/Decay (V2) | Deferred — no new query yet |
| Deadline Warning | Legend present as V2 placeholder (greyed); no radius change yet |
| Tests | `GoalGraphBuilderTest` (9, passing) |

**Phase 6.1 implementation status (audited & aligned):**
- ✅ Domain layer `domain.graph` (`GoalGraphModels`, `GoalGraphBuilder`, `GraphGeometry`) — pure Kotlin, **zero Android/Room/Compose imports** (host-JVM testable, verified). Computes a deterministic polar layout: HIGH inner / MEDIUM middle / LOW outer lanes; priority controls distance **and** node size; completed tasks become faded outer "memory" points; Boulder flag (rescheduleCount ≥ 2) drives UI wobble; `GoalProgress.overall` surfaces as the central Ring Tide glow.
- ✅ Data flow: `TaskEntity`/`GoalEntity` → Repositories → `GoalDetailViewModel` (`combine(goal, tasks, rescheduleCounts, goalProgress)`) → `GoalGraphBuilder.build` → `StateFlow<GoalGraph?>` → Canvas renderer. No Composable touches a DAO.
- ✅ UI: `GoalGraphSheetContent` renders sun + Ring Tide, 3 lane rings, gravity edges, task satellites, tap-to-select Persian label, deterministic Boulder wobble.
- ✅ Help/Legend button + dialog (V2 deadline item greyed as placeholder).

**Architectural decisions (6.1):**
- Model naming diverges from the original spec's `TaskNode`/`OrbitPosition`/`TaskVisualState`: we keep a single `GoalGraphNode` (with `NodeKind.GOAL|TASK`) carrying computed absolute `(cx,cy)`. Intentional — no behavioral difference, avoids churn. Documented in ADR-0005.
- `TaskInput.deadlineEpochMs` is plumbed but currently passed as `null` (TaskEntity has no deadline field). Retained for the V2 urgency-radius mapping.

**Remaining work:** V2 deadline-aware radius + drift/decay; Phase 7 (AI) depends on Mirror + Graph readiness.

**Phase 6.4 — Solar System Motion & Animation (done):**
- Staged one-shot entrance: sun → rings → satellites/clusters (3 `Animatable`s, `FastOutSlowInEasing`).
- Gentle breathing shimmer on satellites (±3% scale / ±0.06 alpha, deterministic per-`id` phase; completed nodes quieter). No orbital movement (honors ADR-0006).
- Sun Ring Tide pulse amplitude raised (0.04→0.05), still tied to `goalProgressOverall`.
- Cluster expand/collapse via `expandProgress` `Animatable` (320ms fade+scale, no spring); non-expanded clusters dim.
- Motion language documented in `docs/ADR/ADR-0008-solar-system-motion.md`. Display-only; 6.1/6.2/6.3 tests unaffected.

**Phase 6.3 — Adaptive Visualization (done):**
- `domain.graph` extended **additively**: `GraphMode` (INDIVIDUAL/CLUSTER), `ClusterType`, `TaskClusterNode(id, clusterType, taskCount, position, visualSize, priorityLevel, memberIds)`; `GoalGraph` gains `mode` + `clusters`. `GoalGraphNode` preserved.
- Adaptive threshold `MAX_VISIBLE_TASKS = 8` (active task count). ≤8 → individual satellites (6.2 behavior); >8 → four deterministic clusters (ACTIVE_HIGH/MEDIUM/LOW + COMPLETED) with count labels.
- Cluster positions deterministic (fixed lane angles, no Random); `visualSize` clamped below sun. Expansion is in-view: tap cluster → its `memberIds` drawn as satellites, others dim; tap task → highlight; tap sun → collapse. No navigation/detail popup.
- Display-only renderer change; ViewModel/flow untouched; 9 existing builder tests + 10 new `AdaptiveGraphModeTest` cases green.
- Decision record: `docs/ADR/ADR-0007-adaptive-solar-system.md`.

**Phase 6.2 — Visual Language Foundation (done):**
- Goal Sun redesigned: dominant gold disc (`AccentGold`) + **progress ring** (arc filled by `GoalProgress.overall`, cyan tint) + **title + %** drawn on the sun + Ring Tide glow that strengthens with progress (no fake values). (Corrected in 6.5.1: an earlier draft said purple; implementation has always used gold.)
- Orbit lanes weighted by visual importance (HIGH brightest/thickest → LOW faintest); deterministic layout unchanged.
- Satellites: HIGH gets a soft outer glow for stronger presence; LOW quieter; completed stay faded outer "memory" points. Tap-to-highlight + Persian label kept as passive aid.
- Color system stabilized via `ColorRole` → UI colors (GOAL=AccentGold, HIGH=AccentRed, MEDIUM=AccentFire, LOW=AccentGreen, COMPLETED=onSurfaceVariant, BOULDER=AccentRed). Domain stays color-free. (Corrected in 6.5.1: an earlier draft said AccentPurple; implementation has always used AccentGold — see `ui/theme/Color.kt:32`.)
- Header subtitle → "وضعیت هدف در یک نگاه"; Help/Legend copy updated (🔥 High Priority, ● Active Task, ○ Completed, ☀ Goal) with temporal/deadline shown as a greyed **future** item.
- Minimal one-shot entrance fade on open (sun→rings→nodes); no physics/random orbit motion.
- Display-only; no architecture/domain/ViewModel changes; 9 builder tests still green.

**Phase 6.3.1 — Solar Identity Refinement (done):**
- Goal title moved above Sun body with max-width constraint + ellipsis (1-line, 13sp), title–Sun gap increased (8dp→12dp).
- Sun body enlarged 1.6×→1.75× for stronger celestial presence; Ring Tide halo tightened (1.35+tide×0.9 → 1.25+tide×0.7) so glow hugs the disc.
- Progress ring repositioned: gap from Sun body increased 22%→35% for clear separation; stroke thickened 4dp→5dp for stronger progress read.
- Satellite visual hierarchy sharpened: HIGH nodes get +15% visual size multiplier, glow alphas strengthened (0.22/0.35→0.28/0.42); MEDIUM halo quieted (0.14→0.10); LOW unchanged (calm); COMPLETED stays ghosted.
- Orbit rings retuned with subtle priority tints: HIGH α0.40/w2dp (warm red), MEDIUM α0.18/w1.5dp (fire), LOW α0.08/w1dp (green) — main language remains solar system, not dashboard.
- Zero domain/data/ViewModel changes — Compose Canvas only. `compileDebugKotlin` + `testDebugUnitTest --tests "com.example.domain.*"` + `assembleDebug` SUCCESS.

Decision history: `docs/ADR/ADR-0002-graph-architecture.md`, `docs/ADR/ADR-0004-graph-solar-system.md`, `docs/ADR/ADR-0005-behavioral-solar-system.md`, `docs/ADR/ADR-0006-graph-visual-language.md`

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
| Goal Dashboard | Segmented tabs (Active / Completed / Archived) via `observeGoalsByStatus`; sorted deadline → recent activity → engagement (`domain.goal.GoalSort`) |
| Goal Card | 3-section redesign: Identity (title + status badge + overflow), Progress (پیشرفت + big % + bar), Activity Momentum (فعالیت اخیر + active-days + "آخرین حرکت") — all numbers English digits |
| Goal Detail | `GoalDetailScreen` + `GoalDetailViewModel`: identity + status-chip dropdown (valid transitions only via `GoalStatus.canTransition`), progress card with "ریتم فعالیت" (window momentum) + active-days/last-activity, Mirror as `IconButton` opening `ModalBottomSheet` (badge-ready), collapsible metadata (why/deadline), tasks section with empty-state that reuses `AddTaskDialog` preselecting this goal |
| Home / Planner | Goal-centric: today's tasks grouped by goal via `PlannerViewModel.goalTaskGroups` (`GoalTaskGroup`); each group = small goal header (🎯 / 📌 بدون هدف) + TaskCards. No progress/mirror in Home |
| Number formatting | `core.util.NumberFormatter` (`toEnglishDigits`/`toEnglishPercent`): Vision Planner renders Persian UI with English numbers; replaces `toPersianDigits` in goals UI + `GoalActivityFormatter` |
| Task-day indicators | `PlannerViewModel.daysWithTasks` (Set<Long> of midnight day-keys, ±60d window via `getTasksBetween`); green dot (`AccentGreen`) in `InfiniteWeekRow` `DayCell` and `CalendarGrid` cells. Distinct from red holiday dot (`HolidayRed`) — both show on overlapping days |
| Goal Dashboard card | `GoalCard` = compact overview: Identity (title + status chip + overflow) + compact "پیشرفت کلی" (18sp % + bar) + minimal activity (`N روز فعالیت` / `آخرین حرکت: …`). Metric boxes / deadline / "فعالیت اخیر" header removed — those live in Goal Detail. Same card bg/typography/bar as Detail. |
| Popup menus | `ui.screens.components.VisionPopupMenu` + `VisionMenuItem`/`VisionMenuDivider`: dark surface, rounded, RTL, optional icons; used by GoalCard + GoalDetail status dropdown |
| Progress | `domain.goal.GoalProgressCalculator`: completion weight 0.7 + activity momentum weight 0.3; momentum from rolling 30-day active-day window (not lifetime) |
| Tasks without goals | Allowed as Inbox/capture; visually separated |
| Life Area | Metadata only |

---

## 10. Testing Status

| Layer | Status |
|-------|--------|
| Domain unit tests | Present for insight + snapshot + mirror + goal calculators (GoalProgressCalculator, GoalSort, GoalActivityFormatter) and GoalStatus transitions |
| Mirror unit tests | Present (`MirrorHeuristicsTest`) |
| Room DAO tests | Present (Robolectric where SDK available) — env-blocked in sandbox (SDK36 needs JDK21 w/ javac; only JDK17 present) |
| ViewModel tests | Not started |

---

## 11. Performance Audit — Phase 5.4

**ADR:** `docs/ADR/ADR-0009-performance-audit-5.4.md`. Scope: speed / responsiveness / stability only —
no new features, no behavior change, no UI redesign.

### Discovered bottlenecks (verified by read-only audit)
| # | Area | Finding | Severity |
|---|------|---------|----------|
| B1 | VM/DB | `enrichAndSort` N+1: 2×N `first()` per-goal queries per tab emission | High |
| B2 | Compose/VM | Per-`GoalCard` flow fan-out (`progressFor` built a fresh `combine().stateIn()` per card) | High |
| B3 | DB | Missing indexes: `tasks.dateEpochMs`, `task_events.eventType`, `goals.status` | High/Med |
| B4 | Graph/Anim | 60fps breathing redraw + `textMeasurer.measure` every frame | Med/High |
| B5 | VM | `refreshMirror()` re-launched on every `goalRate` emission | Med |
| B6 | Compose | `GoalCard` took `viewModel` param + reallocated 6 lambdas per item → defeated skippability | Med |
| B7 | Compose | `SearchDialog` LazyColumn items had no `key` | Med |
| B8 | VM | Suspected duplicate `PlannerViewModel` → **verified non-issue** (nested composable shares owner) | Low/none |

### Applied optimizations
- **O1 — Bulk aggregation.** `GoalViewModel.goalsByTab` now `combine`s four single `GROUP BY` queries
  (`observeGoalCompletionRatesByStatus`, `observeGoalActivityBulk`, `observeGoalActivityBulkInWindow`,
  `observeGoalsByStatus`) instead of per-goal N+1. See `GoalDao` new bulk queries.
- **O2 — Precomputed `DashboardGoalItem`.** New model (`plugins/goals/ui/DashboardGoalItem.kt`) carries
  `goal`, `progress`, `lastActivity`, `activeDays`. `GoalCard` no longer holds a ViewModel or collects
  any Flow; dashboard lambdas are `remember(goalId)`-cached. `GoalSort` signature unchanged (pure
  `GoalEntity` + maps). **Compose responsibility shifted to ViewModel** (ADR-0009).
- **O3 — Indexes.** Schema `10 → 11` via `Migration(10,11)` (`CREATE INDEX IF NOT EXISTS`); entity
  `@Index` annotations added for fresh-install parity.
- **O4 — Graph render caching.** `sunNode`, `taskNodes`, sun title/% text layouts, and cluster-count
  text layouts are `remember(graph)`-cached in the composable scope and passed into `drawSolarSystem`.
  The 4s breathing animation is **kept**; only per-frame text layout is removed.
- **O5 — Mirror debounce.** `combine(goalRate, tasks, activeDays).debounce(250)` coalesces bursts.
- **O6 — Search keys.** `SearchDialog` `items` now use stable `key = { "task:n" / "note:n" }`.
- **O7 — Skipped** (B8 false positive).

### Before / After (analytical)
| Metric | Before | After |
|--------|--------|-------|
| Dashboard queries per tab emit | 2×N suspend `first()` + per-card 3 flows | 4 single `GROUP BY` queries (N-independent) |
| Per `GoalCard` flows collected | 3 (incl. fresh `stateIn` per recomposition) | 0 — stable `DashboardGoalItem` |
| Graph sheet idle frame | re-scan node list + 2–5 `TextMeasurer` calls | cached lookups + text; only pulse math |
| `refreshMirror()` on task toggle | once per `goalRate` emission (bursty) | coalesced at 250ms |
| Status/day/insight queries | full table scan | indexed |

### Rejected changes
- Removing the Graph breathing animation (kept + optimized instead).
- Consolidating all `GoalDetailScreen` flows into one UiState (deferred — larger refactor, no behavior gain).
- Deduplicating `PlannerViewModel.allGoals`/`GoalViewModel.allGoals` dual subscription (low impact).

### Remaining limitations
- `GoalDetailScreen` still collects ~11 independent `stateIn` flows (could be one UiState).
- Active dual subscription to `goals`/`allGoals` between Planner & Goal VMs (low impact).
- Robolectric Room DAO tests remain env-blocked (SDK36/JDK17); domain tests green.

---

## 12. Phase 5.5 — UX Polish & Interaction Improvements

**ADR:** `docs/ADR/ADR-0010-gesture-navigation-and-graph-education.md`. Scope: UX polish only — no DB
redesign, no product-behavior change, no Graph architecture change.

### Gesture navigation (Part 1)
- **Decision:** lightweight `pointerInput { detectHorizontalDragGestures }` on the tab-content
  `Modifier` in `MainScreen`; it only **writes `selectedTabId`** (bottom bar stays the single source
  of truth). `HorizontalPager` was rejected (no existing pager infra; would fight the `AnimatedContent`
  crossfade and require reconciling pager ↔ `selectedTabId` ↔ bottom-bar).
- Direction uses accumulated drag (> 60px threshold) flipped by `LocalLayoutDirection` (RTL-correct).
  An `isTabTransitioning` guard (~350ms) prevents double-switches during the crossfade.
- **Scope:** swipe enabled only between the two bottom tabs (`planner` ↔ `goals`); `notes` excluded
  (top-bar only, no bottom-bar ordinal).

### Graph first-time education (Part 2)
- New `GraphViewPreferences` (`com.example.plugins.goals.GraphViewPreferences`) — DataStore boolean
  `graph_introduction_seen`, mirroring `OnboardingRepository`.
- `GoalDetailViewModel.showGraphEducation = combine(showGraphSheet, hasSeenIntroduction) { open, seen ->
  open && !seen }` (VM-gated, survives tab teardown).
- First Graph open auto-shows the **existing in-sheet legend `AlertDialog`** (`GoalGraphSheetContent`);
  dismiss → `markGraphIntroductionSeen()`. Manual `Info` Help unchanged. Legend copy updated to required
  Persian text; future-feature row removed.

### Animation smoothness (Part 3)
- Audit: main recomposition cost is the full-tab `AnimatedContent` swap (`MainScreen.kt:137`) — accepted
  for 5.5; chrome is outside it. Graph canvas already optimized (ADR-0009). No new `derivedStateOf`/
  blocking-main-thread issues found.
- **Fix:** Graph breathing pulse (`rememberInfiniteTransition`, 4s) now starts only after the staged
  entrance completes (~640ms) via an `entranceDone` flag, so the sheet slide-in + assemble no longer
  overlaps the breathing. Animation kept; visual language preserved.

### Files touched
- `ui/screens/MainScreen.kt` — swipe gesture + transition guard.
- `plugins/goals/GraphViewPreferences.kt` (new) — DataStore education flag.
- `plugins/goals/ui/GoalDetailViewModel.kt` — `showGraphEducation` + `markGraphIntroductionSeen`.
- `plugins/goals/ui/GoalDetailScreen.kt` — passes education flag + dismiss callback.
- `plugins/goals/ui/GoalGraphSheetContent.kt` — auto-show legend, verbatim Persian copy, pulse gating.

---

## 13. Phase 6.5 — Behavioral Solar System Visual Foundation Audit

**ADR:** `docs/ADR/ADR-0011-graph-visual-foundation.md`. Scope: read-only visual audit + safe refactor plan
only. No DB redesign, no domain-model change, no data-flow change, no product-behavior change. (Audit
phase is documentation-only; the 6.5.x implementation steps below are the follow-up.)

> Naming note: the repo had already used "Phase 6.3" for Adaptive Visualization (ADR-0007). This audit is
> re-labeled **6.5** to avoid collision (6.1 foundation, 6.2 visual language ADR-0006, 6.3 adaptive ADR-0007,
> 6.4 motion ADR-0008, 5.4/5.5 polish).

### Current state (verified by read)
- Domain `domain.graph` is pure Kotlin (zero Android/Room/Compose imports); deterministic polar layout;
  lanes HIGH 0.34 / MEDIUM 0.58 / LOW 0.82 / undated 0.92 / completed 0.97·R; sizes HIGH 22 / MEDIUM 16 /
  LOW 12 / completed 6px; `MAX_VISIBLE_TASKS = 8` → CLUSTER mode; boulder = reschedule ≥ 2.
- Data flow `GoalDetailViewModel.goalGraph = combine(goal, tasks, rescheduleCounts, goalProgress)` →
  `GoalGraphBuilder.build` → `StateFlow<GoalGraph?>`; UI renders, no DAO access; VM activity-scoped.
- Renderer draws 5 orbit rings, gold sun + Ring Tide halo + cyan progress arc + title/% inside sun,
  satellites via `colorForRole`, breathing shimmer, boulder wobble, CLUSTER overview + tap-expand.

### Critical finding — doc/code drift (not a code bug)
ADR-0006:30 and §12/§7 of this doc said **Sun = `AccentPurple`**, but the implementation uses
**`AccentGold`** everywhere (`GoalGraphSheetContent.kt:369,380`; `colorForRole` GOAL→AccentGold;
`Color.kt:32` comment "AccentGold — Behavioral Solar System Sun"). The code is internally consistent
(gold); the documents are stale. Resolution: gold is the source of truth (matches the "sun" metaphor).
Only doc text needs correction — no code change.

### Visual problems (ranked)
1. Sun not visually dominant — core small (~12.5% viewport radius); halo low alpha reads as a dot + glow.
2. Five orbit rings = clutter — undated (0.92) and completed (0.97) rings overlap and add noise; only 3
   priority lanes are meaningful.
3. Cross-priority differentiation too subtle at small canvas sizes.
4. No overdue/urgent encoding — `TaskInput.deadlineEpochMs` is plumbed but VM passes `null`
   (`GoalDetailViewModel.kt:140`); TaskEntity has no deadline yet. Blocked until V2.
5. Abrupt individual→cluster handoff exactly at 9 active tasks; expansion can re-clutter at 20+.

### Target visual principles
- **Sun:** larger gold body, stronger core gradient, tighter halo falloff; Ring Tide intensity = progress.
- **Planets:** HIGH inner + largest + glow + optional ring; MEDIUM medium + light glow; LOW small + quiet;
  COMPLETED ghosted outline; BOULDER red halo + wobble (keep); OVERDUE (V2) distinct red marker.
- **Scalability:** L1 ≤8 individual; L2 9–20 cluster overview (tap to drill, dim others); L3 20+ cluster
  overview with capped member sample + "و N بیشتر" hint (additive builder sampling).

### Animation verdict
Keep: staged entrance, cluster expand, Ring Tide breathing (gated post-entrance, Phase 5.5), satellite
shimmer (lower for LOW/completed), boulder wobble. Remove/none: gravity edges already removed. No physics
or orbit motion (honors ADR-0006).

### Recommended implementation order (6.5.x)
1. **6.5.1** Doc correction — record Sun=`AccentGold` in ADR-0006 + this doc (zero code). ✅ DONE
2. **6.5.2** Sun dominance — enlarge core, stronger gradient, tuned halo (`drawSolarSystem` only). ✅ DONE
3. **6.5.3** Orbit denoise — draw only 3 priority rings; drop undated/completed rings (keep the points). ✅ DONE
4. **6.5.4** Priority presence — strengthen HIGH glow/ring, ghost COMPLETED, calm LOW. ✅ DONE
5. **6.5.5** Scalability handoff — lower `MAX_VISIBLE_TASKS` (→6), add L3 sample cap (additive domain). ✅ DONE
6. **6.5.6** Adaptive Density System — `GraphDensityMode { SIMPLE, CLUSTERED, SUMMARY }` replaces
   `GraphMode`; selected by **active** task count (≤6 SIMPLE, ≤20 CLUSTERED, >20 SUMMARY). ✅ DONE

### Implementation notes (6.5.1–6.5.5)
- **6.5.2** Sun: visual body enlarged to `sunR * 1.45`; flat disc → layered radial gradient
  (`0xFFFFE08A` hot core → `AccentGold` body → deeper gold edge) + white-hot center; Ring Tide halo
  tightened (peak alpha 0.30→0.45..0.90, max radius `1.5+tide*0.9` of body) and still driven by
  `goalProgressOverall`. Progress ring repositioned around the larger body; title/% stay centered.
- **6.5.3** Orbits: only HIGH 0.34 / MEDIUM 0.58 / LOW 0.82 rings drawn; undated (0.92) + completed
  (0.97) rings removed (points still render on those radii). ~40% less ring noise.
- **6.5.4** Satellites: HIGH gets tight bright glow ring + larger soft halo; MEDIUM a single soft halo;
  LOW shimmer reduced (0.03→0.02); COMPLETED now ghosted (faint fill + outline, no glow/shimmer).
- **6.5.5** `MAX_VISIBLE_TASKS` 8→6 (calmer L1→L2 handoff). Renderer L3 cap: expanded cluster samples
  top 12 members by priority + shows "و N بیشتر" hint for the rest (no re-clutter at 20+). `AdaptiveGraphModeTest`
  updated to the new 6/7 boundary (6 INDIVIDUAL, 7+ CLUSTER). All domain tests + assembleDebug green.
- Verification: `compileDebugKotlin` + `testDebugUnitTest --tests "com.example.domain.*"` + `assembleDebug` SUCCESS.
- 6.5.6 (overdue) deferred: `TaskEntity` has no deadline; `TaskInput.deadlineEpochMs` already plumbed
  through `GoalGraphBuilder`, so it becomes a pure UI+VM mapping change once deadlines exist.

### 6.5.6 — Adaptive Density System (implemented)
- Replaced the two-tier `GraphMode { INDIVIDUAL, CLUSTER }` with a three-tier `GraphDensityMode`
  `{ SIMPLE, CLUSTERED, SUMMARY }` in `domain.graph`. Threshold constants `SIMPLE_MAX_ACTIVE = 6` and
  `CLUSTERED_MAX_ACTIVE = 20` (replaced `MAX_VISIBLE_TASKS`). Builder computes `densityMode` from the
  **active** task count; individual `nodes` always computed; `clusters` built for CLUSTERED+SUMMARY.
- Renderer: `graph.densityMode != SIMPLE` → cluster overview + tap-expand; the L3 expansion sample cap
  (top-12 by priority + "و N بیشتر" hint) now applies **only in SUMMARY** — CLUSTERED expands all members.
- `GoalGraph.mode` field renamed to `densityMode`; hitTest + render branches updated. Zero schema/VM change.
- Tests: `AdaptiveGraphModeTest` → `AdaptiveDensityModeTest`; asserts SIMPLE(≤6)/CLUSTERED(7–20)/SUMMARY(>20)
  boundaries. compileDebugKotlin + domain tests + assembleDebug SUCCESS.

### What must remain unchanged
- Domain purity, determinism (no `Random`), on-demand compute, no stored graph state.
- Data flow (`combine` → `StateFlow`); no Composable touches DAO.
- Per-goal contextual / Goal-centered / no global graph (ADR-0002/0004).
- Kept animations; no physics. Education gate + swipe nav (Phase 5.5) untouched.

### Files in scope (render-layer only)
- `plugins/goals/ui/GoalGraphSheetContent.kt` — sun, rings, satellites, cluster rendering.
- `ui/theme/Color.kt` — `AccentGold` comment already correct.
- `domain/graph/GoalGraphBuilder.kt` — only for 6.5.5 additive sampling (`MAX_VISIBLE_TASKS` tweak).

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
| 2026-07-16 | Docs hierarchy cleaned; ADRs moved under `docs/ADR/` |
| 2026-07-16 | Phase 4 Mirror Engine implemented (domain.mirror + core.mirror, Goal Detail integration); docs synchronized |
| 2026-07-16 | Phase 5.1 Goal Foundation: DB v10, GoalStatus constants + transitions, optional why/deadlineEpochMs, archive, derived activity queries |
| 2026-07-16 | Phase 5.2 Goal Dashboard Experience: domain.goal (GoalProgressCalculator window-momentum, GoalSort deadline→recent→engagement, GoalActivityFormatter), segmented tabs, rich GoalCard, valid-transition status menu, delete-with-related tasks/events |
| 2026-07-16 | Phase 5.3 Goal Detail Experience: GoalDetailScreen + GoalDetailViewModel (progress/active-days/last-activity/Mirror sheet/status transitions), AddTaskDialog `initialGoalId` preselect, Mirror as IconButton+ModalBottomSheet, activity label "ریتم فعالیت"; pure tests GoalDetailProgressTest + GoalStatusDetailTransitionTest; assembleDebug SUCCESS |
| 2026-07-16 | Phase 5.4 UX Polish & Goal-Centric Home: NumberFormatter (English digits, Persian UI rule), GoalActivityFormatter → English digits, GoalCard 3-section redesign, VisionPopupMenu shared component, Home goal-grouped LazyColumn (`GoalTaskGroup` + `PlannerViewModel.goalTaskGroups`); pure tests NumberFormatterTest(7) + GoalTaskGroupingTest(6); assembleDebug SUCCESS |
| 2026-07-16 | Task-day indicators: `PlannerViewModel.daysWithTasks` (±60d window, `getTasksBetween`), green `AccentGreen` dot in week bar `DayCell` + calendar `CalendarGrid`; red `HolidayRed` holiday dot preserved and distinct; both show on overlap; pure test TaskDayKeysTest(4); assembleDebug SUCCESS |
| 2026-07-16 | Phase 5.2 UI refinement (3rd pass — split Dashboard vs Detail): `GoalCard` made compact — removed metric boxes (`تکمیل تسک‌ها`/`ریتم فعالیت`), "فعالیت اخیر" header, deadline block; keeps "پیشرفت کلی" (18sp % + bar) + minimal activity lines. Goal Detail `GoalProgressCard` unchanged (full analysis). No ViewModel/DB change; assembleDebug SUCCESS |
| 2026-07-17 | Phase 5.4 Performance & UX Stability Audit: eliminated dashboard N+1 (4 bulk GROUP BY queries), baked progress/activity into `DashboardGoalItem` (GoalCard no longer collects flows — Compose responsibility moved to ViewModel), added DB indexes (v10→11, non-destructive), cached Graph Canvas text/node lookups (animation kept), debounced Mirror recompute, keyed SearchDialog list; ADR-0009; compile + domain tests + assembleDebug SUCCESS |
| 2026-07-17 | Phase 5.5 UX Polish: swipe navigation between planner↔goals (lightweight gesture, bottom-bar source of truth, no HorizontalPager), first-time Graph education via `GraphViewPreferences` DataStore boolean (VM-gated `showGraphEducation`, auto-shows existing legend, marks seen on dismiss), delayed Graph breathing pulse until staged entrance completes (no animation removed); ADR-0010; compile + domain tests + assembleDebug SUCCESS |
| 2026-07-17 | Phase 6.5 Visual Foundation Audit (read-only): audited Behavioral Solar System rendering + domain + data flow; found doc/code drift (Sun actually `AccentGold`, ADR-0006 said purple), 5 noisy orbit rings, weak sun dominance, subtle priority differentiation, no overdue signal (blocked until task deadlines). Domain/data-flow confirmed sound — all fixes are render-layer only. Plan + 6.5.x order recorded; ADR-0011. No code changed this phase |
| 2026-07-17 | Phase 6.5 Visual Foundation implemented (6.5.1–6.5.5): 6.5.1 doc correction Sun=`AccentGold`; 6.5.2 enlarged luminous sun (gradient body + tighter Ring Tide halo); 6.5.3 denoised to 3 priority rings; 6.5.4 priority presence (HIGH glow rings, ghosted COMPLETED, calmer LOW); 6.5.5 `MAX_VISIBLE_TASKS` 8→6 + L3 expansion sample cap. Domain/data-flow untouched; `AdaptiveGraphModeTest` boundary updated (6/7); compile + domain tests + assembleDebug SUCCESS. 6.5.6 overdue deferred (no task deadline yet) |
| 2026-07-17 | Phase 6.5.6 Adaptive Density System: replaced `GraphMode{INDIVIDUAL,CLUSTER}` with `GraphDensityMode{SIMPLE,CLUSTERED,SUMMARY}` selected by active task count (≤6 SIMPLE, ≤20 CLUSTERED, >20 SUMMARY); `GoalGraph.mode`→`densityMode`; L3 expansion cap now SUMMARY-only (CLUSTERED expands all). `AdaptiveGraphModeTest`→`AdaptiveDensityModeTest` with 3-tier boundary asserts. compile + domain tests + assembleDebug SUCCESS |
| 2026-07-18 | Phase 6.3.1 — Solar Identity Refinement (Visual Polish Only): Goal title moved outside Sun with max-width constraint (dynamic ~200dp) + ellipsis; title gap increased 8→12dp; Sun body enlarged 1.6×→1.75×, Ring Tide halo tightened (1.35+tide×0.9→1.25+tide×0.7); progress ring gap increased 22%→35%, stroke 4→5dp; satellite hierarchy: HIGH +15% visual size, stronger glow (α0.28/0.42), MEDIUM quieter (α0.10); orbit rings retuned: HIGH α0.40/w2dp (warm red tint), MEDIUM α0.18/w1.5dp (orange tint), LOW α0.08/w1dp (green tint). Zero domain/data/ViewModel changes — Compose Canvas only. compileDebugKotlin + domain tests + assembleDebug SUCCESS |
| 2026-07-18 | Phase 6.3.1 — Solar Identity Refinement (visual polish only): Goal title moved above Sun with max-width constraint + ellipsis (1-line, 13sp), title-Sun gap increased (8dp→12dp); Sun body enlarged 1.6×→1.75×, Ring Tide halo tightened (1.35+tide×0.9 → 1.25+tide×0.7), progress ring gap increased 22%→35%, stroke thickened 4dp→5dp; Satellite hierarchy sharpened: HIGH +15% visual size, glow alphas 0.22/0.35→0.28/0.42; MEDIUM halo alpha 0.14→0.10; LOW unchanged (calm); Orbit rings retuned: HIGH α0.40/w2dp (warm red tint), MEDIUM α0.18/w1.5dp (fire tint), LOW α0.08/w1dp (green tint); Zero domain/data/ViewModel changes — Compose Canvas only; compile + domain tests + assembleDebug SUCCESS |
| 2026-07-18 | Phase 6.3.2 — Orbital Alignment & Goal Identity Separation: Goal title separated from Sun with ≥32dp gap (12dp→32dp) for clear identity hierarchy; coordinate audit verified — builder orbit fractions (HIGH 0.34, MEDIUM 0.58, LOW 0.82) match renderer exactly; satellites sit precisely on orbit rings via shared `viewportRadius * frac * scale` + `toCanvas()` transform. Temporary debug visualization confirmed center/radius alignment (removed after). Orbit fractions unchanged (density tuning deferred). Zero domain/data/ViewModel changes — Compose Canvas only. compileDebugKotlin + domain tests + assembleDebug SUCCESS |
| 2026-07-18 | Regression fix — Deadline placement correction: removed Deadline field from AddTaskDialog (belongs to Goal, not Task); restored Goal selector as primary task→goal linking; added Deadline field to AddGoalDialog for Goal-level strategic timing; Task→Goal relationship restored. Files changed: AddTaskDialog.kt (removed deadlineEpochMs), MainScreen.kt (updated callback), GoalDetailScreen.kt (updated callback), PlannerViewModel.kt (removed deadline param), AddGoalDialog.kt (added Deadline UI). No DB schema change, no domain/ViewModel architecture change. compileDebugKotlin + domain tests + assembleDebug SUCCESS |
| 2026-07-18 | Orbit fit fix — NO_DATE_FRACTION merged into LOW orbit: undated/no-priority tasks (NO_DATE_FRACTION 0.92→0.82) now sit on the same ring as LOW-priority tasks (0.82), eliminating orphan dots outside visible rings. Visual distinction: undated tasks render in muted AccentBlue (α0.7) with 25% alpha reduction instead of AccentGreen, so they read as "unclassified" rather than true LOW priority. GoalGraphBuilder.kt (fraction constant), GoalGraphSheetContent.kt (undated color override + comment update). compileDebugKotlin + domain tests + assembleDebug SUCCESS |

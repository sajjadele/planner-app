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

**Phase 5 — Goal Experience Evolution — IN PROGRESS (5.1–5.3 complete)**

**Next development direction:** Graph remains later (Phase 6).

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
- Goal Sun redesigned: dominant purple disc + **progress ring** (arc filled by `GoalProgress.overall`, cyan tint) + **title + %** drawn on the sun + Ring Tide glow that strengthens with progress (no fake values).
- Orbit lanes weighted by visual importance (HIGH brightest/thickest → LOW faintest); deterministic layout unchanged.
- Satellites: HIGH gets a soft outer glow for stronger presence; LOW quieter; completed stay faded outer "memory" points. Tap-to-highlight + Persian label kept as passive aid.
- Color system stabilized via `ColorRole` → UI colors (GOAL=AccentPurple, HIGH=AccentRed, MEDIUM=AccentFire, LOW=AccentGreen, COMPLETED=onSurfaceVariant, BOULDER=AccentRed). Domain stays color-free.
- Header subtitle → "وضعیت هدف در یک نگاه"; Help/Legend copy updated (🔥 High Priority, ● Active Task, ○ Completed, ☀ Goal) with temporal/deadline shown as a greyed **future** item.
- Minimal one-shot entrance fade on open (sun→rings→nodes); no physics/random orbit motion.
- Display-only; no architecture/domain/ViewModel changes; 9 builder tests still green.

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

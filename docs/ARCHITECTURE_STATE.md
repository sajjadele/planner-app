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
| Schema version | **15** |
| Latest migration | `MIGRATION_14_15` |
| Tables | `goals`, `goal_events`, `goal_progress_snapshot`, `behavior_snapshot`, `tasks`, `task_events`, `activity_events`, `task_steps`, `notes`, `module_settings` |

Migration history: v5→v6 (dayIndex→dateEpochMs), v10→v11 (additive indexes:
`tasks(dateEpochMs)`, `task_events(eventType)`, `goals(status)`), v11→v12
(`tasks.deadlineEpochMs`), v12→v13, v13→v14, v14→v15 (see `AppDatabase.kt` for details).

Projection tables (`goal_progress_snapshot`, `behavior_snapshot`) are rebuildable from `tasks` + `task_events`.

---

## 2. Domain Structure

| Package | Status | Role |
|---------|--------|------|
| `domain.insight` | Implemented | Streak, rate, velocity, procrastination, neglected-goal math |
| `domain.snapshot` | Implemented | Daily goal progress + behavior projection math |
| `domain.mirror` | Implemented | Pattern heuristics + neutral feedback generation |
| `domain.attention` | Implemented | Attention score (date pressure / staleness / avoidance) for graph positioning |
| `domain.graph` | Implemented | Goal→Task polar layout, density modes (SIMPLE/CLUSTERED/SUMMARY) |
| `domain.goal` | Implemented | Goal progress calculator, sort, activity formatter |

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
| Tests | `GoalGraphBuilderTest`, `AdaptiveDensityModeTest`, `VisibilityResolverTest`, `GraphVisibilityIntegrationTest` |

**Current implementation (post-6.x):**
- Domain layer `domain.graph` (`GoalGraphModels`, `GoalGraphBuilder`) — pure Kotlin, zero Android/Room/Compose imports, deterministic polar layout: HIGH inner / MEDIUM middle / LOW outer lanes; priority controls distance **and** node size; completed tasks fade to outer "memory" points; Boulder flag (rescheduleCount ≥ 2) drives UI wobble; `GoalProgress.overall` drives the central Ring Tide glow.
- Density modes via `GraphDensityMode { SIMPLE, CLUSTERED, SUMMARY }` (6.5.6): active-task count ≤6 → individual satellites, 7–20 → four deterministic priority clusters (tap to expand in-view), >20 → SUMMARY with capped member sample + "و N بیشتر" hint. Cluster positions deterministic (no `Random`).
- Data flow: `TaskEntity`/`GoalEntity` → Repositories → `GoalDetailViewModel` (`combine(goal, tasks, rescheduleCounts, goalProgress)`) → `GoalGraphBuilder.build` → `StateFlow<GoalGraph?>` → Canvas renderer. No Composable touches a DAO.
- UI: `GoalGraphSheetContent` renders sun + Ring Tide + 3 priority rings, satellites via `colorForRole` (GOAL=AccentGold, HIGH=AccentRed, MEDIUM=AccentFire, LOW=AccentGreen, COMPLETED=onSurfaceVariant), tap-to-select Persian label, deterministic Boulder wobble, staged entrance + breathing shimmer (no orbital motion — ADR-0006/0008).
- Help/Legend button + first-time education gate (Phase 5.5).
- `TaskInput.deadlineEpochMs` plumbed and populated since DB v12 (ADR-0013); urgency-radius mapping (V2) still not implemented.
- Tests: `GoalGraphBuilderTest`, `AdaptiveDensityModeTest`, `VisibilityResolverTest`, `GraphVisibilityIntegrationTest`.

Decision history: `docs/ADR/ADR-0002-graph-architecture.md`, `ADR-0004-solar-system.md`, `ADR-0005-behavioral-solar-system.md`, `ADR-0006-graph-visual-language.md`, `ADR-0007-adaptive-solar-system.md`, `ADR-0008-solar-system-motion.md`, `ADR-0011-graph-visual-foundation.md`

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

## 11. Performance & UX History (5.4 / 5.5 / 6.5)

Historical optimization and polish work — full details live in their ADRs, not here:

- **Phase 5.4 — Performance & UX Stability Audit** — `docs/ADR/ADR-0009-performance-audit-5.4.md`.
  Eliminated dashboard N+1 (bulk `GROUP BY`), precomputed `DashboardGoalItem`, added DB indexes
  (v10→11), cached Graph Canvas text/node lookups, debounced Mirror recompute, keyed SearchDialog.
- **Phase 5.5 — UX Polish & Interaction** — `docs/ADR/ADR-0010-gesture-navigation-and-graph-education.md`.
  Swipe navigation (planner↔goals), first-time Graph education, gated breathing pulse.
- **Phase 6.5 — Solar System Visual Foundation** — `docs/ADR/ADR-0011-graph-visual-foundation.md`.
  Gold sun dominance, 3 orbit rings, priority presence, `MAX_VISIBLE_TASKS` 8→6, Adaptive Density
  System (`GraphDensityMode { SIMPLE, CLUSTERED, SUMMARY }`).

Current state is captured in §7 (Graph Status); these sections are history.

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

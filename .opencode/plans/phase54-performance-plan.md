# Phase 5.4 — Performance & UX Stability Audit (Implementation Plan)

> **Scope guard:** This phase ONLY improves speed / responsiveness / stability / perceived quality.
> No new features, no behavior changes, no UI redesign, no Graph UX changes, no task/goal behavior changes.

## Audit Method
Read-only exploration (sub-agent `explore`) + targeted file reads to verify the highest-impact claims.
Verified directly:
- `GoalCard` collects 3 flows per card (`GoalCard.kt:66-68`); `GoalViewModel.progressFor` builds a fresh `combine().stateIn()` per call (`GoalViewModel.kt:97-114`).
- `GoalViewModel.enrichAndSort` runs 2×N `first()` queries in a loop (`GoalViewModel.kt:86-90`) — confirmed N+1.
- Entities: `tasks` has only `Index("goalId")` (`TaskEntity.kt:19`); `task_events` has only `Index("taskId")` (`TaskEventEntity.kt:9`); `goals` has **no** index (`GoalEntity.kt:16`); `tasks.dateEpochMs` and `task_events.eventType` **unindexed** — confirmed.
- DB version = 10 (`AppDatabase.kt:35`); `fallbackToDestructiveMigration()` present (`AppDatabase.kt:202`). Index additions need a v10→v11 `Migration` with `CREATE INDEX IF NOT EXISTS`.
- `GoalGraphBuilder.build` is correctly memoized inside `stateIn`'d combine (`GoalDetailViewModel.kt:125-142`) — NOT per-frame. Graph cost is the **infinite redraw** + repeated `textMeasurer.measure` per frame (`GoalGraphSheetContent.kt:94-103, 341, 350, 473, 509`) — confirmed by audit.

---

## Discovered Bottlenecks (verified)

| # | Area | Finding | Severity | Evidence |
|---|------|---------|----------|----------|
| B1 | VM/DB | `enrichAndSort` N+1: 2×N `first()` per-goal queries on every tab switch & list emission | **High** | `GoalViewModel.kt:86-90` |
| B2 | Compose/VM | Per-`GoalCard` flow fan-out: `progressFor` builds fresh `combine().stateIn()` per card; duplicates B1 aggregates | **High** | `GoalCard.kt:66-68`, `GoalViewModel.kt:97` |
| B3 | DB | Missing indexes: `tasks.dateEpochMs`, `task_events.eventType`, `goals.status` → full scans on day/insight/graph recompute | **High/Med** | entity files |
| B4 | Graph/Anim | Infinite 4s breathing redraw + `textMeasurer.measure` every frame (sun title/%, selected label, cluster counts) while sheet open | **Med/High** | `GoalGraphSheetContent.kt` |
| B5 | VM | `refreshMirror()` re-launched on every `goalRate` emission → full Mirror aggregation on each task toggle | **Med** | `GoalDetailViewModel.kt:151-156` |
| B6 | Compose | `GoalCard` takes `viewModel: GoalViewModel` param + creates 6 lambdas per item/per emission → defeats skippability | **Med** | `GoalCard.kt:46-55`, `GoalDashboardScreen.kt:153-170` |
| B7 | Compose | `SearchDialog` LazyColumn items have no `key` | **Med** | `SearchDialog.kt:206` |
| B8 | VM | Duplicate `PlannerViewModel` instance created in `GoalDetailScreen` (`viewModel()` call) | **Med/Low** | `GoalDetailScreen.kt:83` |

Low/non-issues (verified OK, no action): `GoalGraphBuilder.build` memoization, `goalGraph` flow collection once at screen level, `GoalEntity`/`TaskEntity`/`GoalGraph` data-class stability, `flatMapLatest` usage, `MainScreen` VMs surviving tab switches, `ModalBottomSheet` one-shot anims, `AnimatedContent` tab transitions.

---

## Optimization Plan (applied only if justified)

### O1 — Eliminate dashboard N+1 (fixes B1)
Add a single bulk DAO query and replace `enrichAndSort`'s per-goal loop:
- `InsightDao`/`GoalDao`: add `observeGoalActivityBulk(status)` returning `Map<Int, GoalActivity>` via one `GROUP BY goalId` query (last-activity `MAX`, active-days `COUNT(DISTINCT day)`) for the selected status's goals.
- `GoalViewModel.enrichAndSort` → uses the bulk map (one query) instead of N `first()` calls.
- Keep `GoalSort.sort` signature; pass the bulk map.

### O2 — Hoist per-card flows out of `GoalCard` (fixes B2 + B6 partly)
- Compute `progress`, `lastActivity`, `activeDays` once per goal inside `goalsByTab` enrichment (reuse O1's bulk maps) and emit them as part of the dashboard list model.
- Change `GoalCard` signature to receive `progress: GoalProgress?`, `lastActivity: Long?`, `activeDays: Int` as **stable params**; remove `viewModel: GoalViewModel` param and the in-card `collectAsState` calls.
- Cache the 6 action lambdas in `GoalDashboardScreen` with `remember(goal.id)`.
- `progressFor`/`lastActivityFor`/`activeDaysFor` remain in VM for other callers (`GoalDetailScreen`) but are no longer called from the card.

### O3 — Add missing DB indexes (fixes B3)
- Bump `AppDatabase` `version = 11`.
- Add `Migration(10, 11)` with `CREATE INDEX IF NOT EXISTS` for:
  - `tasks(dateEpochMs)`
  - `task_events(eventType)`
  - `goals(status)`
- `exportSchema = false` so no schema file needed; index-only migration is non-destructive.
- Also add `Index("dateEpochMs")` to `TaskEntity`, `Index("status")` to `GoalEntity`, `Index("eventType")` to `TaskEventEntity` so fresh-install schema matches the migration.

### O4 — Cache Graph Canvas work across frames (fixes B4)
All inside `GoalGraphSheetContent`, no behavior change, no animation removal:
- Memoize static node lookups with `remember(graph)`: `sunNode`, `taskNodes`.
- Memoize every `textMeasurer.measure(...)` with `remember(label, size)` so text layout recomputes only on change, not every frame.
- Keep the infinite breathing pulse (visual spec preserved); only per-frame CPU cost drops.

### O5 — Debounce Mirror recompute (fixes B5)
- Replace `goalRate.collect { refreshMirror() }` with a `combine(...).debounce(250)` (or derive `mirrorInsights` via `stateIn` combine) so it recomputes once per settled change. Result identical.

### O6 — `SearchDialog` item keys (fixes B7)
- Add `key = { it.id }` (or stable unique id) to `items(searchResults)`.

### O7 — Avoid duplicate `PlannerViewModel` in GoalDetail (fixes B8)
- Remove the second `viewModel<PlannerViewModel>()` in `GoalDetailScreen`; reuse the existing instance or pass needed data. Verify no UI depends on the detail-scoped instance.

---

## Rejected Changes (documented, not applied)
- **Removing the graph breathing animation** — rejected: violates "animations should remain visually pleasing / do not remove unless measurable problem." We optimize its cost (O4) instead.
- **Consolidating all 11 `GoalDetailScreen` flows into one UiState data class** — deferred: larger refactor; logged as remaining limitation. No behavior change but outside "small optimization" scope.
- **Deduplicating `PlannerViewModel.allGoals`/`GoalViewModel.allGoals` dual subscription** — deferred: low impact; noted as limitation.
- **Premature micro-optimizations** (inline `validNextStatuses`, `drawBehind` PathEffect in Notes) — skipped; negligible cost.

## Remaining Limitations (post-phase)
- GoalDetailScreen still collects multiple independent `stateIn` flows (could be one UiState).
- Active dual subscription to `goals`/`allGoals` between Planner & Goal VMs.
- Breathing pulse still redraws at 60fps (now cheap text/cache-wise; acceptable).

---

## Files to Modify
- `app/src/main/java/com/example/plugins/goals/ui/GoalViewModel.kt` (O1, O2)
- `app/src/main/java/com/example/plugins/goals/ui/GoalCard.kt` (O2)
- `app/src/main/java/com/example/plugins/goals/ui/GoalDashboardScreen.kt` (O2 lambdas)
- `app/src/main/java/com/example/core/database/AppDatabase.kt` (O3 version + Migration 10→11)
- `app/src/main/java/com/example/core/goal/GoalEntity.kt` (O3 index)
- `app/src/main/java/com/example/plugins/planner/data/TaskEntity.kt` (O3 index)
- `app/src/main/java/com/example/plugins/planner/data/TaskEventEntity.kt` (O3 index)
- `app/src/main/java/com/example/plugins/planner/data/InsightDao.kt` (O1 bulk query)
- `app/src/main/java/com/example/plugins/goals/data/GoalDao.kt` or repo (O1 bulk query)
- `app/src/main/java/com/example/plugins/goals/ui/GoalGraphSheetContent.kt` (O4)
- `app/src/main/java/com/example/plugins/goals/ui/GoalDetailViewModel.kt` (O5)
- `app/src/main/java/com/example/ui/screens/SearchDialog.kt` (O6)
- `app/src/main/java/com/example/plugins/goals/ui/GoalDetailScreen.kt` (O7)

## New DAOs / queries (O1)
- `InsightDao.observeGoalActivityBulk(status: String): Flow<Map<Int, GoalActivityRow>>` — single `GROUP BY goalId` over `tasks`/`task_events` for the status's goals. (Exact join shape TBD against existing `observeGoalLastActivity`/`observeGoalActiveDayCount`.)

---

## Verification
1. `./gradlew :app:compileDebugKotlin`
2. `./gradlew :app:testDebugUnitTest` (keep existing domain + VM tests green; add a regression test that `goalsByTab` issues ONE aggregate query, not N — if a test hook exists; otherwise assert via DAO-call count in a new unit test).
3. `./gradlew :app:assembleDebug`
4. Manual smoke (no behavior regression): tab switch, open Goal Detail, open Graph sheet (verify breathing still animates, text centered), open BottomSheet, search.

## Documentation
- Update `docs/ARCHITECTURE_STATE.md` → add **"Performance Audit — Phase 5.4"** section: discovered bottlenecks (B1–B8), applied optimizations (O1–O7), rejected changes, remaining limitations.
- If any change is deemed an architectural decision (bulk-aggregate query model, hoisting card flows out of composables) → create ADR: **ADR-0009 Performance Audit 5.4 optimizations**.
- Append a short subsection to `docs/GRAPH_VIEW_RETROSPECTIVE.md` for O4 (graph canvas caching).

## Stop
After `ARCHITECTURE_STATE.md` updated and build/tests green, produce the Phase 5.4 completion report and stop. No further phases.

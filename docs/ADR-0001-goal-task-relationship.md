# ADR-0001: Goal–Task Relationship Represented Only by `goalId`

- **Status:** Accepted
- **Date:** 2026-07-13
- **Deciders:** Vision Planner core engineering
- **Phase:** Phase 1 — Foundation Cleanup

## Context

The `TaskEntity` previously carried **two** representations of its relationship to a
`Goal`:

1. `goalId: Int?` — a real foreign key (`tasks.goalId → goals.id`, `SET NULL` on delete).
2. `goalName: String?` — a denormalized cache of the goal's title, written at task
   creation / goal reassignment "for backward compatibility."

This duplication created a second, unofficial source of truth. The risks observed in
the codebase:

- `InsightDao.observeCompletionByGoal` grouped completions by the cached `goalName`
  string, while `GoalDetailViewModel` joined by the `goalId` FK — **two different
  answers for the same question**.
- A goal rename left `goalName` stale on every linked task, silently corrupting
  aggregates and any future Graph View edge labels.
- The cache was written from multiple places (`PlannerViewModel.addTask`,
  `AddTaskDialog`, `OnboardingViewModel.finish`, `TaskDetailViewModel.updateTaskGoal`),
  each a chance to drift.

Vision Planner's future features (Graph View, goal-progress visualization, AI insights,
long-term behavioral analysis) all depend on a **single, reliable** relationship between
goals and tasks. A duplicated relationship is a direct blocker for those features.

## Alternatives Considered

### Alt A — Keep `goalName`, add an update-trigger on goal rename
Add a `GoalRepository.updateGoalTitle` path that cascades the new title into every linked
`TaskEntity.goalName`.

- **Rejected.** Still duplicates state (two sources of truth). A rename that fails mid-write
  (process death, crash) leaves linked tasks stale. It also adds write complexity to every
  rename path and bloats each task row with a redundant string.

### Alt B — Keep `goalName`, stop reading it (read-only deprecation)
Retain the column for "backward compatibility" but read titles only via the `goalId` join.

- **Rejected.** Ships a dead column that misleads future developers into using it (the exact
  bug already observed in `InsightDao.observeCompletionByGoal`). Old rows still carry stale
  data that could surface through an unguarded read.

### Alt C — Remove `goalName`, resolve title via the `goalId` FK (CHOSEN)
Drop the cache; resolve titles at read time by joining `goalId → goals.id`.

- **Trade-offs.** Requires a join / `getGoalById` lookup at read time. At personal scale
  (dozens–hundreds of goals) this is free and is the correct cost of correctness. Tasks whose
  goal was deleted show "بدون هدف" (`SET NULL`) instead of a stale title — intended behavior.
- **Benefit.** One source of truth; goal renames, deletions, and future Graph View edges can
  never diverge.

## Decision

**The Goal→Task relationship is represented exclusively by `goalId` (the foreign key).**

- `goalName` is removed from `TaskEntity` (schema `v6 → v7` migration drops the column).
- Goal titles are resolved by joining `goalId → goals.id` at read time (UI, DAOs, insight
  queries). There is no cached title on the task.
- `PlannerViewModel.addTask`, `AddTaskDialog`, `OnboardingViewModel`, and
  `TaskDetailViewModel` no longer accept or persist a goal title.
- The legacy `InsightDao.observeCompletionByGoal` (grouped by `goalName`) is deleted;
  goal breakdowns use `observeGoalCompletionRates` (FK join).

## Consequences

### Positive
- One source of truth: goal renames, deletions, and graph edges can never diverge.
- Smaller rows, simpler writes, no drift bug class.
- Insight pipeline no longer depends on `TaskDao` for title lookups (the
  `runBlocking` lookup in `WeeklyInsightViewModel` is removed in the same phase).

### Negative / Trade-offs
- Resolving a goal's title now requires a join or a lookup by `goalId`. At personal
  scale (dozens–hundreds of goals) this is free; it is the correct cost of correctness.
- A task whose goal was deleted shows "بدون هدف" (FK `SET NULL`) instead of a stale
  cached title. This is the *intended* behavior, not a regression.
- Historical tasks created before this migration that were linked only via the cache
  (never had a valid `goalId`) lose their (already-stale) display title. Acceptable:
  such rows were already inconsistent.

### Neutral
- Existing installs migrate via an explicit `MIGRATION_6_7` (table recreate preserving
  all other columns/rows); `fallbackToDestructiveMigration()` remains only a safety net.

## Constraints Honored

This ADR introduces **no** new architectural layers (no User entity, no progress
snapshots, no behavior store, no DI migration, no AI architecture). It is strictly a
data-model de-duplication within Phase 1 scope.

## References

- `TaskEntity.kt` — `goalName` field removed.
- `AppDatabase.kt` — `MIGRATION_6_7` (drop `goalName`).
- `InsightDao.kt` — `observeCompletionByGoal` / `GoalCompletion` removed;
  `observeRescheduleCounts` now JOINs `tasks` for the title.
- `WeeklyInsightViewModel.kt` — `taskDao` dependency and `runBlocking` lookup removed.
- `PlannerViewModel`, `MainScreen`, `AddTaskDialog`, `OnboardingViewModel`,
  `TaskDetailViewModel`, `TaskDetailScreen` — `goalName` parameter/usage removed.

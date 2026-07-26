# Phase 2A — Attention Foundation: Completion Report

## Objective

Build a pure-domain Attention Score calculator for the Behavioral Solar System that computes how much a task needs the user's attention based solely on behavioral signals — never priority, completion ratio, or subjective importance.

---

## Files Changed

| File | Change | Impact |
|---|---|---|
| `domain/attention/AttentionCalculator.kt` | **Created** — Pure-Kotlin calculator with three signals: DatePressure (logistic), Staleness (quadratic), Avoidance (half-life) | Core domain logic |
| `domain/attention/AttentionResult.kt` | **Created** — Output model: `AttentionResult`, `AttentionComponents`, `AttentionReason` sealed class | Shared domain contract |
| `domain/attention/TaskAttentionInput.kt` | **Created** — Minimal input projection; no priority, no isCompleted | Domain boundary isolation |
| `domain/graph/GoalGraphBuilder.kt` | **Modified** — Added `attentionScore: Float? = null` to `TaskInput` | Future orbit positioning (Phase 2C) |
| `plugins/planner/data/InsightDao.kt` | **Created** — `getLastMeaningfulInteractionPerTask` SQL query from `notes` table | Meaningful interaction source |
| `plugins/planner/data/InsightRepository.kt` | **Created** — Interface for `getLastMeaningfulInteractionPerTask` | Repository boundary |
| `plugins/planner/data/RoomInsightRepository.kt` | **Created** — Room-backed implementation | Data access |
| `plugins/goals/ui/GoalDetailViewModel.kt` | **Modified** — Added `loadMeaningfulInteractions()`, `computeAttention()`, `_attentionResults` StateFlow; fixed dead code bug where `computeAttention` was unreachable | ViewModel integration |
| `domain/attention/AttentionCalculatorTest.kt` | **Created** — 14 unit tests | Verification |

---

## Architecture Validation

### Dependency Isolation
- `AttentionCalculator` imports only `kotlin.math.{exp, abs, min}`
- Zero imports from `domain.goal`, `domain.snapshot`, `domain.insight`, `domain.mirror`, or any Android/Room types
- `TaskAttentionInput` has no `priority` or `isCompleted` field

### Completion Gate
- Filtering happens at the ViewModel layer in `GoalDetailViewModel.computeAttention` (line ~470) via `tasks.filter { !it.isCompleted }`
- Domain layer intentionally unaware of completion status

### Formula Verification
```
Score = DatePressure × 0.40 + Staleness × 0.35 + Avoidance × 0.25
Clamped to [0, 1]
```
All weights and curves match the Design Specification exactly.

### Data Flow
```
Graph sheet opens
  → loadRescheduleCounts()        (Phase 3.1, from DB)
  → loadMeaningfulInteractions()  (Phase 2A, from notes table)
  → computeAttention()            (pure computation, Dispatchers.Default)
  → _attentionResults.value = ...
  → graphSource(rescheduleCounts, _attentionResults.value).collect { ... }
```

---

## Tests Executed

All **14 AttentionCalculatorTest tests** pass (0 failures, 0 errors):

| Test | Verifies |
|---|---|
| `datePressureFavorsNearDeadlineOverFarDeadline` | Deadline proximity ordering |
| `staleOutranksRecentForSameDeadline` | Staleness (14+ days) > recent interaction |
| `farDeadlineAndNoInteractionGivesZeroScore` | No signal = 0 score |
| `futureSubtaskDoesNotCauseArtificialPenalty` | No non-existent signal leakage |
| `highAvoidanceDrivesAttention` | Avoidance signal ordering |
| `noDeadlineMeansZeroDatePressure` | Edge case: null deadline |
| `overdueTaskPressureExceedsOne` | Overdue penalty capped at MAX_PRESSURE |
| `stalenessUsesLastMeaningfulInteraction` | Quadratic decay shape |
| `avoidanceUsesRescheduleCountDiminishingReturns` | Half-life diminishing returns |
| `combinationsOfSignalsProduceCorrectScores` | Multi-signal composition |
| `overdueGeneratesOverdueReason` | Explainability: Overdue reason |
| `nearDeadlineGeneratesNearDeadlineReason` | Explainability: NearDeadline reason |
| `staleInteractionGeneratesStaleInteractionReason` | Explainability: StaleInteraction reason |
| `rescheduleGeneratesAvoidanceReason` | Explainability: Avoidance reason |

### Pre-existing unrelated failures (6)
`GoalDaoTest`, `GoalEventDaoTest`, `RoomGoalRepositoryTest`, `SnapshotAggregatorTest`, `SnapshotDaoTest`, `ExampleRobolectricTest` — not affected by Phase 2A.

---

## Known Deferred Items (Phase 2B)

1. **Live attention updates while graph is open** — Currently computed once on sheet open. Phase 2B should recompute when `allTasks` emits (task toggles, new notes).
2. **Meaningful interaction UNION ALL** — Currently only `notes` table. Extend to subtask events, photo timestamps, etc. when those features land.
3. **UI display of attention scores** — `_attentionResults` is exposed as a StateFlow but no UI reads it yet. Phase 2B should surface scores as badges, tooltips, or orbit proximity.
4. **Explainability formatting** — `AttentionReason` sealed class is defined but no localized string formatting exists yet.
5. **Attention-driven orbit positioning** — `attentionScore: Float?` is on `TaskInput` but unused. Phase 2C will replace `PRIORITY_SIZE` and `LANE_FRACTION` with attention-derived radius.

---

## Remaining Risks

1. **Single-layer completion gate** — Only at ViewModel layer. If a new caller forgets to filter `isCompleted`, completed tasks would receive attention scores. Mitigation: defense-in-depth check could be added to `AttentionCalculator.compute()` itself in Phase 2B.
2. **No reactive attention recompute** — Attention is a snapshot at sheet-open time. Tasks reordered while the sheet is open won't reflect new notes or reschedules until the sheet is closed and reopened.
3. **Meaningful interaction source limited** — `notes` table only. Reschedule events are correctly excluded (they feed Avoidance separately), but other interaction types (subtask completion, file attachment) are not counted.
4. **Attention on inactive tasks** — Tasks assigned to paused/archived goals are still computed if the graph sheet is opened for that goal. No checklist for checking goal status.

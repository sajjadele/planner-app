# Attention Architecture — Behavioral Solar System

## Overview

The Behavioral Solar System is an **Attention Map**, not a priority system or progress tracker.

It answers: *"What needs the user's attention again?"*

Two concepts are strictly separated:

| Concept | Answer | Belongs to |
|---|---|---|
| **Attention** | "Where should the user look?" | `domain/attention/` |
| **Progress** | "How far has the user moved?" | `domain/goal/`, `domain/snapshot/` |

Progress MUST NOT affect Attention. Attention MUST NOT affect Progress.

---

## Signal Definitions

Attention is computed from three independent behavioral signals:

### 1. Date Pressure

**Source:** `TaskEntity.deadlineEpochMs`

**Meaning:** How close is the task to its deadline?

**Curve:** Non-linear (logistic / sigmoid).

- Far deadlines → almost zero pressure.
- 3 days remaining → ≈ 0.5 (inflection point).
- Today → ≈ 0.99.
- Overdue → continues climbing (up to 2.0 max).

**Why logistic:** A task 30 days away and a task 14 days away should feel nearly identical (both "not urgent"). Pressure should spike only when the deadline is imminent. Linear curves fail this test.

### 2. Staleness

**Source:** `lastMeaningfulInteraction` (per task)

**Meaning:** How long since the user last interacted meaningfully with this task?

**Curve:** Quadratic decay.

- No interaction history → 0 (not penalized).
- 1-3 days → below 0.05.
- 7 days → 0.25.
- 14+ days → 1.0 (full staleness).

**Why quadratic:** A task untouched for 10 days feels much more stale than one untouched for 5 days. The decay accelerates.

**Critical rule:** If there is no interaction history, staleness = 0. The system does not punish tasks for being new or for having no notes yet.

### 3. Avoidance

**Source:** `TaskEventEntity` where `eventType = 'rescheduled'`

**Meaning:** How many times has the user postponed this task?

**Curve:** Diminishing returns (half-life model).

- 0 reschedules → 0.
- 1 → 0.50.
- 2 → 0.75.
- 3 → 0.875.
- 5 → 0.969.

**Why diminishing returns:** The difference between 1 and 2 reschedules is meaningful. The difference between 5 and 10 is not. Each additional reschedule adds less signal.

---

## Weighting

```
AttentionScore = DatePressure × 0.40 + Staleness × 0.35 + Avoidance × 0.25
```

Final score is clamped to [0, 1].

| Weight | Value | Justification |
|---|---|---|
| DatePressure | 0.40 | Deadlines are the most universal attention signal |
| Staleness | 0.35 | Long-term neglect is the second most important signal |
| Avoidance | 0.25 | Avoidance amplifies but should not dominate |

---

## Completion Gate

Completed tasks are **excluded** from attention calculation entirely.

- They receive no attention score.
- They are not part of the active Solar System ranking.
- They remain in the database for History/Memory visualization (future work).

The completion gate is applied upstream: `GoalDetailViewModel` filters `allTasks` by `!isCompleted` before passing to `AttentionCalculator`.

---

## Meaningful Interaction

### Definition

A "meaningful interaction" is any user action that indicates real movement on a task's path toward goal completion.

### Current Sources (Phase 2A)

| Source | Table | Timestamp field |
|---|---|---|
| Note/Log created | `notes` | `timestamp` |

### Future Sources (not yet implemented)

| Source | Expected table |
|---|---|
| Subtask completed | `subtask_events` |
| Photo attached | (future entity) |
| Voice note | (future entity) |

### Architecture Rule

The `AttentionCalculator` does NOT know the source of `lastMeaningfulInteraction`. It receives a single `Map<Int, Long>` (taskId → timestamp). The query that produces this map is the only place that decides which events count as "meaningful."

This makes adding future interaction sources a single-query change.

---

## Data Flow

```
InsightDao.getLastMeaningfulInteractionPerTask(goalId)
    ↓
InsightRepository.getLastMeaningfulInteractionPerTask(goalId)
    ↓
GoalDetailViewModel.loadMeaningfulInteractions()
    ↓
GoalDetailViewModel.computeAttention(tasks, rescheduleCounts, meaningfulInteractions, now)
    ↓
AttentionCalculator.compute(inputs, nowMillis)
    ↓
Map<Int, AttentionResult>
    ↓
VisibilityResolver.resolve(VisibilityInput)   ← Phase 2B
    ↓
VisibleGraphModel
    ↓
GoalGraphSheetContent (pure renderer)
```

---

## Visibility Resolver (Phase 2B)

Attention and Visibility are separate:

| Concept | Answer | Package |
|---|---|---|
| **Attention** | "Where should the user look?" (score) | `domain/attention/` |
| **Visibility** | "What should be drawn?" | `domain/graph/` |

### Responsibilities

`VisibilityResolver` (pure Kotlin):

- Overview selection: min 3 / max 7 highest AttentionScore
- Expanded: up to 20 individual tasks
- Insight: attention-band clusters when active count > 20
- Continuous orbit radius from score
- Completed tasks never enter the input set (upstream gate)

### Continuous orbit

```
radius = innerRadius + (outerRadius - innerRadius) * (1 - attentionScore)
```

Score 1.0 → closest to sun; score 0.0 → farthest.
Attention bands (HIGH / MEDIUM / LOW) are for clustering only, not fixed planet lanes.

### Progressive disclosure

| Level | Behavior |
|---|---|
| OVERVIEW | 3–7 highest-attention tasks |
| EXPANDED | Up to 20 individual tasks |
| INSIGHT | Clusters for large collections |

ViewModel owns `VisibilityLevel` and `expandedClusterId`.
Renderer consumes one `VisibleGraphModel` per resolve call.

### Models

| Type | Role |
|---|---|
| `VisibilityInput` | Active tasks + attention results + metadata + level |
| `VisibleGraphModel` | tasks, clusters, hiddenCount, orbitBands |
| `VisibleTask` | taskId, score, radius, angle, reasons, flags |
| `VisibleCluster` | band, memberIds, count, position |

See ADR-0014.

---

## Explainability

Each `AttentionResult` includes a list of `AttentionReason` objects:

| Reason | When generated |
|---|---|
| `NearDeadline(remainingDays)` | DatePressure > threshold, deadline in future |
| `Overdue(overdueDays)` | DatePressure > threshold, deadline in past |
| `StaleInteraction(daysSince)` | Staleness > threshold |
| `Avoidance(rescheduleCount)` | Avoidance > threshold |

The UI formats these into localized strings (e.g., "3 روز تا مهلت باقی است").

---

## Future Expansion Strategy

1. **Phase 2B — Visibility Resolver:** ✅ Done. Attention scores drive overview/expanded/insight visibility and continuous orbit radius.
2. **Phase 3 — Subtask integration:** Add `subtask_events` to the meaningful interaction query. No change to `AttentionCalculator`.
3. **Phase 4 — New interaction sources:** Add photo/voice entities. Union their timestamps into the meaningful interaction query. No change to `AttentionCalculator`.
4. **Phase 5 — Graph cleanup:** Drop legacy priority-lane path in `GoalGraphBuilder` once renderer no longer needs it for sun/progress scaffolding.

---

## Files

| File | Purpose |
|---|---|
| `domain/attention/TaskAttentionInput.kt` | Input data class |
| `domain/attention/AttentionResult.kt` | Output: score, components, reasons |
| `domain/attention/AttentionCalculator.kt` | Pure calculator (no Android deps) |
| `domain/graph/VisibilityModels.kt` | VisibleGraphModel / VisibleTask / VisibleCluster |
| `domain/graph/VisibilityResolver.kt` | Pure visibility decisions (no Android deps) |
| `InsightDao.kt` | `getLastMeaningfulInteractionPerTask` query |
| `InsightRepository.kt` | Repository boundary |
| `RoomInsightRepository.kt` | Room implementation |
| `GoalDetailViewModel.kt` | Wires attention + visibility into the graph sheet flow |
| `GoalGraphSheetContent.kt` | Pure renderer of VisibleGraphModel |

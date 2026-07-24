# Phase 2B Visibility Resolver — Architecture Plan

## 1. Current Architecture

### How GoalGraphBuilder Currently Decides Layout

| Aspect | Current Implementation |
|--------|------------------------|
| **Number of visible nodes** | All active tasks (filtered by `!isCompleted`). Density mode selected by active count: ≤6 → SIMPLE, ≤20 → CLUSTERED, >20 → SUMMARY |
| **Node positions** | Deterministic polar layout. Even angle distribution within lane. `jitter(id)` prevents radial alignment of lanes |
| **Orbit/ring assignment** | **Priority-based lanes**: HIGH (0.34·R), MEDIUM (0.58·R), LOW (0.82·R), undated/no-priority (0.92·R), completed (0.97·R) |
| **Density modes** | `GraphDensityMode` enum: `SIMPLE` (individual satellites), `CLUSTERED` (4 clusters with full expand), `SUMMARY` (4 clusters with L3-capped expand at 12) |

### Class Responsibilities

| Layer | Class | Responsibility |
|-------|-------|----------------|
| **Data Prep** | `GoalDetailViewModel` | Combines reactive sources (`goal`, `allTasks`, `goalProgress`, `rescheduleCounts`, `attentionResults`), maps to `TaskInput`, calls `GoalGraphBuilder.build()` |
| **Visibility Decisions** | `GoalGraphBuilder` (domain) | **ALL visibility/layout decisions**: density mode, lane assignment, clustering, ordering — computed in pure Kotlin |
| **Rendering** | `GoalGraphSheetContent` (UI) | Pure rendering of precomputed `GoalGraph`. Chooses what to draw based on `densityMode` and `expandedClusterId`. Expansion ordering uses `prRank(priority)` |

---

## 2. Current Support for Visibility Resolver

### Does a VisibilityResolver Exist?
**No.** All visibility logic lives in `GoalGraphBuilder.build()`.

### Where Should It Live?
**Domain layer** (`domain/attention/` or `domain/graph/`).

**Reasoning:**
- Current `GoalGraphBuilder` is pure Kotlin, host-JVM testable, no Android deps
- `AttentionCalculator` is also pure domain
- Visibility decisions (which tasks show at which zoom level, clustering) are domain logic, not UI
- Keeps UI dumb renderer (only draws what domain says)

### Input/Output Models

```kotlin
// Input: Attention results + active task metadata
data class VisibilityInput(
    val attentionResults: Map<Int, AttentionResult>,  // taskId → AttentionResult
    val activeTaskIds: Set<Int>,                       // already filtered !isCompleted
    val activeTaskCount: Int,
    val completedTaskIds: Set<Int>                     // for History layer
)

// Output: What the renderer draws at each zoom level
sealed interface VisibleGraphModel {
    data class Overview(
        val tasks: List<VisibleTask>,       // 3–7 tasks
        val attentionThreshold: Float       // min score in overview
    ) : VisibleGraphModel

    data class ExpandedOrbit(
        val tasks: List<VisibleTask>        // 5–15 tasks
    ) : VisibleGraphModel

    data class Clustered(
        val clusters: List<ClusterModel>,   // attention-band clusters
        val expandedCluster: ClusterModel?  // currently tapped
    ) : VisibleGraphModel

    data class Summary(
        val clusters: List<ClusterModel>,
        val expandedCluster: ClusterModel?
    ) : VisibleGraphModel
}

data class VisibleTask(
    val taskId: Int,
    val attentionScore: Float,
    val orbitRadius: Float,      // continuous: function of attentionScore
    val angle: Float,            // deterministic by id
    val reasons: List<AttentionReason>,
    val isBoulder: Boolean,
    val isOverdue: Boolean,
    val isNearDeadline: Boolean
)

data class ClusterModel(
    val id: Int,
    val label: String,           // "23 low-attention tasks"
    val memberIds: List<Int>,
    val orbitRadius: Float,      // band average
    val visualSize: Float,
    val colorRole: ColorRole
)

enum class VisibilityDensityMode { SIMPLE, EXPANDED, CLUSTERED, SUMMARY }
```

---

## 3. Overview Algorithm Design

### Options Comparison

| Option | Description | Pros | Cons |
|--------|-------------|------|------|
| **A) Top N** | Fixed N (e.g., 5) highest attentionScore | Simple, deterministic | Rigid; wastes slots if all scores ~0 |
| **B) Threshold** | Show all above `score > threshold` | Adapts to distribution | May show 0 or 20+ tasks |
| **C) Hybrid (Recommended)** | **Min 3, Max 7, adaptive by threshold** | Guarantees 3–7; respects signal | Slightly more complex |

**Hybrid Algorithm:**
```kotlin
fun adaptiveCount(sorted: List<Entry<Int, AttentionResult>>, total: Int): Int {
    if (total <= 7) return total
    val threshold = sorted[4].value.score // 5th task's score
    return sorted.count { it.value.score >= threshold }.coerceIn(3, 7)
}
```

---

## 4. Expanded Orbit Design

### Options Comparison

| Option | Description | Fits Architecture? |
|--------|-------------|-------------------|
| **A) Show all individually** | Every task as satellite | ❌ Breaks at 50+ |
| **B) Attention-based compression** | High attention = individual; low = clusters | ✅ Matches density mode pattern |
| **C) Summary nodes** | "23 low attention tasks" node | ✅ Already exists in `TaskClusterNode` |

**Recommended: B + C (Hybrid)**
- 10 tasks: All individual (SIMPLE)
- 20 tasks: Top 7 overview + 13 expanded (EXPANDED)
- 50 tasks: Top 5 overview + 10 expanded + 35 in 3 clusters (CLUSTERED)
- 200 tasks: Top 4 overview + 8 expanded + 188 in 3 clusters (SUMMARY)

Density mode thresholds:
```kotlin
when {
    activeCount <= 7 -> SIMPLE
    activeCount <= 20 -> EXPANDED
    activeCount <= 80 -> CLUSTERED
    else -> SUMMARY
}
```

---

## 5. Clustering Without Categories

### Current Clusters (Priority-Based)
```kotlin
ClusterSpec(ACTIVE_HIGH, "HIGH", 0f, 0.34f)
ClusterSpec(ACTIVE_MEDIUM, "MEDIUM", 2π/3, 0.58f)
ClusterSpec(ACTIVE_LOW, "LOW", 4π/3, 0.82f)
ClusterSpec(COMPLETED, null, π, 0.95f)
```

### Attention-Based Clusters (Replace Priority Lanes)

```kotlin
enum class AttentionBand(val range: ClosedRange<Float>, val label: String, val radius: Float, val color: ColorRole) {
    HIGH(0.5f..1.0f, "High Attention", 0.35f, ColorRole.HIGH),      // inner
    MEDIUM(0.2f..0.5f, "Medium Attention", 0.55f, ColorRole.MEDIUM), // middle
    LOW(0.0f..0.2f, "Low Attention", 0.80f, ColorRole.LOW)          // outer
}
```

**Cluster creation:**
```kotlin
fun createAttentionClusters(remainder: List<Entry<Int, AttentionResult>>): List<ClusterModel> {
    return AttentionBand.values().mapNotNull { band ->
        val members = remainder.filter { band.range.contains(it.value.score) }
        if (members.isEmpty()) return@mapNotNull null
        ClusterModel(
            id = CLUSTER_ID_BASE + band.ordinal,
            label = "${members.size} ${band.label.toLowerCase()} tasks",
            memberIds = members.map { it.key },
            orbitRadius = band.radius,
            visualSize = calculateClusterSize(members.size),
            colorRole = band.color
        )
    }
}
```

**MVP Approach:** Replace priority-based clustering with attention-band clustering. Minimal code change — same `ClusterModel` structure, different grouping logic.

---

## 6. Orbit Position Calculation

### Options Comparison

| Option | Description | Recommendation |
|--------|-------------|----------------|
| **A) Discrete rings** | 3 fixed radii (inner/middle/outer) | ❌ Loses attention nuance |
| **B) Continuous** | `radius = inner + (outer - inner) * (1 - score)` | ✅ Best signal fidelity |
| **C) Hybrid** | Continuous position + 3 visual orbit bands | ✅ **Recommended** |

**Hybrid Implementation:**
```kotlin
fun orbitRadius(score: Float): Float {
    // score 1.0 → 0.30·R (inner), 0.0 → 0.85·R (outer)
    return 0.30f + (0.85f - 0.30f) * (1f - score)
}

// UI draws 3 faint orbit bands at 0.35, 0.55, 0.80 for reference
val ORBIT_BANDS = listOf(0.35f, 0.55f, 0.80f)
```

---

## 7. Completed / History Layer

### Current Architecture
- Completed tasks filtered out in `GoalDetailViewModel.computeAttention()` (line 430)
- `GoalGraphBuilder` places completed on outer ring (0.97·R) as faded memory points
- No History access UI exists

### Proposed Architecture
```kotlin
// VisibilityInput adds completedTaskIds
data class VisibilityInput(
    // ... existing ...
    val completedTaskIds: Set<Int>
)

// VisibleGraphModel adds History
data class HistoryModel(
    val completedTaskIds: List<Int>,  // sorted by completion date desc
    val totalCompleted: Int
)

// UI: Separate "History" sheet or toggle on graph sheet
// Graph sheet shows only active (overview + expanded + clusters)
// History sheet reads `HistoryModel` and renders completed as memory ring
```

**Where completed removed:** `VisibilityResolver.resolve()` — never passed to `VisibleGraphModel.overview/expanded/clusters`

---

## 8. Required Code Changes

| File | Change | Risk | Dependency Impact |
|------|--------|------|-------------------|
| `domain/attention/VisibilityResolver.kt` | **NEW** — Core resolver logic | Medium | New domain module; consumes `AttentionResult` |
| `domain/attention/VisibilityModels.kt` | **NEW** — Input/Output data classes | Low | Pure data; no logic |
| `domain/graph/GoalGraphBuilder.kt` | **REFACTOR** — Remove clustering, lane assignment, ordering; keep geometry utilities (`jitter`, `GraphGeometry`, `placeLane` for single-lane use) | High | Output shape changes (`GoalGraph` → `VisibleGraphModel`); renderer must update |
| `domain/graph/GoalGraphModels.kt` | **MODIFY** — Replace `GoalGraph` with `VisibleGraphModel`; remove `priority`, `PRIORITY_SIZE`, `LANE_FRACTION`, `PRIORITY_RANK`, `ClusterType.ACTIVE_*`, `GraphDensityMode` | High | Breaking change for renderer & ViewModel |
| `plugins/goals/ui/GoalDetailViewModel.kt` | **MODIFY** — Call `VisibilityResolver.resolve()` after `computeAttention()`; pass result to `graphSource`; expose `attentionResults` StateFlow | Medium | Flow output type changes |
| `plugins/goals/ui/GoalGraphSheetContent.kt` | **REFACTOR** — Render `VisibleGraphModel`; remove `prRank`, `ColorRole.HIGH/MEDIUM/LOW` mappings; use attention-score-based radius; draw orbit bands; cluster expand uses `memberIds` | High | Complete renderer rewrite for layout logic |
| `domain/attention/AttentionCalculatorTest.kt` | **EXTEND** — Add `VisibilityResolverTest.kt` (14+ tests) | Low | New test file |

---

## 9. Documentation Updates

| Document | Update |
|----------|--------|
| `ATTENTION_ARCHITECTURE.md` | Add § "Visibility Resolver" with data flow diagram; document adaptive overview/expanded/clustering algorithms |
| **New ADR** | `ADR-0014-visibility-resolver.md` — Design decisions: attention-band clustering, continuous radius, hybrid overview, density modes |
| `ADR-0004-graph-solar-system.md` | Mark priority lanes & priority-based clustering as superseded; link to ADR-0014 |
| `ADR-0007-adaptive-solar-system.md` | Mark as superseded; density mode thresholds updated |
| `ROADMAP.md` | Check off Phase 2B; add Phase 2C (History Layer) |

---

## 10. Testing Strategy

### New Unit Tests (`VisibilityResolverTest.kt`)
| Test | Scenario |
|------|----------|
| `overviewAdaptiveCount_min3` | 2 tasks → shows 2; 5 tasks → shows 5; 20 tasks with spread → 5 |
| `overviewAdaptiveCount_max7` | 100 tasks, all high attention → shows 7 |
| `overviewThreshold` | Tasks below threshold excluded even if <7 total |
| `expandedOrbitCount_10` | 10 total → 3 overview + 7 expanded |
| `expandedOrbitCount_50` | 50 total → 5 overview + 10 expanded |
| `clusterCreation_bands` | Remainder split into 3 attention bands |
| `clusterEmptyBand_skipped` | No members in LOW band → no LOW cluster |
| `densityModeThresholds` | Verify 7/20/80 boundaries |
| `orbitRadius_continuous` | Score 1.0 → ~0.30; 0.5 → ~0.575; 0.0 → ~0.85 |
| `completedExcluded` | Completed tasks never in overview/expanded/clusters |

### Regression Tests
- All 9 existing `GoalGraphBuilderTest` tests migrate to `VisibilityResolverTest` + geometry tests
- Screenshot tests for 4 density modes (SIMPLE/EXPANDED/CLUSTERED/SUMMARY)

---

## 11. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| `GoalGraphBuilder` rewrite breaks determinism | Medium | High | Keep `GraphGeometry`, `jitter`, deterministic angle logic; add determinism regression test |
| Renderer migration misses edge cases | Medium | Medium | Screenshot tests for all 4 modes; manual QA on 10/50/200 task goals |
| `AttentionScore` null for some tasks | Low | Medium | Default to 0.0 in resolver; log warning |
| L3 expansion ordering (was priority, now attention) | Medium | Low | Unit test `VisibleTask` ordering explicitly |
| Completed tasks history UX undefined | Low | Low | Defer to Phase 2C; `HistoryModel` is data-only now |

---

## 12. Open Questions for Review

1. **History access pattern** — Separate sheet vs inline toggle? (Deferred but affects `VisibleGraphModel.history` shape)
2. **Orbit band count** — 3 bands (inner/middle/outer) or 4 (matching old priority lanes)? Recommend **3** to match attention bands.
3. **Transition animation** — Overview ↔ Expanded radius changes should animate. Scope for 2B or 2C?
4. **Minimum overview threshold** — If all scores < 0.1, show top 3 anyway or empty? **Recommend: always show top 3**.
5. **Boulder visual emphasis** — Currently by rescheduleCount ≥ 2. High-attention tasks should also get visual weight? **Yes — Boulder = avoidance signal, already in AttentionScore via Avoidance component**.

---

## 13. Implementation Order

1. **Create** `VisibilityResolver.kt`, `VisibilityModels.kt` in `domain/attention/`
2. **Create** `VisibilityResolverTest.kt` with 14 tests
3. **Refactor** `GoalGraphBuilder` → remove clustering/lane logic; keep geometry utils
4. **Modify** `GoalGraphModels.kt` → `VisibleGraphModel` replaces `GoalGraph`
5. **Update** `GoalDetailViewModel` → call resolver, expose `visibleGraphModel`
6. **Refactor** `GoalGraphSheetContent` → render `VisibleGraphModel`
7. **Update** documentation (ADR-0014, ATTENTION_ARCHITECTURE.md, ROADMAP.md)
8. **Screenshot tests** for 4 density modes

---

*Phase 2B Architecture Plan — Ready for Review*
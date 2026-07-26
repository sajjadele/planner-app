# ADR-0014 — Visibility Resolver (Attention-Driven Graph)

- **Status:** Accepted
- **Date:** 2026-07-22
- **Phase:** 2B (Visibility Resolver)
- **Extends:** ADR-0004, ADR-0005, ADR-0007, ADR-0011
- **Depends on:** Phase 2A Attention Foundation (`domain.attention`)

## Context

Phase 2A delivered a pure-domain Attention Score (DatePressure + Staleness + Avoidance).
The Solar System still used **priority lanes** for orbit position, density, and clustering.
That made the graph a priority dashboard, not an attention map.

Product decision: transform the graph into an **attention-driven adaptive visualization**
where AttentionScore controls gravity, visibility adapts to task count, and large
collections never create visual overload.

## Decision

### 1. Attention and Visibility are separate

| Concept | Responsibility | Package |
|---------|----------------|---------|
| AttentionCalculator | WHY a task needs attention | `domain.attention` |
| VisibilityResolver | WHAT should be visible | `domain.graph` |

Do not merge these responsibilities.

### 2. VisibilityResolver lives in `domain.graph`

Visibility is a graph presentation decision, not a behavioral signal calculation.
Expected structure:

```
domain/graph/
  GoalGraphBuilder      — geometry / deterministic placement (incremental)
  VisibilityResolver    — overview selection, clustering, continuous orbit
  VisibilityModels      — VisibleGraphModel, VisibleTask, VisibleCluster
  GraphGeometry         — pure polar math
```

### 3. Why priority lanes were removed

Priority is a user-assigned label. It does not answer “where should the user look?”
AttentionScore is derived from behavioral signals (deadline pressure, staleness, avoidance).
Using priority for orbit radius and cluster membership recreated a static priority dashboard
and ignored Phase 2A’s attention model.

### 4. Why attention controls gravity

Higher AttentionScore → closer to the sun (continuous radius):

```
radius = innerRadius + (outerRadius - innerRadius) * (1 - attentionScore)
```

Bands (HIGH 0.5–1.0, MEDIUM 0.2–0.5, LOW 0.0–0.2) are **aggregation only**, not fixed orbits.

### 5. Why clustering is attention-based

Priority clusters (ACTIVE_HIGH/MEDIUM/LOW) mixed importance labels with presentation.
Attention-band clusters group tasks that need similar attention so 200 tasks never become
200 planets. MVP threshold: ≤20 individual; >20 enable clusters (`CLUSTER_THRESHOLD`).

### 6. Progressive disclosure (3 levels)

| Level | Behavior |
|-------|----------|
| OVERVIEW | Min 3 / max 7 highest-attention tasks |
| EXPANDED | Up to 20 individual tasks |
| INSIGHT | Clusters when count > 20; top tasks still individual |

ViewModel owns the current level and `expandedClusterId`.
VisibilityResolver returns **one** `VisibleGraphModel` for the requested level.

### 7. Completed tasks

Excluded from visibility input (completion gate upstream). History/memory layer is out of 2B scope.

### 8. Renderer contract

`GoalGraphSheetContent` renders `VisibleGraphModel` + goal title/progress only.
No priority-based layout decisions in Compose.

## Consequences

### Benefits

- Attention Map product model is enforceable in domain code
- Large goals stay readable
- JVM-testable visibility policy
- Incremental: GoalGraphBuilder geometry retained during migration

### Trade-offs

- Renderer and ViewModel API changed (`GraphState.Ready` shape)
- Legacy `GoalGraph` still built for sun/progress compatibility during migration
- Cluster expansion for members not in the individual set uses placeholder placement

### Out of scope

- History layer UI for completed tasks
- Live recompute of attention while sheet is open (still snapshot at open + reactive task flow)
- Nested sheets / map-style zoom

## References

- `docs/ATTENTION_ARCHITECTURE.md`
- `app/src/main/java/com/example/domain/graph/VisibilityResolver.kt`
- `app/src/test/java/com/example/domain/graph/VisibilityResolverTest.kt`

# ADR-0003: Progress & Behavior Snapshots

- **Status:** Accepted
- **Date:** 2026-07-13
- **Updated:** 2026-07-16
- **Phase:** Behavior Data Foundation

## Context

Progress and behavior were recomputed on demand from raw events. Future consumers (Mirror, Graph, AI) need a shared historical projection without turning projections into a second authoritative source.

## Decision

Introduce rebuildable projection tables:
- `goal_progress_snapshot` (per goal, per day)
- `behavior_snapshot` (per day rollup)

Refresh strategy:
- real-time upsert for today
- app-launch backfill for historical gaps
- no WorkManager

Snapshots remain projections of events and are also inputs for Mirror analysis.

## Consequences

Positive:
- shared read model for trends
- cheaper historical queries
- clean separation between events (truth) and projections

Trade-offs:
- must keep rebuild path healthy
- consumers must never treat snapshots as authoritative writes

## References

- `docs/ARCHITECTURE_STATE.md`
- `docs/ROADMAP.md` (Behavior Data Foundation → Mirror Engine Foundation)
- `PRODUCT_DIRECTION_DECISION_DOCUMENT.md`

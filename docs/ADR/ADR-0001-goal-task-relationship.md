# ADR-0001: Goal–Task Relationship Represented Only by `goalId`

- **Status:** Accepted
- **Date:** 2026-07-13
- **Phase:** Foundation

## Context

`TaskEntity` previously stored the Goal relationship twice:
1. `goalId` foreign key
2. denormalized `goalName` cache

This created two answers for the same relationship and caused drift after renames/deletes.

## Decision

Represent Goal→Task exclusively through `goalId`.

- remove `goalName`
- resolve titles by join/lookup
- keep events and projections aligned to the FK relationship

## Consequences

Positive:
- single source of truth
- no stale goal titles
- safer future Graph/Mirror analysis

Trade-offs:
- title resolution needs join/lookup
- deleted goals appear as unlinked tasks (`SET NULL`)

## References

- `docs/ARCHITECTURE_STATE.md`
- `docs/ROADMAP.md` (Phase 1)

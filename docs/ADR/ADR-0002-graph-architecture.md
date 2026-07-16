# ADR-0002: Graph Domain Model Scope

- **Status:** Accepted (design only; implementation deferred)
- **Date:** 2026-07-13
- **Updated:** 2026-07-16
- **Phase:** Designed earlier; implementation planned as Graph Exploration phase

## Context

Relationships exist mainly as foreign keys. Lists alone do not give a structural overview of Goal→Task connections.

Earlier drafts considered Life Area as a graph node. Current product direction keeps architecture simpler.

## Decision

- Graph is a pure domain model
- Graph is computed on demand and never stored
- Current scope is Goal→Task only
- Life Area is not a graph node in current direction
- UI renders graph data and does not query DAOs directly

## Consequences

Positive:
- no second source of truth
- minimal schema pressure
- simple, meaningful visualization path

Trade-offs:
- limited expressive power until/unless Life Area promotion is reopened
- assembly cost on demand when graph screen is shown

## References

- `PRODUCT_DIRECTION_DECISION_DOCUMENT.md`
- `docs/ROADMAP.md` (Graph Exploration)
- `docs/ARCHITECTURE_STATE.md`

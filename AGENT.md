# AGENT.md — AI Development Rules

This file defines constraints for AI-assisted work on Vision Planner.

## Authority Hierarchy

1. `PRODUCT_DIRECTION_DECISION_DOCUMENT.md` — product philosophy and boundaries
2. `docs/ARCHITECTURE_STATE.md` — current technical state
3. `docs/ROADMAP.md` — phase priorities
4. `docs/ADR/*` — why decisions were made
5. `CURRENT_IMPLEMENTATION_CONTEXT.md` — short session bootstrap
6. This file — operational rules for agents

If documents conflict, higher items win.

## Product Identity

Vision Planner is a **goal-oriented progress system**, not a simple todo app.

Primary value:
- help users understand progress
- detect behavioral patterns
- provide non-judgmental feedback
- help users redesign their path toward goals

## Non-Negotiable Rules

1. **Goal-first**
   - Goal is the primary entity
   - Task is an execution unit under a goal (optional link allowed)

2. **Low friction**
   - Tasks without goals are allowed (Inbox/capture)
   - Do not force complex planning

3. **Feedback, not judgment**
   - Never blame the user
   - Never create guilt
   - Prefer calm, observational language

4. **Invisible data collection**
   - No forced reflection
   - No mandatory journaling
   - Derive insights from existing events/snapshots

5. **Offline-first**
   - No network dependency
   - No remote AI as a foundation

6. **Event-based truth**
   - Events are source of truth
   - Snapshots are rebuildable projections only

## Technical Constraints

- Kotlin + Compose + Room + MVVM + Flow
- Keep domain logic pure Kotlin and host-JVM testable
- Do not introduce WorkManager for analytics
- Do not store Graph relationships
- Do not promote Life Area to first-class entity unless a new product decision is approved
- Prefer Room Flow reactive UI over manual refresh/event buses

## Current Development Focus

Mirror Engine Foundation: **COMPLETE** (implemented in `domain.mirror` + `core.mirror`, shown in Goal Detail via `MirrorFeedbackCard`).

Mirror V1:
- Boulder
- Initiator vs Finisher
- Goal Attention
- Consistency Decay
- Appears inside Goal Detail (no separate Mirror screen)
- No forced reflection / journaling

Next priorities:
- ViewModel tests (not started)
- UX refinement of feedback card
- Feedback wording validation
- Graph remains later exploration (Phase 6)

## Working Style

- Explain WHY/HOW before large changes when asked
- Prefer incremental implementation
- Comment-out for rollback when replacing risky logic (user preference)
- Do not invent APIs/files; inspect the codebase first
- Do not commit unless explicitly asked
- Keep docs and code assumptions synchronized

## Documentation Rules

- Product philosophy goes in `PRODUCT_DIRECTION_DECISION_DOCUMENT.md`
- Current technical state goes in `docs/ARCHITECTURE_STATE.md`
- Future phases go in `docs/ROADMAP.md`
- Historical decisions go in `docs/ADR/`
- Archive outdated docs under `docs/archive/` (do not delete)

## Forbidden Shortcuts

- Do not rewrite product direction during implementation tasks
- Do not skip ADRs when changing fundamental architecture
- Do not add AI/network features "because they are useful"
- Do not treat task completion stats as the product goal

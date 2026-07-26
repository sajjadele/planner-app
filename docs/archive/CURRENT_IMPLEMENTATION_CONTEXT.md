# Current Implementation Context

> Short context for AI agents. Keep this under ~100 lines.
> Authority: `PRODUCT_DIRECTION_DECISION_DOCUMENT.md` > `docs/ARCHITECTURE_STATE.md` > `docs/ROADMAP.md` > ADRs > `AGENT.md`

## Project
Vision Planner — goal-oriented progress system (not a todo app).

## Current Phase
- Completed: Phase 1–3
- Next focus: **Phase 4 — Mirror Engine Foundation**

## Tech Stack
- Kotlin
- Jetpack Compose + Material3
- Room
- Coroutines + Flow
- MVVM
- Offline-first (no network dependency)

## Architecture Rules
- Goal is the primary entity
- Task is the execution unit and may optionally belong to a Goal
- Events (`task_events`, `goal_events`) are the source of truth
- Snapshots are rebuildable projections, not authoritative writes
- Domain logic is pure Kotlin (`domain.insight`, `domain.snapshot`, planned `domain.mirror`)
- UI stays dumb; math stays out of ViewModels
- No WorkManager for analytics
- No AI dependency in V1

## Product Rules
- Feedback, not judgment
- Invisible data collection
- No forced reflection/journaling
- Tasks without goals are allowed as Inbox/capture
- Life Area is metadata only (not first-class entity / not graph node)
- Graph is future and simple: Goal→Task, computed on demand

## Current Data Surfaces
- Tables: goals, goal_events, tasks, task_events, notes, module_settings, goal_progress_snapshot, behavior_snapshot
- DB version: 9
- Snapshot refresh: real-time today + app-launch backfill

## Mirror V1 Scope
- Patterns: Boulder, Initiator/Finisher, Goal Attention, Consistency Decay
- Placement: inside Goal Dashboard
- No separate Mirror screen yet
- Meaningful analysis after roughly 7 days of usage

## Do Not Do
- Do not reintroduce dual sources of truth
- Do not promote Life Area now
- Do not add remote AI / network sync
- Do not force user input for Mirror
- Do not invent implementation details not present in docs/code

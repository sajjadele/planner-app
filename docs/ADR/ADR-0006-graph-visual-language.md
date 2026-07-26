# ADR-0006 — Graph Visual Language (Behavioral Solar System)

- **Status:** Accepted
- **Date:** 2026-07-17
- **Phase:** 6.2 (Visual Language Foundation)
- **Extends:** ADR-0004 (solar-system design), ADR-0005 (goal-centered model)

## Context

Phase 6.1 delivered a technically correct Behavioral Solar System: a deterministic, pure-Kotlin
domain graph rendered on a Compose Canvas. But the result read as a schematic, not a product
experience — the Goal was a plain dot, lanes were uniform, and there was no immediate sense of
*how this goal is doing*.

The purpose of Phase 6.2 is **not** to add graph features, but to transform the existing
visualization into a clear, understandable product experience through a consistent visual
language. ADR-0004 principles are preserved: Goal-centered, no global graph, no stored graph
data, pure-Kotlin domain, Compose-only rendering.

## Decision

### Goal as Sun (dominant, informative)
- The central Goal node is visually dominant (largest element) and carries **two pieces of live
  information computed by the domain/ViewModel**: the goal title and the `GoalProgress.overall`
  percentage.
- A **progress ring** (arc) is drawn around the sun, filled proportionally to `overall` using the
  app's progress accent (`AccentCyan`). A faint full-track shows the remaining gap.
- The **Ring Tide** halo (from 6.1) remains: its intensity grows with `overall`, so a higher-progress
  goal literally glows stronger. No fake values — everything derives from `goalProgressOverall`.
- Sun color is **`AccentGold`** (warm gold — the solar-system identity; see `ui/theme/Color.kt:32`).
  The app's dark-theme `primary` is `AccentCyan`, used here as the *progress* tint, avoiding a
  cyan-on-cyan sun. (Corrected in 6.5.1: an earlier draft said `AccentPurple`; the implementation has
  always used `AccentGold`.)

### Priority as orbital distance + visual weight
- Layout is **unchanged** (deterministic, from `GoalGraphBuilder`): HIGH inner / MEDIUM middle /
  LOW outer.
- Visual weight now reinforces priority: orbit rings are weighted — HIGH brightest+thickest,
  MEDIUM medium, LOW faintest. HIGH task nodes gain a soft outer glow for stronger presence;
  LOW nodes read quieter.

### Completion as memory
- Completed tasks remain faded, small, on the outer edge ring — "مسیرهای طی شده" (paths traveled),
  not active work. Philosophy unchanged from 6.1.

### Color system (roles, UI-mapped)
The domain exposes `ColorRole` only; the UI maps roles → actual colors. Stabilized mapping:

| Role | Color | Meaning |
|------|-------|---------|
| GOAL | `AccentGold` | Center / identity (sun) |
| HIGH | `AccentRed` | Attention |
| MEDIUM | `AccentFire` | Secondary emphasis |
| LOW | `AccentGreen` | Neutral / calm |
| COMPLETED | `onSurfaceVariant` | Muted memory |
| BOULDER | `AccentRed` | Warning (rescheduled ≥ 2) |

The domain never references Android/`android.graphics.Color` — purity preserved.

### No physics / no random movement
- Animation is minimal and one-shot on open: an entrance fade (sun → rings → nodes) via an
  `Animatable`. A subtle, deterministic Ring Tide breathing pulse and the deterministic Boulder
  wobble remain, but there is **no continuous orbit motion, no physics simulation, no random
  motion**. Product philosophy is focus, not gamification.

### Header & Help/Legend
- Header subtitle communicates "وضعیت هدف در یک نگاه" (goal state at a glance). The mandatory Help
  icon stays.
- Legend copy updated to the agreed metaphor (🔥 High Priority, ● Active Task, ○ Completed Task,
  ☀ Goal) with the temporal/deadline concept shown as a greyed **future** item:
  "نمایش اهمیت زمانی در نسخه آینده اضافه خواهد شد" — no misleading deadline visualization.

## Consequences

### Benefits
- The graph now answers "how is this goal doing?" at a glance (sun glow + progress ring + %).
- Consistent, role-based color language is readable and theme-independent at the domain layer.
- Calm, focused aesthetic aligned with the product's non-gamified tone.
- Zero architecture change — display-only; all behavior preserved and test-backed.

### Trade-offs
- Sun uses `AccentGold` (warm gold) rather than the strict theme `primary` (`AccentCyan`); a conscious
  choice documented here to keep the sun distinct from the cyan progress tint and to read as a "sun".
- Tap-to-highlight + Persian label on a satellite is retained as a passive visual aid (no detail
  card/popup), per 6.2 decision — a minor interaction beyond pure "appearance", but already shipped
  in 6.1 and kept intentionally.

### Future interaction phases (explicitly out of 6.2 scope)
- Task detail card / popup, deadline-aware radius, drift/decay, AI interpretation, global graph,
  new DB fields/migrations. These belong to later phases.

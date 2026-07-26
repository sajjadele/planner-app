# ADR-0008 — Solar System Motion & Animation

- **Status:** Accepted
- **Date:** 2026-07-17
- **Phase:** 6.4 (Motion & Animation)
- **Extends:** ADR-0004/0005/0006/0007 (Behavioral Solar System)

## Context

Phases 6.1–6.3 built a correct, readable, adaptive Solar System but with only a single flat fade-in
and a subtle Ring Tide pulse. The product wants the view to feel *alive* and to guide attention —
without becoming a gamified, distracting toy. This ADR defines the motion language.

## Decision

### Principles
- **Attention, not distraction.** All motion is low-amplitude, slow (2–4s cycles), deterministic.
- **No physics. No random motion. No orbital revolution.** Satellites never change position over
  time — this honors ADR-0006's "no continuous orbit movement" rule. "Gentle satellite motion"
  means a *breathing shimmer* (scale + alpha), never travel.
- **Determinism.** Every animated phase is derived from `time` (a shared clock) plus a per-node
  `id`-based phase, exactly like the existing Boulder wobble. Same graph → same motion, always.

### Motion elements delivered
1. **ورود منظومه (staged entrance).** Three one-shot `Animatable`s cascade on open:
   sun (0–280ms) → orbit rings (140–440ms) → satellites/clusters (300–620ms), using
   `FastOutSlowInEasing`. The system "assembles" calmly instead of popping in.
2. **حرکت آرام ماهواره‌ها (gentle breathing shimmer).** Each non-completed satellite oscillates
   subtly in scale (±3%) and alpha (±0.06) on a 4s sine, phase-offset by `node.id`. Completed
   "memory" nodes shimmer far less (±1.5% scale, no alpha shimmer) so the past stays quiet.
3. **Pulse خورشید (sun breathing).** The Ring Tide halo pulse amplitude is raised slightly
   (0.04→0.05) and remains tied to `goalProgressOverall`: higher progress = stronger, larger glow.
4. **تغییر حالت Cluster → Satellite (calm expand/collapse).** A single `expandProgress`
   `Animatable` (0→1) drives cluster expansion: member satellites fade + scale in; non-expanded
   clusters dim — all via `tween(320ms, FastOutSlowInEasing)`, no spring, no bounce.
5. **Motion for attention.** The only "alert" cue remains the deterministic Boulder wobble
   (rescheduled ≥ 2). No color flashing, no bouncing.

### What is explicitly forbidden (kept from ADR-0006)
- Continuous orbital movement / planets circling the sun.
- Physics simulation, gravity, springs, bouncing.
- Random or non-deterministic motion.
- Task detail popups / navigation triggered by motion.

## Consequences

### Benefits
- The graph reads as a living system; attention is drawn to the sun (progress) and to Boulders.
- Large goals stay calm: clusters shimmer gently, expansion is smooth.
- Zero architecture change; display-only. Existing 6.1/6.2/6.3 tests unaffected.

### Trade-offs
- Slightly more animation state in the composable (3 entrance + 1 expand `Animatable`).
- "Gentle motion" is a subjective calibration; amplitudes are conservative and easy to tune.

### Future
- Phase 7 (AI) may add attention cues, but must stay within this motion language.
- V2 deadline/drift visuals would reuse the same deterministic-phase approach.

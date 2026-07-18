# ADR-0010 — Gesture Tab Navigation & Graph First-Time Education

- **Status:** Accepted
- **Date:** 2026-07-17
- **Phase:** 5.5 (UX Polish & Interaction Improvements)
- **Relates:** ADR-0008 (Solar System motion), ADR-0009 (Performance)

## Context

Phase 5.5 improves interaction feel without changing product behavior, DB, or Graph architecture.

Two interaction gaps were identified from device testing:
1. **No swipe between main tabs.** Tab switching was click-only via `VisionBottomBar`; users expected
   horizontal swipe between Planner and Goals.
2. **No first-time Graph education.** The Behavioral Solar System has a Help (`Info`) button, but a
   first-time user can open it and miss the explanation. A one-time auto-show was wanted.

A smoothness audit (Part 3) also found the Graph sheet's breathing pulse overlapped the sheet
slide-in + staged entrance, reading as a busy double-animation.

## Decision

### Part 1 — Swipe via lightweight gesture, NOT HorizontalPager
`MainScreen` switches tabs with a plain `mutableStateOf("planner")` + `AnimatedContent` crossfade
(`MainScreen.kt:137-166`); `VisionBottomBar` is the source of truth. There is **no** `HorizontalPager`
anywhere and the ViewModels are activity-scoped (survive tab switches). Introducing `HorizontalPager`
would require reconciling pager position ↔ `selectedTabId` ↔ bottom-bar selection and would fight the
existing crossfade.

**Chosen approach:** a `pointerInput { detectHorizontalDragGestures }` handler on the tab-content
`Modifier` that **only writes `selectedTabId`** (never owns tab state). Direction is computed from the
accumulated horizontal drag (`> 60px` threshold) and flipped by `LocalLayoutDirection` so it is correct
in RTL (Persian). A `isTabTransitioning` guard (cleared ~350ms after `selectedTabId` changes) prevents
double-switches during the crossfade. Bottom navigation and swipe share one state → always synchronized.
`Notes` is explicitly excluded (top-bar only, no bottom-bar ordinal).

### Part 2 — First-time Graph education via DataStore boolean, VM-gated
A new `GraphViewPreferences` (`com.example.plugins.goals.GraphViewPreferences`) mirrors the established
`OnboardingRepository` DataStore pattern: `preferencesDataStore(name = "graph_settings")`,
`booleanPreferencesKey("graph_introduction_seen")`, `hasSeenIntroduction: Flow<Boolean>`,
`suspend markIntroductionSeen()`.

`GoalDetailViewModel` exposes `showGraphEducation: StateFlow<Boolean> = combine(showGraphSheet,
hasSeenIntroduction) { open, seen -> open && !seen }`. On first Graph open, `GoalGraphSheetContent`
receives `autoShowEducation = showGraphEducation` and auto-shows its **existing in-sheet legend
`AlertDialog`** (no new dialog chrome). When that legend is dismissed while it was the education, it
calls `onEducationDismissed → viewModel.markGraphIntroductionSeen()`. The `Info` button still opens the
same legend manually; after the first dismiss the flag is set so it never auto-shows again. The gate
lives in the ViewModel, so it survives tab teardown/recreation (composable `remember` would not). The
legend copy was updated to the required Persian text (☀ هدف / 🔥 اولویت بالا / ● تسک فعال / ○ انجام شده)
and the future-feature row was removed per spec ("do not mention future features").

### Part 3 — Breathing pulse delayed until entrance completes
The 4s Ring Tide breathing pulse (`rememberInfiniteTransition`) now starts only after the staged
entrance finishes (~640ms), via an `entranceDone` flag. Before that, `t = 0` (static render, no 60fps
redraw). The animation is **kept** — only its start is deferred so the sheet slide-in + assemble do not
overlap the breathing. No animations removed; visual language preserved.

## Consequences

### Benefits
- Planner ↔ Goals swipe works and stays perfectly in sync with the bottom bar.
- First-time Graph users get the explanation automatically; it never nags again; manual Help intact.
- Graph open no longer shows overlapping entrance + breathing animations.
- No new navigation architecture; ViewModels untouched in scope/lifecycle.

### Trade-offs
- Swipe is limited to the two bottom tabs (by design — Notes is top-bar only).
- A 60px drag threshold means very small horizontal scrolls don't switch tabs (intended).
- `isTabTransitioning` adds a ~350ms guard window after each switch (prevents rapid double-switch).

### Future
- If a richer pager experience is ever wanted, revisit HorizontalPager + bottom-bar reconciliation.
- ADR-0008 motion language unchanged.

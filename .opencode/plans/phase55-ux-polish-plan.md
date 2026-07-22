# Phase 5.5 — UX Polish & Interaction Improvements (Implementation Plan)

> **Scope guard:** UX polish only. No DB redesign, no product-behavior change, no Graph architecture
> change, no features outside this scope. Keep current visual language; improve perceived smoothness.

Read-only audit completed (sub-agent `explore` + direct file reads). Verified facts used below:
- `MainScreen.kt:90` `selectedTabId` is `mutableStateOf("planner")` in composition; tab switch is a
  plain `AnimatedContent` crossfade (`MainScreen.kt:137-166`), **not** a NavHost. No `HorizontalPager`
  anywhere in source. `VisionBottomBar` (`VisionBottomBar.kt:46`) takes `selectedTabId` + `onTabSelected`
  and is the source of truth. Two primary tabs: `planner` (left, index 0) and `goals` (right, index 1);
  `notes` is top-bar only. UI is RTL (Persian).
- `OnboardingRepository.kt` is the canonical DataStore pattern (`preferencesDataStore(name=...)`,
  `booleanPreferencesKey`, `isCompleted: Flow<Boolean>`, `suspend setCompleted()`). No graph flag exists.
- `GoalDetailScreen.kt:235-243` Graph icon → `viewModel.setGraphSheetVisible(true)`;
  `showGraphSheet` is `StateFlow<Boolean>` (`GoalDetailViewModel.kt:144-150`); sheet renders
  `GoalGraphSheetContent` which already has an in-sheet `AlertDialog` legend (the Help/راهنما,
  `GoalGraphSheetContent.kt:187-227`, `LegendRow` `:628-658`). The Graph ViewModel survives tab teardown
  (activity-scoped, keyed per goal), so the gate must live in the VM, not composable `remember`.
- Graph canvas already optimized (ADR-0009): no per-frame text/list work remains.

---

## PART 1 — Main Tab Swipe Navigation

**Decision: lightweight gesture detection, NOT HorizontalPager.**
Rationale (from audit): zero pager infra exists; `selectedTabId` is plain state; `VisionBottomBar` is
the canonical state; introducing `HorizontalPager` would require reconciling pager position ↔
`selectedTabId` ↔ bottom-bar selection and would fight the existing `AnimatedContent` crossfade. The
gesture handler will only *write* `selectedTabId` (never own state), so bottom-nav + swipe stay
synchronized for free, and the existing crossfade animates the transition.

### Implementation
- In `MainScreen.kt`, anchor the swipe handler on the `AnimatedContent` modifier (the tab content
  `Box`). Use `Modifier.pointerInput(primaryTabIds) { detectHorizontalDragGestures { _, dragAmount -> ... } }`
  (or `swipeable`/`anchoredDraggable` if a threshold-based fling is preferred — `detectHorizontalDragGestures`
  is simplest and lowest-risk). On drag end / threshold, compute direction and move to the adjacent
  tab in `primaryTabIds = ["planner","goals"]`:
  - Determine "next/previous" using the **ordered** `primaryTabIds` list (index math), then flip the
    step by `LocalLayoutDirection` (RTL → reverse) so a leftward physical swipe = next tab in LTR terms
    but matches user expectation in RTL. Concretely: in RTL, dragging content to the **left** reveals
    the tab on the **right**; map `dragAmount` sign against `layoutDirection` to pick index±1, clamped.
  - Call `selectedTabId = primaryTabIds[newIndex]`. Guard: ignore while an `AnimatedContent` transition
    is mid-flight (track a `transitioning` flag via `AnimatedContent` `onTransitionEnd`, or debounce) to
    avoid double-switches.
- Bottom navigation remains fully functional and is the single source of truth; swipe is an input alias.
- Keep `VisionBottomBar`/`onTabSelected` unchanged. No new animation layer (reuses existing fade).

### Files
- `app/src/main/java/com/example/ui/screens/MainScreen.kt` (add `pointerInput` + swipe→`selectedTabId`;
  import `detectHorizontalDragGestures`, `LocalLayoutDirection`).

---

## PART 2 — Graph View First-Time Education

**Decision: new DataStore-backed boolean `hasSeenGraphIntroduction`, gated in `GoalDetailViewModel`.**
Reuse the existing in-sheet `AlertDialog` legend surface (no new dialog chrome, no new copy beyond the
required Persian education text). Manual Help (`Info` button) stays available.

### Implementation
- New tiny repository `GraphViewPreferences` (`com.example.core.graph` or `com.example.plugins.goals`):
  mirror `OnboardingRepository` — `preferencesDataStore(name = "graph_settings")`,
  `GRAPH_INTRO_KEY = booleanPreferencesKey("graph_introduction_seen")`,
  `val hasSeenIntroduction: Flow<Boolean> = dataStore.data.map { it[KEY] == true }`,
  `suspend fun markIntroductionSeen() { dataStore.edit { it[KEY] = true } }`.
- In `GoalDetailViewModel`:
  - Hold `private val graphPrefs = GraphViewPreferences(application)`.
  - Add `val showGraphEducation: StateFlow<Boolean> = combine(showGraphSheet, graphPrefs.hasSeenIntroduction) { open, seen -> open && !seen }.stateIn(...)`.
  - Change the Graph-icon action path: tapping the icon still calls `setGraphSheetVisible(true)`; the
    sheet content (`GoalGraphSheetContent`) initializes its `showLegend` from `showGraphEducation`
    (pass `autoShowLegend = showGraphEducation.first()` / observe it) so the legend appears immediately
    on first open. When the legend is dismissed **and** it was the first-time education, call
    `graphPrefs.markIntroductionSeen()`.
  - Simplest integration: pass a `shouldAutoShowLegend` boolean into `GoalGraphSheetContent` (or drive
    its `showLegend` initial state from a `LaunchedEffect(showGraphEducation)`). Keep the existing
    `Info` button so users can reopen Help anytime.
- Education content = the exact Persian bullet text from the spec (☀ هدف / 🔥 اولویت بالا / ● تسک فعال /
  ○ انجام شده), with **no mention of future features**. Append/reuse as `LegendRow`s inside the existing
  legend `AlertDialog` (the spec text maps 1:1 onto the current legend rows; confirm wording matches
  and add the four required bullets verbatim if not already present).
- Gate is in the VM → survives tab teardown; second open shows the normal graph.

### Files
- NEW `GraphViewPreferences.kt` (DataStore boolean).
- `GoalDetailViewModel.kt` (add `showGraphEducation`, mark-seen on dismiss).
- `GoalDetailScreen.kt` (pass education flag into sheet; legend dismiss → markSeen).
- `GoalGraphSheetContent.kt` (accept `autoShowLegend` / drive `showLegend` initial; ensure the four
  required Persian bullets are present verbatim).

---

## PART 3 — Loading & Animation Smoothness Audit

**Audit result:** main recomposition cost is the full-tab `AnimatedContent` swap (`MainScreen.kt:137`)
— acceptable for 5.5; bottom-nav chrome is outside it so it doesn't recompose. The `planner`/`goals`
ViewModels are activity-scoped and survive switches; per-item lambdas and graph canvas are already
hoisted/optimized (ADR-0009). No `derivedStateOf` needed. No blocking main-thread work found.

**Targeted fixes (only if verified warranted, all low-risk, no visual downgrade):**
- **P3.1 (optional polish):** Start the Graph breathing `rememberInfiniteTransition` pulse only after
  the staged entrance completes (`sunEntrance == 1f`), removing the sheet-slide + assemble + breathe
  overlap. Purely visual; animation kept.
- **P3.2:** Keep `ModalBottomSheet` state as-is (fine). Confirm no duplicate animations (only the above
  overlap exists).
- **Rejected:** replacing `AnimatedContent` with pager; removing animations; simplifying UI.
No new `derivedStateOf`/`remember` required — already present. If during implementation any expensive
calc is found in a composable body it will be hoisted, but audit found none.

### Files
- `GoalGraphSheetContent.kt` (P3.1 pulse-start gating, optional).

---

## Testing / Verification
1. `./gradlew :app:compileDebugKotlin`
2. `./gradlew :app:testDebugUnitTest` (domain tests green; Robolectric Room DAO tests remain env-blocked
   — pre-existing SDK36/JDK17 limitation, unrelated).
3. `./gradlew :app:assembleDebug`
4. Manual: (a) swipe planner↔goals both directions + bottom-nav still synced; (b) first Graph open →
   education auto-shows, dismiss → never again, manual Help still works; (c) tab/Graph transitions smooth,
   state preserved (goal detail VM survives tab switch).

## Documentation
- Update `docs/ARCHITECTURE_STATE.md` → add **"Phase 5.5 UX Polish"** section: gesture-navigation
  decision (lightweight gesture, not pager), graph-education onboarding (`GraphViewPreferences` +
  VM gate), performance findings (full-tab AnimatedContent swap is the main cost; already-optimized
  areas), applied fixes. Add changelog entry.
- **ADR:** Create `docs/ADR/ADR-0010-gesture-navigation-and-graph-education.md` (architectural
  decision: swipe-via-gesture-alias rather than HorizontalPager; first-time education via DataStore
  boolean gate in ViewModel).

## Files touched (summary)
- `MainScreen.kt` — Part 1 swipe gesture.
- `GraphViewPreferences.kt` (new) — Part 2 DataStore flag.
- `GoalDetailViewModel.kt` — Part 2 `showGraphEducation` + markSeen.
- `GoalDetailScreen.kt` — Part 2 pass education flag + dismiss handling.
- `GoalGraphSheetContent.kt` — Part 2 auto-show legend; Part 3.1 optional pulse gating.
- `docs/ARCHITECTURE_STATE.md` + `docs/ADR/ADR-0010-*.md` — docs.

## Stop
After docs + ADR written and build/tests green, produce the Phase 5.5 completion report. No auto-advance.

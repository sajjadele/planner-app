# Activity Feed & Timeline Audit

> **Date:** 2026-08-02
> **Type:** Read-only audit — no code changes
> **Project:** Vision Planner Android (Kotlin + Jetpack Compose + MVVM)

---

## 1. Current Architecture

### Data Flow

```
ActivityEventEntity (Room)
      ↓  TaskDetailViewModel.activities (StateFlow, observeActivities(taskId))
      ↓  ActivityMessageMapper.toMessages() — filters SYSTEM events, decodes JSON payload
ActivityMessageModel (StateFlow)
      ↓  combine(activityMessages, _filterState, _selectedActivityDate) → applyFilter()
filteredActivityMessages (StateFlow)
      ↓  collectAsState() in TaskDetailScreen
      ↓  groupActivityMessagesByDay() — groups by Jalali day
      ↓  LazyColumn (message cards)
```

### Two Feed Surfaces
1. **TaskDetailActivityContent** (TaskDetailScreen) — primary feed inside task detail, with date navigator + tag filter chips
2. **TimelineBottomSheet** — modal sheet timeline, date navigation + calendar

### Key Components
- `TaskDetailActivityContent` (TaskDetailScreen.kt) — main LazyColumn feed
- `ActivityFeedFilterChips` — horizontal tag filter chips with `+` add action
- `ActivityFeedDateNavigator` — day-by-day date navigation
- `ActivityMessageCard` — Telegram-style message bubble
- `ActivityComposerBottomSheet` — modal composer (ACTIVITY/EDIT/REPLY modes)
- `ActivityComposerState` + `ActivityComposerReducer` — reducer-pattern composer state

---

## 2. Findings

### F1: Reply Resolution is O(n²) — HIGH

**File:** `TaskDetailScreen.kt` — `TaskDetailActivityContent`
**Location:** Line 1048-1050

```kotlin
items(group.messages, key = { it.id }) { message ->
    val repliedTo = if (message.replyToMessageId != null) {
        messages.find { it.id == message.replyToMessageId }   // O(n) scan per message
    } else null
    ...
}
```

**Problem:** For every message with a `replyToMessageId`, `messages.find { it.id == ... }` performs a linear scan of the ENTIRE message list. A task with m replies triggers m×n comparisons → O(n²) during LazyColumn item composition.

**Impact:**
- 100 activities: ~5,000 comparisons (negligible)
- 1,000 activities: ~500,000 comparisons (noticeable)
- 10,000 activities: ~50,000,000 comparisons (freeze)

**Severity:** High (scaling risk on growing feeds)

---

### F2: Day Grouping is a Blocking O(n) Computation on the Main Thread — HIGH

**File:** `TaskDetailScreen.kt`
**Location:** Line 983

```kotlin
val groups = remember(messages) { groupActivityMessagesByDay(messages) }
```

**Problem:** `groupActivityMessagesByDay()` calls `JalaliDate.fromEpochMs()` and `groupBy` over ALL messages. This runs during composition on the main thread. `remember(messages)` re-runs it whenever `messages` changes (every new activity). Same pattern in `TimelineBottomSheet.kt:86`.

**Impact:** On large histories, every new activity triggers a full re-grouping + sort of the entire list — blocking the UI thread and causing frame drops.

**Severity:** High (per-activity cost grows with history)

---

### F3: No Pagination — Entire History Loaded into Memory — HIGH

**File:** `TaskDetailViewModel.kt`
**Location:** Lines 112-117 (activities), 141-149 (filteredActivityMessages)

```kotlin
val activities: StateFlow<List<ActivityEventEntity>> = activityEventRepository.observeActivities(taskId)
    .stateIn(scope = viewModelScope, started = WhileSubscribed(5000), initialValue = emptyList())
```

**Problem:** `observeActivities(taskId)` loads ALL activity events for a task into a StateFlow, and `filteredActivityMessages` keeps the full filtered list. There is no limit, page size, or date-bound. Room keeps all rows in memory for the flow lifetime.

**Impact:**
- 100 activities: fine
- 1,000 activities: ~memory + slow grouping
- 10,000 activities: high memory + composition jank

**Severity:** High (no horizontal scaling)

---

### F4: `selectedMessageIds` Set Passed to Every Card — Recomposition On Selection Toggle — MEDIUM

**File:** `TaskDetailScreen.kt`
**Location:** Line 1055

```kotlin
isSelected = message.id in selectedMessageIds,   // whole set passed down
```

**Problem:** `selectedMessageIds` is a `Set<Long>` state. When ANY message is selected/deselected, the entire set instance changes. Every card in the LazyColumn re-evaluates `message.id in selectedMessageIds` (O(1) per card, but the set identity change re-reads in every card's recomposition scope). The whole feed list recomposes on each toggle.

**Impact:** In multi-select mode, toggling one message recomposes the visible message cards.

**Severity:** Medium

---

### F5: `TimelineBottomSheet` — Duplicate Grouping + O(n²) Reply Lookup — MEDIUM

**File:** `TimelineBottomSheet.kt`
**Location:** Lines 86, 189

```kotlin
val activityGroups = remember(messages) { groupMessagesByDay(messages) }  // line 86
...
val repliedTo = message.replyToMessageId?.let { replyId ->
    messages.find { it.id == replyId }    // O(n) per reply, line 189
}
```

**Problem:** Same O(n) grouping as F2, PLUS the same O(n²) reply resolution. Duplicate grouping logic between the two feed surfaces means the same cost is paid in both places.

**Severity:** Medium

---

### F6: `JustOneDayFilter` — Date Filter Runs Full List Filter Each Emission — MEDIUM

**File:** `TaskDetailViewModel.kt`
**Location:** Lines 159-187 (`applyFilter`)

```kotlin
if (selectedDate != null) {
    result = result.filter { msg -> normalizeToDayStart(msg.createdAt) == selectedDate }
}
```

**Problem:** When a date is selected, `applyFilter` still iterates the FULL message list and calls `normalizeToDayStart` (a Jalali conversion) per message. This runs on every `activityMessages` emission, even though the date filter makes most messages irrelevant.

**Severity:** Medium

---

### F7: `ActivityComposerBottomSheet` — Whole Sheet Recomputes on Every Keystroke — MEDIUM

**File:** `ActivityComposerBottomSheet.kt`
**Location:** Lines 75-84

```kotlin
var composerState by remember { mutableStateOf(initialState) }
val currentState by rememberUpdatedState(composerState)
val dispatch = remember { { action -> composerState = Reducer.reduce(currentState, action) } }
```

**Problem:** `composerState` holds the entire composer state. Every keystroke (`TextChanged`) creates a new `ActivityComposerState` and recomposes the whole `ModalBottomSheet` including the header, attachment list, and duration UI. The sheet is not the entire screen, but it's a large subtree.

**Impact:** Moderate jank while typing in the composer on lower-end devices.

**Severity:** Medium

---

### F8: `ActivityMessageCard` — `repliedToMessage` Resolution in Parent (F1) — LOW

**File:** `TaskDetailScreen.kt` / `TimelineBottomSheet.kt`

The reply reference preview needs the replied-to message. Currently resolved in the parent LazyColumn item scope (O(n) find) rather than passed via a precomputed map. This couples card rendering to full-list scans.

**Severity:** Low (addressed by F1 fix)

---

### F9: `ActivityFeedDateNavigator` — `onSelectDate` is a No-Op — LOW

**File:** `TaskDetailScreen.kt`
**Location:** Line 1017

```kotlin
onSelectDate = { /* handled by calendar dialog */ }
```

**Problem:** The `onSelectDate` callback in `ActivityFeedDateNavigator` is a no-op — date selection is only possible via the calendar dialog (`onOpenCalendar`). This is dead API surface but also a UX limitation: no direct date-picker affordance on the quick navigator.

**Severity:** Low (UX friction, dead code)

---

### F10: Timeline Range is Only Today → Task Date — MEDIUM UX Constraint

**File:** `TaskDetailViewModel.kt`
**Location:** Lines 246-273

`timelineStartDate` = today, `timelineEndDate` = task's scheduled day. Activities BEFORE the task was created (or after reschedule) may fall outside this window and be inaccessible.

**Impact:** Historical activities that predate the current range are hidden from both the date navigator and the calendar (min/max clamped).

**Severity:** Medium (data accessibility/retention concern)

---

### F11: `ActivityMessageMapper.toMessages` Runs on Main Thread — MEDIUM

**File:** `TaskDetailViewModel.kt` / `ActivityMessageMapper.kt`
**Location:** TaskDetailViewModel line 120-126

```kotlin
val activityMessages = activities
    .map { ActivityMessageMapper.toMessages(it) }   // JSON decode + legacy parse per event
    .stateIn(...)
```

**Problem:** `map` runs on the collector's context (main). For every activity event emission, `ActivityPayloadCodec.decode` (JSON parsing) + `extractText`/`extractDuration` run synchronously for the whole list. Combined with F3 (no pagination), this is JSON-decoding of the entire history.

**Severity:** Medium

---

## 3. Performance Risks

| # | Risk | Where |
|---|------|-------|
| P1 | O(n²) reply resolution | TaskDetailScreen.kt:1048, TimelineBottomSheet.kt:189 |
| P2 | Blocking O(n) day grouping on main thread | TaskDetailScreen.kt:983, TimelineBottomSheet.kt:86 |
| P3 | No pagination — full history in memory | TaskDetailViewModel.kt:112-117 |
| P4 | JSON mapping of full list on main thread | TaskDetailViewModel.kt:120-126 |
| P5 | Full-list filtering + Jalali conversion per event | TaskDetailViewModel.kt:159-187 |
| P6 | Feed re-renders on selection toggle | TaskDetailScreen.kt:1055 |

---

## 4. UX Friction Points

1. **Reply reference requires full-list scan** — with large histories, tapping reply nav could be slow even before scrolling.
2. **No fast jump** — to reach a specific old activity, user must tap calendar or arrow through many days (no date-range picker or search).
3. **Timeline range ambiguity** — user may not know activities fall outside today→task-date window until they can't find them.
4. **`onSelectDate` dead path** — navigator shows a date but only the calendar can change it; inconsistent affordance.
5. **Multi-select recomposes feed** — toggling selection causes visible recomposition, adding latency in selection mode.

---

## 5. Scalability Risks

- **No cap on `observeActivities`** — the DAO returns all rows; no LIMIT/page.
- **No date-window default** — timeline shows all dates; no "last N days" default.
- **O(n²) reply + O(n) grouping per change** — quadratic behaviors on quadratic growth.
- **Two feed surfaces duplicate the same list processing** — cost paid twice.

**Estimate:**
- 100 activities → fine
- 1,000 activities → sluggish on lower-end
- 10,000 activities → likely jank/freeze (O(n²) reply + JSON mapping + grouping)

---

## 6. Recommended Improvements

### P0 — Critical
1. **Precompute a reply map once** instead of `messages.find` per card:
   `val replyMap = remember(messages) { messages.associateBy { it.id } }` then `replyMap[message.replyToMessageId]`. Turns O(n²) → O(n).

2. **Move grouping + mapping off the main thread or bound the input.** At minimum, cap the activity history loaded (see P0-3).

3. **Bound or paginate `observeActivities`** — add a date-window or page-size so the ViewModel doesn't hold the full history.

### P1 — Important
4. **Derive `filteredActivityMessages` with `distinctUntilChanged`** so filtering doesn't re-run on unchanged filter output.
5. **Predicate date filter at the DB level** (DAO with date range) instead of filtering the full list in memory — or at least short-circuit when `selectedDate != null`.
6. **Hoist `isSelected` computation** — pass a single `isSelected: Boolean` (already done) but avoid re-creating the whole set-backed recomposition; consider `selectedDate`-independent card memoization.

### P2 — Future
7. **Unify grouping logic** into one shared utility used by both feed surfaces.
8. **Add a date-range picker / search** instead of day-by-day arrows.
9. **Make composer a lighter subtree** — scope its state so header/metadata don't recompose on text input.
10. **Revisit timeline range** — clarify or widen today→taskDate bounds so historical activities stay reachable.
11. **Add `@Stable`/`@Immutable` annotations** to `ActivityMessageModel`, `ActivityMessageDisplayContent` so Compose can skip recomposition on unchanged card params.

---

## 7. Files Reviewed

| File | Sections |
|------|----------|
| `TaskDetailViewModel.kt` | activities, filteredActivityMessages, applyFilter, creationContext, timeline range |
| `TaskDetailScreen.kt` | TaskDetailActivityContent, ActivityFeedFilterChips, ActivityFeedDateNavigator, grouping, LazyColumn |
| `TimelineBottomSheet.kt` | TimelineSheetContent, groupMessagesByDay, date navigation |
| `ActivityComposerBottomSheet.kt` | ModalBottomSheet, reducer dispatch |
| `ActivityComposerState.kt` | compose state model |
| `ActivityComposerReducer.kt` | pure reducer |
| `ActivityMessageCard.kt` | card rendering, display content, remember usage |
| `ActivityMessageMapper.kt` | entity→model mapping, system event filtering |
| `ActivityMessageModel.kt` | UI read model |
| `ActivityFeedFilterState.kt` | filter state model |

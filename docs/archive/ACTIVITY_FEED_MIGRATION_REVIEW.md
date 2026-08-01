# Activity Feed Architecture Migration — Review & Plan

## A) Current Rendering Flow

```
Database (activity_events)
  │
  ▼
ActivityEventDao.observeByTaskId(taskId)        ← raw entities, desc by timestamp
  │
  ├──▶ TaskDetailViewModel.activities             ← StateFlow<List<ActivityEventEntity>>
  │
  ├──▶ TaskDetailActivityContent(steps, activities, …)
  │       │
  │       ├── Steps loop
  │       │     for each step:
  │       │       1. Filter activities by stepId  (line 543)
  │       │       2. ActivityMessageMapper.toMessage() per event  (line 545)
  │       │       3. StepCardMapper.toCardModel(step, activities)  (line 548)
  │       │       4. StepCard(model) →
  │       │            ├── Collapsed: checkbox, title, stats
  │       │            └── Expanded: ActivityMessageCard for each message
  │       │
  │       ├── Vertical divider
  │       │
  │       └── TimelinePreviewCard(activities)     ← raw entities, redundant mapping
  │               │
  │               ├── ActivityMessageMapper.toMessage(latestActivity)  (line 698)
  │               └── ActivityMessageCard(message)  (line 718)
  │
  └──▶ activityMessages                           ← StateFlow<List<ActivityMessageModel>>
          │                                        (pre-mapped, system-events filtered)
          │
          ├── TimelineBottomSheet (messages = activityMessages)  (line 198)
          │       └── ActivityMessageCard per message, grouped by day
          │
          └── (UNUSED in Activity tab — already available, not consumed)
```

### Duplicated mapping paths

| Path | Input | Mapper | Output | Location |
|------|-------|--------|--------|----------|
| A | raw entities → step loop | `ActivityMessageMapper.toMessage()` per event | `ActivityMessageCard` inside steps | `TaskDetailActivityContent` lines 543–556 |
| B | raw entities → `TimelinePreviewCard` | `ActivityMessageMapper.toMessage()` on latest | `ActivityMessageCard` in preview | `TimelinePreviewCard` line 698 |
| C | raw entities → `TimelineBottomSheet` | `ActivityMessageMapper.toMessages()` in ViewModel (line 117–118) | `ActivityMessageCard` grouped by day | `TaskDetailViewModel.activityMessages` → bottom sheet |

**Path C is the correct feed-oriented path and already exists.** Paths A and B duplicate the mapping and embed activities in a step-centric hierarchy.

---

## B) Components to Keep

| Component | File | Why |
|-----------|------|-----|
| `ActivityMessageModel` | `data/ActivityMessageModel.kt` | Feed-ready read model with nullable `stepId` |
| `ActivityMessageMapper` | `data/ActivityMessageMapper.kt` | Filters system events, produces feed models |
| `ActivityMessageCard` | `ui/components/ActivityMessageCard.kt` | Feed-ready; no step awareness |
| `ActivityComposerBottomSheet` | `ui/ActivityComposerBottomSheet.kt` | Already supports `ComposerMode.ACTIVITY` for standalone activities |
| `ActivityComposerState` | `ui/composer/ActivityComposerState.kt` | No changes needed |
| `ActivityDraft` | `data/ActivityDraft.kt` | Already step-agnostic |
| `ActivityDraftResolver` | `data/ActivityDraftResolver.kt` | Already step-agnostic |
| `ActivityPayload` | `data/ActivityPayload.kt` | Pure content container, no step coupling |
| `ActivityEventDao` | `data/ActivityEventDao.kt` | `observeByTaskId()` returns all events — feed-ready |
| `ActivityEventRepository` | `data/ActivityEventRepository.kt` | No changes needed |
| `TaskDetailViewModel` | `ui/TaskDetailViewModel.kt` | `activityMessages` flow is the feed data source. Keep all existing methods. |
| `TimelineBottomSheet` | `ui/TimelineBottomSheet.kt` | Already takes `List<ActivityMessageModel>` and groups by day — becomes the reference feed implementation |
| `ActivityMessageAction` sealed class | `data/ActivityMessageModel.kt` | Edit/Delete/Reply action contract |
| Capability system | `data/ActivityMessageModel.kt` | `capability()` → `ActivityMessageCapability` |

---

## C) Components to Deprecate

| Component | File | Lines | Status | Reason |
|-----------|------|-------|--------|--------|
| `StepCardMapper` | `data/StepCardMapper.kt` | 1–102 | **Deprecate** | `groupActivitiesByStep()` (line 78–87) filters `stepId != null`, silently dropping task-level activities. The step-as-container aggregation is the opposite of the feed model. |
| `StepCard` | `ui/components/StepCard.kt` | 1–210 | **Deprecate as container** | The expandable step-with-nested-messages pattern will be replaced by filter chips. The `StepCard` composable can be removed entirely. |
| `TimelineEventMapper` | `ui/TimelineEventMapper.kt` | 1–259 | **Deprecate** | Legacy parallel mapping path. Uses `TimelineEventUiModel` instead of `ActivityMessageModel`. All activities already map through `ActivityMessageMapper`. |
| `TimelinePreviewLatestEvent` | `ui/TaskDetailScreen.kt` | 736–779 | **Remove** | Dead code — uses `TimelineEventUiModel` but is no longer called from `TimelinePreviewCard` (which now uses `ActivityMessageCard` directly). |
| `TaskStepItem` | `ui/TaskDetailScreen.kt` | 609–661 | **Remove** | Dead code — legacy step row (checkbox + title + delete) unused in current Activity tab. |

---

## D) Components Requiring Replacement / Rewrite

| Component | File | Lines | Action |
|-----------|------|-------|--------|
| `TaskDetailActivityContent` | `ui/TaskDetailScreen.kt` | 492–602 | **Rewrite entirely.** Replace step-loop + `TimelinePreviewCard` with a flat activity feed consuming `activityMessages`. Add date separators, FAB, and filter bar placeholder. |
| `TimelinePreviewCard` | `ui/TaskDetailScreen.kt` | 669–733 | **Remove.** Its role in the Activity tab is replaced by the feed. (If the "Activity tab → timeline" navigation already opens `TimelineBottomSheet`, this card is redundant.) |
| `selectedStepIdForActivity` | `ui/TaskDetailScreen.kt` | 89 | **Simplify.** Composer opens without step context by default. Step assignment becomes optional — handled inside the composer or via a step-picker chip. |

### Specific code to remove from `TaskDetailActivityContent`

```kotlin
// Lines 509–558: Steps header + StepCard loop — REMOVE entirely
item(key = "steps_header") { ... }
if (steps.isEmpty()) { ... } else { items(steps, ...) { step -> ... StepCard(...) } }

// Lines 560–567: Divider — REMOVE

// Lines 569–592: Activity header + add button — REMOVE (replace with feed structure)

// Lines 594–600: TimelinePreviewCard — REMOVE (replaced by inline feed)
```

### New `TaskDetailActivityContent` will be:

```
FilterBar("All" | Step chips | Images | Files)
  │
FeedHeader + Date separator ("Today")
  │
ActivityMessageCard (from activityMessages)
ActivityMessageCard
  │
Date separator ("Yesterday")
  │
ActivityMessageCard
  │
...
```

---

## E) Migration Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| **1. Step visibility lost** — Users accustomed to step containers won't see their step groupings | High | Step filter chips at the top of the feed show step names + counts. Tapping a chip filters `activityMessages` by `stepId`, restoring step-scoped view. |
| **2. Task-level activities currently lost** — `StepCardMapper.groupActivitiesByStep()` filters `stepId != null`, these exist but are invisible in the Activity tab | Medium | **Already a bug today.** The feed will fix this by using `activityMessages` which includes all activities regardless of `stepId`. |
| **3. `TimelinePreviewCard` is already semi-migrated** — It uses `ActivityMessageMapper` + `ActivityMessageCard` but takes raw entities | Low | Remove this component; its job is fully subsumed by the feed. |
| **4. Duplicate rendering** — `TimelineBottomSheet` and the new feed will both render the same data | Low | `TimelineBottomSheet` becomes the full timeline navigation; the feed is the inline default view. They share data, no duplication. |
| **5. Step creation UX** — The step creation flow (`CreateStepWithActivitiesUseCase`) still creates `STEP_CREATED` events in the same DAO | Medium | System events continue to be filtered by `ActivityMessageMapper`. Step creation is unaffected; only the rendering of existing steps changes. |
| **6. Composer step assignment** — Composer no longer receives `selectedStepIdForActivity` by default | Low | Add an optional step-picker chip/section inside the composer for users who want to tag a message with a step. |
| **7. No database migration** — `stepId` values are preserved on existing entities | None | No migration needed. Existing data works as-is. |

### What stays unchanged

- All existing features: Edit, Delete, Reply, Image preview, File attachment, Duration
- All data layer code: `ActivityEventEntity`, `ActivityEventDao`, `ActivityEventRepository`
- All existing ViewModel methods: `createActivity`, `createStep`, `deleteActivity`, `updateActivity`, `getActivityById`
- `ActivityComposerBottomSheet` — no changes to the composer itself in Phase 5.2.1
- `TimelineBottomSheet` — kept exactly as-is (it already consumes `activityMessages` with day grouping)
- `TaskDetailScreen` core structure — tabs, state variables, overlays remain

---

## F) Phase 5.2.1 — Feed Foundation (Detailed Plan)

### Goal

Replace the step-first rendering in `TaskDetailActivityContent` with a flat activity feed consuming `activityMessages` directly.

### What changes

**1. `TaskDetailScreen.kt` — `TaskDetailActivityContent` signature**

Before:
```kotlin
@Composable
private fun TaskDetailActivityContent(
    steps: List<TaskStepEntity>,
    activities: List<ActivityEventEntity>,
    onTapComposer: () -> Unit,
    onToggleStep: (TaskStepEntity) -> Unit,
    onDeleteStep: (TaskStepEntity) -> Unit,
    onAddActivityToStep: (Int) -> Unit,
    listState: LazyListState,
    onOpenTimeline: () -> Unit
)
```

After:
```kotlin
@Composable
private fun TaskDetailActivityContent(
    messages: List<ActivityMessageModel>,
    onTapComposer: () -> Unit,
    onMessageAction: (ActivityMessageAction) -> Unit,
    listState: LazyListState
)
```

**2. `TaskDetailScreen.kt` — Caller change**

Before (line 174):
```kotlin
1 -> TaskDetailActivityContent(
    steps = steps,
    activities = activities,
    onTapComposer = { showActivityComposer = true },
    onToggleStep = { viewModel.toggleStepCompletion(it) },
    onDeleteStep = { viewModel.deleteStep(it) },
    onAddActivityToStep = { stepId -> ... },
    listState = activityListState,
    onOpenTimeline = { showTimelineSheet = true }
)
```

After:
```kotlin
1 -> TaskDetailActivityContent(
    messages = activityMessages,
    onTapComposer = { showActivityComposer = true },
    onMessageAction = handleMessageAction,
    listState = activityListState
)
```

**3. `TaskDetailActivityContent` body — rewritten**

New structure:
```kotlin
LazyColumn(...) {
    // FAB area or floating button (can be outside column)
    
    // Date grouping from groupMessagesByDay (reuse from TimelineBottomSheet)
    // For each group:
    //   stickyHeader → date separator ("امروز", "دیروز", or Jalali date)
    //   items → ActivityMessageCard for each message
    //   onMessageAction forwarded
}
```

**4. Date grouping** — Reuse the existing `groupMessagesByDay()` from `TimelineBottomSheet.kt` (line 326). Extract to a shared location or call directly.

### What is NOT changed in 5.2.1

- `StepCard` / `StepCardMapper` — still exist but are no longer called from the Activity tab. Removed in 5.2.2.
- `TimelinePreviewCard` — still exists but is no longer called. Removed in 5.2.3.
- Filter bar — placeholder only. Chips added in 5.2.2.
- Pagination — deferred to 5.2.4.

### Files changed

| File | Change |
|------|--------|
| `ui/TaskDetailScreen.kt` | Rewrite `TaskDetailActivityContent` + caller in tab content |
| `ui/TimelineBottomSheet.kt` | Extract `groupMessagesByDay()` to shared utils (or keep in-file, call from both) |

---

## G) Phase 5.2.2 — Step as Filter

- Replace `StepCard` with `StepFilterChip` bar
- Add filter state management
- Chips: "All" | Step1 (n) | Step2 (n) | ... | Images | Files
- Filter modifies which `ActivityMessageModel` entries are shown (by `stepId` or attachment type)
- Remove `StepCardMapper` from call sites
- Remove `selectedStepIdForActivity` state (or simplify to filter intent)

## H) Phase 5.2.3 — Timeline Unification

- Remove `TimelinePreviewCard` from the Activity tab
- Remove `TimelinePreviewLatestEvent` composable (dead code)
- Remove `TimelineEventMapper` entirely
- `TimelineBottomSheet` continues to reuse the same `activityMessages` flow + `groupMessagesByDay`

## I) Phase 5.2.4 — UX Polish

- Empty state when `messages.isEmpty()`
- Loading skeleton
- Smooth scrolling using existing `activityListState`
- Image-only message optimization (show thumbnail without full card)
- Attachment previews in cards
- Pagination preparation (add offset-based query to DAO or Paging 3)

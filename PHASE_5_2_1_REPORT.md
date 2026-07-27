# Phase 5.2.1 — Activity Feed Foundation Report

## Files Changed

| File | Change |
|------|--------|
| `app/.../ui/TaskDetailScreen.kt` | **Rewritten:** `TaskDetailActivityContent` — from step-loop to flat feed |
| `app/.../data/ActivityInteractionFoundationTest.kt` | **Added:** 7 feed tests |

## Architecture Migration

### Before (Step-centric)

```
Tab 1 → TaskDetailActivityContent
         │
         ├── steps header ("مراحل (3)")
         ├── StepCard loop
         │     for each step:
         │       1. Filter activities by stepId  (line 543)
         │       2. ActivityMessageMapper.toMessage() per event
         │       3. StepCardMapper.toCardModel(step, activities)
         │       4. StepCard(model) → ActivityMessageCard
         │
         ├── Divider
         ├── Activity header + Add button
         └── TimelinePreviewCard (raw entities, divergent mapping)
```

### After (Flat Feed)

```
Tab 1 → TaskDetailActivityContent
         │
         ├── Empty state ("هنوز فعالیتی ثبت نشده")
         │
         ├── MessageGroup (today)
         │     ├── ActivityFeedDayHeader ("امروز")
         │     └── ActivityMessageCard × N
         │
         ├── MessageGroup (yesterday)
         │     ├── ActivityFeedDayHeader ("دیروز")
         │     └── ActivityMessageCard × N
         │
         └── ... (older days with Jalali dates)
```

### Data flow

Before — two divergent paths:
```
ActivityEventDao.observeByTaskId()
  ├── activities (raw) → step loop → StepCardMapper → StepCard
  └── activities → ActivityMessageMapper → activityMessages → TimelineBottomSheet
```

After — single path:
```
ActivityEventDao.observeByTaskId()
  └── is → ActivityMessageMapper → activityMessages
        ├── → TaskDetailActivityContent (feed)
        └── → TimelineBottomSheet (unchanged)
```

## What Was Removed from Rendering

- **`StepCard`** — no longer rendered in the Activity tab
- **`StepCardMapper.toCardModel()`** — no longer called
- **`StepCardMapper.groupActivitiesByStep()`** — no longer called from UI
- **`TimelinePreviewCard`** — no longer rendered in the Activity tab
- **`selectedStepIdForActivity`** state — removed; composer creates task-level activities by default
- **`steps` StateFlow collection** — no longer consumed at screen level
- **`activities` StateFlow collection** — no longer consumed at screen level (feed uses `activityMessages`)
- **`onToggleStep`, `onDeleteStep`, `onAddActivityToStep`, `onOpenTimeline`** callbacks — removed from `TaskDetailActivityContent` signature

## What Was Added

- **`groupActivityMessagesByDay()`** — groups `List<ActivityMessageModel>` by normalized Jalali day
- **`ActivityMessageGroup`** — data class holding `dateKey` + messages
- **`ActivityFeedDayHeader()`** — composable rendering "Today" / "Yesterday" / Jalali date headers
- **`ActivityMessageCard` now receives `onAction`** — long-press context menu (Edit/Delete/Reply) works in feed
- Empty state with prompt

## Tests Added (7)

| Test | Validates |
|------|-----------|
| `feed groups messages by day` | Different timestamps → different day groups |
| `feed orders days newest first` | Groups sorted descending by date |
| `feed orders messages by newest first within same day` | Messages sorted descending by createdAt |
| `feed shows task-level activities without stepId` | `stepId = null` messages are visible |
| `feed empty list shows no groups` | Empty input → empty output |
| `feed activityMessages flow includes all activities` | Both step and non-step activities pass through mapper |
| `feed excludes system events` | System events are filtered out |

## Preserved Features

- ✅ **Create activity** — via FAB → composer in `ComposerMode.ACTIVITY`
- ✅ **Create step** — via composer in `ComposerMode.STEP`
- ✅ **Image attachment** — rendered by `ActivityMessageCard`
- ✅ **File attachment** — rendered by `ActivityMessageCard`
- ✅ **Edit message** — long-press → context menu
- ✅ **Delete message** — long-press → confirm dialog
- ✅ **Reply message** — long-press → composer in REPLY mode
- ✅ **Timestamp** — rendered as HH:mm
- ✅ **Duration** — badge on manual activities
- ✅ **Timeline** — `TimelineBottomSheet` unchanged, still consumes `activityMessages`

## Test Results

```
460 tests completed, 6 failed  ← 6 pre-existing, 7 new passed
BUILD SUCCESSFUL (assembleDebug)
```

## Remaining Technical Debt

1. **StepCardMapper is now disconnected** from the rendering path but still exists in the codebase. Planned for deprecation in Phase 5.2.2.
2. **TimelinePreviewCard/TimelinePreviewLatestEvent** are dead code in the Activity tab but remain defined. Planned for removal in Phase 5.2.3.
3. **No filter bar** — users lose the ability to see step-scoped views of activities. Planned for Phase 5.2.2 (Step as Filter chips).
4. **Composer creates only task-level activities** — step assignment requires a step-picker inside the composer or filter-bar integration. Planned for Phase 5.2.2.
5. **No pagination** — large activity lists may impact performance. Planned for Phase 5.2.4.

# Phase 5.5c — Legacy Step Container Removal & Architecture Cleanup Report

**Date:** July 2026  
**Core Principle:** Activity is the primary entity. Step is metadata/tag only.

---

## 1. Files Deleted (6 files)

| File | Reason | Risk |
|------|--------|------|
| `data/StepCardModel.kt` | Container model nesting messages in steps | None — no production imports |
| `data/StepCardMapper.kt` | Container aggregation (group by step) | None — only used by tests |
| `ui/components/StepCard.kt` | Expandable card with nested activities | None — unused in any rendering path |
| `ui/TimelineEventMapper.kt` | System event display (STEP_CREATED etc.) | None — only used by dead `TimelinePreviewLatestEvent` composable |
| `ui/ImageViewerDialog.kt` | Image preview for dead timeline code | None — orphaned after TimelinePreviewLatestEvent removal |
| `test/data/StepCardMapperTest.kt` | Tests for deleted StepCardMapper | None — tests for deleted code |

## 2. Composable Functions Removed (from `TaskDetailScreen.kt`)

| Function | Reason |
|----------|--------|
| `TaskStepItem` | Dead code — never called by any composable |
| `TimelinePreviewCard` | Dead code — never called by any composable |
| `TimelinePreviewLatestEvent` | Dead code — never called by any composable |
| Skeleton step placeholders | Dead step/timeline shimmer placeholders |
| `ImageViewerDialog` call | Only used inside `TimelinePreviewLatestEvent` |

## 3. Imports Cleaned Up (from `TaskDetailScreen.kt`)

| Removed Import | Reason |
|----------------|--------|
| `ContentScale` | Only used in `TimelinePreviewLatestEvent` (deleted) |
| `AsyncImage` | Only used in `TimelinePreviewLatestEvent` (deleted) |
| `ImageRequest` | Only used in `TimelinePreviewLatestEvent` (deleted) |
| `ActivityMessageMapper` | Only used in `TimelinePreviewCard` (deleted) |
| `ActivityEventEntity` | Only used in `TimelinePreviewCard` (deleted) |

## 4. ViewModel Methods Deprecated (6 methods)

| Method | Deprecation Level | Replacement |
|--------|-------------------|-------------|
| `addStep(title)` | Deprecated | `createStep(StepDraft(title = title))` via composer |
| `addNote(text)` | Deprecated | `createActivity(ActivityDraft(text = text))` |
| `addManualActivity(title, duration)` | Deprecated | `createActivity(ActivityDraft(text = title, durationMinutes = ...))` |
| `addImage(uri, description)` | Deprecated | `createActivity(ActivityDraft(attachments = listOf(ActivityAttachment.Image(uri))))` |
| `toggleStepCompletion(step)` | WARNING | Use Activity Feed for tracking progress |
| `deleteStep(step)` | WARNING | Use Activity Feed for managing activities |

These methods are **not called from any UI**. They remain for backward compatibility but will produce `WARNING` deprecation warnings at compile time.

## 5. Architecture Before/After

### Before (Step-as-Container)

```
TaskDetailScreen
├── TaskDetailActivityContent
│   ├── StepCard (container)
│   │   ├── StepCardModel (messages nested)
│   │   └── ActivityMessageCard
│   └── ActivityFeedHeaders
├── TimelineBottomSheet
│   └── TimelineEventMapper (system events as UI)
├── TimelinePreviewCard (dead)
│   ├── ActivityMessageCard (inside column)
│   └── TimelinePreviewLatestEvent
├── TaskStepItem (dead)
│   └── Checkbox + delete
└── Skeleton step placeholders (dead)
```

### After (Activity Feed Only)

```
TaskDetailScreen
├── TaskDetailActivityContent
│   ├── ActivityTagChip (step name on cards)
│   └── ActivityMessageCard (standalone)
├── ActivityFeedHeaders
└── TimelineBottomSheet (preserved, not shown — showTimelineSheet never true)
```

### Data Pipeline (unchanged)

```
ActivityEventEntity (data layer)
    ↓
ActivityMessageMapper (filters system events, decodes payloads)
    ↓
ActivityMessageModel (clean read model — no eventType, no raw JSON)
    ↓
ActivityMessageCard (UI — tags via ActivityTagChip)
```

## 6. What Was NOT Changed

- ❌ **Database schema** — no migrations
- ❌ **ActivityEventEntity** — unchanged (stepId still present for tag/filter)
- ❌ **stepId** — still used for tag classification and filter
- ❌ **TaskStepEntity** — unchanged (name, colorIndex, order)
- ❌ **ActivityDraft** — no stepId field added (composer stays step-agnostic)
- ❌ **ActivityMessageModel** — no changes
- ❌ **Composer** — ActivityComposerBottomSheet, ActivityComposerState unchanged
- ❌ **StepDraft, StepDraftResolver, CreateStepWithActivitiesUseCase** — still active for step creation via composer toggle

## 7. Remaining Container-like Behavior (ComposerMode.STEP)

The **only active** Step-as-Container entry point is the comma:

**Problem:** `ComposerMode.STEP` allows creating a Step with initial activities atomically via `CreateStepWithActivitiesUseCase`. After creation, activities are independent (stepId = tag association), but the creation flow treats Step as container by bundling initial activities with the step.

**Recommended Option A — Keep step creation (simplified):**
- Keep `ComposerMode.STEP` toggle in the composer toolbar
- Remove `initialActivities` from `StepDraft` — steps are created as **pure tags** (title + color only)
- Activities are added independently through the normal creation flow (which automatically gets stepId from context)
- Rename UI label from "ساخت مرحله" to "برچسب جدید" for consistency

**Option B — Remove step creation entirely:**
- Remove `ComposerMode.STEP` toggle
- Steps can only be created as side-effect of first activity with a new tag
- More consistent with pure tag model, but reduces UX flexibility

**My recommendation: Option A.** Creating a tag (step) independently is a valid user action, but it should NOT bundle initial activities. The context-aware creation flow in Phase 5.5b already handles the tag→activity association correctly.

## 8. Remaining Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| `showTimelineSheet` never set to true but `TimelineBottomSheet` call exists | Low | Does not render — no visual impact |
| `TimelineBottomSheet` still imports old activity card code | Low | Compiles fine, never shows |
| Deprecated methods exist in ViewModel | Low | No callers, `@Deprecated` annotation warns at compile time |
| `ImageViewerDialog` deleted — was only used in dead code | None | No impact |

## 9. Phase 6 Tests Added (7 tests)

| Test | Coverage |
|------|----------|
| `message without step has no stepId` | Task-level messages render correctly |
| `message with step has stepId for tag display` | Tagged messages carry stepId |
| `filter by step shows only matching messages` | Step-based filtering works |
| `system step events are filtered out by mapper` | STEP_CREATED/COMPLETED never reach UI |
| `removing StepCard and StepCardMapper does not affect feed` | Direct ActivityMessageModel creation works |
| `edit preserves stepId from original entity` | Edit via entity.copy preserves tag |
| `reply preserves replyToMessageId` | Reply link survives round-trip |

## 10. Recommendation for Next Phase

**Phase 6 — ComposerMode.STEP simplification:**
- Remove `initialActivities` from `StepDraft`
- Remove `CreateStepWithActivitiesUseCase` (simplify to direct step creation)
- Rename step toggle labels
- Keep step creation as pure tag creation

**Phase 7 — AI Insight Layer:**
- Activity pattern detection
- Smart filtering
- Time-based analytics

---

*End of Report — architecture now structurally pure Activity Feed with Step-as-Tag.*

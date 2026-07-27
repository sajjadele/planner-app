# Phase 5.5b — Step Container Audit

> **Audit Date:** July 2026  
> **Scope:** Verify no remaining Step-as-Container patterns after Phase 5.5b

---

## Results Summary

✅ **Activity Feed rendering** — 100% tag-based (no container)
✅ **FAB creation** — context-aware, no container
✅ **Message cards** — stepName displayed as tag chip
⚠️ **Composer step mode** — one remaining container-like flow

---

## 1. Dead Code (Not Used in UI, Safe to Ignore)

| Artifact | Reason Dead | Impact |
|----------|------------|--------|
| `TaskStepItem` composable (line 964) | Defined but **never called** | 🔴 Was container with checkbox |
| `StepCard` composable (`StepCard.kt`) | Defined but **not imported/used** | 🔴 Container with nested activities |
| `StepCardModel` + `StepCardMapper` | Defined but **not used** | 🔴 Container data model |
| `TimelinePreviewCard` (line 1019) | Defined but **never called** | 🟡 Leaks system events |
| `TimelineEventMapper` + `TimelineBottomSheet` | Not triggered from any UI | 🟡 Leaks system events |
| `viewModel.toggleStepCompletion()` | **Not called** from any UI | 🟡 |

## 2. Live Code (Still Active)

### 2.1 Composer Step Mode (ONLY Active Container)

**Call chain:**
```
Composer Toolbar → toggle step mode
    ↓
ActivityComposerBottomSheet line 105-106
    onCreateStep(composerState.toStepDraft())
    ↓
TaskDetailScreen line 297-298
    viewModel.createStep(stepDraft)
    ↓
TaskDetailViewModel.createStep() line 368
    ↓
CreateStepWithActivitiesUseCase.execute()
    ↓
Creates: TaskStepEntity + STEP_CREATED event + optional initial activities
```

**This is the only place where Step is treated as a container** — creating a step with `initialActivities`. The user can toggle the composer to "Step Mode" and create a step with optional initial content.

### 2.2 Filter Chips (Tag Behavior — Correct)

```
ActivityFeedFilterChips line 614
    ↓
onFilterByStep(stepId)
    ↓
TaskDetailViewModel.filterByStep()
    ↓
filterState.selectedStepId = stepId
    ↓
Messages filtered by stepId (tag behavior ✅)
```

This is correct tag-based filtering.

### 2.3 Context-Aware Creation (Tag Behavior — Correct)

```
createActivity(draft)  ← context-aware overload
    ↓
Reads _filterState.selectedStepId
    ↓
Passes stepId to createActivity(draft, stepId)
    ↓
ActivityEventEntity.stepId = tagId ✅
```

This is correct tag-based creation.

---

## 3. Risk Assessment

### 🔴 High Risk (Actively Container)
| Location | Risk | Impact |
|----------|------|--------|
| `ComposerMode.STEP` via `ComposerToolbar` | Users can still create steps with initial activities | **Low** — this is a step *creation* UX, not a rendering container. The created step is just a tag that activities can reference. |
| `CreateStepWithActivitiesUseCase` | Creates `STEP_CREATED` system event | **Low** — this event is filtered out by `ActivityMessageMapper` |

### 🟡 Medium Risk (Dead Code — Will Rot)
| Location | Risk | Impact |
|----------|------|--------|
| `StepCard.kt` | Not used but still in build | **Low** — dead code, no runtime impact |
| `ToggleStepCompletion` | Method exists but no UI calls it | **Low** — dead code |

### 🟢 No Risk (Correct Tag Behavior)
- `ActivityFeedFilterChips` ✅
- `ActivityCreationContext` + `createActivity(draft)` ✅
- `ActivityMessageCard.stepName` ✅
- `ActivityTagChip` ✅

---

## 4. Conclusion

**There are NO active Step-as-Container rendering paths in the current UI.**

The only remaining container-like code is:
1. **Composer step mode** (`ComposerMode.STEP`) — allows creating a step with initial activities. This is a *creation* UX, not a *rendering* container. After creation, activities are independent.
2. **Dead code** — `StepCard`, `StepCardModel`, `TaskStepItem`, `TimelinePreviewCard`, `toggleStepCompletion` — these exist but are never called from any UI path.

### What to Remove (Future Phase)

When ready for cleanup:
1. `StepCard.kt`, `StepCardModel.kt`, `StepCardMapper.kt` — fully replaced by `ActivityTagChip`
2. `StepDraft.kt`, `StepDraftResolver.kt` — step creation can be simplified
3. `CreateStepWithActivitiesUseCase.kt` — step creation can use `TaskStepRepository` directly
4. `TimelineEventMapper.kt`, `TimelineBottomSheet.kt` — no longer triggered
5. `viewModel.toggleStepCompletion()`, `deleteStep()`, `addStep()` — unused
6. `TaskStepItem` composable — unused
7. `ComposerMode.STEP` — depends on product decision about step creation UX

### What to Keep
- `TaskStepEntity`, `TaskStepDao`, `TaskStepRepository` — needed for DB and tag resolution
- `ActivityMessageModel.stepId` — the tag ID
- `ActivityFeedFilterChips` — tag filter UI
- `ActivityTagChip` — tag display

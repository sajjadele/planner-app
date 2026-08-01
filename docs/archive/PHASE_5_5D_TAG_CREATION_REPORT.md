# Phase 5.5d — Step to Tag Creation Simplification Report

> **Date:** July 2026  
> **Commit:** *(next commit)*  
> **Core Principle:** Step is metadata/tag only. Activity creation is independent.

---

## Architecture Before

```
Composer (mode=STEP)
  ├── text → StepDraft.title
  ├── attachments → StepDraft.initialActivities ← container concept
  ├── duration → StepDraft.initialActivities[0].durationMinutes
  └── Submit
       ↓
CreateStepWithActivitiesUseCase.execute()
       ↓
1. TaskStepEntity (from title)
2. STEP_CREATED event
3. ActivityEventEntity[] (from initialActivities) ← container behavior
```

## Architecture After

```
Composer (mode=STEP)
  ├── text → StepDraft.title ← only title
  ├── attachments/duration: hidden in UI ← cannot be entered
  └── Submit
       ↓
CreateStepUseCase.execute()
       ↓
1. TaskStepEntity (from title) ← pure tag
2. STEP_CREATED event (filtered by ActivityMessageMapper)
3. No ActivityEventEntity created ← no container behavior
```

## Files Changed

| File | Status | Change |
|------|--------|--------|
| `data/StepDraft.kt` | **MODIFIED** | Removed `initialActivities` field, `hasInitialContent()`, `getTotalAttachmentCount()` |
| `data/StepDraftResolver.kt` | **MODIFIED** | Removed `hasInitialActivities()`, `getInitialActivities()`, `encodeActivityDescription()`, `resolveActivityEventType()`. Only `getStepTitle()` remains |
| `data/CreateStepUseCase.kt` | **NEW** | Was `CreateStepWithActivitiesUseCase`. Now only creates `TaskStepEntity` + `STEP_CREATED` event. No initial activities loop |
| `data/CreateStepWithActivitiesUseCase.kt` | **DELETED** | Replaced by `CreateStepUseCase` |
| `ui/composer/ActivityComposerState.kt` | **MODIFIED** | `toStepDraft()` returns only title (no initial activities). `canSubmit()` requires only text in STEP mode |
| `ui/composer/UnifiedComposerContent.kt` | **MODIFIED** | Attachment preview and duration chip hidden in STEP mode |
| `ui/TaskDetailViewModel.kt` | **MODIFIED** | Updated reference from `CreateStepWithActivitiesUseCase` to `CreateStepUseCase` |
| `test/data/CreateStepWithActivitiesUseCaseTest.kt` | **DELETED** | Replaced by `StepTagCreationTest` |
| `test/data/StepTagCreationTest.kt` | **NEW** | 10 tests verifying pure tag creation |

## Files Preserved (no changes)

| File | Reason |
|------|--------|
| `ComposerMode.kt` | `STEP` enum kept — no rename per user instruction |
| `ComposerToolbar.kt` | No changes needed — toggle button stays |
| `ActivityComposerBottomSheet.kt` | Submit logic unchanged — `onCreateStep` callback still works |
| `ActivityDraft.kt` | No changes needed — stepId not added |
| `ActivityEventEntity.kt` | No changes — stepId still used for tag/filter |
| Database | No migration |

## Decision About ComposerMode.STEP

**Option A implemented (as agreed):**
- `ComposerMode.STEP` preserved (not renamed)
- Toggle button stays in toolbar
- In STEP mode: attachments + duration UI hidden
- Submit creates pure tag (StepEntity + STEP_CREATED only)
- New tag appears in filter chips after creation

## Tests Added (10 scenarios)

| # | Test | Verifies |
|---|------|----------|
| 1 | `StepDraft contains only title` | No extra fields |
| 2 | `StepDraft has no initialActivities field` | Container concept removed via reflection |
| 3 | `StepDraft empty companion creates empty title` | `EMPTY` works |
| 4 | `StepDraftResolver returns step title` | Resolver simplified |
| 5 | `StepDraftResolver has no initial activities methods` | Container methods removed via reflection |
| 6 | `CreateStepUseCase creates step entity and STEP_CREATED only` | Signature verified |
| 7 | `create activity after selecting tag assigns stepId` | Context-aware creation |
| 8 | `create activity without tag has null stepId` | Task-level activities |
| 9 | `edit tagged activity preserves stepId` | Edit safety |
| 10 | `reply tagged activity preserves replyToMessageId and stepId` | Reply safety |
| 11 | `no code path creates activity from tag creation` | Final safety check |

## Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Compiler catches old class name | None | All references updated |
| Existing step creation still works | Low | `createStep()` signature unchanged |
| ComposerMode.STEP works with simplified UX | Low | Attachments/duration hidden |
| StepDraft.initialActivities in compiled classes | None | Recompilation will fail + fixed |
| Activity creation flow unaffected | None | No changes to `createActivity()` path |

## Next Phase Suggestion

**Phase 6 — Activity Feed UX Polish:**
- Day-group header/tag row overlap fix
- Pagination / lazy loading
- Search within feed
- Media gallery view

---

*End of Report — Step-as-Container pattern fully eliminated. Step is now pure tag/metadata.*

# Phase 5.5b — Context-Aware Activity Creation Report

> **Date:** July 2026  
> **Commit:** `da19fb1`  
> **Scope:** Context injection at ViewModel level — no composer changes, no DB changes

---

## Architecture

```
User selects tag "UI Design" in filter chips
         ↓
_filterState.selectedStepId = 1L
         ↓
creationContext (derived flow)
  stepId = 1L, stepName = "UI Design"
         ↓
FAB → createActivity(draft)
         ↓
context-aware overload reads _filterState
         ↓
Calls createActivity(draft, stepId = 1)
         ↓
ActivityEventEntity.stepId = 1
```

## What Changed

| File | Status | Change |
|------|--------|--------|
| `data/ActivityCreationContext.kt` | **NEW** | Data class with stepId + stepName |
| `ui/TaskDetailViewModel.kt` | **MODIFIED** | Added `creationContext` flow + context-aware `createActivity(draft)` overload |
| `data/ActivityCreationContextTest.kt` | **NEW** | 18 tests across 7 scenarios |

## What Did NOT Change

❌ `ActivityDraft` — no `stepId` field added (composer stays agnostic)  
❌ `ActivityMessageModel` — no changes  
❌ `ActivityEventEntity` — no changes  
❌ `ActivityComposerBottomSheet` — no changes  
❌ `TaskDetailScreen` — no changes (call sites already used single-param `createActivity`)  
❌ Database — no migrations  
❌ No files deleted  

## Behavior Matrix

| Current Filter | New Activity stepId | Scenario |
|---------------|-------------------|----------|
| همه (null) | null | Task-level note |
| UI Design (stepId=1) | 1 | Tagged activity |
| Backend (stepId=2) | 2 | Different tag |
| Switch tag → همه | null | Context resets |
| Switch همه → tag | tag | Context activates |
| Edit tagged activity | Preserved | stepId from original entity |
| Edit task-level activity | null | No change |

## Test Scenarios (18 tests)

1. ✅ Create without tag → stepId = null
2. ✅ Create with tag → stepId = selectedStepId
3. ✅ Filter tag → all → new activities task-level
4. ✅ Filter all → tag → new activities inherit tag
5. ✅ Edit tagged → stepId preserved
6. ✅ Reply → replyToMessageId preserved, composer agnostic
7. ✅ Composer stays step-agnostic (no stepId in ActivityDraft)

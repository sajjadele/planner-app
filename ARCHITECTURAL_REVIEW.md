# Vision Planner — TaskDetail Activity System Architectural Review

**Date:** 2026-07-26
**Scope:** TaskDetail Activity System (Phases 4.6–4.10)
**Reviewer:** Hermes Agent

---

# Executive Summary

The Activity system has evolved through 5 phases (4.6–4.10) and reached a functional but **architecturally fragile** state. The core Draft → Payload → Event pipeline works correctly, and the Telegram-style unified composer provides a good UX foundation. However, several architectural decisions are creating increasing maintenance burden and will block future features (replies, editing, AI analysis).

**Key finding:** The system conflates two distinct concepts — **Step creation** and **Activity creation** — into a single `ActivityDraft` model. This creates hidden complexity in the ViewModel where `createStepWithActivities()` must reverse-engineer what the user actually wanted.

**Recommendation:** Continue improving the current architecture, but refactor the Step/Activity separation before adding Phase 7 (AI Insight Layer).

---

# Current Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        UI LAYER                             │
│                                                             │
│  TaskDetailScreen                                           │
│    ├── StepCard[] ← StepCardModel ← StepCardMapper          │
│    │     └── ActivityMessageCard[] ← ActivityMessageModel   │
│    ├── FAB → ActivityComposerBottomSheet                    │
│    │     └── UnifiedComposerContent ← ActivityComposerState │
│    │           └── ComposerToolbar                           │
│    │           └── AttachmentPreview                         │
│    │           └── DurationPickerDialog                      │
│    └── TimelineBottomSheet ← TimelineEventMapper            │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                    STATE MANAGEMENT                          │
│                                                             │
│  ActivityComposerState (data class)                         │
│    └── toDraft() → ActivityDraft                            │
│                                                             │
│  ActivityComposerAction (sealed class)                      │
│    └── ActivityComposerReducer.reduce() → new State         │
│                                                             │
│  TaskDetailViewModel                                        │
│    ├── createActivity(draft)                                │
│    │     └── STEP_CREATED → createStepWithActivities()      │
│    │     └── else → addEvent()                              │
│    └── createStepWithActivities(draft)                      │
│          ├── addStep() → stepId                             │
│          └── if attachments → addEvent(NOTE_ADDED)          │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                     DOMAIN LAYER                            │
│                                                             │
│  ActivityDraft ──→ ActivityDraftResolver                    │
│       │                    │                                │
│       │                    ├── resolveEventType()           │
│       │                    ├── encodeDescription()          │
│       │                    └── getStepTitle()               │
│       │                                                     │
│       └──→ ActivityPayload ──→ ActivityPayloadCodec         │
│                                 └── encode/decode JSON      │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                      DATA LAYER                             │
│                                                             │
│  ActivityEventEntity (Room)                                 │
│    ├── id, taskId, stepId?, eventType, description, timestamp│
│    │                                                        │
│  TaskStepEntity (Room)                                      │
│    ├── id, taskId, title, isCompleted, createdAt            │
│    │                                                        │
│  Repositories:                                              │
│    ├── ActivityEventRepository → ActivityEventDao           │
│    └── TaskStepRepository → TaskStepDao + ActivityEventDao  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

# What Is Good

## 1. Unified Composer (Phase 4.7.4)
**Excellent.** The single text input + toolbar pattern is exactly right for Telegram-style UX. Users don't need to choose between "note" vs "image" vs "step" — they just type and attach.

## 2. State Machine Pattern (Phase 4.7.3)
**Good.** `ActivityComposerState` + `ActivityComposerAction` + `ActivityComposerReducer` is a clean, testable pattern. Replacing multiple boolean states (`isStepExpanded`, `isNoteExpanded`) was the right call.

## 3. Read Model Separation (Phase 4.8)
**Good.** `ActivityMessageModel` and `StepCardModel` properly decouple UI from database entities. The mapper layer (`ActivityMessageMapper`, `StepCardMapper`) is clean and testable.

## 4. JSON Payload Storage (Phase 4.7.2)
**Good decision.** Storing rich content as JSON in `description` field avoids database migrations while supporting attachments. The backward-compatible decoder handles legacy formats gracefully.

## 5. Step Card Expand/Collapse (Phase 4.9)
**Good UX.** Collapsed shows summary, expanded shows activities. The "افزودن فعالیت" button inside expanded content is discoverable.

---

# Architectural Risks

## Risk 1: ActivityDraft Identity Crisis (HIGH)

**Problem:** `ActivityDraft` serves two masters:
- Creating a Step (intent=STEP)
- Creating an Activity (intent=ACTIVITY)

When intent=STEP, the draft's `text` becomes the step title. When intent=ACTIVITY, `text` is the activity content. This dual meaning creates confusion:

```kotlin
// In createStepWithActivities():
val stepTitle = ActivityDraftResolver.getStepTitle(draft) // draft.text = step title
val childDraft = draft.copy(
    intent = ActivityIntent.ACTIVITY,
    stepId = stepId,
    text = null  // "step title already on the step itself"
)
```

We're manually nulling out `text` because it means different things in different contexts.

**Risk:** As features grow (replies, editing, AI suggestions), this conflation will cause more bugs.

**Recommendation:** Separate `StepDraft` and `ActivityDraft` in future refactor.

## Risk 2: stepId Propagation Fragility (MEDIUM)

**Problem:** `stepId` must be manually propagated through:
1. StepCard `onAddActivity(stepId)` callback
2. TaskDetailScreen `selectedStepIdForActivity` state
3. Draft `copy(stepId = selectedStepIdForActivity)`
4. ViewModel `createActivity(draftWithStepId)`

Any break in this chain loses the step association. We already fixed this once (Phase 4.9.1).

**Risk:** Future composers or entry points may forget to set stepId.

**Recommendation:** Consider a `StepContext` wrapper that automatically provides stepId.

## Risk 3: Event Type Explosion (MEDIUM)

**Problem:** `ActivityEventType` has 8 values:
```kotlin
STEP_CREATED, STEP_COMPLETED, STEP_REOPENED, STEP_DELETED,
NOTE_ADDED, FILE_ADDED, MANUAL_ACTIVITY, IMAGE_ADDED
```

But the unified composer only creates: `STEP_CREATED`, `NOTE_ADDED`, `MANUAL_ACTIVITY`. The others (`IMAGE_ADDED`, `FILE_ADDED`) are legacy.

**Risk:** New features may add more event types, making the enum unwieldy.

**Recommendation:** Eventually migrate to a single `ACTIVITY_CREATED` event with typed payloads.

## Risk 4: Dual Creation Paths (LOW)

**Problem:** Steps can be created via:
1. Composer (intent=STEP) → `createStepWithActivities()`
2. Legacy `addStep(title)` method

Both paths create `STEP_CREATED` events, but path 1 also creates child activities.

**Risk:** Inconsistent behavior if users use different paths.

**Recommendation:** Deprecate legacy `addStep()` method.

---

# UX Problems

## Problem 1: Step Creation Flow Inconsistency

**Current:**
- Composer → intent=STEP → type title → submit → Step created (no activities)
- Composer → intent=STEP → type title + add image → submit → Step + child activity

**Issue:** The user doesn't know whether their image will be "on the step" or "as a child activity." The mental model is unclear.

**Better:** When creating a step, all content (text, images) should be **on the step itself**, not as a child activity. The STEP_CREATED event should carry the full payload.

## Problem 2: "افزودن فعالیت" Button Discoverability

**Current:** The button is only visible when:
1. Step is expanded (requires clicking expand arrow)
2. Step has messages (expand arrow only shows if `model.messages.isNotEmpty()`)

**Issue:** For a newly created step with no activities, there's no way to add activities because:
- `messages` is empty → no expand arrow → no "افزودن فعالیت" button

**Wait:** Actually, `addStep()` creates a STEP_CREATED event which IS included in messages. So `messages.isNotEmpty()` should be true. But this is confusing — the STEP_CREATED event is not really a "message."

**Recommendation:** Always show the expand arrow and "افزودن فعالیت" button, regardless of message count.

## Problem 3: Timeline Shows Raw Event Types

**Current:** `TimelineEventMapper` maps event types to icons and text. But it must handle both new JSON format and legacy format for each event type.

**Issue:** As more event types are added, this mapper becomes a maintenance burden.

**Recommendation:** Use `ActivityMessageModel` as the source for timeline rendering, not raw entities.

---

# Data Flow Problems

## Problem 1: createStepWithActivities() Complexity

```kotlin
fun createStepWithActivities(draft: ActivityDraft) {
    viewModelScope.launch {
        val stepTitle = ActivityDraftResolver.getStepTitle(draft) ?: return@launch
        val step = TaskStepEntity(taskId = taskId, title = stepTitle)
        val stepId = taskStepRepository.addStep(step)

        if (draft.attachments.isNotEmpty()) {
            val childDraft = draft.copy(
                intent = ActivityIntent.ACTIVITY,
                stepId = stepId,
                text = null
            )
            val description = ActivityDraftResolver.encodeDescription(childDraft)
            activityEventRepository.addEvent(...)
        }
    }
}
```

**Issues:**
1. Manually nulling `text` is a code smell
2. Creating a "child draft" from the original draft is fragile
3. The method must know about both Step and Activity creation

**Better:** If intent=STEP and has attachments, encode attachments directly into the STEP_CREATED event's description.

## Problem 2: Reducer Has Log Statements

```kotlin
// ActivityComposerReducer.kt
Log.d("COMPOSER_DEBUG", "⚡ Reducer called: action=...")
Log.d("COMPOSER_DEBUG", "⚡ Reducer result: ...")
```

**Issue:** Reducers should be pure functions with no side effects. Logging is a side effect.

**Recommendation:** Remove logs from reducer, add them in the ViewModel or use a debug flag.

## Problem 3: UnifiedComposerContent Has Debug UI

```kotlin
// UnifiedComposerContent.kt
Box(
    modifier = Modifier
        .background(Color.Green.copy(alpha = 0.2f)) // DEBUG: visible background
)
```

**Issue:** Debug colors remain in production code.

**Recommendation:** Remove all debug visual artifacts.

---

# Recommended Changes

## 1. Clean Up Debug Artifacts
- **Priority:** High
- **Reason:** Debug logs and colors in production code cause confusion
- **Expected impact:** Cleaner codebase, no functional change
- **Files affected:**
  - `ActivityComposerReducer.kt` — Remove Log statements
  - `UnifiedComposerContent.kt` — Remove debug background colors
  - `AttachmentPreview.kt` — Remove debug logs and red background
  - `ActivityComposerBottomSheet.kt` — Remove debug logs

## 2. Fix Step Creation with Content
- **Priority:** High
- **Reason:** When creating a step with attachments, content should be ON the step, not as a child activity
- **Expected impact:** Simpler mental model, cleaner data model
- **Files affected:**
  - `TaskStepRepository.kt` — `addStep()` should accept optional payload
  - `TaskDetailViewModel.kt` — `createStepWithActivities()` simplified
  - `ActivityDraftResolver.kt` — New method for step payload encoding

## 3. Always Show Expand Arrow on StepCard
- **Priority:** Medium
- **Reason:** Users should always be able to add activities to a step
- **Expected impact:** Better discoverability
- **Files affected:**
  - `StepCard.kt` — Remove `model.messages.isNotEmpty()` condition

## 4. Deprecate Legacy addStep() Method
- **Priority:** Medium
- **Reason:** Two creation paths cause inconsistency
- **Expected impact:** Single code path for step creation
- **Files affected:**
  - `TaskDetailViewModel.kt` — Mark `addStep()` as `@Deprecated`

## 5. Separate StepDraft and ActivityDraft (Future)
- **Priority:** Low (Phase 7+)
- **Reason:** Current conflation will cause more bugs as features grow
- **Expected impact:** Cleaner domain model, easier to extend
- **Files affected:**
  - New: `StepDraft.kt`
  - Modified: `ActivityDraft.kt` (simplified)
  - Modified: `ActivityDraftResolver.kt`
  - Modified: `TaskDetailViewModel.kt`

---

# Debugging Lessons Review

| Problem | Root Cause | Current Prevention | Status |
|---------|-----------|-------------------|--------|
| Compose stale state | `remember()` capturing old state | `rememberUpdatedState()` | ✅ Fixed |
| Multiple boolean UI states | `isStepExpanded`, `isNoteExpanded` | Single state machine | ✅ Fixed |
| Mapper losing data | Missing stepId propagation | stepId in ActivityDraft | ⚠️ Fragile |
| Image rendering Task vs Step | stepId always null | stepId propagation chain | ⚠️ Fragile |
| Package declaration corruption | Python scripts editing Kotlin | Use `patch` tool | ✅ Lesson learned |
| Duplicate imports | Adding import without checking | `grep` before adding | ✅ Lesson learned |
| Missing import after adding Log | Forgetting import | Always add import with Log | ✅ Lesson learned |

**The architecture does NOT fully prevent these problems.** The stepId propagation chain is particularly fragile — it relies on manual state management across multiple composable levels.

---

# Final Recommendation

**Should we continue improving the current architecture or redesign part of the Activity system before adding more features?**

**Answer: Continue improving, but with one targeted refactor first.**

The current architecture is **80% correct**. The unified composer, state machine, and read model patterns are solid. The main issue is the **Step/Activity conflation** in `ActivityDraft`.

**Before Phase 7 (AI Insight Layer), refactor:**
1. Make STEP_CREATED event carry its own payload (text + attachments)
2. Remove the "child activity" pattern for step content
3. Clean up all debug artifacts

This refactor is **bounded** (affects ~5 files) and **low-risk** (no database migration needed). It will make the system cleaner for AI analysis features that need to understand step content.

**Do NOT redesign from scratch.** The current system works. Incremental improvement is the right path.

---

*Review completed. No code changes made.*

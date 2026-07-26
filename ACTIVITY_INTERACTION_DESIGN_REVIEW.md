# Activity Interaction Flow Design — Phase 4.15

**Date:** 2026-07-26
**Phase:** 4.15 (Design Only — No Code Changes)
**Scope:** Edit, Delete, Reply, Context Menu, Interaction State

---

## Executive Summary

Vision Planner has completed the transition from event-log architecture to message-oriented architecture (Phases 4.12–4.14). The `ActivityMessageModel` now has full identity (`id`, `taskId`, `stepId`, `createdAt`) and interaction capabilities (`canEdit`, `canDelete`, `replyToMessageId`, `capability()`).

This design document defines **exactly** how Edit, Delete, Reply, and Context Menu interactions will work in Phase 5. It is a specification — not implementation.

**Bottom line:** Edit should reuse the Unified Composer (Option A). Delete should be soft (isDeleted flag). Reply should be a single-reference model (not a full thread tree). A lightweight interaction state container handles all UI-level interaction tracking.

---

## 1. Current Architecture Analysis

### Message Creation Flow (Current)

```
User types in UnifiedComposerBottomSheet
  ↓
ActivityComposerState (text, attachments, duration, mode)
  ↓
ActivityComposerAction dispatched to ActivityComposerReducer
  ↓
ActivityComposerState updated (pure function)
  ↓
User submits → ActivityComposerBottomSheet.onSubmit
  ↓
TaskDetailViewModel.createActivity(draft, stepId?)
  ↓
ActivityDraftResolver.resolveEventType(draft) → ActivityEventType
  ↓
ActivityDraftResolver.encodeDescription(draft) → JSON string
  ↓
ActivityEventEntity(taskId, stepId, eventType, description, timestamp)
  ↓
activityEventRepository.addEvent(entity)
  ↓
Room DB inserts into activity_events table
  ↓
ActivityEventDao.observeByTaskId emits new entity
  ↓
TaskDetailViewModel.activities → StateFlow<List<ActivityEventEntity>>
  ↓
TaskDetailViewModel.activityMessages maps via ActivityMessageMapper
  ↓
activityMessages → StateFlow<List<ActivityMessageModel>> → UI consumption
```

### Message Display Flow (Current)

```
activityMessages → List<ActivityMessageModel>
  ↓
TaskDetailScreen passes to:
  ├── StepCard → messages → ActivityMessageCard (per message)
  └── TimelineBottomSheet → messages → ActivityMessageCard (per message)
```

### Current State Summary

| Component | State |
|-----------|-------|
| **Composer** | ActivityComposerState (text, attachments, duration, mode) |
| **Actions** | ActivityComposerAction (TextChanged, AddAttachment, RemoveAttachment, DurationChanged, ConvertToStep, ConvertToActivity, Reset) |
| **Reducer** | Pure function: (state, action) → newState |
| **Mapper** | ActivityMessageMapper (entity → model, filters system events) |
| **Model** | ActivityMessageModel (id, taskId, stepId, text, attachments, durationMinutes, createdAt, canEdit, canDelete, isDeleted, replyToMessageId) |
| **Capability** | ActivityMessageCapability (canEdit, canDelete, canReply) — derived from model.state |
| **Actions (intent)** | ActivityMessageAction (Edit, Delete, Reply) — sealed class |
| **Card Renderer** | ActivityMessageCard — Telegram-style bubble |
| **Persistence** | ActivityEventEntity in Room — no fields for edit/delete/reply tracking |

### What's Missing for Interaction

| Gap | Current State | Needed |
|-----|---------------|--------|
| Edit payload | No stored previous version | Need original payload preserved |
| Delete tracking | `isDeleted` on model doesn't persist | Need DB field or separate approach |
| Reply reference | `replyToMessageId` on model doesn't persist | Need DB field to store reference |
| Interaction state | No global interaction state | Need UI-level state container |
| Context menu | No menu composable | New composable needed (Phase 5 UI) |

---

## 2. Edit Flow Design

### Option A — Reuse Unified Composer ✅ **RECOMMENDED**

```
User long-presses message
  ↓
ContextMenu appears: Reply | Edit | Delete
  ↓
User taps Edit
  ↓
ActivityMessageModel payload extracted:
  - text → composer.text
  - attachments → composer.attachments
  - durationMinutes → composer.durationMinutes
  ↓
Composer opens in EDIT mode (new ComposerMode.EDIT)
  ↓
User modifies content
  ↓
User taps "Save" (✓) or "Cancel" (×)
  ↓
On Save:
  - ActivityMessageAction.Edit(messageId=id)
  - ViewModel updates ActivityEventEntity.description with new JSON payload
```

**Why Option A is better than Option B:**

| Criterion | Option A (Reuse) | Option B (Separate) |
|-----------|-------------------|---------------------|
| Code reuse | ✅ 100% reuse of composer | ❌ New editor component |
| User experience | ✅ Familiar interface | ❌ Different UI to learn |
| Consistency | ✅ Same as creating | ❌ Inconsistent |
| Maintenance | ✅ One code path | ❌ Two code paths |
| Implementation | ✅ ~4 hours | ❌ ~8 hours |
| Testing | ✅ Existing composer tests | ❌ New test suite |
| Telegram alignment | ✅ Telegram edits in same input | ❌ Telegram has separate edit field |

**Note on Telegram alignment:** Actually, Telegram uses the SAME input field for edits — when you edit a message, the text goes back into the composer. Option A aligns with Telegram's actual UX.

### Edit Implementation Details

**New field needed on `ComposerMode` enum:**
```kotlin
sealed class ComposerMode {
    data object ACTIVITY : ComposerMode()
    data object STEP : ComposerMode()
    data class EDIT(val messageId: Long) : ComposerMode()  // NEW
}
```

**Edit flow on ActivityDraft resolution:**
```kotlin
// When in EDIT mode, existing content pre-fills the composer
if (mode is ActivityComposerMode.EDIT) {
    val existingMessage = findMessageById(mode.messageId)
    state = ActivityComposerState(
        text = existingMessage.text ?: "",
        attachments = existingMessage.attachments,
        durationMinutes = existingMessage.durationMinutes,
        mode = mode
    )
}
```

**On save (ViewModel):**
```kotlin
fun editMessage(messageId: Long, updatedDraft: ActivityDraft) {
    viewModelScope.launch {
        val eventType = ActivityDraftResolver.resolveEventType(updatedDraft)
        val description = ActivityDraftResolver.encodeDescription(updatedDraft)
        
        // Update the existing ActivityEventEntity
        activityEventRepository.updateEvent(
            ActivityEventEntity(
                id = messageId.toInt(),
                taskId = currentTaskId,
                stepId = findStepId(messageId),  // from existing entity
                eventType = eventType.name,
                description = description
            )
        )
    }
}
```

**⚠️ Database limitation:** `ActivityEventDao` currently has no `update()` method. Adding one requires either:
1. A 1-line Room DAO method (no migration — `@Query("UPDATE activity_events SET ...")`)
2. Or using `@Update` annotation on the existing DAO

Since `@Update` works without schema changes when all fields are present, this is safe.

---

## 3. Delete Strategy Design

### Decision: Soft Delete with `isDeleted` Flag ✅ **RECOMMENDED**

```
User long-presses message → taps Delete
  ↓
Confirmation dialog: "حذف این پیام؟" (Delete this message?)
  ↓
On confirm → ActivityMessageAction.Delete(messageId)
  ↓
ViewModel.setMessageDeleted(messageId)
  ↓
ActivityEventDao soft-update: description = "{\"$deleted\":true}"
  OR better: use a dedicated column or companion table
  OR pragmatic: just mark model.isDeleted = true on UI level
```

### Hard Delete vs Soft Delete Analysis

| Criterion | Hard Delete | Soft Delete |
|-----------|-------------|-------------|
| Simplicity | ✅ Simple SQL DELETE | ❌ Needs flag/column |
| Data recovery | ❌ Impossible | ✅ Can restore |
| AI Insight compatibility | ❌ Loses data for analysis | ✅ Deleted messages still analyzable |
| Behavior history | ❌ Gap in timeline | ✅ Shows user deleted something |
| Rollback capability | ❌ None | ✅ Can undo |
| DB migration needed | ❌ | ✅ Needs `isDeleted` column |
| Performance | ✅ Frees space | ~ Same |

### ⚠️ Database Limitation

**Soft delete requires a DB change** — either:
1. New column `isDeleted INTEGER NOT NULL DEFAULT 0` on `activity_events` — this **is a migration**
2. Companion table `activity_deleted_events` — also a migration

**Hard DELETE requires no migration.**

### Decision

Since Phase 4.15 rules say **no migrations**, and the spec says Phase 5 implementation will happen after this design phase:

**Recommendation:** Use soft delete with `isDeleted` flag, but the `ActivityMessageModel.isDeleted` field serves as the UI state layer **for now**. The database migration for `isDeleted` column will be added in Phase 5 alongside actual implementation.

This aligns with the Phase 4.12+ architecture principle: `ActivityMessageModel` already has `isDeleted`. The DB migration is the only missing piece.

**Alternatively**, hard delete removes the entity from Room, and the UI simply doesn't show it — `ActivityMessageMapper.toMessage()` handles it by returning null. But this loses all historical data.

### Final Decision

**Phase 5 will use Soft Delete:**
- `isDeleted` flag on `ActivityMessageModel` (exists ✅)
- Requires one-time DB migration (deferred to Phase 5 implementation)
- UI marks message as deleted → renders with strikethrough + "حذف شده" overlay
- Message still appears in timeline for AI analysis context

---

## 4. Reply System Design

### What "Reply" Means in Vision Planner

Unlike Telegram (which has threaded conversations), Vision Planner has a **task → step → activity** hierarchy. A "reply" means:

> "This message is a comment/response to another message in the same task/step context."

### Design: Reference-Based Reply

```
Original message:
  id=1, taskId=100, stepId=5, text="تصویر اولیه طراحی"
  attachments=[Image("content://img/1.jpg")]

Reply message:
  id=2, taskId=100, stepId=5, text="رنگ آبی رو تغییر بدم"
  replyToMessageId=1
```

### Visual Rendering

```
📷 تصویر اولیه طراحی          ← original
  [image preview]

↩️ رد شده از طراحی:          ← reply indicator
"رنگ آبی رو تغییر بدم"         ← reply text
```

### Data Model

**`ActivityMessageModel` already has `replyToMessageId` field ✅**

**No DB changes needed for now:**
- `replyToMessageId` is stored in `ActivityEventEntity.description` JSON as part of the payload
- New field on `ActivityPayload`:
```kotlin
data class ActivityPayload(
    val text: String? = null,
    val attachments: List<ActivityAttachment> = emptyList(),
    val durationMinutes: Int? = null,
    val replyToMessageId: Long? = null  // ← NEW (stored in JSON, no DB column needed)
)
```

This stores reply reference **inside JSON payload**, not as a DB column — avoiding migration.

### Reply Flow

```
User long-presses message → taps Reply
  ↓
Composer opens in REPLY mode with context
  ↓
Composer pre-fills:
  - header text: "↩️ رد روی: [original message preview]"
  - replyToMessageId set in state
  ↓
User types reply text
  ↓
User submits
  ↓
ActivityDraft gets replyToMessageId set
  ↓
Payload encodes: {"text":"...", "replyToMessageId":1}
  ↓
New ActivityEventEntity created (same as normal activity)
```

### Thread Consideration

This is **NOT a full thread tree** (like Telegram). It's a **flat reply reference**:
- No nested replies
- No reply-to-reply
- Simple one-level reference

This is sufficient for the current scope and can be extended to full threading in Phase 6+.

---

## 5. Context Menu Design

### Trigger: Long Press on Message

```
Long press on ActivityMessageCard
  ↓
ContextMenu composable appears anchored to message
  ↓
Options shown based on ActivityMessageCapability:
```

### Menu Layout

```
┌─────────────────────┐
│ ↩️ پاسخ               │ ← always visible (canReply)
│ ✏️ ویرایش             │ ← if canEdit
│ 🗑 حذف                │ ← if canDelete
└─────────────────────┘
```

### Capability-Based Visibility

```kotlin
val capability = message.capability()

ContextMenu(
    items = buildList {
        add(ContextMenuItem("↩️ پاسخ", "reply", capability.canReply))
        add(ContextMenuItem("✏️ ویرایش", "edit", capability.canEdit))
        add(ContextMenuItem("🗑 حذف", "delete", capability.canDelete))
    }
)
```

### Menu Behavior

| Action | Pre-condition | Result |
|--------|---------------|--------|
| Reply | `canReply == true` | Opens composer in reply mode |
| Edit | `canEdit == true` | Opens composer pre-filled with message content |
| Delete | `canDelete == true` | Shows confirmation dialog, then soft-deletes |

**Default capabilities for new messages:**
- `canEdit = true` — user can always edit their own messages
- `canDelete = false` — deletion requires explicit grant (owner or admin, future: based on user role)
- `canReply = true` — any message can be replied to

---

## 6. Interaction State Design

### Minimal Interaction State Container

```kotlin
object ActivityInteractionState {
    // Currently selected message for context menu
    var selectedMessageId: Long? = null
    
    // Message being edited (set when Edit is tapped)
    var editingMessageId: Long? = null
    
    // Message being replied to (set when Reply is tapped)
    var replyingToMessageId: Long? = null
    
    // Context menu visibility
    var showContextMenu: Boolean = false
    
    // Delete confirmation dialog visibility
    var showDeleteConfirmation: Boolean = false
    
    // Message to be deleted (ID only, set when Delete is confirmed)
    var pendingDeleteMessageId: Long? = null
}
```

### Where This Lives

This state **belongs in TaskDetailViewModel**, not as a singleton object. This ensures:
1. Lifecycle-aware (survives configuration changes)
2. Tied to specific task's message scope
3. Testable with ViewModel testing
4. Clean separation from message data

### ViewModel Additions

```kotlin
// Interaction state
private val _interactionState = MutableStateFlow(ActivityInteractionState())
val interactionState: StateFlow<ActivityInteractionState> = _interactionState.asStateFlow()

fun onMessageLongPressed(messageId: Long) {
    _interactionState.value = _interactionState.value.copy(
        selectedMessageId = messageId,
        showContextMenu = true
    )
}

fun onEditPressed(messageId: Long) {
    _interactionState.value = _interactionState.value.copy(
        selectedMessageId = null,
        showContextMenu = false,
        editingMessageId = messageId
    )
}

fun onReplyPressed(messageId: Long) {
    _interactionState.value = _interactionState.value.copy(
        selectedMessageId = null,
        showContextMenu = false,
        replyingToMessageId = messageId
    )
}

fun onDeletePressed(messageId: Long) {
    _interactionState.value = _interactionState.value.copy(
        selectedMessageId = null,
        showContextMenu = false,
        showDeleteConfirmation = true,
        pendingDeleteMessageId = messageId
    )
}

fun closeContextMenu() {
    _interactionState.value = _interactionState.value.copy(
        showContextMenu = false,
        selectedMessageId = null
    )
}

fun closeDeleteConfirmation() {
    _interactionState.value = _interactionState.value.copy(
        showDeleteConfirmation = false,
        pendingDeleteMessageId = null
    )
}
```

---

## 7. Database Limitations Review

### Current `ActivityEventEntity` Schema

```kotlin
@Entity(tableName = "activity_events")
data class ActivityEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val taskId: Int,
    val stepId: Int? = null,
    val eventType: String,
    val description: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
```

### Requirements vs Current Schema

| Interaction | Field Needed | Currently Present? | Required Change |
|-------------|-------------|-------------------|-----------------|
| **Edit** | Store previous versions for undo/audit | ❌ No | ❌ Not needed for Phase 5 — overwrite is enough |
| **Delete** | `isDeleted` column OR delete row | ❌ No column | ✅ Delete row = hard delete (no migration) |
| **Reply reference** | Stored in JSON payload | ✅ In `description` JSON | ✅ No DB change needed |
| **Edit history** | Previous payload storage | ❌ No | ❌ Defer to Phase 6 (AI insight) |

### Verdict

| Interaction | DB Migration Needed? | Notes |
|-------------|---------------------|-------|
| **Edit** | ❌ No — `@Update` on DAO works without schema change | Payload is overwritten in place |
| **Delete (hard)** | ❌ No — `@Delete` on DAO works without schema change | Loses data permanently |
| **Delete (soft)** | ⚠️ Yes — needs `isDeleted` column | Defer to implementation phase |
| **Reply** | ❌ No — stored in JSON payload | No schema change |

**For Phase 5 without migration:**
- **Edit**: ✅ Works immediately (overwrite `description` JSON via `@Update`)
- **Delete**: Hard DELETE only (no `isDeleted` column yet) — message disappears from UI
- **Reply**: ✅ Works immediately (replyToMessageId in JSON payload)
- **Soft Delete**: Requires migration (defer or add alongside Phase 5 implementation)

---

## 8. Phase 5 Implementation Plan

### Order of Implementation

```
Phase 5 Activity Interaction (estimated: 3 days)
│
├── Day 1: Foundation
│   ├── Add ComposerMode.EDIT to ActivityComposerState
│   ├── Add replyToMessageId to ActivityPayload & ActivityPayloadCodec
│   ├── Add ActivityEventDao.update() method
│   ├── Add ActivityEventDao.updateDescription() convenience
│   └── Add TaskDetailViewModel interaction state
│
├── Day 2: Interaction Logic
│   ├── Implement replyToMessageId in ActivityDraftResolver
│   ├── Implement editMessage() in TaskDetailViewModel
│   ├── Implement deleteMessage() in TaskDetailViewModel (hard delete)
│   ├── Implement replyToMessageId propagation in mapper
│   └── Add ActivityMessageMapper capability() usage tests
│
└── Day 3: UI Integration
    ├── Create ContextMenu composable (reusable)
    ├── Add long-press modifier to ActivityMessageCard
    ├── Wire composer to EDIT mode (pre-fill from message)
    ├── Wire composer to REPLY mode (show reply context)
    └── Wire delete confirmation dialog
```

### No Migration Required for Hard Delete + Edit + Reply

All three can be implemented without DB migration:
- Edit → overwrites existing row (`@Update`)
- Hard Delete → removes row (`@Delete`)
- Reply → payload stored in JSON (`description` field)

Soft delete requires migration, but hard delete works immediately as a fallback.

---

## 9. Test Plan for Phase 5

### Unit Tests (Mapper + Reducer)

```kotlin
// ActivityMessageMapperTest
fun `edit preserves message id`() { }
fun `replyToMessageId survives mapping to model`() { }
fun `capability() returns canReply=true by default`() { }
fun `deleted messages are filtered in messages()`() { }

// ActivityMessageReducerTest (new)
fun `edit action returns EDITING state with message`() { }
fun `reply action sets replyingToMessageId`() { }
fun `delete action sets pendingDeleteMessageId`() { }
fun `cancel interaction resets state`() { }

// ActivityPayloadCodecTest
fun `replyToMessageId encodes and decodes in JSON`() { }
fun `empty payload without replyToMessageId has null reference`() { }
```

---

## 10. Summary Decision Table

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Edit flow | **Option A** (reuse Unified Composer) | Less code, consistent UX, Telegram-aligned |
| Delete strategy | **Soft delete** (defer DB migration) | AI analysis benefit, rollback capability |
| Reply model | **Reference-based** (not thread tree) | Simple, sufficient for current scope |
| Context menu | Capability-driven, long-press | Clean, role-based, 3 actions |
| Interaction state | In ViewModel | Lifecycle-aware, testable, scoped |
| Edit DB | `@Update` (no migration) | Works with existing schema |
| Reply DB | JSON payload (no migration) | Stored in `description` field |
| Delete DB | Hard DELETE for now (no migration) | Soft delete needs migration → Phase 5 deferral |

---

## Appendix: Current Field Mapping

```
ActivityMessageModel (Phase 4.14 state)
├── id: Long                    ← activity_events.id
├── taskId: Long                ← activity_events.taskId
├── stepId: Long?               ← activity_events.stepId
├── text: String?               ← extracted from payload/description
├── attachments: List           ← extracted from payload/description
├── durationMinutes: Int?       ← extracted from payload/description
├── createdAt: Long             ← activity_events.timestamp
├── canEdit: Boolean = true     ← capability (not persisted)
├── canDelete: Boolean = false  ← capability (not persisted)
├── isDeleted: Boolean = false  ← capability (not persisted yet)
└── replyToMessageId: Long?     ← in payload JSON (Phase 5)
```

### What's Missing (to be added in Phase 5)

```
ActivityEventEntity (Phase 5 additions)
├── No new columns needed for hard delete + edit + reply
├── replyToMessageId → stored in JSON payload ✅
├── isDeleted → needs column for soft delete (deferred)
└── edit history → needs separate table or JSON array (Phase 6)
```

---

*Review completed. No code changes made. Implementation plan defined for subsequent phase.*

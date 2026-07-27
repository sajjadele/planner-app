# Activity Interaction Readiness Report

## Executive Summary

**Overall readiness: NEEDS SMALL FIXES**

The architecture is thoughtfully designed with clear separation of concerns, well-defined domain models, and a solid foundation for Phase 5. The model layer (`ActivityMessageModel`, `ActivityMessageCapability`, `ActivityMessageAction`) is fully ready. The payload layer is backward-compatible and extensible.

However, **three concrete gaps prevent starting Phase 5 safely**:
1. **DAO lacks `update` and single-entity `delete` queries** — the database layer cannot persist edits or deletions.
2. **`ActivityMessageMapper` hardcodes interaction flags** — `canEdit`, `canDelete`, `isDeleted`, and `replyToMessageId` are all set to static values; no real data flows through.
3. **Composer has no EDIT or REPLY mode** — `ComposerMode` only has `ACTIVITY` and `STEP`; state, actions, and reducer need extension.

These are small, contained changes — not architectural rewrites.

---

## Architecture Score

| Layer       | Score | Notes |
|-------------|-------|-------|
| Database    | 4/10  | Missing `update`, `delete(id)`, `soft-delete`, `replyTo` column |
| Payload     | 8/10  | Extensible JSON; backward-compatible codec; missing `replyToMessageId` field |
| Mapper      | 6/10  | Clean mapping logic; hardcoded interaction flags block real functionality |
| Composer    | 4/10  | No EDIT/REPLY mode; state/actions/reducer need extension; bottom sheet needs new callback |
| UI          | 8/10  | Components are clean and composable; context menu slot is trivial to add |
| **Overall** | **6/10** | Small, well-scoped gaps; no fundamental architectural issues |

---

## Blocking Issues

### B1. DAO Missing `update` and single-entity `delete`

**File:** `ActivityEventDao.kt`

The DAO currently supports:
- `insert` ✓
- `observeByTaskId` ✓ (returns `Flow`)
- `deleteByTaskId` (bulk only) ✓
- `findLatestEvent` ✓
- `getEventsByStepId` ✓

It does **not** support:
- `suspend fun update(event: ActivityEventEntity)` — needed for saving edited description
- `suspend fun deleteById(id: Int)` — needed for single-message deletion
- `suspend fun softDeleteById(id: Int)` — needed for soft-delete (optional but recommended)
- `observeById(id: Int): Flow<ActivityEventEntity?>` — needed for reactive edit updates

**Impact:** Cannot persist message edits or deletions. Phase 5 cannot function without these.

**Effort:** ~20 lines of DAO + ~10 lines in Repository.

---

### B2. `ActivityMessageMapper` hardcodes interaction flags

**File:** `ActivityMessageMapper.kt`, lines 84-87

```kotlin
return ActivityMessageModel(
    ...
    canEdit = true,
    canDelete = false,
    isDeleted = false,
    replyToMessageId = null
)
```

Every mapped message gets the same static values. There is no mechanism to vary these per-message. This means:
- `canDelete` is always `false` — the UI can never show a delete option.
- `isDeleted` is always `false` — soft-deleted messages still appear as normal.
- `replyToMessageId` is always `null` — reply threading is invisible.

**Root cause:** The entity does not store these fields. The mapper has no data to read from.

**Fix needed:** Add columns to `ActivityEventEntity` and wire them through the mapper.

**Effort:** DB migration + entity change + mapper wiring.

---

### B3. Composer has no EDIT or REPLY mode

**Files:**
- `ComposerMode.kt` — only `ACTIVITY` and `STEP`
- `ActivityComposerState.kt` — no `editingMessageId`, `replyingToMessageId`, or `originalMessage` fields
- `ActivityComposerAction.kt` — no `SetMode(ComposerMode.Edit)`, `LoadExistingContent(message)`
- `ActivityComposerReducer.kt` — cannot transition to edit/reply state
- `ActivityComposerBottomSheet.kt` — no `onUpdateActivity` callback

To support:
- **EDIT mode**: populate composer text, attachments, duration from existing message; submit triggers `onUpdateActivity` instead of `onCreateActivity`
- **REPLY mode**: show quoted message preview above the text input; submit creates a new activity with `replyToMessageId` set

**Impact:** Without these, users cannot edit or reply to messages through the composer — the primary interaction surface.

**Effort:** New enum values + state fields + actions + reducer cases + bottom sheet callback.

---

## Non-blocking Improvements

### N1. Add `editedAt` column to `ActivityEventEntity`

Future edit tracking needs a way to know if a message was modified. An `editedAt: Long?` column (null = never edited) is the simplest approach. Not blocking because Phase 5 can start without edit history.

### N2. Add `editHistory` JSON field (optional)

For full version history, store previous payloads as a JSON array. Low priority; can be added in Phase 6.

### N3. `eventType` as String is fragile

Current `ActivityEventType.valueOf(raw)` can throw if the stored string doesn't match an enum name. Consider using `@TypeConverter` to store the enum as an int index or use a stable string mapping. Not blocking because existing data is consistent.

### N4. `ActivityMessageCard` needs context menu slot

Currently `ActivityMessageCard` has no `onLongClick` or action callback. To support context menus, add `onAction: (ActivityMessageAction) -> Unit` parameter. Cleanly additive (default no-op) — not blocking Phase 5 design.

### N5. `StepCard` and `TimelineBottomSheet` need action propagation

Both render `ActivityMessageCard` but don't pass action callbacks. Once `ActivityMessageCard` supports onAction, these need to propagate it. Trivial wiring.

### N6. ActivityDraftResolver should handle edit encoding

When updating an existing message, the resolver should produce the same event type as the original message (not auto-detect based on content). Currently `resolveEventType` infers from presence of `durationMinutes`. Not blocking but needs attention during edit implementation.

---

## Recommended Phase 5 Changes

### Phase 5.1 — Database: Add update and delete support

Files:
- `ActivityEventDao.kt` — add `update(entity)`, `deleteById(id)`, `softDeleteById(id)`, `observeById(id)`
- `ActivityEventRepository.kt` — expose new DAO methods
- `ActivityEventEntity.kt` — add `isDeleted: Boolean = false`, `replyToMessageId: Int? = null`, `editedAt: Long? = null`
- `AppDatabase.kt` — bump version, add migration (ALTER TABLE)

Dependencies: None.

---

### Phase 5.2 — Mapper: Wire real flags from entity

Files:
- `ActivityMessageMapper.kt` — read `isDeleted`, `replyToMessageId` from entity; compute `canEdit`/`canDelete` from these
- `ActivityPayload.kt` — add `replyToMessageId: Long? = null` field
- `ActivityPayloadCodec.kt` — encode/decode `replyToMessageId` in JSON

Dependencies: Phase 5.1 (entity columns must exist).

---

### Phase 5.3 — Composer: Add EDIT and REPLY modes

Files:
- `ComposerMode.kt` — add `EDIT`, `REPLY` enum values
- `ActivityComposerState.kt` — add `originalMessageId: Long?`, `replyingToMessageId: Long?`, `originalText: String?`, `originalAttachments: List<ActivityAttachment>`
- `ActivityComposerAction.kt` — add `StartEdit(message: ActivityMessageModel)`, `StartReply(messageId: Long)`, `CancelEdit`
- `ActivityComposerReducer.kt` — handle new actions (populate state from existing message)
- `ActivityComposerBottomSheet.kt` — add `onUpdateActivity: (ActivityDraft) -> Unit` callback; conditionally show "ذخیره" vs "ثبت"

Dependencies: Phase 5.1, 5.2.

---

### Phase 5.4 — UI: Add context menu to ActivityMessageCard

Files:
- `ActivityMessageCard.kt` — add `onAction: ((ActivityMessageAction) -> Unit)? = null` parameter; wire `combinedClickable` for long press; show `DropdownMenu` with Edit/Delete/Reply options based on `message.capability()`
- `StepCard.kt` — propagate `onAction` to `ActivityMessageCard`
- `TimelineBottomSheet.kt` — propagate `onAction` to `ActivityMessageCard`

Dependencies: Phase 5.2 (capability must return real values).

---

### Phase 5.5 — ViewModel: Wire interaction actions

Files:
- `TaskDetailViewModel.kt` — add `editActivity(messageId, draft)`, `deleteActivity(messageId)`, `replyToActivity(messageId, draft)` methods
- Connect UI actions to ViewModel methods via callbacks

Dependencies: Phase 5.1, 5.2, 5.4.

---

## Final Decision

**Can we start Phase 5 implementation safely?**

**YES — with the following caveats:**

1. The first 5.1 sprint must be DB work (DAO + entity migration) before any UI work.
2. The mapper hardcoding (B2) means any UI built before 5.2 will see `canDelete=false` and no `replyToMessageId` — design the UI defensively using capability checks that will work once real values flow.
3. The composer (5.3) and UI (5.4) work can be done in parallel once 5.1 and 5.2 are complete.

**Do NOT start UI work before DAO/entity changes are done.** The database layer is the single true blocking dependency. Everything else is additive.

Risk is LOW — all changes are contained within the `plugins/planner` module, backward-compatible (additive only), and follow existing architectural patterns.

# Phase 4.16 — Activity Interaction Foundation Report

## Executive Summary

**Status: READY for Phase 5.**

All 6 infrastructure tasks completed. The pipeline now supports Edit/Delete/Reply at the data, capability, and composer levels. Phase 5 can safely implement UI without further infrastructure changes.

---

## 1. Current Architecture Status

### Pipeline (after Phase 4.16)

```
ActivityDraft (text, attachments, durationMinutes)
    ↓  ActivityDraftResolver
ActivityEventEntity → Room DB (description = JSON or legacy string)
    ↓  ActivityPayloadCodec.decode
ActivityPayload (text, attachments, durationMinutes, replyToMessageId)
    ↓  ActivityMessageMapper.toMessage
ActivityMessageModel (id, text, attachments, canEdit, canDelete, replyToMessageId, ...)
    ↓  .capability()
ActivityMessageCapability (canEdit, canDelete, canReply — derived from model state)
    ↓
TimelineEventMapper / StepCardMapper → UI
```

### What changed

| Layer | Before | After |
|-------|--------|-------|
| ActivityPayload | 3 fields | 4 fields (+ `replyToMessageId`) |
| ActivityPayloadCodec | JSON mocked in tests | Real JSON in tests via `org.json:json` |
| ActivityEventDao | insert, query, bulk-delete | + `update`, `deleteById`, `observeById` |
| ActivityEventRepository | addEvent, findLatest | + `updateEvent`, `deleteEvent`, `observeById` |
| ActivityMessageMapper | hardcoded flags | `canDelete=true`, reads `replyToMessageId` from payload |
| ActivityMessageCapability | `DEFAULT` had `canDelete=false` | `DEFAULT` = all true |
| ComposerMode | ACTIVITY, STEP | + EDIT, REPLY |
| ActivityComposerState | text, attachments, duration, mode | + `existingMessageId`, `replyToMessageId` |
| ActivityComposerAction | 7 actions | 10 actions (+ `StartEdit`, `StartReply`, `CancelInteraction`) |
| ActivityComposerReducer | 7 branches | 10 branches |

### Test infrastructure

`org.json:json:20210307` added as `testImplementation` dependency. All JSON encode/decode can now be validated in unit tests.

---

## 2. What Is Ready

### Fully tested: 67 passing tests

| Test suite | Tests | Status |
|------------|-------|--------|
| ActivityPayloadRoundTripTest | 11 | ✅ All pass |
| ActivityPayloadCodecTest | 14 | ✅ All pass |
| ActivityMessageMapperTest | 19 | ✅ All pass |
| ActivityComposerReducerTest | 22 | ✅ All pass (incl. EDIT/REPLY) |
| ActivityInteractionContractTest | 28 | ✅ All pass |
| ActivityInteractionFoundationTest | 25 | ✅ All pass |
| StepCardMapperTest | — | ✅ Compiles and passes |

### Pre-existing (unrelated) failures: 6

All are Robolectric/Room/Snapshot tests unrelated to the activity pipeline.

### Ready for Phase 5

- **Edit**: DAO has `update`, mapper extracts payload from JSON, composer has EDIT mode
- **Delete**: DAO has `deleteById`, model has `canDelete=true`, capability disables on `isDeleted`
- **Reply**: `replyToMessageId` in ActivityPayload + codec + mapper + composer REPLY mode

---

## 3. Changed Files

### Production code (10 files)

| File | Change |
|------|--------|
| `app/build.gradle.kts` | Added `testImplementation("org.json:json:20210307")` |
| `data/ActivityPayload.kt` | Added `replyToMessageId: Long? = null` |
| `data/ActivityPayloadCodec.kt` | Encode/decode `replyToMessageId`, updated empty check |
| `data/ActivityEventDao.kt` | Added `update`, `deleteById`, `observeById` |
| `data/ActivityEventRepository.kt` | Added `updateEvent`, `deleteEvent`, `observeById` |
| `data/ActivityMessageMapper.kt` | `canDelete=true`, extracts `replyToMessageId` from payload |
| `data/ActivityMessageCapability.kt` | `DEFAULT` = all true |
| `ui/TimelineEventMapper.kt` | Fixed `objectText = decoded.text` (no `?: ""`) |
| `ui/composer/ComposerMode.kt` | Added `EDIT`, `REPLY` |
| `ui/composer/ActivityComposerState.kt` | Added `existingMessageId`, `replyToMessageId`, helper methods |
| `ui/composer/ActivityComposerAction.kt` | Added `StartEdit`, `StartReply`, `CancelInteraction` |
| `ui/composer/ActivityComposerReducer.kt` | Handles EDIT/REPLY/cancel transitions |

### Test code (5 files)

| File | Change |
|------|--------|
| `data/ActivityPayloadRoundTripTest.kt` | Added reply-to round-trip tests |
| `data/ActivityMessageMapperTest.kt` | Updated `canDelete=false` → `true`, added `assertTrue` |
| `data/ActivityInteractionContractTest.kt` | Updated to reflect `canDelete=true` capability |
| `data/ActivityPayloadCodecTest.kt` | Fixed malformed JSON test assertion |
| `data/ActivityInteractionFoundationTest.kt` | NEW: 25 tests across identity, payload, capability, DAO, composer |
| `ui/composer/ActivityComposerReducerTest.kt` | Added 8 EDIT/REPLY mode tests |

---

## 4. Remaining Risks

### Low: No database migration

- `ActivityEventEntity` still has no `isDeleted`, `replyToMessageId`, or `editedAt` columns.
- `deleteById` and `update` work on existing columns.
- Reply references are stored in JSON payload (`ActivityPayload.replyToMessageId`), not as a column.
- Soft delete (`isDeleted`) not implemented in DB — requires migration.
- Decision: JSON storage is sufficient for Phase 5. Schema changes deferred.

### Low: Composer pre-population

- `StartEdit` sets EDIT mode with `existingMessageId` but does not pre-populate composer fields.
- Phase 5 ViewModel must load the existing message and dispatch `TextChanged`/`AddAttachment`/`DurationChanged`.
- State has the `existingMessageId` — wiring is a Phase 5 task.

### Medium (pre-existing): TimelineEventMapper IMAGE_ADDED fix

- `objectText = decoded.text ?: ""` → `objectText = decoded.text`
- Bug was dormant with mocked JSONObject (decode always fell back to legacy).
- Now fixed as part of this phase.

---

## 5. What Phase 5 Can Safely Implement

### Edit message

```
UI long-press → ActivityMessageAction.Edit(messageId)
    → ViewModel finds existing ActivityMessageModel
    → dispatches StartEdit(messageId) + pre-populates composer
    → user modifies text/attachments/duration
    → ViewModel calls repo.updateEvent(entity.withUpdatedDescription())
```

Prerequisites done: `DAO.update`, `ComposerMode.EDIT`, state support.

### Delete message

```
UI long-press → ActivityMessageAction.Delete(messageId)
    → ViewModel calls repo.deleteEvent(messageId)
    → Flow emits updated list → message disappears
```

Prerequisites done: `DAO.deleteById`, `model.canDelete=true`.

### Reply to message

```
UI long-press → ActivityMessageAction.Reply(messageId)
    → dispatches StartReply(messageId)
    → user types reply
    → ViewModel creates ActivityPayload with replyToMessageId
    → repo.insert(entity)
```

Prerequisites done: `ActivityPayload.replyToMessageId`, `ComposerMode.REPLY`, codec encode/decode.

---

## 6. Verification

```
./gradlew test        → BUILD SUCCESSFUL (437 tests, 6 pre-existing failures)
./gradlew assembleDebug → BUILD SUCCESSFUL
```

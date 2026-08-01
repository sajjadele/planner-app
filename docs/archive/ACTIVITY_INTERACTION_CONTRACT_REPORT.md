# Activity Interaction Contract Report

## Executive Summary

**Status: VALIDATED — with critical gaps discovered.**

The pipeline architecture is sound at the design level. However, three significant contract gaps were discovered during validation. One infrastructure issue (`org.json.JSONObject` mocking) prevents comprehensive unit testing of the JSON payload path. Two design gaps (hardcoded mapper flags and no DAO update/delete) block real Edit/Delete/Reply functionality.

---

## Current Architecture Status

### Pipeline Flow

```
ActivityDraft
    ↓  ActivityDraftResolver.resolveEventType + encodeDescription
ActivityEventEntity (.description = JSON string or plain text)
    ↓  ActivityPayloadCodec.decode
ActivityPayload (text, attachments, duration)
    ↓  ActivityMessageMapper.toMessage (combines payload + entity metadata)
ActivityMessageModel (id, text, attachments, capability flags)
    ↓  ActivityMessageCard / StepCard / Timeline UI
```

### Pipeline Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        CREATE FLOW                              │
│                                                                 │
│  User Input                                                     │
│     ↓                                                           │
│  ActivityComposerState                                          │
│     ↓  toActivityDraft()                                        │
│  ActivityDraft (text, attachments, durationMinutes)             │
│     ↓  ActivityDraftResolver                                    │
│  ┌─ resolveEventType → NOTE_ADDED / MANUAL_ACTIVITY             │
│  └─ encodeDescription → "plain" / "title|dur" / JSON           │
│     ↓                                                           │
│  ActivityEventEntity (DB insert)                                │
│     ↓                                                           │
│  ActivityPayloadCodec.decode(description)                       │
│     ↓                                                           │
│  ActivityPayload + ActivityMessageMapper                         │
│     ↓                                                           │
│  ActivityMessageModel → UI                                       │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                         READ FLOW                               │
│                                                                 │
│  Room DB → ActivityEventDao.observeByTaskId()                   │
│     ↓  Flow<List<ActivityEventEntity>>                          │
│  ActivityMessageMapper.toMessages()                             │
│     ↓  filters system events, decodes payloads                  │
│  List<ActivityMessageModel>                                     │
│     ↓                                                           │
│  StepCardMapper.groupActivitiesByStep()                         │
│     ↓                                                           │
│  StepCardModel (per step) + Timeline (all messages)             │
└─────────────────────────────────────────────────────────────────┘
```

---

## Passing Tests

The following contract validations PASS in the current codebase:

### Identity Preservation (6 tests)
- Entity `id` → Model `id`: 1:1 preserved
- Entity `taskId` → Model `taskId`: preserved
- Entity `stepId` → Model `stepId`: preserved (including null)
- Entity `timestamp` → Model `createdAt`: preserved
- Int → Long widening: safe (no overflow)

### Step Relation (6 tests)
- Messages correctly filterable by `stepId`
- Task-level messages (`stepId = null`) isolated from step queries
- `StepCardMapper.groupActivitiesByStep` correctly groups by `stepId`
- `StepCardMapper.groupActivitiesByStep` filters null-stepId events
- `StepCardMapper.toCardModel` assigns correct messages to each step

### System Event Filtering (2 tests)
- All system events (STEP_CREATED, STEP_COMPLETED, STEP_REOPENED, STEP_DELETED) return null from mapper
- All user events (NOTE_ADDED, IMAGE_ADDED, FILE_ADDED, MANUAL_ACTIVITY) pass through

### Capability Model (8 tests)
- `capability()` correctly derives `ActivityMessageCapability` from model fields
- `FULL`, `READ_ONLY`, `REPLY_ONLY`, `DEFAULT` presets all work
- Deleted message correctly disables all interactions
- Capability has no external dependencies

### Legacy Format Compatibility (3 tests)
- Plain text: `NOTE_ADDED` with "Hello" → `text = "Hello"`
- Triple-colon: `IMAGE_ADDED` with "uri:::desc" → `text = "desc"`, 1 attachment
- Pipe format: `MANUAL_ACTIVITY` with "title|60" → `text = "title"`, `durationMinutes = 60`

### ActivityDraftResolver (3 tests)
- `draft.durationMinutes != null` → `MANUAL_ACTIVITY` ✓
- `draft.durationMinutes == null` → `NOTE_ADDED` ✓
- Attachments are ignored for event type resolution (determined solely by duration)

**Total: 28 passing validation tests.**

---

## Missing Pieces

### M1. ActivityMessageMapper cannot decode JSON payloads

**Status: BROKEN**

`ActivityMessageMapper.toMessage` calls `ActivityPayloadCodec.decode(entity.description)` to parse JSON payloads. However, the `org.json.JSONObject(String)` constructor used inside `decodeJsonObject` throws in the Android unit test environment (method not mocked). Consequently:

1. **All JSON descriptions return raw JSON as text.** The mapper falls through to `decodeLegacyFormat`, which doesn't recognize JSON and returns the raw JSON string as plain text.
2. **Attachments in JSON are never extracted.** `payload?.attachments` returns empty because `decode` returns an `ActivityPayload` created by legacy fallback (text = full JSON, no attachments).
3. **Duration in JSON is never extracted.** Same reason.
4. **`ActivityPayloadRoundTripTest` is effectively broken.** It passes `encode` output through `decode`, but `decode` returns wrong results. The tests "pass" only because the encoded JSON contains the original text somewhere in the string, so string assertions happen to match.

**Impact:** Critical for Phase 5. JSON payloads are the primary format for rich activities (images, files, mixed attachments). Without working `decode`, all activities created with the unified composer will fail to render properly when read back from the database.

**Root cause:** The `decodeJsonObject` method in `ActivityPayloadCodec` uses `org.json.JSONObject` which is a mocked Android API in unit tests. On a real device, this works correctly.

**Recommendation:** Either:
- Add `testImplementation 'org.json:json:20210307'` to build.gradle (provides a real JSONObject for tests), OR
- Add `testOptions { unitTests.returnDefaultValues = true }` to make mocked methods return defaults instead of throwing, OR
- Replace `org.json.JSONObject` with a JVM-compatible JSON library (kotlinx.serialization, Gson, Moshi) for better testability.

---

### M2. DAO and Entity lack Edit/Delete/Reply support

**Status: NOT IMPLEMENTED**

Database layer analysis:

| Operation | Supported | Details |
|-----------|-----------|---------|
| Insert | ✅ | `DAO.insert()` |
| Read by taskId | ✅ | `DAO.observeByTaskId()` (Flow) |
| Read by stepId | ✅ | `DAO.getEventsByStepId()` |
| Update single entity | ❌ | No `DAO.update(event)` method |
| Delete single entity | ❌ | Only `DAO.deleteByTaskId(taskId)` — bulk only |
| Soft delete | ❌ | No `isDeleted` column in entity |
| Delete by ID | ❌ | No `DAO.deleteById(id)` method |
| Reply threading | ❌ | No `replyToMessageId` column |
| Edit tracking | ❌ | No `editedAt` column |
| Observe by ID | ❌ | No `observeById(id): Flow<ActivityEventEntity?>` |

**Impact:** Without `update` and single-entity `delete`, Phase 5 Edit/Delete operations cannot persist to the database. Without `isDeleted`, soft-delete cannot be implemented. Without `replyToMessageId`, reply threading is invisible.

**Recommendation:** Add to `ActivityEventEntity`:
- `isDeleted: Boolean = false`
- `replyToMessageId: Int? = null`
- `editedAt: Long? = null`

Add to `ActivityEventDao`:
- `suspend fun update(event: ActivityEventEntity)`
- `suspend fun deleteById(id: Int)`
- `suspend fun softDeleteById(id: Int)`
- `fun observeById(id: Int): Flow<ActivityEventEntity?>`

---

### M3. Mapper interaction flags are hardcoded

**Status: PLACEHOLDER**

`ActivityMessageMapper.toMessage()` at lines 84-87:

```kotlin
return ActivityMessageModel(
    ...
    canEdit = true,
    canDelete = false,
    isDeleted = false,
    replyToMessageId = null
)
```

Every flag is static. The mapper reads nothing from the entity because the entity has no corresponding columns.

**Impact:** 
- Can never show delete UI (`canDelete` is always false)
- Can never show reply threading (`replyToMessageId` is always null)
- Soft-deleted messages always appear as normal (`isDeleted` is always false)

**Recommendation:** After adding columns to `ActivityEventEntity` (M2), wire them through the mapper.

---

### M4. Composer has no EDIT or REPLY mode

**Status: PLACEHOLDER**

| Composer Feature | Supported | Details |
|-----------------|-----------|---------|
| ACTIVITY mode | ✅ | Current default |
| STEP mode | ✅ | Fully implemented |
| EDIT mode | ❌ | No `ComposerMode.EDIT` |
| REPLY mode | ❌ | No `ComposerMode.REPLY` |
| Edit state | ❌ | No `editingMessageId` in `ActivityComposerState` |
| Reply state | ❌ | No `replyingToMessageId` |
| Edit action | ❌ | No `ActivityComposerAction.StartEdit` |
| Reply action | ❌ | No `ActivityComposerAction.StartReply` |
| Update callback | ❌ | No `onUpdateActivity` in `ActivityComposerBottomSheet` |

**Impact:** Cannot support Phase 5 interaction. The composer is the primary interaction surface for creating, editing, and replying to messages.

**Recommendation:** Add `EDIT` and `REPLY` to `ComposerMode`, add state fields and actions, extend the reducer, and add an `onUpdateActivity` callback.

---

## Risks

### R1. JSON payload testability gap (HIGH)

The entire JSON payload path (`ActivityPayloadCodec.encode` + `decode`) cannot be validated in unit tests. Bugs in JSON encoding/decoding will only surface on real devices or in instrumentation tests. This is especially dangerous because:

- The `encode` path for attachments uses JSON
- The `decode` path for stored activities reads JSON
- If encoding and decoding are inconsistent, stored data becomes corrupted

**Mitigation:** Add `testImplementation 'org.json:json:20210307'` to build.gradle or add instrumentation tests.

### R2. ActivityDraftResolver event type mapped by duration only (LOW)

`ActivityDraftResolver.resolveEventType` uses `durationMinutes != null` to choose between `MANUAL_ACTIVITY` and `NOTE_ADDED`. Attachments are ignored. This means:

- An image-only activity (no text, no duration) → `NOTE_ADDED`
- A manual activity WITH an image AND duration → `MANUAL_ACTIVITY`

This is consistent but could be confusing. In future, consider a unified `ACTIVITY_CREATED` event type to simplify.

### R3. `ActivityDraftResolver.encodeDescription` path inconsistency (LOW)

The encoding strategy:
- Attachments present → JSON format (via `ActivityPayloadCodec.encode`)
- Duration present, no attachments → legacy `"title|duration"` format
- Text only → plain text

This means the mapper must handle three formats. JSON is the only format that supports all fields simultaneously. Consider unifying to JSON-only after Phase 5.

### R4. `FILE_ADDED` event type in mapper (LOW)

`ActivityMessageMapper` treats `FILE_ADDED` the same as `NOTE_ADDED` in `extractText`. This is correct but `FILE_ADDED` is currently only emitted indirectly — activities with file attachments are stored as `NOTE_ADDED` or `MANUAL_ACTIVITY` with a JSON payload that includes file attachments. There is no scenario that creates a `FILE_ADDED` event with the current code.

---

## Recommended Phase 5 Order

### Phase 5.0 — Infrastructure (BEFORE any interaction code)

1. **Fix JSONObject testability** — Add `testImplementation 'org.json:json:20210307'` to `app/build.gradle`
2. **Fix ActivityPayloadRoundTripTest** — Verify JSON encode/decode round trip after JSONObject is real
3. **Fix ActivityMessageMapperTest** — JSON payload tests will pass once JSONObject works
4. **Fix ActivityPayloadCodecTest** — Add comprehensive JSON format validation

### Phase 5.1 — Database Layer

1. Add columns to `ActivityEventEntity`: `isDeleted`, `replyToMessageId`, `editedAt`
2. Add to `ActivityEventDao`: `update`, `deleteById`, `softDeleteById`, `observeById`
3. Add to `ActivityEventRepository`: expose new DAO methods
4. Bump database version, add migration
5. Wire new entity fields through `ActivityMessageMapper`

### Phase 5.2 — Composer Extension

1. Add `EDIT`, `REPLY` to `ComposerMode`
2. Add `originalMessageId`, `replyingToMessageId` to `ActivityComposerState`
3. Add `StartEdit`, `StartReply`, `CancelEdit` to `ActivityComposerAction`
4. Handle new actions in `ActivityComposerReducer`
5. Add `onUpdateActivity` callback to `ActivityComposerBottomSheet`
6. Update `UnifiedComposerContent` for edit/reply mode

### Phase 5.3 — UI Context Menu

1. Add `onAction: ((ActivityMessageAction) -> Unit)?` to `ActivityMessageCard`
2. Wire `combinedClickable` for long press
3. Show `DropdownMenu` with Edit/Delete/Reply based on `message.capability()`
4. Propagate `onAction` through `StepCard` and `TimelineBottomSheet`

### Phase 5.4 — ViewModel Wiring

1. Add `editActivity(messageId, draft)` method
2. Add `deleteActivity(messageId)` method
3. Add `replyToActivity(messageId, draft)` method
4. Connect UI action callbacks to ViewModel methods

---

## Architectural Grade Summary

| Area | Grade | Notes |
|------|-------|-------|
| Identity model | A | Entity ID → Model ID: 1:1, type-safe |
| Step relations | A | Correct grouping, filtering, isolation |
| Capability model | A | Clean sealed class, derived from state |
| System event filtering | A | All 4 system events filtered correctly |
| Legacy format support | A | Plain text, pipe, triple-colon all work |
| Action model | A | Clean sealed class with Edit/Delete/Reply |
| Payload codec design | B | Good design, untestable in unit tests |
| Mapper integration | D | JSON decode broken in unit tests, flags hardcoded |
| DAO completeness | F | Missing update, single delete, soft delete |
| Composer completeness | F | No EDIT/REPLY mode |
| **Overall** | **C+** | Good foundation, critical gaps in DB + mapper + testability |

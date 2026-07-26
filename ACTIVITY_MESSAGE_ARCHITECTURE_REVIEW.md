# Activity Message Domain Refactor — Architecture Review

**Phase:** 4.12
**Date:** 2026-07-26
**Scope:** Activity event → Message domain layer separation

---

## Executive Summary

The current system conflates **system events** (STEP_CREATED, STEP_COMPLETED, etc.) with **user messages** (NOTE_ADDED, IMAGE_ADDED, MANUAL_ACTIVITY). This review identifies the problems and proposes a clean domain/UI separation layer per the Phase 4.12 specification.

**Key finding:** The `ActivityMessageMapper` exists but does NOT filter system events. STEP_CREATED and STEP_COMPLETED currently appear as visible messages in both the StepCard and TimelineBottomSheet. The UI model (`ActivityMessageModel`) also leaks internal fields (`isStep`, `isCompleted`, `eventTypeRaw`) that should never reach the presentation layer.

---

## 1. Current Architecture

### Data Flow

```
ActivityEventEntity (Room DB)
    │
    ├─────────────────────────────────────────────────────┐
    │                                                     │
    ▼                                                     ▼
ActivityMessageMapper                              TimelineEventMapper
    │                                                     │
    ▼                                                     ▼
ActivityMessageModel                              TimelineEventUiModel
    │                                                     │
    ▼                                                     ▼
StepCard ← StepCardMapper                      TimelineBottomSheet
```

### Key Files

| File | Role |
|------|------|
| `ActivityEventEntity.kt` | Room DB entity — `id, taskId, stepId?, eventType, description, timestamp` |
| `ActivityEventType.kt` | Enum: STEP_CREATED, STEP_COMPLETED, STEP_REOPENED, STEP_DELETED, NOTE_ADDED, FILE_ADDED, MANUAL_ACTIVITY, IMAGE_ADDED |
| `ActivityMessageModel.kt` | Existing UI model — but exposes `isStep`, `isCompleted`, `eventTypeRaw` |
| `ActivityMessageMapper.kt` | Existing mapper — maps ALL event types to messages, doesn't filter system events |
| `ActivityPayload.kt` | Rich content container — `text, attachments, durationMinutes` |
| `ActivityPayloadCodec.kt` | JSON encode/decode with legacy format fallback |
| `ActivityAttachment.kt` | Sealed class: `Image(uri)`, `File(uri, name?)` |
| `ActivityEventDao.kt` | Room DAO — observeByTaskId, getEventsByStepId, etc. |
| `ActivityEventRepository.kt` | Repository wrapping DAO |
| `StepCardModel.kt` | Step-level UI model — contains `messages: List<ActivityMessageModel>` |
| `StepCardMapper.kt` | Maps steps + activities to StepCardModel |
| `StepCard.kt` | Compose component — displays step with expandable messages |
| `ActivityMessageCard.kt` | Compose component — renders a single ActivityMessageModel |
| `TimelineBottomSheet.kt` | Date-scoped timeline — takes raw `List<ActivityEventEntity>` |
| `TimelineEventMapper.kt` | Maps events to TimelineEventUiModel — shows system events |
| `TimelineEventUiModel` | UI model for timeline items |
| `TaskDetailScreen.kt` | Screen that wires everything together |
| `TaskDetailViewModel.kt` | ViewModel — exposes `activities: StateFlow<List<ActivityEventEntity>>` |

---

## 2. Problems Identified

### Problem 1: System Events Visible in UI (HIGH)

`ActivityMessageMapper.toMessage()` maps ALL event types including STEP_CREATED, STEP_COMPLETED, STEP_REOPENED, STEP_DELETED. These system events appear as messages in StepCard and as timeline items in TimelineBottomSheet.

**Current behavior:**
```
StepCard("Design UI")
  ├── 📝 "Step created"          ← WRONG: system event visible
  ├── 📝 "Initial wireframe completed"  ← CORRECT: user message
  └── 📷 Screenshot              ← CORRECT
```

**Expected behavior:**
```
StepCard("Design UI")
  ├── 📝 "Initial wireframe completed"
  └── 📷 Screenshot
```

### Problem 2: UI Model Leaks Internal Fields (MEDIUM)

`ActivityMessageModel` exposes fields that are implementation details:
- `isStep: Boolean` — internal concept, never shown to user
- `isCompleted: Boolean` — internal concept
- `eventTypeRaw: String` — storage-level detail, should never reach UI

Per Phase 4.12 spec, the model should instead have:
- `taskId: Long`
- `stepId: Long?`
- `isEditable: Boolean`
- `isDeleted: Boolean`
- `replyToMessageId: Long?`

### Problem 3: Two Parallel Mapping Paths (MEDIUM)

There are two separate mappers serving two different use cases:
1. `ActivityMessageMapper` — for StepCard messages (currently includes system events)
2. `TimelineEventMapper` — for TimelineBottomSheet (currently includes system events)

Both map from `ActivityEventEntity` but produce different UI models. The duplication creates maintenance burden and inconsistency.

### Problem 4: TimelineBottomSheet Takes Raw Entities (MEDIUM)

`TimelineBottomSheet` receives `List<ActivityEventEntity>` directly and uses `TimelineEventMapper` internally. Per the Phase 4.12 spec, it should receive a pre-mapped stream of `ActivityMessageModel` and filter out system events there.

### Problem 5: `hasContent()` and `getSummary()` Leak Domain Logic (LOW)

`ActivityMessageModel.hasContent()` and `getSummary()` inject domain logic ("1 فایل پیوست") into the model. These belong in the mapper or a separate display formatter.

### Problem 6: JSON Leakage Risk (LOW-MEDIUM)

While `ActivityPayloadCodec.decode()` handles JSON parsing correctly, the `TimelineEventMapper` still extracts raw `eventType` and `description` fields for the fallback `null` case in `mapEvents`. If a new event type is added but not handled in the mapper, it falls through to displaying the raw `eventType` string as action text.

---

## 3. Proposed Changes

### 3.1 Redesign ActivityMessageModel

Replace the existing model with one that matches the Phase 4.12 spec:

```kotlin
data class ActivityMessageModel(
    val id: Long,
    val taskId: Long,
    val stepId: Long?,
    val text: String?,
    val attachments: List<ActivityAttachment>,
    val durationMinutes: Int?,
    val timestamp: Long,
    val isEditable: Boolean,
    val isDeleted: Boolean,
    val replyToMessageId: Long?
)
```

**Removed fields:**
- `isStep` — system concept
- `isCompleted` — system concept
- `eventTypeRaw` — storage leak
- `hasContent()` — domain logic in model
- `getSummary()` — domain logic in model
- `EMPTY` companion — not needed for data class

**New fields:**
- `taskId` — for future AI features that need task context
- `isEditable` — enables edit/delete UI interactions
- `isDeleted` — soft delete support
- `replyToMessageId` — enables reply threading (future)

### 3.2 Rewrite ActivityMessageMapper

New responsibilities:
1. Decode ActivityPayload JSON via ActivityPayloadCodec
2. Extract text, attachments, duration
3. Handle old/legacy formats
4. **Filter out system events** (STEP_CREATED, STEP_COMPLETED, STEP_REOPENED, STEP_DELETED)
5. Never expose raw JSON to UI
6. Fallback to "Unsupported message" if decoding fails entirely

```kotlin
object ActivityMessageMapper {
    // System events that should NEVER appear as messages
    private val SYSTEM_EVENTS = setOf(
        ActivityEventType.STEP_CREATED,
        ActivityEventType.STEP_COMPLETED,
        ActivityEventType.STEP_REOPENED,
        ActivityEventType.STEP_DELETED
    )

    fun toMessage(entity: ActivityEventEntity): ActivityMessageModel? {
        // Return null for system events — they are invisible to UI
        val eventType = parseEventType(entity.eventType) ?: return null
        if (eventType in SYSTEM_EVENTS) return null

        val payload = ActivityPayloadCodec.decode(entity.description)
        // ... extract text, attachments, duration
        return ActivityMessageModel(
            id = entity.id.toLong(),
            taskId = entity.taskId.toLong(),
            stepId = entity.stepId?.toLong(),
            text = text,
            attachments = attachments,
            durationMinutes = durationMinutes,
            timestamp = entity.timestamp,
            isEditable = true,
            isDeleted = false,
            replyToMessageId = null
        )
    }
}
```

Returning `ActivityMessageModel?` (nullable) cleanly filters system events. The caller can use `.filterNotNull()` or `.mapNotNull { toMessage(it) }`.

### 3.3 Update StepCardMapper

`StepCardMapper.groupActivitiesByStep()` should use the new mapper which filters system events. The StepCard already calls `ActivityMessageMapper.toMessage()` — this will now automatically exclude system events.

### 3.4 Update TimelineBottomSheet

`TimelineBottomSheet` should accept `List<ActivityMessageModel>` instead of `List<ActivityEventEntity>`. This means:
1. `TaskDetailViewModel` should expose pre-mapped `messages` flow
2. `TaskDetailScreen` maps entities before passing to TimelineBottomSheet
3. `TimelineEventMapper` is replaced by using `ActivityMessageModel` directly in the bottom sheet

### 3.5 Update TaskDetailViewModel

Expose mapped models instead of raw entities:
```kotlin
// Before
val activities: StateFlow<List<ActivityEventEntity>> = ...

// After — add pre-mapped stream
val activityMessages: StateFlow<List<ActivityMessageModel>> = activities
    .map { it.mapNotNull { ActivityMessageMapper.toMessage(it) } }
    .stateIn(...)
```

### 3.6 Update UI Components

- `StepCard.kt` — no changes needed, already uses `ActivityMessageModel.messages`
- `ActivityMessageCard.kt` — update icon/label logic: remove `isStep` branch, add `isEditable`/`isDeleted` indicators
- `TimelineBottomSheet.kt` — refactor to use `ActivityMessageModel` instead of `TimelineEventUiModel`

### 3.7 Tests

Update/replace `ActivityMessageMapperTest.kt` with spec-required test cases:
1. NOTE_ADDED → text message ✓
2. IMAGE_ADDED → image attachment extracted ✓
3. Multiple attachments ✓
4. MANUAL_ACTIVITY → duration extracted ✓
5. STEP_CREATED → filtered out (returns null) ✓
6. STEP_COMPLETED → filtered out (returns null) ✓
7. Old format compatibility ✓
8. Malformed JSON fallback ✓

---

## 4. Risks

| Risk | Level | Mitigation |
|------|-------|------------|
| TimelineBottomSheet behavior change | Medium | Keep TimelineEventMapper for now as fallback; timeline is a separate concern |
| STEP_CREATED event becoming invisible | High | STEP_CREATED is an internal audit event and should remain invisible per spec |
| isEditable always true for now | Low | Correct for current phase; edit/delete interactions come in Phase 5 |
| replyToMessageId always null | Low | Correct for current phase; threading comes in Phase 6 |

### Migration Plan

1. **No DB migration needed** — `ActivityEventEntity` stays unchanged
2. **Mapper change is additive** — mapping now returns nullable, callers use `filterNotNull`
3. **TimelineBottomSheet** — update gradually; keep old mapper temporarily
4. **Backward compatible** — old events with new mapper automatically filtered

---

## 5. Implementation Order

1. Redesign `ActivityMessageModel` (new fields, remove leaked fields)
2. Rewrite `ActivityMessageMapper` (add system event filtering)
3. Update `TaskDetailViewModel` (expose mapped message stream)
4. Update `TaskDetailScreen` (pass mapped models to components)
5. Update `ActivityMessageCard` (remove `isStep` branch logic)
6. Update `StepCardMapper.groupActivitiesByStep` (uses new mapper)
7. Rewrite `ActivityMessageMapperTest` (per spec test cases)
8. Update `TimelineBottomSheet` to use `ActivityMessageModel`

---

## 6. Verification Steps

After implementation:

1. `NOTE_ADDED` event → `ActivityMessageModel` with text content
2. `IMAGE_ADDED` event → `ActivityMessageModel` with Image attachment
3. `STEP_CREATED` event → `null` (filtered out)
4. `STEP_COMPLETED` event → `null` (filtered out)
5. `STEP_REOPENED` event → `null` (filtered out)
6. `STEP_DELETED` event → `null` (filtered out)
7. Malformed JSON → `ActivityMessageModel` with text or null
8. Legacy format → correctly decoded
9. No raw JSON exposed in UI
10. System events do not appear in StepCard messages
11. System events do not appear in TimelineBottomSheet

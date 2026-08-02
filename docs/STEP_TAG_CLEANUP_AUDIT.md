# Step → Tag Cleanup — Audit Report

> **Date:** 2026-08-02
> **Status:** Read-only audit — awaiting approval before implementation

---

## 1. Current State

### TaskStepEntity (table: `task_steps`)
```kotlin
data class TaskStepEntity(
    val id: Int = 0,
    val taskId: Int,
    val title: String,
    val colorHex: String? = null,
    val isCompleted: Boolean = false,    // ← REMNANT (Tag doesn't have completion)
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null        // ← REMNANT (Tag doesn't have completion)
)
```

### ActivityEventType
```kotlin
enum class ActivityEventType {
    STEP_CREATED,    // ← Used by CreateStepUseCase + TaskStepRepository
    STEP_COMPLETED,  // ← Used ONLY by debug ViewModels (ActivityLab/Scenario)
    STEP_REOPENED,   // ← Used ONLY by debug ViewModels (ActivityLab/Scenario)
    STEP_DELETED,    // ← Used by TaskDetailViewModel.deleteStep()
    NOTE_ADDED,
    FILE_ADDED,
    MANUAL_ACTIVITY,
    IMAGE_ADDED
}
```

### ActivityMessageMapper
- Filters out STEP_* events (invisible to UI) ✅
- System events never reach the user ✅

### Current DB Version: 16

---

## 2. Problems Found

### P1: `isCompleted` + `completedAt` on TaskStepEntity 🔴

**Problem:** Tags have completion state fields. A Tag cannot be "completed."

**Impact:** Dead code. Confusing for future developers. No UI uses these fields.

**Fix:** Drop columns via migration v16→v17.

---

### P2: STEP_CREATED event on tag creation 🔴

**Problem:** `CreateStepUseCase.execute()` and `TaskStepRepository.addStep()` both insert a `STEP_CREATED` ActivityEvent when creating a tag.

**Why it's wrong:** Tag creation is metadata management, not a user action. Activity timeline represents user behavior. Creating a tag should not pollute the activity feed.

**Impact:** Every tag creation adds a hidden system event. The event is filtered by ActivityMessageMapper so the user never sees it, but it wastes storage and query time.

**Fix:** Remove STEP_CREATED event insertion from both `CreateStepUseCase` and `TaskStepRepository.addStep()`.

---

### P3: STEP_DELETED event on tag deletion 🟡

**Problem:** `TaskDetailViewModel.deleteStep()` inserts a `STEP_DELETED` ActivityEvent before deleting the tag.

**Why it's wrong:** Same as P2 — tag deletion is metadata management, not a user action.

**Impact:** Hidden system event waste.

**Fix:** Remove STEP_DELETED event insertion from `deleteStep()`.

---

### P4: STEP_COMPLETED / STEP_REOPENED — debug-only 🟡

**Problem:** `STEP_COMPLETED` and `STEP_REOPENED` are defined in `ActivityEventType` but only used by debug ViewModels (`ActivityLabViewModel`, `ActivityScenarioViewModel`).

**Impact:** Dead enum values in production code.

**Fix:** Keep in ActivityEventType for debug tooling, but clearly mark as debug-only in comments. Do NOT remove — debug scenarios depend on them.

---

### P5: Naming remnants 🟢

**Problem:** Code still uses "Step" terminology in variable names, function names, and comments:
- `steps` StateFlow in TaskDetailViewModel
- `createStep()` method
- `addStep()` method (deprecated)
- `deleteStep()` method
- `StepDraft`, `StepDraftResolver`, `CreateStepUseCase`

**Impact:** Confusing for new developers. But renaming is risky — touches many files.

**Fix:** Rename only safe, low-risk items. Keep `TaskStepEntity` name (DB table is `task_steps` — renaming entity risks Room issues). Update comments and documentation to use "Tag" terminology.

---

## 3. Root Cause

The Step → Tag product decision was made in Phase 5.5, but the cleanup was incomplete:
1. Entity fields (`isCompleted`, `completedAt`) were never removed
2. System events (`STEP_CREATED`, `STEP_DELETED`) were never removed from creation/deletion flows
3. Naming was partially updated but not completely

---

## 4. Migration Risk

### Dropping `isCompleted` + `completedAt` from `task_steps`

**Risk: LOW**

- These columns are **never read** by production code (no query uses them)
- No UI displays completion state for tags
- `isCompleted` has a default (`false`) — all existing rows have `false`
- `completedAt` is nullable — all existing rows have `null`

**Migration:**
```sql
-- SQLite doesn't support DROP COLUMN on all API levels
-- Recreate table (same pattern as v6→v7 migration)
ALTER TABLE task_steps RENAME TO task_steps_old;
CREATE TABLE task_steps (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    taskId INTEGER NOT NULL,
    title TEXT NOT NULL,
    colorHex TEXT,
    createdAt INTEGER NOT NULL,
    FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE
);
INSERT INTO task_steps (id, taskId, title, colorHex, createdAt)
SELECT id, taskId, title, colorHex, createdAt FROM task_steps_old;
DROP TABLE task_steps_old;
CREATE INDEX IF NOT EXISTS index_task_steps_taskId ON task_steps(taskId);
```

**Data preserved:** id, taskId, title, colorHex, createdAt
**Data lost:** isCompleted (always false), completedAt (always null)

---

## 5. Files To Change

| File | Change | Risk |
|------|--------|------|
| `TaskStepEntity.kt` | Remove `isCompleted`, `completedAt` | Low |
| `AppDatabase.kt` | v16→v17 migration, version bump | Low |
| `CreateStepUseCase.kt` | Remove STEP_CREATED event insertion | Low |
| `TaskStepRepository.kt` | Remove STEP_CREATED event insertion from `addStep()` | Low |
| `TaskDetailViewModel.kt` | Remove STEP_DELETED event from `deleteStep()` | Low |
| `ActivityEventType.kt` | Add debug-only comments to STEP_* | None |
| `ActivityMessageMapper.kt` | Update comments (STEP_* are debug-only) | None |

---

## 6. Implementation Plan

### Phase A: Schema (v16→v17)
1. Update `TaskStepEntity` — remove `isCompleted`, `completedAt`
2. Add migration v16→v17 in `AppDatabase`
3. Bump version to 17

### Phase B: Remove System Events
1. `CreateStepUseCase.execute()` — remove STEP_CREATED insertion
2. `TaskStepRepository.addStep()` — remove STEP_CREATED insertion
3. `TaskDetailViewModel.deleteStep()` — remove STEP_DELETED insertion

### Phase C: Documentation
1. Update comments in `ActivityEventType` (STEP_* = debug-only)
2. Update comments in `ActivityMessageMapper`

### Phase D: Verify
1. `./gradlew test`
2. `./gradlew assembleDebug`
3. Manual verification checklist

---

*Awaiting approval before implementation.*

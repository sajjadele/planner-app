# Phase 5.5a — Step as Tag: UI Migration Report

> **Date:** July 2026  
> **Commit:** `71aecfe`  
> **Scope:** UI migration only — no files deleted, no DB changes, no architecture removal

---

## 1. Files Changed

| File | Status | Purpose |
|------|--------|---------|
| `ui/components/ActivityTagChip.kt` | **NEW** | Compact step name badge composable |
| `ui/components/ActivityMessageCard.kt` | **MODIFIED** | Added `stepName: String?` param, renders tag chip inline |
| `ui/TaskDetailScreen.kt` | **MODIFIED** | Fixed header count, added stepName lookup, passed to cards |
| `data/ActivityFeedTagMigrationTest.kt` | **NEW** | 18 tests |

## 2. What Changed in Rendering

### Before
```
┌──────────────────────┐
│ 📝 Research notes     │
│                       │
│ 10:30                │
└──────────────────────┘
```

### After (with step tag)
```
┌──────────────────────┐
│ 📝 Research notes     │
│                       │
│ [ 🟣 UI Design ]      │  ← ActivityTagChip
│ 10:30                │
└──────────────────────┘
```

### After (without step — unchanged)
```
┌──────────────────────┐
│ 📝 General note       │
│                       │
│ 10:30                │
└──────────────────────┘
```

## 3. What Was Fixed

### Problem 1: Header count showed total, not filtered
- **Before:** `ActivityFeedHeader(count = allMessages.size)` — always showed total count
- **After:** `ActivityFeedHeader(count = messages.size)` — shows count of currently visible messages

### Problem 2: Step had no visual identity in feed
- **Before:** stepId was invisible in the UI
- **After:** Each message with stepId shows the step name as a compact tag chip

### Problem 3: Step container confusion
- **Before:** StepCard could render messages inside a step (nested)
- **After:** Messages always render as standalone `ActivityMessageCard` — step is just a tag

## 4. What Was Preserved

✅ All existing interactions (Edit, Delete, Reply, ReplyNavigation, Attachment)
✅ ActivityComposerBottomSheet  
✅ StepCard (left untouched — not used in the rendering path)  
✅ StepCardMapper, StepDraft, CreateStepWithActivitiesUseCase, TimelineBottomSheet  
✅ Database schema (no migrations)  
✅ All existing tests  

## 5. Tests Added (18)

| Category | Tests | What They Verify |
|----------|-------|------------------|
| Tag resolution | 4 | stepId → stepName lookup, unknown stepId, multiple messages same tag |
| Header count | 4 | Shows filtered count, filter reduces count, zero when empty |
| Mixed messages | 4 | task-level + step-level coexist, filter by step, show all includes task-level |
| Migration safety | 2 | ActivityMessageModel has no nested messages, tag is compact |

## 6. Verification

- No files were deleted
- Database schema unchanged (version 14)
- No architecture refactoring
- StepCard still exists but is no longer part of the feed rendering path
- Step is now visually a tag, not a container

```bash
git pull
./gradlew assembleDebug
./gradlew test
```

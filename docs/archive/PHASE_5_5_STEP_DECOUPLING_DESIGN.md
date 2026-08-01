# Phase 5.5 — Step Decoupling Design Review

> **Date:** July 2026  
> **Status:** Analysis & Design Only  
> **Prerequisite:** Phase 5.4.1 (Quick Stabilization) complete

---

## 1. Step's Role in the Final Product

### Current Identity Crisis

After Phase 5.3, the codebase has two competing mental models for Step:

| Aspect | Container Model (Old) | Tag Model (New) |
|--------|----------------------|-----------------|
| **UI** | Overview tab: checkboxes, cards, nested activities | Feed filter: step chips in header |
| **Timeline** | Step events shown in TimelineBottomSheet | Step events filtered OUT by ActivityMessageMapper |
| **Filter** | N/A | `stepId` used as optional filter |
| **Data** | `StepCardModel` nests `List<ActivityMessageModel>` inside each step | `stepId` is a nullable column on `ActivityEventEntity` |
| **Creation** | `CreateStepWithActivitiesUseCase` creates step + activities atomically | Activities created independently, step is optional |
| **Completion** | Steps are TODO checkboxes | Steps don't need completion — they're labels |

### Recommendation: Step = Tag (Metadata)

**Rationale:**

The product philosophy is "Telegram Saved Messages + Personal Knowledge Memory." In Telegram Saved Messages:
- Messages are primary. You save a photo, a file, a note.
- Folders/tags organize them, but don't "contain" them.
- A message can belong to multiple categories (future).

The same applies to Vision Planner:
- Activities (notes, images, files, manual activities) are **primary**
- Steps are **optional tag/filter metadata**
- An activity without a step is **valid and common** — not an "orphan"
- Steps don't "contain" activities — they **classify** them

### What Step Becomes

**Current:**
```
Step
├── isCompleted: Boolean (checkbox)
├── title: String
├── messages: List<ActivityMessageModel> (nested)
└── totalDurationMinutes: Int (aggregated from children)
```

**Future:**
```
StepTag
├── name: String
├── color: String? (optional tag color)
└── icon: String? (optional emoji icon)
No messages nested inside
No isCompleted — tags aren't TODO items
```

---

## 2. Final UI Vision

### Overview Tab (Before / After)

**Before (Container):**
```
┌─────────────────────────────────┐
│ ☐ Research Phase          3 act  │
│ ☑ Design Phase           1 act  │
│ ☐ Development Phase       0 act  │
└─────────────────────────────────┘
```
- Steps look like TODO lists
- Numbers show "how many items inside"
- Checkboxes imply "task completion"

**After (Tag):**
```
┌─────────────────────────────────┐
│ 📝 Task Description              │
│                                   │
│ 🏷️ Tags                         │
│ ┌──────┐ ┌────────┐ ┌──────┐    │
│ │تحقیق │ │ طراحی  │ │ + Add│    │
│ └──────┘ └────────┘ └──────┘    │
│                                   │
│ 📊 Summary                       │
│ 12 فعالیت • 2 تصویر • 90 دقیقه   │
│                                   │
│ فعال‌ترین برچسب: تحقیق (5 فعالیت) │
└─────────────────────────────────┘
```
- Tags are visual chips, not checkboxes
- No "completion" state — tags are classification
- Summary shows aggregate data across ALL activities
- The "most active tag" gives insight

### Activity Feed Tab (Before / After)

**Before:**
```
┌─────────────────────────────────┐
│ فعالیت‌ها                        │
│ ┌──┐ ┌─────┐                    │
│ │همه│ │تحقیق│ │طراحی│            │
│ └──┘ └─────┘                    │
│                                   │
│ ┌ [📝] Research note         ─┐  │
│ │               10:30         │  │
│ └─────────────────────────────┘  │
│ ┌ [📷] UI Mockup [image]    ─┐  │
│ │   🏷️ طراحی                 │  │
│ │               09:15         │  │
│ └─────────────────────────────┘  │
└─────────────────────────────────┘
```

**After (with Tag Pills):**
```
┌─────────────────────────────────┐
│ فعالیت‌ها                        │
│ ┌─────┐ ┌──────┐ ┌─────────┐   │
│ │ همه │ │ تحقیق │ │ 📐 طراحی│   │
│ └─────┘ └──────┘ └─────────┘   │
│                                   │
│ ┌ [📝] Research note         ─┐  │
│ │   🏷️ تحقیق                 │  │
│ │               10:30         │  │
│ └─────────────────────────────┘  │
│ ┌ [📷] UI Mockup [image]    ─┐  │
│ │   🏷️ طراحی                 │  │
│ │               09:15         │  │
│ └─────────────────────────────┘  │
│                                   │
│ [📅 Jump to date]                 │
└─────────────────────────────────┘
```
- `stepId` → `tagId` (future rename)
- Tag chips show in both header filter AND on individual messages
- Clicking a tag on a message filters to similar messages

---

## 3. What Gets Removed (Deleted Files)

These are legacy step-container artifacts that must be deleted:

| File | Reason for Deletion | Risk |
|------|--------------------|------|
| `data/StepCardModel.kt` | Nests `List<ActivityMessageModel>` inside step — container model | **High** — used in Overview |
| `data/StepCardMapper.kt` | Groups activities by step — container aggregation | **High** — used in ViewModel |
| `data/StepPreviewModel.kt` | Old read model, duplicates StepCardModel | **Low** — dead code? |
| `data/CreateStepWithActivitiesUseCase.kt` | Atomic step+activity creation — assumes container | **High** — used in ViewModel |
| `data/StepDraft.kt` | Contains `initialActivities` — container assumption | **Medium** — used in ViewModel |
| `data/StepDraftResolver.kt` | Coordinates step-activity creation | **Medium** — used in UseCase |
| `ui/components/StepCard.kt` | Card UI with nested activity display | **Medium** — UI component |
| `ui/TimelineEventMapper.kt` | Leaks system event types (STEP_CREATED, etc.) | **Medium** — used by TimelineSheet |
| `ui/TimelineBottomSheet.kt` | Competes with Activity Feed | **Medium** — triggered from screen |

**Total: 9 files to delete**

**Net lines of code removed:** ~1,200+

### What Must Be Kept

| File | Reason |
|------|--------|
| `data/TaskStepEntity.kt` | DB entity — needed for migration safety |
| `data/TaskStepDao.kt` | DB access — needed for querying |
| `data/TaskStepRepository.kt` | Repository — needed until migration complete |
| `data/ActivityFeedFilterState.kt` | Still needs `selectedStepId` (renamed → `selectedTagId`) |

---

## 4. What Gets Created (New Files)

| File | Purpose |
|------|---------|
| `data/ActivityTag.kt` | New domain model — lightweight tag (name, color, icon, activityCount) |
| `data/ActivityTagMapper.kt` | Maps `TaskStepEntity → ActivityTag` (transition; later `TagEntity`) |
| `data/ActivityTagFilterState.kt` | Renamed from `ActivityFeedFilterState` — uses tagId |
| `ui/components/TagChip.kt` | New composable — colored chip with text |
| `ui/components/TagRow.kt` | New composable — horizontal scrollable row of tag chips |

---

## 5. What Gets Modified

| File | Change | Complexity |
|------|--------|------------|
| `ui/TaskDetailScreen.kt` | Remove Overview step cards, add tag row + summary | **High** |
| `ui/TaskDetailViewModel.kt` | Replace StepCardModel with ActivityTag, remove timeline navigation | **High** |
| `ui/TaskDetailActivityContent.kt` | Show tag chips on messages | **Medium** |
| `ui/components/ActivityMessageCard.kt` | Add tag pill display on messages | **Low** |
| `data/ActivityMessageModel.kt` | No change needed — stepId is already a nullable field | **None** |

---

## 6. Migration Path (Safe, Multi-Phase)

### Phase 5.5a: Decouple UI (No DB Changes)
1. Replace StepCard with ActivityTag in Overview tab
2. Remove TimelineBottomSheet trigger from UI
3. Add TagRow to Overview with summary section
4. Add tag pills to ActivityMessageCard
5. Remove StepCard.kt, StepCardModel.kt, StepCardMapper.kt
6. Keep TaskStepEntity untouched

### Phase 5.5b: Decouple Creation (No DB Changes)
1. Simplify step creation UI — no "initial activities" concept
2. Remove `CreateStepWithActivitiesUseCase`
3. Remove `StepDraft` + `StepDraftResolver`
4. Replace with simple: `viewModel.addTag(name: String)`
5. Activity creation (in composer) optionally accepts tagId

### Phase 5.5c: Remove Timeline System
1. Remove `TimelineEventMapper` + `TimelineBottomSheet`
2. Add date-jump button to Activity Feed header
3. Implement `scrollToDate` logic in feed

### Phase 5.5d: DB Migration (Future)
1. Rename table `task_steps` → `activity_tags` (or use composite key)
2. Rename column `stepId` → `tagId` on `activity_events`
3. Remove `isCompleted` and `completedAt` columns
4. Add `color` and `icon` columns
5. Keep backward compatibility layer

---

## 7. Risk Analysis

### High Risk Changes (Need Rollback Plan)

| Change | Risk | Mitigation |
|--------|------|------------|
| Removing StepCardModel | **HIGH** — Overview tab will be blank until new UI is ready | 1. Create new UI first 2. Delete old code only after new UI compiles |
| Removing TimelineBottomSheet | **MEDIUM** — Lose date navigation | 1. Add date-jump to feed first 2. Then remove Timeline |
| Removing CreateStepWithActivitiesUseCase | **MEDIUM** — Existing saved steps become orphaned unless handled | 1. Make ViewModel handle both old and new creation 2. Deprecate, don't delete |
| Removing StepDraft | **LOW** — Not persisted, only used in creation flow | Safe to remove |

### Reverse Compatibility Layer

```kotlin
// Phase 5.5 — keep in codebase, allows old code to compile
typealias StepId = Long // stepId will become tagId in Phase 5.5d
typealias ActivityTag = TaskStepEntity // wraps old entity until DB migration
```

---

## 8. Database Migration (Phase 5.5d+)

```sql
-- Future migration (not implemented yet)
ALTER TABLE task_steps RENAME TO activity_tags;
ALTER TABLE activity_tags ADD COLUMN color TEXT;
ALTER TABLE activity_tags ADD COLUMN icon TEXT;
ALTER TABLE activity_tags DROP COLUMN isCompleted;
ALTER TABLE activity_tags DROP COLUMN completedAt;
ALTER TABLE activity_events RENAME COLUMN stepId TO tagId;
```

**When to implement:**
- Only when new UI is stable and tested
- Requires Room migration with `@Database(version = 15 → 16)`
- Keep backward-compatible views for old queries

---

## 9. Data Flow After Decoupling

### Current (Container):
```
StepDraft
  ↓
CreateStepWithActivitiesUseCase
  ↓
TaskStepEntity + ActivityEventEntity(stepId=X)
  ↓
StepCardMapper.toCardModel()
  ↓
StepCardModel(messages=[...])  // nested!
  ↓
StepCard UI (checkboxes + nested activities)
```

### Future (Tag):
```
ActivityTag(name="تحقیق", color=...)
  ↓
viewModel.addTag(name)
  ↓
save to DB (task_steps table for now)
  ↓
ActivityTagMapper.toTag(entity)
  ↓
ActivityTag(name, activityCount)
  ↓
TagChip UI (colored chip, no nesting)
```

### Feed filter stays the same:
```
ActivityEventEntity.stepId
  ↓
ActivityMessageMapper.toMessage()
  ↓
ActivityMessageModel.stepId  // unchanged
  ↓
filterState.selectedStepId
  ↓
filteredActivityMessages
```

---

## 10. Summary

### The Recommendation

**Step becomes a Tag.** No checkboxes, no nesting, no completion state. Tags are visual chips that:

1. **Classify** activities (not contain them)
2. **Filter** the Activity Feed
3. **Display** on individual message cards
4. **Summarize** in the Overview tab (counts, most active)

### Immediate Action Items (Phase 5.5a)

1. Create `ActivityTag` domain model
2. Create `TagChip` + `TagRow` composables
3. Replace Overview step cards with tag row + summary
4. Add tag display on ActivityMessageCard
5. Hide TimelineBottomSheet trigger (keep code for safety)
6. **Do NOT** delete timeline files yet

### Files to Delete (Phase 5.5b)

Only after new UI is verified:
- `StepCardModel.kt`, `StepCardMapper.kt`
- `StepDraft.kt`, `StepDraftResolver.kt`
- `CreateStepWithActivitiesUseCase.kt`
- `StepPreviewModel.kt` (if truly dead)

### Files to Delete (Phase 5.5c)

After date-jump added to feed:
- `TimelineEventMapper.kt`
- `TimelineBottomSheet.kt`

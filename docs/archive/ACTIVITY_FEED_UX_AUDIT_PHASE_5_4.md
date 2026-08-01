# Activity Feed UX & Architecture Audit — Phase 5.4

> **Date:** July 2026  
> **Scope:** Complete product & architecture audit of the Activity Feed  
> **Status:** Analysis only — no code changes

---

## 1. Executive Summary

The Vision Planner Activity Feed has successfully migrated from a step-centric to a feed-first architecture. The core abstractions (ActivityMessageModel → ActivityMessageCapability → ActivityMessageAction) are sound and well-separated. The Telegram Saved Messages inspiration is correct for the product vision.

**However, several UX mismatches and architectural risks remain:**

1. The **TimelineBottomSheet** and **TimelineEventMapper** (from the old step-centric era) create a **competing UI** that bypasses the feed architecture and exposes raw event types to the user.
2. The **Step relationship** is still unclear — the UI treats steps as containers (in the overview tab) but the data model treats them as tags (via `stepId`). This inconsistency leaks to the user.
3. **AttachmentPreview in Composer** still contains heavy debug logging that should have been removed before Phase 5.3.
4. **No pagination** — the feed loads all messages for a task, creating a scalability risk for tasks with 200+ activities.
5. **The "ویرایش شده" flag** is present in the model but has no persistence path, meaning it only works within a session.
6. **The FAB options** (یادداشت, تصویر, فایل, فعالیت دستی) all open the same generic composer — there's no direct action.

---

## 2. Current Architecture Assessment

### 2.1 Data Flow

```
User Action
    ↓
ActivityComposerBottomSheet (UI)
    ↓
ActivityDraft (domain)
    ↓
ActivityDraftResolver (maps to event type + encodes payload)
    ↓
ActivityEventEntity (Room entity)
    ↓
ActivityEventRepository
    ↓
ActivityEventDao (SQLite)
    ↓
[Room Flow re-emits]
    ↓
ActivityEventRepository.observeActivities()
    ↓
ActivityMessageMapper.toMessages() (filters system events, decodes payloads)
    ↓
ActivityMessageModel (clean read model)
    ↓
TaskDetailViewModel (filter + combine flows)
    ↓
ActivityMessageCard (UI composable)
```

**Assessment: GOOD** — The data flow is clean with clear separation of concerns. The mapper layer is the right place for domain transformation.

### 2.2 Architecture Diagram

```
┌─────────────────────────────────────────────┐
│              TaskDetailScreen                │
│  ┌────────────┐  ┌────────────────────────┐ │
│  │ Overview   │  │   Activity Feed        │ │
│  │ Tab        │  │  ┌──────────────────┐  │ │
│  │            │  │  │ Header + Filters  │  │ │
│  │ - Title    │  │  ├──────────────────┤  │ │
│  │ - Goal     │  │  │ Day Groups       │  │ │
│  │ - Switch   │  │  │  ┌────────────┐  │  │ │
│  │ - Reminder │  │  │  │ Msg Card   │  │  │ │
│  │ - Steps    │  │  │  └────────────┘  │  │ │
│  └────────────┘  │  └──────────────────┘  │ │
│                  │  ┌──────────────────┐  │ │
│                  │  │    FAB + Menu    │  │ │
│                  │  └──────────────────┘  │ │
│  ┌──────────────────────────────────────┐ │
│  │    TimelineBottomSheet (OLD/LEGACY) │ │
│  └──────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

**Key finding:** The `TimelineBottomSheet` and the feed coexist, creating two parallel "history" experiences. The Timeline sheet still uses `TimelineEventMapper` which renders system events (STEP_CREATED, etc.) — directly contradicting the phase 4.12 principle of hiding system events from the UI.

### 2.3 Component Responsibilities

| Component | Responsibility | Assessment |
|-----------|---------------|------------|
| `ActivityMessageModel` | Clean read model, UI-independent | ✅ Clean |
| `ActivityMessageMapper` | Entity → Model mapping, system event filtering | ✅ Clean |
| `ActivityMessageCapability` | Permission model for interactions | ✅ Clean |
| `ActivityMessageAction` | User intent sealed class | ✅ Clean |
| `ActivityComposerState` | Composer state machine | ✅ Clean |
| `ActivityComposerReducer` | Pure state reducer | ✅ Clean |
| `ActivityFeedFilterState` | Filter state | ✅ Clean |
| `ActivityMessageInteractionState` | Selection state | ✅ Clean |
| `ActivityDraft` | Domain model for creation | ⚠️ Slight overlap with payload |
| `ActivityDraftResolver` | Maps draft → event type + encodes | ⚠️ Has legacy format knowledge |
| `TimelineEventMapper` | Legacy step-centric UI model | ❌ Should be deprecated |
| `TimelineBottomSheet` | Legacy date-navigation view | ❌ Competes with feed |
| `AttachmentPreview` | Composer attachment list | ⚠️ Has debug artifacts |

---

## 3. UX Problems

### 3.1 P0 — Blocks Core Product Experience

#### Problem 1: TimelineBottomSheet competes with Activity Feed
- **Location:** `TimelineBottomSheet.kt`
- **Severity:** Users get two different "history" UIs
- **Details:**
  - The feed uses `ActivityMessageCard` (clean, filtered)
  - The timeline uses `TimelineEventMapper` which renders raw event types like "مرحله جدید ایجاد شد" and "تصویر اضافه شد"
  - These are system-language descriptions, not user-content
  - The feed groups by "امروز/دیروز/تاریخ" — the timeline groups by day with date navigation
  - **Recommendation:** Migrate the TimelineBottomSheet to reuse the feed architecture, or remove it entirely and add date-navigation to the feed

#### Problem 2: No direct action from FAB options
- **Location:** `TaskDetailScreen.kt` — `ActivityFab`
- **Severity:** All 4 FAB options open the same generic composer
- **Details:**
  - User taps "📷 تصویر" → expects camera/gallery to open directly
  - Instead: composer opens, user must tap 🖼 button in composer toolbar
  - This is a 2-step flow for a 1-step expectation
  - **Recommendation:** `onSelectImage` should launch the image picker directly, pre-populating the composer with the selected image

#### Problem 3: Legacy debug logging visible in Composer
- **Location:** `AttachmentPreview.kt` (entire file)
- **Severity:** P0 for production — Logcat pollution, security concern for URI leaks
- **Details:**
  - `Log.d("COMPOSER_DEBUG", ...)` throughout the file
  - Logs include URIs like `uri=content://...`
  - Red debug background `Color.Red.copy(alpha = 0.3f)` in ImageAttachmentPreview
  - Coil listener callbacks with error logging
  - `onGloballyPositioned` with position logging
  - This should have been cleaned up before Phase 5.2.2
  - **Recommendation:** Remove all `Log.d`, debug background, and `onGloballyPositioned` calls

### 3.2 P1 — Important UX Issues

#### Problem 4: Step ambiguity — container vs. tag
- **Location:** Throughout Overview tab
- **Severity:** Users don't know what steps are
- **Details:**
  - In the Overview tab, Steps look like containers (checkboxes with titles)
  - In the Feed, `stepId` is just a filter
  - The mental model is inconsistent: "Should I create a step for everything, or just tag activities?"
  - The Overview shows steps as TODO items, but the Feed treats them as tags
  - **Recommendation:** Decide on the step mental model and make it consistent everywhere. Either:
    - (A) Steps are TODO lists (container) — keep in Overview, remove from feed filters
    - (B) Steps are tags (metadata) — remove checkbox behavior, make them pure labels
    - Current hybrid is confusing

#### Problem 5: Image picker opens composer instead of picking directly
- **Location:** FAB → onSelectImage
- **Severity:** Adds friction to the most common creation flow
- **Details:**
  - User taps "📷 تصویر" from FAB
  - Composer opens in generic mode
  - User must tap 🖼 in toolbar to pick an image
  - Telegram lets you pick media in 1 tap
  - **Recommendation:** Have `onSelectImage` launch the image picker `ActivityResultContracts.PickVisualMedia` directly, then open composer pre-filled with the selected image

#### Problem 6: Scroll-to-message uses `filteredActivityMessages` index
- **Location:** `TaskDetailScreen.kt` — ReplyNavigation handler
- **Severity:** Wrong index if filter is active
- **Details:**
  - `val index = filteredActivityMessages.indexOfFirst { ... }` 
  - If a step filter is active (e.g. showing only Step 1 messages), and the user taps a reply reference pointing to a Step 2 message, the index will be -1
  - No fallback for this case
  - **Recommendation:** Scroll in the `allMessages` list instead, or ensure the filter temporarily clears for the scroll

#### Problem 7: isEdited has no persistence
- **Location:** `ActivityMessageModel.isEdited`
- **Severity:** "ویرایش شده" only works within session
- **Details:**
  - Added in Phase 5.3 as `isEdited: Boolean = false`
  - No DB field to persist this state
  - After ViewModel recreation or app restart, all messages appear unedited
  - **Recommendation:** Add an `editedAt` nullable timestamp to `ActivityEventEntity`, or accept this as in-memory-only until a future DB migration

### 3.3 P2 — Polish / Future Improvements

#### Problem 8: Feed has no "select all images" or media gallery view
- While the filter system can filter by images, there's no thumbnail grid view for quick browsing

#### Problem 9: No message search
- Users with 100+ messages have no way to search

#### Problem 10: FAB options text-only, no icons
- The `FabOption` composable uses emoji icons which look different across devices

#### Problem 11: No undo after delete
- Delete is hard-delete with no recovery path
- The phase 5.3 spec said "prepare UI abstraction for future soft delete" but didn't implement it

#### Problem 12: `User confusion about"ویرایش شده"`  
- Since isEdited isn't persisted, the label only shows during the session

---

## 4. Product Philosophy Mismatches

### 4.1 "Telegram Saved Messages" vs Current State

| Telegram Saved Messages | Vision Planner Current | Gap |
|------------------------|----------------------|-----|
| Tap + → immediate action (photo, file) | FAB → composer → toolbar → action | ❌ 2 extra taps |
| Messages feel personal and owned | Messages feel like "task events" | ⚠️ Improving |
| Search available | No search | ❌ |
| Media gallery view | Only list view | ❌ |
| Undo/delete with recover | Hard delete | ❌ |
| Messages feel permanent | "ویرایش شده" not persisted | ⚠️ |
| Clean, production UI | Debug code in production | ❌ |

### 4.2 "Step" Identity Crisis

**Current behavior:** Steps are created in the Overview tab as TODO checkboxes. In the Feed, `stepId` is a tag/filter.

**User confusion:** "Is a step a task I need to do, or a category for my notes?"

**Recommendation:** Choose ONE mental model:
- **If Step = Container:** Show step messages nested/numbered in feed. Remove step filter chips.
- **If Step = Tag (recommended):** Remove checkbox behavior from steps. Make them pure filter labels. The Overview shows step names as colored tags, not TODO items.

The tag model fits better with "Personal Knowledge Memory" philosophy.

### 4.3 "Timeline" vs "Feed"

The TimelineBottomSheet was designed for the old step-centric architecture. It renders system events (STEP_CREATED, STEP_COMPLETED) — directly violating the core principle that "users should never see database representation."

**Recommendation:** Deprecate TimelineBottomSheet. Add date-navigation controls directly to the feed (a "jump to date" button in the header area).

---

## 5. Recommended Future Phases

| Phase | Title | Priority | Description |
|-------|-------|----------|-------------|
| 5.5 | **Timeline Deprecation & Cleanup** | P0 | Remove TimelineBottomSheet + TimelineEventMapper. Add date-jump to feed. Remove all debug code from AttachmentPreview. |
| 5.6 | **Direct Action Creation** | P0 | FAB → تصویر opens picker directly, composer pre-filled with result. Same for file. |
| 5.7 | **Step Identity Resolution** | P1 | Choose container vs. tag model and make UI consistent. |
| 5.8 | **Delete UX — Soft Delete Foundation** | P1 | Add `isDeleted` to entity (it already exists in model), add undo snackbar. |
| 5.9 | **Feed Pagination** | P1 | Add page-based loading (e.g., 50 messages per page) with lazy loading as user scrolls up. |
| 6.0 | **AI Insight Layer** | P2 | The original Phase 7 target — AI-generated summaries, patterns, suggestions. |
| 6.1 | **Message Search** | P2 | Full-text search over activity text content. |
| 6.2 | **Media Gallery** | P2 | Grid view of all images in the task. |
| 6.3 | **isEdited Persistence** | P2 | DB migration to add `editedAt` field to ActivityEventEntity. |

### Priority Rationale

**P0 — Must fix before next release:**
- Timeline sheet leaks system events to users
- Debug code in production (security concern)
- FAB options don't do what they say

**P1 — Should fix soon:**
- Step mental model confusion affects core UX
- Soft delete needed for safety
- Pagination prevents crashes on large tasks
- Scroll-to-reply broken with active filters

**P2 — Nice to have:**
- Search, gallery, AI features are enhancements
- isEdited persistence requires DB migration (avoid if possible)

---

## 6. Risk Analysis

### 6.1 Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Removing TimelineBottomSheet breaks existing users | Medium | Medium | Keep it but hide from UI first; add date navigation to feed |
| Step mental model change confuses existing users | Low | High | Clear migration guide; keep both models temporarily with a setting |
| Debug log in AttachmentPreview leaks user URIs | High | Medium | Immediate fix: remove all Log.d calls |
| Feed with 500+ messages causes slow rendering | Medium | High | Implement pagination before scaling |
| Reply navigation with filter active fails silently | High | Low | Fix index lookup to use allMessages or handle -1 gracefully |

### 6.2 Product Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Users don't understand "activity" vs "step" | High | High | Onboarding tips, clear labels, consistent mental model |
| FAB → composer feels slow | Medium | Medium | Direct actions in Phase 5.6 |
| No undo for delete erodes trust | High | Medium | Soft delete in Phase 5.8 |

---

## 7. Implementation Order Suggestion

### Immediate (before next build)
1. **Remove debug code from `AttachmentPreview.kt`** — Log.d, red background, Coil listeners, onGloballyPositioned
2. **Fix ReplyNavigation index lookup** — Use `allMessages` instead of `filteredActivityMessages`, or handle -1
3. **Add safe fallback for scroll-to-message** — If target message is outside current filter, temporarily clear filter

### Phase 5.5 — Timeline Deprecation
4. Hide TimelineBottomSheet trigger from UI
5. Add "📅 Jump to date" button in feed header
6. Implement scroll-to-date logic in feed
7. Remove `TimelineEventMapper` or mark as deprecated

### Phase 5.6 — Direct Actions
8. Refactor FAB — `onSelectImage` launches picker directly
9. Pre-fill composer state with selected image
10. Same for file action

### Phase 5.7 — Step Resolution
11. Decide on step mental model (recommendation: tags)
12. Remove checkbox behavior from steps in Overview
13. Make steps pure filterable tags in both tabs

### Phase 5.8 — Soft Delete
14. Add `editedAt` / `isDeleted` fields to DB entity (migration-safe)
15. Implement undo snackbar on delete
16. Show "این پیام حذف شده است" for deleted messages

---

## 8. Final Product Gap Table

| Area | Current State | Target State | Gap | Priority |
|------|--------------|-------------|-----|----------|
| **Timeline vs Feed** | Two competing history UIs | Single feed with date navigation | ❌ Confusing | **P0** |
| **Debug code** | Log.d, red background in composer | Clean production code | ❌ Security | **P0** |
| **Direct actions** | FAB → generic composer | FAB → direct action | ❌ Extra taps | **P0** |
| **Step mental model** | Hybrid container + tag | Clear single model | ❌ Confusing | **P1** |
| **Scroll-to-reply** | Uses filtered index | Uses correct index | ⚠️ Bug | **P1** |
| **isEdited persistence** | In-memory only | Persisted | ⚠️ Transient | **P1** |
| **Delete safety** | Hard delete | Soft delete + undo | ⚠️ Risky | **P1** |
| **Pagination** | Load all messages | Page-based loading | ⚠️ Scaling | **P1** |
| **Search** | None | Full-text search | ❌ Missing | **P2** |
| **Media gallery** | List only | Grid view | ❌ Missing | **P2** |
| **AI features** | Not started | Insight layer | ❌ Future | **P2** |
| **Message composition** | Good | Telegram-quality | ✅ Close | — |
| **Filter system** | Good | Complete | ✅ Working | — |
| **Interaction model** | Good | Natural | ✅ Close | — |
| **Architecture** | Clean | Clean | ✅ Good | — |

---

## Appendix A: Quick Wins (can fix now)

These are small, safe fixes that don't need a full phase:

1. `AttachmentPreview.kt` — Remove lines 46-52 (debug Log.d), remove lines 83-95 (debug logging + red background + onGloballyPositioned), remove lines 100-112 (Coil listener logging)
2. `TaskDetailScreen.kt` — Change `filteredActivityMessages.indexOfFirst` to `allMessages.indexOfFirst` in ReplyNavigation handler
3. `ActivityFeedFilterChips` — Add a subtle "فیلتر:" label before the chips to clarify what they do

---

*End of Audit Report — Phase 5.4*

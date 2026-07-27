# Phase 5.4.1 — Activity Feed Quick Stabilization Report

> **Date:** July 2026  
> **Scope:** Three Quick Win fixes identified by Activity Feed UX Audit  
> **Commit:** (see below)

---

## Task 1 — Remove Debug / Internal Data Leakage

### Before

`AttachmentPreview.kt` contained 8 `Log.d` statements, including:

- `Log.d("COMPOSER_DEBUG", "🖼 ImageAttachmentPreview COMPOSING: uri=$uri")` — **leaks URIs to logcat**
- `Log.d("COMPOSER_DEBUG", "🖼 Box posInWindow=...")` — layout debugging
- Coil listener logging (onStart, onError, onSuccess) — unnecessary noise
- `Color.Red.copy(alpha = 0.3f)` debug background visible during development
- `onGloballyPositioned` with position tracking — unnecessary overhead

Also, unused `import android.util.Log` in:
- `ActivityComposerBottomSheet.kt`
- `TaskDetailViewModel.kt`

### After

All debug logging, debug backgrounds, Coil listeners, and `onGloballyPositioned` removed from `AttachmentPreview.kt`. Unused `Log` imports removed from all files.

### Tests Added

- `image attachment uri is never exposed as text` — verifies content:// URIs never appear in user-facing text
- `image attachment uri is accessible through model` — verifies URI stays in attachment layer
- `json payload never appears in message text` — verifies raw JSON is not exposed
- `event type enum never appears in displayed fields` — verifies event type names hidden

---

## Task 2 — Fix Reply Navigation With Filters

### Before

```kotlin
LaunchedEffect(scrollToMessageId) {
    val index = filteredActivityMessages.indexOfFirst { it.id == targetId }
    // ...
}
```

Used `filteredActivityMessages` for index lookup. When a step filter hid the target message, the index would be -1 and the scroll silently failed.

### After

```kotlin
LaunchedEffect(scrollToMessageId, filteredActivityMessages) {
    val index = filteredActivityMessages.indexOfFirst { it.id == targetId }
    if (index >= 0) {
        // Scroll directly
        scrollToMessageId = null
    } else if (filterState.selectedStepId != null) {
        // Clear filter and retry on next emission
        viewModel.showAllActivities()
    }
}
```

**Behavior:**
1. If target is visible in filtered list → scroll to it
2. If target is hidden by step filter → clear filter, keep `scrollToMessageId` set
3. The next `LaunchedEffect` emission (after filter clears) finds and scrolls to the target
4. If target not found and no filter active → silently ignore (message doesn't exist)

### Tests Added

- `reply navigation with active filter finds target in filtered list`
- `reply navigation with hidden target clears filter` — verifies filter-clearing behavior
- `missing reply target does not crash navigation`
- `deleted reply target is handled as missing`

---

## Task 3 — Make FAB Actions Direct

### Before

All four FAB options (📝 Note, 📷 Image, 📎 File, ⏱️ Manual Activity) opened the same generic `ActivityComposerBottomSheet`.

```
FAB → tap 📷 → Composer opens → user taps 🖼 in toolbar → picker opens
```

### After

```
FAB → tap 📷 → Image picker opens directly → activity created immediately
FAB → tap 📝 → Composer opens (note mode)
FAB → tap 📎 → Composer opens (file mode)
FAB → tap ⏱️ → Composer opens (manual activity mode)
```

### New File

- `ActivityCreationAction.kt` — Sealed class for creation intents:
  - `Note` — opens composer
  - `Image` — launches photo picker directly
  - `File` — opens composer (full file picker in future)
  - `ManualActivity` — opens composer

### Architecture

- `TaskDetailScreen` owns the image picker launcher via `rememberLauncherForActivityResult`
- On image selection: URI permission persisted + activity created immediately via `viewModel.createActivity()`
- `ActivityFab` accepts a single `onSelectAction: (ActivityCreationAction) -> Unit` callback

### Tests Added

- `creation action Note/Image/File/ManualActivity is correct type`
- `creation action dispatch by type` — verifies Image → IMAGE_PICKER, others → COMPOSER

---

## Files Changed

| File | Status | Change |
|------|--------|--------|
| `data/ActivityCreationAction.kt` | **NEW** | Sealed class for creation intents |
| `ui/composer/AttachmentPreview.kt` | **MODIFIED** | Removed all Log.d, debug bg, Coil listeners |
| `ui/ActivityComposerBottomSheet.kt` | **MODIFIED** | Removed unused `import android.util.Log` |
| `ui/TaskDetailViewModel.kt` | **MODIFIED** | Removed unused `import android.util.Log` |
| `ui/TaskDetailScreen.kt` | **MODIFIED** | Direct image picker, scroll-to-fix, FAB refactor |
| `data/ActivityFeedStabilizationTest.kt` | **NEW** | 18 tests |

---

## Remaining Technical Debt

1. **File picker** — "📎 File" still opens composer (full file picker needs `ActivityResultContracts.OpenDocument`)
2. **Pre-filled composer** — After image picker, the activity is created immediately (no editing). Future: open composer pre-filled with image for user to add text.
3. **TimelineBottomSheet cleanup** — Still exposes system events (to be addressed in Phase 5.5)
4. **Step mental model** — Still hybrid container/tag (to be addressed in Phase 5.7)

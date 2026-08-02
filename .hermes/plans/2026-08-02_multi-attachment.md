# Multi-Attachment Activity Feed — Implementation Plan

> **For Hermes:** Implement task-by-task, push to GitHub after each task.

**Goal:** Support multiple images + multiple files per activity, with compact thumbnails.

**Design Philosophy:** 
- Small previews (icons/thumbnails) to keep feed compact
- Tap to view full content (fullscreen image / open file)
- Max 5 images + 5 files per activity

**Files to change:**
- `ActivityMessageDisplayContent.kt` — new MultiMediaContent type
- `ActivityMessageCard.kt` — compact multi-attachment rendering
- `TaskDetailScreen.kt` — multi-select pickers
- `AttachmentPreview.kt` — compact composer preview

---

## Task 1: ActivityMessageDisplayContent — New multi-attachment type

**File:** `app/src/main/java/com/example/plugins/planner/data/ActivityMessageDisplayContent.kt`

### Changes:

Replace `MediaContent` and `MediaWithText` with a single `MultiMediaContent`:

```kotlin
sealed class ActivityMessageDisplayContent {
    data class TextContent(
        val text: String,
        val durationMinutes: Int? = null
    ) : ActivityMessageDisplayContent()

    data class MultiMediaContent(
        val images: List<ActivityAttachment.Image>,
        val files: List<ActivityAttachment.File>,
        val text: String? = null,
        val durationMinutes: Int? = null
    ) : ActivityMessageDisplayContent()

    data object EmptyMessage : ActivityMessageDisplayContent()
    data object DeletedMessage : ActivityMessageDisplayContent()
}
```

Update `from()` method:
- Collect all images and files
- Return `MultiMediaContent` with all attachments
- Keep backward compatibility (single attachment = list of 1)

---

## Task 2: ActivityMessageCard — Compact multi-attachment UI

**File:** `app/src/main/java/com/example/plugins/planner/ui/components/ActivityMessageCard.kt`

### Changes:

Replace `ImagePreview` and `FilePreview` with compact versions:

**ImageGallery (new):**
- If 1 image: single thumbnail (80×80 dp)
- If 2 images: 2 thumbnails side by side (each 60×60 dp)
- If 3+ images: 2 thumbnails + counter badge ("+N")
- Tap → fullscreen viewer

**FileList (new):**
- Compact row per file: icon (📄) + name (truncated) + size
- Tap → open with external app
- Max 3 visible, then "+N more" if needed

**MessageContent updated:**
```kotlin
is MultiMediaContent -> {
    Column {
        // Text first (if exists)
        if (displayContent.text != null) {
            Text(text = displayContent.text, ...)
        }
        // Images gallery
        if (displayContent.images.isNotEmpty()) {
            ImageGallery(images = displayContent.images, onImageClick = ...)
        }
        // Files list
        if (displayContent.files.isNotEmpty()) {
            FileList(files = displayContent.files, onFileClick = ...)
        }
    }
}
```

---

## Task 3: TaskDetailScreen — Multi-select pickers

**File:** `app/src/main/java/com/example/plugins/planner/ui/TaskDetailScreen.kt`

### Changes:

**Image Picker:**
```kotlin
// OLD: PickVisualMediaRequest(ImageOnly)
// NEW: PickMultipleVisualMediaRequest(ImageOnly, maxSelections = 5)
```

**File Picker:**
```kotlin
// OLD: contract = ActivityResultContracts.GetContent()
// NEW: Use custom launcher with EXTRA_ALLOW_MULTIPLE
```

**On result:**
```kotlin
onResult = { uris ->
    if (uris is List<Uri>) {
        // Multiple files
        uris.forEach { uri -> addAttachment(uri) }
    } else if (uris is Uri) {
        // Single file (fallback)
        addAttachment(uris)
    }
}
```

---

## Task 4: AttachmentPreview — Compact composer preview

**File:** `app/src/main/java/com/example/plugins/planner/ui/composer/AttachmentPreview.kt`

### Changes:

**Image previews:** 
- Horizontal scrollable row of thumbnails (60×60 dp)
- Each with ✕ button to remove

**File previews:**
- Keep current list style (already compact)

---

## Verification

1. Create activity with 2 images → compact thumbnails shown
2. Create activity with 3 images + 1 file → thumbnails + file icon
3. Tap image thumbnail → fullscreen viewer
4. Tap file → opens with external app
5. Edit activity → all attachments preserved
6. Old activities (single attachment) still display correctly

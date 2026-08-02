# Activity Message Interaction — Telegram-style Plan

> **For Hermes:** Implement task-by-task, push to GitHub after each task.

**Goal:** Single tap opens context menu, long press selects for batch operations (like Telegram).

**Files to change:**
- `ActivityMessageCard.kt` — remove three-dot icon, change tap/long-press behavior
- `ActivityMessageAction.kt` — add Select action
- `TaskDetailScreen.kt` — selection mode top bar, batch delete

---

## Task 1: ActivityMessageCard — Remove three-dot icon, change interaction

**File:** `app/src/main/java/com/example/plugins/planner/ui/components/ActivityMessageCard.kt`

### Changes:

1. **Remove** the three-dot `IconButton` and its `Row` wrapper (lines 149-173)
2. **Restore** `MessageMetadataRow` to original (remove `modifier` param)
3. **Change** `combinedClickable` behavior:
   - `onClick` → toggle context menu (if capabilities exist)
   - `onLongClick` → call new `onLongPress` callback
4. **Add** `onLongPress: (() -> Unit)?` parameter
5. **Remove** `Icons.Default.MoreVert` import

### New signature:
```kotlin
fun ActivityMessageCard(
    message: ActivityMessageModel,
    repliedToMessage: ActivityMessageModel? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,  // NEW
    onAction: ((ActivityMessageAction) -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,  // NEW
    onAttachmentClick: ((ActivityAttachment) -> Unit)? = null,
    onReplyReferenceClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
    maxBubbleWidthFraction: Float = 0.85f
)
```

### New interaction:
```kotlin
.combinedClickable(
    onClick = {
        if (isSelectionMode) {
            // In selection mode: toggle select/deselect
            onLongPress?.invoke()
        } else {
            // Normal mode: toggle context menu
            if (showMenu) showMenu = false
            else if (capability.canEdit || capability.canDelete || capability.canReply) showMenu = true
        }
    },
    onLongClick = {
        // Always enter selection mode
        onLongPress?.invoke()
    }
)
```

---

## Task 2: ActivityMessageAction — Add Select action

**File:** `app/src/main/java/com/example/plugins/planner/data/ActivityMessageAction.kt`

Add:
```kotlin
/** Select/deselect a message for batch operations. */
data class Select(
    val messageId: Long
) : ActivityMessageAction()
```

---

## Task 3: TaskDetailScreen — Selection mode

**File:** `app/src/main/java/com/example/plugins/planner/ui/TaskDetailScreen.kt`

### Changes:

1. **Add state:**
   ```kotlin
   var selectedMessageIds by remember { mutableStateOf(setOf<Long>()) }
   val isSelectionMode = selectedMessageIds.isNotEmpty()
   ```

2. **Add selection top bar** (when `isSelectionMode`):
   - Show count: "${selectedMessageIds.size} انتخاب شده"
   - Close button (X) to exit selection mode
   - Delete button for batch delete

3. **Update handleMessageAction** to handle Select:
   ```kotlin
   is ActivityMessageAction.Select -> {
       selectedMessageIds = if (action.messageId in selectedMessageIds) {
           selectedMessageIds - action.messageId
       } else {
           selectedMessageIds + action.messageId
       }
   }
   ```

4. **Pass onLongPress** to ActivityMessageCard:
   ```kotlin
   isSelectionMode = isSelectionMode,
   onLongPress = {
       selectedMessageIds = if (message.id in selectedMessageIds) {
           selectedMessageIds - message.id  // Deselect if already selected
       } else {
           selectedMessageIds + message.id  // Select
       }
   }
   ```

5. **Update isSelected** in ActivityMessageCard:
   ```kotlin
   isSelected = message.id in selectedMessageIds
   ```

6. **Batch delete** with VisionConfirmDialog:
   ```kotlin
   if (showBatchDeleteConfirm) {
       VisionConfirmDialog(
           title = "حذف پیام‌ها",
           message = "${selectedMessageIds.size} پیام حذف خواهد شد.",
           confirmText = "حذف",
           onConfirm = {
               selectedMessageIds.forEach { viewModel.deleteActivity(it) }
               selectedMessageIds = emptySet()
           },
           isDestructive = true
       )
   }
   ```

---

## Verification

1. Single tap on message → context menu opens
2. Single tap again → context menu closes
3. Long press → enters selection mode, message gets selected (highlighted)
4. While in selection mode, tap on other messages → selects/deselects them (NOT opens menu)
5. Tap X or tap empty area → exits selection mode, deselects all
6. Tap delete in selection mode → confirmation dialog → batch delete

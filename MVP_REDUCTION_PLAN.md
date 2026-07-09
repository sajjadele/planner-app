# MVP Reduction Phase 3 — PlannerScreen God Composable Refactor

## Current State
- **File**: `PlannerScreen.kt` — 667 lines, single monolithic `@Composable`
- **Main pain point**: `AddTaskDialog` alone is ~312 lines (lines 353-665)
- **Goal**: Break into focused, testable, reusable composables

---

## Refactor Plan — 5 Steps

### Step 1: Extract Day Selector (Calendar Row)
**Target**: Lines 85–138 → New file `DaySelector.kt`
- Input: `daysOfWeek: List<Pair<String, String>>`, `selectedIndex: Int`, `onSelect: (Int) -> Unit`
- Output: Reusable horizontal day picker with circles
- Testability: Pure UI, easy to preview/screenshot test

### Step 2: Extract Section Header
**Target**: Lines 143–166 → New file `SectionHeader.kt`
- Input: `dayName: String`, `completedCount: Int`, `totalCount: Int`
- Output: Row with day title + "X of Y done" badge
- Testability: Pure UI, trivial to test

### Step 3: Extract Empty State
**Target**: Lines 170–199 → New file `EmptyState.kt`
- Input: `dayName: String`, `onAddClick: () -> Unit` (optional)
- Output: Centered illustration + message + hint
- Testability: Pure UI

### Step 4: Extract Task Item
**Target**: Lines 212–321 → New file `TaskItem.kt`
- Input: `task: Task`, `onToggle: () -> Unit`, `onDelete: () -> Unit`
- Output: Row with checkbox, title, priority badge, reminder, delete button
- Testability: Pure UI, good for screenshot testing

### Step 5: **Major** — Extract Add Task Dialog (Biggest win)
**Target**: Lines 353–665 → New file `AddTaskDialog.kt` + sub-components
- **Sub-components**:
  - `AddTaskDialog` (orchestrator) — state management, callbacks
  - `TaskTitleInput` — OutlinedTextField with focus handling
  - `OptionalFieldsSection` — collapsible container (expand/collapse)
  - `PrioritySelector` — 3-chip selector (HIGH/MEDIUM/LOW)
  - `ReminderPicker` — TimePickerDialog wrapper + display
  - `GoalInput` — OutlinedTextField for goal name
  - `ValueTagSelector` — FilterChip row for semantic tags
  - `DialogActions` — Cancel/Save buttons
- **State**: Hoist to `AddTaskDialogState` data class or keep in dialog (MVP: keep local)
- **Callbacks**: `onSave: (TaskInput) -> Unit`, `onDismiss: () -> Unit`

---

## File Structure After Refactor

```
app/src/main/java/com/example/plugins/planner/
├── PlannerScreen.kt           (~150 lines — orchestrator only)
├── components/
│   ├── DaySelector.kt
│   ├── SectionHeader.kt
│   ├── EmptyState.kt
│   ├── TaskItem.kt
│   └── dialog/
│       ├── AddTaskDialog.kt
│       ├── TaskTitleInput.kt
│       ├── OptionalFieldsSection.kt
│       ├── PrioritySelector.kt
│       ├── ReminderPicker.kt
│       ├── GoalInput.kt
│       ├── ValueTagSelector.kt
│       └── DialogActions.kt
└── PlannerViewModel.kt        (unchanged)
```

---

## Acceptance Criteria per Step

| Step | Build | Device Test | Preview Test |
|------|-------|-------------|--------------|
| 1    | ✅    | ✅ Day selector works | ✅ |
| 2    | ✅    | ✅ Header shows counts | ✅ |
| 3    | ✅    | ✅ Empty state visible | ✅ |
| 4    | ✅    | ✅ Task list renders | ✅ |
| 5    | ✅    | ✅ Full dialog flow works | ✅ Each sub-component |

---

## Risk Mitigation

1. **Build after EACH step** — never batch multiple extractions
2. **Device test after EACH step** — user already has workflow
3. **Keep `@Preview` annotations** on new composables for quick iteration
4. **No logic changes** — pure UI extraction, ViewModel untouched
5. **Comment-out old code** (not delete) until verified — user preference

---

## Estimated Effort

| Step | Lines Extracted | New Files | Est. Time |
|------|----------------|-----------|-----------|
| 1    | ~54            | 1         | 15 min    |
| 2    | ~24            | 1         | 10 min    |
| 3    | ~30            | 1         | 10 min    |
| 4    | ~110           | 1         | 20 min    |
| 5    | ~312           | 8         | 45 min    |
| **Total** | **~530** | **12** | **~1.5 hr** |

---

## Next Action

Start with **Step 1: DaySelector** — lowest risk, highest confidence, sets pattern for rest.
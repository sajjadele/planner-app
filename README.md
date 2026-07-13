# Vision Planner

Vision Planner is **not** a task management application. It is a goal-oriented system designed to transform personal goals into daily actionable tasks and to learn from behavioral patterns — building the foundation for a future AI Coach.

---

## Product Philosophy

> *"Creating a task should feel like capturing a thought, not filling a form."*

**Non-negotiable principles:**

1. **The Goal is more important than the Task.** A task is merely a unit of execution.
2. **Simplicity in input, depth in analysis.** Task creation must be rapid and frictionless. The system builds deep context over time.
3. **Collect signal, not noise.** We only capture data that has the potential to generate meaningful insights.

### Core Mental Model

```
Goal → Task → Event → Insight
```

- **Goal** — Long-term objective (e.g., "Learn AI Engineering")
- **Task** — Unit of execution, optionally linked to a Goal
- **Event** — Behavioral signal (created, completed, rescheduled, deleted)
- **Insight** — Analysis computed from event history

---

## Feature Status

### Implemented (Phase 1–3 Complete)

| Feature | Status | Description |
|---|---|---|
| Goal Dashboard | ✅ | Create, complete, pause, resume, abandon, delete goals |
| Goal Detail Screen | ✅ | View goal stats, linked tasks, completion rate |
| Planner (Task System) | ✅ | Day-based task creation, completion, undo, delete |
| Task Detail & Editing | ✅ | Edit title, reassign goal, add progress logs |
| Goal–Task Linking | ✅ | FK relationship, goal picker in task creation |
| Task Logs (Notes) | ✅ | Attach progress notes to tasks with timestamps |
| Behavior Insights | ✅ | Streak, velocity, procrastination alerts, goal neglect |
| Life Area Categories | ✅ | 6 fixed life areas for task organization |
| Reminder System | ✅ | AlarmManager-based task reminders with boot restore |
| Cross-Module Search | ✅ | Search across tasks and notes |
| Persian/Farsi UI | ✅ | RTL layout, Persian digits, Persian week (Saturday start) |
| Neumorphic Design | ✅ | Custom NeumorphicSurface, consistent elevation system |
| Plugin Architecture | ✅ | AppPlugin interface, PluginRegistry, Module Settings |
| Offline-First | ✅ | 100% local, zero network dependency |
| First-Run Onboarding | ✅ | Guided 2-step "Value Discovery Journey" (Goal → Task) shown once; creates first goal + task, deep-links to the new goal |
| Light / Dark / System Theme | ✅ | ThemeSettingsDialog + DataStore-backed ThemeRepository + `LocalIsDarkTheme` |
| Jalali Calendar Picker | ✅ | True Jalali month grid in a bottom sheet; month nav + month picker |
| Holiday Awareness | ✅ | Bundled `holidays.json`; Iranian holidays marked in calendar + shown in day context |
| Infinite Week Navigation | ✅ | Snap-scrolling horizontal week row (±19 years) |
| Real-Time Search | ✅ | Instant search across tasks + notes with badges, priority markers, one-tap jump |
| Quick Notes | ✅ | Standalone notes screen (top-bar) with add/delete; separate from task-linked logs |
| Task Priority | ✅ | HIGH / MEDIUM / LOW priority set in AddTaskDialog, shown on tasks |
| Weekly Insight Card | ✅ | Compact ring + streak + best day, expandable to a detail bottom sheet |
| Goal Context Menu | ✅ | Long-press a goal for complete / pause / resume / abandon / delete |

### Feature Notes

Details on features not obvious from the table above:

- **First-Run Onboarding (`core/onboarding`, `ui/onboarding`).** On first launch the app shows a two-step, RTL, animated flow: `OnboardingGoalScreen` (pick/create a long-term goal, with suggestion chips) → `OnboardingTaskScreen` (capture one small task and explicitly *link* it to the goal via a connector node). Submit is disabled until the task is linked, teaching the goal→task model. On finish it writes a real Goal + Task to the DB, sets `OnboardingRepository.isCompleted`, and deep-links into `GoalDetailScreen` for the new goal (`OnboardingDeepLink`). Motion is reduced-motion aware.
- **Theming (`core/preferences/ThemeRepository`, `ui/theme`, `ui/screens/components/ThemeSettingsDialog`).** `ThemeMode` (LIGHT / DARK / SYSTEM) is persisted in DataStore. `LocalIsDarkTheme` exposes the resolved mode to composables; all colors are read from `MaterialTheme.colorScheme.*`. The dialog is reached from the top-bar settings icon.
- **Jalali Calendar Picker (`planner/ui/components/DaySelector.kt` → `CalendarPopup`).** A `ModalBottomSheet` rendering a real Jalali month grid (Sat→Fri headers, Persian digits), with prev/next month and a month-picker grid. Selecting a day jumps the Planner to that date.
- **Holidays (`core/data/HolidayRepository`, `assets/holidays.json`).** Offline holiday data; holiday days get a red marker in the calendar grid and are listed in the expandable **Day Context panel** (`DayContextPanel`) alongside the Gregorian date.
- **Infinite Week Row (`InfiniteWeekRow`).** Replaces a fixed day selector — a snapping `LazyRow` of weeks spanning ~±19 years, centered on the selected date.
- **Real-Time Search (`core/search/SearchDialog`).** A full-screen dialog with live filtering across tasks and notes (separate result types), showing day/priority/completion badges. Tapping a task jumps the Planner to that task's date; tapping a note opens the Notes tab.
- **Quick Notes (`plugins/notes`).** A standalone notes screen (top-bar 📝) for ad-hoc capture and deletion, distinct from the task-linked progress logs created in `TaskDetailScreen`.
- **Weekly Insight Card (`planner/ui/components/WeeklyInsightCard`).** Compact `NeumorphicSurface` with a completion-rate ring, streak 🔥, and best day; the "بیشتر" affordance opens `InsightDetailsSheetContent` (velocity, procrastination alerts, neglected goal, life-area distribution, unorganized count).
- **Goal Context Menu.** Long-press on a `GoalCard` opens an `AlertDialog` menu for complete / pause / resume / abandon / delete.

### Deferred (Per Vision Doc)

- ❌ Graph View
- ❌ AI Chat Interface
- ❌ Social/Community Features
- ❌ Complex Note-Taking System
- ❌ User-Facing Plugin/Extension System

---

## Architecture

### Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Database | Room (KSP annotation processing) |
| Async | Coroutines + Flow |
| Architecture | MVVM (AndroidViewModel + StateFlow + Repository) |
| Persistence | Offline-first local storage |
| Build System | Gradle |
| Min SDK | 24 |
| Target SDK | 36 |

### Database Schema (v9)

```
goals          → GoalEntity (id, title, description, status, createdAt, completedAt)
goal_events    → GoalEventEntity (id, goalId FK, eventType, timestamp)  [Phase 2]
goal_progress_snapshot → GoalProgressSnapshotEntity (dateEpochMs, goalId FK, completed, total, rate)  [Phase 3]
behavior_snapshot → BehaviorSnapshotEntity (dateEpochMs PK, completed, created, streak, velocity, rescheduleRate)  [Phase 3]
tasks          → TaskEntity (id, title, priority, isCompleted, dateEpochMs, timestamp, reminderHour, reminderMinute, goalId FK, lifeAreaId, valueTag)
task_events    → TaskEventEntity (id, taskId, eventType, timestamp)
notes          → NoteEntity (id, content, timestamp, goalId?, taskId?)
module_settings → ModuleSettingsEntity (moduleId, isEnabled)
```

**Key relationships:**
- `tasks.goalId` → `goals.id` (ForeignKey, SET_NULL on delete)
- `notes.taskId` → `tasks.id` (indexed, nullable)
- `notes.goalId` → `goals.id` (indexed, nullable)
- `task_events.taskId` → `tasks.id` (indexed)
- `goal_progress_snapshot.goalId` → `goals.id` (ForeignKey, CASCADE on delete)
- `goal_progress_snapshot` / `behavior_snapshot` are **rebuildable projections** of `tasks` +
  `task_events` (Phase 3, see `docs/ADR-0003`) — not authoritative writes.

### Project Structure

```
app/src/main/java/com/example/
│
├── core/
│   ├── constants/          # DateConstants, LifeAreas
│   ├── data/               # HolidayRepository (bundled holidays.json)
│   ├── database/           # AppDatabase (v9), ModuleSettings
│   ├── domain/             # CalendarDate, Holiday, DayContext
│   ├── goal/               # GoalEntity, GoalDao, GoalRepository
│   └── snapshot/           # Phase 3: GoalProgressSnapshotEntity, BehaviorSnapshotEntity,
│                       # SnapshotDao, SnapshotRepository, SnapshotAggregator
│   ├── onboarding/         # OnboardingStep, OnboardingViewModel, OnboardingRepository, OnboardingDeepLink
│   ├── plugin/             # AppPlugin, PluginRegistry, ModuleSettingsViewModel
│   ├── preferences/        # ThemeRepository (DataStore)
│   ├── receiver/           # BootReceiver, ReminderReceiver, ReminderScheduler
│   ├── search/             # SearchDialog, SearchViewModel
│   └── util/               # DateTimeUtils, PersianDigits, JalaliDate
│
├── plugins/
│   ├── goals/              # GoalsPlugin, GoalDashboardScreen, GoalDetailScreen
│   │   └── ui/             # GoalViewModel, GoalDetailViewModel, GoalCard, AddGoalDialog, EditGoalDialog
│   ├── notes/              # NotesPlugin
│   │   ├── data/           # NoteEntity, NoteDao, NoteRepository
│   │   └── ui/             # NotesScreen, NotesViewModel
│   └── planner/            # PlannerPlugin
│       ├── data/           # TaskEntity, TaskEventEntity, TaskDao, TaskEventDao,
│       │                   # InsightDao, TaskRepository
│       └── ui/             # PlannerScreen, PlannerViewModel, TaskDetailScreen,
│           │               # TaskDetailViewModel, WeeklyInsightViewModel
│           └── components/ # AddTaskDialog, TaskCard, InfiniteWeekRow, CalendarPopup,
│           │               # DayContextPanel, NeumorphicComponents, WeeklyInsightCard, PlannerEmptyState
│
├── ui/
│   ├── onboarding/         # OnboardingHost, OnboardingGoalScreen, OnboardingTaskScreen, OnboardingMotion
│   ├── screens/            # MainScreen, VisionBottomBar, MainTopBar
│   └── theme/              # Color, Theme, Type
│
└── MainActivity.kt
```

### Plugin System

Each feature module implements `AppPlugin`:

```kotlin
interface AppPlugin {
    val id: String
    val name: String
    val description: String
    val icon: ImageVector

    @Composable
    fun Content(modifier: Modifier, onNavigateToSettings: () -> Unit, onBack: () -> Unit)
}
```

Registered plugins: **Planner**, **Goals**, **Notes**

Navigation:
- **Bottom bar:** Planner + Goals (primary tabs)
- **Top bar:** Search + Notes + Settings

### Data Flow

```
Room DAO (Flow) → ViewModel (StateFlow via stateIn) → Composable (collectAsState)
```

- All reads are reactive via Room `Flow`
- Writes use `suspend` functions in ViewModels
- No manual refresh needed — Room re-emits on data changes

### One-Shot UI Events

State that must trigger a side-effect **exactly once** (e.g. a snackbar, navigation, a toast) is **never** exposed as sticky `StateFlow` state. Sticky state survives recomposition and re-entry into composition, so an observer keyed on it (e.g. `LaunchedEffect(state)`) will re-fire every time the screen re-enters — for example when switching tabs via `AnimatedContent`.

One-shot side-effects are delivered through a replay-free `Channel` exposed as a `Flow`:

```kotlin
// ViewModel
private val _completionEvents = Channel<TaskEntity>(Channel.BUFFERED)
val completionEvents = _completionEvents.receiveAsFlow()

// emit once at the action site
_completionEvents.trySend(updatedTask)

// UI — keyed on Unit, not on the event value
LaunchedEffect(Unit) {
    viewModel.completionEvents.collect { task ->
        /* show snackbar / navigate / etc. */
    }
}
```

The sticky value (e.g. `_lastCompletedTask`, kept for the snackbar's undo action) remains separate from the event stream, so re-composition cannot re-trigger it.

---

## Behavioral Insights (Phase 3)

The insight system computes high-signal metrics from the event log:

| Metric | Source | Purpose |
|---|---|---|
| Completion Streak | `task_events` (completed) | Consecutive days with completions |
| Weekly Velocity | Current vs previous week | Accelerating / stable / declining |
| Procrastination Alerts | `task_events` (rescheduled ≥ 3x) | Tasks being avoided |
| Neglected Goals | Goal completion rate (FK join) | Goals falling behind |
| Life Area Distribution | Tasks grouped by `lifeAreaId` | Balance across life domains |

All metrics are computed at the database level (SQL aggregation) — no heavy in-memory processing.

---

## UI Design System

**Neumorphic components:**
- `NeumorphicSurface` — elevated card with light/dark shadow simulation
- `NeumorphicCircle` — circular variant for interactive elements

**Color system:** Purple accent (#8B5CF6), gradient backgrounds, consistent surface whites.

**RTL support:** All Persian text wrapped with `‏` RTL marks to prevent BiDi reordering of mixed number/text strings.

---

## Build Instructions

### Requirements

- JDK 17
- Android SDK 36
- Gradle 9+
- Android platform-tools (adb)

### Build

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

### Install on Device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Development Environment

```
VS Code + Android SDK CLI + Gradle CLI + Real Android Device
```

Android Studio is not required.

---

## Development Phases

| Phase | Status | Description |
|---|---|---|
| Phase 1A | ✅ | Schema stabilization — GoalEntity, Task→Goal FK, Note→Goal/Task links, InsightDao extraction |
| Phase 1B | ✅ | Relationship stabilization — Goal picker in AddTaskDialog, Notes context binding |
| Phase 2 | ✅ | Goal Dashboard — Goal CRUD, GoalDetailScreen, Goal stats, navigation |
| Phase 3 | ✅ | Behavior Insights — Streak, velocity, procrastination, goal neglect, RTL fixes |
| Phase 3.5 | ✅ | Task Detail & Editing — TaskDetailScreen, progress logs, edit button, goal reassignment |
| Phase 3.6 | ✅ | Onboarding, theming, Jalali calendar picker, holiday awareness, infinite week nav, real-time search, quick notes, insight card/sheet |
| Phase 4 | 🔜 | AI Integration Layer |

---

## Recent Changes

| Date | Change | Files |
|------|--------|-------|
| 2026-07-12 | **Fixed duplicate task-completion notification.** The completion snackbar re-fired whenever the user switched tabs (`وظایف` ⇄ `اهداف`) and returned, because the trigger was a sticky `StateFlow` (`lastCompletedTask`) observed via `LaunchedEffect(lastCompletedTask)`. Replaced it with a one-shot `Channel`-based event stream (`completionEvents`) consumed by `LaunchedEffect(Unit)`, while keeping `_lastCompletedTask` as the undo target. | `PlannerViewModel.kt`, `PlannerScreen.kt` |

---

## Constraints

The project intentionally avoids:

- Premature architecture expansion
- Framework-first development
- Feature accumulation without validation
- AI-first workflows before product validation
- Network dependencies
- Mandatory organization (hierarchy is always optional)

```text
Build the smallest stable product
capable of validating the goal-execution hypothesis.
```

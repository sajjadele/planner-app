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

### Database Schema (v4)

```
goals          → GoalEntity (id, title, description, status, createdAt, completedAt)
tasks          → TaskEntity (id, title, priority, isCompleted, dayIndex, goalId FK, lifeAreaId, ...)
task_events    → TaskEventEntity (id, taskId, eventType, timestamp)
notes          → NoteEntity (id, content, timestamp, goalId?, taskId?)
module_settings → ModuleSettingsEntity (moduleId, isEnabled)
```

**Key relationships:**
- `tasks.goalId` → `goals.id` (ForeignKey, SET_NULL on delete)
- `notes.taskId` → `tasks.id` (indexed, nullable)
- `notes.goalId` → `goals.id` (indexed, nullable)
- `task_events.taskId` → `tasks.id` (indexed)

### Project Structure

```
app/src/main/java/com/example/
│
├── core/
│   ├── constants/          # DateConstants, LifeAreas
│   ├── database/           # AppDatabase (v4), ModuleSettings
│   ├── goal/               # GoalEntity, GoalDao, GoalRepository
│   ├── plugin/             # AppPlugin interface, PluginRegistry, ModuleSettingsViewModel
│   ├── receiver/           # BootReceiver, ReminderReceiver, ReminderScheduler
│   ├── search/             # SearchDialog, SearchViewModel
│   └── util/               # DateTimeUtils, PersianDigits
│
├── plugins/
│   ├── goals/              # GoalsPlugin, GoalDashboardScreen, GoalDetailScreen
│   │   └── ui/             # GoalViewModel, GoalDetailViewModel, GoalCard, AddGoalDialog
│   ├── notes/              # NotesPlugin
│   │   ├── data/           # NoteEntity, NoteDao, NoteRepository
│   │   └── ui/             # NotesScreen, NotesViewModel
│   └── planner/            # PlannerPlugin
│       ├── data/           # TaskEntity, TaskEventEntity, TaskDao, TaskEventDao,
│       │                   # InsightDao, TaskRepository
│       └── ui/             # PlannerScreen, PlannerViewModel, TaskDetailScreen,
│           │               # TaskDetailViewModel, WeeklyInsightViewModel
│           └── components/ # AddTaskDialog, TaskCard, DaySelector, NeumorphicComponents,
│                           # WeeklyInsightCard, PlannerEmptyState
│
├── ui/
│   ├── screens/            # MainScreen, MainBottomBar, MainTopBar
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
| Phase 4 | 🔜 | AI Integration Layer |

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

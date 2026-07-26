# Vision Planner

Vision Planner is **not** a task-management app. It is a **goal-oriented progress system** that helps users understand their progress, identify behavioral patterns, and improve their path toward meaningful goals.

---

## Product Philosophy

> "Creating a task should feel like capturing a thought, not filling a form."

**Non-negotiable principles:**

1. **The Goal is more important than the Task.** A task is merely a unit of execution under a goal.
2. **Low friction, high signal.** Task creation must be rapid and optional in structure; the system builds deep context over time.
3. **Feedback, not judgment.** Insights describe behavior and patterns. They do not criticize performance or create guilt.
4. **Invisible data collection.** Insights are derived from natural behavior. No forced journals, daily reflections, or mandatory check-ins.
5. **Offline-first.** Everything stays on-device. No network dependency.

### Core Mental Model

```
Goal → Task → Event → Snapshot → Mirror → Feedback
```

- **Goal** — meaningful direction
- **Task** — smallest execution unit, ideally linked to a goal
- **Event** — state transitions: created, completed, rescheduled, deleted
- **Snapshot** — daily progress/behavior projections
- **Mirror** — pattern detection + feedback inside Goal Dashboard

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
| Behavior Insights | ✅ | Streak, velocity, procrastination alerts |
| Task Priority | ✅ | HIGH / MEDIUM / LOW priority set in AddTaskDialog, shown on tasks |
| Reminder System | ✅ | AlarmManager-based task reminders with boot restore |
| Cross-Module Search | ✅ | Search across tasks and notes |
| Persian/Farsi UI | ✅ | RTL layout, Persian digits, Persian week (Saturday start) |
| Neumorphic Design | ✅ | Custom NeumorphicSurface, consistent elevation system |
| Plugin Architecture | ✅ | AppPlugin interface, PluginRegistry, Module Settings |
| Offline-First | ✅ | 100% local, zero network dependency |
| First-Run Onboarding | ✅ | Guided 2-step value-discovery flow |
| Light / Dark / System Theme | ✅ | DataStore-backed ThemeRepository |
| Jalali Calendar Picker | ✅ | True Jalali month grid in a bottom sheet |
| Holiday Awareness | ✅ | Bundled holiday data; Iranian holidays in calendar/day context |
| Infinite Week Navigation | ✅ | Snap-scrolling horizontal week row |
| Real-Time Search | ✅ | Instant search across tasks + notes |
| Quick Notes | ✅ | Standalone notes screen |
| Weekly Insight Card | ✅ | Compact insight summary with expandable detail sheet |
| Mirror Engine (V1) | ✅ | `domain.mirror` + `core.mirror`; patterns Boulder, Initiator/Finisher, Goal Attention, Consistency Decay |
| Mirror Feedback UI | ✅ | `MirrorFeedbackCard` inside Goal Detail; neutral language, no separate screen |

### In Progress / Planned

| Feature | Status | Notes |
|---|---|---|
| Task Inbox behavior | 🔜 | Task creation without a goal remains allowed as quick capture |
| Goal-first main layout | 🔜 | Preserve daily task flow while emphasizing goal context |
| Graph visualization | 🔜 | Simple Goal→Task graph, computed on demand, not stored |

### Deferred / Out of Scope

- ❌ AI Chat Interface
- ❌ Social/Community Features
- ❌ Complex Note-Taking System
- ❌ User-Facing Plugin/Extension System
- ❌ Life Area as a first-class entity
- ❌ Network sync or remote AI

---

## Architecture

### Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Database | Room |
| Async | Coroutines + Flow |
| Architecture | MVVM |
| Persistence | Offline-first local storage |
| Build System | Gradle |
| Min SDK | 24 |
| Target SDK | 36 |

### Database Schema (v9)

```
goals          → GoalEntity (id, title, description, status, createdAt, completedAt)
goal_events    → GoalEventEntity (id, goalId FK, eventType, timestamp)
goal_progress_snapshot → GoalProgressSnapshotEntity (dateEpochMs, goalId FK, completed, total, rate)
behavior_snapshot → BehaviorSnapshotEntity (dateEpochMs PK, completed, created, streak, velocity, rescheduleRate)
tasks          → TaskEntity (id, title, priority, isCompleted, dateEpochMs, timestamp, reminderHour, reminderMinute, goalId FK, valueTag)
task_events    → TaskEventEntity (id, taskId, eventType, timestamp)
notes          → NoteEntity (id, content, timestamp, goalId?, taskId?)
module_settings → ModuleSettingsEntity (moduleId, isEnabled)
```

**Key relationships:**
- `tasks.goalId` → `goals.id` (ForeignKey, SET NULL on delete)
- `notes.taskId` → `tasks.id` (indexed, nullable)
- `notes.goalId` → `goals.id` (indexed, nullable)
- `task_events.taskId` → `tasks.id` (indexed)
- `goal_progress_snapshot` / `behavior_snapshot` are **rebuildable projections** of `tasks` + `task_events`

### Project Structure

```
app/src/main/java/com/example/
│
├── core/
│   ├── database/           # AppDatabase (v9)
│   ├── goal/               # GoalEntity, GoalDao, GoalRepository
│   ├── snapshot/           # GoalProgressSnapshotEntity, BehaviorSnapshotEntity,
│   │                        # SnapshotDao, SnapshotRepository, SnapshotAggregator
│   ├── mirror/             # MirrorRepository, RoomMirrorRepository
│   ├── onboarding/         # OnboardingStep, OnboardingViewModel, OnboardingRepository
│   ├── plugin/             # AppPlugin, PluginRegistry, ModuleSettingsViewModel
│   ├── preferences/        # ThemeRepository (DataStore)
│   ├── receiver/           # BootReceiver, ReminderReceiver, ReminderScheduler
│   ├── search/             # SearchDialog, SearchViewModel
│   └── util/               # DateTimeUtils, PersianDigits, JalaliDate
│
├── domain/
│   ├── insight/            # Streak, rate, velocity, procrastination calculators
│   ├── snapshot/           # Daily goal progress + behavior projection math
│   └── mirror/             # MirrorEngine, MirrorHeuristics, MirrorSignal, MirrorInsight
│
├── plugins/
│   ├── goals/              # GoalsPlugin, GoalDashboardScreen, GoalDetailScreen
│   │   └── ui/             # GoalViewModel, GoalDetailViewModel, GoalCard
│   ├── notes/              # NotesPlugin
│   │   ├── data/           # NoteEntity, NoteDao, NoteRepository
│   │   └── ui/             # NotesScreen, NotesViewModel
│   │   └── ui/components/  # Neumorphic components, dialogs, insight card
│   └── planner/            # PlannerPlugin
│       ├── data/           # TaskEntity, TaskEventEntity, TaskDao, TaskEventDao,
│       │                   # InsightDao, TaskRepository
│       └── ui/             # PlannerScreen, PlannerViewModel, TaskDetailScreen,
│           │               # TaskDetailViewModel, WeeklyInsightViewModel
│           └── components/ # AddTaskDialog, TaskCard, DaySelector,
│                           # DayContextPanel, WeeklyInsightCard, PlannerEmptyState
│
├── ui/
│   ├── onboarding/         # OnboardingHost, OnboardingGoalScreen, OnboardingTaskScreen
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
- **Bottom bar:** Planner + Goals
- **Top bar:** Search + Notes + Settings

### Data Flow

```
Room DAO (Flow) → Repository interface → ViewModel StateFlow → Compose collectAsState
```

- All reads are reactive via Room `Flow`
- Writes use `suspend` functions in ViewModels
- No manual refresh needed — Room re-emits on data changes
- Insight/mirror math lives in pure-Kotlin domain calculators, not ViewModels

### One-Shot UI Events

State that must trigger a side-effect exactly once is never exposed as sticky `StateFlow`.

One-shot side-effects are delivered through a replay-free `Channel` exposed as a `Flow`, keyed on `Unit`, not on the event value.

---

## Behavioral Insights & Mirror (Phase 3 → Phase 3.5)

Current Phase 3 insight metrics:

| Metric | Source | Purpose |
|---|---|---|
| Completion Streak | `task_events` (completed) | Consecutive days with completions |
| Weekly Velocity | Current vs previous week | Accelerating / stable / declining |
| Procrastination Alerts | `task_events` (rescheduled ≥ 3x) | Tasks being avoided |
| Goal Attention | Goal activity over time | Goals receiving less attention recently |

Mirror V1 scope:
- Boulder pattern
- Initiator vs Finisher pattern
- Goal Attention pattern
- Consistency Decay pattern

Mirror architecture:
- `domain.mirror` — pure-Kotlin: `MirrorEngine` (signal → feedback rendering), `MirrorHeuristics` (four detectors), `MirrorSignal` / `MirrorSignalType` / `MirrorInsight`
- `core.mirror` — `MirrorRepository` interface + `RoomMirrorRepository` (wires Insight / Goal / Snapshot repositories)
- UI — `MirrorFeedbackCard` rendered inside the **Goal Detail** screen, driven by `GoalDetailViewModel.mirrorInsights`

Mirror constraints:
- No separate Mirror screen in V1
- Feedback appears inside the Goal experience (currently Goal Detail)
- No forced input, no judgmental language
- Real analysis matures after roughly 7 days of usage

---

## UI Design System

**Neumorphic components:**
- `NeumorphicSurface` — elevated card with light/dark shadow simulation
- `NeumorphicCircle` — circular variant for interactive elements

**Color system:** Purple accent (`#8B5CF6`), gradient backgrounds, consistent surface whites.

**RTL support:** All Persian text uses RTL mark characters to prevent BiDi reordering.

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

VS Code + Android SDK CLI + Gradle CLI + Real Android Device  
Android Studio is not required.

---

## Development Phases

| Phase | Status | Description |
|---|---|---|
| Phase 1 | ✅ | Schema stabilization, Goal→Task FK, InsightDao extraction |
| Phase 2 | ✅ | Architecture stabilization, domain layer, `goal_events` |
| Phase 3 | ✅ | Progress & behavior snapshots, backfill engine |
| Phase 4 | ✅ | Mirror Engine Foundation: heuristics, feedback engine, Goal Detail integration |
| Phase 5 | 🔜 | Goal Experience Evolution: strengthen goal-first daily UX, Inbox separation |
| Phase 6 | 🔜 | Graph Exploration: simple Goal→Task graph, computed on demand |
| Phase 7 | 🔜 | AI Insight Generator: consumes structured data and Mirror outputs |

---

## Constraints

The project intentionally avoids:
- Premature architecture expansion
- Framework-first development
- Feature accumulation without validation
- AI-first workflows before product validation
- Network dependencies
- Mandatory organization

---

*For the latest product decisions, see `PRODUCT_DIRECTION_DECISION_DOCUMENT.md`.*  
*For current architecture status, see `docs/ARCHITECTURE_STATE.md`.*  
*For implementation roadmap, see `docs/ROADMAP.md`.*

# Vision Planner — Product & Architecture Reference

> **Generated:** 2026-07-10  
> **Purpose:** This document is a complete reference for the current state of the Vision Planner project. It is designed to be given to AI models (or human developers) to provide full context for discussions, code generation, and architectural decisions.

---

## 1. Product Identity

### What is Vision Planner?

Vision Planner is **not** a task management application. It is:

- A **goal-oriented system** that transforms personal long-term goals into daily actionable tasks.
- A **behavioral learning engine** that analyzes the user's execution patterns.
- An **offline-first, privacy-preserving** Android application built entirely on-device.

### Core Philosophy (3 Principles)

| # | Principle | Meaning |
|---|-----------|---------|
| 1 | **The Goal is more important than the Task.** | A task is merely a unit of execution under a goal. Tasks without goals are noise. |
| 2 | **Simplicity in input, depth in analysis.** | Task creation must be frictionless (2 taps). The system builds deep context over time. |
| 3 | **Collect signal, not noise.** | Only high-signal behavioral data is stored (completion events, reschedule counts, delays). Not every interaction. |

---

## 2. Mental Model

```
Goal (long-term objective)
 │
 ├── Task (unit of execution, linked to a Goal)
 ├── Note (progress log, linked to a Task)
 ├── Event (state transitions: created, completed, rescheduled, deleted)
 └── Insight (weekly behavioral analysis from event data)
```

### Practical Example

```
Goal: "Learn AI Engineering"
 ├── Task: "Learn FastAPI"          [linked to goalId]
 ├── Task: "Build API Auth"         [linked to goalId]
 ├── Note: "FastAPI architecture notes"  [logged under task]
 ├── Event: TaskCompleted { taskId, timestamp }
 └── Insight: "Backend tasks are improving. Testing is being neglected."
```

---

## 3. Data Models

### 3.1 GoalEntity (`goals` table)

| Field | Type | Notes |
|-------|------|-------|
| `id` | `Int` (PK, autoGenerate) | |
| `title` | `String` | |
| `description` | `String?` | |
| `status` | `String` | `"active"` / `"completed"` / `"paused"` / `"abandoned"` |
| `createdAt` | `Long` | epoch millis |
| `completedAt` | `Long?` | |

### 3.2 TaskEntity (`tasks` table)

| Field | Type | Notes |
|-------|------|-------|
| `id` | `Int` (PK, autoGenerate) | |
| `title` | `String` | |
| `priority` | `String?` | `"HIGH"` / `"MEDIUM"` / `"LOW"` |
| `isCompleted` | `Boolean` | |
| `dateEpochMs` | `Long` | Midnight epoch ms of the scheduled day (local timezone) |
| `reminderHour` | `Int?` | 0–23 |
| `reminderMinute` | `Int?` | 0–59 |
| `goalId` | `Int?` | FK → goals.id (SET NULL on delete). **Only** source of goal linkage |
| `lifeAreaId` | `Int?` | FK → LifeAreas fixed list |
| `valueTag` | `String?` | |
| `timestamp` | `Long` | epoch millis |

### 3.3 TaskEventEntity (`task_events` table)

| Field | Type | Notes |
|-------|------|-------|
| `id` | `Long` (PK, autoGenerate) | |
| `taskId` | `Int` | |
| `eventType` | `String` | `"created"` / `"completed"` / `"reopened"` / `"deleted"` / `"rescheduled"` / `"priority_changed"` |
| `timestamp` | `Long` | epoch millis |

### 3.4 NoteEntity (`notes` table)

| Field | Type | Notes |
|-------|------|-------|
| `id` | `Int` (PK, autoGenerate) | |
| `content` | `String` | |
| `taskId` | `Int?` | Optional link to task |
| `timestamp` | `Long` | epoch millis |

### 3.5 WeeklyInsightState (in-memory, not persisted)

```kotlin
data class WeeklyInsightState(
    completedCount: Int,
    createdCount: Int,
    completionRate: Float,          // %
    lifeAreaBreakdown: List<LifeAreaCompletion>?,
    unorganizedCount: Int,
    streakDays: Int,                // consecutive days with completions
    bestDayIndex: Int?,
    bestDayCount: Int,
    procrastinationAlerts: List<ProcrastinationAlert>,  // tasks rescheduled ≥3x
    neglectedGoalTitle: String?,   // goal with lowest completion rate
    neglectedGoalRate: Float,
    weeklyVelocity: Velocity,       // IMPROVING / STABLE / DECLINING
    weeklyVelocityPercent: Float
)
```

Key insight types derived from event data:
- **ProcrastinationAlert:** A task that has been rescheduled 3+ times.
- **WeeklyVelocity:** Current week's completion rate vs. previous week's.
- **NeglectedGoal:** The goal with the lowest completion rate across its tasks.
- **StreakDays:** Consecutive days where at least one task was completed.

### 3.6 GoalEventEntity (`goal_events` table) — Phase 2

Append-only audit trail of goal lifecycle transitions (distinct from `task_events`, which tracks
Task actions). Required later for behavioral analysis (Phase 3) and the AI Coach (Phase 5).

| Field | Type | Notes |
|-------|------|-------|
| `id` | `Long` (PK, autoGenerate) | |
| `goalId` | `Int` | FK → goals.id (CASCADE on delete) |
| `eventType` | `String` | `"created"` / `"completed"` / `"paused"` / `"resumed"` / `"abandoned"` |
| `timestamp` | `Long` | epoch millis |

Written by `GoalViewModel` on every lifecycle transition; never updated or deleted.

### 3.7 GoalProgressSnapshotEntity / BehaviorSnapshotEntity (`goal_progress_snapshot` / `behavior_snapshot`) — Phase 3

Two **rebuildable projection** tables (DB v9) that store progress/behavior over time so future
features (Graph View, AI Coach) read one source instead of recomputing from raw events. They are
always recomputable from `tasks` + `task_events`; the raw events remain the single source of truth.
See `docs/ADR-0003`.

**`goal_progress_snapshot`** — per goal, per day (PK = `dateEpochMs` + `goalId`):

| Field | Type | Notes |
|-------|------|-------|
| `dateEpochMs` | `Long` | midnight epoch of the day (PK part) |
| `goalId` | `Int` | FK → goals.id (CASCADE on delete), PK part |
| `completed` | `Int` | tasks for this goal completed on the day |
| `total` | `Int` | tasks for this goal scheduled on the day |
| `rate` | `Float` | completion rate % (`completed/total`) |

**`behavior_snapshot`** — one row per day (PK = `dateEpochMs`), task-focused (no `goal_events`
coupling, no `lifeAreaId` — per-life-area detail stays re-derivable via `InsightDao`):

| Field | Type | Notes |
|-------|------|-------|
| `dateEpochMs` | `Long` | midnight epoch of the day (PK) |
| `completed` | `Int` | tasks completed that day |
| `created` | `Int` | tasks scheduled that day |
| `streak` | `Int` | completion streak **as of** that day (end-of-day) |
| `velocity` | `String` | `Velocity` enum name: `IMPROVING` / `STABLE` / `DECLINING` (vs previous day) |
| `rescheduleRate` | `Float` | reschedules that day ÷ tasks created that day |

**Refresh strategy:** real-time upsert of *today* via `SnapshotAggregator.recordDay(today)` (fired
from `PlannerViewModel` and `GoalDetailViewModel` after task writes) + an App-Launch Backfill Engine
(`SnapshotAggregator.backfillIfNeeded()` from `MainActivity`) that fills any missing historical day.
No `WorkManager`/scheduler.

---

## 4. Architecture — Plugin System

### 4.1 AppPlugin Interface

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

### 4.2 Registered Plugins

| Plugin | ID | Tab Name | Description |
|--------|----|----------|-------------|
| **PlannerPlugin** | `"planner"` | وظایف | Fast task capture + weekly insights + reminders |
| **GoalsPlugin** | `"goals"` | اهداف | Long-term goal management + progress tracking |
| **NotesPlugin** | `"notes"` | یادداشت سریع | Quick notes/logs (access via top bar icon, not bottom nav) |

All plugins registered in `PluginRegistry.allPlugins`. The bottom navigation bar only shows `primaryTabIds = listOf("planner", "goals")`.

### 4.3 Plugin Flow

```
MainScreen
 ├── MainTopBar (title, search, notes, settings buttons)
 ├── MainBottomBar (tab strip: Planner | Goals)
 └── AnimatedContent(tabId)
      └── PluginRegistry.find(tabId).Content()
           ├── PlannerPlugin → PlannerScreen
           ├── GoalsPlugin → GoalDashboardScreen
           └── (notes opened via top bar → NotesScreen)
```

Tab switching uses `AnimatedContent` with a fade transition — no Navigation Component, no back stack. Task/goal detail screens are shown inline (they `return` early from their parent composable, replacing the list view).

### 4.4 Domain Layer & Repository Boundary (Phase 2)

Pure-Kotlin, framework-free logic lives in `com.example.domain.*` (no Android imports), so it
is unit-testable on the host JVM without Robolectric or an emulator.

- `domain.insight.InsightCalculator` — all insight math (streak, completion rate, weekly
  velocity, procrastination detection, neglected-goal, Persian week range). Extracted from
  `WeeklyInsightViewModel` in Phase 2; the ViewModel now only assembles the reactive
  `StateFlow` and delegates computation to the calculator.
- `domain.insight.Velocity` / `ProcrastinationAlert` — domain models (moved out of the UI
  package so the domain layer does not depend on the UI).

**Repository boundary (Phase 2):** consumers depend on repository *interfaces*, not DAOs.

- `GoalRepository` (interface) → `RoomGoalRepository` (wraps `GoalDao` + `GoalEventDao`).
- `InsightRepository` (interface) → `RoomInsightRepository` (wraps `InsightDao`).
- `WeeklyInsightViewModel` and `GoalDetailViewModel` consume these interfaces; `PlannerViewModel`
  still reaches `TaskDao` / `GoalDao` directly (a later DI refactor is intentionally deferred —
  see ROADMAP Phase 2 scope guards).

**Data flow (reactive):** `DAO Flow → Repository interface → stateIn / combine → ViewModel
StateFlow → Compose collectAsState`. Insight derivation happens in the pure `InsightCalculator`,
keeping `AndroidViewModel`s free of analytic logic.

---

## 5. Theme System

### 5.1 Architecture

Light/Dark/System mode controlled via `ThemeRepository` (DataStore Preferences):

```
MainActivity
 └── ThemeRepository (DataStore) → themeMode: Flow<ThemeMode>
      └── MyApplicationTheme(themeMode) → DarkColorScheme or LightColorScheme
           └── CompositionLocalProvider(LocalIsDarkTheme)
                └── MaterialTheme(content)
```

### 5.2 ThemeMode Enum

```kotlin
enum class ThemeMode { LIGHT, DARK, SYSTEM }
```

Persisted in DataStore under key `"theme_mode"`. Defaults to `SYSTEM`.

### 5.3 Color Palettes

**Light Mode:** Purple/Indigo on soft lavender

| Token | Hex | Usage |
|-------|-----|-------|
| `background` | `#F0E6FF` | All screen backgrounds |
| `surface` | `#FFFFFF` | All cards (Neumorphic, tasks, goals, insights) |
| `onBackground` | `#1C1B1F` | Header titles, dates, nav arrows |
| `onSurface` | `#211A2D` | Text inside cards |
| `onSurfaceVariant` | `#5B5270` | Secondary text, muted labels |
| `primary` | `#8A56EC` | FAB, active day, progress rings, checkboxes |
| `surfaceVariant` | `#F8F4FF` | Subtle alt backgrounds |
| `outline` | `#DDD6E8` | Borders, dividers |
| `error` | `#B3261E` | Delete actions, high priority |

**Dark Mode:** Midnight blue with Cyan accent

| Token | Hex | Usage |
|-------|-----|-------|
| `background` | `#0F172A` | All screen backgrounds |
| `surface` | `#1E293B` | All cards |
| `surfaceVariant` | `#263548` | Elevated surfaces |
| `onBackground` | `#F8FAFC` | Header titles, dates, nav arrows |
| `onSurface` | `#F1F5F9` | Text inside cards |
| `onSurfaceVariant` | `#CBD5E1` | Secondary text |
| `primary` | `#38BDF8` | FAB, active day, progress rings, checkboxes |
| `outline` | `#334155` | Borders |
| `outlineVariant` | `#334155` | Dividers |
| `error` | `#EF4444` | Delete actions |

### 5.4 Critical Rule (enforced in code)

All composables must read colors from `MaterialTheme.colorScheme.*` — **no** hardcoded `if (isDark) X else Y` ternaries in screen-level files. The only exceptions are:
- Neumorphic shadow colors (ambient/spot) which vary by theme and aren't part of `ColorScheme`.
- Semantic colors outside the scheme (`AccentFire`, `AccentBlue`, `AccentGreen`).

### 5.5 ThemeSettingsDialog

Accessible from the top-right settings icon in `MainTopBar`. Three radio-button options:
- **حالت روشن** (Light) — forces light mode
- **حالت تاریک** (Dark) — forces dark mode
- **هماهنگ با سیستم** (System) — follows device setting

---

## 6. UI Screen Map

### 6.1 Screen Hierarchy

```
MainActivity
 └── MainScreen
      ├── MainTopBar
      │    ├── Title "Vision Planner"
      │    ├── Persian date
      │    ├── Search button → SearchDialog
      │    ├── Quick Notes button → NotesScreen (sets tabId="notes")
      │    └── Settings button → ThemeSettingsDialog
      ├── MainBottomBar
      │    ├── [وظایف] → PlannerScreen
      │    └── [اهداف] → GoalDashboardScreen
      └── AnimatedContent (tab content)

PlannerScreen
 ├── WeeklyInsightCard (compact: ring + stats + "بیشتر" expander)
 ├── DaySelector (Persian week day circles)
 ├── Section header (day name + progress counter)
 ├── Task list (LazyColumn of TaskCards) or PlannerEmptyState
 ├── FAB (+) → AddTaskDialog
 └── ModalBottomSheet (InsightDetailsSheetContent, triggered by "بیشتر")

TaskDetailScreen (replaces PlannerScreen inline)
 ├── Back arrow + "جزئیات تسک" header
 ├── Editable title OutlinedTextField
 ├── Goal reassignment dropdown
 ├── Completion Switch toggle
 ├── Reminder time picker (native TimePickerDialog)
 ├── Log section header
 ├── TaskLogs list (LazyColumn of NoteEntity items)
 └── Quick-capture log input + Send button

GoalDashboardScreen
 ├── Section header + active goal count
 ├── Goal list (LazyColumn of GoalCards) or empty state
 ├── FAB (+) → AddGoalDialog
 └── Long-press AlertDialog menu (complete/pause/resume/abandon/delete)

GoalDetailScreen (replaces GoalDashboardScreen inline)
 ├── Back arrow + goal title + edit icon
 ├── Goal info NeumorphicSurface (status badge, description, stats row)
 ├── "تسک‌های مرتبط" header
 ├── Task rows list (LazyColumn of GoalDetailTaskRows)
 └── EditGoalDialog (triggered by edit icon)

NotesScreen
 ├── Back arrow + "یادداشت سریع" header
 └── Notes list
```

### 6.2 Dialog Components

| Dialog | Trigger | Location |
|--------|---------|----------|
| `AddTaskDialog` | FAB (+) | `PlannerScreen` |
| `AddGoalDialog` | FAB (+) | `GoalDashboardScreen` |
| `EditGoalDialog` | Edit icon in header | `GoalDetailScreen` |
| `ThemeSettingsDialog` | Settings icon | `MainTopBar` |
| `SearchDialog` | Search icon | `MainTopBar` |
| `GoalContextMenu` | Long-press on GoalCard | `GoalDashboardScreen` |
| `TimePickerDialog` | Reminder tap | `AddTaskDialog`, `TaskDetailScreen` |

---

## 7. UI Components Library

### 7.1 NeumorphicComponents

- **`NeumorphicSurface`** — Card with soft dual-shadow effect. Defaults: `backgroundColor = colorScheme.surface`, elevation-based blur. Reads `LocalIsDarkTheme` for shadow colors (ambient/spot).
- **`NeumorphicCircle`** — Circular container. Defaults: `selectedColor = colorScheme.primary`, `unselectedBackground = colorScheme.surfaceVariant`.

### 7.2 Common Patterns

- **FAB placement:** Bottom-end corner, `8.dp` padding from bottom, `16.dp` from end. Tight gap above bottom nav bar.
- **ModalBottomSheet:** Used for insight details. `containerColor = colorScheme.surface`.
- **Empty states:** Persian text + emoji centered in the available space.
- **RTL:** Persian text uses the `RTL` mark character (`‏ ‏`) to prevent BiDi reordering of numbers.

---

## 8. Database

### 8.1 AppDatabase (Room, version 9)

**Tables:** `goals`, `goal_events`, `goal_progress_snapshot`, `behavior_snapshot`, `tasks`,
`task_events`, `notes`, `module_settings`

Migration strategy: `fallbackToDestructiveMigration()` — safe for local offline development.

**Migrations:** `MIGRATION_5_6` (dayIndex → dateEpochMs), `MIGRATION_6_7` (drop `goalName`),
`MIGRATION_7_8` (add `goal_events` for goal lifecycle signal), `MIGRATION_8_9` (add
`goal_progress_snapshot` + `behavior_snapshot` projection tables, Phase 3).

**DAOs:** `GoalDao`, `GoalEventDao`, `SnapshotDao`, `TaskDao`, `TaskEventDao`, `InsightDao`, `NoteDao`, `ModuleSettingsDao`

### 8.2 Key Queries

- `TaskDao.getTasksForDay(dateEpochMs)` — tasks for PlannerScreen filtered by selected day
- `TaskDao.getActiveReminders()` — tasks with non-null reminder, used by BootReceiver
- `InsightDao.observeWeeklyStats(...)` — weekly completion + streak + best day
- `InsightDao.observeGoalCompletionRate(goalId)` — per-goal rate
- `InsightDao.getProcrastinationAlerts(...)` — tasks rescheduled ≥3 times
- `InsightDao.observeWeeklyVelocity(...)` — compares current vs. previous week completion rates
- `SnapshotDao.observeBehaviorRange(start, end)` — daily behavior time-series (Phase 3/4)
- `SnapshotDao.observeGoalProgress(goalId)` — per-goal daily progress trend (Phase 4)

---

## 9. Notification & Alarm System

### 9.1 Components

- **`ReminderScheduler`** — schedules `AlarmManager.setExactAndAllowWhileIdle` for task reminders. Computes the next occurrence from `dateEpochMs` (scheduled day) + `reminderHour`/`reminderMinute`. Cancels via `PendingIntent` lookup.
- **`ReminderReceiver`** (BroadcastReceiver) — shows a system notification with priority markers (`🚨` for HIGH, `☕` for LOW). Creates notification channel `"vision_planner_reminders"`.
- **`BootReceiver`** (BroadcastReceiver) — re-schedules all active reminders after device reboot. Queries `TaskDao.getActiveReminders()` and re-registers each with `ReminderScheduler`.

### 9.2 Permissions

- `POST_NOTIFICATIONS` requested on Android 13+ (Tiramisu) in `MainActivity.onCreate()`.

---

## 10. Project Structure

```
app/src/main/java/com/example/
│
├── core/
│   ├── constants/
│   │   ├── DateConstants.kt       — Persian week day names
│   │   └── LifeAreas.kt           — 6 fixed life areas (health, learning, family, work, growth, discipline)
│   ├── database/
│   │   ├── AppDatabase.kt         — Room database (v5, 5 tables)
│   │   ├── ModuleSettingsDao.kt
│   │   └── ModuleSettingsEntity.kt
│   ├── goal/
│   │   ├── GoalDao.kt             — Room DAO for goals
│   │   ├── GoalEntity.kt          — Goal data model
│   │   └── GoalRepository.kt
│   ├── plugin/
│   │   ├── AppPlugin.kt           — Plugin interface
│   │   ├── PluginRegistry.kt      — Master list of all plugins
│   │   └── ModuleSettingsViewModel.kt
│   ├── preferences/
│   │   └── ThemeRepository.kt     — DataStore-backed theme persistence
│   ├── receiver/
│   │   ├── BootReceiver.kt        — Re-schedule alarms after reboot
│   │   ├── ReminderReceiver.kt    — Show notification on alarm
│   │   └── ReminderScheduler.kt   — AlarmManager scheduling
│   ├── search/
│   │   ├── SearchDialog.kt
│   │   └── SearchViewModel.kt
│   └── util/
│       ├── DateTimeUtils.kt
│       └── PersianDigits.kt
│
├── plugins/
│   ├── goals/
│   │   ├── GoalsPlugin.kt
│   │   └── ui/
│   │       ├── AddGoalDialog.kt
│   │       ├── EditGoalDialog.kt
│   │       ├── GoalCard.kt
│   │       ├── GoalDashboardScreen.kt
│   │       ├── GoalDetailScreen.kt
│   │       ├── GoalDetailViewModel.kt
│   │       └── GoalViewModel.kt
│   ├── notes/
│   │   ├── data/
│   │   │   ├── NoteDao.kt
│   │   │   ├── NoteEntity.kt
│   │   │   └── NoteRepository.kt
│   │   ├── NotesPlugin.kt
│   │   └── ui/
│   │       ├── NotesScreen.kt
│   │       └── NotesViewModel.kt
│   └── planner/
│       ├── data/
│       │   ├── InsightDao.kt
│       │   ├── TaskDao.kt
│       │   ├── TaskEntity.kt
│       │   ├── TaskEventDao.kt
│       │   ├── TaskEventEntity.kt
│       │   └── TaskRepository.kt
│       ├── PlannerPlugin.kt
│       └── ui/
│           ├── components/
│           │   ├── AddTaskDialog.kt
│           │   ├── DaySelector.kt
│           │   ├── NeumorphicComponents.kt
│           │   ├── PlannerEmptyState.kt
│           │   ├── TaskCard.kt
│           │   └── WeeklyInsightCard.kt
│           ├── PlannerScreen.kt
│           ├── PlannerViewModel.kt
│           ├── TaskDetailScreen.kt
│           ├── TaskDetailViewModel.kt
│           ├── WeeklyInsightState.kt
│           └── WeeklyInsightViewModel.kt
│
├── ui/
│   ├── screens/
│   │   ├── components/
│   │   │   ├── MainBottomBar.kt
│   │   │   ├── MainTopBar.kt
│   │   │   └── ThemeSettingsDialog.kt
│   │   └── MainScreen.kt
│   └── theme/
│       ├── Color.kt       — All color definitions + backward-compat aliases
│       ├── Theme.kt       — MyApplicationTheme, LocalIsDarkTheme, color schemes
│       └── Type.kt        — Typography
│
├── MainActivity.kt
└── AndroidManifest.xml
```

---

## 11. Build Configuration

| Setting | Value |
|---------|-------|
| **Namespace** | `com.example` |
| **App ID** | `com.aistudio.visionplanner.zqxrtv` |
| **minSdk** | 24 |
| **targetSdk** | 36 |
| **compileSdk** | 36 |
| **Language** | Kotlin (Compose) |
| **Database** | Room (with KSP) |
| **Theme** | DataStore Preferences |
| **DI** | None (manual singletons) |
| **Navigation** | No Navigation Component (inline composable switching) |

Frozen dependencies (commented out, preserve for future): Firebase, Retrofit, Moshi, Coil, CameraX, OkHttp, Navigation Compose, Roborazzi.

---

## 12. Completed Work Log

| Phase | Changes |
|-------|---------|
| **Initial project** | AppPlugin interface, Planner/Goals/Notes plugins, Room database |
| **Refactor** | Decomposed main screen and planner screen into components |
| **Bug fixes** | Goal edit button, task edit data loading, animation lag (FastOutSlowIn) |
| **Reminders** | TimePicker in AddTaskDialog, reminder editing in TaskDetailScreen, ReminderScheduler + BootReceiver |
| **UI Part 1** | FAB padding 80→96dp, WeeklyInsightCard → ModalBottomSheet, faded completed tasks alpha(0.5f) |
| **UI Part 2** | Neumorphic shadows, DaySelector polish, header visibility fix |
| **Bottom nav** | Soft shadow, active/inactive icon/text states |
| **Goal UX** | Moved edit from GoalCard to GoalDetailScreen header, streamlined GoalCard |
| **Dark mode infra** | ThemeRepository (DataStore), ThemeSettingsDialog, CompositionLocal (LocalIsDarkTheme), ThemeMode enum |
| **Color overhaul** | Exact hex specs: light purple/indigo, dark midnight blue/cyan |
| **Final refactor** | All 14 UI files migrated from `if(isDark)` hardcoded colors to `MaterialTheme.colorScheme.*` — single source of truth |
| **FAB padding** | Both PlannerScreen and GoalDashboardScreen FABs moved to 8dp from bottom |

---

## 13. Development Roadmap (Planned Phases)

| Phase | Focus | Status |
|-------|-------|--------|
| **Phase 1** | Architecture stabilization (models, DAOs, plugin system) | ✅ Complete |
| **Phase 2** | Goal Dashboard (CRUD, task linkage, detail screen) | ✅ Complete |
| **Phase 3** | Behavior Insights (weekly velocity, procrastination detection, neglected goals) | ✅ Complete |
| **Phase 4** | AI Integration Layer (data schema supports it, not yet built) | ⏳ Future |

### What We Don't Build Yet

- ❌ Graph/chart visualizations
- ❌ AI Chat Interface
- ❌ Social/Community Features
- ❌ Complex Note-Taking System
- ❌ Service-side sync (truly offline-first)

---

## 14. How to Build & Run

```bash
# Debug build
cd /path/to/project
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

No API keys, no Firebase setup, no environment variables required for development builds. The app runs completely offline.

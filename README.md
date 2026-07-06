# Vision Planner

Vision Planner is an offline-first Android task management application built with Kotlin, Jetpack Compose, Room, and Coroutines.

The project originally started as a broader modular productivity system, but is now being intentionally reduced into a focused MVP centered around task execution.

---

# MVP Direction

The current product goal is intentionally narrow:

```text
Validate whether optional lightweight hierarchy
(Goal + LifeArea)
improves task execution compared to a standard todo app.
```

This is NOT currently a full life-planning platform.

The project is being simplified around:

- Fast task capture
- Low-friction task execution
- Optional organization layers
- Offline-first reliability
- Minimal cognitive overhead

---

# Current MVP Scope

## CORE FEATURES

- Task creation
- Task completion tracking
- Optional reminders
- Offline persistence
- Reactive task updates via Flow
- Minimal Compose-based UI

## OPTIONAL ORGANIZATION (In Progress)

The system is evolving toward:

```text
LifeArea → Goal → Task
```

But hierarchy is intentionally:

- optional
- lightweight
- non-blocking
- never required for task creation

Users must always be able to use the app as a simple todo application.

---

# Feature State Model

The codebase contains multiple subsystems, but not all of them are part of the active MVP surface.

## CORE (Active MVP Path)

These systems are currently prioritized:

- Planner / Task system
- Task creation flow (auto-focus, IME Done)
- Task completion flow (with Snackbar Undo)
- Task-centric UX simplification
- Validation infrastructure (TaskEvent logging)
- Future optional hierarchy support (lifeAreaId field ready)

## BACKGROUND (Preserved but De-emphasized)

These systems remain in the codebase but are intentionally hidden from the primary UX:

- Notes system
- Search system
- Reminder infrastructure
- Existing plugin/module architecture

These features:

- remain functional
- are not removed
- are not primary navigation targets
- should not increase cognitive load

## FROZEN

The following areas are intentionally frozen:

- Experimental AI integrations
- Plugin architecture evolution
- Complex architectural expansion
- Framework-style abstraction growth

Frozen means:

- keep existing code
- avoid redesign
- avoid extension
- avoid architectural investment

---

# Current Architecture

The project currently uses:

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Database | Room |
| Async | Coroutines + Flow |
| Architecture | Feature-based modular structure |
| Persistence | Offline-first local storage |
| Build System | Gradle |
| Minimum SDK | 24 |
| Target SDK | 36 |
|

---

# Project Structure

```text
app/src/main/java/com/example/
│
├── core/
│   ├── constants/
│   ├── database/
│   ├── plugin/
│   ├── receiver/
│   └── search/
│
├── plugins/
│   ├── notes/
│   └── planner/
│
├── ui/
│   ├── screens/
│   └── theme/
│
└── MainActivity.kt
```

---

# Architectural Principles

The MVP intentionally prioritizes:

- Simplicity over extensibility
- Execution over planning complexity
- Low friction over feature richness
- Incremental improvement over rewrites
- Minimal diffs over architectural churn
- Stable offline behavior over cloud dependency

Important engineering constraints:

- Room is the single source of truth
- Flow is used for reads
- suspend functions are used for writes
- No new architecture layers unless absolutely necessary
- Existing stable systems should be preserved instead of deleted

---

# Current UX Direction

The application is actively shifting from:

```text
Planning-centric UX
```

Toward:

```text
Task execution-centric UX
```

Recent MVP reductions include:

- Planner becoming the default screen
- Module management removed from primary navigation
- Notes moved into background state
- Reduced exposure of modular architecture concepts
- Simplified product positioning around tasks

---

# Development Environment

The project is intentionally designed for lightweight Android development workflows.

Environment:

```text
VS Code
+ Android SDK CLI
+ Gradle CLI
+ Real Android Device
```

Android Studio is not required.

---

# Build Instructions

## Requirements

- JDK 17
- Android SDK 36
- Gradle 9+
- Android platform-tools

---

## Build Debug APK

```bash
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Install on Device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

# Current Technical Direction

The current development focus is:

## Phase 1 — MVP Reduction ✅

- Simplify UX
- Reduce cognitive overhead
- Remove architectural prominence from UI
- Stabilize task-centric interactions

## Phase 2.1 — Task Flow Polish & Instrumentation ✅

Completed changes for task execution UX:

**Task Flow Simplification:**
- Auto-focus on title field when opening Add Task dialog
- IME "Done" action on keyboard submits the task directly
- Task completion shows a Snackbar with "Undo" (بازگردانی) option for 4 seconds
- Empty state now shows the selected day name instead of hardcoded "today"

**Validation Infrastructure:**
- New `TaskEventEntity` table (`task_events`) for logging task lifecycle events
- Events recorded: `created`, `completed`, `reopened`, `deleted` with timestamps
- New `TaskEventDao` for querying event data
- Baseline metrics collection enabled before hierarchy introduction

**Schema Preparation:**
- Added `lifeAreaId: Int?` field to `TaskEntity` (nullable, unused in UI)
- Room database version bumped to 2 (destructive migration for dev safety)

## Phase 2.2 — Lightweight Hierarchy (Planned)

Planned additions:

- Goal entity
- LifeArea entity
- Optional task linking
- Lightweight organization flows

Without introducing:

- mandatory workflows
- complex planning systems
- heavy setup friction

## Phase 3 — Validation (Planned)

After MVP stabilization:

- Observe usage patterns
- Measure hierarchy adoption
- Validate execution impact
- Decide future direction based on real behavior

---

# Long-Term Constraint

The project intentionally avoids:

- premature architecture expansion
- framework-first development
- feature accumulation without validation
- AI-first workflows before product validation

The current objective is not to build the most advanced productivity system.

The current objective is:

```text
Build the smallest stable product
capable of validating the hierarchy hypothesis.
```

# AGENT.md — Vision Planner Agent Contract

> This document is the **binding instruction set** for any agent working on Vision Planner.
> It supersedes generic agent instructions. Before modifying code, architecture, configuration, or documentation,
> the agent must read the mandatory files listed below and obey the rules in this document.
> Project location: `/home/sajjad/Desktop/code/andriod Project`

---

## 1. Project Identity

| Attribute | Value |
|---|---|
| **Project name** | Vision Planner |
| **Application ID** | `com.aistudio.visionplanner.zqxrtv` |
| **Project type** | Android offline-first application |
| **Language** | Kotlin |
| **UI framework** | Jetpack Compose + Material 3 |
| **Database** | Room with KSP annotation processing |
| **Async** | Kotlin Coroutines + Flow |
| **Architecture pattern** | MVVM on `AndroidViewModel` + `StateFlow` + Repository interfaces |
| **Root package** | `com.example` |
| **Persistence** | 100% local storage; no network dependency; no remote sync |
| **Min SDK** | 24 |
| **Target SDK** | 36 |
| **Compile SDK** | 36 |
| **Java compatibility** | Java 11 |
| **Module layout** | Single-module app (`:app`) |

### Version matrix

| Component | Version | Source |
|---|---|---|
| **Gradle** | 9.3.1 | `gradle/wrapper/gradle-wrapper.properties` |
| **AGP** | 9.1.1 | `gradle/libs.versions.toml` → `agp` |
| **Kotlin** | 2.2.10 | `gradle/libs.versions.toml` → `kotlin` |
| **Compose BOM** | 2024.09.00 | `gradle/libs.versions.toml` → `composeBom` |
| **Room** | 2.7.0 | `gradle/libs.versions.toml` → `roomRuntime/ktx/compiler` |
| **KSP** | 2.3.5 | `gradle/libs.versions.toml` → `googleDevtoolsKsp` |
| **DataStore** | 1.1.7 | `gradle/libs.versions.toml` → `datastorePreferences` |
| **kotlinx-coroutines** | 1.10.2 | main + test |
| **MockK** | 1.13.13 | test |
| **Turbine** | 1.2.0 | test |
| **Robolectric** | 4.16.1 | test |
| **JDK memory** | `-Xmx4g` | `gradle.properties` |

---

## 2. Mandatory Pre-Work Reading Order

The agent **must** read these files before proposing or applying any change.

### Tier 1 — always read first

| # | File | Purpose |
|---|---|---|
| 1 | `Vision_Planner.md` | Canonical product reference: mental model, entity schemas, data flow, feature status, one-shot event rule |
| 2 | `docs/ROADMAP.md` | Phase completion status, explicit out-of-scope rules, phase dependencies |
| 3 | `docs/ARCHITECTURE_STATE.md` | Live architecture snapshot: DB version, repository status, event/snapshot/graph/AI readiness, testing status |
| 4 | `docs/ARCHITECTURE_DECISION_LOG.md` | Open decision areas for Phase 4+ |
| 5 | `README.md` | Project overview, stack matrix, build instructions, development environment, constraints |

### Tier 2 — read when touching related areas

| # | File | Trigger |
|---|---|---|
| 6 | `docs/ADR-0001-goal-task-relationship.md` | Any task/goal linkage or schema change |
| 7 | `docs/ADR-0002-graph-architecture.md` | Any Phase 4 graph/node/life-area work |
| 8 | `docs/ADR-0003-progress-behavior-snapshots.md` | Any snapshot/backfill/aggregation change |
| 9 | `app/src/main/java/com/example/core/database/AppDatabase.kt` | Any schema/migration change |
| 10 | `app/src/main/java/com/example/plugins/planner/data/TaskEntity.kt` | Any task field or nullability change |
| 11 | `app/build.gradle.kts` | Any dependency, plugin, or build-configuration change |
| 12 | `gradle.properties` | Any build behavior, proxy, caching, or JVM argument change |

---

## 3. Architecture Rules

### Layer structure

```
com.example
├── core/                     # Framework-adjacent infrastructure
│   ├── constants/
│   ├── data/
│   ├── database/
│   ├── domain/
│   ├── goal/
│   ├── snapshot/
│   ├── onboarding/
│   ├── plugin/
│   ├── preferences/
│   ├── receiver/
│   ├── search/
│   └── util/
├── domain/                   # Pure Kotlin, NO Android imports
│   ├── insight/
│   └── snapshot/
├── plugins/
│   ├── goals/
│   ├── notes/
│   └── planner/
└── ui/
    ├── onboarding/
    ├── screens/
    └── theme/
```

### Hard rules

| Rule | Requirement |
|---|---|
| **Business logic location** | Must live in `domain.*` or `core.*` sub-packages that contain no Android UI/framework imports, never in ViewModel or Composable |
| **Data flow** | Room DAO `Flow<T>` → Repository → ViewModel `StateFlow` → Composable `collectAsState()` |
| **One-shot UI events** | Must use replay-free `Channel`/`Flow`, never sticky `StateFlow`; consume with `LaunchedEffect(Unit)` |
| **Repository interfaces** | Required for major subsystems: `GoalRepository`, `InsightRepository`, `SnapshotRepository` already exist; new major subsystems follow this pattern |
| **Direct DAO access from ViewModel** | Grandfathered only in `PlannerViewModel`; new ViewModels and new features must use repository interfaces, with no new direct DAO access |
| **Dependency Injection** | No DI framework; manual `AppDatabase.getDatabase(application)` construction is the current pattern |
| **Offline-first** | Absolute; no network, no auth, no sync, no remote AI |
| **Naming** | `*Dao`, `*Entity`, `*Repository`, `Room*Repository`, `*ViewModel`; packages snake_case; FROZEN disabled code marked with `// FROZEN:` comments |

---

## 4. Development Workflow Commands

Run from project root: `/home/sajjad/Desktop/code/andriod Project`

### Build commands

```bash
# Standard debug build — always include --no-configuration-cache
./gradlew --no-configuration-cache assembleDebug

# Clean build
./gradlew clean assembleDebug
```

**Required context:** `gradle.properties` enables `org.gradle.configuration-cache=true`, but this repo has a broken configuration-cache state after dependency cleanup. Omitting `--no-configuration-cache` will fail. This flag must remain until the cache is explicitly repaired and validated.

### Test commands

```bash
# Unit tests: JUnit + Robolectric + MockK + Turbine
./gradlew test

# Single test class
./gradlew testDebugUnitTest --tests "com.example.plugins.planner.data.RoomInsightRepositoryTest"

# Instrumented tests on device/emulator
./gradlew connectedDebugAndroidTest
```

### Verification commands

```bash
# Kotlin compile check only
./gradlew --no-configuration-cache compileDebugKotlin

# Lint
./gradlew --no-configuration-cache lintDebug

# Dependency tree
./gradlew :app:dependencies --configuration debugRuntimeClasspath
```

### Install on device

```bash
./gradlew --no-configuration-cache assembleDebug && \
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Clean / refresh

```bash
./gradlew clean
```

### Device requirements

- Physical Android device required for final UI, RTL, animation, and touch-target validation.
- Emulator may be used for compile verification only.
- `adb` must be available; USB debugging must be enabled.
- Debug keystore exists at project root: `debug.keystore`; debug builds sign automatically.

---

## 5. Testing Strategy

| Layer | Framework | Notes |
|---|---|---|
| **Domain unit tests** | JUnit 4 + Turbine + MockK | Host JVM; covers `InsightCalculator`, `BehaviorCalculator`, `GoalProgressCalculator` |
| **Room DAO tests** | Robolectric + JUnit 4 + MockK | Covers DAOs and repository classes |
| **Compose UI tests** | Compose UI test + JUnit4 | Scaffolded |
| **Screenshot tests** | Roborazzi | **FROZEN** — dependencies commented out in `app/build.gradle.kts` |
| **Instrumented tests** | Espresso + AndroidJUnitRunner | Run on device via `connectedDebugAndroidTest` |

### Pre-commit minimum

- `./gradlew --no-configuration-cache test` must pass.
- New domain logic requires host-JVM unit tests.
- New Room queries should have Robolectric DAO/repository coverage when feasible.

---

## 6. Code Change Protocol

### Before any change

1. Read Tier 1 mandatory docs.
2. Identify the current phase from `docs/ROADMAP.md`.
3. Inspect working tree state via `git status --short`, `git diff HEAD`, `git diff --staged`, and check for unexpected untracked files.
4. Identify affected files and side effects.
5. Propose a plan and wait for direction on broad changes; apply directly only for small scoped fixes. "Small" means bounded to one feature area, does not change architecture, DB schema, or cross-cutting abstractions.

### During changes

- Apply minimal, incremental changes.
- Do not introduce new architectural patterns, layers, or abstractions without an explicit ADR and approval.
- Respect Single Responsibility.
- Preserve Room migrations; never break existing upgrade path.
- Do not delete user data or introduce destructive operations without explicit confirmation.
- Comment out, do not delete, for FROZEN or deprecated code paths; prefix with `// FROZEN:`.
- Each change step must leave the project in a compilable state.

### After changes

- Run `./gradlew --no-configuration-cache assembleDebug`.
- Run `./gradlew test`.
- Report verification result, known issues, and remaining risks.
- Update documentation per Section 7 before declaring work complete.

---

## 7. Architecture Documentation Rule

### Mandatory documentation update map

| Change type | Files that must be reviewed and updated when needed |
|---|---|
| **Any DB schema/migration change** | `Vision_Planner.md`, `docs/ARCHITECTURE_STATE.md`, `docs/ROADMAP.md`, relevant ADR |
| **New architectural decision** | New `docs/ADR-NNN-*.md`; update `docs/ARCHITECTURE_DECISION_LOG.md` to mark the area resolved |
| **Repository/domain layer change** | `docs/ARCHITECTURE_STATE.md` relevant table |
| **Event system change** | `docs/ARCHITECTURE_STATE.md` Event System table |
| **New feature surface** | `README.md`, `Vision_Planner.md`, `docs/ROADMAP.md` if phase scope changes |
| **Build/dependency/environment change** | `README.md` Build Instructions if requirements change |
| **Phase completion** | `docs/ROADMAP.md`, `docs/ARCHITECTURE_STATE.md`, `Vision_Planner.md`, `README.md` |

### When a new ADR is required

Create a new ADR when:
- A new layer, abstraction, or module is introduced.
- A reversible decision is retired or overridden.
- Database behavior changes beyond straightforward additive migration.
- A system-wide invariant is changed, such as offline-first, single source of truth, or event ownership.

ADR location: `docs/ADR-NNN-short-name.md`. Numbering must use the next unused sequence number in `docs/`; do not reuse or skip numbers without reason. Update `docs/ARCHITECTURE_DECISION_LOG.md` to reflect the decision outcome. ADRs require explicit user approval before implementation.

---

## 8. Database Rules

| Rule | Value |
|---|---|
| **Database class** | `AppDatabase` in `app/src/main/java/com/example/core/database/AppDatabase.kt` |
| **Current schema version** | 9 |
| **Destructive migration** | `fallbackToDestructiveMigration()` is present as a safety net only; every production schema change needs an explicit `Migration(from, to)` class |
| **Migration pattern** | Table renames via `ALTER TABLE ... RENAME TO ...`, then `CREATE TABLE ...`, then `INSERT INTO ...`; SQLite on older APIs cannot `DROP COLUMN` |
| **Snapshot tables** | `goal_progress_snapshot` and `behavior_snapshot` are rebuildable projections; use REPLACE/upsert; never authoritative writes |
| **Snapshot trigger** | Real-time today upsert via `SnapshotAggregator.recordDay(today)` from `PlannerViewModel` and `GoalDetailViewModel`, plus App-Launch Backfill Engine in `MainActivity` |
| **Foreign keys** | `tasks.goalId → goals.id ON DELETE SET NULL`; `goal_progress_snapshot.goalId → goals.id ON DELETE CASCADE`; `task_events.taskId` indexed |
| **Before schema change** | 1. Read existing migrations in `AppDatabase.kt`. 2. Implement new `Migration`. 3. Add or update a Robolectric migration test when feasible. 4. Bump DB version. 5. Update docs. |

---

## 9. Git / Change Reporting

### Workflow

- Branch: `main` only by current convention. Do not create feature branches unless the user explicitly requests a branch-based workflow.
- The agent must not auto-push or auto-commit.
- The user commits their own changes.
- Before finishing a task, the agent must present a change report.

### Pre-commit change report

The agent must explicitly report:

1. **Files changed** with paths.
2. **Reason of change**.
3. **Architecture impact**: affected layers, new/removed/edited abstractions.
4. **Database impact**: schema version change, migration added/removed, data risk.
5. **Documentation updated**: which docs were touched.
6. **Test result**: build/test outcome and device install status if applicable.
7. **Remaining risks**: unresolved items, follow-up tasks, regressions.

---

## 10. Scope Guard

The following are **explicitly out of scope** until a new ADR or phase approval changes this:

- AI implementation, on-device model loading, or remote AI/LLM calls
- Network layer, HTTP clients, auth, cloud sync, or remote APIs
- `User` entity, multi-profile support, or account system
- Graph View UI without an approved ADR/phase decision; Phase 4 implementation is planned and requires explicit approval before UI work begins
- Heavy refactors that change package structure or core abstractions without prior planning
- UI redesigns during architecture-only tasks unless requested

---

## 11. Forbidden Actions

The agent must not do the following without explicit user confirmation:

1. Change architecture without reading `Vision_Planner.md`, `ROADMAP.md`, `ARCHITECTURE_STATE.md`, and relevant ADRs first.
2. Change database schema without writing a `Migration` class and preserving user data.
3. Use `!!` operator; use null-safe defaults or conditional rendering instead.
4. Add network/cloud/AI dependencies while offline-first constraint is active.
5. Delete FROZEN code; always comment out with `// FROZEN:` prefix.
6. Change AGP/Kotlin/Compose/Room/KSP versions without explicit reason and impact analysis.
7. Introduce a DI framework without an approved ADR.
8. Create a `User` entity unless a new ADR explicitly overrides the current decision.
9. Access DAO or database directly from UI composables; use ViewModel/repository path.
10. Build Graph View UI before Phase 4 ADR-0002 implementation is explicitly approved; ADR-0002 is approved as design only, not implementation.
11. Destructive git operations such as `git checkout .` or hard resets without confirmation.
12. Delete documentation files; update or append instead.
13. Change `applicationId` or signing configuration without user awareness.
14. Touch `settings.gradle.kts` unless adding or removing a module.
15. Omit `--no-configuration-cache` from Gradle builds **only when it is not required**; the current repo state requires it, so any build command in Section 4 must keep it unless the configuration cache is explicitly repaired and validated.

---

## 12. Problem Diagnosis Rules

When a build or test fails:

1. Read the exact error message and stack trace.
2. Check dependency resolution, Gradle configuration, environment variables, and network/proxy.
3. Check recent `git diff HEAD` to correlate failures with recent changes.
4. Identify root cause before editing code.
5. Prefer minimal surgical fixes over broad reversions.

---

## 13. Communication Format

The agent should communicate in a structured format.

### Before implementation

```
Analysis:
Affected files:
Risk:
Architecture impact:
Documentation impact:
Implementation plan:
```

### After implementation

```
Completed:
Files changed:
Phase / ADR alignment:
Build result:
Test result:
Device install result:
Documentation updated:
Known issues:
Next recommended step:
```

---

## 14. Long-Term Project Principles

The project prioritizes:

- Clean architecture boundaries over speed of feature addition.
- Stable foundations over speculative generalization.
- Explicit architectural decisions over implicit conventions.
- Documentation consistency with real implementation.
- Testable domain logic.
- Incremental evolution without destructive rebuilds.

The agent must optimize for maintainability, reviewability, and correctness.

---

## 15. Project Phase Snapshot

| Item | Status |
|---|---|
| **Current phase** | Phase 3 — Progress & Behavior Foundation — COMPLETE |
| **Next phase** | Phase 4 — Graph View Foundation |
| **DB schema version** | 9 |
| **Phase 1** | Foundation Cleanup — COMPLETE |
| **Phase 2** | Architecture Stabilization — COMPLETE |
| **Phase 3** | Progress & Behavior Foundation — COMPLETE |
| **Open decision log** | 7 exploratory decision areas in `docs/ARCHITECTURE_DECISION_LOG.md` — draft only, not approved pending decisions |
| **Uncommitted changes** | `DaySelector.kt`, `ConvexBottomBarShape.kt`, `VisionBottomBar.kt`, launcher drawables, `docs/ARCHITECTURE_DECISION_LOG.md` |
| **FROZEN dependencies** | Firebase, Retrofit/Moshi/OkHttp, Roborazzi, Secrets, Google Services — all commented out in `app/build.gradle.kts` |
| **ADL status** | `docs/ARCHITECTURE_DECISION_LOG.md` is untracked draft content; do not treat its options as binding architectural decisions |

---

*This document is derived solely from the actual repository state. It should be updated whenever architecture, dependencies, tooling, or phase status changes.*

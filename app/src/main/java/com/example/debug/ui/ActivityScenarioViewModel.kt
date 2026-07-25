package com.example.debug.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.util.JalaliDate
import com.example.plugins.planner.data.ActivityEventEntity
import com.example.plugins.planner.data.ActivityEventRepository
import com.example.plugins.planner.data.ActivityEventType
import com.example.plugins.planner.data.ImageEventParser
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskStepEntity
import com.example.plugins.planner.data.TaskStepRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScenarioResult(
    val name: String,
    val passed: Boolean,
    val details: List<String>,
    val durationMs: Long
)

data class ActivityScenarioUiState(
    val isRunning: Boolean = false,
    val results: List<ScenarioResult> = emptyList(),
    val status: String? = null
)

enum class ActivityScenario(val label: String) {
    TASK_LIFECYCLE("Task Lifecycle"),
    TIMELINE_MULTI_DAY("Timeline Multi-Day"),
    PERSISTENCE("Persistence"),
    TIMELINE_RANGE("Timeline Range"),
    NOTE_LIFECYCLE("Note Lifecycle"),
    MANUAL_ACTIVITY_LIFECYCLE("Manual Activity Lifecycle"),
    IMAGE_LIFECYCLE("Image Lifecycle")
}

class ActivityScenarioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val stepRepo = TaskStepRepository(database.taskStepDao(), database.activityEventDao())
    private val activityRepo = ActivityEventRepository(database.activityEventDao())

    private val _state = MutableStateFlow(ActivityScenarioUiState())
    val state: StateFlow<ActivityScenarioUiState> = _state.asStateFlow()

    fun runScenario(scenario: ActivityScenario) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRunning = true, status = "Running ${scenario.label}...")
            try {
                val result = withContext(Dispatchers.IO) {
                    when (scenario) {
                        ActivityScenario.TASK_LIFECYCLE -> runTaskLifecycle()
                        ActivityScenario.TIMELINE_MULTI_DAY -> runTimelineMultiDay()
                        ActivityScenario.PERSISTENCE -> runPersistence()
                        ActivityScenario.TIMELINE_RANGE -> runTimelineRange()
                        ActivityScenario.NOTE_LIFECYCLE -> runNoteLifecycle()
                        ActivityScenario.MANUAL_ACTIVITY_LIFECYCLE -> runManualActivityLifecycle()
                        ActivityScenario.IMAGE_LIFECYCLE -> runImageLifecycle()
                    }
                }
                val current = _state.value
                _state.value = current.copy(
                    isRunning = false,
                    results = current.results + result,
                    status = "${result.name}: ${if (result.passed) "PASS" else "FAIL"} (${result.durationMs}ms)"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isRunning = false, status = "Error: ${e.message}")
            }
        }
    }

    fun clearResults() {
        _state.value = _state.value.copy(results = emptyList(), status = "Results cleared")
    }

    // ── Scenario: Task Lifecycle ──────────────────────────────────────────

    private suspend fun runTaskLifecycle(): ScenarioResult {
        val start = System.currentTimeMillis()
        val details = mutableListOf<String>()
        var passed = true
        val tag = start % 100000

        val task = TaskEntity(
            title = "[Sc] Lifecycle $tag",
            dateEpochMs = JalaliDate.toEpochMs(JalaliDate.today())
        )
        val taskId = taskDao.insertTask(task).toInt()
        details.add("Created task id=$taskId")

        try {
            // Step A: create
            val stepA = TaskStepEntity(taskId = taskId, title = "Step A-$tag")
            val stepAId = stepRepo.addStep(stepA)
            details.add("Step A created id=$stepAId")

            val createdEvent = activityRepo.findLatestEvent(taskId, ActivityEventType.STEP_CREATED.name)
            if (createdEvent?.stepId == stepAId) {
                details.add("PASS: STEP_CREATED event present for step $stepAId")
            } else {
                details.add("FAIL: STEP_CREATED event missing or mismatched stepId")
                passed = false
            }

            // Step A: complete
            stepRepo.updateStep(
                stepRepo.getStepById(stepAId)!!.copy(isCompleted = true, completedAt = System.currentTimeMillis())
            )
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = stepAId, eventType = ActivityEventType.STEP_COMPLETED.name, description = stepA.title)
            )
            val stepACompleted = stepRepo.getStepById(stepAId)
            if (stepACompleted?.isCompleted == true && stepACompleted.completedAt != null) {
                details.add("PASS: STEP_COMPLETED updated state correctly")
            } else {
                details.add("FAIL: STEP_COMPLETED state mismatch")
                passed = false
            }

            // Step A: reopen
            stepRepo.updateStep(stepACompleted!!.copy(isCompleted = false, completedAt = null))
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = stepAId, eventType = ActivityEventType.STEP_REOPENED.name, description = stepA.title)
            )
            val stepAReopened = stepRepo.getStepById(stepAId)
            if (stepAReopened?.isCompleted == false && stepAReopened.completedAt == null) {
                details.add("PASS: STEP_REOPENED updated state correctly")
            } else {
                details.add("FAIL: STEP_REOPENED state mismatch")
                passed = false
            }

            // Step A: complete again (ensure re-complete works)
            stepRepo.updateStep(stepAReopened!!.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = stepAId, eventType = ActivityEventType.STEP_COMPLETED.name, description = stepA.title)
            )

            // Step B: create + delete
            val stepB = TaskStepEntity(taskId = taskId, title = "Step B-$tag")
            val stepBId = stepRepo.addStep(stepB)
            details.add("Step B created id=$stepBId")
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = stepBId, eventType = ActivityEventType.STEP_CREATED.name, description = stepB.title)
            )
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = stepBId, eventType = ActivityEventType.STEP_DELETED.name, description = stepB.title)
            )
            stepRepo.deleteStep(stepBId)

            if (stepRepo.getStepById(stepBId) == null) {
                details.add("PASS: Step B deleted from DB")
            } else {
                details.add("FAIL: Step B still exists after deletion")
                passed = false
            }

            // Verify events for Step A
            val eventsA = activityRepo.getEventsByStepId(stepAId)
            val typesA = eventsA.map { it.eventType }.toSet()
            val expectedA = setOf(
                ActivityEventType.STEP_CREATED.name,
                ActivityEventType.STEP_COMPLETED.name,
                ActivityEventType.STEP_REOPENED.name
            )
            if (typesA.containsAll(expectedA)) {
                details.add("PASS: Step A has ${eventsA.size} events with types=$typesA")
            } else {
                details.add("FAIL: Step A missing event types, got=$typesA")
                passed = false
            }

            // Verify events for Step B
            val eventsB = activityRepo.getEventsByStepId(stepBId)
            val typesB = eventsB.map { it.eventType }.toSet()
            val expectedB = setOf(
                ActivityEventType.STEP_CREATED.name,
                ActivityEventType.STEP_DELETED.name
            )
            if (typesB.containsAll(expectedB)) {
                details.add("PASS: Step B has ${eventsB.size} events with types=$typesB")
            } else {
                details.add("FAIL: Step B missing event types, got=$typesB")
                passed = false
            }

        } finally {
            taskDao.deleteTask(task)
            details.add("Cleanup: task $taskId deleted (cascade removes steps + events)")
        }

        return ScenarioResult("Task Lifecycle", passed, details, System.currentTimeMillis() - start)
    }

    // ── Scenario: Timeline Multi-Day ──────────────────────────────────────

    private suspend fun runTimelineMultiDay(): ScenarioResult {
        val start = System.currentTimeMillis()
        val details = mutableListOf<String>()
        var passed = true
        val tag = start % 100000

        val todayMs = JalaliDate.toEpochMs(JalaliDate.today())
        val dayMinus1 = todayMs - 86400000L
        val dayMinus2 = todayMs - 2 * 86400000L

        val task = TaskEntity(title = "[Sc] MultiDay $tag", dateEpochMs = todayMs)
        val taskId = taskDao.insertTask(task).toInt()
        details.add("Created task id=$taskId")

        try {
            val step = TaskStepEntity(taskId = taskId, title = "MultiDay Step")
            val stepId = stepRepo.addStep(step)
            val autoCreatedAt = activityRepo.findLatestEvent(taskId, ActivityEventType.STEP_CREATED.name)!!.timestamp

            // Events at 3 different timestamps (different Jalali days)
            // STEP_CREATED is auto-created by repository; use NOTE_ADDED for custom timestamp
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = stepId, eventType = ActivityEventType.NOTE_ADDED.name, description = "Day -2", timestamp = dayMinus2 + 1000L)
            )
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = stepId, eventType = ActivityEventType.STEP_COMPLETED.name, description = "Day -1", timestamp = dayMinus1 + 2000L)
            )
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = stepId, eventType = ActivityEventType.STEP_REOPENED.name, description = "Today", timestamp = todayMs + 3000L)
            )
            details.add("Created 4 events (1 auto STEP_CREATED + 3 explicit) spanning 3 Jalali days")

            // Group by JalaliDate group key
            val events = activityRepo.getEventsByStepId(stepId)
            val groups = events.groupBy { JalaliDate.toEpochMs(JalaliDate.fromEpochMs(it.timestamp)) }
            val groupDates = groups.keys.sorted().map { JalaliDate.fromEpochMs(it).toEnglishString() }
            details.add("Events form ${groups.size} Jalali day group(s): $groupDates")

            if (groups.size >= 2) {
                details.add("PASS: Events span at least 2 Jalali days")
            } else {
                details.add("FAIL: All events fall on the same Jalali day")
                passed = false
            }

            // Verify timestamp preservation
            val sorted = events.sortedBy { it.timestamp }
            val expectedTimestamps = listOf(dayMinus2 + 1000L, dayMinus1 + 2000L, todayMs + 3000L, autoCreatedAt)
            val timestampsOk = sorted.map { it.timestamp } == expectedTimestamps
            if (timestampsOk) {
                details.add("PASS: All ${sorted.size} timestamps preserved exactly")
            } else {
                val actual = sorted.map { it.timestamp }
                details.add("FAIL: Timestamp mismatch. Expected $expectedTimestamps, got $actual")
                passed = false
            }

            stepRepo.deleteStep(stepId)

        } finally {
            taskDao.deleteTask(task)
            details.add("Cleanup: task $taskId deleted")
        }

        return ScenarioResult("Timeline Multi-Day", passed, details, System.currentTimeMillis() - start)
    }

    // ── Scenario: Persistence ─────────────────────────────────────────────

    private suspend fun runPersistence(): ScenarioResult {
        val start = System.currentTimeMillis()
        val details = mutableListOf<String>()
        var passed = true
        val tag = start % 100000

        val todayMs = JalaliDate.toEpochMs(JalaliDate.today())

        val task = TaskEntity(title = "[Sc] Persist $tag", dateEpochMs = todayMs)
        val taskId = taskDao.insertTask(task).toInt()
        details.add("Created task id=$taskId")

        try {
            val stepTitle = "PersistStep-$tag"
            val step = TaskStepEntity(taskId = taskId, title = stepTitle)
            val stepId = stepRepo.addStep(step)
            details.add("Created step id=$stepId")

            // STEP_CREATED is auto-created by repository
            val completionTime = System.currentTimeMillis()
            stepRepo.updateStep(stepRepo.getStepById(stepId)!!.copy(isCompleted = true, completedAt = completionTime))
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = stepId, eventType = ActivityEventType.STEP_COMPLETED.name, description = stepTitle)
            )

            // Re-read and verify
            val rereadStep = stepRepo.getStepById(stepId)
            if (rereadStep != null) {
                val titleOk = rereadStep.title == stepTitle
                val completedOk = rereadStep.isCompleted
                val completedAtOk = rereadStep.completedAt == completionTime

                if (titleOk && completedOk && completedAtOk) {
                    details.add("PASS: Step round-trip intact — title=$stepTitle, isCompleted=true, completedAt=$completionTime")
                } else {
                    details.add("FAIL: Step corruption — title=$titleOk, isCompleted=$completedOk, completedAt=$completedAtOk")
                    passed = false
                }
            } else {
                details.add("FAIL: Step not found after creation")
                passed = false
            }

            val rereadEvents = activityRepo.getEventsByStepId(stepId)
            if (rereadEvents.size == 2) {
                val hasCreated = rereadEvents.any { it.eventType == ActivityEventType.STEP_CREATED.name }
                val hasCompleted = rereadEvents.any { it.eventType == ActivityEventType.STEP_COMPLETED.name }
                if (hasCreated && hasCompleted) {
                    details.add("PASS: Both events persisted (${rereadEvents.size})")
                } else {
                    details.add("FAIL: Missing event types — created=$hasCreated, completed=$hasCompleted")
                    passed = false
                }
            } else {
                details.add("FAIL: Expected 2 events, found ${rereadEvents.size}")
                passed = false
            }

            // Verify findLatestEvent works
            val latestCreated = activityRepo.findLatestEvent(taskId, ActivityEventType.STEP_CREATED.name)
            if (latestCreated != null && latestCreated.stepId == stepId) {
                details.add("PASS: findLatestEvent returns correct result")
            } else {
                details.add("FAIL: findLatestEvent returned null or wrong step")
                passed = false
            }

            stepRepo.deleteStep(stepId)

        } finally {
            taskDao.deleteTask(task)
            details.add("Cleanup: task $taskId deleted")
        }

        return ScenarioResult("Persistence", passed, details, System.currentTimeMillis() - start)
    }

    // ── Scenario: Timeline Range ──────────────────────────────────────────

    private suspend fun runTimelineRange(): ScenarioResult {
        val start = System.currentTimeMillis()
        val details = mutableListOf<String>()
        var passed = true
        val tag = start % 100000

        // Task set to 5 days ago — timeline range is [today, task.dateEpochMs] (5 days)
        val todayMs = JalaliDate.toEpochMs(JalaliDate.today())
        val fiveDaysAgoMs = todayMs - 5 * 86400000L

        val task = TaskEntity(title = "[Sc] Range $tag", dateEpochMs = fiveDaysAgoMs)
        val taskId = taskDao.insertTask(task).toInt()
        details.add("Created task id=$taskId, dateEpochMs=5 days ago, range=[today, $fiveDaysAgoMs]")

        try {
            val step = TaskStepEntity(taskId = taskId, title = "Range Step")
            val stepId = stepRepo.addStep(step)

            // Event at the earliest boundary (task.dateEpochMs)
            // STEP_CREATED is auto-created by repository; use FILE_ADDED for custom timestamp
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = stepId, eventType = ActivityEventType.FILE_ADDED.name, description = "Boundary start", timestamp = fiveDaysAgoMs)
            )

            // Event at the latest boundary (today)
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = stepId, eventType = ActivityEventType.STEP_COMPLETED.name, description = "Boundary end", timestamp = todayMs)
            )

            // Event inside the range
            val midRange = fiveDaysAgoMs + 2 * 86400000L
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = stepId, eventType = ActivityEventType.STEP_REOPENED.name, description = "Mid range", timestamp = midRange)
            )

            details.add("Created 4 events (1 auto STEP_CREATED + 3 explicit) spanning timeline range")

            // Verify all events are retrievable with correct timestamps
            val events = activityRepo.getEventsByStepId(stepId).sortedBy { it.timestamp }
            if (events.size == 4) {
                val timestamps = events.map { it.timestamp }
                val boundaryPresent = timestamps.contains(fiveDaysAgoMs) && timestamps.contains(todayMs)

                if (boundaryPresent) {
                    details.add("PASS: Boundary events present, ${events.size} total events: $timestamps")
                } else {
                    details.add("FAIL: Boundary events missing, got=$timestamps")
                    passed = false
                }
            } else {
                details.add("FAIL: Expected 4 events, found ${events.size}")
                passed = false
            }

            stepRepo.deleteStep(stepId)

        } finally {
            taskDao.deleteTask(task)
            details.add("Cleanup: task $taskId deleted")
        }

        return ScenarioResult("Timeline Range", passed, details, System.currentTimeMillis() - start)
    }

    // ── Scenario: Note Lifecycle ──────────────────────────────────────

    private suspend fun runNoteLifecycle(): ScenarioResult {
        val start = System.currentTimeMillis()
        val details = mutableListOf<String>()
        var passed = true
        val tag = start % 100000

        val task = TaskEntity(title = "[Sc] Note Lifecycle $tag", dateEpochMs = JalaliDate.toEpochMs(JalaliDate.today()))
        val taskId = taskDao.insertTask(task).toInt()
        details.add("Created task id=$taskId")

        try {
            val noteText = "Test note $tag"
            activityRepo.addEvent(
                ActivityEventEntity(
                    taskId = taskId,
                    stepId = null,
                    eventType = ActivityEventType.NOTE_ADDED.name,
                    description = noteText
                )
            )
            details.add("Added NOTE_ADDED event with description")

            val noteEvent = activityRepo.findLatestEvent(taskId, ActivityEventType.NOTE_ADDED.name)

            if (noteEvent != null) {
                details.add("PASS: NOTE_ADDED event found in query")
            } else {
                details.add("FAIL: NOTE_ADDED event not found")
                passed = false
            }

            noteEvent?.let { event ->
                if (event.stepId == null) {
                    details.add("PASS: NOTE_ADDED event has stepId=null")
                } else {
                    details.add("FAIL: NOTE_ADDED event has non-null stepId")
                    passed = false
                }

                if (event.description == noteText) {
                    details.add("PASS: NOTE_ADDED description preserved")
                } else {
                    details.add("FAIL: NOTE_ADDED description mismatch")
                    passed = false
                }

                if (event.timestamp != null && event.timestamp > 0) {
                    details.add("PASS: NOTE_ADDED timestamp exists")
                } else {
                    details.add("FAIL: NOTE_ADDED missing timestamp")
                    passed = false
                }
            }

        } finally {
            taskDao.deleteTask(task)
            details.add("Cleanup: task $taskId deleted")
        }

        return ScenarioResult("Note Lifecycle", passed, details, System.currentTimeMillis() - start)
    }

    // ── Scenario: Manual Activity Lifecycle ──────────────────────

    private suspend fun runManualActivityLifecycle(): ScenarioResult {
        val start = System.currentTimeMillis()
        val details = mutableListOf<String>()
        var passed = true
        val tag = start % 100000

        val task = TaskEntity(title = "[Sc] Manual Activity $tag", dateEpochMs = JalaliDate.toEpochMs(JalaliDate.today()))
        val taskId = taskDao.insertTask(task).toInt()
        details.add("Created task id=$taskId")

        try {
            val title = "Test activity $tag"
            activityRepo.addEvent(
                ActivityEventEntity(
                    taskId = taskId,
                    stepId = null,
                    eventType = ActivityEventType.MANUAL_ACTIVITY.name,
                    description = title
                )
            )
            details.add("Added MANUAL_ACTIVITY event")

            val manualEvent = activityRepo.findLatestEvent(taskId, ActivityEventType.MANUAL_ACTIVITY.name)

            if (manualEvent != null) {
                details.add("PASS: MANUAL_ACTIVITY event found in query")
            } else {
                details.add("FAIL: MANUAL_ACTIVITY event not found")
                passed = false
            }

            if (manualEvent?.stepId == null) {
                details.add("PASS: MANUAL_ACTIVITY event has stepId=null")
            } else {
                details.add("FAIL: MANUAL_ACTIVITY event has non-null stepId")
                passed = false
            }

            if (manualEvent?.description == title) {
                details.add("PASS: MANUAL_ACTIVITY description preserved")
            } else {
                details.add("FAIL: MANUAL_ACTIVITY description mismatch")
                passed = false
            }

            if (manualEvent?.timestamp != null && manualEvent.timestamp > 0) {
                details.add("PASS: MANUAL_ACTIVITY timestamp exists")
            } else {
                details.add("FAIL: MANUAL_ACTIVITY missing timestamp")
                passed = false
            }

        } finally {
            taskDao.deleteTask(task)
            details.add("Cleanup: task $taskId deleted")
        }

        return ScenarioResult("Manual Activity Lifecycle", passed, details, System.currentTimeMillis() - start)
    }

    // ── Scenario: Image Lifecycle ──────────────────────────────────────

    private suspend fun runImageLifecycle(): ScenarioResult {
        val start = System.currentTimeMillis()
        val details = mutableListOf<String>() 
        var passed = true
        val tag = start % 100000

        val task = TaskEntity(title = "[Sc] Image Lifecycle $tag", dateEpochMs = JalaliDate.toEpochMs(JalaliDate.today()))
        val taskId = taskDao.insertTask(task).toInt()
        details.add("Created task id=$taskId")

        try {
            val testUri = "content://test/image_$tag.jpg"
            val testDescription = "Test description $tag"
            val encodedDescription = ImageEventParser.encode(testUri, testDescription)
            
            activityRepo.addEvent(
                ActivityEventEntity(
                    taskId = taskId,
                    stepId = null,
                    eventType = ActivityEventType.IMAGE_ADDED.name,
                    description = encodedDescription
                )
            )
            details.add("Added IMAGE_ADDED event with URI and description")

            val imageEvent = activityRepo.findLatestEvent(taskId, ActivityEventType.IMAGE_ADDED.name)

            if (imageEvent != null) {
                details.add("PASS: IMAGE_ADDED event found in query")
            } else {
                details.add("FAIL: IMAGE_ADDED event not found")
                passed = false
            }

            imageEvent?.let { event ->
                if (event.stepId == null) {
                    details.add("PASS: IMAGE_ADDED event has stepId=null")
                } else {
                    details.add("FAIL: IMAGE_ADDED event has non-null stepId")
                    passed = false
                }

                if (event.eventType == ActivityEventType.IMAGE_ADDED.name) {
                    details.add("PASS: IMAGE_ADDED eventType preserved")
                } else {
                    details.add("FAIL: IMAGE_ADDED eventType mismatch")
                    passed = false
                }

                val decodedData = ImageEventParser.decode(event.description)
                if (decodedData.uri == testUri) {
                    details.add("PASS: IMAGE_ADDED URI preserved")
                } else {
                    details.add("FAIL: IMAGE_ADDED URI mismatch. Expected $testUri, got ${decodedData.uri}")
                    passed = false
                }

                if (decodedData.description == testDescription) {
                    details.add("PASS: IMAGE_ADDED description preserved")
                } else {
                    details.add("FAIL: IMAGE_ADDED description mismatch. Expected $testDescription, got ${decodedData.description}")
                    passed = false
                }

                if (event.timestamp != null && event.timestamp > 0) {
                    details.add("PASS: IMAGE_ADDED timestamp exists")
                } else {
                    details.add("FAIL: IMAGE_ADDED missing timestamp")
                    passed = false
                }
            }

        } finally {
            taskDao.deleteTask(task)
            details.add("Cleanup: task $taskId deleted")
        }

        return ScenarioResult("Image Lifecycle", passed, details, System.currentTimeMillis() - start)
    }
}

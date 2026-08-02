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
    IMAGE_LIFECYCLE("Image Lifecycle"),
    IMAGE_PERSISTENCE_LIFECYCLE("Image Persistence Lifecycle")
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
                        ActivityScenario.IMAGE_PERSISTENCE_LIFECYCLE -> runImagePersistenceLifecycle()
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
            // Tag A: create
            val tagA = TaskStepEntity(taskId = taskId, title = "Tag A-$tag")
            val tagAId = stepRepo.addStep(tagA)
            details.add("Tag A created id=$tagAId")

            // Tags are metadata — no activity event should be created
            val noCreatedEvent = activityRepo.findLatestEvent(taskId, ActivityEventType.STEP_CREATED.name) == null
            if (noCreatedEvent) {
                details.add("PASS: No STEP_CREATED event for tag creation (metadata only)")
            } else {
                details.add("FAIL: Unexpected STEP_CREATED event for tag creation")
                passed = false
            }

            // Tags are metadata — no completion state to test
            details.add("SKIP: Tag completion state (tags are metadata only)")

            // Tag B: create + delete
            val tagB = TaskStepEntity(taskId = taskId, title = "Tag B-$tag")
            val tagBId = stepRepo.addStep(tagB)
            details.add("Tag B created id=$tagBId")
            stepRepo.deleteStep(tagBId)

            if (stepRepo.getStepById(tagBId) == null) {
                details.add("PASS: Tag B deleted from DB")
            } else {
                details.add("FAIL: Tag B still exists after deletion")
                passed = false
            }

            // Verify events for Tag A — tags create no activity events
            val eventsA = activityRepo.getEventsByStepId(tagAId)
            if (eventsA.isEmpty()) {
                details.add("PASS: Tag A has no activity events (metadata only)")
            } else {
                details.add("FAIL: Tag A has unexpected events: ${eventsA.map { it.eventType }}")
                passed = false
            }

            // Verify events for Tag B — tags create no activity events
            val eventsB = activityRepo.getEventsByStepId(tagBId)
            if (eventsB.isEmpty()) {
                details.add("PASS: Tag B has no activity events (metadata only)")
            } else {
                details.add("FAIL: Tag B has unexpected events: ${eventsB.map { it.eventType }}")
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
            val tag = TaskStepEntity(taskId = taskId, title = "MultiDay Tag")
            val tagId = stepRepo.addStep(tag)

            // Tags are metadata — no auto STEP_CREATED event
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = tagId, eventType = ActivityEventType.NOTE_ADDED.name, description = "Day -2", timestamp = dayMinus2 + 1000L)
            )
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = tagId, eventType = ActivityEventType.NOTE_ADDED.name, description = "Day -1", timestamp = dayMinus1 + 2000L)
            )
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = tagId, eventType = ActivityEventType.NOTE_ADDED.name, description = "Today", timestamp = todayMs + 3000L)
            )
            details.add("Created 3 events (all NOTE_ADDED) spanning 3 Jalali days")

            // Group by JalaliDate group key
            val events = activityRepo.getEventsByStepId(tagId)
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
            val expectedTimestamps = listOf(dayMinus2 + 1000L, dayMinus1 + 2000L, todayMs + 3000L)
            val timestampsOk = sorted.map { it.timestamp } == expectedTimestamps
            if (timestampsOk) {
                details.add("PASS: All ${sorted.size} timestamps preserved exactly")
            } else {
                details.add("FAIL: Timestamps mismatch — expected=$expectedTimestamps, got=${sorted.map { it.timestamp }}")
                passed = false
            }

            stepRepo.deleteStep(tagId)

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
            val tagTitle = "PersistTag-$tag"
            val tag = TaskStepEntity(taskId = taskId, title = tagTitle)
            val tagId = stepRepo.addStep(tag)
            details.add("Created tag id=$tagId")

            // Tags are metadata — verify persistence of basic fields only
            val rereadTag = stepRepo.getStepById(tagId)
            if (rereadTag != null) {
                val titleOk = rereadTag.title == tagTitle
                val taskIdOk = rereadTag.taskId == taskId

                if (titleOk && taskIdOk) {
                    details.add("PASS: Tag round-trip intact — title=$tagTitle, taskId=$taskId")
                } else {
                    details.add("FAIL: Tag corruption — title=$titleOk, taskId=$taskIdOk")
                    passed = false
                }
            }

            val rereadEvents = activityRepo.getEventsByStepId(tagId)
            if (rereadEvents.size == 2) {
                val hasNote = rereadEvents.any { it.eventType == ActivityEventType.NOTE_ADDED.name }
                val hasCompleted = rereadEvents.any { it.eventType == ActivityEventType.STEP_COMPLETED.name }
                if (hasNote && hasCompleted) {
                    details.add("PASS: Both events persisted (${rereadEvents.size})")
                } else {
                    details.add("FAIL: Missing event types — note=$hasNote, completed=$hasCompleted")
                    passed = false
                }
            } else {
                details.add("FAIL: Expected 2 events, found ${rereadEvents.size}")
                passed = false
            }

            // Verify findLatestEvent works
            val latestNote = activityRepo.findLatestEvent(taskId, ActivityEventType.NOTE_ADDED.name)
            if (latestNote != null && latestNote.stepId == tagId) {
                details.add("PASS: findLatestEvent returns correct result")
            } else {
                details.add("FAIL: findLatestEvent returned null or wrong tag")
                passed = false
            }

            stepRepo.deleteStep(tagId)

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
            val tag = TaskStepEntity(taskId = taskId, title = "Range Tag")
            val tagId = stepRepo.addStep(tag)

            // Events at 3 different timestamps (different Jalali days)
            // Tags are metadata — all events are user activities (NOTE_ADDED)
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = tagId, eventType = ActivityEventType.FILE_ADDED.name, description = "Boundary start", timestamp = fiveDaysAgoMs)
            )
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = tagId, eventType = ActivityEventType.NOTE_ADDED.name, description = "Boundary end", timestamp = todayMs)
            )
            activityRepo.addEvent(
                ActivityEventEntity(taskId = taskId, stepId = tagId, eventType = ActivityEventType.NOTE_ADDED.name, description = "Mid range", timestamp = fiveDaysAgoMs + 2 * 86400000L)
            )
            details.add("Created 3 events spanning timeline range")

            // Verify all events are retrievable with correct timestamps
            val events = activityRepo.getEventsByStepId(tagId).sortedBy { it.timestamp }
            if (events.size == 3) {
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

            stepRepo.deleteStep(tagId)

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
                if (decodedData.uri.isNotBlank() && decodedData.uri == testUri) {
                    details.add("PASS: IMAGE_ADDED URI extracted and preserved")
                } else {
                    details.add("FAIL: IMAGE_ADDED URI extraction failed or mismatch")
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

    // ── Scenario: Image Persistence Lifecycle ──────────────────────

    private suspend fun runImagePersistenceLifecycle(): ScenarioResult {
        val start = System.currentTimeMillis()
        val details = mutableListOf<String>()
        var passed = true
        val tag = start % 100000

        val task = TaskEntity(title = "[Sc] Image Persistence $tag", dateEpochMs = JalaliDate.toEpochMs(JalaliDate.today()))
        val taskId = taskDao.insertTask(task).toInt()
        details.add("Created task id=$taskId")

        try {
            val testUri = "content://test/image.jpg"
            val testDescription = "Persistence test"
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

            // Reload from repository (simulates app restart / fresh read)
            val imageEvent = activityRepo.findLatestEvent(taskId, ActivityEventType.IMAGE_ADDED.name)

            if (imageEvent != null) {
                details.add("PASS: IMAGE_ADDED event found after reload")
            } else {
                details.add("FAIL: IMAGE_ADDED event not found after reload")
                passed = false
            }

            imageEvent?.let { event ->
                if (event.eventType == ActivityEventType.IMAGE_ADDED.name) {
                    details.add("PASS: IMAGE_ADDED eventType preserved")
                } else {
                    details.add("FAIL: IMAGE_ADDED eventType mismatch")
                    passed = false
                }

                if (event.stepId == null) {
                    details.add("PASS: IMAGE_ADDED event has stepId=null")
                } else {
                    details.add("FAIL: IMAGE_ADDED event has non-null stepId")
                    passed = false
                }

                val decodedData = ImageEventParser.decode(event.description)
                if (decodedData.uri == testUri) {
                    details.add("PASS: URI preserved after reload")
                } else {
                    details.add("FAIL: URI mismatch after reload. Expected $testUri, got ${decodedData.uri}")
                    passed = false
                }

                if (decodedData.description == testDescription) {
                    details.add("PASS: description preserved after reload")
                } else {
                    details.add("FAIL: description mismatch after reload. Expected $testDescription, got ${decodedData.description}")
                    passed = false
                }

                if (event.timestamp != null && event.timestamp > 0) {
                    details.add("PASS: timestamp exists after reload")
                } else {
                    details.add("FAIL: missing timestamp after reload")
                    passed = false
                }
            }

        } finally {
            taskDao.deleteTask(task)
            details.add("Cleanup: task $taskId deleted")
        }

        return ScenarioResult("Image Persistence Lifecycle", passed, details, System.currentTimeMillis() - start)
    }
}

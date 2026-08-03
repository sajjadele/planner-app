package com.example.debug.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.debug.scenarios.GeneratedScenarioResult
import com.example.debug.scenarios.ScenarioType
import com.example.debug.validation.ValidationCheck
import com.example.debug.validation.ValidationResult
import com.example.debug.validation.ValidationReportFormatter
import com.example.domain.mirror.MirrorInsight
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskStepEntity
import com.example.ui.screens.components.DebugMirrorViewModel
import com.example.ui.screens.components.SolarSystemQaViewModel
import com.example.ui.theme.*

@Composable
fun DeveloperLabDialog(
    onDismiss: () -> Unit,
    viewModel: DeveloperLabViewModel = viewModel(),
    solarQaViewModel: SolarSystemQaViewModel = viewModel(),
    mirrorViewModel: DebugMirrorViewModel = viewModel(),
    activityLabViewModel: ActivityLabViewModel = viewModel(),
    activityScenarioViewModel: ActivityScenarioViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val solarStatus by solarQaViewModel.status.collectAsState()
    val mirrorResult by mirrorViewModel.lastResult.collectAsState()
    val mirrorStatus by mirrorViewModel.status.collectAsState()
    val mirrorScenario by mirrorViewModel.lastScenario.collectAsState()
    val activityLabState by activityLabViewModel.state.collectAsState()
    val activityScenarioState by activityScenarioViewModel.state.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (LocalIsDarkTheme.current) DarkSurfaceVariant else Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Developer Lab",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
                )
                Text(
                    text = "ابزار توسعه‌دهنده — فقط نسخه DEBUG",
                    fontSize = 11.sp,
                    color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary
                )

                state.message?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                ScenarioGeneratorSection(
                    isRunning = state.isRunning,
                    onGenerate = { viewModel.generate(it) },
                    lastGenerated = state.lastGenerated
                )

                ValidationRunnerSection(
                    validationResult = state.validationResult,
                    validationScenario = state.validationScenario,
                    isRunning = state.isRunning,
                    onValidate = { viewModel.validate(it) }
                )

                SolarSystemQaSection(
                    viewModel = solarQaViewModel,
                    status = solarStatus
                )

                MirrorTestingSection(
                    viewModel = mirrorViewModel,
                    result = mirrorResult,
                    status = mirrorStatus,
                    scenario = mirrorScenario
                )

                ActivityLabSection(
                    state = activityLabState,
                    onSelectTask = { activityLabViewModel.selectTask(it) },
                    onStepInputChange = { activityLabViewModel.updateStepInput(it) },
                    onAddStep = { activityLabViewModel.addStep() },
                    onDeleteStep = { activityLabViewModel.deleteStep(it) },
                    onRunVerification = { activityLabViewModel.runVerification() }
                )

                ActivityScenarioSection(
                    state = activityScenarioState,
                    onRunScenario = { activityScenarioViewModel.runScenario(it) },
                    onClearResults = { activityScenarioViewModel.clearResults() }
                )

                CleanupSection(
                    isRunning = state.isRunning,
                    onClear = { viewModel.clearAll() },
                    onClearSolarQa = { solarQaViewModel.clearQaData() },
                    onClearMirror = { mirrorViewModel.clearTestData() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("بستن", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ScenarioGeneratorSection(
    isRunning: Boolean,
    onGenerate: (ScenarioType) -> Unit,
    lastGenerated: GeneratedScenarioResult?
) {
    SectionCard(title = "Scenario Generator", subtitle = "Generate test users") {
        ScenarioButton("Productive User", Color(0xFF16A34A), ScenarioType.PRODUCTIVE_USER, isRunning, onGenerate)
        ScenarioButton("Procrastinator User", Color(0xFFEA580C), ScenarioType.PROCRASTINATOR_USER, isRunning, onGenerate)
        ScenarioButton("Chaos User", Color(0xFF2563EB), ScenarioType.CHAOS_USER, isRunning, onGenerate)
        ScenarioButton("Recovery User", Color(0xFF7C3AED), ScenarioType.RECOVERY_USER, isRunning, onGenerate)

        lastGenerated?.let { result ->
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Generated: ${result.description}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "Goal ID: ${result.goalId} | Tasks: ${result.createdTaskIds.size}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ValidationRunnerSection(
    validationResult: ValidationResult?,
    validationScenario: ScenarioType?,
    isRunning: Boolean,
    onValidate: (ScenarioType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    SectionCard(title = "Validation Runner", subtitle = "Run scenario validation checks") {
        ScenarioButton("Validate Productive", Color(0xFF16A34A), ScenarioType.PRODUCTIVE_USER, isRunning, onValidate)
        ScenarioButton("Validate Procrastinator", Color(0xFFEA580C), ScenarioType.PROCRASTINATOR_USER, isRunning, onValidate)
        ScenarioButton("Validate Chaos", Color(0xFF2563EB), ScenarioType.CHAOS_USER, isRunning, onValidate)
        ScenarioButton("Validate Recovery", Color(0xFF7C3AED), ScenarioType.RECOVERY_USER, isRunning, onValidate)

        validationResult?.let { result ->
            Spacer(modifier = Modifier.height(8.dp))
            val passColor = if (result.passed) Color(0xFF16A34A) else Color(0xFFDC2626)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = passColor.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (result.passed) "PASS" else "FAIL",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = passColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = result.scenario.label,
                            fontSize = 12.sp,
                            color = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
                        )
                    }
                    Text(
                        text = result.summary,
                        fontSize = 10.sp,
                        color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary,
                        modifier = Modifier.padding(top = 4.dp),
                        lineHeight = 14.sp
                    )

                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            if (expanded) "Hide checks" else "Show ${result.checks.size} checks",
                            fontSize = 11.sp,
                            color = AccentPurple
                        )
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            result.checks.forEach { check ->
                                ValidationCheckRow(check)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ValidationCheckRow(check: ValidationCheck) {
    val icon = if (check.passed) "✓" else "✗"
    val color = if (check.passed) Color(0xFF16A34A) else Color(0xFFDC2626)
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = check.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
            )
            Text(
                text = "expected: ${check.expected} | actual: ${check.actual}",
                fontSize = 10.sp,
                color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary
            )
        }
    }
}

@Composable
private fun SolarSystemQaSection(
    viewModel: SolarSystemQaViewModel,
    status: String?
) {
    SectionCard(title = "Solar System QA", subtitle = "Test visual states") {
        val scenarios = listOf(
            Triple("1. Empty Goal", Color(0xFF6B7280)) { viewModel.createEmptyGoal() },
            Triple("2. Calm Orbit", Color(0xFF16A34A)) { viewModel.createCalmOrbit() },
            Triple("3. Urgent Tasks", Color(0xFFDC2626)) { viewModel.createUrgentTasks() },
            Triple("4. Boulder Field", Color(0xFFEA580C)) { viewModel.createBoulderField() },
            Triple("5. Near Deadline", Color(0xFFF59E0B)) { viewModel.createNearDeadline() },
            Triple("6. Priority Mix", Color(0xFF2563EB)) { viewModel.createPriorityMix() },
            Triple("7. Cluster Stress", Color(0xFF7C3AED)) { viewModel.createClusterStress() },
            Triple("8. All Completed", Color(0xFF059669)) { viewModel.createAllCompleted() },
            Triple("9. Activity Rich", Color(0xFF9333EA)) { viewModel.createActivityRich() },
        )
        scenarios.forEach { (label, color, action) ->
            Button(
                onClick = action,
                colors = ButtonDefaults.buttonColors(containerColor = color),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(label, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }
        }
        Button(
            onClick = { viewModel.clearQaData() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            Text("Clear QA Data", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
        }
        status?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                fontSize = 11.sp,
                color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary
            )
        }
    }
}

@Composable
private fun MirrorTestingSection(
    viewModel: DebugMirrorViewModel,
    result: List<MirrorInsight>,
    status: String?,
    scenario: String?
) {
    SectionCard(title = "Mirror Testing", subtitle = "Test Mirror signal detection") {
        val buttons = listOf(
            "7-Day History" to { viewModel.simulateHistory() },
            "Boulder Scenario" to { viewModel.createBoulderScenario() },
            "Goal Attention" to { viewModel.createGoalAttentionScenario() },
            "Initiator/Finisher" to { viewModel.createInitiatorFinisherScenario() },
            "Consistency Decay" to { viewModel.createConsistencyDecayScenario() }
        )
        buttons.forEach { (label, action) ->
            Button(
                onClick = action,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Text(label, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
        }
        Button(
            onClick = { viewModel.clearTestData() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
        ) {
            Text("Clear Mirror Data", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        }

        status?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                fontSize = 11.sp,
                color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary
            )
        }

        if (result.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    scenario?.let {
                        Text(
                            text = "Scenario: $it",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = "Mirror detected (${result.size}):",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    result.forEach { insight ->
                        Text(
                            text = "• ${insight.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = insight.message,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityLabSection(
    state: ActivityLabUiState,
    onSelectTask: (TaskEntity) -> Unit,
    onStepInputChange: (String) -> Unit,
    onAddStep: () -> Unit,
    onDeleteStep: (TaskStepEntity) -> Unit,
    onRunVerification: () -> Unit
) {
    SectionCard(title = "Activity Lab", subtitle = "Test steps and activity events") {
        // Task selector
        Text(
            text = "Task Selector",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))

        var taskDropdownExpanded by remember { mutableStateOf(false) }
        Box {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { taskDropdownExpanded = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.selectedTask?.let { "${it.title} (ID: ${it.id})" } ?: "Select a task...",
                        fontSize = 12.sp,
                        color = if (state.selectedTask != null)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text("▼", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DropdownMenu(
                expanded = taskDropdownExpanded,
                onDismissRequest = { taskDropdownExpanded = false }
            ) {
                state.tasks.forEach { task ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "${task.title} (ID: ${task.id})",
                                fontSize = 12.sp
                            )
                        },
                        onClick = {
                            onSelectTask(task)
                            taskDropdownExpanded = false
                        }
                    )
                }
                if (state.tasks.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No tasks available", fontSize = 12.sp) },
                        onClick = { taskDropdownExpanded = false }
                    )
                }
            }
        }

        if (state.selectedTask != null) {
            Spacer(modifier = Modifier.height(12.dp))

            // Step input
            Text(
                text = "Add Step",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.stepInput,
                    onValueChange = onStepInputChange,
                    placeholder = { Text("Step title...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAddStep,
                    enabled = state.stepInput.isNotBlank() && !state.isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add", color = Color.White, fontSize = 12.sp)
                }
            }

            // Steps list
            if (state.steps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Steps (${state.steps.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                state.steps.forEach { step ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = step.title,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.title,
                                    fontSize = 12.sp,
                                    color = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
                                )
                                Text(
                                    text = "ID: ${step.id}",
                                    fontSize = 9.sp,
                                    color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary
                                )
                            }
                            TextButton(
                                onClick = { onDeleteStep(step) },
                                modifier = Modifier.padding(0.dp)
                            ) {
                                Text("Delete", fontSize = 10.sp, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }
            }

            // Activity timeline
            if (state.activities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Activity Timeline (${state.activities.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                state.activities.take(10).forEach { event ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = event.eventType,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "stepId: ${event.stepId ?: "none"}",
                                    fontSize = 9.sp,
                                    color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary
                                )
                            }
                            event.description?.let {
                                Text(
                                    text = it,
                                    fontSize = 11.sp,
                                    color = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
                                )
                            }
                        }
                    }
                }
                if (state.activities.size > 10) {
                    Text(
                        text = "...and ${state.activities.size - 10} more",
                        fontSize = 10.sp,
                        color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Verification button
        Button(
            onClick = onRunVerification,
            enabled = state.selectedTask != null && !state.isRunning,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Run Verification", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        }

        // Status
        state.status?.let { status ->
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = status,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ActivityScenarioSection(
    state: ActivityScenarioUiState,
    onRunScenario: (ActivityScenario) -> Unit,
    onClearResults: () -> Unit
) {
    SectionCard(title = "Activity Scenarios", subtitle = "Automated data layer scenario testing") {
        val buttons = listOf(
            ActivityScenario.TASK_LIFECYCLE to Color(0xFF16A34A),
            ActivityScenario.TIMELINE_MULTI_DAY to Color(0xFF2563EB),
            ActivityScenario.PERSISTENCE to Color(0xFF7C3AED),
            ActivityScenario.TIMELINE_RANGE to Color(0xFFEA580C),
            ActivityScenario.NOTE_LIFECYCLE to Color(0xFF0891B2),
            ActivityScenario.MANUAL_ACTIVITY_LIFECYCLE to Color(0xFFD97706),
            ActivityScenario.IMAGE_LIFECYCLE to Color(0xFF1565C0),
            ActivityScenario.IMAGE_PERSISTENCE_LIFECYCLE to Color(0xFF2196F3)
        )
        buttons.forEach { (scenario, color) ->
            Button(
                onClick = { onRunScenario(scenario) },
                enabled = !state.isRunning,
                colors = ButtonDefaults.buttonColors(containerColor = color),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            ) {
                Text(scenario.label, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
        }

        if (state.results.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClearResults,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            ) {
                Text("Clear Results", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }

            state.results.forEach { result ->
                Spacer(modifier = Modifier.height(6.dp))
                val passColor = if (result.passed) Color(0xFF16A34A) else Color(0xFFDC2626)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = passColor.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (result.passed) "PASS" else "FAIL",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = passColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = result.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${result.durationMs}ms",
                                fontSize = 10.sp,
                                color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary
                            )
                        }

                        var expanded by remember { mutableStateOf(false) }
                        TextButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                if (expanded) "Hide details" else "Show ${result.details.size} checks",
                                fontSize = 11.sp,
                                color = AccentPurple
                            )
                        }

                        AnimatedVisibility(visible = expanded) {
                            Column(modifier = Modifier.padding(top = 4.dp)) {
                                result.details.forEach { detail ->
                                    val detailColor = when {
                                        detail.startsWith("PASS:") -> Color(0xFF16A34A)
                                        detail.startsWith("FAIL:") -> Color(0xFFDC2626)
                                        detail.startsWith("Cleanup:") -> if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary
                                        else -> if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
                                    }
                                    Text(
                                        text = detail,
                                        fontSize = 10.sp,
                                        color = detailColor,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        state.status?.let { status ->
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = status,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun CleanupSection(
    isRunning: Boolean,
    onClear: () -> Unit,
    onClearSolarQa: () -> Unit,
    onClearMirror: () -> Unit
) {
    SectionCard(title = "Cleanup", subtitle = "Remove all developer test data") {
        Button(
            onClick = {
                onClear()
                onClearSolarQa()
                onClearMirror()
            },
            enabled = !isRunning,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear All Developer Test Data", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "Removes: 🧪 Lab, 🧪 QA, 🧪 Mirror Test data only",
            fontSize = 10.sp,
            color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ScenarioButton(
    label: String,
    color: Color,
    scenario: ScenarioType,
    isRunning: Boolean,
    onClick: (ScenarioType) -> Unit
) {
    Button(
        onClick = { onClick(scenario) },
        enabled = !isRunning,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (LocalIsDarkTheme.current)
                DarkBackground.copy(alpha = 0.5f)
            else
                Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}
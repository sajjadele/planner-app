1|package com.example.plugins.planner.ui
2|
3|4|import androidx.compose.foundation.background
5|import androidx.compose.foundation.clickable
6|import androidx.compose.foundation.layout.*
7|import androidx.compose.foundation.lazy.LazyColumn
8|import androidx.compose.foundation.lazy.LazyListState
9|import androidx.compose.foundation.lazy.items
10|import androidx.compose.foundation.lazy.rememberLazyListState
11|import androidx.compose.foundation.shape.RoundedCornerShape
12|import androidx.compose.foundation.text.KeyboardActions
13|import androidx.compose.foundation.text.KeyboardOptions
14|import androidx.compose.material.icons.Icons
15|import androidx.compose.material.icons.filled.Add
16|import androidx.compose.material.icons.filled.ArrowBack
17|import androidx.compose.material.icons.filled.ArrowDropDown
18|import androidx.compose.material.icons.filled.Close
19|import androidx.compose.material3.*
20|import androidx.compose.runtime.*
21|import androidx.compose.ui.Alignment
22|import androidx.compose.ui.Modifier
23|import androidx.compose.ui.draw.clip
24|import androidx.compose.ui.graphics.Color
25|import androidx.compose.ui.layout.ContentScale
26|import androidx.compose.ui.platform.LocalContext
27|import androidx.compose.ui.platform.LocalFocusManager
28|import androidx.compose.ui.text.font.FontWeight
29|import androidx.compose.ui.text.input.ImeAction
30|import androidx.compose.ui.text.style.TextOverflow
31|import androidx.compose.ui.unit.dp
32|import androidx.compose.ui.unit.sp
33|import androidx.lifecycle.viewmodel.compose.viewModel
34|import coil.compose.AsyncImage
35|import coil.request.ImageRequest
36|import com.example.core.util.RTL
37|import com.example.plugins.planner.data.ActivityEventEntity
38|import com.example.plugins.planner.data.ActivityMessageMapper
39|import com.example.plugins.planner.data.StepCardMapper
40|import com.example.plugins.planner.data.StepCardModel
41|import com.example.plugins.planner.data.TaskEntity
42|import com.example.plugins.planner.data.TaskStepEntity
43|import com.example.plugins.planner.ui.components.NeumorphicSurface
44|import com.example.plugins.planner.ui.components.ReminderSection
45|import com.example.plugins.planner.ui.components.StepCard
46|import com.example.plugins.planner.ui.components.skeletonShimmerBrush
47|import com.example.ui.theme.*
48|import kotlinx.coroutines.delay
49|
50|@Composable
51|fun TaskDetailScreen(
52|    taskId: Int,
53|    onBack: () -> Unit,
54|    modifier: Modifier = Modifier
55|) {
56|    val viewModel: TaskDetailViewModel = viewModel(
57|        key = "task_detail_$taskId",
58|        factory = TaskDetailViewModel.factory(
59|            LocalContext.current.applicationContext as android.app.Application,
60|            taskId
61|        )
62|    )
63|
64|    val task by viewModel.task.collectAsState()
65|    val isTaskLoaded by viewModel.isTaskLoaded.collectAsState()
66|    val activeGoals by viewModel.activeGoals.collectAsState()
67|    val allGoals by viewModel.allGoals.collectAsState()
68|    val taskLogs by viewModel.taskLogs.collectAsState()
69|    val steps by viewModel.steps.collectAsState()
70|    val activities by viewModel.activities.collectAsState()
71|    val selectedActivityDate by viewModel.selectedActivityDate.collectAsState()
72|    val timelineStartDate by viewModel.timelineStartDate.collectAsState()
73|    val timelineEndDate by viewModel.timelineEndDate.collectAsState()
74|
75|    var editableTitle by remember(task) { mutableStateOf(task?.title ?: "") }
76|    var showGoalDropdown by remember { mutableStateOf(false) }
77|    var logInput by remember { mutableStateOf("") }
78|    val focusManager = LocalFocusManager.current
79|    var selectedTab by remember { mutableIntStateOf(0) }
80|    val activityListState = rememberLazyListState()
81|    var showTimelineSheet by remember { mutableStateOf(false) }
82|    var showActivityComposer by remember { mutableStateOf(false) }
83|    var selectedStepIdForActivity by remember { mutableIntStateOf(-1) }
84|
85|    val currentGoalName = remember(task, allGoals) {
86|        task?.goalId?.let { gid -> allGoals.firstOrNull { it.id == gid }?.title }
87|    }
88|
89|    // ── Skeleton visibility: enforce minimum 250ms for smooth perceived loading ──
90|    var showSkeleton by remember { mutableStateOf(true) }
91|    LaunchedEffect(isTaskLoaded) {
92|        if (isTaskLoaded) {
93|            delay(250)
94|            showSkeleton = false
95|        }
96|    }
97|
98|    if (showSkeleton) {
99|        TaskDetailSkeleton(
100|            modifier = modifier
101|                .fillMaxSize()
102|                .background(MaterialTheme.colorScheme.background)
103|        )
104|        return
105|    }
106|
107|    Box(
108|        modifier = modifier
109|            .fillMaxSize()
110|            .background(MaterialTheme.colorScheme.background)
111|    ) {
112|        Column(
113|            modifier = Modifier.fillMaxSize()
114|        ) {
115|            TaskDetailTopBar(onBack = onBack)
116|
117|            TaskDetailTabSelector(
118|                selectedTab = selectedTab,
119|                onTabSelected = {
120|                    selectedTab = it
121|                    focusManager.clearFocus()
122|                }
123|            )
124|
125|            when (selectedTab) {
126|                0 -> TaskDetailOverviewContent(
127|                    task = task,
128|                    editableTitle = editableTitle,
129|                    onEditableTitleChange = { editableTitle = it },
130|                    showGoalDropdown = showGoalDropdown,
131|                    onShowGoalDropdownChange = { showGoalDropdown = it },
132|                    currentGoalName = currentGoalName,
133|                    activeGoals = activeGoals,
134|                    onUpdateTitle = { viewModel.updateTitle(it) },
135|                    onUpdateGoal = { viewModel.updateTaskGoal(it) },
136|                    onToggleCompletion = { viewModel.toggleTaskCompletion() },
137|                    onSetReminder = { h, m -> viewModel.setReminder(h, m) },
138|                    onClearReminder = { viewModel.clearReminder() },
139|                    focusManager = focusManager
140|                )
141|
142|                1 -> TaskDetailActivityContent(
143|                    modifier = Modifier
144|                        .weight(1f)
145|                        .padding(horizontal = 16.dp),
146|                    steps = steps,
147|                    activities = activities,
148|                    onTapComposer = { showActivityComposer = true },
149|                    onToggleStep = { viewModel.toggleStepCompletion(it) },
150|                    onDeleteStep = { viewModel.deleteStep(it) },
151|                    onAddActivityToStep = { stepId ->
152|                        selectedStepIdForActivity = stepId
153|                        showActivityComposer = true
154|                    },
155|                    listState = activityListState,
156|                    onOpenTimeline = { showTimelineSheet = true }
157|                )
158|            }
159|        }
160|
161|        TimelineBottomSheet(
162|            visible = showTimelineSheet,
163|            selectedDate = selectedActivityDate,
164|            timelineStartDate = timelineStartDate,
165|            timelineEndDate = timelineEndDate,
166|            activities = activities,
167|            onDismiss = { showTimelineSheet = false },
168|            onSelectDate = { viewModel.selectActivityDate(it) },
169|            onGoToToday = { viewModel.goToToday() },
170|            onMoveDate = { viewModel.moveActivityDate(it) }
171|        )
172|
173|        if (showActivityComposer) {
174|            ActivityComposerBottomSheet(
175|                onDismiss = { 
176|                    showActivityComposer = false
177|                    selectedStepIdForActivity = -1
178|                },
179|                onCreateActivity = { draft ->
180|                    val draftWithStepId = if (selectedStepIdForActivity != -1) {
182|                        draft.copy(stepId = selectedStepIdForActivity)
183|                    } else {
185|                        draft
186|                    }
187|                    viewModel.createActivity(draftWithStepId)
188|                    showActivityComposer = false
189|                    selectedStepIdForActivity = -1
190|                    focusManager.clearFocus()
191|                }
192|            )
193|        }
194|    }
195|}
196|
197|// ════════════════════════════════════════════════════════════════
198|// TOP BAR
199|// ════════════════════════════════════════════════════════════════
200|
201|@Composable
202|private fun TaskDetailTopBar(
203|    onBack: () -> Unit
204|) {
205|    Row(
206|        modifier = Modifier
207|            .fillMaxWidth()
208|            .padding(horizontal = 8.dp, vertical = 8.dp),
209|        verticalAlignment = Alignment.CenterVertically
210|    ) {
211|        IconButton(onClick = onBack) {
212|            Icon(
213|                imageVector = Icons.Default.ArrowBack,
214|                contentDescription = "بازگشت",
215|                tint = MaterialTheme.colorScheme.onBackground
216|            )
217|        }
218|        Text(
219|            text = "${RTL}جزئیات تسک",
220|            fontSize = 16.sp,
221|            fontWeight = FontWeight.Bold,
222|            color = MaterialTheme.colorScheme.onBackground,
223|            modifier = Modifier.weight(1f)
224|        )
225|    }
226|}
227|
228|// ════════════════════════════════════════════════════════════════
229|// TAB SELECTOR
230|// ════════════════════════════════════════════════════════════════
231|
232|@Composable
233|private fun TaskDetailTabSelector(
234|    selectedTab: Int,
235|    onTabSelected: (Int) -> Unit
236|) {
237|    val tabs = listOf("${RTL}نمای کلی", "${RTL}فعالیت")
238|    SingleChoiceSegmentedButtonRow(
239|        modifier = Modifier
240|            .fillMaxWidth()
241|            .padding(horizontal = 16.dp, vertical = 8.dp)
242|    ) {
243|        tabs.forEachIndexed { index, label ->
244|            SegmentedButton(
245|                selected = selectedTab == index,
246|                onClick = { onTabSelected(index) },
247|                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
248|                label = { Text(label, fontSize = 12.sp) }
249|            )
250|        }
251|    }
252|}
253|
254|// ════════════════════════════════════════════════════════════════
255|// OVERVIEW — stable task identity and configuration
256|// ════════════════════════════════════════════════════════════════
257|
258|@Composable
259|private fun TaskDetailOverviewContent(
260|    task: TaskEntity?,
261|    editableTitle: String,
262|    onEditableTitleChange: (String) -> Unit,
263|    showGoalDropdown: Boolean,
264|    onShowGoalDropdownChange: (Boolean) -> Unit,
265|    currentGoalName: String?,
266|    activeGoals: List<com.example.core.goal.GoalEntity>,
267|    onUpdateTitle: (String) -> Unit,
268|    onUpdateGoal: (Int?) -> Unit,
269|    onToggleCompletion: () -> Unit,
270|    onSetReminder: (Int, Int) -> Unit,
271|    onClearReminder: () -> Unit,
272|    focusManager: androidx.compose.ui.focus.FocusManager,
273|    modifier: Modifier = Modifier
274|) {
275|    NeumorphicSurface(
276|        modifier = modifier
277|            .fillMaxWidth()
278|            .padding(horizontal = 16.dp, vertical = 4.dp),
279|        shape = RoundedCornerShape(16.dp),
280|        elevation = 6
281|    ) {
282|        Column(
283|            modifier = Modifier
284|                .fillMaxWidth()
285|                .padding(16.dp)
286|        ) {
287|            // ── Editable Title ──
288|            OutlinedTextField(
289|                value = editableTitle,
290|                onValueChange = onEditableTitleChange,
291|                label = { Text("${RTL}عنوان تسک", fontSize = 12.sp) },
292|                modifier = Modifier.fillMaxWidth(),
293|                singleLine = true,
294|                colors = OutlinedTextFieldDefaults.colors(
295|                    focusedBorderColor = MaterialTheme.colorScheme.primary,
296|                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
297|                    cursorColor = MaterialTheme.colorScheme.primary,
298|                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
299|                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
300|                ),
301|                shape = RoundedCornerShape(12.dp),
302|                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
303|                keyboardActions = KeyboardActions(onDone = {
304|                    if (editableTitle.isNotBlank() && editableTitle != task?.title) {
305|                        onUpdateTitle(editableTitle.trim())
306|                    }
307|                    focusManager.clearFocus()
308|                })
309|            )
310|
311|            Spacer(modifier = Modifier.height(12.dp))
312|
313|            // ── Goal Reassignment ──
314|            Text(
315|                text = "${RTL}هدف مرتبط",
316|                fontSize = 12.sp,
317|                fontWeight = FontWeight.Bold,
318|                color = MaterialTheme.colorScheme.onSurfaceVariant
319|            )
320|            Spacer(modifier = Modifier.height(6.dp))
321|
322|            Box {
323|                Row(
324|                    modifier = Modifier
325|                        .fillMaxWidth()
326|                        .clip(RoundedCornerShape(12.dp))
327|                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
328|                        .clickable { onShowGoalDropdownChange(true) }
329|                        .padding(horizontal = 14.dp, vertical = 12.dp),
330|                    verticalAlignment = Alignment.CenterVertically,
331|                    horizontalArrangement = Arrangement.SpaceBetween
332|                ) {
333|                    Text(
334|                        text = currentGoalName ?: "بدون هدف",
335|                        fontSize = 13.sp,
336|                        color = if (currentGoalName != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
337|                        modifier = Modifier.weight(1f)
338|                    )
339|                    Icon(
340|                        imageVector = Icons.Default.ArrowDropDown,
341|                        contentDescription = null,
342|                        tint = MaterialTheme.colorScheme.primary,
343|                        modifier = Modifier.size(20.dp)
344|                    )
345|                }
346|
347|                DropdownMenu(
348|                    expanded = showGoalDropdown,
349|                    onDismissRequest = { onShowGoalDropdownChange(false) },
350|                    containerColor = MaterialTheme.colorScheme.surface
351|                ) {
352|                    DropdownMenuItem(
353|                        text = {
354|                            Text("بدون هدف", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
355|                        },
356|                        onClick = {
357|                            onUpdateGoal(null)
358|                            onShowGoalDropdownChange(false)
359|                        }
360|                    )
361|                    activeGoals.forEach { goal ->
362|                        DropdownMenuItem(
363|                            text = {
364|                                Text(goal.title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
365|                            },
366|                            onClick = {
367|                                onUpdateGoal(goal.id)
368|                                onShowGoalDropdownChange(false)
369|                            }
370|                        )
371|                    }
372|                }
373|            }
374|
375|            Spacer(modifier = Modifier.height(12.dp))
376|
377|            // ── Completion Toggle ──
378|            Row(
379|                modifier = Modifier.fillMaxWidth(),
380|                verticalAlignment = Alignment.CenterVertically,
381|                horizontalArrangement = Arrangement.SpaceBetween
382|            ) {
383|                Text(
384|                    text = "${RTL}وضعیت انجام",
385|                    fontSize = 12.sp,
386|                    fontWeight = FontWeight.Bold,
387|                    color = MaterialTheme.colorScheme.onSurfaceVariant
388|                )
389|                Switch(
390|                    checked = task?.isCompleted ?: false,
391|                    onCheckedChange = { onToggleCompletion() },
392|                    colors = SwitchDefaults.colors(
393|                        checkedThumbColor = Color.White,
394|                        checkedTrackColor = MaterialTheme.colorScheme.primary,
395|                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
396|                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
397|                    )
398|                )
399|            }
400|
401|            Spacer(modifier = Modifier.height(12.dp))
402|
403|            // ── Reminder ──
404|            ReminderSection(
405|                reminderHour = task?.reminderHour,
406|                reminderMinute = task?.reminderMinute,
407|                onSetReminder = onSetReminder,
408|                onClearReminder = onClearReminder
409|            )
410|        }
411|    }
412|}
413|
414|// ════════════════════════════════════════════════════════════════
415|// ACTIVITY — steps structure + timeline preview
416|// ════════════════════════════════════════════════════════════════
417|
418|@Composable
419|private fun TaskDetailActivityContent(
420|    modifier: Modifier = Modifier,
421|    steps: List<TaskStepEntity>,
422|    activities: List<ActivityEventEntity>,
423|    onTapComposer: () -> Unit,
424|    onToggleStep: (TaskStepEntity) -> Unit,
425|    onDeleteStep: (TaskStepEntity) -> Unit,
426|    onAddActivityToStep: (Int) -> Unit,
427|    listState: LazyListState = rememberLazyListState(),
428|    onOpenTimeline: () -> Unit
429|) {
430|    // Debug: Log activity content
432|    activities.forEachIndexed { idx, event ->
434|    }
435|
436|    LazyColumn(
437|        modifier = modifier,
438|        state = listState,
439|        verticalArrangement = Arrangement.spacedBy(6.dp),
440|        contentPadding = PaddingValues(bottom = 8.dp)
441|    ) {
442|        // ── Steps Header ──
443|        item(key = "steps_header") {
444|            Text(
445|                text = "${RTL}مراحل (${steps.size})",
446|                fontSize = 14.sp,
447|                fontWeight = FontWeight.Bold,
448|                color = MaterialTheme.colorScheme.onSurfaceVariant,
449|                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
450|            )
451|        }
452|
453|        // ── Steps List or Empty State ──
454|        if (steps.isEmpty()) {
455|            item(key = "steps_empty") {
456|                Box(
457|                    modifier = Modifier
458|                        .fillMaxWidth()
459|                        .padding(vertical = 16.dp),
460|                    contentAlignment = Alignment.Center
461|                ) {
462|                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
463|                        Text(text = "📝", fontSize = 24.sp)
464|                        Spacer(modifier = Modifier.height(6.dp))
465|                        Text(
466|                            text = "${RTL}هنوز مرحله‌ای تعریف نشده",
467|                            fontSize = 12.sp,
468|                            color = MaterialTheme.colorScheme.onSurfaceVariant
469|                        )
470|                    }
471|                }
472|            }
473|        } else {
474|            items(steps, key = { it.id }) { step ->
475|                // Convert step to StepCardModel with its activities
476|                val stepActivities = remember(activities, step.id) {
477|                    val filtered = activities.filter { it.stepId == step.id }
479|                    filtered.map { ActivityMessageMapper.toMessage(it) }
480|                }
481|                val stepCard = remember(step, stepActivities) {
482|                    StepCardMapper.toCardModel(step, stepActivities)
483|                }
484|
485|                StepCard(
486|                    model = stepCard,
487|                    onToggle = { onToggleStep(step) },
488|                    onDelete = { onDeleteStep(step) },
489|                    onAddActivity = onAddActivityToStep
490|                )
491|            }
492|        }
493|
494|        // ── Divider ──
495|        item(key = "divider") {
496|            HorizontalDivider(
497|                modifier = Modifier.padding(vertical = 8.dp),
498|                thickness = 1.dp,
499|                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
500|            )
501|        }
502|
503|        // ── Activity Header ──
504|        item(key = "activity_header") {
505|            Row(
506|                modifier = Modifier
507|                    .fillMaxWidth()
508|                    .padding(horizontal = 8.dp, vertical = 4.dp),
509|                horizontalArrangement = Arrangement.SpaceBetween,
510|                verticalAlignment = Alignment.CenterVertically
511|            ) {
512|                Text(
513|                    text = "${RTL}فعالیت‌ها",
514|                    fontSize = 14.sp,
515|                    fontWeight = FontWeight.Bold,
516|                    color = MaterialTheme.colorScheme.onSurfaceVariant
517|                )
518|                IconButton(onClick = onTapComposer) {
519|                    Icon(
520|                        imageVector = Icons.Default.Add,
521|                        contentDescription = "${RTL}افزودن فعالیت",
522|                        tint = MaterialTheme.colorScheme.primary
523|                    )
524|                }
525|            }
526|        }
527|
528|        // ── Timeline Preview Card ──
529|        item(key = "timeline_preview") {
530|            TimelinePreviewCard(
531|                activities = activities,
532|                onClick = onOpenTimeline
533|            )
534|        }
535|    }
536|}
537|
538|// ════════════════════════════════════════════════════════════════
539|// STEP ITEM
540|// ════════════════════════════════════════════════════════════════
541|
542|@Composable
543|private fun TaskStepItem(
544|    step: TaskStepEntity,
545|    onToggle: (TaskStepEntity) -> Unit,
546|    onDelete: (TaskStepEntity) -> Unit
547|) {
548|    NeumorphicSurface(
549|        modifier = Modifier.fillMaxWidth(),
550|        shape = RoundedCornerShape(12.dp),
551|        elevation = 3
552|    ) {
553|        Row(
554|            modifier = Modifier
555|                .fillMaxWidth()
556|                .padding(horizontal = 12.dp, vertical = 8.dp),
557|            verticalAlignment = Alignment.CenterVertically
558|        ) {
559|            Checkbox(
560|                checked = step.isCompleted,
561|                onCheckedChange = { onToggle(step) },
562|                modifier = Modifier.size(22.dp),
563|                colors = CheckboxDefaults.colors(
564|                    checkedColor = MaterialTheme.colorScheme.primary,
565|                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
566|                )
567|            )
568|
569|            Spacer(modifier = Modifier.width(10.dp))
570|
571|            Text(
572|                text = "${RTL}${step.title}",
573|                fontSize = 13.sp,
574|                color = if (step.isCompleted)
575|                    MaterialTheme.colorScheme.onSurfaceVariant
576|                else
577|                    MaterialTheme.colorScheme.onSurface,
578|                modifier = Modifier.weight(1f),
579|                maxLines = 2,
580|                overflow = TextOverflow.Ellipsis
581|            )
582|
583|            IconButton(
584|                onClick = { onDelete(step) },
585|                modifier = Modifier.size(32.dp)
586|            ) {
587|                Icon(
588|                    imageVector = Icons.Default.Close,
589|                    contentDescription = "${RTL}حذف",
590|                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
591|                    modifier = Modifier.size(16.dp)
592|                )
593|            }
594|        }
595|    }
596|}
597|
598|// ════════════════════════════════════════════════════════════════
599|// TIMELINE PREVIEW CARD
600|// ════════════════════════════════════════════════════════════════
601|
602|@Composable
603|private fun TimelinePreviewCard(
604|    activities: List<ActivityEventEntity>,
605|    onClick: () -> Unit
606|) {
607|    NeumorphicSurface(
608|        modifier = Modifier
609|            .fillMaxWidth()
610|            .clickable(onClick = onClick),
611|        shape = RoundedCornerShape(14.dp),
612|        elevation = 4
613|    ) {
614|        if (activities.isEmpty()) {
615|            Box(
616|                modifier = Modifier
617|                    .fillMaxWidth()
618|                    .padding(16.dp),
619|                contentAlignment = Alignment.Center
620|            ) {
621|                Text(
622|                    text = "${RTL}هنوز فعالیتی ثبت نشده",
623|                    fontSize = 12.sp,
624|                    color = MaterialTheme.colorScheme.onSurfaceVariant
625|                )
626|            }
627|        } else {
628|            val colorScheme = MaterialTheme.colorScheme
629|            val uiModels = remember(activities, colorScheme) {
630|                TimelineEventMapper.mapEvents(
631|                    events = activities,
632|                    useTimeOnly = false,
633|                    primary = colorScheme.primary,
634|                    error = colorScheme.error,
635|                    tertiary = colorScheme.tertiary,
636|                    outline = colorScheme.outline
637|                )
638|            }
639|            val latestModel = uiModels.maxByOrNull { it.timestamp }
640|            Column(
641|                modifier = Modifier
642|                    .fillMaxWidth()
643|                    .padding(14.dp)
644|            ) {
645|                Row(verticalAlignment = Alignment.CenterVertically) {
646|                    Text(text = "📅", fontSize = 16.sp)
647|                    Spacer(modifier = Modifier.width(6.dp))
648|                    Text(
649|                        text = "${RTL}تاریخچه فعالیت",
650|                        fontSize = 14.sp,
651|                        fontWeight = FontWeight.Bold,
652|                        color = MaterialTheme.colorScheme.onSurface
653|                    )
654|                }
655|
656|                if (latestModel != null) {
657|                    Spacer(modifier = Modifier.height(8.dp))
658|                    TimelinePreviewLatestEvent(uiModel = latestModel)
659|                }
660|
661|                Spacer(modifier = Modifier.height(6.dp))
662|                Text(
663|                    text = "${RTL}مشاهده تاریخچه ←",
664|                    fontSize = 12.sp,
665|                    fontWeight = FontWeight.Medium,
666|                    color = MaterialTheme.colorScheme.primary
667|                )
668|            }
669|        }
670|    }
671|}
672|
673|@Composable
674|private fun TimelinePreviewLatestEvent(uiModel: TimelineEventUiModel) {
675|    var showImageViewer by remember { mutableStateOf(false) }
676|
677|    Column {
678|        Row(verticalAlignment = Alignment.CenterVertically) {
679|            Text(
680|                text = uiModel.icon,
681|                fontSize = 12.sp,
682|                color = uiModel.color
683|            )
684|            Spacer(modifier = Modifier.width(4.dp))
685|            Text(
686|                text = uiModel.actionText,
687|                fontSize = 12.sp,
688|                color = uiModel.color,
689|                fontWeight = FontWeight.Medium
690|            )
691|        }
692|
693|        if (uiModel.imageUri != null) {
694|            Spacer(modifier = Modifier.height(4.dp))
695|            Surface(
696|                modifier = Modifier
697|                    .fillMaxWidth()
698|                    .height(100.dp)
699|                    .clip(RoundedCornerShape(8.dp))
700|                    .clickable { showImageViewer = true },
701|                shape = RoundedCornerShape(8.dp),
702|                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
703|            ) {
704|                Box(modifier = Modifier.fillMaxSize()) {
705|                    AsyncImage(
706|                        model = ImageRequest.Builder(LocalContext.current)
707|                            .data(uiModel.imageUri)
708|                            .crossfade(true)
709|                            .build(),
710|                        contentDescription = null,
711|                        modifier = Modifier.fillMaxSize(),
712|                        contentScale = ContentScale.Crop
713|                    )
714|                }
715|            }
716|        }
717|
718|        uiModel.objectText?.let { obj ->
719|            if (obj.isNotBlank()) {
720|                Spacer(modifier = Modifier.height(2.dp))
721|                Text(
722|                    text = "${RTL}$obj",
723|                    fontSize = 11.sp,
724|                    color = MaterialTheme.colorScheme.onSurface,
725|                    modifier = Modifier.padding(start = 16.dp)
726|                )
727|            }
728|        }
729|
730|        uiModel.supportingText?.let { support ->
731|            if (support.isNotBlank()) {
732|                Spacer(modifier = Modifier.height(2.dp))
733|                Text(
734|                    text = support,
735|                    fontSize = 10.sp,
736|                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
737|                    modifier = Modifier.padding(start = 16.dp)
738|                )
739|            }
740|        }
741|
742|        Spacer(modifier = Modifier.height(2.dp))
743|        Text(
744|            text = uiModel.timeText,
745|            fontSize = 10.sp,
746|            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
747|            modifier = Modifier.padding(start = 16.dp)
748|        )
749|    }
750|
751|    if (showImageViewer) {
752|        ImageViewerDialog(
753|            imageUri = uiModel.imageUri!!,
754|            onDismiss = { showImageViewer = false }
755|        )
756|    }
757|}
758|
759|// ════════════════════════════════════════════════════════════════
760|// SKELETON LOADING — mirrors real TaskDetailScreen layout
761|// ════════════════════════════════════════════════════════════════
762|
763|/** A single shimmer bar used by [TaskDetailSkeleton]. */
764|@Composable
765|private fun SkeletonBar(
766|    modifier: Modifier = Modifier,
767|    height: androidx.compose.ui.unit.Dp = 12.dp
768|) {
769|    Box(
770|        modifier = modifier
771|            .height(height)
772|            .clip(RoundedCornerShape(6.dp))
773|            .background(skeletonShimmerBrush())
774|    )
775|}
776|
777|/**
778| * Full skeleton for [TaskDetailScreen]. Mirrors the real layout:
779| * top bar → task edit card (title + goal + switch + reminder) → steps section → timeline preview card.
780| */
781|@Composable
782|private fun TaskDetailSkeleton(modifier: Modifier = Modifier) {
783|    Column(
784|        modifier = modifier.padding(horizontal = 0.dp)
785|    ) {
786|        // ── Top bar ──
787|        Row(
788|            modifier = Modifier
789|                .fillMaxWidth()
790|                .padding(horizontal = 8.dp, vertical = 8.dp),
791|            verticalAlignment = Alignment.CenterVertically
792|        ) {
793|            Box(
794|                modifier = Modifier
795|                    .size(40.dp)
796|                    .clip(RoundedCornerShape(20.dp))
797|                    .background(skeletonShimmerBrush())
798|            )
799|            Spacer(modifier = Modifier.width(8.dp))
800|            SkeletonBar(modifier = Modifier.weight(1f), height = 16.sp.value.dp)
801|        }
802|
803|        Spacer(modifier = Modifier.height(4.dp))
804|
805|        // ── Tab selector placeholder ──
806|        Row(
807|            modifier = Modifier
808|                .fillMaxWidth()
809|                .padding(horizontal = 16.dp, vertical = 8.dp),
810|            horizontalArrangement = Arrangement.spacedBy(8.dp)
811|        ) {
812|            SkeletonBar(modifier = Modifier.weight(1f), height = 36.dp)
813|            SkeletonBar(modifier = Modifier.weight(1f), height = 36.dp)
814|        }
815|
816|        Spacer(modifier = Modifier.height(4.dp))
817|
818|        // ── Task Edit Card ──
819|        NeumorphicSurface(
820|            modifier = Modifier
821|                .fillMaxWidth()
822|                .padding(horizontal = 16.dp, vertical = 4.dp),
823|            shape = RoundedCornerShape(16.dp),
824|            elevation = 6
825|        ) {
826|            Column(
827|                modifier = Modifier
828|                    .fillMaxWidth()
829|                    .padding(16.dp),
830|                verticalArrangement = Arrangement.spacedBy(12.dp)
831|            ) {
832|                // Title field placeholder
833|                SkeletonBar(modifier = Modifier.fillMaxWidth(), height = 48.dp)
834|
835|                // Goal relation placeholder
836|                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
837|                    SkeletonBar(modifier = Modifier.fillMaxWidth(0.3f), height = 10.sp.value.dp)
838|                    SkeletonBar(modifier = Modifier.fillMaxWidth(), height = 40.dp)
839|                }
840|
841|                // Completion toggle placeholder
842|                Row(
843|                    modifier = Modifier.fillMaxWidth(),
844|                    horizontalArrangement = Arrangement.SpaceBetween,
845|                    verticalAlignment = Alignment.CenterVertically
846|                ) {
847|                    SkeletonBar(modifier = Modifier.fillMaxWidth(0.35f), height = 10.sp.value.dp)
848|                    Box(
849|                        modifier = Modifier
850|                            .size(width = 44.dp, height = 24.dp)
851|                            .clip(RoundedCornerShape(12.dp))
852|                            .background(skeletonShimmerBrush())
853|                    )
854|                }
855|
856|                // Reminder section placeholder
857|                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
858|                    SkeletonBar(modifier = Modifier.fillMaxWidth(0.3f), height = 10.sp.value.dp)
859|                    Row(
860|                        modifier = Modifier.fillMaxWidth(),
861|                        horizontalArrangement = Arrangement.spacedBy(8.dp)
862|                    ) {
863|                        SkeletonBar(modifier = Modifier.weight(1f), height = 40.dp)
864|                        SkeletonBar(modifier = Modifier.weight(1f), height = 40.dp)
865|                    }
866|                }
867|            }
868|        }
869|
870|        Spacer(modifier = Modifier.height(12.dp))
871|
872|        // ── Steps section header ──
873|        SkeletonBar(
874|            modifier = Modifier
875|                .padding(horizontal = 24.dp)
876|                .fillMaxWidth(0.3f),
877|            height = 14.sp.value.dp
878|        )
879|
880|        Spacer(modifier = Modifier.height(8.dp))
881|
882|        // ── Add step input placeholder ──
883|        SkeletonBar(
884|            modifier = Modifier
885|                .padding(horizontal = 16.dp)
886|                .fillMaxWidth()
887|                .height(44.dp)
888|        )
889|
890|        Spacer(modifier = Modifier.height(6.dp))
891|
892|        // ── Step items skeleton ──
893|        Column(
894|            modifier = Modifier.padding(horizontal = 16.dp),
895|            verticalArrangement = Arrangement.spacedBy(6.dp)
896|        ) {
897|            repeat(2) {
898|                NeumorphicSurface(
899|                    modifier = Modifier.fillMaxWidth(),
900|                    shape = RoundedCornerShape(12.dp),
901|                    elevation = 3
902|                ) {
903|                    Row(
904|                        modifier = Modifier
905|                            .fillMaxWidth()
906|                            .padding(horizontal = 12.dp, vertical = 10.dp),
907|                        verticalAlignment = Alignment.CenterVertically
908|                    ) {
909|                        Box(
910|                            modifier = Modifier
911|                                .size(22.dp)
912|                                .clip(RoundedCornerShape(4.dp))
913|                                .background(skeletonShimmerBrush())
914|                        )
915|                        Spacer(modifier = Modifier.width(10.dp))
916|                        SkeletonBar(modifier = Modifier.weight(1f), height = 13.sp.value.dp)
917|                    }
918|                }
919|            }
920|        }
921|
922|        Spacer(modifier = Modifier.height(12.dp))
923|
924|        // ── Divider placeholder ──
925|        Box(
926|            modifier = Modifier
927|                .padding(horizontal = 16.dp)
928|                .fillMaxWidth()
929|                .height(1.dp)
930|                .background(skeletonShimmerBrush())
931|        )
932|
933|        Spacer(modifier = Modifier.height(12.dp))
934|
935|        // ── Timeline preview card skeleton ──
936|        NeumorphicSurface(
937|            modifier = Modifier
938|                .fillMaxWidth()
939|                .padding(horizontal = 16.dp),
940|            shape = RoundedCornerShape(14.dp),
941|            elevation = 4
942|        ) {
943|            Column(
944|                modifier = Modifier
945|                    .fillMaxWidth()
946|                    .padding(14.dp),
947|                verticalArrangement = Arrangement.spacedBy(8.dp)
948|            ) {
949|                Row(verticalAlignment = Alignment.CenterVertically) {
950|                    SkeletonBar(modifier = Modifier.size(16.dp))
951|                    Spacer(modifier = Modifier.width(6.dp))
952|                    SkeletonBar(modifier = Modifier.fillMaxWidth(0.4f), height = 14.sp.value.dp)
953|                }
954|                SkeletonBar(modifier = Modifier.fillMaxWidth(0.5f), height = 12.sp.value.dp)
955|                SkeletonBar(modifier = Modifier.fillMaxWidth(0.3f), height = 12.sp.value.dp)
956|            }
957|        }
958|    }
959|}
960|
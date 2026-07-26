1|package com.example.plugins.planner.data
2|
3|4|
5|/**
6| * StepCardMapper — Converts TaskStepEntity + ActivityMessageModels to StepCardModel.
7| *
8| * Architecture (Phase 4.9.2):
9| * - Single responsibility: Step + Activities → StepCardModel
10| * - Aggregates activity data (attachments, duration)
11| * - Handles empty/missing data gracefully
12| * - Never throws exceptions
13| *
14| * Usage:
15| * ```
16| * val cardModel = StepCardMapper.toCardModel(step, activities)
17| * // or
18| * val cardModels = StepCardMapper.toCardModels(steps, activitiesMap)
19| * ```
20| */
21|object StepCardMapper {
22|
23|    /**
24|     * Convert a single TaskStepEntity with its activities to StepCardModel.
25|     *
26|     * @param step The task step entity
27|     * @param activities List of activity messages associated with this step
28|     * @return StepCardModel with aggregated data
29|     */
30|    fun toCardModel(
31|        step: TaskStepEntity,
32|        activities: List<ActivityMessageModel> = emptyList()
33|    ): StepCardModel {
34|        // Debug: Log step card mapping
37|        activities.forEachIndexed { idx, msg ->
39|            msg.attachments.filterIsInstance<ActivityAttachment.Image>().forEach { img ->
41|            }
42|        }
43|        // Sort activities by timestamp ascending
44|        val sortedActivities = activities.sortedBy { it.timestamp }
45|
46|        // Calculate total attachment count
47|        val attachmentCount = sortedActivities.sumOf { it.attachments.size }
48|
49|        // Calculate total duration from manual activities
50|        val totalDurationMinutes = sortedActivities
51|            .mapNotNull { it.durationMinutes }
52|            .takeIf { it.isNotEmpty() }
53|            ?.sum()
54|
55|        return StepCardModel(
56|            id = step.id.toLong(),
57|            title = step.title,
58|            isCompleted = step.isCompleted,
59|            messages = sortedActivities,
60|            attachmentCount = attachmentCount,
61|            totalDurationMinutes = totalDurationMinutes,
62|            createdAt = step.createdAt
63|        )
64|    }
65|
66|    /**
67|     * Convert a list of TaskStepEntity with their activities to StepCardModel list.
68|     *
69|     * @param steps List of task step entities
70|     * @param activitiesMap Map of stepId to list of activity messages
71|     * @return List of StepCardModel
72|     */
73|    fun toCardModels(
74|        steps: List<TaskStepEntity>,
75|        activitiesMap: Map<Int, List<ActivityMessageModel>> = emptyMap()
76|    ): List<StepCardModel> {
77|        return steps.map { step ->
78|            val activities = activitiesMap[step.id] ?: emptyList()
79|            toCardModel(step, activities)
80|        }
81|    }
82|
83|    /**
84|     * Convert a list of ActivityEventEntity to ActivityMessageModel and group by stepId.
85|     *
86|     * @param events List of activity event entities
87|     * @return Map of stepId to list of ActivityMessageModel
88|     */
89|    fun groupActivitiesByStep(
90|        events: List<ActivityEventEntity>
91|    ): Map<Int, List<ActivityMessageModel>> {
92|        return events
93|            .filter { it.stepId != null }
94|            .groupBy { it.stepId!! }
95|            .mapValues { (_, stepEvents) ->
96|                stepEvents.map { ActivityMessageMapper.toMessage(it) }
97|            }
98|    }
99|
100|    /**
101|     * Create a summary text for the step card.
102|     */
103|    fun createSummary(model: StepCardModel): String {
104|        return model.getActivitySummary()
105|    }
106|
107|    /**
108|     * Check if a step has any previewable content.
109|     */
110|    fun hasPreviewContent(model: StepCardModel): Boolean {
111|        return model.hasRichContent()
112|    }
113|}
114|
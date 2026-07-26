1|package com.example.plugins.planner.data
2|
3|4|
5|/**
6| * ActivityMessageMapper — Converts ActivityEventEntity to ActivityMessageModel.
7| *
8| * Architecture (Phase 4.8.3):
9| * - Single responsibility: Entity → Model mapping
10| * - Decodes JSON payloads using ActivityPayloadCodec
11| * - Falls back to legacy format for backward compatibility
12| * - Never exposes raw JSON to UI
13| *
14| * Usage:
15| * ```
16| * val model = ActivityMessageMapper.toMessage(entity)
17| * // or
18| * val models = ActivityMessageMapper.toMessages(entities)
19| * ```
20| */
21|object ActivityMessageMapper {
22|
23|    /**
24|     * Convert a single ActivityEventEntity to ActivityMessageModel.
25|     */
26|    fun toMessage(entity: ActivityEventEntity): ActivityMessageModel {
27|        val eventType = parseEventType(entity.eventType)
28|
29|        // Try to decode JSON payload first
30|        val payload = decodePayload(entity.description)
31|
32|        // Debug: Log mapping details
35|        payload?.attachments?.forEachIndexed { idx, att ->
36|            if (att is ActivityAttachment.Image) {
38|            }
39|        }
40|
41|        // Extract fields based on event type
42|        val text = extractText(payload, entity.description, eventType)
43|        val attachments = payload?.attachments ?: emptyList()
44|        val durationMinutes = extractDuration(payload, entity, eventType)
45|        val isStep = eventType == ActivityEventType.STEP_CREATED
46|        val isCompleted = eventType == ActivityEventType.STEP_COMPLETED
47|
48|        return ActivityMessageModel(
49|            id = entity.id.toLong(),
50|            text = text,
51|            attachments = attachments,
52|            durationMinutes = durationMinutes,
53|            timestamp = entity.timestamp,
54|            isStep = isStep,
55|            isCompleted = isCompleted,
56|            eventTypeRaw = entity.eventType
57|        )
58|    }
59|
60|    /**
61|     * Convert a list of ActivityEventEntity to ActivityMessageModel list.
62|     */
63|    fun toMessages(entities: List<ActivityEventEntity>): List<ActivityMessageModel> {
64|        return entities.map { toMessage(it) }
65|    }
66|
67|    /**
68|     * Decode JSON payload using ActivityPayloadCodec.
69|     * Returns null if not JSON format.
70|     */
71|    private fun decodePayload(description: String?): ActivityPayload? {
72|        if (description == null) return null
73|        return ActivityPayloadCodec.decode(description)
74|    }
75|
76|    /**
77|     * Extract text based on payload and event type.
78|     */
79|    private fun extractText(
80|        payload: ActivityPayload?,
81|        description: String?,
82|        eventType: ActivityEventType?
83|    ): String? {
84|        // If payload has text, use it
85|        if (payload?.text != null) {
86|            return payload.text
87|        }
88|
89|        // Legacy format handling
90|        return when (eventType) {
91|            ActivityEventType.MANUAL_ACTIVITY -> {
92|                // Legacy: "title|duration"
93|                description?.split("|")?.firstOrNull()
94|            }
95|            ActivityEventType.IMAGE_ADDED -> {
96|                // Legacy: "uri:::description"
97|                description?.split(":::")?.getOrNull(1)
98|            }
99|            else -> description
100|        }
101|    }
102|
103|    /**
104|     * Extract duration based on payload and event type.
105|     */
106|    private fun extractDuration(
107|        payload: ActivityPayload?,
108|        entity: ActivityEventEntity,
109|        eventType: ActivityEventType?
110|    ): Int? {
111|        // If payload has duration, use it
112|        if (payload?.durationMinutes != null) {
113|            return payload.durationMinutes
114|        }
115|
116|        // Legacy format: "title|duration"
117|        if (eventType == ActivityEventType.MANUAL_ACTIVITY) {
118|            return entity.description?.split("|")?.getOrNull(1)?.toIntOrNull()
119|        }
120|
121|        return null
122|    }
123|
124|    /**
125|     * Parse event type string to ActivityEventType enum.
126|     */
127|    private fun parseEventType(raw: String): ActivityEventType? {
128|        return try {
129|            ActivityEventType.valueOf(raw)
130|        } catch (_: Exception) {
131|            null
132|        }
133|    }
134|}
135|
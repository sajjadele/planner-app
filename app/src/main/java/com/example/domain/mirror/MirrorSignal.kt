package com.example.domain.mirror

data class MirrorSignal(
    val type: MirrorSignalType,
    val goalId: Int? = null,
    val taskId: Int? = null,
    val confidence: Float = 0f,
    val metadata: Map<String, String> = emptyMap()
)

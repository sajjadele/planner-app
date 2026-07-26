package com.example.domain.mirror

data class MirrorInsight(
    val title: String,
    val message: String,
    val relatedGoalId: Int? = null,
    val relatedTaskId: Int? = null
)

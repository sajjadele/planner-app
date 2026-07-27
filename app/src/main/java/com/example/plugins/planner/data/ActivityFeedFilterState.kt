package com.example.plugins.planner.data

/**
 * ActivityFeedFilterState — UI state for filtering the activity feed.
 *
 * Phase 5.2.2: Activity Feed UX Layer
 *
 * Filtering happens entirely in the ViewModel/UI layer.
 * No database changes. Uses existing ActivityMessageModel.stepId.
 *
 * Default state (همه): shows all activities including task-level ones.
 * Step filter: shows only activities matching selectedStepId.
 * Image/file filters: for future use.
 */
data class ActivityFeedFilterState(
    /** Selected step ID. Null means show all activities. */
    val selectedStepId: Long? = null,
    /** Filter to show only image messages. */
    val showImagesOnly: Boolean = false,
    /** Filter to show only file messages. */
    val showFilesOnly: Boolean = false
) {
    companion object {
        val DEFAULT = ActivityFeedFilterState()
    }
}

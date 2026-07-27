package com.example.plugins.planner.data

/**
 * ActivityCreationAction — User intent for creating new content.
 *
 * Phase 5.4.1: Activity Feed Quick Stabilization
 *
 * Each action corresponds to a direct user intention from the FAB menu.
 * The ViewModel/Screen uses this to choose the appropriate creation flow.
 */
sealed class ActivityCreationAction {
    /** Create a text note. Opens the composer in text mode. */
    data object Note : ActivityCreationAction()

    /** Capture or pick an image. Opens image picker directly. */
    data object Image : ActivityCreationAction()

    /** Attach a file. Opens file picker directly. */
    data object File : ActivityCreationAction()

    /** Record a manual activity with duration. Opens composer with duration mode. */
    data object ManualActivity : ActivityCreationAction()
}

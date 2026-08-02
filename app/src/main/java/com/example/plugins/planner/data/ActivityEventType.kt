package com.example.plugins.planner.data

enum class ActivityEventType {
    // ── Debug-only (used by ActivityLabViewModel / ActivityScenarioViewModel) ──
    STEP_CREATED,
    STEP_COMPLETED,
    STEP_REOPENED,
    STEP_DELETED,
    // ── User-facing events ──
    NOTE_ADDED,
    FILE_ADDED,
    MANUAL_ACTIVITY,
    IMAGE_ADDED
}

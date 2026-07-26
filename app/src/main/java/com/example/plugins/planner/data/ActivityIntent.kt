package com.example.plugins.planner.data

/**
 * ActivityIntent — Declares the user's intent when creating an activity.
 *
 * Replaces boolean flags like `convertToStep`.
 * More extensible for future intents (e.g., REMINDER, BOOKMARK).
 */
enum class ActivityIntent {
    /** Regular activity — note, manual work, image log, etc. */
    ACTIVITY,

    /** Convert this activity into a TaskStep */
    STEP
}

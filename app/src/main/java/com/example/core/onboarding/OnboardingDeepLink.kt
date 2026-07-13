package com.example.core.onboarding

/**
 * One-shot bridge from the onboarding host (which lives above the tab system)
 * to GoalDashboardScreen, so the user lands directly on their new GoalDetailScreen.
 */
object OnboardingDeepLink {
    private var goalId: Int? = null
    private var showHomeHint: Boolean = false

    fun set(goalId: Int, showHomeHint: Boolean = false) {
        this.goalId = goalId
        this.showHomeHint = showHomeHint
    }

    /** Returns (goalId, showHomeHint) once, then clears. */
    fun consume(): Pair<Int, Boolean>? {
        val id = goalId ?: return null
        val hint = showHomeHint
        goalId = null
        showHomeHint = false
        return id to hint
    }
}

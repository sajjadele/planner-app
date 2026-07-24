package com.example.debug.scenarios

/**
 * Debug-only scenario types for Developer Lab testing.
 *
 * Each type represents a distinct user behavior pattern
 * that exercises different parts of the Attention/Mirror/Visibility system.
 */
enum class ScenarioType(val label: String) {
    /** Calm behavior: high completion, low reschedule, no overdue */
    PRODUCTIVE_USER("Productive User"),

    /** High attention: many reschedules, overdue, boulders */
    PROCRASTINATOR_USER("Procrastinator User"),

    /** Mixed behavior: varied attention scores, possible clustering */
    CHAOS_USER("Chaos User"),

    /** Behavior improvement: bad start → good finish */
    RECOVERY_USER("Recovery User")
}

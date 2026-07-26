package com.example.debug.scenarios

/**
 * Debug-only result of scenario generation.
 *
 * Contains all information needed for inspection and cleanup.
 */
data class GeneratedScenarioResult(
    /** Goal ID created by the generator */
    val goalId: Int,

    /** Which persona was generated */
    val scenarioType: ScenarioType,

    /** All task IDs created (for inspection) */
    val createdTaskIds: List<Int>,

    /** Human-readable description of what was generated */
    val description: String
)

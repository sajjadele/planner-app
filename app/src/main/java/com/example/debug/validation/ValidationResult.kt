package com.example.debug.validation

import com.example.debug.scenarios.ScenarioType

data class ValidationResult(
    val scenario: ScenarioType,
    val passed: Boolean,
    val checks: List<ValidationCheck>,
    val summary: String
)

data class ValidationCheck(
    val name: String,
    val expected: String,
    val actual: String,
    val passed: Boolean
)
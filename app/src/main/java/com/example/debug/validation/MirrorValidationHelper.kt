package com.example.debug.validation

import com.example.domain.mirror.MirrorSignalType

data class MirrorValidationResult(
    val expectedSignals: List<MirrorSignalType>,
    val actualSignals: List<MirrorSignalType>,
    val checks: List<ValidationCheck>
)

object MirrorValidationHelper {

    fun validate(
        expectedSignals: List<MirrorSignalType>,
        actualSignals: List<MirrorSignalType>
    ): MirrorValidationResult {
        val checks = mutableListOf<ValidationCheck>()

        for (expected in expectedSignals) {
            val actual = actualSignals.contains(expected)
            checks.add(
                ValidationCheck(
                    name = "Mirror: $expected",
                    expected = "Present",
                    actual = if (actual) "Present" else "Absent",
                    passed = actual
                )
            )
        }

        for (actual in actualSignals) {
            if (!expectedSignals.contains(actual)) {
                checks.add(
                    ValidationCheck(
                        name = "Mirror: $actual (unexpected)",
                        expected = "Absent",
                        actual = "Present",
                        passed = false
                    )
                )
            }
        }

        return MirrorValidationResult(
            expectedSignals = expectedSignals,
            actualSignals = actualSignals,
            checks = checks
        )
    }

    fun format(result: MirrorValidationResult): String {
        return buildString {
            appendLine("Expected: ${result.expectedSignals}")
            appendLine("Actual: ${result.actualSignals}")
        }
    }
}
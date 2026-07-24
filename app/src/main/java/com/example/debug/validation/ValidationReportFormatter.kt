package com.example.debug.validation

import com.example.debug.scenarios.ScenarioType
import com.example.domain.graph.VisibilityLevel

object ValidationReportFormatter {

    fun format(result: ValidationResult, attentionSummary: String? = null): String {
        val sb = StringBuilder()

        sb.appendLine("========================")
        sb.appendLine()
        sb.appendLine("Scenario: ${result.scenario.label}")
        sb.appendLine()

        sb.appendLine(result.summary)
        sb.appendLine()

        if (attentionSummary != null) {
            sb.append(attentionSummary)
            sb.appendLine()
        }

        sb.appendLine("CHECKS")
        sb.appendLine()

        for (check in result.checks) {
            val status = if (check.passed) "PASS" else "FAIL"
            sb.appendLine("  $status | ${check.name}")
            sb.appendLine("         expected: ${check.expected}")
            sb.appendLine("         actual:   ${check.actual}")
        }

        sb.appendLine()
        sb.appendLine("FINAL RESULT: ${if (result.passed) "PASS" else "FAIL"}")
        sb.appendLine()
        sb.appendLine("========================")

        return sb.toString()
    }

    fun formatCompact(result: ValidationResult): String {
        val failedChecks = result.checks.filter { !it.passed }
        val failedSummary = if (failedChecks.isEmpty()) "none" else
            failedChecks.joinToString(", ") { it.name }

        return "${result.scenario.label} | " +
                "${if (result.passed) "PASS" else "FAIL"} | " +
                "checks: ${result.checks.size} | " +
                "failed: $failedSummary"
    }

    fun formatTableRow(result: ValidationResult): String {
        val failed = result.checks.count { !it.passed }
        return "${result.scenario.label.padEnd(22)} " +
                "| ${if (result.passed) "PASS" else "FAIL"} " +
                "| failed: $failed/${result.checks.size}"
    }
}
package com.example.domain.insight

/**
 * Weekly velocity: is the user accelerating, maintaining, or declining?
 * Compared by completion rate (completed/created) of current vs previous week.
 */
enum class Velocity {
    IMPROVING,  // completing more than last week
    STABLE,     // roughly the same (±5%)
    DECLINING   // completing less than last week
}

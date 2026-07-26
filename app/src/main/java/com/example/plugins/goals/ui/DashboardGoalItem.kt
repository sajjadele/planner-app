package com.example.plugins.goals.ui

import com.example.core.goal.GoalEntity
import com.example.domain.goal.GoalProgress

/**
 * Precomputed dashboard row model — the single source of truth for one goal card.
 *
 * Phase 5.4 (ADR-0009): goal activity aggregates (`lastActivity`, `activeDays`) and the rolling
 * progress score (`progress`) were previously re-queried per card (each `GoalCard` collected its
 * own flows / built its own `combine().stateIn()`). They are now computed ONCE per dashboard
 * emission inside [GoalViewModel.goalsByTab] and handed to the Composable as stable values, so
 * the card no longer subscribes to any Flow and stays fully skippable.
 */
data class DashboardGoalItem(
    val goal: GoalEntity,
    val progress: GoalProgress?,
    val lastActivity: Long?,
    val activeDays: Int
)

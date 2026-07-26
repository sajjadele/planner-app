package com.example.core.plugin

import com.example.plugins.goals.GoalsPlugin
import com.example.plugins.planner.PlannerPlugin

object PluginRegistry {
    /**
     * Master list of all available plugins.
     * To add any future module (e.g., "Fitness Tracker"), simply implement AppPlugin and add it here.
     */
    val allPlugins: List<AppPlugin> = listOf(
        PlannerPlugin(),
        GoalsPlugin()
    )
}

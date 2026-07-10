package com.example.core.constants

/**
 * Predefined life areas for the lightweight hierarchy.
 * No CRUD needed — users select from this fixed list.
 */
object LifeAreas {
    data class LifeArea(val id: Int, val name: String, val icon: String)

    val all = listOf(
        LifeArea(1, "سلامتی", "💪"),
        LifeArea(2, "یادگیری", "📚"),
        LifeArea(3, "خانواده", "👨‍👩‍👧"),
        LifeArea(4, "کار", "💼"),
        LifeArea(5, "رشد شخصی", "🌱"),
        LifeArea(6, "انضباط فردی", "🎯")
    )

    fun getName(id: Int): String = all.find { it.id == id }?.name ?: "نامشخص"

    fun getIcon(id: Int): String = all.find { it.id == id }?.icon ?: "📌"
}

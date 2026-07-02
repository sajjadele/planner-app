package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "module_settings")
data class ModuleSettingsEntity(
    @PrimaryKey val moduleId: String,
    val isEnabled: Boolean
)

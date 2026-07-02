package com.example.core.plugin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.database.ModuleSettingsEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ModuleSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).moduleSettingsDao()

    // Map of moduleId to isEnabled (defaulting to true if not saved yet)
    val enabledModulesState: StateFlow<Map<String, Boolean>> = dao.getAllSettings()
        .map { list ->
            val map = list.associate { it.moduleId to it.isEnabled }.toMutableMap()
            // For any registered plugin not in database, default to enabled = true
            PluginRegistry.allPlugins.forEach { plugin ->
                if (!map.containsKey(plugin.id)) {
                    map[plugin.id] = true
                }
            }
            map
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun toggleModule(moduleId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            dao.insertSetting(ModuleSettingsEntity(moduleId, isEnabled))
        }
    }
}

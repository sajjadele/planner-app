package com.example.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

class ThemeRepository(private val context: Context) {

    private val dataStore = context.themeDataStore

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }

    /** Observe the persisted theme selection (defaults to SYSTEM) */
    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        val stored = preferences[THEME_MODE_KEY] ?: return@map ThemeMode.SYSTEM
        try {
            ThemeMode.valueOf(stored)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    /** Persist the user's theme selection */
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
}

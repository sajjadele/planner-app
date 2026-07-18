package com.example.plugins.goals

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Phase 5.5 — first-time education gate for the Behavioral Solar System Graph.
 *
 * Tracks whether the user has seen the one-time Graph introduction. Mirrors the established
 * [com.example.core.onboarding.OnboardingRepository] DataStore pattern: a `preferencesDataStore`
 * delegate + a boolean `Flow` read and a suspend write. The gate is read/written only from the
 * ViewModel (never composable `remember`) so it survives tab teardown/recreation.
 */
private val Context.graphSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "graph_settings"
)

class GraphViewPreferences(private val context: Context) {
    private val dataStore = context.graphSettingsDataStore

    companion object {
        private val INTRODUCTION_SEEN_KEY = booleanPreferencesKey("graph_introduction_seen")
    }

    /** True once the user has dismissed the Graph introduction at least once. */
    val hasSeenIntroduction: Flow<Boolean> = dataStore.data.map { it[INTRODUCTION_SEEN_KEY] == true }

    /** Persist that the introduction has been seen (called after the education dialog is dismissed). */
    suspend fun markIntroductionSeen() {
        dataStore.edit { it[INTRODUCTION_SEEN_KEY] = true }
    }
}

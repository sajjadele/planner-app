package com.example.core.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding_settings")

class OnboardingRepository(private val context: Context) {
    private val dataStore = context.onboardingDataStore

    companion object {
        private val COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    }

    /** Once-only gate. Defaults to false -> onboarding shows on first launch. */
    val isCompleted: Flow<Boolean> = dataStore.data.map { it[COMPLETED_KEY] == true }

    /** Flipped LAST, only after Room writes confirm (no empty-state leakage). */
    suspend fun setCompleted() {
        dataStore.edit { it[COMPLETED_KEY] = true }
    }
}

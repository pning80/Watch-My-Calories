package com.pning80.watchmycalories.ui.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val IS_METRIC = booleanPreferencesKey("is_metric")
        val APP_THEME = stringPreferencesKey("app_theme")
        val AI_CONSENT = stringPreferencesKey("ai_consent")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }

    // --- Unit System ---
    val isMetricFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_METRIC] ?: true // Default to metric
        }

    suspend fun setMetric(isMetric: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_METRIC] = isMetric
        }
    }

    // --- App Theme (System / Light / Dark) ---
    val appThemeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[APP_THEME] ?: "System"
        }

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME] = theme
        }
    }

    // --- AI Consent (notAsked / accepted / declined) ---
    val aiConsentFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[AI_CONSENT] ?: "notAsked"
        }

    suspend fun setAiConsent(consent: String) {
        context.dataStore.edit { preferences ->
            preferences[AI_CONSENT] = consent
        }
    }

    // --- Onboarding ---
    val hasCompletedOnboardingFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] ?: false
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] = completed
        }
    }
}

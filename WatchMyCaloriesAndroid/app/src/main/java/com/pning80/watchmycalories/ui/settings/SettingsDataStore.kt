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
        // Whether the user has dismissed the one-time "estimates are approximate"
        // disclaimer (PORTING_CRITERIA.md T1.1, parity with iOS SettingsStore
        // hasSeenEstimateDisclaimer).
        val HAS_SEEN_ESTIMATE_DISCLAIMER = booleanPreferencesKey("has_seen_estimate_disclaimer")
    }

    // --- Unit System ---
    val isMetricFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            // Default follows locale, mirroring iOS SettingsStore.localeDefault
            // (region == "US" ? .us : .metric, SettingsStore.swift:11). Android
            // previously hardcoded metric, so US-locale users defaulted to cm/kg
            // while iOS defaulted to ft/lbs.
            preferences[IS_METRIC] ?: (java.util.Locale.getDefault().country != "US")
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

    // --- Estimate disclaimer (one-time) ---
    val hasSeenEstimateDisclaimerFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAS_SEEN_ESTIMATE_DISCLAIMER] ?: false
        }

    suspend fun setSeenEstimateDisclaimer(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_SEEN_ESTIMATE_DISCLAIMER] = seen
        }
    }
}

package com.pning80.caloriewatcherandroid.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pning80.caloriewatcherandroid.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val DATA_STORE_API_KEY = stringPreferencesKey("gemini_api_key")
    private val PREFS_FILE = "secret_shared_prefs"
    private val ENCRYPTED_KEY = "encrypted_gemini_api_key"

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _apiKeyFlow = MutableStateFlow<String?>(null)
    val apiKey: Flow<String?> = _apiKeyFlow.asStateFlow()

    init {
        // Load initially from EncryptedSharedPreferences
        val storedKey = sharedPreferences.getString(ENCRYPTED_KEY, null)
        if (storedKey != null) {
            _apiKeyFlow.value = storedKey
        }

        // Async migration or fallback check
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (_apiKeyFlow.value == null) {
                // Check DataStore for migration
                val dataStoreKey = context.dataStore.data.map { it[DATA_STORE_API_KEY] }.first()
                if (!dataStoreKey.isNullOrBlank()) {
                    saveApiKey(dataStoreKey)
                    context.dataStore.edit { it.remove(DATA_STORE_API_KEY) }
                } else {
                    // Check BuildConfig
                    val buildConfigKey = BuildConfig.GEMINI_API_KEY
                    if (buildConfigKey.isNotBlank()) {
                        saveApiKey(buildConfigKey)
                    }
                }
            }
        }
    }

    suspend fun saveApiKey(key: String) {
        sharedPreferences.edit().putString(ENCRYPTED_KEY, key).apply()
        _apiKeyFlow.value = key
    }
}

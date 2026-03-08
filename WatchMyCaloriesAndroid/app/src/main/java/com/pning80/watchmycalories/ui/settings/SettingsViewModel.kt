package com.pning80.watchmycalories.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pning80.watchmycalories.data.database.UserProfileDao
import com.pning80.watchmycalories.data.model.ActivityLevel
import com.pning80.watchmycalories.data.model.Gender
import com.pning80.watchmycalories.data.model.UserProfile
import com.pning80.watchmycalories.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userProfileDao: UserProfileDao
) : ViewModel() {

    val apiKey: StateFlow<String?> = settingsRepository.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userProfile: StateFlow<UserProfile?> = userProfileDao.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // IMPORTANT: The Google AI client-side SDK for Android (com.google.ai.client.generativeai)
    // does not provide a function to list the available models from the server.
    // Therefore, we use a curated list mapping user-friendly names to the correct API identifiers.
    private val modelMap = mapOf(
        "Gemini Flash (Latest)" to "gemini-flash-latest",
        "Gemini Pro (Latest)" to "gemini-pro-latest",
        "Gemini 2.5 Flash" to "gemini-2.5-flash"
    )

    private val _models = MutableStateFlow<List<String>>(modelMap.keys.toList())
    val models: StateFlow<List<String>> = _models.asStateFlow()

    // This flow holds the friendly name for the UI
    val selectedModel: StateFlow<String> = settingsRepository.selectedModel
        .map { savedApiModel ->
            // Find the friendly name corresponding to the saved API model name,
            // or default to the first model in our list.
            modelMap.entries.find { it.value == savedApiModel }?.key ?: modelMap.keys.first()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), modelMap.keys.first())

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.saveApiKey(key)
        }
    }

    // The UI passes the friendly name, so we convert it to the API name before saving.
    fun saveSelectedModel(friendlyName: String) {
        viewModelScope.launch {
            val apiName = modelMap[friendlyName] ?: return@launch
            settingsRepository.saveSelectedModel(apiName)
        }
    }

    fun saveProfile(
        height: Double,
        weight: Double,
        age: Int,
        gender: Gender,
        activityLevel: ActivityLevel,
        targetCalories: Int
    ) {
        viewModelScope.launch {
            val profile = userProfile.first()?.copy(
                heightCm = height,
                weightKg = weight,
                age = age,
                gender = gender,
                activityLevel = activityLevel,
                targetCalories = targetCalories.toDouble()
            ) ?: UserProfile(
                heightCm = height,
                weightKg = weight,
                age = age,
                gender = gender,
                activityLevel = activityLevel,
                targetCalories = targetCalories.toDouble()
            )
            userProfileDao.insertUserProfile(profile)
        }
    }
}

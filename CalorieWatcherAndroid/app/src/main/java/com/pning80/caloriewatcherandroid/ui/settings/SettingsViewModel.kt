package com.pning80.caloriewatcherandroid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pning80.caloriewatcherandroid.data.database.UserProfileDao
import com.pning80.caloriewatcherandroid.data.model.ActivityLevel
import com.pning80.caloriewatcherandroid.data.model.Gender
import com.pning80.caloriewatcherandroid.data.model.UserProfile
import com.pning80.caloriewatcherandroid.data.repository.SettingsRepository
import com.pning80.caloriewatcherandroid.util.CalorieCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.saveApiKey(key)
        }
    }

    fun saveProfile(
        height: Double,
        weight: Double,
        age: Int,
        gender: Gender,
        activityLevel: ActivityLevel
    ) {
        viewModelScope.launch {
            val bmr = CalorieCalculator.calculateBMR(weight, height, age, gender)
            val tdee = CalorieCalculator.calculateTDEE(bmr, activityLevel)
            
            val profile = UserProfile(
                heightCm = height,
                weightKg = weight,
                age = age,
                gender = gender,
                activityLevel = activityLevel,
                targetCalories = tdee
            )
            userProfileDao.insertUserProfile(profile)
        }
    }
}

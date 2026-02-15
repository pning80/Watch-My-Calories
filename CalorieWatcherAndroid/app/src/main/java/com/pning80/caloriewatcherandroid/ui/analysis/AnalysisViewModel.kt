package com.pning80.caloriewatcherandroid.ui.analysis

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pning80.caloriewatcherandroid.data.model.FoodAnalysisResult
import com.pning80.caloriewatcherandroid.data.model.FoodEntry
import com.pning80.caloriewatcherandroid.data.model.MealType
import com.pning80.caloriewatcherandroid.data.repository.GeminiRepository
import com.pning80.caloriewatcherandroid.data.database.FoodEntryDao
import com.pning80.caloriewatcherandroid.util.ImageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

sealed class AnalysisUiState {
    object Idle : AnalysisUiState()
    object Loading : AnalysisUiState()
    data class Success(val result: FoodAnalysisResult) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val foodEntryDao: FoodEntryDao,
    private val settingsRepository: com.pning80.caloriewatcherandroid.data.repository.SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    fun analyzeImages(imagePaths: List<String>) {
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Loading
            
            // Load bitmaps
            val bitmaps = imagePaths.mapNotNull { ImageHelper.loadImageFromPath(it) }
            
            if (bitmaps.isEmpty()) {
                _uiState.value = AnalysisUiState.Error("Failed to load images")
                return@launch
            }

            // Fetch API Key
            val apiKey = settingsRepository.apiKey.firstOrNull() ?: ""

            val result = geminiRepository.estimateCalories(bitmaps, apiKey)
            
            result.onSuccess {
                _uiState.value = AnalysisUiState.Success(it)
            }.onFailure {
                _uiState.value = AnalysisUiState.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun saveEntry(entry: FoodEntry) {
        viewModelScope.launch {
            foodEntryDao.insertEntry(entry)
        }
    }
}

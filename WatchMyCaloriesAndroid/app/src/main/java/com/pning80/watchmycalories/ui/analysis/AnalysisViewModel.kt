package com.pning80.watchmycalories.ui.analysis

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pning80.watchmycalories.data.model.FoodAnalysisResult
import com.pning80.watchmycalories.data.model.FoodEntry
import com.pning80.watchmycalories.data.repository.GeminiRepository
import com.pning80.watchmycalories.data.database.FoodEntryDao
import com.pning80.watchmycalories.util.ImageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AnalysisUiState {
    object Idle : AnalysisUiState()
    object Loading : AnalysisUiState()
    data class Success(val result: FoodAnalysisResult) : AnalysisUiState()
    data class Error(val message: String, val apiKeySuffix: String?) : AnalysisUiState()
}

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val foodEntryDao: FoodEntryDao,
    private val settingsRepository: com.pning80.watchmycalories.data.repository.SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    fun analyzeImages(imagePaths: List<String>) {
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Loading
            
            val bitmaps = imagePaths.mapNotNull { ImageHelper.loadImageFromPath(it) }
            
            if (bitmaps.isEmpty()) {
                _uiState.value = AnalysisUiState.Error("Failed to load images", null)
                return@launch
            }

            val apiKey = settingsRepository.apiKey.firstOrNull() ?: ""
            val selectedModel = settingsRepository.selectedModel.firstOrNull() ?: "gemini-1.5-flash"
            
            val result = geminiRepository.estimateCalories(bitmaps, apiKey, selectedModel)
            
            result.onSuccess {
                _uiState.value = AnalysisUiState.Success(it)
            }.onFailure {
                val apiKeySuffix = if (apiKey.length > 8) {
                    "...${apiKey.takeLast(8)}"
                } else {
                    apiKey
                }
                _uiState.value = AnalysisUiState.Error(it.message ?: "Unknown error", apiKeySuffix)
            }
        }
    }

    fun saveEntry(entry: FoodEntry) {
        viewModelScope.launch {
            foodEntryDao.insertEntry(entry)
        }
    }
}

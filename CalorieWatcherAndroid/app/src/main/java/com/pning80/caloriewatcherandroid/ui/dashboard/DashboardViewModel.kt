package com.pning80.caloriewatcherandroid.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pning80.caloriewatcherandroid.data.model.FoodEntry
import com.pning80.caloriewatcherandroid.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val todayEntries: List<FoodEntry> = emptyList(),
    val totalConsumed: Int = 0,
    val targetCalories: Int = 2000,
    val burnedCalories: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load burned calories
            val burned = healthRepository.getTodayActiveCalories()
            _uiState.update { it.copy(burnedCalories = burned.toInt()) }
            
            // TODO: Load food entries from a repository
            // For now, we'll just use an empty list or mock data if needed.
        }
    }
}

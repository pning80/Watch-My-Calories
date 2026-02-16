package com.pning80.caloriewatcherandroid.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pning80.caloriewatcherandroid.data.database.FoodEntryDao
import com.pning80.caloriewatcherandroid.data.database.UserProfileDao
import com.pning80.caloriewatcherandroid.data.model.FoodEntry
import com.pning80.caloriewatcherandroid.data.model.UserProfile
import com.pning80.caloriewatcherandroid.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class TodayUiState(
    val todayEntries: List<FoodEntry> = emptyList(),
    val userProfile: UserProfile? = null,
    val totalConsumed: Int = 0,
    val burnedCalories: Int = 0,
    val targetCalories: Int = 2000,
    val remainingCalories: Int = 2000
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val foodEntryDao: FoodEntryDao,
    private val userProfileDao: UserProfileDao,
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        val todayStart = getStartOfDay()
        val todayEnd = getEndOfDay()

        viewModelScope.launch {
            // Combine flows for food entries and user profile
            combine(
                foodEntryDao.getEntriesForDate(todayStart, todayEnd),
                userProfileDao.getUserProfile()
            ) { entries, profile ->
                val totalConsumed = entries.sumOf { it.calories }.toInt()
                val target = profile?.targetCalories?.toInt() ?: 2000
                
                // Re-fetch burned calories in case they've updated (or set up a periodic refresh if needed)
                val currentBurned = healthRepository.getTodayActiveCalories().toInt()
                
                // Calculate remaining: (Target + Burned) - Consumed
                // Usually Remaining = Budget - Consumed. Budget = Base Target + Active Burned.
                val remaining = (target + currentBurned) - totalConsumed

                TodayUiState(
                    todayEntries = entries,
                    userProfile = profile,
                    totalConsumed = totalConsumed,
                    burnedCalories = currentBurned,
                    targetCalories = target,
                    remainingCalories = remaining
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    fun refreshBurnedCalories() {
        viewModelScope.launch {
            val burned = healthRepository.getTodayActiveCalories().toInt()
            _uiState.update { currentState ->
                val target = currentState.targetCalories
                val consumed = currentState.totalConsumed
                val remaining = (target + burned) - consumed
                
                currentState.copy(
                    burnedCalories = burned,
                    remainingCalories = remaining
                )
            }
        }
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getEndOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}

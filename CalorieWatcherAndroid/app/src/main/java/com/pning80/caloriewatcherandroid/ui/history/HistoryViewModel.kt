package com.pning80.caloriewatcherandroid.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pning80.caloriewatcherandroid.data.database.FoodEntryDao
import com.pning80.caloriewatcherandroid.data.model.FoodEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DailySummary(
    val date: String,
    val totalCalories: Int,
    val entries: List<FoodEntry>
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val foodEntryDao: FoodEntryDao
) : ViewModel() {

    val history: StateFlow<List<DailySummary>> = foodEntryDao.getAllEntries()
        .map { entries ->
            entries.groupBy { 
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp))
            }.map { (date, dailyEntries) ->
                DailySummary(
                    date = date,
                    totalCalories = dailyEntries.sumOf { it.calories }.toInt(),
                    entries = dailyEntries
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

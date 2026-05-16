package com.pning80.watchmycalories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pning80.watchmycalories.data.AppDatabase
import com.pning80.watchmycalories.data.FoodEntry
import com.pning80.watchmycalories.data.FoodEntryDao
import com.pning80.watchmycalories.data.MenuScan
import com.pning80.watchmycalories.data.MenuScanDao
import com.pning80.watchmycalories.data.UserProfile
import com.pning80.watchmycalories.data.UserProfileDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.pning80.watchmycalories.health.HealthConnectManager
import java.util.Calendar
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val foodDao: FoodEntryDao = db.foodEntryDao()
    private val userDao: UserProfileDao = db.userProfileDao()
    private val menuScanDao: MenuScanDao = db.menuScanDao()
    val healthConnectManager = HealthConnectManager(application)

    /** All food entries, sorted by timestamp DESC. */
    val allEntries: StateFlow<List<FoodEntry>> = foodDao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** All menu scans. */
    val allMenuScans: StateFlow<List<MenuScan>> = menuScanDao.getAllScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Today's entries only. */
    val todayEntries: StateFlow<List<FoodEntry>> = foodDao.getAllEntries()
        .map { entries ->
            val calendar = Calendar.getInstance()
            val todayStart = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            entries.filter { it.timestamp >= todayStart }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** The user's profile (null if not set up yet). */
    val userProfile: StateFlow<UserProfile?> = userDao.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Target calories — from profile or default 2000. */
    val targetCalories: StateFlow<Double> = userProfile
        .map { it?.targetCalories ?: 2000.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000.0)

    val burnedCalories: StateFlow<Double> = healthConnectManager.activeEnergyBurned
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Mutations ---

    fun addEntry(entry: FoodEntry) {
        viewModelScope.launch {
            foodDao.insertEntry(entry)
        }
    }

    fun updateEntry(entry: FoodEntry) {
        viewModelScope.launch { foodDao.updateEntry(entry) }
    }

    fun updateEntries(entries: List<FoodEntry>) {
        viewModelScope.launch {
            entries.forEach { foodDao.updateEntry(it) }
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch { foodDao.deleteEntry(id) }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch { userDao.insertProfile(profile) }
    }

    fun addMenuScan(scan: MenuScan) {
        viewModelScope.launch { menuScanDao.insertScan(scan) }
    }

    fun deleteMenuScan(id: String) {
        viewModelScope.launch { menuScanDao.deleteScan(id) }
    }
}

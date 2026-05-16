package com.pning80.watchmycalories.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)
}

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<FoodEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: FoodEntry)
    
    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteEntry(id: String)
    
    @androidx.room.Update
    suspend fun updateEntry(entry: FoodEntry)
}

@Dao
interface MenuScanDao {
    @Query("SELECT * FROM menu_scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<MenuScan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: MenuScan)

    @Query("DELETE FROM menu_scans WHERE id = :id")
    suspend fun deleteScan(id: String)
}

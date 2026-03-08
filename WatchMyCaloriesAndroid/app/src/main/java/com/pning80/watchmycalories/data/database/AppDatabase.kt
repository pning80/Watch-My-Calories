package com.pning80.watchmycalories.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pning80.watchmycalories.data.model.FoodEntry
import com.pning80.watchmycalories.data.model.UserProfile

@Database(entities = [FoodEntry::class, UserProfile::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun userProfileDao(): UserProfileDao
}

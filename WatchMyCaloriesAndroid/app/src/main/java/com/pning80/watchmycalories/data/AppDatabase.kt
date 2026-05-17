package com.pning80.watchmycalories.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Version bumped to 3 for PORTING_CRITERIA.md T1.2: FoodEntry.imageID → imageID,
// MenuScan.imageID → imageID, MenuScan.itemsData → itemsData. `fallbackToDestructive-
// Migration` wipes existing dev installs (acceptable pre-launch; remove the fallback
// and write real migrations when the app first ships to Play Store).
@Database(entities = [UserProfile::class, FoodEntry::class, MenuScan::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun menuScanDao(): MenuScanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "watch_my_calories_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.pning80.caloriewatcherandroid.di

import android.content.Context
import androidx.room.Room
import com.pning80.caloriewatcherandroid.data.database.AppDatabase
import com.pning80.caloriewatcherandroid.data.database.FoodEntryDao
import com.pning80.caloriewatcherandroid.data.database.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "calorie_watcher_db"
        ).build()
    }

    @Provides
    fun provideFoodEntryDao(appDatabase: AppDatabase): FoodEntryDao {
        return appDatabase.foodEntryDao()
    }

    @Provides
    fun provideUserProfileDao(appDatabase: AppDatabase): UserProfileDao {
        return appDatabase.userProfileDao()
    }
}

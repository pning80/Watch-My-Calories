package com.pning80.watchmycalories.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val height: Double,
    val weight: Double,
    val age: Int,
    val genderRaw: String,
    val activityLevelRaw: String,
    val targetCalories: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val calories: Double,
    val quantity: String,
    val timestamp: Long,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?,
    // iOS uses `imageID` (capital ID). Match exactly per PORTING_CRITERIA.md T1.2.
    val imageID: String?,
    val mealName: String?,
    val mealTypeRaw: String
)

@Entity(tableName = "menu_scans")
data class MenuScan(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val restaurantName: String?,
    val imageID: String?,
    val timestamp: Long,
    // iOS field is `itemsData` (typed as Data on iOS, JSON String on Android via
    // TypeConverter equivalence — same payload). PORTING_CRITERIA.md T1.2.
    val itemsData: String
)

data class MenuItemResult(
    val name: String,
    val description: String?,
    val calories: Double,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?
)

package com.pning80.watchmycalories

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pning80.watchmycalories.data.AppDatabase
import com.pning80.watchmycalories.data.FoodEntry
import com.pning80.watchmycalories.data.FoodEntryDao
import com.pning80.watchmycalories.data.UserProfile
import com.pning80.watchmycalories.data.UserProfileDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var foodDao: FoodEntryDao
    private lateinit var userDao: UserProfileDao

    @Before
    fun createDb() {
        // Spin up temporary RAM database decoupled from physical Disk
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        foodDao = db.foodEntryDao()
        userDao = db.userProfileDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testUserProfileInsertionAndRetrieval() = runBlocking {
        val profile = UserProfile(
            height = 180.0,
            weight = 75.0,
            age = 30,
            genderRaw = "Male",
            activityLevelRaw = "Sedentary",
            targetCalories = 2136.0
        )
        userDao.insertProfile(profile)

        val retrievedProfile = userDao.getUserProfile().first()
        assertNotNull(retrievedProfile)
        assertEquals(2136.0, retrievedProfile?.targetCalories)
        assertEquals(1, retrievedProfile?.id) // Primary Key is Hardcoded 1
    }

    @Test
    fun testFoodEntryTemporalSorting() = runBlocking {
        val entry1 = FoodEntry(
            id = UUID.randomUUID().toString(),
            name = "Lunch Entry",
            calories = 500.0,
            quantity = "1",
            timestamp = 1000000L, // Older
            protein = 20.0, carbs = 30.0, fat = 10.0,
            imageID = null, mealName = null, mealTypeRaw = "Lunch"
        )

        val entry2 = FoodEntry(
            id = UUID.randomUUID().toString(),
            name = "Dinner Entry",
            calories = 700.0,
            quantity = "1",
            timestamp = 2000000L, // Newer
            protein = 30.0, carbs = 40.0, fat = 15.0,
            imageID = null, mealName = null, mealTypeRaw = "Dinner"
        )

        // Insert out of order
        foodDao.insertEntry(entry1)
        foodDao.insertEntry(entry2)

        val list = foodDao.getAllEntries().first()
        assertEquals(2, list.size)
        // Verify temporal sorting: DESC means newer (entry2) is first
        assertEquals("Dinner Entry", list[0].name)
    }

    @Test
    fun testFoodEntryDeletion() = runBlocking {
        val entryId = UUID.randomUUID().toString()
        val entry = FoodEntry(
            id = entryId,
            name = "Snack",
            calories = 150.0,
            quantity = "1",
            timestamp = 1500000L,
            protein = 5.0, carbs = 20.0, fat = 5.0,
            imageID = null, mealName = null, mealTypeRaw = "Snack"
        )

        foodDao.insertEntry(entry)
        assertEquals(1, foodDao.getAllEntries().first().size)

        foodDao.deleteEntry(entryId)
        assertEquals(0, foodDao.getAllEntries().first().size)
    }
}

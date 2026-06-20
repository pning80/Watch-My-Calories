package com.pning80.watchmycalories

import android.content.Context
import android.content.Intent
import com.pning80.watchmycalories.ads.AdManager
import com.pning80.watchmycalories.ai.GeminiRepository
import com.pning80.watchmycalories.ai.MockGeminiRepository
import com.pning80.watchmycalories.data.AppDatabase
import com.pning80.watchmycalories.data.FoodEntry
import com.pning80.watchmycalories.data.MealType
import com.pning80.watchmycalories.data.MenuScan
import com.pning80.watchmycalories.data.UserProfile
import com.pning80.watchmycalories.ui.settings.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

/**
 * Test-mode seed helpers driven by intent extras. Mirrors the iOS
 * `--uitesting`/`--seed-data`/`--seed-multi-item-meal` launch arg system that
 * landed in PR #1.
 *
 * Used only when the launching Intent contains `EXTRA_UI_TESTING = true`. In
 * production launches the extra is absent and none of this code runs.
 *
 * Counterpart in iOS: `WatchMyCalories/WatchMyCalories/WatchMyCaloriesApp.swift`
 * (the `--uitesting` / `Self.shouldSeed*` block).
 */
object TestSeed {
    const val EXTRA_UI_TESTING = "wmc.test.uitesting"
    const val EXTRA_SEED_DATA = "wmc.test.seedData"
    const val EXTRA_SEED_HISTORY = "wmc.test.seedHistory"
    const val EXTRA_SEED_MULTI_ITEM_MEAL = "wmc.test.seedMultiItemMeal"
    const val EXTRA_SEED_MENU_SCANS = "wmc.test.seedMenuScans"
    const val EXTRA_AI_CONSENT = "wmc.test.aiConsent"                    // "accepted" / "declined" / "notAsked"
    const val EXTRA_MOCK_ESTIMATION_MODE = "wmc.test.mockEstimationMode" // "success" / "error" / "noFood"
    const val EXTRA_MOCK_MENU_ANALYSIS_MODE = "wmc.test.mockMenuAnalysisMode"
    const val EXTRA_RESET_ONBOARDING = "wmc.test.resetOnboarding"        // mirror of iOS --reset-onboarding
    const val EXTRA_START_AT_ANALYSIS = "wmc.test.startAtAnalysis"       // skip camera, jump to AnalysisScreen
    const val EXTRA_START_AT_MENU_ANALYSIS = "wmc.test.startAtMenuAnalysis" // skip camera, jump to MenuAnalysisScreen

    /**
     * True once a UI-testing launch has been applied via [applyIfTesting]. Lets
     * composables gate test-only behavior (e.g. skipping the Health Connect
     * permission request, which would launch a system Activity that backgrounds
     * MainActivity and detaches the Compose tree) without re-deriving the Activity
     * intent from a (possibly wrapped) Compose `LocalContext`. False in production.
     */
    @Volatile
    var uiTestingActive: Boolean = false
        private set

    fun isUiTesting(intent: Intent?): Boolean =
        intent?.getBooleanExtra(EXTRA_UI_TESTING, false) == true

    fun shouldStartAtAnalysis(intent: Intent?): Boolean =
        isUiTesting(intent) && intent?.getBooleanExtra(EXTRA_START_AT_ANALYSIS, false) == true

    fun shouldStartAtMenuAnalysis(intent: Intent?): Boolean =
        isUiTesting(intent) && intent?.getBooleanExtra(EXTRA_START_AT_MENU_ANALYSIS, false) == true

    /** 1x1 placeholder bitmap used as the input image for direct-to-analysis test launches. */
    fun stubBitmap(): android.graphics.Bitmap =
        android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)

    /**
     * Returns a `MockGeminiRepository` configured from the intent extras if the
     * launch is in test mode AND a mock-mode extra was set; otherwise returns
     * a real `GeminiRepository`. The activity calls this exactly once at
     * composition time (mirror of iOS `AppEnvironment.estimationService` /
     * `MockEstimationService.swap`).
     */
    fun geminiRepositoryFor(context: Context, intent: Intent?): GeminiRepository {
        if (!isUiTesting(intent)) return GeminiRepository(context)
        val estMode = intent?.getStringExtra(EXTRA_MOCK_ESTIMATION_MODE)
        val menuMode = intent?.getStringExtra(EXTRA_MOCK_MENU_ANALYSIS_MODE)
        if (estMode == null && menuMode == null) return GeminiRepository(context)
        return MockGeminiRepository(
            context = context,
            estimationMode = when (estMode) {
                "error" -> MockGeminiRepository.EstimationMode.ERROR
                "noFood" -> MockGeminiRepository.EstimationMode.NO_FOOD
                else -> MockGeminiRepository.EstimationMode.SUCCESS
            },
            menuMode = when (menuMode) {
                "error" -> MockGeminiRepository.MenuMode.ERROR
                "notAMenu" -> MockGeminiRepository.MenuMode.NOT_A_MENU
                else -> MockGeminiRepository.MenuMode.SUCCESS
            },
        )
    }

    /**
     * Apply intent-extra seed instructions BEFORE setContent runs, so the
     * first composition sees the seeded state. Synchronous on purpose — uses
     * `runBlocking` so the activity doesn't render an empty state then jump.
     */
    fun applyIfTesting(context: Context, intent: Intent?) {
        uiTestingActive = isUiTesting(intent)
        if (!isUiTesting(intent)) return

        // Suppress AdMob entry points in test mode — keeps the UMP consent
        // flow from firing during instrumented runs and prevents any future
        // fullscreen ad surface from tearing down the compose tree mid-test.
        // (The Android-only manual-entry-save interstitial that originally
        // motivated this flag was removed in PR K; the flag itself stays as
        // the canonical disable-ads-in-test hook.)
        AdManager.disableForUITesting = true

        val db = AppDatabase.getDatabase(context)
        val foodDao = db.foodEntryDao()
        val userDao = db.userProfileDao()
        val menuScanDao = db.menuScanDao()
        val settingsDataStore = SettingsDataStore(context)

        runBlocking(Dispatchers.IO) {
            // Wipe Room tables so each test starts clean (Room built-in).
            db.clearAllTables()

            // Mark onboarding complete so tests don't have to skip it —
            // UNLESS EXTRA_RESET_ONBOARDING is set, in which case explicitly
            // reset to false so onboarding shows on next launch.
            val resetOnboarding = intent?.getBooleanExtra(EXTRA_RESET_ONBOARDING, false) == true
            settingsDataStore.setOnboardingCompleted(!resetOnboarding)

            // AI consent — mirrors iOS `--ai-consent-accepted` (defaults to notAsked).
            when (intent?.getStringExtra(EXTRA_AI_CONSENT)) {
                "accepted" -> settingsDataStore.setAiConsent("accepted")
                "declined" -> settingsDataStore.setAiConsent("declined")
                "notAsked", null -> { /* leave at default */ }
            }

            if (intent?.getBooleanExtra(EXTRA_SEED_DATA, false) == true) {
                seedBasicData(foodDao, userDao)
            }
            if (intent?.getBooleanExtra(EXTRA_SEED_HISTORY, false) == true) {
                seedHistoryData(foodDao, userDao)
            }
            if (intent?.getBooleanExtra(EXTRA_SEED_MULTI_ITEM_MEAL, false) == true) {
                seedMultiItemMeal(foodDao, userDao)
            }
            if (intent?.getBooleanExtra(EXTRA_SEED_MENU_SCANS, false) == true) {
                seedMenuScans(menuScanDao)
            }
        }
    }

    private suspend fun seedBasicData(
        foodDao: com.pning80.watchmycalories.data.FoodEntryDao,
        userDao: com.pning80.watchmycalories.data.UserProfileDao,
    ) = withContext(Dispatchers.IO) {
        val profile = UserProfile(
            id = 1, height = 175.0, weight = 70.0, age = 30,
            genderRaw = "Male", activityLevelRaw = "Moderately Active",
            targetCalories = 2200.0,
        )
        userDao.insertProfile(profile)
        val cal = Calendar.getInstance()
        val breakfast = cal.apply { set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 0) }.timeInMillis
        val lunch = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 30) }.timeInMillis
        foodDao.insertEntry(FoodEntry(
            id = UUID.randomUUID().toString(),
            name = "Oatmeal with Berries",
            calories = 300.0, quantity = "1 bowl",
            timestamp = breakfast,
            protein = 10.0, carbs = 50.0, fat = 6.0,
            imageID = null, mealName = null,
            mealTypeRaw = MealType.BREAKFAST.displayName,
        ))
        foodDao.insertEntry(FoodEntry(
            id = UUID.randomUUID().toString(),
            name = "Chicken Salad",
            calories = 450.0, quantity = "1 plate",
            timestamp = lunch,
            protein = 35.0, carbs = 20.0, fat = 18.0,
            imageID = null, mealName = null,
            mealTypeRaw = MealType.LUNCH.displayName,
        ))
    }

    private suspend fun seedHistoryData(
        foodDao: com.pning80.watchmycalories.data.FoodEntryDao,
        userDao: com.pning80.watchmycalories.data.UserProfileDao,
    ) = withContext(Dispatchers.IO) {
        seedBasicData(foodDao, userDao)
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 0)
        }.timeInMillis
        foodDao.insertEntry(FoodEntry(
            id = UUID.randomUUID().toString(),
            name = "Pasta Bolognese",
            calories = 600.0, quantity = "1 plate",
            timestamp = yesterday,
            protein = 25.0, carbs = 70.0, fat = 20.0,
            imageID = null, mealName = null,
            mealTypeRaw = MealType.DINNER.displayName,
        ))
        // Second yesterday entry — iOS seeds a Latte here too (WatchMyCaloriesApp.swift:185),
        // making yesterday total 750 / P33·C82·F26. Android was missing it, so the
        // History day-card paired diff diverged (600 vs 750) on every run.
        val yesterdayMorning = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 30)
        }.timeInMillis
        foodDao.insertEntry(FoodEntry(
            id = UUID.randomUUID().toString(),
            name = "Latte",
            calories = 150.0, quantity = "1 cup",
            timestamp = yesterdayMorning,
            protein = 8.0, carbs = 12.0, fat = 6.0,
            imageID = null, mealName = null,
            mealTypeRaw = MealType.BREAKFAST.displayName,
        ))
        val twoDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -2); set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0)
        }.timeInMillis
        foodDao.insertEntry(FoodEntry(
            id = UUID.randomUUID().toString(),
            name = "Turkey Sandwich",
            calories = 400.0, quantity = "1 sandwich",
            timestamp = twoDaysAgo,
            protein = 30.0, carbs = 35.0, fat = 12.0,
            imageID = null, mealName = null,
            mealTypeRaw = MealType.LUNCH.displayName,
        ))
    }

    private suspend fun seedMultiItemMeal(
        foodDao: com.pning80.watchmycalories.data.FoodEntryDao,
        userDao: com.pning80.watchmycalories.data.UserProfileDao,
    ) = withContext(Dispatchers.IO) {
        val profile = UserProfile(
            id = 1, height = 175.0, weight = 70.0, age = 30,
            genderRaw = "Male", activityLevelRaw = "Moderately Active",
            targetCalories = 2200.0,
        )
        userDao.insertProfile(profile)
        val lunchTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 30)
        }.timeInMillis
        val mealName = "Mock Bento Box"
        data class SeedItem(val name: String, val cals: Double, val qty: String, val p: Double, val c: Double, val f: Double)
        listOf(
            SeedItem("Brown Rice", 220.0, "1 cup", 5.0, 45.0, 2.0),
            SeedItem("Teriyaki Chicken", 350.0, "5 oz", 30.0, 12.0, 18.0),
            SeedItem("Edamame", 120.0, "1 cup", 11.0, 9.0, 5.0),
        ).forEach { item ->
            foodDao.insertEntry(FoodEntry(
                id = UUID.randomUUID().toString(),
                name = item.name,
                calories = item.cals, quantity = item.qty,
                timestamp = lunchTime,
                protein = item.p, carbs = item.c, fat = item.f,
                imageID = null,
                mealName = mealName,
                mealTypeRaw = MealType.LUNCH.displayName,
            ))
        }
    }

    private suspend fun seedMenuScans(
        menuScanDao: com.pning80.watchmycalories.data.MenuScanDao,
    ) = withContext(Dispatchers.IO) {
        menuScanDao.insertScan(MenuScan(
            id = UUID.randomUUID().toString(),
            restaurantName = "Mock Italian Place",
            imageID = null,
            timestamp = System.currentTimeMillis(),
            itemsData = """[{"name":"Margherita Pizza","description":"Classic tomato + mozzarella","calories":800,"protein":30,"carbs":90,"fat":30},{"name":"Caesar Salad","description":null,"calories":350,"protein":15,"carbs":12,"fat":25}]""",
        ))
        menuScanDao.insertScan(MenuScan(
            id = UUID.randomUUID().toString(),
            restaurantName = "Mock Sushi Bar",
            imageID = null,
            timestamp = System.currentTimeMillis() - 86_400_000L,
            itemsData = """[{"name":"Salmon Roll","description":null,"calories":320,"protein":18,"carbs":38,"fat":10},{"name":"Tuna Sashimi","description":"6 pieces","calories":180,"protein":28,"carbs":0,"fat":6}]""",
        ))
    }
}

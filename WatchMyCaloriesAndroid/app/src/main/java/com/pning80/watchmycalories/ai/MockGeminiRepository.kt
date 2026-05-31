package com.pning80.watchmycalories.ai

import android.content.Context
import android.graphics.Bitmap
import com.pning80.watchmycalories.data.MenuItemResult
import kotlinx.coroutines.delay

/**
 * Test-only stand-in for [GeminiRepository]. Wired via `TestSeed` when the
 * launching Intent carries `EXTRA_MOCK_ESTIMATION_MODE` or
 * `EXTRA_MOCK_MENU_ANALYSIS_MODE`. Mirrors iOS `MockEstimationService` and
 * `MockMenuAnalysisService` (see `Services.swift` on the iOS side).
 *
 * Each call sleeps briefly so UI tests can observe the loading state.
 */
class MockGeminiRepository(
    context: Context,
    var estimationMode: EstimationMode = EstimationMode.SUCCESS,
    var menuMode: MenuMode = MenuMode.SUCCESS,
) : GeminiRepository(context) {

    enum class EstimationMode { SUCCESS, ERROR, NO_FOOD }
    enum class MenuMode { SUCCESS, ERROR, NOT_A_MENU }

    override suspend fun estimateCalories(
        images: List<Bitmap>,
        isMetric: Boolean,
    ): Result<EstimationResult> {
        delay(150)  // observable loading state
        return when (estimationMode) {
            EstimationMode.SUCCESS -> Result.success(
                EstimationResult(
                    mealName = "Mock Bento Box",
                    items = listOf(
                        EstimationItem(
                            name = "Brown Rice", quantity = "1 cup",
                            calories = 220.0, confidence = 0.92,
                            protein = 5.0, carbs = 45.0, fat = 2.0,
                        ),
                        EstimationItem(
                            name = "Teriyaki Chicken", quantity = "5 oz",
                            calories = 350.0, confidence = 0.88,
                            protein = 30.0, carbs = 12.0, fat = 18.0,
                        ),
                        EstimationItem(
                            name = "Edamame", quantity = "1 cup",
                            calories = 120.0, confidence = 0.90,
                            protein = 11.0, carbs = 9.0, fat = 5.0,
                        ),
                    ),
                )
            )
            EstimationMode.ERROR -> Result.failure(
                Exception("Mock estimation error (EXTRA_MOCK_ESTIMATION_MODE=error)")
            )
            EstimationMode.NO_FOOD -> Result.success(
                EstimationResult(mealName = null, items = emptyList())
            )
        }
    }

    override suspend fun analyzeMenu(
        image: Bitmap,
        locality: String?,
        coordinates: String?,
        isMetric: Boolean,
    ): Result<MenuAnalysisResult> {
        delay(150)
        return when (menuMode) {
            MenuMode.SUCCESS -> Result.success(
                MenuAnalysisResult(
                    error = null,
                    restaurantName = "Mock Italian Place",
                    items = listOf(
                        MenuItemResult(
                            name = "Margherita Pizza",
                            description = "Classic tomato + mozzarella",
                            calories = 800.0, protein = 30.0, carbs = 90.0, fat = 30.0,
                        ),
                        MenuItemResult(
                            name = "Caesar Salad",
                            description = "Romaine + parmesan + dressing",
                            calories = 350.0, protein = 8.0, carbs = 12.0, fat = 28.0,
                        ),
                    ),
                )
            )
            MenuMode.ERROR -> Result.failure(
                Exception("Mock menu analysis error (EXTRA_MOCK_MENU_ANALYSIS_MODE=error)")
            )
            MenuMode.NOT_A_MENU -> Result.failure(Exception("not_a_menu"))
        }
    }
}

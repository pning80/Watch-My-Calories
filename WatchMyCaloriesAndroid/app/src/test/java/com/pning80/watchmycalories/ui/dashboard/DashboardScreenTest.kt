package com.pning80.watchmycalories.ui.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.pning80.watchmycalories.BaseComposeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h4000dp")
class DashboardScreenTest : BaseComposeTest() {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testEmptyStateShowsWhenNoEntries() {
        composeTestRule.setContent {
            DashboardScreen(
                entries = emptyList(),
                targetCalories = 2200.0,
                burnedCalories = 0.0,
                onLogFood = {},
                onNavigateToSettings = {}
            )
        }

        composeTestRule.onNodeWithText("No meals tracked yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap to scan your first meal.").assertIsDisplayed()
    }

    @Test
    fun testHeroCardShowsCalculatedCalories() {
        composeTestRule.setContent {
            DashboardScreen(
                entries = getSampleEntries(), // 300 + 450 = 750
                targetCalories = 2200.0,
                burnedCalories = 456.0,
                onLogFood = {},
                onNavigateToSettings = {}
            )
        }

        // Test Consumed (750)
        composeTestRule.onNodeWithText("750").assertIsDisplayed()

        // Test Target (2200)
        composeTestRule.onNodeWithText("2200").assertIsDisplayed()

        // Test Remaining (2200 + 456 - 750 = 1906)
        composeTestRule.onNodeWithText("1906").assertIsDisplayed()
    }

    @Test
    fun testMealSectionsAppearWithSeedData() {
        composeTestRule.setContent {
            DashboardScreen(
                entries = getSampleEntries(),
                targetCalories = 2200.0,
                burnedCalories = 0.0,
                onLogFood = {},
                onNavigateToSettings = {}
            )
        }

        // Verify empty state is hidden
        composeTestRule.onNodeWithText("No meals tracked yet").assertDoesNotExist()

        // In LazyColumn, items may not be in the visible viewport.
        // Scroll the list to the node to force composition and visibility.
        composeTestRule.onNodeWithTag("DashboardLazyColumn")
            .performScrollToNode(hasText("Oatmeal with Berries"))
        composeTestRule.onNodeWithText("Oatmeal with Berries").assertIsDisplayed()

        composeTestRule.onNodeWithTag("DashboardLazyColumn")
            .performScrollToNode(hasText("Chicken Salad"))
        composeTestRule.onNodeWithText("Chicken Salad").assertIsDisplayed()

        // Verify meals
        // Note: groupedMeals logic dynamically assigns Breakfast, Lunch based on logic,
        // but since we mocked timestamp to CURRENT, they might fall under whatever time it is now.
        // We just assert the items are there to mirror the SwiftUI `testSeedDataShowsFoodEntryNames`.
    }
}

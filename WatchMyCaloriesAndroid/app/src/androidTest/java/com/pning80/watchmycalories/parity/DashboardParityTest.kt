package com.pning80.watchmycalories.parity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import org.junit.Test

/**
 * Parity mirror of iOS `WatchMyCaloriesUITests/DashboardTests.swift`.
 *
 * Validates the Phase 3 test infrastructure (intent-driven TestSeed +
 * MainActivityComposeTest base class) by exercising a few high-confidence
 * dashboard checks. Once green on Pixel 9a, this is the template for
 * the rest of the Android parity mirror suite (Phase 5).
 */
class DashboardParityTest : MainActivityComposeTest() {

    /** Mirror of iOS `testEmptyStateShowsWhenNoEntries`. */
    @Test
    fun testEmptyStateShowsWhenNoEntries() {
        launchEmpty()
        composeTestRule.onNodeWithText("No meals tracked yet").assertIsDisplayed()
    }

    /** Mirror of iOS `testHeroCardShowsConsumedCalories` — 300 + 450 = 750 from seed data. */
    @Test
    fun testHeroCardShowsConsumedCalories() {
        launchWithSeedData()
        composeTestRule.onNodeWithText("750").assertIsDisplayed()
    }

    /** Mirror of iOS `testSeedDataShowsFoodEntryNames`. */
    @Test
    fun testSeedDataShowsFoodEntryNames() {
        launchWithSeedData()
        composeTestRule.onNodeWithText("Oatmeal with Berries").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chicken Salad").assertIsDisplayed()
    }

    /** Mirror of iOS `testHeroCardShowsTargetCalories` — seed profile has 2200 kcal goal. */
    @Test
    fun testHeroCardShowsTargetCalories() {
        launchWithSeedData()
        composeTestRule.onNodeWithText("2200").assertIsDisplayed()
    }
}

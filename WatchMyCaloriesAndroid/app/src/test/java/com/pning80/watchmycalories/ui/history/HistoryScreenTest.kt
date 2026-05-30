package com.pning80.watchmycalories.ui.history

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.pning80.watchmycalories.BaseComposeTest
import com.pning80.watchmycalories.utils.AccessibilityTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoryScreenTest : BaseComposeTest() {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testEmptyStateShowsWhenNoHistory() {
        composeTestRule.setContent {
            HistoryScreen(
                entries = emptyList(),
                onLogFood = {}
            )
        }

        // Title is hosted by MainActivity's TopAppBar (not the standalone HistoryScreen),
        // so we only verify the empty-state body here.
        composeTestRule.onNodeWithTag(AccessibilityTags.History.EMPTY_STATE).assertIsDisplayed()
        composeTestRule.onNodeWithText("No history yet").assertIsDisplayed()
    }

    @Test
    fun testHistoryCardsExpandOnClick() {
        composeTestRule.setContent {
            HistoryScreen(
                entries = getSampleEntries(),
                onLogFood = {}
            )
        }

        // Initially, the sum of both logged items should appear in the collapsed tile
        // 300 + 450 = 750. Scroll to it first.
        composeTestRule.onNodeWithTag("HistoryLazyColumn")
            .performScrollToNode(hasTestTag("HistoryDayCard_750"))
            
        composeTestRule.onNodeWithTag("HistoryDayCard_750").assertIsDisplayed()
        composeTestRule.onNodeWithText("750").assertIsDisplayed()

        // But the internal rows inside the collapsed AnimatedVisibility should NOT exist or be visible yet
        composeTestRule.onNodeWithText("Oatmeal with Berries").assertDoesNotExist()

        // Perform click on the card
        composeTestRule.onNodeWithTag("HistoryDayCard_750").performClick()

        // Now the details should smoothly animate in
        composeTestRule.onNodeWithText("Oatmeal with Berries").assertExists()
        composeTestRule.onNodeWithText("Chicken Salad").assertExists()
    }

    @Test
    fun testHistoryEmptyStateAccessibilityID() {
        composeTestRule.setContent {
            HistoryScreen(
                entries = emptyList(),
                onLogFood = {}
            )
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.History.EMPTY_STATE).assertIsDisplayed()
    }

    @Test
    fun testHistoryDayCardAccessibilityID() {
        composeTestRule.setContent {
            HistoryScreen(
                entries = getSampleEntries(),
                onLogFood = {}
            )
        }

        // The history_dayCard tag is on an inner Row inside HistoryDayCard;
        // since the tag is on a merged-away child, we need useUnmergedTree
        composeTestRule.onNodeWithTag(AccessibilityTags.History.DAY_CARD, useUnmergedTree = true).assertExists()
    }

    @Test
    fun testDayCardShowsMacroRow() {
        composeTestRule.setContent {
            HistoryScreen(
                entries = getSampleEntries(), // has P/C/F
                onLogFood = {}
            )
        }

        // The macro row tag is on a nested Row inside HistoryDayCard.
        // Since the tag is inside a clickable parent, use useUnmergedTree
        composeTestRule.onNodeWithTag(AccessibilityTags.History.DAY_CARD_MACROS, useUnmergedTree = true).assertExists()
    }

    @Test
    fun testDayCardMacroRowHiddenWhenNoMacros() {
        // Entries without macros
        val noMacroEntries = listOf(
            com.pning80.watchmycalories.data.FoodEntry(
                id = "1",
                name = "Coffee",
                calories = 5.0,
                quantity = "1 cup",
                protein = null,
                carbs = null,
                fat = null,
                timestamp = System.currentTimeMillis(),
                imageID = null,
                mealName = null,
                mealTypeRaw = "Breakfast"
            )
        )

        composeTestRule.setContent {
            HistoryScreen(
                entries = noMacroEntries,
                onLogFood = {}
            )
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.History.DAY_CARD_MACROS).assertDoesNotExist()
    }

    @Test
    fun testExpandedDayCardShowsMealSections() {
        composeTestRule.setContent {
            HistoryScreen(
                entries = getSampleEntries(),
                onLogFood = {}
            )
        }

        // Expand the day card
        composeTestRule.onNodeWithTag("HistoryLazyColumn")
            .performScrollToNode(hasTestTag("HistoryDayCard_750"))
        composeTestRule.onNodeWithTag("HistoryDayCard_750").performClick()

        // Seed data has Breakfast and Lunch entries - displayed as uppercase
        composeTestRule.onNodeWithText("BREAKFAST").assertExists()
        composeTestRule.onNodeWithText("LUNCH").assertExists()
    }

    @Test
    fun testDayCardShowsCalorieTotal() {
        composeTestRule.setContent {
            HistoryScreen(
                entries = getSampleEntries(), // 300 + 450 = 750
                onLogFood = {}
            )
        }

        composeTestRule.onNodeWithTag("HistoryLazyColumn")
            .performScrollToNode(hasTestTag("HistoryDayCard_750"))

        composeTestRule.onNodeWithText("750").assertIsDisplayed()
    }

    @Test
    fun testHistoryShowsMultipleDays() {
        // Create entries across multiple days
        val now = System.currentTimeMillis()
        val yesterday = now - 24 * 60 * 60 * 1000
        val twoDaysAgo = now - 2 * 24 * 60 * 60 * 1000

        val multiDayEntries = listOf(
            com.pning80.watchmycalories.data.FoodEntry(
                id = "1", name = "Oatmeal", calories = 300.0, quantity = "1 bowl",
                protein = 10.0, carbs = 50.0, fat = 6.0, timestamp = now,
                imageID = null, mealName = null, mealTypeRaw = "Breakfast"
            ),
            com.pning80.watchmycalories.data.FoodEntry(
                id = "2", name = "Chicken Salad", calories = 450.0, quantity = "1 plate",
                protein = 35.0, carbs = 20.0, fat = 18.0, timestamp = now,
                imageID = null, mealName = null, mealTypeRaw = "Lunch"
            ),
            com.pning80.watchmycalories.data.FoodEntry(
                id = "3", name = "Grilled Salmon", calories = 600.0, quantity = "1 fillet",
                protein = 40.0, carbs = 5.0, fat = 30.0, timestamp = yesterday,
                imageID = null, mealName = null, mealTypeRaw = "Dinner"
            ),
            com.pning80.watchmycalories.data.FoodEntry(
                id = "4", name = "Turkey Sandwich", calories = 400.0, quantity = "1 whole",
                protein = 25.0, carbs = 40.0, fat = 12.0, timestamp = twoDaysAgo,
                imageID = null, mealName = null, mealTypeRaw = "Lunch"
            )
        )

        composeTestRule.setContent {
            HistoryScreen(
                entries = multiDayEntries,
                onLogFood = {}
            )
        }

        // Today's total: 300 + 450 = 750
        composeTestRule.onNodeWithTag("HistoryLazyColumn")
            .performScrollToNode(hasTestTag("HistoryDayCard_750"))
        composeTestRule.onNodeWithText("750").assertIsDisplayed()

        // Yesterday's total: 600
        composeTestRule.onNodeWithTag("HistoryLazyColumn")
            .performScrollToNode(hasTestTag("HistoryDayCard_600"))
        composeTestRule.onNodeWithText("600").assertIsDisplayed()

        // Two days ago total: 400
        composeTestRule.onNodeWithTag("HistoryLazyColumn")
            .performScrollToNode(hasTestTag("HistoryDayCard_400"))
        composeTestRule.onNodeWithText("400").assertIsDisplayed()
    }
}

package com.pning80.watchmycalories.ui.history

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.pning80.watchmycalories.BaseComposeTest
import org.junit.Rule
import org.junit.Test

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

        composeTestRule.onNodeWithTag("HistoryTitle").assertIsDisplayed()
        composeTestRule.onNodeWithText("No meals tracked yet").assertIsDisplayed()
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
}

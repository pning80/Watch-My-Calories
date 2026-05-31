package com.pning80.watchmycalories.parity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pning80.watchmycalories.utils.AccessibilityTags
import org.junit.Test

/**
 * Parity mirror of iOS `WatchMyCaloriesUITests/HistoryTests.swift`.
 *
 * Covers the day-card list (empty / single-day / multi-day), expand-on-tap,
 * macro row visibility, and meal-section rendering inside expanded day cards.
 *
 * Skipped — parity GAPs (recorded in PORTING_DEVIATIONS.md / .INCONSISTENCIES.md):
 *   - testDeleteEntryFromHistory — Android history has no long-press context menu
 *   - testHistoryMultiItemMealGroup* — D-005 (Android groups by imageID, not mealName)
 *   - testHistoryThumbnailTapOpensFullScreenImage — needs launchWithImage TestHook
 *
 * Known platform-rendering divergence (asserted on the Android side):
 *   - Empty-state copy: Android shows "No history yet", iOS shows "No meals tracked yet".
 *     Tests assert the EMPTY_STATE testTag, which is platform-agnostic.
 *   - Macro row format: iOS uses "P: 45g", Android uses chips with separate "P" + "45g"
 *     labels. Tests assert macro-row testTag visibility, not specific text format.
 *   - Meal-section headers: UPPERCASE on Android (D-006).
 */
class HistoryParityTest : MainActivityComposeTest() {

    private fun goToHistory() {
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.HISTORY).performClick()
        composeTestRule.waitForIdle()
    }

    // MARK: - Empty State

    /** Mirror of iOS `testHistoryEmptyState`. */
    @Test
    fun testHistoryEmptyState() {
        launchEmpty()
        goToHistory()
        composeTestRule.onNodeWithTag(AccessibilityTags.History.EMPTY_STATE).assertIsDisplayed()
    }

    /** Mirror of iOS `testHistoryEmptyStateAccessibilityID`. */
    @Test
    fun testHistoryEmptyStateAccessibilityID() {
        launchEmpty()
        goToHistory()
        composeTestRule.onNodeWithTag(AccessibilityTags.History.EMPTY_STATE).assertIsDisplayed()
    }

    /** Mirror of iOS `testHistoryTitleAccessibilityID`. */
    @Test
    fun testHistoryTitleAccessibilityID() {
        launchWithSeedData()
        goToHistory()
        composeTestRule.onNodeWithTag("HistoryTitle").assertIsDisplayed()
    }

    // MARK: - Seed Data Day Card

    /** Mirror of iOS `testHistoryShowsDayCardWithSeedData` — 300 + 450 = 750. */
    @Test
    fun testHistoryShowsDayCardWithSeedData() {
        launchWithSeedData()
        goToHistory()
        composeTestRule.onNodeWithText("750").assertIsDisplayed()
    }

    /** Mirror of iOS `testHistoryDayCardAccessibilityID`. */
    @Test
    fun testHistoryDayCardAccessibilityID() {
        launchWithSeedData()
        goToHistory()
        // The DAY_CARD testTag is on a child Row inside a clickable Column, so its
        // semantics get merged into the parent's. Query the unmerged tree to find
        // the tag where it was originally applied.
        assert(
            composeTestRule
                .onAllNodesWithTag(AccessibilityTags.History.DAY_CARD, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        ) { "History day card testTag should be present after seed" }
    }

    /** Mirror of iOS `testExpandDayCardShowsEntries`. */
    @Test
    fun testExpandDayCardShowsEntries() {
        launchWithSeedData()
        goToHistory()
        // Tap the day-card calorie total to expand
        composeTestRule.onNodeWithText("750").performClick()
        composeTestRule.waitForIdle()
        // Seeded entries should appear inside the expanded card
        composeTestRule.onNodeWithText("Oatmeal with Berries").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chicken Salad").assertIsDisplayed()
    }

    /** Mirror of iOS `testExpandedDayCardShowsMealSections`. */
    @Test
    fun testExpandedDayCardShowsMealSections() {
        launchWithSeedData()
        goToHistory()
        composeTestRule.onNodeWithText("750").performClick()
        composeTestRule.waitForIdle()
        // Android renders meal headers UPPERCASE (D-006).
        composeTestRule.onNodeWithText("BREAKFAST").assertIsDisplayed()
        composeTestRule.onNodeWithText("LUNCH").assertIsDisplayed()
    }

    /** Mirror of iOS `testExpandedMealCardShowsProportionalBarNotGramLabels` (D-007 closed). */
    @Test
    fun testExpandedMealCardShowsProportionalBarNotGramLabels() {
        launchWithSeedData()
        goToHistory()
        composeTestRule.onNodeWithText("750").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Oatmeal with Berries").assertIsDisplayed()
        // After D-007 fix the per-entry row uses MacroProportionalBar — no literal
        // "P: 10g" gram-label text anywhere on the expanded meal entry.
        assert(composeTestRule.onAllNodesWithText("P: 10g").fetchSemanticsNodes().isEmpty()) {
            "Expanded meal entry should show proportional bar, not literal gram labels (D-007)"
        }
    }

    // MARK: - Macro Row

    /** Mirror of iOS `testDayCardShowsMacroRow`. */
    @Test
    fun testDayCardShowsMacroRow() {
        launchWithSeedData()
        goToHistory()
        // Same unmerged-tree caveat as testHistoryDayCardAccessibilityID.
        assert(
            composeTestRule
                .onAllNodesWithTag(AccessibilityTags.History.DAY_CARD_MACROS, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        ) { "Day card macro row should appear when seeded entries have macros" }
    }

    /** Mirror of iOS `testDayCardMacroRowHiddenWhenNoMacros`. */
    @Test
    fun testDayCardMacroRowHiddenWhenNoMacros() {
        launchEmpty()
        goToHistory()
        // No entries at all → no macro row anywhere (unmerged tree still empty).
        assert(
            composeTestRule
                .onAllNodesWithTag(AccessibilityTags.History.DAY_CARD_MACROS, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        ) { "Macro row should be hidden when there are no entries" }
    }

    // MARK: - Multi-Day History

    /** Mirror of iOS `testHistoryShowsMultipleDays`. */
    @Test
    fun testHistoryShowsMultipleDays() {
        launchWithHistoryData()
        goToHistory()
        composeTestRule.onNodeWithTag("HistoryTitle").assertIsDisplayed()
        // Today: 750 kcal
        composeTestRule.onNodeWithText("750").assertIsDisplayed()
        // 2-days-ago: 400 kcal (unique value confirms multi-day rendering)
        composeTestRule.onNodeWithText("400").performScrollTo().assertIsDisplayed()
    }

    /** Mirror of iOS `testHistoryDayCardShowsYesterdayEntries`. */
    @Test
    fun testHistoryDayCardShowsYesterdayEntries() {
        launchWithHistoryData()
        goToHistory()
        composeTestRule.onNodeWithText("400").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Turkey Sandwich").assertIsDisplayed()
    }
}

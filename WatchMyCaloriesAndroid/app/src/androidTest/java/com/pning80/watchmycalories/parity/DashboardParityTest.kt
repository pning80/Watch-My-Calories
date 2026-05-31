package com.pning80.watchmycalories.parity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pning80.watchmycalories.utils.AccessibilityTags
import org.junit.Test

/**
 * Parity mirror of iOS `WatchMyCaloriesUITests/DashboardTests.swift`.
 *
 * Expanded in Phase 5b to cover the easy mirror surface (element existence,
 * seed content, meal section visibility, hero card, multi-item meal group,
 * log-food sheet open).
 *
 * Skipped — no Android counterpart yet (parity GAPs, recorded in
 * PARITY_INCONSISTENCIES.md, not mirror gaps):
 *   - testHeroCardRemainingCalories (needs HealthConnect burned-cal seed)
 *   - testThumbnailTapOpensFullScreenImage (needs launchWithImage TestHook)
 *   - testDeleteEntryFromDashboard, testEdit*, testView* — Android dashboard has
 *     no long-press → context-menu UI
 *   - testMultiItemMeal* — Android does NOT render a group card for multi-item
 *     meals; the entries appear as individual cards under the meal section.
 *     iOS groups them under a single "Mock Bento Box" card with expand.
 *
 * Known platform-rendering divergence (not a parity gap, just a visual choice):
 *   - Meal section headers are UPPERCASE on Android ("BREAKFAST") vs title case
 *     on iOS ("Breakfast"). Tests assert on the Android rendering.
 */
class DashboardParityTest : MainActivityComposeTest() {

    // MARK: - Empty State

    /** Mirror of iOS `testEmptyStateShowsWhenNoEntries`. */
    @Test
    fun testEmptyStateShowsWhenNoEntries() {
        launchEmpty()
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.EMPTY_STATE_CARD).assertIsDisplayed()
    }

    /** Mirror of iOS `testEmptyStateManualEntryLinkExists`. */
    @Test
    fun testEmptyStateManualEntryLinkExists() {
        launchEmpty()
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.MANUAL_ENTRY_LINK).assertIsDisplayed()
    }

    /** Mirror of iOS `testEmptyStateManualEntryLinkOpensSheet`. */
    @Test
    fun testEmptyStateManualEntryLinkOpensSheet() {
        launchEmpty()
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.MANUAL_ENTRY_LINK).performClick()
        composeTestRule.waitForIdle()
        // Log Food sheet opens — its "Log Manually" option is a stable text marker.
        composeTestRule.onNodeWithText("Log Manually").assertIsDisplayed()
    }

    // MARK: - Hero Card / Stat Elements

    /** Mirror of iOS `testHeroCardElementExists`. */
    @Test
    fun testHeroCardElementExists() {
        launchWithSeedData()
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.HERO_CARD).assertIsDisplayed()
    }

    /** Mirror of iOS `testGoalValueElementExists`. */
    @Test
    fun testGoalValueElementExists() {
        launchWithSeedData()
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.GOAL_VALUE).assertIsDisplayed()
    }

    /** Mirror of iOS `testRemainingValueElementExists`. */
    @Test
    fun testRemainingValueElementExists() {
        launchWithSeedData()
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.REMAINING_VALUE).assertIsDisplayed()
    }

    /** Mirror of iOS `testConsumedCaloriesElementExists`. */
    @Test
    fun testConsumedCaloriesElementExists() {
        launchWithSeedData()
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.CONSUMED_CALORIES).assertIsDisplayed()
    }

    /** Mirror of iOS `testHeroCardShowsConsumedCalories` — 300 + 450 = 750 from seed data. */
    @Test
    fun testHeroCardShowsConsumedCalories() {
        launchWithSeedData()
        composeTestRule.onNodeWithText("750").assertIsDisplayed()
    }

    /** Mirror of iOS `testHeroCardShowsTargetCalories` — seed profile has 2200 kcal goal. */
    @Test
    fun testHeroCardShowsTargetCalories() {
        launchWithSeedData()
        composeTestRule.onNodeWithText("2200").assertIsDisplayed()
    }

    // MARK: - Meal Sections / Seeded Content

    /** Mirror of iOS `testSeedDataShowsFoodEntryNames`. */
    @Test
    fun testSeedDataShowsFoodEntryNames() {
        launchWithSeedData()
        composeTestRule.onNodeWithText("Oatmeal with Berries").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chicken Salad").assertIsDisplayed()
    }

    /** Mirror of iOS `testMealSectionsAppearWithSeedData`. */
    @Test
    fun testMealSectionsAppearWithSeedData() {
        launchWithSeedData()
        // With seed data, empty state should NOT appear
        composeTestRule.onAllNodesWithText("No meals tracked yet").fetchSemanticsNodes().let {
            assert(it.isEmpty()) { "Empty state should not appear when seed entries exist" }
        }
        // Breakfast and Lunch sections should be visible (seed has entries in both).
        // Android renders headers UPPERCASE; iOS uses title case (visual divergence).
        composeTestRule.onNodeWithText("BREAKFAST").assertIsDisplayed()
        composeTestRule.onNodeWithText("LUNCH").assertIsDisplayed()
    }

    /** Mirror of iOS `testOnlyRelevantMealSectionsAppear`. */
    @Test
    fun testOnlyRelevantMealSectionsAppear() {
        launchWithSeedData()
        composeTestRule.onNodeWithText("BREAKFAST").assertIsDisplayed()
        composeTestRule.onNodeWithText("LUNCH").assertIsDisplayed()
        // Dinner and Snack should NOT appear (no seeded entries for them).
        assert(composeTestRule.onAllNodesWithText("DINNER").fetchSemanticsNodes().isEmpty()) {
            "Dinner section should not appear without seeded Dinner entries"
        }
        assert(composeTestRule.onAllNodesWithText("SNACK").fetchSemanticsNodes().isEmpty()) {
            "Snack section should not appear without seeded Snack entries"
        }
    }

    // MARK: - Log Food Tab

    /** Mirror of iOS `testLogFoodTabOpensSheet`. */
    @Test
    fun testLogFoodTabOpensSheet() {
        launchEmpty()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.CAMERA).performClick()
        composeTestRule.waitForIdle()
        // Log Food sheet exposes three options
        composeTestRule.onNodeWithText("Scan Food").assertIsDisplayed()
        composeTestRule.onNodeWithText("Choose from Library").assertIsDisplayed()
        composeTestRule.onNodeWithText("Log Manually").assertIsDisplayed()
    }

    // MARK: - Multi-Item Meal Group (D-005 closed — strict iOS mirrors)

    /** Mirror of iOS `testMultiItemMealGroupCardShowsMealName`. */
    @Test
    fun testMultiItemMealGroupCardShowsMealName() {
        launchWithMultiItemMeal()
        composeTestRule.onNodeWithText("Mock Bento Box").assertIsDisplayed()
    }

    /** Mirror of iOS `testMultiItemMealGroupSummaryRowExpandsToShowItems`. */
    @Test
    fun testMultiItemMealGroupSummaryRowExpandsToShowItems() {
        launchWithMultiItemMeal()
        composeTestRule.onNodeWithText("Mock Bento Box").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mock Bento Box").performClick()
        composeTestRule.waitForIdle()
        // Three seeded sub-items now visible inside the expanded group card.
        composeTestRule.onNodeWithText("Brown Rice", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Teriyaki Chicken", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Edamame", substring = true).assertIsDisplayed()
    }
}

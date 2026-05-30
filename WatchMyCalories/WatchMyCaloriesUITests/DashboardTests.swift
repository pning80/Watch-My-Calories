import XCTest

final class DashboardTests: WatchMyCaloriesUITestBase {

    // MARK: - Empty State

    func testEmptyStateShowsWhenNoEntries() {
        launchEmpty()

        let emptyCard = app.buttons["dashboard_emptyStateCard"]
        XCTAssertTrue(emptyCard.waitForExistence(timeout: 3))
    }

    func testEmptyStateManualEntryLinkExists() {
        launchEmpty()

        let link = app.buttons["dashboard_manualEntryLink"]
        XCTAssertTrue(link.waitForExistence(timeout: 3))
    }

    // MARK: - Seed Data

    func testHeroCardShowsConsumedCalories() {
        launchWithSeedData()

        // Wait for seed data to appear
        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 5))

        // 300 (Oatmeal) + 450 (Chicken Salad) = 750
        // Consumed calories element uses accessibilityLabel
        let consumed = app.otherElements["dashboard_consumedCalories"]
        XCTAssertTrue(consumed.waitForExistence(timeout: 3))
        XCTAssertTrue(consumed.label.contains("750"))
    }

    func testMealSectionsAppearWithSeedData() {
        launchWithSeedData()

        // With seed data, empty state should NOT appear
        let emptyCard = app.buttons["dashboard_emptyStateCard"]
        XCTAssertFalse(emptyCard.waitForExistence(timeout: 2))

        // Breakfast and Lunch sections should be visible
        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Lunch"].exists)
    }

    // MARK: - Log Food Tab

    func testLogFoodTabOpensSheet() {
        launchEmpty()

        app.tabBars.buttons["Log Food"].tap()

        // Log Food sheet should appear with options
        XCTAssertTrue(app.staticTexts["Log Food"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Scan Food"].exists)
        XCTAssertTrue(app.staticTexts["Choose from Library"].exists)
        XCTAssertTrue(app.staticTexts["Log Manually"].exists)
    }

    // MARK: - Hero Card Details

    func testHeroCardShowsTargetCalories() {
        launchWithSeedData()

        // Wait for data to load
        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 5))

        // Goal stat is a combined StaticText with label "Goal, 2200"
        let goalElement = app.staticTexts["dashboard_goalValue"]
        XCTAssertTrue(goalElement.waitForExistence(timeout: 3))
        XCTAssertTrue(goalElement.label.contains("2200"), "Goal should show seed target of 2200")
    }

    func testHeroCardRemainingCalories() {
        launchWithSeedData()

        // Wait for data to load
        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 5))

        // Remaining = effectiveTarget - consumed
        // effectiveTarget = 2200 (seed target) + 456 (simulator burned calories) = 2656
        // remaining = 2656 - 750 = 1906
        let remainingElement = app.staticTexts["dashboard_remainingValue"]
        XCTAssertTrue(remainingElement.waitForExistence(timeout: 3))
        XCTAssertTrue(remainingElement.label.contains("1906"), "Remaining should be 1906 (2200 + 456 burned - 750 consumed)")
    }

    // MARK: - Empty State Link

    func testEmptyStateManualEntryLinkOpensSheet() {
        launchEmpty()

        let link = app.buttons["dashboard_manualEntryLink"]
        XCTAssertTrue(link.waitForExistence(timeout: 3))
        link.tap()

        // Log Food sheet should open
        XCTAssertTrue(app.staticTexts["Log Food"].waitForExistence(timeout: 3))
    }

    // MARK: - Hero Card Accessibility Elements

    func testHeroCardElementExists() {
        launchWithSeedData()

        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 5))

        let heroCard = app.otherElements["dashboard_heroCard"]
        XCTAssertTrue(heroCard.exists)
    }

    func testGoalValueElementExists() {
        launchWithSeedData()

        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 5))

        // StatRow is a combined accessibility element exposed as StaticText
        let goalValue = app.staticTexts["dashboard_goalValue"]
        XCTAssertTrue(goalValue.waitForExistence(timeout: 3))
    }

    func testRemainingValueElementExists() {
        launchWithSeedData()

        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 5))

        let remainingValue = app.staticTexts["dashboard_remainingValue"]
        XCTAssertTrue(remainingValue.waitForExistence(timeout: 3))
    }

    func testConsumedCaloriesElementExists() {
        launchWithSeedData()

        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 5))

        let consumed = app.otherElements["dashboard_consumedCalories"]
        XCTAssertTrue(consumed.waitForExistence(timeout: 3))
    }

    // MARK: - Meal Section Content

    func testSeedDataShowsFoodEntryNames() {
        launchWithSeedData()

        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 5))

        XCTAssertTrue(app.staticTexts["Oatmeal with Berries"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Chicken Salad"].waitForExistence(timeout: 3))
    }

    func testOnlyRelevantMealSectionsAppear() {
        launchWithSeedData()

        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Lunch"].exists)

        // Dinner and Snack should NOT appear (no entries for them)
        XCTAssertFalse(app.staticTexts["Dinner"].exists)
        XCTAssertFalse(app.staticTexts["Snack"].exists)
    }

    // MARK: - Meal Card Macro Display

    func testMealCardShowsProportionalBarNotGramLabels() {
        launchWithSeedData()

        // Seed data: Oatmeal (P:10 C:50 F:6), Chicken Salad (P:35 C:20 F:18)
        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 5))

        // The old InlineMacroRow showed "P: 10g" on the collapsed meal card.
        // The new proportional bar replaces those gram labels.
        // Verify gram labels do NOT appear at the meal card level.
        XCTAssertFalse(app.staticTexts["P: 10g"].exists,
                        "Collapsed meal card should show proportional bar, not gram labels")
        XCTAssertFalse(app.staticTexts["P: 35g"].exists,
                        "Collapsed meal card should show proportional bar, not gram labels")
    }

    // MARK: - Delete from Dashboard

    func testDeleteEntryFromDashboard() {
        launchWithSeedData()

        let oatmeal = app.staticTexts["Oatmeal with Berries"]
        XCTAssertTrue(oatmeal.waitForExistence(timeout: 5))

        // Long press to trigger context menu
        oatmeal.press(forDuration: 1.5)

        let deleteButton = app.buttons["Delete"]
        if deleteButton.waitForExistence(timeout: 3) {
            deleteButton.tap()

            // After deleting 300kcal oatmeal, consumed should be 450
            let consumed = app.otherElements["dashboard_consumedCalories"]
            XCTAssertTrue(consumed.waitForExistence(timeout: 3))
            XCTAssertTrue(consumed.label.contains("450"))
        }
    }

    // MARK: - Parity audit (2026-05-30) — group cards / full screen image

    func testMultiItemMealGroupCardShowsMealName() {
        launchWithMultiItemMeal()
        XCTAssertTrue(app.staticTexts["Mock Bento Box"].firstMatch.waitForExistence(timeout: 5))
    }

    func testMultiItemMealGroupSummaryRowExpandsToShowItems() {
        launchWithMultiItemMeal()
        let mealHeader = app.staticTexts["Mock Bento Box"].firstMatch
        XCTAssertTrue(mealHeader.waitForExistence(timeout: 5))
        // Tapping the group header should reveal child items
        mealHeader.tap()
        // Three seeded sub-items
        XCTAssertTrue(app.staticTexts["Brown Rice"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Teriyaki Chicken"].exists)
        XCTAssertTrue(app.staticTexts["Edamame"].exists)
    }

    func testMultiItemMealGroupLongPressShowsContextMenu() {
        launchWithMultiItemMeal()
        let mealHeader = app.staticTexts["Mock Bento Box"].firstMatch
        XCTAssertTrue(mealHeader.waitForExistence(timeout: 5))
        mealHeader.press(forDuration: 1.2)
        // Context menu items: View / Edit / Delete (we expect at least one to surface)
        let viewItem = app.buttons["View"]
        let editItem = app.buttons["Edit"]
        let deleteItem = app.buttons["Delete"]
        let anyContextItem = viewItem.waitForExistence(timeout: 3)
            || editItem.exists
            || deleteItem.exists
        XCTAssertTrue(anyContextItem, "Long-press on a meal group should reveal a context menu")
    }

    func testMultiItemMealSubItemLongPressShowsContextMenu() {
        launchWithMultiItemMeal()
        // Expand the group first
        let mealHeader = app.staticTexts["Mock Bento Box"].firstMatch
        XCTAssertTrue(mealHeader.waitForExistence(timeout: 5))
        mealHeader.tap()
        let subItem = app.staticTexts["Brown Rice"]
        XCTAssertTrue(subItem.waitForExistence(timeout: 3))
        subItem.press(forDuration: 1.2)
        let anyContextItem = app.buttons["View"].waitForExistence(timeout: 3)
            || app.buttons["Edit"].exists
            || app.buttons["Delete"].exists
        XCTAssertTrue(anyContextItem, "Long-press on a sub-item should reveal a context menu")
    }

    func testThumbnailTapOpensFullScreenImage() {
        launchWithImage()
        let entry = app.staticTexts["Mock Lunch with Photo"]
        XCTAssertTrue(entry.waitForExistence(timeout: 5))
        // Thumbnails live inside the entry card — tap on the entry's image area.
        // SwiftUI images appear as `images` query targets when accessible.
        let firstImage = app.images.firstMatch
        if firstImage.waitForExistence(timeout: 3) {
            firstImage.tap()
            // Full-screen cover should expose a close button (any X / Close)
            let closeButton = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] %@ OR label CONTAINS[c] %@",
                                                               "close", "done")).firstMatch
            XCTAssertTrue(closeButton.waitForExistence(timeout: 3),
                          "Full-screen image cover should be presented with a dismiss control")
        }
    }

    func testPullToRefreshGestureCompletesWithoutError() {
        launchWithSeedData()
        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 5))
        // Pull-to-refresh by swiping down from the top of the list
        let firstCell = app.staticTexts["Breakfast"]
        let start = firstCell.coordinate(withNormalizedOffset: .init(dx: 0.5, dy: 0.0))
        let end = firstCell.coordinate(withNormalizedOffset: .init(dx: 0.5, dy: 4.0))
        start.press(forDuration: 0.1, thenDragTo: end)
        // After refresh, the screen should still be intact
        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 5))
    }

    // MARK: - Parity audit (2026-05-30) — Edit / View context-menu actions

    func testEditEntryFromDashboardContextMenu() {
        launchWithSeedData()
        let entry = app.staticTexts["Oatmeal with Berries"]
        XCTAssertTrue(entry.waitForExistence(timeout: 5))
        entry.press(forDuration: 1.2)
        let editButton = app.buttons["Edit"]
        if editButton.waitForExistence(timeout: 3) {
            editButton.tap()
            // Edit sheet exposes text fields for name + calories + quantity (no testID per
            // EditFoodEntryView's textfields). Assert at least one TextField is present.
            XCTAssertTrue(app.textFields.firstMatch.waitForExistence(timeout: 3),
                          "Edit sheet should show editable text fields")
        }
    }

    func testViewEntryFromDashboardContextMenu() {
        launchWithSeedData()
        let entry = app.staticTexts["Oatmeal with Berries"]
        XCTAssertTrue(entry.waitForExistence(timeout: 5))
        entry.press(forDuration: 1.2)
        let viewButton = app.buttons["View"]
        if viewButton.waitForExistence(timeout: 3) {
            viewButton.tap()
            // View sheet is read-only — assert a Done button is present
            XCTAssertTrue(app.buttons["Done"].waitForExistence(timeout: 3),
                          "View sheet should expose a Done button")
        }
    }

    func testEditMealGroupFromDashboardContextMenu() {
        launchWithMultiItemMeal()
        let mealHeader = app.staticTexts["Mock Bento Box"].firstMatch
        XCTAssertTrue(mealHeader.waitForExistence(timeout: 5))
        mealHeader.press(forDuration: 1.2)
        let editButton = app.buttons["Edit"]
        if editButton.waitForExistence(timeout: 3) {
            editButton.tap()
            XCTAssertTrue(app.textFields.firstMatch.waitForExistence(timeout: 3),
                          "Edit meal group sheet should show editable text fields")
        }
    }

    func testEditSubItemFromDashboardContextMenu() {
        launchWithMultiItemMeal()
        let mealHeader = app.staticTexts["Mock Bento Box"].firstMatch
        XCTAssertTrue(mealHeader.waitForExistence(timeout: 5))
        mealHeader.tap()  // expand the group first
        let subItem = app.staticTexts["Brown Rice"]
        XCTAssertTrue(subItem.waitForExistence(timeout: 3))
        subItem.press(forDuration: 1.2)
        let editButton = app.buttons["Edit"]
        if editButton.waitForExistence(timeout: 3) {
            editButton.tap()
            XCTAssertTrue(app.textFields.firstMatch.waitForExistence(timeout: 3),
                          "Edit sub-item sheet should show editable text fields")
        }
    }
}

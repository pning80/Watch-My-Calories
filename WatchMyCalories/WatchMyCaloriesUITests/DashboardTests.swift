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
}

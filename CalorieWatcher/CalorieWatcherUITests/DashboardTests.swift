import XCTest

final class DashboardTests: CalorieWatcherUITestBase {

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
        // Verify "750" is displayed somewhere on the dashboard
        XCTAssertTrue(app.staticTexts["750"].waitForExistence(timeout: 3))
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

    // MARK: - Add Button

    func testAddButtonOpensManualEntry() {
        launchEmpty()

        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 3))
        addButton.tap()

        // Manual entry sheet should appear
        let foodNameField = app.textFields["manualEntry_foodName"]
        XCTAssertTrue(foodNameField.waitForExistence(timeout: 3))
    }

    // MARK: - Hero Card Details

    func testHeroCardShowsTargetCalories() {
        launchWithSeedData()

        // Wait for data to load
        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 5))

        // Goal stat should show "2200" (seed profile target)
        XCTAssertTrue(app.staticTexts["2200"].waitForExistence(timeout: 3))
    }

    func testHeroCardRemainingCalories() {
        launchWithSeedData()

        // Wait for data to load
        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 5))

        // Remaining = 2200 - 750 = 1450 (burned=0 in test mode)
        XCTAssertTrue(app.staticTexts["1450"].waitForExistence(timeout: 3))
    }

    // MARK: - Empty State Link

    func testEmptyStateManualEntryLinkOpensSheet() {
        launchEmpty()

        let link = app.buttons["dashboard_manualEntryLink"]
        XCTAssertTrue(link.waitForExistence(timeout: 3))
        link.tap()

        // Manual entry sheet should open
        let foodNameField = app.textFields["manualEntry_foodName"]
        XCTAssertTrue(foodNameField.waitForExistence(timeout: 3))
    }
}

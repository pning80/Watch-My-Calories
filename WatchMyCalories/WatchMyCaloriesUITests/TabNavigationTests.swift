import XCTest

final class TabNavigationTests: WatchMyCaloriesUITestBase {

    func testAllTabsAreVisible() {
        launchEmpty()

        XCTAssertTrue(app.tabBars.buttons["Today"].exists)
        XCTAssertTrue(app.tabBars.buttons["Log Food"].exists)
        XCTAssertTrue(app.tabBars.buttons["Scan Menu"].exists)
        XCTAssertTrue(app.tabBars.buttons["History"].exists)
    }

    func testSettingsTabDoesNotExist() {
        launchEmpty()

        XCTAssertFalse(app.tabBars.buttons["Settings"].exists)
    }

    func testTappingHistoryTabShowsHistory() {
        launchEmpty()

        app.tabBars.buttons["History"].tap()

        let historyTitle = app.staticTexts["history_title"]
        XCTAssertTrue(historyTitle.waitForExistence(timeout: 3))
    }

    func testTappingTodayTabReturnsToDashboard() {
        launchEmpty()

        app.tabBars.buttons["History"].tap()
        app.tabBars.buttons["Today"].tap()

        let addButton = app.buttons["dashboard_emptyStateCard"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 3))
    }

    func testTappingLogFoodTabShowsSheet() {
        launchEmpty()

        app.tabBars.buttons["Log Food"].tap()

        // Should show the log food sheet
        XCTAssertTrue(app.staticTexts["Log Food"].waitForExistence(timeout: 3))
    }

    func testTappingScanMenuTabShowsSheet() {
        launchEmpty()

        app.tabBars.buttons["Scan Menu"].tap()

        // Should show the scan menu sheet, not navigate away
        XCTAssertTrue(app.staticTexts["Scan Menu"].waitForExistence(timeout: 3))
    }

    func testRoundTripTabNavigation() {
        launchEmpty()

        // Navigate to History
        app.tabBars.buttons["History"].tap()
        XCTAssertTrue(app.staticTexts["history_title"].waitForExistence(timeout: 3))

        // Return to Today
        app.tabBars.buttons["Today"].tap()
        let emptyState = app.buttons["dashboard_emptyStateCard"]
        XCTAssertTrue(emptyState.waitForExistence(timeout: 3))
    }
}

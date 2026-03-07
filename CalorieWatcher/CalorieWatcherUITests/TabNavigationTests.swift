import XCTest

final class TabNavigationTests: CalorieWatcherUITestBase {

    func testAllTabsAreVisible() {
        launchEmpty()

        XCTAssertTrue(app.tabBars.buttons["Today"].exists)
        XCTAssertTrue(app.tabBars.buttons["Scan"].exists)
        XCTAssertTrue(app.tabBars.buttons["History"].exists)
        XCTAssertTrue(app.tabBars.buttons["Settings"].exists)
    }

    func testTappingHistoryTabShowsHistory() {
        launchEmpty()

        app.tabBars.buttons["History"].tap()

        let historyTitle = app.staticTexts["history_title"]
        XCTAssertTrue(historyTitle.waitForExistence(timeout: 3))
    }

    func testTappingSettingsTabShowsSettings() {
        launchEmpty()

        app.tabBars.buttons["Settings"].tap()

        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))
    }

    func testTappingTodayTabReturnsToDashboard() {
        launchEmpty()

        app.tabBars.buttons["Settings"].tap()
        app.tabBars.buttons["Today"].tap()

        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 3))
    }

    func testTappingScanTabSwitchesTab() {
        launchEmpty()

        app.tabBars.buttons["Scan"].tap()

        // Should no longer show the dashboard add button
        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertFalse(addButton.waitForExistence(timeout: 2))
    }

    func testRoundTripTabNavigation() {
        launchEmpty()

        // Navigate through all tabs and return to Today
        app.tabBars.buttons["History"].tap()
        XCTAssertTrue(app.staticTexts["history_title"].waitForExistence(timeout: 3))

        app.tabBars.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))

        app.tabBars.buttons["Scan"].tap()

        app.tabBars.buttons["Today"].tap()
        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 3))
    }
}

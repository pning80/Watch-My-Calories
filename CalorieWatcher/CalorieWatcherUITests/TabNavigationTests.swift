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
}

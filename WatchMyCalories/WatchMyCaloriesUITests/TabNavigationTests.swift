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

    func testScanMenuTabOpensSheetWithOptions() {
        launchEmpty()

        app.tabBars.buttons["Scan Menu"].tap()

        // Scan Menu sheet should appear with all three options
        XCTAssertTrue(app.staticTexts["Scan Menu"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Choose from Library"].exists)
        XCTAssertTrue(app.staticTexts["Stored Menus"].exists)
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

    // MARK: - Parity audit (2026-05-30) — modal-root cancel/done flows

    func testCameraRootCancelReturnsToDashboard() {
        launchEmpty()
        app.tabBars.buttons["Log Food"].tap()
        let scanFood = app.staticTexts["Scan Food"]
        XCTAssertTrue(scanFood.waitForExistence(timeout: 3))
        scanFood.tap()
        let cancelButton = app.buttons["Cancel"]
        XCTAssertTrue(cancelButton.waitForExistence(timeout: 5))
        cancelButton.tap()
        XCTAssertTrue(app.buttons["dashboard_emptyStateCard"].waitForExistence(timeout: 5))
    }

    func testScannedMenusDoneReturnsToDashboard() {
        let app = self.app!
        app.launchArguments.append("--seed-menu-scans")
        app.launch()
        app.tabBars.buttons["Scan Menu"].tap()
        let storedButton = app.descendants(matching: .any)["scanMenuSheet_storedMenus"].firstMatch
        XCTAssertTrue(storedButton.waitForExistence(timeout: 3))
        storedButton.tap()
        // The Stored Menus screen presents a Done button to dismiss.
        let doneButton = app.buttons["Done"]
        XCTAssertTrue(doneButton.waitForExistence(timeout: 5))
        doneButton.tap()
        XCTAssertTrue(app.buttons["dashboard_emptyStateCard"].waitForExistence(timeout: 5))
    }
}

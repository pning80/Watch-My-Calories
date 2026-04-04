import XCTest

final class ScanMenuTests: WatchMyCaloriesUITestBase {

    // MARK: - Scan Menu Sheet Accessibility IDs

    func testScanMenuSheetAccessibilityIDs() {
        launchEmpty()

        app.tabBars.buttons["Scan Menu"].tap()

        XCTAssertTrue(app.staticTexts["Scan Menu"].waitForExistence(timeout: 3))

        // All three option buttons should have their accessibility IDs
        let scanButton = app.descendants(matching: .any)["scanMenuSheet_scan"].firstMatch
        let libraryButton = app.descendants(matching: .any)["scanMenuSheet_chooseFromLibrary"].firstMatch
        let storedButton = app.descendants(matching: .any)["scanMenuSheet_storedMenus"].firstMatch

        XCTAssertTrue(scanButton.waitForExistence(timeout: 3))
        XCTAssertTrue(libraryButton.exists)
        XCTAssertTrue(storedButton.exists)
    }

    // MARK: - Stored Menus

    func testStoredMenusShowsEmptyState() {
        launchEmpty()

        app.tabBars.buttons["Scan Menu"].tap()
        XCTAssertTrue(app.staticTexts["Scan Menu"].waitForExistence(timeout: 3))

        // Tap Stored Menus
        let storedButton = app.descendants(matching: .any)["scanMenuSheet_storedMenus"].firstMatch
        XCTAssertTrue(storedButton.waitForExistence(timeout: 3))
        storedButton.tap()

        // Should show empty state
        let emptyState = app.descendants(matching: .any)["scannedMenus_emptyState"].firstMatch
        XCTAssertTrue(emptyState.waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["No scanned menus yet"].exists)
    }

    // MARK: - Sheet Dismissal

    func testScanMenuSheetDismissOnSwipe() {
        launchEmpty()

        app.tabBars.buttons["Scan Menu"].tap()
        XCTAssertTrue(app.staticTexts["Scan Menu"].waitForExistence(timeout: 3))

        // Swipe down to dismiss
        app.swipeDown()

        // Sheet should be dismissed — dashboard should be visible
        let emptyState = app.buttons["dashboard_emptyStateCard"]
        XCTAssertTrue(emptyState.waitForExistence(timeout: 3))
    }
}

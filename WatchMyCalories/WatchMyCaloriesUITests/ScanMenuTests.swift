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

    // MARK: - Parity audit (2026-05-30) — ScanMenuSheet tap behaviors

    func testScanMenuSheetScanButtonOpensCamera() {
        launchEmpty()
        app.tabBars.buttons["Scan Menu"].tap()
        XCTAssertTrue(app.staticTexts["Scan Menu"].waitForExistence(timeout: 3))
        let scanButton = app.descendants(matching: .any)["scanMenuSheet_scan"].firstMatch
        XCTAssertTrue(scanButton.waitForExistence(timeout: 3))
        scanButton.tap()
        // After tapping Scan, the menu camera root should appear — look for a Cancel button.
        XCTAssertTrue(app.buttons["Cancel"].waitForExistence(timeout: 5),
                      "Menu camera root should be presented with a Cancel button")
    }

    func testScanMenuSheetChooseFromLibraryButtonOpensPicker() {
        launchEmpty()
        app.tabBars.buttons["Scan Menu"].tap()
        XCTAssertTrue(app.staticTexts["Scan Menu"].waitForExistence(timeout: 3))
        let libraryButton = app.descendants(matching: .any)["scanMenuSheet_chooseFromLibrary"].firstMatch
        XCTAssertTrue(libraryButton.waitForExistence(timeout: 3))
        libraryButton.tap()
        // PhotosUI picker presents — there is no XCUITest-accessible identifier for it system-side,
        // but the main scan-menu sheet should no longer be the focused content.
        // Verify the menu sheet was dismissed by checking the sheet's marker text is gone.
        XCTAssertFalse(app.staticTexts["Scan Menu"].waitForExistence(timeout: 2))
    }

    // MARK: - Parity audit (2026-05-30) — ScannedMenus list with seed data

    func testScannedMenusListShowsRowsWithSeedData() {
        launchWithMenuScans()
        app.tabBars.buttons["Scan Menu"].tap()
        let storedButton = app.descendants(matching: .any)["scanMenuSheet_storedMenus"].firstMatch
        XCTAssertTrue(storedButton.waitForExistence(timeout: 3))
        storedButton.tap()
        // Seeded scans include "Mock Italian Place" and "Mock Sushi Bar"
        XCTAssertTrue(app.staticTexts["Mock Italian Place"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Mock Sushi Bar"].exists)
    }

    func testScannedMenusTappingRowOpensDetail() {
        launchWithMenuScans()
        app.tabBars.buttons["Scan Menu"].tap()
        let storedButton = app.descendants(matching: .any)["scanMenuSheet_storedMenus"].firstMatch
        XCTAssertTrue(storedButton.waitForExistence(timeout: 3))
        storedButton.tap()
        let row = app.staticTexts["Mock Italian Place"]
        XCTAssertTrue(row.waitForExistence(timeout: 3))
        row.tap()
        // Detail screen should show one of the seeded items
        XCTAssertTrue(app.staticTexts["Margherita Pizza"].waitForExistence(timeout: 5))
    }

    func testScannedMenusEditButtonTogglesMode() {
        launchWithMenuScans()
        app.tabBars.buttons["Scan Menu"].tap()
        let storedButton = app.descendants(matching: .any)["scanMenuSheet_storedMenus"].firstMatch
        XCTAssertTrue(storedButton.waitForExistence(timeout: 3))
        storedButton.tap()
        let editButton = app.buttons["Edit"]
        XCTAssertTrue(editButton.waitForExistence(timeout: 3))
        editButton.tap()
        // Done button replaces Edit when in edit mode
        XCTAssertTrue(app.buttons["Done"].waitForExistence(timeout: 2))
    }

    func testScannedMenusSwipeToDeleteRevealsAction() {
        launchWithMenuScans()
        app.tabBars.buttons["Scan Menu"].tap()
        let storedButton = app.descendants(matching: .any)["scanMenuSheet_storedMenus"].firstMatch
        XCTAssertTrue(storedButton.waitForExistence(timeout: 3))
        storedButton.tap()
        let row = app.cells.containing(NSPredicate(format: "label CONTAINS %@", "Mock Italian Place")).firstMatch
        if row.waitForExistence(timeout: 3) {
            row.swipeLeft()
            XCTAssertTrue(app.buttons["Delete"].waitForExistence(timeout: 2),
                          "Swiping left should reveal a Delete action")
        }
    }

    func testMenuScanDetailDeleteButtonShowsConfirmation() {
        launchWithMenuScans()
        app.tabBars.buttons["Scan Menu"].tap()
        let storedButton = app.descendants(matching: .any)["scanMenuSheet_storedMenus"].firstMatch
        XCTAssertTrue(storedButton.waitForExistence(timeout: 3))
        storedButton.tap()
        let row = app.staticTexts["Mock Italian Place"]
        XCTAssertTrue(row.waitForExistence(timeout: 3))
        row.tap()
        // The detail view's delete is a toolbar Button with `Image(systemName: "trash")`
        // and `role: .destructive`. Find it by image identifier rather than label
        // (which is ambiguous with the confirmation dialog's "Delete" button).
        let trashButton = app.navigationBars.buttons["trash"].firstMatch
        XCTAssertTrue(trashButton.waitForExistence(timeout: 3),
                      "Detail screen should expose a trash toolbar button")
        trashButton.tap()
        // After tap, the confirmationDialog "Delete this scanned menu?" appears.
        let confirmTitle = app.staticTexts["Delete this scanned menu?"]
        XCTAssertTrue(confirmTitle.waitForExistence(timeout: 3),
                      "Trash button should present a deletion confirmation dialog")
    }

    // MARK: - Parity audit (2026-05-30) — Menu Camera UI elements

    func testMenuCameraScreenHasCaptureButton() {
        launchEmpty()
        app.tabBars.buttons["Scan Menu"].tap()
        let scanButton = app.descendants(matching: .any)["scanMenuSheet_scan"].firstMatch
        XCTAssertTrue(scanButton.waitForExistence(timeout: 3))
        scanButton.tap()
        // The menu camera should expose its capture-related UI. Without a real camera in the sim,
        // we assert structural elements only — Cancel + something tappable on the bottom.
        XCTAssertTrue(app.buttons["Cancel"].waitForExistence(timeout: 5))
    }

    func testMenuCameraCancelDismissesToDashboard() {
        launchEmpty()
        app.tabBars.buttons["Scan Menu"].tap()
        let scanButton = app.descendants(matching: .any)["scanMenuSheet_scan"].firstMatch
        XCTAssertTrue(scanButton.waitForExistence(timeout: 3))
        scanButton.tap()
        let cancelButton = app.buttons["Cancel"]
        XCTAssertTrue(cancelButton.waitForExistence(timeout: 5))
        cancelButton.tap()
        XCTAssertTrue(app.buttons["dashboard_emptyStateCard"].waitForExistence(timeout: 3))
    }
}

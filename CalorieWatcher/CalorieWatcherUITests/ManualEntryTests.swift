import XCTest

final class ManualEntryTests: CalorieWatcherUITestBase {

    private func openManualEntry() {
        launchEmpty()
        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 3))
        addButton.tap()
    }

    // MARK: - Form Fields

    func testManualEntryFieldsExist() {
        openManualEntry()

        XCTAssertTrue(app.textFields["manualEntry_foodName"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.textFields["manualEntry_calories"].exists)
        XCTAssertTrue(app.textFields["manualEntry_quantity"].exists)
    }

    func testSaveButtonDisabledWhenEmpty() {
        openManualEntry()

        let saveButton = app.buttons["manualEntry_saveButton"]
        XCTAssertTrue(saveButton.waitForExistence(timeout: 3))
        XCTAssertFalse(saveButton.isEnabled)
    }

    func testCancelDismissesSheet() {
        openManualEntry()

        let cancelButton = app.buttons["manualEntry_cancelButton"]
        XCTAssertTrue(cancelButton.waitForExistence(timeout: 3))
        cancelButton.tap()

        // Sheet should dismiss — add button should reappear on dashboard
        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 3))
    }

    func testCanSaveEntry() {
        openManualEntry()

        let nameField = app.textFields["manualEntry_foodName"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 3))
        nameField.tap()
        nameField.typeText("Apple")

        let caloriesField = app.textFields["manualEntry_calories"]
        caloriesField.tap()
        caloriesField.typeText("95")

        let quantityField = app.textFields["manualEntry_quantity"]
        quantityField.tap()
        quantityField.typeText("1 medium")

        let saveButton = app.buttons["manualEntry_saveButton"]
        XCTAssertTrue(saveButton.isEnabled)
        saveButton.tap()

        // After save, we should be back on dashboard with the entry visible
        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 3))

        // The food entry should appear on the dashboard
        XCTAssertTrue(app.staticTexts["Apple"].waitForExistence(timeout: 3))
    }

    // MARK: - Meal Type Picker

    func testMealTypePickerInteraction() {
        openManualEntry()

        let snackButton = app.buttons["Snack"]
        XCTAssertTrue(snackButton.waitForExistence(timeout: 3))
        snackButton.tap()

        let dinnerButton = app.buttons["Dinner"]
        XCTAssertTrue(dinnerButton.exists)
        dinnerButton.tap()
    }

    func testSaveWithDinnerMealType() {
        openManualEntry()

        let nameField = app.textFields["manualEntry_foodName"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 3))
        nameField.tap()
        nameField.typeText("Steak")

        let caloriesField = app.textFields["manualEntry_calories"]
        caloriesField.tap()
        caloriesField.typeText("500")

        let quantityField = app.textFields["manualEntry_quantity"]
        quantityField.tap()
        quantityField.typeText("8 oz")

        // Select Dinner meal type
        app.buttons["Dinner"].tap()

        let saveButton = app.buttons["manualEntry_saveButton"]
        XCTAssertTrue(saveButton.isEnabled)
        saveButton.tap()

        // Verify "Dinner" section appears on dashboard
        XCTAssertTrue(app.staticTexts["Dinner"].waitForExistence(timeout: 3))
    }

    // MARK: - Validation

    func testZeroCaloriesDisablesSave() {
        openManualEntry()

        let nameField = app.textFields["manualEntry_foodName"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 3))
        nameField.tap()
        nameField.typeText("Water")

        let caloriesField = app.textFields["manualEntry_calories"]
        caloriesField.tap()
        caloriesField.typeText("0")

        let quantityField = app.textFields["manualEntry_quantity"]
        quantityField.tap()
        quantityField.typeText("1 glass")

        let saveButton = app.buttons["manualEntry_saveButton"]
        XCTAssertFalse(saveButton.isEnabled)
    }

    // MARK: - Scan Button

    func testScanButtonExistsInManualEntry() {
        openManualEntry()

        XCTAssertTrue(app.staticTexts["Scan with Camera"].waitForExistence(timeout: 3))
    }
}

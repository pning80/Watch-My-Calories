import XCTest

final class ManualEntryTests: WatchMyCaloriesUITestBase {

    private func openManualEntry() {
        launchEmpty()
        // Open via Log Food tab → Log Manually
        app.tabBars.buttons["Log Food"].tap()
        let logManually = app.staticTexts["Log Manually"]
        XCTAssertTrue(logManually.waitForExistence(timeout: 3))
        logManually.tap()
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
        let addButton = app.buttons["dashboard_emptyStateCard"]
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
        XCTAssertTrue(app.staticTexts["Apple"].waitForExistence(timeout: 5))
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

    // MARK: - Save Button Enabled

    func testSaveButtonEnabledWhenAllFieldsFilled() {
        openManualEntry()

        let nameField = app.textFields["manualEntry_foodName"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 3))
        nameField.tap()
        nameField.typeText("Banana")

        let caloriesField = app.textFields["manualEntry_calories"]
        caloriesField.tap()
        caloriesField.typeText("105")

        let quantityField = app.textFields["manualEntry_quantity"]
        quantityField.tap()
        quantityField.typeText("1 medium")

        let saveButton = app.buttons["manualEntry_saveButton"]
        XCTAssertTrue(saveButton.isEnabled)
    }

    // MARK: - Partial Validation

    func testMissingNameDisablesSave() {
        openManualEntry()

        // Fill calories and quantity but NOT name
        let caloriesField = app.textFields["manualEntry_calories"]
        XCTAssertTrue(caloriesField.waitForExistence(timeout: 3))
        caloriesField.tap()
        caloriesField.typeText("200")

        let quantityField = app.textFields["manualEntry_quantity"]
        quantityField.tap()
        quantityField.typeText("1 cup")

        let saveButton = app.buttons["manualEntry_saveButton"]
        XCTAssertFalse(saveButton.isEnabled)
    }

    func testMissingQuantityDisablesSave() {
        openManualEntry()

        // Fill name and calories but NOT quantity
        let nameField = app.textFields["manualEntry_foodName"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 3))
        nameField.tap()
        nameField.typeText("Rice")

        let caloriesField = app.textFields["manualEntry_calories"]
        caloriesField.tap()
        caloriesField.typeText("200")

        let saveButton = app.buttons["manualEntry_saveButton"]
        XCTAssertFalse(saveButton.isEnabled)
    }

    // MARK: - Nutrition Details

    func testNutritionDisclosureGroupExpands() {
        openManualEntry()

        let disclosure = app.staticTexts["Nutrition Details (optional)"]
        XCTAssertTrue(disclosure.waitForExistence(timeout: 3))
        disclosure.tap()

        // After expanding, nutrient fields should appear
        XCTAssertTrue(app.staticTexts["Protein (g)"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Carbs (g)"].exists)
        XCTAssertTrue(app.staticTexts["Fat (g)"].exists)
    }

    // MARK: - Meal Picker Segments

    func testAllMealTypeSegmentsExist() {
        openManualEntry()

        XCTAssertTrue(app.buttons["Breakfast"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["Lunch"].exists)
        XCTAssertTrue(app.buttons["Dinner"].exists)
        XCTAssertTrue(app.buttons["Snack"].exists)
    }

    // MARK: - Navigation Title

    func testManualEntryNavigationTitle() {
        openManualEntry()

        XCTAssertTrue(app.navigationBars["Log Food"].waitForExistence(timeout: 3))
    }

    // MARK: - Parity audit (2026-05-30) — nutrition field coverage

    /// The nutrition `NutrientField` uses `TextField("—", text: ...)` so the underlying
    /// XCUIElement label is "—", not "Protein". We index by position: after expanding the
    /// disclosure, the 4th/5th/6th text fields (0-indexed 3/4/5) are Protein/Carbs/Fat.
    /// The first three are Food Name / Calories / Quantity.
    private func expandNutritionAndGetField(at index: Int) -> XCUIElement {
        let disclosure = app.staticTexts["Nutrition Details (optional)"]
        XCTAssertTrue(disclosure.waitForExistence(timeout: 3))
        disclosure.tap()
        // Wait for the new fields to appear (count should grow from 3 to 6)
        let predicate = NSPredicate(format: "count >= 6")
        let expectation = XCTNSPredicateExpectation(predicate: predicate, object: app.textFields)
        _ = XCTWaiter().wait(for: [expectation], timeout: 3)
        return app.textFields.element(boundBy: index)
    }

    func testProteinFieldAcceptsInput() {
        openManualEntry()
        let proteinField = expandNutritionAndGetField(at: 3)
        XCTAssertTrue(proteinField.waitForExistence(timeout: 3))
        proteinField.tap()
        proteinField.typeText("12")
        XCTAssertEqual(proteinField.value as? String, "12")
    }

    func testCarbsFieldAcceptsInput() {
        openManualEntry()
        let carbsField = expandNutritionAndGetField(at: 4)
        XCTAssertTrue(carbsField.waitForExistence(timeout: 3))
        carbsField.tap()
        carbsField.typeText("30")
        XCTAssertEqual(carbsField.value as? String, "30")
    }

    func testFatFieldAcceptsInput() {
        openManualEntry()
        let fatField = expandNutritionAndGetField(at: 5)
        XCTAssertTrue(fatField.waitForExistence(timeout: 3))
        fatField.tap()
        fatField.typeText("8")
        XCTAssertEqual(fatField.value as? String, "8")
    }

    func testManualEntryWithFullNutritionSaves() {
        openManualEntry()
        let nameField = app.textFields["manualEntry_foodName"]
        nameField.tap()
        nameField.typeText("Mock Salad")

        let caloriesField = app.textFields["manualEntry_calories"]
        caloriesField.tap()
        caloriesField.typeText("400")

        let quantityField = app.textFields["manualEntry_quantity"]
        quantityField.tap()
        quantityField.typeText("1 bowl")

        let proteinField = expandNutritionAndGetField(at: 3)
        proteinField.tap()
        proteinField.typeText("25")

        let saveButton = app.buttons["manualEntry_saveButton"]
        XCTAssertTrue(saveButton.isEnabled)
        saveButton.tap()
        XCTAssertTrue(app.staticTexts["Mock Salad"].waitForExistence(timeout: 5))
    }
}

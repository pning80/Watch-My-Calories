import XCTest

final class SettingsTests: CalorieWatcherUITestBase {

    private func openSettings() {
        launchEmpty()
        app.tabBars.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))
    }

    func testSettingsFieldsExist() {
        openSettings()

        XCTAssertTrue(app.buttons["settings_saveButton"].waitForExistence(timeout: 3))

        // Daily Goals section is below the fold — scroll to reveal
        app.swipeUp()
        XCTAssertTrue(app.buttons["settings_calculateGoal"].waitForExistence(timeout: 3))

        // Scroll down to reveal the Privacy section
        app.swipeUp()

        let toggle = app.switches["settings_aiConsentToggle"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 3))
    }

    func testCalculateGoalSetsTargetCalories() {
        openSettings()

        // Daily Goals section is below the fold — scroll to reveal
        app.swipeUp()

        let calculateButton = app.buttons["settings_calculateGoal"]
        XCTAssertTrue(calculateButton.waitForExistence(timeout: 3))
        calculateButton.tap()

        // After calculation, the target calories field should have a value
        let targetField = app.textFields["settings_targetCalories"]
        XCTAssertTrue(targetField.waitForExistence(timeout: 3))

        let value = targetField.value as? String ?? ""
        XCTAssertFalse(value.isEmpty, "Target calories should be populated after calculation")
    }

    func testSaveNavigatesToDashboard() {
        openSettings()

        let saveButton = app.buttons["settings_saveButton"]
        XCTAssertTrue(saveButton.waitForExistence(timeout: 3))
        saveButton.tap()

        // Should navigate to dashboard tab
        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 3))
    }

    func testAIConsentToggleExists() {
        openSettings()

        // Scroll down to reveal the Privacy section
        app.swipeUp()

        let toggle = app.switches["settings_aiConsentToggle"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 3))
    }

    // MARK: - Picker Interactions

    func testGenderPickerInteraction() {
        openSettings()

        // Gender picker should exist and show "Male" as default
        let genderPicker = app.buttons["settings_genderPicker"]
        XCTAssertTrue(genderPicker.waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Male"].exists)
    }

    func testActivityLevelPickerInteraction() {
        openSettings()

        // Activity picker should exist and show "Sedentary" as default
        let activityPicker = app.buttons["settings_activityPicker"]
        XCTAssertTrue(activityPicker.waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Sedentary"].exists)
    }

    // MARK: - Calculate Goal

    func testCalculateGoalMatchesMifflinStJeor() {
        // Launch with seed data (30yo male, 175cm, 70kg, moderately active)
        launchWithSeedData()

        app.tabBars.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))

        // Daily Goals section is below the fold — scroll to reveal
        app.swipeUp()

        let calculateButton = app.buttons["settings_calculateGoal"]
        XCTAssertTrue(calculateButton.waitForExistence(timeout: 3))
        calculateButton.tap()

        // Mifflin-St Jeor with imperial conversion (175cm → 5'8" = 172.72cm, 70kg → 154lbs = ~69.87kg):
        // BMR ≈ (10*69.87) + (6.25*172.72) - (5*30) + 5 ≈ 1633
        // TDEE ≈ 1633 * 1.55 ≈ 2531
        let targetField = app.textFields["settings_targetCalories"]
        XCTAssertTrue(targetField.waitForExistence(timeout: 3))

        let value = targetField.value as? String ?? ""
        // Extract digits only for comparison (value may contain commas)
        let digits = value.filter { $0.isNumber }
        let numericValue = Int(digits) ?? 0
        XCTAssertTrue(numericValue >= 2500 && numericValue <= 2560,
                       "Expected calculated goal in range 2500-2560, got \(value)")
    }

    // MARK: - Unsaved Changes Alert

    private func openSettingsWithSeedData() {
        launchWithSeedData()
        app.tabBars.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))
    }

    func testUnsavedChangesAlertOnTabSwitch() {
        openSettingsWithSeedData()

        // Daily Goals section is below the fold — scroll to reveal
        app.swipeUp()

        // Make a change — calculate goal to modify target calories
        let calculateButton = app.buttons["settings_calculateGoal"]
        XCTAssertTrue(calculateButton.waitForExistence(timeout: 3))
        calculateButton.tap()

        // Switch tab — should trigger unsaved changes alert
        app.tabBars.buttons["Today"].tap()

        let alert = app.alerts["Unsaved Changes"]
        XCTAssertTrue(alert.waitForExistence(timeout: 3))
        XCTAssertTrue(alert.buttons["Save"].exists)
        XCTAssertTrue(alert.buttons["Discard"].exists)
    }

    func testUnsavedChangesAlertSave() {
        openSettingsWithSeedData()

        // Daily Goals section is below the fold — scroll to reveal
        app.swipeUp()

        let calculateButton = app.buttons["settings_calculateGoal"]
        XCTAssertTrue(calculateButton.waitForExistence(timeout: 3))
        calculateButton.tap()

        app.tabBars.buttons["Today"].tap()

        let alert = app.alerts["Unsaved Changes"]
        XCTAssertTrue(alert.waitForExistence(timeout: 3))
        alert.buttons["Save"].tap()

        // Should navigate to dashboard
        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 3))
    }

    func testUnsavedChangesAlertDiscard() {
        openSettingsWithSeedData()

        // Daily Goals section is below the fold — scroll to reveal
        app.swipeUp()

        let calculateButton = app.buttons["settings_calculateGoal"]
        XCTAssertTrue(calculateButton.waitForExistence(timeout: 3))
        calculateButton.tap()

        app.tabBars.buttons["Today"].tap()

        let alert = app.alerts["Unsaved Changes"]
        XCTAssertTrue(alert.waitForExistence(timeout: 3))
        alert.buttons["Discard"].tap()

        // Should navigate to dashboard
        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 3))
    }

    // MARK: - AI Consent Toggle

    func testAIConsentToggleCanBeToggled() {
        openSettings()

        // Scroll enough to fully reveal the Privacy section
        app.swipeUp()
        app.swipeUp()

        let toggle = app.switches["settings_aiConsentToggle"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 3))

        let initialValue = toggle.value as? String ?? ""
        toggle.switches.firstMatch.tap()

        let newValue = toggle.value as? String ?? ""
        XCTAssertNotEqual(initialValue, newValue, "Toggle value should change after tap")
    }

    // MARK: - Theme & Unit Pickers

    func testThemePickerExists() {
        openSettings()

        // Theme picker should exist in the App Appearance section
        XCTAssertTrue(app.staticTexts["Theme"].waitForExistence(timeout: 3))

        // Verify the theme picker is present via its accessibility identifier
        let themePicker = app.buttons["settings_themePicker"]
        XCTAssertTrue(themePicker.exists, "Theme picker should exist")
    }

    func testUnitPickerExists() {
        openSettings()

        XCTAssertTrue(app.staticTexts["Unit System"].waitForExistence(timeout: 3))
    }

    // MARK: - Target Calories Manual Edit

    func testTargetCaloriesFieldEditable() {
        openSettings()

        // Daily Goals section is below the fold — scroll to reveal
        app.swipeUp()

        let targetField = app.textFields["settings_targetCalories"]
        XCTAssertTrue(targetField.waitForExistence(timeout: 3))
        targetField.tap()
        targetField.typeText("1800")

        let value = targetField.value as? String ?? ""
        XCTAssertTrue(value.contains("1800"), "Target calories should reflect typed value")
    }

    // MARK: - Profile Fields with Seed Data

    func testSettingsLoadSeedProfileValues() {
        openSettingsWithSeedData()

        // Seed profile: 30yo, male, moderately active
        // Height in US: 5'8" (175cm → 68.9in → 5ft 8in)
        // Weight in US: 154 lbs (70kg)
        XCTAssertTrue(app.staticTexts["Male"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Moderately Active"].exists)
    }

    // MARK: - No Unsaved Changes Without Edits

    func testNoUnsavedAlertWhenUnchanged() {
        openSettingsWithSeedData()

        // Switch tab without making changes — no alert should appear
        app.tabBars.buttons["Today"].tap()

        // Should navigate directly to dashboard without alert
        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 3))

        // Confirm no alert is shown
        XCTAssertFalse(app.alerts["Unsaved Changes"].exists)
    }

    // MARK: - Profile Section Labels

    func testProfileSectionFieldsVisible() {
        openSettings()

        XCTAssertTrue(app.staticTexts["Height"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Weight"].exists)
        XCTAssertTrue(app.staticTexts["Age"].exists)
        XCTAssertTrue(app.staticTexts["Gender"].exists)
        XCTAssertTrue(app.staticTexts["Activity"].exists)
    }

    // MARK: - Daily Goals Section

    func testDailyGoalsSectionVisible() {
        openSettings()

        // Daily Goals section is below the fold — scroll to reveal
        app.swipeUp()

        XCTAssertTrue(app.staticTexts["Target Calories"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["settings_calculateGoal"].exists)
    }

    // MARK: - Privacy Section Text

    func testPrivacySectionInfoText() {
        openSettings()

        app.swipeUp()

        let infoText = app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "food photos are sent to Google Gemini"))
        XCTAssertTrue(infoText.firstMatch.waitForExistence(timeout: 3))
    }
}

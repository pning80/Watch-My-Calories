import XCTest

final class SettingsTests: WatchMyCaloriesUITestBase {

    /// Opens Settings via the app menu on Today screen
    private func openSettings() {
        launchEmpty()
        let menuButton = app.buttons["appMenu_button"]
        XCTAssertTrue(menuButton.waitForExistence(timeout: 3))
        menuButton.tap()
        app.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))
    }

    private func openSettingsWithSeedData() {
        launchWithSeedData()
        let menuButton = app.buttons["appMenu_button"]
        XCTAssertTrue(menuButton.waitForExistence(timeout: 3))
        menuButton.tap()
        app.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))
    }

    // MARK: - Fields

    func testSettingsFieldsExist() {
        openSettings()

        // Done button should exist (replaced Save)
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

        app.swipeUp()

        let calculateButton = app.buttons["settings_calculateGoal"]
        XCTAssertTrue(calculateButton.waitForExistence(timeout: 3))
        calculateButton.tap()

        let targetField = app.textFields["settings_targetCalories"]
        XCTAssertTrue(targetField.waitForExistence(timeout: 3))

        let value = targetField.value as? String ?? ""
        XCTAssertFalse(value.isEmpty, "Target calories should be populated after calculation")
    }

    // MARK: - Save & Cancel

    func testSaveButtonDismissesSheet() {
        openSettings()

        let saveButton = app.buttons["settings_saveButton"]
        XCTAssertTrue(saveButton.waitForExistence(timeout: 3))
        saveButton.tap()

        // Sheet should dismiss — we should be back on the dashboard
        let emptyState = app.buttons["dashboard_emptyStateCard"]
        XCTAssertTrue(emptyState.waitForExistence(timeout: 3))
    }

    func testCancelButtonDismissesWhenNoChanges() {
        openSettings()

        app.buttons["Cancel"].tap()

        // Should dismiss directly without dialog
        let emptyState = app.buttons["dashboard_emptyStateCard"]
        XCTAssertTrue(emptyState.waitForExistence(timeout: 3))
    }

    // MARK: - AI Consent

    func testAIConsentToggleExists() {
        openSettings()

        app.swipeUp()

        let toggle = app.switches["settings_aiConsentToggle"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 3))
    }

    func testAIConsentToggleCanBeToggled() {
        openSettings()

        app.swipeUp()
        app.swipeUp()

        let toggle = app.switches["settings_aiConsentToggle"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 3))

        let initialValue = toggle.value as? String ?? ""
        toggle.switches.firstMatch.tap()

        let newValue = toggle.value as? String ?? ""
        XCTAssertNotEqual(initialValue, newValue, "Toggle value should change after tap")
    }

    // MARK: - Picker Interactions

    func testGenderPickerInteraction() {
        openSettings()

        let genderPicker = app.buttons["settings_genderPicker"]
        XCTAssertTrue(genderPicker.waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Male"].exists)
    }

    func testActivityLevelPickerInteraction() {
        openSettings()

        let activityPicker = app.buttons["settings_activityPicker"]
        XCTAssertTrue(activityPicker.waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Sedentary"].exists)
    }

    // MARK: - Calculate Goal with Seed Data

    func testCalculateGoalMatchesMifflinStJeor() {
        openSettingsWithSeedData()

        app.swipeUp()

        let calculateButton = app.buttons["settings_calculateGoal"]
        XCTAssertTrue(calculateButton.waitForExistence(timeout: 3))
        calculateButton.tap()

        let targetField = app.textFields["settings_targetCalories"]
        XCTAssertTrue(targetField.waitForExistence(timeout: 3))

        let value = targetField.value as? String ?? ""
        let digits = value.filter { $0.isNumber }
        let numericValue = Int(digits) ?? 0
        XCTAssertTrue(numericValue >= 2500 && numericValue <= 2560,
                       "Expected calculated goal in range 2500-2560, got \(value)")
    }

    // MARK: - Cancel with Unsaved Changes

    func testCancelShowsDiscardDialogWhenChanged() {
        openSettingsWithSeedData()

        app.swipeUp()

        // Make a change — calculate goal to modify target calories
        let calculateButton = app.buttons["settings_calculateGoal"]
        XCTAssertTrue(calculateButton.waitForExistence(timeout: 3))
        calculateButton.tap()

        // Tap Cancel — should trigger discard confirmation dialog
        app.buttons["Cancel"].tap()

        let discardButton = app.buttons["Discard Changes"]
        XCTAssertTrue(discardButton.waitForExistence(timeout: 3))
    }

    func testDiscardChangesFromCancelDialog() {
        openSettingsWithSeedData()

        app.swipeUp()

        let calculateButton = app.buttons["settings_calculateGoal"]
        XCTAssertTrue(calculateButton.waitForExistence(timeout: 3))
        calculateButton.tap()

        app.buttons["Cancel"].tap()

        let discardButton = app.buttons["Discard Changes"]
        XCTAssertTrue(discardButton.waitForExistence(timeout: 3))
        discardButton.tap()

        // Sheet should dismiss — back on dashboard
        let heroCard = app.otherElements["dashboard_heroCard"]
        XCTAssertTrue(heroCard.waitForExistence(timeout: 3))
    }

    func testKeepEditingFromCancelDialog() {
        openSettingsWithSeedData()

        app.swipeUp()

        let calculateButton = app.buttons["settings_calculateGoal"]
        XCTAssertTrue(calculateButton.waitForExistence(timeout: 3))
        calculateButton.tap()

        app.buttons["Cancel"].tap()

        let keepButton = app.buttons["Keep Editing"]
        XCTAssertTrue(keepButton.waitForExistence(timeout: 3))
        keepButton.tap()

        // Should still be on Settings
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))
    }

    func testCancelNoDialogWhenUnchanged() {
        openSettingsWithSeedData()

        // Tap Cancel without making changes — no dialog, just dismiss
        app.buttons["Cancel"].tap()

        // Should dismiss directly to dashboard
        let heroCard = app.otherElements["dashboard_heroCard"]
        XCTAssertTrue(heroCard.waitForExistence(timeout: 3))
    }

    func testDeviceAttestationNotInSettings() {
        openSettings()

        app.swipeUp()
        app.swipeUp()

        let attestation = app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "Device Attestation"))
        XCTAssertFalse(attestation.firstMatch.waitForExistence(timeout: 2))
    }

    // MARK: - Theme & Unit Pickers

    func testThemePickerExists() {
        openSettings()

        XCTAssertTrue(app.staticTexts["Theme"].waitForExistence(timeout: 3))

        let themePicker = app.buttons["settings_themePicker"]
        XCTAssertTrue(themePicker.exists, "Theme picker should exist")
    }

    func testUnitPickerExists() {
        openSettings()

        XCTAssertTrue(app.staticTexts["Unit System"].waitForExistence(timeout: 3))
    }

    // MARK: - Target Calories

    func testTargetCaloriesFieldEditable() {
        openSettings()

        app.swipeUp()

        let targetField = app.textFields["settings_targetCalories"]
        XCTAssertTrue(targetField.waitForExistence(timeout: 3))
        targetField.tap()
        targetField.typeText("1800")

        let value = targetField.value as? String ?? ""
        XCTAssertTrue(value.contains("1800"), "Target calories should reflect typed value")
    }

    // MARK: - Profile Fields

    func testProfileSectionFieldsVisible() {
        openSettings()

        XCTAssertTrue(app.staticTexts["Height"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Weight"].exists)
        XCTAssertTrue(app.staticTexts["Age"].exists)
        XCTAssertTrue(app.staticTexts["Gender"].exists)
        XCTAssertTrue(app.staticTexts["Activity"].exists)
    }

    func testDailyGoalsSectionVisible() {
        openSettings()

        app.swipeUp()

        XCTAssertTrue(app.staticTexts["Target Calories"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["settings_calculateGoal"].exists)
    }

    // MARK: - Privacy Section

    func testPrivacySectionInfoText() {
        openSettings()

        app.swipeUp()

        let infoText = app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "food photos are sent to Google Gemini"))
        XCTAssertTrue(infoText.firstMatch.waitForExistence(timeout: 3))
    }

    func testPrivacyPolicyLinkNotInSettings() {
        openSettings()

        app.swipeUp()
        app.swipeUp()

        // Privacy Policy link should no longer be in Settings (moved to About)
        let privacyLink = app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "Privacy Policy"))
        XCTAssertFalse(privacyLink.firstMatch.waitForExistence(timeout: 2))
    }

    // MARK: - Profile with Seed Data

    func testSettingsLoadSeedProfileValues() {
        openSettingsWithSeedData()

        XCTAssertTrue(app.staticTexts["Male"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Moderately Active"].exists)
    }
}

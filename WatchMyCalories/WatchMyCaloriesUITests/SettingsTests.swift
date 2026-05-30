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

        // Wait for the discard dialog to appear
        let discardButton = app.buttons["Discard Changes"]
        XCTAssertTrue(discardButton.waitForExistence(timeout: 5))

        // "Keep Editing" has role: .cancel — find via predicate as the system
        // may render it differently in the action sheet
        let keepButton = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] 'Keep Editing'")).firstMatch
        if keepButton.waitForExistence(timeout: 3) {
            keepButton.tap()
        } else {
            // Fallback: tap outside the action sheet to dismiss (same as cancel)
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.3)).tap()
        }

        // Should still be on Settings
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))
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

    // MARK: - Parity audit (2026-05-30) — picker value-change coverage

    func testThemePickerCanChangeSelection() {
        openSettings()
        let themePicker = app.buttons["settings_themePicker"]
        XCTAssertTrue(themePicker.waitForExistence(timeout: 3))
        themePicker.tap()
        let lightOption = app.buttons["Light"]
        XCTAssertTrue(lightOption.waitForExistence(timeout: 3))
        lightOption.tap()
        // After selecting Light, the picker should reflect Light as the active value.
        XCTAssertTrue(themePicker.label.contains("Light"))
    }

    func testUnitPickerCanChangeSelection() {
        openSettings()
        let unitPicker = app.buttons["settings_unitPicker"]
        XCTAssertTrue(unitPicker.waitForExistence(timeout: 3))
        let initialLabel = unitPicker.label
        unitPicker.tap()
        let usOption = app.buttons["US Customary"]
        let metricOption = app.buttons["Metric"]
        let nextOption = initialLabel.contains("US") ? metricOption : usOption
        XCTAssertTrue(nextOption.waitForExistence(timeout: 3))
        nextOption.tap()
        let updatedLabel = unitPicker.label
        XCTAssertNotEqual(initialLabel, updatedLabel)
    }

    func testGenderPickerCanChangeSelection() {
        openSettings()
        let genderPicker = app.buttons["settings_genderPicker"]
        XCTAssertTrue(genderPicker.waitForExistence(timeout: 3))
        genderPicker.tap()
        let femaleOption = app.buttons["Female"]
        XCTAssertTrue(femaleOption.waitForExistence(timeout: 3))
        femaleOption.tap()
        XCTAssertTrue(genderPicker.label.contains("Female"))
    }

    func testActivityPickerCanChangeSelection() {
        openSettings()
        let activityPicker = app.buttons["settings_activityPicker"]
        XCTAssertTrue(activityPicker.waitForExistence(timeout: 3))
        activityPicker.tap()
        let veryActiveOption = app.buttons["Very Active"]
        XCTAssertTrue(veryActiveOption.waitForExistence(timeout: 3))
        veryActiveOption.tap()
        XCTAssertTrue(activityPicker.label.contains("Very Active"))
    }

    /// Force metric mode so Height/Weight render as DisclosureGroup + wheel
    /// (US Customary shows separate inline feet/inches pickers instead).
    private func switchToMetric() {
        let unitPicker = app.buttons["settings_unitPicker"]
        XCTAssertTrue(unitPicker.waitForExistence(timeout: 3))
        if !unitPicker.label.contains("Metric") {
            unitPicker.tap()
            let metricOption = app.buttons["Metric"]
            XCTAssertTrue(metricOption.waitForExistence(timeout: 3))
            metricOption.tap()
        }
    }

    func testHeightDisclosureGroupExpandsInMetricMode() {
        openSettings()
        switchToMetric()
        let heightLabel = app.staticTexts["Height"]
        XCTAssertTrue(heightLabel.waitForExistence(timeout: 3))
        heightLabel.tap()
        XCTAssertTrue(app.pickerWheels.firstMatch.waitForExistence(timeout: 2))
    }

    func testWeightDisclosureGroupExpandsInMetricMode() {
        openSettings()
        switchToMetric()
        let weightLabel = app.staticTexts["Weight"]
        XCTAssertTrue(weightLabel.waitForExistence(timeout: 3))
        weightLabel.tap()
        XCTAssertTrue(app.pickerWheels.firstMatch.waitForExistence(timeout: 2))
    }

    func testAgeDisclosureGroupExpands() {
        openSettings()
        let ageLabel = app.staticTexts["Age"]
        XCTAssertTrue(ageLabel.waitForExistence(timeout: 3))
        ageLabel.tap()
        XCTAssertTrue(app.pickerWheels.firstMatch.waitForExistence(timeout: 2))
    }

    func testHeightFeetAndInchesPickersExistInUSMode() {
        openSettings()
        let unitPicker = app.buttons["settings_unitPicker"]
        if unitPicker.label.contains("Metric") {
            unitPicker.tap()
            app.buttons["US Customary"].tap()
        }
        // US mode shows inline feet/inches pickers without a disclosure
        XCTAssertTrue(app.pickerWheels.count >= 1, "US mode should expose at least one inline picker wheel for height")
    }

    func testManagePrivacyChoicesButtonExists() {
        openSettings()
        app.swipeUp()
        app.swipeUp()
        // "Manage Privacy Choices" reveals the UMP form. We only assert presence,
        // not the form contents (the SDK presents a sheet that's not under our control).
        let privacyChoices = app.buttons.containing(NSPredicate(format: "label CONTAINS %@", "Privacy Choices")).firstMatch
        XCTAssertTrue(privacyChoices.waitForExistence(timeout: 3))
    }

    func testKeyboardDoneButtonDismissesKeyboard() {
        openSettings()
        app.swipeUp()
        let targetField = app.textFields["settings_targetCalories"]
        XCTAssertTrue(targetField.waitForExistence(timeout: 3))
        targetField.tap()
        // Verify keyboard is up by checking for the Return key
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 2))
        let doneButton = app.buttons["Done"]
        XCTAssertTrue(doneButton.waitForExistence(timeout: 2))
        doneButton.tap()
        // Keyboard should dismiss
        XCTAssertFalse(app.keyboards.firstMatch.waitForExistence(timeout: 2))
    }
}

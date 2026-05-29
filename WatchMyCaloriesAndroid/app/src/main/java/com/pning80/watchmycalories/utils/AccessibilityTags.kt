package com.pning80.watchmycalories.utils

/**
 * UI test identifiers shared with iOS for cross-platform UI-test reuse.
 *
 * Per PORTING_CRITERIA.md T2.4, this object must be a **strict superset** of
 * iOS `AccessibilityIdentifiers.swift`. Every iOS string value must appear here
 * (Android-only extras are allowed). The CI script `scripts/accessibility-diff.sh`
 * enforces this on every PR.
 *
 * When adding a tag here, also add the matching `testTag(...)` modifier to the
 * Compose component (or `Modifier.semantics { contentDescription = ... }` where
 * a description is more appropriate).
 */
object AccessibilityTags {
    object Common {
        const val BACK_BUTTON = "common_backButton"
        const val SAVE_BUTTON = "common_saveButton"
    }

    object Tab {
        const val DASHBOARD = "tab_dashboard"
        const val CAMERA = "tab_camera"
        const val HISTORY = "tab_history"
        const val SETTINGS = "tab_settings"
        const val SCAN_MENU = "tab_scanMenu"
    }

    object Dashboard {
        const val TITLE = "dashboard_title"                 // Android extra (UI-test convenience)
        const val EMPTY_STATE = "dashboard_emptyState"      // Android extra
        const val EMPTY_STATE_CARD = "dashboard_emptyStateCard"
        const val ADD_BUTTON = "dashboard_addButton"
        const val MANUAL_ENTRY_LINK = "dashboard_manualEntryLink"
        const val HERO_CARD = "dashboard_heroCard"
        const val CONSUMED_CALORIES = "dashboard_consumedCalories"
        const val MEAL_SECTION = "dashboard_mealSection"
        const val GOAL_VALUE = "dashboard_goalValue"
        const val REMAINING_VALUE = "dashboard_remainingValue"
    }

    object ManualEntry {
        const val FOOD_NAME = "manualEntry_foodName"
        const val CALORIES = "manualEntry_calories"
        const val QUANTITY = "manualEntry_quantity"
        const val MEAL_PICKER = "manualEntry_mealPicker"
        const val SAVE_BUTTON = "manualEntry_saveButton"
        const val CANCEL_BUTTON = "manualEntry_cancelButton"
    }

    object Settings {
        const val TITLE = "settings_title"                  // Android extra
        const val THEME_PICKER = "settings_themePicker"
        const val UNIT_PICKER = "settings_unitPicker"
        const val SAVE_BUTTON = "settings_saveButton"
        const val TARGET_CALORIES = "settings_targetCalories"
        const val CALCULATE_GOAL = "settings_calculateGoal"
        const val GENDER_PICKER = "settings_genderPicker"
        const val ACTIVITY_PICKER = "settings_activityPicker"
        const val AI_CONSENT_TOGGLE = "settings_aiConsentToggle"
    }

    object History {
        const val TITLE = "history_title"                   // Android extra
        const val DAY_CARD = "history_dayCard"
        const val EMPTY_STATE = "history_emptyState"
        const val DAY_CARD_MACROS = "history_dayCard_macros"
    }

    object PhotoLibrary {
        const val CHOOSE_AGAIN_BUTTON = "photoLibrary_chooseAgainButton"
        const val USE_PHOTO_BUTTON = "photoLibrary_usePhotoButton"
    }

    object Camera {
        const val CAPTURE_BUTTON = "camera_captureButton"
        const val TOGGLE_FLASH = "camera_toggleFlash"        // Android extra
        const val RETAKE_BUTTON = "camera_retakeButton"
        const val USE_PHOTO_BUTTON = "camera_usePhotoButton"
    }

    object Onboarding {
        const val SKIP_BUTTON = "onboarding_skipButton"
        const val GET_STARTED_BUTTON = "onboarding_getStartedButton"
        const val NEXT_BUTTON = "onboarding_nextButton"
        const val FINISH_BUTTON = "onboarding_finishButton"
        const val CALCULATE_GOAL_BUTTON = "onboarding_calculateGoal"
        const val TARGET_CALORIES_FIELD = "onboarding_targetCalories"
        const val CONNECT_HEALTH_BUTTON = "onboarding_connectHealth"
        const val AI_CONSENT_TOGGLE = "onboarding_aiConsentToggle"
    }

    object MealTypePicker {
        const val PICKER = "mealTypePicker"
    }

    object AppMenu {
        const val MENU_BUTTON = "appMenu_button"
    }

    object About {
        const val VERSION_LABEL = "about_versionLabel"
        const val HELP_AND_SUPPORT = "about_helpAndSupport"
        const val PRIVACY_POLICY = "about_privacyPolicy"
        const val RATE_ON_APP_STORE = "about_rateOnAppStore"
    }

    object Disclaimer {
        const val DONT_SHOW_TOGGLE = "disclaimer_dontShowToggle"
        const val CONTINUE_BUTTON = "disclaimer_continueButton"
    }

    object ScanMenuSheet {
        const val SCAN_BUTTON = "scanMenuSheet_scan"
        const val CHOOSE_FROM_LIBRARY_BUTTON = "scanMenuSheet_chooseFromLibrary"
        const val STORED_MENUS_BUTTON = "scanMenuSheet_storedMenus"
    }

    object ScannedMenus {
        const val EMPTY_STATE = "scannedMenus_emptyState"
    }

    object Ads {
        const val BANNER = "ads_banner"
        const val NATIVE = "ads_native"
        const val VIEW_RESULTS_BUTTON = "ads_viewResultsButton"
    }

    object EstimationReview {
        const val LOADING_VIEW = "review_loading"
        const val ERROR_VIEW = "review_error"
        const val SUCCESS_VIEW = "review_success"
        const val NO_FOOD_VIEW = "review_noFood"
        const val DONE_BUTTON = "review_doneButton"
        const val TRY_AGAIN_BUTTON = "review_tryAgainButton"
        const val CANCEL_BUTTON = "review_cancelButton"
    }
}

import Foundation

enum AccessibilityID {

    enum Tab {
        static let dashboard = "tab_dashboard"
        static let camera = "tab_camera"
        static let history = "tab_history"
        static let settings = "tab_settings"
    }

    enum Dashboard {
        static let addButton = "dashboard_addButton"
        static let emptyStateCard = "dashboard_emptyStateCard"
        static let manualEntryLink = "dashboard_manualEntryLink"
        static let heroCard = "dashboard_heroCard"
        static let consumedCalories = "dashboard_consumedCalories"
        static let mealSection = "dashboard_mealSection"
        static let goalValue = "dashboard_goalValue"
        static let remainingValue = "dashboard_remainingValue"
    }

    enum ManualEntry {
        static let foodName = "manualEntry_foodName"
        static let calories = "manualEntry_calories"
        static let quantity = "manualEntry_quantity"
        static let mealPicker = "manualEntry_mealPicker"
        static let saveButton = "manualEntry_saveButton"
        static let cancelButton = "manualEntry_cancelButton"
        static let scanButton = "manualEntry_scanButton"
        static let photoLibraryButton = "manualEntry_photoLibraryButton"
    }

    enum Settings {
        static let saveButton = "settings_saveButton"
        static let targetCalories = "settings_targetCalories"
        static let calculateGoal = "settings_calculateGoal"
        static let genderPicker = "settings_genderPicker"
        static let activityPicker = "settings_activityPicker"
        static let aiConsentToggle = "settings_aiConsentToggle"
        static let themePicker = "settings_themePicker"
        static let unitPicker = "settings_unitPicker"
    }

    enum History {
        static let title = "history_title"
        static let emptyState = "history_emptyState"
        static let dayCard = "history_dayCard"
    }

    enum PhotoLibrary {
        static let chooseAgainButton = "photoLibrary_chooseAgainButton"
        static let usePhotoButton = "photoLibrary_usePhotoButton"
    }

    enum Camera {
        static let captureButton = "camera_captureButton"
        static let retakeButton = "camera_retakeButton"
        static let usePhotoButton = "camera_usePhotoButton"
    }

    enum Onboarding {
        static let skipButton = "onboarding_skipButton"
        static let getStartedButton = "onboarding_getStartedButton"
        static let nextButton = "onboarding_nextButton"
        static let finishButton = "onboarding_finishButton"
        static let calculateGoalButton = "onboarding_calculateGoal"
        static let targetCaloriesField = "onboarding_targetCalories"
        static let connectHealthButton = "onboarding_connectHealth"
        static let aiConsentToggle = "onboarding_aiConsentToggle"
    }

    enum Ads {
        static let banner = "ads_banner"
        static let native = "ads_native"
        static let viewResultsButton = "ads_viewResultsButton"
    }

    enum EstimationReview {
        static let loadingView = "review_loading"
        static let errorView = "review_error"
        static let successView = "review_success"
        static let noFoodView = "review_noFood"
        static let doneButton = "review_doneButton"
        static let tryAgainButton = "review_tryAgainButton"
        static let cancelButton = "review_cancelButton"
    }
}

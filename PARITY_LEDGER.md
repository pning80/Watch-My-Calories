# Parity Ledger — iOS ↔ Android UI Test Coverage

**Goal:** every iOS interactive surface has an iOS XCUITest function (the spec) AND a corresponding Android instrumented test on Pixel 9a (the verification). Robolectric counts as WEAKER-EVIDENCE per the strict bar set 2026-05-30.

**Status legend:**
- `iOS spec` column: `✅ <TestFile.testName>` (covered) · `❌ SPEC-GAP` (missing iOS test) · `n/a` (display-only)
- `Android verify` column: `✅ instr <TestFile.testName>` · `⚠ robo-only <TestFile.testName>` · `❌ GAP` · `n/a`
- `Inconsistency` column: `OK` · `GAP` · `FAIL` · `DRIFT` · `D-NNN` (intentional deviation)

**Baselines (2026-05-30):**
- iOS XCUITest suite: running on iPhone 16 Pro · iOS 18.5 sim (results in `/tmp/parity-audit/ios-baseline.xcresult`)
- Android instrumented: 11 tests passed on Pixel 9a (Android 16). See `/tmp/parity-audit/android-baseline.log`.

---

## DashboardView.swift

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | App Menu Button | Menu | `appMenu_button` | tap | open menu (Settings/About) | | | |
| 2 | Empty State Card | Button | `dashboard_emptyStateCard` | tap | trigger onLogFood | | | |
| 3 | Manual Entry Link | Button | `dashboard_manualEntryLink` | tap | trigger onLogFood | | | |
| 4 | HeroSummaryCard – Consumed Calories | View | `dashboard_consumedCalories` | none | display only | | | n/a |
| 5 | Goal Value | StatRow | `dashboard_goalValue` | none | display only | | | n/a |
| 6 | Remaining Value | StatRow | `dashboard_remainingValue` | none | display only | | | n/a |
| 7 | Meal Section header | View | `dashboard_mealSection` | none | display only | | | n/a |
| 8 | Food Entry Group Card – summary row | Button | (none) | tap | expand/collapse group | | | |
| 9 | Food Entry Group Card – thumbnail | Button | (none) | tap | show full screen image | | | |
| 10 | Food Entry Group Card – context menu (multi-item) | ContextMenu | (none) | long-press | View / Edit / Delete group | | | |
| 11 | Food Entry Group Card – context menu (single item) | ContextMenu | (none) | long-press | View / Edit / Delete item | | | |
| 12 | Food Entry Group Card – sub-item row context menu | ContextMenu | (none) | long-press | View / Edit / Delete sub-item | | | |
| 13 | Pull-to-refresh | Gesture | (none) | pull-down | refresh HealthKit data | | | |
| 14 | Full-screen image cover (after thumbnail tap) | View | (none) | tap | display full-screen image | | | |

## HistoryView.swift

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | History title | View | `history_title` | none | display only | | | n/a |
| 2 | App Menu Button | Menu | `appMenu_button` | tap | open menu | | | |
| 3 | Empty state card | Button | `history_emptyState` | tap | trigger onLogFood | | | |
| 4 | History Day Card – header | Button | `history_dayCard` | tap | expand/collapse day | | | |
| 5 | History Day Card – macro row | View | `history_dayCard_macros` | none | display only | | | n/a |
| 6 | Food Entry Group Card – summary row | Button | (none) | tap | expand/collapse | | | |
| 7 | Food Entry Group Card – thumbnail | Button | (none) | tap | full screen image | | | |
| 8 | Food Entry Group Card – context menu (multi-item) | ContextMenu | (none) | long-press | View / Edit / Delete group | | | |
| 9 | Food Entry Group Card – context menu (single item) | ContextMenu | (none) | long-press | View / Edit / Delete item | | | |
| 10 | Food Entry Group Card – sub-item row context menu | ContextMenu | (none) | long-press | View / Edit / Delete sub-item | | | |
| 11 | Full-screen image cover | View | (none) | tap | display full-screen image | | | |

## SettingsView.swift

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | Theme picker | Picker | `settings_themePicker` | tap | change theme | | | |
| 2 | Unit System picker | Picker | `settings_unitPicker` | tap | change unit system | | | |
| 3 | Height (US) – Feet picker | Picker | (none) | tap | adjust feet | | | |
| 4 | Height (US) – Inches picker | Picker | (none) | tap | adjust inches | | | |
| 5 | Weight (US) DisclosureGroup | DisclosureGroup | (none) | tap | expand weight picker | | | |
| 6 | Weight (US) wheel | Picker | (none) | tap/swipe | select weight | | | |
| 7 | Height (Metric) DisclosureGroup | DisclosureGroup | (none) | tap | expand height picker | | | |
| 8 | Height (Metric) wheel | Picker | (none) | tap/swipe | select height | | | |
| 9 | Weight (Metric) DisclosureGroup | DisclosureGroup | (none) | tap | expand weight picker | | | |
| 10 | Weight (Metric) wheel | Picker | (none) | tap/swipe | select weight | | | |
| 11 | Age DisclosureGroup | DisclosureGroup | (none) | tap | expand age picker | | | |
| 12 | Age wheel | Picker | (none) | tap/swipe | select age | | | |
| 13 | Gender picker | Picker | `settings_genderPicker` | tap | select gender | | | |
| 14 | Activity Level picker | Picker | `settings_activityPicker` | tap | select activity level | | | |
| 15 | Target Calories TextField | TextField | `settings_targetCalories` | tap | enter target | | | |
| 16 | Calculate Recommended Goal button | Button | `settings_calculateGoal` | tap | compute recommended | | | |
| 17 | AI Photo Analysis toggle | Toggle | `settings_aiConsentToggle` | tap | enable/disable AI | | | |
| 18 | Manage Privacy Choices button | Button | (none) | tap | present privacy form | | | |
| 19 | Cancel button (toolbar) | Button | (none) | tap | dismiss / show discard alert | | | |
| 20 | Save button (toolbar) | Button | `settings_saveButton` | tap | save settings | | | |
| 21 | Done button (keyboard) | Button | (none) | tap | dismiss keyboard | | | |
| 22 | Discard alert – Discard Changes | Button | (none) | tap | discard + dismiss | | | |
| 23 | Discard alert – Keep Editing | Button | (none) | tap | stay in form | | | |

## OnboardingView.swift

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | Skip button (top-right) | Button | `onboarding_skipButton` | tap | skip onboarding | | | |
| 2 | Get Started button (step 0) | Button | `onboarding_getStartedButton` | tap | step 0 → 1 | | | |
| 3 | Height (US) Feet picker | Picker | (none) | tap | select feet | | | |
| 4 | Height (US) Inches picker | Picker | (none) | tap | select inches | | | |
| 5 | Weight (US) picker | Picker | (none) | tap/swipe | select weight | | | |
| 6 | Height (Metric) picker | Picker | (none) | tap/swipe | select height | | | |
| 7 | Weight (Metric) picker | Picker | (none) | tap/swipe | select weight | | | |
| 8 | Age picker | Picker | (none) | tap/swipe | select age | | | |
| 9 | Gender picker | Picker | (none) | tap | select gender | | | |
| 10 | Activity Level picker | Picker | (none) | tap | select activity level | | | |
| 11 | Target Calories TextField | TextField | `onboarding_targetCalories` | tap | enter target | | | |
| 12 | Calculate Recommended Goal button | Button | `onboarding_calculateGoal` | tap | compute recommended | | | |
| 13 | AI Photo Analysis toggle (step 1) | Toggle | `onboarding_aiConsentToggle` | tap | enable/disable AI | | | |
| 14 | Connect Apple Health button | Button | `onboarding_connectHealth` | tap | request HealthKit auth | | | |
| 15 | Next button (step 1) | Button | `onboarding_nextButton` | tap | step 1 → 2 | | | |
| 16 | Start Tracking button (step 2) | Button | `onboarding_finishButton` | tap | finish onboarding | | | |
| 17 | Keyboard Done button | Button | (none) | tap | dismiss keyboard | | | |

## CameraView.swift (food capture)

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | Camera preview | UIViewRepresentable | (none) | none | display feed | | | n/a |
| 2 | Capture button | Button | `camera_captureButton` | tap | take photo | | | |
| 3 | MealTypePicker – Breakfast | Button | `mealTypePicker` | tap | select breakfast | | | |
| 4 | MealTypePicker – Lunch | Button | `mealTypePicker` | tap | select lunch | | | |
| 5 | MealTypePicker – Dinner | Button | `mealTypePicker` | tap | select dinner | | | |
| 6 | MealTypePicker – Snack | Button | `mealTypePicker` | tap | select snack | | | |
| 7 | Retake button | Button | `camera_retakeButton` | tap | reset photo | | | |
| 8 | Use Photo button | Button | `camera_usePhotoButton` | tap | confirm + proceed | | | |
| 9 | Open Settings button (cam denied) | Button | (none) | tap | open device Settings | | | |
| 10 | Calorie Disclaimer – Continue | Button | (none) | tap | acknowledge | | | |
| 11 | Calorie Disclaimer – Don't Show toggle | Toggle | `disclaimer_dontShowToggle` | tap | suppress future | | | |
| 12 | Camera Alert – OK | Button | (none) | tap | dismiss alert | | | |

## MenuCameraView.swift

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | Camera preview | UIViewRepresentable | (none) | none | display feed | | | n/a |
| 2 | Capture button | Button | (none) | tap | take photo | | | |
| 3 | Retake button | Button | (none) | tap | reset photo | | | |
| 4 | Analyze Menu button | Button | (none) | tap | analyze | | | |
| 5 | Open Settings button (cam denied) | Button | (none) | tap | open device Settings | | | |
| 6 | Calorie Disclaimer – Continue | Button | (none) | tap | acknowledge | | | |
| 7 | Calorie Disclaimer – Don't Show toggle | Toggle | `disclaimer_dontShowToggle` | tap | suppress future | | | |
| 8 | Camera Alert – OK | Button | (none) | tap | dismiss alert | | | |
| 9 | Menu Photo Library picker (PhotosUI) | PhotosPicker | (none) | tap | open library | | | |
| 10 | Menu Photo – Reselect | Button | (none) | tap | back to picker | | | |
| 11 | Menu Photo – Use | Button | (none) | tap | analyze | | | |

## EstimationReviewView.swift

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | Native ad | NativeAdView | `ads_native` | tap | open ad URL | | | |
| 2 | Show/Hide Details (error) | Button | (none) | tap | toggle error details | | | |
| 3 | Try Again (error) | Button | `review_tryAgainButton` | tap | retry | | | |
| 4 | Cancel (error) | Button | `review_cancelButton` | tap | dismiss | | | |
| 5 | View Results (complete) | Button | `ads_viewResultsButton` | tap | show results | | | |
| 6 | Try Again (no-food) | Button | `review_tryAgainButton` | tap | retry | | | |
| 7 | Cancel (no-food) | Button | `review_cancelButton` | tap | dismiss | | | |
| 8 | Done (success) | Button | `review_doneButton` | tap | save + dismiss | | | |
| 9 | AI Consent – Accept | Button | (none) | tap | enable AI + estimate | | | |
| 10 | AI Consent – Decline | Button | (none) | tap | disable AI + dismiss | | | |

## MenuAnalysisView.swift

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | Native ad | NativeAdView | `ads_native` | tap | open ad URL | | | |
| 2 | Show Details (error) | Button | (none) | tap | toggle error details | | | |
| 3 | Try Again (error) | Button | (none) | tap | retry | | | |
| 4 | Cancel (error) | Button | (none) | tap | dismiss | | | |
| 5 | Scan Again (success) | Button | (none) | tap | back to camera | | | |
| 6 | Done (success) | Button | (none) | tap | dismiss | | | |
| 7 | Try Again (not-a-menu) | Button | (none) | tap | back to camera | | | |
| 8 | AI Consent – Accept | Button | (none) | tap | enable AI + analyze | | | |
| 9 | AI Consent – Decline | Button | (none) | tap | disable AI + dismiss | | | |

## ScannedMenusView.swift

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | Empty state (no scans) | View | `scannedMenus_emptyState` | none | display only | | | n/a |
| 2 | Edit button (toolbar) | EditButton | (none) | tap | toggle edit mode | | | |
| 3 | Scanned menu row | NavigationLink | (none) | tap | view detail | | | |
| 4 | Scanned menu row – swipe delete | SwipeAction | (none) | swipe-left | delete scan | | | |
| 5 | Menu Scan Detail – photo | Button | (none) | tap | full-screen image | | | |
| 6 | Menu Scan Detail – Delete (toolbar) | Button | (none) | tap | confirm delete | | | |
| 7 | Menu Scan Detail – Delete confirm | Button | (none) | tap | confirm + delete | | | |

## PhotoLibraryReviewView.swift

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | PhotosPicker | PhotosPicker | (none) | tap | open library | | | |
| 2 | MealTypePicker – Breakfast | Button | `mealTypePicker` | tap | select breakfast | | | |
| 3 | MealTypePicker – Lunch | Button | `mealTypePicker` | tap | select lunch | | | |
| 4 | MealTypePicker – Dinner | Button | `mealTypePicker` | tap | select dinner | | | |
| 5 | MealTypePicker – Snack | Button | `mealTypePicker` | tap | select snack | | | |
| 6 | Reselect | Button | `photoLibrary_chooseAgainButton` | tap | back to picker | | | |
| 7 | Use Photo | Button | `photoLibrary_usePhotoButton` | tap | confirm + estimate | | | |
| 8 | Calorie Disclaimer – Continue | Button | `disclaimer_continueButton` | tap | acknowledge | | | |
| 9 | Calorie Disclaimer – Don't Show toggle | Toggle | `disclaimer_dontShowToggle` | tap | suppress future | | | |

## LogFoodSheet.swift

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | Scan Food | Button | (none) | tap | onScanFood | | | |
| 2 | Choose from Library | Button | (none) | tap | onChooseFromLibrary | | | |
| 3 | Log Manually | Button | (none) | tap | onLogManually | | | |

## ScanMenuSheet.swift

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | Scan Menu | Button | `scanMenuSheet_scan` | tap | onScanMenu | | | |
| 2 | Choose from Library | Button | `scanMenuSheet_chooseFromLibrary` | tap | onChooseFromLibrary | | | |
| 3 | Stored Menus | Button | `scanMenuSheet_storedMenus` | tap | onStoredMenus | | | |

## AboutView.swift

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | Version label (copy) | Button | `about_versionLabel` | tap | copy version to clipboard | | | |
| 2 | Rate on App Store | Button | `about_rateOnAppStore` | tap | open review prompt | | | |
| 3 | Help & Support | Link | `about_helpAndSupport` | tap | open help URL | | | |
| 4 | Privacy Policy | Link | `about_privacyPolicy` | tap | open privacy URL | | | |

## ContentView.swift (Tab navigation)

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | Dashboard tab | TabItem | `tab_dashboard` | tap | navigate to dashboard | | | |
| 2 | Log Food tab (intercepted) | TabItem | (none) | tap | show LogFoodSheet | | | |
| 3 | Scan Menu tab (intercepted) | TabItem | (none) | tap | show ScanMenuSheet | | | |
| 4 | History tab | TabItem | `tab_history` | tap | navigate to history | | | |
| 5 | Camera root – Cancel | Button | (none) | tap | dismiss camera | | | |
| 6 | Menu Camera root – Cancel | Button | (none) | tap | dismiss menu camera | | | |
| 7 | Scanned Menus – Done | Button | (none) | tap | dismiss list | | | |

## ManualEntryView (presented from LogFoodSheet)

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | Food Name TextField | TextField | `manualEntry_foodName` | tap | enter food name | | | |
| 2 | Calories TextField | TextField | `manualEntry_calories` | tap | enter calories | | | |
| 3 | Quantity TextField | TextField | `manualEntry_quantity` | tap | enter quantity | | | |
| 4 | Meal Type picker | Picker | `manualEntry_mealPicker` | tap | select meal type | | | |
| 5 | Protein TextField (optional) | TextField | (none) | tap | enter protein g | | | |
| 6 | Carbs TextField (optional) | TextField | (none) | tap | enter carbs g | | | |
| 7 | Fat TextField (optional) | TextField | (none) | tap | enter fat g | | | |
| 8 | Nutrition DisclosureGroup | DisclosureGroup | (none) | tap | expand/collapse | | | |
| 9 | Cancel (toolbar) | Button | `manualEntry_cancelButton` | tap | dismiss | | | |
| 10 | Save (toolbar) | Button | `manualEntry_saveButton` | tap | save + dismiss | | | |
| 11 | Done (keyboard) | Button | (none) | tap | dismiss keyboard | | | |

## Components.swift / SharedComponents

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | FullScreenImageView – pinch | MagnifyGesture | (none) | pinch | zoom | | | |
| 2 | FullScreenImageView – drag | DragGesture | (none) | drag | pan zoomed | | | |
| 3 | FullScreenImageView – double-tap | TapGesture | (none) | double-tap | toggle 3× zoom | | | |
| 4 | FullScreenImageView – Close | Button | (none) | tap | dismiss | | | |
| 5 | FoodEntryCard – thumbnail | Button | (none) | tap | full screen | | | |
| 6 | FoodEntryCard – context menu | ContextMenu | (none) | long-press | View/Edit/Delete | | | |
| 7 | FoodEntryGroupCard – summary row | Button | (none) | tap | expand/collapse | | | |
| 8 | FoodEntryGroupCard – thumbnail | Button | (none) | tap | full screen | | | |
| 9 | FoodEntryGroupCard – ctx menu (group) | ContextMenu | (none) | long-press | View/Edit/Delete group | | | |
| 10 | FoodEntryGroupCard – ctx menu (item) | ContextMenu | (none) | long-press | View/Edit/Delete item | | | |
| 11 | FoodEntryGroupCard – sub-item ctx menu | ContextMenu | (none) | long-press | View/Edit/Delete sub-item | | | |
| 12 | EditFoodEntryView – Food Name TextField | TextField | (none) | tap | edit food name | | | |
| 13 | EditFoodEntryView – Meal Name TextField | TextField | (none) | tap | edit meal name | | | |
| 14 | EditFoodEntryView – Calories TextField | TextField | (none) | tap | edit calories | | | |
| 15 | EditFoodEntryView – Quantity TextField | TextField | (none) | tap | edit quantity | | | |
| 16 | EditFoodEntryView – Meal Type Picker | Picker | (none) | tap | select meal type | | | |
| 17 | EditFoodEntryView – Nutrition DisclosureGroup | DisclosureGroup | (none) | tap | expand/collapse | | | |
| 18 | EditFoodEntryView – Cancel | Button | (none) | tap | dismiss | | | |
| 19 | EditFoodEntryView – Save | Button | (none) | tap | save | | | |
| 20 | EditMealGroupView – Meal Name TextField | TextField | (none) | tap | edit meal name | | | |
| 21 | EditMealGroupView – Meal Type Picker | Picker | (none) | tap | select meal type | | | |
| 22 | EditMealGroupView – Item Name TextField | TextField | (none) | tap | edit item name | | | |
| 23 | EditMealGroupView – Item Calories TextField | TextField | (none) | tap | edit calories | | | |
| 24 | EditMealGroupView – Item Quantity TextField | TextField | (none) | tap | edit quantity | | | |
| 25 | EditMealGroupView – Item Nutrition DisclosureGroup | DisclosureGroup | (none) | tap | expand/collapse | | | |
| 26 | EditMealGroupView – Cancel | Button | (none) | tap | dismiss | | | |
| 27 | EditMealGroupView – Save | Button | (none) | tap | save all | | | |
| 28 | ViewFoodEntryView – Done | Button | (none) | tap | dismiss | | | |
| 29 | ViewMealGroupView – Done | Button | (none) | tap | dismiss | | | |
| 30 | AIConsentSheet – Allow | Button | (none) | tap | accept | | | |
| 31 | AIConsentSheet – Don't Allow | Button | (none) | tap | decline | | | |

## AppMenu / Toolbar

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | Menu button | Menu | `appMenu_button` | tap | open menu | | | |
| 2 | Settings menu item | Button | (none) | tap | show SettingsView | | | |
| 3 | About menu item | Button | (none) | tap | show AboutView | | | |
| 4 | About sheet – Done | Button | (none) | tap | dismiss About | | | |

## BannerAdView.swift / NativeAdView.swift

| # | Element | Type | iOS A11y ID | Trigger | What it does | iOS spec | Android verify | Inconsistency |
|---|---|---|---|---|---|---|---|---|
| 1 | Banner Ad | BannerView | `ads_banner` | tap | open ad URL | | | |
| 2 | Native Ad container | NativeAdView | `ads_native` | tap | open ad URL | | | |
| 3 | Native Ad – CTA button | Button | (none) | tap | open ad URL | | | |

---

## Coverage roll-up (to be filled in Task #4 and Task #6)

Pending: count of OK / GAP / FAIL / DRIFT / D-NNN rows per screen and totals.

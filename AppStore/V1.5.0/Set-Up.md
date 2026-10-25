# App Store Connect Update Setup — Watch My Calories v1.5.0

**Date: 2026-10-25**

All the content you need to fill in App Store Connect for your version update.

---

## 1. App Information

| Field | Value |
|-------|-------|
| **App Name** | Watch My Calories |
| **Subtitle** | Snap, Track, Stay Fit with AI |
| **Bundle ID** | `com.pning80.WatchMyCalories` |
| **SKU** | `watch-my-calories-ios-001` |
| **Primary Category** | Health & Fitness |
| **Secondary Category** | Food & Drink |
| **Primary Language** | English (U.S.) |
| **Copyright** | © 2026 pning80 |

### Content Rights

- Does your app contain, show, or access third-party content? **Yes**

> Google AdMob serves third-party advertisements within the App. Select **Yes** and confirm you have the rights to distribute this content (covered by the AdMob Terms of Service).

### Age Rating Questionnaire

Answer **None** / **No** to all questions:

| Question | Answer |
|----------|--------|
| Cartoon or Fantasy Violence | None |
| Realistic Violence | None |
| Prolonged Graphic or Sadistic Realistic Violence | None |
| Profanity or Crude Humor | None |
| Mature/Suggestive Themes | None |
| Horror/Fear Themes | None |
| Medical/Treatment Information | None |
| Simulated Gambling | None |
| Unrestricted Web Access | No |
| Gambling with Real Currency | No |

**Expected Rating**: 4+ (all ages)

---

## 2. Version Information

### What's New in This Version

> A fresh new look! History now shows a calorie trend chart for the last 7 or 30 days, with your daily average, days on target, and logging streak at a glance.
>
> Also in this update:
> - Refreshed design across the Dashboard, History, and scan results
> - Improved VoiceOver support for charts and the scan results screen
> - Fixed an issue on iOS 27 where Log Food and Scan Menu could show a blank screen
> - Added a Cancel button when reviewing a photo from your library

### App Description

> Track your daily calories effortlessly with AI-powered food recognition. Simply snap a photo of your meal — or choose one from your photo library — and Watch My Calories instantly identifies each food item, estimates portions, and calculates calories and macronutrients. You can also scan restaurant menus to see calorie estimates for every dish before you order.
>
> KEY FEATURES
>
> - AI Food Analysis — Snap a photo of your meal or choose one from your photo library. Watch My Calories uses Google Gemini AI to identify foods and estimate calories, protein, carbs, and fat.
> - Menu Scanning — Take a photo of a restaurant menu to see AI calorie estimates for every item. Your approximate location helps identify the restaurant's cuisine for more accurate results. Scanned menus are saved automatically.
> - Smart Meal Tracking — Entries are automatically categorized as Breakfast, Lunch, Dinner, or Snack based on the time of day.
> - Daily Dashboard — See your calorie intake at a glance with a nutrition hero card showing calories consumed, remaining, and a protein/carbs/fat breakdown.
> - HealthKit Integration — Reads your active energy burned from Apple Health to dynamically adjust your effective calorie target.
> - Personalized Goals — Set your profile (height, weight, age, activity level) and the app calculates a recommended daily calorie target using the Mifflin-St Jeor formula.
> - Meal History & Trends — See a 7- or 30-day calorie trend chart with your daily average, days on target, and logging streak. Browse all past entries organized by date and meal type, with daily nutrition summaries. View, edit, or delete individual items or entire meal groups.
> - Manual Entry — Prefer to log food yourself? Add entries manually with full control over name, calories, and macros.
> - Privacy First — All your food and health data stays on your device. No accounts, no sign-ups. Food and menu photos are sent to Google Gemini solely for analysis and are not stored. The app is supported by ads served via Google AdMob.
>
> Watch My Calories is designed for anyone who wants a simple, private, and intelligent way to stay on top of their nutrition.

### Promotional Text

> Snap a photo of a meal or restaurant menu — AI estimates calories and macros for every item. Now with a fresh design and calorie trends in History. Private and on-device.

*(Promotional text can be updated without a new app version.)*

### Keywords

```
calorie,tracker,food,AI,nutrition,macro,health,diet,meal,trend,menu,restaurant,scan,protein
```

*(100 characters max, comma-separated, no spaces after commas.)*

*Change from v1.4.1: `camera` → `trend` ("camera" is implied by every calorie-scanner listing; "trend" covers the new History chart). Optional — revert if you prefer.*

### Support URL

> https://gist.github.com/pning80/7dc8a85c83edcc03845d182386cab470

### Marketing URL

> **https://pning80.github.io**
> *(CRITICAL FOR ADMOB: This domain hosts your `app-ads.txt` file at the root. Do not change this unless you move ad hosting.)*

---

## 3. Screenshots & App Previews

The v1.5.0 screenshot folder is at `AppStore/V1.5.0/Screenshots/`.

> **Every screen changed in the v1.5.0 design overhaul — all 10 screenshots must be replaced.** The v1.4.1 set is 6.5" (1242 × 2688). v1.5.0 targets **6.9" (1320 × 2868)**.

### Format & Limits

- **Formats**: `.jpeg`, `.jpg`, `.png`
- **Quantity**: 1–10 screenshots per localized listing
- **App previews** (optional): Up to 3 per localization, 15–30 seconds, `.mov`/`.mp4`/`.m4v`, max 500 MB. Dimensions must match the corresponding screenshot size for that device.

### Required Screenshot Sizes

You only need to provide screenshots for the **largest device size** per device family. App Store Connect automatically scales them down for smaller sizes.

#### iPhone (required — provide one of these)

| Display | Devices | Portrait (px) | Landscape (px) |
|---------|---------|---------------|-----------------|
| **6.9"** | iPhone 16 Pro Max | 1320 × 2868 | 2868 × 1320 |
| **6.5"** | iPhone XS Max, 11 Pro Max | 1242 × 2688 | 2688 × 1242 |

> Either 6.9" or 6.5" is required. Apple auto-scales for 6.7", 6.1", 5.8", 5.5", and smaller.

#### iPhone (optional — only if you want device-specific screenshots)

| Display | Devices | Portrait (px) | Landscape (px) |
|---------|---------|---------------|-----------------|
| **6.7"** | iPhone 16 Plus, 15 Pro Max, 15 Plus, 14 Pro Max | 1290 × 2796 | 2796 × 1290 |
| **6.1"** | iPhone XR, 11 | 828 × 1792 | 1792 × 828 |
| **5.8"** | iPhone X, XS, 11 Pro | 1125 × 2436 | 2436 × 1125 |
| **5.5"** | iPhone 8 Plus, 7 Plus, 6s Plus | 1242 × 2208 | 2208 × 1242 |
| **4.7"** | iPhone 8, 7, 6s, SE (3rd/2nd gen) | 750 × 1334 | 1334 × 750 |
| **4"** | iPhone SE (1st gen), 5s | 640 × 1136 | 1136 × 640 |

### Suggested Screenshots

Capture all 10 manually on the **iPhone 18 Pro Max simulator (iOS 27)**, which renders at exactly 1320 × 2868 (6.9"). Save them to `AppStore/V1.5.0/Screenshots/`.

| Screenshot | Notes |
|------------|-------|
| SS-1 Camera scan food | Camera view for food scanning |
| SS-2 Food scan results | Success screen: per-item macros + total card |
| SS-3 Dashboard | Hero ring card + meal sections |
| SS-4 Camera scan menu | Camera view for menu scanning |
| SS-5 Menu scan results | Menu items with calorie estimates |
| SS-6 Pick from library | Photo library review flow |
| SS-7 Entry menu | Log Food sheet (Scan Food / Choose from Library / Log Manually) |
| SS-8 Meal history | **New:** trend chart + Avg/day, On target, Day streak — consider making this the 2nd screenshot |
| SS-9 Meal info | View Meal: meal name, type, and per-item calories / macros |
| SS-10 Settings | Profile, calorie goal, privacy choices |

**Tips:**
- Light mode screenshots are generally preferred as lead images for the App Store listing.
- Before capturing on a simulator, clean the status bar: `xcrun simctl status_bar booted override --time 9:41 --batteryState charged --batteryLevel 100 --cellularBars 4 --wifiBars 3`.
- Simulator drafts come from a DEBUG/UI-test build. Check each for test ad creative or test-only content before uploading.

- [x] All 10 captured on the iPhone 18 Pro Max simulator (iOS 27), 1320 × 2868, test-ad banners kept as in v1.4.1 (2026-10-25)
- [ ] Upload all 10 at 6.9" (1320 × 2868)

*Reference: [Apple Screenshot Specifications](https://developer.apple.com/help/app-store-connect/reference/screenshot-specifications/)*

---

## 4. Privacy Details (App Privacy "Nutrition Labels")

No changes from v1.4.1. `PrivacyInfo.xcprivacy` and the data model are unchanged, and the History trend is computed on-device from existing entries. All privacy labels carry over automatically.

App Store Connect requires you to disclose all data your app collects. Fill in as follows:

### Data Types Collected

#### 1. Photos or Videos

| Field | Value |
|-------|-------|
| **Data Type** | Photos |
| **Collection** | Yes |
| **Usage Purpose** | App Functionality |
| **Linked to User's Identity** | No |
| **Used for Tracking** | No |

*Note: Food and menu photos are sent to Google Gemini AI for analysis. Only the first photo per meal is stored locally on-device. Photos are not stored on the server. Users can also choose photos from their photo library for analysis.*

#### 2. Device ID (collected by Google AdMob SDK)

| Field | Value |
|-------|-------|
| **Data Type** | Identifiers — Device ID |
| **Collection** | Yes |
| **Usage Purpose** | Third-Party Advertising |
| **Linked to User's Identity** | No |
| **Used for Tracking** | No |

*Note: Google AdMob may collect device identifiers for ad serving and measurement. This data is handled by Google's SDK and is not accessed by the app itself.*

#### 3. Advertising Data (collected by Google AdMob SDK)

| Field | Value |
|-------|-------|
| **Data Type** | Usage Data — Advertising Data |
| **Collection** | Yes |
| **Usage Purpose** | Third-Party Advertising |
| **Linked to User's Identity** | No |
| **Used for Tracking** | No |

*Note: Ad interaction data (impressions, taps) collected by the AdMob SDK for ad performance measurement.*

#### 4. Diagnostics (collected by Google AdMob SDK)

| Field | Value |
|-------|-------|
| **Data Type** | Diagnostics — Performance Data |
| **Collection** | Yes |
| **Usage Purpose** | Third-Party Advertising |
| **Linked to User's Identity** | No |
| **Used for Tracking** | No |

*Note: The AdMob SDK may collect performance and crash diagnostics related to ad rendering.*

#### 5. Coarse Location

| Field | Value |
|-------|-------|
| **Data Type** | Location — Coarse Location |
| **Collection** | Yes |
| **Usage Purpose** | App Functionality |
| **Linked to User's Identity** | No |
| **Used for Tracking** | No |

*Note: When scanning restaurant menus, the user's approximate location (reduced accuracy) may be sent alongside the menu photo to Google Gemini AI to help identify the restaurant's cuisine for more accurate calorie estimates. Location data is used only during the analysis request and is not stored by the app or any server. Location access is optional — menu scanning works without it.*

### Data NOT Collected

Confirm that the following are **not collected**:

- Health & Fitness (Active Energy Burned) — *read from HealthKit, used on-device only*
- Body (Height, Weight) — *user-entered, stored on-device only*
- Contact Info (name, email, phone)
- Precise Location — *only coarse location is collected; see above*
- Contacts
- User Content (other than photos)
- Identifiers (user ID) — *Device ID is collected by AdMob; see above*
- Browsing History
- Search History
- Purchases
- Financial Info
- Sensitive Info

### Privacy Policy URL

> https://gist.github.com/pning80/fc4cc0aab367f96202371566241ec7cb

---

## 5. App Review Notes

Paste this into the **Notes for Review** field:

> **No login or account required.** The app works entirely on-device with no sign-up.
>
> **Camera access required:** The core feature is scanning food with the camera. On launch, the app requests camera permission. Please grant camera access to test food scanning. Point the camera at any food or food image, take a photo, and tap "Use" to start AI analysis.
>
> **Photo library access:** Users can also choose a photo from their photo library for food or menu analysis. Tap the "Log Food" tab, then select "Choose from Library." Or tap the "Scan Menu" tab, then "Choose from Library."
>
> **Location access:** When scanning a restaurant menu, the app may request location permission. Approximate location is sent alongside the menu photo to help identify the restaurant's cuisine for more accurate calorie estimates. Location is entirely optional — menu scanning works without it. Location data is not stored.
>
> **HealthKit access:** The app requests permission to read Active Energy Burned from Apple Health. This is optional — the app works without it, but the daily calorie goal adjusts based on activity data when granted.
>
> **Navigation:** Settings and About are accessed via the app menu (ellipsis button "..." in the top-right corner of the Dashboard or History screen), not from a tab.
>
> **Ads:** The app displays banner ads via Google AdMob on the Dashboard, History, Log Food sheet, Scan Menu sheet, About screen, Stored Menus, and during manual entry. A native ad appears during food analysis. Where required by regional regulations (e.g., GDPR), a consent form is presented using Google's User Messaging Platform (UMP). Users in those regions can later change their privacy choices from Settings > Privacy > "Manage Privacy Choices." No App Tracking Transparency prompt is shown — the app does not use the ATT framework.
>
> **Network dependency:** Food and menu analysis require an internet connection. The app sends photos to a backend service that uses Google Gemini AI for nutritional analysis. The backend verifies the app's identity using Apple App Attest. If the device is offline, analysis will fail with an error message.
>
> **Demo flow:**
> 1. Open the app. Complete 3-step onboarding (Welcome > Your Privacy > Your Goal). UMP consent form may appear (region-dependent).
> 2. Dashboard shows daily summary with a nutrition hero card (calories + macros) and a banner ad.
> 3. Tap the "Log Food" tab. A sheet appears with three options: "Scan Food", "Choose from Library", "Log Manually".
> 4. Tap "Scan Food". Grant camera permission. Take a photo of food. Tap "Use". The app analyzes the photo and displays estimated food items with calories, and a macro breakdown (protein, carbs, fat with grams, percentages, and proportional bars) per item and as a total summary. Tap "Done" (pinned at the bottom) to save.
> 5. Return to Dashboard to see the logged entry and the updated hero card.
> 6. Tap "Log Food" > "Choose from Library". Select a food photo. Tap "Use". Review results. Tap "Done".
> 7. Tap the "Scan Menu" tab. A sheet appears with "Scan Menu", "Choose from Library", "Stored Menus".
> 8. Tap "Scan Menu". Take a photo of a restaurant menu. Tap "Use". Grant location permission (optional). View calorie estimates for each menu item. Tap items to expand macro details. Tap "Done" to save.
> 9. Tap "Scan Menu" tab > "Stored Menus" to view previously scanned menus.
> 10. Tap the "History" tab. A trend card shows a calorie chart for the last 7 days (switch to 30 days with the segmented control) plus Avg / day, On target, and Day streak. Below it, past entries are organized by date with daily nutrition summaries.
> 11. Tap the "..." (ellipsis) menu button. Tap "Settings" to see profile data, calorie goal, and privacy choices. Tap "About" to see app version, App Store rating, support links, and device attestation status.

### Review Contact Information

| Field | Value |
|-------|-------|
| **First Name** | *(your first name)* |
| **Last Name** | *(your last name)* |
| **Email** | pning80.git@gmail.com |
| **Phone** | *(your phone number)* |

### Sign-In Required

**No** — no login or demo credentials needed.

---

## 6. Export Compliance

When prompted about export compliance:

| Question | Answer |
|----------|--------|
| Does your app use encryption? | **Yes** |
| Does your app qualify for any exemptions? | **Yes** |
| Exemption type | The app only uses HTTPS (TLS/SSL) for network communication — it does not contain custom encryption. Select: **"Uses encryption exempt from EAR"** and check **"The app only uses standard encryption protocols (HTTPS, TLS)"** |

No CCATS or ERN number is needed. Apple's standard HTTPS exemption applies.

`ITSAppUsesNonExemptEncryption = NO` is already set in the Xcode project.

---

## 7. Build Upload

1. Verify version and build number in Xcode:
   - **Marketing Version**: `1.5.0`
   - **Build Number**: `11`
2. In Xcode: **Product > Archive**
3. In the Organizer window: select the archive > **Distribute App > App Store Connect**
4. Follow the wizard (automatic signing recommended)
5. Wait for the build to finish processing in App Store Connect (~10-30 minutes)
6. **TestFlight first:** install the processed build from TestFlight on the Test iPhone 14 and smoke-test one food scan, the Log Food / Scan Menu tabs, and History
7. Select the build in your App Store Connect version page

---

## 8. Pricing & Availability

| Field | Value |
|-------|-------|
| **Price** | Free |
| **Availability** | All territories |
| **Pre-Order** | No |

---

## 9. Submission Checklist

Complete these items before clicking **Submit for Review**:

### Done (pre-submission engineering, 2026-10-25)

- [x] Version bumped to 1.5.0 (build 11)
- [x] Release build verified: production AdMob IDs, empty legacy backend key, no internal `.md` notes in the bundle, `ITSAppUsesNonExemptEncryption = NO`, MinimumOSVersion 18.0 (raised from 17.0 in this release)
- [x] Full UI suite green on Test iPhone 14 (iOS 27.0.1) and iOS 18.5 simulator; unit tests green
- [x] Parity gates + CI green
- [x] Production backend healthy; Firestore `(default)` DB present

### Must Do (Blockers)

- [ ] **Device smoke test of the Release build** (installed on Test iPhone 14) — see Testing below
- [ ] **What's New in This Version** — Section 2
- [ ] **Upload Build** — Archive and upload v1.5.0 (11) via Xcode. Section 7
- [ ] **TestFlight smoke test** of the processed build
- [ ] **Select Build** — 1.5.0 (11)
- [ ] **App Review Notes** — updated History step. Section 5
- [ ] **Screenshots** — replace all 10 at 6.9". Section 3

### Optional (If Changed)

- [ ] **Copyright** — change from "© 2026 Peng Ning" to "© 2026 pning80" (version page, General App Information). Section 1
- [ ] **Description** — Meal History line now mentions trends. Section 2
- [ ] **Promotional Text** — Section 2
- [ ] **Keywords** — `camera` → `trend`. Section 2

### Minimum iOS Version

v1.5.0 raises the minimum from **iOS 17.0 to iOS 18.0**. Nothing to configure in App Store Connect — it reads `MinimumOSVersion` from the build. Users on iOS 17 keep v1.4.1; anyone who downloaded the app before can reinstall v1.4.1 as the "last compatible version", but iOS 17 users who never had the app can no longer get it. (Xcode 27 can no longer download an iOS 17 simulator, so iOS 17 could not be tested.)

### Release Option

Recommended: release to all users at once — *not* phased. v1.4.1 shows a blank screen for Log Food / Scan Menu on iOS 27, and a phased release only throttles automatic updates.

### Testing

- [ ] **Log Food / Scan Menu tabs** — tap each ~5× on iOS 27. Sheet opens, previous tab stays selected, no blank-screen flash.
- [ ] **Food scan (production)** — real Gemini result via App Attest (Release build has no legacy key, so this proves attestation works).
- [ ] **History trend** — chart + chips match logged data; 7D ↔ 30D switch; delete an entry and confirm the chips update; VoiceOver reads the chart summary.
- [ ] **Choose from Library** — one food photo, one menu photo.
- [ ] **Scan Menu** — with location allowed and denied.
- [ ] **Dashboard** — HealthKit allowed (burned calories raise the goal) and denied.
- [ ] **Light and dark mode** — Dashboard, History, scan results.
- [ ] **Ads render** — banners + native ad. ⚠️ Do **not** tap production ads on your own device (AdMob invalid traffic) unless it is registered as an AdMob test device.
- [ ] **Offline** — airplane mode → scan shows an error, no hang.
- [ ] **Camera denied** — "Camera Access Required" screen with Settings link.
- [ ] **Upgrade from 1.4.1** — on a device with App Store 1.4.1 data, install 1.5.0 over it (TestFlight) and confirm entries and profile survive.

### Post-Release (first 72 h)

- [ ] Cloud Run prod logs filtered on `X-App-Version: 1.5.0` — watch 401 (attestation) and 5xx rates
- [ ] Xcode Organizer / App Store Connect crash reports
- [ ] AdMob fill rate and invalid-traffic warnings
- [ ] Hotfix path if needed: 1.5.1 (build 12) + expedited review

---

## Quick Reference — App Store Connect Fields Map

| App Store Connect Section | Where to Find Content |
|--------------------------|----------------------|
| App Information > Name, Subtitle, Categories | Section 1 |
| App Information > Content Rights | Section 1 |
| App Information > Age Rating | Section 1 |
| Version > What's New | Section 2 |
| Version > Description | Section 2 |
| Version > Promotional Text | Section 2 |
| Version > Keywords | Section 2 |
| Version > Support URL | Section 2 |
| Version > Screenshots | Section 3 |
| App Privacy | Section 4 |
| App Review > Notes | Section 5 |
| App Review > Sign-In Required | Section 5 |
| App Review > Contact Info | Section 5 |
| Export Compliance | Section 6 |
| Build Upload | Section 7 |
| Pricing & Availability | Section 8 |

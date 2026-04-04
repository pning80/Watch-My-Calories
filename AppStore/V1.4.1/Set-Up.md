# App Store Connect Update Setup — Watch My Calories v1.4.1

**Date: 2026-04-03**

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
| **Copyright** | © 2026 Peng Ning |

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

> See your nutrition at a glance! After scanning food, the results screen now shows a full macro breakdown — protein, carbs, and fat with grams, percentages, and a visual proportional bar for each item and the meal total.
>
> Also in this update:
> - Meal total summary — your combined nutrition is highlighted in a dedicated card so you can quickly see the overall picture
> - The Done button now stays visible at the bottom of the screen, even when you've logged many items
> - Food entries on the Dashboard and History now show a proportional bar visualizing your macro balance

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
> - Meal History — Browse all past entries organized by date and meal type, with daily nutrition summaries. View, edit, or delete individual items or entire meal groups.
> - Manual Entry — Prefer to log food yourself? Add entries manually with full control over name, calories, and macros.
> - Privacy First — All your food and health data stays on your device. No accounts, no sign-ups. Food and menu photos are sent to Google Gemini solely for analysis and are not stored. The app is supported by ads served via Google AdMob.
>
> Watch My Calories is designed for anyone who wants a simple, private, and intelligent way to stay on top of their nutrition.

### Promotional Text

> Snap a photo of your meal or a restaurant menu — AI instantly estimates calories and macros for every item. Now with detailed nutrition breakdowns after every scan. Private and on-device.

*(Promotional text can be updated without a new app version.)*

### Keywords

```
calorie,tracker,food,AI,nutrition,macro,health,diet,meal,camera,menu,restaurant,scan,protein
```

*(100 characters max, comma-separated, no spaces after commas.)*

### Support URL

> https://gist.github.com/pning80/7dc8a85c83edcc03845d182386cab470

### Marketing URL

> **https://pning80.github.io**
> *(CRITICAL FOR ADMOB: This domain hosts your `app-ads.txt` file at the root. Do not change this unless you move ad hosting.)*

---

## 3. Screenshots & App Previews

The v1.4.1 screenshot folder is at `AppStore/V1.4.1/Screenshots/`.

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

| Screenshot | Notes | Status |
|------------|-------|--------|
| Camera scan food | Camera view for food scanning | Carried from v1.4.0 |
| Food scan results | Post-capture success screen with macro details per item and total summary card | **RETAKE NEEDED** — new macro breakdown and pinned Done button |
| Dashboard with hero card | Shows nutrition hero card and meal entries with inline proportional bars | **RETAKE NEEDED** — entry cards now show proportional bars |
| Camera scan menu | Camera view for menu scanning | Carried from v1.4.0 |
| Menu scan results | Menu items with calorie estimates | Carried from v1.4.0 |
| Pick from library | Photo library selection flow | Carried from v1.4.0 |
| Entry menu | Tap-to-expand entry detail | Carried from v1.4.0 |
| Meal history | Past entries by date with daily macro summaries | **RETAKE NEEDED** — entry cards now show proportional bars |
| Your privacy | Privacy information screen | Carried from v1.4.0 |
| Settings | Profile data, calorie goal, privacy choices | Carried from v1.4.0 |

**Tips:**
- Light mode screenshots are generally preferred as lead images for the App Store listing.
- Dark mode variants can be included for optional use or marketing materials.
- You can add text overlays and frames using tools like [Shotbot](https://shotbot.io) or [Screenshots Pro](https://screenshots.pro).

- [ ] **Retake SS-2-FoodScanResults** — success screen now shows macro breakdown per item + total summary card + pinned Done button
- [ ] **Retake SS-3-Dashboard** — food entry cards now show inline proportional bars
- [ ] **Retake SS-8-MealHistory** — food entry cards now show inline proportional bars

*Reference: [Apple Screenshot Specifications](https://developer.apple.com/help/app-store-connect/reference/screenshot-specifications/)*

---

## 4. Privacy Details (App Privacy "Nutrition Labels")

No changes from v1.4.0. All privacy labels carry over automatically.

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
> 5. Return to Dashboard to see the logged entry with inline macro proportional bars and the updated hero card.
> 6. Tap "Log Food" > "Choose from Library". Select a food photo. Tap "Use". Review results. Tap "Done".
> 7. Tap the "Scan Menu" tab. A sheet appears with "Scan Menu", "Choose from Library", "Stored Menus".
> 8. Tap "Scan Menu". Take a photo of a restaurant menu. Tap "Use". Grant location permission (optional). View calorie estimates for each menu item. Tap items to expand macro details. Tap "Done" to save.
> 9. Tap "Scan Menu" tab > "Stored Menus" to view previously scanned menus.
> 10. Tap the "History" tab. View past entries organized by date with daily nutrition summaries (macros) and inline proportional bars per entry.
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
   - **Marketing Version**: `1.4.1`
   - **Build Number**: `10`
2. In Xcode: **Product > Archive**
3. In the Organizer window: select the archive > **Distribute App > App Store Connect**
4. Follow the wizard (automatic signing recommended)
5. Wait for the build to finish processing in App Store Connect (~10-30 minutes)
6. Select the build in your App Store Connect version page

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

### Must Do (Blockers)

Since this is a minor update, most settings carry over automatically. You only need to complete the following:

- [ ] **What's New in This Version** — Add release notes for v1.4.1. See Section 2.
- [ ] **Upload Build** — Archive and upload v1.4.1 via Xcode. See Section 7.
- [ ] **Select Build** — Choose the uploaded 1.4.1 (build 10) build in App Store Connect.
- [ ] **App Review Notes** — Updated to describe new success screen macro details. See Section 5.
- [ ] **Screenshots** — Retake 3 screenshots that changed (food scan results, dashboard, meal history). See Section 3.

### Optional (If Changed)

- [ ] **Promotional Text** — Updated to mention nutrition breakdowns. See Section 2.

### Testing

- [ ] **Test on a physical device** — Verify camera, HealthKit, ad loading, consent sheet, location, and App Attest all work.
- [ ] **Test food scan success screen** — Scan food. Verify each item card shows CompactMacroRow (proportional bar + grams + percentages). Verify total summary card shows hero-style macro labels with proportional bar on tinted background.
- [ ] **Test Done button pinning** — Scan food with multiple items. Verify Done button stays visible at the bottom without scrolling.
- [ ] **Test success screen with no macros** — If possible, trigger a result with no macro data. Verify macro sections hide gracefully.
- [ ] **Test Dashboard entry cards** — Verify food entry cards show inline proportional bars for macros.
- [ ] **Test History entry cards** — Verify food entry cards in history show inline proportional bars.
- [ ] **Test Log Food sheet** — Tap "Log Food" tab. Verify sheet appears with Scan Food, Choose from Library, and Log Manually. Test each option.
- [ ] **Test Scan Menu flow** — Tap "Scan Menu" tab. Scan a restaurant menu. Verify analysis shows calorie estimates per item.
- [ ] **Test camera denied path** — Deny camera permission and verify the app shows a "Camera Access Required" screen with a link to Settings.
- [ ] **Test offline behavior** — Verify the app shows an appropriate error when food or menu analysis is attempted without internet.
- [ ] **Test ad placements** — Confirm banner ads render in Dashboard, History, Log Food sheet, Scan Menu sheet, About, Stored Menus, and manual entry. Confirm native ad renders during food analysis.
- [ ] **Test the "no HealthKit" path** — Deny HealthKit permission and verify the app still works without crashing.

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

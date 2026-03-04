# App Store Connect Setup — Calorie Watcher

All the content you need to fill in App Store Connect for your first submission.

---

## 1. App Information

| Field | Value |
|-------|-------|
| **App Name** | Calorie Watcher: AI-Powered Food Tracker |
| **Subtitle** | Snap, Track, and Stay Fit with AI. |
| **Bundle ID** | `com.pning80.CalorieWatcher` |
| **SKU** | `calorie-watcher-ios-001` |
| **Primary Category** | Health & Fitness |
| **Secondary Category** | Food & Drink |
| **Primary Language** | English (U.S.) |

### Content Rights

- Does your app contain, show, or access third-party content? **No**

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

### App Description

> Track your daily calories effortlessly with AI-powered food recognition. Simply snap a photo of your meal and Calorie Watcher instantly identifies each food item, estimates portions in familiar US units, and calculates calories and macronutrients.
>
> KEY FEATURES
>
> - AI Food Analysis — Snap a photo of your meal. Calorie Watcher uses Google Gemini AI to identify foods and estimate calories, protein, carbs, and fat.
> - Smart Meal Tracking — Entries are automatically categorized as Breakfast, Lunch, Dinner, or Snack based on the time of day.
> - Daily Dashboard — See your calorie intake at a glance with a visual progress card showing how close you are to your daily goal.
> - HealthKit Integration — Reads your active energy burned from Apple Health to dynamically adjust your effective calorie target.
> - Personalized Goals — Set your profile (height, weight, age, activity level) and the app calculates a recommended daily calorie target using the Mifflin-St Jeor formula.
> - Meal History — Browse all past entries organized by date and meal type. View, edit, or delete individual items or entire meal groups.
> - Manual Entry — Prefer to log food yourself? Add entries manually with full control over name, calories, and macros.
> - Privacy First — All your data stays on your device. No accounts, no sign-ups, no analytics. Food photos are sent to Google Gemini solely for analysis and are not stored.
>
> Calorie Watcher is designed for anyone who wants a simple, private, and intelligent way to stay on top of their nutrition.

### Promotional Text

> Stop guessing and start tracking! Snap a photo of your meal for instant AI calorie estimates. Private, fast, and synced with your fitness goals.

*(Promotional text can be updated without a new app version.)*

### Keywords

```
calorie,tracker,food,AI,nutrition,macro,health,diet,meal,camera,fitness,weight,protein,carbs,HealthKit
```

*(100 characters max, comma-separated, no spaces after commas.)*

### What's New in This Version

> Initial release of Calorie Watcher — AI-powered calorie tracking with camera-based food recognition, HealthKit integration, and on-device data storage.

### Support URL

> https://gist.github.com/pning80/7dc8a85c83edcc03845d182386cab470

### Marketing URL

> *(Optional — leave blank or provide a landing page URL.)*

---

## 3. Screenshot Guidance

### Required Device Sizes

You must provide screenshots for at least one size per device family. Recommended:

| Device | Screenshot Size (pixels) | Minimum |
|--------|-------------------------|---------|
| **iPhone 6.9"** (iPhone 16 Pro Max) | 1320 × 2868 | Required for "all iPhones" |
| **iPhone 6.7"** (iPhone 15 Plus / 14 Pro Max) | 1290 × 2796 | Optional (can reuse 6.9") |
| **iPhone 6.5"** (iPhone 11 Pro Max) | 1242 × 2688 | Optional (can reuse 6.9") |
| **iPhone 5.5"** (iPhone 8 Plus) | 1242 × 2208 | Only if supporting older devices |

### Screens to Capture

Capture these in order — they tell the story of the app:

1. **Dashboard** — Show the daily summary hero card with calorie progress, active energy, and a few meal entries below. This is your lead screenshot.
2. **Camera Scan** — The live camera view with the shutter button, or the photo review screen showing "Retake" and "Use Photo" options.
3. **Analysis Results** — The estimation review screen showing identified food items with calories, quantities, and macros.
4. **Meal History** — The history tab showing entries grouped by date and meal type.
5. **Settings / Profile** — The settings screen with user profile fields and calorie goal.

**Tips:**
- Use a real meal photo for screenshots 2 and 3 — staged food looks better than empty states.
- Light mode is generally preferred for App Store screenshots.
- You can add text overlays and frames using tools like [Shotbot](https://shotbot.io) or [Screenshots Pro](https://screenshots.pro).

---

## 4. Privacy Details (App Privacy "Nutrition Labels")

App Store Connect requires you to disclose all data your app collects. Fill in as follows:

### Data Types Collected

#### 1. Health & Fitness

| Field | Value |
|-------|-------|
| **Data Type** | Health — Active Energy Burned |
| **Collection** | Yes |
| **Usage Purpose** | App Functionality |
| **Linked to User's Identity** | No |
| **Used for Tracking** | No |

#### 2. Photos or Videos

| Field | Value |
|-------|-------|
| **Data Type** | Photos |
| **Collection** | Yes |
| **Usage Purpose** | App Functionality |
| **Linked to User's Identity** | No |
| **Used for Tracking** | No |

*Note: Food photos are sent to Google Gemini AI for analysis. Only the first photo per meal is stored locally on-device. Photos are not stored on the server.*

#### 3. Body — Height, Weight

| Field | Value |
|-------|-------|
| **Data Type** | Body — Height and Weight |
| **Collection** | Yes |
| **Usage Purpose** | App Functionality |
| **Linked to User's Identity** | No |
| **Used for Tracking** | No |

*Note: User-entered profile data used for BMR/calorie goal calculation. Stored only on-device.*

### Data NOT Collected

Confirm that the following are **not collected**:

- Contact Info (name, email, phone)
- Location
- Contacts
- User Content (other than photos)
- Identifiers (user ID, device ID)
- Browsing History
- Search History
- Diagnostics (crash logs, performance data)
- Usage Data
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
> **Camera access required:** The core feature is scanning food with the camera. On launch, the app requests camera permission. Please grant camera access to test food scanning. Point the camera at any food or food image, take a photo, and tap "Analyze" to see AI-estimated calories.
>
> **HealthKit access:** The app requests permission to read Active Energy Burned from Apple Health. This is optional — the app works without it, but the daily calorie goal adjusts based on activity data when granted.
>
> **Network dependency:** Food analysis requires an internet connection. The app sends food photos to a backend service that uses Google Gemini AI for nutritional analysis. If the device is offline, analysis will fail with an error message.
>
> **Demo flow:**
> 1. Open the app → Dashboard tab shows daily summary (empty on first launch)
> 2. Tap the Camera tab → grant camera permission → take a photo of food → review and tap "Use Photo" → tap "Analyze"
> 3. Review the AI-estimated food items, calories, and macros → tap "Save"
> 4. Return to Dashboard to see the logged entry
> 5. Tap History tab to view past entries
> 6. Tap Settings tab to enter profile data and see the calculated calorie goal

### Review Contact Information

| Field | Value |
|-------|-------|
| **First Name** | *(your first name)* |
| **Last Name** | *(your last name)* |
| **Email** | pn80online@gmail.com |
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

~~*Tip: You can set the `ITSAppUsesNonExemptEncryption` key to `NO` in your Info.plist to skip this question on future submissions.*~~ **Done** — already added to the Xcode project build settings.

---

## 7. Build Upload

Before submitting for review, upload a build via Xcode:

1. In Xcode: **Product → Archive**
2. In the Organizer window: select the archive → **Distribute App → App Store Connect**
3. Follow the wizard (automatic signing recommended)
4. Wait for the build to finish processing in App Store Connect (~10–30 minutes)
5. Select the build in your App Store Connect version page

---

## 8. Pricing & Availability

| Field | Value |
|-------|-------|
| **Price** | Free (or set your price) |
| **Availability** | All territories (or select specific countries) |
| **Pre-Order** | No |

---

## 9. Submission Checklist

Complete these items before clicking **Submit for Review**:

### Must Do (Blockers)

- [x] **Privacy Policy URL** — https://gist.github.com/pning80/fc4cc0aab367f96202371566241ec7cb
- [x] **Support URL** — https://gist.github.com/pning80/7dc8a85c83edcc03845d182386cab470
- [ ] **Screenshots** — Take screenshots using iPhone 16 Pro Max simulator (see Section 3 above).
- [x] **App Icon** — 1024×1024 icon is in the Xcode asset catalog (`AppIcon.png` also copied to `AppStore/`).
- [ ] **Upload Build** — Archive and upload via Xcode (see Section 7).
- [ ] **Select Build** — Choose the uploaded build in App Store Connect.
- [ ] **Fill in Privacy Details** — Complete the App Privacy section (see Section 4).
- [x] **Export Compliance** — `ITSAppUsesNonExemptEncryption = NO` added to Xcode project build settings.
- [ ] **Review Contact Info** — Add your name, email (pn80online@gmail.com), and phone for App Review.

### Recommended

- [x] **Add `ITSAppUsesNonExemptEncryption = NO`** to Info.plist — Done.
- [ ] **Test on a physical device** — Verify camera, HealthKit, and network analysis all work before submitting.
- [ ] **Test the "no HealthKit" path** — Deny HealthKit permission and verify the app still works without crashing.
- [ ] **Test offline behavior** — Verify the app shows an appropriate error when food analysis is attempted without internet.
- [ ] **Proofread description and keywords** — Check for typos and ensure keywords are relevant.

---

## Quick Reference — App Store Connect Fields Map

| App Store Connect Section | Where to Find Content |
|--------------------------|----------------------|
| App Information → Name, Subtitle, Categories | Section 1 |
| App Information → Content Rights | Section 1 |
| App Information → Age Rating | Section 1 |
| Version → Description | Section 2 |
| Version → Promotional Text | Section 2 |
| Version → Keywords | Section 2 |
| Version → What's New | Section 2 |
| Version → Support URL | Section 2 (you must provide) |
| Version → Screenshots | Section 3 |
| App Privacy | Section 4 |
| App Review → Notes | Section 5 |
| App Review → Sign-In Required | Section 5 |
| App Review → Contact Info | Section 5 |
| Export Compliance | Section 6 |
| Pricing & Availability | Section 8 |

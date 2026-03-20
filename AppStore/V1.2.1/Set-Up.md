# App Store Connect Update Setup — Watch My Calories v1.2.1

**Date: 2026-03-19**

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

> We've made behind-the-scenes improvements to keep the app running smoothly and ensure compliance with our ads platform.

### App Description

> Track your daily calories effortlessly with AI-powered food recognition. Simply snap a photo of your meal and Watch My Calories instantly identifies each food item, estimates portions, and calculates calories and macronutrients.
>
> KEY FEATURES
>
> - AI Food Analysis — Snap a photo of your meal. Watch My Calories uses Google Gemini AI to identify foods and estimate calories, protein, carbs, and fat.
> - Smart Meal Tracking — Entries are automatically categorized as Breakfast, Lunch, Dinner, or Snack based on the time of day.
> - Daily Dashboard — See your calorie intake at a glance with a visual progress card showing how close you are to your daily goal.
> - HealthKit Integration — Reads your active energy burned from Apple Health to dynamically adjust your effective calorie target.
> - Personalized Goals — Set your profile (height, weight, age, activity level) and the app calculates a recommended daily calorie target using the Mifflin-St Jeor formula.
> - Meal History — Browse all past entries organized by date and meal type. View, edit, or delete individual items or entire meal groups.
> - Manual Entry — Prefer to log food yourself? Add entries manually with full control over name, calories, and macros.
> - Privacy First — All your food and health data stays on your device. No accounts, no sign-ups. Food photos are sent to Google Gemini solely for analysis and are not stored. The app is supported by ads served via Google AdMob.
>
> Watch My Calories is designed for anyone who wants a simple, private, and intelligent way to stay on top of their nutrition.

### Promotional Text

> Stop guessing and start tracking! Snap a photo of your meal for instant AI calorie estimates. Private, fast, and synced with your fitness goals.

*(Promotional text can be updated without a new app version.)*

### Keywords

```
calorie,tracker,food,AI,nutrition,macro,health,diet,meal,camera,fitness,weight,protein,carbs
```

*(100 characters max, comma-separated, no spaces after commas.)*

### Support URL

> https://gist.github.com/pning80/7dc8a85c83edcc03845d182386cab470

### Marketing URL

> **https://pning80.github.io**
> *(CRITICAL FOR ADMOB: This domain hosts your `app-ads.txt` file at the root. Do not change this unless you move ad hosting.)*

---

## 3. Screenshots & App Previews

The v1.2.1 screenshot folder is at `AppStore/V1.2.1/Screenshots/`.

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

| Screenshot | Notes |
|------------|-------|
| Onboarding | Welcome, privacy, and goal screens |
| Dashboard with ad | Shows the banner ad placement |
| Camera scan | Camera view |
| AI analysis results | Post-capture analysis |
| Meal history | Past entries by date |
| Settings | Profile data, calorie goal, Device Attestation status |

**Tips:**
- Light mode screenshots are generally preferred as lead images for the App Store listing.
- Dark mode variants can be included for optional use or marketing materials.
- You can add text overlays and frames using tools like [Shotbot](https://shotbot.io) or [Screenshots Pro](https://screenshots.pro).

- [ ] **Take screenshots** and add them to the `AppStore/V1.2.1/Screenshots/` folder.

*Reference: [Apple Screenshot Specifications](https://developer.apple.com/help/app-store-connect/reference/screenshot-specifications/)*

---

## 4. Privacy Details (App Privacy "Nutrition Labels")

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

*Note: Food photos are sent to Google Gemini AI for analysis. Only the first photo per meal is stored locally on-device. Photos are not stored on the server.*

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

### Data NOT Collected

Confirm that the following are **not collected**:

- Health & Fitness (Active Energy Burned) — *read from HealthKit, used on-device only*
- Body (Height, Weight) — *user-entered, stored on-device only*
- Contact Info (name, email, phone)
- Location
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
> **Camera access required:** The core feature is scanning food with the camera. On launch, the app requests camera permission. Please grant camera access to test food scanning. Point the camera at any food or food image, take a photo, and tap "Use Photo" to start AI analysis.
>
> **HealthKit access:** The app requests permission to read Active Energy Burned from Apple Health. This is optional — the app works without it, but the daily calorie goal adjusts based on activity data when granted.
>
> **Ads:** The app displays banner and native ads via Google AdMob. Where required by regional regulations (e.g., GDPR), a consent form is presented using Google's User Messaging Platform (UMP). Users in those regions can later change their privacy choices from Settings → Privacy → "Manage Privacy Choices." No App Tracking Transparency prompt is shown — the app does not use the ATT framework.
>
> **Network dependency:** Food analysis requires an internet connection. The app sends food photos to a backend service that uses Google Gemini AI for nutritional analysis. The backend verifies the app's identity using Apple App Attest. If the device is offline, analysis will fail with an error message.
>
> **Demo flow:**
> 1. Open the app → Complete 3-step onboarding (Welcome → Your Privacy → Your Goal) → UMP consent form may appear (region-dependent)
> 2. Dashboard tab shows daily summary with a banner ad
> 3. Tap the Camera tab → grant camera permission → take a photo of food → tap "Use Photo"
> 4. The app automatically analyzes the photo and displays estimated food items, calories, and macros → tap "Done" to save
> 5. Return to Dashboard to see the logged entry
> 6. Tap History tab to view past entries
> 7. Tap Settings tab to see profile data, calorie goal, Device Attestation status, and "Manage Privacy Choices" (visible in GDPR regions)

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
   - **Marketing Version**: `1.2.1`
   - **Build Number**: Increment if a previous build was already uploaded
2. In Xcode: **Product → Archive**
3. In the Organizer window: select the archive → **Distribute App → App Store Connect**
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

Since this is an update, most settings (like Privacy, Categories, and Age Rating) carry over automatically. You only need to complete the following:

- [ ] **What's New in This Version** — Add release notes. See Section 2.
- [ ] **Marketing URL** — Set this to `https://pning80.github.io` so AdMob can find `app-ads.txt`.
- [ ] **Support URL** — Keep as `https://gist.github.com/pning80/7dc8a85c83edcc03845d182386cab470`.
- [ ] **Upload Build** — Archive and upload V1.2.1 via Xcode. See Section 7.
- [ ] **Select Build** — Choose the uploaded 1.2.1 build in App Store Connect.
- [ ] **App Review Notes** — Paste the reviewer notes if they cleared out. See Section 5.

### Optional (If Changed)

- [ ] **Screenshots** — Only if the UI changed.
- [ ] **Promotional Text** — Update if running a new campaign.

- [ ] **Test on a physical device** — Verify camera, HealthKit, ad loading, consent sheet, and App Attest all work.
- [ ] **Test the consent flow** — Verify the UMP consent sheet appears on first launch and that ads load correctly afterward.
- [ ] **Test offline behavior** — Verify the app shows an appropriate error when food analysis is attempted without internet.
- [ ] **Test ad placements** — Confirm banner and native ads render correctly and do not obscure app content.
- [ ] **Test the "no HealthKit" path** — Deny HealthKit permission and verify the app still works without crashing.
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
| Version → Support URL | Section 2 |
| Version → Screenshots | Section 3 |
| App Privacy | Section 4 |
| App Review → Notes | Section 5 |
| App Review → Sign-In Required | Section 5 |
| App Review → Contact Info | Section 5 |
| Export Compliance | Section 6 |
| Pricing & Availability | Section 8 |

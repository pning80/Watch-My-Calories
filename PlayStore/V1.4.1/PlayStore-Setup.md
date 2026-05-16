# Google Play Console Update Setup — Watch My Calories v1.4.1

**Date: 2026-04-20**

All the content you need to fill in Google Play Console for your Android version update.

---

## 1. App Information (Store Listing)

| Play Console Field | Value | Notes |
|-------|-------|-------|
| **App Name** | Watch My Calories | Must be ≤ 30 characters |
| **Short description** | Snap a photo of your meal to track calories and macros instantly with AI! | Must be ≤ 80 characters |

### Full Description
*(Must be ≤ 4,000 characters. Retaining iOS SEO layout without keyword stuffing)*

> Track your daily calories effortlessly with AI-powered food recognition. Simply snap a photo of your meal — or choose one from your photo library — and Watch My Calories instantly identifies each food item, estimates portions, and calculates calories and macronutrients. You can also scan restaurant menus to see calorie estimates for every dish before you order.
>
> KEY FEATURES
>
> - AI Food Analysis — Snap a photo of your meal or choose one from your photo library. Watch My Calories uses Google Gemini AI to identify foods and estimate calories, protein, carbs, and fat.
> - Menu Scanning — Take a photo of a restaurant menu to see AI calorie estimates for every item. Your approximate location helps identify the restaurant's cuisine for more accurate results. Scanned menus are saved automatically.
> - Smart Meal Tracking — Entries are automatically categorized as Breakfast, Lunch, Dinner, or Snack based on the time of day.
> - Daily Dashboard — See your calorie intake at a glance with a nutrition hero card showing calories consumed, remaining, and a protein/carbs/fat breakdown.
> - Health Connect Integration — Reads your active energy burned from Android Health Connect to dynamically adjust your effective calorie target.
> - Personalized Goals — Set your profile (height, weight, age, activity level) and the app calculates a recommended daily calorie target using the Mifflin-St Jeor formula.
> - Meal History — Browse all past entries organized by date and meal type, with daily nutrition summaries. View, edit, or delete individual items or entire meal groups.
> - Manual Entry — Prefer to log food yourself? Add entries manually with full control over name, calories, and macros.
> - Privacy First — All your food and health data stays on your device. No accounts, no sign-ups. Food and menu photos are sent to Google Gemini solely for analysis and are not stored. The app is supported by ads served via Google AdMob.
>
> Watch My Calories is designed for anyone who wants a simple, private, and intelligent way to stay on top of their nutrition.

---

## 2. Release Notes

*Add this to "Release Notes" when rolling out the Android App Bundle (AAB):*

```text
See your nutrition at a glance! After scanning food, the results screen now shows a full macro breakdown — protein, carbs, and fat with grams, percentages, and a visual proportional bar.

Also in this update:
- Built for Android 14+ with full Jetpack Compose native support
- Migrated Apple Health integration seamlessly over to Google Health Connect
- Food entries on the Dashboard and History now show a proportional bar visualizing your macro balance
```

---

## 3. Data Safety Questionnaire

Unlike iOS Nutrition Labels, Google Play Data Safety requires specific granular selections regarding encrypted transit and deletion mechanisms.

### Core Declarations
* **Does your app collect or share any of the required user data types?** Yes
* **Is all of the user data collected by your app encrypted in transit?** Yes (Images pushed to Gemini / Ad data pushed to Google use HTTPS).
* **Do you provide a way for users to request that their data is deleted?** No (Because data is never permanently associated with accounts. App uninstall scrubs local DB).

### Data Types Collected

#### 1. Photos
* **Category**: Photos and Videos
* **Collected or Shared**: Collected
* **Is this data processed ephemerally?**: Yes (Sent to Gemini for ML analysis, then discarded off-server).
* **Purpose**: App Functionality

#### 2. Location
* **Category**: Location -> Approximate Location
* **Collected or Shared**: Collected
* **Is this data processed ephemerally?**: Yes (Sent to Gemini for menu geography mapping).
* **Purpose**: App Functionality

#### 3. Health & Fitness
* **Category**: Health and Fitness -> Health info / Fitness info
* **Collected or Shared**: Collected
* **Is this data processed ephemerally?**: No (Stored natively via Room local database).
* **Purpose**: App Functionality

#### 4. Device or other IDs (AdMob)
* **Category**: Device or other IDs
* **Collected or Shared**: Collected & Shared
* **Is this data processed ephemerally?**: No
* **Purpose**: Advertising or Marketing

---

## 4. Ads
* **Does your app have ads?** Yes. (Ensures Google flags it correctly for COPPA and regional Ad policies).
* **Target Audience**: 13-15, 16-17, 18 and over. (We avoid checking the <13 categories to avoid triggering strict Play "Designed for Families" rules which conflict with AdMob default SDK settings).

---

## 5. Contact Info
* **Support Email**: pning80.git@gmail.com
* **Website**: https://pning80.github.io

*NOTE: Do not remove the `app-ads.txt` from your GitHub Pages root, as Play Store AdMob relies heavily on this for Authorized Buyers verification.*

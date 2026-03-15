# App Store Connect Update — Watch My Calories v1.2.0

**Date: 2026-03-15**

This guide covers what needs to change in App Store Connect for the v1.2.0 update. Sections that carry over unchanged from v1.0.0 are noted but not repeated — see [V1.0.0 Set-Up.md](../V1.0.0/Set-Up.md) for the original setup.

---

## 1. App Information Updates

### Content Rights

- Does your app contain, show, or access third-party content? **Yes**

> Google AdMob serves third-party advertisements within the App. Select **Yes** and confirm you have the rights to distribute this content (covered by the AdMob Terms of Service).

### Age Rating Questionnaire

No changes to the questionnaire answers (all None/No). However, verify in App Store Connect that the resulting rating is still **4+** after updating Content Rights above. If you configure child-directed ad treatment in AdMob, this remains appropriate.

All other App Information fields (Name, Subtitle, Categories, Copyright, Language) are unchanged.

---

## 2. Version Information

### What's New in This Version

> - Removed App Tracking Transparency — ads now work without the tracking permission prompt
> - Improved AI consent flow with a cleaner, more informative consent sheet
> - Faster camera startup and better image framing on the review screen
> - Enhanced backend security with App Attest resilience and automatic retry
> - Fixed keyboard dismissal issues during onboarding
> - Bug fixes and stability improvements

### App Description

No changes needed — the v1.0.0 description already mentions AdMob and all current features.

### Promotional Text

*(Optional — can be updated without a new version. Consider refreshing to highlight the smoother ad experience.)*

### Keywords

No changes needed.

### Support URL

> https://gist.github.com/pning80/7dc8a85c83edcc03845d182386cab470

*(Unchanged)*

---

## 3. Screenshots

The v1.2.0 screenshot folder is at `AppStore/V1.2.0/Screenshots/`.

**Screenshots to capture:**

New screenshots are needed if any of the following UI changes are visible:
- AI consent sheet (redesigned)
- Dashboard or other screens now showing banner/native ads
- Settings screen (now shows App Attestation status)

Use the same **1242 x 2688** (iPhone 6.5" portrait) format as v1.0.0. Refer to [V1.0.0 Set-Up.md](../V1.0.0/Set-Up.md) Section 3 for size requirements and tips.

| Suggested Screenshots | Notes |
|-----------------------|-------|
| Dashboard with ad | Shows the banner ad placement |
| AI consent sheet | New consent flow |
| Settings with attestation status | New UI element |
| Reuse from v1.0.0 | Onboarding, Camera, Analysis, History screens if unchanged |

- [ ] **Take new screenshots** and add them to the `AppStore/V1.2.0/Screenshots/` folder.

---

## 4. Privacy Details (App Privacy "Nutrition Labels")

No changes needed — the v1.0.0 setup already declared all AdMob-related data types (Device ID, Advertising Data, Diagnostics). See [V1.0.0 Set-Up.md](../V1.0.0/Set-Up.md) Section 4.

### Privacy Policy URL

> https://gist.github.com/pning80/fc4cc0aab367f96202371566241ec7cb

*(Unchanged — but remember to update the Gist content with the fixes made to `AppStore/V1.2.0/PrivacyPolicy.md` before submission.)*

---

## 5. App Review Notes

Paste this into the **Notes for Review** field:

> **No login or account required.** The app works entirely on-device with no sign-up.
>
> **Camera access required:** The core feature is scanning food with the camera. On launch, the app requests camera permission. Please grant camera access to test food scanning. Point the camera at any food or food image, take a photo, and tap "Use Photo" to start AI analysis.
>
> **HealthKit access:** The app requests permission to read Active Energy Burned from Apple Health. This is optional — the app works without it, but the daily calorie goal adjusts based on activity data when granted.
>
> **Ads:** The app displays banner and native ads via Google AdMob. On first launch, a consent sheet is presented using Google's User Messaging Platform (UMP) in compliance with GDPR/consent regulations. No App Tracking Transparency prompt is shown — the app does not use the ATT framework.
>
> **Network dependency:** Food analysis requires an internet connection. The app sends food photos to a backend service that uses Google Gemini AI for nutritional analysis. The backend verifies the app's identity using Apple App Attest. If the device is offline, analysis will fail with an error message.
>
> **Demo flow:**
> 1. Open the app → Complete 4-step onboarding (Welcome → Profile → Goals → Permissions) → Consent sheet appears for ad personalization
> 2. Dashboard tab shows daily summary with a banner ad
> 3. Tap the Camera tab → grant camera permission → take a photo of food → tap "Use Photo"
> 4. The app automatically analyzes the photo and displays estimated food items, calories, and macros → tap "Done" to save
> 5. Return to Dashboard to see the logged entry
> 6. Tap History tab to view past entries
> 7. Tap Settings tab to see profile data, calorie goal, and App Attestation status

### Review Contact Information

*(Same as v1.0.0 — see [V1.0.0 Set-Up.md](../V1.0.0/Set-Up.md) Section 5.)*

### Sign-In Required

**No** — no login or demo credentials needed.

---

## 6. Export Compliance

No changes — `ITSAppUsesNonExemptEncryption = NO` is already set in the Xcode project.

---

## 7. Build Upload

1. Verify version and build number in Xcode:
   - **Marketing Version**: `1.2.0`
   - **Build Number**: Increment if a previous build was already uploaded (currently `2` in the project)
2. In Xcode: **Product → Archive**
3. In the Organizer window: select the archive → **Distribute App → App Store Connect**
4. Follow the wizard (automatic signing recommended)
5. Wait for the build to finish processing in App Store Connect (~10–30 minutes)
6. Select the build in your App Store Connect version page

---

## 8. Pricing & Availability

No changes — remains Free, all territories.

---

## 9. Submission Checklist

### Must Do (Blockers)

- [ ] **Update Privacy Policy Gist** — Push the fixes from `AppStore/V1.2.0/PrivacyPolicy.md` to the live Gist before submission.
- [ ] **Update Content Rights** — Change to **Yes** (third-party ad content via AdMob). See Section 1.
- [ ] **Verify Age Rating** — Confirm still 4+ after Content Rights change. See Section 1.
- [ ] **New Screenshots** — Capture updated screenshots showing ads and new UI. See Section 3.
- [ ] **Upload Build** — Archive and upload v1.2.0 via Xcode. See Section 7.
- [ ] **Select Build** — Choose the uploaded build in App Store Connect.
- [ ] **Update What's New** — Paste the v1.2.0 release notes. See Section 2.
- [ ] **Update App Review Notes** — Paste the updated reviewer notes (mentions ads, UMP consent, App Attest). See Section 5.

### Recommended

- [ ] **Test on a physical device** — Verify camera, HealthKit, ad loading, consent sheet, and App Attest all work.
- [ ] **Test the consent flow** — Verify the UMP consent sheet appears on first launch and that ads load correctly afterward.
- [ ] **Test offline behavior** — Verify the app shows an appropriate error when food analysis is attempted without internet.
- [ ] **Test ad placements** — Confirm banner and native ads render correctly and do not obscure app content.
- [ ] **Proofread What's New** — Check for typos in release notes.

---

## Changes Summary (v1.0.0 → v1.2.0)

| What Changed | Action in App Store Connect |
|--------------|----------------------------|
| AdMob ads added (banner + native) | Update Content Rights to **Yes**; update App Review Notes |
| ATT framework removed | No App Store Connect change (ATT was never declared) |
| UMP consent sheet added | Mention in App Review Notes |
| App Attest backend security | Mention in App Review Notes |
| Camera and onboarding UI fixes | New screenshots recommended |
| Privacy policy corrected | Update the live Gist |

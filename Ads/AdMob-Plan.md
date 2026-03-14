# AdMob Plan for Watch My Calories

We start with the iOS version of the app. Hopefully all the design decisions here can be easily extended to the Android version of the app.

## 1. Ads Platform & SDK

We will use Google AdMob to display ads in the app. The AdMob SDK should be initialized as early as possible in the app lifecycle (e.g., inside `AppDelegate` or the main SwiftUI `App` struct's `init`), on a background thread if possible, to prevent blocking the main thread.

## 2. Privacy & Consent (ATT & UMP)

Before initializing the AdMob SDK or requesting ads, the app must handle user consent:
- **App Tracking Transparency (ATT)**: Request permission from iOS users to track their data.
- **User Messaging Platform (UMP) SDK**: Integrate Google's UMP SDK to gather necessary consent and comply with privacy regulations such as GDPR and CCPA.

## 3. Banner Ads Implementation

We will display banner ads on:
- Today screen, right below the Hero Card.
- History screen, right below the History heading.
- Settings screen, right below the Settings heading.

All banner ads will be Adaptive Banner Ads. The layout should ensure they are displayed correctly on different screen sizes, determining the optimum size so the overall screen remains visually appealing.

### Ads Container Design
- The appearance of the ads container should be consistent with other UI elements on the screen.
- Instead of custom 5-second rotation logic or swipe gestures, we will rely exclusively on AdMob's built-in automatic refresh (configured in the AdMob dashboard) to rotate ads, ensuring compliance with AdMob policies and preventing invalid traffic.
- Ads will be persistent and cannot be closed by the user. 
- The Ads Containers on different screens should each retrieve and manage ads from AdMob separately.

## 4. Native Ads on the AI Analysis Screen

We will display native ads on:
- AI analysis screen, right after the user taps the "Use Photo" button while waiting for the AI analysis result to process.

The AI analysis screen should be structured as follows:
- **Top section**: Display the heading "Watch My Calories".
- **Middle section**: Reserved space for the Native Ad.
- **Bottom section**: Display the progress view and the text "Analyzing food" below it. 
- Once the AI analysis is complete, enable a "View Results" button rather than automatically transitioning to the next screen. This prevents accidental ad clicks if the geometry changes immediately as the user goes to read the screen.

When the user taps on the ad, the control will follow the ad link and leave the app. The app should continue to wait for the analysis response from the server in the background. The user can switch back to the app, tap the "View Results" button, and see the response.

## 5. Development & Test Ads

To safely develop and test the integration:
- Always use **Google's Test Ad Unit IDs** during all phases of development.
- Any live ad requests made from Xcode, iOS Simulators, or test devices must be registered as test devices to avoid generating invalid traffic and risking account suspension.

## 6. Ads Testing

We will test the ads implementation thoroughly to ensure that the ads are displayed correctly, that the "View Results" button transition prevents accidental clicks, and that the ads do not interfere with the core user experience.

## 7. Ads Release

We will release the ads implementation in a new version of the app once all testing and privacy requirements are met.
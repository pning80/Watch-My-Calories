# Privacy Policy — Watch My Calories

**Last Updated: March 28, 2026**

Watch My Calories ("the App") is a mobile application that helps you track your daily calorie intake using AI-powered food recognition. This Privacy Policy explains what data the App collects, how it is used, and your choices.

## Data We Collect

### Personal Profile Data

You may optionally enter profile information including height, weight, age, gender, and activity level. This data is used solely to calculate your recommended daily calorie goal using the Mifflin-St Jeor formula. **This data is stored only on your device and is never transmitted to any server.**

### Food & Nutrition Data

When you log meals — either manually or via camera scan — the App stores food names, quantities, calorie counts, macronutrient values (protein, carbs, fat), timestamps, and meal types. **This data is stored only on your device and is never transmitted to any server.**

### Food & Menu Photos

When you use the camera scan feature or choose a photo from your photo library, photos of your food or restaurant menus are sent to our backend server, which forwards them to Google Gemini AI for nutritional analysis. The photos are:

- Transmitted over HTTPS (encrypted in transit)
- Used solely to identify foods and estimate nutritional content
- **Not stored on our server** — they are forwarded to Google Gemini in real time and discarded
- Subject to [Google's API Terms of Service](https://ai.google.dev/gemini-api/terms) and [Privacy Policy](https://policies.google.com/privacy)

When you choose a photo from your photo library, the App uses Apple's standard photo picker. This grants access only to the specific photo you select — the App does not have persistent access to your photo library.

The photo from each meal session or menu scan is saved locally on your device for your reference. You can delete it at any time by deleting the food entry or stored menu.

Before your first food scan, the App asks for your explicit consent to send photos to the AI service. You can change this choice at any time in Settings. If you decline, the camera scan and photo library features are unavailable, but all other app features continue to work.

### Location Data

When scanning restaurant menus, the App may request your approximate location. This coarse location data (reduced accuracy) is sent alongside the menu photo to Google Gemini AI to help identify the restaurant's cuisine for more accurate calorie estimates. Location data is:

- Used only during the menu analysis request and is **not stored** by the App or any external service
- Entirely optional — menu scanning works without location access
- Never used for tracking, advertising, or any purpose other than improving menu analysis accuracy

You can grant or revoke location access at any time in your device's **Settings > Watch My Calories > Location**.

### Health Data (Apple HealthKit)

With your permission, the App reads **Active Energy Burned** from Apple Health. This data is used on-device to adjust your daily calorie goal based on your physical activity. **Health data is never transmitted to any server, shared with third parties, or used for advertising.**

The App does not write any data to Apple Health.

### App Preferences

Your selected app theme (Light, Dark, or System) and unit system preference (US Customary or Metric) are stored locally on your device using standard app preferences.

## Data We Do NOT Collect

The App does **not** collect, store, or transmit any of the following:

- Names, email addresses, or contact information
- Usage analytics or crash reports *(minimal app version info is sent with photo analysis requests for diagnostics; see "Food & Menu Photos" above)*
- Browsing or search history
- Financial or payment information

*Note: Google AdMob may collect limited device information for ad serving and measurement. See "Third-Party Services" below for details.*

## Accounts & Authentication

The App does not require or support user accounts, sign-ups, or login. There is no authentication system. All data is stored locally on your device.

## Third-Party Services

The App uses the following third-party services:

- **Google Gemini AI** — Food and menu photos are sent through our backend server to Google's Gemini API for nutritional analysis. When scanning menus, approximate location data may also be included to help identify the restaurant's cuisine. The app platform and version are also sent with each request for diagnostic purposes. No other data (profile, health, nutrition logs) is sent. Google's use of this data is governed by their [API Terms of Service](https://ai.google.dev/gemini-api/terms) and [Privacy Policy](https://policies.google.com/privacy).

- **Google AdMob** — The App displays non-personalized (contextual) advertisements provided by Google AdMob. AdMob may collect limited device information (such as device model, OS version, and IP address) to serve and measure ads. The App does not request access to Apple's advertising identifier (IDFA). Ad data is handled by Google and is governed by [Google's Privacy Policy](https://policies.google.com/privacy). Where required by regional regulations (e.g., GDPR), the App uses Google's User Messaging Platform (UMP) to present a consent form.

The App does not include any analytics SDKs, crash reporting tools, or other third-party integrations beyond those listed above.

## Data Storage & Security

- All personal, nutritional, and health data is stored on-device only using Apple's SwiftData framework.
- Food and menu photos are stored in the app's local Documents directory.
- Network communication uses HTTPS (TLS encryption).

## Data Retention & Deletion

Your data remains on your device for as long as you choose to keep it. You can:

- Delete individual food entries or entire meal groups from within the App
- Delete stored menu scans from within the App
- Delete all App data by uninstalling the App from your device

The only data stored on our server is anonymous device attestation keys used to verify the authenticity of the App. These keys contain no personal information and cannot be used to identify you. No personal, nutritional, health, or profile data is stored remotely, so there is nothing personal to request deletion of.

## Children's Privacy

The App is not directed at children under the age of 13 and does not knowingly collect personal information from children. The App's age rating is 4+, and it collects no identifying information from any user.

## Changes to This Policy

If this Privacy Policy is updated, the revised version will be posted here with an updated "Last Updated" date. Continued use of the App after changes constitutes acceptance of the revised policy.

## Contact

If you have questions about this Privacy Policy, please contact:

- **Email:** pning80.git@gmail.com

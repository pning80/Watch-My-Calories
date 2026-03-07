# Privacy Policy — Calorie Watcher

**Last Updated: March 7, 2026**

Calorie Watcher ("the App") is a mobile application that helps you track your daily calorie intake using AI-powered food recognition. This Privacy Policy explains what data the App collects, how it is used, and your choices.

## Data We Collect

### Personal Profile Data

You may optionally enter profile information including height, weight, age, gender, and activity level. This data is used solely to calculate your recommended daily calorie goal using the Mifflin-St Jeor formula. **This data is stored only on your device and is never transmitted to any server.**

### Food & Nutrition Data

When you log meals — either manually or via camera scan — the App stores food names, quantities, calorie counts, macronutrient values (protein, carbs, fat), timestamps, and meal types. **This data is stored only on your device and is never transmitted to any server.**

### Food Photos

When you use the camera scan feature, photos of your food are sent to our backend server, which forwards them to Google Gemini AI for nutritional analysis. The photos are:

- Transmitted over HTTPS (encrypted in transit)
- Used solely to identify foods and estimate nutritional content
- **Not stored on our server** — they are forwarded to Google Gemini in real time and discarded
- Subject to [Google's API Terms of Service](https://ai.google.dev/gemini-api/terms) and [Privacy Policy](https://policies.google.com/privacy)

The photo from each meal session is saved locally on your device for your reference. You can delete it at any time by deleting the food entry.

### Health Data (Apple HealthKit)

With your permission, the App reads **Active Energy Burned** from Apple Health. This data is used on-device to adjust your daily calorie goal based on your physical activity. **Health data is never transmitted to any server, shared with third parties, or used for advertising.**

The App does not write any data to Apple Health.

### App Preferences

Your selected app theme (Light, Dark, or System) and unit system preference (US Customary or Metric) are stored locally on your device using standard app preferences.

## Data We Do NOT Collect

The App does **not** collect, store, or transmit any of the following:

- Names, email addresses, or contact information
- Location data
- Device identifiers or advertising IDs
- Usage analytics, crash reports, or diagnostics
- Browsing or search history
- Financial or payment information

## Accounts & Authentication

The App does not require or support user accounts, sign-ups, or login. There is no authentication system. All data is stored locally on your device.

## Third-Party Services

The App uses one third-party service:

- **Google Gemini AI** — Food photos are sent through our backend server to Google's Gemini API for nutritional analysis. No other data (profile, health, nutrition logs) is sent. Google's use of this data is governed by their [API Terms of Service](https://ai.google.dev/gemini-api/terms) and [Privacy Policy](https://policies.google.com/privacy).

The App does not include any analytics SDKs, advertising frameworks, crash reporting tools, or other third-party integrations.

## Data Storage & Security

- All personal, nutritional, and health data is stored on-device only using Apple's SwiftData framework.
- Food photos are stored in the app's local Documents directory.
- Network communication uses HTTPS (TLS encryption).

## Data Retention & Deletion

Your data remains on your device for as long as you choose to keep it. You can:

- Delete individual food entries or entire meal groups from within the App
- Delete all App data by uninstalling the App from your device

Since no data is stored on our servers, there is nothing to request deletion of remotely.

## Children's Privacy

The App is not directed at children under the age of 13 and does not knowingly collect personal information from children. The App's age rating is 4+, and it collects no identifying information from any user.

## Changes to This Policy

If this Privacy Policy is updated, the revised version will be posted here with an updated "Last Updated" date. Continued use of the App after changes constitutes acceptance of the revised policy.

## Contact

If you have questions about this Privacy Policy, please contact:

- **Email:** pning80.git@gmail.com

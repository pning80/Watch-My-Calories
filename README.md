# Watch My Calories

Track your daily calories effortlessly with AI-powered food recognition. Snap a
photo of your meal and Watch My Calories identifies each food item, estimates
portions, and calculates calories and macronutrients — all while keeping your
data on your device.

This repository contains **two independent native apps** (iOS and Android) that
mirror the same features, plus a small backend proxy. There is no shared code
between the platforms.

## Features

- **AI food analysis** — Snap a photo; Google Gemini identifies foods and
  estimates calories, protein, carbs, and fat.
- **Smart meal tracking** — Entries auto-categorize as Breakfast, Lunch, Dinner,
  or Snack based on the time of day.
- **Daily dashboard** — See intake at a glance with a progress card toward your
  daily goal.
- **Health integration** — Reads active energy burned (Apple Health / Health
  Connect) to adjust your effective calorie target.
- **Personalized goals** — Calculates a recommended daily target from your
  profile using the Mifflin-St Jeor formula.
- **Meal history** — Browse, edit, and delete past entries by date and meal type.
- **Manual entry** — Log food by hand with full control over name, calories, and
  macros.
- **Privacy first** — All data stays on-device. No accounts, no sign-ups, no
  analytics. Food photos are sent to Google Gemini solely for analysis and are
  not stored.

## Project layout

| Path | What it is |
|------|------------|
| `WatchMyCalories/` | iOS app — SwiftUI + SwiftData (iOS 17+), Apple frameworks only |
| `WatchMyCaloriesAndroid/` | Android app — Jetpack Compose + Room (API 26+) |
| `Backend/` | Node.js + TypeScript proxy on Google Cloud Run that protects the Gemini API key |
| `shared-fixtures/` | Cross-platform numeric test fixtures consumed by both app test suites |

Both apps POST food images to the same backend, which forwards them to Gemini.
All other data is on-device. See [`CLAUDE.md`](CLAUDE.md) for a detailed
architecture overview.

## Build & run

### iOS (`WatchMyCalories/`)
- Open `WatchMyCalories/WatchMyCalories.xcodeproj` in Xcode and run (Cmd+R).
- No SPM/CocoaPods third-party deps beyond Google Mobile Ads (SPM).
- Camera and HealthKit need a physical device; the simulator is guarded with
  `#if targetEnvironment(simulator)`.

### Android (`WatchMyCaloriesAndroid/`)
```bash
cd WatchMyCaloriesAndroid
./gradlew assembleDebug      # Build
./gradlew installDebug       # Install on a connected device
./gradlew test               # Unit tests
```
- Camera needs a physical device (API 26+); Health Connect needs Android 14+.

### Backend (`Backend/`)
```bash
cd Backend
npm install
npm test                     # Self-contained — no network/env vars needed
cp .env.example .env         # then fill in your own values
npm run dev                  # Local server
```
Deploy to Cloud Run with `./deploy.sh dev` / `./deploy.sh prod` (requires your
own GCP project and `.env.dev` / `.env.prod`).

## Configuration & keys

This repo ships **no secrets**. To run your own instance you provide your own:

- **Gemini API key** — set `GEMINI_API_KEY` in `Backend/.env` (see
  `Backend/.env.example`). The key lives only on the backend, never in the apps.
- **AdMob unit IDs** — the repo builds with Google's published **test** ad IDs by
  default. To use real ads, supply your own IDs via the gitignored override
  files (`Ads/AdMob-iOS.local.xcconfig`, and `Ads/AdMob-Android.properties` —
  copy from `Ads/AdMob-Android.properties.example` — or `local.properties`).
- **Dev backend key** — for simulator/emulator builds that can't attest, an
  optional legacy `x-backend-key` is sourced from a single place,
  `Backend/.env.dev` (`APP_BACKEND_API_KEY`). After setting it, run
  `scripts/sync-dev-backend-key.sh` to propagate it to the (gitignored) client
  configs. Production builds don't use it — they authenticate with App Attest /
  Play Integrity.
- **Signing / attestation** — iOS App Attest and Android Play Integrity require
  your own Apple Team ID and GCP project; see `Backend/.env.example`.

## License

Licensed under the [Apache License 2.0](LICENSE). See [`NOTICE`](NOTICE).

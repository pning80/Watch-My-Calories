# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Calorie Watcher is a dual-platform native mobile app (iOS + Android) that estimates food calories using Google Gemini AI, tracks daily consumption, and displays calorie history. There is **no shared code** between platforms — each is an independent native project with mirrored features.

## Build & Run

### iOS (`CalorieWatcher/`)
- Open `CalorieWatcher/CalorieWatcher.xcodeproj` in Xcode
- Requires physical device for camera and HealthKit (simulator has limited support with `#if targetEnvironment(simulator)` guards)
- No SPM or CocoaPods dependencies — all Apple frameworks only
- Build/run: Cmd+R in Xcode
- No test targets currently configured

### Android (`CalorieWatcherAndroid/`)
```bash
cd CalorieWatcherAndroid
./gradlew assembleDebug          # Build
./gradlew installDebug           # Install on connected device
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumentation tests (requires device)
./gradlew lint                   # Lint
```
- Requires physical device (API 26+) for camera; Health Connect needs Android 14+
- Test device: Pixel 9a

## Architecture

### iOS — SwiftUI + SwiftData (iOS 17+)
- **Persistence**: SwiftData `@Model` classes (`UserProfile`, `FoodEntry` in `DataModels.swift`). Views query via `@Query` and `@Environment(\.modelContext)`.
- **Singletons**: `AppEnvironment` (holds `EstimationService` protocol, injectable for testing via `MockEstimationService`), `SettingsStore` (API key in Keychain, model name in UserDefaults).
- **Camera flow**: `CameraManager` (AVFoundation) → `CameraView` → `EstimationReviewView` (calls Gemini, saves to SwiftData).
- **Images**: Stored in Documents directory, keyed by UUID in `FoodEntry.imageID`.
- **Health**: `HealthKitManager` reads `activeEnergyBurned` with background delivery.

### Android — MVVM + Hilt + Jetpack Compose + Room
- **Repository layer**: `GeminiRepository` (remote API → local AI fallback), `HealthRepository`, `SettingsRepository` — all Hilt-injected.
- **ViewModels**: Expose `StateFlow<UiState>` to composables. No direct DAO access from UI.
- **Navigation**: Single `NavHost` in `MainActivity`.
- **Secure storage**: `EncryptedSharedPreferences` for API key; Jetpack DataStore for preferences.
- **DI**: Hilt module in `di/AppModule.kt` provides `AppDatabase` and DAOs.

## Key Data Conventions

- **Units**: UI displays Imperial (lbs, ft/in); storage is always **metric** (kg, cm). Conversion happens at the view layer.
- **MealType auto-assignment**: Breakfast 7–9, Lunch 11–14, Dinner 17–20, Snack otherwise (based on entry timestamp).
- **Calorie goal**: Mifflin-St Jeor BMR × activity multiplier + active calories burned from HealthKit/Health Connect.
- **No backend**: All data is on-device. Only food images are sent to Gemini API for analysis.

## Gemini API Integration

Both platforms call the same REST endpoint:
```
POST https://generativelanguage.googleapis.com/v1beta/{model}:generateContent
```
with base64-encoded images + a text prompt. Response is parsed as JSON with `{ name, quantity, calories, protein, carbs, fat, confidence }` items.

- API key is user-provided at runtime (Settings screen), stored in Keychain (iOS) / EncryptedSharedPreferences (Android).
- Available models are fetched dynamically; default preference is a "flash" or "lite" model.

## Key Files

| iOS | Purpose |
|-----|---------|
| `CalorieWatcher/Services.swift` | Gemini API client + `EstimationService` protocol |
| `CalorieWatcher/DataModels.swift` | SwiftData models (`UserProfile`, `FoodEntry`, `MealType`) |
| `CalorieWatcher/DashboardView.swift` | Main "Today" screen |
| `CalorieWatcher/DesignSystem.swift` | Color palette and view modifiers |
| `CalorieWatcher/Components.swift` | Shared UI components (`HeroSummaryCard`, `FoodEntryCard`) |

| Android | Purpose |
|---------|---------|
| `data/repository/GeminiRepository.kt` | AI layer with remote + local fallback |
| `data/model/FoodEntry.kt` | Room entity + `MealType` enum |
| `data/model/UserProfile.kt` | Room entity |
| `ui/theme/Color.kt` | Color system (mirrors iOS palette) |
| `util/CalorieCalculator.kt` | BMR/TDEE computation |
| `app/build.gradle.kts` | Full dependency manifest |

## Important Notes

- **Dual codebase**: Changes to shared logic (e.g., calorie formula, meal time windows) must be applied to both platforms independently.
- **Legacy iOS files**: `TodayView.swift`, `CaptureView.swift`, `CoreDataService.swift`, and `GeminiEstimationService.swift` are stubs or superseded — the active implementations are `DashboardView.swift` and `Services.swift`.
- **Android local.properties**: Can pre-seed `GEMINI_API_KEY=...` for development.

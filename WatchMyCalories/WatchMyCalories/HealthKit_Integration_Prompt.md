# HealthKit Integration Prompts

This document contains a sequence of prompts designed to guide Xcode Intelligence in adding HealthKit integration to the **Calorie-Watcher** app. The goal is to fetch "Active Energy Burned" and use it to offset the daily calorie intake.

> [!IMPORTANT]
> **Before Starting**:
> 1.  Add the **HealthKit** capability in your project's "Signing & Capabilities" tab.
> 2.  Ensure your target's `Info.plist` (or Info tab) has the following keys:
>     -   `NSHealthShareUsageDescription`: "We need access to your active energy data to adjust your daily calorie goals."

## Phase 1: Logic & Data Fetching

**Prompt 1: HealthKit Manager**
> Create a new file named `HealthKitManager.swift` in the `WatchMyCalories` folder.
> Implement a class `HealthKitManager` that conforms to `ObservableObject`.
>
> **Requirements:**
> -   **Imports**: `import HealthKit`, `import SwiftUI`.
> -   **Properties**:
>     -   `healthStore`: A private `HKHealthStore` instance.
>     -   `@Published var activeEnergyBurned: Double = 0.0`: Stores the total calories burned today.
>     -   `@Published var isAuthorized: Bool = false`: Tracks authorization status.
> -   **Methods**:
>     -   `checkAuthorizationStatus()`: Check if HealthKit is available on the device.
>     -   `requestAuthorization()`: Requests permission to read `HKQuantityType.activeEnergyBurned`. Handle success/failure and update `isAuthorized` on the main thread.
>     -   `fetchTodayEnergyBurned()`: strict query for `activeEnergyBurned` samples starting from the beginning of the current day (`Calendar.current.startOfDay(for: Date())`) to now.
>         -   Use `HKStatisticsQuery` with `.cumulativeSum`.
>         -   Convert the result to Kilocalories (`HKUnit.kilocalorie()`).
>         -   Update `activeEnergyBurned` on the main thread.
>     -   `startObserving()`: Set up a background observer query (`HKObserverQuery`) to automatically refetch data when HealthKit updates the energy burned samples.
> -   **Initialization**: Check `HKHealthStore.isHealthDataAvailable()` before initializing.

## Phase 2: UI Integration (Dashboard & Components)

**Prompt 2: Update Components & Dashboard**
> Integrate `HealthKitManager` into the UI.
>
> **1. Update `HeroSummaryCard` in `Components.swift`**:
> -   Modify `HeroSummaryCard` struct to accept an optional `burnedCalories: Double` (default to 0).
> -   Update the `progress` calculation: `consumed / (targetCalories + burnedCalories)`.
> -   Update the displayed stats: Add a new `StatRow` for "Burned" with a flame icon and orange color.
> -   Update the "Remaining" calculation: `(targetCalories + burnedCalories) - consumed`.
>
> **2. Update `DashboardView.swift`**:
> -   Add `@StateObject private var healthKitManager = HealthKitManager()` to `DashboardView`.
> -   In `onAppear`, call `healthKitManager.requestAuthorization()` followed by `healthKitManager.fetchTodayEnergyBurned()`.
> -   Pass `healthKitManager.activeEnergyBurned` to the invalid `HeroSummaryCard` initializer.
>
> **3. Add "Pull to Refresh"**:
> -   Add `refreshable` to the ScrollView in `DashboardView` to trigger `healthKitManager.fetchTodayEnergyBurned()`.

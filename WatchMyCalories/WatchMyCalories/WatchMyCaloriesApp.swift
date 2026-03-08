//
//  WatchMyCaloriesApp.swift
//  WatchMyCalories
//
//  Created by pning80.git on 2/9/26.
//

import SwiftUI
import SwiftData

@main
struct WatchMyCaloriesApp: App {
    static var isUITesting: Bool {
        ProcessInfo.processInfo.arguments.contains("--uitesting")
    }

    private static var shouldSeedData: Bool {
        ProcessInfo.processInfo.arguments.contains("--seed-data")
    }

    private static var shouldSeedHistory: Bool {
        ProcessInfo.processInfo.arguments.contains("--seed-history")
    }

    static var shouldResetOnboarding: Bool {
        ProcessInfo.processInfo.arguments.contains("--reset-onboarding")
    }

    let container: ModelContainer?

    init() {
        if Self.shouldResetOnboarding {
            UserDefaults.standard.removeObject(forKey: "hasCompletedOnboarding")
            SettingsStore.shared.hasCompletedOnboarding = false
        }

        let schema = Schema([UserProfile.self, FoodEntry.self])

        if Self.isUITesting {
            // Always use in-memory store for UI tests
            container = try? ModelContainer(for: schema, configurations: [
                ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
            ])
            AppEnvironment.shared.swapService(MockEstimationService())
        } else if let persistent = try? ModelContainer(for: schema, configurations: [
            ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
        ]) {
            container = persistent
        } else {
            container = try? ModelContainer(for: schema, configurations: [
                ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
            ])
        }
    }

    @ObservedObject private var store = SettingsStore.shared

    var body: some Scene {
        WindowGroup {
            if let container {
                Group {
                    if Self.isUITesting || store.hasCompletedOnboarding {
                        ContentView()
                    } else {
                        OnboardingView()
                    }
                }
                .environmentObject(AppEnvironment.shared)
                .modelContainer(container)
                .onAppear {
                    if Self.shouldSeedData {
                        seedTestData(container: container)
                    }
                    if Self.shouldSeedHistory {
                        seedHistoryData(container: container)
                    }
                }
            } else {
                Text("Unable to load data. Please restart the app.")
                    .foregroundStyle(.secondary)
                    .padding()
            }
        }
    }

    @MainActor
    private func seedHistoryData(container: ModelContainer) {
        let context = container.mainContext

        // Only seed if empty
        let descriptor = FetchDescriptor<FoodEntry>()
        guard (try? context.fetchCount(descriptor)) == 0 else { return }

        // Seed same profile + today entries as seedTestData
        let profile = UserProfile(
            height: 175,
            weight: 70,
            age: 30,
            gender: .male,
            activityLevel: .moderatelyActive,
            targetCalories: 2200
        )
        context.insert(profile)

        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())

        let oatmeal = FoodEntry(
            name: "Oatmeal with Berries",
            calories: 300,
            quantity: "1 bowl",
            timestamp: calendar.date(bySettingHour: 8, minute: 0, second: 0, of: today)!,
            protein: 10, carbs: 50, fat: 6,
            mealType: .breakfast
        )
        context.insert(oatmeal)

        let salad = FoodEntry(
            name: "Chicken Salad",
            calories: 450,
            quantity: "1 plate",
            timestamp: calendar.date(bySettingHour: 12, minute: 30, second: 0, of: today)!,
            protein: 35, carbs: 20, fat: 18,
            mealType: .lunch
        )
        context.insert(salad)

        // Yesterday's entries
        let yesterday = calendar.date(byAdding: .day, value: -1, to: today)!

        let pasta = FoodEntry(
            name: "Pasta Bolognese",
            calories: 600,
            quantity: "1 plate",
            timestamp: calendar.date(bySettingHour: 18, minute: 0, second: 0, of: yesterday)!,
            protein: 25, carbs: 70, fat: 20,
            mealType: .dinner
        )
        context.insert(pasta)

        let latte = FoodEntry(
            name: "Latte",
            calories: 150,
            quantity: "1 cup",
            timestamp: calendar.date(bySettingHour: 8, minute: 30, second: 0, of: yesterday)!,
            protein: 8, carbs: 12, fat: 6,
            mealType: .breakfast
        )
        context.insert(latte)

        // 2-days-ago entry
        let twoDaysAgo = calendar.date(byAdding: .day, value: -2, to: today)!

        let sandwich = FoodEntry(
            name: "Turkey Sandwich",
            calories: 400,
            quantity: "1 sandwich",
            timestamp: calendar.date(bySettingHour: 12, minute: 0, second: 0, of: twoDaysAgo)!,
            protein: 30, carbs: 35, fat: 12,
            mealType: .lunch
        )
        context.insert(sandwich)
    }

    @MainActor
    private func seedTestData(container: ModelContainer) {
        let context = container.mainContext

        // Only seed if empty
        let descriptor = FetchDescriptor<FoodEntry>()
        guard (try? context.fetchCount(descriptor)) == 0 else { return }

        // Seed a user profile
        let profile = UserProfile(
            height: 175,
            weight: 70,
            age: 30,
            gender: .male,
            activityLevel: .moderatelyActive,
            targetCalories: 2200
        )
        context.insert(profile)

        // Seed food entries for today
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())

        let oatmeal = FoodEntry(
            name: "Oatmeal with Berries",
            calories: 300,
            quantity: "1 bowl",
            timestamp: calendar.date(bySettingHour: 8, minute: 0, second: 0, of: today)!,
            protein: 10, carbs: 50, fat: 6,
            mealType: .breakfast
        )
        context.insert(oatmeal)

        let salad = FoodEntry(
            name: "Chicken Salad",
            calories: 450,
            quantity: "1 plate",
            timestamp: calendar.date(bySettingHour: 12, minute: 30, second: 0, of: today)!,
            protein: 35, carbs: 20, fat: 18,
            mealType: .lunch
        )
        context.insert(salad)
    }
}

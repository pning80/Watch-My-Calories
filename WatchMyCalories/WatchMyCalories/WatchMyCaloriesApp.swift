//
//  WatchMyCaloriesApp.swift
//  WatchMyCalories
//
//  Created by pning80.git on 2/9/26.
//

import SwiftUI
import SwiftData
import UIKit

@main
struct WatchMyCaloriesApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    static var isUITesting: Bool {
        ProcessInfo.processInfo.arguments.contains("--uitesting")
    }

    private static var shouldSeedData: Bool {
        ProcessInfo.processInfo.arguments.contains("--seed-data")
    }

    private static var shouldSeedHistory: Bool {
        ProcessInfo.processInfo.arguments.contains("--seed-history")
    }

    private static var shouldSeedMenuScans: Bool {
        ProcessInfo.processInfo.arguments.contains("--seed-menu-scans")
    }

    private static var shouldSeedMultiItemMeal: Bool {
        ProcessInfo.processInfo.arguments.contains("--seed-multi-item-meal")
    }

    private static var shouldSeedWithImage: Bool {
        ProcessInfo.processInfo.arguments.contains("--seed-with-image")
    }

    private static var shouldPreAcceptAIConsent: Bool {
        ProcessInfo.processInfo.arguments.contains("--ai-consent-accepted")
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

        let schema = Schema([UserProfile.self, FoodEntry.self, MenuScan.self])

        if Self.isUITesting {
            // Reset transient flags so tests get a consistent initial state
            UserDefaults.standard.removeObject(forKey: "hasSeenEstimateDisclaimer")
            SettingsStore.shared.hasSeenEstimateDisclaimer = false
            UserDefaults.standard.removeObject(forKey: "aiConsentStatus")
            SettingsStore.shared.aiConsent = .notAsked

            if Self.shouldPreAcceptAIConsent {
                SettingsStore.shared.saveAIConsent(.accepted)
            }

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
                .task(id: store.hasCompletedOnboarding) {
                    guard !Self.isUITesting else { return }
                    guard store.hasCompletedOnboarding else { return }
                    await AdManager.shared.enableAds()
                }
                .onAppear {
                    if Self.shouldSeedData {
                        seedTestData(container: container)
                    }
                    if Self.shouldSeedHistory {
                        seedHistoryData(container: container)
                    }
                    if Self.shouldSeedMenuScans {
                        seedMenuScans(container: container)
                    }
                    if Self.shouldSeedMultiItemMeal {
                        seedMultiItemMeal(container: container)
                    }
                    if Self.shouldSeedWithImage {
                        seedWithImage(container: container)
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

    @MainActor
    private func seedMenuScans(container: ModelContainer) {
        let context = container.mainContext

        // Only seed if no menu scans yet
        let descriptor = FetchDescriptor<MenuScan>()
        guard (try? context.fetchCount(descriptor)) == 0 else { return }

        let calendar = Calendar.current
        let now = Date()

        let scan1 = MenuScan(
            restaurantName: "Mock Italian Place",
            imageID: nil,
            timestamp: now,
            items: [
                MenuItemResult(name: "Margherita Pizza", description: "Classic tomato + mozzarella",
                               calories: 800, protein: 30, carbs: 90, fat: 30),
                MenuItemResult(name: "Caesar Salad", description: nil,
                               calories: 350, protein: 15, carbs: 12, fat: 25)
            ]
        )
        context.insert(scan1)

        let scan2 = MenuScan(
            restaurantName: "Mock Sushi Bar",
            imageID: nil,
            timestamp: calendar.date(byAdding: .day, value: -1, to: now)!,
            items: [
                MenuItemResult(name: "Salmon Roll", description: nil,
                               calories: 320, protein: 18, carbs: 38, fat: 10),
                MenuItemResult(name: "Tuna Sashimi", description: "6 pieces",
                               calories: 180, protein: 28, carbs: 0, fat: 6)
            ]
        )
        context.insert(scan2)
    }

    @MainActor
    private func seedMultiItemMeal(container: ModelContainer) {
        let context = container.mainContext

        // Only seed if empty
        let descriptor = FetchDescriptor<FoodEntry>()
        guard (try? context.fetchCount(descriptor)) == 0 else { return }

        // Seed user profile so dashboard renders normally
        let profile = UserProfile(
            height: 175, weight: 70, age: 30,
            gender: .male, activityLevel: .moderatelyActive,
            targetCalories: 2200
        )
        context.insert(profile)

        let calendar = Calendar.current
        let lunchTime = calendar.date(bySettingHour: 12, minute: 30, second: 0, of: calendar.startOfDay(for: Date()))!
        let mealName = "Mock Bento Box"

        // Three items grouped by mealName — exercises FoodEntryGroupCard multi-item path
        let rice = FoodEntry(
            name: "Brown Rice", calories: 220, quantity: "1 cup",
            timestamp: lunchTime, protein: 5, carbs: 45, fat: 2,
            mealName: mealName, mealType: .lunch
        )
        let chicken = FoodEntry(
            name: "Teriyaki Chicken", calories: 350, quantity: "5 oz",
            timestamp: lunchTime, protein: 30, carbs: 12, fat: 18,
            mealName: mealName, mealType: .lunch
        )
        let edamame = FoodEntry(
            name: "Edamame", calories: 120, quantity: "1 cup",
            timestamp: lunchTime, protein: 11, carbs: 9, fat: 5,
            mealName: mealName, mealType: .lunch
        )
        context.insert(rice)
        context.insert(chicken)
        context.insert(edamame)
    }

    @MainActor
    private func seedWithImage(container: ModelContainer) {
        let context = container.mainContext

        // Only seed if empty
        let descriptor = FetchDescriptor<FoodEntry>()
        guard (try? context.fetchCount(descriptor)) == 0 else { return }

        // Seed user profile
        let profile = UserProfile(
            height: 175, weight: 70, age: 30,
            gender: .male, activityLevel: .moderatelyActive,
            targetCalories: 2200
        )
        context.insert(profile)

        // Generate a tiny synthetic JPEG, write to Documents, attach via imageID
        let imageID = UUID()
        let image = synthesizeTestImage(size: CGSize(width: 320, height: 240))
        if let data = image.jpegData(compressionQuality: 0.8) {
            let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let url = docs.appendingPathComponent("\(imageID.uuidString).jpg")
            try? data.write(to: url)
        }

        let calendar = Calendar.current
        let lunchTime = calendar.date(bySettingHour: 12, minute: 30, second: 0, of: calendar.startOfDay(for: Date()))!
        let entry = FoodEntry(
            name: "Mock Lunch with Photo",
            calories: 500, quantity: "1 plate",
            timestamp: lunchTime,
            protein: 30, carbs: 50, fat: 15,
            imageID: imageID,
            mealType: .lunch
        )
        context.insert(entry)
    }

    private func synthesizeTestImage(size: CGSize) -> UIImage {
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { ctx in
            UIColor.systemGreen.setFill()
            ctx.fill(CGRect(origin: .zero, size: size))
            UIColor.white.setStroke()
            ctx.cgContext.setLineWidth(4)
            ctx.cgContext.stroke(CGRect(origin: .init(x: 8, y: 8),
                                        size: .init(width: size.width - 16, height: size.height - 16)))
        }
    }
}

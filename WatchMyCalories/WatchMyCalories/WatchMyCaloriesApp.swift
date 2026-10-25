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

    private static var shouldSeedOverGoal: Bool {
        ProcessInfo.processInfo.arguments.contains("--seed-over-goal")
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

    /// DEBUG only: fills the six days before today with realistic meals in the
    /// persistent store so the History trend looks lived-in for App Store screenshots.
    private static var shouldSeedScreenshotWeek: Bool {
        #if DEBUG
        ProcessInfo.processInfo.arguments.contains("--screenshot-week")
        #else
        false
        #endif
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
            AppEnvironment.shared.swapMenuAnalysisService(MockMenuAnalysisService())
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
                    if Self.shouldSeedOverGoal {
                        seedOverGoalData(container: container)
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
                    if Self.shouldSeedScreenshotWeek {
                        seedScreenshotWeek(container: container)
                    }
                }
            } else {
                Text("Unable to load data. Please restart the app.")
                    .foregroundStyle(.secondary)
                    .padding()
            }
        }
    }

    /// Adds meals to each of the six days before today that has no entries yet,
    /// so relaunching with the flag never duplicates. Today is left untouched.
    @MainActor
    private func seedScreenshotWeek(container: ModelContainer) {
        let context = container.mainContext
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())

        typealias Meal = (name: String, quantity: String, hour: Int, minute: Int, calories: Double, protein: Double, carbs: Double, fat: Double, type: MealType)
        let week: [[Meal]] = [
            [("Greek Yogurt with Granola", "1 bowl", 7, 45, 380, 22, 48, 10, .breakfast),
             ("Grilled Chicken Caesar Salad", "1 plate", 12, 30, 520, 42, 18, 30, .lunch),
             ("Salmon with Roasted Vegetables", "1 plate", 18, 45, 640, 44, 32, 34, .dinner),
             ("Apple with Almond Butter", "1 apple", 15, 30, 290, 7, 30, 17, .snack)],
            [("Avocado Toast with Egg", "2 slices", 8, 0, 420, 18, 36, 22, .breakfast),
             ("Turkey Sandwich", "1 sandwich", 12, 15, 480, 34, 46, 16, .lunch),
             ("Beef Stir-Fry with Rice", "1 plate", 19, 0, 720, 38, 82, 22, .dinner),
             ("Latte", "1 cup", 15, 0, 190, 10, 18, 8, .snack)],
            [("Oatmeal with Berries", "1 bowl", 7, 30, 340, 11, 58, 7, .breakfast),
             ("Chicken Burrito Bowl", "1 bowl", 12, 45, 690, 42, 74, 22, .lunch),
             ("Shrimp Tacos", "3 tacos", 18, 30, 560, 32, 52, 22, .dinner)],
            [("Scrambled Eggs and Toast", "1 plate", 8, 15, 410, 24, 30, 20, .breakfast),
             ("Pork Katsu Curry", "1 plate", 12, 30, 780, 34, 92, 28, .lunch),
             ("Miso Soup and Edamame", "1 bowl", 18, 45, 300, 20, 24, 12, .dinner),
             ("Protein Bar", "1 bar", 16, 0, 210, 20, 22, 7, .snack)],
            [("Smoothie Bowl", "1 bowl", 7, 45, 390, 14, 66, 8, .breakfast),
             ("Margherita Pizza", "2 slices", 12, 30, 560, 24, 66, 22, .lunch),
             ("Grilled Lobster Tail", "1 plate", 19, 15, 580, 46, 26, 32, .dinner),
             ("Mixed Nuts", "1 handful", 15, 30, 200, 6, 8, 17, .snack)],
            [("Banana Pancakes", "3 pancakes", 8, 30, 460, 12, 78, 11, .breakfast),
             ("Seafood Paella", "1 plate", 13, 0, 650, 36, 72, 22, .lunch),
             ("Garden Salad with Tuna", "1 bowl", 18, 30, 430, 34, 18, 24, .dinner)],
        ]

        for (index, meals) in week.enumerated() {
            let day = calendar.date(byAdding: .day, value: -(index + 1), to: today)!
            let nextDay = calendar.date(byAdding: .day, value: 1, to: day)!
            let existing = FetchDescriptor<FoodEntry>(predicate: #Predicate { $0.timestamp >= day && $0.timestamp < nextDay })
            guard (try? context.fetchCount(existing)) == 0 else { continue }

            for meal in meals {
                context.insert(FoodEntry(
                    name: meal.name,
                    calories: meal.calories,
                    quantity: meal.quantity,
                    timestamp: calendar.date(bySettingHour: meal.hour, minute: meal.minute, second: 0, of: day)!,
                    protein: meal.protein, carbs: meal.carbs, fat: meal.fat,
                    mealType: meal.type
                ))
            }
        }
        try? context.save()
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

    /// Today's consumption exceeds the effective goal: 1200 target + 456 UI-test burned
    /// = 1656, consumed 2000 → hero shows the over-goal state ("Over by 344").
    @MainActor
    private func seedOverGoalData(container: ModelContainer) {
        let context = container.mainContext

        // Only seed if empty
        let descriptor = FetchDescriptor<FoodEntry>()
        guard (try? context.fetchCount(descriptor)) == 0 else { return }

        let profile = UserProfile(
            height: 175,
            weight: 70,
            age: 30,
            gender: .male,
            activityLevel: .moderatelyActive,
            targetCalories: 1200
        )
        context.insert(profile)

        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())

        let pancakes = FoodEntry(
            name: "Pancake Stack",
            calories: 1000,
            quantity: "1 plate",
            timestamp: calendar.date(bySettingHour: 8, minute: 0, second: 0, of: today)!,
            protein: 20, carbs: 140, fat: 38,
            mealType: .breakfast
        )
        context.insert(pancakes)

        let burger = FoodEntry(
            name: "Double Cheeseburger",
            calories: 1000,
            quantity: "1 burger",
            timestamp: calendar.date(bySettingHour: 12, minute: 30, second: 0, of: today)!,
            protein: 50, carbs: 60, fat: 60,
            mealType: .lunch
        )
        context.insert(burger)
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

        // The app groups a multi-item meal by shared imageID (one photo → several food
        // items), with mealName as the editable group label. Attach one synthetic image
        // so the three items form a single FoodEntryGroupCard (the multi-item path).
        let imageID = UUID()
        let image = synthesizeTestImage(size: CGSize(width: 320, height: 240))
        if let data = image.jpegData(compressionQuality: 0.8) {
            let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            try? data.write(to: docs.appendingPathComponent("\(imageID.uuidString).jpg"))
        }

        let rice = FoodEntry(
            name: "Brown Rice", calories: 220, quantity: "1 cup",
            timestamp: lunchTime, protein: 5, carbs: 45, fat: 2,
            imageID: imageID, mealName: mealName, mealType: .lunch
        )
        let chicken = FoodEntry(
            name: "Teriyaki Chicken", calories: 350, quantity: "5 oz",
            timestamp: lunchTime, protein: 30, carbs: 12, fat: 18,
            imageID: imageID, mealName: mealName, mealType: .lunch
        )
        let edamame = FoodEntry(
            name: "Edamame", calories: 120, quantity: "1 cup",
            timestamp: lunchTime, protein: 11, carbs: 9, fat: 5,
            imageID: imageID, mealName: mealName, mealType: .lunch
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

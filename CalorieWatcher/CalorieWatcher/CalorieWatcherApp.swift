//
//  CalorieWatcherApp.swift
//  CalorieWatcher
//
//  Created by pning80.git on 2/9/26.
//

import SwiftUI
import SwiftData

@main
struct CalorieWatcherApp: App {
    let container: ModelContainer

    init() {
        do {
            let schema = Schema([
                UserProfile.self,
                FoodEntry.self,
            ])
            let modelConfiguration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)

            container = try ModelContainer(for: schema, configurations: [modelConfiguration])
        } catch {
            print("Failed to create ModelContainer: \(error)")
            // Fallback for development: Use in-memory if persistent store fails
            let schema = Schema([UserProfile.self, FoodEntry.self])
            let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
            container = try! ModelContainer(for: schema, configurations: [config])
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(AppEnvironment.shared)
        }
        .modelContainer(container)
    }
}

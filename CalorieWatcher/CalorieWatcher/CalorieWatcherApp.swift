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
            // Fallback: Use in-memory store if persistent store fails
            let schema = Schema([UserProfile.self, FoodEntry.self])
            let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
            do {
                container = try ModelContainer(for: schema, configurations: [config])
            } catch {
                fatalError("Failed to create both persistent and in-memory ModelContainer: \(error)")
            }
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

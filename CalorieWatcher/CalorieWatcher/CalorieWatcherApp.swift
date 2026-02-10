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
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(AppEnvironment.shared)
        }
        .modelContainer(for: [UserProfile.self, FoodEntry.self])
    }
}

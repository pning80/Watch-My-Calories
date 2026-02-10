//
//  CalorieWatcherApp.swift
//  CalorieWatcher
//
//  Created by pning80.git on 2/9/26.
//

import SwiftUI
import CoreData

@main
struct CalorieWatcherApp: App {
    let persistenceController = PersistenceController.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, persistenceController.container.viewContext)
        }
    }
}

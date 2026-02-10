import SwiftUI
import CoreData

@main
struct CalorieWatcherApp: App {
    let persistenceController = PersistenceController.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, persistenceController.container.viewContext)
                .preferredColorScheme(.dark) // Force Dark Mode
        }
    }
}

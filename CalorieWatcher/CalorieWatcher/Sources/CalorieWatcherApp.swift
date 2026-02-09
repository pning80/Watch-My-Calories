import SwiftUI
import SwiftData

@main
struct CalorieWatcherApp: App {
    @State private var isOnboardingComplete: Bool = false

    init() {
        if let key = KeychainService.load(), !key.isEmpty {
            _isOnboardingComplete = State(initialValue: true)
        }
    }

    var sharedModelContainer: ModelContainer = {
        let schema = Schema([
            // TODO: Add models here
        ])
        let modelConfiguration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)

        do {
            return try ModelContainer(for: schema, configurations: [modelConfiguration])
        } catch {
            fatalError("Could not create ModelContainer: \(error)")
        }
    }()

    var body: some Scene {
        WindowGroup {
            if isOnboardingComplete {
                ContentView()
            } else {
                OnboardingView(isOnboardingComplete: $isOnboardingComplete)
            }
        }
        .modelContainer(sharedModelContainer)
    }
}

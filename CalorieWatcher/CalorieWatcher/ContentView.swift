import SwiftUI
import SwiftData

struct ContentView: View {
    @State private var selectedTab: Tab = .dashboard
    
    // State to trigger scrolling
    @State private var scrollToMeal: MealType?
    
    @ObservedObject private var store = SettingsStore.shared
    
    // State for Settings unsaved changes alerts
    @State private var settingsHasUnsavedChanges: Bool = false
    @State private var showUnsavedWarning: Bool = false
    @State private var pendingTab: Tab? = nil
    
    var tabBinding: Binding<Tab> {
        Binding(
            get: { selectedTab },
            set: { newTab in
                if selectedTab == .settings && newTab != .settings && settingsHasUnsavedChanges {
                    pendingTab = newTab
                    showUnsavedWarning = true
                } else {
                    selectedTab = newTab
                }
            }
        )
    }
    
    enum Tab: Hashable {
        case dashboard, camera, history, settings
    }
    
    var body: some View {
        TabView(selection: tabBinding) {
            DashboardView(selectedTab: $selectedTab, scrollToMeal: $scrollToMeal)
                .tabItem {
                    Label("Today", systemImage: "flame.fill")
                }
                .tag(Tab.dashboard)
            
            CameraRootView(selectedTab: $selectedTab, scrollToMeal: $scrollToMeal)
                .tabItem {
                    Label("Scan", systemImage: "camera.fill")
                }
                .tag(Tab.camera)
            
            HistoryView()
                .tabItem {
                    Label("History", systemImage: "calendar")
                }
                .tag(Tab.history)
            
            SettingsView(selectedTab: $selectedTab, hasUnsavedChanges: $settingsHasUnsavedChanges)
                .tabItem {
                    Label("Settings", systemImage: "gearshape.fill")
                }
                .tag(Tab.settings)
        }
        .alert("Unsaved Changes", isPresented: $showUnsavedWarning) {
            Button("Discard", role: .destructive) {
                if let t = pendingTab {
                    NotificationCenter.default.post(name: Notification.Name("DiscardSettings"), object: nil)
                    selectedTab = t
                }
            }
            Button("Cancel", role: .cancel) {
                pendingTab = nil
            }
        } message: {
            Text("You have unsaved changes. Are you sure you want to leave?")
        }
        .tint(Color.cwPrimary)
        .preferredColorScheme(store.appTheme.colorScheme)
        .onAppear {
            // Configure Tab Bar appearance safely once the view is loaded
            let appearance = UITabBarAppearance()
            appearance.configureWithOpaqueBackground()
            appearance.backgroundColor = UIColor(Color.cwSurface)
            
            UITabBar.appearance().standardAppearance = appearance
            UITabBar.appearance().scrollEdgeAppearance = appearance
        }
    }
}

// Wrapper for Camera to handle navigation to review
struct CameraRootView: View {
    @Binding var selectedTab: ContentView.Tab
    @Binding var scrollToMeal: MealType?
    
    @State private var capturedImages: [UIImage] = []
    @State private var reviewData: [Data]?
    @State private var resetID = UUID()
    
    var body: some View {
        NavigationStack {
            ZStack {
                CameraView { images in
                    self.capturedImages = images
                    self.reviewData = images.compactMap { $0.jpegData(compressionQuality: 0.8) }
                }
                .id(resetID)
            }
            .navigationDestination(isPresented: Binding(
                get: { reviewData != nil },
                set: { if !$0 { reviewData = nil; capturedImages.removeAll() } }
            )) {
                if let data = reviewData {
                    EstimationReviewView(images: data, onDone: {
                        // Switch to dashboard tab on completion
                        selectedTab = .dashboard
                        // Trigger scroll to the current time's meal type
                        scrollToMeal = MealType.from(date: Date())
                    })
                }
            }
        }
        .onAppear {
            capturedImages.removeAll()
            reviewData = nil
            resetID = UUID()
        }
    }
}


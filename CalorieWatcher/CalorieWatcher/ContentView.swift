import SwiftUI
import SwiftData

struct ContentView: View {
    @State private var selectedTab: Tab = .dashboard
    
    // State to trigger scrolling
    @State private var scrollToMeal: MealType?
    
    enum Tab: Hashable {
        case dashboard, camera, history, settings
    }
    
    var body: some View {
        TabView(selection: $selectedTab) {
            DashboardView(scrollToMeal: $scrollToMeal)
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
            
            SettingsView(selectedTab: $selectedTab)
                .tabItem {
                    Label("Settings", systemImage: "gearshape.fill")
                }
                .tag(Tab.settings)
        }
        .tint(Color.cwPrimary)
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


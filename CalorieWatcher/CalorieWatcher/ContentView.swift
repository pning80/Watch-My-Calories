import SwiftUI
import SwiftData

struct ContentView: View {
    @State private var selectedTab: Tab = .dashboard
    
    enum Tab: Hashable {
        case dashboard, camera, history, settings
    }
    
    // Customizing Tab Bar Appearance
    init() {
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(Color.cwSurface)
        
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }
    
    var body: some View {
        TabView(selection: $selectedTab) {
            DashboardView()
                .tabItem {
                    Label("Today", systemImage: "flame.fill")
                }
                .tag(Tab.dashboard)
            
            CameraRootView()
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
        .tint(Color.cwPrimary) // Apply primary brand color to active tab
    }
}

// Wrapper for Camera to handle navigation to review
struct CameraRootView: View {
    @State private var capturedImages: [UIImage] = []
    @State private var reviewData: [Data]?
    
    // Unique ID to force view recreation on tab switch if needed, 
    // or we can use onAppear to clear state.
    @State private var resetID = UUID()
    
    var body: some View {
        NavigationStack {
            ZStack {
                CameraView { images in
                    self.capturedImages = images
                    self.reviewData = images.compactMap { $0.jpegData(compressionQuality: 0.8) }
                }
                .id(resetID) // Force recreation if ID changes
            }
            .navigationDestination(isPresented: Binding(
                get: { reviewData != nil },
                set: { if !$0 { reviewData = nil; capturedImages.removeAll() } }
            )) {
                if let data = reviewData {
                    EstimationReviewView(images: data)
                }
            }
        }
        .onAppear {
            // clear state when tab is selected
            capturedImages.removeAll()
            reviewData = nil
            resetID = UUID() // This forces the CameraView (and its internal CameraManager) to re-init
        }
    }
}


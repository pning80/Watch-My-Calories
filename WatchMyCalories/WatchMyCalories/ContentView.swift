import SwiftUI
import SwiftData

extension Notification.Name {
    static let saveSettings = Notification.Name("SaveSettings")
    static let discardSettings = Notification.Name("DiscardSettings")
}

struct ContentView: View {
    @State private var selectedTab: Tab = .dashboard

    // State to trigger scrolling
    @State private var scrollToMeal: MealType?

    // Photo library flow: route through camera tab
    @State private var photoLibraryRequested: Bool = false
    
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
            DashboardView(selectedTab: $selectedTab, scrollToMeal: $scrollToMeal, photoLibraryRequested: $photoLibraryRequested)
                .tabItem {
                    Label("Today", systemImage: "flame.fill")
                }
                .tag(Tab.dashboard)
            
            CameraRootView(selectedTab: $selectedTab, scrollToMeal: $scrollToMeal, photoLibraryRequested: $photoLibraryRequested)
                .tabItem {
                    Label("Scan", systemImage: "camera.fill")
                }
                .tag(Tab.camera)
            
            HistoryView(selectedTab: $selectedTab)
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
            Button("Save", role: .cancel) {
                if let t = pendingTab {
                    selectedTab = t
                    pendingTab = nil
                    NotificationCenter.default.post(name: .saveSettings, object: nil)
                }
            }
            Button("Discard", role: .destructive) {
                if let t = pendingTab {
                    selectedTab = t
                    pendingTab = nil
                    NotificationCenter.default.post(name: .discardSettings, object: nil)
                }
            }
        } message: {
            Text("Do you want to save or discard your changes?")
        }
        .tint(Color.cwPrimary)
        .preferredColorScheme(store.appTheme.colorScheme)
        .onAppear {
            // Configure Tab Bar appearance safely once the view is loaded
            let appearance = UITabBarAppearance()
            appearance.configureWithOpaqueBackground()
            appearance.backgroundColor = UIColor { tc in
                tc.userInterfaceStyle == .dark ? .secondarySystemBackground : .white
            }
            
            UITabBar.appearance().standardAppearance = appearance
            UITabBar.appearance().scrollEdgeAppearance = appearance
        }
    }
}

// Wrapper for Camera to handle navigation to review
struct CameraRootView: View {
    @Binding var selectedTab: ContentView.Tab
    @Binding var scrollToMeal: MealType?
    @Binding var photoLibraryRequested: Bool
    @StateObject private var cameraManager = CameraManager()
    @State private var capturedImages: [UIImage] = []
    @State private var reviewData: [Data]?
    @State private var capturedDate: Date? = nil
    @State private var selectedMealType: MealType? = nil

    var body: some View {
        NavigationStack {
            ZStack {
                if photoLibraryRequested {
                    PhotoLibraryReviewView(onImagesCaptured: { images, mealType in
                        self.capturedImages = images
                        self.reviewData = images.compactMap { $0.downscaled(maxDimension: 2048).jpegData(compressionQuality: 0.8) }
                        self.capturedDate = Date()
                        self.selectedMealType = mealType
                    }, onCancel: {
                        photoLibraryRequested = false
                        selectedTab = .dashboard
                    })
                } else {
                    CameraView(model: cameraManager) { images, mealType in
                        self.capturedImages = images
                        self.reviewData = images.compactMap { $0.downscaled(maxDimension: 2048).jpegData(compressionQuality: 0.8) }
                        self.capturedDate = Date()
                        self.selectedMealType = mealType
                    }
                }
            }
            .navigationDestination(isPresented: Binding(
                get: { reviewData != nil },
                set: { if !$0 {
                    reviewData = nil
                    capturedImages.removeAll()
                    capturedDate = nil
                    selectedMealType = nil
                    photoLibraryRequested = false
                    cameraManager.reset()
                } }
            )) {
                if let data = reviewData, let date = capturedDate, let mealType = selectedMealType {
                    EstimationReviewView(images: data, captureDate: date, mealType: mealType, onDone: {
                        photoLibraryRequested = false
                        selectedTab = .dashboard
                        scrollToMeal = mealType
                    })
                }
            }
        }
        .onAppear {
            if !photoLibraryRequested {
                capturedImages.removeAll()
                reviewData = nil
                cameraManager.reset()
            }
        }
    }
}

private extension UIImage {
    func downscaled(maxDimension: CGFloat) -> UIImage {
        let maxSide = max(size.width, size.height)
        guard maxSide > maxDimension else { return self }
        let scale = maxDimension / maxSide
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)
        let renderer = UIGraphicsImageRenderer(size: newSize)
        return renderer.image { _ in draw(in: CGRect(origin: .zero, size: newSize)) }
    }
}

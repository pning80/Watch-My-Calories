import SwiftUI
import SwiftData

struct ContentView: View {
    @State private var selectedTab: Tab = .dashboard

    // Log Food sheet + flow state
    @State private var showLogFoodSheet = false
    @State private var showScanFoodFlow = false
    @State private var showPhotoLibraryFlow = false
    @State private var showManualEntryFlow = false

    // Scan Menu sheet + flow state
    @State private var showScanMenuSheet = false
    @State private var showScanMenuCameraFlow = false
    @State private var showMenuPhotoLibraryFlow = false
    @State private var showStoredMenusFlow = false

    // State to trigger scrolling after food is logged
    @State private var scrollToMeal: MealType?

    @ObservedObject private var store = SettingsStore.shared

    enum Tab: Hashable {
        case dashboard, logFood, scanMenu, history
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            DashboardView(onLogFood: { showLogFoodSheet = true }, scrollToMeal: $scrollToMeal)
                .background(
                    TabBarInterceptor(
                        onLogFood: { showLogFoodSheet = true },
                        onScanMenu: { showScanMenuSheet = true }
                    )
                )
                .tabItem {
                    Label("Today", systemImage: "flame.fill")
                }
                .tag(Tab.dashboard)

            // Placeholder view for the action tab — never actually shown
            Color(.systemBackground)
                .tabItem {
                    Label("Log Food", systemImage: "plus.circle.fill")
                }
                .tag(Tab.logFood)

            // Placeholder view for the scan menu tab — never actually shown
            Color(.systemBackground)
                .tabItem {
                    Label("Scan Menu", systemImage: "doc.viewfinder")
                }
                .tag(Tab.scanMenu)

            HistoryView(onLogFood: { showLogFoodSheet = true })
                .tabItem {
                    Label("History", systemImage: "calendar")
                }
                .tag(Tab.history)
        }
        .tint(Color.cwPrimary)
        .preferredColorScheme(store.appTheme.colorScheme)
        .onAppear {
            let appearance = UITabBarAppearance()
            appearance.configureWithOpaqueBackground()
            appearance.backgroundColor = UIColor { tc in
                tc.userInterfaceStyle == .dark ? .secondarySystemBackground : .white
            }

            UITabBar.appearance().standardAppearance = appearance
            UITabBar.appearance().scrollEdgeAppearance = appearance
        }
        .sheet(isPresented: $showLogFoodSheet) {
            LogFoodSheet(
                onScanFood: {
                    showLogFoodSheet = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        showScanFoodFlow = true
                    }
                },
                onChooseFromLibrary: {
                    showLogFoodSheet = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        showPhotoLibraryFlow = true
                    }
                },
                onLogManually: {
                    showLogFoodSheet = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        showManualEntryFlow = true
                    }
                }
            )
        }
        .fullScreenCover(isPresented: $showScanFoodFlow) {
            CameraRootView(onDone: { mealType in
                showScanFoodFlow = false
                selectedTab = .dashboard
                scrollToMeal = mealType
            })
        }
        .fullScreenCover(isPresented: $showPhotoLibraryFlow) {
            PhotoLibraryRootView(onDone: { mealType in
                showPhotoLibraryFlow = false
                selectedTab = .dashboard
                scrollToMeal = mealType
            }, onCancel: {
                showPhotoLibraryFlow = false
            })
        }
        .sheet(isPresented: $showManualEntryFlow) {
            ManualEntryRootView(onDone: {
                showManualEntryFlow = false
                selectedTab = .dashboard
            })
        }
        .sheet(isPresented: $showScanMenuSheet) {
            ScanMenuSheet(
                onScanMenu: {
                    showScanMenuSheet = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        showScanMenuCameraFlow = true
                    }
                },
                onChooseFromLibrary: {
                    showScanMenuSheet = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        showMenuPhotoLibraryFlow = true
                    }
                },
                onStoredMenus: {
                    showScanMenuSheet = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        showStoredMenusFlow = true
                    }
                }
            )
        }
        .fullScreenCover(isPresented: $showMenuPhotoLibraryFlow) {
            MenuPhotoLibraryRootView(onDone: {
                showMenuPhotoLibraryFlow = false
                selectedTab = .dashboard
            }, onCancel: {
                showMenuPhotoLibraryFlow = false
            })
        }
        .fullScreenCover(isPresented: $showScanMenuCameraFlow) {
            MenuCameraRootView(onDone: {
                showScanMenuCameraFlow = false
                selectedTab = .dashboard
            })
        }
        .sheet(isPresented: $showStoredMenusFlow) {
            NavigationStack {
                ScannedMenusView()
                    .toolbar {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button("Done") { showStoredMenusFlow = false }
                        }
                    }
            }
        }
    }
}

// MARK: - Camera Root (presented modally now, not as a tab)

struct CameraRootView: View {
    var onDone: (MealType) -> Void
    @StateObject private var cameraManager = CameraManager()
    @State private var capturedImages: [UIImage] = []
    @State private var reviewData: [Data]?
    @State private var capturedDate: Date? = nil
    @State private var selectedMealType: MealType? = nil
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            CameraView(model: cameraManager) { images, mealType in
                self.capturedImages = images
                self.reviewData = images.compactMap { $0.downscaled(maxDimension: 2048).jpegData(compressionQuality: 0.8) }
                self.capturedDate = Date()
                self.selectedMealType = mealType
            }
            .navigationDestination(isPresented: Binding(
                get: { reviewData != nil },
                set: { if !$0 {
                    reviewData = nil
                    capturedImages.removeAll()
                    capturedDate = nil
                    selectedMealType = nil
                    cameraManager.reset()
                } }
            )) {
                if let data = reviewData, let date = capturedDate, let mealType = selectedMealType {
                    EstimationReviewView(images: data, captureDate: date, mealType: mealType, onDone: {
                        onDone(mealType)
                    })
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
        .onAppear {
            capturedImages.removeAll()
            reviewData = nil
            cameraManager.reset()
        }
    }
}

// MARK: - Photo Library Root (presented modally)

struct PhotoLibraryRootView: View {
    var onDone: (MealType) -> Void
    var onCancel: () -> Void
    @State private var capturedImages: [UIImage] = []
    @State private var reviewData: [Data]?
    @State private var capturedDate: Date? = nil
    @State private var selectedMealType: MealType? = nil

    var body: some View {
        NavigationStack {
            PhotoLibraryReviewView(onImagesCaptured: { images, mealType in
                self.capturedImages = images
                self.reviewData = images.compactMap { $0.downscaled(maxDimension: 2048).jpegData(compressionQuality: 0.8) }
                self.capturedDate = Date()
                self.selectedMealType = mealType
            }, onCancel: {
                onCancel()
            })
            .navigationDestination(isPresented: Binding(
                get: { reviewData != nil },
                set: { if !$0 {
                    reviewData = nil
                    capturedImages.removeAll()
                    capturedDate = nil
                    selectedMealType = nil
                } }
            )) {
                if let data = reviewData, let date = capturedDate, let mealType = selectedMealType {
                    EstimationReviewView(images: data, captureDate: date, mealType: mealType, onDone: {
                        onDone(mealType)
                    })
                }
            }
        }
    }
}

// MARK: - Manual Entry Root (presented as sheet)

struct ManualEntryRootView: View {
    @Environment(\.modelContext) private var modelContext
    var onDone: () -> Void

    var body: some View {
        NavigationStack {
            ManualEntryView(onSave: { entry in
                modelContext.insert(entry)
                onDone()
            })
        }
    }
}

// MARK: - Tab Bar Interceptor

/// Prevents UIKit's TabView from switching to placeholder tabs (Log Food, Scan Menu).
/// Instead, calls the provided closures so the sheet can appear without any tab transition flash.
private struct TabBarInterceptor: UIViewControllerRepresentable {
    var onLogFood: () -> Void
    var onScanMenu: () -> Void

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIViewController(context: Context) -> UIViewController { UIViewController() }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        context.coordinator.onLogFood = onLogFood
        context.coordinator.onScanMenu = onScanMenu
        DispatchQueue.main.async {
            guard let tabBarController = uiViewController.tabBarController else { return }
            tabBarController.delegate = context.coordinator
        }
    }

    class Coordinator: NSObject, UITabBarControllerDelegate {
        var onLogFood: (() -> Void)?
        var onScanMenu: (() -> Void)?

        func tabBarController(_ tabBarController: UITabBarController, shouldSelect viewController: UIViewController) -> Bool {
            guard let vcs = tabBarController.viewControllers,
                  let index = vcs.firstIndex(of: viewController) else { return true }
            switch index {
            case 1:
                onLogFood?()
                return false
            case 2:
                onScanMenu?()
                return false
            default:
                return true
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

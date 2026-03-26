import SwiftUI
import AVFoundation

struct MenuCameraView: View {
    @ObservedObject var model: CameraManager
    @Environment(\.scenePhase) private var scenePhase

    var onImageCaptured: (Data) -> Void

    @State private var photoToReview: UIImage?
    @State private var cameraAuthStatus: AVAuthorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)
    @State private var showEstimateDisclaimer = false
    @ObservedObject private var store = SettingsStore.shared

    private var isCameraDenied: Bool {
        #if targetEnvironment(simulator)
        return false
        #else
        return cameraAuthStatus == .denied || cameraAuthStatus == .restricted
        #endif
    }

    var body: some View {
        ZStack {
            if isCameraDenied {
                cameraDeniedView
            } else if let image = photoToReview {
                // Photo review
                Color.black.ignoresSafeArea()

                GeometryReader { geometry in
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .frame(width: geometry.size.width, height: geometry.size.height)
                        .clipped()
                }
                .ignoresSafeArea()

                VStack {
                    Spacer()

                    HStack(spacing: 40) {
                        // Retake
                        Button(action: {
                            withAnimation {
                                photoToReview = nil
                                model.reset()
                            }
                        }) {
                            Label("Retake", systemImage: "arrow.counterclockwise")
                                .font(.body)
                                .fontWeight(.semibold)
                                .foregroundStyle(.white)
                                .padding(.horizontal, 24)
                                .padding(.vertical, 14)
                                .background(Capsule().fill(Color.white.opacity(0.25)))
                        }

                        // Analyze Menu
                        Button(action: {
                            if let data = image.downscaledForMenu(maxDimension: 2048).jpegData(compressionQuality: 0.8) {
                                onImageCaptured(data)
                            }
                        }) {
                            Label("Analyze Menu", systemImage: "checkmark")
                                .font(.body)
                                .fontWeight(.semibold)
                                .foregroundStyle(.white)
                                .padding(.horizontal, 24)
                                .padding(.vertical, 14)
                                .background(Capsule().fill(Color.cwAccent))
                                .shadow(radius: 4)
                        }
                    }
                    .padding(.bottom, 50)
                }
            } else {
                // Camera preview
                CameraPreview(session: model.session)
                    .ignoresSafeArea()

                VStack {
                    LinearGradient(colors: [.black.opacity(0.6), .clear], startPoint: .top, endPoint: .bottom)
                        .frame(height: 100)
                        .ignoresSafeArea()
                    Spacer()
                    LinearGradient(colors: [.clear, .black.opacity(0.8)], startPoint: .top, endPoint: .bottom)
                        .frame(height: 160)
                        .ignoresSafeArea()
                }
                .allowsHitTesting(false)

                VStack {
                    Spacer()

                    Button(action: {
                        let generator = UIImpactFeedbackGenerator(style: .heavy)
                        generator.impactOccurred()
                        model.takePhoto()
                    }) {
                        ZStack {
                            Circle()
                                .stroke(Color.white, lineWidth: 4)
                                .frame(width: 80, height: 80)

                            Circle()
                                .fill(Color.cwPrimary)
                                .frame(width: 68, height: 68)
                        }
                    }
                    .disabled(model.isCapturing)
                    .opacity(model.isCapturing ? 0.5 : 1.0)
                    .padding(.bottom, 40)
                }
            }
        }
        .onAppear {
            model.start()
            LocationManager.shared.requestPermission()
        }
        .onDisappear { model.stop() }
        .onChange(of: model.capturedImages.count) {
            if let image = model.capturedImages.first {
                withAnimation { photoToReview = image }
                if !store.hasSeenEstimateDisclaimer {
                    showEstimateDisclaimer = true
                }
            } else {
                withAnimation { photoToReview = nil }
            }
        }
        .sheet(isPresented: $showEstimateDisclaimer) {
            CalorieDisclaimerSheet { dontShowAgain in
                if dontShowAgain {
                    store.dismissEstimateDisclaimer()
                }
                showEstimateDisclaimer = false
            }
        }
        .onChange(of: scenePhase) {
            if scenePhase == .active {
                cameraAuthStatus = AVCaptureDevice.authorizationStatus(for: .video)
            }
        }
        .alert(isPresented: Binding<Bool>(
            get: {
                guard let alert = model.alert else { return false }
                if case .unauthorized = alert { return false }
                return true
            },
            set: { _ in model.alert = nil }
        )) {
            Alert(title: Text("Camera Error"), message: Text(model.alert?.localizedDescription ?? "Unknown error"), dismissButton: .default(Text("OK")))
        }
    }

    private var cameraDeniedView: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "camera.fill")
                .font(.system(size: 64))
                .foregroundStyle(Color.cwPrimary.opacity(0.6))

            Text("Camera Access Required")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundStyle(Color.cwTextPrimary)

            Text("To scan restaurant menus, please allow camera access in Settings.")
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.gray)
                .padding(.horizontal, 40)

            Button {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            } label: {
                Label("Open Settings", systemImage: "gear")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color.cwPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .padding(.horizontal, 40)

            Spacer()
        }
    }
}

private extension UIImage {
    func downscaledForMenu(maxDimension: CGFloat) -> UIImage {
        let maxSide = max(size.width, size.height)
        guard maxSide > maxDimension else { return self }
        let scale = maxDimension / maxSide
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)
        let renderer = UIGraphicsImageRenderer(size: newSize)
        return renderer.image { _ in draw(in: CGRect(origin: .zero, size: newSize)) }
    }
}

// MARK: - Root View for Scan Menu Tab

struct MenuCameraRootView: View {
    @Binding var selectedTab: ContentView.Tab
    @StateObject private var cameraManager = CameraManager()
    @State private var menuReviewData: Data?

    var body: some View {
        #if targetEnvironment(simulator)
        let _ = cameraManager.simulatorPhotos = ["MenuPhoto1"]
        #endif
        NavigationStack {
            MenuCameraView(model: cameraManager) { imageData in
                menuReviewData = imageData
            }
            .navigationDestination(isPresented: Binding(
                get: { menuReviewData != nil },
                set: { if !$0 {
                    menuReviewData = nil
                    cameraManager.reset()
                } }
            )) {
                if let data = menuReviewData {
                    MenuAnalysisView(imageData: data, onDone: {
                        menuReviewData = nil
                        cameraManager.reset()
                        selectedTab = .dashboard
                    }, onScanAgain: {
                        menuReviewData = nil
                        cameraManager.reset()
                    })
                }
            }
        }
        .onAppear {
            cameraManager.reset()
        }
    }
}

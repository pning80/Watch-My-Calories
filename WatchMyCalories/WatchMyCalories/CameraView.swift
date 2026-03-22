import SwiftUI
import AVFoundation
import UIKit

struct CameraView: View {
    @ObservedObject var model: CameraManager
    @Environment(\.dismiss) private var dismiss
    @Environment(\.scenePhase) private var scenePhase

    var onImagesCaptured: ([UIImage]) -> Void

    @State private var photoToReview: UIImage?
    @State private var cameraAuthStatus: AVAuthorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)

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
                        .accessibilityIdentifier(AccessibilityID.Camera.retakeButton)

                        // Use Photo
                        Button(action: {
                            onImagesCaptured(model.capturedImages)
                        }) {
                            Label("Use", systemImage: "checkmark")
                                .font(.body)
                                .fontWeight(.semibold)
                                .foregroundStyle(.white)
                                .padding(.horizontal, 24)
                                .padding(.vertical, 14)
                                .background(Capsule().fill(Color.cwAccent))
                                .shadow(radius: 4)
                        }
                        .accessibilityIdentifier(AccessibilityID.Camera.usePhotoButton)
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
                    .accessibilityIdentifier(AccessibilityID.Camera.captureButton)
                    .padding(.bottom, 40)
                }
            }
        }
        .onAppear { model.start() }
        .onDisappear { model.stop() }
        .onChange(of: model.capturedImages.count) {
            if let image = model.capturedImages.first {
                withAnimation { photoToReview = image }
            } else {
                withAnimation { photoToReview = nil }
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

            Text("To scan your meals, please allow camera access in Settings.")
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

// UIKit wrapper for AVCaptureVideoPreviewLayer
struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> VideoPreviewView {
        let view = VideoPreviewView()
        view.videoPreviewLayer.session = session
        view.videoPreviewLayer.videoGravity = .resizeAspectFill
        return view
    }

    func updateUIView(_ uiView: VideoPreviewView, context: Context) {
        if uiView.videoPreviewLayer.session != session {
            uiView.videoPreviewLayer.session = session
        }
    }

    class VideoPreviewView: UIView {
        override class var layerClass: AnyClass {
            AVCaptureVideoPreviewLayer.self
        }

        var videoPreviewLayer: AVCaptureVideoPreviewLayer {
            return layer as! AVCaptureVideoPreviewLayer
        }

        override func layoutSubviews() {
            super.layoutSubviews()
            videoPreviewLayer.frame = bounds
            if let connection = videoPreviewLayer.connection {
                let angle: CGFloat = switch UIDevice.current.orientation {
                case .portrait: 90
                case .landscapeLeft: 0
                case .landscapeRight: 180
                case .portraitUpsideDown: 270
                default: 90
                }
                connection.videoRotationAngle = angle
            }
        }
    }
}

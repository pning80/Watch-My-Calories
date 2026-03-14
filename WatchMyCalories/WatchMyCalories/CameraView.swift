import SwiftUI
import AVFoundation
import UIKit

struct CameraView: View {
    @ObservedObject var model: CameraManager
    @Environment(\.dismiss) private var dismiss

    var onImagesCaptured: ([UIImage]) -> Void

    @State private var photoToReview: UIImage?

    var body: some View {
        ZStack {
            if let image = photoToReview {
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
                            Label("Use Photo", systemImage: "checkmark")
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
        .alert(isPresented: Binding<Bool>(
            get: { model.alert != nil },
            set: { _ in model.alert = nil }
        )) {
            Alert(title: Text("Camera Error"), message: Text(model.alert?.localizedDescription ?? "Unknown error"), dismissButton: .default(Text("OK")))
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
        // Ensure layer is connected if session changes (rare)
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
        }
    }
}

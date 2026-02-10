import SwiftUI
import AVFoundation
import UIKit

struct CameraView: View {
    @StateObject private var model = CameraManager()
    @Environment(\.dismiss) private var dismiss
    
    var onImagesCaptured: ([UIImage]) -> Void

    var body: some View {
        ZStack {
            CameraPreview(session: model.session)
                .ignoresSafeArea()
            
            // Gradient Overlay for readability
            VStack {
                LinearGradient(colors: [.black.opacity(0.6), .clear], startPoint: .top, endPoint: .bottom)
                    .frame(height: 100)
                    .ignoresSafeArea()
                Spacer()
                LinearGradient(colors: [.clear, .black.opacity(0.8)], startPoint: .top, endPoint: .bottom)
                    .frame(height: 240) // Increased height for thumbnails
                    .ignoresSafeArea()
            }
            .allowsHitTesting(false)
            
            // UI Overlays
            VStack {
                // Top Bar
                HStack {
                    Spacer()
                    if !model.capturedImages.isEmpty {
                        HStack(spacing: 4) {
                            Image(systemName: "photo.stack")
                            Text("\(model.capturedImages.count) / 3")
                        }
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundStyle(Color.cwPrimary)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(Capsule().fill(Color.white))
                        .shadow(radius: 4)
                    }
                }
                .padding()
                
                Spacer()
                
                // Thumbnails
                if !model.capturedImages.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(model.capturedImages.indices, id: \.self) { index in
                                Image(uiImage: model.capturedImages[index])
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: 60, height: 60)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(Color.white, lineWidth: 2)
                                    )
                                    .shadow(radius: 4)
                                    .transition(.scale.combined(with: .opacity))
                            }
                        }
                        .padding(.horizontal)
                        .padding(.bottom, 20)
                    }
                    .frame(height: 70)
                }
                
                // Bottom Controls
                HStack(alignment: .center, spacing: 50) {
                    
                    // Reset Button
                    if !model.capturedImages.isEmpty {
                        Button(action: { withAnimation { model.reset() } }) {
                            Circle()
                                .fill(Color.white.opacity(0.2))
                                .frame(width: 50, height: 50)
                                .overlay(
                                    Image(systemName: "trash.fill")
                                        .foregroundStyle(.white)
                                )
                        }
                    } else {
                        Color.clear.frame(width: 50, height: 50)
                    }
                    
                    // Shutter Button
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
                                .fill(model.capturedImages.count >= 3 ? Color.gray : Color.cwPrimary) // Brand color
                                .frame(width: 68, height: 68)
                        }
                    }
                    .disabled(model.capturedImages.count >= 3)
                    
                    // Done Button
                    if !model.capturedImages.isEmpty {
                        Button(action: {
                            onImagesCaptured(model.capturedImages)
                        }) {
                            Circle()
                                .fill(Color.cwAccent) // Brand Accent
                                .frame(width: 50, height: 50)
                                .overlay(
                                    Image(systemName: "arrow.right")
                                        .font(.title3)
                                        .fontWeight(.bold)
                                        .foregroundStyle(.white)
                                )
                                .shadow(radius: 4)
                        }
                    } else {
                         Color.clear.frame(width: 50, height: 50)
                    }
                }
                .padding(.bottom, 40)
            }
        }
        .onAppear { model.start() }
        .onDisappear { model.stop() }
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

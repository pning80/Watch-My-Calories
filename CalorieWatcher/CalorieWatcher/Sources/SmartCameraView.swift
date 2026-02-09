import SwiftUI
import Combine

struct SmartCameraView: View {
    @StateObject private var cameraManager = CameraManager()
    @StateObject private var stabilityDetector = StabilityDetector()
    
    @State private var captureCount = 0
    @State private var isReviewing = false
    
    // Auto-capture logic
    @State private var stableStartTime: Date?
    let requiredStabilityDuration: TimeInterval = 0.5
    
    var body: some View {
        ZStack {
            // Camera Feed
            CameraPreview(session: cameraManager.session)
                .ignoresSafeArea()
            
            // Overlays
            VStack {
                HStack {
                    Text("Images: \(captureCount)/5")
                        .padding(8)
                        .background(.ultraThinMaterial)
                        .cornerRadius(8)
                    Spacer()
                    StabilityIndicator(isStable: stabilityDetector.isStable)
                }
                .padding()
                
                Spacer()
                
                if captureCount >= 3 {
                    Button("Analyze Food") {
                        isReviewing = true // Navigate to review/analysis
                    }
                    .buttonStyle(.borderedProminent)
                    .padding(.bottom, 30)
                }
                
                Text(stabilityDetector.isStable ? "Hold still..." : "Move around the food")
                    .font(.headline)
                    .foregroundColor(.white)
                    .padding()
                    .background(Color.black.opacity(0.5))
                    .cornerRadius(10)
                    .padding(.bottom, 20)
            }
        }
        .onAppear {
            cameraManager.startSession()
            stabilityDetector.startMonitoring()
        }
        .onDisappear {
            cameraManager.stopSession()
            stabilityDetector.stopMonitoring()
        }
        .onChange(of: stabilityDetector.isStable) { isStable in
            if isStable {
                stableStartTime = Date()
                // Schedule capture if stable for duration
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    if stabilityDetector.isStable && captureCount < 5 {
                        cameraManager.capturePhoto()
                        captureCount += 1
                        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                    }
                }
            } else {
                stableStartTime = nil
            }
        }
    }
}

struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession
    
    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: UIScreen.main.bounds)
        let previewLayer = AVCaptureVideoPreviewLayer(session: session)
        previewLayer.frame = view.frame
        previewLayer.videoGravity = .resizeAspectFill
        view.layer.addSublayer(previewLayer)
        return view
    }
    
    func updateUIView(_ uiView: UIView, context: Context) {}
}

struct StabilityIndicator: View {
    var isStable: Bool
    
    var body: some View {
        Circle()
            .fill(isStable ? Color.green : Color.yellow)
            .frame(width: 12, height: 12)
            .overlay(Circle().stroke(Color.white, lineWidth: 2))
            .shadow(radius: 2)
    }
}

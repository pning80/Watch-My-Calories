import SwiftUI
import AVFoundation

struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession
    
    func makeUIView(context: Context) -> some UIView {
        let view = UIView(frame: UIScreen.main.bounds)
        view.backgroundColor = .black
        
        let previewLayer = AVCaptureVideoPreviewLayer(session: session)
        previewLayer.frame = view.frame
        previewLayer.videoGravity = .resizeAspectFill
        view.layer.addSublayer(previewLayer)
        
        return view
    }
    
    func updateUIView(_ uiView: UIViewType, context: Context) {}
}

struct CameraView: View {
    @StateObject private var cameraService = CameraService()
    @Environment(\.dismiss) var dismiss
    @Binding var capturedImages: [UIImage]
    
    var body: some View {
        ZStack {
            if cameraService.permissionGranted {
                CameraPreview(session: cameraService.captureSession)
                    .edgesIgnoringSafeArea(.all)
            } else {
                Text("Camera permission denied")
                    .foregroundColor(.white)
            }
            
            VStack {
                HStack {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 24))
                            .foregroundColor(.white)
                            .padding()
                            .background(Color.black.opacity(0.5))
                            .clipShape(Circle())
                    }
                    Spacer()
                }
                .padding()
                
                Spacer()
                
                HStack(spacing: 30) {
                    // Gallery / Recent thumbnails
                    if !capturedImages.isEmpty {
                        ScrollView(.horizontal) {
                            HStack {
                                ForEach(capturedImages, id: \.self) { img in
                                    Image(uiImage: img)
                                        .resizable()
                                        .scaledToFill()
                                        .frame(width: 50, height: 50)
                                        .clipShape(RoundedRectangle(cornerRadius: 8))
                                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.white, lineWidth: 1))
                                }
                            }
                        }
                        .frame(height: 60)
                    }
                    
                    Button(action: {
                        cameraService.capturePhoto()
                    }) {
                        Circle()
                            .stroke(Color.white, lineWidth: 4)
                            .frame(width: 80, height: 80)
                            .overlay(
                                Circle()
                                    .fill(Color.white)
                                    .frame(width: 70, height: 70)
                            )
                    }
                    
                    Button("Done") {
                        dismiss()
                    }
                    .padding()
                    .background(Color.mainGreen)
                    .foregroundColor(.black)
                    .cornerRadius(20)
                    .opacity(capturedImages.isEmpty ? 0 : 1)
                }
                .padding(.bottom, 30)
            }
        }
        .onReceive(cameraService.$capturedImage) { image in
            if let image = image {
                capturedImages.append(image)
            }
        }
    }
}

extension Color {
    static let mainGreen = Color(red: 0.8, green: 1.0, blue: 0.0)
}

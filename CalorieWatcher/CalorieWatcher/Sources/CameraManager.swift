import AVFoundation
import UIKit

class CameraManager: NSObject, ObservableObject {
    @Published var session = AVCaptureSession()
    @Published var capturedImages: [UIImage] = []
    @Published var isSessionRunning = false
    
    private let output = AVCapturePhotoOutput()
    private let queue = DispatchQueue(label: "camera.queue")
    
    override init() {
        super.init()
        checkPermissions()
    }
    
    func checkPermissions() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            setupCamera()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                if granted { self?.setupCamera() }
            }
        default:
            break
        }
    }
    
    private func setupCamera() {
        queue.async { [weak self] in
            guard let self = self else { return }
            self.session.beginConfiguration()
            
            guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
                  let input = try? AVCaptureDeviceInput(device: device) else {
                return
            }
            
            if self.session.canAddInput(input) {
                self.session.addInput(input)
            }
            
            if self.session.canAddOutput(self.output) {
                self.session.addOutput(self.output)
            }
            
            self.session.commitConfiguration()
            self.startSession()
        }
    }
    
    func startSession() {
        queue.async { [weak self] in
            guard let self = self, !self.session.isRunning else { return }
            self.session.startRunning()
            DispatchQueue.main.async { self.isSessionRunning = true }
        }
    }
    
    func stopSession() {
        queue.async { [weak self] in
            guard let self = self, self.session.isRunning else { return }
            self.session.stopRunning()
            DispatchQueue.main.async { self.isSessionRunning = false }
        }
    }
    
    func capturePhoto() {
        let settings = AVCapturePhotoSettings()
        output.capturePhoto(with: settings, delegate: self)
    }
}

extension CameraManager: AVCapturePhotoCaptureDelegate {
    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        guard let data = photo.fileDataRepresentation(),
              let image = UIImage(data: data) else { return }
        
        DispatchQueue.main.async {
            self.capturedImages.append(image)
        }
    }
}

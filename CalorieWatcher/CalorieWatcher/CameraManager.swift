// Suppress Sendable warnings for AVFoundation classes in Swift 6
@preconcurrency import AVFoundation
import UIKit
import SwiftUI
import Combine

@MainActor
class CameraManager: NSObject, ObservableObject {
    
    // MARK: - Properties
    
    // Immutable, thread-safe references to AV objects.
    // Marked nonisolated so background threads can access them.
    nonisolated public let session = AVCaptureSession()
    nonisolated private let output = AVCapturePhotoOutput()
    
    @Published var alert: CameraError?
    @Published var capturedImages: [UIImage] = []
    
    // Global serial queue for camera operations
    nonisolated static let sessionQueue = DispatchQueue(label: "camera.session.queue")
    
    enum CameraError: Error, LocalizedError {
        case unauthorized
        case configurationFailed
        case runtimeError(String)
        
        var errorDescription: String? {
            switch self {
            case .unauthorized: return "Camera access was denied. Please enable it in Settings."
            case .configurationFailed: return "Unable to capture media."
            case .runtimeError(let message): return "Camera error: \(message)"
            }
        }
    }
    
    override init() {
        super.init()
        
        // Handle Runtime Errors (iOS 18+ and legacy support)
        let notificationName: NSNotification.Name
        if #available(iOS 18.0, *) {
            notificationName = AVCaptureSession.runtimeErrorNotification
        } else {
            notificationName = .AVCaptureSessionRuntimeError
        }
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(sessionRuntimeError),
            name: notificationName,
            object: nil
        )
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
        // Fire and forget stop
        let sessionToStop = session
        // Use the static queue directly to avoid capturing 'self'
        CameraManager.sessionQueue.async {
            if sessionToStop.isRunning {
                sessionToStop.stopRunning()
            }
        }
    }
    
    // MARK: - Public API
    
    func start() {
        #if targetEnvironment(simulator)
        return
        #endif
        
        // Dispatch to background queue
        CameraManager.sessionQueue.async { [weak self] in
            guard let self = self else { return }
            self.performStartup()
        }
    }
    
    func stop() {
        #if targetEnvironment(simulator)
        return
        #endif
        
        CameraManager.sessionQueue.async { [weak self] in
            guard let self = self else { return }
            if self.session.isRunning {
                self.session.stopRunning()
            }
        }
    }
    
    func takePhoto() {
        #if targetEnvironment(simulator)
        simulatePhotoCapture()
        return
        #endif
        
        CameraManager.sessionQueue.async { [weak self] in
            guard let self = self else { return }
            self.performCapture()
        }
    }
    
    func reset() {
        capturedImages.removeAll()
    }
    
    // MARK: - Private Non-Isolated Helpers
    
    // These methods run on the background queue (sessionQueue).
    // They are 'nonisolated' so they don't block the Main Actor.
    
    nonisolated private func performStartup() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.ambient, mode: .default, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            // Audio session configuration failed — camera still usable
        }
        
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            self.configureAndStart()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                if granted {
                    CameraManager.sessionQueue.async {
                        self.configureAndStart()
                    }
                }
            }
        case .denied, .restricted:
            Task { @MainActor in self.alert = .unauthorized }
        @unknown default:
            break
        }
    }
    
    nonisolated private func configureAndStart() {
        // Check if already configured by inspecting inputs.
        // This replaces the 'isConfigured' state variable.
        if !session.inputs.isEmpty {
            if !session.isRunning { session.startRunning() }
            return
        }
        
        session.beginConfiguration()
        session.sessionPreset = .photo
        
        // Cleanup existing inputs/outputs
        session.inputs.forEach { session.removeInput($0) }
        session.outputs.forEach { session.removeOutput($0) }
        
        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else {
            Task { @MainActor in self.alert = .configurationFailed }
            session.commitConfiguration()
            return
        }
        
        do {
            let input = try AVCaptureDeviceInput(device: device)
            if session.canAddInput(input) {
                session.addInput(input)
            } else {
                Task { @MainActor in self.alert = .configurationFailed }
                session.commitConfiguration()
                return
            }
        } catch {
            Task { @MainActor in self.alert = .configurationFailed }
            session.commitConfiguration()
            return
        }
        
        if session.canAddOutput(output) {
            session.addOutput(output)
        } else {
            Task { @MainActor in self.alert = .configurationFailed }
            session.commitConfiguration()
            return
        }
        
        session.commitConfiguration()
        
        if !session.isRunning {
            session.startRunning()
        }
    }
    
    nonisolated private func performCapture() {
        let settings = AVCapturePhotoSettings()
        if let connection = self.output.connection(with: .video), connection.isActive {
            self.output.capturePhoto(with: settings, delegate: self)
        }
    }
    
    private func simulatePhotoCapture() {
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: 300, height: 300))
        let image = renderer.image { ctx in
            UIColor.darkGray.setFill()
            ctx.fill(CGRect(x: 0, y: 0, width: 300, height: 300))
            let attrs: [NSAttributedString.Key: Any] = [.font: UIFont.systemFont(ofSize: 24), .foregroundColor: UIColor.white]
            "Mock Food Photo".draw(at: CGPoint(x: 50, y: 130), withAttributes: attrs)
        }
        self.capturedImages.append(image)
    }

    // MARK: - Error Handling
    
    @objc private func sessionRuntimeError(notification: Notification) {
        guard let error = notification.userInfo?[AVCaptureSessionErrorKey] as? AVError else { return }
        
        if error.code == .mediaServicesWereReset {
            CameraManager.sessionQueue.async { [weak self] in
                guard let self = self else { return }
                if !self.session.isRunning {
                    self.session.startRunning()
                }
            }
        } else {
            self.alert = .runtimeError(error.localizedDescription)
        }
    }
}

// MARK: - Delegate
extension CameraManager: AVCapturePhotoCaptureDelegate {
    nonisolated func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        if let error = error {
            // Photo processing error
            return
        }
        
        if let data = photo.fileDataRepresentation(), let image = UIImage(data: data) {
            Task { @MainActor in
                self.capturedImages.append(image)
            }
        }
    }
}

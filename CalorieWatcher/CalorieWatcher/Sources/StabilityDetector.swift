import CoreMotion
import Combine

class StabilityDetector: ObservableObject {
    private let motionManager = CMMotionManager()
    @Published var isStable: Bool = false
    
    // Stability thresholds
    private let accelerationThreshold = 0.05
    private let stabilityDuration: TimeInterval = 0.5
    
    private var lastUnstableTime: Date = Date()
    private var timer: Timer?
    
    init() {
        startMonitoring()
    }
    
    func startMonitoring() {
        guard motionManager.isAccelerometerAvailable else { return }
        motionManager.accelerometerUpdateInterval = 0.1
        
        motionManager.startAccelerometerUpdates(to: .main) { [weak self] data, error in
            guard let self = self, let data = data else { return }
            self.checkStability(acceleration: data.acceleration)
        }
    }
    
    func stopMonitoring() {
        motionManager.stopAccelerometerUpdates()
        timer?.invalidate()
    }
    
    private func checkStability(acceleration: CMAcceleration) {
        // Simple magnitude check (removing gravity component roughly)
        // For better results, we might want to use deviceMotion and userAcceleration.
        // But raw accelerometer variance is often enough for "holding still".
        
        // Let's assume user holds phone somewhat upright. 
        // A better metric is variance over a window, but instantaneous low acceleration jitter is a good proxy.
        // Actually, we want to know if the phone is MOVING. 
        // UserAcceleration from DeviceMotion is better.
        
        // Switching to DeviceMotion for better gravity separation
        if motionManager.isDeviceMotionAvailable && !motionManager.isDeviceMotionActive {
            motionManager.stopAccelerometerUpdates()
            motionManager.deviceMotionUpdateInterval = 0.1
            motionManager.startDeviceMotionUpdates(to: .main) { [weak self] data, error in
                guard let self = self, let data = data else { return }
                self.checkDeviceMotionHigh(userAccel: data.userAcceleration)
            }
        }
    }
    
    private func checkDeviceMotionHigh(userAccel: CMAcceleration) {
        let magnitude = sqrt(pow(userAccel.x, 2) + pow(userAccel.y, 2) + pow(userAccel.z, 2))
        
        if magnitude < accelerationThreshold {
            if Date().timeIntervalSince(lastUnstableTime) > stabilityDuration {
                if !isStable { isStable = true }
            }
        } else {
            lastUnstableTime = Date()
            if isStable { isStable = false }
        }
    }
}

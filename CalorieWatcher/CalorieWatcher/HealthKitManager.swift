import Foundation
import HealthKit
import SwiftUI
import Combine

class HealthKitManager: ObservableObject {
    private let healthStore = HKHealthStore()
    
    @Published var activeEnergyBurned: Double = 0.0
    @Published var isAuthorized: Bool = false
    
    init() {
        // Check if HealthKit is available on this device
        guard HKHealthStore.isHealthDataAvailable() else {
            return
        }
    }
    
    func checkAuthorizationStatus() {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        
        let type = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned)!
        let status = healthStore.authorizationStatus(for: type)
        
        DispatchQueue.main.async {
            self.isAuthorized = (status == .sharingAuthorized)
        }
    }
    
    func requestAuthorization() {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        
        let readTypes: Set = [HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned)!]
        
        // Request authorization to read active energy burned
        healthStore.requestAuthorization(toShare: nil, read: readTypes) { success, error in
            if success {
                DispatchQueue.main.async {
                    self.isAuthorized = true
                    self.fetchTodayEnergyBurned()
                    self.startObserving()
                }
            } else {
                print("HealthKit Authorization Failed: \(String(describing: error))")
                if let error = error {
                    print("Error details: \(error.localizedDescription)")
                }
            }
        }
    }
    
    func fetchTodayEnergyBurned() {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        
        let type = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned)!
        let now = Date()
        let startOfDay = Calendar.current.startOfDay(for: now)
        let predicate = HKQuery.predicateForSamples(withStart: startOfDay, end: now, options: .strictStartDate)
        
        let query = HKStatisticsQuery(quantityType: type, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, result, error in
            guard let result = result, let sum = result.sumQuantity() else {
                if let error = error {
                    print("Failed to fetch active energy: \(error.localizedDescription)")
                }
                return
            }
            
            let burnedKcal = sum.doubleValue(for: HKUnit.kilocalorie())
            
            DispatchQueue.main.async {
                self.activeEnergyBurned = burnedKcal
            }
        }
        
        healthStore.execute(query)
    }
    
    func startObserving() {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        
        let type = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned)!
        
        let query = HKObserverQuery(sampleType: type, predicate: nil) { [weak self] _, _, error in
            if let error = error {
                print("Observer query failed: \(error.localizedDescription)")
                return
            }
            // Fetch new data when an update is observed
            self?.fetchTodayEnergyBurned()
        }
        
        healthStore.execute(query)
        
        // Enable background delivery
        healthStore.enableBackgroundDelivery(for: type, frequency: .immediate) { success, error in
            if !success {
                print("Failed to enable background delivery: \(String(describing: error))")
            }
        }
    }
}

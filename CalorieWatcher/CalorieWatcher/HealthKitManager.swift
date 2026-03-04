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
            guard error == nil else { return }
            self?.fetchTodayEnergyBurned()
        }
        
        healthStore.execute(query)
        
        // Enable background delivery
        healthStore.enableBackgroundDelivery(for: type, frequency: .immediate) { _, _ in }
    }
}

import HealthKit
import Combine

class HealthKitService: ObservableObject {
    private let healthStore = HKHealthStore()
    
    @Published var activeEnergyBurned: Double = 0.0
    @Published var authorized = false
    
    func requestAuthorization() {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        
        let typesToRead: Set<HKObjectType> = [
            HKObjectType.quantityType(forIdentifier: .activeEnergyBurned)!,
            HKObjectType.characteristicType(forIdentifier: .dateOfBirth)!,
            HKObjectType.characteristicType(forIdentifier: .biologicalSex)!,
            HKObjectType.quantityType(forIdentifier: .height)!,
            HKObjectType.quantityType(forIdentifier: .bodyMass)!
        ]
        
        let typesToWrite: Set<HKSampleType> = [
            HKObjectType.quantityType(forIdentifier: .dietaryEnergyConsumed)!,
            HKObjectType.quantityType(forIdentifier: .dietaryProtein)!,
            HKObjectType.quantityType(forIdentifier: .dietaryCarbohydrates)!,
            HKObjectType.quantityType(forIdentifier: .dietaryFatTotal)!
        ]
        
        healthStore.requestAuthorization(toShare: typesToWrite, read: typesToRead) { success, error in
            DispatchQueue.main.async {
                self.authorized = success
                if success {
                    self.fetchActiveEnergyBurned()
                }
            }
        }
    }
    
    func fetchActiveEnergyBurned() {
        guard let type = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) else { return }
        
        let startOfDay = Calendar.current.startOfDay(for: Date())
        let predicate = HKQuery.predicateForSamples(withStart: startOfDay, end: Date(), options: .strictStartDate)
        
        let query = HKStatisticsQuery(quantityType: type, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, result, _ in
            guard let result = result, let sum = result.sumQuantity() else { return }
            
            let calories = sum.doubleValue(for: HKUnit.kilocalorie())
            DispatchQueue.main.async {
                self.activeEnergyBurned = calories
            }
        }
        
        healthStore.execute(query)
    }
    
    func saveFood(calories: Double, protein: Double, carbs: Double, fat: Double, date: Date = Date()) {
        guard authorized else { return }
        
        let calType = HKQuantityType.quantityType(forIdentifier: .dietaryEnergyConsumed)!
        let protType = HKQuantityType.quantityType(forIdentifier: .dietaryProtein)!
        let carbType = HKQuantityType.quantityType(forIdentifier: .dietaryCarbohydrates)!
        let fatType = HKQuantityType.quantityType(forIdentifier: .dietaryFatTotal)!
        
        let calSample = HKQuantitySample(type: calType, quantity: HKQuantity(unit: .kilocalorie(), doubleValue: calories), start: date, end: date)
        let protSample = HKQuantitySample(type: protType, quantity: HKQuantity(unit: .gram(), doubleValue: protein), start: date, end: date)
        let carbSample = HKQuantitySample(type: carbType, quantity: HKQuantity(unit: .gram(), doubleValue: carbs), start: date, end: date)
        let fatSample = HKQuantitySample(type: fatType, quantity: HKQuantity(unit: .gram(), doubleValue: fat), start: date, end: date)
        
        healthStore.save([calSample, protSample, carbSample, fatSample]) { success, error in
            if let error = error {
                print("Error saving to HealthKit: \(error)")
            }
        }
    }
}

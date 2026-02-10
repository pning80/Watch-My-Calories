import Foundation
import CoreData

final class CoreDataService: DailySummaryService {
    private let context: NSManagedObjectContext

    init(context: NSManagedObjectContext) {
        self.context = context
    }

    func caloriesConsumed(on date: Date) throws -> Double {
        let request: NSFetchRequest<FoodLog> = NSFetchRequest<FoodLog>(entityName: "FoodLog")
        
        // Filter by start/end of day
        let calendar = Calendar.current
        let startOfDay = calendar.startOfDay(for: date)
        let endOfDay = calendar.date(byAdding: .day, value: 1, to: startOfDay)!
        
        request.predicate = NSPredicate(format: "timestamp >= %@ AND timestamp < %@", startOfDay as NSDate, endOfDay as NSDate)
        
        let logs = try context.fetch(request)
        return logs.reduce(0) { $0 + ($1.calories) }
    }

    func goalCalories(for date: Date) throws -> Double {
        // In the future, this can read from UserProfile or HealthKit
        return 2000
    }
    
    func saveFoodLog(from items: [EstimationItem], images: [Data]) throws {
        let now = Date()
        
        // Save images to disk and get paths (omitted for brevity, saving as Data for now if entity allows, or just ignore image)
        // Ideally, you save images to the Documents directory and store the filename in Core Data.
        
        for item in items {
            let log = FoodLog(context: context)
            log.id = UUID()
            log.timestamp = now
            log.name = item.name
            log.calories = item.calories
            log.quantity = item.quantity
            // log.imagePath = ... 
        }
        
        try context.save()
    }
}

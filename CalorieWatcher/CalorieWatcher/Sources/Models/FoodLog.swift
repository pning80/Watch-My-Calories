import Foundation
import CoreData

@objc(FoodLog)
public class FoodLog: NSManagedObject {

}

extension FoodLog {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<FoodLog> {
        return NSFetchRequest<FoodLog>(entityName: "FoodLog")
    }

    @NSManaged public var calories: Int64
    @NSManaged public var protein: Int64
    @NSManaged public var carbs: Int64
    @NSManaged public var fat: Int64
    @NSManaged public var timestamp: Date?
    @NSManaged public var id: UUID?
    @NSManaged public var name: String?
    @NSManaged public var note: String?
    @NSManaged public var imagePaths: String? // JSON String of filenames

}

extension FoodLog : Identifiable {

}

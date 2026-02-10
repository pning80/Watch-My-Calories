import Foundation
import SwiftData

@Model
final class UserProfile {
    var height: Double // cm
    var weight: Double // kg
    var age: Int
    var genderRaw: String
    var activityLevelRaw: String
    var targetCalories: Double
    var createdAt: Date
    
    var gender: Gender {
        get { Gender(rawValue: genderRaw) ?? .other }
        set { genderRaw = newValue.rawValue }
    }
    
    var activityLevel: ActivityLevel {
        get { ActivityLevel(rawValue: activityLevelRaw) ?? .sedentary }
        set { activityLevelRaw = newValue.rawValue }
    }
    
    init(height: Double, weight: Double, age: Int, gender: Gender, activityLevel: ActivityLevel, targetCalories: Double) {
        self.height = height
        self.weight = weight
        self.age = age
        self.genderRaw = gender.rawValue
        self.activityLevelRaw = activityLevel.rawValue
        self.targetCalories = targetCalories
        self.createdAt = Date()
    }
}

enum Gender: String, CaseIterable, Identifiable {
    case male = "Male"
    case female = "Female"
    case other = "Other"
    var id: Self { self }
}

enum ActivityLevel: String, CaseIterable, Identifiable {
    case sedentary = "Sedentary"
    case lightlyActive = "Lightly Active"
    case moderatelyActive = "Moderately Active"
    case veryActive = "Very Active"
    var id: Self { self }
}

@Model
final class FoodEntry {
    var id: UUID
    var name: String
    var calories: Double
    var quantity: String
    var timestamp: Date
    var protein: Double?
    var carbs: Double?
    var fat: Double?
    var imageID: UUID? // Reference to image stored in Documents directory
    
    init(name: String, calories: Double, quantity: String, timestamp: Date = Date(), protein: Double? = nil, carbs: Double? = nil, fat: Double? = nil, imageID: UUID? = nil) {
        self.id = UUID()
        self.name = name
        self.calories = calories
        self.quantity = quantity
        self.timestamp = timestamp
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
        self.imageID = imageID
    }
}

import Foundation
import SwiftData

@Model
final class UserProfile {
    var height: Double // cm (Reverted to Metric storage)
    var weight: Double // kg (Reverted to Metric storage)
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

enum CalorieCalculator {
    static func recommended(heightCm: Double, weightKg: Double, age: Int, gender: Gender, activityLevel: ActivityLevel) -> Double {
        var bmr: Double = (10 * weightKg) + (6.25 * heightCm) - (5 * Double(age))
        if gender == .male {
            bmr += 5
        } else {
            bmr -= 161
        }
        let multiplier: Double
        switch activityLevel {
        case .sedentary: multiplier = 1.2
        case .lightlyActive: multiplier = 1.375
        case .moderatelyActive: multiplier = 1.55
        case .veryActive: multiplier = 1.725
        }
        return (bmr * multiplier).rounded()
    }
}

enum MealType: String, Codable, CaseIterable {
    case breakfast = "Breakfast"
    case lunch = "Lunch"
    case dinner = "Dinner"
    case snack = "Snack"
    
    static func from(date: Date) -> MealType {
        let calendar = Calendar.current
        let hour = calendar.component(.hour, from: date)
        
        switch hour {
        case 7..<10:
            return .breakfast
        case 11..<15:
            return .lunch
        case 17..<21:
            return .dinner
        default:
            return .snack
        }
    }
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
    var imageID: UUID?
    var mealName: String?
    var mealTypeRaw: String = "Snack"
    
    var mealType: MealType {
        get { MealType(rawValue: mealTypeRaw) ?? .snack }
        set { mealTypeRaw = newValue.rawValue }
    }
    
    init(name: String, calories: Double, quantity: String, timestamp: Date = Date(), protein: Double? = nil, carbs: Double? = nil, fat: Double? = nil, imageID: UUID? = nil, mealName: String? = nil, mealType: MealType? = nil) {
        self.id = UUID()
        self.name = name
        self.calories = calories
        self.quantity = quantity
        self.timestamp = timestamp
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
        self.imageID = imageID
        self.mealName = mealName
        if let explicitMealType = mealType {
            self.mealTypeRaw = explicitMealType.rawValue
        } else {
            self.mealTypeRaw = MealType.from(date: timestamp).rawValue
        }
    }
}

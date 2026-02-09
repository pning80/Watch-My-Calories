import Foundation
import SwiftData

@Model
class UserProfile {
    var age: Int
    var gender: Gender
    var height: Double // cm
    var weight: Double // kg
    var activityLevel: ActivityLevel
    var goal: Goal
    
    init(age: Int = 30, gender: Gender = .male, height: Double = 175, weight: Double = 75, activityLevel: ActivityLevel = .moderate, goal: Goal = .maintain) {
        self.age = age
        self.gender = gender
        self.height = height
        self.weight = weight
        self.activityLevel = activityLevel
        self.goal = goal
    }
    
    // Mifflin-St Jeor Equation
    var bmr: Double {
        let s: Double = (gender == .male) ? 5 : -161
        return (10 * weight) + (6.25 * height) - (5 * Double(age)) + s
    }
    
    var tdee: Double {
        return bmr * activityLevel.multiplier
    }
    
    var dailyCalorieTarget: Int {
        switch goal {
        case .loseWeight: return Int(tdee - 500)
        case .maintain: return Int(tdee)
        case .gainMuscle: return Int(tdee + 300)
        }
    }
}

enum Gender: String, Codable, CaseIterable {
    case male, female
}

enum ActivityLevel: String, Codable, CaseIterable {
    case sedentary // 1.2
    case light // 1.375
    case moderate // 1.55
    case active // 1.725
    case veryActive // 1.9
    
    var multiplier: Double {
        switch self {
        case .sedentary: return 1.2
        case .light: return 1.375
        case .moderate: return 1.55
        case .active: return 1.725
        case .veryActive: return 1.9
        }
    }
}

enum Goal: String, Codable, CaseIterable {
    case loseWeight, maintain, gainMuscle
}

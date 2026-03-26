import Foundation
import Combine
import SwiftUI

enum UnitSystem: String, CaseIterable, Identifiable {
    case us = "US Customary"
    case metric = "Metric"

    var id: String { rawValue }

    static var localeDefault: UnitSystem {
        Locale.current.region?.identifier == "US" ? .us : .metric
    }
}

enum AIConsentStatus: String {
    case notAsked
    case accepted
    case declined
}

enum AppTheme: String, CaseIterable, Identifiable {
    case system = "System"
    case light = "Light"
    case dark = "Dark"

    var id: String { rawValue }

    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}

final class SettingsStore: ObservableObject {
    static let shared = SettingsStore()

    @Published var appTheme: AppTheme = .system
    @Published var unitSystem: UnitSystem = UnitSystem.localeDefault
    @Published var aiConsent: AIConsentStatus = .notAsked

    private let defaults = UserDefaults.standard

    @Published var hasCompletedOnboarding: Bool = false
    @Published var hasSeenEstimateDisclaimer: Bool = false

    private let themeKey = "appTheme"
    private let unitSystemKey = "unitSystem"
    private let aiConsentKey = "aiConsentStatus"
    private let onboardingKey = "hasCompletedOnboarding"
    private let estimateDisclaimerKey = "hasSeenEstimateDisclaimer"

    var savedAppTheme: AppTheme {
        if let themeRaw = defaults.string(forKey: themeKey), let theme = AppTheme(rawValue: themeRaw) {
            return theme
        }
        return .system
    }

    var savedUnitSystem: UnitSystem {
        if let raw = defaults.string(forKey: unitSystemKey), let unit = UnitSystem(rawValue: raw) {
            return unit
        }
        return UnitSystem.localeDefault
    }

    private init() {
        load()
    }

    func load() {
        if let themeRaw = defaults.string(forKey: themeKey), let theme = AppTheme(rawValue: themeRaw) {
            appTheme = theme
        }
        if let unitRaw = defaults.string(forKey: unitSystemKey), let unit = UnitSystem(rawValue: unitRaw) {
            unitSystem = unit
        }
        if let consentRaw = defaults.string(forKey: aiConsentKey), let consent = AIConsentStatus(rawValue: consentRaw) {
            aiConsent = consent
        }
        hasCompletedOnboarding = defaults.bool(forKey: onboardingKey)
        hasSeenEstimateDisclaimer = defaults.bool(forKey: estimateDisclaimerKey)
    }

    func save() {
        // Write both values then synchronize as a single batch
        defaults.set(appTheme.rawValue, forKey: themeKey)
        defaults.set(unitSystem.rawValue, forKey: unitSystemKey)
        defaults.synchronize()
    }

    func saveAIConsent(_ status: AIConsentStatus) {
        // Persist to disk before updating in-memory state
        defaults.set(status.rawValue, forKey: aiConsentKey)
        defaults.synchronize()
        aiConsent = status
    }

    func completeOnboarding() {
        defaults.set(true, forKey: onboardingKey)
        defaults.synchronize()
        hasCompletedOnboarding = true
    }

    func dismissEstimateDisclaimer() {
        defaults.set(true, forKey: estimateDisclaimerKey)
        defaults.synchronize()
        hasSeenEstimateDisclaimer = true
    }
}

// MARK: - Display-time quantity unit conversion

extension UnitSystem {

    /// Parses a quantity string (e.g. "20 g") and converts to the receiver's
    /// unit system. Count-based and unrecognized units pass through unchanged.
    func convertQuantity(_ raw: String) -> String {
        let trimmed = raw.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return raw }

        // Split into leading number and trailing unit token
        // Handles "20 g", "1.5 cups", "200ml" (no space)
        let scanner = Scanner(string: trimmed)
        scanner.charactersToBeSkipped = nil

        // Skip leading whitespace
        _ = scanner.scanCharacters(from: .whitespaces)

        guard let value = scanner.scanDouble() else {
            // No leading number — return as-is (e.g. "a handful")
            return raw
        }

        // Skip optional whitespace between number and unit
        _ = scanner.scanCharacters(from: .whitespaces)

        let unit = String(trimmed[scanner.currentIndex...])
            .trimmingCharacters(in: .whitespaces)
            .lowercased()

        guard !unit.isEmpty else { return raw }

        // Count-based units — no conversion
        let countUnits: Set<String> = [
            "piece", "pieces", "slice", "slices", "item", "items",
            "serving", "servings", "cookie", "cookies", "egg", "eggs",
            "strip", "strips", "patty", "patties", "wing", "wings",
            "drumstick", "drumsticks", "fillet", "fillets",
            "handful", "handfuls", "bunch", "bunches", "ear", "ears",
            "sheet", "sheets", "stick", "sticks", "clove", "cloves",
            "sprig", "sprigs", "leaf", "leaves", "head", "heads",
            "whole", "halves", "half", "quarter", "quarters",
            "small", "medium", "large"
        ]
        if countUnits.contains(unit) { return raw }

        // Determine conversion
        if let (converted, targetUnit) = convert(value: value, fromUnit: unit) {
            return formatQuantity(converted, unit: targetUnit)
        }

        return raw
    }

    // Returns (convertedValue, targetUnitLabel) or nil if no conversion needed/possible
    private func convert(value: Double, fromUnit: String) -> (Double, String)? {
        switch self {
        case .us:
            // Target is US — convert metric → US
            switch fromUnit {
            case "g", "gram", "grams":
                return (value / 28.3495, "oz")
            case "kg", "kilogram", "kilograms":
                return (value * 2.20462, "lbs")
            case "ml", "milliliter", "milliliters", "millilitre", "millilitres":
                return (value / 29.5735, "fl oz")
            case "l", "liter", "liters", "litre", "litres":
                return (value * 4.22675, "cups")
            // Already US units — no conversion
            case "oz", "ounce", "ounces",
                 "lb", "lbs", "pound", "pounds",
                 "cup", "cups",
                 "tbsp", "tablespoon", "tablespoons",
                 "tsp", "teaspoon", "teaspoons",
                 "fl oz", "fluid ounce", "fluid ounces",
                 "qt", "quart", "quarts",
                 "pt", "pint", "pints",
                 "gal", "gallon", "gallons":
                return nil
            default:
                return nil
            }

        case .metric:
            // Target is metric — convert US → metric
            switch fromUnit {
            case "oz", "ounce", "ounces":
                return (value * 28.3495, "g")
            case "lb", "lbs", "pound", "pounds":
                return (value / 2.20462, "kg")
            case "fl oz", "fluid ounce", "fluid ounces":
                return (value * 29.5735, "ml")
            case "cup", "cups":
                return (value * 236.588, "ml")
            case "tbsp", "tablespoon", "tablespoons":
                return (value * 14.787, "ml")
            case "tsp", "teaspoon", "teaspoons":
                return (value * 4.929, "ml")
            case "qt", "quart", "quarts":
                return (value * 946.353, "ml")
            case "pt", "pint", "pints":
                return (value * 473.176, "ml")
            case "gal", "gallon", "gallons":
                return (value * 3785.41, "ml")
            // Already metric — no conversion
            case "g", "gram", "grams",
                 "kg", "kilogram", "kilograms",
                 "ml", "milliliter", "milliliters", "millilitre", "millilitres",
                 "l", "liter", "liters", "litre", "litres":
                return nil
            default:
                return nil
            }
        }
    }

    private func formatQuantity(_ value: Double, unit: String) -> String {
        // Use natural rounding based on magnitude
        if value >= 100 {
            // Large values: round to nearest 10 (e.g. 236.6 → 240)
            let rounded = (value / 10).rounded() * 10
            return "\(Int(rounded)) \(unit)"
        } else if value >= 10 {
            // Medium values: round to whole number (e.g. 28.3 → 28)
            return "\(Int(value.rounded())) \(unit)"
        } else {
            // Small values: 1 decimal place, drop .0 (e.g. 0.7, 3.5)
            let rounded = (value * 10).rounded() / 10
            if rounded == rounded.rounded() && rounded >= 1 {
                return "\(Int(rounded)) \(unit)"
            } else {
                return String(format: "%.1f \(unit)", rounded)
            }
        }
    }
}

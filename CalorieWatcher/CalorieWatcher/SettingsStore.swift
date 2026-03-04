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

    private let defaults = UserDefaults.standard

    private let themeKey = "appTheme"
    private let unitSystemKey = "unitSystem"

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
    }

    func save() {
        defaults.set(appTheme.rawValue, forKey: themeKey)
        defaults.set(unitSystem.rawValue, forKey: unitSystemKey)
    }
}

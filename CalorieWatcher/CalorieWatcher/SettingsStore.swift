import Foundation
import Combine
import SwiftUI

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

    private let defaults = UserDefaults.standard

    private let themeKey = "appTheme"

    var savedAppTheme: AppTheme {
        if let themeRaw = defaults.string(forKey: themeKey), let theme = AppTheme(rawValue: themeRaw) {
            return theme
        }
        return .system
    }

    private init() {
        load()
    }

    func load() {
        if let themeRaw = defaults.string(forKey: themeKey), let theme = AppTheme(rawValue: themeRaw) {
            appTheme = theme
        }
    }

    func save() {
        defaults.set(appTheme.rawValue, forKey: themeKey)
    }
}

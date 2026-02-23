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

    @Published var apiKey: String = ""
    @Published var selectedModel: String = "gemini-2.0-flash-exp" // Default updated
    @Published var appTheme: AppTheme = .system

    private let defaults = UserDefaults.standard

    private let service = "CalorieWatcher.Gemini"
    private let account = "apiKey"

    private let modelKey = "selectedModel"
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
        // Load model selection from UserDefaults
        if let model = defaults.string(forKey: modelKey) {
            selectedModel = model
        }
        // Load theme from UserDefaults
        if let themeRaw = defaults.string(forKey: themeKey), let theme = AppTheme(rawValue: themeRaw) {
            appTheme = theme
        }
        
        // Load API key from Keychain
        if let data = try? KeychainHelper.load(service: service, account: account),
           let key = String(data: data, encoding: .utf8) {
            apiKey = key
        }
    }

    func save() {
        // Save model selection to UserDefaults
        defaults.set(selectedModel, forKey: modelKey)
        // Save theme selection to UserDefaults
        defaults.set(appTheme.rawValue, forKey: themeKey)
        
        // Save API key to Keychain
        if let data = apiKey.data(using: .utf8) {
            try? KeychainHelper.save(data, service: service, account: account)
        }
    }
}

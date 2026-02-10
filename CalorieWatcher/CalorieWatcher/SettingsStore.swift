import Foundation

final class SettingsStore: ObservableObject {
    static let shared = SettingsStore()

    @Published var apiKey: String = ""
    @Published var selectedModel: String = "gemini-1.5-flash"

    private let defaults = UserDefaults.standard

    private let service = "CalorieWatcher.Gemini"
    private let account = "apiKey"

    private let modelKey = "selectedModel"

    private init() {
        load()
    }

    func load() {
        // Load model selection from UserDefaults
        if let model = defaults.string(forKey: modelKey) {
            selectedModel = model
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
        // Save API key to Keychain
        if let data = apiKey.data(using: .utf8) {
            try? KeychainHelper.save(data, service: service, account: account)
        }
    }
}

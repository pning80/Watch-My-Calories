import SwiftUI

struct SettingsView: View {
    @State private var apiKey: String = ""
    @State private var selectedModel: String = "gemini-1.5-flash"

    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Gemini API"), footer: Text("Your key is stored securely on device. We do not send your data to our servers.")) {
                    SecureField("API Key", text: $apiKey)
                        .textContentType(.password)
                    Picker("Model", selection: $selectedModel) {
                        Text("1.5 Flash").tag("gemini-1.5-flash")
                        Text("1.5 Pro").tag("gemini-1.5-pro")
                        Text("1.5 Flash Lite").tag("gemini-1.5-flash-lite")
                    }
                    Link("Get an API key", destination: URL(string: "https://aistudio.google.com/app/apikey")!)
                }

                Section("About") {
                    Text("Calorie Watcher keeps your data on-device.")
                }
            }
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        // TODO: Save to Keychain and preferences
                    }
                }
            }
        }
    }
}

#Preview {
    SettingsView()
}

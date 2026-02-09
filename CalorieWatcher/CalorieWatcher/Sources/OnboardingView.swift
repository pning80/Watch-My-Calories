import SwiftUI

struct OnboardingView: View {
    @Binding var isOnboardingComplete: Bool
    @State private var apiKey: String = ""
    @State private var showTutorial = false

    var body: some View {
        VStack(spacing: 20) {
            Text("Welcome to Calorie Watcher")
                .font(.largeTitle)
                .bold()
                .padding(.top, 40)

            Text("To get started, you need a free Google Gemini API Key.")
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Button(action: {
                if let url = URL(string: "https://aistudio.google.com/app/apikey") {
                    UIApplication.shared.open(url)
                }
            }) {
                Text("Get Free API Key")
                    .bold()
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            .padding(.horizontal)

             // Tutorial Section (Placeholder for GIF)
            VStack {
                Text("How to create a key:")
                    .font(.caption)
                    .foregroundColor(.secondary)
                HStack {
                    Image(systemName: "1.circle.fill")
                    Text("Tap 'Create API Key'")
                }
                HStack {
                    Image(systemName: "2.circle.fill")
                    Text("Copy the key")
                }
            }
            .padding()
            .background(Color.gray.opacity(0.1))
            .cornerRadius(10)

            TextField("Paste your API Key here", text: $apiKey)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding(.horizontal)
                .onChange(of: apiKey) { oldValue, newValue in
                    if newValue.starts(with: "AIza") {
                        // Optimistic validation
                    }
                }

            Button("Auto-Paste from Clipboard") {
                if let string = UIPasteboard.general.string {
                    if string.starts(with: "AIza") {
                        apiKey = string
                    }
                }
            }
            .font(.caption)

            Spacer()

            Button("Continue") {
                if !apiKey.isEmpty {
                    KeychainService.save(key: apiKey)
                    isOnboardingComplete = true
                }
            }
            .disabled(apiKey.isEmpty)
            .padding(.bottom, 40)
        }
    }
}

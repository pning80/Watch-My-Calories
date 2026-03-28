import SwiftUI

/// A self-contained menu button that can be placed anywhere in the view hierarchy.
/// Owns its own sheet state for Scanned Menus, About, and Settings.
struct AppMenuButton: View {
    @State private var showAbout = false
    @State private var showSettings = false

    var body: some View {
        Menu {
            Button {
                showSettings = true
            } label: {
                Label("Settings", systemImage: "gearshape")
            }

            Button {
                showAbout = true
            } label: {
                Label("About", systemImage: "info.circle")
            }
        } label: {
            Image(systemName: "ellipsis.circle")
                .font(.system(size: 20))
                .foregroundStyle(Color.cwPrimary)
        }
        .accessibilityIdentifier(AccessibilityID.AppMenu.menuButton)
        .sheet(isPresented: $showAbout) {
            NavigationStack {
                AboutView()
                    .toolbar {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button("Done") { showAbout = false }
                        }
                    }
            }
        }
        .sheet(isPresented: $showSettings) {
            SettingsView()
        }
    }
}

import SwiftUI

struct ScanMenuSheet: View {
    var onScanMenu: () -> Void
    var onChooseFromLibrary: () -> Void
    var onStoredMenus: () -> Void

    @State private var sheetHeight: CGFloat = 250

    var body: some View {
        VStack(spacing: 0) {
            // Handle bar
            Capsule()
                .fill(Color.secondary.opacity(0.3))
                .frame(width: 36, height: 5)
                .padding(.top, 8)
                .padding(.bottom, 20)

            Text("Scan Menu")
                .font(.system(.title3, design: .serif, weight: .bold))
                .foregroundStyle(Color.cwPrimary)
                .padding(.bottom, 20)

            VStack(spacing: 12) {
                optionButton(
                    title: "Scan Menu",
                    subtitle: "Photograph a restaurant menu",
                    icon: "doc.viewfinder",
                    action: onScanMenu
                )

                optionButton(
                    title: "Choose from Library",
                    subtitle: "Select a photo from your library",
                    icon: "photo.on.rectangle",
                    action: onChooseFromLibrary
                )

                optionButton(
                    title: "Stored Menus",
                    subtitle: "View previously scanned menus",
                    icon: "menucard",
                    action: onStoredMenus
                )
            }
            .padding(.horizontal)
            .padding(.bottom, 24)
        }
        .background(
            GeometryReader { geo in
                Color.clear.preference(key: SheetHeightKey.self, value: geo.size.height)
            }
        )
        .onPreferenceChange(SheetHeightKey.self) { sheetHeight = $0 }
        .presentationDetents([.height(sheetHeight)])
    }

    private func optionButton(title: String, subtitle: String, icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundStyle(Color.cwPrimary)
                    .frame(width: 44, height: 44)
                    .background(Color.cwPrimary.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.body)
                        .fontWeight(.semibold)
                        .foregroundStyle(Color.cwTextPrimary)
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
            .padding()
            .background(Color.cwSurface)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 2)
        }
        .buttonStyle(.plain)
    }
}

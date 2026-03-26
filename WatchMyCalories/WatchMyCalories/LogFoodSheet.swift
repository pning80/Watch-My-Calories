import SwiftUI

struct LogFoodSheet: View {
    var onScanFood: () -> Void
    var onChooseFromLibrary: () -> Void
    var onLogManually: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // Handle bar
            Capsule()
                .fill(Color.secondary.opacity(0.3))
                .frame(width: 36, height: 5)
                .padding(.top, 8)
                .padding(.bottom, 20)

            Text("Log Food")
                .font(.system(.title3, design: .serif, weight: .bold))
                .foregroundStyle(Color.cwPrimary)
                .padding(.bottom, 20)

            VStack(spacing: 12) {
                logOptionButton(
                    title: "Scan Food",
                    subtitle: "Take a photo of your meal",
                    icon: "camera.fill",
                    action: onScanFood
                )

                logOptionButton(
                    title: "Choose from Library",
                    subtitle: "Select a photo from your library",
                    icon: "photo.on.rectangle",
                    action: onChooseFromLibrary
                )

                logOptionButton(
                    title: "Log Manually",
                    subtitle: "Enter food details by hand",
                    icon: "square.and.pencil",
                    action: onLogManually
                )
            }
            .padding(.horizontal)

            Spacer()
        }
        .presentationDetents([.medium])
    }

    private func logOptionButton(title: String, subtitle: String, icon: String, action: @escaping () -> Void) -> some View {
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

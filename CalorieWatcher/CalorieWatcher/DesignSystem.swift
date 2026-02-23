import SwiftUI

// MARK: - Color Palette
extension Color {
    static let cwPrimary = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(red: 0.4, green: 0.8, blue: 0.6, alpha: 1.0) : UIColor(red: 0.18, green: 0.42, blue: 0.31, alpha: 1.0)
    })
    static let cwSecondary = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(red: 0.15, green: 0.3, blue: 0.2, alpha: 1.0) : UIColor(red: 0.85, green: 0.95, blue: 0.86, alpha: 1.0)
    })
    static let cwAccent = Color(red: 1.0, green: 0.62, blue: 0.11) // Energetic Orange
    static let cwBackground = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor.systemBackground : UIColor(red: 0.98, green: 0.98, blue: 0.98, alpha: 1.0)
    })
    static let cwSurface = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor.secondarySystemBackground : UIColor.white
    })
    static let cwTextPrimary = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor.label : UIColor(red: 0.1, green: 0.1, blue: 0.1, alpha: 1.0)
    })
}

// MARK: - Typography Modifiers
struct CWTitleModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .font(.system(.largeTitle, design: .serif))
            .fontWeight(.bold)
            .foregroundStyle(Color.cwPrimary)
            .lineLimit(1)
            .minimumScaleFactor(0.5)
    }
}

struct CWCardModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding()
            .background(Color.cwSurface)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .shadow(color: Color.black.opacity(0.05), radius: 10, x: 0, y: 4)
    }
}

extension View {
    func cwTitle() -> some View {
        modifier(CWTitleModifier())
    }
    
    func cwCard() -> some View {
        modifier(CWCardModifier())
    }
}

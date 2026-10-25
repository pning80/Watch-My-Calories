import SwiftUI

// MARK: - Color Palette
extension Color {
    // Brand
    static let cwPrimary = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(red: 0.4, green: 0.8, blue: 0.6, alpha: 1.0) : UIColor(red: 0.18, green: 0.42, blue: 0.31, alpha: 1.0)
    })
    static let cwSecondary = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(red: 0.15, green: 0.3, blue: 0.2, alpha: 1.0) : UIColor(red: 0.85, green: 0.95, blue: 0.86, alpha: 1.0)
    })
    static let cwAccent = Color(UIColor { tc in
        // Energetic Orange — now dark-aware (slightly brighter in dark for contrast on dark surfaces)
        tc.userInterfaceStyle == .dark ? UIColor(red: 1.0, green: 0.68, blue: 0.26, alpha: 1.0) : UIColor(red: 1.0, green: 0.62, blue: 0.11, alpha: 1.0)
    })
    static let cwBackground = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor.systemBackground : UIColor(red: 0.98, green: 0.98, blue: 0.98, alpha: 1.0)
    })
    static let cwSurface = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor.secondarySystemBackground : UIColor.white
    })
    static let cwTextPrimary = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor.label : UIColor(red: 0.1, green: 0.1, blue: 0.1, alpha: 1.0)
    })

    // Secondary / tertiary text — replaces scattered raw Color.gray
    static let cwTextSecondary = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor.secondaryLabel : UIColor(red: 0.43, green: 0.43, blue: 0.45, alpha: 1.0)
    })
    static let cwTextTertiary = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor.tertiaryLabel : UIColor(red: 0.6, green: 0.6, blue: 0.62, alpha: 1.0)
    })

    // State colors
    static let cwWarning = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(red: 1.0, green: 0.7, blue: 0.28, alpha: 1.0) : UIColor(red: 0.9, green: 0.55, blue: 0.1, alpha: 1.0)
    })
    static let cwOverGoal = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(red: 1.0, green: 0.46, blue: 0.42, alpha: 1.0) : UIColor(red: 0.84, green: 0.25, blue: 0.21, alpha: 1.0)
    })
    static let cwLink = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(red: 0.42, green: 0.78, blue: 0.95, alpha: 1.0) : UIColor(red: 0.0, green: 0.48, blue: 0.75, alpha: 1.0)
    })

    // Macro triad — intentionally distinct from the ring's green (cwPrimary) / orange (cwAccent)
    // so a single color never means two things (protein≠consumed, carbs≠burned, etc.)
    static let cwMacroProtein = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(red: 0.55, green: 0.57, blue: 0.95, alpha: 1.0) : UIColor(red: 0.35, green: 0.36, blue: 0.82, alpha: 1.0)
    })
    static let cwMacroCarbs = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(red: 0.28, green: 0.74, blue: 0.72, alpha: 1.0) : UIColor(red: 0.0, green: 0.57, blue: 0.56, alpha: 1.0)
    })
    static let cwMacroFat = Color(UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(red: 0.78, green: 0.55, blue: 0.9, alpha: 1.0) : UIColor(red: 0.6, green: 0.35, blue: 0.72, alpha: 1.0)
    })
}

// MARK: - Spacing tokens
/// Named spacing scale (mirrors the Android ui/theme/Spacing.kt approach) + semantic aliases.
enum CWSpacing {
    static let xs: CGFloat = 4
    static let s: CGFloat = 8
    static let m: CGFloat = 12
    static let l: CGFloat = 16
    static let xl: CGFloat = 24
    static let xxl: CGFloat = 40

    // Semantic
    static let pageHorizontal: CGFloat = 16
    static let cardContent: CGFloat = 16
    static let cardGap: CGFloat = 20
    static let sectionGap: CGFloat = 24
}

// MARK: - Corner radius tokens
/// Collapses the previously-scattered 9 ad-hoc radii into a 3-step scale.
enum CWRadius {
    static let tile: CGFloat = 12   // rows, thumbnails, small controls
    static let card: CGFloat = 16   // standard cards
    static let hero: CGFloat = 24   // branded hero / large surfaces
}

// MARK: - Elevation / surface
/// A 3-step elevation system. Routes every card through one modifier instead of
/// open-coding background + clip + shadow per component.
enum CWSurfaceRole {
    case row, card, hero

    var radius: CGFloat {
        switch self {
        case .row: CWRadius.tile
        case .card: CWRadius.card
        case .hero: CWRadius.hero
        }
    }
    var shadowOpacity: Double {
        switch self {
        case .row: 0.04
        case .card: 0.05
        case .hero: 0.05
        }
    }
    var shadowRadius: CGFloat {
        switch self {
        case .row: 4
        case .card: 8
        case .hero: 10
        }
    }
    var shadowY: CGFloat {
        switch self {
        case .row: 2
        case .card: 3
        case .hero: 4
        }
    }
}

struct CWSurfaceModifier: ViewModifier {
    let role: CWSurfaceRole
    func body(content: Content) -> some View {
        content
            .background(Color.cwSurface)
            .clipShape(RoundedRectangle(cornerRadius: role.radius, style: .continuous))
            .shadow(color: Color.black.opacity(role.shadowOpacity), radius: role.shadowRadius, x: 0, y: role.shadowY)
    }
}

// MARK: - Typography Modifiers
enum CWTitleRole { case large, compact }

struct CWTitleModifier: ViewModifier {
    var role: CWTitleRole = .large
    func body(content: Content) -> some View {
        content
            .font(.system(role == .large ? .largeTitle : .title2, design: .serif))
            .fontWeight(.bold)
            .foregroundStyle(Color.cwPrimary)
            .lineLimit(1)
            .minimumScaleFactor(role == .large ? 0.5 : 0.8)
    }
}

/// Rounded-heavy numeral styling that scales with Dynamic Type (no hardcoded point sizes).
enum CWNumeralRole { case dayTotal, hero, large }

struct CWNumeralModifier: ViewModifier {
    var role: CWNumeralRole = .hero
    func body(content: Content) -> some View {
        let style: Font.TextStyle = switch role {
        case .dayTotal: .title3
        case .hero: .title
        case .large: .largeTitle
        }
        return content
            .font(.system(style, design: .rounded))
            .fontWeight(.heavy)
    }
}

extension View {
    func cwTitle(role: CWTitleRole = .large) -> some View {
        modifier(CWTitleModifier(role: role))
    }

    func cwNumeral(_ role: CWNumeralRole = .hero) -> some View {
        modifier(CWNumeralModifier(role: role))
    }

    /// Background + rounded clip + themed shadow for the given elevation role.
    func cwSurface(role: CWSurfaceRole = .card) -> some View {
        modifier(CWSurfaceModifier(role: role))
    }

    /// Padded branded hero surface — the original cwCard look (content padding + rounded-24 + soft shadow).
    func cwCard() -> some View {
        self
            .padding(CWSpacing.cardContent)
            .cwSurface(role: .hero)
    }
}

// MARK: - Primary button style
/// Single source of truth for the app's primary action button (previously copy-pasted across 6 files).
struct CWPrimaryButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Color.cwPrimary.opacity(isEnabled ? 1.0 : 0.5))
            .clipShape(RoundedRectangle(cornerRadius: CWRadius.card, style: .continuous))
            .opacity(configuration.isPressed ? 0.85 : 1.0)
            .scaleEffect(configuration.isPressed ? 0.98 : 1.0)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

// MARK: - Shared empty / status state
/// One reusable big-glyph status view, replacing the 64pt SF Symbol block duplicated across 7 files.
/// The glyph scales with Dynamic Type via @ScaledMetric.
struct CWEmptyState<Actions: View>: View {
    @ScaledMetric(relativeTo: .largeTitle) private var glyphSize: CGFloat = 64
    let symbol: String
    let title: String
    var message: String?
    var tint: Color
    @ViewBuilder var actions: () -> Actions

    init(symbol: String, title: String, message: String? = nil, tint: Color = .cwPrimary, @ViewBuilder actions: @escaping () -> Actions) {
        self.symbol = symbol
        self.title = title
        self.message = message
        self.tint = tint
        self.actions = actions
    }

    var body: some View {
        VStack(spacing: CWSpacing.l) {
            Image(systemName: symbol)
                .font(.system(size: glyphSize))
                .foregroundStyle(tint)
            Text(title)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundStyle(Color.cwTextPrimary)
            if let message {
                Text(message)
                    .font(.subheadline)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(Color.cwTextSecondary)
            }
            actions()
                .padding(.top, CWSpacing.s)
        }
        .padding(CWSpacing.xl)
    }
}

extension CWEmptyState where Actions == EmptyView {
    init(symbol: String, title: String, message: String? = nil, tint: Color = .cwPrimary) {
        self.init(symbol: symbol, title: title, message: message, tint: tint, actions: { EmptyView() })
    }
}

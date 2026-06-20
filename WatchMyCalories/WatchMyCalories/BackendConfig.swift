import Foundation

/// Compile-time backend configuration.
///
/// The dev-only legacy `x-backend-key` is **not** hardcoded here. The single
/// source of truth is `Backend/.env.dev` (`APP_BACKEND_API_KEY`). In DEBUG it is
/// injected at build time via `Ads/AdMob-iOS.xcconfig` → `Info.plist`
/// (`AppBackendApiKey`); run `scripts/sync-dev-backend-key.sh` to populate the
/// gitignored `Ads/AdMob-iOS.local.xcconfig` from the source. In RELEASE there
/// is no legacy key — production authenticates with App Attest, and the backend
/// rejects `x-backend-key` outright (`BACKEND_ENV=prod`).
enum BackendConfig {

    /// Cloud Run base URL (no trailing slash).
    #if DEBUG
    static let baseURL = "https://watchmycalories-backend-dev-657698311127.us-central1.run.app"
    #else
    static let baseURL = "https://watchmycalories-backend-657698311127.us-central1.run.app"
    #endif

    /// Dev-only legacy backend key. Empty in RELEASE (App Attest is used instead).
    /// Only sent when App Attest is unsupported (simulator / dev fallback).
    static var apiKey: String {
        #if DEBUG
        return Bundle.main.infoDictionary?["AppBackendApiKey"] as? String ?? ""
        #else
        return ""
        #endif
    }
}

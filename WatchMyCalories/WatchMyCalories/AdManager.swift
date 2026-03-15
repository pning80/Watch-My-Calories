import Foundation
import GoogleMobileAds
import UserMessagingPlatform
import AppTrackingTransparency

final class AdManager: ObservableObject {
    static let shared = AdManager()

    @Published var canRequestAds: Bool = false

    static var isUITestingMode: Bool {
        WatchMyCaloriesApp.isUITesting
    }

    private static let userAllowedAdsKey = "userAllowedAds"
    private static let adReminderDismissedDateKey = "adReminderDismissedDate"

    /// Date the user last dismissed the ad tracking reminder.
    var adReminderDismissedDate: Date? {
        get { UserDefaults.standard.object(forKey: Self.adReminderDismissedDateKey) as? Date }
        set { UserDefaults.standard.set(newValue, forKey: Self.adReminderDismissedDateKey) }
    }

    /// Whether the ad tracking reminder should be shown.
    /// True when: ads not disabled, user hasn't allowed ads, and 7+ days since last dismissal.
    var shouldShowAdReminder: Bool {
        guard !Self.isUITestingMode else { return false }
        guard !userAllowedAds else { return false }
        if let lastDismissed = adReminderDismissedDate {
            return Date().timeIntervalSince(lastDismissed) > 24 * 60 * 60
        }
        return true
    }

    /// Whether the user explicitly allowed ads (persisted across launches).
    /// Defaults to `false` — ads are opt-in.
    var userAllowedAds: Bool {
        get { UserDefaults.standard.bool(forKey: Self.userAllowedAdsKey) }
        set { UserDefaults.standard.set(newValue, forKey: Self.userAllowedAdsKey) }
    }

    // MARK: - Ad Unit IDs

    #if DEBUG
    static let bannerAdUnitID = "ca-app-pub-3940256099942544/2934735716"
    static let nativeAdUnitID = "ca-app-pub-3940256099942544/3986624511"
    #else
    static let bannerAdUnitID = "ca-app-pub-FIXME/FIXME"
    static let nativeAdUnitID = "ca-app-pub-FIXME/FIXME"
    #endif

    private init() {
        guard !Self.isUITestingMode else { return }
    }

    // MARK: - ATT

    func requestATTPermission() async {
        guard !Self.isUITestingMode else { return }
        await withCheckedContinuation { continuation in
            ATTrackingManager.requestTrackingAuthorization { _ in
                continuation.resume()
            }
        }
    }

    // MARK: - UMP Consent

    func gatherConsent() async {
        guard !Self.isUITestingMode else { return }
        guard userAllowedAds else { return }

        let params = RequestParameters()
        params.isTaggedForUnderAgeOfConsent = false

        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            ConsentInformation.shared.requestConsentInfoUpdate(with: params) { error in
                if let error {
                    print("[AdManager] Consent info update error: \(error.localizedDescription)")
                    continuation.resume()
                    return
                }

                Task { @MainActor in
                    guard let windowScene = UIApplication.shared.connectedScenes
                        .compactMap({ $0 as? UIWindowScene })
                        .first,
                        let rootVC = windowScene.windows.first?.rootViewController else {
                        continuation.resume()
                        return
                    }

                    ConsentForm.loadAndPresentIfRequired(from: rootVC) { formError in
                        if let formError {
                            print("[AdManager] Consent form error: \(formError.localizedDescription)")
                        }
                        continuation.resume()
                    }
                }
            }
        }

        let canRequest = ConsentInformation.shared.canRequestAds
        if canRequest {
            startSDK()
        }
    }

    // MARK: - SDK Start

    private func startSDK() {
        guard !Self.isUITestingMode else { return }
        MobileAds.shared.start { _ in
            Task { @MainActor in
                self.canRequestAds = true
            }
        }
    }
}

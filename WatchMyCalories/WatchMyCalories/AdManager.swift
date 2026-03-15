import Foundation
import GoogleMobileAds
import UserMessagingPlatform

final class AdManager: ObservableObject {
    static let shared = AdManager()

    @Published var canRequestAds: Bool = false

    static var isUITestingMode: Bool {
        WatchMyCaloriesApp.isUITesting
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

    // MARK: - UMP Consent

    func gatherConsent() async {
        guard !Self.isUITestingMode else { return }

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

    // MARK: - Enable Ads

    /// Gathers UMP consent and starts the SDK (non-personalized ads only).
    func enableAds() async {
        guard !Self.isUITestingMode else { return }
        await gatherConsent()
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
